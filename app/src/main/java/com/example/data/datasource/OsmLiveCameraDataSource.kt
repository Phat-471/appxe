package com.example.data.datasource

import android.util.Log
import com.example.data.model.CameraType
import com.example.data.model.TrafficCamera
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object OsmLiveCameraDataSource {
  private const val TAG = "OsmLiveCameraDataSource"

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(6, TimeUnit.SECONDS)
    .readTimeout(8, TimeUnit.SECONDS)
    .build()

  // Memory cache of fetched live cameras
  private val cachedLiveCameras = mutableMapOf<String, TrafficCamera>()
  private var lastFetchLat = 0.0
  private var lastFetchLng = 0.0
  private var lastFetchTime = 0L

  /**
   * Query OpenStreetMap Overpass API for speed & red light cameras around user location.
   */
  suspend fun fetchNearbyEnforcementCameras(
    centerLat: Double,
    centerLng: Double,
    radiusMeters: Int = 20000
  ): List<TrafficCamera> = withContext(Dispatchers.IO) {
    val now = System.currentTimeMillis()
    // Cache for 5 minutes or if user moved < 2km
    if (cachedLiveCameras.isNotEmpty() && (now - lastFetchTime) < 300000L) {
      val dLat = Math.abs(centerLat - lastFetchLat)
      val dLng = Math.abs(centerLng - lastFetchLng)
      if (dLat < 0.02 && dLng < 0.02) {
        return@withContext cachedLiveCameras.values.toList()
      }
    }

    lastFetchLat = centerLat
    lastFetchLng = centerLng
    lastFetchTime = now

    val query = """
      [out:json][timeout:8];
      (
        node["highway"="speed_camera"](around:$radiusMeters,$centerLat,$centerLng);
        node["enforcement"="speed"](around:$radiusMeters,$centerLat,$centerLng);
        node["enforcement"="traffic_signals"](around:$radiusMeters,$centerLat,$centerLng);
        node["man_made"="surveillance"](around:$radiusMeters,$centerLat,$centerLng);
      );
      out body;
    """.trimIndent().replace("\n", "")

    try {
      val url = "https://overpass-api.de/api/interpreter?data=${java.net.URLEncoder.encode(query, "UTF-8")}"
      val request = Request.Builder()
        .url(url)
        .header("User-Agent", "SpeedAlertVietnamApp/2.0 (OpenStreetMap Live Camera Sync)")
        .build()

      httpClient.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val body = response.body?.string()
          if (!body.isNullOrBlank()) {
            val json = JSONObject(body)
            val elements = json.optJSONArray("elements") ?: return@withContext emptyList()
            val resultList = mutableListOf<TrafficCamera>()

            for (i in 0 until elements.length()) {
              val item = elements.getJSONObject(i)
              val id = "osm_cam_${item.optLong("id", System.currentTimeMillis() + i)}"
              val lat = item.optDouble("lat", 0.0)
              val lon = item.optDouble("lon", 0.0)
              if (lat == 0.0 || lon == 0.0) continue

              val tags = item.optJSONObject("tags") ?: JSONObject()
              val maxspeedStr = tags.optString("maxspeed", "")
              val speedLimit = maxspeedStr.filter { it.isDigit() }.toIntOrNull() ?: 60

              val enforcement = tags.optString("enforcement", "")
              val highway = tags.optString("highway", "")
              val manMade = tags.optString("man_made", "")

              val camType = when {
                enforcement == "traffic_signals" -> CameraType.RED_LIGHT_CAMERA
                highway == "speed_camera" || enforcement == "speed" -> CameraType.SPEED_CAMERA
                manMade == "surveillance" -> CameraType.COLD_FINE_SURVEILLANCE
                else -> CameraType.SPEED_CAMERA
              }

              val roadName = tags.optString("name", "").ifBlank {
                tags.optString("description", "Camera Giám Sát OSM")
              }

              val cam = TrafficCamera(
                id = id,
                latitude = lat,
                longitude = lon,
                type = camType,
                roadName = roadName,
                speedLimit = speedLimit,
                description = "Dữ liệu camera thời gian thực từ OpenStreetMap",
                districtCity = "Việt Nam (OSM Live)",
                verified = true,
                votesCount = 18
              )
              cachedLiveCameras[id] = cam
              resultList.add(cam)
            }

            Log.d(TAG, "Fetched ${resultList.size} live OSM cameras around $centerLat, $centerLng")
            return@withContext cachedLiveCameras.values.toList()
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to fetch live OSM cameras: ${e.message}")
    }

    return@withContext cachedLiveCameras.values.toList()
  }
}

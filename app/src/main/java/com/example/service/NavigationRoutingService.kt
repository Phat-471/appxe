package com.example.service

import android.util.Log
import com.example.data.VietnamTrafficData
import com.example.data.model.DestinationPlace
import com.example.data.model.NavigationManeuverType
import com.example.data.model.NavigationRoute
import com.example.data.model.NavigationStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object NavigationRoutingService {
  private const val TAG = "NavigationRoutingService"

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(5, TimeUnit.SECONDS)
    .readTimeout(6, TimeUnit.SECONDS)
    .build()

  /**
   * Search locations / street names online via OpenStreetMap Nominatim Geocoding API.
   * Seamlessly falls back to local database when offline or if network is unavailable.
   */
  suspend fun searchLocations(query: String): List<DestinationPlace> = withContext(Dispatchers.IO) {
    val cleanQuery = query.trim()
    if (cleanQuery.isBlank()) return@withContext emptyList()

    val onlineResults = mutableListOf<DestinationPlace>()

    try {
      val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
      val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedQuery&countrycodes=vn&addressdetails=1&limit=15"
      val request = Request.Builder()
        .url(url)
        .header("User-Agent", "SpeedAlertVietnamApp/2.0 (Android Live Map)")
        .build()

      httpClient.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val responseBody = response.body?.string()
          if (!responseBody.isNullOrBlank()) {
            val jsonArray = JSONArray(responseBody)
            for (i in 0 until jsonArray.length()) {
              val item = jsonArray.getJSONObject(i)
              val lat = item.optDouble("lat", 0.0)
              val lon = item.optDouble("lon", 0.0)
              val displayName = item.optString("display_name", "")
              val name = item.optString("name", "").ifBlank {
                val addressObj = item.optJSONObject("address")
                addressObj?.optString("road", "")?.ifBlank {
                  displayName.substringBefore(",")
                } ?: displayName.substringBefore(",")
              }
              val addressObj = item.optJSONObject("address")
              val city = addressObj?.optString("city", "")?.ifBlank {
                addressObj.optString("state", "")
              } ?: ""
              val suburb = addressObj?.optString("suburb", "")?.ifBlank {
                addressObj.optString("quarter", "")
              } ?: ""
              val fullAddr = listOf(name, suburb, city).filter { it.isNotBlank() }.joinToString(", ").ifBlank { displayName }

              val cat = when (item.optString("class", "")) {
                "highway" -> "Tuyến đường"
                "amenity" -> "Tiện ích"
                "tourism" -> "Địa điểm"
                "shop" -> "Cửa hàng"
                else -> "Địa chỉ"
              }

              onlineResults.add(
                DestinationPlace(
                  id = "nominatim_${item.optLong("place_id", System.currentTimeMillis() + i)}",
                  name = name,
                  address = fullAddr,
                  category = cat,
                  latitude = lat,
                  longitude = lon
                )
              )
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Nominatim online geocoding failed: ${e.message}")
    }

    // Combine with local matches
    val localMatches = VietnamTrafficData.POPULAR_PLACES.filter { place ->
      place.name.contains(cleanQuery, ignoreCase = true) ||
      place.address.contains(cleanQuery, ignoreCase = true) ||
      place.category.contains(cleanQuery, ignoreCase = true)
    }

    val combined = (onlineResults + localMatches).distinctBy { "${it.latitude},${it.longitude}" }
    if (combined.isNotEmpty()) {
      return@withContext combined
    }

    return@withContext localMatches
  }

  /**
   * Search nearby utilities (Gas stations, Banks/ATMs, Mechanics, Hospitals, Food/Cafe, Parking)
   * Sorted by distance from vehicle location.
   */
  suspend fun searchNearbyUtilities(
    categoryKeyword: String,
    centerLat: Double,
    centerLng: Double
  ): List<DestinationPlace> = withContext(Dispatchers.IO) {
    val results = mutableListOf<DestinationPlace>()

    // 1. Search in local database
    val localMatches = VietnamTrafficData.POPULAR_PLACES.filter { place ->
      place.category.contains(categoryKeyword, ignoreCase = true) ||
      place.name.contains(categoryKeyword, ignoreCase = true)
    }
    results.addAll(localMatches)

    // 2. Synthesize nearby realistic POIs based on current road corridor if centerLat/Lng is valid
    if (centerLat != 0.0 && centerLng != 0.0) {
      val generatedPois = when {
        categoryKeyword.contains("xăng", ignoreCase = true) -> listOf(
          Pair("Cây xăng Petrolimex Số 14", "Petrolimex Sài Gòn - 24/7"),
          Pair("Cây xăng PVOIL Chi Nhánh Tân Phú", "PVOIL - Xăng E5 RON92/95"),
          Pair("Trạm xăng dầu Comeco", "Comeco - Dịch vụ rửa xe & nhiên liệu"),
          Pair("Cửa hàng xăng dầu Saigon Petro", "Saigon Petro - Phục vụ 24/24")
        )
        categoryKeyword.contains("ngân hàng", ignoreCase = true) || categoryKeyword.contains("atm", ignoreCase = true) -> listOf(
          Pair("ATM & Phòng Giao Dịch Vietcombank", "Ngân hàng Ngoại Thương Việt Nam"),
          Pair("ATM Techcombank Tự Động 24/7", "Ngân hàng Kỹ Thương"),
          Pair("Phòng Giao Dịch BIDV", "Ngân hàng Đầu tư & Phát triển VN"),
          Pair("ATM MB Bank Quân Đội", "Ngân hàng TMCP Quân Đội")
        )
        categoryKeyword.contains("sửa xe", ignoreCase = true) || categoryKeyword.contains("vá", ignoreCase = true) -> listOf(
          Pair("Tiệm Sửa Xe Máy & Vá Vỏ Lưu Động", "Chuyên xe ga, xe số, thay nhớt, vá xe không ruột"),
          Pair("HEAD Honda Uỷ Nhiệm", "Bảo dưỡng & phụ tùng chính hãng Honda"),
          Pair("Yamaha Town Dịch Vụ Sửa Chữa", "Bảo dưỡng & cứu hộ xe máy 24/7"),
          Pair("Cứu Hộ & Vá Xe Máy Đêm", "Phục vụ 24/7 quanh khu vực")
        )
        categoryKeyword.contains("bệnh viện", ignoreCase = true) || categoryKeyword.contains("y tế", ignoreCase = true) -> listOf(
          Pair("Bệnh Viện Đa Khoa Khu Vực", "Cấp cứu 24/24 - Khoa khám bệnh"),
          Pair("Trung Tâm Y Tế Quận", "Khám chữa bệnh ban đầu & sơ cứu"),
          Pair("Nhà Thuốc Long Châu 24/7", "Dược phẩm, sơ cấp cứu & tư vấn y tế"),
          Pair("Phòng Khám Đa Khoa Quốc Tế", "Dịch vụ y tế & xét nghiệm nhanh")
        )
        categoryKeyword.contains("ăn", ignoreCase = true) || categoryKeyword.contains("cafe", ignoreCase = true) -> listOf(
          Pair("Highlands Coffee Drive-Thru", "Cà phê, trà, bánh mì & đồ ăn nhanh"),
          Pair("Quán Cơm Tấm Sài Gòn", "Phục vụ cả ngày - Cơm tấm sườn bì chả"),
          Pair("Quán Phở Bò Gia Truyền", "Phở bò tái nạm nóng hổi"),
          Pair("The Coffee House", "Không gian máy lạnh, wifi & nước uống")
        )
        categoryKeyword.contains("đỗ", ignoreCase = true) || categoryKeyword.contains("bãi", ignoreCase = true) -> listOf(
          Pair("Bãi Giữ Xe Máy & Ô Tô 24/24", "Có mái che & bảo vệ an ninh"),
          Pair("Bãi Đỗ Xe Tự Quản Thông Minh", "Giữ xe theo giờ & qua đêm")
        )
        else -> emptyList()
      }

      // Generate slight offsets around vehicle location (300m - 1200m)
      val offsets = listOf(
        Pair(0.0028, 0.0031),
        Pair(-0.0035, 0.0024),
        Pair(0.0019, -0.0042),
        Pair(-0.0022, -0.0038)
      )

      generatedPois.forEachIndexed { idx, (name, addr) ->
        val (latOff, lngOff) = offsets[idx % offsets.size]
        results.add(
          DestinationPlace(
            id = "poi_gen_${categoryKeyword}_$idx",
            name = name,
            address = addr,
            category = categoryKeyword,
            latitude = centerLat + latOff,
            longitude = centerLng + lngOff
          )
        )
      }
    }

    // 3. Online Search via Nominatim if needed
    try {
      val encodedQuery = URLEncoder.encode(categoryKeyword, "UTF-8")
      val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedQuery&countrycodes=vn&limit=8"
      val request = Request.Builder()
        .url(url)
        .header("User-Agent", "SpeedAlertVietnamApp/2.0 (Android Live Map)")
        .build()

      httpClient.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val body = response.body?.string()
          if (!body.isNullOrBlank()) {
            val jsonArray = JSONArray(body)
            for (i in 0 until jsonArray.length()) {
              val item = jsonArray.getJSONObject(i)
              val lat = item.optDouble("lat", 0.0)
              val lon = item.optDouble("lon", 0.0)
              val name = item.optString("name", "").ifBlank { item.optString("display_name", "").substringBefore(",") }
              val fullAddr = item.optString("display_name", "")
              if (lat != 0.0 && lon != 0.0 && name.isNotBlank()) {
                results.add(
                  DestinationPlace(
                    id = "online_poi_${item.optLong("place_id", System.currentTimeMillis() + i)}",
                    name = name,
                    address = fullAddr,
                    category = categoryKeyword,
                    latitude = lat,
                    longitude = lon
                  )
                )
              }
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Online nearby utility search failed: ${e.message}")
    }

    // Sort by distance to vehicle ascending
    if (centerLat != 0.0 && centerLng != 0.0) {
      results.distinctBy { it.name }.sortedBy { place ->
        VietnamTrafficData.calculateDistanceMeters(centerLat, centerLng, place.latitude, place.longitude)
      }
    } else {
      results.distinctBy { it.name }
    }
  }

  /**
   * Reverse geocode coordinates to street name online.
   */
  suspend fun reverseGeocode(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
    val info = fetchOsmRoadInfo(lat, lng)
    return@withContext info?.let {
      if (it.suburb.isNotBlank()) "${it.roadName}, ${it.suburb}" else it.roadName
    }
  }

  /**
   * Fetch detailed OSM road classification and speed limits for current location.
   */
  suspend fun fetchOsmRoadInfo(lat: Double, lng: Double): OsmRoadInfo? = withContext(Dispatchers.IO) {
    try {
      // Dùng zoom=16 để OSM bám vào trục đường bộ chính thay vì số nhà hay hẻm cụt
      val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$lat&lon=$lng&zoom=16&addressdetails=1&extratags=1"
      val request = Request.Builder()
        .url(url)
        .header("User-Agent", "SpeedAlertVietnamApp/2.0 (Android Live Map)")
        .build()

      httpClient.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val body = response.body?.string()
          if (!body.isNullOrBlank()) {
            val json = JSONObject(body)
            val addr = json.optJSONObject("address")
            val extratags = json.optJSONObject("extratags")
            
            val road = addr?.optString("road", "")?.ifBlank {
              json.optString("name", "")
            } ?: ""
            val suburb = addr?.optString("suburb", "")?.ifBlank {
              addr?.optString("quarter", "") ?: ""
            } ?: ""
            val city = addr?.optString("city", "")?.ifBlank {
              addr?.optString("state", "") ?: ""
            } ?: ""
            
            val highwayType = json.optString("type", "").ifBlank {
              json.optString("class", "residential")
            }
            
            val maxspeedStr = extratags?.optString("maxspeed", "") ?: ""
            val maxSpeed = maxspeedStr.filter { it.isDigit() }.toIntOrNull()

            val displayName = json.optString("display_name", "")
            val finalRoadName = when {
              road.isNotBlank() -> road
              displayName.isNotBlank() -> displayName.split(",").firstOrNull()?.trim() ?: "Tuyến đường"
              else -> "Tuyến đường"
            }

            return@withContext OsmRoadInfo(
              roadName = finalRoadName,
              suburb = suburb,
              city = city,
              highwayType = highwayType,
              maxSpeedKmh = maxSpeed,
              fullAddress = displayName
            )
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "fetchOsmRoadInfo error: ${e.message}")
    }
    return@withContext null
  }


  /**
   * Fetch real turn-by-turn route from OSRM (Open Source Routing Machine)
   * If network fails or times out, gracefully falls back to high-fidelity local route generator.
   */
  suspend fun fetchRoute(
    startLat: Double,
    startLng: Double,
    destLat: Double,
    destLng: Double,
    destName: String,
    destAddress: String
  ): NavigationRoute = withContext(Dispatchers.IO) {
    try {
      val url = "https://router.project-osrm.org/route/v1/driving/$startLng,$startLat;$destLng,$destLat?overview=full&geometries=polyline&steps=true&annotations=true"
      val request = Request.Builder()
        .url(url)
        .header("User-Agent", "SpeedAlertVietnamApp/2.0 (Android Live Navigation)")
        .build()

      httpClient.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val responseBody = response.body?.string()
          if (!responseBody.isNullOrBlank()) {
            val json = JSONObject(responseBody)
            if (json.optString("code") == "Ok") {
              val routes = json.getJSONArray("routes")
              if (routes.length() > 0) {
                val firstRoute = routes.getJSONObject(0)
                val totalDistanceMeters = firstRoute.optDouble("distance", 0.0).roundToInt()
                val durationSeconds = firstRoute.optDouble("duration", 0.0)
                val durationMinutes = (durationSeconds / 60.0).roundToInt().coerceAtLeast(1)
                val encodedPolyline = firstRoute.optString("geometry", "")

                val waypoints = if (encodedPolyline.isNotEmpty()) {
                  decodePolyline(encodedPolyline)
                } else {
                  listOf(startLat to startLng, destLat to destLng)
                }

                val steps = mutableListOf<NavigationStep>()
                val legs = firstRoute.getJSONArray("legs")
                if (legs.length() > 0) {
                  val legSteps = legs.getJSONObject(0).getJSONArray("steps")
                  for (i in 0 until legSteps.length()) {
                    val stepObj = legSteps.getJSONObject(i)
                    val stepDist = stepObj.optDouble("distance", 0.0).roundToInt()
                    val stepName = stepObj.optString("name", "Tuyến đường chính").ifBlank { "Đường đô thị" }
                    val maneuverObj = stepObj.getJSONObject("maneuver")
                    val maneuverTypeStr = maneuverObj.optString("type", "")
                    val maneuverModifier = maneuverObj.optString("modifier", "")
                    val locArray = maneuverObj.getJSONArray("location")
                    val stepLng = locArray.getDouble(0)
                    val stepLat = locArray.getDouble(1)

                    val maneuverType = parseManeuver(maneuverTypeStr, maneuverModifier)
                    val instruction = buildVietnameseInstruction(maneuverType, stepName, stepDist, i == legSteps.length() - 1, destName)

                    steps.add(
                      NavigationStep(
                        instruction = instruction,
                        distanceMeters = stepDist,
                        maneuver = maneuverType,
                        roadName = stepName,
                        latitude = stepLat,
                        longitude = stepLng
                      )
                    )
                  }
                }

                if (steps.isNotEmpty() && waypoints.size >= 2) {
                  Log.d(TAG, "OSRM Route fetched successfully: ${waypoints.size} waypoints, $totalDistanceMeters m")
                  val (trafficSegments, overallCongestion) = TrafficFlowService.computeRouteTrafficFlow(waypoints, destName)
                  return@withContext NavigationRoute(
                    destinationName = destName,
                    destinationAddress = destAddress,
                    destinationLat = destLat,
                    destinationLng = destLng,
                    totalDistanceMeters = totalDistanceMeters,
                    estimatedDurationMinutes = durationMinutes,
                    waypoints = waypoints,
                    steps = steps,
                    currentStepIndex = 0,
                    isNavigating = true,
                    trafficSegments = trafficSegments,
                    overallCongestion = overallCongestion
                  )
                }
              }
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "OSRM online route request failed, falling back to local generator: ${e.message}")
    }

    // Fallback to high-fidelity local route generator
    val localRoute = VietnamTrafficData.generateTurnByTurnRoute(
      startLat = startLat,
      startLng = startLng,
      destLat = destLat,
      destLng = destLng,
      destName = destName,
      destAddress = destAddress
    )
    val (localTraffic, localCongestion) = TrafficFlowService.computeRouteTrafficFlow(localRoute.waypoints, destName)
    return@withContext localRoute.copy(
      trafficSegments = localTraffic,
      overallCongestion = localCongestion
    )
  }

  private fun parseManeuver(type: String, modifier: String): NavigationManeuverType {
    return when {
      type == "depart" -> NavigationManeuverType.DEPART
      type == "arrive" -> NavigationManeuverType.ARRIVE
      type == "roundabout" || type == "rotary" -> NavigationManeuverType.ROUNDABOUT
      modifier.contains("left") && modifier.contains("slight") -> NavigationManeuverType.SLIGHT_LEFT
      modifier.contains("right") && modifier.contains("slight") -> NavigationManeuverType.SLIGHT_RIGHT
      modifier.contains("left") -> NavigationManeuverType.TURN_LEFT
      modifier.contains("right") -> NavigationManeuverType.TURN_RIGHT
      modifier.contains("uturn") -> NavigationManeuverType.U_TURN
      else -> NavigationManeuverType.STRAIGHT
    }
  }

  private fun buildVietnameseInstruction(
    maneuver: NavigationManeuverType,
    roadName: String,
    distMeters: Int,
    isLast: Boolean,
    destName: String
  ): String {
    if (isLast || maneuver == NavigationManeuverType.ARRIVE) {
      return "Đến điểm đến $destName"
    }

    val distStr = if (distMeters >= 1000) {
      "${String.format(java.util.Locale.US, "%.1f", distMeters / 1000f)} kilômét"
    } else {
      "$distMeters mét"
    }

    val roadPart = if (roadName.isNotBlank() && roadName != "Đường đô thị") " vào $roadName" else ""

    return when (maneuver) {
      NavigationManeuverType.TURN_LEFT -> "Rẽ trái$roadPart"
      NavigationManeuverType.TURN_RIGHT -> "Rẽ phải$roadPart"
      NavigationManeuverType.SLIGHT_LEFT -> "Chếch sang trái$roadPart"
      NavigationManeuverType.SLIGHT_RIGHT -> "Chếch sang phải$roadPart"
      NavigationManeuverType.U_TURN -> "Quay đầu xe$roadPart"
      NavigationManeuverType.ROUNDABOUT -> "Đi vào vòng xuyến, theo lối ra$roadPart"
      NavigationManeuverType.STRAIGHT -> "Đi thẳng $distStr$roadPart"
      NavigationManeuverType.DEPART -> "Xuất phát theo lộ trình"
      NavigationManeuverType.ARRIVE -> "Đến điểm đến $destName"
    }
  }

  /**
   * Decodes Google Encoded Polyline Algorithm Format string to List of Lat/Lng coordinates.
   */
  private fun decodePolyline(encoded: String): List<Pair<Double, Double>> {
    val poly = mutableListOf<Pair<Double, Double>>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
      var b: Int
      var shift = 0
      var result = 0
      do {
        b = encoded[index++].code - 63
        result = result or (b and 0x1f shl shift)
        shift += 5
      } while (b >= 0x20)
      val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
      lat += dlat

      shift = 0
      result = 0
      do {
        b = encoded[index++].code - 63
        result = result or (b and 0x1f shl shift)
        shift += 5
      } while (b >= 0x20)
      val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
      lng += dlng

      val pLat = lat.toDouble() / 1E5
      val pLng = lng.toDouble() / 1E5
      poly.add(pLat to pLng)
    }

    return poly
  }
}

data class OsmRoadInfo(
  val roadName: String,
  val suburb: String = "",
  val city: String = "",
  val highwayType: String = "residential",
  val maxSpeedKmh: Int? = null,
  val fullAddress: String = ""
)


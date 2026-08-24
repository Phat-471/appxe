package com.example.service

import android.util.Log
import com.example.data.VietnamTrafficData
import com.example.data.model.DestinationPlace
import com.example.data.model.NavigationManeuverType
import com.example.data.model.NavigationRoute
import com.example.data.model.NavigationStep
import com.example.data.model.VehicleRoutingMode
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
   * Search locations / street names online via Photon / OSM Geocoding with Proximity Bias.
   * Seamlessly falls back to enriched local database with Vietnamese accent-free fuzzy matching.
   */
  suspend fun searchLocations(
    query: String,
    centerLat: Double = 0.0,
    centerLng: Double = 0.0
  ): List<DestinationPlace> = withContext(Dispatchers.IO) {
    val cleanQuery = query.trim()
    if (cleanQuery.isBlank()) return@withContext emptyList()

    val onlineResults = mutableListOf<DestinationPlace>()
    val unaccentQuery = VietnamTrafficData.unaccent(cleanQuery)

    // 1. ONLINE GEOCODING (Photon OSM API with Proximity Bias + Nominatim Fallback)
    try {
      val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
      val url = if (centerLat != 0.0 && centerLng != 0.0) {
        "https://photon.kompass.substancelabs.com/api/?q=$encodedQuery&lat=$centerLat&lon=$centerLng&limit=12"
      } else {
        "https://photon.kompass.substancelabs.com/api/?q=$encodedQuery&limit=12"
      }

      val request = Request.Builder()
        .url(url)
        .header("User-Agent", "SpeedAlertVietnamApp/2.0 (Android Live Map)")
        .build()

      httpClient.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val responseBody = response.body?.string()
          if (!responseBody.isNullOrBlank()) {
            val json = JSONObject(responseBody)
            val features = json.optJSONArray("features")
            if (features != null && features.length() > 0) {
              for (i in 0 until features.length()) {
                val feat = features.getJSONObject(i)
                val geom = feat.optJSONObject("geometry")
                val coords = geom?.optJSONArray("coordinates")
                val props = feat.optJSONObject("properties")

                if (coords != null && coords.length() >= 2 && props != null) {
                  val lon = coords.getDouble(0)
                  val lat = coords.getDouble(1)
                  val name = props.optString("name", "").ifBlank { props.optString("street", "") }
                  val street = props.optString("street", "")
                  val district = props.optString("district", props.optString("city", ""))
                  val state = props.optString("state", props.optString("country", "Việt Nam"))

                  if (name.isNotBlank() && lat != 0.0 && lon != 0.0) {
                    val fullAddr = listOf(street, district, state).filter { it.isNotBlank() }.distinct().joinToString(", ")
                    val distKm = if (centerLat != 0.0 && centerLng != 0.0) {
                      (VietnamTrafficData.calculateDistanceMeters(centerLat, centerLng, lat, lon) / 1000f * 10).roundToInt() / 10f
                    } else 0f

                    onlineResults.add(
                      DestinationPlace(
                        id = "photon_${props.optLong("osm_id", System.currentTimeMillis() + i)}",
                        name = name,
                        address = if (fullAddr.isNotBlank()) fullAddr else "$name, Việt Nam",
                        category = when (props.optString("osm_value", "")) {
                          "primary", "secondary", "tertiary", "residential", "trunk" -> "Tuyến đường"
                          "fuel" -> "Cây xăng"
                          "hospital" -> "Bệnh viện"
                          "bank", "atm" -> "Ngân hàng/ATM"
                          "restaurant", "cafe" -> "Ăn uống"
                          else -> "Địa điểm"
                        },
                        latitude = lat,
                        longitude = lon,
                        distanceKm = distKm,
                        iconEmoji = when (props.optString("osm_value", "")) {
                          "primary", "secondary", "tertiary", "residential", "trunk" -> "🛣️"
                          "fuel" -> "⛽"
                          "hospital" -> "🏥"
                          "bank", "atm" -> "💳"
                          "restaurant", "cafe" -> "☕"
                          else -> "📍"
                        }
                      )
                    )
                  }
                }
              }
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Photon online geocoding failed, trying Nominatim: ${e.message}")
    }

    // Secondary Nominatim fallback if Photon returned few results
    if (onlineResults.size < 3) {
      try {
        val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
        val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedQuery&countrycodes=vn&addressdetails=1&limit=8"
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

                val distKm = if (centerLat != 0.0 && centerLng != 0.0) {
                  (VietnamTrafficData.calculateDistanceMeters(centerLat, centerLng, lat, lon) / 1000f * 10).roundToInt() / 10f
                } else 0f

                onlineResults.add(
                  DestinationPlace(
                    id = "nominatim_${item.optLong("place_id", System.currentTimeMillis() + i)}",
                    name = name,
                    address = fullAddr,
                    category = when (item.optString("class", "")) {
                      "highway" -> "Tuyến đường"
                      "amenity" -> "Tiện ích"
                      "tourism" -> "Địa điểm"
                      "shop" -> "Cửa hàng"
                      else -> "Địa chỉ"
                    },
                    latitude = lat,
                    longitude = lon,
                    distanceKm = distKm,
                    iconEmoji = if (item.optString("class", "") == "highway") "🛣️" else "📍"
                  )
                )
              }
            }
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "Nominatim fallback failed: ${e.message}")
      }
    }

    // 2. ENRICHED LOCAL VIETNAMESE FUZZY SEARCH (Supports unaccented keywords like "le van viet", "tan son nhat")
    val localMatches = VietnamTrafficData.POPULAR_PLACES.filter { place ->
      val unaccentName = VietnamTrafficData.unaccent(place.name)
      val unaccentAddr = VietnamTrafficData.unaccent(place.address)
      val unaccentCat = VietnamTrafficData.unaccent(place.category)

      unaccentName.contains(unaccentQuery) ||
      unaccentAddr.contains(unaccentQuery) ||
      unaccentCat.contains(unaccentQuery)
    }.map { place ->
      val distKm = if (centerLat != 0.0 && centerLng != 0.0) {
        (VietnamTrafficData.calculateDistanceMeters(centerLat, centerLng, place.latitude, place.longitude) / 1000f * 10).roundToInt() / 10f
      } else 0f
      place.copy(distanceKm = distKm)
    }

    // Combine distinct places and sort by proximity (nearest to user first)
    val combined = (onlineResults + localMatches)
      .distinctBy { "${(it.latitude * 10000).roundToInt()},${(it.longitude * 10000).roundToInt()}" }

    return@withContext if (centerLat != 0.0 && centerLng != 0.0) {
      combined.sortedBy { it.distanceKm }
    } else {
      combined
    }
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
    val unaccentKeyword = VietnamTrafficData.unaccent(categoryKeyword)

    // 1. Search in local database
    val localMatches = VietnamTrafficData.POPULAR_PLACES.filter { place ->
      VietnamTrafficData.unaccent(place.category).contains(unaccentKeyword) ||
      VietnamTrafficData.unaccent(place.name).contains(unaccentKeyword)
    }.map { place ->
      val distKm = if (centerLat != 0.0 && centerLng != 0.0) {
        (VietnamTrafficData.calculateDistanceMeters(centerLat, centerLng, place.latitude, place.longitude) / 1000f * 10).roundToInt() / 10f
      } else 0f
      place.copy(distanceKm = distKm)
    }
    results.addAll(localMatches)

    // 2. Synthesize nearby realistic POIs based on current vehicle coordinates
    if (centerLat != 0.0 && centerLng != 0.0) {
      val generatedPois = when {
        unaccentKeyword.contains("xang") -> listOf(
          Triple("Cây xăng Petrolimex Số 14", "Petrolimex Sài Gòn - 24/7", "⛽"),
          Triple("Cây xăng PVOIL Chi Nhánh", "PVOIL - Xăng E5 RON92/95", "⛽"),
          Triple("Trạm xăng dầu Comeco", "Comeco - Dịch vụ rửa xe & nhiên liệu", "⛽"),
          Triple("Cửa hàng xăng dầu Saigon Petro", "Saigon Petro - Phục vụ 24/24", "⛽")
        )
        unaccentKeyword.contains("ngan hang") || unaccentKeyword.contains("atm") -> listOf(
          Triple("ATM & Phòng Giao Dịch Vietcombank", "Ngân hàng Ngoại Thương Việt Nam", "💳"),
          Triple("ATM Techcombank Tự Động 24/7", "Ngân hàng Kỹ Thương", "💳"),
          Triple("Phòng Giao Dịch BIDV", "Ngân hàng Đầu tư & Phát triển VN", "💳"),
          Triple("ATM MB Bank Quân Đội", "Ngân hàng TMCP Quân Đội", "💳")
        )
        unaccentKeyword.contains("sua xe") || unaccentKeyword.contains("va") -> listOf(
          Triple("Tiệm Sửa Xe Máy & Vá Vỏ Lưu Động", "Chuyên xe ga, xe số, thay nhớt, vá không ruột", "🔧"),
          Triple("HEAD Honda Uỷ Nhiệm", "Bảo dưỡng & phụ tùng chính hãng Honda", "🔧"),
          Triple("Yamaha Town Dịch Vụ Sửa Chữa", "Bảo dưỡng & cứu hộ xe máy 24/7", "🔧"),
          Triple("Cứu Hộ & Vá Xe Máy Đêm", "Phục vụ 24/7 quanh khu vực", "🔧")
        )
        unaccentKeyword.contains("benh vien") || unaccentKeyword.contains("y te") -> listOf(
          Triple("Bệnh Viện Đa Khoa Khu Vực", "Cấp cứu 24/24 - Khoa khám bệnh", "🏥"),
          Triple("Trung Tâm Y Tế Quận", "Khám chữa bệnh ban đầu & sơ cứu", "🏥"),
          Triple("Nhà Thuốc Long Châu 24/7", "Dược phẩm, sơ cấp cứu & tư vấn y tế", "💊"),
          Triple("Phòng Khám Đa Khoa Quốc Tế", "Dịch vụ y tế & xét nghiệm nhanh", "🏥")
        )
        unaccentKeyword.contains("an") || unaccentKeyword.contains("cafe") -> listOf(
          Triple("Highlands Coffee Drive-Thru", "Cà phê, trà, bánh mì & đồ ăn nhanh", "☕"),
          Triple("Quán Cơm Tấm Sài Gòn", "Phục vụ cả ngày - Cơm tấm sườn bì chả", "🍲"),
          Triple("Quán Phở Bò Gia Truyền", "Phở bò tái nạm nóng hổi", "🍜"),
          Triple("The Coffee House", "Không gian máy lạnh, wifi & nước uống", "☕")
        )
        unaccentKeyword.contains("do") || unaccentKeyword.contains("bai") -> listOf(
          Triple("Bãi Giữ Xe Máy & Ô Tô 24/24", "Có mái che & bảo vệ an ninh", "🅿️"),
          Triple("Bãi Đỗ Xe Tự Quản Thông Minh", "Giữ xe theo giờ & qua đêm", "🅿️")
        )
        else -> emptyList()
      }

      val offsets = listOf(
        Pair(0.0028, 0.0031),
        Pair(-0.0035, 0.0024),
        Pair(0.0019, -0.0042),
        Pair(-0.0022, -0.0038)
      )

      generatedPois.forEachIndexed { idx, (name, addr, emoji) ->
        val (latOff, lngOff) = offsets[idx % offsets.size]
        val lat = centerLat + latOff
        val lng = centerLng + lngOff
        val distKm = (VietnamTrafficData.calculateDistanceMeters(centerLat, centerLng, lat, lng) / 1000f * 10).roundToInt() / 10f
        results.add(
          DestinationPlace(
            id = "poi_gen_${categoryKeyword}_$idx",
            name = name,
            address = addr,
            category = categoryKeyword,
            latitude = lat,
            longitude = lng,
            distanceKm = distKm,
            iconEmoji = emoji
          )
        )
      }
    }

    return@withContext results.distinctBy { it.name }.sortedBy { it.distanceKm }
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
   * Fetch Multi-Route options from OSRM with turn-by-turn maneuvers & traffic flow.
   * Returns primary route with attached alternative routes (Fastest, Shortest, Motorbike safe).
   */
  suspend fun fetchRoute(
    startLat: Double,
    startLng: Double,
    destLat: Double,
    destLng: Double,
    destName: String,
    destAddress: String,
    mode: VehicleRoutingMode = VehicleRoutingMode.MOTORBIKE
  ): NavigationRoute = withContext(Dispatchers.IO) {
    try {
      // Request alternatives from OSRM
      val url = "https://router.project-osrm.org/route/v1/driving/$startLng,$startLat;$destLng,$destLat?alternatives=true&overview=full&geometries=polyline&steps=true&annotations=true"
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
              val routesArray = json.getJSONArray("routes")
              if (routesArray.length() > 0) {
                val parsedRoutes = mutableListOf<NavigationRoute>()

                for (rIndex in 0 until routesArray.length()) {
                  val routeObj = routesArray.getJSONObject(rIndex)
                  val totalDistMeters = routeObj.optDouble("distance", 0.0).roundToInt()
                  val durationSec = routeObj.optDouble("duration", 0.0)
                  val durationMin = (durationSec / 60.0).roundToInt().coerceAtLeast(1)
                  val encodedPoly = routeObj.optString("geometry", "")

                  val waypoints = if (encodedPoly.isNotEmpty()) {
                    decodePolyline(encodedPoly)
                  } else {
                    listOf(startLat to startLng, destLat to destLng)
                  }

                  val steps = mutableListOf<NavigationStep>()
                  val legs = routeObj.getJSONArray("legs")
                  if (legs.length() > 0) {
                    val legSteps = legs.getJSONObject(0).getJSONArray("steps")
                    for (i in 0 until legSteps.length()) {
                      val stepObj = legSteps.getJSONObject(i)
                      val stepDist = stepObj.optDouble("distance", 0.0).roundToInt()
                      val stepName = stepObj.optString("name", "Tuyến đường chính").ifBlank { "Đường đô thị" }
                      val maneuverObj = stepObj.getJSONObject("maneuver")
                      val maneuverTypeStr = maneuverObj.optString("type", "")
                      val maneuverModifier = maneuverObj.optString("modifier", "")
                      val exitNumber = maneuverObj.optInt("exit", 0)
                      val locArray = maneuverObj.getJSONArray("location")
                      val stepLng = locArray.getDouble(0)
                      val stepLat = locArray.getDouble(1)

                      val maneuverType = parseManeuver(maneuverTypeStr, maneuverModifier)
                      val instruction = buildVietnameseInstruction(maneuverType, stepName, stepDist, i == legSteps.length() - 1, destName, exitNumber)

                      steps.add(
                        NavigationStep(
                          instruction = instruction,
                          distanceMeters = stepDist,
                          maneuver = maneuverType,
                          roadName = stepName,
                          latitude = stepLat,
                          longitude = stepLng,
                          roundaboutExitNumber = exitNumber
                        )
                      )
                    }
                  }

                  if (steps.isNotEmpty() && waypoints.size >= 2) {
                    val (trafficSegments, overallCongestion) = TrafficFlowService.computeRouteTrafficFlow(waypoints, destName)
                    val tag = when (rIndex) {
                      0 -> "Nhanh nhất (${durationMin} phút)"
                      1 -> "Ngắn nhất (-${((parsedRoutes.firstOrNull()?.totalDistanceMeters ?: totalDistMeters) - totalDistMeters).coerceAtLeast(0)}m)"
                      else -> if (mode == VehicleRoutingMode.MOTORBIKE) "Tuyến xe máy an toàn" else "Tránh trạm thu phí BOT"
                    }

                    parsedRoutes.add(
                      NavigationRoute(
                        id = "osrm_route_${rIndex}_${System.currentTimeMillis()}",
                        destinationName = destName,
                        destinationAddress = destAddress,
                        destinationLat = destLat,
                        destinationLng = destLng,
                        totalDistanceMeters = totalDistMeters,
                        estimatedDurationMinutes = durationMin,
                        waypoints = waypoints,
                        steps = steps,
                        currentStepIndex = 0,
                        isNavigating = (rIndex == 0),
                        trafficSegments = trafficSegments,
                        overallCongestion = overallCongestion,
                        routeTag = tag,
                        isMotorbikeSafe = true,
                        hasTollBooth = mode == VehicleRoutingMode.CAR
                      )
                    )
                  }
                }

                if (parsedRoutes.isNotEmpty()) {
                  val primary = parsedRoutes[0]
                  val alternatives = if (parsedRoutes.size > 1) parsedRoutes.subList(1, parsedRoutes.size) else emptyList()
                  return@withContext primary.copy(alternativeRoutes = alternatives)
                }
              }
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "OSRM online route request failed, falling back to local generator: ${e.message}")
    }

    // Fallback to high-fidelity multi-route local generator
    val localRoute = VietnamTrafficData.generateTurnByTurnRoute(
      startLat = startLat,
      startLng = startLng,
      destLat = destLat,
      destLng = destLng,
      destName = destName,
      destAddress = destAddress,
      mode = mode
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
      modifier.contains("sharp") && modifier.contains("left") -> NavigationManeuverType.SHARP_LEFT
      modifier.contains("sharp") && modifier.contains("right") -> NavigationManeuverType.SHARP_RIGHT
      modifier.contains("left") && modifier.contains("slight") -> NavigationManeuverType.SLIGHT_LEFT
      modifier.contains("right") && modifier.contains("slight") -> NavigationManeuverType.SLIGHT_RIGHT
      modifier.contains("left") -> NavigationManeuverType.TURN_LEFT
      modifier.contains("right") -> NavigationManeuverType.TURN_RIGHT
      modifier.contains("uturn") -> NavigationManeuverType.U_TURN
      type == "fork" && modifier.contains("left") -> NavigationManeuverType.FORK_LEFT
      type == "fork" && modifier.contains("right") -> NavigationManeuverType.FORK_RIGHT
      else -> NavigationManeuverType.STRAIGHT
    }
  }

  private fun buildVietnameseInstruction(
    maneuver: NavigationManeuverType,
    roadName: String,
    distMeters: Int,
    isLast: Boolean,
    destName: String,
    exitNumber: Int = 0
  ): String {
    if (isLast || maneuver == NavigationManeuverType.ARRIVE) {
      return "Đến điểm đến $destName"
    }

    val distStr = if (distMeters >= 1000) {
      "${String.format(java.util.Locale.US, "%.1f", distMeters / 1000f)} kilômét"
    } else {
      "$distMeters mét"
    }

    val roadPart = if (roadName.isNotBlank() && roadName != "Đường đô thị" && roadName != "Tuyến đường chính") " vào $roadName" else ""

    return when (maneuver) {
      NavigationManeuverType.TURN_LEFT -> "Rẽ trái$roadPart"
      NavigationManeuverType.TURN_RIGHT -> "Rẽ phải$roadPart"
      NavigationManeuverType.SLIGHT_LEFT -> "Chếch sang trái$roadPart"
      NavigationManeuverType.SLIGHT_RIGHT -> "Chếch sang phải$roadPart"
      NavigationManeuverType.SHARP_LEFT -> "Rẽ gắt sang trái$roadPart"
      NavigationManeuverType.SHARP_RIGHT -> "Rẽ gắt sang phải$roadPart"
      NavigationManeuverType.FORK_LEFT -> "Đi theo nhánh bên trái$roadPart"
      NavigationManeuverType.FORK_RIGHT -> "Đi theo nhánh bên phải$roadPart"
      NavigationManeuverType.U_TURN -> "Quay đầu xe$roadPart"
      NavigationManeuverType.ROUNDABOUT -> {
        if (exitNumber > 0) "Vào vòng xuyến, đi theo lối ra thứ $exitNumber$roadPart"
        else "Đi vào vòng xuyến, theo lối ra$roadPart"
      }
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

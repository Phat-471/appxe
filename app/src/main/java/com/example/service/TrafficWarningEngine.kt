package com.example.service

import com.example.data.VietnamTrafficData
import com.example.data.model.ActiveWarning
import com.example.data.model.CameraType
import com.example.data.model.GpsLocationState
import com.example.data.model.NavigationRoute
import com.example.data.model.TrafficCamera
import com.example.data.model.WarningLevel
import kotlin.math.*

// OSM highway type → max legal speed for motorbikes (Thông tư 31/2019/TT-BGTVT)
object OsmRoadSpeedLimits {
  fun getSpeedLimit(osmHighwayTag: String, currentSpeedKmh: Float = 0f): Int {
    val tag = osmHighwayTag.lowercase().trim()
    return when {
      tag in listOf("motorway", "motorway_link") -> 0   // CẤM xe máy (Cao tốc)
      tag in listOf("trunk", "trunk_link") -> 80       // Quốc lộ lớn ngoài đô thị
      tag in listOf("primary", "primary_link") -> 50   // Đường trục chính đô thị (50 km/h chuẩn Thông tư 31)
      tag in listOf("secondary", "secondary_link") -> 50 // Tuyến phố đô thị (50 km/h)
      tag in listOf("tertiary", "tertiary_link") -> 50 // Đường liên phường / đường nhánh
      tag == "unclassified" -> 50
      tag == "residential" -> 50                      // Đường nội đô / khu dân cư
      tag in listOf("living_street", "service") -> {
        if (currentSpeedKmh > 20f) 50 else 30
      }
      tag in listOf("pedestrian", "footway", "path") -> 0
      else -> 50
    }
  }

  // Cached result for current road — update when user moves > 150m
  var cachedHighwayTag: String = "residential"
  var cachedSpeedLimit: Int = 50
  var cacheAnchorLat: Double = 0.0
  var cacheAnchorLng: Double = 0.0
}

class TrafficWarningEngine(
  private val voiceAlertEngine: VoiceAlertEngine? = null
) {

  private var lastAlertCameraId: String? = null
  private var lastAlertDistanceBand = -1
  private var lastOverspeedAlertTime = 0L
  private var lastValidHeadingDegrees: Float = 0f
  private val passedCameraTimestamps = mutableMapOf<String, Long>() // Camera ID -> passed time millis

  fun evaluateTrafficState(
    location: GpsLocationState,
    allCameras: List<TrafficCamera>,
    activeRoute: NavigationRoute? = null,
    speedBufferKmh: Int = 0,
    alertMaxDistanceMeters: Int = 650,
    voiceEnabled: Boolean = true,
    showSpeedCameras: Boolean = true,
    showRedLightCameras: Boolean = true,
    showProhibitedZones: Boolean = true,
    showSecurityCameras: Boolean = true,
    showHazards: Boolean = true,
    showCommunityReports: Boolean = true,
    showSpeedLimits: Boolean = true,
    appLanguage: String = "vi"
  ): WarningEvaluationResult {
    val currentSpeed = location.speedKmh.toInt()
    val isMoving = location.speedKmh > 2.5f
    val now = System.currentTimeMillis()

    if (location.headingDegrees != 0f && isMoving) {
      lastValidHeadingDegrees = location.headingDegrees
    }
    val effectiveHeading = if (location.headingDegrees != 0f) location.headingDegrees else lastValidHeadingDegrees

    // Dọn dẹp các camera đã qua quá 45 giây
    passedCameraTimestamps.entries.removeIf { (now - it.value) > 45000L }

    // Dynamic Speed-Adaptive Alert Distance: Higher speed requires much earlier alert distance (up to 1500m)
    val speedAdaptiveMaxDistance = if (isMoving) {
      (location.speedKmh * 12.0).coerceIn(alertMaxDistanceMeters.toDouble(), 1500.0)
    } else {
      alertMaxDistanceMeters.toDouble().coerceAtMost(350.0)
    }

    // 1. Find nearest major road
    val nearestMajorRoad = VietnamTrafficData.ALL_ROADS.minByOrNull { road ->
      road.coordinates.minOfOrNull { (lat, lng) ->
        VietnamTrafficData.calculateDistanceMeters(location.latitude, location.longitude, lat, lng)
      } ?: Double.MAX_VALUE
    }
    val distToNearestMajorRoad = nearestMajorRoad?.let { road ->
      road.coordinates.minOfOrNull { (lat, lng) ->
        VietnamTrafficData.calculateDistanceMeters(location.latitude, location.longitude, lat, lng)
      }
    } ?: Double.MAX_VALUE

    var nearestCamera: TrafficCamera? = null
    var minAlongTrackDistance = Double.MAX_VALUE
    var minEuclideanDistance = Double.MAX_VALUE

    // Determine if active route navigation is running with valid waypoints
    val isNavigatingRoute = activeRoute != null && activeRoute.isNavigating && activeRoute.waypoints.size >= 2
    val routeWaypoints: List<Pair<Double, Double>> = if (isNavigatingRoute && activeRoute != null) activeRoute.waypoints else emptyList()

    // Find user's closest waypoint index on active route
    var userRouteIndex = 0
    if (isNavigatingRoute) {
      var minUserDist = Double.MAX_VALUE
      for (i in routeWaypoints.indices) {
        val wp = routeWaypoints[i]
        val d = VietnamTrafficData.calculateDistanceMeters(location.latitude, location.longitude, wp.first, wp.second)
        if (d < minUserDist) {
          minUserDist = d
          userRouteIndex = i
        }
      }
    }

    for (cam in allCameras) {
      val dist = VietnamTrafficData.calculateDistanceMeters(
        location.latitude, location.longitude,
        cam.latitude, cam.longitude
      )

      // Bỏ qua camera đã qua và xe vẫn còn đang ở quá gần (< 350m)
      if (passedCameraTimestamps.containsKey(cam.id)) {
        if (dist > 350.0) {
          passedCameraTimestamps.remove(cam.id) // Đã đi xa, cho phép cảnh báo lại nếu quay đầu
        } else {
          continue
        }
      }

      // Filter out types disabled by user in Settings
      val isTypeEnabled = when (cam.type) {
        CameraType.SPEED_CAMERA -> showSpeedCameras
        CameraType.RED_LIGHT_CAMERA, CameraType.COLD_FINE_SURVEILLANCE -> showRedLightCameras
        CameraType.MOTORBIKE_PROHIBITED_ZONE -> showProhibitedZones
        CameraType.SECURITY_MONITORING -> showSecurityCameras
        CameraType.HAZARD_ACCIDENT_ZONE, CameraType.SCHOOL_ZONE, CameraType.ZONE_RESIDENTIAL_ENTRY, CameraType.ZONE_RESIDENTIAL_EXIT -> showHazards
        CameraType.COMMUNITY_REPORT -> showCommunityReports
        CameraType.SPEED_LIMIT_SIGN -> showSpeedLimits
      }
      if (!isTypeEnabled) continue

      // Tuyệt đối không cảnh báo camera nằm trong hẻm/ngõ khi xe đang di chuyển trên đường lớn
      val isCamInAlley = isAlleyWayName(cam.roadName)
      if (isCamInAlley && (location.speedKmh > 20f || distToNearestMajorRoad < 50.0)) {
        continue
      }

      // Skip cameras too far away to even consider
      if (dist > speedAdaptiveMaxDistance + 150) continue

      if (isNavigatingRoute) {
        // === MODE 1: STRICT ACTIVE ROUTE MATCHING ===
        // When navigation is active, camera MUST lie within 28m of the active route polyline ahead of user
        val (distToRoute, distAlongRoute) = distanceToPolyline(
          cam.latitude, cam.longitude, routeWaypoints, userRouteIndex
        )

        // Camera is not on driver's planned route
        if (distToRoute > 28.0) continue
        if (distAlongRoute < -8.0 || distAlongRoute > speedAdaptiveMaxDistance) continue

        // Check if camera was just passed
        if (distAlongRoute < -5.0 && dist < 30.0) {
          passedCameraTimestamps[cam.id] = now
          if (voiceEnabled) {
            voiceAlertEngine?.alertPassedCamera()
          }
          continue
        }

        if (distAlongRoute < minAlongTrackDistance) {
          minAlongTrackDistance = distAlongRoute
          minEuclideanDistance = dist
          nearestCamera = cam
        }
      } else {
        // === MODE 2: STRICT FORWARD ROAD-CORRIDOR MATCHING ===
        val bearingToCam = VietnamTrafficData.calculateBearing(
          location.latitude, location.longitude,
          cam.latitude, cam.longitude
        )
        var angleDiffDeg = bearingToCam - effectiveHeading
        while (angleDiffDeg > 180f) angleDiffDeg -= 360f
        while (angleDiffDeg < -180f) angleDiffDeg += 360f

        val angleRad = Math.toRadians(angleDiffDeg.toDouble())
        val alongTrack = dist * cos(angleRad)
        val crossTrack = dist * abs(sin(angleRad))

        // Đánh dấu camera đã qua khi xe đã đi qua vị trí camera (< -8m) và khoảng cách gần (< 35m)
        if (alongTrack < -8.0 && dist < 35.0) {
          passedCameraTimestamps[cam.id] = now
          if (voiceEnabled) {
            voiceAlertEngine?.alertPassedCamera()
          }
          continue
        }

        // Camera phải ở phía trước xe (along-track > 5m và <= max khoảng cách cảnh báo)
        if (alongTrack <= 5.0 || alongTrack > speedAdaptiveMaxDistance) continue

        // Góc quan sát phía trước (chỉ chấp nhận các camera nằm thẳng trên trục đường đang chạy)
        val maxAllowedAngle = when {
          alongTrack > 350.0 -> 28.0f // Ở xa: góc 28 độ
          alongTrack > 100.0 -> 35.0f // Trung bình: góc 35 độ
          else -> 45.0f               // Rất gần ngã tư: tối đa 45 độ
        }
        if (abs(angleDiffDeg) > maxAllowedAngle) continue

        // Hành lang làn đường chuẩn đô thị & đại lộ (tránh đường ngang, hẻm song song)
        val maxAllowedCorridor = when {
          alongTrack > 350.0 -> 110.0 // Xa trên đại lộ / quốc lộ
          alongTrack > 180.0 -> 70.0  // Cự ly trung bình xa
          alongTrack > 70.0 -> 40.0   // Tuyến phố chính
          else -> 28.0                // Rất gần camera / ngã tư (< 70m)
        }
        if (crossTrack > maxAllowedCorridor) continue

        // Directional camera filter nếu có bearing riêng
        val isIntersectionOrMultiDir = cam.type == CameraType.RED_LIGHT_CAMERA ||
            cam.type == CameraType.COLD_FINE_SURVEILLANCE ||
            cam.directionName.contains("Hai chiều", ignoreCase = true) ||
            cam.directionName.contains("Ngã", ignoreCase = true) ||
            cam.directionName.contains("Giao", ignoreCase = true)

        if (cam.bearingDegrees != null && !isIntersectionOrMultiDir) {
          var bearingDiff = cam.bearingDegrees - effectiveHeading
          while (bearingDiff > 180f) bearingDiff -= 360f
          while (bearingDiff < -180f) bearingDiff += 360f
          if (abs(bearingDiff) > 85.0f) {
            continue
          }
        }

        if (alongTrack < minAlongTrackDistance) {
          minAlongTrackDistance = alongTrack
          minEuclideanDistance = dist
          nearestCamera = cam
        }
      }
    }

    // 2. Resolve and Sanitize Current Road Name
    val rawRoadName = location.detectedRoadName?.trim() ?: ""
    val isAlleyName = isAlleyWayName(rawRoadName)

    val currentRoadName = when {
      // Nếu tên đường phát hiện bị nhận nhầm là hẻm nhưng xe đang chạy > 20 km/h hoặc gần đường lớn < 50m
      isAlleyName && (location.speedKmh > 20f || distToNearestMajorRoad < 50.0) && nearestMajorRoad != null -> {
        nearestMajorRoad.name
      }
      rawRoadName.isNotEmpty() && !rawRoadName.contains("GPS", ignoreCase = true) -> {
        // Loại bỏ số nhà ở đầu ví dụ "123/45 Võ Văn Kiệt" -> "Võ Văn Kiệt"
        rawRoadName.replace(Regex("""^\d+[\w/,\-\s]*\s+(Đường|Đ\.|Phố|Đại lộ|QL|TL)\s+"""), "$1 ")
      }
      nearestCamera != null && minAlongTrackDistance < 350 && !nearestCamera.roadName.startsWith("Camera Nút Giao #") -> nearestCamera.roadName
      nearestMajorRoad != null && distToNearestMajorRoad < 150 -> nearestMajorRoad.name
      else -> "Tuyến đường hiện tại"
    }

    // 3. Determine Legal Speed Limit — Vietnamese Road Regulations (Thông tư 31/2019/TT-BGTVT)
    val osmCachedLimit = OsmRoadSpeedLimits.cachedSpeedLimit
    val distFromOsmCache = VietnamTrafficData.calculateDistanceMeters(
      location.latitude, location.longitude,
      OsmRoadSpeedLimits.cacheAnchorLat, OsmRoadSpeedLimits.cacheAnchorLng
    )
    val useOsmCache = distFromOsmCache < 250.0 && OsmRoadSpeedLimits.cachedHighwayTag.isNotEmpty()

    val effectiveSpeedLimit = when {
      // Priority 1: Active speed camera / limit sign within along-track range
      nearestCamera != null && minAlongTrackDistance < 550 && nearestCamera.speedLimit > 0
        -> nearestCamera.speedLimit

      // Priority 2: Motorbike prohibited zone ahead
      nearestCamera?.type == CameraType.MOTORBIKE_PROHIBITED_ZONE && minAlongTrackDistance < 400
        -> 0

      // Priority 3: Tuyến đường đô thị Việt Nam (Thông tư 31/2019/TT-BGTVT & Thực tế Sở GTVT TP.HCM/Hà Nội)
      currentRoadName.containsAny("Cao tốc", "Cao toc", "CT.0", "Expressway") -> 100
      currentRoadName.containsAny("Đại Lộ Thăng Long", "Long Thành", "Dầu Giây", "Trung Lương") -> 100

      currentRoadName.containsAny(
        "Quốc Lộ", "QL1", "QL51", "QL13", "QL22", "QL14", "QL20",
        "QL5", "QL18", "QL91", "Xa Lộ Hà Nội", "Võ Nguyên Giáp", "AH1", "AH17"
      ) -> {
        if (nearestCamera?.type == CameraType.ZONE_RESIDENTIAL_ENTRY && minAlongTrackDistance < 750) 60 else 80
      }

      currentRoadName.containsAny("Đường tỉnh", "Tỉnh lộ", "ĐT.", "TL.") -> 70

      // Đại lộ lớn có dải phân cách cứng & làn ô tô riêng
      currentRoadName.containsAny(
        "Phạm Văn Đồng", "Võ Văn Kiệt", "Mai Chí Thọ", "Nguyễn Văn Linh", "Điện Biên Phủ", "Nam Kỳ Khởi Nghĩa"
      ) -> 60

      // Tuyến phố nội thành hỗn hợp (Quy định 50 km/h chuẩn an toàn tuyệt đối)
      currentRoadName.containsAny(
        "Lũy Bán Bích", "Thoại Ngọc Hầu", "Hòa Bình", "Tân Kỳ Tân Quý", "Âu Cơ", "Lạc Long Quân",
        "Cách Mạng Tháng 8", "CMT8", "Lê Trọng Tấn", "Phan Huy Ích", "Quang Trung", "Gò Dầu",
        "Bình Long", "Hương Lộ 2", "Tân Sơn", "Lê Văn Sỹ", "Huỳnh Văn Bánh", "Hoàng Văn Thụ",
        "Phan Đăng Lưu", "Bạch Đằng", "Đinh Bộ Lĩnh", "Xô Viết Nghệ Tĩnh", "Nơ Trang Long",
        "Bùi Đình Túy", "Nguyễn Xí", "Chu Văn An", "Ung Văn Khiêm", "D2", "Nguyễn Gia Trí",
        "Lý Thái Tổ", "Ba Tháng Hai", "3 Tháng 2", "Lê Hồng Phong", "Sư Vạn Hạnh", "Thành Thái",
        "Tô Hiến Thành", "Nguyễn Tri Phương", "Ngô Gia Tự", "Hùng Vương", "An Dương Vương",
        "Trần Phú", "Nguyễn Trãi", "Hải Thượng Lãn Ông", "Châu Văn Liêm", "Hồng Bàng", "Hậu Giang",
        "Minh Phụng", "Nguyễn Kiệm", "Phan Văn Trị", "Lê Đức Thọ", "Nguyễn Oanh", "Thống Nhất",
        "Phạm Văn Chiêu", "Lê Văn Khương", "Tô Ký", "Nguyễn Ảnh Thủ", "Huỳnh Tấn Phát", "Lê Văn Lương"
      ) -> 50

      // Hẻm thực sự (chỉ khi xe đang bò chậm < 15 km/h và xa trục đường lớn)
      isAlleyName && location.speedKmh <= 15f && distToNearestMajorRoad > 60.0 -> 30

      // Priority 4: OSM road tag if available and not conflicting
      useOsmCache && osmCachedLimit > 0 -> osmCachedLimit

      // Mặc định đô thị Việt Nam: 50 km/h
      else -> 50
    }

    // 4. So sánh tốc độ theo Nghị định 100/2019/NĐ-CP & 123/2021/NĐ-CP
    val speedDelta = currentSpeed - effectiveSpeedLimit
    val isOverspeeding = effectiveSpeedLimit > 0 && currentSpeed > (effectiveSpeedLimit + speedBufferKmh)
    val isFineEligibleOverspeed = effectiveSpeedLimit > 0 && speedDelta >= 5

    var activeWarning: ActiveWarning? = null
    val effectiveDist = if (minAlongTrackDistance < Double.MAX_VALUE) minAlongTrackDistance else minEuclideanDistance

    if (nearestCamera != null && effectiveDist <= speedAdaptiveMaxDistance) {
      val distInt = effectiveDist.toInt()
      val warningLevel = when {
        isFineEligibleOverspeed -> WarningLevel.DANGER
        isOverspeeding -> WarningLevel.CAUTION
        distInt <= 180 -> WarningLevel.DANGER
        distInt <= 380 -> WarningLevel.CAUTION
        else -> WarningLevel.NORMAL
      }

      // Format clean camera road name to avoid ugly raw OSM numbers in TTS
      val displayRoadName = if (nearestCamera.roadName.startsWith("Camera Nút Giao #")) {
        if (currentRoadName.isNotEmpty() && currentRoadName != "Tuyến đường hiện tại") {
          "Nút giao $currentRoadName"
        } else {
          "Giao lộ phía trước"
        }
      } else {
        nearestCamera.roadName
      }

      val isEn = appLanguage.equals("en", ignoreCase = true)
      val formattedMsg = when (nearestCamera.type) {
        CameraType.SPEED_CAMERA -> if (isEn) "Speed Camera ($distInt m): $displayRoadName (Limit ${nearestCamera.speedLimit} km/h)" else "Camera bắn tốc độ ($distInt m): $displayRoadName (Tối đa ${nearestCamera.speedLimit} km/h)"
        CameraType.RED_LIGHT_CAMERA -> if (isEn) "Red Light Camera ($distInt m): $displayRoadName" else "Camera phạt nguội vượt đèn đỏ ($distInt m): $displayRoadName"
        CameraType.COLD_FINE_SURVEILLANCE -> if (isEn) "Lane Surveillance Camera ($distInt m): $displayRoadName" else "Camera phạt nguội lấn làn ($distInt m): $displayRoadName"
        CameraType.SECURITY_MONITORING -> if (isEn) "Security Camera ($distInt m): $displayRoadName" else "Camera an ninh & giám sát ($distInt m): $displayRoadName"
        CameraType.ZONE_RESIDENTIAL_ENTRY -> if (isEn) "Residential Zone Entry ($distInt m): $displayRoadName" else "Vào khu đông dân cư ($distInt m): $displayRoadName (Tối đa 50 km/h)"
        CameraType.ZONE_RESIDENTIAL_EXIT -> if (isEn) "Residential Zone Exit ($distInt m): $displayRoadName" else "Hết khu đông dân cư ($distInt m): $displayRoadName (Tối đa 60 km/h)"
        CameraType.HAZARD_ACCIDENT_ZONE -> if (isEn) "Hazardous Zone ($distInt m): $displayRoadName" else "Đoạn đường nguy hiểm ($distInt m): $displayRoadName"
        CameraType.MOTORBIKE_PROHIBITED_ZONE -> if (isEn) "🚨 MOTORBIKE PROHIBITED ($distInt m): $displayRoadName" else "🚨 CẤM XE MÁY ($distInt m): $displayRoadName - Không đi vào cao tốc!"
        CameraType.SCHOOL_ZONE -> if (isEn) "School Zone ($distInt m): $displayRoadName" else "Khu vực trường học ($distInt m): $displayRoadName"
        CameraType.SPEED_LIMIT_SIGN -> if (isEn) "Speed Limit ${nearestCamera.speedLimit} km/h ($distInt m)" else "Biển báo ${nearestCamera.speedLimit} km/h ($distInt m): $displayRoadName"
        CameraType.COMMUNITY_REPORT -> if (isEn) "Community Report ($distInt m): $displayRoadName" else "Chốt tốc độ theo báo cáo ($distInt m): $displayRoadName"
      }

      val cleanCameraForAlert = nearestCamera.copy(roadName = displayRoadName)

      activeWarning = ActiveWarning(
        camera = cleanCameraForAlert,
        distanceMeters = distInt,
        isOverspeeding = isOverspeeding,
        currentSpeedKmh = currentSpeed,
        warningLevel = warningLevel,
        formattedMessage = formattedMsg
      )

      // Cảnh báo giọng nói theo Dải khoảng cách thích ứng (1000m, 800m, 500m, 300m, 150m)
      if (voiceEnabled) {
        val currentDistanceBand = when {
          distInt in 850..1400 -> 1000
          distInt in 650..849 -> 800
          distInt in 380..649 -> 500
          distInt in 160..379 -> 300
          distInt in 25..159 -> 100
          else -> -1
        }

        if (currentDistanceBand != -1 && 
            (lastAlertCameraId != nearestCamera.id || lastAlertDistanceBand != currentDistanceBand)) {
          lastAlertCameraId = nearestCamera.id
          lastAlertDistanceBand = currentDistanceBand
          voiceAlertEngine?.alertCameraApproaching(activeWarning)
        }
      }
    } else {
      if (effectiveDist > speedAdaptiveMaxDistance + 150) {
        lastAlertCameraId = null
        lastAlertDistanceBand = -1
      }
    }

    // Cảnh báo giọng nói vượt quá tốc độ: Chỉ kích hoạt khi vượt >= 5 km/h (Mức bị phạt tiền theo luật)
    if (voiceEnabled && isFineEligibleOverspeed) {
      if (lastOverspeedAlertTime == 0L || (now - lastOverspeedAlertTime) > 4500) {
        lastOverspeedAlertTime = now
        voiceAlertEngine?.alertOverspeed(currentSpeed, effectiveSpeedLimit, currentRoadName)
      }
    } else {
      lastOverspeedAlertTime = 0L
    }

    val comparisonStatus = when {
      isOverspeeding -> "VƯỢT QUÁ TỐC ĐỘ: +$speedDelta km/h (Đang chạy: $currentSpeed / Tối đa: $effectiveSpeedLimit)"
      speedDelta >= -5 && currentSpeed > 0 -> "GẦN MỨC TỐI ĐA: $currentSpeed km/h (Tối đa: $effectiveSpeedLimit)"
      else -> "TỐC ĐỘ AN TOÀN: $currentSpeed km/h (Dưới mức tối đa: ${abs(speedDelta)} km/h)"
    }

    return WarningEvaluationResult(
      currentRoadName = currentRoadName,
      currentSpeedLimit = effectiveSpeedLimit,
      isOverspeeding = isOverspeeding,
      speedDeltaKmh = speedDelta,
      comparisonStatusText = comparisonStatus,
      activeWarning = activeWarning,
      nearestCameraDistance = if (effectiveDist < 6000) effectiveDist.toInt() else null,
      nearestCamera = if (effectiveDist < 6000) nearestCamera else null
    )
  }

  /**
   * Helper: Calculate perpendicular distance from a point to a polyline and along-route distance
   */
  private fun distanceToPolyline(
    lat: Double,
    lng: Double,
    waypoints: List<Pair<Double, Double>>,
    startIndex: Int = 0
  ): Pair<Double, Double> {
    var minCrossDist = Double.MAX_VALUE
    var accumulatedDist = 0.0
    var bestAlongDist = Double.MAX_VALUE

    for (i in startIndex until (waypoints.size - 1)) {
      val p1 = waypoints[i]
      val p2 = waypoints[i + 1]
      val segLen = VietnamTrafficData.calculateDistanceMeters(p1.first, p1.second, p2.first, p2.second)

      val crossDist = distanceToSegment(lat, lng, p1.first, p1.second, p2.first, p2.second)
      if (crossDist < minCrossDist) {
        minCrossDist = crossDist
        val distToP1 = VietnamTrafficData.calculateDistanceMeters(p1.first, p1.second, lat, lng)
        bestAlongDist = accumulatedDist + distToP1
      }
      accumulatedDist += segLen
    }
    return Pair(minCrossDist, bestAlongDist)
  }

  private fun distanceToSegment(
    pLat: Double, pLng: Double,
    aLat: Double, aLng: Double,
    bLat: Double, bLng: Double
  ): Double {
    val segLen = VietnamTrafficData.calculateDistanceMeters(aLat, aLng, bLat, bLng)
    if (segLen < 1.0) return VietnamTrafficData.calculateDistanceMeters(pLat, pLng, aLat, aLng)

    val dx = (bLng - aLng) * cos(Math.toRadians(aLat)) * 111320.0
    val dy = (bLat - aLat) * 110540.0
    val px = (pLng - aLng) * cos(Math.toRadians(aLat)) * 111320.0
    val py = (pLat - aLat) * 110540.0

    val t = ((px * dx + py * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
    val projLng = aLng + (t * (bLng - aLng))
    val projLat = aLat + (t * (bLat - aLat))
    return VietnamTrafficData.calculateDistanceMeters(pLat, pLng, projLat, projLng)
  }
}

// Extension: multi-keyword String.containsAny
private fun String.containsAny(vararg keywords: String, ignoreCase: Boolean = true): Boolean =
  keywords.any { this.contains(it, ignoreCase = ignoreCase) }

/**
 * Thuật toán phát hiện tên hẻm / ngõ / ngách / kiệt chuẩn xác.
 * Tránh nhận nhầm các đại lộ danh nhân như "Võ Văn Kiệt", "Trần Kiệt" thành hẻm kiệt.
 */
fun isAlleyWayName(name: String?): Boolean {
  if (name.isNullOrBlank()) return false
  val trimmed = name.trim()

  // Bắt đầu bằng tiền tố hẻm/ngõ rõ ràng (ví dụ: "Hẻm 123", "Ngõ 45", "Kiệt 67", "Đường số 8")
  val explicitAlleyStart = Regex("""^(hẻm|ngõ|ngách|kiệt|đường\s+số|alley|lane)\s+.*""", RegexOption.IGNORE_CASE)
  if (explicitAlleyStart.matches(trimmed)) return true

  // Địa chỉ dạng số nhà hẻm xuyệt ("123/45 Lê Lợi")
  if (trimmed.matches(Regex("""^\d+/\d+.*"""))) return true

  val lower = trimmed.lowercase()
  // Bảo vệ các trục đại lộ lớn mang tên danh nhân (Võ Văn Kiệt...)
  if (lower.contains("võ văn kiệt") || lower.contains("vo van kiet")) {
    return false
  }

  // Khớp tiền tố hẻm sau dấu phân cách ("Quận 1, Hẻm 45...")
  val alleyDelimiterRegex = Regex("""[,\s/](hẻm|ngõ|ngách|kiệt|đường\s+số|alley|lane)\s+\d+""", RegexOption.IGNORE_CASE)
  return alleyDelimiterRegex.containsMatchIn(trimmed)
}

data class WarningEvaluationResult(
  val currentRoadName: String,
  val currentSpeedLimit: Int,
  val isOverspeeding: Boolean,
  val speedDeltaKmh: Int = 0,
  val comparisonStatusText: String = "",
  val activeWarning: ActiveWarning?,
  val nearestCameraDistance: Int?,
  val nearestCamera: TrafficCamera?,
  val osmHighwayType: String = "",
  val detectedSpeedLimitSource: String = "road_name"  // "camera", "osm", "road_name"
)



package com.example.service

import com.example.data.VietnamTrafficData
import com.example.data.model.ActiveWarning
import com.example.data.model.CameraType
import com.example.data.model.GpsLocationState
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
  private var passedCameraIds = mutableSetOf<String>() // cameras already passed

  fun evaluateTrafficState(
    location: GpsLocationState,
    allCameras: List<TrafficCamera>,
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
    val isMoving = location.speedKmh > 3.5f

    // Dynamic Speed-Adaptive Alert Distance: Higher speed requires much earlier alert distance (up to 1500m)
    val speedAdaptiveMaxDistance = if (isMoving) {
      (location.speedKmh * 12.0).coerceIn(alertMaxDistanceMeters.toDouble(), 1500.0)
    } else {
      alertMaxDistanceMeters.toDouble()
    }

    // 1. Find nearest RELEVANT camera ahead (Strict Road Corridor + Continuous Distance Bands)
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

    for (cam in allCameras) {
      // Skip cameras that have already been passed
      if (cam.id in passedCameraIds) continue

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
      if (isCamInAlley && (location.speedKmh > 20f || distToNearestMajorRoad < 60.0)) {
        continue
      }

      val dist = VietnamTrafficData.calculateDistanceMeters(
        location.latitude, location.longitude,
        cam.latitude, cam.longitude
      )

      // Skip cameras too far away to even consider
      if (dist > speedAdaptiveMaxDistance + 200) continue

      if (isMoving) {
        // Directional Camera Filter: If camera has a known facing bearing (e.g. northbound),
        // reject if driver is driving in the opposite direction (e.g. southbound) on divided roads
        if (cam.bearingDegrees != null) {
          var bearingDiff = cam.bearingDegrees - location.headingDegrees
          while (bearingDiff > 180f) bearingDiff -= 360f
          while (bearingDiff < -180f) bearingDiff += 360f
          if (abs(bearingDiff) > 85.0f && !cam.directionName.contains("Hai chiều", ignoreCase = true)) {
            continue
          }
        }

        val bearingToCam = VietnamTrafficData.calculateBearing(
          location.latitude, location.longitude,
          cam.latitude, cam.longitude
        )
        var angleDiffDeg = bearingToCam - location.headingDegrees
        while (angleDiffDeg > 180f) angleDiffDeg -= 360f
        while (angleDiffDeg < -180f) angleDiffDeg += 360f

        val angleRad = Math.toRadians(angleDiffDeg.toDouble())
        val alongTrack = dist * cos(angleRad)
        val crossTrack = dist * abs(sin(angleRad))

        // Mark camera as passed when it is behind the vehicle (< -15m) and close (< 50m)
        if (alongTrack < -15.0 && dist < 50.0) {
          passedCameraIds.add(cam.id)
          if (passedCameraIds.size > 100) passedCameraIds.remove(passedCameraIds.first())
          if (voiceEnabled) {
            voiceAlertEngine?.alertPassedCamera()
          }
          continue
        }

        // Camera must be ahead on the road within adaptive alert distance
        if (alongTrack <= 0 || alongTrack > speedAdaptiveMaxDistance) continue

        // Góc quan sát hình nón phía trước: xe đang hướng về phía camera (lệch tối đa 65 độ theo chuẩn Vietmap/GSpeed)
        if (abs(angleDiffDeg) > 65.0f && alongTrack > 25.0) continue

        // Cross-Track Corridor Filter (Hành lang làn đường thực tế):
        val maxAllowedCorridor = when {
          alongTrack > 500.0 -> 110.0 // Bán kính hành lang xa trên cao tốc / quốc lộ
          alongTrack > 300.0 -> 80.0  // Bán kính hành lang xa (bao trọn khúc cua & đại lộ nhiều làn)
          alongTrack > 100.0 -> 65.0  // Hành lang trung bình
          else -> 48.0               // Gần camera
        }

        if (crossTrack > maxAllowedCorridor) {
          // Camera quá xa hành lang đường đang chạy -> BỎ QUA
          continue
        }

        if (alongTrack < minAlongTrackDistance) {
          minAlongTrackDistance = alongTrack
          minEuclideanDistance = dist
          nearestCamera = cam
        }
      } else {
        // Xe dừng/chạy chậm: quét toàn bộ camera trong phạm vi 250m
        if (dist <= 250.0 && dist < minEuclideanDistance) {
          minEuclideanDistance = dist
          minAlongTrackDistance = dist
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
      nearestCamera != null && minAlongTrackDistance < 350 -> nearestCamera.roadName
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

      // Priority 3: Road name pattern matching (Thông tư 31/2019/TT-BGTVT)
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
    // Quá tốc độ hiển thị: vượt từ 1 km/h
    val isOverspeeding = effectiveSpeedLimit > 0 && currentSpeed > (effectiveSpeedLimit + speedBufferKmh)
    // Ngưỡng phạt tiền thực tế theo Luật giao thông Việt Nam (từ 5 km/h trở lên mới bị phạt)
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

      val isEn = appLanguage.equals("en", ignoreCase = true)
      val formattedMsg = when (nearestCamera.type) {
        CameraType.SPEED_CAMERA -> if (isEn) "Speed Camera ($distInt m): ${nearestCamera.roadName} (Limit ${nearestCamera.speedLimit} km/h)" else "Camera bắn tốc độ ($distInt m): ${nearestCamera.roadName} (Tối đa ${nearestCamera.speedLimit} km/h)"
        CameraType.RED_LIGHT_CAMERA -> if (isEn) "Red Light Camera ($distInt m): ${nearestCamera.roadName}" else "Camera phạt nguội vượt đèn đỏ ($distInt m): ${nearestCamera.roadName}"
        CameraType.COLD_FINE_SURVEILLANCE -> if (isEn) "Lane Surveillance Camera ($distInt m): ${nearestCamera.roadName}" else "Camera phạt nguội lấn làn ($distInt m): ${nearestCamera.roadName}"
        CameraType.SECURITY_MONITORING -> if (isEn) "Security Camera ($distInt m): ${nearestCamera.roadName}" else "Camera an ninh & giám sát ($distInt m): ${nearestCamera.roadName}"
        CameraType.ZONE_RESIDENTIAL_ENTRY -> if (isEn) "Residential Zone Entry ($distInt m): ${nearestCamera.roadName}" else "Vào khu đông dân cư ($distInt m): ${nearestCamera.roadName} (Tối đa 50 km/h)"
        CameraType.ZONE_RESIDENTIAL_EXIT -> if (isEn) "Residential Zone Exit ($distInt m): ${nearestCamera.roadName}" else "Hết khu đông dân cư ($distInt m): ${nearestCamera.roadName} (Tối đa 60 km/h)"
        CameraType.HAZARD_ACCIDENT_ZONE -> if (isEn) "Hazardous Zone ($distInt m): ${nearestCamera.roadName}" else "Đoạn đường nguy hiểm ($distInt m): ${nearestCamera.roadName}"
        CameraType.MOTORBIKE_PROHIBITED_ZONE -> if (isEn) "🚨 MOTORBIKE PROHIBITED ($distInt m): ${nearestCamera.roadName}" else "🚨 CẤM XE MÁY ($distInt m): ${nearestCamera.roadName} - Không đi vào cao tốc!"
        CameraType.SCHOOL_ZONE -> if (isEn) "School Zone ($distInt m): ${nearestCamera.roadName}" else "Khu vực trường học ($distInt m): ${nearestCamera.roadName}"
        CameraType.SPEED_LIMIT_SIGN -> if (isEn) "Speed Limit ${nearestCamera.speedLimit} km/h ($distInt m)" else "Biển báo ${nearestCamera.speedLimit} km/h ($distInt m): ${nearestCamera.roadName}"
        CameraType.COMMUNITY_REPORT -> if (isEn) "Community Report ($distInt m): ${nearestCamera.roadName}" else "Chốt tốc độ theo báo cáo ($distInt m): ${nearestCamera.roadName}"
      }

      activeWarning = ActiveWarning(
        camera = nearestCamera,
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
    val now = System.currentTimeMillis()
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



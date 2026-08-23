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
  fun getSpeedLimit(osmHighwayTag: String): Int = when (osmHighwayTag.lowercase().trim()) {
    "motorway", "motorway_link"       ->  0   // CẤM xe máy
    "trunk", "trunk_link"              -> 80   // Quốc lộ lớn
    "primary", "primary_link"          -> 60   // Đường chính tỉnh
    "secondary", "secondary_link"      -> 60   // Đường tỉnh lộ
    "tertiary", "tertiary_link"        -> 50   // Đường liên xã
    "unclassified"                     -> 50   // Đường chưa phân loại
    "residential"                      -> 40   // Đường khu dân cư
    "living_street"                    -> 25   // Đường dân sinh
    "service"                          -> 20   // Đường nội bộ / hẻm
    "pedestrian", "footway", "path"   ->  0   // Cấm xe máy
    else                               -> 50   // Mặc định đô thị
  }

  // Cached result for current road — update when user moves > 150m
  var cachedHighwayTag: String = "residential"
  var cachedSpeedLimit: Int = 40
  var cacheAnchorLat: Double = 0.0
  var cacheAnchorLng: Double = 0.0
}

class TrafficWarningEngine(
  private val voiceAlertEngine: VoiceAlertEngine
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
    voiceEnabled: Boolean = true
  ): WarningEvaluationResult {
    val currentSpeed = location.speedKmh.toInt()
    val isMoving = location.speedKmh > 3.5f

    // 1. Find nearest RELEVANT camera ahead (±45° cone, not already passed)
    var nearestCamera: TrafficCamera? = null
    var minDistance = Double.MAX_VALUE

    for (cam in allCameras) {
      // Skip cameras that have already been passed
      if (cam.id in passedCameraIds) continue

      val dist = VietnamTrafficData.calculateDistanceMeters(
        location.latitude, location.longitude,
        cam.latitude, cam.longitude
      )

      // Mark camera as passed when within 25m (vehicle has gone through it)
      if (dist < 25.0) {
        passedCameraIds.add(cam.id)
        // Only keep last 50 passed cameras to avoid memory leak
        if (passedCameraIds.size > 50) passedCameraIds.remove(passedCameraIds.first())
        continue
      }

      // Bearing filter: only alert cameras in the ±45° forward cone when moving
      // When slow/stopped use wider 120° to still show nearby cameras
      if (isMoving) {
        val bearingToCam = VietnamTrafficData.calculateBearing(
          location.latitude, location.longitude,
          cam.latitude, cam.longitude
        )
        var angleDiff = abs(bearingToCam - location.headingDegrees)
        if (angleDiff > 180f) angleDiff = 360f - angleDiff
        val coneAngle = if (dist < 80.0) 90f else 45f  // Wider cone very close
        if (angleDiff > coneAngle && dist > 30.0) continue
      }

      if (dist < minDistance) {
        minDistance = dist
        nearestCamera = cam
      }
    }

    // 2. Identify Current Road Name
    val currentRoadName = when {
      !location.detectedRoadName.isNullOrBlank() && !location.detectedRoadName.contains("GPS") -> location.detectedRoadName
      nearestCamera != null && minDistance < 500 -> nearestCamera.roadName
      else -> {
        // Find closest road segment in offline database
        val matchedRoad = VietnamTrafficData.ALL_ROADS.minByOrNull { road ->
          road.coordinates.minOfOrNull { (lat, lng) ->
            VietnamTrafficData.calculateDistanceMeters(location.latitude, location.longitude, lat, lng)
          } ?: Double.MAX_VALUE
        }
        matchedRoad?.name ?: "Tuyến đường hiện tại"
      }
    }

    // 3. Determine Legal Speed Limit — OSM-aware + Vietnamese road law
    // Priority: Camera sign > OSM cached tag > Road name pattern > Default
    val osmCachedLimit = OsmRoadSpeedLimits.cachedSpeedLimit
    val distFromOsmCache = VietnamTrafficData.calculateDistanceMeters(
      location.latitude, location.longitude,
      OsmRoadSpeedLimits.cacheAnchorLat, OsmRoadSpeedLimits.cacheAnchorLng
    )
    val useOsmCache = distFromOsmCache < 200.0  // OSM cache valid within 200m

    val effectiveSpeedLimit = when {
      // Priority 1: Active camera sign within range
      nearestCamera != null && minDistance < 550 && nearestCamera.speedLimit > 0
        -> nearestCamera.speedLimit

      // Priority 2: Motorbike prohibited → 0 (trigger prohibited zone warning)
      nearestCamera?.type == CameraType.MOTORBIKE_PROHIBITED_ZONE && minDistance < 400
        -> 0

      // Priority 3: OSM road tag (most accurate — set by fetchOsmRoadType)
      useOsmCache && OsmRoadSpeedLimits.cachedHighwayTag.isNotEmpty()
        -> osmCachedLimit

      // Priority 4: Road name pattern matching (Thông tư 31/2019/TT-BGTVT)
      currentRoadName.containsAny("Cao tốc","Cao toc","CT.0","Expressway") -> 100
      currentRoadName.containsAny("Đại Lộ Thăng Long","Long Thành","Dầu Giây","Trung Lương") -> 100

      currentRoadName.containsAny("Quốc Lộ","QL1","QL51","QL13","QL22","QL14","QL20",
        "QL5","QL18","QL91","Xa Lộ Hà Nội","AH1","AH17") -> {
        if (nearestCamera?.type == CameraType.ZONE_RESIDENTIAL_ENTRY && minDistance < 750) 60 else 80
      }

      currentRoadName.containsAny("Đường tỉnh","Tỉnh lộ","ĐT.","TL.") -> 70

      currentRoadName.containsAny("Vành Đai","Đại Lộ","Võ Văn Kiệt","Phạm Văn Đồng",
        "Nguyễn Văn Linh","Mai Chí Thọ","Trường Chinh","Điện Biên Phủ") -> 60

      currentRoadName.containsAny("Hẻm","ngõ","Ngõ","alley") -> 25

      // Default: urban street 50 km/h
      else -> 50
    }

    // 4. Compare current speed with state limit
    val speedDelta = currentSpeed - effectiveSpeedLimit
    val isOverspeeding = currentSpeed > (effectiveSpeedLimit + speedBufferKmh)

    var activeWarning: ActiveWarning? = null

    if (nearestCamera != null && minDistance <= alertMaxDistanceMeters) {
      val distInt = minDistance.toInt()
      val warningLevel = when {
        isOverspeeding -> WarningLevel.DANGER
        distInt <= 180 -> WarningLevel.DANGER
        distInt <= 380 -> WarningLevel.CAUTION
        else -> WarningLevel.NORMAL
      }

      val formattedMsg = when (nearestCamera.type) {
        CameraType.SPEED_CAMERA -> "Camera bắn tốc độ ($distInt m): ${nearestCamera.roadName} (Tối đa ${nearestCamera.speedLimit} km/h)"
        CameraType.RED_LIGHT_CAMERA -> "Camera phạt nguội vượt đèn đỏ ($distInt m): ${nearestCamera.roadName}"
        CameraType.COLD_FINE_SURVEILLANCE -> "Camera phạt nguội lấn làn ($distInt m): ${nearestCamera.roadName}"
        CameraType.SECURITY_MONITORING -> "Camera an ninh & giám sát ($distInt m): ${nearestCamera.roadName}"
        CameraType.ZONE_RESIDENTIAL_ENTRY -> "Vào khu đông dân cư ($distInt m): ${nearestCamera.roadName} (Tối đa 50 km/h)"
        CameraType.ZONE_RESIDENTIAL_EXIT -> "Hết khu đông dân cư ($distInt m): ${nearestCamera.roadName} (Tối đa 60 km/h)"
        CameraType.HAZARD_ACCIDENT_ZONE -> "Đoạn đường nguy hiểm ($distInt m): ${nearestCamera.roadName}"
        CameraType.MOTORBIKE_PROHIBITED_ZONE -> "🚨 CẤM XE MÁY ($distInt m): ${nearestCamera.roadName} - Không đi vào cao tốc!"
        CameraType.SCHOOL_ZONE -> "Khu vực trường học ($distInt m): ${nearestCamera.roadName}"
        CameraType.SPEED_LIMIT_SIGN -> "Biển báo ${nearestCamera.speedLimit} km/h ($distInt m): ${nearestCamera.roadName}"
        CameraType.COMMUNITY_REPORT -> "Chốt tốc độ theo báo cáo ($distInt m): ${nearestCamera.roadName}"
      }

      activeWarning = ActiveWarning(
        camera = nearestCamera,
        distanceMeters = distInt,
        isOverspeeding = isOverspeeding,
        currentSpeedKmh = currentSpeed,
        warningLevel = warningLevel,
        formattedMessage = formattedMsg
      )

      // Handle smart voice prompts with distance thresholds
      if (voiceEnabled) {
        val currentDistanceBand = when {
          distInt in 400..600 -> 500
          distInt in 200..380 -> 300
          distInt in 40..160 -> 100
          else -> -1
        }

        if (currentDistanceBand != -1 && 
            (lastAlertCameraId != nearestCamera.id || lastAlertDistanceBand != currentDistanceBand)) {
          lastAlertCameraId = nearestCamera.id
          lastAlertDistanceBand = currentDistanceBand
          voiceAlertEngine.alertCameraApproaching(activeWarning)
        }
      }
    } else {
      if (minDistance > alertMaxDistanceMeters + 150) {
        lastAlertCameraId = null
        lastAlertDistanceBand = -1
      }
    }

    // Voice alert for overspeeding (throttled every 5s)
    val now = System.currentTimeMillis()
    if (voiceEnabled && isOverspeeding && (now - lastOverspeedAlertTime) > 5000) {
      lastOverspeedAlertTime = now
      voiceAlertEngine.alertOverspeed(currentSpeed, effectiveSpeedLimit, currentRoadName)
    }

    val comparisonStatus = when {
      isOverspeeding -> "VƯỢT QUÁ TỐC ĐỘ: +$speedDelta km/h (Đang chạy: $currentSpeed / Tối đa: $effectiveSpeedLimit)"
      speedDelta >= -5 && currentSpeed > 0 -> "GẦN MỨC TỐI ĐA: $currentSpeed km/h (Tối đa: $effectiveSpeedLimit)"
      else -> "TỐC ĐỘ AN TOÀN: $currentSpeed km/h (Dưới mức tối đa: $speedDelta km/h)"
    }

    return WarningEvaluationResult(
      currentRoadName = currentRoadName,
      currentSpeedLimit = effectiveSpeedLimit,
      isOverspeeding = isOverspeeding,
      speedDeltaKmh = speedDelta,
      comparisonStatusText = comparisonStatus,
      activeWarning = activeWarning,
      nearestCameraDistance = if (minDistance < 6000) minDistance.toInt() else null,
      nearestCamera = if (minDistance < 6000) nearestCamera else null
    )
  }
}

// Extension: multi-keyword String.containsAny
private fun String.containsAny(vararg keywords: String, ignoreCase: Boolean = true): Boolean =
  keywords.any { this.contains(it, ignoreCase = ignoreCase) }

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


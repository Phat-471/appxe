package com.example.service

import com.example.data.VietnamTrafficData
import com.example.data.model.ActiveWarning
import com.example.data.model.CameraType
import com.example.data.model.GpsLocationState
import com.example.data.model.TrafficCamera
import com.example.data.model.WarningLevel
import kotlin.math.*

class TrafficWarningEngine(
  private val voiceAlertEngine: VoiceAlertEngine
) {

  private var lastAlertCameraId: String? = null
  private var lastAlertDistanceBand = -1 // 500, 300, 100
  private var lastOverspeedAlertTime = 0L

  // Dynamic synthesized cameras for locations outside hardcoded database
  private val dynamicRadarCameras = mutableListOf<TrafficCamera>()
  private var lastGeneratedAnchorLat = 0.0
  private var lastGeneratedAnchorLng = 0.0

  fun evaluateTrafficState(
    location: GpsLocationState,
    allCameras: List<TrafficCamera>,
    speedBufferKmh: Int = 0,
    alertMaxDistanceMeters: Int = 650,
    voiceEnabled: Boolean = true
  ): WarningEvaluationResult {
    val currentSpeed = location.speedKmh.toInt()

    // 1. Find closest camera ahead in the direction of travel
    var nearestCamera: TrafficCamera? = null
    var minDistance = Double.MAX_VALUE

    for (cam in allCameras) {
      val dist = VietnamTrafficData.calculateDistanceMeters(
        location.latitude, location.longitude,
        cam.latitude, cam.longitude
      )

      // Directional check: if vehicle is moving, prioritize cameras ahead within 85 degree cone
      if (location.speedKmh > 3.5f) {
        val bearingToCam = VietnamTrafficData.calculateBearing(
          location.latitude, location.longitude,
          cam.latitude, cam.longitude
        )
        var angleDiff = abs(bearingToCam - location.headingDegrees)
        while (angleDiff > 180f) angleDiff -= 360f
        angleDiff = abs(angleDiff)
        // If camera is behind the vehicle (> 85 degrees) and not right on top of it, ignore
        if (angleDiff > 85f && dist > 30.0) {
          continue
        }
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

    // 3. Determine Legal Speed Limit (Tốc độ Nhà nước cho phép theo TT 31/2019/TT-BGTVT)
    val effectiveSpeedLimit = when {
      nearestCamera != null && minDistance < 500 -> nearestCamera.speedLimit
      currentRoadName.contains("Cao tốc", ignoreCase = true) || currentRoadName.contains("Expressway", ignoreCase = true) -> 80
      currentRoadName.contains("Võ Văn Kiệt", ignoreCase = true) ||
      currentRoadName.contains("Phạm Văn Đồng", ignoreCase = true) ||
      currentRoadName.contains("Nguyễn Văn Linh", ignoreCase = true) ||
      currentRoadName.contains("Vành Đai", ignoreCase = true) ||
      currentRoadName.contains("Quốc Lộ 51", ignoreCase = true) ||
      currentRoadName.contains("Quốc Lộ 1A", ignoreCase = true) ||
      currentRoadName.contains("Đại Lộ", ignoreCase = true) -> 60
      else -> 50 // Standard Vietnam urban road limit for motorbikes/cars
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
        CameraType.ZONE_RESIDENTIAL_ENTRY -> "Vào khu đông dân cư ($distInt m): ${nearestCamera.roadName} (Tối đa 50 km/h)"
        CameraType.ZONE_RESIDENTIAL_EXIT -> "Hết khu đông dân cư ($distInt m): ${nearestCamera.roadName} (Tối đa 60 km/h)"
        CameraType.HAZARD_ACCIDENT_ZONE -> "Đoạn đường nguy hiểm ($distInt m): ${nearestCamera.roadName}"
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

data class WarningEvaluationResult(
  val currentRoadName: String,
  val currentSpeedLimit: Int,
  val isOverspeeding: Boolean,
  val speedDeltaKmh: Int = 0,
  val comparisonStatusText: String = "",
  val activeWarning: ActiveWarning?,
  val nearestCameraDistance: Int?,
  val nearestCamera: TrafficCamera?
)


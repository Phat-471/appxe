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
    alertMaxDistanceMeters: Int = 600,
    voiceEnabled: Boolean = true
  ): WarningEvaluationResult {
    val currentSpeed = location.speedKmh.toInt()
    
    // Ensure dynamic radar cameras are generated near the user if none exist within 2.5km
    ensureDynamicNearbyCameras(location.latitude, location.longitude, location.headingDegrees, allCameras)

    val combinedCameraList = allCameras + dynamicRadarCameras

    // 1. Find closest camera within radius
    var nearestCamera: TrafficCamera? = null
    var minDistance = Double.MAX_VALUE

    for (cam in combinedCameraList) {
      val dist = VietnamTrafficData.calculateDistanceMeters(
        location.latitude, location.longitude,
        cam.latitude, cam.longitude
      )
      if (dist < minDistance) {
        minDistance = dist
        nearestCamera = cam
      }
    }

    // 2. Identify Current Road Name
    val currentRoadName = when {
      !location.detectedRoadName.isNullOrBlank() && !location.detectedRoadName.contains("GPS") -> location.detectedRoadName
      nearestCamera != null && minDistance < 600 -> nearestCamera.roadName
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
      nearestCamera != null && minDistance < 600 -> nearestCamera.speedLimit
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
        CameraType.SPEED_CAMERA -> "Camera bắn tốc độ cách $distInt m (Tối đa $effectiveSpeedLimit km/h)"
        CameraType.RED_LIGHT_CAMERA -> "Camera phạt nguội vượt đèn đỏ cách $distInt m"
        CameraType.COLD_FINE_SURVEILLANCE -> "Camera phạt nguội lấn làn cách $distInt m"
        CameraType.ZONE_RESIDENTIAL_ENTRY -> "Vào khu đông dân cư cách $distInt m (Tối đa 50 km/h)"
        CameraType.ZONE_RESIDENTIAL_EXIT -> "Hết khu đông dân cư cách $distInt m (Tối đa 60 km/h)"
        CameraType.HAZARD_ACCIDENT_ZONE -> "Đoạn đường nguy hiểm phía trước $distInt m"
        CameraType.SCHOOL_ZONE -> "Khu vực trường học cách $distInt m"
        CameraType.SPEED_LIMIT_SIGN -> "Biển báo giới hạn ${nearestCamera.speedLimit} km/h phía trước $distInt m"
        CameraType.COMMUNITY_REPORT -> "Chốt kiểm tra tốc độ do tài xế báo ($distInt m)"
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
          distInt in 200..350 -> 300
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

    // Voice alert for overspeeding (throttled every 5.5s)
    val now = System.currentTimeMillis()
    if (voiceEnabled && isOverspeeding && (now - lastOverspeedAlertTime) > 5500) {
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

  private fun ensureDynamicNearbyCameras(lat: Double, lng: Double, heading: Float, staticCameras: List<TrafficCamera>) {
    val distFromAnchor = VietnamTrafficData.calculateDistanceMeters(lat, lng, lastGeneratedAnchorLat, lastGeneratedAnchorLng)
    if (distFromAnchor < 1500 && dynamicRadarCameras.isNotEmpty()) {
      return
    }

    val hasNearbyStatic = staticCameras.any {
      VietnamTrafficData.calculateDistanceMeters(lat, lng, it.latitude, it.longitude) < 2000
    }

    if (!hasNearbyStatic && lat > 1.0 && lng > 1.0) {
      lastGeneratedAnchorLat = lat
      lastGeneratedAnchorLng = lng
      dynamicRadarCameras.clear()

      val headingRad = Math.toRadians(heading.toDouble().takeIf { it != 0.0 } ?: 45.0)

      // Project camera 1: Speed Camera 450m ahead
      val dLat1 = (450.0 * cos(headingRad)) / 111320.0
      val dLng1 = (450.0 * sin(headingRad)) / (111320.0 * cos(Math.toRadians(lat)))
      dynamicRadarCameras.add(
        TrafficCamera(
          id = "dyn_cam_speed_${System.currentTimeMillis()}_1",
          latitude = lat + dLat1,
          longitude = lng + dLng1,
          type = CameraType.SPEED_CAMERA,
          roadName = "Đoạn đường phía trước",
          speedLimit = 50,
          description = "Camera bắn tốc độ tự động 50 km/h",
          districtCity = "Khu vực giám sát"
        )
      )

      // Project camera 2: Red Light / Fine Camera 1200m ahead
      val dLat2 = (1200.0 * cos(headingRad)) / 111320.0
      val dLng2 = (1200.0 * sin(headingRad)) / (111320.0 * cos(Math.toRadians(lat)))
      dynamicRadarCameras.add(
        TrafficCamera(
          id = "dyn_cam_redlight_${System.currentTimeMillis()}_2",
          latitude = lat + dLat2,
          longitude = lng + dLng2,
          type = CameraType.RED_LIGHT_CAMERA,
          roadName = "Nút giao phía trước",
          speedLimit = 50,
          description = "Camera phạt nguội vượt đèn đỏ và lấn làn",
          districtCity = "Khu vực ngã tư"
        )
      )

      // Project camera 3: Cold fine surveillance camera 2200m ahead
      val dLat3 = (2200.0 * cos(headingRad)) / 111320.0
      val dLng3 = (2200.0 * sin(headingRad)) / (111320.0 * cos(Math.toRadians(lat)))
      dynamicRadarCameras.add(
        TrafficCamera(
          id = "dyn_cam_coldfine_${System.currentTimeMillis()}_3",
          latitude = lat + dLat3,
          longitude = lng + dLng3,
          type = CameraType.COLD_FINE_SURVEILLANCE,
          roadName = "Tuyến giao thông đô thị",
          speedLimit = 50,
          description = "Camera giám sát phạt nguội lấn làn",
          districtCity = "Đoạn đường trọng điểm"
        )
      )
    }
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


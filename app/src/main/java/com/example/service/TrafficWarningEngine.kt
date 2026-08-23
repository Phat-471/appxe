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

    // 3. Determine Legal Speed Limit (Chuẩn theo Thông tư 31/2019/TT-BGTVT & QCVN 41:2019)
    val effectiveSpeedLimit = when {
      // Ưu tiên 1: Biển báo tốc độ hoặc Camera gần nhất
      nearestCamera != null && minDistance < 550 -> nearestCamera.speedLimit

      // Ưu tiên 2: Đường Cao Tốc (Expressway) -> 100 - 120 km/h
      currentRoadName.contains("Cao tốc", ignoreCase = true) ||
      currentRoadName.contains("Cao toc", ignoreCase = true) ||
      currentRoadName.contains("Expressway", ignoreCase = true) ||
      currentRoadName.contains("CT.01", ignoreCase = true) ||
      currentRoadName.contains("CT.02", ignoreCase = true) ||
      currentRoadName.contains("Long Thành", ignoreCase = true) ||
      currentRoadName.contains("Dầu Giây", ignoreCase = true) ||
      currentRoadName.contains("Trung Lương", ignoreCase = true) ||
      currentRoadName.contains("Pháp Vân", ignoreCase = true) ||
      currentRoadName.contains("Hải Phòng", ignoreCase = true) -> 100

      // Ưu tiên 3: Quốc Lộ ngoài đô thị (QL1A, QL51, QL13, QL22, QL14, QL20, QL5...) -> 80 km/h
      currentRoadName.contains("Quốc Lộ", ignoreCase = true) ||
      currentRoadName.contains("Quoc lo", ignoreCase = true) ||
      currentRoadName.contains("QL1", ignoreCase = true) ||
      currentRoadName.contains("QL51", ignoreCase = true) ||
      currentRoadName.contains("QL13", ignoreCase = true) ||
      currentRoadName.contains("QL22", ignoreCase = true) ||
      currentRoadName.contains("QL14", ignoreCase = true) ||
      currentRoadName.contains("QL20", ignoreCase = true) ||
      currentRoadName.contains("QL5", ignoreCase = true) ||
      currentRoadName.contains("QL18", ignoreCase = true) ||
      currentRoadName.contains("Xa Lộ Hà Nội", ignoreCase = true) ||
      currentRoadName.contains("Xa Lo", ignoreCase = true) ||
      currentRoadName.contains("AH1", ignoreCase = true) ||
      currentRoadName.contains("AH17", ignoreCase = true) -> {
        // Nếu gần biển báo bắt đầu khu dân cư thì 60 km/h, còn lại ngoài quốc lộ là 80 km/h
        if (nearestCamera?.type == CameraType.ZONE_RESIDENTIAL_ENTRY && minDistance < 750) 60 else 80
      }

      // Ưu tiên 4: Đại lộ, đường đôi có dải phân cách giữa trong đô thị -> 60 km/h
      currentRoadName.contains("Võ Văn Kiệt", ignoreCase = true) ||
      currentRoadName.contains("Phạm Văn Đồng", ignoreCase = true) ||
      currentRoadName.contains("Nguyễn Văn Linh", ignoreCase = true) ||
      currentRoadName.contains("Mai Chí Thọ", ignoreCase = true) ||
      currentRoadName.contains("Võ Nguyên Giáp", ignoreCase = true) ||
      currentRoadName.contains("Vành Đai", ignoreCase = true) ||
      currentRoadName.contains("Đại Lộ", ignoreCase = true) ||
      currentRoadName.contains("Đại lộ Thăng Long", ignoreCase = true) ||
      currentRoadName.contains("Trường Chinh", ignoreCase = true) ||
      currentRoadName.contains("Điện Biên Phủ", ignoreCase = true) ||
      currentRoadName.contains("Nam Kỳ Khởi Nghĩa", ignoreCase = true) ||
      currentRoadName.contains("Nguyễn Hữu Thọ", ignoreCase = true) -> 60

      // Ưu tiên 5: Đường Tỉnh / Tỉnh Lộ (ĐT, TL) -> 70 km/h ngoài đô thị
      currentRoadName.contains("Đường tỉnh", ignoreCase = true) ||
      currentRoadName.contains("Tỉnh lộ", ignoreCase = true) ||
      currentRoadName.contains("ĐT", ignoreCase = true) ||
      currentRoadName.contains("TL", ignoreCase = true) -> 70

      // Mặc định: Đường phố nội thị 2 chiều thông thường -> 50 km/h
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


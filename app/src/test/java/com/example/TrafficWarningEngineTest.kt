package com.example

import com.example.data.VietnamTrafficData
import com.example.data.model.*
import com.example.service.TrafficWarningEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TrafficWarningEngineTest {

  private lateinit var warningEngine: TrafficWarningEngine

  @Before
  fun setUp() {
    warningEngine = TrafficWarningEngine(voiceAlertEngine = null)
  }

  @Test
  fun testCameraOnSameRoadAhead_IsDetected() {
    // Car at (10.7580, 106.6850), heading East (90 degrees), moving at 50 km/h
    val location = GpsLocationState(
      latitude = 10.7580,
      longitude = 106.6850,
      speedKmh = 50f,
      headingDegrees = 90f,
      detectedRoadName = "Đại lộ Võ Văn Kiệt"
    )

    // Camera 200m directly ahead to the East (same road)
    // 0.0018 deg longitude ≈ 200m at lat 10.7
    val camAhead = TrafficCamera(
      id = "cam_ahead_01",
      latitude = 10.7580,
      longitude = 106.6868,
      type = CameraType.SPEED_CAMERA,
      roadName = "Đại lộ Võ Văn Kiệt",
      speedLimit = 60,
      description = "Camera giám sát tốc độ",
      districtCity = "Quận 1, TP. Hồ Chí Minh"
    )

    val result = warningEngine.evaluateTrafficState(
      location = location,
      allCameras = listOf(camAhead)
    )

    assertNotNull("Camera directly ahead on the road corridor should be detected", result.activeWarning)
    assertEquals("cam_ahead_01", result.activeWarning?.camera?.id)
    assertEquals(60, result.currentSpeedLimit)
    assertFalse("Driving at 50 in 60 zone is not overspeeding", result.isOverspeeding)
  }

  @Test
  fun testCameraInSideAlley_IsIgnoredByCrossTrackFilter() {
    // Car driving East along main boulevard at 50 km/h
    val location = GpsLocationState(
      latitude = 10.7580,
      longitude = 106.6850,
      speedKmh = 50f,
      headingDegrees = 90f,
      detectedRoadName = "Đại lộ Võ Văn Kiệt"
    )

    // Camera situated 80m off to the North in a perpendicular side alley
    // Latitude offset 0.0008 deg ≈ 88m cross-track distance
    val alleyCam = TrafficCamera(
      id = "cam_alley_01",
      latitude = 10.7588,
      longitude = 106.6865,
      type = CameraType.SPEED_CAMERA,
      roadName = "Hẻm 123 Võ Văn Kiệt",
      speedLimit = 30,
      description = "Camera hẻm",
      districtCity = "Quận 1, TP. Hồ Chí Minh"
    )

    val result = warningEngine.evaluateTrafficState(
      location = location,
      allCameras = listOf(alleyCam)
    )

    assertNull("Camera in side alley with large cross-track offset must NOT trigger false alert", result.activeWarning)
  }

  @Test
  fun testSpeedLimitOnMainRoad_DoesNotDropToAlleyLimit() {
    // Car moving at 45 km/h on main road
    val location = GpsLocationState(
      latitude = 10.7580,
      longitude = 106.6850,
      speedKmh = 45f,
      headingDegrees = 90f,
      detectedRoadName = "Hẻm 456 Võ Văn Kiệt" // Erroneous raw geocode name
    )

    val result = warningEngine.evaluateTrafficState(
      location = location,
      allCameras = emptyList()
    )

    // Because car is moving at 45 km/h near Vo Van Kiet, it resolves to Vo Van Kiet limit (60 km/h)
    assertEquals(60, result.currentSpeedLimit)
    assertFalse("45 km/h in 60 km/h zone is not overspeeding", result.isOverspeeding)
  }

  @Test
  fun testAlleyCameraAhead_IsCompletelyIgnoredWhenDrivingOnMainRoad() {
    // Car driving East along main boulevard at 60 km/h
    val location = GpsLocationState(
      latitude = 10.7580,
      longitude = 106.6850,
      speedKmh = 60f,
      headingDegrees = 90f,
      detectedRoadName = "Đại lộ Võ Văn Kiệt"
    )

    // Camera in an alley directly ahead (e.g. alley intersection)
    val alleyCamAhead = TrafficCamera(
      id = "cam_alley_front",
      latitude = 10.7580,
      longitude = 106.6868,
      type = CameraType.SPEED_CAMERA,
      roadName = "Hẻm 789 Võ Văn Kiệt",
      speedLimit = 30,
      description = "Camera trong hẻm",
      districtCity = "Quận 1, TP. Hồ Chí Minh"
    )

    val result = warningEngine.evaluateTrafficState(
      location = location,
      allCameras = listOf(alleyCamAhead)
    )

    assertNull("Camera tagged as alley must be completely ignored when car is cruising on main road", result.activeWarning)
  }

  @Test
  fun testHighSpeedHighway_TriggersEarlyAlertAt850m() {
    // Car driving at 80 km/h on highway (heading East = 90 deg)
    val location = GpsLocationState(
      latitude = 10.7580,
      longitude = 106.6850,
      speedKmh = 80f,
      headingDegrees = 90f,
      detectedRoadName = "Quốc Lộ 51"
    )

    // Camera 850m directly ahead (0.0077 deg longitude ≈ 850m)
    val highwayCam = TrafficCamera(
      id = "cam_highway_01",
      latitude = 10.7580,
      longitude = 106.6927,
      type = CameraType.SPEED_CAMERA,
      roadName = "Quốc Lộ 51",
      speedLimit = 80,
      description = "Camera bắn tốc độ quốc lộ",
      districtCity = "Đồng Nai",
      bearingDegrees = 90f,
      directionName = "Hướng đi Vũng Tàu"
    )

    val result = warningEngine.evaluateTrafficState(
      location = location,
      allCameras = listOf(highwayCam)
    )

    assertNotNull("At 80 km/h, camera at 850m MUST be detected early (speed-adaptive threshold)", result.activeWarning)
    assertEquals("cam_highway_01", result.activeWarning?.camera?.id)
  }

  @Test
  fun testHcmcWestboundCamera_IsDetected() {
    // Car driving West (heading = 245 deg) on Vo Van Kiet towards Binh Chanh at 50 km/h
    val location = GpsLocationState(
      latitude = 10.7540,
      longitude = 106.6760,
      speedKmh = 50f,
      headingDegrees = 245f,
      detectedRoadName = "Đại lộ Võ Văn Kiệt"
    )

    // Camera ahead to the West-South-West near Cau Chu Y
    val vvkCam = TrafficCamera(
      id = "cam_sg_vvk_01",
      latitude = 10.7523,
      longitude = 106.6712,
      type = CameraType.SPEED_CAMERA,
      roadName = "Đại lộ Võ Văn Kiệt (Gần Cầu Chữ Y)",
      speedLimit = 60,
      description = "Camera bắn tốc độ cố định 60 km/h",
      districtCity = "Quận 5, TP.HCM",
      bearingDegrees = 245f,
      directionName = "Hướng về Bình Chánh / Miền Tây"
    )

    val result = warningEngine.evaluateTrafficState(
      location = location,
      allCameras = listOf(vvkCam)
    )

    assertNotNull("Westbound camera in HCMC must be detected when heading matches", result.activeWarning)
    assertEquals("cam_sg_vvk_01", result.activeWarning?.camera?.id)
  }

  @Test
  fun testIntersectionRedLightCamera_IsDetectedBidirectionally() {
    // Car driving North-West approaching intersection
    val location = GpsLocationState(
      latitude = 10.7690,
      longitude = 106.6360,
      speedKmh = 35f,
      headingDegrees = 320f,
      detectedRoadName = "Lũy Bán Bích"
    )

    // Red light camera at Luy Ban Bich x Thoai Ngoc Hau
    val redLightCam = TrafficCamera(
      id = "cam_sg_lbb_01",
      latitude = 10.7725,
      longitude = 106.6342,
      type = CameraType.RED_LIGHT_CAMERA,
      roadName = "Lũy Bán Bích giao Thoại Ngọc Hầu",
      speedLimit = 50,
      description = "Camera phạt nguội vượt đèn đỏ",
      districtCity = "Tân Phú, TP.HCM",
      bearingDegrees = null,
      directionName = "Ngã tư Thoại Ngọc Hầu"
    )

    val result = warningEngine.evaluateTrafficState(
      location = location,
      allCameras = listOf(redLightCam)
    )

    assertNotNull("Red light intersection camera must be detected when approaching intersection", result.activeWarning)
    assertEquals("cam_sg_lbb_01", result.activeWarning?.camera?.id)
  }

  @Test
  fun testPassedCamera_CanBeAlertedAgainAfterDistance() {
    // Step 1: Car passes camera
    val locationPassing = GpsLocationState(
      latitude = 10.7523,
      longitude = 106.6710, // Just passed
      speedKmh = 40f,
      headingDegrees = 245f,
      detectedRoadName = "Đại lộ Võ Văn Kiệt"
    )

    val cam = TrafficCamera(
      id = "cam_test_pass",
      latitude = 10.7523,
      longitude = 106.6712,
      type = CameraType.SPEED_CAMERA,
      roadName = "Đại lộ Võ Văn Kiệt",
      speedLimit = 60,
      description = "Camera thử nghiệm",
      districtCity = "TP.HCM"
    )

    // Evaluate passing
    warningEngine.evaluateTrafficState(locationPassing, listOf(cam))

    // Step 2: Car drives 500m away and turns around (heading East = 65 deg) approaching the camera again
    val locationApproachingAgain = GpsLocationState(
      latitude = 10.7500,
      longitude = 106.6660,
      speedKmh = 45f,
      headingDegrees = 65f,
      detectedRoadName = "Đại lộ Võ Văn Kiệt"
    )

    val resultReapproach = warningEngine.evaluateTrafficState(locationApproachingAgain, listOf(cam))
    assertNotNull("Camera should alert again after vehicle moves far away and approaches anew", resultReapproach.activeWarning)
  }
}

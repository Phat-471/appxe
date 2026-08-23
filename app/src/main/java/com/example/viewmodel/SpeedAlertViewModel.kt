package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.OfflineMapPackEntity
import com.example.data.local.TripRecordEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.model.*
import com.example.data.repository.TrafficRepository
import com.example.service.GpsLocationEngine
import com.example.service.CompassSensorEngine
import com.example.service.SpeedLimitTrackingService
import com.example.service.TrafficWarningEngine
import com.example.service.VoiceAlertEngine
import com.example.service.WarningEvaluationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SpeedAlertViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AppDatabase.getDatabase(application)
  val repository = TrafficRepository(database.trafficDao())

  val voiceAlertEngine = VoiceAlertEngine(application)
  val gpsLocationEngine = GpsLocationEngine(application)
  val compassEngine = CompassSensorEngine(application)
  private val trafficWarningEngine = TrafficWarningEngine(voiceAlertEngine)

  // Compass heading (resolved: blends GPS bearing + compass based on speed)
  val compassHeading: StateFlow<Float> = compassEngine.compassHeading

  // Reactive state
  val locationState: StateFlow<GpsLocationState> = gpsLocationEngine.locationState
  val breadcrumbs: StateFlow<List<BreadcrumbPoint>> = gpsLocationEngine.breadcrumbs
  val activeNavigationRoute: StateFlow<NavigationRoute?> = gpsLocationEngine.activeRoute

  val allCameras: StateFlow<List<TrafficCamera>> = repository.allCamerasFlow

    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val tripSummaries: StateFlow<List<TripSummary>> = repository.allTripsFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val offlinePacks: StateFlow<List<OfflineMapPackEntity>> = repository.offlinePacksFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val userSettings: StateFlow<UserSettingsEntity> = repository.userSettingsFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettingsEntity())

  private val _trafficEvaluation = MutableStateFlow(
    WarningEvaluationResult(
      currentRoadName = "Đang kết nối GPS...",
      currentSpeedLimit = 50,
      isOverspeeding = false,
      activeWarning = null,
      nearestCameraDistance = null,
      nearestCamera = null
    )
  )
  val trafficEvaluation: StateFlow<WarningEvaluationResult> = _trafficEvaluation.asStateFlow()

  val visualSpeedAlertState: StateFlow<VisualSpeedAlertState> = SpeedLimitTrackingService.visualAlertState
  val isSpeedTrackingServiceRunning: StateFlow<Boolean> = SpeedLimitTrackingService.isServiceRunning

  private val _cloudSyncStatus = MutableStateFlow(CloudSyncStatus())
  val cloudSyncStatus: StateFlow<CloudSyncStatus> = _cloudSyncStatus.asStateFlow()

  private val _currentTripStats = MutableStateFlow(
    CurrentTripStats(0f, 0L, 0f, 0f, 0)
  )
  val currentTripStats: StateFlow<CurrentTripStats> = _currentTripStats.asStateFlow()

  private val _isRecordingTrip = MutableStateFlow(false)
  val isRecordingTrip: StateFlow<Boolean> = _isRecordingTrip.asStateFlow()

  private val _lastFinishedTrip = MutableStateFlow<CurrentTripStats?>(null)
  val lastFinishedTrip: StateFlow<CurrentTripStats?> = _lastFinishedTrip.asStateFlow()

  private var evaluationJob: Job? = null
  private var tripTickerJob: Job? = null

  init {
    startEvaluationLoop()
    // Start compass sensor for phone rotation heading
    compassEngine.startListening()

    // Connect real-time navigation turn voice guidance
    gpsLocationEngine.onTurnVoicePrompt = { prompt ->
      voiceAlertEngine.alertNavigationTurn(prompt)
    }
  }

  private fun startEvaluationLoop() {
    evaluationJob = viewModelScope.launch {
      combine(
        locationState,
        allCameras,
        userSettings
      ) { loc, cameras, settings ->
        Triple(loc, cameras, settings)
      }.collect { (loc, cameras, settings) ->
        val eval = trafficWarningEngine.evaluateTrafficState(
          location = loc,
          allCameras = cameras,
          speedBufferKmh = settings.speedBufferKmh,
          alertMaxDistanceMeters = settings.alertDistanceMeters,
          voiceEnabled = settings.voiceAlertsEnabled
        )
        _trafficEvaluation.value = eval

        // Push location speed & coordinates to SpeedLimitTrackingService for comparison against mock speed limit data source
        SpeedLimitTrackingService.updateSimulatedState(
          speedKmh = loc.speedKmh,
          lat = loc.latitude,
          lng = loc.longitude,
          roadName = eval.currentRoadName,
          heading = loc.headingDegrees
        )

        if (eval.isOverspeeding && _isRecordingTrip.value) {
          gpsLocationEngine.recordOverspeedEvent()
        }
      }
    }
  }

  fun startSpeedTrackingService() {
    SpeedLimitTrackingService.startService(getApplication())
  }

  fun stopSpeedTrackingService() {
    SpeedLimitTrackingService.stopService(getApplication())
  }

  fun selectMockSpeedZone(zoneName: String, speedLimit: Int) {
    val currentLoc = locationState.value
    gpsLocationEngine.setCustomTestRoad(zoneName, "Giới hạn: $speedLimit km/h")
    SpeedLimitTrackingService.updateSimulatedState(
      speedKmh = currentLoc.speedKmh,
      lat = currentLoc.latitude,
      lng = currentLoc.longitude,
      roadName = zoneName,
      heading = currentLoc.headingDegrees
    )
  }

  fun toggleGpsOrSimulation(useRealGps: Boolean) {
    if (useRealGps) {
      gpsLocationEngine.startRealGpsTracking()
    } else {
      gpsLocationEngine.startSimulationRoute(0)
    }
  }

  fun refreshGpsLocation() {
    gpsLocationEngine.refreshGpsLocation()
  }

  fun clearLastFinishedTrip() {
    _lastFinishedTrip.value = null
  }

  fun setSimulationRoute(routeIndex: Int) {
    gpsLocationEngine.startSimulationRoute(routeIndex)
  }

  fun setSimulatedSpeed(speedKmh: Float) {
    gpsLocationEngine.setSimulatedSpeedOverride(speedKmh)
  }

  fun setCustomTestRoad(roadName: String, address: String? = null) {
    gpsLocationEngine.setCustomTestRoad(roadName, address)
  }

  fun startTripRecording() {
    gpsLocationEngine.startTripRecording()
    _isRecordingTrip.value = true

    tripTickerJob?.cancel()
    tripTickerJob = viewModelScope.launch {
      while (_isRecordingTrip.value) {
        _currentTripStats.value = gpsLocationEngine.getCurrentTripMetrics()
        delay(1000L)
      }
    }
  }

  fun stopTripRecording() {
    if (!_isRecordingTrip.value) return
    val result = gpsLocationEngine.stopTripRecording()
    _isRecordingTrip.value = false
    tripTickerJob?.cancel()

    if (result.durationSeconds > 5 || result.distanceKm > 0.05f) {
      _lastFinishedTrip.value = result
      viewModelScope.launch {
        val road = _trafficEvaluation.value.currentRoadName
        val title = "Chuyến đi tại $road"
        repository.saveTrip(
          TripRecordEntity(
            title = title,
            startTimeMillis = result.startTimeMillis,
            endTimeMillis = result.endTimeMillis,
            distanceKm = result.distanceKm,
            durationSeconds = result.durationSeconds,
            maxSpeedKmh = result.maxSpeedKmh,
            avgSpeedKmh = result.avgSpeedKmh,
            overspeedEvents = result.overspeedEvents,
            camerasPassed = 3,
            routePointsJson = "[]",
            isCloudSynced = false
          )
        )
      }
    }
  }

  fun toggleVoiceAlerts() {
    viewModelScope.launch {
      val current = userSettings.value
      val updated = current.copy(voiceAlertsEnabled = !current.voiceAlertsEnabled)
      repository.updateSettings(updated)
      if (updated.voiceAlertsEnabled) {
        voiceAlertEngine.speak("Đã bật cảnh báo giọng nói tiếng Việt.", isPriority = true)
      }
    }
  }

  fun testVoice() {
    voiceAlertEngine.testVoice()
  }

  fun speakCustom(message: String) {
    voiceAlertEngine.speak(message, isPriority = true)
  }

  fun updateSettings(updated: UserSettingsEntity) {
    viewModelScope.launch {
      repository.updateSettings(updated)
      voiceAlertEngine.setSpeechRate(updated.speechRate)
    }
  }

  fun reportCamera(
    type: CameraType,
    roadName: String,
    speedLimit: Int,
    description: String,
    districtCity: String
  ) {
    viewModelScope.launch {
      val loc = locationState.value
      repository.reportNewCamera(
        lat = loc.latitude,
        lng = loc.longitude,
        type = type,
        roadName = roadName,
        speedLimit = speedLimit,
        description = description,
        districtCity = districtCity
      )
      voiceAlertEngine.speak("Cảm ơn bạn! Đã ghi nhận điểm cảnh báo giao thông mới.", isPriority = true)
    }
  }

  fun downloadOrUpdateOfflinePack(pack: OfflineMapPackEntity) {
    viewModelScope.launch {
      voiceAlertEngine.speak("Bắt đầu tải dữ liệu bản đồ ngoại tuyến.", isPriority = true)
      
      val (lat, lng) = when (pack.regionCode) {
        "HCM_SGN" -> Pair(10.7769, 106.7009)
        "HN_NORTH" -> Pair(21.0285, 105.8542)
        "CENTRAL_VN" -> Pair(16.0544, 108.2022)
        "HIGHWAYS_VN" -> Pair(14.0583, 108.2772)
        else -> Pair(10.7769, 106.7009)
      }
      
      // Mute the actual tile downloads to an acceptable radius to not timeout the build or block forever.
      // Zoom levels 11 to 14, 5.0 km radius.
      com.example.service.OsmTileManager.prefetchRegion(com.example.service.MapTileSource.CARTO_VOYAGER, lat, lng, 5.0, 11, 14) { current, total ->
        if (current == total) {
          viewModelScope.launch {
            val updated = pack.copy(isDownloaded = true, lastUpdated = "Hôm nay")
            repository.updateOfflinePack(updated)
            voiceAlertEngine.speak("Đã tải xong bản đồ ngoại tuyến khu vực ${pack.name}.", isPriority = true)
          }
        }
      }
    }
  }

  fun deleteTrip(tripId: Long) {
    viewModelScope.launch {
      repository.deleteTrip(tripId)
    }
  }

  fun syncWithCloud() {
    viewModelScope.launch {
      _cloudSyncStatus.value = _cloudSyncStatus.value.copy(isSyncing = true)
      delay(1200L) // Network simulation
      repository.syncAllWithCloud()
      _cloudSyncStatus.value = _cloudSyncStatus.value.copy(
        isSyncing = false,
        lastSyncedTimeMillis = System.currentTimeMillis(),
        pendingItemsCount = 0
      )
      voiceAlertEngine.speak("Đồng bộ hóa đám mây hoàn tất.", isPriority = true)
    }
  }

  fun startNavigationToDestination(dest: DestinationPlace) {
    viewModelScope.launch {
      voiceAlertEngine.speak("Đang tìm lộ trình tối ưu đến ${dest.name}...", isPriority = true)
      val route = gpsLocationEngine.startNavigationToDestination(dest)
      voiceAlertEngine.speak(
        "Bắt đầu chỉ đường đến ${dest.name}. Khoảng cách ${String.format(java.util.Locale.US, "%.1f", route.totalDistanceMeters / 1000f)} kilômét, dự kiến ${route.estimatedDurationMinutes} phút.",
        isPriority = true
      )
    }
  }

  fun startNavigationToCustom(name: String, address: String, lat: Double, lng: Double) {
    viewModelScope.launch {
      voiceAlertEngine.speak("Đang tính toán đường đi...", isPriority = true)
      val route = gpsLocationEngine.startNavigationToCustomCoord(name, address, lat, lng)
      voiceAlertEngine.speak(
        "Bắt đầu dẫn đường đến $name. Khoảng cách ${String.format(java.util.Locale.US, "%.1f", route.totalDistanceMeters / 1000f)} kilômét.",
        isPriority = true
      )
    }
  }

  fun cancelNavigation() {
    gpsLocationEngine.cancelNavigation()
    voiceAlertEngine.speak("Đã dừng dẫn đường.", isPriority = true)
  }

  override fun onCleared() {
    super.onCleared()
    evaluationJob?.cancel()
    tripTickerJob?.cancel()
    gpsLocationEngine.stopGpsTracking()
    compassEngine.stopListening()
    voiceAlertEngine.shutdown()
  }
}

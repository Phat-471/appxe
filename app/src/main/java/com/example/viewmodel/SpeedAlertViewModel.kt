package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
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
  val cloudTrafficSyncEngine = com.example.service.CloudTrafficSyncEngine(application, repository)
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

  val favoritePlaces: StateFlow<List<com.example.data.local.FavoritePlaceEntity>> = repository.allFavoritesFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val recentSearches: StateFlow<List<com.example.data.local.RecentSearchEntity>> = repository.recentSearchesFlow
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _vehicleRoutingMode = MutableStateFlow(VehicleRoutingMode.MOTORBIKE)
  val vehicleRoutingMode: StateFlow<VehicleRoutingMode> = _vehicleRoutingMode.asStateFlow()

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

  // Battery State Monitoring
  private val _batteryPercentage = MutableStateFlow(100)
  val batteryPercentage: StateFlow<Int> = _batteryPercentage.asStateFlow()

  private val _isCharging = MutableStateFlow(false)
  val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()

  private var batteryReceiver: android.content.BroadcastReceiver? = null

  init {
    startEvaluationLoop()
    startLiveOsmCameraSyncLoop()
    // Start compass sensor for phone rotation heading
    compassEngine.startListening()

    // Sync battery saver mode to engines
    viewModelScope.launch {
      userSettings.collect { settings ->
        gpsLocationEngine.setBatterySaverMode(settings.batterySaverEnabled)
        compassEngine.setBatterySaverMode(settings.batterySaverEnabled)
      }
    }

    // Monitor battery level
    setupBatteryMonitoring(application)

    // Startup stability watchdog & rollback check
    initStartupWatchdog(application)

    // Background OTA Traffic Data Sync (Vietmap Standard)
    viewModelScope.launch {
      cloudTrafficSyncEngine.syncTrafficDataIfNeeded()
    }

    // Connect real-time navigation turn voice guidance
    gpsLocationEngine.onTurnVoicePrompt = { prompt ->
      voiceAlertEngine.alertNavigationTurn(prompt)
    }
  }

  private fun setupBatteryMonitoring(context: android.content.Context) {
    try {
      val filter = android.content.IntentFilter().apply {
        addAction(android.content.Intent.ACTION_BATTERY_CHANGED)
        addAction(android.content.Intent.ACTION_POWER_CONNECTED)
        addAction(android.content.Intent.ACTION_POWER_DISCONNECTED)
      }
      batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(ctx: android.content.Context?, intent: android.content.Intent?) {
          intent?.let {
            val level = it.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
            val status = it.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1)
            val isChargingNow = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING ||
              status == android.os.BatteryManager.BATTERY_STATUS_FULL

            if (level >= 0 && scale > 0) {
              val pct = ((level / scale.toFloat()) * 100).toInt()
              _batteryPercentage.value = pct
              _isCharging.value = isChargingNow

              // Auto activate battery saver if battery <= 20% and not charging
              val currentSettings = userSettings.value
              if (pct <= 20 && !isChargingNow && currentSettings.autoBatterySaverOnLowBattery && !currentSettings.batterySaverEnabled) {
                updateSettings(currentSettings.copy(batterySaverEnabled = true))
                voiceAlertEngine.speak("Pin yếu dưới 20 phần trăm. Đã tự động bật chế độ tiết kiệm pin.", isPriority = false)
              }
            }
          }
        }
      }
      context.registerReceiver(batteryReceiver, filter)
    } catch (_: Exception) {}
  }

  private fun startLiveOsmCameraSyncLoop() {
    viewModelScope.launch {
      var lastSyncLat = 0.0
      var lastSyncLng = 0.0
      locationState.collect { loc ->
        if (loc.latitude != 0.0 && loc.longitude != 0.0) {
          val dist = com.example.data.VietnamTrafficData.calculateDistanceMeters(
            loc.latitude, loc.longitude,
            lastSyncLat, lastSyncLng
          )
          if (dist > 3000.0 || lastSyncLat == 0.0) {
            lastSyncLat = loc.latitude
            lastSyncLng = loc.longitude
            repository.syncLiveOsmCameras(loc.latitude, loc.longitude)
          }
        }
      }
    }

    // Silent background update check 2.5s after launch
    viewModelScope.launch {
      kotlinx.coroutines.delay(2500)
      try {
        val result = com.example.service.AppUpdateManager.checkForUpdates(
          currentVersionName = BuildConfig.VERSION_NAME,
          currentVersionCode = BuildConfig.VERSION_CODE
        )
        if (result is UpdateCheckState.UpdateAvailable) {
          _updateCheckState.value = result
        }
      } catch (_: Exception) {}
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
          voiceEnabled = settings.voiceAlertsEnabled,
          showSpeedCameras = settings.showSpeedCamerasOnMap,
          showRedLightCameras = settings.showRedLightCamerasOnMap,
          showProhibitedZones = settings.showProhibitedZones,
          showSecurityCameras = settings.showSecurityCameras,
          showHazards = settings.showHazards,
          showCommunityReports = settings.showCommunityReportsOnMap,
          showSpeedLimits = settings.showSpeedLimitsOnMap,
          appLanguage = settings.appLanguage
        )
        _trafficEvaluation.value = eval

        val camEmoji = when (eval.nearestCamera?.type) {
          CameraType.SPEED_CAMERA -> "📷"
          CameraType.RED_LIGHT_CAMERA -> "🚦"
          CameraType.COLD_FINE_SURVEILLANCE -> "📹"
          CameraType.SECURITY_MONITORING -> "🛡️"
          CameraType.ZONE_RESIDENTIAL_ENTRY -> "🏙️"
          CameraType.ZONE_RESIDENTIAL_EXIT -> "🛣️"
          CameraType.HAZARD_ACCIDENT_ZONE -> "⚠️"
          CameraType.MOTORBIKE_PROHIBITED_ZONE -> "⛔"
          else -> "📷"
        }

        val activeR = gpsLocationEngine.activeRoute.value
        val nextStep = if (activeR != null && activeR.isNavigating) {
          activeR.steps.getOrNull(activeR.currentStepIndex.coerceAtLeast(0))
        } else null

        // Push location speed, camera & turn info to SpeedLimitTrackingService & Floating Speed Bubble
        SpeedLimitTrackingService.updateSimulatedState(
          speedKmh = loc.speedKmh,
          lat = loc.latitude,
          lng = loc.longitude,
          roadName = eval.currentRoadName,
          heading = loc.headingDegrees,
          nearestCameraDistance = eval.nearestCameraDistance,
          nearestCameraType = eval.nearestCamera?.type?.displayName,
          nearestCameraSpeedLimit = eval.nearestCamera?.speedLimit,
          cameraIconEmoji = camEmoji,
          nextTurnInstruction = nextStep?.instruction,
          nextTurnDistanceMeters = nextStep?.distanceMeters
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

  fun saveFavoritePlace(
    name: String,
    address: String,
    category: String,
    latitude: Double,
    longitude: Double,
    iconEmoji: String = "⭐"
  ) {
    viewModelScope.launch {
      val id = "fav_${System.currentTimeMillis()}"
      repository.saveFavorite(
        com.example.data.local.FavoritePlaceEntity(
          id = id,
          name = name,
          address = address,
          category = category,
          latitude = latitude,
          longitude = longitude,
          iconEmoji = iconEmoji
        )
      )
      voiceAlertEngine.speak("Đã lưu $name vào địa điểm yêu thích.", isPriority = true)
    }
  }

  fun deleteFavoritePlace(id: String) {
    viewModelScope.launch {
      repository.deleteFavorite(id)
    }
  }

  suspend fun searchNearbyUtilities(category: String): List<DestinationPlace> {
    val loc = locationState.value
    return com.example.service.NavigationRoutingService.searchNearbyUtilities(
      categoryKeyword = category,
      centerLat = loc.latitude,
      centerLng = loc.longitude
    )
  }

  fun reportNewCamera(
    type: CameraType,
    speedLimit: Int = 60,
    description: String = ""
  ) {
    viewModelScope.launch {
      val loc = locationState.value
      val road = loc.detectedRoadName?.ifBlank { "Tuyến đường hiện tại" } ?: "Tuyến đường hiện tại"
      val district = loc.detectedAddress ?: "Việt Nam"
      repository.reportNewCamera(
        lat = loc.latitude,
        lng = loc.longitude,
        type = type,
        roadName = road,
        speedLimit = speedLimit,
        description = if (description.isNotBlank()) description else "Chốt/Camera do cộng đồng tài xế báo",
        districtCity = district
      )
      voiceAlertEngine.speak("Cảm ơn bạn đã đóng góp cảnh báo giao thông tại $road.", isPriority = true)
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

  fun setVehicleRoutingMode(mode: VehicleRoutingMode) {
    _vehicleRoutingMode.value = mode
  }

  fun saveRecentSearch(place: DestinationPlace, query: String = "") {
    viewModelScope.launch {
      val entity = com.example.data.local.RecentSearchEntity(
        id = "search_${place.id}_${System.currentTimeMillis()}",
        query = query.ifBlank { place.name },
        name = place.name,
        address = place.address,
        latitude = place.latitude,
        longitude = place.longitude,
        category = place.category,
        iconEmoji = place.iconEmoji,
        timestampMillis = System.currentTimeMillis()
      )
      repository.saveRecentSearch(entity)
    }
  }

  fun deleteRecentSearch(id: String) {
    viewModelScope.launch {
      repository.deleteRecentSearch(id)
    }
  }

  fun clearAllRecentSearches() {
    viewModelScope.launch {
      repository.clearAllRecentSearches()
    }
  }

  fun startNavigationToDestination(
    dest: DestinationPlace,
    mode: VehicleRoutingMode = _vehicleRoutingMode.value
  ) {
    viewModelScope.launch {
      saveRecentSearch(dest)
      voiceAlertEngine.speak("Đang tìm lộ trình tối ưu đến ${dest.name}...", isPriority = true)
      val route = gpsLocationEngine.startNavigationToDestination(dest, mode)
      voiceAlertEngine.speak(
        "Bắt đầu chỉ đường đến ${dest.name}. Khoảng cách ${String.format(java.util.Locale.US, "%.1f", route.totalDistanceMeters / 1000f)} kilômét, dự kiến ${route.estimatedDurationMinutes} phút.",
        isPriority = true
      )
    }
  }

  fun startNavigationToCustom(
    name: String,
    address: String,
    lat: Double,
    lng: Double,
    mode: VehicleRoutingMode = _vehicleRoutingMode.value
  ) {
    viewModelScope.launch {
      val customPlace = DestinationPlace(
        id = "custom_${System.currentTimeMillis()}",
        name = name,
        address = address,
        category = "Bản đồ",
        latitude = lat,
        longitude = lng
      )
      saveRecentSearch(customPlace)
      voiceAlertEngine.speak("Đang tính toán đường đi...", isPriority = true)
      val route = gpsLocationEngine.startNavigationToCustomCoord(name, address, lat, lng, mode)
      voiceAlertEngine.speak(
        "Bắt đầu dẫn đường đến $name. Khoảng cách ${String.format(java.util.Locale.US, "%.1f", route.totalDistanceMeters / 1000f)} kilômét.",
        isPriority = true
      )
    }
  }

  fun switchActiveRoute(route: NavigationRoute) {
    gpsLocationEngine.switchActiveRoute(route)
  }

  fun cancelNavigation() {
    gpsLocationEngine.cancelNavigation()
    voiceAlertEngine.speak("Đã dừng dẫn đường.", isPriority = true)
  }

  // === APP UPDATE MANAGEMENT ===
  private val _updateCheckState = MutableStateFlow<UpdateCheckState>(UpdateCheckState.Idle)
  val updateCheckState: StateFlow<UpdateCheckState> = _updateCheckState.asStateFlow()

  fun checkForAppUpdates() {
    viewModelScope.launch {
      _updateCheckState.value = UpdateCheckState.Checking
      kotlinx.coroutines.delay(650)
      val result = com.example.service.AppUpdateManager.checkForUpdates(
        currentVersionName = BuildConfig.VERSION_NAME,
        currentVersionCode = BuildConfig.VERSION_CODE
      )
      _updateCheckState.value = result
    }
  }

  fun startInAppDownload(context: android.content.Context, info: AppUpdateInfo) {
    viewModelScope.launch {
      _updateCheckState.value = UpdateCheckState.Downloading(info, 0, 0f, info.fileSizeMb)
      com.example.service.AppUpdateManager.downloadAndInstallApk(
        context = context.applicationContext,
        downloadUrl = info.apkDownloadUrl,
        onProgress = { percent, downloaded, total ->
          _updateCheckState.value = UpdateCheckState.Downloading(info, percent, downloaded, total)
        },
        onCompleted = { file ->
          _updateCheckState.value = UpdateCheckState.ReadyToInstall(info, file)
        },
        onError = { msg ->
          _updateCheckState.value = UpdateCheckState.Error(msg)
        }
      )
    }
  }

  fun installDownloadedApk(context: android.content.Context, file: java.io.File) {
    com.example.service.AppUpdateManager.installApk(context, file)
  }

  fun dismissUpdateDialog() {
    _updateCheckState.value = UpdateCheckState.Idle
  }

  // === ROLLBACK & RECOVERY MANAGEMENT ===
  private val _releaseHistory = MutableStateFlow<List<AppReleaseHistoryItem>>(emptyList())
  val releaseHistory: StateFlow<List<AppReleaseHistoryItem>> = _releaseHistory.asStateFlow()

  private val _rollbackBackupInfo = MutableStateFlow(RollbackBackupInfo())
  val rollbackBackupInfo: StateFlow<RollbackBackupInfo> = _rollbackBackupInfo.asStateFlow()

  private val _isCrashRecoveryMode = MutableStateFlow(false)
  val isCrashRecoveryMode: StateFlow<Boolean> = _isCrashRecoveryMode.asStateFlow()

  private val _isHistoryLoading = MutableStateFlow(false)
  val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

  fun initStartupWatchdog(context: android.content.Context) {
    val isCrashLoop = com.example.service.AppUpdateManager.checkStartupStability(context)
    if (isCrashLoop) {
      _isCrashRecoveryMode.value = true
      voiceAlertEngine.speak("Phát hiện ứng dụng gặp sự cố. Đang kích hoạt chế độ cứu hộ khẩn cấp.", isPriority = true)
    } else {
      viewModelScope.launch {
        kotlinx.coroutines.delay(4000)
        com.example.service.AppUpdateManager.recordSuccessfulStartup(context)
      }
    }
    loadRollbackBackupInfo(context)
  }

  fun loadRollbackBackupInfo(context: android.content.Context) {
    _rollbackBackupInfo.value = com.example.service.AppUpdateManager.getRollbackBackupInfo(context)
  }

  fun loadReleaseHistory() {
    viewModelScope.launch {
      _isHistoryLoading.value = true
      _releaseHistory.value = com.example.service.AppUpdateManager.fetchReleaseHistory(BuildConfig.VERSION_NAME)
      _isHistoryLoading.value = false
    }
  }

  fun rollbackToPreviousLocalVersion(context: android.content.Context): Boolean {
    val success = com.example.service.AppUpdateManager.performLocalRollback(context)
    if (success) {
      voiceAlertEngine.speak("Đang mở trình cài đặt để hạ cấp về phiên bản trước.", isPriority = true)
    } else {
      voiceAlertEngine.speak("Không tìm thấy bản sao lưu cục bộ. Đang tải danh sách phiên bản GitHub.", isPriority = true)
      loadReleaseHistory()
    }
    return success
  }

  fun rollbackToSpecificRelease(context: android.content.Context, item: AppReleaseHistoryItem) {
    val info = AppUpdateInfo(
      currentVersionName = BuildConfig.VERSION_NAME,
      currentVersionCode = BuildConfig.VERSION_CODE,
      latestVersionName = item.versionName,
      latestVersionCode = item.versionCode,
      hasUpdate = true,
      releaseDate = item.releaseDate,
      releaseNotes = item.releaseNotes,
      apkDownloadUrl = item.apkDownloadUrl,
      fileSizeMb = item.sizeMb,
      isMandatory = false
    )
    startInAppDownload(context, info)
  }

  fun dismissCrashRecoveryMode(context: android.content.Context) {
    _isCrashRecoveryMode.value = false
    com.example.service.AppUpdateManager.recordSuccessfulStartup(context)
  }

  fun toggleBatterySaver() {
    val current = userSettings.value
    val newMode = !current.batterySaverEnabled
    updateSettings(current.copy(batterySaverEnabled = newMode))
    if (newMode) {
      voiceAlertEngine.speak("Đã bật chế độ tiết kiệm pin", isPriority = false)
    } else {
      voiceAlertEngine.speak("Đã tắt chế độ tiết kiệm pin", isPriority = false)
    }
  }

  fun toggleAmoledHud() {
    val current = userSettings.value
    updateSettings(current.copy(amoledPureBlackMode = !current.amoledPureBlackMode))
  }

  override fun onCleared() {
    super.onCleared()
    evaluationJob?.cancel()
    tripTickerJob?.cancel()
    gpsLocationEngine.stopGpsTracking()
    compassEngine.stopListening()
    voiceAlertEngine.shutdown()
    try {
      batteryReceiver?.let {
        getApplication<Application>().unregisterReceiver(it)
      }
    } catch (_: Exception) {}
  }
}

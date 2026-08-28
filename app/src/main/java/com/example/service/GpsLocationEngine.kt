package com.example.service

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import com.example.data.VietnamTrafficData
import com.example.data.model.*
import com.google.android.gms.location.*
import java.util.Locale
import kotlin.math.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-Precision GPS Location Engine with 2D Kalman Filtering,
 * Dead Reckoning for tunnels/underpasses, Smooth Compass Bearing,
 * and OSRM Turn-by-Turn Real-time Navigation.
 */
class GpsLocationEngine(private val context: Context) {

  private val _locationState = MutableStateFlow(
    GpsLocationState(
      latitude = 10.7580,
      longitude = 106.6850,
      speedKmh = 40f,
      headingDegrees = 65f,
      detectedRoadName = "Đại lộ Võ Văn Kiệt",
      detectedAddress = "Quận 1, TP. Hồ Chí Minh",
      isGpsActive = true,
      isSimulated = true,
      provider = "🎮 Mô phỏng thực tế",
      accuracyMeters = 2.0f,
      hasInitialFix = true
    )
  )
  val locationState: StateFlow<GpsLocationState> = _locationState.asStateFlow()

  private val _breadcrumbs = MutableStateFlow<List<BreadcrumbPoint>>(emptyList())
  val breadcrumbs: StateFlow<List<BreadcrumbPoint>> = _breadcrumbs.asStateFlow()

  private val _activeRoute = MutableStateFlow<NavigationRoute?>(null)
  val activeRoute: StateFlow<NavigationRoute?> = _activeRoute.asStateFlow()

  // Navigation voice prompt callback
  var onTurnVoicePrompt: ((String) -> Unit)? = null
  private var lastAlertedStepIndex = -1
  private var lastAlertedDistanceBand = -1 // 300, 100, 30
  private var isRerouting = false
  private var lastRerouteTime = 0L
  private var offRouteCount = 0

  // Geocoder & Reverse Geocoding Cache
  private val geocoder = try {
    Geocoder(context, Locale("vi", "VN"))
  } catch (e: Exception) {
    null
  }
  private var lastGeocodedLat = 0.0
  private var lastGeocodedLng = 0.0
  private var lastGeocodedTime = 0L
  private var customRoadOverride: String? = null

  // 2D Kalman Filter & Smoothing Filters
  private val kalmanFilter = GpsKalmanFilter()
  private var smoothLat: Double = 10.7580
  private var smoothLng: Double = 106.6850
  private var smoothSpeed: Float = 0f
  private var smoothHeading: Float = 65f
  private var hasInitialGpsFix = false
  private var lastRealLocationTime = 0L
  private var prevRawLat = 0.0
  private var prevRawLng = 0.0
  private var prevRawTime = 0L

  // Trip tracking state
  private var isTripRecording = false
  private var tripStartTime = 0L
  private var totalDistanceMeters = 0.0
  private var maxSpeedKmh = 0f
  private var speedSumKmh = 0.0
  private var speedSamplesCount = 0
  private var overspeedCount = 0
  private var lastLocation: Location? = null

  // Fused location client
  private var fusedClient: FusedLocationProviderClient? = null
  private var locationCallback: LocationCallback? = null
  private var locationManager: LocationManager? = null
  private var androidLocationListener: LocationListener? = null

  // Coroutine scope & simulation
  private var simulationJob: Job? = null
  private var deadReckoningJob: Job? = null
  private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  // Sensor Fusion (Accelerometer / Linear Acceleration)
  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
  private val linearAccSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
  private var recentAccMagnitude = 0f
  private var sensorListener: SensorEventListener? = null

  init {
    try {
      fusedClient = LocationServices.getFusedLocationProviderClient(context)
      locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      
      sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
          if (event != null) {
            val ax = event.values[0]
            val ay = event.values[1]
            val az = if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) event.values[2] else event.values[2] - 9.81f
            val mag = sqrt(ax * ax + ay * ay + az * az)
            // Low-pass filter
            recentAccMagnitude = recentAccMagnitude * 0.8f + mag * 0.2f
          }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
      }
    } catch (e: Exception) {
      Log.e("GpsLocationEngine", "Init error: ${e.message}")
    }
  }

  private var isBatterySaverMode = false

  fun setBatterySaverMode(enabled: Boolean) {
    if (isBatterySaverMode != enabled) {
      isBatterySaverMode = enabled
      if (_locationState.value.isGpsActive && !_locationState.value.isSimulated) {
        startRealGpsTracking()
      }
    }
  }

  @SuppressLint("MissingPermission")
  fun startRealGpsTracking() {
    stopSimulation()
    customRoadOverride = null
    hasInitialGpsFix = false
    kalmanFilter.reset()

    try {
      // Register sensor fusion accelerometer only if not in battery saver
      if (!isBatterySaverMode && linearAccSensor != null && sensorListener != null) {
        sensorManager?.registerListener(sensorListener, linearAccSensor, SensorManager.SENSOR_DELAY_UI)
      } else if (sensorListener != null) {
        sensorManager?.unregisterListener(sensorListener)
      }

      // 1. Fetch immediate last known location from all available providers
      val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      val lastNet = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
      val lastPassive = locationManager?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
      val bestImmediate = lastGps ?: lastNet ?: lastPassive
      if (bestImmediate != null) {
        processRealGpsLocation(bestImmediate)
      }

      // 2. Instantly request high-accuracy current location fix via FusedClient
      fusedClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)?.addOnSuccessListener { loc ->
        if (loc != null) {
          processRealGpsLocation(loc)
        }
      }
      fusedClient?.lastLocation?.addOnSuccessListener { loc ->
        if (loc != null && !hasInitialGpsFix) {
          processRealGpsLocation(loc)
        }
      }

      // 3. Fused location stream (1000ms for battery saver, 150ms for performance mode)
      val interval = if (isBatterySaverMode) 1000L else 150L
      val minInterval = if (isBatterySaverMode) 800L else 100L
      val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
        .setMinUpdateIntervalMillis(minInterval)
        .setMinUpdateDistanceMeters(0f)
        .setWaitForAccurateLocation(false)
        .build()

      locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
          result.lastLocation?.let { loc ->
            // Bỏ qua các toạ độ có sai số quá lớn (> 35m) khi xe đang chạy
            if (loc.accuracy <= 35f || !hasInitialGpsFix) {
              processRealGpsLocation(loc)
            }
          }
        }
      }

      fusedClient?.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())

      // 4. Dual Hardware GPS + Network Fallback listeners (Only in high-performance mode)
      if (!isBatterySaverMode) {
        androidLocationListener = object : LocationListener {
          override fun onLocationChanged(loc: Location) {
            if (loc.accuracy <= 35f || !hasInitialGpsFix) {
              processRealGpsLocation(loc)
            }
          }
          override fun onProviderEnabled(provider: String) {}
          override fun onProviderDisabled(provider: String) {}
          @Deprecated("Deprecated in Java")
          override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        try {
          locationManager?.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            200L,
            0f,
            androidLocationListener!!,
            Looper.getMainLooper()
          )
          locationManager?.requestLocationUpdates(
            LocationManager.NETWORK_PROVIDER,
            500L,
            0f,
            androidLocationListener!!,
            Looper.getMainLooper()
          )
        } catch (e: Exception) { /* ignore */ }
      }

      _locationState.value = _locationState.value.copy(
        isGpsActive = true,
        isSimulated = false,
        provider = if (isBatterySaverMode) "GPS Tiết Kiệm Pin" else "GPS Vệ Tinh (Đang dò)",
        hasInitialFix = hasInitialGpsFix
      )

      startDeadReckoningWatcher()
    } catch (e: SecurityException) {
      Log.e("GpsLocationEngine", "Permission denied for GPS: ${e.message}")
      startSimulationRoute()
    } catch (e: Exception) {
      Log.e("GpsLocationEngine", "Error starting GPS: ${e.message}")
      startSimulationRoute()
    }
  }

  @SuppressLint("MissingPermission")
  fun refreshGpsLocation() {
    try {
      fusedClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)?.addOnSuccessListener { loc ->
        if (loc != null) {
          processRealGpsLocation(loc)
        }
      }
      val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      val lastNet = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
      val best = lastGps ?: lastNet
      if (best != null) {
        processRealGpsLocation(best)
      }
    } catch (e: Exception) {
      Log.w("GpsLocationEngine", "Refresh location error: ${e.message}")
    }
  }

  fun stopGpsTracking() {
    try {
      locationCallback?.let { fusedClient?.removeLocationUpdates(it) }
      androidLocationListener?.let { locationManager?.removeUpdates(it) }
      sensorListener?.let { sensorManager?.unregisterListener(it) }
    } catch (e: Exception) {
      Log.e("GpsLocationEngine", "Stop error: ${e.message}")
    }
    stopSimulation()
    deadReckoningJob?.cancel()
    deadReckoningJob = null
    _locationState.value = _locationState.value.copy(isGpsActive = false)
  }

  /**
   * Dead Reckoning Watcher: If vehicle was moving and GPS temporarily drops (in tunnel/underpass),
   * smoothly extrapolates position for up to 5 seconds.
   */
  private fun startDeadReckoningWatcher() {
    deadReckoningJob?.cancel()
    deadReckoningJob = scope.launch {
      while (isActive) {
        delay(200L)
        val now = System.currentTimeMillis()
        val isRealGps = !_locationState.value.isSimulated && _locationState.value.isGpsActive
        if (isRealGps && hasInitialGpsFix && (now - lastRealLocationTime) in 900..5000) {
          val curSpeed = smoothSpeed
          if (curSpeed > 6.0f) {
            // Extrapolate position along heading vector
            val dt = 0.2 // 200ms
            val speedMps = curSpeed / 3.6
            val distMeters = speedMps * dt
            val headingRad = Math.toRadians(smoothHeading.toDouble())

            val dLat = (distMeters * cos(headingRad)) / 111320.0
            val dLng = (distMeters * sin(headingRad)) / (111320.0 * cos(Math.toRadians(smoothLat)))

            smoothLat += dLat
            smoothLng += dLng

            val deadReckonLoc = Location("dead_reckoning").apply {
              latitude = smoothLat
              longitude = smoothLng
              speed = curSpeed / 3.6f
              bearing = smoothHeading
              accuracy = 18f
              time = now
            }

            processLocationUpdate(
              location = deadReckonLoc,
              isSimulated = false,
              speedKmh = curSpeed,
              heading = smoothHeading,
              roadName = customRoadOverride,
              provider = "GPS (Quán tính hầm/cầu)",
              hasFix = true
            )
          }
        }
      }
    }
  }

  // Stationary Noise Gate & Velocity Zero-Lock (Khóa 0 km/h khi đứng yên)
  private var stationaryAnchorLat = 0.0
  private var stationaryAnchorLng = 0.0
  private var isStationaryLocked = true
  private var consecutiveLowSpeedCount = 0
  private var consecutiveHighSpeedCount = 0

  /**
   * Real GPS Processing with 2D Kalman Filter, Dynamic Vector Heading, & Instant Speed
   */
  private fun processRealGpsLocation(rawLoc: Location) {
    val now = System.currentTimeMillis()
    lastRealLocationTime = now

    // 1. Tính toán khoảng cách và thời gian di chuyển từ toạ độ GPS trước đó
    val distMoved = if (prevRawLat != 0.0 && prevRawLng != 0.0) {
      VietnamTrafficData.calculateDistanceMeters(prevRawLat, prevRawLng, rawLoc.latitude, rawLoc.longitude)
    } else 0.0
    val dtSeconds = if (prevRawTime > 0L) (now - prevRawTime) / 1000.0 else 1.0

    // 2. Tính tốc độ thực tế (từ cảm biến vệ tinh hoặc vector di chuyển)
    var calculatedSpeedKmh = if (rawLoc.hasSpeed() && rawLoc.speed > 0f) {
      rawLoc.speed * 3.6f
    } else if (distMoved > 1.2 && dtSeconds in 0.1..4.0) {
      ((distMoved / dtSeconds) * 3.6).toFloat()
    } else {
      0f
    }
    // Tránh nhảy tốc độ bất thường do sai số GPS đơn lẻ
    if (calculatedSpeedKmh > 180f) calculatedSpeedKmh = smoothSpeed

    // 3. Tính toán góc di chuyển (Heading/Bearing) vector thực tế khi xe chạy
    val vectorBearing = if (distMoved >= 1.5 && prevRawLat != 0.0) {
      VietnamTrafficData.calculateBearing(prevRawLat, prevRawLng, rawLoc.latitude, rawLoc.longitude)
    } else null

    val rawBearing = when {
      rawLoc.hasBearing() && rawLoc.bearing != 0f -> rawLoc.bearing
      vectorBearing != null -> vectorBearing
      else -> smoothHeading
    }

    prevRawLat = rawLoc.latitude
    prevRawLng = rawLoc.longitude
    prevRawTime = now

    if (!hasInitialGpsFix) {
      kalmanFilter.setState(rawLoc.latitude, rawLoc.longitude, calculatedSpeedKmh)
      smoothLat = rawLoc.latitude
      smoothLng = rawLoc.longitude
      smoothSpeed = if (calculatedSpeedKmh >= 2.5f) calculatedSpeedKmh else 0f
      smoothHeading = rawBearing
      stationaryAnchorLat = rawLoc.latitude
      stationaryAnchorLng = rawLoc.longitude
      isStationaryLocked = calculatedSpeedKmh < 2.5f
      hasInitialGpsFix = true
    } else {
      // Ngưỡng 2.5 km/h: Triệt tiêu hoàn toàn nhiễu Doppler khi đứng yên
      if (calculatedSpeedKmh < 2.5f) {
        isStationaryLocked = true
        smoothSpeed = 0f

        if (stationaryAnchorLat == 0.0 || stationaryAnchorLng == 0.0) {
          stationaryAnchorLat = rawLoc.latitude
          stationaryAnchorLng = rawLoc.longitude
        }

        val distDrift = VietnamTrafficData.calculateDistanceMeters(
          stationaryAnchorLat, stationaryAnchorLng,
          rawLoc.latitude, rawLoc.longitude
        )
        if (distDrift < 10.0) {
          smoothLat = stationaryAnchorLat
          smoothLng = stationaryAnchorLng
        } else {
          smoothLat = rawLoc.latitude
          smoothLng = rawLoc.longitude
          stationaryAnchorLat = rawLoc.latitude
          stationaryAnchorLng = rawLoc.longitude
        }
      } else {
        isStationaryLocked = false
        stationaryAnchorLat = rawLoc.latitude
        stationaryAnchorLng = rawLoc.longitude
        smoothSpeed = calculatedSpeedKmh

        // Run through 2D Kalman Filter for position
        val kalmanResult = kalmanFilter.update(
          rawLat = rawLoc.latitude,
          rawLng = rawLoc.longitude,
          rawAccuracy = rawLoc.accuracy.coerceAtLeast(1.0f),
          rawSpeedKmh = calculatedSpeedKmh,
          timestampMs = rawLoc.time
        )

        smoothLat = kalmanResult.lat
        smoothLng = kalmanResult.lng

        // Cập nhật hướng di chuyển mượt mà và chính xác khi xe di chuyển
        val targetHeading = when {
          rawLoc.hasBearing() && rawLoc.bearing != 0f -> rawLoc.bearing
          vectorBearing != null -> vectorBearing
          else -> smoothHeading
        }

        var diff = targetHeading - smoothHeading
        while (diff > 180f) diff -= 360f
        while (diff < -180f) diff += 360f
        val smoothingFactor = if (smoothSpeed > 15f) 0.85f else 0.65f
        smoothHeading = (smoothHeading + diff * smoothingFactor + 360f) % 360f

        // Apply Vietmap-Grade Map Matching (Orthogonal Snap-to-Road Centerline)
        if (smoothSpeed > 3.0f) {
          val snapped = MapMatchingEngine.snapToRoad(
            rawLat = smoothLat,
            rawLng = smoothLng,
            rawBearing = smoothHeading,
            speedKmh = smoothSpeed
          )
          if (snapped.isSnapped) {
            smoothLat = snapped.snappedLatitude
            smoothLng = snapped.snappedLongitude
            customRoadOverride = snapped.roadName
          } else {
            customRoadOverride = null
          }
        } else {
          customRoadOverride = null
        }
      }
    }

    val smoothedLocation = Location(rawLoc).apply {
      latitude = smoothLat
      longitude = smoothLng
      speed = smoothSpeed / 3.6f
      bearing = smoothHeading
    }

    val accInt = rawLoc.accuracy.toInt().coerceAtLeast(1)
    val provName = when {
      rawLoc.accuracy <= 5f -> "GPS Vệ Tinh (±${accInt}m - Tuyệt hảo)"
      rawLoc.accuracy <= 12f -> "GPS Vệ Tinh (±${accInt}m - Tốt)"
      rawLoc.accuracy <= 25f -> "GPS (±${accInt}m - Khá)"
      else -> "Mạng / Vệ tinh (±${accInt}m)"
    }

    processLocationUpdate(
      location = smoothedLocation,
      isSimulated = false,
      speedKmh = smoothSpeed,
      heading = smoothHeading,
      roadName = customRoadOverride,
      provider = provName,
      hasFix = true
    )

    // Trigger async Geocoding
    if (customRoadOverride == null) {
      triggerAsyncReverseGeocoding(smoothLat, smoothLng)
    }

    // Update active navigation progress
    if (_activeRoute.value != null) {
      updateNavigationProgress(smoothLat, smoothLng)
    }
  }

  /**
   * Ultra-smooth GPS simulation engine (60ms ticks)
   */
  fun startSimulationRoute(routeIndex: Int = 0, speedMultiplier: Float = 1.0f) {
    stopSimulation()
    try {
      locationCallback?.let { fusedClient?.removeLocationUpdates(it) }
    } catch (e: Exception) { /* ignore */ }

    val defaultRoutePoints = when (routeIndex) {
      1 -> VietnamTrafficData.SIMULATION_ROUTE_HANOI
      2 -> VietnamTrafficData.SIMULATION_ROUTE_DANANG
      3 -> VietnamTrafficData.SIMULATION_ROUTE_QL51
      else -> VietnamTrafficData.SIMULATION_ROUTE_SAIGON
    }

    simulationJob = scope.launch {
      var currentSpeed = 45f
      var segmentIndex = 0
      var segmentProgress = 0.0

      var waypoints: List<Pair<Double, Double>> = defaultRoutePoints.map { it.lat to it.lng }
      var currentRoad = defaultRoutePoints[0].roadName

      val tickIntervalMs = 50L // 20 FPS physics update
      val dtSeconds = tickIntervalMs / 1000.0

      while (isActive) {
        val activeNav = _activeRoute.value
        if (activeNav != null && activeNav.waypoints.size >= 2) {
          waypoints = activeNav.waypoints
          currentRoad = activeNav.destinationName
        } else {
          waypoints = defaultRoutePoints.map { it.lat to it.lng }
          currentRoad = defaultRoutePoints[segmentIndex % defaultRoutePoints.size].roadName
        }

        if (waypoints.size < 2) {
          delay(tickIntervalMs)
          continue
        }

        val fromPt = waypoints[segmentIndex % waypoints.size]
        val nextIdx = (segmentIndex + 1) % waypoints.size
        val toPt = waypoints[nextIdx]

        val segmentDistanceMeters = VietnamTrafficData.calculateDistanceMeters(
          fromPt.first, fromPt.second,
          toPt.first, toPt.second
        ).coerceAtLeast(4.0)

        val targetSpeedKmh = if (activeNav != null) {
          50f
        } else {
          defaultRoutePoints[segmentIndex % defaultRoutePoints.size].targetSpeedKmh * speedMultiplier
        }

        // Smooth acceleration / deceleration
        val userSpeedOverride = _locationState.value.speedKmh
        if (abs(userSpeedOverride - currentSpeed) > 8f && userSpeedOverride > 0f) {
          currentSpeed += (userSpeedOverride - currentSpeed) * 0.25f
        } else {
          currentSpeed += (targetSpeedKmh - currentSpeed) * 0.07f
        }

        val speedMps = (currentSpeed / 3.6)
        val distanceTraveledMeters = speedMps * dtSeconds
        segmentProgress += (distanceTraveledMeters / segmentDistanceMeters)

        if (segmentProgress >= 1.0) {
          segmentProgress = 0.0
          segmentIndex = nextIdx
          if (activeNav != null && segmentIndex >= waypoints.size - 1) {
            segmentIndex = 0
          }
        }

        val currentLat = fromPt.first + (toPt.first - fromPt.first) * segmentProgress
        val currentLng = fromPt.second + (toPt.second - fromPt.second) * segmentProgress

        val targetHeading = VietnamTrafficData.calculateBearing(fromPt.first, fromPt.second, toPt.first, toPt.second)
        var headingDiff = targetHeading - smoothHeading
        while (headingDiff > 180f) headingDiff -= 360f
        while (headingDiff < -180f) headingDiff += 360f
        smoothHeading += headingDiff * 0.15f

        val loc = Location("smooth_simulation").apply {
          latitude = currentLat
          longitude = currentLng
          speed = (currentSpeed / 3.6f)
          bearing = smoothHeading
          accuracy = 2.0f
          time = System.currentTimeMillis()
        }

        processLocationUpdate(
          location = loc,
          isSimulated = true,
          speedKmh = currentSpeed,
          heading = smoothHeading,
          roadName = customRoadOverride ?: currentRoad
        )

        if (activeNav != null) {
          updateNavigationProgress(currentLat, currentLng)
        }

        delay(tickIntervalMs)
      }
    }

    _locationState.value = _locationState.value.copy(isGpsActive = true, isSimulated = true)
  }

  fun stopSimulation() {
    simulationJob?.cancel()
    simulationJob = null
  }

  private fun processLocationUpdate(
    location: Location,
    isSimulated: Boolean,
    speedKmh: Float,
    heading: Float,
    roadName: String?,
    provider: String = if (isSimulated) "🎮 Mô phỏng thực tế" else "GPS Vệ Tinh",
    hasFix: Boolean = true
  ) {
    val existingRoad = customRoadOverride ?: roadName ?: _locationState.value.detectedRoadName
    val existingAddress = _locationState.value.detectedAddress

    _locationState.value = GpsLocationState(
      latitude = location.latitude,
      longitude = location.longitude,
      speedKmh = speedKmh.coerceAtLeast(0f),
      headingDegrees = heading,
      altitudeMeters = location.altitude,
      accuracyMeters = location.accuracy,
      timestampMillis = location.time,
      detectedRoadName = existingRoad,
      detectedAddress = existingAddress,
      isGpsActive = true,
      isSimulated = isSimulated,
      provider = provider,
      hasInitialFix = hasFix
    )

    // Trip recording calculations
    if (isTripRecording) {
      lastLocation?.let { prevLoc ->
        val distDelta = location.distanceTo(prevLoc).toDouble()
        if (distDelta in 1.0..300.0) {
          totalDistanceMeters += distDelta
        }
      }
      lastLocation = location

      if (speedKmh > maxSpeedKmh) {
        maxSpeedKmh = speedKmh
      }
      speedSumKmh += speedKmh
      speedSamplesCount++

      val now = System.currentTimeMillis()
      val currentList = _breadcrumbs.value
      val shouldRecord = currentList.isEmpty() || (now - currentList.last().timestamp) > 2200L
      if (shouldRecord && speedKmh > 2f) {
        val newPoint = BreadcrumbPoint(
          latitude = location.latitude,
          longitude = location.longitude,
          speedKmh = speedKmh,
          timestamp = now
        )
        _breadcrumbs.value = (currentList + newPoint).takeLast(140)
      }
    }
  }

  // Active Navigation with Multi-Route Routing
  suspend fun startNavigationToDestination(
    dest: DestinationPlace,
    mode: VehicleRoutingMode = VehicleRoutingMode.MOTORBIKE
  ): NavigationRoute {
    val current = _locationState.value
    val route = NavigationRoutingService.fetchRoute(
      startLat = current.latitude,
      startLng = current.longitude,
      destLat = dest.latitude,
      destLng = dest.longitude,
      destName = dest.name,
      destAddress = dest.address,
      mode = mode
    )
    _activeRoute.value = route
    customRoadOverride = "Đến ${dest.name}"
    lastAlertedStepIndex = -1
    lastAlertedDistanceBand = -1
    offRouteCount = 0
    return route
  }

  suspend fun startNavigationToCustomCoord(
    name: String,
    address: String,
    lat: Double,
    lng: Double,
    mode: VehicleRoutingMode = VehicleRoutingMode.MOTORBIKE
  ): NavigationRoute {
    val current = _locationState.value
    val route = NavigationRoutingService.fetchRoute(
      startLat = current.latitude,
      startLng = current.longitude,
      destLat = lat,
      destLng = lng,
      destName = name,
      destAddress = address,
      mode = mode
    )
    _activeRoute.value = route
    customRoadOverride = "Đến $name"
    lastAlertedStepIndex = -1
    lastAlertedDistanceBand = -1
    offRouteCount = 0
    return route
  }

  fun switchActiveRoute(selectedRoute: NavigationRoute) {
    _activeRoute.value = selectedRoute.copy(isNavigating = true)
    lastAlertedStepIndex = -1
    lastAlertedDistanceBand = -1
    offRouteCount = 0
    onTurnVoicePrompt?.invoke("Đã chuyển sang ${selectedRoute.routeTag}")
  }

  fun cancelNavigation() {
    _activeRoute.value = null
    customRoadOverride = null
    lastAlertedStepIndex = -1
    lastAlertedDistanceBand = -1
    offRouteCount = 0
  }

  private fun updateNavigationProgress(currentLat: Double, currentLng: Double) {
    val route = _activeRoute.value ?: return
    val distToDest = VietnamTrafficData.calculateDistanceMeters(
      currentLat, currentLng,
      route.destinationLat, route.destinationLng
    ).toInt()

    // Dynamic Intelligent ETA Calculation based on Vietnam Traffic Time-of-day & Real-time Speed
    val calendar = java.util.Calendar.getInstance()
    val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
    val minute = calendar.get(java.util.Calendar.MINUTE)
    val timeDecimal = hour + (minute / 60.0)

    val isMorningPeak = timeDecimal in 7.0..9.0
    val isEveningPeak = timeDecimal in 16.5..19.0
    val isNightClear = timeDecimal >= 21.5 || timeDecimal < 6.0

    val baseTrafficSpeed = when {
      isMorningPeak || isEveningPeak -> 18.0 // Giờ cao điểm TP.HCM / Hà Nội
      isNightClear -> 48.0 // Đêm khuya vắng xe
      else -> 32.0 // Giờ bình thường
    }

    val curSpeed = _locationState.value.speedKmh
    val effectiveSpeed = if (curSpeed > 10f) {
      (curSpeed * 0.6 + baseTrafficSpeed * 0.4).coerceIn(12.0, 80.0)
    } else {
      baseTrafficSpeed
    }

    val durationMinutes = ((distToDest / 1000.0) / effectiveSpeed * 60.0).toInt().coerceAtLeast(1)

    // 1. SMART ANTI-SPURIOUS OFF-ROUTE DETECTION & AUTO-REROUTING
    var minDistanceToRoute = Double.MAX_VALUE
    val waypoints = route.waypoints
    if (waypoints.isNotEmpty()) {
      for (i in 0 until waypoints.size) {
        val dist = VietnamTrafficData.calculateDistanceMeters(
          currentLat, currentLng,
          waypoints[i].first, waypoints[i].second
        )
        if (dist < minDistanceToRoute) {
          minDistanceToRoute = dist
        }
      }
    }

    val now = System.currentTimeMillis()
    // Tolerant off-route threshold (38m) to avoid false rerouting in alleys or GPS drift
    val isCandidateOffRoute = minDistanceToRoute > 38.0 && distToDest > 45

    if (isCandidateOffRoute && !isRerouting && (now - lastRerouteTime) > 4500) {
      offRouteCount++
      // Require 3 consecutive confirmations (~3.5 - 4.5 seconds) before rerouting
      if (offRouteCount >= 3) {
        offRouteCount = 0
        isRerouting = true
        lastRerouteTime = now
        scope.launch {
          try {
            onTurnVoicePrompt?.invoke("Đang tính lộ trình mới phía trước.")
            val newRoute = NavigationRoutingService.fetchRoute(
              startLat = currentLat,
              startLng = currentLng,
              destLat = route.destinationLat,
              destLng = route.destinationLng,
              destName = route.destinationName,
              destAddress = route.destinationAddress
            )
            _activeRoute.value = newRoute
            lastAlertedStepIndex = -1
            lastAlertedDistanceBand = -1
          } catch (e: Exception) {
            Log.w("GpsLocationEngine", "Auto-reroute failed: ${e.message}")
          } finally {
            isRerouting = false
          }
        }
        return
      }
    } else if (minDistanceToRoute <= 25.0) {
      offRouteCount = 0
    }

    // 2. DESTINATION ARRIVAL CHECK
    if (distToDest <= 25) {
      if (lastAlertedDistanceBand != 9999) {
        lastAlertedDistanceBand = 9999
        onTurnVoicePrompt?.invoke("Bạn đã đến đích!")
      }
    }

    // 3. STEP PROGRESSION & MANEUVER VOICE GUIDANCE
    var currentStepIndex = route.currentStepIndex
    val updatedSteps = route.steps.toMutableList()

    if (updatedSteps.isNotEmpty()) {
      var targetManeuverIndex = if (currentStepIndex == 0 && updatedSteps.size > 1 && updatedSteps[0].maneuver == NavigationManeuverType.DEPART) {
        1
      } else {
        (currentStepIndex + 1).coerceAtMost(updatedSteps.size - 1)
      }

      val targetStep = updatedSteps[targetManeuverIndex]
      val distToTargetStep = VietnamTrafficData.calculateDistanceMeters(
        currentLat, currentLng,
        targetStep.latitude, targetStep.longitude
      ).toInt()

      // Advance step when user reaches within 20m of the maneuver intersection
      if (distToTargetStep < 20 && targetManeuverIndex < updatedSteps.size - 1) {
        currentStepIndex = targetManeuverIndex
        targetManeuverIndex = (currentStepIndex + 1).coerceAtMost(updatedSteps.size - 1)
        lastAlertedDistanceBand = -1
      }

      // Update countdown distance for active step
      val activeManeuverStep = updatedSteps[targetManeuverIndex]
      val activeDist = VietnamTrafficData.calculateDistanceMeters(
        currentLat, currentLng,
        activeManeuverStep.latitude, activeManeuverStep.longitude
      ).toInt()

      updatedSteps[targetManeuverIndex] = activeManeuverStep.copy(distanceMeters = activeDist.coerceAtLeast(0))

      // Timely, Concise Voice Guidance Prompts (300m, 100m, 30m)
      val band = when {
        activeDist in 220..380 -> 300
        activeDist in 60..150 -> 100
        activeDist in 15..45 -> 30
        else -> -1
      }

      if (band != -1 && (lastAlertedStepIndex != targetManeuverIndex || lastAlertedDistanceBand != band)) {
        lastAlertedStepIndex = targetManeuverIndex
        lastAlertedDistanceBand = band
        val cleanInstruction = activeManeuverStep.instruction.replace("Phía trước ", "", ignoreCase = true)
        val prompt = when (band) {
          30 -> "Chuẩn bị $cleanInstruction!"
          100 -> "100 mét nữa, $cleanInstruction."
          else -> "300 mét nữa, $cleanInstruction."
        }
        onTurnVoicePrompt?.invoke(prompt)
      }
    }

    _activeRoute.value = route.copy(
      currentStepIndex = currentStepIndex,
      steps = updatedSteps,
      totalDistanceMeters = distToDest,
      estimatedDurationMinutes = durationMinutes
    )
  }

  fun setSimulatedSpeedOverride(speedKmh: Float) {
    val current = _locationState.value
    _locationState.value = current.copy(
      speedKmh = speedKmh.coerceAtLeast(0f),
      isSimulated = true
    )
  }

  fun setCustomTestRoad(roadName: String, address: String? = null) {
    customRoadOverride = roadName
    val current = _locationState.value
    _locationState.value = current.copy(
      detectedRoadName = roadName,
      detectedAddress = address ?: "Tuyến đường thử nghiệm"
    )
  }

  private fun triggerAsyncReverseGeocoding(lat: Double, lng: Double) {
    val now = System.currentTimeMillis()
    val dist = VietnamTrafficData.calculateDistanceMeters(lat, lng, lastGeocodedLat, lastGeocodedLng)
    if (dist < 25.0 && (now - lastGeocodedTime) < 4000) {
      return
    }

    lastGeocodedLat = lat
    lastGeocodedLng = lng
    lastGeocodedTime = now

    scope.launch(Dispatchers.IO) {
      try {
        val currentSpeed = _locationState.value.speedKmh

        // Check nearest major road from preloaded arterial network
        val nearestMajorRoad = VietnamTrafficData.ALL_ROADS.minByOrNull { road ->
          road.coordinates.minOfOrNull { (rLat, rLng) ->
            VietnamTrafficData.calculateDistanceMeters(lat, lng, rLat, rLng)
          } ?: Double.MAX_VALUE
        }
        val distToMajorRoad = nearestMajorRoad?.let { road ->
          road.coordinates.minOfOrNull { (rLat, rLng) ->
            VietnamTrafficData.calculateDistanceMeters(lat, lng, rLat, rLng)
          }
        } ?: Double.MAX_VALUE

        // 1. Try OSM Online Road & Speed Tag Lookup First
        val osmInfo = NavigationRoutingService.fetchOsmRoadInfo(lat, lng)
        if (osmInfo != null && osmInfo.roadName.isNotBlank() && osmInfo.roadName != "Tuyến đường") {
          // Update OsmRoadSpeedLimits Cache
          OsmRoadSpeedLimits.cachedHighwayTag = osmInfo.highwayType
          OsmRoadSpeedLimits.cachedSpeedLimit = osmInfo.maxSpeedKmh ?: OsmRoadSpeedLimits.getSpeedLimit(osmInfo.highwayType, currentSpeed)
          OsmRoadSpeedLimits.cacheAnchorLat = lat
          OsmRoadSpeedLimits.cacheAnchorLng = lng

          val rawName = osmInfo.roadName.trim()
          val isAlley = isAlleyWayName(rawName)

          val resolvedRoadName = if (isAlley && (currentSpeed > 20f || distToMajorRoad < 45.0) && nearestMajorRoad != null) {
            nearestMajorRoad.name
          } else if (rawName.startsWith("Đường", ignoreCase = true) ||
            rawName.startsWith("Đại lộ", ignoreCase = true) ||
            rawName.startsWith("Quốc lộ", ignoreCase = true) ||
            rawName.startsWith("Hẻm", ignoreCase = true) ||
            rawName.startsWith("Ngõ", ignoreCase = true) ||
            rawName.startsWith("Ngách", ignoreCase = true) ||
            rawName.startsWith("Cầu", ignoreCase = true) ||
            rawName.startsWith("Tỉnh lộ", ignoreCase = true) ||
            rawName.startsWith("Phố", ignoreCase = true)) {
            rawName
          } else {
            "Đường $rawName"
          }

          val fullAddr = listOf(osmInfo.suburb, osmInfo.city).filter { it.isNotBlank() }.joinToString(", ")

          withContext(Dispatchers.Main) {
            _locationState.value = _locationState.value.copy(
              detectedRoadName = resolvedRoadName,
              detectedAddress = fullAddr.ifBlank { "Việt Nam" }
            )
          }
          return@launch
        }

        // 2. Fallback to Android Hardware Geocoder
        if (geocoder != null) {
          @Suppress("DEPRECATION")
          val addresses = geocoder.getFromLocation(lat, lng, 1)
          if (!addresses.isNullOrEmpty()) {
            val addr = addresses[0]
            val street = (addr.thoroughfare ?: addr.featureName ?: addr.subLocality)?.trim()
            val districtCity = listOfNotNull(addr.subAdminArea, addr.adminArea).joinToString(", ")
            
            val isAlley = street?.let { isAlleyWayName(it) } ?: false

            val fullRoadName = when {
              isAlley && (currentSpeed > 20f || distToMajorRoad < 45.0) && nearestMajorRoad != null -> {
                nearestMajorRoad.name
              }
              !street.isNullOrBlank() -> {
                if (street.startsWith("Đường", ignoreCase = true) ||
                  street.startsWith("Đại lộ", ignoreCase = true) ||
                  street.startsWith("Quốc lộ", ignoreCase = true) ||
                  street.startsWith("Hẻm", ignoreCase = true) ||
                  street.startsWith("Ngõ", ignoreCase = true) ||
                  street.startsWith("Cầu", ignoreCase = true) ||
                  street.startsWith("Phố", ignoreCase = true)) {
                  street
                } else {
                  "Đường $street"
                }
              }
              nearestMajorRoad != null && distToMajorRoad < 100.0 -> nearestMajorRoad.name
              else -> null
            }

            if (fullRoadName != null) {
              withContext(Dispatchers.Main) {
                _locationState.value = _locationState.value.copy(
                  detectedRoadName = fullRoadName,
                  detectedAddress = districtCity.ifBlank { "Việt Nam" }
                )
              }
            }
          }
        }
      } catch (e: Exception) {
        Log.w("GpsLocationEngine", "Geocode non-fatal: ${e.message}")
      }
    }
  }

  fun startTripRecording() {
    isTripRecording = true
    tripStartTime = System.currentTimeMillis()
    totalDistanceMeters = 0.0
    maxSpeedKmh = 0f
    speedSumKmh = 0.0
    speedSamplesCount = 0
    overspeedCount = 0
    _breadcrumbs.value = emptyList()
    lastLocation = null
  }

  fun recordOverspeedEvent() {
    overspeedCount++
  }

  fun stopTripRecording(): CurrentTripStats {
    isTripRecording = false
    val now = System.currentTimeMillis()
    val durationSec = if (tripStartTime > 0) (now - tripStartTime) / 1000 else 0
    val avgSpeed = if (speedSamplesCount > 0) (speedSumKmh / speedSamplesCount).toFloat() else 0f

    return CurrentTripStats(
      distanceKm = (totalDistanceMeters / 1000.0).toFloat(),
      durationSeconds = durationSec,
      maxSpeedKmh = maxSpeedKmh,
      avgSpeedKmh = avgSpeed,
      overspeedEvents = overspeedCount,
      startTimeMillis = tripStartTime,
      endTimeMillis = now
    )
  }

  fun getCurrentTripMetrics(): CurrentTripStats {
    val now = System.currentTimeMillis()
    val durationSec = if (tripStartTime > 0) (now - tripStartTime) / 1000 else 0
    val avgSpeed = if (speedSamplesCount > 0) (speedSumKmh / speedSamplesCount).toFloat() else 0f

    return CurrentTripStats(
      distanceKm = (totalDistanceMeters / 1000.0).toFloat(),
      durationSeconds = durationSec,
      maxSpeedKmh = maxSpeedKmh,
      avgSpeedKmh = avgSpeed,
      overspeedEvents = overspeedCount,
      startTimeMillis = tripStartTime,
      endTimeMillis = now
    )
  }
}

/**
 * 2D Kalman Filter for GPS coordinates (Lat, Lng) and velocity.
 */
class GpsKalmanFilter {
  private var lat = 0.0
  private var lng = 0.0
  private var variance = -1.0 // -1 means uninitialized
  private var speedKmh = 0f
  private var lastTimestampMs = 0L

  data class KalmanResult(val lat: Double, val lng: Double, val speedKmh: Float)

  fun setState(initialLat: Double, initialLng: Double, initialSpeed: Float) {
    lat = initialLat
    lng = initialLng
    speedKmh = initialSpeed
    variance = 16.0
    lastTimestampMs = System.currentTimeMillis()
  }

  fun reset() {
    variance = -1.0
  }

  fun update(rawLat: Double, rawLng: Double, rawAccuracy: Float, rawSpeedKmh: Float, timestampMs: Long): KalmanResult {
    if (variance < 0.0) {
      setState(rawLat, rawLng, rawSpeedKmh)
      return KalmanResult(rawLat, rawLng, rawSpeedKmh)
    }

    val dtSeconds = ((timestampMs - lastTimestampMs) / 1000.0).coerceIn(0.1, 3.0)
    lastTimestampMs = timestampMs

    // Process noise (Q) - higher value allows faster adaptation to real movement
    val qMetresPerSecond = 3.0
    variance += dtSeconds * qMetresPerSecond * qMetresPerSecond

    // Measurement noise (R) proportional to GPS accuracy
    val rVariance = (rawAccuracy * rawAccuracy).toDouble()

    // Kalman Gain (K)
    val kGain = variance / (variance + rVariance)

    // State update
    lat += kGain * (rawLat - lat)
    lng += kGain * (rawLng - lng)
    variance = (1.0 - kGain) * variance

    speedKmh = rawSpeedKmh

    return KalmanResult(lat, lng, speedKmh)
  }
}

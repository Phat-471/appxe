package com.example.service

import android.annotation.SuppressLint
import android.content.Context
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

  init {
    try {
      fusedClient = LocationServices.getFusedLocationProviderClient(context)
      locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    } catch (e: Exception) {
      Log.e("GpsLocationEngine", "Init error: ${e.message}")
    }
  }

  @SuppressLint("MissingPermission")
  fun startRealGpsTracking() {
    stopSimulation()
    customRoadOverride = null
    hasInitialGpsFix = false
    kalmanFilter.reset()

    try {
      // 1. Instantly request high-accuracy current location fix
      fusedClient?.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)?.addOnSuccessListener { loc ->
        if (loc != null) {
          processRealGpsLocation(loc)
        }
      }

      // 2. Fetch last known location as instant baseline
      fusedClient?.lastLocation?.addOnSuccessListener { loc ->
        if (loc != null && !hasInitialGpsFix) {
          processRealGpsLocation(loc)
        }
      }

      val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      val lastNet = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
      val bestLast = lastGps ?: lastNet
      if (bestLast != null && !hasInitialGpsFix) {
        processRealGpsLocation(bestLast)
      }

      // 3. High-precision Real-time Fused location stream (200ms interval, 0m displacement)
      val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 200L)
        .setMinUpdateIntervalMillis(150L)
        .setMinUpdateDistanceMeters(0f)
        .setWaitForAccurateLocation(false)
        .build()

      locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
          result.lastLocation?.let { loc ->
            processRealGpsLocation(loc)
          }
        }
      }

      fusedClient?.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())

      // 4. Hardware GPS & Network fallback listeners
      androidLocationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
          processRealGpsLocation(loc)
        }
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
      }

      try {
        locationManager?.requestLocationUpdates(
          LocationManager.GPS_PROVIDER,
          350L,
          0f,
          androidLocationListener!!,
          Looper.getMainLooper()
        )
      } catch (e: Exception) { /* ignore */ }

      try {
        locationManager?.requestLocationUpdates(
          LocationManager.NETWORK_PROVIDER,
          700L,
          0f,
          androidLocationListener!!,
          Looper.getMainLooper()
        )
      } catch (e: Exception) { /* ignore */ }

      _locationState.value = _locationState.value.copy(
        isGpsActive = true,
        isSimulated = false,
        provider = "GPS Vệ Tinh (Đang dò)",
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
   * Real GPS Processing with 2D Kalman Filter, Stationary Noise Gate & Bearing Smoothing
   */
  private fun processRealGpsLocation(rawLoc: Location) {
    lastRealLocationTime = System.currentTimeMillis()
    val rawSpeedKmh = if (rawLoc.hasSpeed()) rawLoc.speed * 3.6f else 0f
    val rawBearing = if (rawLoc.hasBearing() && rawLoc.bearing != 0f) rawLoc.bearing else smoothHeading

    if (!hasInitialGpsFix) {
      kalmanFilter.setState(rawLoc.latitude, rawLoc.longitude, 0f)
      smoothLat = rawLoc.latitude
      smoothLng = rawLoc.longitude
      smoothSpeed = 0f
      smoothHeading = rawBearing
      stationaryAnchorLat = rawLoc.latitude
      stationaryAnchorLng = rawLoc.longitude
      isStationaryLocked = true
      hasInitialGpsFix = true
    } else {
      if (stationaryAnchorLat == 0.0) {
        stationaryAnchorLat = rawLoc.latitude
        stationaryAnchorLng = rawLoc.longitude
      }

      val distFromAnchor = VietnamTrafficData.calculateDistanceMeters(
        stationaryAnchorLat, stationaryAnchorLng,
        rawLoc.latitude, rawLoc.longitude
      )

      // Stationary noise gate: detect if vehicle is stopped (< 3.8 km/h or within 4m)
      if (rawSpeedKmh < 3.8f || (distFromAnchor < 4.0 && rawSpeedKmh < 4.8f)) {
        consecutiveLowSpeedCount++
        consecutiveHighSpeedCount = 0
        if (consecutiveLowSpeedCount >= 2 || rawSpeedKmh < 2.5f) {
          isStationaryLocked = true
          smoothSpeed = 0f
          // Freeze position to stationary anchor to prevent map drift
          smoothLat = stationaryAnchorLat
          smoothLng = stationaryAnchorLng
        }
      } else {
        consecutiveHighSpeedCount++
        if (consecutiveHighSpeedCount >= 2 || rawSpeedKmh >= 5.5f) {
          isStationaryLocked = false
          consecutiveLowSpeedCount = 0
          stationaryAnchorLat = rawLoc.latitude
          stationaryAnchorLng = rawLoc.longitude

          // Run through 2D Kalman Filter for position & velocity
          val kalmanResult = kalmanFilter.update(
            rawLat = rawLoc.latitude,
            rawLng = rawLoc.longitude,
            rawAccuracy = rawLoc.accuracy.coerceAtLeast(1.0f),
            rawSpeedKmh = rawSpeedKmh,
            timestampMs = rawLoc.time
          )

          smoothLat = kalmanResult.lat
          smoothLng = kalmanResult.lng
          smoothSpeed = rawSpeedKmh

          // Fast, responsive heading update only when vehicle is moving
          if (rawLoc.hasBearing() && rawLoc.bearing != 0f && smoothSpeed > 2.0f) {
            var diff = rawLoc.bearing - smoothHeading
            while (diff > 180f) diff -= 360f
            while (diff < -180f) diff += 360f
            smoothHeading += diff * 0.7f
          }
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

  // Active Navigation with OSRM Routing
  suspend fun startNavigationToDestination(dest: DestinationPlace): NavigationRoute {
    val current = _locationState.value
    val route = NavigationRoutingService.fetchRoute(
      startLat = current.latitude,
      startLng = current.longitude,
      destLat = dest.latitude,
      destLng = dest.longitude,
      destName = dest.name,
      destAddress = dest.address
    )
    _activeRoute.value = route
    customRoadOverride = "Đến ${dest.name}"
    lastAlertedStepIndex = -1
    lastAlertedDistanceBand = -1
    return route
  }

  suspend fun startNavigationToCustomCoord(name: String, address: String, lat: Double, lng: Double): NavigationRoute {
    val current = _locationState.value
    val route = NavigationRoutingService.fetchRoute(
      startLat = current.latitude,
      startLng = current.longitude,
      destLat = lat,
      destLng = lng,
      destName = name,
      destAddress = address
    )
    _activeRoute.value = route
    customRoadOverride = "Đến $name"
    lastAlertedStepIndex = -1
    lastAlertedDistanceBand = -1
    return route
  }

  fun cancelNavigation() {
    _activeRoute.value = null
    customRoadOverride = null
    lastAlertedStepIndex = -1
    lastAlertedDistanceBand = -1
  }

  private fun updateNavigationProgress(currentLat: Double, currentLng: Double) {
    val route = _activeRoute.value ?: return
    val distToDest = VietnamTrafficData.calculateDistanceMeters(
      currentLat, currentLng,
      route.destinationLat, route.destinationLng
    ).toInt()

    val durationMinutes = ((distToDest / 1000.0) / 30.0 * 60.0).toInt().coerceAtLeast(1)

    // Check if vehicle has deviated from the route polyline (Off-route detection)
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
    if (minDistanceToRoute > 28.0 && distToDest > 50 && !isRerouting && (now - lastRerouteTime) > 3000) {
      offRouteCount++
      if (offRouteCount >= 1) {
        offRouteCount = 0
        isRerouting = true
        lastRerouteTime = now
        scope.launch {
          try {
            onTurnVoicePrompt?.invoke("Đang tự động tính lại lộ trình mới!")
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

    // Find nearest step index & update realtime step distance
    var currentStepIndex = route.currentStepIndex
    val updatedSteps = route.steps.toMutableList()

    if (updatedSteps.isNotEmpty() && currentStepIndex < updatedSteps.size) {
      val nextStep = updatedSteps[currentStepIndex]
      val distToNextStep = VietnamTrafficData.calculateDistanceMeters(
        currentLat, currentLng,
        nextStep.latitude, nextStep.longitude
      ).toInt()

      // Update remaining distance for the upcoming maneuver
      updatedSteps[currentStepIndex] = nextStep.copy(distanceMeters = distToNextStep.coerceAtLeast(0))

      if (distToNextStep < 25 && currentStepIndex < updatedSteps.size - 1) {
        currentStepIndex++
      }

      // Voice prompt trigger for upcoming turn
      val upcomingStep = updatedSteps.getOrNull(currentStepIndex)
      if (upcomingStep != null) {
        val stepDist = distToNextStep.coerceAtLeast(15)
        val band = when {
          stepDist in 220..380 -> 300
          stepDist in 60..160 -> 100
          stepDist < 35 -> 30
          else -> -1
        }

        if (band != -1 && (lastAlertedStepIndex != currentStepIndex || lastAlertedDistanceBand != band)) {
          lastAlertedStepIndex = currentStepIndex
          lastAlertedDistanceBand = band
          val prompt = if (band == 30) {
            "${upcomingStep.instruction} ngay bây giờ!"
          } else {
            "Phía trước $band mét, ${upcomingStep.instruction.lowercase()}."
          }
          onTurnVoicePrompt?.invoke(prompt)
        }
      }
    }

    _activeRoute.value = route.copy(
      totalDistanceMeters = distToDest,
      estimatedDurationMinutes = durationMinutes,
      currentStepIndex = currentStepIndex,
      steps = updatedSteps
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
        if (geocoder != null) {
          @Suppress("DEPRECATION")
          val addresses = geocoder.getFromLocation(lat, lng, 1)
          if (!addresses.isNullOrEmpty()) {
            val addr = addresses[0]
            val street = addr.thoroughfare ?: addr.featureName ?: addr.subLocality
            val districtCity = listOfNotNull(addr.subAdminArea, addr.adminArea).joinToString(", ")
            val fullRoadName = if (!street.isNullOrBlank()) {
              if (street.startsWith("Đường", ignoreCase = true) || street.startsWith("Đại lộ", ignoreCase = true) || street.startsWith("Quốc lộ", ignoreCase = true)) {
                street
              } else {
                "Đường $street"
              }
            } else {
              null
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

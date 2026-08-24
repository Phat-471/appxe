package com.example.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.datasource.MockSpeedLimitDataSource
import com.example.data.model.VisualAlertLevel
import com.example.data.model.VisualSpeedAlertState
import com.example.ui.widget.SpeedAlertWidgetProvider
import com.google.android.gms.location.*
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SpeedLimitTrackingService : Service() {

  companion object {
    const val TAG = "SpeedLimitTrackingService"
    const val NOTIFICATION_CHANNEL_ID = "speed_limit_alert_channel"
    const val NOTIFICATION_ID = 2026

    const val ACTION_START = "com.example.action.START_SPEED_TRACKING"
    const val ACTION_STOP = "com.example.action.STOP_SPEED_TRACKING"
    const val ACTION_SET_SPEED_LIMIT_OVERRIDE = "com.example.action.SET_LIMIT_OVERRIDE"

    // Global Reactive StateFlow accessible by UI and ViewModels
    private val _visualAlertState = MutableStateFlow(VisualSpeedAlertState())
    val visualAlertState: StateFlow<VisualSpeedAlertState> = _visualAlertState.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    fun startService(context: Context) {
      val intent = Intent(context, SpeedLimitTrackingService::class.java).apply {
        action = ACTION_START
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
      } else {
        context.startService(intent)
      }
    }

    fun stopService(context: Context) {
      val intent = Intent(context, SpeedLimitTrackingService::class.java).apply {
        action = ACTION_STOP
      }
      context.startService(intent)
    }

    // Direct simulated injection helper for developer testing & UI previews
    fun updateSimulatedState(
      speedKmh: Float,
      lat: Double = 10.7580,
      lng: Double = 106.6850,
      roadName: String? = null,
      heading: Float = 65f
    ) {
      val (speedLimit, detectedRoad) = MockSpeedLimitDataSource.getSpeedLimitForLocation(lat, lng, roadName)
      val currentSpeedInt = speedKmh.roundToInt().coerceAtLeast(0)
      val speedDelta = currentSpeedInt - speedLimit
      val isOverspeeding = speedDelta > 0

      val alertLevel = when {
        speedDelta > 15 -> VisualAlertLevel.CRITICAL
        speedDelta in 6..15 -> VisualAlertLevel.DANGER
        speedDelta in 1..5 -> VisualAlertLevel.WARNING
        speedDelta in -3..0 -> VisualAlertLevel.APPROACHING_LIMIT
        speedDelta < -15 -> VisualAlertLevel.SAFE
        else -> VisualAlertLevel.NORMAL
      }

      val message = when (alertLevel) {
        VisualAlertLevel.CRITICAL -> "🚨 VƯỢT TỐC ĐỘ NGUY HIỂM! (+${speedDelta} km/h)"
        VisualAlertLevel.DANGER -> "⚠️ CẢNH BÁO: ĐANG CHẠY QUÁ TỐC ĐỘ (+${speedDelta} km/h)"
        VisualAlertLevel.WARNING -> "⚡ Chú ý: Vượt giới hạn nhẹ (+${speedDelta} km/h)"
        VisualAlertLevel.APPROACHING_LIMIT -> "👀 Đang tiến sát giới hạn ($currentSpeedInt/$speedLimit km/h)"
        VisualAlertLevel.SAFE -> "✅ Tốc độ an toàn lý tưởng ($currentSpeedInt/$speedLimit km/h)"
        VisualAlertLevel.NORMAL -> "👍 Tốc độ trong quy định ($currentSpeedInt/$speedLimit km/h)"
      }

      _visualAlertState.value = VisualSpeedAlertState(
        currentSpeedKmh = currentSpeedInt,
        speedLimitKmh = speedLimit,
        speedDeltaKmh = speedDelta,
        isOverspeeding = isOverspeeding,
        alertLevel = alertLevel,
        roadName = detectedRoad,
        alertMessage = message,
        isServiceActive = _isServiceRunning.value,
        isGpsLocked = true,
        latitude = lat,
        longitude = lng,
        headingDegrees = heading,
        timestamp = System.currentTimeMillis()
      )
    }
  }

  private val binder = LocalBinder()
  private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  private var fusedLocationClient: FusedLocationProviderClient? = null
  private var locationCallback: LocationCallback? = null
  private var locationManager: LocationManager? = null
  private var fallbackLocationListener: LocationListener? = null
  private var geocoder: Geocoder? = null

  private var manualLimitOverride: Int? = null

  inner class LocalBinder : Binder() {
    fun getService(): SpeedLimitTrackingService = this@SpeedLimitTrackingService
  }

  override fun onBind(intent: Intent?): IBinder = binder

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    geocoder = try {
      Geocoder(this, Locale("vi", "VN"))
    } catch (_: Exception) {
      null
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_STOP -> {
        stopTracking()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return START_NOT_STICKY
      }
      ACTION_SET_SPEED_LIMIT_OVERRIDE -> {
        val limit = intent.getIntExtra("LIMIT", -1)
        manualLimitOverride = if (limit > 0) limit else null
      }
      else -> {
        startForeground(NOTIFICATION_ID, buildNotification(_visualAlertState.value))
        startTracking()
      }
    }
    return START_STICKY
  }

  @SuppressLint("MissingPermission")
  private fun startTracking() {
    _isServiceRunning.value = true
    initLocationTracking()
  }

  @SuppressLint("MissingPermission")
  private fun initLocationTracking() {
    try {
      val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 250L)
        .setMinUpdateIntervalMillis(150L)
        .setMinUpdateDistanceMeters(0f)
        .build()

      locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
          result.lastLocation?.let { location ->
            processNewLocation(location)
          }
        }
      }

      locationCallback?.let { cb ->
        fusedLocationClient?.requestLocationUpdates(locationRequest, cb, Looper.getMainLooper())
      }

      // Fallback location listener using Android LocationManager
      fallbackLocationListener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
          processNewLocation(loc)
        }
        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
      }

      locationManager?.let { mgr ->
        if (mgr.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
          fallbackLocationListener?.let { lsnr ->
            mgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 250L, 0f, lsnr, Looper.getMainLooper())
          }
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error starting location tracking: ${e.message}")
    }
  }

  private fun processNewLocation(location: Location) {
    serviceScope.launch {
      // 1. Calculate speed in km/h immediately
      val speedKmh = if (location.hasSpeed()) {
        location.speed * 3.6f
      } else {
        0f
      }

      val lat = location.latitude
      val lng = location.longitude
      val heading = if (location.hasBearing()) location.bearing else 0f

      val currentRoad = _visualAlertState.value.roadName
      val (mockLimit, roadName) = MockSpeedLimitDataSource.getSpeedLimitForLocation(lat, lng, currentRoad, speedKmh)
      val effectiveLimit = manualLimitOverride ?: mockLimit

      // 2. Compare speed vs limit and evaluate visual alert level instantly
      val currentSpeedInt = speedKmh.roundToInt().coerceAtLeast(0)
      val speedDelta = currentSpeedInt - effectiveLimit
      val isOverspeeding = speedDelta > 0

      val alertLevel = when {
        speedDelta > 15 -> VisualAlertLevel.CRITICAL
        speedDelta in 6..15 -> VisualAlertLevel.DANGER
        speedDelta in 1..5 -> VisualAlertLevel.WARNING
        speedDelta in -3..0 -> VisualAlertLevel.APPROACHING_LIMIT
        speedDelta < -15 -> VisualAlertLevel.SAFE
        else -> VisualAlertLevel.NORMAL
      }

      val message = when (alertLevel) {
        VisualAlertLevel.CRITICAL -> "🚨 VƯỢT TỐC ĐỘ NGUY HIỂM! (+${speedDelta} km/h)"
        VisualAlertLevel.DANGER -> "⚠️ CẢNH BÁO: ĐANG CHẠY QUÁ TỐC ĐỘ (+${speedDelta} km/h)"
        VisualAlertLevel.WARNING -> "⚡ Chú ý: Vượt giới hạn nhẹ (+${speedDelta} km/h)"
        VisualAlertLevel.APPROACHING_LIMIT -> "👀 Đang tiến sát giới hạn ($currentSpeedInt/$effectiveLimit km/h)"
        VisualAlertLevel.SAFE -> "✅ Tốc độ an toàn lý tưởng ($currentSpeedInt/$effectiveLimit km/h)"
        VisualAlertLevel.NORMAL -> "👍 Tốc độ trong quy định ($currentSpeedInt/$effectiveLimit km/h)"
      }

      val newState = VisualSpeedAlertState(
        currentSpeedKmh = currentSpeedInt,
        speedLimitKmh = effectiveLimit,
        speedDeltaKmh = speedDelta,
        isOverspeeding = isOverspeeding,
        alertLevel = alertLevel,
        roadName = roadName,
        alertMessage = message,
        isServiceActive = true,
        isGpsLocked = true,
        latitude = lat,
        longitude = lng,
        headingDegrees = heading,
        timestamp = System.currentTimeMillis()
      )

      _visualAlertState.value = newState
      updateNotification(newState)

      // 3. Asynchronous background reverse geocoding
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          geocoder?.getFromLocation(lat, lng, 1) { addrs ->
            if (addrs.isNotEmpty()) {
              val detected = addrs[0].thoroughfare ?: addrs[0].featureName
              if (!detected.isNullOrBlank() && detected != roadName) {
                _visualAlertState.value = _visualAlertState.value.copy(roadName = detected)
              }
            }
          }
        }
      } catch (_: Exception) {}
    }
  }

  private fun stopTracking() {
    _isServiceRunning.value = false
    try {
      locationCallback?.let { fusedLocationClient?.removeLocationUpdates(it) }
      fallbackLocationListener?.let { locationManager?.removeUpdates(it) }
    } catch (e: Exception) {
      Log.e(TAG, "Error stopping location updates: ${e.message}")
    }
    _visualAlertState.value = _visualAlertState.value.copy(isServiceActive = false)
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        "Cảnh Báo Tốc Độ & Giới Hạn Giao Thông",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Theo dõi GPS tốc độ và phát cảnh báo trực quan khi vượt quá giới hạn"
        setShowBadge(true)
      }
      val notificationManager = getSystemService(NotificationManager::class.java)
      notificationManager?.createNotificationChannel(channel)
    }
  }

  private fun buildNotification(state: VisualSpeedAlertState): Notification {
    val openAppIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
      },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val stopServiceIntent = PendingIntent.getService(
      this,
      1,
      Intent(this, SpeedLimitTrackingService::class.java).apply {
        action = ACTION_STOP
      },
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val title = if (state.isOverspeeding) {
      "🚨 VƯỢT TỐC ĐỘ: ${state.currentSpeedKmh} / ${state.speedLimitKmh} km/h (+${state.speedDeltaKmh})"
    } else {
      "🚗 GPS Speed Alert: ${state.currentSpeedKmh} km/h (Giới hạn: ${state.speedLimitKmh} km/h)"
    }

    val contentText = "${state.roadName} • ${state.alertMessage}"
    val priority = if (state.isOverspeeding) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW

    return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
      .setContentTitle(title)
      .setContentText(contentText)
      .setSmallIcon(android.R.drawable.ic_menu_compass)
      .setContentIntent(openAppIntent)
      .setOngoing(true)
      .setPriority(priority)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .addAction(android.R.drawable.ic_menu_mapmode, "🗺️ Mở Bản Đồ", openAppIntent)
      .addAction(android.R.drawable.ic_menu_close_clear_cancel, "⏹️ Dừng", stopServiceIntent)
      .build()
  }

  private fun updateNotification(state: VisualSpeedAlertState) {
    try {
      val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      notificationManager?.notify(NOTIFICATION_ID, buildNotification(state))
      // Synchronize Lockscreen / Homescreen Widget
      SpeedAlertWidgetProvider.updateAllWidgets(applicationContext, state)
    } catch (_: Exception) {}
  }

  override fun onDestroy() {
    stopTracking()
    serviceScope.cancel()
    super.onDestroy()
  }
}

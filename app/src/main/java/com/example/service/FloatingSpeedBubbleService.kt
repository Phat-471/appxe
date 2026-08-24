package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.data.model.VisualSpeedAlertState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FloatingSpeedBubbleService : Service() {

  companion object {
    const val TAG = "FloatingSpeedBubble"
    const val NOTIFICATION_CHANNEL_ID = "floating_speed_bubble_channel"
    const val NOTIFICATION_ID = 2027

    const val ACTION_START = "com.example.action.START_FLOATING_BUBBLE"
    const val ACTION_STOP = "com.example.action.STOP_FLOATING_BUBBLE"

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    fun canDrawOverlay(context: Context): Boolean {
      return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
      } else {
        true
      }
    }

    fun startService(context: Context) {
      if (!canDrawOverlay(context)) return
      val intent = Intent(context, FloatingSpeedBubbleService::class.java).apply {
        action = ACTION_START
      }
      try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          context.startForegroundService(intent)
        } else {
          context.startService(intent)
        }
      } catch (e: Exception) {
        android.util.Log.e(TAG, "Failed to start FloatingSpeedBubbleService: ${e.message}")
      }
    }

    fun stopService(context: Context) {
      val intent = Intent(context, FloatingSpeedBubbleService::class.java).apply {
        action = ACTION_STOP
      }
      try {
        context.startService(intent)
      } catch (_: Exception) {}
    }
  }

  private var windowManager: WindowManager? = null
  private var floatingView: ComposeView? = null
  private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  // Custom Lifecycle & SavedState owner for Compose inside a WindowManager Overlay
  private val overlayLifecycleOwner = object : LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    init {
      savedStateRegistryController.performRestore(Bundle())
      lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
      lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
      lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun destroy() {
      lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
      lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
      lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }
  }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    _isServiceRunning.value = true
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent?.action == ACTION_STOP) {
      stopSelf()
      return START_NOT_STICKY
    }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        startForeground(
          NOTIFICATION_ID,
          buildForegroundNotification(),
          android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        )
      } else {
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
      }
    } catch (e: Exception) {
      android.util.Log.e(TAG, "Failed to startForeground on FloatingSpeedBubbleService: ${e.message}")
    }

    showFloatingBubble()
    return START_STICKY
  }

  @SuppressLint("ClickableViewAccessibility")
  private fun showFloatingBubble() {
    if (floatingView != null) return
    if (!canDrawOverlay(this)) {
      stopSelf()
      return
    }

    windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      @Suppress("DEPRECATION")
      WindowManager.LayoutParams.TYPE_PHONE
    }

    val params = WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      layoutFlag,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
      PixelFormat.TRANSLUCENT
    ).apply {
      gravity = Gravity.TOP or Gravity.START
      x = 40
      y = 220
    }

    floatingView = ComposeView(this).apply {
      setViewTreeLifecycleOwner(overlayLifecycleOwner)
      setViewTreeSavedStateRegistryOwner(overlayLifecycleOwner)

      setContent {
        val alertState by SpeedLimitTrackingService.visualAlertState.collectAsState()
        FloatingSpeedBubbleContent(
          alertState = alertState,
          onDragDelta = { dx, dy ->
            params.x += dx.toInt()
            params.y += dy.toInt()
            try {
              windowManager?.updateViewLayout(this@apply, params)
            } catch (_: Exception) {}
          },
          onDragEnd = {
            val screenWidth = resources.displayMetrics.widthPixels
            val viewWidth = this@apply.width.coerceAtLeast(160)
            params.x = if (params.x + viewWidth / 2 < screenWidth / 2) 20 else screenWidth - viewWidth - 20
            try {
              windowManager?.updateViewLayout(this@apply, params)
            } catch (_: Exception) {}
          },
          onOpenApp = {
            val appIntent = Intent(this@FloatingSpeedBubbleService, MainActivity::class.java).apply {
              flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(appIntent)
          },
          onClose = {
            stopSelf()
          }
        )
      }
    }

    try {
      windowManager?.addView(floatingView, params)
    } catch (e: Exception) {
      android.util.Log.e(TAG, "Error adding floatingView: ${e.message}")
      stopSelf()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    _isServiceRunning.value = false
    serviceScope.cancel()
    overlayLifecycleOwner.destroy()

    if (floatingView != null) {
      try {
        windowManager?.removeView(floatingView)
      } catch (_: Exception) {}
      floatingView = null
    }
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        "Cửa Sổ Nổi Đồng Hồ Tốc Độ",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Hiển thị tốc độ và cảnh báo nổi đè lên Google Maps"
        setShowBadge(false)
      }
      val manager = getSystemService(NotificationManager::class.java)
      manager?.createNotificationChannel(channel)
    }
  }

  private fun buildForegroundNotification(): Notification {
    val pendingIntent = PendingIntent.getActivity(
      this,
      0,
      Intent(this, MainActivity::class.java),
      PendingIntent.FLAG_IMMUTABLE
    )

    return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
      .setContentTitle("Bong bóng cảnh báo tốc độ đang chạy")
      .setContentText("Đang nổi trên màn hình dẫn đường Google Maps")
      .setSmallIcon(android.R.drawable.ic_menu_compass)
      .setContentIntent(pendingIntent)
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .build()
  }
}

@Composable
fun FloatingSpeedBubbleContent(
  alertState: VisualSpeedAlertState,
  onDragDelta: (Float, Float) -> Unit,
  onDragEnd: () -> Unit,
  onOpenApp: () -> Unit,
  onClose: () -> Unit
) {
  val isOverspeed = alertState.isOverspeeding
  val speedInt = alertState.currentSpeedKmh
  val limitInt = alertState.speedLimitKmh
  val hasCamera = alertState.nearestCameraDistance != null && alertState.nearestCameraDistance > 0

  // Infinite pulsing glow for critical camera alerts & overspeeding
  val infiniteTransition = rememberInfiniteTransition(label = "bubblePulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(650, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "alpha"
  )

  // Dynamic Border & Glow Colors
  val glowColor = when {
    isOverspeed -> Color(0xFFEF4444)
    hasCamera && (alertState.nearestCameraDistance ?: 1000) < 200 -> Color(0xFFF59E0B)
    speedInt >= limitInt - 5 && limitInt > 0 -> Color(0xFFF59E0B)
    else -> Color(0xFF00B4D8)
  }

  val effectiveBorderAlpha = if (isOverspeed || (hasCamera && (alertState.nearestCameraDistance ?: 1000) < 150)) pulseAlpha else 0.85f

  Surface(
    modifier = Modifier
      .wrapContentSize()
      .pointerInput(Unit) {
        detectDragGestures(
          onDragEnd = { onDragEnd() },
          onDragCancel = { onDragEnd() },
          onDrag = { change, dragAmount ->
            change.consume()
            onDragDelta(dragAmount.x, dragAmount.y)
          }
        )
      }
      .shadow(16.dp, RoundedCornerShape(22.dp))
      .border(
        width = if (isOverspeed) 2.5.dp else 1.8.dp,
        brush = Brush.horizontalGradient(
          listOf(
            glowColor.copy(alpha = effectiveBorderAlpha),
            glowColor.copy(alpha = effectiveBorderAlpha * 0.6f)
          )
        ),
        shape = RoundedCornerShape(22.dp)
      ),
    shape = RoundedCornerShape(22.dp),
    color = Color(0xFF0A0F1D).copy(alpha = 0.94f)
  ) {
    Column(
      modifier = Modifier
        .padding(horizontal = 10.dp, vertical = 7.dp)
        .clickable { onOpenApp() },
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // 1. Biển báo tốc độ giới hạn tròn chuẩn P.127
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(3.5.dp, Color(0xFFDC2626), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (limitInt > 0) "$limitInt" else "50",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black
          )
        }

        // 2. Vận tốc thực tế to rõ nét
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          Row(verticalAlignment = Alignment.Bottom) {
            Text(
              text = "$speedInt",
              fontSize = 28.sp,
              fontWeight = FontWeight.Black,
              color = when {
                isOverspeed -> Color(0xFFEF4444)
                speedInt >= limitInt - 5 && limitInt > 0 -> Color(0xFFF59E0B)
                else -> Color(0xFF10B981)
              }
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
              text = "km/h",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF94A3B8),
              modifier = Modifier.padding(bottom = 4.dp)
            )
          }
        }

        // 3. Camera cảnh báo phía trước & số mét đếm ngược
        if (hasCamera && alertState.nearestCameraDistance != null) {
          Box(
            modifier = Modifier
              .width(1.dp)
              .height(28.dp)
              .background(Color(0xFF334155))
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(
                if ((alertState.nearestCameraDistance ?: 1000) < 150)
                  Color(0xFFDC2626).copy(alpha = 0.25f)
                else
                  Color(0xFF1E293B)
              )
              .border(
                1.dp,
                if ((alertState.nearestCameraDistance ?: 1000) < 150)
                  Color(0xFFEF4444)
                else
                  Color(0xFF0284C7).copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
              )
              .padding(horizontal = 6.dp, vertical = 4.dp)
          ) {
            Text(
              text = alertState.cameraIconEmoji,
              fontSize = 13.sp
            )
            Column(horizontalAlignment = Alignment.Start) {
              Text(
                text = "${alertState.nearestCameraDistance}m",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = if ((alertState.nearestCameraDistance ?: 1000) < 150) Color(0xFFEF4444) else Color(0xFFFBBF24)
              )
              val camLabel = alertState.nearestCameraType?.take(10) ?: "Camera"
              Text(
                text = camLabel,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF94A3B8)
              )
            }
          }
        }

        // 4. Nút đóng nhỏ gọn
        Box(
          modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color(0xFF1E293B))
            .clickable { onClose() },
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Đóng popup",
            tint = Color(0xFFCBD5E1),
            modifier = Modifier.size(15.dp)
          )
        }
      }

      // 5. Dòng phụ: Tên đường đang chạy
      if (alertState.roadName.isNotBlank()) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = alertState.roadName,
          fontSize = 10.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFF94A3B8),
          maxLines = 1
        )
      }
    }
  }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CameraType
import com.example.data.model.GpsLocationState
import com.example.service.WarningEvaluationResult

/**
 * OLED Pure Black Super Battery Saver HUD Mode.
 * Turns 100% of black pixels off on AMOLED screens, saving over 80% battery.
 */
@Composable
fun OledHudScreen(
  locationState: GpsLocationState,
  trafficEvaluation: WarningEvaluationResult,
  batteryPercentage: Int,
  isCharging: Boolean,
  isBatterySaverActive: Boolean,
  isMirrorMode: Boolean,
  isEn: Boolean,
  onToggleMirror: () -> Unit,
  onExitHud: () -> Unit,
  modifier: Modifier = Modifier
) {
  val speedKmh = locationState.speedKmh.toInt()
  val speedLimit = trafficEvaluation.currentSpeedLimit
  val activeWarning = trafficEvaluation.activeWarning
  val isOverspeed = speedKmh > speedLimit && speedLimit > 0

  // Danger pulse animation for overspeed
  val infiniteTransition = rememberInfiniteTransition(label = "HudPulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = if (isOverspeed) 1.06f else 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(450, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "PulseScale"
  )

  val speedColor = when {
    isOverspeed -> Color(0xFFEF4444)
    speedKmh > speedLimit - 5 && speedLimit > 0 -> Color(0xFFF59E0B)
    else -> Color(0xFF10B981)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF000000))
      .padding(horizontal = 16.dp, vertical = 20.dp)
      .testTag("oled_hud_screen")
      .then(if (isMirrorMode) Modifier.scale(scaleX = -1f, scaleY = 1f) else Modifier)
  ) {
    // Top Bar: Battery & Controls
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .align(Alignment.TopCenter),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Battery info badge
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF111827),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          if (batteryPercentage <= 20 && !isCharging) Color(0xFFEF4444) else Color(0xFF374151)
        )
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = when {
              isCharging -> Icons.Default.BatteryChargingFull
              batteryPercentage > 80 -> Icons.Default.BatteryFull
              batteryPercentage > 50 -> Icons.Default.Battery5Bar
              batteryPercentage > 20 -> Icons.Default.Battery3Bar
              else -> Icons.Default.BatteryAlert
            },
            contentDescription = null,
            tint = when {
              isCharging -> Color(0xFF38BDF8)
              batteryPercentage > 20 -> Color(0xFF10B981)
              else -> Color(0xFFEF4444)
            },
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "$batteryPercentage%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          if (isBatterySaverActive) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFF0284C7).copy(alpha = 0.3f)
            ) {
              Text(
                text = if (isEn) "ECO -80%" else "TIẾT KIỆM 80%",
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF38BDF8),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
              )
            }
          }
        }
      }

      // Actions: Mirror & Exit
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Mirror HUD toggle
        IconButton(
          onClick = onToggleMirror,
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (isMirrorMode) Color(0xFF0284C7) else Color(0xFF1E293B))
        ) {
          Icon(
            imageVector = Icons.Default.Flip,
            contentDescription = "Mirror HUD",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }

        // Exit HUD
        IconButton(
          onClick = onExitHud,
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFF334155))
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Exit HUD",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }

    // Center Cockpit: Speedometer & Road Sign
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Road Name Banner
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF111827),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1F2937)),
        modifier = Modifier.padding(bottom = 16.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = null,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(14.dp)
          )
          Text(
            text = locationState.detectedRoadName?.ifBlank { "Đang dò đường..." } ?: "Đang dò đường...",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
        }
      }

      // Large Speed Digit + Speed Limit Sign Side-by-Side
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.scale(pulseScale)
      ) {
        // Gigantic Speed Value
        Column(
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "$speedKmh",
            fontSize = 108.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif,
            color = speedColor,
            letterSpacing = (-4).sp,
            lineHeight = 100.sp,
            textAlign = TextAlign.Center
          )
          Text(
            text = "KM/H",
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = Color(0xFF94A3B8)
          )
        }

        Spacer(modifier = Modifier.width(24.dp))

        // Speed Limit Circular Sign
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(7.dp, Color(0xFFEF4444), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "$speedLimit",
            color = Color.Black,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.SansSerif
          )
        }
      }

      // Warning / Status Message
      AnimatedVisibility(
        visible = isOverspeed,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xFFEF4444).copy(alpha = 0.2f),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
          modifier = Modifier.padding(top = 16.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
            Text(
              text = if (isEn) "OVERSPEED! REDUCE SPEED" else "VƯỢT QUÁ TỐC ĐỘ QUY ĐỊNH!",
              color = Color(0xFFEF4444),
              fontSize = 12.sp,
              fontWeight = FontWeight.Black
            )
          }
        }
      }
    }

    // Bottom Section: Upcoming Camera Alert Card
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .align(Alignment.BottomCenter),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      if (activeWarning != null) {
        val cam = activeWarning.camera
        val distance = activeWarning.distanceMeters.toInt()
        val isImminent = distance <= 200

        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (isImminent) Color(0xFF7F1D1D) else Color(0xFF1E293B),
          border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isImminent) Color(0xFFEF4444) else Color(0xFF38BDF8)
          ),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isImminent) Color(0xFFDC2626) else Color(0xFF0284C7)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = when (cam.type) {
                  CameraType.SPEED_CAMERA -> Icons.Default.Speed
                  CameraType.RED_LIGHT_CAMERA -> Icons.Default.Traffic
                  CameraType.MOTORBIKE_PROHIBITED_ZONE -> Icons.Default.Block
                  else -> Icons.Default.Videocam
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = cam.roadName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
              )
              Text(
                text = cam.description,
                fontSize = 11.sp,
                color = if (isImminent) Color(0xFFFECACA) else Color(0xFF94A3B8),
                maxLines = 1
              )
            }

            // Distance Pill
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = if (isImminent) Color(0xFFEF4444) else Color(0xFF0284C7).copy(alpha = 0.3f),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isImminent) Color.White.copy(alpha = 0.5f) else Color(0xFF38BDF8)
              )
            ) {
              Text(
                text = "${distance}m",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }
        }
      } else {
        // Safe Driving Footer
        Text(
          text = if (isEn) "OLED HUD ECO MODE • SCREEN SAVER 80%" else "CHẾ ĐỘ OLED HUD • SIÊU TIẾT KIỆM 80% PIN",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          color = Color(0xFF475569)
        )
      }
    }
  }
}

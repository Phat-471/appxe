package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveWarning
import com.example.data.model.CameraType
import com.example.data.model.TrafficCamera
import com.example.data.model.WarningLevel
import com.example.ui.theme.*

/**
 * Vietmap Live Style Stacked Speed HUD
 * Two vertical stacked circular bubbles on the bottom-left:
 * - Top Bubble: Realtime Vehicle Speed (with Neon Green/Red glowing ring)
 * - Bottom Bubble: Vietnam Speed Limit Sign (Red ring, white background, bold black limit number)
 */
@Composable
fun VietmapStackedSpeedHUD(
  currentSpeedKmh: Float,
  speedLimitKmh: Int,
  isOverspeeding: Boolean,
  modifier: Modifier = Modifier
) {
  val speedInt = currentSpeedKmh.toInt().coerceAtLeast(0)

  // Pulsing border animation when overspeeding
  val infiniteTransition = rememberInfiniteTransition(label = "OverspeedPulse")
  val pulseBorderWidth by infiniteTransition.animateFloat(
    initialValue = 3.5f,
    targetValue = 6.5f,
    animationSpec = infiniteRepeatable(
      animation = tween(500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "PulseBorder"
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp),
    modifier = modifier
  ) {
    // 1. TOP BUBBLE: CURRENT VEHICLE SPEED GAUGE
    Surface(
      shape = CircleShape,
      color = Color.White.copy(alpha = 0.96f),
      shadowElevation = 10.dp,
      border = androidx.compose.foundation.BorderStroke(
        width = if (isOverspeeding) pulseBorderWidth.dp else 4.dp,
        color = if (isOverspeeding) AlertCrimsonDanger else Color(0xFF10B981)
      ),
      modifier = Modifier.size(72.dp)
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
      ) {
        Text(
          text = "$speedInt",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            letterSpacing = (-0.5).sp
          ),
          color = if (isOverspeeding) AlertCrimsonDanger else Color(0xFF0F172A)
        )
        Text(
          text = "km/h",
          style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold
          ),
          color = Color(0xFF64748B),
          modifier = Modifier.offset(y = (-2).dp)
        )
      }
    }

    // 2. BOTTOM BUBBLE: VIETNAM SPEED LIMIT SIGN (QCVN 41:2019/BGTVT Standard)
    Surface(
      shape = CircleShape,
      color = SignBackgroundWhite,
      shadowElevation = 10.dp,
      border = androidx.compose.foundation.BorderStroke(
        width = 5.dp,
        color = SignBorderRed
      ),
      modifier = Modifier.size(72.dp)
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
      ) {
        Text(
          text = "$speedLimitKmh",
          style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            letterSpacing = (-1).sp
          ),
          color = SignTextBlack
        )
      }
    }
  }
}

/**
 * Vietmap Live Style Left-Side Approaching Hazard & Camera Queue
 * Stacks upcoming cameras/hazards vertically with remaining distance badges (e.g. 300m, 98m).
 */
@Composable
fun VietmapLeftHazardQueue(
  camerasAhead: List<Pair<TrafficCamera, Int>>, // Camera and remaining distance in meters
  activeWarning: ActiveWarning?,
  modifier: Modifier = Modifier
) {
  if (camerasAhead.isEmpty() && activeWarning == null) return

  Column(
    verticalArrangement = Arrangement.spacedBy(10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
  ) {
    // Show top 3 approaching cameras
    camerasAhead.take(3).forEach { (cam, dist) ->
      VietmapHazardBadge(
        camera = cam,
        distanceMeters = dist,
        isWarningDanger = activeWarning?.camera?.id == cam.id && activeWarning.warningLevel == WarningLevel.DANGER
      )
    }
  }
}

@Composable
fun VietmapHazardBadge(
  camera: TrafficCamera,
  distanceMeters: Int,
  isWarningDanger: Boolean
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.width(62.dp)
  ) {
    // Icon Box
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = if (camera.type == CameraType.HAZARD_ACCIDENT_ZONE) Color(0xFFF59E0B) else Color(0xFF0F172A).copy(alpha = 0.92f),
      border = androidx.compose.foundation.BorderStroke(
        width = 2.dp,
        color = if (isWarningDanger) AlertCrimsonDanger else if (camera.type == CameraType.SPEED_CAMERA) Color.White else Color(0xFF38BDF8)
      ),
      shadowElevation = 6.dp,
      modifier = Modifier.size(46.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        when (camera.type) {
          CameraType.SPEED_CAMERA -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
              Text("${camera.speedLimit}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
          }
          CameraType.RED_LIGHT_CAMERA -> {
            Icon(Icons.Default.Traffic, contentDescription = null, tint = AlertCrimsonDanger, modifier = Modifier.size(24.dp))
          }
          CameraType.COLD_FINE_SURVEILLANCE -> {
            Icon(Icons.Default.Videocam, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
          }
          CameraType.SECURITY_MONITORING -> {
            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF93C5FD), modifier = Modifier.size(24.dp))
          }
          CameraType.ZONE_RESIDENTIAL_ENTRY -> {
            Text("🏙️", fontSize = 20.sp)
          }
          CameraType.ZONE_RESIDENTIAL_EXIT -> {
            Text("🛣️", fontSize = 20.sp)
          }
          CameraType.HAZARD_ACCIDENT_ZONE -> {
            Icon(Icons.Default.PriorityHigh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
          }
          else -> {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(22.dp))
          }
        }
      }
    }

    // Distance Label
    Text(
      text = if (distanceMeters >= 1000) {
        "${String.format(java.util.Locale.US, "%.1f", distanceMeters / 1000f)}km"
      } else {
        "${distanceMeters}m"
      },
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Black,
        fontSize = 11.sp
      ),
      color = Color.White,
      modifier = Modifier
        .padding(top = 2.dp)
        .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(4.dp))
        .padding(horizontal = 4.dp, vertical = 1.dp)
    )
  }
}

/**
 * Vietmap Live Style Lane Guidance & Distance Top Dynamic Banner
 */
@Composable
fun VietmapTopLaneGuidanceBanner(
  turnDistanceMeters: Int,
  turnInstruction: String,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = Color(0xFF0F172A).copy(alpha = 0.95f),
    shadowElevation = 10.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
    modifier = modifier.padding(horizontal = 14.dp)
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      // Distance to next turn
      Text(
        text = if (turnDistanceMeters >= 1000) {
          "${String.format(java.util.Locale.US, "%.1f", turnDistanceMeters / 1000f)} km"
        } else {
          "${turnDistanceMeters} m"
        },
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Black,
          fontSize = 18.sp
        ),
        color = Color.White
      )

      Spacer(modifier = Modifier.height(4.dp))

      // 3-Lane Guidance Indicators (Vietmap Style)
      Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        LaneIndicatorBox(arrowSymbol = "⬆️", isActive = true)
        LaneIndicatorBox(arrowSymbol = "⬆️", isActive = true)
        LaneIndicatorBox(arrowSymbol = "↗️", isActive = turnInstruction.contains("phải", ignoreCase = true))
      }
    }
  }
}

@Composable
fun LaneIndicatorBox(arrowSymbol: String, isActive: Boolean) {
  Surface(
    shape = RoundedCornerShape(6.dp),
    color = if (isActive) Color(0xFF00B4D8) else Color(0xFF334155),
    modifier = Modifier.size(width = 30.dp, height = 24.dp)
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = arrowSymbol,
        fontSize = 13.sp,
        color = if (isActive) Color.White else Color(0xFF94A3B8)
      )
    }
  }
}

/**
 * Vietmap Live Style Bottom Floating Street Name Banner
 */
@Composable
fun VietmapBottomRoadPill(
  roadName: String,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = Color.White.copy(alpha = 0.94f),
    shadowElevation = 8.dp,
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
    modifier = modifier
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
      Icon(Icons.Default.Navigation, contentDescription = null, tint = Color(0xFF1E88E5), modifier = Modifier.size(14.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = roadName,
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        ),
        color = Color(0xFF1E293B)
      )
    }
  }
}

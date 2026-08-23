package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedometerGauge(
  currentSpeed: Float,
  speedLimit: Int,
  isOverspeeding: Boolean,
  hudMirrorMode: Boolean = false,
  modifier: Modifier = Modifier
) {
  val animatedSpeed by animateFloatAsState(
    targetValue = currentSpeed,
    animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "speedAnimation"
  )

  // Determine dynamic color state
  val speedRatio = if (speedLimit > 0) currentSpeed / speedLimit else 0f
  val gaugeColor by animateColorAsState(
    targetValue = when {
      isOverspeeding -> AlertCrimsonDanger
      speedRatio >= 0.85f -> AlertAmberPrimary
      else -> AlertEmeraldSafe
    },
    label = "gaugeColor"
  )

  // Pulsing animation when overspeeding
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = if (isOverspeeding) 1.06f else 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  val mirrorScale = if (hudMirrorMode) -1f else 1f

  Box(
    modifier = modifier
      .scale(scaleX = mirrorScale, scaleY = 1f)
      .size(270.dp)
      .testTag("speedometer_gauge"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
      val center = Offset(size.width / 2f, size.height / 2f)
      val radius = (size.width - 24.dp.toPx()) / 2f

      val startAngle = 140f
      val sweepAngle = 260f
      val maxGaugeSpeed = 120f

      // Outer background arc track
      drawArc(
        color = GaugeTrackDark,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2f, radius * 2f),
        style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
      )

      // Active progress arc
      val activeSweep = ((animatedSpeed / maxGaugeSpeed).coerceIn(0f, 1f)) * sweepAngle
      if (activeSweep > 0.5f) {
        val arcBrush = Brush.sweepGradient(
          0.0f to AlertEmeraldSafe,
          0.6f to AlertAmberPrimary,
          1.0f to AlertCrimsonDanger,
          center = center
        )
        drawArc(
          brush = arcBrush,
          startAngle = startAngle,
          sweepAngle = activeSweep,
          useCenter = false,
          topLeft = Offset(center.x - radius, center.y - radius),
          size = Size(radius * 2f, radius * 2f),
          style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
        )
      }

      // Ticks & Speed Marker notches
      val totalTicks = 12
      for (i in 0..totalTicks) {
        val tickFraction = i.toFloat() / totalTicks
        val tickAngle = startAngle + tickFraction * sweepAngle
        val angleRad = Math.toRadians(tickAngle.toDouble())

        val innerRadius = radius - 16.dp.toPx()
        val outerRadius = radius - 6.dp.toPx()

        val startX = center.x + innerRadius * cos(angleRad).toFloat()
        val startY = center.y + innerRadius * sin(angleRad).toFloat()
        val endX = center.x + outerRadius * cos(angleRad).toFloat()
        val endY = center.y + outerRadius * sin(angleRad).toFloat()

        val isMajor = i % 2 == 0
        drawCircle(
          color = if (isMajor) CockpitTextSecondary else GaugeTickColor,
          radius = if (isMajor) 2.5.dp.toPx() else 1.5.dp.toPx(),
          center = Offset(startX, startY)
        )
      }

      // Speed limit threshold indicator line on gauge
      val limitFraction = (speedLimit / maxGaugeSpeed).coerceIn(0f, 1f)
      val limitAngle = startAngle + limitFraction * sweepAngle
      val limitRad = Math.toRadians(limitAngle.toDouble())
      val limitOuter = radius + 12.dp.toPx()
      val limitInner = radius - 12.dp.toPx()

      drawLine(
        color = AlertCrimsonDanger,
        start = Offset(
          center.x + limitInner * cos(limitRad).toFloat(),
          center.y + limitInner * sin(limitRad).toFloat()
        ),
        end = Offset(
          center.x + limitOuter * cos(limitRad).toFloat(),
          center.y + limitOuter * sin(limitRad).toFloat()
        ),
        strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round
      )
    }

    // Center Readout
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .scale(if (isOverspeeding) pulseScale else 1.0f)
    ) {
      // Speed Limit Badge (Vietnamese Traffic Sign Standard)
      SpeedLimitBadge(speedLimit = speedLimit)

      Spacer(modifier = Modifier.height(4.dp))

      // Big Digital Speed Number
      Text(
        text = animatedSpeed.toInt().toString(),
        style = MaterialTheme.typography.displayLarge.copy(
          fontSize = 62.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = (-2).sp
        ),
        color = gaugeColor,
        modifier = Modifier.testTag("current_speed_text")
      )

      Text(
        text = "km/h",
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        ),
        color = CockpitTextSecondary
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Status Tag
      Box(
        modifier = Modifier
          .clip(CircleShape)
          .background(
            if (isOverspeeding) AlertCrimsonDanger.copy(alpha = 0.25f)
            else AlertEmeraldSafe.copy(alpha = 0.15f)
          )
          .border(
            width = 1.dp,
            color = if (isOverspeeding) AlertCrimsonDanger else AlertEmeraldSafe.copy(alpha = 0.6f),
            shape = CircleShape
          )
          .padding(horizontal = 10.dp, vertical = 2.dp)
      ) {
        Text(
          text = if (isOverspeeding) "VƯỢT TỐC ĐỘ!" else "TỐC ĐỘ AN TOÀN",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
          ),
          color = if (isOverspeeding) AlertCrimsonDanger else AlertEmeraldSafe
        )
      }
    }
  }
}

@Composable
fun SpeedLimitBadge(speedLimit: Int, modifier: Modifier = Modifier) {
  Box(
    modifier = modifier
      .size(44.dp)
      .clip(CircleShape)
      .background(SignBackgroundWhite)
      .border(4.dp, SignBorderRed, CircleShape)
      .testTag("speed_limit_badge"),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = speedLimit.toString(),
      color = SignTextBlack,
      fontSize = 18.sp,
      fontWeight = FontWeight.Black,
      letterSpacing = (-0.5).sp
    )
  }
}

@Composable
fun SpeedComparisonCard(
  currentSpeed: Float,
  speedLimit: Int,
  roadName: String,
  isOverspeeding: Boolean,
  modifier: Modifier = Modifier
) {
  val speedInt = currentSpeed.toInt()
  val delta = speedInt - speedLimit
  val infiniteTransition = rememberInfiniteTransition(label = "warning_pulse")
  val borderAlpha by infiniteTransition.animateFloat(
    initialValue = 0.5f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(450, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "border_alpha"
  )

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = if (isOverspeeding) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
    shadowElevation = 4.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(
        width = if (isOverspeeding) 2.dp else 1.dp,
        color = if (isOverspeeding) AlertCrimsonDanger.copy(alpha = borderAlpha) else AlertEmeraldSafe.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
      )
      .testTag("speed_comparison_card")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Header: Road Name & Location Tag
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = "📍 $roadName",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = Color(0xFF0F172A),
            maxLines = 1
          )
        }
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = if (isOverspeeding) AlertCrimsonDanger else AlertEmeraldSafe,
          modifier = Modifier.padding(start = 6.dp)
        ) {
          Text(
            text = if (isOverspeeding) "VƯỢT +$delta km/h" else "AN TOÀN",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Side-by-side comparison row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Left Column: Nhà nước cho phép
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = "NHÀ NƯỚC CHO PHÉP",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF64748B)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(3.5.dp, SignBorderRed, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "$speedLimit",
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "km/h",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = Color(0xFF475569)
            )
          }
        }

        // Middle Divider / VS symbol
        Text(
          text = if (isOverspeeding) " > " else " ≤ ",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Black,
            fontSize = 24.sp
          ),
          color = if (isOverspeeding) AlertCrimsonDanger else AlertEmeraldSafe
        )

        // Right Column: Tốc độ của bạn
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = "TỐC ĐỘ CỦA BẠN",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF64748B)
          )
          Spacer(modifier = Modifier.height(4.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "$speedInt",
              style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                fontSize = 26.sp
              ),
              color = if (isOverspeeding) AlertCrimsonDanger else AlertEmeraldDark
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "km/h",
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = Color(0xFF475569)
            )
          }
        }
      }

      // Bottom warning banner if overspeeding
      if (isOverspeeding) {
        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = AlertCrimsonDanger,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text("⚠️", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Cảnh báo: Bạn đang chạy vượt quá tốc độ cho phép $delta km/h!",
              style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
              )
            )
          }
        }
      }
    }
  }
}

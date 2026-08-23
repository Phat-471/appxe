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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.datasource.MockSpeedLimitDataSource
import com.example.data.model.VisualAlertLevel
import com.example.data.model.VisualSpeedAlertState
import com.example.ui.theme.*
import kotlin.math.abs

@Composable
fun VisualSpeedAlertHUD(
  alertState: VisualSpeedAlertState,
  onSelectMockZone: (name: String, limit: Int) -> Unit = { _, _ -> },
  onSetCustomSpeed: (Float) -> Unit = {},
  modifier: Modifier = Modifier
) {
  // Infinite pulsing animation for warning / danger states
  val infiniteTransition = rememberInfiniteTransition(label = "AlertPulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1.0f,
    targetValue = if (alertState.isOverspeeding) 1.08f else 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (alertState.alertLevel == VisualAlertLevel.CRITICAL) 400 else 700,
        easing = FastOutSlowInEasing
      ),
      repeatMode = RepeatMode.Reverse
    ),
    label = "PulseScale"
  )

  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = if (alertState.isOverspeeding) 1.0f else 0.8f,
    animationSpec = infiniteRepeatable(
      animation = tween(
        durationMillis = if (alertState.alertLevel == VisualAlertLevel.CRITICAL) 350 else 600,
        easing = LinearEasing
      ),
      repeatMode = RepeatMode.Reverse
    ),
    label = "PulseAlpha"
  )

  // Color mapping
  val bannerBgGradient = when (alertState.alertLevel) {
    VisualAlertLevel.CRITICAL -> Brush.horizontalGradient(
      colors = listOf(Color(0xFF991B1B), Color(0xFFDC2626), Color(0xFFB91C1C))
    )
    VisualAlertLevel.DANGER -> Brush.horizontalGradient(
      colors = listOf(Color(0xFFB91C1C), Color(0xFFEF4444))
    )
    VisualAlertLevel.WARNING -> Brush.horizontalGradient(
      colors = listOf(Color(0xFFC2410C), Color(0xFFF97316))
    )
    VisualAlertLevel.APPROACHING_LIMIT -> Brush.horizontalGradient(
      colors = listOf(Color(0xFFD97706), Color(0xFFF59E0B))
    )
    VisualAlertLevel.SAFE -> Brush.horizontalGradient(
      colors = listOf(Color(0xFF065F46), Color(0xFF10B981))
    )
    VisualAlertLevel.NORMAL -> Brush.horizontalGradient(
      colors = listOf(Color(0xFF0369A1), Color(0xFF0284C7))
    )
  }

  var showZoneDialog by remember { mutableStateOf(false) }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    modifier = modifier
      .fillMaxWidth()
      .testTag("visual_speed_alert_hud")
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // 1. Dynamic Alert Banner Header
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .background(bannerBgGradient)
          .padding(horizontal = 16.dp, vertical = 10.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Icon(
              imageVector = if (alertState.isOverspeeding) Icons.Default.Warning else Icons.Default.GpsFixed,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier
                .size(22.dp)
                .scale(if (alertState.isOverspeeding) pulseScale else 1.0f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = alertState.alertMessage,
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                  fontSize = 13.5.sp
                )
              )
              Text(
                text = "${alertState.roadName} • Mock Data Source",
                style = MaterialTheme.typography.labelSmall.copy(
                  color = Color.White.copy(alpha = 0.9f),
                  fontSize = 11.sp
                )
              )
            }
          }

          // Button to change mock speed zone
          Surface(
            onClick = { showZoneDialog = true },
            color = Color.White.copy(alpha = 0.25f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(start = 6.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Đổi giới hạn", style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.5.sp))
            }
          }
        }
      }

      // 2. Main Speed Comparison Display
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Left: Current Speed Dial
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(68.dp)
              .clip(CircleShape)
              .background(
                if (alertState.isOverspeeding) Color(0xFFFEE2E2) else Color(0xFFE0F2FE)
              )
              .border(
                width = 3.dp,
                color = if (alertState.isOverspeeding) Color(0xFFEF4444) else Color(0xFF0284C7),
                shape = CircleShape
              )
              .scale(if (alertState.isOverspeeding) pulseScale else 1.0f)
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "${alertState.currentSpeedKmh}",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.Black,
                  color = if (alertState.isOverspeeding) Color(0xFFDC2626) else Color(0xFF0369A1),
                  fontSize = 26.sp
                )
              )
              Text(
                text = "km/h",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF64748B)
                )
              )
            }
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column {
            Text(
              text = "Tốc độ hiện tại",
              style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
            )
            Spacer(modifier = Modifier.height(2.dp))
            if (alertState.isOverspeeding) {
              Surface(
                color = Color(0xFFDC2626),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = "QUÁ +${alertState.speedDeltaKmh} km/h",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 11.sp
                  ),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            } else {
              Surface(
                color = Color(0xFFECFDF5),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text(
                  text = "Dưới giới hạn (${abs(alertState.speedDeltaKmh)} km/h)",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669),
                    fontSize = 11.sp
                  ),
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
              }
            }
          }
        }

        // Right: Regulatory Speed Limit Sign (Standard Circular Red Border)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(58.dp)
              .clip(CircleShape)
              .background(SignBackgroundWhite)
              .border(4.dp, SignBorderRed, CircleShape)
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "${alertState.speedLimitKmh}",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Black,
                  color = Color.Black,
                  fontSize = 22.sp
                )
              )
              Text(
                text = "km/h",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  color = Color.DarkGray
                )
              )
            }
          }
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = "Giới hạn luật",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.5.sp,
              color = Color(0xFF64748B),
              fontWeight = FontWeight.Bold
            )
          )
        }
      }

      // 3. Visual Progress Bar comparing Speed vs Limit
      val progress = (alertState.currentSpeedKmh.toFloat() / (alertState.speedLimitKmh * 1.5f).coerceAtLeast(1f))
        .coerceIn(0f, 1f)

      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 4.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = "0 km/h",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = Color(0xFF94A3B8))
          )
          Text(
            text = "Giới hạn: ${alertState.speedLimitKmh} km/h",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (alertState.isOverspeeding) Color(0xFFDC2626) else Color(0xFF0284C7)
            )
          )
          Text(
            text = "${(alertState.speedLimitKmh * 1.5f).toInt()} km/h",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = Color(0xFF94A3B8))
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
          color = when {
            alertState.isOverspeeding -> Color(0xFFDC2626)
            alertState.alertLevel == VisualAlertLevel.APPROACHING_LIMIT -> Color(0xFFF59E0B)
            else -> Color(0xFF10B981)
          },
          trackColor = Color(0xFFF1F5F9)
        )
      }

      // 4. Quick Simulation Speed Triggers for Live Testing
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Test nhanh:",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            color = Color(0xFF64748B),
            fontSize = 10.sp
          )
        )

        val safeSpeed = (alertState.speedLimitKmh - 10).coerceAtLeast(20).toFloat()
        val limitSpeed = alertState.speedLimitKmh.toFloat()
        val overSpeedLight = (alertState.speedLimitKmh + 5).toFloat()
        val overSpeedHeavy = (alertState.speedLimitKmh + 18).toFloat()

        SuggestionChip(
          onClick = { onSetCustomSpeed(safeSpeed) },
          label = { Text("${safeSpeed.toInt()} km/h", fontSize = 10.sp) },
          colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = Color(0xFFECFDF5),
            labelColor = Color(0xFF065F46)
          ),
          modifier = Modifier.weight(1f)
        )

        SuggestionChip(
          onClick = { onSetCustomSpeed(limitSpeed) },
          label = { Text("${limitSpeed.toInt()} km/h", fontSize = 10.sp) },
          colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = Color(0xFFEFF6FF),
            labelColor = Color(0xFF1E40AF)
          ),
          modifier = Modifier.weight(1f)
        )

        SuggestionChip(
          onClick = { onSetCustomSpeed(overSpeedLight) },
          label = { Text("+5 km/h ⚠️", fontSize = 10.sp) },
          colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = Color(0xFFFFF7ED),
            labelColor = Color(0xFFC2410C)
          ),
          modifier = Modifier.weight(1f)
        )

        SuggestionChip(
          onClick = { onSetCustomSpeed(overSpeedHeavy) },
          label = { Text("+18 🚨", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
          colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = Color(0xFFFEF2F2),
            labelColor = Color(0xFFDC2626)
          ),
          modifier = Modifier.weight(1f)
        )
      }
    }
  }

  // Dialog to select Mock Speed Limit Zone
  if (showZoneDialog) {
    AlertDialog(
      onDismissRequest = { showZoneDialog = false },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Speed, contentDescription = null, tint = Color(0xFF1A73E8))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Chọn Mock Speed Limit Zone", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "Dữ liệu giới hạn tốc độ mô phỏng theo từng khu vực đường:",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
          )

          MockSpeedLimitDataSource.MOCK_SPEED_ZONES.forEach { zone ->
            Surface(
              onClick = {
                onSelectMockZone(zone.name, zone.speedLimitKmh)
                showZoneDialog = false
              },
              shape = RoundedCornerShape(12.dp),
              color = if (alertState.speedLimitKmh == zone.speedLimitKmh) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
              border = if (alertState.speedLimitKmh == zone.speedLimitKmh) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0284C7)) else null,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = zone.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                  )
                  Text(
                    text = zone.description,
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 11.sp)
                  )
                }

                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SignBackgroundWhite)
                    .border(3.dp, SignBorderRed, CircleShape)
                ) {
                  Text(
                    text = "${zone.speedLimitKmh}",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, color = Color.Black)
                  )
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showZoneDialog = false }) {
          Text("Đóng")
        }
      }
    )
  }
}

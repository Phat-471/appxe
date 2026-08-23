package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrentTripStats
import com.example.data.model.GpsLocationState
import com.example.data.model.VisualSpeedAlertState
import com.example.service.WarningEvaluationResult
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun CockpitDashboardScreen(
  locationState: GpsLocationState,
  trafficEvaluation: WarningEvaluationResult,
  tripStats: CurrentTripStats,
  visualAlertState: VisualSpeedAlertState = VisualSpeedAlertState(),
  isServiceRunning: Boolean = false,
  isRecordingTrip: Boolean,
  voiceEnabled: Boolean,
  hudMirrorMode: Boolean,
  onToggleVoice: () -> Unit,
  onToggleHudMirror: () -> Unit,
  onToggleTripRecording: () -> Unit,
  onToggleGpsOrSimulation: (Boolean) -> Unit,
  onToggleService: () -> Unit = {},
  onSelectMockZone: (name: String, limit: Int) -> Unit = { _, _ -> },
  onSelectSimulationRoute: (Int) -> Unit,
  onSetSimulatedSpeed: (Float) -> Unit,
  onSetCustomRoad: (String) -> Unit = {},
  onOpenReportDialog: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showSimulationControls by remember { mutableStateOf(true) }
  var testSpeedSlider by remember { mutableFloatStateOf(locationState.speedKmh) }
  var showRoadSelectDialog by remember { mutableStateOf(false) }
  var customRoadInput by remember { mutableStateOf("") }

  LaunchedEffect(locationState.speedKmh) {
    testSpeedSlider = locationState.speedKmh
  }

  // 1. FULLSCREEN NIGHT HUD MIRROR MODE (Reflects onto Windshield)
  if (hudMirrorMode) {
    FullscreenMirrorHUDView(
      currentSpeed = locationState.speedKmh.toInt(),
      speedLimit = trafficEvaluation.currentSpeedLimit,
      isOverspeeding = trafficEvaluation.isOverspeeding,
      roadName = trafficEvaluation.currentRoadName,
      nearestCamera = trafficEvaluation.nearestCamera,
      nearestCameraDistance = trafficEvaluation.nearestCameraDistance,
      onExitMirrorMode = onToggleHudMirror,
      modifier = modifier
    )
    return
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(NavLightBackground)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Top Bar Status & Quick Actions
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      // GPS Status Chip
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = NavLightSurface,
        shadowElevation = 2.dp,
        modifier = Modifier
          .border(1.dp, NavLightCardBorder, RoundedCornerShape(20.dp))
          .clickable { onToggleGpsOrSimulation(!locationState.isSimulated) }
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(if (locationState.isGpsActive) AlertEmeraldSafe else AlertAmberPrimary)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (locationState.isSimulated) "Mô phỏng thực tế" else "GPS Vệ tinh",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = NavLightTextPrimary
          )
          Spacer(modifier = Modifier.width(4.dp))
          Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = "Chuyển GPS",
            tint = NavLightTextSecondary,
            modifier = Modifier.size(14.dp)
          )
        }
      }

      // Quick Action Buttons (Voice & HUD Mirror & Service Toggle)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Location Service Toggle Button
        Surface(
          onClick = onToggleService,
          shape = CircleShape,
          color = if (isServiceRunning) Color(0xFFDCFCE7) else NavLightSurface,
          border = androidx.compose.foundation.BorderStroke(1.dp, if (isServiceRunning) AlertEmeraldSafe else NavLightCardBorder),
          modifier = Modifier.size(38.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (isServiceRunning) Icons.Default.LocationOn else Icons.Default.LocationOff,
              contentDescription = "Dịch vụ chạy ngầm",
              tint = if (isServiceRunning) AlertEmeraldDark else NavLightTextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }

        // Voice Mute/Unmute
        IconButton(
          onClick = onToggleVoice,
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (voiceEnabled) Color(0xFFE0EDFF) else NavLightSurface)
            .border(1.dp, if (voiceEnabled) NavRouteBlue else NavLightCardBorder, CircleShape)
            .testTag("toggle_voice_button")
        ) {
          Icon(
            imageVector = if (voiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
            contentDescription = "Bật tắt giọng nói",
            tint = if (voiceEnabled) NavRouteBlue else NavLightTextSecondary,
            modifier = Modifier.size(20.dp)
          )
        }

        // HUD Mirror Mode
        IconButton(
          onClick = onToggleHudMirror,
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (hudMirrorMode) Color(0xFFDCFCE7) else NavLightSurface)
            .border(1.dp, if (hudMirrorMode) AlertEmeraldSafe else NavLightCardBorder, CircleShape)
            .testTag("toggle_hud_button")
        ) {
          Icon(
            imageVector = Icons.Default.Flip,
            contentDescription = "Chế độ kính lái HUD",
            tint = if (hudMirrorMode) AlertEmeraldDark else NavLightTextSecondary,
            modifier = Modifier.size(20.dp)
          )
        }

        // Quick Report Camera
        IconButton(
          onClick = onOpenReportDialog,
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(NavRouteBlue)
            .testTag("quick_report_camera_button")
        ) {
          Icon(
            imageVector = Icons.Default.AddLocationAlt,
            contentDescription = "Báo Camera",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // DYNAMIC VISUAL SPEED LIMIT ALERT HUD (Primary Component comparing Speed vs Mock Speed Limit)
    VisualSpeedAlertHUD(
      alertState = visualAlertState,
      onSelectMockZone = onSelectMockZone,
      onSetCustomSpeed = { speed ->
        testSpeedSlider = speed
        onSetSimulatedSpeed(speed)
      }
    )

    Spacer(modifier = Modifier.height(14.dp))

    Spacer(modifier = Modifier.height(10.dp))

    // Active Road Name Banner
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = NavLightSurface,
      shadowElevation = 2.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, NavLightCardBorder, RoundedCornerShape(14.dp))
        .clickable { showRoadSelectDialog = true }
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Navigation,
          contentDescription = null,
          tint = NavRouteBlue,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "TUYẾN ĐƯỜNG ĐANG LƯU THÔNG (NHẤP ĐỔI)",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            ),
            color = NavLightTextTertiary
          )
          Text(
            text = trafficEvaluation.currentRoadName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = NavLightTextPrimary,
            maxLines = 1
          )
        }
        Icon(
          imageVector = Icons.Default.Edit,
          contentDescription = "Đổi tuyến đường",
          tint = NavRouteBlue,
          modifier = Modifier.size(16.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Speedometer Gauge
    SpeedometerGauge(
      currentSpeed = locationState.speedKmh,
      speedLimit = trafficEvaluation.currentSpeedLimit,
      isOverspeeding = trafficEvaluation.isOverspeeding,
      hudMirrorMode = hudMirrorMode
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Prominent State Speed Limit vs. User Speed Comparison Card
    SpeedComparisonCard(
      currentSpeed = locationState.speedKmh,
      speedLimit = trafficEvaluation.currentSpeedLimit,
      roadName = trafficEvaluation.currentRoadName,
      isOverspeeding = trafficEvaluation.isOverspeeding
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Active Camera Warning Card (Reveals when approaching a camera)
    CameraAlertCard(
      activeWarning = trafficEvaluation.activeWarning,
      modifier = Modifier.padding(vertical = 4.dp)
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Live Trip Statistics Card
    TripStatsCard(
      tripStats = tripStats,
      isRecording = isRecordingTrip
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Trip Recording Action Button
    Button(
      onClick = onToggleTripRecording,
      colors = ButtonDefaults.buttonColors(
        containerColor = if (isRecordingTrip) AlertCrimsonDanger else NavRouteBlue,
        contentColor = Color.White
      ),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .height(50.dp)
        .testTag("toggle_trip_recording_button")
    ) {
      Icon(
        imageVector = if (isRecordingTrip) Icons.Default.Stop else Icons.Default.PlayArrow,
        contentDescription = null,
        modifier = Modifier.size(24.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = if (isRecordingTrip) "KẾT THÚC & LƯU CHUYẾN ĐI" else "BẮT ĐẦU CHUYẾN ĐI MỚI",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Simulation Test Controls (Collapsible)
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = NavLightSurface,
      shadowElevation = 2.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, NavLightCardBorder, RoundedCornerShape(14.dp))
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showSimulationControls = !showSimulationControls }
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Tune,
              contentDescription = null,
              tint = NavRouteBlue,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Bảng Điều Khiển Chạy Thử (Mô Phỏng)",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = NavLightTextPrimary
            )
          }
          Icon(
            imageVector = if (showSimulationControls) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = NavLightTextSecondary
          )
        }

        AnimatedVisibility(visible = showSimulationControls) {
          Column(modifier = Modifier.padding(top = 10.dp)) {
            // Speed indicator & delta
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Tốc độ di chuyển: ${testSpeedSlider.toInt()} km/h",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (testSpeedSlider > trafficEvaluation.currentSpeedLimit) AlertCrimsonDanger else AlertEmeraldDark
              )
              Text(
                text = "Quy định: ${trafficEvaluation.currentSpeedLimit} km/h",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF64748B)
              )
            }

            Slider(
              value = testSpeedSlider,
              onValueChange = {
                testSpeedSlider = it
                onSetSimulatedSpeed(it)
              },
              valueRange = 0f..95f,
              colors = SliderDefaults.colors(
                thumbColor = NavRouteBlue,
                activeTrackColor = if (testSpeedSlider > trafficEvaluation.currentSpeedLimit) AlertCrimsonDanger else NavRouteBlue,
                inactiveTrackColor = Color(0xFFE2E8F0)
              ),
              modifier = Modifier.fillMaxWidth().testTag("simulation_speed_slider")
            )

            // Preset speed testing buttons
            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Button(
                onClick = {
                  testSpeedSlider = 35f
                  onSetSimulatedSpeed(35f)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = NavLightTextPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text("35 km/h", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
              }

              Button(
                onClick = {
                  testSpeedSlider = 50f
                  onSetSimulatedSpeed(50f)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9), contentColor = NavLightTextPrimary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.weight(1.1f)
              ) {
                Text("50 (Chuẩn)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
              }

              Button(
                onClick = {
                  testSpeedSlider = 58f
                  onSetSimulatedSpeed(58f)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = AlertCrimsonDanger),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.weight(1.2f)
              ) {
                Text("58 km/h ⚠️", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
              }

              Button(
                onClick = {
                  testSpeedSlider = 75f
                  onSetSimulatedSpeed(75f)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2), contentColor = AlertCrimsonDanger),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.weight(1.2f)
              ) {
                Text("75 km/h 🚨", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Test Road Presets
            Text(
              text = "Chọn nhanh tuyến đường kiểm tra:",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Button(
                onClick = { onSetCustomRoad("Đường Nguyễn Văn A") },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (trafficEvaluation.currentRoadName == "Đường Nguyễn Văn A") Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                  contentColor = NavLightTextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.weight(1.2f)
              ) {
                Text("Đ. Nguyễn Văn A (50)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), maxLines = 1)
              }

              Button(
                onClick = { onSetCustomRoad("Đại lộ Võ Văn Kiệt") },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (trafficEvaluation.currentRoadName == "Đại lộ Võ Văn Kiệt") Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                  contentColor = NavLightTextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.weight(1.2f)
              ) {
                Text("Võ Văn Kiệt (60)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), maxLines = 1)
              }

              Button(
                onClick = { onSetCustomRoad("Cao Tốc Long Thành") },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (trafficEvaluation.currentRoadName == "Cao Tốc Long Thành") Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                  contentColor = NavLightTextPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.weight(1.1f)
              ) {
                Text("Cao Tốc (80)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), maxLines = 1)
              }
            }
          }
        }
      }
    }

    // Road Selection Dialog
    if (showRoadSelectDialog) {
      AlertDialog(
        onDismissRequest = { showRoadSelectDialog = false },
        containerColor = Color.White,
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Navigation, contentDescription = null, tint = NavRouteBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Chọn hoặc nhập tuyến đường",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color(0xFF0F172A)
            )
          }
        },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Chọn tuyến đường hoặc nhập tên đường (ví dụ Đường Nguyễn Văn A):",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF64748B)
            )

            val presets = listOf(
              "Đường Nguyễn Văn A" to "Đô thị (Tối đa 50 km/h)",
              "Đường Lê Duẩn" to "Đô thị (Tối đa 50 km/h)",
              "Đại lộ Võ Văn Kiệt" to "Đại lộ (Tối đa 60 km/h)",
              "Cao Tốc Long Thành" to "Cao tốc (Tối đa 80 km/h)"
            )

            presets.forEach { (road, desc) ->
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (trafficEvaluation.currentRoadName == road) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(
                  1.dp,
                  if (trafficEvaluation.currentRoadName == road) NavRouteBlue else Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable {
                    onSetCustomRoad(road)
                    showRoadSelectDialog = false
                  }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(
                      text = road,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                      color = Color(0xFF0F172A)
                    )
                    Text(
                      text = desc,
                      style = MaterialTheme.typography.labelSmall,
                      color = Color(0xFF64748B)
                    )
                  }
                  if (trafficEvaluation.currentRoadName == road) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NavRouteBlue, modifier = Modifier.size(18.dp))
                  }
                }
              }
            }

            OutlinedTextField(
              value = customRoadInput,
              onValueChange = { customRoadInput = it },
              placeholder = { Text("Hoặc nhập tên đường khác...") },
              label = { Text("Tên đường tuỳ chỉnh") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              if (customRoadInput.isNotBlank()) {
                onSetCustomRoad(customRoadInput.trim())
              }
              showRoadSelectDialog = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = NavRouteBlue)
          ) {
            Text("Áp dụng", fontWeight = FontWeight.Bold)
          }
        },
        dismissButton = {
          TextButton(onClick = { showRoadSelectDialog = false }) {
            Text("Hủy", color = Color(0xFF64748B))
          }
        }
      )
    }
  }
}

/**
 * Fullscreen OLED Pitch-Black HUD View with Mirror Flip for Windshield Projection
 */
@Composable
fun FullscreenMirrorHUDView(
  currentSpeed: Int,
  speedLimit: Int,
  isOverspeeding: Boolean,
  roadName: String,
  nearestCamera: com.example.data.model.TrafficCamera?,
  nearestCameraDistance: Int?,
  onExitMirrorMode: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isMirrored by remember { mutableStateOf(true) }
  val speedColor = if (isOverspeeding) Color(0xFFFF2A6D) else Color(0xFF00F0FF)

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
      .graphicsLayer(
        scaleY = if (isMirrored) -1f else 1f,
        scaleX = 1f
      )
      .padding(24.dp)
  ) {
    // Top Row: Road Name & Mode Toggle
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.TopCenter)
    ) {
      Text(
        text = roadName,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        ),
        color = Color(0xFF94A3B8),
        maxLines = 1
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        // Toggle Flip Button
        Surface(
          shape = CircleShape,
          color = Color(0xFF1E293B),
          modifier = Modifier
            .size(44.dp)
            .clickable { isMirrored = !isMirrored }
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Flip,
              contentDescription = "Lật hình",
              tint = Color(0xFF38BDF8),
              modifier = Modifier.size(24.dp)
            )
          }
        }

        // Exit HUD Mode Button
        Surface(
          shape = CircleShape,
          color = Color(0xFFDC2626),
          modifier = Modifier
            .size(44.dp)
            .clickable { onExitMirrorMode() }
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Đóng HUD",
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
          }
        }
      }
    }

    // Center Display: Huge Neon Speedometer
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.align(Alignment.Center)
    ) {
      Text(
        text = "$currentSpeed",
        style = MaterialTheme.typography.displayLarge.copy(
          fontWeight = FontWeight.Black,
          fontSize = 118.sp,
          letterSpacing = (-4).sp
        ),
        color = speedColor
      )
      Text(
        text = "km/h",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 22.sp
        ),
        color = Color(0xFF64748B)
      )
    }

    // Bottom Display: Speed Limit Badge & Camera Countdown
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
    ) {
      // Speed Limit Sign
      Surface(
        shape = CircleShape,
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(5.dp, Color(0xFFDC2626)),
        modifier = Modifier.size(68.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = "$speedLimit",
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Black,
              fontSize = 24.sp
            ),
            color = Color.Black
          )
        }
      }

      // Next Camera Card
      if (nearestCamera != null && nearestCameraDistance != null) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = Color(0xFF0F172A),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, speedColor),
          modifier = Modifier.padding(start = 12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
          ) {
            Text(
              text = "📷 ${nearestCameraDistance}m",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
              color = speedColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = nearestCamera.type.displayName.take(15),
              style = MaterialTheme.typography.bodySmall,
              color = Color.White
            )
          }
        }
      }
    }
  }
}


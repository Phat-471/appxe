package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OfflineMapPackEntity
import com.example.data.local.UserSettingsEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  settings: UserSettingsEntity,
  offlinePacks: List<OfflineMapPackEntity>,
  onUpdateSettings: (UserSettingsEntity) -> Unit,
  onTestVoice: () -> Unit,
  onDownloadPack: (OfflineMapPackEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  var showSpeedBufferDialog by remember { mutableStateOf(false) }
  var showDistanceDialog by remember { mutableStateOf(false) }
  var showVoiceVolumeDialog by remember { mutableStateOf(false) }
  var showVehicleTypeDialog by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "Cài đặt",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF0F172A)
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8FAFC))
      )
    },
    containerColor = Color(0xFFF1F5F9),
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

      // GROUP 1: CHUNG
      SectionTitleText(text = "Chung")

      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
      ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
          SettingsNavigationRow(
            icon = Icons.Default.Speed,
            title = "Đơn vị tốc độ",
            valueText = "km/h",
            onClick = { /* Fixed in VN */ }
          )
          RowDivider()
          SettingsNavigationRow(
            icon = Icons.Default.Tune,
            title = "Khoảng cách báo trước",
            valueText = "${settings.alertDistanceMeters}m",
            onClick = { showDistanceDialog = true }
          )
          RowDivider()
          SettingsNavigationRow(
            icon = Icons.Default.WarningAmber,
            title = "Cảnh báo quá tốc độ",
            valueText = "+${settings.speedBufferKmh} km/h",
            onClick = { showSpeedBufferDialog = true }
          )
          RowDivider()
          SettingsNavigationRow(
            icon = Icons.Default.DarkMode,
            title = "Chế độ tối (Dark Mode)",
            valueText = if (settings.darkMapMode) "Bật" else "Tắt",
            onClick = { onUpdateSettings(settings.copy(darkMapMode = !settings.darkMapMode)) }
          )
          RowDivider()
          SettingsNavigationRow(
            icon = Icons.Default.TwoWheeler,
            title = "Loại phương tiện",
            valueText = "Xe máy (Ưu tiên)",
            onClick = { showVehicleTypeDialog = true }
          )
          RowDivider()
          SettingsNavigationRow(
            icon = Icons.Default.Language,
            title = "Ngôn ngữ",
            valueText = "Tiếng Việt",
            onClick = { /* Tiếng Việt standard */ }
          )
          RowDivider()
          SettingsNavigationRow(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            title = "Âm thanh cảnh báo",
            valueText = if (settings.voiceAlertsEnabled) "Bật — 100%" else "Tắt",
            onClick = { showVoiceVolumeDialog = true }
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // GROUP 2: HIỂN THỊ CẢNH BÁO
      SectionTitleText(text = "Hiển thị cảnh báo")

      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
      ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
          SettingsSwitchRow(
            icon = Icons.Default.Place,
            title = "Hiện cảnh báo xung quanh",
            subText = "Hiển thị các cảnh báo quanh bạn trên bản đồ",
            isChecked = settings.showSpeedCamerasOnMap,
            onCheckedChange = { onUpdateSettings(settings.copy(showSpeedCamerasOnMap = it, showRedLightCamerasOnMap = it)) }
          )
          RowDivider()
          SettingsSwitchRow(
            icon = Icons.Default.Explore,
            title = "Xoay theo hướng đi",
            subText = "Tự động xoay bản đồ theo la bàn cảm biến điện thoại",
            isChecked = settings.compassEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(compassEnabled = it)) }
          )
          RowDivider()
          SettingsSwitchRow(
            icon = Icons.Default.Lightbulb,
            title = "Giữ màn hình luôn sáng",
            subText = "Không tắt màn hình khi đang mở ứng dụng dẫn đường",
            isChecked = settings.autoScreenOn,
            onCheckedChange = { onUpdateSettings(settings.copy(autoScreenOn = it)) }
          )
          RowDivider()
          SettingsSwitchRow(
            icon = Icons.Default.NotificationsActive,
            title = "Âm bíp trước cảnh báo",
            subText = "Phát tiếng bíp nhẹ báo hiệu trước khi đọc",
            isChecked = settings.chimeOnAlert,
            onCheckedChange = { onUpdateSettings(settings.copy(chimeOnAlert = it)) }
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // GROUP 3: BẢN ĐỒ NGOẠI TUYẾN
      SectionTitleText(text = "Bản đồ & Dữ liệu ngoại tuyến")

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        offlinePacks.forEach { pack ->
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier
              .fillMaxWidth()
              .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = pack.name,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF0F172A)
                )
                Text(
                  text = "${pack.cameraCount} camera • ${pack.sizeMb.toInt()} MB",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFF64748B)
                )
              }
              Button(
                onClick = { onDownloadPack(pack) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (pack.isDownloaded) Color(0xFFE2E8F0) else NavRouteBlue,
                  contentColor = if (pack.isDownloaded) Color(0xFF475569) else Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
              ) {
                Text(
                  text = if (pack.isDownloaded) "Đã tải" else "Tải về",
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(30.dp))
    }
  }

  // Speed Buffer Dialog
  if (showSpeedBufferDialog) {
    AlertDialog(
      onDismissRequest = { showSpeedBufferDialog = false },
      title = { Text("Cảnh báo quá tốc độ", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(0, 3, 5, 10).forEach { buffer ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onUpdateSettings(settings.copy(speedBufferKmh = buffer))
                  showSpeedBufferDialog = false
                }
                .padding(vertical = 10.dp)
            ) {
              RadioButton(
                selected = settings.speedBufferKmh == buffer,
                onClick = {
                  onUpdateSettings(settings.copy(speedBufferKmh = buffer))
                  showSpeedBufferDialog = false
                }
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("+$buffer km/h (Báo khi vượt $buffer km/h)")
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSpeedBufferDialog = false }) { Text("Đóng") }
      }
    )
  }

  // Distance Dialog
  if (showDistanceDialog) {
    AlertDialog(
      onDismissRequest = { showDistanceDialog = false },
      title = { Text("Khoảng cách báo trước", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(300, 500, 700, 1000).forEach { dist ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onUpdateSettings(settings.copy(alertDistanceMeters = dist))
                  showDistanceDialog = false
                }
                .padding(vertical = 10.dp)
            ) {
              RadioButton(
                selected = settings.alertDistanceMeters == dist,
                onClick = {
                  onUpdateSettings(settings.copy(alertDistanceMeters = dist))
                  showDistanceDialog = false
                }
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("$dist mét trước camera")
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showDistanceDialog = false }) { Text("Đóng") }
      }
    )
  }

  // Voice Volume Dialog
  if (showVoiceVolumeDialog) {
    AlertDialog(
      onDismissRequest = { showVoiceVolumeDialog = false },
      title = { Text("Âm thanh cảnh báo", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Bật giọng nói tiếng Việt", fontWeight = FontWeight.SemiBold)
            Switch(
              checked = settings.voiceAlertsEnabled,
              onCheckedChange = { onUpdateSettings(settings.copy(voiceAlertsEnabled = it)) }
            )
          }
          Button(
            onClick = { onTestVoice() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0EDFF), contentColor = NavRouteBlue),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Nghe thử giọng đọc", fontWeight = FontWeight.Bold)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showVoiceVolumeDialog = false }) { Text("Xong") }
      }
    )
  }

  // Vehicle Type Dialog
  if (showVehicleTypeDialog) {
    AlertDialog(
      onDismissRequest = { showVehicleTypeDialog = false },
      title = { Text("Loại phương tiện", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { showVehicleTypeDialog = false }
              .padding(vertical = 10.dp)
          ) {
            RadioButton(selected = true, onClick = { showVehicleTypeDialog = false })
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text("🏍️ Xe máy (Mặc định)", fontWeight = FontWeight.Bold)
              Text("Tự động tránh cao tốc & cảnh báo đường cấm xe 2 bánh", fontSize = 12.sp, color = Color.Gray)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showVehicleTypeDialog = false }) { Text("Xác nhận") }
      }
    )
  }
}

@Composable
private fun SectionTitleText(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleMedium.copy(
      fontWeight = FontWeight.Bold,
      fontSize = 14.5.sp
    ),
    color = Color(0xFF0284C7),
    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
  )
}

@Composable
private fun SettingsNavigationRow(
  icon: ImageVector,
  title: String,
  valueText: String,
  onClick: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() }
      .padding(horizontal = 16.dp, vertical = 14.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = Color(0xFF64748B),
      modifier = Modifier.size(20.dp)
    )
    Spacer(modifier = Modifier.width(14.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
      color = Color(0xFF1E293B),
      modifier = Modifier.weight(1f)
    )
    Text(
      text = valueText,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
      color = Color(0xFF64748B)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Icon(
      imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
      contentDescription = null,
      tint = Color(0xFF94A3B8),
      modifier = Modifier.size(13.dp)
    )
  }
}

@Composable
private fun SettingsSwitchRow(
  icon: ImageVector,
  title: String,
  subText: String? = null,
  isChecked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = Color(0xFF64748B),
      modifier = Modifier.size(20.dp)
    )
    Spacer(modifier = Modifier.width(14.dp))
    Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Normal),
        color = Color(0xFF1E293B)
      )
      if (!subText.isNullOrBlank()) {
        Text(
          text = subText,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
          color = Color(0xFF94A3B8)
        )
      }
    }
    Switch(
      checked = isChecked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = Color(0xFF0284C7),
        uncheckedTrackColor = Color(0xFFE2E8F0)
      )
    )
  }
}

@Composable
private fun RowDivider() {
  HorizontalDivider(
    modifier = Modifier.padding(horizontal = 16.dp),
    color = Color(0xFFF1F5F9),
    thickness = 0.8.dp
  )
}

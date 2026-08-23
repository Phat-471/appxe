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
import com.example.ui.i18n.AppStrings
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
  var showLanguageDialog by remember { mutableStateOf(false) }

  val lang = settings.appLanguage
  val isEn = lang.equals("en", ignoreCase = true)

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = AppStrings.get("settings_title", lang),
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

      // GROUP 1: CHUNG / GENERAL
      SectionTitleText(text = AppStrings.get("section_general", lang))

      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
      ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
          // Language selection
          SettingsNavigationRow(
            icon = Icons.Default.Language,
            title = AppStrings.get("language", lang),
            valueText = if (isEn) "English 🇬🇧" else "Tiếng Việt 🇻🇳",
            onClick = { showLanguageDialog = true }
          )
          RowDivider()
          // Vehicle Icon Picker
          SettingsNavigationRow(
            icon = Icons.Default.TwoWheeler,
            title = AppStrings.get("vehicle_icon", lang),
            valueText = when (settings.vehicleIconType) {
              "SCOOTER" -> "🛵 ${if (isEn) "Scooter" else "Xe tay ga"}"
              "MOTORBIKE" -> "🏍️ ${if (isEn) "Motorbike" else "Xe máy"}"
              "CAR" -> "🚗 ${if (isEn) "Car" else "Ô tô"}"
              "TRUCK" -> "🚛 ${if (isEn) "Truck" else "Xe tải"}"
              else -> "↑ ${if (isEn) "Arrow" else "Mũi tên"}"
            },
            onClick = { showVehicleTypeDialog = true }
          )
          RowDivider()
          SettingsNavigationRow(
            icon = Icons.Default.Tune,
            title = AppStrings.get("alert_distance", lang),
            valueText = "${settings.alertDistanceMeters}m",
            onClick = { showDistanceDialog = true }
          )
          RowDivider()
          SettingsNavigationRow(
            icon = Icons.Default.WarningAmber,
            title = AppStrings.get("overspeed_buffer", lang),
            valueText = "+${settings.speedBufferKmh} km/h",
            onClick = { showSpeedBufferDialog = true }
          )
          RowDivider()
          SettingsNavigationRow(
            icon = Icons.Default.DarkMode,
            title = AppStrings.get("dark_mode", lang),
            valueText = if (settings.darkMapMode) (if (isEn) "ON" else "Bật") else (if (isEn) "OFF" else "Tắt"),
            onClick = { onUpdateSettings(settings.copy(darkMapMode = !settings.darkMapMode)) }
          )
          RowDivider()
          SettingsNavigationRow(
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            title = AppStrings.get("voice_volume", lang),
            valueText = if (settings.voiceAlertsEnabled) "${(settings.voiceVolume * 100).toInt()}%" else (if (isEn) "OFF" else "Tắt"),
            onClick = { showVoiceVolumeDialog = true }
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // GROUP 2: BỘ LỌC QUÉT & CẢNH BÁO (ALERT NOTIFICATION FILTERS)
      SectionTitleText(text = AppStrings.get("section_alerts", lang))

      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
      ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
          // Speed Cameras
          SettingsSwitchRow(
            icon = Icons.Default.CameraAlt,
            title = AppStrings.get("alert_speed_cam", lang),
            subText = AppStrings.get("alert_speed_cam_desc", lang),
            isChecked = settings.showSpeedCamerasOnMap,
            onCheckedChange = { onUpdateSettings(settings.copy(showSpeedCamerasOnMap = it)) }
          )
          RowDivider()
          // Red Light & Cold Fine Cameras
          SettingsSwitchRow(
            icon = Icons.Default.Traffic,
            title = AppStrings.get("alert_red_light", lang),
            subText = AppStrings.get("alert_red_light_desc", lang),
            isChecked = settings.showRedLightCamerasOnMap,
            onCheckedChange = { onUpdateSettings(settings.copy(showRedLightCamerasOnMap = it)) }
          )
          RowDivider()
          // Motorcycle Prohibited Zones (Expressways)
          SettingsSwitchRow(
            icon = Icons.Default.DoNotDisturb,
            title = AppStrings.get("alert_prohibited", lang),
            subText = AppStrings.get("alert_prohibited_desc", lang),
            isChecked = settings.showProhibitedZones,
            onCheckedChange = { onUpdateSettings(settings.copy(showProhibitedZones = it)) }
          )
          RowDivider()
          // Security Cameras
          SettingsSwitchRow(
            icon = Icons.Default.Shield,
            title = AppStrings.get("alert_security", lang),
            subText = AppStrings.get("alert_security_desc", lang),
            isChecked = settings.showSecurityCameras,
            onCheckedChange = { onUpdateSettings(settings.copy(showSecurityCameras = it)) }
          )
          RowDivider()
          // Hazards, Accidents & School Zones
          SettingsSwitchRow(
            icon = Icons.Default.Warning,
            title = AppStrings.get("alert_hazards", lang),
            subText = AppStrings.get("alert_hazards_desc", lang),
            isChecked = settings.showHazards,
            onCheckedChange = { onUpdateSettings(settings.copy(showHazards = it)) }
          )
          RowDivider()
          // Gas Stations & POI
          SettingsSwitchRow(
            icon = Icons.Default.LocalGasStation,
            title = AppStrings.get("alert_pois", lang),
            subText = AppStrings.get("alert_pois_desc", lang),
            isChecked = settings.showPois,
            onCheckedChange = { onUpdateSettings(settings.copy(showPois = it)) }
          )
          RowDivider()
          // Community Reports
          SettingsSwitchRow(
            icon = Icons.Default.Group,
            title = AppStrings.get("alert_community", lang),
            subText = AppStrings.get("alert_community_desc", lang),
            isChecked = settings.showCommunityReportsOnMap,
            onCheckedChange = { onUpdateSettings(settings.copy(showCommunityReportsOnMap = it)) }
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // GROUP 3: TÙY CHỌN DẪN ĐƯỜNG & HỆ THỐNG
      SectionTitleText(text = AppStrings.get("section_display", lang))

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
            icon = Icons.Default.Explore,
            title = AppStrings.get("compass_rotation", lang),
            subText = if (isEn) "Rotate map with phone sensor" else "Tự động xoay bản đồ theo la bàn điện thoại",
            isChecked = settings.compassEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(compassEnabled = it)) }
          )
          RowDivider()
          SettingsSwitchRow(
            icon = Icons.Default.Lightbulb,
            title = AppStrings.get("keep_screen_on", lang),
            subText = if (isEn) "Prevent screen from sleeping while navigating" else "Không tắt màn hình khi đang mở ứng dụng",
            isChecked = settings.autoScreenOn,
            onCheckedChange = { onUpdateSettings(settings.copy(autoScreenOn = it)) }
          )
          RowDivider()
          SettingsSwitchRow(
            icon = Icons.Default.NotificationsActive,
            title = if (isEn) "Chime before voice alert" else "Âm bíp trước cảnh báo",
            subText = if (isEn) "Play soft beep sound before speaking" else "Phát tiếng bíp nhẹ báo hiệu trước khi đọc",
            isChecked = settings.chimeOnAlert,
            onCheckedChange = { onUpdateSettings(settings.copy(chimeOnAlert = it)) }
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // GROUP 4: BẢN ĐỒ NGOẠI TUYẾN
      SectionTitleText(text = AppStrings.get("section_offline", lang))

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
                  text = if (pack.isDownloaded) (if (isEn) "Downloaded" else "Đã tải") else (if (isEn) "Download" else "Tải về"),
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

  // Language Selection Dialog
  if (showLanguageDialog) {
    AlertDialog(
      onDismissRequest = { showLanguageDialog = false },
      title = { Text(if (isEn) "Select Language" else "Chọn ngôn ngữ", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(
            "vi" to "Tiếng Việt 🇻🇳",
            "en" to "English 🇬🇧"
          ).forEach { (code, name) ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onUpdateSettings(settings.copy(appLanguage = code))
                  showLanguageDialog = false
                }
                .padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
              RadioButton(
                selected = settings.appLanguage == code,
                onClick = {
                  onUpdateSettings(settings.copy(appLanguage = code))
                  showLanguageDialog = false
                }
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showLanguageDialog = false }) { Text(if (isEn) "Cancel" else "Hủy") }
      }
    )
  }

  // Speed Buffer Dialog
  if (showSpeedBufferDialog) {
    AlertDialog(
      onDismissRequest = { showSpeedBufferDialog = false },
      title = { Text(AppStrings.get("overspeed_buffer", lang), fontWeight = FontWeight.Bold) },
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
                .padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
              RadioButton(
                selected = settings.speedBufferKmh == buffer,
                onClick = {
                  onUpdateSettings(settings.copy(speedBufferKmh = buffer))
                  showSpeedBufferDialog = false
                }
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (buffer == 0) (if (isEn) "+0 km/h (Strict standard)" else "+0 km/h (Chuẩn tuyệt đối)") else "+$buffer km/h",
                style = MaterialTheme.typography.bodyMedium
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showSpeedBufferDialog = false }) { Text(if (isEn) "Cancel" else "Hủy") }
      }
    )
  }

  // Alert Distance Dialog
  if (showDistanceDialog) {
    AlertDialog(
      onDismissRequest = { showDistanceDialog = false },
      title = { Text(AppStrings.get("alert_distance", lang), fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(300, 500, 700, 1000).forEach { distance ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onUpdateSettings(settings.copy(alertDistanceMeters = distance))
                  showDistanceDialog = false
                }
                .padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
              RadioButton(
                selected = settings.alertDistanceMeters == distance,
                onClick = {
                  onUpdateSettings(settings.copy(alertDistanceMeters = distance))
                  showDistanceDialog = false
                }
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (distance == 500) "${distance}m (${if (isEn) "Recommended" else "Khuyến nghị"})" else "${distance}m",
                style = MaterialTheme.typography.bodyMedium
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showDistanceDialog = false }) { Text(if (isEn) "Cancel" else "Hủy") }
      }
    )
  }

  // Voice Volume Dialog
  if (showVoiceVolumeDialog) {
    var tempVolume by remember { mutableFloatStateOf(settings.voiceVolume) }
    var tempVoiceEnabled by remember { mutableStateOf(settings.voiceAlertsEnabled) }

    AlertDialog(
      onDismissRequest = { showVoiceVolumeDialog = false },
      title = { Text(AppStrings.get("voice_volume", lang), fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(AppStrings.get("voice_enabled", lang), style = MaterialTheme.typography.bodyMedium)
            Switch(
              checked = tempVoiceEnabled,
              onCheckedChange = { tempVoiceEnabled = it }
            )
          }

          if (tempVoiceEnabled) {
            Column {
              Text(
                "${AppStrings.get("voice_volume", lang)}: ${(tempVolume * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
              )
              Slider(
                value = tempVolume,
                onValueChange = { tempVolume = it },
                valueRange = 0.1f..1.0f,
                steps = 8
              )
            }
          }

          OutlinedButton(
            onClick = onTestVoice,
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(AppStrings.get("test_voice", lang))
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            onUpdateSettings(
              settings.copy(
                voiceAlertsEnabled = tempVoiceEnabled,
                voiceVolume = tempVolume
              )
            )
            showVoiceVolumeDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = NavRouteBlue)
        ) {
          Text(if (isEn) "Save" else "Lưu")
        }
      },
      dismissButton = {
        TextButton(onClick = { showVoiceVolumeDialog = false }) { Text(if (isEn) "Cancel" else "Hủy") }
      }
    )
  }

  // Vehicle Icon Dialog
  if (showVehicleTypeDialog) {
    AlertDialog(
      onDismissRequest = { showVehicleTypeDialog = false },
      title = { Text(AppStrings.get("vehicle_icon", lang), fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          listOf(
            Triple("SCOOTER", "🛵", if (isEn) "Scooter (Default)" else "Xe tay ga / xe số (Mặc định)"),
            Triple("MOTORBIKE", "🏍️", if (isEn) "Big Motorbike" else "Xe máy phân khối lớn"),
            Triple("CAR", "🚗", if (isEn) "Car" else "Ô tô"),
            Triple("TRUCK", "🚛", if (isEn) "Truck" else "Xe tải"),
            Triple("ARROW", "↑", if (isEn) "Classic Arrow" else "Mũi tên cổ điển")
          ).forEach { (type, icon, label) ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  onUpdateSettings(settings.copy(vehicleIconType = type))
                  showVehicleTypeDialog = false
                }
                .padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
              RadioButton(
                selected = settings.vehicleIconType == type,
                onClick = {
                  onUpdateSettings(settings.copy(vehicleIconType = type))
                  showVehicleTypeDialog = false
                }
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = icon,
                fontSize = 22.sp,
                modifier = Modifier.width(36.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(label, style = MaterialTheme.typography.bodyMedium)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showVehicleTypeDialog = false }) { Text(if (isEn) "Close" else "Xác nhận") }
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

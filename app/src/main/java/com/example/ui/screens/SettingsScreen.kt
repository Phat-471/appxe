package com.example.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OfflineMapPackEntity
import com.example.data.local.UserSettingsEntity
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
  settings: UserSettingsEntity,
  offlinePacks: List<OfflineMapPackEntity>,
  onUpdateSettings: (UserSettingsEntity) -> Unit,
  onTestVoice: () -> Unit,
  onDownloadPack: (OfflineMapPackEntity) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(NavLightBackground)
      .verticalScroll(rememberScrollState())
      .padding(16.dp)
  ) {
    // Header
    Column {
      Text(
        text = "HỆ THỐNG CÀI ĐẶT",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp
        ),
        color = NavRouteBlue
      )
      Text(
        text = "Tùy Chỉnh Cảnh Báo & Dữ Liệu",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = NavLightTextPrimary
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // SECTION 1: CẢNH BÁO GIỌNG NÓI
    SettingsSectionHeader(icon = Icons.Default.RecordVoiceOver, title = "CẢNH BÁO BẰNG GIỌNG NÓI")

    Surface(
      shape = RoundedCornerShape(14.dp),
      color = NavLightSurface,
      shadowElevation = 2.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, NavLightCardBorder, RoundedCornerShape(14.dp))
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        // Toggle voice
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Bật giọng nói tiếng Việt",
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
              color = NavLightTextPrimary
            )
            Text(
              text = "Đọc cảnh báo camera tốc độ, phạt nguội, khu dân cư",
              style = MaterialTheme.typography.bodySmall,
              color = NavLightTextSecondary
            )
          }
          Switch(
            checked = settings.voiceAlertsEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(voiceAlertsEnabled = it)) },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = NavRouteBlue
            )
          )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = NavLightCardBorder)

        // Khoảng cách cảnh báo trước
        Text(
          text = "Khoảng cách cảnh báo trước: ${settings.alertDistanceMeters}m",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
          color = NavLightTextPrimary
        )
        Slider(
          value = settings.alertDistanceMeters.toFloat(),
          onValueChange = { onUpdateSettings(settings.copy(alertDistanceMeters = it.toInt())) },
          valueRange = 200f..1000f,
          steps = 7,
          colors = SliderDefaults.colors(
            thumbColor = NavRouteBlue,
            activeTrackColor = NavRouteBlue,
            inactiveTrackColor = Color(0xFFE2E8F0)
          )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Nút thử giọng nói
        Button(
          onClick = onTestVoice,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE0EDFF),
            contentColor = NavRouteBlue
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Nghe thử giọng mẫu thông báo", fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // SECTION 2: CẢM BIẾN & ĐIỀU HƯỚNG LA BÀN
    SettingsSectionHeader(icon = Icons.Default.Explore, title = "CẢM BIẾN LA BÀN & ĐIỀU HƯỚNG")

    Surface(
      shape = RoundedCornerShape(14.dp),
      color = NavLightSurface,
      shadowElevation = 2.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, NavLightCardBorder, RoundedCornerShape(14.dp))
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        SettingsToggleRow(
          title = "Cảm biến xoay theo điện thoại",
          description = "Tự động xoay bản đồ theo hướng cầm điện thoại (kiểu Google Maps)",
          isChecked = settings.compassEnabled,
          onCheckedChange = { onUpdateSettings(settings.copy(compassEnabled = it)) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = NavLightCardBorder)
        SettingsToggleRow(
          title = "Giữ màn hình luôn sáng",
          description = "Không tắt màn hình khi đang mở ứng dụng dẫn đường",
          isChecked = settings.autoScreenOn,
          onCheckedChange = { onUpdateSettings(settings.copy(autoScreenOn = it)) }
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // SECTION 3: CHẠY NGẦM & TỐI ƯU PIN
    SettingsSectionHeader(icon = Icons.Default.BatteryChargingFull, title = "CHẠY NGẦM & TỐI ƯU PIN")

    Surface(
      shape = RoundedCornerShape(14.dp),
      color = NavLightSurface,
      shadowElevation = 2.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, NavLightCardBorder, RoundedCornerShape(14.dp))
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        SettingsToggleRow(
          title = "Chạy ngầm đọc cảnh báo",
          description = "Vẫn phát âm thanh cảnh báo camera khi ẩn app hoặc tắt màn hình (tiết kiệm pin)",
          isChecked = settings.backgroundServiceEnabled,
          onCheckedChange = { onUpdateSettings(settings.copy(backgroundServiceEnabled = it)) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = NavLightCardBorder)
        SettingsToggleRow(
          title = "Âm thanh bíp khi có camera",
          description = "Phát tiếng bíp nhẹ báo hiệu trước khi đọc cảnh báo",
          isChecked = settings.chimeOnAlert,
          onCheckedChange = { onUpdateSettings(settings.copy(chimeOnAlert = it)) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = NavLightCardBorder)
        SettingsToggleRow(
          title = "Rung khi vượt quá tốc độ",
          description = "Rung máy phản hồi xúc giác khi chạy quá giới hạn",
          isChecked = settings.vibrateOnAlert,
          onCheckedChange = { onUpdateSettings(settings.copy(vibrateOnAlert = it)) }
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // SECTION 4: TÙY CHỈNH HIỂN THỊ BẢN ĐỒ
    SettingsSectionHeader(icon = Icons.Default.Map, title = "TÙY CHỈNH HIỂN THỊ BẢN ĐỒ")

    Surface(
      shape = RoundedCornerShape(14.dp),
      color = NavLightSurface,
      shadowElevation = 2.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, NavLightCardBorder, RoundedCornerShape(14.dp))
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        // Chế độ ban đêm
        SettingsToggleRow(
          title = "Bản Đồ Ban Đêm (Dark Mode)",
          description = "Sử dụng giao diện bản đồ nền tối dịu mắt khi lái xe ban đêm",
          isChecked = settings.darkMapMode,
          onCheckedChange = { onUpdateSettings(settings.copy(darkMapMode = it)) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = NavLightCardBorder)

        // Vết lộ trình
        SettingsToggleRow(
          title = "Vết Lộ Trình Đã Đi",
          description = "Vẽ đường lịch sử các điểm xe vừa di chuyển qua",
          isChecked = settings.showBreadcrumbs,
          onCheckedChange = { onUpdateSettings(settings.copy(showBreadcrumbs = it)) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = NavLightCardBorder)

        // Biển báo tốc độ
        SettingsToggleRow(
          title = "Biển Báo Tốc Độ",
          description = "Hiển thị giới hạn tốc độ trên các cung đường",
          isChecked = settings.showSpeedLimitsOnMap,
          onCheckedChange = { onUpdateSettings(settings.copy(showSpeedLimitsOnMap = it)) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = NavLightCardBorder)
        
        // Camera phạt nguội
        SettingsToggleRow(
          title = "Camera Phạt Nguội & Đo Tốc Độ",
          description = "Hiển thị vị trí các camera phạt nguội và bắn tốc độ",
          isChecked = settings.showSpeedCamerasOnMap,
          onCheckedChange = { onUpdateSettings(settings.copy(showSpeedCamerasOnMap = it)) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = NavLightCardBorder)
        
        // Camera đèn đỏ
        SettingsToggleRow(
          title = "Camera Vượt Đèn Đỏ",
          description = "Hiển thị vị trí camera giám sát đèn tín hiệu giao thông",
          isChecked = settings.showRedLightCamerasOnMap,
          onCheckedChange = { onUpdateSettings(settings.copy(showRedLightCamerasOnMap = it)) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = NavLightCardBorder)
        
        // Báo cáo cộng đồng
        SettingsToggleRow(
          title = "Báo Cáo Cộng Đồng",
          description = "Hiển thị điểm báo tai nạn, chốt CSGT từ cộng đồng người dùng",
          isChecked = settings.showCommunityReportsOnMap,
          onCheckedChange = { onUpdateSettings(settings.copy(showCommunityReportsOnMap = it)) }
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // SECTION 5: GÓI DỮ LIỆU BẢN ĐỒ NGOẠI TUYẾN
    SettingsSectionHeader(icon = Icons.Default.DownloadForOffline, title = "GÓI BẢN ĐỒ & CAMERA NGOẠI TUYẾN")

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      offlinePacks.forEach { pack ->
        OfflinePackItemCard(pack = pack, onDownload = { onDownloadPack(pack) })
      }
    }
  }
}

@Composable
private fun SettingsSectionHeader(icon: ImageVector, title: String) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(bottom = 8.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = NavRouteBlue,
      modifier = Modifier.size(18.dp)
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.5.sp
      ),
      color = NavLightTextSecondary
    )
  }
}

@Composable
private fun SettingsToggleRow(
  title: String,
  description: String,
  isChecked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
        color = NavLightTextPrimary
      )
      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = NavLightTextSecondary
      )
    }
    Switch(
      checked = isChecked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = NavRouteBlue
      )
    )
  }
}

@Composable
private fun OfflinePackItemCard(
  pack: OfflineMapPackEntity,
  onDownload: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = NavLightSurface,
    shadowElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, NavLightCardBorder, RoundedCornerShape(14.dp))
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.padding(14.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = pack.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = NavLightTextPrimary
          )
          Spacer(modifier = Modifier.width(8.dp))
          if (pack.isDownloaded) {
            Box(
              modifier = Modifier
                .clip(CircleShape)
                .background(Color(0xFFDCFCE7))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = "Đã tải",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                color = AlertEmeraldDark
              )
            }
          }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${pack.cameraCount} camera giám sát • ${pack.sizeMb.toInt()} MB",
          style = MaterialTheme.typography.bodySmall,
          color = NavLightTextSecondary
        )
      }

      Button(
        onClick = onDownload,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (pack.isDownloaded) Color(0xFFF1F5F9) else NavRouteBlue,
          contentColor = if (pack.isDownloaded) NavLightTextPrimary else Color.White
        ),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Icon(
          imageVector = if (pack.isDownloaded) Icons.Default.Check else Icons.Default.Download,
          contentDescription = null,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = if (pack.isDownloaded) "Cập nhật" else "Tải về",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
        )
      }
    }
  }
}

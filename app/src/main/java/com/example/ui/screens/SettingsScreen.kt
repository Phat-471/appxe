package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
        text = "TÙY CHỈNH ỨNG DỤNG",
        style = MaterialTheme.typography.labelSmall.copy(
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.5.sp
        ),
        color = NavRouteBlue
      )
      Text(
        text = "Cài Đặt & Cảnh Báo",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
        color = NavLightTextPrimary
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // SECTION 1: PHƯƠNG TIỆN XE MÁY & DẪN ĐƯỜNG
    SettingsSectionHeader(icon = Icons.Default.TwoWheeler, title = "CHẾ ĐỘ XE MÁY & DẪN ĐƯỜNG")

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = NavLightSurface,
      shadowElevation = 2.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, NavLightCardBorder, RoundedCornerShape(16.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        SettingsToggleRow(
          title = "🏍️ Chế độ ưu tiên xe máy",
          description = "Tự động tránh toàn bộ đường cao tốc và đường cấm xe 2 bánh",
          isChecked = true,
          onCheckedChange = { /* Always active for motorbike */ }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = NavLightCardBorder)
        SettingsToggleRow(
          title = "🚫 Cảnh báo đường cấm xe máy",
          description = "Phát âm thanh cảnh báo sớm khi đến gần lối vào cao tốc hoặc làn ô tô",
          isChecked = true,
          onCheckedChange = { /* Default enabled */ }
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // SECTION 2: CẢNH BÁO GIỌNG NÓI & ÂM THANH
    SettingsSectionHeader(icon = Icons.Default.RecordVoiceOver, title = "CẢNH BÁO GIỌNG NÓI & ÂM THANH")

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = NavLightSurface,
      shadowElevation = 2.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, NavLightCardBorder, RoundedCornerShape(16.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        // Toggle voice
        SettingsToggleRow(
          title = "Giọng nói tiếng Việt",
          description = "Đọc cảnh báo camera tốc độ, phạt nguội, khu dân cư",
          isChecked = settings.voiceAlertsEnabled,
          onCheckedChange = { onUpdateSettings(settings.copy(voiceAlertsEnabled = it)) }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = NavLightCardBorder)

        // Khoảng cách cảnh báo trước
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Khoảng cách báo trước",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = NavLightTextPrimary
          )
          Text(
            text = "${settings.alertDistanceMeters}m",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = NavRouteBlue
          )
        }
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

        Spacer(modifier = Modifier.height(4.dp))

        // Âm báo bíp
        SettingsToggleRow(
          title = "Âm bíp trước cảnh báo",
          description = "Phát tiếng bíp nhẹ báo hiệu trước khi đọc",
          isChecked = settings.chimeOnAlert,
          onCheckedChange = { onUpdateSettings(settings.copy(chimeOnAlert = it)) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Nút thử giọng nói
        Button(
          onClick = onTestVoice,
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE0EDFF),
            contentColor = NavRouteBlue
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text("Nghe thử giọng mẫu", fontWeight = FontWeight.Bold)
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // SECTION 3: BẢN ĐỒ & HIỂN THỊ
    SettingsSectionHeader(icon = Icons.Default.Map, title = "BẢN ĐỒ & HIỂN THỊ")

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = NavLightSurface,
      shadowElevation = 2.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, NavLightCardBorder, RoundedCornerShape(16.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        SettingsToggleRow(
          title = "Cảm biến xoay theo điện thoại",
          description = "Tự động xoay bản đồ theo hướng cầm lái",
          isChecked = settings.compassEnabled,
          onCheckedChange = { onUpdateSettings(settings.copy(compassEnabled = it)) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = NavLightCardBorder)
        SettingsToggleRow(
          title = "Bản đồ ban đêm (Dark Mode)",
          description = "Giao diện nền tối dịu mắt khi lái xe trời tối",
          isChecked = settings.darkMapMode,
          onCheckedChange = { onUpdateSettings(settings.copy(darkMapMode = it)) }
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = NavLightCardBorder)
        SettingsToggleRow(
          title = "Giữ màn hình luôn sáng",
          description = "Không tắt màn hình khi đang mở bản đồ",
          isChecked = settings.autoScreenOn,
          onCheckedChange = { onUpdateSettings(settings.copy(autoScreenOn = it)) }
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // SECTION 4: CHẠY NGẦM & DỮ LIỆU OFFLINE
    SettingsSectionHeader(icon = Icons.Default.DownloadForOffline, title = "CHẠY NGẦM & BẢN ĐỒ OFFLINE")

    Surface(
      shape = RoundedCornerShape(16.dp),
      color = NavLightSurface,
      shadowElevation = 2.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, NavLightCardBorder, RoundedCornerShape(16.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        SettingsToggleRow(
          title = "Chạy ngầm khi tắt màn hình",
          description = "Vẫn phát âm thanh cảnh báo camera để tiết kiệm pin",
          isChecked = settings.backgroundServiceEnabled,
          onCheckedChange = { onUpdateSettings(settings.copy(backgroundServiceEnabled = it)) }
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Offline Packs list
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      offlinePacks.forEach { pack ->
        OfflinePackItemCard(pack = pack, onDownload = { onDownloadPack(pack) })
      }
    }

    Spacer(modifier = Modifier.height(30.dp))
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
    shadowElevation = 1.dp,
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
        Text(
          text = pack.name,
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
          color = NavLightTextPrimary
        )
        Text(
          text = "${pack.cameraCount} camera • ${pack.sizeMb.toInt()} MB",
          style = MaterialTheme.typography.bodySmall,
          color = NavLightTextSecondary
        )
      }
      Button(
        onClick = onDownload,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (pack.isDownloaded) Color(0xFFE2E8F0) else NavRouteBlue,
          contentColor = if (pack.isDownloaded) Color(0xFF475569) else Color.White
        ),
        shape = RoundedCornerShape(10.dp)
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

package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.example.service.FloatingSpeedBubbleService
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
  var showVehicleTypeDialog by remember { mutableStateOf(false) }
  var showLanguageDialog by remember { mutableStateOf(false) }
  val context = LocalContext.current

  // Accordion Expand/Collapse States (Mở sẵn mục 1 và 4 làm điểm nhấn)
  var expandedVoice by remember { mutableStateOf(true) }
  var expandedCameras by remember { mutableStateOf(false) }
  var expandedSpeed by remember { mutableStateOf(false) }
  var expandedMap by remember { mutableStateOf(true) }
  var expandedMountainGps by remember { mutableStateOf(false) }
  var expandedOffline by remember { mutableStateOf(false) }

  val lang = settings.appLanguage
  val isEn = lang.equals("en", ignoreCase = true)

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = null,
              tint = Color(0xFF0284C7),
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = if (isEn) "System Settings" else "Cài Đặt Hệ Thống",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = Color(0xFF0F172A)
            )
          }
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
        .padding(horizontal = 14.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

      // ==========================================
      // NHÓM 1: ÂM THANH & GIỌNG NÓI CẢNH BÁO
      // ==========================================
      AccordionSectionCard(
        title = if (isEn) "Voice & Audio Alerts" else "Âm Thanh & Giọng Nói",
        subtitle = if (settings.voiceAlertsEnabled) "${(settings.voiceVolume * 100).toInt()}% • ${if (isEn) "TTS Active" else "Đang bật"}" else (if (isEn) "Disabled" else "Đã tắt"),
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        iconTint = Color(0xFF0284C7),
        isExpanded = expandedVoice,
        onToggle = { expandedVoice = !expandedVoice }
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // Bật/Tắt giọng nói
          SettingsSwitchRow(
            icon = Icons.Default.SpatialAudio,
            title = if (isEn) "Voice Alerts" else "Giọng nói cảnh báo",
            subtitle = if (isEn) "Speaks speed cameras and hazard prompts" else "Đọc cảnh báo camera, quá tốc độ và biển báo",
            checked = settings.voiceAlertsEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(voiceAlertsEnabled = it)) }
          )

          if (settings.voiceAlertsEnabled) {
            RowDivider()
            // Volume Slider
            Text(
              text = "${if (isEn) "Voice Volume" else "Âm lượng giọng nói"}: ${(settings.voiceVolume * 100).toInt()}%",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = Color(0xFF334155)
            )
            Slider(
              value = settings.voiceVolume,
              onValueChange = { onUpdateSettings(settings.copy(voiceVolume = it)) },
              valueRange = 0.1f..1.0f,
              steps = 8,
              colors = SliderDefaults.colors(
                thumbColor = Color(0xFF0284C7),
                activeTrackColor = Color(0xFF0284C7),
                inactiveTrackColor = Color(0xFFE2E8F0)
              )
            )

            RowDivider()
            // Tốc độ đọc TTS
            Text(
              text = if (isEn) "Speech Rate" else "Tốc độ đọc giọng nói",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
              color = Color(0xFF334155)
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              val rates = listOf(
                Pair(0.85f, if (isEn) "Slow (0.85x)" else "Chậm (0.85x)"),
                Pair(1.0f, if (isEn) "Normal (1.0x)" else "Chuẩn (1.0x)"),
                Pair(1.2f, if (isEn) "Fast (1.2x)" else "Nhanh (1.2x)")
              )
              rates.forEach { (rate, label) ->
                val isSelected = (settings.speechRate - rate) in -0.05f..0.05f
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = if (isSelected) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                  modifier = Modifier
                    .weight(1f)
                    .clickable { onUpdateSettings(settings.copy(speechRate = rate)) }
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                  ) {
                    Text(
                      text = label,
                      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                      color = if (isSelected) Color.White else Color(0xFF475569)
                    )
                  }
                }
              }
            }

            RowDivider()
            // Tiếng bíp Chime
            SettingsSwitchRow(
              icon = Icons.Default.NotificationsActive,
              title = if (isEn) "Chime Beep On Alert" else "Tiếng bíp 'Ting' cảnh báo",
              subtitle = if (isEn) "Short audio chime before speaking" else "Phát âm thanh bíp trước khi đọc giọng nói",
              checked = settings.chimeOnAlert,
              onCheckedChange = { onUpdateSettings(settings.copy(chimeOnAlert = it)) }
            )

            // Test Voice Button
            OutlinedButton(
              onClick = onTestVoice,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF0284C7))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isEn) "Test Voice Output" else "Thử Giọng Nói Cảnh Báo",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF0284C7)
              )
            }
          }
        }
      }

      // ==========================================
      // NHÓM 2: BỘ LỌC CAMERA & PHẠT NGUỘI
      // ==========================================
      AccordionSectionCard(
        title = if (isEn) "Camera & Penalty Filter" else "Bộ Lọc Camera & Phạt Nguội",
        subtitle = if (isEn) "Toggle speed, red light & lane cameras" else "Bật/tắt quét từng loại camera giám sát",
        icon = Icons.Default.Videocam,
        iconTint = Color(0xFFDC2626),
        isExpanded = expandedCameras,
        onToggle = { expandedCameras = !expandedCameras }
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SettingsSwitchRow(
            icon = Icons.Default.Speed,
            title = if (isEn) "Speed Cameras" else "Camera bắn tốc độ cố định",
            subtitle = if (isEn) "Alert speed enforcement points" else "Cảnh báo vị trí đo tốc độ tự động",
            checked = settings.showSpeedCamerasOnMap,
            onCheckedChange = { onUpdateSettings(settings.copy(showSpeedCamerasOnMap = it)) }
          )
          RowDivider()
          SettingsSwitchRow(
            icon = Icons.Default.Traffic,
            title = if (isEn) "Red Light Cameras" else "Camera phạt nguội vượt đèn đỏ",
            subtitle = if (isEn) "Junction red light surveillance" else "Phạt nguội vượt đèn tại giao lộ",
            checked = settings.showRedLightCamerasOnMap,
            onCheckedChange = { onUpdateSettings(settings.copy(showRedLightCamerasOnMap = it)) }
          )
          RowDivider()
          SettingsSwitchRow(
            icon = Icons.Default.AltRoute,
            title = if (isEn) "Lane Enforcement Cameras" else "Camera phạt nguội lấn làn",
            subtitle = if (isEn) "Solid line & wrong lane surveillance" else "Phạt đè vạch liền, đi sai làn đường",
            checked = settings.showSpeedLimitsOnMap,
            onCheckedChange = { onUpdateSettings(settings.copy(showSpeedLimitsOnMap = it)) }
          )
          RowDivider()
          SettingsSwitchRow(
            icon = Icons.Default.Block,
            title = if (isEn) "Motorbike Prohibited Zones" else "Cảnh báo CẤM XE MÁY & Cao tốc",
            subtitle = if (isEn) "Warn before entering prohibited expressways" else "Nhắc nhở giữ làn phải, tránh đi nhầm vào cao tốc",
            checked = settings.showProhibitedZones,
            onCheckedChange = { onUpdateSettings(settings.copy(showProhibitedZones = it)) }
          )
          RowDivider()
          SettingsSwitchRow(
            icon = Icons.Default.Security,
            title = if (isEn) "Security & Order Cameras" else "Camera an ninh & trật tự",
            subtitle = if (isEn) "Public security surveillance cameras" else "Camera giám sát đô thị & trật tự",
            checked = settings.showSecurityCameras,
            onCheckedChange = { onUpdateSettings(settings.copy(showSecurityCameras = it)) }
          )
          RowDivider()
          SettingsSwitchRow(
            icon = Icons.Default.Warning,
            title = if (isEn) "Hazard & School Zones" else "Điểm đen tai nạn & Trường học",
            subtitle = if (isEn) "High accident rate zones and schools" else "Đoạn đường nguy hiểm, khu vực trường học",
            checked = settings.showHazards,
            onCheckedChange = { onUpdateSettings(settings.copy(showHazards = it)) }
          )
          RowDivider()
          SettingsSwitchRow(
            icon = Icons.Default.Group,
            title = if (isEn) "Community Driver Reports" else "Báo cáo chốt CSGT từ tài xế",
            subtitle = if (isEn) "Live speed traps shared by drivers" else "Vị trí kiểm tra tốc độ do cộng đồng chia sẻ",
            checked = settings.showCommunityReportsOnMap,
            onCheckedChange = { onUpdateSettings(settings.copy(showCommunityReportsOnMap = it)) }
          )
        }
      }

      // ==========================================
      // NHÓM 3: TỐC ĐỘ & NGƯỠNG CẢNH BÁO
      // ==========================================
      AccordionSectionCard(
        title = if (isEn) "Speed Limits & Thresholds" else "Tốc Độ & Ngưỡng Cảnh Báo",
        subtitle = "+${settings.speedBufferKmh} km/h • ${settings.alertDistanceMeters}m",
        icon = Icons.Default.Tune,
        iconTint = Color(0xFFF59E0B),
        isExpanded = expandedSpeed,
        onToggle = { expandedSpeed = !expandedSpeed }
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // Bù dung sai tốc độ
          SettingsNavigationRow(
            icon = Icons.Default.WarningAmber,
            title = if (isEn) "Overspeed Buffer" else "Dung sai bù vượt tốc độ",
            valueText = "+${settings.speedBufferKmh} km/h",
            onClick = { showSpeedBufferDialog = true }
          )
          RowDivider()
          // Khoảng cách báo trước
          SettingsNavigationRow(
            icon = Icons.Default.NearMe,
            title = if (isEn) "Alert Distance Ahead" else "Khoảng cách cảnh báo trước",
            valueText = "${settings.alertDistanceMeters}m",
            onClick = { showDistanceDialog = true }
          )
          RowDivider()
          // Quy chuẩn Thông tư
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFEFF6FF),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = if (isEn) "Standard: Circular 31/2019/TT-BGTVT (Urban 50-60 km/h, Highway 80-100 km/h)"
                else "Áp dụng: Thông tư 31/2019/TT-BGTVT (Đô thị 50-60 km/h, Quốc lộ/Cao tốc 80-100 km/h)",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = Color(0xFF1E40AF)
              )
            }
          }
        }
      }

      // ==========================================
      // NHÓM 4: BẢN ĐỒ & HIỂN THỊ SIÊU NÉT HD
      // ==========================================
      AccordionSectionCard(
        title = if (isEn) "Map & Visual Display" else "Bản Đồ & Hiển Thị Siêu Nét",
        subtitle = "${if (settings.darkMapMode) "Dark HUD" else "Google Style HD"} • Icon ${(settings.vehicleIconScale * 100).toInt()}%",
        icon = Icons.Default.Map,
        iconTint = Color(0xFF10B981),
        isExpanded = expandedMap,
        onToggle = { expandedMap = !expandedMap }
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // Kích thước Icon xe
          Text(
            text = "${if (isEn) "Vehicle Marker Size" else "Kích thước biểu tượng xe"}: ${when {
              settings.vehicleIconScale >= 2.0f -> if (isEn) "Max (2.2x)" else "Cực đại (2.2x)"
              settings.vehicleIconScale >= 1.7f -> if (isEn) "Extra Large (1.85x)" else "Rất to (1.85x)"
              settings.vehicleIconScale >= 1.4f -> if (isEn) "Large (1.5x)" else "To (1.5x)"
              settings.vehicleIconScale >= 1.1f -> if (isEn) "Medium (1.2x)" else "Vừa (1.2x)"
              else -> if (isEn) "Small (0.9x)" else "Nhỏ (0.9x)"
            }}",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color(0xFF334155)
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            val sizes = listOf(
              Pair(0.9f, if (isEn) "0.9x" else "Nhỏ"),
              Pair(1.2f, if (isEn) "1.2x" else "Vừa"),
              Pair(1.5f, if (isEn) "1.5x" else "To"),
              Pair(1.85f, if (isEn) "1.85x" else "Rất to"),
              Pair(2.2f, if (isEn) "2.2x" else "Cực đại")
            )
            sizes.forEach { (scale, label) ->
              val isSelected = (settings.vehicleIconScale - scale) in -0.09f..0.09f
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                modifier = Modifier
                  .weight(1f)
                  .clickable { onUpdateSettings(settings.copy(vehicleIconScale = scale)) }
              ) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier.padding(vertical = 8.dp)
                ) {
                  Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                      fontSize = 11.sp
                    ),
                    color = if (isSelected) Color.White else Color(0xFF475569)
                  )
                }
              }
            }
          }

          RowDivider()
          // Chọn loại biểu tượng xe
          SettingsNavigationRow(
            icon = Icons.Default.TwoWheeler,
            title = if (isEn) "3D Vehicle Icon Model" else "Mô hình xe",
            valueText = when (settings.vehicleIconType) {
              "SCOOTER" -> "🛵 ${if (isEn) "3D Scooter" else "Xe tay ga"}"
              "MOTORBIKE" -> "🏍️ ${if (isEn) "3D Superbike" else "Xe phân khối lớn"}"
              "CAR" -> "🚗 ${if (isEn) "3D Sport Car" else "Xe ô tô thể thao"}"
              "TRUCK" -> "🚛 ${if (isEn) "Truck" else "Xe tải"}"
              else -> "🔺 ${if (isEn) "Neon Cyan Arrow" else "Mũi tên Neon Cyan"}"
            },
            onClick = { showVehicleTypeDialog = true }
          )

          RowDivider()
          // Góc nhìn 3D Tilt
          SettingsSwitchRow(
            icon = Icons.Default.ViewInAr,
            title = if (isEn) "3D Perspective Camera Tilt" else "Góc nhìn 3D Tilt nghiêng phối cảnh",
            subtitle = if (isEn) "Tilts map camera forward to see upcoming cameras (Google Maps 3D)" else "Nghiêng bản đồ về phía trước giúp quan sát xa cung đường và camera",
            checked = settings.mapCameraTilt3D,
            onCheckedChange = { onUpdateSettings(settings.copy(mapCameraTilt3D = it)) }
          )

          RowDivider()
          // Bong bóng nổi Google Maps
          SettingsSwitchRow(
            icon = Icons.Default.Layers,
            title = if (isEn) "Floating HUD Over Google Maps" else "Bong bóng tốc độ nổi đè lên Google Maps",
            subtitle = if (isEn) "Mini floating speed bubble while using navigation apps" else "Hiển thị đồng hồ tốc độ và cảnh báo camera mini nổi trên màn hình",
            checked = settings.floatingBubbleEnabled,
            onCheckedChange = { isEnabled ->
              if (isEnabled) {
                onUpdateSettings(settings.copy(floatingBubbleEnabled = true))
                if (!FloatingSpeedBubbleService.canDrawOverlay(context)) {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val intent = Intent(
                      Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                      Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                  }
                } else {
                  FloatingSpeedBubbleService.startService(context)
                }
              } else {
                FloatingSpeedBubbleService.stopService(context)
                onUpdateSettings(settings.copy(floatingBubbleEnabled = false))
              }
            }
          )

          RowDivider()
          // Bản đồ ban đêm Dark HUD
          SettingsSwitchRow(
            icon = Icons.Default.DarkMode,
            title = if (isEn) "Night HUD Mode" else "Chế độ Bản đồ Ban đêm (Dark HUD)",
            subtitle = if (isEn) "High-contrast dark palette for night driving" else "Giao diện tối chống chói mắt khi lái xe ban đêm",
            checked = settings.darkMapMode,
            onCheckedChange = { onUpdateSettings(settings.copy(darkMapMode = it)) }
          )

          RowDivider()
          // Bản đồ Retina HD
          SettingsSwitchRow(
            icon = Icons.Default.HighQuality,
            title = if (isEn) "Retina HD 512px Tiles" else "Bản đồ Retina HD Siêu Nét (@2x)",
            subtitle = if (isEn) "High-DPI sharp rendering for high-resolution screens" else "Hiển thị chữ và mặt đường sắc nét không bị mờ vỡ hạt",
            checked = settings.highDpiMapEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(highDpiMapEnabled = it)) }
          )

          RowDivider()
          // La bàn số & Vết đi
          SettingsSwitchRow(
            icon = Icons.Default.Explore,
            title = if (isEn) "Compass Rotation" else "La bàn số xoay theo hướng xe",
            subtitle = if (isEn) "Auto-rotate map in Track-Up mode" else "Tự động xoay bản đồ theo hướng di chuyển",
            checked = settings.compassEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(compassEnabled = it)) }
          )

          RowDivider()
          SettingsSwitchRow(
            icon = Icons.Default.Timeline,
            title = if (isEn) "Show Breadcrumbs Trail" else "Hiện vệt đường đã đi (Breadcrumbs)",
            subtitle = if (isEn) "Visual blue trail of recent travel" else "Hiển thị vệt xanh hành trình đã đi qua",
            checked = settings.showBreadcrumbs,
            onCheckedChange = { onUpdateSettings(settings.copy(showBreadcrumbs = it)) }
          )

          RowDivider()
          // Ngôn ngữ
          SettingsNavigationRow(
            icon = Icons.Default.Language,
            title = if (isEn) "Language" else "Ngôn ngữ ứng dụng",
            valueText = if (isEn) "English 🇬🇧" else "Tiếng Việt 🇻🇳",
            onClick = { showLanguageDialog = true }
          )
        }
      }

      // ==========================================
      // NHÓM 5: BÁM TIM ĐƯỜNG & GPS ĐÈO DỐC
      // ==========================================
      AccordionSectionCard(
        title = if (isEn) "Mountain Pass & Road Snapping" else "Bám Tim Đường & GPS Đèo Dốc",
        subtitle = if (isEn) "Centerline snap & Dead Reckoning" else "Khớp tim đường, chống trôi GPS mép đường",
        icon = Icons.Default.Terrain,
        iconTint = Color(0xFF8B5CF6),
        isExpanded = expandedMountainGps,
        onToggle = { expandedMountainGps = !expandedMountainGps }
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          SettingsSwitchRow(
            icon = Icons.Default.Polyline,
            title = if (isEn) "Centerline Road Snapping" else "Chế độ Bám Tim Đường Thông Minh",
            subtitle = if (isEn) "Snaps vehicle marker smoothly onto roadway on wide avenues" else "Ghim xe chính giữa làn đường (như Google Maps), không bị trôi ra mép cỏ hay dải phân cách",
            checked = settings.roadSnappingEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(roadSnappingEnabled = it)) }
          )
          RowDivider()
          SettingsSwitchRow(
            icon = Icons.Default.ScreenLockPortrait,
            title = if (isEn) "Keep Screen On" else "Giữ màn hình luôn sáng khi lái xe",
            subtitle = if (isEn) "Prevents phone from sleeping during driving" else "Không tắt màn hình khi đang di chuyển",
            checked = settings.autoScreenOn,
            onCheckedChange = { onUpdateSettings(settings.copy(autoScreenOn = it)) }
          )
          RowDivider()
          SettingsSwitchRow(
            icon = Icons.Default.Sensors,
            title = if (isEn) "Background Floating Service" else "Cảnh báo chạy nền khi ẩn app",
            subtitle = if (isEn) "Runs in background while using Google Maps" else "Vẫn phát cảnh báo âm thanh khi dùng Google Maps hoặc tắt màn hình",
            checked = settings.backgroundServiceEnabled,
            onCheckedChange = { onUpdateSettings(settings.copy(backgroundServiceEnabled = it)) }
          )
        }
      }

      // ==========================================
      // NHÓM 6: BẢN ĐỒ NGOẠI TUYẾN & DỮ LIỆU
      // ==========================================
      AccordionSectionCard(
        title = if (isEn) "Offline Maps & 63 Provinces" else "Bản Đồ Ngoại Tuyến 63 Tỉnh Thành",
        subtitle = "${offlinePacks.count { it.isDownloaded }} ${if (isEn) "Downloaded" else "đã tải"}",
        icon = Icons.Default.DownloadForOffline,
        iconTint = Color(0xFF0284C7),
        isExpanded = expandedOffline,
        onToggle = { expandedOffline = !expandedOffline }
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          offlinePacks.forEach { pack ->
            OfflinePackRow(pack = pack, onDownload = { onDownloadPack(pack) })
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }

  // ==========================================
  // POPUP DIALOGS
  // ==========================================
  if (showSpeedBufferDialog) {
    SpeedBufferDialog(
      currentBuffer = settings.speedBufferKmh,
      onSelect = { onUpdateSettings(settings.copy(speedBufferKmh = it)); showSpeedBufferDialog = false },
      onDismiss = { showSpeedBufferDialog = false },
      isEn = isEn
    )
  }

  if (showDistanceDialog) {
    DistanceSelectDialog(
      currentDistance = settings.alertDistanceMeters,
      onSelect = { onUpdateSettings(settings.copy(alertDistanceMeters = it)); showDistanceDialog = false },
      onDismiss = { showDistanceDialog = false },
      isEn = isEn
    )
  }

  if (showVehicleTypeDialog) {
    VehicleTypeSelectDialog(
      currentType = settings.vehicleIconType,
      onSelect = { onUpdateSettings(settings.copy(vehicleIconType = it)); showVehicleTypeDialog = false },
      onDismiss = { showVehicleTypeDialog = false },
      isEn = isEn
    )
  }

  if (showLanguageDialog) {
    LanguageSelectDialog(
      currentLang = settings.appLanguage,
      onSelect = { onUpdateSettings(settings.copy(appLanguage = it)); showLanguageDialog = false },
      onDismiss = { showLanguageDialog = false }
    )
  }
}

// ==========================================
// ACCORDION CARD COMPONENT
// ==========================================
@Composable
fun AccordionSectionCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  isExpanded: Boolean,
  onToggle: () -> Unit,
  content: @Composable () -> Unit
) {
  val arrowRotation by animateFloatAsState(
    targetValue = if (isExpanded) 180f else 0f,
    animationSpec = tween(280),
    label = "ChevronRotation"
  )

  Surface(
    shape = RoundedCornerShape(18.dp),
    color = Color.White,
    shadowElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .border(0.8.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
  ) {
    Column {
      // Header (Luôn hiển thị)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onToggle)
          .padding(horizontal = 16.dp, vertical = 14.dp)
      ) {
        Surface(
          shape = CircleShape,
          color = iconTint.copy(alpha = 0.12f),
          modifier = Modifier.size(38.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
          }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
            color = Color(0xFF0F172A)
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = Color(0xFF64748B)
          )
        }
        Icon(
          imageVector = Icons.Default.KeyboardArrowDown,
          contentDescription = null,
          tint = Color(0xFF64748B),
          modifier = Modifier
            .size(24.dp)
            .rotate(arrowRotation)
        )
      }

      // Expandable Content Body
      AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(animationSpec = tween(280)) + fadeIn(animationSpec = tween(200)),
        exit = shrinkVertically(animationSpec = tween(280)) + fadeOut(animationSpec = tween(150))
      ) {
        Column {
          HorizontalDivider(thickness = 0.8.dp, color = Color(0xFFF1F5F9))
          content()
        }
      }
    }
  }
}

// ==========================================
// REUSABLE ROWS & HELPERS
// ==========================================
@Composable
fun SettingsSwitchRow(
  icon: ImageVector,
  title: String,
  subtitle: String? = null,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onCheckedChange(!checked) }
      .padding(vertical = 4.dp)
  ) {
    Icon(icon, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
    Spacer(modifier = Modifier.width(12.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
        color = Color(0xFF0F172A)
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
          color = Color(0xFF64748B)
        )
      }
    }
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = Color(0xFF0284C7),
        uncheckedThumbColor = Color.White,
        uncheckedTrackColor = Color(0xFFCBD5E1)
      )
    )
  }
}

@Composable
fun SettingsNavigationRow(
  icon: ImageVector,
  title: String,
  valueText: String,
  onClick: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp)
  ) {
    Icon(icon, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(20.dp))
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
      color = Color(0xFF0F172A),
      modifier = Modifier.weight(1f)
    )
    Surface(
      shape = RoundedCornerShape(8.dp),
      color = Color(0xFFF1F5F9)
    ) {
      Text(
        text = valueText,
        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF0284C7)),
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
      )
    }
    Spacer(modifier = Modifier.width(6.dp))
    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
  }
}

@Composable
fun RowDivider() {
  HorizontalDivider(
    thickness = 0.6.dp,
    color = Color(0xFFF1F5F9),
    modifier = Modifier.padding(vertical = 4.dp)
  )
}

@Composable
fun OfflinePackRow(
  pack: OfflineMapPackEntity,
  onDownload: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(12.dp),
    color = Color(0xFFF8FAFC),
    border = androidx.compose.foundation.BorderStroke(0.6.dp, Color(0xFFE2E8F0)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(12.dp)
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = pack.name,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
          color = Color(0xFF0F172A)
        )
        Text(
          text = "${pack.sizeMb} MB • ${pack.cameraCount} cameras • ${pack.version}",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
          color = Color(0xFF64748B)
        )
      }
      if (pack.isDownloaded) {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = Color(0xFFDCFCE7)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Đã lưu",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
            )
          }
        }
      } else {
        FilledTonalButton(
          onClick = onDownload,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = "Tải về", style = MaterialTheme.typography.labelSmall)
        }
      }
    }
  }
}

// ==========================================
// DIALOGS
// ==========================================
@Composable
fun SpeedBufferDialog(
  currentBuffer: Int,
  onSelect: (Int) -> Unit,
  onDismiss: () -> Unit,
  isEn: Boolean
) {
  val options = listOf(
    Pair(0, if (isEn) "0 km/h (Strict - Alert immediately at limit)" else "0 km/h (Tuyệt đối - Báo ngay khi chạm mốc)"),
    Pair(3, if (isEn) "+3 km/h (Safe margin)" else "+3 km/h (Dung sai an toàn đô thị)"),
    Pair(5, if (isEn) "+5 km/h (Vietnamese Traffic Law tolerance)" else "+5 km/h (Dung sai theo Luật Giao thông VN)"),
    Pair(10, if (isEn) "+10 km/h (Expressway buffer)" else "+10 km/h (Dung sai chạy Cao tốc)")
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (isEn) "Overspeed Buffer Tolerance" else "Dung Sai Bù Tốc Độ Cảnh Báo",
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (currentBuffer == value) Color(0xFFEFF6FF) else Color.Transparent,
            border = if (currentBuffer == value) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0284C7)) else null,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelect(value) }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(12.dp)
            ) {
              RadioButton(
                selected = (currentBuffer == value),
                onClick = { onSelect(value) },
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = if (currentBuffer == value) FontWeight.Bold else FontWeight.Normal,
                  color = if (currentBuffer == value) Color(0xFF0284C7) else Color(0xFF1E293B)
                )
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(text = if (isEn) "Cancel" else "Hủy")
      }
    }
  )
}

@Composable
fun DistanceSelectDialog(
  currentDistance: Int,
  onSelect: (Int) -> Unit,
  onDismiss: () -> Unit,
  isEn: Boolean
) {
  val options = listOf(
    Pair(300, if (isEn) "300 meters (Urban / City streets)" else "300 mét (Phù hợp đường nội đô đông đúc)"),
    Pair(500, if (isEn) "500 meters (Standard - Recommended)" else "500 mét (Tiêu chuẩn - Khuyên dùng)"),
    Pair(800, if (isEn) "800 meters (National Highways)" else "800 mét (Phù hợp Quốc lộ & Đường tỉnh)"),
    Pair(1000, if (isEn) "1000 meters (Expressways / High-speed)" else "1000 mét (Cao tốc / Tốc độ cao 100-120 km/h)")
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (isEn) "Alert Distance Ahead" else "Khoảng Cách Cảnh Báo Trước",
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (currentDistance == value) Color(0xFFEFF6FF) else Color.Transparent,
            border = if (currentDistance == value) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0284C7)) else null,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelect(value) }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(12.dp)
            ) {
              RadioButton(
                selected = (currentDistance == value),
                onClick = { onSelect(value) },
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontWeight = if (currentDistance == value) FontWeight.Bold else FontWeight.Normal,
                  color = if (currentDistance == value) Color(0xFF0284C7) else Color(0xFF1E293B)
                )
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(text = if (isEn) "Cancel" else "Hủy")
      }
    }
  )
}

@Composable
fun VehicleTypeSelectDialog(
  currentType: String,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit,
  isEn: Boolean
) {
  val options = listOf(
    Pair("SCOOTER", "🛵 ${if (isEn) "3D Scooter Rider (Google Maps style)" else "Xe Tay Ga"}"),
    Pair("MOTORBIKE", "🏍️ ${if (isEn) "3D Sport Motorbike" else "Xe Phân Khối Lớn"}"),
    Pair("CAR", "🚗 ${if (isEn) "3D Sport Car Sedan" else "Xe Ô Tô"}"),
    Pair("TRUCK", "🚛 ${if (isEn) "Truck / Heavy Vehicle" else "Xe Tải"}")
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (isEn) "Select 3D Vehicle Model" else "Chọn Mô Hình Xe 3D Trên Bản Đồ",
        fontWeight = FontWeight.Bold
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (type, label) ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (currentType == type) Color(0xFFEFF6FF) else Color.Transparent,
            border = if (currentType == type) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0284C7)) else null,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelect(type) }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(12.dp)
            ) {
              RadioButton(
                selected = (currentType == type),
                onClick = { onSelect(type) },
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                  fontWeight = if (currentType == type) FontWeight.Bold else FontWeight.Normal,
                  color = if (currentType == type) Color(0xFF0284C7) else Color(0xFF1E293B)
                )
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(text = if (isEn) "Cancel" else "Hủy")
      }
    }
  )
}

@Composable
fun LanguageSelectDialog(
  currentLang: String,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val options = listOf(
    Pair("vi", "Tiếng Việt 🇻🇳"),
    Pair("en", "English 🇬🇧")
  )

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(text = "Ngôn ngữ / Language", fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (langCode, label) ->
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (currentLang == langCode) Color(0xFFEFF6FF) else Color.Transparent,
            border = if (currentLang == langCode) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0284C7)) else null,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelect(langCode) }
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(12.dp)
            ) {
              RadioButton(
                selected = (currentLang == langCode),
                onClick = { onSelect(langCode) },
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                  fontWeight = if (currentLang == langCode) FontWeight.Bold else FontWeight.Normal,
                  color = if (currentLang == langCode) Color(0xFF0284C7) else Color(0xFF1E293B)
                )
              )
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(text = "Đóng") }
    }
  )
}

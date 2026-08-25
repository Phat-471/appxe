package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VietnamTrafficData
import com.example.data.model.CameraType
import com.example.data.model.DestinationPlace
import com.example.data.model.GpsLocationState
import com.example.data.model.TrafficCamera
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraDetailBottomSheet(
  camera: TrafficCamera?,
  currentLocation: GpsLocationState,
  onDismiss: () -> Unit,
  onStartNavigation: (DestinationPlace) -> Unit,
  onSpeakCamera: (String) -> Unit
) {
  if (camera == null) return

  val distMeters = VietnamTrafficData.calculateDistanceMeters(
    currentLocation.latitude, currentLocation.longitude,
    camera.latitude, camera.longitude
  ).toInt()

  val (typeName, penaltyInfo) = when (camera.type) {
    CameraType.SPEED_CAMERA -> "Camera Bắn Tốc Độ Tự Động" to "Phạt quá tốc độ: 800.000đ - 12.000.000đ, tước GPLX 1-4 tháng theo NĐ 100/123/NĐ-CP."
    CameraType.RED_LIGHT_CAMERA -> "Camera Phạt Nguội Vượt Đèn Đỏ" to "Phạt vượt đèn đỏ/vàng: 4.000.000đ - 6.000.000đ (ô tô) / 800.000đ - 1.000.000đ (xe máy)."
    CameraType.COLD_FINE_SURVEILLANCE -> "Camera Phạt Nguội Lấn Làn & Đè Vạch" to "Phạt đi sai làn, đè vạch liền: 3.000.000đ - 5.000.000đ (ô tô) / 400.000đ - 600.000đ (xe máy)."
    CameraType.SECURITY_MONITORING -> "Camera An Ninh & Giám Sát Đô Thị" to "Camera an ninh trật tự công an và quan sát luồng giao thông VOV. Không đo phạt bắn tốc độ."
    CameraType.ZONE_RESIDENTIAL_ENTRY -> "Biển Báo Khu Đông Dân Cư (R.420)" to "Tốc độ tối đa quy định: 50 km/h (đường đôi/1 chiều 2 làn) hoặc 60 km/h theo TT 31/2019."
    CameraType.ZONE_RESIDENTIAL_EXIT -> "Hết Khu Đông Dân Cư (R.421)" to "Được phép tăng tốc độ lên tối đa 60 km/h (xe máy) / 80 km/h (ô tô ngoài đô thị)."
    CameraType.HAZARD_ACCIDENT_ZONE -> "Đoạn Đường Thường Xảy Ra Tai Nạn" to "Khu vực điểm đen giao thông, giảm tốc độ và chú ý quan sát xe qua lại."
    CameraType.MOTORBIKE_PROHIBITED_ZONE -> "Đường Cấm Xe Máy / Lối Vào Cao Tốc" to "CẤM XE MÁY ĐI VÀO! Phạt 2.000.000đ - 3.000.000đ và tước GPLX 3 - 5 tháng theo NĐ 100/2019/NĐ-CP."
    CameraType.SCHOOL_ZONE -> "Khu Vực Trường Học Trọng Điểm" to "Giảm tốc độ, chú ý học sinh sang đường và tuân thủ vạch kẻ người đi bộ."
    CameraType.SPEED_LIMIT_SIGN -> "Biển Báo Giới Hạn Tốc Độ (P.127)" to "Tốc độ tối đa cho phép ${camera.speedLimit} km/h theo quy chuẩn QCVN 41:2019/BGTVT."
    CameraType.COMMUNITY_REPORT -> "Chốt Kiểm Tra Tốc Độ Do Tài Xế Báo" to "Cộng đồng lái xe đóng góp thông tin chốt tuần tra và camera cơ động."
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF0F172A),
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(vertical = 10.dp)
          .size(width = 40.dp, height = 4.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(Color(0xFF475569))
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Header with Visual Badge & Title
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        CameraVisualBadge(camera = camera, size = 64.dp)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = typeName,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = Color.White
          )
          Text(
            text = camera.roadName,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF38BDF8)
          )
          Text(
            text = camera.districtCity,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
          )
        }
      }

      // 2. Info Cards Grid (Speed Limit, Distance, Coordinates)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        CameraInfoCard(
          icon = Icons.Default.Speed,
          title = "Giới hạn",
          value = "${camera.speedLimit} km/h",
          color = Color(0xFFEF4444),
          modifier = Modifier.weight(1f)
        )
        CameraInfoCard(
          icon = Icons.Default.NearMe,
          title = "Khoảng cách",
          value = if (distMeters >= 1000) String.format(java.util.Locale.US, "%.1f km", distMeters / 1000f) else "$distMeters m",
          color = Color(0xFF10B981),
          modifier = Modifier.weight(1f)
        )
        CameraInfoCard(
          icon = Icons.Default.GpsFixed,
          title = "Tọa độ GPS",
          value = String.format(java.util.Locale.US, "%.3f, %.3f", camera.latitude, camera.longitude),
          color = Color(0xFFF59E0B),
          modifier = Modifier.weight(1.2f)
        )
      }

      // 3. Detailed Description & Legal Penalty Box
      Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Chi tiết vị trí & Quy định", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
          }
          Text(text = camera.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
          HorizontalDivider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 4.dp))
          Text(
            text = "⚖️ $penaltyInfo",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = Color(0xFFFBBF24)
          )
        }
      }

      // 4. Tra cứu phạt nguội online button (Cần Internet)
      val context = androidx.compose.ui.platform.LocalContext.current
      Button(
        onClick = {
          try {
            val browserIntent = android.content.Intent(
              android.content.Intent.ACTION_VIEW,
              android.net.Uri.parse("https://phatnguoi.vn")
            )
            browserIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(browserIntent)
          } catch (e: Exception) {
            /* ignore */
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Tra Cứu Phạt Nguội Trực Tuyến (Online CSGT)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
      }

      // 5. Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Voice preview button
        OutlinedButton(
          onClick = {
            val speech = "Chú ý: Camera ${camera.roadName}, tốc độ tối đa ${camera.speedLimit} kilômét một giờ."
            onSpeakCamera(speech)
          },
          colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1f)
        ) {
          Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Nghe cảnh báo", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        // Navigate button
        Button(
          onClick = {
            onStartNavigation(
              DestinationPlace(
                id = "cam_dest_${camera.id}",
                name = camera.roadName,
                address = "${camera.roadName}, ${camera.districtCity}",
                latitude = camera.latitude,
                longitude = camera.longitude,
                category = "Camera"
              )
            )
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.weight(1.2f)
        ) {
          Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Chỉ đường tới đây", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }
    }
  }
}

@Composable
fun CameraInfoCard(
  icon: ImageVector,
  title: String,
  value: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Surface(
    color = Color(0xFF1E293B),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
    modifier = modifier
  ) {
    Column(
      modifier = Modifier.padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp), color = Color(0xFF94A3B8))
      }
      Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
        color = Color.White
      )
    }
  }
}

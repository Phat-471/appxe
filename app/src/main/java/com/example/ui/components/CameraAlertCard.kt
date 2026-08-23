package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveWarning
import com.example.data.model.CameraType
import com.example.data.model.WarningLevel
import com.example.ui.theme.*

@Composable
fun CameraAlertCard(
  activeWarning: ActiveWarning?,
  modifier: Modifier = Modifier
) {
  AnimatedVisibility(
    visible = activeWarning != null,
    enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
    exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut(),
    modifier = modifier
  ) {
    if (activeWarning != null) {
      val cam = activeWarning.camera
      val isDanger = activeWarning.warningLevel == WarningLevel.DANGER || activeWarning.isOverspeeding
      val borderColor = if (isDanger) AlertCrimsonDanger else AlertAmberPrimary
      val bgColor = if (isDanger) AlertCrimsonDanger.copy(alpha = 0.08f) else AlertAmberPrimary.copy(alpha = 0.08f)

      val (icon, typeTitle) = when (cam.type) {
        CameraType.SPEED_CAMERA -> Icons.Default.Videocam to "CAMERA BẮN TỐC ĐỘ"
        CameraType.RED_LIGHT_CAMERA -> Icons.Default.Traffic to "PHẠT NGUỘI VƯỢT ĐÈN ĐỎ"
        CameraType.COLD_FINE_SURVEILLANCE -> Icons.Default.CameraAlt to "CAMERA PHẠT NGUỘI LẤN LÀN"
        CameraType.SECURITY_MONITORING -> Icons.Default.Security to "CAMERA AN NINH & GIÁM SÁT"
        CameraType.ZONE_RESIDENTIAL_ENTRY -> Icons.Default.LocationCity to "BẮT ĐẦU KHU DÂN CƯ"
        CameraType.ZONE_RESIDENTIAL_EXIT -> Icons.Default.DirectionsCar to "HẾT KHU DÂN CƯ"
        CameraType.HAZARD_ACCIDENT_ZONE -> Icons.Default.Warning to "ĐOẠN ĐƯỜNG NGUY HIỂM"
        CameraType.SCHOOL_ZONE -> Icons.Default.School to "KHU VỰC TRƯỜNG HỌC"
        CameraType.SPEED_LIMIT_SIGN -> Icons.Default.Speed to "BIỂN BÁO TỐC ĐỘ"
        CameraType.COMMUNITY_REPORT -> Icons.Default.Report to "CHỐT BÁO CỘNG ĐỒNG"
      }

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = NavLightSurface,
        shadowElevation = 4.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(2.dp, borderColor, RoundedCornerShape(16.dp))
          .testTag("camera_alert_card")
      ) {
        Column(
          modifier = Modifier
            .background(bgColor)
            .padding(14.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            // Authentic Camera Visual Badge
            CameraVisualBadge(camera = cam, size = 46.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = typeTitle,
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Black,
                  letterSpacing = 0.5.sp
                ),
                color = if (isDanger) AlertCrimsonDanger else AlertAmberDark
              )
              Text(
                text = cam.roadName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = NavLightTextPrimary,
                maxLines = 1
              )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Distance Countdown Display
            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "${activeWarning.distanceMeters}m",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontWeight = FontWeight.Black,
                  fontSize = 22.sp
                ),
                color = if (isDanger) AlertCrimsonDanger else AlertAmberDark
              )
              Text(
                text = "Tối đa ${cam.speedLimit}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = NavLightTextSecondary
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Distance Progress Indicator (closer = full bar)
          val progress = (1f - (activeWarning.distanceMeters / 600f)).coerceIn(0.1f, 1.0f)
          LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = if (isDanger) AlertCrimsonDanger else AlertAmberPrimary,
            trackColor = Color(0xFFE2E8F0),
          )

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = cam.description,
            style = MaterialTheme.typography.bodySmall,
            color = NavLightTextSecondary,
            maxLines = 2
          )
        }
      }
    }
  }
}

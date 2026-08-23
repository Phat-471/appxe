package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CurrentTripStats
import com.example.ui.theme.*

import java.util.Locale

@Composable
fun TripStatsCard(
  tripStats: CurrentTripStats,
  isRecording: Boolean,
  modifier: Modifier = Modifier
) {
  val minutes = tripStats.durationSeconds / 60
  val seconds = tripStats.durationSeconds % 60
  val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = NavLightSurface,
    shadowElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, NavLightCardBorder, RoundedCornerShape(16.dp))
      .testTag("trip_stats_card")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          if (isRecording) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(AlertCrimsonDanger)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "ĐANG GHI LỘ TRÌNH",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = AlertCrimsonDanger
            )
          } else {
            Text(
              text = "THÔNG SỐ CHUYẾN ĐI HIỆN TẠI",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = NavLightTextSecondary
            )
          }
        }

        if (tripStats.overspeedEvents > 0) {
          Text(
            text = "⚠️ ${tripStats.overspeedEvents} lần quá tốc độ",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = AlertAmberDark
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Quãng đường
        StatMetricItem(
          label = "Quãng đường",
          value = String.format(Locale.US, "%.1f", tripStats.distanceKm),
          unit = "km",
          modifier = Modifier.weight(1f)
        )

        // Thời gian
        StatMetricItem(
          label = "Thời gian",
          value = timeFormatted,
          unit = "phút",
          modifier = Modifier.weight(1f)
        )

        // Tốc độ cao nhất
        StatMetricItem(
          label = "Tốc độ Max",
          value = tripStats.maxSpeedKmh.toInt().toString(),
          unit = "km/h",
          modifier = Modifier.weight(1f)
        )

        // Tốc độ TB
        StatMetricItem(
          label = "Tốc độ TB",
          value = tripStats.avgSpeedKmh.toInt().toString(),
          unit = "km/h",
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

@Composable
private fun StatMetricItem(
  label: String,
  value: String,
  unit: String,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = NavLightTextTertiary
    )
    Spacer(modifier = Modifier.height(2.dp))
    Row(verticalAlignment = Alignment.Bottom) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        ),
        color = NavLightTextPrimary
      )
      Spacer(modifier = Modifier.width(2.dp))
      Text(
        text = unit,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = NavLightTextSecondary
      )
    }
  }
}

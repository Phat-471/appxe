package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CloudSyncStatus
import com.example.data.model.TripSummary
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TripHistoryScreen(
  trips: List<TripSummary>,
  cloudSyncStatus: CloudSyncStatus,
  onSyncCloud: () -> Unit,
  onDeleteTrip: (Long) -> Unit,
  modifier: Modifier = Modifier
) {
  val syncDateFormat = remember { SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault()) }
  val lastSyncedText = syncDateFormat.format(Date(cloudSyncStatus.lastSyncedTimeMillis))

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(NavLightBackground)
      .padding(16.dp)
  ) {
    // Header
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column {
        Text(
          text = "NHẬT KÝ LỘ TRÌNH",
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          ),
          color = NavRouteBlue
        )
        Text(
          text = "Lịch Sử Chuyến Đi",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = NavLightTextPrimary
        )
      }

      Surface(
        shape = RoundedCornerShape(12.dp),
        color = NavLightSurface,
        shadowElevation = 2.dp,
        modifier = Modifier.border(1.dp, NavLightCardBorder, RoundedCornerShape(12.dp))
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = Icons.Default.TwoWheeler,
            contentDescription = null,
            tint = NavRouteBlue,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "${trips.size} chuyến",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = NavLightTextPrimary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Cloud Sync Status Card
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
        modifier = Modifier.padding(12.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(Color(0xFFE0EDFF)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (cloudSyncStatus.isSyncing) Icons.Default.Sync else Icons.Default.CloudDone,
              contentDescription = null,
              tint = NavRouteBlue,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = if (cloudSyncStatus.isSyncing) "Đang sao lưu đám mây..." else "Đồng bộ đám mây",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = NavLightTextPrimary
            )
            Text(
              text = "Cập nhật: $lastSyncedText",
              style = MaterialTheme.typography.bodySmall,
              color = NavLightTextSecondary
            )
          }
        }

        Button(
          onClick = onSyncCloud,
          enabled = !cloudSyncStatus.isSyncing,
          colors = ButtonDefaults.buttonColors(
            containerColor = NavRouteBlue,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Text(
            text = if (cloudSyncStatus.isSyncing) "Đang đồng bộ..." else "Đồng bộ ngay",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // List of Trips
    if (trips.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.Route,
            contentDescription = null,
            tint = NavLightTextTertiary,
            modifier = Modifier.size(48.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Chưa có chuyến đi nào được ghi nhận",
            style = MaterialTheme.typography.bodyMedium,
            color = NavLightTextSecondary
          )
          Text(
            text = "Bấm 'Bắt đầu chuyến đi' trên bản đồ để tự động ghi lại lộ trình",
            style = MaterialTheme.typography.bodySmall,
            color = NavLightTextTertiary
          )
        }
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f).testTag("trip_history_list")
      ) {
        items(trips, key = { it.id }) { trip ->
          TripHistoryItemCard(trip = trip, onDelete = { onDeleteTrip(trip.id) })
        }
      }
    }
  }
}

@Composable
private fun TripHistoryItemCard(
  trip: TripSummary,
  onDelete: () -> Unit
) {
  val dateFormat = remember { SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()) }
  val dateStr = dateFormat.format(Date(trip.startTimeMillis))
  val durationMin = trip.durationSeconds / 60
  val durationSec = trip.durationSeconds % 60

  Surface(
    shape = RoundedCornerShape(14.dp),
    color = NavLightSurface,
    shadowElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, NavLightCardBorder, RoundedCornerShape(14.dp))
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          Text(
            text = trip.title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = NavLightTextPrimary
          )
          Text(
            text = dateStr,
            style = MaterialTheme.typography.bodySmall,
            color = NavLightTextTertiary
          )
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = "Xóa",
            tint = Color(0xFFEF4444),
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = NavLightCardBorder)
      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        TripDataChip(label = "Quãng đường", value = String.format(Locale.US, "%.1f km", trip.distanceKm))
        TripDataChip(label = "Thời gian", value = String.format(Locale.US, "%d ph %02d s", durationMin, durationSec))
        TripDataChip(label = "Tốc độ TB", value = "${trip.avgSpeedKmh.toInt()} km/h")
        TripDataChip(label = "Tốc độ Max", value = "${trip.maxSpeedKmh.toInt()} km/h")
      }

      if (trip.overspeedEvents > 0) {
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = AlertAmberDark,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = "Phát hiện ${trip.overspeedEvents} lần vượt quá tốc độ trên lộ trình",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = AlertAmberDark
          )
        }
      }
    }
  }
}

@Composable
private fun TripDataChip(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
      color = NavLightTextTertiary
    )
    Text(
      text = value,
      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
      color = NavLightTextPrimary
    )
  }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VietnamTrafficData
import com.example.data.model.DestinationPlace
import com.example.data.model.GpsLocationState
import com.example.data.model.MapPoi
import com.example.data.model.PoiType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoiDetailBottomSheet(
  poi: MapPoi?,
  currentLocation: GpsLocationState,
  onDismiss: () -> Unit,
  onStartNavigation: (DestinationPlace) -> Unit
) {
  if (poi == null) return

  val distMeters = VietnamTrafficData.calculateDistanceMeters(
    currentLocation.latitude, currentLocation.longitude,
    poi.latitude, poi.longitude
  ).toInt()

  val (bgColor, iconVector) = when (poi.type) {
    PoiType.GAS_STATION -> Color(0xFFF97316) to Icons.Default.LocalGasStation
    PoiType.TOLL_BOOTH -> Color(0xFF0284C7) to Icons.Default.Toll
    PoiType.HOSPITAL -> Color(0xFFDC2626) to Icons.Default.LocalHospital
    PoiType.TIRE_REPAIR -> Color(0xFFF59E0B) to Icons.Default.Build
    PoiType.ACCIDENT_HOTSPOT -> Color(0xFFE11D48) to Icons.Default.Warning
    PoiType.BRIDGE -> Color(0xFF64748B) to Icons.Default.LocationOn
    PoiType.REST_STOP -> Color(0xFF10B981) to Icons.Default.LocalParking
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
      // Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(2.dp, Color.White, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = iconVector,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
          )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = poi.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = Color.White
          )
          Text(
            text = poi.type.label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF38BDF8)
          )
          if (poi.subtitle.isNotBlank()) {
            Text(
              text = poi.subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF94A3B8)
            )
          }
        }
      }

      // Quick Info Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        CameraInfoCard(
          icon = Icons.Default.Category,
          title = "Danh mục",
          value = poi.type.label,
          color = bgColor,
          modifier = Modifier.weight(1.2f)
        )
        CameraInfoCard(
          icon = Icons.Default.NearMe,
          title = "Khoảng cách",
          value = if (distMeters >= 1000) String.format(java.util.Locale.US, "%.1f km", distMeters / 1000f) else "$distMeters m",
          color = Color(0xFF10B981),
          modifier = Modifier.weight(1f)
        )
      }

      // Navigate button
      Button(
        onClick = {
          onStartNavigation(
            DestinationPlace(
              id = "poi_dest_${poi.id}",
              name = poi.name,
              address = poi.subtitle.ifBlank { poi.name },
              latitude = poi.latitude,
              longitude = poi.longitude,
              category = poi.type.label
            )
          )
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Dẫn đường đến đây ngay", fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }
    }
  }
}

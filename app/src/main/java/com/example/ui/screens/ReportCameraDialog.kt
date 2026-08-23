package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
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
import androidx.compose.ui.window.Dialog
import com.example.data.model.CameraType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportCameraDialog(
  currentRoadName: String,
  currentSpeedLimit: Int,
  onDismiss: () -> Unit,
  onSubmit: (type: CameraType, road: String, limit: Int, desc: String, city: String) -> Unit
) {
  var selectedType by remember { mutableStateOf(CameraType.SPEED_CAMERA) }
  var roadName by remember { mutableStateOf(currentRoadName) }
  var speedLimit by remember { mutableIntStateOf(currentSpeedLimit) }
  var description by remember { mutableStateOf("") }
  var city by remember { mutableStateOf("TP. Hồ Chí Minh") }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = CockpitSurfaceElevated,
      modifier = Modifier
        .fillMaxWidth()
        .border(1.dp, CockpitCardBorder, RoundedCornerShape(20.dp))
        .testTag("report_camera_dialog")
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // Title Row
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Flag,
              contentDescription = null,
              tint = AlertAmberPrimary,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Báo Cáo Điểm Cảnh Báo",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = CockpitTextPrimary
            )
          }
          IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = "Đóng", tint = CockpitTextSecondary)
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Loại cảnh báo
        Text(
          text = "Loại cảnh báo / Camera:",
          style = MaterialTheme.typography.labelMedium,
          color = CockpitTextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))

        val types = listOf(
          CameraType.SPEED_CAMERA to "Camera Bắn Tốc Độ",
          CameraType.RED_LIGHT_CAMERA to "Phạt Nguội Đèn Đỏ",
          CameraType.COLD_FINE_SURVEILLANCE to "Phạt Nguội Lấn Làn",
          CameraType.ZONE_RESIDENTIAL_ENTRY to "Vào Khu Dân Cư",
          CameraType.HAZARD_ACCIDENT_ZONE to "Đoạn Đường Nguy Hiểm"
        )

        types.forEach { (type, label) ->
          val isSelected = selectedType == type
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) AlertAmberPrimary.copy(alpha = 0.15f) else CockpitSurface)
              .border(
                1.dp,
                if (isSelected) AlertAmberPrimary else CockpitCardBorder,
                RoundedCornerShape(10.dp)
              )
              .clickable { selectedType = type }
              .padding(horizontal = 12.dp, vertical = 10.dp)
          ) {
            RadioButton(
              selected = isSelected,
              onClick = { selectedType = type },
              colors = RadioButtonDefaults.colors(selectedColor = AlertAmberPrimary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = label,
              style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
              ),
              color = if (isSelected) AlertAmberPrimary else CockpitTextPrimary
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tốc độ giới hạn
        Text(
          text = "Tốc độ giới hạn (km/h):",
          style = MaterialTheme.typography.labelMedium,
          color = CockpitTextSecondary
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          listOf(40, 50, 60, 70, 80).forEach { limit ->
            val isSelected = speedLimit == limit
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) AlertAmberPrimary else CockpitSurface)
                .border(
                  1.dp,
                  if (isSelected) AlertAmberPrimary else CockpitCardBorder,
                  RoundedCornerShape(8.dp)
                )
                .clickable { speedLimit = limit }
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "$limit",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) CockpitBackground else CockpitTextPrimary
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tên tuyến đường
        OutlinedTextField(
          value = roadName,
          onValueChange = { roadName = it },
          label = { Text("Tên đoạn đường") },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AlertAmberPrimary,
            unfocusedBorderColor = CockpitCardBorder,
            focusedTextColor = CockpitTextPrimary,
            unfocusedTextColor = CockpitTextPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Ghi chú chi tiết
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Mô tả vị trí (cột mốc, ngã tư...)") },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AlertAmberPrimary,
            unfocusedBorderColor = CockpitCardBorder,
            focusedTextColor = CockpitTextPrimary,
            unfocusedTextColor = CockpitTextPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Submit Button
        Button(
          onClick = {
            val desc = if (description.isBlank()) "Điểm cảnh báo do tài xế đóng góp" else description
            onSubmit(selectedType, roadName, speedLimit, desc, city)
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = AlertAmberPrimary,
            contentColor = CockpitBackground
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("submit_camera_report_button")
        ) {
          Text(
            text = "GỬI BÁO CÁO CỘNG ĐỒNG",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
      }
    }
  }
}

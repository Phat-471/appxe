package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CameraType
import com.example.data.model.TrafficCamera

@Composable
fun CameraVisualBadge(
  camera: TrafficCamera,
  size: Dp = 56.dp,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .size(size)
      .shadow(6.dp, CircleShape),
    contentAlignment = Alignment.Center
  ) {
    when (camera.type) {
      CameraType.SPEED_CAMERA -> {
        SpeedLimitSignBadge(speedLimit = camera.speedLimit, size = size, hasCameraTag = true)
      }
      CameraType.RED_LIGHT_CAMERA -> {
        RedLightCameraBadge(size = size)
      }
      CameraType.COLD_FINE_SURVEILLANCE -> {
        ColdFineCameraBadge(size = size)
      }
      CameraType.ZONE_RESIDENTIAL_ENTRY -> {
        ResidentialZoneBadge(isEntry = true, size = size)
      }
      CameraType.ZONE_RESIDENTIAL_EXIT -> {
        ResidentialZoneBadge(isEntry = false, size = size)
      }
      CameraType.HAZARD_ACCIDENT_ZONE -> {
        HazardWarningBadge(size = size)
      }
      CameraType.SCHOOL_ZONE -> {
        SchoolZoneBadge(size = size)
      }
      CameraType.SPEED_LIMIT_SIGN -> {
        SpeedLimitSignBadge(speedLimit = camera.speedLimit, size = size, hasCameraTag = false)
      }
      CameraType.COMMUNITY_REPORT -> {
        CommunityReportBadge(size = size)
      }
    }
  }
}

@Composable
fun SpeedLimitSignBadge(
  speedLimit: Int,
  size: Dp,
  hasCameraTag: Boolean = true
) {
  Box(
    modifier = Modifier
      .size(size)
      .clip(CircleShape)
      .background(Color.White)
      .border(with(size) { (size.value * 0.12f).dp }, Color(0xFFEF4444), CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = "$speedLimit",
      fontWeight = FontWeight.Black,
      fontSize = (size.value * 0.42f).sp,
      color = Color(0xFF0F172A)
    )

    if (hasCameraTag) {
      Box(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .offset(x = 2.dp, y = 2.dp)
          .size(size * 0.4f)
          .clip(CircleShape)
          .background(Color(0xFF0284C7))
          .border(1.5.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Videocam,
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(size * 0.25f)
        )
      }
    }
  }
}

@Composable
fun RedLightCameraBadge(size: Dp) {
  Box(
    modifier = Modifier
      .size(size)
      .clip(RoundedCornerShape(size * 0.25f))
      .background(Color(0xFF1E293B))
      .border(2.dp, Color(0xFF475569), RoundedCornerShape(size * 0.25f)),
    contentAlignment = Alignment.Center
  ) {
    Column(
      verticalArrangement = Arrangement.SpaceEvenly,
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxHeight(0.85f)
    ) {
      // Red light (glowing)
      Box(
        modifier = Modifier
          .size(size * 0.22f)
          .clip(CircleShape)
          .background(Color(0xFFEF4444))
          .border(1.dp, Color(0xFFFCA5A5), CircleShape)
      )
      // Yellow light
      Box(
        modifier = Modifier
          .size(size * 0.22f)
          .clip(CircleShape)
          .background(Color(0xFFF59E0B))
      )
      // Green light
      Box(
        modifier = Modifier
          .size(size * 0.22f)
          .clip(CircleShape)
          .background(Color(0xFF10B981))
      )
    }

    // Camera badge on corner
    Box(
      modifier = Modifier
        .align(Alignment.TopEnd)
        .size(size * 0.38f)
        .clip(CircleShape)
        .background(Color(0xFFDC2626))
        .border(1.dp, Color.White, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.PhotoCamera,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(size * 0.22f)
      )
    }
  }
}

@Composable
fun ColdFineCameraBadge(size: Dp) {
  Box(
    modifier = Modifier
      .size(size)
      .clip(RoundedCornerShape(size * 0.22f))
      .background(Color(0xFF0284C7))
      .border(2.5.dp, Color.White, RoundedCornerShape(size * 0.22f)),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = Icons.Default.CameraAlt,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(size * 0.55f)
    )
  }
}

@Composable
fun ResidentialZoneBadge(isEntry: Boolean, size: Dp) {
  Box(
    modifier = Modifier
      .size(size)
      .clip(RoundedCornerShape(size * 0.2f))
      .background(Color(0xFF1D4ED8))
      .border(2.dp, Color.White, RoundedCornerShape(size * 0.2f)),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = Icons.Default.LocationCity,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(size * 0.55f)
    )

    if (!isEntry) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        drawLine(
          color = Color(0xFFEF4444),
          start = Offset(0f, size.toPx()),
          end = Offset(size.toPx(), 0f),
          strokeWidth = 3.dp.toPx()
        )
      }
    }
  }
}

@Composable
fun HazardWarningBadge(size: Dp) {
  Box(
    modifier = Modifier.size(size),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width
      val h = this.size.height
      val path = Path().apply {
        moveTo(w / 2f, 2.dp.toPx())
        lineTo(w - 2.dp.toPx(), h - 2.dp.toPx())
        lineTo(2.dp.toPx(), h - 2.dp.toPx())
        close()
      }
      drawPath(path, color = Color(0xFFF59E0B))
      drawPath(path, color = Color(0xFF0F172A), style = Stroke(width = 2.5.dp.toPx()))
    }
    Text(
      text = "!",
      fontWeight = FontWeight.Black,
      fontSize = (size.value * 0.45f).sp,
      color = Color(0xFF0F172A),
      modifier = Modifier.offset(y = (size.value * 0.08f).dp)
    )
  }
}

@Composable
fun SchoolZoneBadge(size: Dp) {
  Box(
    modifier = Modifier
      .size(size)
      .clip(RoundedCornerShape(size * 0.22f))
      .background(Color(0xFFF59E0B))
      .border(2.dp, Color.White, RoundedCornerShape(size * 0.22f)),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = Icons.Default.School,
      contentDescription = null,
      tint = Color(0xFF0F172A),
      modifier = Modifier.size(size * 0.55f)
    )
  }
}

@Composable
fun CommunityReportBadge(size: Dp) {
  Box(
    modifier = Modifier
      .size(size)
      .clip(CircleShape)
      .background(Color(0xFF8B5CF6))
      .border(2.dp, Color.White, CircleShape),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = Icons.Default.Shield,
      contentDescription = null,
      tint = Color.White,
      modifier = Modifier.size(size * 0.55f)
    )
  }
}

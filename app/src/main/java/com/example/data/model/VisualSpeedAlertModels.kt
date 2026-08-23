package com.example.data.model

enum class VisualAlertLevel(val label: String, val colorHex: Long) {
  SAFE("Tốc độ an toàn", 0xFF10B981),              // Green
  NORMAL("Trong giới hạn", 0xFF0284C7),             // Sky Blue
  APPROACHING_LIMIT("Gần tới giới hạn", 0xFFF59E0B), // Amber Yellow (e.g. within 3 km/h of limit)
  WARNING("Vượt tốc độ nhẹ (1-5 km/h)", 0xFFF97316), // Orange
  DANGER("Vượt quá tốc độ (6-15 km/h)", 0xFFEF4444), // Red
  CRITICAL("Vượt quá tốc độ nghiêm trọng (>15 km/h)", 0xFFDC2626) // Deep Flashing Red
}

data class VisualSpeedAlertState(
  val currentSpeedKmh: Int = 0,
  val speedLimitKmh: Int = 50,
  val speedDeltaKmh: Int = -50,
  val isOverspeeding: Boolean = false,
  val alertLevel: VisualAlertLevel = VisualAlertLevel.SAFE,
  val roadName: String = "Đại lộ Võ Văn Kiệt",
  val alertMessage: String = "Tốc độ trong ngưỡng an toàn",
  val isServiceActive: Boolean = false,
  val isGpsLocked: Boolean = false,
  val latitude: Double = 10.7580,
  val longitude: Double = 106.6850,
  val headingDegrees: Float = 65f,
  val timestamp: Long = System.currentTimeMillis()
)

data class MockSpeedZone(
  val id: String,
  val name: String,
  val speedLimitKmh: Int,
  val description: String,
  val centerLat: Double,
  val centerLng: Double,
  val radiusMeters: Double
)

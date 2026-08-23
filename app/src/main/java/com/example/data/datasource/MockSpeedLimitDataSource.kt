package com.example.data.datasource

import com.example.data.VietnamTrafficData
import com.example.data.model.MockSpeedZone
import kotlin.math.*

object MockSpeedLimitDataSource {

  // Preset Mock Speed Zones (for demonstration & simulated or real GPS geofencing)
  val MOCK_SPEED_ZONES = listOf(
    MockSpeedZone(
      id = "zone_school_1",
      name = "Khu Vực Trường Học & Bệnh Viện",
      speedLimitKmh = 30,
      description = "Giới hạn 30 km/h: An toàn học sinh & bệnh viện",
      centerLat = 10.7600,
      centerLng = 106.6900,
      radiusMeters = 350.0
    ),
    MockSpeedZone(
      id = "zone_urban_standard",
      name = "Nội Đô Đô Thị - Đường Phố Chuẩn",
      speedLimitKmh = 50,
      description = "Giới hạn 50 km/h: Khu vực đông dân cư nội thành",
      centerLat = 10.7769,
      centerLng = 106.7009,
      radiusMeters = 1500.0
    ),
    MockSpeedZone(
      id = "zone_boulevard",
      name = "Đại Lộ Có Dải Phân Cách (Võ Văn Kiệt / Phạm Văn Đồng)",
      speedLimitKmh = 60,
      description = "Giới hạn 60 km/h: Trục đại lộ đô thị rộng",
      centerLat = 10.7580,
      centerLng = 106.6850,
      radiusMeters = 4000.0
    ),
    MockSpeedZone(
      id = "zone_national_highway",
      name = "Quốc Lộ 1A / Xa Lộ Hà Nội",
      speedLimitKmh = 80,
      description = "Giới hạn 80 km/h: Đường quốc lộ liên tỉnh",
      centerLat = 10.8500,
      centerLng = 106.7800,
      radiusMeters = 6000.0
    ),
    MockSpeedZone(
      id = "zone_expressway",
      name = "Cao Tốc TP.HCM - Long Thành - Dầu Giây",
      speedLimitKmh = 100,
      description = "Giới hạn 100 km/h: Cao tốc tiêu chuẩn",
      centerLat = 10.7950,
      centerLng = 106.8200,
      radiusMeters = 8000.0
    ),
    MockSpeedZone(
      id = "zone_expressway_fast",
      name = "Cao Tốc Trung Lương / Dầu Giây - Phan Thiết",
      speedLimitKmh = 120,
      description = "Giới hạn 120 km/h: Cao tốc loại A",
      centerLat = 10.9000,
      centerLng = 107.1000,
      radiusMeters = 10000.0
    )
  )

  // Map of road name keywords to mock speed limits
  private val ROAD_SPEED_LIMIT_RULES = mapOf(
    "trường học" to 30,
    "school" to 30,
    "ngõ" to 30,
    "hẻm" to 30,
    "nội bộ" to 30,
    "khu dân cư" to 50,
    "nguyễn huệ" to 50,
    "lê lợi" to 50,
    "nam kỳ khởi nghĩa" to 50,
    "hai bà trưng" to 50,
    "trần hưng đạo" to 50,
    "nguyễn văn a" to 50,
    "nguyễn thị minh khai" to 50,
    "võ văn kiệt" to 60,
    "phạm văn đồng" to 60,
    "nguyễn văn linh" to 60,
    "mai chí thọ" to 60,
    "đại lộ" to 60,
    "xa lộ hà nội" to 80,
    "quốc lộ 1a" to 80,
    "quốc lộ" to 80,
    "cao tốc" to 100,
    "expressway" to 100,
    "long thành" to 100,
    "phan thiết" to 120
  )

  /**
   * Evaluates mock speed limit based on coordinates and detected road name.
   */
  fun getSpeedLimitForLocation(
    latitude: Double,
    longitude: Double,
    roadName: String? = null
  ): Pair<Int, String> {
    // 1. Check if point is inside any specific geofenced MockSpeedZone
    for (zone in MOCK_SPEED_ZONES) {
      val dist = VietnamTrafficData.calculateDistanceMeters(
        latitude, longitude,
        zone.centerLat, zone.centerLng
      )
      if (dist <= zone.radiusMeters) {
        val effectiveName = roadName?.takeIf { it.isNotBlank() } ?: zone.name
        return Pair(zone.speedLimitKmh, effectiveName)
      }
    }

    // 2. Check road name rules
    if (!roadName.isNullOrBlank()) {
      val lowerName = roadName.lowercase()
      for ((keyword, limit) in ROAD_SPEED_LIMIT_RULES) {
        if (lowerName.contains(keyword)) {
          return Pair(limit, roadName)
        }
      }
    }

    // 3. Fallback to standard urban limit
    return Pair(50, roadName ?: "Đường Đô Thị Nội Thành")
  }
}

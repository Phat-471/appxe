package com.example.data.datasource

import com.example.data.VietnamTrafficData
import com.example.data.model.MockSpeedZone
import kotlin.math.*

object MockSpeedLimitDataSource {

  // Preset Mock Speed Zones (for demonstration & simulated or real GPS geofencing)
  // Preset Mock Speed Zones (for demonstration & simulated or real GPS geofencing)
  val MOCK_SPEED_ZONES = listOf(
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
    ),
    MockSpeedZone(
      id = "zone_school_1",
      name = "Khu Vực Trường Học (Nơ Trang Long)",
      speedLimitKmh = 30,
      description = "Giới hạn 30 km/h: Khu vực trường THPT Gia Định",
      centerLat = 10.8150,
      centerLng = 106.6960,
      radiusMeters = 80.0
    )
  )

  // Map of road name keywords to speed limits (Thông tư 31/2019/TT-BGTVT)
  private val ROAD_SPEED_LIMIT_RULES = mapOf(
    "cao tốc" to 100,
    "cao toc" to 100,
    "expressway" to 100,
    "long thành" to 100,
    "trung lương" to 100,
    "phan thiết" to 120,
    "quốc lộ 1a" to 80,
    "quốc lộ" to 80,
    "xa lộ hà nội" to 80,
    "ql1" to 80,
    "ql51" to 80,
    "tỉnh lộ" to 70,
    "đường tỉnh" to 70,
    "võ văn kiệt" to 60,
    "phạm văn đồng" to 60,
    "nguyễn văn linh" to 60,
    "mai chí thọ" to 60,
    "đại lộ" to 60,
    "vành đai" to 60,
    "điện biên phủ" to 60,
    "trường chinh" to 60,
    "nam kỳ khởi nghĩa" to 60,
    "lũy bán bích" to 50,
    "thoại ngọc hầu" to 50,
    "hòa bình" to 50,
    "tân kỳ tân quý" to 50,
    "âu cơ" to 50,
    "lạc long quân" to 50,
    "cách mạng tháng 8" to 50,
    "cmt8" to 50,
    "quang trung" to 50,
    "lê trọng tấn" to 50,
    "phan huy ích" to 50,
    "hoàng văn thụ" to 50,
    "phan đăng lưu" to 50,
    "bạch đằng" to 50,
    "đinh bộ lĩnh" to 50,
    "ba tháng hai" to 50,
    "3 tháng 2" to 50,
    "lý thường kiệt" to 50,
    "nguyễn tri phương" to 50,
    "nguyễn trãi" to 50,
    "hồng bàng" to 50,
    "hậu giang" to 50,
    "minh phụng" to 50,
    "nguyễn huệ" to 50,
    "lê lợi" to 50,
    "hai bà trưng" to 50,
    "trần hưng đạo" to 50,
    "nguyễn thị minh khai" to 50,
    "khu dân cư" to 50,
    "trường học" to 30,
    "school" to 30
  )

  /**
   * Evaluates mock speed limit based on coordinates and detected road name.
   */
  fun getSpeedLimitForLocation(
    latitude: Double,
    longitude: Double,
    roadName: String? = null,
    currentSpeedKmh: Float = 0f
  ): Pair<Int, String> {
    // 1. Check road name rules first (Primary Authority)
    if (!roadName.isNullOrBlank()) {
      val lowerName = roadName.lowercase()
      for ((keyword, limit) in ROAD_SPEED_LIMIT_RULES) {
        if (lowerName.contains(keyword)) {
          // Bỏ qua giới hạn trường học/hẻm nếu xe đang chạy trên 25 km/h
          if (limit <= 30 && currentSpeedKmh > 25f) continue
          return Pair(limit, roadName)
        }
      }
    }

    // 2. Check if point is inside any specific geofenced MockSpeedZone
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

    // 3. Fallback to standard urban limit
    return Pair(50, roadName ?: "Đường Đô Thị Nội Thành")
  }
}

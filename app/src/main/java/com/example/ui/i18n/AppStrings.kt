package com.example.ui.i18n

object AppStrings {
  fun get(key: String, lang: String = "vi"): String {
    val isEn = lang.equals("en", ignoreCase = true)
    return when (key) {
      // Bottom Navigation
      "tab_map" -> if (isEn) "Map & Alerts" else "Bản Đồ & Cảnh Báo"
      "tab_hud" -> if (isEn) "Cockpit HUD" else "Đồng Hồ HUD"
      "tab_settings" -> if (isEn) "Settings" else "Cài Đặt"

      // Search & Headers
      "search_hint" -> if (isEn) "Search destination..." else "Tìm kiếm... (BETA)"
      "recenter_vehicle" -> if (isEn) "Center Map on Vehicle" else "Tâm bản đồ về xe"
      "gps_satellite" -> if (isEn) "GPS Satellite" else "GPS Vệ tinh"
      "simulation_mode" -> if (isEn) "Simulation Mode" else "Mô phỏng thực tế"

      // Filter Chips
      "filter_all" -> if (isEn) "All" else "Tất cả"
      "filter_speed_cam" -> if (isEn) "Speed Cam" else "Bắn tốc độ"
      "filter_red_light" -> if (isEn) "Traffic Light" else "Phạt nguội"
      "filter_security" -> if (isEn) "Security Cam" else "Camera an ninh"

      // Warnings
      "overspeed_danger" -> if (isEn) "OVERSPEED DANGER!" else "VƯỢT TỐC ĐỘ NGUY HIỂM!"
      "overspeed_warning" -> if (isEn) "WARNING: OVERSPEEDING" else "CẢNH BÁO: ĐANG CHẠY QUÁ TỐC ĐỘ"
      "approaching_limit" -> if (isEn) "Approaching Speed Limit" else "Đang tiến sát giới hạn"
      "safe_speed" -> if (isEn) "Safe Speed" else "Tốc độ an toàn"
      "speed_normal" -> if (isEn) "Speed in Regulation" else "Tốc độ trong quy định"
      "prohibited_motorcycle" -> if (isEn) "MOTORCYCLE PROHIBITED ZONE" else "CẤM XE MÁY"
      "max_limit" -> if (isEn) "Limit" else "Tối đa"

      // Settings Screen
      "settings_title" -> if (isEn) "Settings" else "Cài đặt"
      "section_general" -> if (isEn) "General" else "Chung"
      "section_alerts" -> if (isEn) "Alert & Scanner Filters" else "Bộ Lọc Quét & Cảnh Báo"
      "section_voice" -> if (isEn) "Voice Guidance" else "Âm Thanh & Giọng Nói"
      "section_display" -> if (isEn) "Map & Display" else "Bản Đồ & Hiển Thị"
      "section_offline" -> if (isEn) "Offline Maps" else "Bản Đồ Ngoại Tuyến"

      "language" -> if (isEn) "Language" else "Ngôn ngữ"
      "speed_unit" -> if (isEn) "Speed Unit" else "Đơn vị tốc độ"
      "alert_distance" -> if (isEn) "Alert Distance" else "Khoảng cách báo trước"
      "overspeed_buffer" -> if (isEn) "Overspeed Buffer" else "Dung sai quá tốc độ"

      "alert_speed_cam" -> if (isEn) "Speed Cameras" else "Camera bắn tốc độ"
      "alert_speed_cam_desc" -> if (isEn) "Scan and warn upcoming speed cameras" else "Quét và cảnh báo camera bắn tốc độ cố định & di động"
      "alert_red_light" -> if (isEn) "Red Light & Fine Cameras" else "Camera phạt nguội & Vượt đèn đỏ"
      "alert_red_light_desc" -> if (isEn) "Warn traffic light violation surveillance" else "Cảnh báo camera phạt nguội ngã tư & lấn làn"
      "alert_prohibited" -> if (isEn) "Motorcycle Prohibited Roads" else "Đoạn đường cấm xe máy"
      "alert_prohibited_desc" -> if (isEn) "Alert when approaching expressways" else "Cảnh báo khi chạy vào đường cao tốc, cầu cấm xe 2 bánh"
      "alert_security" -> if (isEn) "Security Surveillance Cameras" else "Camera an ninh & Giám sát"
      "alert_security_desc" -> if (isEn) "Urban security cameras" else "Camera an ninh khu phố, trật tự đô thị"
      "alert_hazards" -> if (isEn) "Accident Hotspots & Schools" else "Điểm đen tai nạn & Trường học"
      "alert_hazards_desc" -> if (isEn) "High-risk zones, schools, residential" else "Khu vực nguy hiểm, trường học, đông dân cư"
      "alert_pois" -> if (isEn) "Gas Stations & Repair POIs" else "Cây xăng, Vá xe & Cứu hộ"
      "alert_pois_desc" -> if (isEn) "Show essential utility icons on route" else "Hiển thị trạm xăng, vá xe, trạm thu phí trên bản đồ"
      "alert_community" -> if (isEn) "Community Camera Reports" else "Báo cáo đóng góp cộng đồng"
      "alert_community_desc" -> if (isEn) "User submitted traffic posts" else "Hiển thị điểm báo chốt do người dùng đóng góp"

      "vehicle_icon" -> if (isEn) "Vehicle Icon" else "Biểu tượng xe di chuyển"
      "compass_rotation" -> if (isEn) "Compass Auto-Rotate" else "Tự xoay theo la bàn"
      "dark_mode" -> if (isEn) "Dark Map Mode" else "Chế độ bản đồ tối"
      "keep_screen_on" -> if (isEn) "Keep Screen On" else "Giữ màn hình luôn sáng"

      "voice_enabled" -> if (isEn) "Voice Alerts" else "Bật giọng nói cảnh báo"
      "voice_volume" -> if (isEn) "Voice Volume" else "Âm lượng giọng nói"
      "test_voice" -> if (isEn) "Test Voice Announcement" else "Thử giọng nói mẫu"
      "service_bg" -> if (isEn) "Background GPS Service" else "Dịch vụ chạy nền khi tắt app"

      // HUD Screen
      "hud_mirror_mode" -> if (isEn) "Windshield HUD Mode" else "Chế độ chiếu kính lái HUD"
      "flip_view" -> if (isEn) "Flip" else "Lật hình"
      "exit_hud" -> if (isEn) "Close" else "Đóng"

      else -> key
    }
  }
}

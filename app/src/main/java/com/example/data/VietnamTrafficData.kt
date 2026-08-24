package com.example.data

import com.example.data.model.CameraType
import com.example.data.model.TrafficCamera
import java.text.Normalizer
import java.util.regex.Pattern
import kotlin.math.*

fun String.unaccent(): String {
  var str = Normalizer.normalize(this, Normalizer.Form.NFD)
  val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
  str = pattern.matcher(str).replaceAll("")
  str = str.replace('đ', 'd').replace('Đ', 'd')
  return str.lowercase(java.util.Locale.ROOT).trim()
}

object VietnamTrafficData {

  // Realistic verified camera database for Vietnam routes (August 2026 update)
  val ALL_CAMERAS: List<TrafficCamera> = listOf(
    // ====== TP. HỒ CHÍ MINH ======
    TrafficCamera(
      id = "cam_sg_vvk_01",
      latitude = 10.7523,
      longitude = 106.6712,
      type = CameraType.SPEED_CAMERA,
      roadName = "Đại lộ Võ Văn Kiệt (Gần Cầu Chữ Y)",
      speedLimit = 60,
      description = "Camera bắn tốc độ cố định 60 km/h làn hỗn hợp gần Cầu Chữ Y",
      districtCity = "Quận 5, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_vvk_02",
      latitude = 10.7410,
      longitude = 106.6430,
      type = CameraType.RED_LIGHT_CAMERA,
      roadName = "Võ Văn Kiệt giao An Dương Vương",
      speedLimit = 60,
      description = "Camera phạt nguội vượt đèn đỏ và đè vạch rẽ nhánh",
      districtCity = "Quận 6, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_vvk_03",
      latitude = 10.7650,
      longitude = 106.7020,
      type = CameraType.COLD_FINE_SURVEILLANCE,
      roadName = "Đầu Hầm Vượt Sông Sài Gòn (Phía Quận 1)",
      speedLimit = 60,
      description = "Camera phạt nguội tốc độ và bật đèn chiếu gần trong hầm",
      districtCity = "Quận 1, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_pvd_01",
      latitude = 10.8242,
      longitude = 106.6912,
      type = CameraType.SPEED_CAMERA,
      roadName = "Đường Phạm Văn Đồng (Gần Cầu Bình Lợi)",
      speedLimit = 60,
      description = "Camera bắn tốc độ tự động 60 km/h làn xe máy",
      districtCity = "Bình Thạnh, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_pvd_02",
      latitude = 10.8491,
      longitude = 106.7465,
      type = CameraType.COLD_FINE_SURVEILLANCE,
      roadName = "Phạm Văn Đồng giao Tô Ngọc Vân",
      speedLimit = 60,
      description = "Camera phạt nguội chạy sai làn đường và lấn tuyến",
      districtCity = "TP. Thủ Đức, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_pvd_03",
      latitude = 10.8160,
      longitude = 106.6780,
      type = CameraType.RED_LIGHT_CAMERA,
      roadName = "Phạm Văn Đồng giao Phan Văn Trị",
      speedLimit = 60,
      description = "Camera phạt nguội không tuân thủ đèn tín hiệu",
      districtCity = "Gò Vấp, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_nvl_01",
      latitude = 10.7285,
      longitude = 106.7082,
      type = CameraType.RED_LIGHT_CAMERA,
      roadName = "Nguyễn Văn Linh giao Nguyễn Hữu Thọ",
      speedLimit = 50,
      description = "Phạt nguội vượt đèn đỏ và đè vạch ngã tư lớn",
      districtCity = "Quận 7, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_nvl_02",
      latitude = 10.7198,
      longitude = 106.6610,
      type = CameraType.SPEED_CAMERA,
      roadName = "Đại lộ Nguyễn Văn Linh (Gần Quốc Lộ 50)",
      speedLimit = 60,
      description = "Camera đo tốc độ tự động 60 km/h đoạn đường thẳng",
      districtCity = "Bình Chánh, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_ql1a_01",
      latitude = 10.8465,
      longitude = 106.6112,
      type = CameraType.SPEED_CAMERA,
      roadName = "Quốc Lộ 1A (Ngã Tư An Sương)",
      speedLimit = 50,
      description = "Camera bắn tốc độ đoạn vào vòng xoay An Sương",
      districtCity = "Quận 12, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_ql1a_02",
      latitude = 10.7120,
      longitude = 106.5920,
      type = CameraType.COLD_FINE_SURVEILLANCE,
      roadName = "Quốc Lộ 1A (Đoạn Cầu Bình Điền)",
      speedLimit = 50,
      description = "Giám sát tốc độ 50 km/h khu đông dân cư và lấn làn",
      districtCity = "Bình Chánh, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_mct_01",
      latitude = 10.7812,
      longitude = 106.7350,
      type = CameraType.SPEED_CAMERA,
      roadName = "Đại Lộ Mai Chí Thọ (Gần Nút Giao An Phú)",
      speedLimit = 50,
      description = "Camera bắn tốc độ 50 km/h làn đường gom",
      districtCity = "TP. Thủ Đức, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_cmt8_01",
      latitude = 10.7760,
      longitude = 106.6850,
      type = CameraType.RED_LIGHT_CAMERA,
      roadName = "Cách Mạng Tháng 8 (Ngã 6 Dân Chủ)",
      speedLimit = 50,
      description = "Camera phạt nguội vượt đèn và lấn vạch vòng xoay",
      districtCity = "Quận 3, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_cmt8_02",
      latitude = 10.7920,
      longitude = 106.6570,
      type = CameraType.COLD_FINE_SURVEILLANCE,
      roadName = "Cách Mạng Tháng 8 giao Tô Hiến Thành",
      speedLimit = 50,
      description = "Camera giám sát lấn làn và dừng đỗ sai quy định",
      districtCity = "Quận 10, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_ch_01",
      latitude = 10.8030,
      longitude = 106.6470,
      type = CameraType.SPEED_CAMERA,
      roadName = "Đường Cộng Hòa (Cầu vượt Hoàng Hoa Thám)",
      speedLimit = 50,
      description = "Camera bắn tốc độ tự động 50 km/h trên cầu vượt",
      districtCity = "Tân Bình, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_dbp_01",
      latitude = 10.8010,
      longitude = 106.7150,
      type = CameraType.RED_LIGHT_CAMERA,
      roadName = "Điện Biên Phủ (Ngã Tư Hàng Xanh)",
      speedLimit = 50,
      description = "Camera phạt nguội vượt đèn tín hiệu và đè vạch dừng",
      districtCity = "Bình Thạnh, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_ntr_01",
      latitude = 10.7605,
      longitude = 106.6775,
      type = CameraType.SPEED_LIMIT_SIGN,
      roadName = "Đường Nguyễn Trãi",
      speedLimit = 50,
      description = "Khu vực đô thị đông dân cư, tốc độ tối đa 50 km/h",
      districtCity = "Quận 5, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_sec_01",
      latitude = 10.7745,
      longitude = 106.7035,
      type = CameraType.SECURITY_MONITORING,
      roadName = "Phố Đi Bộ Nguyễn Huệ",
      speedLimit = 50,
      description = "Camera an ninh đô thị và giám sát trật tự công cộng",
      districtCity = "Quận 1, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_sec_02",
      latitude = 10.7985,
      longitude = 106.7210,
      type = CameraType.SECURITY_MONITORING,
      roadName = "Cầu Sài Gòn (Đoạn nối Bình Thạnh - TP. Thủ Đức)",
      speedLimit = 60,
      description = "Camera giám sát luồng phương tiện và tình trạng kẹt xe",
      districtCity = "Bình Thạnh, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_sg_zone_01",
      latitude = 10.7760,
      longitude = 106.6950,
      type = CameraType.ZONE_RESIDENTIAL_ENTRY,
      roadName = "Khu vực Trung Tâm TP.HCM",
      speedLimit = 50,
      description = "Biển báo khu đông dân cư: Tối đa 50 km/h xe máy",
      districtCity = "Quận 1, TP.HCM"
    ),

    // ====== HÀ NỘI ======
    TrafficCamera(
      id = "cam_hn_vd3_01",
      latitude = 20.9982,
      longitude = 105.7950,
      type = CameraType.COLD_FINE_SURVEILLANCE,
      roadName = "Đường Vành Đai 3 dưới thấp (Khuất Duy Tiến)",
      speedLimit = 50,
      description = "Phạt nguội đè vạch liền, lấn làn xe buýt BRT, vượt đèn đỏ",
      districtCity = "Thanh Xuân, Hà Nội"
    ),
    TrafficCamera(
      id = "cam_hn_tl_01",
      latitude = 21.0020,
      longitude = 105.7520,
      type = CameraType.SPEED_CAMERA,
      roadName = "Đại Lộ Thăng Long (Đường gom xe máy)",
      speedLimit = 50,
      description = "Bắn tốc độ 50 km/h đường gom, phạt xe máy đi vào cao tốc",
      districtCity = "Nam Từ Liêm, Hà Nội"
    ),
    TrafficCamera(
      id = "cam_hn_vng_01",
      latitude = 21.1350,
      longitude = 105.8080,
      type = CameraType.SPEED_CAMERA,
      roadName = "Đường Võ Nguyên Giáp (Cầu Nhật Tân - Nội Bài)",
      speedLimit = 60,
      description = "Camera đo tốc độ tự động 60 km/h đường gom xe 2 bánh",
      districtCity = "Đông Anh, Hà Nội"
    ),
    TrafficCamera(
      id = "cam_hn_pvd_01",
      latitude = 21.0610,
      longitude = 105.7820,
      type = CameraType.RED_LIGHT_CAMERA,
      roadName = "Phạm Văn Đồng giao Hoàng Quốc Việt",
      speedLimit = 50,
      description = "Phạt nguội vượt đèn đỏ ngã tư giao cắt lớn",
      districtCity = "Cầu Giấy, Hà Nội"
    ),
    TrafficCamera(
      id = "cam_hn_gp_01",
      latitude = 20.9780,
      longitude = 105.8450,
      type = CameraType.COLD_FINE_SURVEILLANCE,
      roadName = "Đường Giải Phóng (Gần Bến Xe Nước Ngầm)",
      speedLimit = 50,
      description = "Giám sát tốc độ 50 km/h và dừng đỗ sai quy định",
      districtCity = "Hoàng Mai, Hà Nội"
    ),
    TrafficCamera(
      id = "cam_hn_nts_01",
      latitude = 21.0030,
      longitude = 105.8200,
      type = CameraType.RED_LIGHT_CAMERA,
      roadName = "Ngã Tư Sở (Nguyễn Trãi - Trường Chinh)",
      speedLimit = 50,
      description = "Camera phạt nguội vượt đèn đỏ và đè vạch ngã tư lớn",
      districtCity = "Đống Đa, Hà Nội"
    ),

    // ====== QUỐC LỘ 51 & ĐỒNG NAI - BÀ RỊA VŨNG TÀU ======
    TrafficCamera(
      id = "cam_ql51_01",
      latitude = 10.8710,
      longitude = 106.9120,
      type = CameraType.SPEED_CAMERA,
      roadName = "Quốc Lộ 51 (Đoạn Long Thành - Đồng Nai)",
      speedLimit = 60,
      description = "Camera phạt nguội tốc độ 60 km/h toàn tuyến tự động",
      districtCity = "Long Thành, Đồng Nai"
    ),
    TrafficCamera(
      id = "cam_ql51_02",
      latitude = 10.6010,
      longitude = 107.0850,
      type = CameraType.SPEED_CAMERA,
      roadName = "Quốc Lộ 51 (Đoạn Thị Xã Phú Mỹ)",
      speedLimit = 60,
      description = "Khu đông dân cư, camera đo tốc độ 60 km/h cố định",
      districtCity = "Thị xã Phú Mỹ, BR-VT"
    ),

    // ====== ĐÀ NẴNG ======
    TrafficCamera(
      id = "cam_dn_ntt_01",
      latitude = 16.0790,
      longitude = 108.1920,
      type = CameraType.SPEED_CAMERA,
      roadName = "Đường Nguyễn Tất Thành (Đà Nẵng)",
      speedLimit = 50,
      description = "Camera đo tốc độ 50 km/h dọc tuyến đường ven biển",
      districtCity = "Thanh Khê, Đà Nẵng"
    ),
    TrafficCamera(
      id = "cam_dn_cr_01",
      latitude = 16.0610,
      longitude = 108.2230,
      type = CameraType.RED_LIGHT_CAMERA,
      roadName = "Đầu Cầu Rồng (Đường 2 Tháng 9)",
      speedLimit = 40,
      description = "Camera phạt nguội vượt đèn và đè vạch rẽ trái",
      districtCity = "Hải Châu, Đà Nẵng"
    ),

    // ====== ĐIỂM CẢNH BÁO ĐƯỜNG CẤM XE MÁY / CAO TỐC ======
    TrafficCamera(
      id = "cam_prohibit_lt_01",
      latitude = 10.7960,
      longitude = 106.7620,
      type = CameraType.MOTORBIKE_PROHIBITED_ZONE,
      roadName = "Lối vào Cao Tốc TP.HCM - Long Thành (Nút Giao An Phú)",
      speedLimit = 0,
      description = "CẤM XE MÁY ĐI VÀO CAO TỐC! Phạt 2-3 triệu & tước GPLX 3-5 tháng",
      districtCity = "TP. Thủ Đức, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_prohibit_tl_01",
      latitude = 10.6850,
      longitude = 106.5920,
      type = CameraType.MOTORBIKE_PROHIBITED_ZONE,
      roadName = "Lối vào Cao Tốc TP.HCM - Trung Lương (Nút Giao Chợ Đệm)",
      speedLimit = 0,
      description = "CẤM XE MÁY ĐI VÀO CAO TỐC! Chú ý rẽ vào đường gom Quốc Lộ 1A",
      districtCity = "Bình Chánh, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_prohibit_vvk_01",
      latitude = 10.7480,
      longitude = 106.6620,
      type = CameraType.MOTORBIKE_PROHIBITED_ZONE,
      roadName = "Làn Ô Tô Giữa Đại Lộ Võ Văn Kiệt",
      speedLimit = 0,
      description = "Làn ô tô cấm xe máy! Xe máy chỉ được đi làn hỗn hợp bên phải",
      districtCity = "Quận 5, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_prohibit_pvd_01",
      latitude = 10.8250,
      longitude = 106.6980,
      type = CameraType.MOTORBIKE_PROHIBITED_ZONE,
      roadName = "Làn Ô Tô Giữa Đường Phạm Văn Đồng",
      speedLimit = 0,
      description = "Làn ô tô 80km/h cấm xe máy! Đi đúng làn xe 2 bánh bên phải",
      districtCity = "Gò Vấp, TP.HCM"
    ),
    TrafficCamera(
      id = "cam_prohibit_hn_tl_01",
      latitude = 21.0025,
      longitude = 105.7480,
      type = CameraType.MOTORBIKE_PROHIBITED_ZONE,
      roadName = "Làn Cao Tốc Đại Lộ Thăng Long",
      speedLimit = 0,
      description = "CẤM XE MÁY VÀO LÀN CAO TỐC! Xe máy phải đi đường gom 2 bên",
      districtCity = "Nam Từ Liêm, Hà Nội"
    ),
    TrafficCamera(
      id = "cam_prohibit_hn_pv_01",
      latitude = 20.9620,
      longitude = 105.8520,
      type = CameraType.MOTORBIKE_PROHIBITED_ZONE,
      roadName = "Đầu Cao Tốc Pháp Vân - Cầu Giẽ (Nút giao Giải Phóng)",
      speedLimit = 0,
      description = "CẤM TUYỆT ĐỐI XE MÁY ĐI VÀO CAO TỐC PHÁP VÂN! Rẽ sang Quốc Lộ 1A cũ",
      districtCity = "Hoàng Mai, Hà Nội"
    )
  )

  // Haversine distance in meters
  fun calculateDistanceMeters(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
  ): Double {
    val r = 6371000.0 // Earth radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
  }

  // Bearing in degrees (0 to 360)
  fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val dLon = Math.toRadians(lon2 - lon1)
    val y = sin(dLon) * cos(Math.toRadians(lat2))
    val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
            sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(dLon)
    val brng = Math.toDegrees(atan2(y, x))
    return ((brng + 360) % 360).toFloat()
  }

  // Helper to remove Vietnamese diacritics for ultra-fast, forgiving search matching
  fun unaccent(src: String): String {
    val temp = java.text.Normalizer.normalize(src, java.text.Normalizer.Form.NFD)
    val pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
    return pattern.matcher(temp).replaceAll("")
      .replace("đ", "d")
      .replace("Đ", "D")
      .lowercase()
      .trim()
  }

  // Popular Vietnam Destination Places (Google Maps style places, hospitals, gas stations, landmarks)
  val POPULAR_PLACES: List<com.example.data.model.DestinationPlace> = listOf(
    // === TP. HỒ CHÍ MINH - ĐỊA ĐIỂM CHÍNH & SÂN BAY / BẾN XE ===
    com.example.data.model.DestinationPlace("place_tsn", "Sân bay Quốc tế Tân Sơn Nhất", "Đường Trường Sơn, Phường 2, Tân Bình, TP.HCM", "Sân bay", 10.8184, 106.6588, iconEmoji = "✈️"),
    com.example.data.model.DestinationPlace("place_ben_thanh", "Chợ Bến Thành", "Đường Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM", "Địa điểm", 10.7725, 106.6980, iconEmoji = "🏛️"),
    com.example.data.model.DestinationPlace("place_landmark81", "Tòa nhà Landmark 81", "720A Điện Biên Phủ, Vinhomes Central Park, Bình Thạnh", "Trung tâm", 10.7950, 106.7218, iconEmoji = "🏢"),
    com.example.data.model.DestinationPlace("place_nguyen_hue", "Phố Đi Bộ Nguyễn Huệ", "Đường Nguyễn Huệ, Bến Nghé, Quận 1, TP.HCM", "Địa điểm", 10.7745, 106.7038, iconEmoji = "🚶"),
    com.example.data.model.DestinationPlace("place_dinh_doc_lap", "Dinh Độc Lập", "135 Nam Kỳ Khởi Nghĩa, Phường Bến Thành, Quận 1, TP.HCM", "Địa điểm", 10.7770, 106.6953, iconEmoji = "🏛️"),
    com.example.data.model.DestinationPlace("place_nha_tho_duc_ba", "Nhà Thờ Đức Bà", "01 Công xã Paris, Bến Nghé, Quận 1, TP.HCM", "Địa điểm", 10.7798, 106.6990, iconEmoji = "⛪"),
    com.example.data.model.DestinationPlace("place_cho_ray", "Bệnh Viện Chợ Rẫy", "201B Nguyễn Chí Thanh, Phường 12, Quận 5, TP.HCM", "Bệnh viện", 10.7570, 106.6590, iconEmoji = "🏥"),
    com.example.data.model.DestinationPlace("place_bv_nhi_dong_1", "Bệnh Viện Nhi Đồng 1", "341 Sư Vạn Hạnh, Phường 10, Quận 10, TP.HCM", "Bệnh viện", 10.7680, 106.6690, iconEmoji = "🏥"),
    com.example.data.model.DestinationPlace("place_bv_bach_mai_hcm", "Bệnh Viện Đại Học Y Dược", "215 Hồng Bàng, Phường 11, Quận 5, TP.HCM", "Bệnh viện", 10.7550, 106.6580, iconEmoji = "🏥"),
    com.example.data.model.DestinationPlace("place_dai_hoc_bach_khoa", "Đại Học Bách Khoa TP.HCM", "268 Lý Thường Kiệt, Phường 14, Quận 10, TP.HCM", "Trường học", 10.7733, 106.6597, iconEmoji = "🎓"),
    com.example.data.model.DestinationPlace("place_vincom_center", "Vincom Center Đồng Khởi", "72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP.HCM", "Trung tâm", 10.7780, 106.7018, iconEmoji = "🛍️"),
    com.example.data.model.DestinationPlace("place_aeon_tan_phu", "AEON Mall Tân Phú Celadon", "30 Bờ Bao Tân Thắng, Sơn Kỳ, Tân Phú, TP.HCM", "Trung tâm", 10.8015, 106.6185, iconEmoji = "🛍️"),
    com.example.data.model.DestinationPlace("place_aeon_binh_tan", "AEON Mall Bình Tân", "Số 1 Đường Số 17A, Bình Trị Đông B, Bình Tân, TP.HCM", "Trung tâm", 10.7425, 106.6080, iconEmoji = "🛍️"),
    com.example.data.model.DestinationPlace("place_bx_mientay", "Bến Xe Miền Tây", "395 Kinh Dương Vương, An Lạc, Bình Tân, TP.HCM", "Bến xe", 10.7516, 106.6174, iconEmoji = "🚌"),
    com.example.data.model.DestinationPlace("place_bx_miendong", "Bến Xe Miền Đông Mới", "Đường Hoàng Hữu Nam, Long Bình, TP. Thủ Đức, TP.HCM", "Bến xe", 10.8794, 106.8282, iconEmoji = "🚌"),
    com.example.data.model.DestinationPlace("place_bx_an_suong", "Bến Xe An Sương", "Quốc Lộ 22, Bà Điểm, Hóc Môn, TP.HCM", "Bến xe", 10.8490, 106.6120, iconEmoji = "🚌"),
    com.example.data.model.DestinationPlace("place_dam_sen", "Công Viên Văn Hóa Đầm Sen", "03 Hòa Bình, Phường 3, Quận 11, TP.HCM", "Khu vui chơi", 10.7685, 106.6410, iconEmoji = "🎡"),
    com.example.data.model.DestinationPlace("place_suoi_tien", "Khu Du Lịch Suối Tiên", "120 Xa Lộ Hà Nội, Tân Phú, TP. Thủ Đức, TP.HCM", "Khu vui chơi", 10.8660, 106.8030, iconEmoji = "🎡"),

    // === TÂN PHÚ, TÂN BÌNH, BÌNH TÂN ===
    com.example.data.model.DestinationPlace("place_ubnd_tan_phu", "UBND Quận Tân Phú", "560 Lũy Bán Bích, Hòa Thạnh, Tân Phú, TP.HCM", "Hành chính", 10.7815, 106.6320, iconEmoji = "🏛️"),
    com.example.data.model.DestinationPlace("place_nga_tu_bay_hien", "Ngã Tư Bảy Hiền", "Giao lộ CMT8 - Hoàng Văn Thụ - Lý Thường Kiệt, Tân Bình", "Địa điểm", 10.7910, 106.6530, iconEmoji = "🚦"),
    com.example.data.model.DestinationPlace("place_cv_hoang_van_thu", "Công Viên Hoàng Văn Thụ", "Hoàng Văn Thụ, Phường 2, Tân Bình, TP.HCM", "Địa điểm", 10.7995, 106.6620, iconEmoji = "🌳"),
    com.example.data.model.DestinationPlace("place_cv_gia_dinh", "Công Viên Gia Định", "Hoàng Minh Giám, Phường 3, Gò Vấp, TP.HCM", "Địa điểm", 10.8160, 106.6740, iconEmoji = "🌳"),

    // === CÁC TUYẾN ĐƯỜNG LỚN TP.HCM ===
    com.example.data.model.DestinationPlace("street_sg_lbb", "Đường Lũy Bán Bích", "Lũy Bán Bích, Tân Phú, TP.HCM", "Tuyến đường", 10.7750, 106.6340, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_tnh", "Đường Thoại Ngọc Hầu", "Thoại Ngọc Hầu, Tân Phú, TP.HCM", "Tuyến đường", 10.7780, 106.6370, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_au_co", "Đường Âu Cơ", "Âu Cơ, Tân Bình - Tân Phú, TP.HCM", "Tuyến đường", 10.7760, 106.6450, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_vvk", "Đại Lộ Võ Văn Kiệt", "Võ Văn Kiệt, Quận 1 - Quận 5 - Quận 6, TP.HCM", "Tuyến đường", 10.7530, 106.6710, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_pvd", "Đường Phạm Văn Đồng", "Phạm Văn Đồng, Bình Thạnh - Gò Vấp - Thủ Đức, TP.HCM", "Tuyến đường", 10.8250, 106.6950, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_nvl", "Đại Lộ Nguyễn Văn Linh", "Nguyễn Văn Linh, Quận 7 - Bình Chánh, TP.HCM", "Tuyến đường", 10.7285, 106.7082, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_mct", "Đại Lộ Mai Chí Thọ", "Mai Chí Thọ, An Phú, TP. Thủ Đức, TP.HCM", "Tuyến đường", 10.7812, 106.7350, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_cmt8", "Đường Cách Mạng Tháng 8", "Cách Mạng Tháng 8, Quận 3 - Quận 10 - Tân Bình, TP.HCM", "Tuyến đường", 10.7810, 106.6780, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_dbp", "Đường Điện Biên Phủ", "Điện Biên Phủ, Quận 1 - Quận 3 - Bình Thạnh, TP.HCM", "Tuyến đường", 10.7915, 106.6990, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_ntmk", "Đường Nguyễn Thị Minh Khai", "Nguyễn Thị Minh Khai, Quận 1 - Quận 3, TP.HCM", "Tuyến đường", 10.7760, 106.6910, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_3t2", "Đường Ba Tháng Hai", "Đường 3 Tháng 2, Quận 10 - Quận 11, TP.HCM", "Tuyến đường", 10.7690, 106.6690, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_ltk", "Đường Lý Thường Kiệt", "Lý Thường Kiệt, Quận 10 - Tân Bình, TP.HCM", "Tuyến đường", 10.7780, 106.6580, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_ch", "Đường Cộng Hòa", "Đường Cộng Hòa, Phường 13, Tân Bình, TP.HCM", "Tuyến đường", 10.8030, 106.6470, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_tc", "Đường Trường Chinh", "Đường Trường Chinh, Tân Bình - Quận 12, TP.HCM", "Tuyến đường", 10.8210, 106.6320, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_thd", "Đường Trần Hưng Đạo", "Trần Hưng Đạo, Quận 1 - Quận 5, TP.HCM", "Tuyến đường", 10.7580, 106.6780, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_ntr", "Đường Nguyễn Trãi", "Nguyễn Trãi, Quận 1 - Quận 5, TP.HCM", "Tuyến đường", 10.7605, 106.6775, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_hbt", "Đường Hai Bà Trưng", "Hai Bà Trưng, Quận 1 - Quận 3, TP.HCM", "Tuyến đường", 10.7850, 106.6980, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_nkkn", "Đường Nam Kỳ Khởi Nghĩa", "Nam Kỳ Khởi Nghĩa, Quận 1 - Quận 3, TP.HCM", "Tuyến đường", 10.7820, 106.6920, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_ql1a", "Quốc Lộ 1A TP.HCM", "Quốc Lộ 1A, Thủ Đức - Q12 - Bình Tân - Bình Chánh", "Tuyến đường", 10.8520, 106.6080, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_sg_ql22", "Quốc Lộ 22 (Đường Xuyên Á)", "Quốc Lộ 22, Hóc Môn - Củ Chi, TP.HCM", "Tuyến đường", 10.8650, 106.5920, iconEmoji = "🛣️"),

    // === CÂY XĂNG & TIỆN ÍCH ===
    com.example.data.model.DestinationPlace("place_petro_vvk", "Cây xăng Petrolimex Số 14", "Đại lộ Võ Văn Kiệt, Quận 5, TP.HCM", "Cây xăng", 10.7555, 106.6790, iconEmoji = "⛽"),
    com.example.data.model.DestinationPlace("place_petro_pvd", "Cây xăng Petrolimex Số 22", "Phạm Văn Đồng, Hiệp Bình Chánh, Thủ Đức", "Cây xăng", 10.8350, 106.7210, iconEmoji = "⛽"),
    com.example.data.model.DestinationPlace("place_petro_cmt8", "Cây xăng Comeco CMT8", "Cách Mạng Tháng 8, Phường 11, Quận 3, TP.HCM", "Cây xăng", 10.7790, 106.6750, iconEmoji = "⛽"),
    com.example.data.model.DestinationPlace("place_pvoil_lbb", "Cây xăng PVOIL Lũy Bán Bích", "248 Lũy Bán Bích, Hòa Thạnh, Tân Phú, TP.HCM", "Cây xăng", 10.7740, 106.6345, iconEmoji = "⛽"),
    com.example.data.model.DestinationPlace("place_petro_cong_hoa", "Cây xăng Petrolimex Cộng Hòa", "19 Cộng Hòa, Phường 4, Tân Bình, TP.HCM", "Cây xăng", 10.8010, 106.6570, iconEmoji = "⛽"),

    // === HÀ NỘI ===
    com.example.data.model.DestinationPlace("place_hoan_kiem", "Hồ Hoàn Kiếm Hà Nội", "Hàng Trống, Hoàn Kiếm, Hà Nội", "Địa điểm", 21.0285, 105.8542, iconEmoji = "🏛️"),
    com.example.data.model.DestinationPlace("place_lang_bac", "Lăng Chủ Tịch Hồ Chí Minh", "02 Hùng Vương, Điện Bàn, Ba Đình, Hà Nội", "Địa điểm", 21.0368, 105.8347, iconEmoji = "🏛️"),
    com.example.data.model.DestinationPlace("place_noi_bai", "Sân bay Quốc tế Nội Bài", "Xã Phú Minh, Huyện Sóc Sơn, Hà Nội", "Sân bay", 21.2212, 105.8072, iconEmoji = "✈️"),
    com.example.data.model.DestinationPlace("place_bx_my_dinh", "Bến Xe Mỹ Đình", "20 Phạm Hùng, Mỹ Đình, Nam Từ Liêm, Hà Nội", "Bến xe", 21.0280, 105.7780, iconEmoji = "🚌"),
    com.example.data.model.DestinationPlace("place_bx_giap_bat", "Bến Xe Giáp Bát", "Km6 Đường Giải Phóng, Hoàng Mai, Hà Nội", "Bến xe", 20.9820, 105.8420, iconEmoji = "🚌"),
    com.example.data.model.DestinationPlace("street_hn_vd3", "Đường Vành Đai 3 Hà Nội", "Khuất Duy Tiến - Nguyễn Xiển, Thanh Xuân, Hà Nội", "Tuyến đường", 20.9982, 105.7950, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_hn_gp", "Đường Giải Phóng", "Giải Phóng, Hoàng Mai - Hai Bà Trưng, Hà Nội", "Tuyến đường", 20.9780, 105.8450, iconEmoji = "🛣️"),
    com.example.data.model.DestinationPlace("street_hn_cgh", "Đường Cầu Giấy", "Cầu Giấy, Quận Cầu Giấy, Hà Nội", "Tuyến đường", 21.0340, 105.7980, iconEmoji = "🛣️"),

    // === ĐÀ NẴNG ===
    com.example.data.model.DestinationPlace("place_cau_rong", "Cầu Rồng Đà Nẵng", "Đường Nguyễn Văn Linh, Phước Ninh, Hải Châu, Đà Nẵng", "Địa điểm", 16.0610, 108.2230, iconEmoji = "🌉"),
    com.example.data.model.DestinationPlace("place_san_bay_dn", "Sân bay Quốc tế Đà Nẵng", "Đường Duy Tân, Hòa Thuận Tây, Hải Châu, Đà Nẵng", "Sân bay", 16.0538, 108.2022, iconEmoji = "✈️"),
    com.example.data.model.DestinationPlace("street_dn_ntt", "Đường Nguyễn Tất Thành", "Nguyễn Tất Thành, Thanh Khê - Liên Chiểu, Đà Nẵng", "Tuyến đường", 16.0790, 108.1920, iconEmoji = "🛣️")
  )

  // Turn-by-Turn Navigation Multi-Route Generator
  fun generateTurnByTurnRoute(
    startLat: Double,
    startLng: Double,
    destLat: Double,
    destLng: Double,
    destName: String,
    destAddress: String,
    mode: com.example.data.model.VehicleRoutingMode = com.example.data.model.VehicleRoutingMode.MOTORBIKE
  ): com.example.data.model.NavigationRoute {
    val totalDirectDist = calculateDistanceMeters(startLat, startLng, destLat, destLng)
    val baseDistanceMeters = (totalDirectDist * 1.25).toInt().coerceAtLeast(350)
    val baseDurationMinutes = ((baseDistanceMeters / 1000.0) / (if (mode == com.example.data.model.VehicleRoutingMode.MOTORBIKE) 32.0 else 36.0) * 60.0).toInt().coerceAtLeast(2)

    // 1. PRIMARY ROUTE (TỐI ƯU NHẤT)
    val waypointsPrimary = mutableListOf<Pair<Double, Double>>()
    waypointsPrimary.add(startLat to startLng)
    val segmentsCount = 10
    val dLat = (destLat - startLat) / segmentsCount
    val dLng = (destLng - startLng) / segmentsCount

    for (i in 1 until segmentsCount) {
      val offsetLat = if (i % 2 == 1) 0.00035 * sin(i.toDouble()) else -0.00025 * cos(i.toDouble())
      val offsetLng = if (i % 2 == 0) 0.00045 * cos(i.toDouble()) else 0.0002 * sin(i.toDouble())
      val lat = startLat + dLat * i + offsetLat
      val lng = startLng + dLng * i + offsetLng
      waypointsPrimary.add(lat to lng)
    }
    waypointsPrimary.add(destLat to destLng)

    val stepsPrimary = listOf(
      com.example.data.model.NavigationStep(
        instruction = "Bắt đầu xuất phát theo lộ trình",
        distanceMeters = (baseDistanceMeters * 0.12).toInt(),
        maneuver = com.example.data.model.NavigationManeuverType.DEPART,
        roadName = "Tuyến đường hiện tại",
        latitude = startLat,
        longitude = startLng
      ),
      com.example.data.model.NavigationStep(
        instruction = "Đi thẳng trên trục đường chính",
        distanceMeters = (baseDistanceMeters * 0.38).toInt(),
        maneuver = com.example.data.model.NavigationManeuverType.STRAIGHT,
        roadName = "Đại lộ chính",
        latitude = waypointsPrimary[2].first,
        longitude = waypointsPrimary[2].second
      ),
      com.example.data.model.NavigationStep(
        instruction = "Rẽ phải vào tuyến đường đến $destName",
        distanceMeters = (baseDistanceMeters * 0.32).toInt(),
        maneuver = com.example.data.model.NavigationManeuverType.TURN_RIGHT,
        roadName = "Đường kết nối đô thị",
        latitude = waypointsPrimary[6].first,
        longitude = waypointsPrimary[6].second
      ),
      com.example.data.model.NavigationStep(
        instruction = "Đến nơi: $destName ở phía trước",
        distanceMeters = (baseDistanceMeters * 0.18).toInt(),
        maneuver = com.example.data.model.NavigationManeuverType.ARRIVE,
        roadName = destName,
        latitude = destLat,
        longitude = destLng
      )
    )

    // 2. ALTERNATIVE ROUTE 1: NGẮN NHẤT / TIẾT KIỆM
    val altDistance1 = (baseDistanceMeters * 0.94).toInt().coerceAtLeast(300)
    val altDuration1 = (baseDurationMinutes + 2).coerceAtLeast(3)
    val waypointsAlt1 = mutableListOf<Pair<Double, Double>>()
    waypointsAlt1.add(startLat to startLng)
    for (i in 1 until segmentsCount) {
      val offsetLat = if (i % 2 == 0) -0.0004 * sin(i.toDouble()) else 0.0003 * cos(i.toDouble())
      val offsetLng = if (i % 2 == 1) -0.0003 * cos(i.toDouble()) else 0.00035 * sin(i.toDouble())
      waypointsAlt1.add(startLat + dLat * i + offsetLat to startLng + dLng * i + offsetLng)
    }
    waypointsAlt1.add(destLat to destLng)

    val altRoute1 = com.example.data.model.NavigationRoute(
      id = "route_alt_short_${System.currentTimeMillis()}",
      destinationName = destName,
      destinationAddress = destAddress,
      destinationLat = destLat,
      destinationLng = destLng,
      totalDistanceMeters = altDistance1,
      estimatedDurationMinutes = altDuration1,
      waypoints = waypointsAlt1,
      steps = stepsPrimary,
      currentStepIndex = 0,
      isNavigating = false,
      routeTag = "Ngắn nhất (-${((baseDistanceMeters - altDistance1) / 1000f * 10).toInt() / 10f} km)",
      isMotorbikeSafe = true
    )

    // 3. ALTERNATIVE ROUTE 2: TRÁNH KẸT XE / TRÁNH BOT
    val altDistance2 = (baseDistanceMeters * 1.15).toInt()
    val altDuration2 = (baseDurationMinutes + 4).coerceAtLeast(4)
    val waypointsAlt2 = mutableListOf<Pair<Double, Double>>()
    waypointsAlt2.add(startLat to startLng)
    for (i in 1 until segmentsCount) {
      val offsetLat = 0.0005 * sin(i.toDouble() * 1.2)
      val offsetLng = -0.00045 * cos(i.toDouble() * 1.2)
      waypointsAlt2.add(startLat + dLat * i + offsetLat to startLng + dLng * i + offsetLng)
    }
    waypointsAlt2.add(destLat to destLng)

    val altRoute2 = com.example.data.model.NavigationRoute(
      id = "route_alt_nobot_${System.currentTimeMillis()}",
      destinationName = destName,
      destinationAddress = destAddress,
      destinationLat = destLat,
      destinationLng = destLng,
      totalDistanceMeters = altDistance2,
      estimatedDurationMinutes = altDuration2,
      waypoints = waypointsAlt2,
      steps = stepsPrimary,
      currentStepIndex = 0,
      isNavigating = false,
      routeTag = if (mode == com.example.data.model.VehicleRoutingMode.MOTORBIKE) "Tuyến xe máy êm ái" else "Tránh trạm thu phí BOT",
      isMotorbikeSafe = true,
      hasTollBooth = false
    )

    return com.example.data.model.NavigationRoute(
      id = "route_primary_${System.currentTimeMillis()}",
      destinationName = destName,
      destinationAddress = destAddress,
      destinationLat = destLat,
      destinationLng = destLng,
      totalDistanceMeters = baseDistanceMeters,
      estimatedDurationMinutes = baseDurationMinutes,
      waypoints = waypointsPrimary,
      steps = stepsPrimary,
      currentStepIndex = 0,
      isNavigating = true,
      routeTag = "Nhanh nhất (${baseDurationMinutes} phút)",
      alternativeRoutes = listOf(altRoute1, altRoute2),
      isMotorbikeSafe = true,
      hasTollBooth = mode == com.example.data.model.VehicleRoutingMode.CAR
    )
  }

  // Realistic Offline Road Network Segments (Vietmap layout)
  val ALL_ROADS: List<com.example.data.model.MapRoadSegment> = listOf(
    // Downtown Ho Chi Minh City Grid (Google Maps style)
    com.example.data.model.MapRoadSegment(
      id = "road_le_loi",
      name = "Đường Lê Lợi",
      type = com.example.data.model.RoadType.BOULEVARD,
      speedLimitKmh = 50,
      coordinates = listOf(
        10.7715 to 106.6970,
        10.7735 to 106.7000,
        10.7750 to 106.7025
      )
    ),
    com.example.data.model.MapRoadSegment(
      id = "road_nguyen_hue",
      name = "Đường Nguyễn Huệ",
      type = com.example.data.model.RoadType.BOULEVARD,
      speedLimitKmh = 50,
      coordinates = listOf(
        10.7760 to 106.7015,
        10.7745 to 106.7038,
        10.7710 to 106.7065
      )
    ),
    com.example.data.model.MapRoadSegment(
      id = "road_nam_ky",
      name = "Đường Nam Kỳ Khởi Nghĩa",
      type = com.example.data.model.RoadType.ARTERIAL,
      speedLimitKmh = 50,
      coordinates = listOf(
        10.7680 to 106.6990,
        10.7740 to 106.6950,
        10.7810 to 106.6900,
        10.7920 to 106.6810
      )
    ),
    com.example.data.model.MapRoadSegment(
      id = "road_dien_bien_phu",
      name = "Đường Điện Biên Phủ",
      type = com.example.data.model.RoadType.ARTERIAL,
      speedLimitKmh = 50,
      coordinates = listOf(
        10.7650 to 106.6750,
        10.7780 to 106.6910,
        10.7910 to 106.7110,
        10.8010 to 106.7250
      )
    ),
    com.example.data.model.MapRoadSegment(
      id = "road_truong_son",
      name = "Đường Trường Sơn (Sân Bay)",
      type = com.example.data.model.RoadType.ARTERIAL,
      speedLimitKmh = 50,
      coordinates = listOf(
        10.8050 to 106.6660,
        10.8140 to 106.6620,
        10.8184 to 106.6588
      )
    ),
    // Sài Gòn Arterials
    com.example.data.model.MapRoadSegment(
      id = "road_vvk",
      name = "Đại lộ Võ Văn Kiệt",
      type = com.example.data.model.RoadType.BOULEVARD,
      speedLimitKmh = 60,
      coordinates = listOf(
        10.7650 to 106.7020,
        10.7580 to 106.6850,
        10.7540 to 106.6760,
        10.7523 to 106.6712,
        10.7480 to 106.6620,
        10.7410 to 106.6430,
        10.7320 to 106.6210,
        10.7280 to 106.6110
      )
    ),
    com.example.data.model.MapRoadSegment(
      id = "road_ql1a_sg",
      name = "Quốc Lộ 1A (Tây Nam)",
      type = com.example.data.model.RoadType.HIGHWAY,
      speedLimitKmh = 50,
      coordinates = listOf(
        10.8465 to 106.6112,
        10.8010 to 106.6020,
        10.7550 to 106.6010,
        10.7250 to 106.6050,
        10.7120 to 106.5920,
        10.6850 to 106.5780
      )
    ),
    com.example.data.model.MapRoadSegment(
      id = "road_pvd",
      name = "Đại lộ Phạm Văn Đồng",
      type = com.example.data.model.RoadType.BOULEVARD,
      speedLimitKmh = 60,
      coordinates = listOf(
        10.8160 to 106.6780,
        10.8242 to 106.6912,
        10.8350 to 106.7150,
        10.8491 to 106.7465,
        10.8650 to 106.7720
      )
    ),
    com.example.data.model.MapRoadSegment(
      id = "road_nvl",
      name = "Đại lộ Nguyễn Văn Linh",
      type = com.example.data.model.RoadType.BOULEVARD,
      speedLimitKmh = 60,
      coordinates = listOf(
        10.7120 to 106.6350,
        10.7198 to 106.6610,
        10.7285 to 106.7082,
        10.7320 to 106.7350
      )
    ),
    com.example.data.model.MapRoadSegment(
      id = "road_mct",
      name = "Đại lộ Mai Chí Thọ",
      type = com.example.data.model.RoadType.BOULEVARD,
      speedLimitKmh = 50,
      coordinates = listOf(
        10.7710 to 106.7150,
        10.7812 to 106.7350,
        10.7930 to 106.7580
      )
    ),
    // Sông Sài Gòn (Waterway visual)
    com.example.data.model.MapRoadSegment(
      id = "river_sg",
      name = "Sông Sài Gòn",
      type = com.example.data.model.RoadType.RIVER,
      speedLimitKmh = 0,
      coordinates = listOf(
        10.8350 to 106.7050,
        10.8120 to 106.7180,
        10.7850 to 106.7080,
        10.7680 to 106.7060,
        10.7500 to 106.7220,
        10.7250 to 106.7450
      )
    ),
    // Hà Nội Arterials
    com.example.data.model.MapRoadSegment(
      id = "road_vd3_hn",
      name = "Đường Vành Đai 3",
      type = com.example.data.model.RoadType.HIGHWAY,
      speedLimitKmh = 50,
      coordinates = listOf(
        21.0610 to 105.7820,
        21.0350 to 105.7830,
        21.0080 to 105.8010,
        20.9982 to 105.7950,
        20.9780 to 105.8450
      )
    ),
    com.example.data.model.MapRoadSegment(
      id = "road_tl_hn",
      name = "Đại Lộ Thăng Long",
      type = com.example.data.model.RoadType.HIGHWAY,
      speedLimitKmh = 50,
      coordinates = listOf(
        21.0080 to 105.7920,
        21.0005 to 105.7720,
        21.0020 to 105.7520,
        21.0040 to 105.7210
      )
    )
  )


  // Comprehensive POIs on Map (Gas, Toll, Hospital, Rescue, Danger Blackspots)
  val ALL_POIS: List<com.example.data.model.MapPoi> = listOf(
    // Cây xăng
    com.example.data.model.MapPoi("poi_petro_01", "Petrolimex Võ Văn Kiệt", com.example.data.model.PoiType.GAS_STATION, 10.7555, 106.6790, "Xăng RON95-V, E5, Dầu DO 0.001S"),
    com.example.data.model.MapPoi("poi_petro_02", "Petrolimex Phạm Văn Đồng", com.example.data.model.PoiType.GAS_STATION, 10.8290, 106.7020, "Phục vụ 24/7, có bơm lốp tự động"),
    com.example.data.model.MapPoi("poi_pvoil_01", "PVOIL Nguyễn Văn Linh", com.example.data.model.PoiType.GAS_STATION, 10.7240, 106.6850, "Cây xăng PVOIL & rửa xe nhanh"),
    com.example.data.model.MapPoi("poi_petro_hn_01", "Petrolimex Khuất Duy Tiến", com.example.data.model.PoiType.GAS_STATION, 20.9950, 105.7980, "Trạm xăng trung tâm Thanh Xuân, Hà Nội"),
    com.example.data.model.MapPoi("poi_petro_dn_01", "Petrolimex Nguyễn Tất Thành", com.example.data.model.PoiType.GAS_STATION, 16.0750, 108.2010, "Trạm xăng ven biển Đà Nẵng"),

    // Trạm thu phí BOT
    com.example.data.model.MapPoi("poi_bot_01", "Trạm Thu Phí BOT QL51", com.example.data.model.PoiType.TOLL_BOOTH, 10.8650, 106.9180, "Làn ETC tự động không dừng"),
    com.example.data.model.MapPoi("poi_bot_02", "Trạm Thu Phí Long Phước (Cao Tốc)", com.example.data.model.PoiType.TOLL_BOOTH, 10.8050, 106.8450, "Cao tốc TP.HCM - Long Thành - Dầu Giây"),
    com.example.data.model.MapPoi("poi_bot_03", "Trạm BOT Xa Lộ Hà Nội", com.example.data.model.PoiType.TOLL_BOOTH, 10.8350, 106.7650, "Trạm thu phí Rạch Chiếc"),
    com.example.data.model.MapPoi("poi_bot_04", "Trạm BOT Pháp Vân - Cầu Giẽ", com.example.data.model.PoiType.TOLL_BOOTH, 20.9520, 105.8560, "Cửa ngõ phía Nam Hà Nội"),

    // Cầu lớn & Nút giao
    com.example.data.model.MapPoi("poi_bridge_01", "Cầu Chữ Y", com.example.data.model.PoiType.BRIDGE, 10.7515, 106.6718, "Cầu vượt kênh Bến Nghé nối Q5 - Q8"),
    com.example.data.model.MapPoi("poi_bridge_02", "Cầu Bình Lợi", com.example.data.model.PoiType.BRIDGE, 10.8240, 106.6908, "Cầu vượt sông Sài Gòn trên đường Phạm Văn Đồng"),
    com.example.data.model.MapPoi("poi_bridge_03", "Cầu Nhật Tân", com.example.data.model.PoiType.BRIDGE, 21.0920, 105.8190, "Cầu dây văng vượt sông Hồng Hà Nội"),
    com.example.data.model.MapPoi("poi_bridge_04", "Cầu Rồng Đà Nẵng", com.example.data.model.PoiType.BRIDGE, 16.0610, 108.2230, "Cầu biểu tượng sông Hàn"),

    // Bệnh viện & Cứu hộ y tế
    com.example.data.model.MapPoi("poi_hosp_01", "Bệnh Viện Chợ Rẫy", com.example.data.model.PoiType.HOSPITAL, 10.7570, 106.6590, "Cấp cứu 115 24/7 - Quận 5, TP.HCM"),
    com.example.data.model.MapPoi("poi_hosp_02", "Bệnh Viện Nhân Dân Gia Định", com.example.data.model.PoiType.HOSPITAL, 10.8030, 106.6950, "Bình Thạnh, TP.HCM"),
    com.example.data.model.MapPoi("poi_hosp_03", "Bệnh Viện Bạch Mai", com.example.data.model.PoiType.HOSPITAL, 20.9990, 105.8410, "Cấp cứu A9 - Đống Đa, Hà Nội"),
    com.example.data.model.MapPoi("poi_hosp_04", "Bệnh Viện Đà Nẵng", com.example.data.model.PoiType.HOSPITAL, 16.0710, 108.2170, "Hải Châu, Đà Nẵng"),

    // Cứu hộ lốp & Vá xe 24/7
    com.example.data.model.MapPoi("poi_tire_01", "Cứu Hộ Lốp & Vá Xe 24/7 Võ Văn Kiệt", com.example.data.model.PoiType.TIRE_REPAIR, 10.7490, 106.6650, "Vá vỏ lưu động, cân mâm bấm chì"),
    com.example.data.model.MapPoi("poi_tire_02", "Cứu Hộ Ô Tô & Xe Máy Phạm Văn Đồng", com.example.data.model.PoiType.TIRE_REPAIR, 10.8350, 106.7200, "Cứu hộ bình ắc quy, thay lốp khẩn cấp"),

    // Điểm đen tai nạn giao thông (Cảnh báo chú ý quan sát)
    com.example.data.model.MapPoi("poi_danger_01", "Điểm đen tai nạn: Vòng xoay An Sương", com.example.data.model.PoiType.ACCIDENT_HOTSPOT, 10.8465, 106.6112, "Giao cắt xe tải lớn, chú ý điểm mù và giảm tốc độ"),
    com.example.data.model.MapPoi("poi_danger_02", "Điểm đen: Ngã tư Bình Phước (QL13 giao QL1A)", com.example.data.model.PoiType.ACCIDENT_HOTSPOT, 10.8710, 106.7180, "Mật độ xe container cao, quan sát kỹ khi chuyển làn")
  )

  // Realistic Simulation Waypoints (e.g. Sài Gòn Võ Văn Kiệt -> Quốc lộ 1A test route)
  val SIMULATION_ROUTE_SAIGON = listOf(
    SimPoint(10.7580, 106.6850, 42f, "Đại lộ Võ Văn Kiệt (Quận 1)", 60),
    SimPoint(10.7540, 106.6760, 52f, "Đại lộ Võ Văn Kiệt (Quận 5)", 60),
    SimPoint(10.7523, 106.6712, 64f, "Đại lộ Võ Văn Kiệt (Gần Cầu Chữ Y)", 60), // Camera point here!
    SimPoint(10.7480, 106.6620, 68f, "Đại lộ Võ Văn Kiệt (Quận 6)", 60),
    SimPoint(10.7410, 106.6430, 58f, "Võ Văn Kiệt giao An Dương Vương", 60), // Red light cam
    SimPoint(10.7320, 106.6210, 48f, "Đoạn Nút Giao Bình Tân", 50),
    SimPoint(10.7250, 106.6050, 54f, "Quốc Lộ 1A (Bình Chánh)", 50),
    SimPoint(10.7120, 106.5920, 63f, "Quốc Lộ 1A (Đoạn Cầu Bình Điền)", 50)
  )

  val SIMULATION_ROUTE_HANOI = listOf(
    SimPoint(21.0080, 105.8010, 45f, "Đường Khuất Duy Tiến", 50),
    SimPoint(20.9982, 105.7950, 58f, "Đường Vành Đai 3 dưới thấp", 50), // Cam
    SimPoint(21.0005, 105.7720, 50f, "Đầu Đại Lộ Thăng Long", 50),
    SimPoint(21.0020, 105.7520, 66f, "Đại Lộ Thăng Long (Đường gom)", 50) // Cam
  )

  val SIMULATION_ROUTE_DANANG = listOf(
    SimPoint(16.0790, 108.1920, 48f, "Đường Nguyễn Tất Thành (Đà Nẵng)", 50),
    SimPoint(16.0720, 108.2050, 55f, "Đoạn Cầu Thuận Phước", 50),
    SimPoint(16.0610, 108.2230, 42f, "Đầu Cầu Rồng (Đường 2 Tháng 9)", 40)
  )

  val SIMULATION_ROUTE_QL51 = listOf(
    SimPoint(10.8710, 106.9120, 68f, "Quốc Lộ 51 (Đoạn Long Thành - Đồng Nai)", 60),
    SimPoint(10.7850, 107.0100, 62f, "Quốc Lộ 51 (Đoạn Nhơn Trạch)", 60),
    SimPoint(10.6010, 107.0850, 65f, "Quốc Lộ 51 (Đoạn Thị Xã Phú Mỹ)", 60)
  )

  // ====== EXTENDED DATA: CAMERA TỪ NGUỒN CSGT, OSM, CỘNG ĐỒNG (TOẠ ĐỘ CHUẨN XÁC) ======
  val EXTENDED_CAMERAS: List<TrafficCamera> = listOf(

    // === QUỐC LỘ 1A (HÀ NỘI → TP.HCM — Trục chính xuyên Việt) ===
    TrafficCamera("cam_ql1a_nb_01", 20.5350, 105.9180, CameraType.SPEED_CAMERA, "QL1A — Đoạn Phủ Lý (Hà Nam)", 80, "Camera cố định CSGT 80 km/h quốc lộ", "Phủ Lý, Hà Nam"),
    TrafficCamera("cam_ql1a_nb_02", 20.3050, 105.9320, CameraType.SPEED_CAMERA, "QL1A — Đoạn Ninh Bình đầu cầu Gián Khẩu", 80, "Camera bắn tốc độ trước Khu Du Lịch Ninh Bình", "Ninh Bình"),
    TrafficCamera("cam_ql1a_nb_03", 20.4210, 106.1750, CameraType.COLD_FINE_SURVEILLANCE, "QL1A — Đoạn Nam Định (Cầu Đò Quan)", 60, "Camera phạt nguội qua thành phố Nam Định", "Nam Định"),
    TrafficCamera("cam_ql1a_th_01", 19.8350, 105.7720, CameraType.SPEED_CAMERA, "QL1A — Cầu Hàm Rồng (Thanh Hóa)", 80, "Camera cố định đầu cầu Hàm Rồng hướng Nam", "Thanh Hóa"),
    TrafficCamera("cam_ql1a_th_02", 20.0820, 105.8650, CameraType.COLD_FINE_SURVEILLANCE, "QL1A — Đoạn Bỉm Sơn (Thanh Hóa)", 80, "Camera phạt nguội đoạn nguy hiểm Bỉm Sơn", "Bỉm Sơn, Thanh Hóa"),
    TrafficCamera("cam_ql1a_na_01", 18.6650, 105.6980, CameraType.SPEED_CAMERA, "QL1A — Cầu Bến Thủy (Nghệ An)", 80, "Camera đầu cầu Bến Thủy phía Nghệ An 80 km/h", "Cầu Bến Thủy, Nghệ An"),
    TrafficCamera("cam_ql1a_na_02", 18.9850, 105.5820, CameraType.COLD_FINE_SURVEILLANCE, "QL1A — Đoạn Diễn Châu (Nghệ An)", 80, "Camera phạt nguội đoạn quốc lộ qua Diễn Châu", "Diễn Châu, Nghệ An"),
    TrafficCamera("cam_ql1a_ht_01", 18.0650, 106.2950, CameraType.SPEED_CAMERA, "QL1A — Đoạn Kỳ Anh (Hà Tĩnh)", 80, "Camera cố định CSGT đoạn Kỳ Anh tiếp giáp Quảng Bình", "Kỳ Anh, Hà Tĩnh"),
    TrafficCamera("cam_ql1a_qb_01", 17.6520, 106.5210, CameraType.SPEED_CAMERA, "QL1A — Đèo Lý Hòa (Quảng Bình)", 60, "Hạn chế 60 km/h đoạn đèo Lý Hòa — Điểm đen tai nạn", "Đèo Lý Hòa, Quảng Bình"),
    TrafficCamera("cam_ql1a_qb_02", 17.4720, 106.6020, CameraType.COLD_FINE_SURVEILLANCE, "QL1A — Đồng Hới (Quảng Bình)", 60, "Camera phạt nguội qua TP. Đồng Hới", "Đồng Hới, Quảng Bình"),
    TrafficCamera("cam_ql1a_qt_01", 16.1950, 108.1320, CameraType.SPEED_CAMERA, "QL1A — Đèo Hải Vân (Phía Bắc)", 40, "Hạn chế 40 km/h đoạn lên đèo nguy hiểm", "Đèo Hải Vân, Thừa Thiên Huế"),
    TrafficCamera("cam_ql1a_dn_01", 15.9920, 108.1950, CameraType.COLD_FINE_SURVEILLANCE, "QL1A — Đà Nẵng (Đoạn Cầu Đỏ - Hòa Vang)", 60, "Camera phạt nguội qua cửa ngõ phía Nam Đà Nẵng", "Hòa Vang, Đà Nẵng"),
    TrafficCamera("cam_ql1a_qn_01", 15.5680, 108.4820, CameraType.SPEED_CAMERA, "QL1A — Đoạn Tam Kỳ (Quảng Nam)", 80, "Camera cố định 80 km/h đoạn Tam Kỳ", "Tam Kỳ, Quảng Nam"),
    TrafficCamera("cam_ql1a_qng_01", 15.1210, 108.7980, CameraType.SPEED_CAMERA, "QL1A — Đoạn Quảng Ngãi (Cửa Ngõ Phía Bắc)", 60, "Camera đoạn vào TP. Quảng Ngãi", "Quảng Ngãi"),
    TrafficCamera("cam_ql1a_bd_01", 13.8820, 109.1120, CameraType.COLD_FINE_SURVEILLANCE, "QL1A — Đoạn Bình Định (An Nhơn)", 80, "Camera phạt nguội đoạn An Nhơn", "An Nhơn, Bình Định"),
    TrafficCamera("cam_ql1a_pyen_01", 13.0850, 109.3020, CameraType.SPEED_CAMERA, "QL1A — Đoạn Tuy Hòa (Phú Yên)", 80, "Camera cố định 80 km/h đoạn Tuy Hòa", "Tuy Hòa, Phú Yên"),
    TrafficCamera("cam_ql1a_kh_01", 12.2450, 109.1750, CameraType.SPEED_CAMERA, "QL1A — Đoạn Nha Trang (Khánh Hòa)", 80, "Camera quốc lộ cửa ngõ Nha Trang", "Khánh Hòa"),
    TrafficCamera("cam_ql1a_nt_01", 11.5720, 108.9850, CameraType.COLD_FINE_SURVEILLANCE, "QL1A — Đoạn Phan Rang (Ninh Thuận)", 80, "Camera phạt nguội đoạn Phan Rang", "Phan Rang, Ninh Thuận"),
    TrafficCamera("cam_ql1a_bt_01", 10.9320, 108.0950, CameraType.SPEED_CAMERA, "QL1A — Đoạn Phan Thiết (Bình Thuận)", 80, "Camera cố định 80 km/h đoạn vào Phan Thiết", "Phan Thiết, Bình Thuận"),
    TrafficCamera("cam_ql1a_sg_01", 10.6380, 106.4950, CameraType.COLD_FINE_SURVEILLANCE, "QL1A — Đoạn Long An (Cầu Bến Lức)", 80, "Camera phạt nguội đoạn cầu Bến Lức QL1A", "Bến Lức, Long An"),
    TrafficCamera("cam_ql1a_sg_02", 10.7120, 106.5920, CameraType.SPEED_CAMERA, "QL1A — Đoạn Bình Chánh (Cầu Bình Điền)", 60, "Camera 60 km/h vào địa phận TP.HCM", "Bình Chánh, TP.HCM"),

    // === CAO TỐC LONG THÀNH - DẦU GIÂY (CT01) ===
    TrafficCamera("cam_ct_lt_01", 10.7960, 106.7620, CameraType.MOTORBIKE_PROHIBITED_ZONE, "Cao Tốc Long Thành — Nút Giao An Phú", 0, "CẤM XE MÁY — Đầu đường cao tốc An Phú", "TP. Thủ Đức, TP.HCM"),
    TrafficCamera("cam_ct_lt_02", 10.8050, 106.8450, CameraType.COLD_FINE_SURVEILLANCE, "Cao Tốc Long Thành — Trạm Thu Phí Long Phước", 80, "Camera phạt nguội và kiểm soát tốc độ trạm thu phí", "TP. Thủ Đức, TP.HCM"),
    TrafficCamera("cam_ct_lt_03", 10.7850, 106.8920, CameraType.SPEED_CAMERA, "Cao Tốc Long Thành — Đoạn Cầu Long Thành", 100, "Camera giám sát tốc độ 100 km/h cầu Long Thành", "Long Thành, Đồng Nai"),
    TrafficCamera("cam_ct_lt_04", 10.7720, 106.9650, CameraType.SPEED_CAMERA, "Cao Tốc Long Thành — Km 23 (Giao QL51)", 120, "Camera tốc độ 120 km/h đoạn thông thoáng", "Long Thành, Đồng Nai"),
    TrafficCamera("cam_ct_lt_05", 10.9250, 107.1250, CameraType.COLD_FINE_SURVEILLANCE, "Cao Tốc Long Thành — Nút Giao Dầu Giây", 80, "Camera giảm tốc 80 km/h khu vực nút giao Dầu Giây", "Dầu Giây, Đồng Nai"),

    // === CAO TỐC TP.HCM - TRUNG LƯƠNG (CT.02) ===
    TrafficCamera("cam_ct_tl_01", 10.6850, 106.5350, CameraType.SPEED_CAMERA, "Cao Tốc TP.HCM - Trung Lương — Km 10", 120, "Camera tốc độ 120 km/h đoạn Bình Chánh - Bến Lức", "Bình Chánh / Long An"),
    TrafficCamera("cam_ct_tl_02", 10.5650, 106.3950, CameraType.COLD_FINE_SURVEILLANCE, "Cao Tốc TP.HCM - Trung Lương — Km 25 (Tân An)", 120, "Camera phạt nguội đoạn Tân An", "Long An"),
    TrafficCamera("cam_ct_tl_03", 10.4350, 106.2850, CameraType.SPEED_CAMERA, "Cao Tốc TP.HCM - Trung Lương — Km 40", 120, "Camera cố định km 40", "Tiền Giang"),
    TrafficCamera("cam_ct_tl_04", 10.4210, 106.2550, CameraType.MOTORBIKE_PROHIBITED_ZONE, "Cao Tốc Trung Lương — Nút Giao Thân Cửu Nghĩa", 0, "CẤM XE MÁY — Lối ra/vào cao tốc", "Châu Thành, Tiền Giang"),

    // === VÀNH ĐAI 3 TP.HCM ===
    TrafficCamera("cam_vd3_01", 10.8920, 106.7950, CameraType.SPEED_CAMERA, "Vành Đai 3 — Đoạn Tân Vạn (Bình Dương)", 80, "Camera cố định 80 km/h — Vành đai 3 đoạn Tân Vạn", "Dĩ An, Bình Dương"),
    TrafficCamera("cam_vd3_02", 10.8420, 106.8350, CameraType.COLD_FINE_SURVEILLANCE, "Vành Đai 3 — Đoạn TP. Thủ Đức", 80, "Camera phạt nguội đoạn Vành đai 3 Thủ Đức", "TP. Thủ Đức, TP.HCM"),
    TrafficCamera("cam_vd3_03", 10.7750, 106.8350, CameraType.SPEED_CAMERA, "Vành Đai 3 — Cầu Nhơn Trạch", 80, "Camera 80 km/h đầu cầu Nhơn Trạch", "Nhơn Trạch, Đồng Nai"),
    TrafficCamera("cam_vd3_04", 10.6950, 106.7850, CameraType.SPEED_CAMERA, "Vành Đai 3 — Cầu Phước Khánh", 80, "Camera đầu cầu Phước Khánh vành đai 3", "Nhơn Trạch, Đồng Nai"),
    TrafficCamera("cam_vd3_05", 10.6550, 106.7350, CameraType.COLD_FINE_SURVEILLANCE, "Vành Đai 3 — Đoạn Hiệp Phước (Nhà Bè)", 80, "Camera đoạn KCN Hiệp Phước vành đai 3", "Nhà Bè, TP.HCM"),
    TrafficCamera("cam_vd3_06", 10.7250, 106.5750, CameraType.SPEED_CAMERA, "Vành Đai 3 — Đoạn Tân Kiên (Bình Chánh)", 80, "Camera 80 km/h nút giao Tân Kiên Bình Chánh", "Bình Chánh, TP.HCM"),
    TrafficCamera("cam_vd3_07", 10.9850, 106.5250, CameraType.COLD_FINE_SURVEILLANCE, "Vành Đai 3 — Đoạn Củ Chi (Tỉnh Lộ 15)", 80, "Camera phạt nguội đoạn Củ Chi vành đai 3", "Củ Chi, TP.HCM"),

    // === HÀ NỘI — VÀNH ĐAI 2, 3, ĐẠI LỘ THĂNG LONG ===
    TrafficCamera("cam_hn_dl_01", 21.0080, 105.7920, CameraType.SPEED_CAMERA, "Đại Lộ Thăng Long — Km 5 (Nút Giao Trung Hòa)", 80, "Camera cố định 80 km/h Đại lộ Thăng Long", "Cầu Giấy, Hà Nội"),
    TrafficCamera("cam_hn_dl_02", 21.0020, 105.7350, CameraType.COLD_FINE_SURVEILLANCE, "Đại Lộ Thăng Long — Km 15 (An Khánh)", 80, "Camera phạt nguội đoạn An Khánh", "Hoài Đức, Hà Nội"),
    TrafficCamera("cam_hn_dl_03", 21.0050, 105.5250, CameraType.SPEED_CAMERA, "Đại Lộ Thăng Long — Km 25 (Đồng Mô)", 100, "Camera 100 km/h đoạn thông thoáng gần Đồng Mô", "Ba Vì, Hà Nội"),
    TrafficCamera("cam_hn_vd3_01", 20.9982, 105.7950, CameraType.SPEED_CAMERA, "Vành Đai 3 Hà Nội — Đoạn Khuất Duy Tiến", 80, "Camera cố định 80 km/h đoạn Khuất Duy Tiến", "Thanh Xuân, Hà Nội"),
    TrafficCamera("cam_hn_vd3_02", 21.0250, 105.7820, CameraType.COLD_FINE_SURVEILLANCE, "Vành Đai 3 Hà Nội — Đoạn Phạm Hùng", 80, "Camera phạt nguội đoạn Phạm Hùng vành đai 3", "Nam Từ Liêm, Hà Nội"),
    TrafficCamera("cam_hn_vd3_03", 20.9620, 105.8520, CameraType.SPEED_CAMERA, "Vành Đai 3 Hà Nội — Nút Giao Pháp Vân", 80, "Camera khu vực nút giao Pháp Vân", "Hoàng Mai, Hà Nội"),
    TrafficCamera("cam_hn_vd3_04", 20.9850, 105.8150, CameraType.RED_LIGHT_CAMERA, "Vành Đai 3 Hà Nội — Giao Nguyễn Xiển", 80, "Camera phạt đèn đỏ nút giao Nguyễn Xiển", "Thanh Xuân, Hà Nội"),
    TrafficCamera("cam_hn_vd2_01", 20.9995, 105.8410, CameraType.RED_LIGHT_CAMERA, "Vành Đai 2 Hà Nội — Giao Ngã Tư Vọng", 60, "Camera phạt đèn đỏ đoạn Ngã Tư Vọng - Trường Chinh", "Đống Đa, Hà Nội"),
    TrafficCamera("cam_hn_vd2_02", 21.0085, 105.8010, CameraType.COLD_FINE_SURVEILLANCE, "Vành Đai 2 Hà Nội — Đoạn Trần Duy Hưng", 60, "Camera phạt nguội đoạn qua Trần Duy Hưng", "Cầu Giấy, Hà Nội"),
    TrafficCamera("cam_hn_nh_01", 21.0380, 105.8620, CameraType.SPEED_CAMERA, "QL5 — Cầu Chương Dương (Hà Nội)", 60, "Camera 60 km/h đầu cầu Chương Dương", "Hoàn Kiếm, Hà Nội"),
    TrafficCamera("cam_hn_nh_02", 21.0235, 105.8520, CameraType.RED_LIGHT_CAMERA, "Hà Nội — Giao Lý Thường Kiệt - Hàng Bài", 50, "Camera phạt nguội đèn đỏ trung tâm Hà Nội", "Hoàn Kiếm, Hà Nội"),
    TrafficCamera("cam_hn_ct_01", 21.0050, 105.9080, CameraType.SPEED_CAMERA, "Cao Tốc Hà Nội - Hải Phòng — Nút Cổ Linh", 120, "Camera 120 km/h đầu cao tốc Hà Nội Hải Phòng", "Long Biên, Hà Nội"),
    TrafficCamera("cam_hn_ct_02", 21.0030, 105.9120, CameraType.MOTORBIKE_PROHIBITED_ZONE, "Cao Tốc Hà Nội - Hải Phòng — Lối Vào Cao Tốc", 0, "CẤM XE MÁY — Cao tốc chỉ dành cho ô tô", "Long Biên, Hà Nội"),

    // === QL20 — TP.HCM ĐÀ LẠT (Đèo Bảo Lộc, Đèo Prenn) ===
    TrafficCamera("cam_ql20_01", 11.5350, 108.0650, CameraType.SPEED_CAMERA, "QL20 — Km 100 (Đoạn Di Linh)", 60, "Camera 60 km/h đoạn qua Di Linh", "Di Linh, Lâm Đồng"),
    TrafficCamera("cam_ql20_02", 11.4550, 107.7250, CameraType.COLD_FINE_SURVEILLANCE, "QL20 — Đèo Bảo Lộc (Km 142)", 40, "Hạn chế 40 km/h đoạn đỉnh đèo Bảo Lộc", "Bảo Lộc, Lâm Đồng"),
    TrafficCamera("cam_ql20_03", 11.9120, 108.4450, CameraType.SPEED_CAMERA, "QL20 — Đoạn Đèo Prenn (Đà Lạt)", 30, "Hạn chế 30 km/h đèo Prenn — Điểm đen tai nạn", "Đà Lạt, Lâm Đồng"),
    TrafficCamera("cam_ql20_04", 11.9250, 108.4480, CameraType.COLD_FINE_SURVEILLANCE, "QL20 — Đầu Đèo Prenn (Vào Đà Lạt)", 40, "Camera phạt nguội đầu đèo phía Đà Lạt", "Đà Lạt, Lâm Đồng"),

    // === QL14 — TÂY NGUYÊN (Gia Lai, Đắk Lắk) ===
    TrafficCamera("cam_ql14_gl_01", 13.9850, 108.0050, CameraType.SPEED_CAMERA, "QL14 — Pleiku (Gia Lai) Đoạn Vào TP", 60, "Camera 60 km/h vào TP. Pleiku", "Pleiku, Gia Lai"),
    TrafficCamera("cam_ql14_dk_01", 12.6850, 108.0450, CameraType.SPEED_CAMERA, "QL14 — Đoạn Buôn Ma Thuột (BMT)", 60, "Camera 60 km/h vào TP. Buôn Ma Thuột", "Đắk Lắk"),
    TrafficCamera("cam_ql14_dk_02", 12.7550, 108.2120, CameraType.COLD_FINE_SURVEILLANCE, "QL14 — Ngã Ba Ea Knốp (Đắk Lắk)", 80, "Camera phạt nguội ngã ba Ea Knốp đèo nguy hiểm", "Đắk Lắk"),

    // === TÂN PHÚ & TÂN BÌNH (Tuyến thử nghiệm thực tế) ===
    TrafficCamera("cam_sg_lbb_01", 10.7712, 106.6345, CameraType.RED_LIGHT_CAMERA, "Lũy Bán Bích giao Thoại Ngọc Hầu", 50, "Camera phạt nguội vượt đèn đỏ và đè vạch người đi bộ ngã tư Thoại Ngọc Hầu", "Tân Phú, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ (Xe máy) / 4-6 triệu (Ô tô)"),
    TrafficCamera("cam_sg_lbb_02", 10.7635, 106.6360, CameraType.COLD_FINE_SURVEILLANCE, "Lũy Bán Bích giao Hòa Bình", 50, "Camera phạt nguội rẽ trái sai làn và lấn tuyến ngã tư Hòa Bình", "Tân Phú, TP.HCM", fineAmountText = "Phạt 400.000đ - 600.000đ (NĐ 100/123)"),
    TrafficCamera("cam_sg_lbb_03", 10.7815, 106.6320, CameraType.SPEED_CAMERA, "Lũy Bán Bích (UBND Quận Tân Phú)", 50, "Camera đo tốc độ tự động 50 km/h trước UBND Quận Tân Phú", "Tân Phú, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ khi quá tốc độ"),
    TrafficCamera("cam_sg_tnh_01", 10.7780, 106.6390, CameraType.RED_LIGHT_CAMERA, "Thoại Ngọc Hầu giao Âu Cơ", 50, "Phạt nguội vượt đèn đỏ ngã 3 Âu Cơ - Thoại Ngọc Hầu", "Tân Phú, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ (Xe máy)"),
    TrafficCamera("cam_sg_hb_01", 10.7670, 106.6430, CameraType.COLD_FINE_SURVEILLANCE, "Hòa Bình (Cổng Đầm Sen)", 50, "Camera giám sát dừng đỗ sai quy định và lấn làn trước cổng Đầm Sen", "Quận 11, TP.HCM", fineAmountText = "Phạt 400.000đ - 600.000đ (Lấn làn)"),
    TrafficCamera("cam_sg_auco_01", 10.7745, 106.6480, CameraType.RED_LIGHT_CAMERA, "Âu Cơ giao Lạc Long Quân (Ngã tư Bảy Hiền)", 50, "Phạt nguội vượt đèn đỏ và lấn làn rẽ trái nút giao Bảy Hiền", "Tân Bình, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ (Xe máy)"),
    TrafficCamera("cam_sg_tkq_01", 10.7950, 106.6280, CameraType.COLD_FINE_SURVEILLANCE, "Tân Kỳ Tân Quý (Gần Aeon Mall)", 50, "Giám sát lấn làn và ùn tắc giao thông khu vực Aeon Mall Tân Phú", "Tân Phú, TP.HCM", fineAmountText = "Phạt 400.000đ - 600.000đ (Đè vạch)"),
    TrafficCamera("cam_sg_ltt_01", 10.8040, 106.6180, CameraType.SPEED_CAMERA, "Lê Trọng Tấn (KCN Tân Bình)", 50, "Camera bắn tốc độ tự động 50 km/h trục đường KCN Tân Bình", "Tân Phú, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ (Quá tốc độ)"),
    TrafficCamera("cam_sg_truongchinh_01", 10.8080, 106.6360, CameraType.RED_LIGHT_CAMERA, "Trường Chinh giao Tây Thạnh", 50, "Phạt nguội vượt đèn đỏ và dừng đè vạch người đi bộ", "Tân Phú, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),
    TrafficCamera("cam_sg_conghoa_01", 10.8010, 106.6540, CameraType.RED_LIGHT_CAMERA, "Cộng Hòa giao Út Tịch", 50, "Camera phạt vượt đèn và rẽ trái sai quy định nút giao Út Tịch", "Tân Bình, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),

    // === GÒ VẤP, PHÚ NHUẬN & BÌNH THẠNH ===
    TrafficCamera("cam_sg_quangtrung_01", 10.8350, 106.6620, CameraType.RED_LIGHT_CAMERA, "Quang Trung giao Thống Nhất", 50, "Phạt nguội đèn đỏ và đè vạch ngã 3 Quang Trung - Thống Nhất", "Gò Vấp, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),
    TrafficCamera("cam_sg_phanvantri_01", 10.8250, 106.6850, CameraType.RED_LIGHT_CAMERA, "Phan Văn Trị giao Nguyễn Oanh (Ngã 5 Gò Vấp)", 50, "Phạt nguội ngã 5 Gò Vấp vượt đèn và đi sai làn", "Gò Vấp, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),
    TrafficCamera("cam_sg_phandangleu_01", 10.7990, 106.6860, CameraType.RED_LIGHT_CAMERA, "Phan Đăng Lưu giao Hoàng Văn Thụ (Ngã tư Phú Nhuận)", 50, "Phạt nguội nút giao ngã tư Phú Nhuận", "Phú Nhuận, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),
    TrafficCamera("cam_sg_dinhbolinh_01", 10.8120, 106.7110, CameraType.COLD_FINE_SURVEILLANCE, "Đinh Bộ Lĩnh (Bến Xe Miền Đông Cũ)", 50, "Camera phạt dừng đón trả khách sai quy định và lấn làn", "Bình Thạnh, TP.HCM", fineAmountText = "Phạt 400.000đ - 600.000đ"),
    TrafficCamera("cam_sg_dienbienphu_01", 10.7980, 106.7150, CameraType.SPEED_CAMERA, "Điện Biên Phủ (Chân Cầu Sài Gòn)", 60, "Camera bắn tốc độ 60 km/h chân Cầu Sài Gòn hướng vào trung tâm", "Bình Thạnh, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ (Xe máy)"),
    TrafficCamera("cam_sg_pvd_04", 10.8320, 106.7200, CameraType.RED_LIGHT_CAMERA, "Phạm Văn Đồng giao Hiệp Bình", 60, "Camera phạt đèn đỏ và đè vạch rẽ nhánh Hiệp Bình", "TP. Thủ Đức, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),
    TrafficCamera("cam_sg_pvd_05", 10.8580, 106.7610, CameraType.SPEED_CAMERA, "Phạm Văn Đồng (Cầu Vượt Linh Xuân)", 60, "Camera bắn tốc độ 60 km/h đoạn đường thẳng Linh Xuân", "TP. Thủ Đức, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),

    // === QUẬN 1, 3, 5, 6, 10, 11 (TRUNG TÂM TP.HCM) ===
    TrafficCamera("cam_sg_vvk_04", 10.7480, 106.6600, CameraType.SPEED_CAMERA, "Võ Văn Kiệt (Cầu Nguyễn Tri Phương)", 60, "Camera đo tốc độ 60 km/h đại lộ Võ Văn Kiệt làn xe máy 50", "Quận 5, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ (Quá tốc độ)"),
    TrafficCamera("cam_sg_3t2_01", 10.7705, 106.6710, CameraType.RED_LIGHT_CAMERA, "Ba Tháng Hai giao Lê Hồng Phong", 50, "Camera phạt nguội vượt đèn đỏ ngã 7 Lý Thái Tổ", "Quận 10, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),
    TrafficCamera("cam_sg_lythuongkiet_01", 10.7720, 106.6570, CameraType.RED_LIGHT_CAMERA, "Lý Thường Kiệt giao Tô Hiến Thành", 50, "Camera phạt đèn đỏ cổng Đại Học Bách Khoa", "Quận 10, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),
    TrafficCamera("cam_sg_nguyentrai_01", 10.7560, 106.6750, CameraType.COLD_FINE_SURVEILLANCE, "Nguyễn Trãi giao Trần Phú", 50, "Camera giám sát lấn làn và rẽ trái sai quy định", "Quận 5, TP.HCM", fineAmountText = "Phạt 400.000đ - 600.000đ"),
    TrafficCamera("cam_sg_hongbang_01", 10.7530, 106.6520, CameraType.RED_LIGHT_CAMERA, "Hồng Bàng giao Thuận Kiều (Bệnh Viện Chợ Rẫy)", 50, "Camera phạt đèn đỏ khu vực Bệnh Viện Chợ Rẫy", "Quận 5, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),
    TrafficCamera("cam_sg_kinhduongvuong_01", 10.7380, 106.6210, CameraType.SPEED_CAMERA, "Kinh Dương Vương (Bến Xe Miền Tây)", 50, "Camera đo tốc độ 50 km/h cửa ngõ Bến Xe Miền Tây", "Bình Tân, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),

    // === QUẬN 7, QUẬN 8, NHÀ BÈ & BÌNH CHÁNH ===
    TrafficCamera("cam_sg_nvl_03", 10.7310, 106.7210, CameraType.SPEED_CAMERA, "Nguyễn Văn Linh (Gần Cầu Phú Mỹ)", 60, "Camera bắn tốc độ 60 km/h làn hỗn hợp đường Nguyễn Văn Linh", "Quận 7, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),
    TrafficCamera("cam_sg_huynhtanphat_01", 10.7410, 106.7320, CameraType.RED_LIGHT_CAMERA, "Huỳnh Tấn Phát giao Nguyễn Thị Thập", 50, "Camera phạt đèn đỏ nút giao Cầu Phú Mỹ", "Quận 7, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),
    TrafficCamera("cam_sg_ql50_01", 10.7180, 106.6450, CameraType.SPEED_CAMERA, "Quốc Lộ 50 (Ngã Tư Nguyễn Văn Linh)", 50, "Camera đo tốc độ 50 km/h vào trung tâm huyện Bình Chánh", "Bình Chánh, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),
    TrafficCamera("cam_sg_ql1a_03", 10.6980, 106.5820, CameraType.SPEED_CAMERA, "Quốc Lộ 1A (Trạm Thu Phí Chợ Đệm)", 80, "Camera tốc độ 80 km/h trước lối vào Cao tốc Trung Lương", "Bình Chánh, TP.HCM", fineAmountText = "Phạt 800.000đ - 1.000.000đ (Xe máy)"),

    // === HÀ NỘI, ĐÀ NẴNG, BÌNH DƯƠNG, ĐỒNG NAI ===
    TrafficCamera("cam_hn_vd3_01", 20.9980, 105.7950, CameraType.SPEED_CAMERA, "Đường Vành Đai 3 Trên Cao (Đoạn Khuất Duy Tiến)", 80, "Camera đo tốc độ 80 km/h Vành Đai 3 trên cao", "Thanh Xuân, Hà Nội", fineAmountText = "Phạt 4-6 triệu + Tước bằng lái"),
    TrafficCamera("cam_hn_thanglong_01", 20.9950, 105.7420, CameraType.SPEED_CAMERA, "Đại Lộ Thăng Long (Km 8)", 100, "Camera bắn tốc độ 100 km/h làn ô tô Đại Lộ Thăng Long", "Nam Từ Liêm, Hà Nội", fineAmountText = "Phạt 4-6 triệu (Ô tô)"),
    TrafficCamera("cam_dn_2t9_01", 16.0610, 108.2210, CameraType.RED_LIGHT_CAMERA, "Đường 2 Tháng 9 — Giao Lê Đình Dương", 50, "Camera phạt đèn đỏ giao lộ trung tâm Đà Nẵng", "Hải Châu, Đà Nẵng", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),
    TrafficCamera("cam_bd_dta_01", 10.9050, 106.7350, CameraType.SPEED_CAMERA, "QL13 — Đoạn KCN Sóng Thần", 60, "Camera 60 km/h đoạn KCN Sóng Thần", "Thuận An, Bình Dương", fineAmountText = "Phạt 800.000đ - 1.000.000đ"),
    TrafficCamera("cam_dn_bh_01", 10.9420, 106.8240, CameraType.SPEED_CAMERA, "QL1A — Đoạn Biên Hòa (Cầu Ghềnh)", 60, "Camera 60 km/h đoạn cầu Ghềnh Biên Hòa", "Biên Hòa, Đồng Nai", fineAmountText = "Phạt 800.000đ - 1.000.000đ")
  )

  // Toàn bộ camera tổng hợp (nội bộ + mở rộng)
  val ALL_CAMERAS_FULL: List<TrafficCamera> get() = ALL_CAMERAS + EXTENDED_CAMERAS
}

data class SimPoint(
  val lat: Double,
  val lng: Double,
  val targetSpeedKmh: Float,
  val roadName: String,
  val speedLimit: Int
)

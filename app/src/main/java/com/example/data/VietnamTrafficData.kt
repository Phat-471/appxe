package com.example.data

import com.example.data.model.CameraType
import com.example.data.model.TrafficCamera
import kotlin.math.*

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

  // Popular Vietnam Destination Places (Google Maps style places & major streets)
  val POPULAR_PLACES: List<com.example.data.model.DestinationPlace> = listOf(
    // TP. Hồ Chí Minh - Địa điểm chính
    com.example.data.model.DestinationPlace("place_tsn", "Sân bay Quốc tế Tân Sơn Nhất", "Đường Trường Sơn, Phường 2, Tân Bình, TP.HCM", "Sân bay", 10.8184, 106.6588),
    com.example.data.model.DestinationPlace("place_ben_thanh", "Chợ Bến Thành", "Đường Lê Lợi, Phường Bến Thành, Quận 1, TP.HCM", "Địa điểm nổi tiếng", 10.7725, 106.6980),
    com.example.data.model.DestinationPlace("place_landmark81", "Tòa nhà Landmark 81", "720A Điện Biên Phủ, Vinhomes Central Park, Bình Thạnh", "Trung tâm", 10.7950, 106.7218),
    com.example.data.model.DestinationPlace("place_nguyen_hue", "Phố Đi Bộ Nguyễn Huệ", "Đường Nguyễn Huệ, Bến Nghé, Quận 1, TP.HCM", "Khu vui chơi", 10.7745, 106.7038),
    com.example.data.model.DestinationPlace("place_cho_ray", "Bệnh Viện Chợ Rẫy", "201B Nguyễn Chí Thanh, Phường 12, Quận 5, TP.HCM", "Bệnh viện", 10.7570, 106.6590),
    com.example.data.model.DestinationPlace("place_dai_hoc_bach_khoa", "Đại Học Bách Khoa TP.HCM", "268 Lý Thường Kiệt, Phường 14, Quận 10, TP.HCM", "Trường học", 10.7733, 106.6597),
    com.example.data.model.DestinationPlace("place_vincom_center", "Vincom Center Đồng Khởi", "72 Lê Thánh Tôn, Bến Nghé, Quận 1, TP.HCM", "Trung tâm", 10.7780, 106.7018),
    com.example.data.model.DestinationPlace("place_bx_mientay", "Bến Xe Miền Tây", "395 Kinh Dương Vương, An Lạc, Bình Tân, TP.HCM", "Bến xe", 10.7516, 106.6174),
    com.example.data.model.DestinationPlace("place_bx_miendong", "Bến Xe Miền Đông Mới", "Đường Hoàng Hữu Nam, Long Bình, TP. Thủ Đức, TP.HCM", "Bến xe", 10.8794, 106.8282),

    // TP. Hồ Chí Minh - Các Tuyến Đường Lớn
    com.example.data.model.DestinationPlace("street_sg_vvk", "Đại Lộ Võ Văn Kiệt", "Võ Văn Kiệt, Quận 1 - Quận 5 - Quận 6, TP.HCM", "Tuyến đường", 10.7530, 106.6710),
    com.example.data.model.DestinationPlace("street_sg_pvd", "Đường Phạm Văn Đồng", "Phạm Văn Đồng, Bình Thạnh - Gò Vấp - Thủ Đức, TP.HCM", "Tuyến đường", 10.8250, 106.6950),
    com.example.data.model.DestinationPlace("street_sg_nvl", "Đại Lộ Nguyễn Văn Linh", "Nguyễn Văn Linh, Quận 7 - Bình Chánh, TP.HCM", "Tuyến đường", 10.7285, 106.7082),
    com.example.data.model.DestinationPlace("street_sg_mct", "Đại Lộ Mai Chí Thọ", "Mai Chí Thọ, An Phú, TP. Thủ Đức, TP.HCM", "Tuyến đường", 10.7812, 106.7350),
    com.example.data.model.DestinationPlace("street_sg_cmt8", "Đường Cách Mạng Tháng 8", "Cách Mạng Tháng 8, Quận 3 - Quận 10 - Tân Bình, TP.HCM", "Tuyến đường", 10.7810, 106.6780),
    com.example.data.model.DestinationPlace("street_sg_dbp", "Đường Điện Biên Phủ", "Điện Biên Phủ, Quận 1 - Quận 3 - Bình Thạnh, TP.HCM", "Tuyến đường", 10.7915, 106.6990),
    com.example.data.model.DestinationPlace("street_sg_ntmk", "Đường Nguyễn Thị Minh Khai", "Nguyễn Thị Minh Khai, Quận 1 - Quận 3, TP.HCM", "Tuyến đường", 10.7760, 106.6910),
    com.example.data.model.DestinationPlace("street_sg_3t2", "Đường Ba Tháng Hai", "Đường 3 Tháng 2, Quận 10 - Quận 11, TP.HCM", "Tuyến đường", 10.7690, 106.6690),
    com.example.data.model.DestinationPlace("street_sg_ltk", "Đường Lý Thường Kiệt", "Lý Thường Kiệt, Quận 10 - Tân Bình, TP.HCM", "Tuyến đường", 10.7780, 106.6580),
    com.example.data.model.DestinationPlace("street_sg_ch", "Đường Cộng Hòa", "Đường Cộng Hòa, Phường 13, Tân Bình, TP.HCM", "Tuyến đường", 10.8030, 106.6470),
    com.example.data.model.DestinationPlace("street_sg_tc", "Đường Trường Chinh", "Đường Trường Chinh, Tân Bình - Quận 12, TP.HCM", "Tuyến đường", 10.8210, 106.6320),
    com.example.data.model.DestinationPlace("street_sg_thd", "Đường Trần Hưng Đạo", "Trần Hưng Đạo, Quận 1 - Quận 5, TP.HCM", "Tuyến đường", 10.7580, 106.6780),
    com.example.data.model.DestinationPlace("street_sg_ntr", "Đường Nguyễn Trãi", "Nguyễn Trãi, Quận 1 - Quận 5, TP.HCM", "Tuyến đường", 10.7605, 106.6775),
    com.example.data.model.DestinationPlace("street_sg_hbt", "Đường Hai Bà Trưng", "Hai Bà Trưng, Quận 1 - Quận 3, TP.HCM", "Tuyến đường", 10.7850, 106.6980),
    com.example.data.model.DestinationPlace("street_sg_nkkn", "Đường Nam Kỳ Khởi Nghĩa", "Nam Kỳ Khởi Nghĩa, Quận 1 - Quận 3, TP.HCM", "Tuyến đường", 10.7820, 106.6920),

    // Cây xăng chính
    com.example.data.model.DestinationPlace("place_petro_vvk", "Cây xăng Petrolimex Võ Văn Kiệt", "Đại lộ Võ Văn Kiệt, Quận 5, TP.HCM", "Cây xăng", 10.7555, 106.6790),
    com.example.data.model.DestinationPlace("place_petro_pvd", "Cây xăng Petrolimex Phạm Văn Đồng", "Phạm Văn Đồng, Hiệp Bình Chánh, Thủ Đức", "Cây xăng", 10.8350, 106.7210),
    com.example.data.model.DestinationPlace("place_petro_cmt8", "Cây xăng Comeco CMT8", "Cách Mạng Tháng 8, Phường 11, Quận 3, TP.HCM", "Cây xăng", 10.7790, 106.6750),

    // Hà Nội
    com.example.data.model.DestinationPlace("place_hoan_kiem", "Hồ Hoàn Kiếm Hà Nội", "Hàng Trống, Hoàn Kiếm, Hà Nội", "Địa điểm nổi tiếng", 21.0285, 105.8542),
    com.example.data.model.DestinationPlace("place_noi_bai", "Sân bay Quốc tế Nội Bài", "Xã Phú Minh, Huyện Sóc Sơn, Hà Nội", "Sân bay", 21.2212, 105.8072),
    com.example.data.model.DestinationPlace("street_hn_vd3", "Đường Vành Đai 3 Hà Nội", "Khuất Duy Tiến - Nguyễn Xiển, Thanh Xuân, Hà Nội", "Tuyến đường", 20.9982, 105.7950),
    com.example.data.model.DestinationPlace("street_hn_gp", "Đường Giải Phóng", "Giải Phóng, Hoàng Mai - Hai Bà Trưng, Hà Nội", "Tuyến đường", 20.9780, 105.8450),
    com.example.data.model.DestinationPlace("street_hn_cgh", "Đường Cầu Giấy", "Cầu Giấy, Quận Cầu Giấy, Hà Nội", "Tuyến đường", 21.0340, 105.7980),

    // Đà Nẵng
    com.example.data.model.DestinationPlace("place_cau_rong", "Cầu Rồng Đà Nẵng", "Đường Nguyễn Văn Linh, Phước Ninh, Hải Châu, Đà Nẵng", "Địa điểm nổi tiếng", 16.0610, 108.2230),
    com.example.data.model.DestinationPlace("place_san_bay_dn", "Sân bay Quốc tế Đà Nẵng", "Đường Duy Tân, Hòa Thuận Tây, Hải Châu, Đà Nẵng", "Sân bay", 16.0538, 108.2022),
    com.example.data.model.DestinationPlace("street_dn_ntt", "Đường Nguyễn Tất Thành", "Nguyễn Tất Thành, Thanh Khê - Liên Chiểu, Đà Nẵng", "Tuyến đường", 16.0790, 108.1920)
  )

  // Turn-by-Turn Navigation Route Generator
  fun generateTurnByTurnRoute(
    startLat: Double,
    startLng: Double,
    destLat: Double,
    destLng: Double,
    destName: String,
    destAddress: String
  ): com.example.data.model.NavigationRoute {
    val totalDirectDist = calculateDistanceMeters(startLat, startLng, destLat, destLng)
    val distanceMeters = (totalDirectDist * 1.28).toInt().coerceAtLeast(300)
    val durationMinutes = ((distanceMeters / 1000.0) / 32.0 * 60.0).toInt().coerceAtLeast(2)

    // Generate smooth polyline waypoints along road grid
    val waypoints = mutableListOf<Pair<Double, Double>>()
    waypoints.add(startLat to startLng)

    // Intermediate waypoints interpolation to simulate Google Maps turn grid
    val segmentsCount = 8
    val dLat = (destLat - startLat) / segmentsCount
    val dLng = (destLng - startLng) / segmentsCount

    for (i in 1 until segmentsCount) {
      // Add slight Manhattan/road grid curvature
      val offsetLat = if (i % 2 == 1) 0.0004 * sin(i.toDouble()) else -0.0003 * cos(i.toDouble())
      val offsetLng = if (i % 2 == 0) 0.0005 * cos(i.toDouble()) else 0.0002 * sin(i.toDouble())
      val lat = startLat + dLat * i + offsetLat
      val lng = startLng + dLng * i + offsetLng
      waypoints.add(lat to lng)
    }
    waypoints.add(destLat to destLng)

    // Generate Turn-by-Turn Maneuvers
    val steps = mutableListOf<com.example.data.model.NavigationStep>()
    steps.add(
      com.example.data.model.NavigationStep(
        instruction = "Bắt đầu khởi hành theo hướng đông",
        distanceMeters = (distanceMeters * 0.15).toInt(),
        maneuver = com.example.data.model.NavigationManeuverType.DEPART,
        roadName = "Tuyến đường hiện tại",
        latitude = startLat,
        longitude = startLng
      )
    )
    steps.add(
      com.example.data.model.NavigationStep(
        instruction = "Đi thẳng trên đường chính",
        distanceMeters = (distanceMeters * 0.35).toInt(),
        maneuver = com.example.data.model.NavigationManeuverType.STRAIGHT,
        roadName = "Đại lộ chính",
        latitude = waypoints[2].first,
        longitude = waypoints[2].second
      )
    )
    steps.add(
      com.example.data.model.NavigationStep(
        instruction = "Rẽ phải vào tuyến đường đến $destName",
        distanceMeters = (distanceMeters * 0.3).toInt(),
        maneuver = com.example.data.model.NavigationManeuverType.TURN_RIGHT,
        roadName = "Đường nhánh đô thị",
        latitude = waypoints[5].first,
        longitude = waypoints[5].second
      )
    )
    steps.add(
      com.example.data.model.NavigationStep(
        instruction = "Đến nơi: $destName ở bên phải",
        distanceMeters = (distanceMeters * 0.2).toInt(),
        maneuver = com.example.data.model.NavigationManeuverType.ARRIVE,
        roadName = destName,
        latitude = destLat,
        longitude = destLng
      )
    )

    return com.example.data.model.NavigationRoute(
      destinationName = destName,
      destinationAddress = destAddress,
      destinationLat = destLat,
      destinationLng = destLng,
      totalDistanceMeters = distanceMeters,
      estimatedDurationMinutes = durationMinutes,
      waypoints = waypoints,
      steps = steps,
      currentStepIndex = 0,
      isNavigating = true
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
}

data class SimPoint(
  val lat: Double,
  val lng: Double,
  val targetSpeedKmh: Float,
  val roadName: String,
  val speedLimit: Int
)

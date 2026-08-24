package com.example.data.model

enum class CameraType(val displayName: String, val iconDescription: String) {
  SPEED_CAMERA("Camera Bắn Tốc Độ", "Camera đo tốc độ tự động, phạt vi phạm tốc độ"),
  RED_LIGHT_CAMERA("Camera Phạt Nguội Đèn Đỏ", "Camera phạt nguội vượt đèn đỏ / đèn vàng"),
  COLD_FINE_SURVEILLANCE("Camera Phạt Nguội Lấn Làn", "Camera phạt nguội lấn làn, đè vạch liền, đi sai làn"),
  SECURITY_MONITORING("Camera An Ninh & Giám Sát", "Camera an ninh đô thị, giám sát trật tự và luồng giao thông"),
  SPEED_LIMIT_SIGN("Biển Báo Tốc Độ", "Biển báo giới hạn tốc độ xe máy & ô tô"),
  ZONE_RESIDENTIAL_ENTRY("Bắt Đầu Khu Dân Cư", "Biển báo vào khu đông dân cư (Tối đa 50 km/h)"),
  ZONE_RESIDENTIAL_EXIT("Hết Khu Dân Cư", "Biển báo hết khu dân cư (Tối đa 60 km/h)"),
  HAZARD_ACCIDENT_ZONE("Đoạn Đường Nguy Hiểm", "Khu vực hay xảy ra va chạm, đường cong gắt"),
  MOTORBIKE_PROHIBITED_ZONE("Đường Cấm Xe Máy / Cao Tốc", "Cấm xe máy đi vào - Phạt 2-3 triệu & tước bằng lái"),
  SCHOOL_ZONE("Khu Vực Trường Học", "Khu vực trường học, học sinh qua đường"),
  COMMUNITY_REPORT("Báo Cáo Cộng Đồng", "Điểm chốt kiểm tra tốc độ do tài xế báo")
}

data class TrafficCamera(
  val id: String,
  val latitude: Double,
  val longitude: Double,
  val type: CameraType,
  val roadName: String,
  val speedLimit: Int, // km/h
  val description: String,
  val districtCity: String,
  val verified: Boolean = true,
  val votesCount: Int = 12,
  val fineAmountText: String = "Phạt 800.000đ - 1.000.000đ (NĐ 100/123/NĐ-CP)",
  val source: String = "Hệ thống CSGT / Sở GTVT"
)

data class ActiveWarning(
  val camera: TrafficCamera,
  val distanceMeters: Int,
  val isOverspeeding: Boolean,
  val currentSpeedKmh: Int,
  val warningLevel: WarningLevel,
  val formattedMessage: String
)

enum class WarningLevel {
  NORMAL,
  CAUTION,
  DANGER
}

data class GpsLocationState(
  val latitude: Double = 10.7769, // Defaults to SG / VN coordinate
  val longitude: Double = 106.7009,
  val speedKmh: Float = 0f,
  val headingDegrees: Float = 0f,
  val altitudeMeters: Double = 0.0,
  val accuracyMeters: Float = 0f,
  val timestampMillis: Long = System.currentTimeMillis(),
  val detectedRoadName: String? = null,
  val detectedAddress: String? = null,
  val isGpsActive: Boolean = false,
  val isSimulated: Boolean = false,
  val provider: String = "GPS",
  val hasInitialFix: Boolean = false
)

data class BreadcrumbPoint(
  val latitude: Double,
  val longitude: Double,
  val speedKmh: Float,
  val timestamp: Long
)

data class TripSummary(
  val id: Long = 0,
  val startTimeMillis: Long,
  val endTimeMillis: Long,
  val title: String,
  val distanceKm: Float,
  val durationSeconds: Long,
  val maxSpeedKmh: Float,
  val avgSpeedKmh: Float,
  val overspeedEvents: Int,
  val camerasPassed: Int,
  val isCloudSynced: Boolean = false
)

data class OfflineMapPack(
  val id: String,
  val name: String,
  val regionCode: String,
  val sizeMb: Float,
  val version: String,
  val isDownloaded: Boolean,
  val downloadProgress: Float = 1.0f,
  val poiCount: Int,
  val cameraCount: Int,
  val lastUpdated: String
)

data class CloudSyncStatus(
  val isSyncing: Boolean = false,
  val lastSyncedTimeMillis: Long = System.currentTimeMillis(),
  val pendingItemsCount: Int = 0,
  val totalSyncedTrips: Int = 4,
  val cloudStorageUsedKb: Long = 128
)

enum class RoadType {
  HIGHWAY,
  ARTERIAL,
  BOULEVARD,
  RIVER,
  BRIDGE
}

data class MapRoadSegment(
  val id: String,
  val name: String,
  val type: RoadType,
  val coordinates: List<Pair<Double, Double>>, // lat, lng points
  val speedLimitKmh: Int = 50
)

enum class PoiType(val label: String, val iconEmoji: String) {
  GAS_STATION("Cây Xăng", "⛽"),
  TOLL_BOOTH("Trạm Thu Phí BOT", "🚧"),
  BRIDGE("Cầu Vượt / Cầu Lớn", "🌉"),
  HOSPITAL("Bệnh Viện Cấp Cứu", "🏥"),
  REST_STOP("Trạm Dừng Nghỉ", "🅿️"),
  TIRE_REPAIR("Cứu Hộ / Vá Lốp", "🔧"),
  ACCIDENT_HOTSPOT("Điểm Đen Tai Nạn", "⚠️")
}

data class MapPoi(
  val id: String,
  val name: String,
  val type: PoiType,
  val latitude: Double,
  val longitude: Double,
  val subtitle: String = ""
)

enum class NavigationManeuverType(val symbol: String, val vietnameseText: String) {
  DEPART("🚀", "Bắt đầu khởi hành"),
  STRAIGHT("⬆️", "Đi thẳng"),
  TURN_LEFT("⬅️", "Rẽ trái"),
  TURN_RIGHT("➡️", "Rẽ phải"),
  SLIGHT_LEFT("↖️", "Chếch sang trái"),
  SLIGHT_RIGHT("↗️", "Chếch sang phải"),
  SHARP_LEFT("↙️", "Rẽ gắt sang trái"),
  SHARP_RIGHT("↘️", "Rẽ gắt sang phải"),
  FORK_LEFT("⎇", "Đi theo nhánh bên trái"),
  FORK_RIGHT("🔀", "Đi theo nhánh bên phải"),
  ROUNDABOUT("🔄", "Vào vòng xuyến"),
  U_TURN("↩️", "Quay đầu xe"),
  ARRIVE("🏁", "Đến điểm đích")
}

data class NavigationStep(
  val instruction: String,
  val distanceMeters: Int,
  val maneuver: NavigationManeuverType,
  val roadName: String,
  val latitude: Double,
  val longitude: Double,
  val roundaboutExitNumber: Int = 0,
  val turnBearingDegrees: Float = 0f
)

enum class TrafficCongestion(val label: String, val colorHex: Long) {
  CLEAR("Thông thoáng", 0xFF10B981),      // Emerald Green
  MODERATE("Di chuyển chậm", 0xFFF59E0B), // Amber / Yellow
  HEAVY("Ùn tắc", 0xFFEF4444),            // Red
  BLOCKED("Kẹt xe nghiêm trọng", 0xFF991B1B) // Dark Red
}

data class RouteTrafficSegment(
  val startIndex: Int,
  val endIndex: Int,
  val congestion: TrafficCongestion = TrafficCongestion.CLEAR
)

enum class VehicleRoutingMode(val label: String, val iconEmoji: String, val description: String) {
  MOTORBIKE("Xe Máy", "🏍️", "Tránh cao tốc, cho phép đi đường ngõ/hẻm thuận tiện"),
  CAR("Ô Tô", "🚗", "Tối ưu đường lớn, thông báo phí BOT & tránh hẻm nhỏ")
}

data class NavigationRoute(
  val id: String = "route_${System.currentTimeMillis()}",
  val destinationName: String,
  val destinationAddress: String,
  val destinationLat: Double,
  val destinationLng: Double,
  val totalDistanceMeters: Int,
  val estimatedDurationMinutes: Int,
  val waypoints: List<Pair<Double, Double>>,
  val steps: List<NavigationStep>,
  val currentStepIndex: Int = 0,
  val isNavigating: Boolean = false,
  val trafficSegments: List<RouteTrafficSegment> = emptyList(),
  val overallCongestion: TrafficCongestion = TrafficCongestion.CLEAR,
  val routeTag: String = "Tối ưu nhất", // "Tối ưu nhất", "Ngắn nhất", "Tránh BOT", "Tuyến xe máy"
  val alternativeRoutes: List<NavigationRoute> = emptyList(),
  val isMotorbikeSafe: Boolean = true,
  val hasTollBooth: Boolean = false
)

data class DestinationPlace(
  val id: String,
  val name: String,
  val address: String,
  val category: String,
  val latitude: Double,
  val longitude: Double,
  val distanceKm: Float = 0f,
  val iconEmoji: String = "📍",
  val isRecent: Boolean = false,
  val isFavorite: Boolean = false
)

data class CurrentTripStats(
  val distanceKm: Float = 0f,
  val durationSeconds: Long = 0L,
  val maxSpeedKmh: Float = 0f,
  val avgSpeedKmh: Float = 0f,
  val overspeedEvents: Int = 0,
  val startTimeMillis: Long = 0L,
  val endTimeMillis: Long = 0L
)



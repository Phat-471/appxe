package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trip_records")
data class TripRecordEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val startTimeMillis: Long,
  val endTimeMillis: Long,
  val distanceKm: Float,
  val durationSeconds: Long,
  val maxSpeedKmh: Float,
  val avgSpeedKmh: Float,
  val overspeedEvents: Int,
  val camerasPassed: Int,
  val routePointsJson: String, // Stored serialized points
  val isCloudSynced: Boolean = false
)

@Entity(tableName = "community_cameras")
data class CommunityCameraEntity(
  @PrimaryKey val id: String,
  val latitude: Double,
  val longitude: Double,
  val typeName: String,
  val roadName: String,
  val speedLimit: Int,
  val description: String,
  val districtCity: String,
  val reportedTimeMillis: Long,
  val isCloudSynced: Boolean = false
)

@Entity(tableName = "offline_map_packs")
data class OfflineMapPackEntity(
  @PrimaryKey val id: String,
  val name: String,
  val regionCode: String,
  val sizeMb: Float,
  val version: String,
  val isDownloaded: Boolean,
  val poiCount: Int,
  val cameraCount: Int,
  val lastUpdated: String
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
  @PrimaryKey val id: Int = 1,
  val voiceAlertsEnabled: Boolean = true,
  val voiceVolume: Float = 1.0f,
  val speechRate: Float = 1.0f,
  val speedBufferKmh: Int = 0, // alert when speed > limit + buffer
  val alertDistanceMeters: Int = 500, // 300, 500, 800m
  val autoOfflineSync: Boolean = true,
  val cloudSyncEnabled: Boolean = true,
  val motorcycleType: String = "Xe Tay Ga / Xe Số",
  val hudMirrorMode: Boolean = false,
  val vibrateOnAlert: Boolean = true,
  val showSpeedCamerasOnMap: Boolean = true,
  val showRedLightCamerasOnMap: Boolean = true,
  val showSpeedLimitsOnMap: Boolean = true,
  val showCommunityReportsOnMap: Boolean = true,
  // === NEW SETTINGS ===
  val compassEnabled: Boolean = true,           // Cảm biến la bàn xoay hướng
  val backgroundServiceEnabled: Boolean = true, // Chạy nền khi tắt app
  val darkMapMode: Boolean = false,              // Bản đồ tối ban đêm
  val autoScreenOn: Boolean = true,              // Giữ màn hình sáng khi lái
  val showBreadcrumbs: Boolean = true,           // Hiện vết đi trên bản đồ
  val chimeOnAlert: Boolean = true,              // Tiếng bíp khi cảnh báo
  val speedUnit: String = "km/h",                // Đơn vị tốc độ
  val vehicleIconType: String = "SCOOTER",       // Loại biểu tượng xe trên bản đồ: ARROW, MOTORBIKE, SCOOTER, CAR, TRUCK
  // === SCANNER & ALERT FILTER TOGGLES ===
  val showProhibitedZones: Boolean = true,       // Quét cảnh báo đường cấm xe máy
  val showSecurityCameras: Boolean = true,       // Quét cảnh báo camera an ninh
  val showHazards: Boolean = true,               // Quét cảnh báo điểm đen tai nạn & trường học
  val showPois: Boolean = true,                  // Quét hiển thị trạm xăng, vá xe, cứu hộ
  val appLanguage: String = "vi",                // Ngôn ngữ ứng dụng: "vi" (Tiếng Việt), "en" (English)
  // === ADVANCED NAVIGATION & DISPLAY ===
  val vehicleIconScale: Float = 1.3f,            // Tỷ lệ kích thước icon xe: 0.9f (Nhỏ), 1.1f (Vừa), 1.35f (To), 1.7f (Rất to)
  val roadSnappingEnabled: Boolean = true,       // Bám tim đường mượt mà (Snap to Road Centerline)
  val highDpiMapEnabled: Boolean = true,         // Bản đồ Retina HD siêu nét (@2x)
  val mapTileSource: String = "GOOGLE_MAPS_HD",  // Nguồn tile bản đồ mặc định
  val floatingBubbleEnabled: Boolean = false,    // Cửa sổ nổi / Bong bóng Mini HUD đè lên Google Maps
  val mapCameraTilt3D: Boolean = true,           // Góc nhìn Camera 3D Tilt khi di chuyển
  val vehicle3DModel: String = "3D_SCOOTER"      // 3D_SCOOTER, 3D_MOTORBIKE, 3D_SPORT_CAR, 3D_ARROW
)

@Entity(tableName = "favorite_places")
data class FavoritePlaceEntity(
  @PrimaryKey val id: String,
  val name: String,
  val address: String,
  val category: String, // "HOME", "WORK", "FAVORITE", "GAS", "CUSTOM"
  val latitude: Double,
  val longitude: Double,
  val iconEmoji: String = "⭐",
  val createdAtMillis: Long = System.currentTimeMillis()
)


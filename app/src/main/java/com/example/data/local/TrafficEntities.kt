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
  val speedUnit: String = "km/h"                 // Đơn vị tốc độ
)


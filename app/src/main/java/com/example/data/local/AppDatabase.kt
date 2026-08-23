package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
  entities = [
    TripRecordEntity::class,
    CommunityCameraEntity::class,
    OfflineMapPackEntity::class,
    UserSettingsEntity::class
  ],
  version = 3,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun trafficDao(): TrafficDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN showSpeedCamerasOnMap INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN showRedLightCamerasOnMap INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN showSpeedLimitsOnMap INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN showCommunityReportsOnMap INTEGER NOT NULL DEFAULT 1")
      }
    }

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "speed_alert_vietnam.db"
        )
          .addMigrations(MIGRATION_1_2)
          .addCallback(object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
              super.onCreate(db)
              // Prepopulate default settings and offline packs
              CoroutineScope(Dispatchers.IO).launch {
                val dao = getDatabase(context).trafficDao()
                dao.saveUserSettings(
                  UserSettingsEntity(
                    id = 1,
                    voiceAlertsEnabled = true,
                    speedBufferKmh = 0,
                    alertDistanceMeters = 500,
                    motorcycleType = "Xe Tay Ga / Xe Số"
                  )
                )
                dao.insertOfflinePacks(
                  listOf(
                    OfflineMapPackEntity(
                      id = "vn_hcm_south",
                      name = "TP. Hồ Chí Minh & Đông Nam Bộ",
                      regionCode = "HCM_SGN",
                      sizeMb = 24.5f,
                      version = "v2026.08.1",
                      isDownloaded = true,
                      poiCount = 3840,
                      cameraCount = 428,
                      lastUpdated = "21/08/2026"
                    ),
                    OfflineMapPackEntity(
                      id = "vn_hn_north",
                      name = "Hà Nội & Vùng Thủ Đô",
                      regionCode = "HN_NORTH",
                      sizeMb = 21.8f,
                      version = "v2026.08.1",
                      isDownloaded = true,
                      poiCount = 3210,
                      cameraCount = 385,
                      lastUpdated = "21/08/2026"
                    ),
                    OfflineMapPackEntity(
                      id = "vn_central",
                      name = "Đà Nẵng & Miền Trung",
                      regionCode = "CENTRAL_VN",
                      sizeMb = 16.2f,
                      version = "v2026.07.4",
                      isDownloaded = false,
                      poiCount = 1920,
                      cameraCount = 180,
                      lastUpdated = "15/07/2026"
                    ),
                    OfflineMapPackEntity(
                      id = "vn_highways",
                      name = "Cao Tốc & Quốc Lộ Toàn Quốc (QL1A, QL51, QL20)",
                      regionCode = "HIGHWAYS_VN",
                      sizeMb = 31.0f,
                      version = "v2026.08.2",
                      isDownloaded = true,
                      poiCount = 4500,
                      cameraCount = 612,
                      lastUpdated = "20/08/2026"
                    )
                  )
                )

                // Prepopulate initial sample trip record
                dao.insertTrip(
                  TripRecordEntity(
                    title = "Lộ trình Võ Văn Kiệt - Quốc Lộ 1A",
                    startTimeMillis = System.currentTimeMillis() - 86400000L,
                    endTimeMillis = System.currentTimeMillis() - 86400000L + 2400000L,
                    distanceKm = 18.6f,
                    durationSeconds = 2400L,
                    maxSpeedKmh = 58.4f,
                    avgSpeedKmh = 37.2f,
                    overspeedEvents = 0,
                    camerasPassed = 4,
                    routePointsJson = "[]",
                    isCloudSynced = true
                  )
                )
              }
            }
          })
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}

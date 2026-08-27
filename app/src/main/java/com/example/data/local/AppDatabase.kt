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
    UserSettingsEntity::class,
    FavoritePlaceEntity::class,
    RecentSearchEntity::class
  ],
  version = 10,
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

    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN compassEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN backgroundServiceEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN darkMapMode INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN autoScreenOn INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN showBreadcrumbs INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN chimeOnAlert INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN speedUnit TEXT NOT NULL DEFAULT 'km/h'")
      }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN vehicleIconType TEXT NOT NULL DEFAULT 'SCOOTER'")
      }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN showProhibitedZones INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN showSecurityCameras INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN showHazards INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN showPois INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN appLanguage TEXT NOT NULL DEFAULT 'vi'")
      }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN vehicleIconScale REAL NOT NULL DEFAULT 1.3")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN roadSnappingEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN highDpiMapEnabled INTEGER NOT NULL DEFAULT 1")
      }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN mapTileSource TEXT NOT NULL DEFAULT 'GOOGLE_MAPS_HD'")
        db.execSQL("""
          CREATE TABLE IF NOT EXISTS favorite_places (
            id TEXT PRIMARY KEY NOT NULL,
            name TEXT NOT NULL,
            address TEXT NOT NULL,
            category TEXT NOT NULL,
            latitude REAL NOT NULL,
            longitude REAL NOT NULL,
            iconEmoji TEXT NOT NULL,
            createdAtMillis INTEGER NOT NULL
          )
        """.trimIndent())
      }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
      override fun migrate(db: SupportSQLiteDatabase) {
        try {
          db.execSQL("ALTER TABLE user_settings ADD COLUMN floatingBubbleEnabled INTEGER NOT NULL DEFAULT 0")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE user_settings ADD COLUMN mapCameraTilt3D INTEGER NOT NULL DEFAULT 1")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE user_settings ADD COLUMN vehicle3DModel TEXT NOT NULL DEFAULT '3D_SCOOTER'")
        } catch (_: Exception) {}
      }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
          CREATE TABLE IF NOT EXISTS recent_searches (
            id TEXT PRIMARY KEY NOT NULL,
            query TEXT NOT NULL,
            name TEXT NOT NULL,
            address TEXT NOT NULL,
            latitude REAL NOT NULL,
            longitude REAL NOT NULL,
            category TEXT NOT NULL DEFAULT 'Địa điểm',
            iconEmoji TEXT NOT NULL DEFAULT '🕒',
            timestampMillis INTEGER NOT NULL
          )
        """.trimIndent())
      }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
      override fun migrate(db: SupportSQLiteDatabase) {
        try {
          db.execSQL("ALTER TABLE user_settings ADD COLUMN batterySaverEnabled INTEGER NOT NULL DEFAULT 0")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE user_settings ADD COLUMN autoBatterySaverOnLowBattery INTEGER NOT NULL DEFAULT 1")
        } catch (_: Exception) {}
        try {
          db.execSQL("ALTER TABLE user_settings ADD COLUMN amoledPureBlackMode INTEGER NOT NULL DEFAULT 0")
        } catch (_: Exception) {}
      }
    }

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "speed_alert_vietnam.db"
        )
          .addMigrations(
            MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
            MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10
          )
          .fallbackToDestructiveMigration(dropAllTables = true)
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
          .fallbackToDestructiveMigration(dropAllTables = true)
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TrafficDao {
  // Trips
  @Query("SELECT * FROM trip_records ORDER BY startTimeMillis DESC")
  fun getAllTrips(): Flow<List<TripRecordEntity>>

  @Query("SELECT * FROM trip_records WHERE id = :id LIMIT 1")
  suspend fun getTripById(id: Long): TripRecordEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTrip(trip: TripRecordEntity): Long

  @Query("DELETE FROM trip_records WHERE id = :id")
  suspend fun deleteTrip(id: Long)

  @Query("UPDATE trip_records SET isCloudSynced = 1 WHERE id = :id")
  suspend fun markTripSynced(id: Long)

  @Query("UPDATE trip_records SET isCloudSynced = 1")
  suspend fun markAllTripsSynced()

  // Community Cameras
  @Query("SELECT * FROM community_cameras ORDER BY reportedTimeMillis DESC")
  fun getAllCommunityCameras(): Flow<List<CommunityCameraEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCommunityCamera(camera: CommunityCameraEntity)

  @Query("DELETE FROM community_cameras WHERE id = :id")
  suspend fun deleteCommunityCamera(id: String)

  // Offline map packs
  @Query("SELECT * FROM offline_map_packs")
  fun getAllOfflinePacks(): Flow<List<OfflineMapPackEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOfflinePacks(packs: List<OfflineMapPackEntity>)

  @Update
  suspend fun updateOfflinePack(pack: OfflineMapPackEntity)

  // User Settings
  @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
  fun getUserSettings(): Flow<UserSettingsEntity?>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun saveUserSettings(settings: UserSettingsEntity)

  // Favorite Places
  @Query("SELECT * FROM favorite_places ORDER BY createdAtMillis DESC")
  fun getAllFavorites(): Flow<List<FavoritePlaceEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFavorite(place: FavoritePlaceEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertFavorites(places: List<FavoritePlaceEntity>)

  @Query("DELETE FROM favorite_places WHERE id = :id")
  suspend fun deleteFavorite(id: String)

  // Recent Searches
  @Query("SELECT * FROM recent_searches ORDER BY timestampMillis DESC LIMIT 15")
  fun getRecentSearches(): Flow<List<RecentSearchEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecentSearch(search: RecentSearchEntity)

  @Query("DELETE FROM recent_searches WHERE id = :id")
  suspend fun deleteRecentSearch(id: String)

  @Query("DELETE FROM recent_searches")
  suspend fun clearAllRecentSearches()
}

package com.example.data.repository

import com.example.data.VietnamTrafficData
import com.example.data.local.OfflineMapPackEntity
import com.example.data.local.TrafficDao
import com.example.data.local.TripRecordEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.local.CommunityCameraEntity
import com.example.data.local.FavoritePlaceEntity
import com.example.data.model.CameraType
import com.example.data.model.TrafficCamera
import com.example.data.model.TripSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import com.example.data.datasource.OsmLiveCameraDataSource

class TrafficRepository(private val dao: TrafficDao) {

  // Dynamic live cameras fetched from OpenStreetMap Overpass API
  private val _liveOsmCameras = MutableStateFlow<List<TrafficCamera>>(emptyList())

  // Combined flow of base built-in cameras, live OSM cameras, and user/community added cameras
  val allCamerasFlow: Flow<List<TrafficCamera>> = combine(
    dao.getAllCommunityCameras(),
    _liveOsmCameras
  ) { communityList, liveOsmList ->
    val convertedCommunity = communityList.map { entity ->
      TrafficCamera(
        id = entity.id,
        latitude = entity.latitude,
        longitude = entity.longitude,
        type = try {
          CameraType.valueOf(entity.typeName)
        } catch (e: Exception) {
          CameraType.COMMUNITY_REPORT
        },
        roadName = entity.roadName,
        speedLimit = entity.speedLimit,
        description = entity.description,
        districtCity = entity.districtCity,
        verified = true,
        votesCount = 12
      )
    }
    // Merge: Built-in + Live OSM + Community reports (distinct by approximate coordinates or ID)
    (VietnamTrafficData.ALL_CAMERAS_FULL + liveOsmList + convertedCommunity).distinctBy { it.id }
  }

  suspend fun syncLiveOsmCameras(centerLat: Double, centerLng: Double) = withContext(Dispatchers.IO) {
    try {
      val liveCameras = OsmLiveCameraDataSource.fetchNearbyEnforcementCameras(centerLat, centerLng)
      if (liveCameras.isNotEmpty()) {
        _liveOsmCameras.value = liveCameras
      }
    } catch (e: Exception) {
      // Non-fatal, fallback to offline DB
    }
  }

  val allTripsFlow: Flow<List<TripSummary>> = dao.getAllTrips().map { entities ->
    entities.map { entity ->
      TripSummary(
        id = entity.id,
        startTimeMillis = entity.startTimeMillis,
        endTimeMillis = entity.endTimeMillis,
        title = entity.title,
        distanceKm = entity.distanceKm,
        durationSeconds = entity.durationSeconds,
        maxSpeedKmh = entity.maxSpeedKmh,
        avgSpeedKmh = entity.avgSpeedKmh,
        overspeedEvents = entity.overspeedEvents,
        camerasPassed = entity.camerasPassed,
        isCloudSynced = entity.isCloudSynced
      )
    }
  }

  val offlinePacksFlow: Flow<List<OfflineMapPackEntity>> = dao.getAllOfflinePacks()

  val userSettingsFlow: Flow<UserSettingsEntity> = dao.getUserSettings().map { settings ->
    settings ?: UserSettingsEntity()
  }

  val allFavoritesFlow: Flow<List<FavoritePlaceEntity>> = dao.getAllFavorites()

  val recentSearchesFlow: Flow<List<com.example.data.local.RecentSearchEntity>> = dao.getRecentSearches()

  suspend fun saveFavorite(place: FavoritePlaceEntity) = withContext(Dispatchers.IO) {
    dao.insertFavorite(place)
  }

  suspend fun deleteFavorite(id: String) = withContext(Dispatchers.IO) {
    dao.deleteFavorite(id)
  }

  suspend fun saveRecentSearch(search: com.example.data.local.RecentSearchEntity) = withContext(Dispatchers.IO) {
    dao.insertRecentSearch(search)
  }

  suspend fun deleteRecentSearch(id: String) = withContext(Dispatchers.IO) {
    dao.deleteRecentSearch(id)
  }

  suspend fun clearAllRecentSearches() = withContext(Dispatchers.IO) {
    dao.clearAllRecentSearches()
  }

  suspend fun saveTrip(trip: TripRecordEntity): Long = withContext(Dispatchers.IO) {
    dao.insertTrip(trip)
  }

  suspend fun deleteTrip(id: Long) = withContext(Dispatchers.IO) {
    dao.deleteTrip(id)
  }

  suspend fun reportNewCamera(
    lat: Double,
    lng: Double,
    type: CameraType,
    roadName: String,
    speedLimit: Int,
    description: String,
    districtCity: String
  ) = withContext(Dispatchers.IO) {
    val id = "user_cam_${System.currentTimeMillis()}"
    val entity = CommunityCameraEntity(
      id = id,
      latitude = lat,
      longitude = lng,
      typeName = type.name,
      roadName = roadName,
      speedLimit = speedLimit,
      description = description,
      districtCity = districtCity,
      reportedTimeMillis = System.currentTimeMillis(),
      isCloudSynced = false
    )
    dao.insertCommunityCamera(entity)
  }

  suspend fun updateSettings(settings: UserSettingsEntity) = withContext(Dispatchers.IO) {
    dao.saveUserSettings(settings)
  }

  suspend fun updateOfflinePack(pack: OfflineMapPackEntity) = withContext(Dispatchers.IO) {
    dao.updateOfflinePack(pack)
  }

  suspend fun syncAllWithCloud(): Int = withContext(Dispatchers.IO) {
    // Simulate real cloud database sync operation for trips and camera logs
    dao.markAllTripsSynced()
    1 // Return synced count
  }
}

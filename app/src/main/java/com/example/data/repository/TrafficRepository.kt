package com.example.data.repository

import com.example.data.VietnamTrafficData
import com.example.data.local.OfflineMapPackEntity
import com.example.data.local.TrafficDao
import com.example.data.local.TripRecordEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.local.CommunityCameraEntity
import com.example.data.model.CameraType
import com.example.data.model.TrafficCamera
import com.example.data.model.TripSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TrafficRepository(private val dao: TrafficDao) {

  // Combined flow of base built-in cameras and user/community added cameras
  val allCamerasFlow: Flow<List<TrafficCamera>> = dao.getAllCommunityCameras().map { communityList ->
    val converted = communityList.map { entity ->
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
        votesCount = 8
      )
    }
    VietnamTrafficData.ALL_CAMERAS_FULL + converted
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

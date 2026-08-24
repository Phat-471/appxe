package com.example.service

import com.example.data.VietnamTrafficData
import com.example.data.model.MapRoadSegment
import kotlin.math.*

/**
 * High-Precision GPS Snap-to-Road & Map-Matching Engine.
 * 
 * Uses orthogonal point-to-segment projection, bearing filtering, and cross-track error
 * damping to snap raw GPS coordinates cleanly to the centerline of the nearest matching road.
 * This prevents vehicle marker from jittering onto curbs, sidewalks, or parallel alleys.
 */
data class SnappedRoadPosition(
  val snappedLatitude: Double,
  val snappedLongitude: Double,
  val roadName: String,
  val speedLimit: Int,
  val crossTrackDistanceMeters: Double,
  val isSnapped: Boolean,
  val segmentBearing: Float
)

object MapMatchingEngine {

  private const val MAX_SNAP_RADIUS_METERS = 38.0 // Maximum distance to pull GPS point to centerline
  private const val MAX_BEARING_DELTA_DEG = 48.0f // Vehicle bearing must align with road heading
  private const val SPATIAL_ENVELOPE_DEG = 0.0035 // ~380m rough bounding box check

  // Road-Lock Hysteresis: Preference to stay on the current matched road to prevent hopping to side alleys
  private var lastMatchedRoadId: String? = null
  private var roadLockConsecutiveTicks: Int = 0

  /**
   * Snaps a raw GPS point to the nearest valid road segment based on location, heading and hysteresis.
   */
  fun snapToRoad(
    rawLat: Double,
    rawLng: Double,
    rawBearing: Float,
    speedKmh: Float,
    allRoads: List<MapRoadSegment> = VietnamTrafficData.ALL_ROADS
  ): SnappedRoadPosition {

    if (speedKmh < 1.0f && rawBearing == 0f) {
      // Vehicle is stationary, return raw without aggressive snapping
      return SnappedRoadPosition(
        snappedLatitude = rawLat,
        snappedLongitude = rawLng,
        roadName = "Vị trí hiện tại",
        speedLimit = 50,
        crossTrackDistanceMeters = 0.0,
        isSnapped = false,
        segmentBearing = 0f
      )
    }

    var bestSnappedLat = rawLat
    var bestSnappedLng = rawLng
    var minPerpendicularDist = Double.MAX_VALUE
    var matchedRoad: MapRoadSegment? = null
    var matchedSegmentBearing = rawBearing
    var foundValidSnap = false

    // 1. Spatial Pre-filtering & Hysteresis Evaluation
    for (road in allRoads) {
      val coords: List<Pair<Double, Double>> = road.coordinates
      if (coords.size < 2) continue

      // Rough Bounding Box Pruning: Skip road if entire bounding box is too far
      val minLat = road.coordinates.minOf { it.first } - SPATIAL_ENVELOPE_DEG
      val maxLat = road.coordinates.maxOf { it.first } + SPATIAL_ENVELOPE_DEG
      val minLng = road.coordinates.minOf { it.second } - SPATIAL_ENVELOPE_DEG
      val maxLng = road.coordinates.maxOf { it.second } + SPATIAL_ENVELOPE_DEG

      if (rawLat < minLat || rawLat > maxLat || rawLng < minLng || rawLng > maxLng) {
        continue
      }

      // Hysteresis bias: Give current locked road an 8-meter distance advantage against switching to alleys
      val isCurrentLockedRoad = (lastMatchedRoadId != null && road.id == lastMatchedRoadId)
      val hysteresisBonus = if (isCurrentLockedRoad) 8.0 else 0.0

      for (i in 0 until coords.size - 1) {
        val (latA, lngA) = coords[i]
        val (latB, lngB) = coords[i + 1]

        // Quick segment envelope check
        val segMinLat = min(latA, latB) - 0.0006
        val segMaxLat = max(latA, latB) + 0.0006
        val segMinLng = min(lngA, lngB) - 0.0006
        val segMaxLng = max(lngA, lngB) + 0.0006
        if (rawLat < segMinLat || rawLat > segMaxLat || rawLng < segMinLng || rawLng > segMaxLng) {
          continue
        }

        // 1. Calculate road segment heading
        val segmentBearing = VietnamTrafficData.calculateBearing(latA, lngA, latB, lngB)
        
        // Check alignment with vehicle direction (bidirectional)
        var headingDelta = abs(rawBearing - segmentBearing)
        while (headingDelta > 180f) headingDelta = abs(headingDelta - 360f)

        val reverseSegmentBearing = (segmentBearing + 180f) % 360f
        var reverseHeadingDelta = abs(rawBearing - reverseSegmentBearing)
        while (reverseHeadingDelta > 180f) reverseHeadingDelta = abs(reverseHeadingDelta - 360f)

        val isAlignedForward = headingDelta <= MAX_BEARING_DELTA_DEG
        val isAlignedBackward = reverseHeadingDelta <= MAX_BEARING_DELTA_DEG

        if (!isAlignedForward && !isAlignedBackward && speedKmh > 8.0f) {
          // Road segment is perpendicular: skip
          continue
        }

        // 2. Orthogonal Projection of (rawLat, rawLng) onto segment [A, B]
        val proj = projectPointToSegment(rawLat, rawLng, latA, lngA, latB, lngB)
        val rawDistToSegment = VietnamTrafficData.calculateDistanceMeters(rawLat, rawLng, proj.first, proj.second)
        val effectiveDist = (rawDistToSegment - hysteresisBonus).coerceAtLeast(0.0)

        if (effectiveDist < minPerpendicularDist && rawDistToSegment <= MAX_SNAP_RADIUS_METERS) {
          minPerpendicularDist = effectiveDist
          bestSnappedLat = proj.first
          bestSnappedLng = proj.second
          matchedRoad = road
          matchedSegmentBearing = if (isAlignedForward) segmentBearing else reverseSegmentBearing
          foundValidSnap = true
        }
      }
    }

    if (foundValidSnap && matchedRoad != null) {
      if (lastMatchedRoadId == matchedRoad.id) {
        roadLockConsecutiveTicks++
      } else {
        lastMatchedRoadId = matchedRoad.id
        roadLockConsecutiveTicks = 1
      }

      return SnappedRoadPosition(
        snappedLatitude = bestSnappedLat,
        snappedLongitude = bestSnappedLng,
        roadName = matchedRoad.name,
        speedLimit = matchedRoad.speedLimitKmh,
        crossTrackDistanceMeters = minPerpendicularDist,
        isSnapped = true,
        segmentBearing = matchedSegmentBearing
      )
    } else {
      lastMatchedRoadId = null
      roadLockConsecutiveTicks = 0
      return SnappedRoadPosition(
        snappedLatitude = rawLat,
        snappedLongitude = rawLng,
        roadName = matchedRoad?.name ?: "Vị trí hiện tại",
        speedLimit = matchedRoad?.speedLimitKmh ?: 50,
        crossTrackDistanceMeters = 0.0,
        isSnapped = false,
        segmentBearing = rawBearing
      )
    }
  }

  /**
   * Reset road lock hysteresis state when user cancels route or teleports.
   */
  fun resetHysteresis() {
    lastMatchedRoadId = null
    roadLockConsecutiveTicks = 0
  }

  /**
   * Orthogonal projection formula: Finds the closest point on segment AB to point P.
   */
  private fun projectPointToSegment(
    pLat: Double, pLng: Double,
    aLat: Double, aLng: Double,
    bLat: Double, bLng: Double
  ): Pair<Double, Double> {
    val meanLat = (aLat + bLat) / 2.0
    val cosLat = cos(Math.toRadians(meanLat))

    // Convert to planar Cartesian coordinates (meters relative to A)
    val dx = (bLng - aLng) * 111320.0 * cosLat
    val dy = (bLat - aLat) * 110574.0

    val px = (pLng - aLng) * 111320.0 * cosLat
    val py = (pLat - aLat) * 110574.0

    val segmentLengthSq = dx * dx + dy * dy
    if (segmentLengthSq < 1e-6) {
      return Pair(aLat, aLng)
    }

    // Scalar projection t = (AP · AB) / |AB|^2 clamped to [0, 1]
    val t = ((px * dx + py * dy) / segmentLengthSq).coerceIn(0.0, 1.0)

    val projLat = aLat + t * (bLat - aLat)
    val projLng = aLng + t * (bLng - aLng)

    return Pair(projLat, projLng)
  }
}

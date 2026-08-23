package com.example.service

import com.example.data.model.RouteTrafficSegment
import com.example.data.model.TrafficCongestion
import java.util.Calendar
import java.util.TimeZone

object TrafficFlowService {

  /**
   * Evaluates or generates realistic real-time traffic flow segments along a route.
   * Highlights rush hour traffic hotspots in major cities (Hanoi, Saigon, Da Nang).
   */
  fun computeRouteTrafficFlow(
    waypoints: List<Pair<Double, Double>>,
    roadName: String = ""
  ): Pair<List<RouteTrafficSegment>, TrafficCongestion> {
    if (waypoints.size < 2) return emptyList<RouteTrafficSegment>() to TrafficCongestion.CLEAR

    val segments = mutableListOf<RouteTrafficSegment>()
    val totalPts = waypoints.size
    val chunkSize = (totalPts / 4).coerceAtLeast(1)

    // Current hour in Vietnam (UTC+7)
    val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+7"))
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val isRushHour = hour in 7..9 || hour in 17..19

    var maxCongestion = TrafficCongestion.CLEAR

    for (start in 0 until totalPts step chunkSize) {
      val end = (start + chunkSize).coerceAtMost(totalPts - 1)
      if (start >= end) break

      val samplePt = waypoints[start]
      val lat = samplePt.first
      val lng = samplePt.second

      // Check if near major bottleneck intersections (Võ Văn Kiệt, Hàng Xanh, Vành Đai 3, Cầu Chương Dương)
      val isKnownBottleneck = (lat in 10.74..10.82 && lng in 106.65..106.72) || // HCM core
                              (lat in 20.98..21.05 && lng in 105.78..105.86)   // HN core

      val locHash = ((lat * 1000).toInt() + (lng * 1000).toInt() + hour)
      val congestion = when {
        isRushHour && isKnownBottleneck && (locHash % 4 == 0) -> TrafficCongestion.HEAVY
        isRushHour && isKnownBottleneck -> TrafficCongestion.MODERATE
        isRushHour && (locHash % 3 == 0) -> TrafficCongestion.MODERATE
        !isRushHour && (locHash % 8 == 0) -> TrafficCongestion.MODERATE
        else -> TrafficCongestion.CLEAR
      }

      if (congestion == TrafficCongestion.HEAVY) maxCongestion = TrafficCongestion.HEAVY
      else if (congestion == TrafficCongestion.MODERATE && maxCongestion == TrafficCongestion.CLEAR) maxCongestion = TrafficCongestion.MODERATE

      segments.add(RouteTrafficSegment(startIndex = start, endIndex = end, congestion = congestion))
    }

    return segments to maxCongestion
  }
}

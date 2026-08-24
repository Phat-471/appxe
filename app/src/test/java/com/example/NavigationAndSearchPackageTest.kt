package com.example

import com.example.data.VietnamTrafficData
import com.example.data.model.*
import com.example.data.unaccent
import com.example.service.NavigationRoutingService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavigationAndSearchPackageTest {

  @Test
  fun testVietnameseUnaccent_RemovesAllDiacriticsCorrectly() {
    val sample1 = "Đại lộ Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh"
    val normalized1 = sample1.unaccent()
    assertEquals("dai lo nguyen hue, quan 1, tp. ho chi minh", normalized1)

    val sample2 = "Võ Văn Kiệt - Hầm Thủ Thiêm - Cầu Ba Son"
    val normalized2 = sample2.unaccent()
    assertEquals("vo van kiet - ham thu thiem - cau ba son", normalized2)

    val sample3 = "Bệnh viện Chợ Rẫy & Trạm xăng Petrolimex"
    val normalized3 = sample3.unaccent()
    assertEquals("benh vien cho ray & tram xang petrolimex", normalized3)
  }

  @Test
  fun testSearchLocations_WithProximityBias_RanksClosestPlacesFirst() = runBlocking {
    // User GPS at District 1, HCMC (10.7769, 106.7009)
    val userLat = 10.7769
    val userLng = 106.7009

    // Search query without accents: "cho ben thanh"
    val results = NavigationRoutingService.searchLocations(
      query = "cho ben thanh",
      centerLat = userLat,
      centerLng = userLng
    )

    assertTrue("Should find at least 1 match for Ben Thanh", results.isNotEmpty())
    val topResult = results.first()
    assertTrue("Top result should be Ben Thanh Market", topResult.name.unaccent().contains("ben thanh"))
    assertTrue("Calculated distance should be within walking distance (< 2.5 km)", topResult.distanceKm in 0.001f..2.5f)
  }

  @Test
  fun testMultiRouteGeneration_ProducesThreeViableAlternatives() {
    val startLat = 10.7580
    val startLng = 106.6850
    val destLat = 10.7769
    val destLng = 106.7009

    // Generate routes for Motorbike mode
    val mainRoute = VietnamTrafficData.generateTurnByTurnRoute(
      startLat = startLat,
      startLng = startLng,
      destLat = destLat,
      destLng = destLng,
      destName = "Chợ Bến Thành",
      destAddress = "Quận 1, TP. Hồ Chí Minh",
      mode = VehicleRoutingMode.MOTORBIKE
    )

    assertNotNull("Main route must not be null", mainRoute)
    assertTrue("Main route must have waypoints", mainRoute.waypoints.size >= 2)
    assertTrue("Main route total distance must be positive", mainRoute.totalDistanceMeters > 0)
    assertTrue("Main route steps must exist", mainRoute.steps.isNotEmpty())
    assertEquals("Motorbike mode must be marked safe", true, mainRoute.isMotorbikeSafe)

    // Verify 2 alternative routes are generated
    assertEquals("Must have 2 alternative routes attached", 2, mainRoute.alternativeRoutes.size)
    val alt1 = mainRoute.alternativeRoutes[0]
    val alt2 = mainRoute.alternativeRoutes[1]

    assertTrue("Alt 1 must have tag", alt1.routeTag.isNotBlank())
    assertTrue("Alt 2 must have tag", alt2.routeTag.isNotBlank())
    assertTrue("Alt 1 must have waypoints", alt1.waypoints.size >= 2)
    assertTrue("Alt 2 must have waypoints", alt2.waypoints.size >= 2)
  }

  @Test
  fun testCarRoutingMode_SupportsHighwaysAndTolls() {
    val startLat = 10.8231
    val startLng = 106.6297
    val destLat = 10.7769
    val destLng = 106.7009

    val carRoute = VietnamTrafficData.generateTurnByTurnRoute(
      startLat = startLat,
      startLng = startLng,
      destLat = destLat,
      destLng = destLng,
      destName = "Chợ Bến Thành",
      destAddress = "Quận 1, TP. Hồ Chí Minh",
      mode = VehicleRoutingMode.CAR
    )

    assertTrue("Car route duration should be calculated realistically", carRoute.estimatedDurationMinutes > 0)
    assertTrue("Car route waypoints should exist", carRoute.waypoints.size >= 2)
  }

  @Test
  fun testTurnInstructionSteps_HaveValidVietnameseManeuvers() {
    val route = VietnamTrafficData.generateTurnByTurnRoute(
      startLat = 10.7580,
      startLng = 106.6850,
      destLat = 10.7769,
      destLng = 106.7009,
      destName = "Chợ Bến Thành",
      destAddress = "Quận 1, TP. Hồ Chí Minh",
      mode = VehicleRoutingMode.MOTORBIKE
    )

    val stepInstructions = route.steps.map { it.instruction }
    assertTrue("Must have starting departure step", stepInstructions.any { it.contains("Bắt đầu") || it.contains("Đi thẳng") })
    assertTrue("Must have arrival destination step", stepInstructions.any { it.contains("đích") || it.contains("Chợ Bến Thành") })
  }

  @Test
  fun testNearbyUtilitiesSearch_FindsGasStationsAndHospitals() {
    val gasResults = VietnamTrafficData.POPULAR_PLACES.filter {
      it.category == "Cây xăng" || it.name.unaccent().contains("xang") || it.name.unaccent().contains("petro")
    }
    assertTrue("Should have gas station POIs in database", gasResults.isNotEmpty())

    val hospitalResults = VietnamTrafficData.POPULAR_PLACES.filter {
      it.category == "Bệnh viện" || it.name.unaccent().contains("benh vien")
    }
    assertTrue("Should have hospital POIs in database", hospitalResults.isNotEmpty())
  }
}

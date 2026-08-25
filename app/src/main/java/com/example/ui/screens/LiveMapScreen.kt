package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import com.example.data.VietnamTrafficData
import com.example.data.local.UserSettingsEntity
import com.example.data.model.*
import com.example.service.FloatingSpeedBubbleService
import com.example.service.NavigationRoutingService
import com.example.service.WarningEvaluationResult
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LiveMapScreen(
  locationState: GpsLocationState,
  trafficEvaluation: WarningEvaluationResult,
  cameras: List<TrafficCamera>,
  breadcrumbs: List<BreadcrumbPoint>,
  activeRoute: NavigationRoute? = null,
  tripStats: CurrentTripStats? = null,
  isRecordingTrip: Boolean = false,
  lastFinishedTrip: CurrentTripStats? = null,
  voiceEnabled: Boolean = true,
  compassHeading: Float = 0f,
  userSettings: UserSettingsEntity = UserSettingsEntity(),
  favorites: List<com.example.data.local.FavoritePlaceEntity> = emptyList(),
  recentSearches: List<com.example.data.local.RecentSearchEntity> = emptyList(),
  vehicleRoutingMode: VehicleRoutingMode = VehicleRoutingMode.MOTORBIKE,
  onToggleVoice: () -> Unit = {},
  onToggleTripRecording: () -> Unit = {},
  onCloseTripSummary: () -> Unit = {},
  onToggleGpsOrSimulation: (Boolean) -> Unit = {},
  onStartNavigation: (DestinationPlace) -> Unit = {},
  onStartCustomNavigation: (name: String, address: String, lat: Double, lng: Double) -> Unit = { _, _, _, _ -> },
  onSwitchActiveRoute: (NavigationRoute) -> Unit = {},
  onSetVehicleRoutingMode: (VehicleRoutingMode) -> Unit = {},
  onDeleteRecentSearch: (String) -> Unit = {},
  onClearAllRecentSearches: () -> Unit = {},
  onCancelNavigation: () -> Unit = {},
  onSelectRoute: (Int) -> Unit = {},
  onSetSpeed: (Float) -> Unit = {},
  onSetCustomRoad: (String) -> Unit = {},
  onTestSound: () -> Unit = {},
  onSpeakAlert: (String) -> Unit = {},
  onSaveFavorite: (name: String, address: String, category: String, lat: Double, lng: Double, icon: String) -> Unit = { _, _, _, _, _, _ -> },
  onDeleteFavorite: (id: String) -> Unit = {},
  onSearchNearbyUtilities: (suspend (String) -> List<DestinationPlace>)? = null,
  onRefreshLocation: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()
  var inspectingCamera by remember { mutableStateOf<TrafficCamera?>(null) }
  var inspectingPoi by remember { mutableStateOf<MapPoi?>(null) }
  var selectedLayerFilter by remember { mutableStateOf("Tất cả") }
  var showDestinationSearchDialog by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategoryFilter by remember { mutableStateOf("Tất cả") }
  var showRoadSelectDialog by remember { mutableStateOf(false) }
  var showSpeedLimitPicker by remember { mutableStateOf(false) }
  var customRoadInput by remember { mutableStateOf("") }
  var previewTapPlace by remember { mutableStateOf<DestinationPlace?>(null) }

  // Quick Utilities & Favorite Sheet States
  var activeUtilityCategory by remember { mutableStateOf<String?>(null) }
  var utilityPlacesList by remember { mutableStateOf<List<DestinationPlace>>(emptyList()) }
  var isUtilityLoading by remember { mutableStateOf(false) }
  var showFavoriteToast by remember { mutableStateOf<String?>(null) }

  // Online Geocoding Search States
  var searchResults by remember { mutableStateOf<List<DestinationPlace>>(emptyList()) }
  var isSearchingOnline by remember { mutableStateOf(false) }
  var searchJob by remember { mutableStateOf<Job?>(null) }

  val activeWarning = trafficEvaluation.activeWarning
  val nearestCam = trafficEvaluation.nearestCamera
  val currentSpeedLimit = if (trafficEvaluation.currentSpeedLimit > 0) trafficEvaluation.currentSpeedLimit else 50
  val isOverspeed = trafficEvaluation.isOverspeeding

  val allPlaces = remember { VietnamTrafficData.POPULAR_PLACES }
  val allPois = remember { VietnamTrafficData.ALL_POIS }

  // Calculate upcoming cameras list ahead with distances
  val camerasAhead = remember(cameras, locationState) {
    cameras.mapNotNull { cam ->
      val dist = VietnamTrafficData.calculateDistanceMeters(
        locationState.latitude, locationState.longitude,
        cam.latitude, cam.longitude
      ).toInt()
      if (dist in 15..950) cam to dist else null
    }.sortedBy { it.second }
  }

  // Calculate upcoming POIs ahead
  val poisAhead = remember(allPois, locationState) {
    allPois.mapNotNull { poi ->
      val dist = VietnamTrafficData.calculateDistanceMeters(
        locationState.latitude, locationState.longitude,
        poi.latitude, poi.longitude
      ).toInt()
      if (dist in 15..1500) poi to dist else null
    }.sortedBy { it.second }
  }

  // Layer filtered lists for canvas (Pre-filtered within 4.5km radius for 60FPS fluid canvas)
  val filteredCameras = remember(cameras, userSettings, selectedLayerFilter, locationState.latitude, locationState.longitude) {
    val nearCameras = cameras.filter { cam ->
      VietnamTrafficData.calculateDistanceMeters(
        locationState.latitude, locationState.longitude,
        cam.latitude, cam.longitude
      ) <= 4500.0
    }

    when (selectedLayerFilter) {
      "Bắn tốc độ" -> nearCameras.filter { it.type == CameraType.SPEED_CAMERA || it.type == CameraType.SPEED_LIMIT_SIGN }
      "Phạt nguội" -> nearCameras.filter { it.type == CameraType.RED_LIGHT_CAMERA || it.type == CameraType.COLD_FINE_SURVEILLANCE }
      "Camera an ninh" -> nearCameras.filter { it.type == CameraType.SECURITY_MONITORING }
      "Cây xăng", "Trạm BOT", "Cứu hộ/Y tế", "Điểm đen" -> emptyList()
      else -> {
        nearCameras.filter { cam ->
          when (cam.type) {
            CameraType.SPEED_CAMERA, CameraType.COLD_FINE_SURVEILLANCE -> userSettings.showSpeedCamerasOnMap
            CameraType.RED_LIGHT_CAMERA -> userSettings.showRedLightCamerasOnMap
            CameraType.SPEED_LIMIT_SIGN, CameraType.ZONE_RESIDENTIAL_ENTRY, CameraType.ZONE_RESIDENTIAL_EXIT -> userSettings.showSpeedLimitsOnMap
            CameraType.COMMUNITY_REPORT -> userSettings.showCommunityReportsOnMap
            else -> true
          }
        }
      }
    }
  }

  val displayedPois = remember(allPois, selectedLayerFilter, locationState.latitude, locationState.longitude) {
    val nearPois = allPois.filter { poi ->
      VietnamTrafficData.calculateDistanceMeters(
        locationState.latitude, locationState.longitude,
        poi.latitude, poi.longitude
      ) <= 4000.0
    }

    when (selectedLayerFilter) {
      "Tất cả" -> nearPois
      "Cây xăng" -> nearPois.filter { it.type == PoiType.GAS_STATION }
      "Trạm BOT" -> nearPois.filter { it.type == PoiType.TOLL_BOOTH }
      "Cứu hộ/Y tế" -> nearPois.filter { it.type == PoiType.HOSPITAL || it.type == PoiType.TIRE_REPAIR }
      "Điểm đen" -> nearPois.filter { it.type == PoiType.ACCIDENT_HOTSPOT }
      "Bắn tốc độ", "Phạt nguội", "Camera an ninh" -> emptyList()
      else -> nearPois
    }
  }

  // Debounced live geocoding search for street names and addresses with Proximity Bias
  LaunchedEffect(searchQuery, selectedCategoryFilter, locationState.latitude, locationState.longitude) {
    searchJob?.cancel()
    if (searchQuery.isBlank()) {
      searchResults = allPlaces.filter {
        selectedCategoryFilter == "Tất cả" || it.category == selectedCategoryFilter
      }
      isSearchingOnline = false
    } else {
      isSearchingOnline = true
      searchJob = coroutineScope.launch {
        delay(300)
        val results = NavigationRoutingService.searchLocations(
          query = searchQuery,
          centerLat = locationState.latitude,
          centerLng = locationState.longitude
        )
        searchResults = if (selectedCategoryFilter == "Tất cả") {
          results
        } else {
          results.filter { it.category == selectedCategoryFilter }
        }
        isSearchingOnline = false
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFFF1F5F9))
  ) {
    // 1. FULLSCREEN HIGH-PERFORMANCE VECTOR MAP CANVAS
    OfflineMapCanvas(
      locationState = locationState,
      cameras = filteredCameras,
      breadcrumbs = breadcrumbs,
      activeRoute = activeRoute,
      nearestCamera = nearestCam,
      nearestCameraDistance = trafficEvaluation.nearestCameraDistance,
      activeWarning = trafficEvaluation.activeWarning,
      targetFocusPlace = previewTapPlace,
      pois = displayedPois,
      onSelectCamera = { inspectingCamera = it },
      onSelectPoi = { inspectingPoi = it },
      onMapTapLocation = { lat, lng ->
        coroutineScope.launch {
          val streetName = NavigationRoutingService.reverseGeocode(lat, lng)
            ?: "Toạ độ: ${String.format(java.util.Locale.US, "%.4f", lat)}, ${String.format(java.util.Locale.US, "%.4f", lng)}"

          previewTapPlace = DestinationPlace(
            id = "custom_tap_${System.currentTimeMillis()}",
            name = streetName,
            address = "Vị trí đã chọn trên bản đồ (${String.format(java.util.Locale.US, "%.4f", lat)}, ${String.format(java.util.Locale.US, "%.4f", lng)})",
            category = "Bản đồ",
            latitude = lat,
            longitude = lng
          )
        }
      },
      onRefreshLocation = onRefreshLocation,
      onToggleVoice = onToggleVoice,
      voiceEnabled = voiceEnabled,
      compassHeading = compassHeading,
      compassEnabled = userSettings.compassEnabled,
      darkMapMode = userSettings.darkMapMode,
      showBreadcrumbs = userSettings.showBreadcrumbs,
      vehicleIconType = when (userSettings.vehicleIconType) {
        "MOTORBIKE" -> VehicleIconType.MOTORBIKE
        "CAR" -> VehicleIconType.CAR
        "TRUCK" -> VehicleIconType.TRUCK
        "ARROW" -> VehicleIconType.ARROW
        else -> VehicleIconType.SCOOTER  // "SCOOTER" default
      },
      vehicleIconScale = userSettings.vehicleIconScale,
      roadSnappingEnabled = userSettings.roadSnappingEnabled,
      cameraTilt3D = userSettings.mapCameraTilt3D,
      modifier = Modifier.fillMaxSize()
    )

    // 2. TOP HEADER: VIETMAP LIVE LANE GUIDANCE OR SEARCH PILL
    if (activeRoute != null && activeRoute.isNavigating) {
      val targetIndex = if (activeRoute.currentStepIndex == 0 && activeRoute.steps.size > 1 && activeRoute.steps[0].maneuver == NavigationManeuverType.DEPART) {
        1
      } else {
        (activeRoute.currentStepIndex + 1).coerceAtMost(activeRoute.steps.size - 1)
      }
      val currentStep = activeRoute.steps.getOrNull(targetIndex)
        ?: activeRoute.steps.lastOrNull()

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 10.dp)
      ) {
        VietmapTopLaneGuidanceBanner(
          turnDistanceMeters = currentStep?.distanceMeters ?: activeRoute.totalDistanceMeters,
          turnInstruction = currentStep?.instruction ?: "Đi thẳng",
          overallCongestion = activeRoute.overallCongestion
        )
      }
    } else {
      // Top Search Pill (Vietmap Dark Theme Style)
      Column(
        modifier = Modifier
          .align(Alignment.TopCenter)
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = Color.White.copy(alpha = 0.97f),
          shadowElevation = 8.dp,
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showDestinationSearchDialog = true }
            .testTag("search_bar_trigger")
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = "Tìm kiếm",
              tint = Color(0xFF1E88E5),
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = if (searchQuery.isNotBlank()) searchQuery else "Tìm kiếm... (BETA)",
              style = MaterialTheme.typography.bodyMedium,
              color = if (searchQuery.isNotBlank()) Color(0xFF1E293B) else Color(0xFF64748B),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal Map Layer & Utility Filter Chips (1-tap access)
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          val filterOptions = listOf("Tất cả", "⛽ Cây xăng", "🏦 Ngân hàng/ATM", "🔧 Sửa xe/Vá", "🏥 Cứu hộ/Y tế", "☕ Ăn uống/Cafe", "🅿️ Bãi đỗ xe", "⭐ Yêu thích", "Bắn tốc độ", "Phạt nguội", "Trạm BOT")
          items(filterOptions) { filter ->
            val isSelected = selectedLayerFilter == filter
            val (icon, badgeColor) = when {
              filter.contains("Cây xăng") -> "⛽" to Color(0xFFF97316)
              filter.contains("Ngân hàng") -> "🏦" to Color(0xFF0284C7)
              filter.contains("Sửa xe") -> "🔧" to Color(0xFFD97706)
              filter.contains("Cứu hộ") || filter.contains("Y tế") -> "🏥" to Color(0xFF10B981)
              filter.contains("Ăn uống") || filter.contains("Cafe") -> "☕" to Color(0xFF8B5CF6)
              filter.contains("Bãi đỗ") -> "🅿️" to Color(0xFF6366F1)
              filter.contains("Yêu thích") -> "⭐" to Color(0xFFF59E0B)
              filter.contains("Bắn tốc độ") -> "🔴" to Color(0xFFEF4444)
              filter.contains("Phạt nguội") -> "🚦" to Color(0xFFDC2626)
              filter.contains("Trạm BOT") -> "🚧" to Color(0xFF0284C7)
              else -> "🗺️" to Color(0xFF1E88E5)
            }
            Surface(
              onClick = {
                selectedLayerFilter = filter
                val cleanCat = when {
                  filter.contains("Cây xăng") -> "Cây xăng"
                  filter.contains("Ngân hàng") -> "Ngân hàng / ATM"
                  filter.contains("Sửa xe") -> "Sửa xe / Vá vỏ"
                  filter.contains("Cứu hộ") || filter.contains("Y tế") -> "Bệnh viện & Cứu hộ Y tế"
                  filter.contains("Ăn uống") || filter.contains("Cafe") -> "Ăn uống & Cafe"
                  filter.contains("Bãi đỗ") -> "Bãi đỗ xe"
                  filter.contains("Yêu thích") -> "Địa điểm Yêu thích"
                  else -> null
                }
                if (cleanCat != null) {
                  activeUtilityCategory = cleanCat
                  if (cleanCat == "Địa điểm Yêu thích") {
                    utilityPlacesList = emptyList()
                  } else {
                    isUtilityLoading = true
                    coroutineScope.launch {
                      val searchKey = when (cleanCat) {
                        "Cây xăng" -> "xăng"
                        "Ngân hàng / ATM" -> "ngân hàng"
                        "Sửa xe / Vá vỏ" -> "sửa xe"
                        "Bệnh viện & Cứu hộ Y tế" -> "bệnh viện"
                        "Ăn uống & Cafe" -> "ăn"
                        "Bãi đỗ xe" -> "đỗ"
                        else -> cleanCat
                      }
                      utilityPlacesList = onSearchNearbyUtilities?.invoke(searchKey) ?: emptyList()
                      isUtilityLoading = false
                    }
                  }
                }
              },
              shape = RoundedCornerShape(14.dp),
              color = if (isSelected) badgeColor else Color.White.copy(alpha = 0.92f),
              shadowElevation = if (isSelected) 4.dp else 2.dp,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isSelected) badgeColor else Color(0xFFE2E8F0)
              )
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp)
              ) {
                Text(text = icon, fontSize = 10.5.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = filter.replace("⛽ ", "").replace("🏦 ", "").replace("🔧 ", "").replace("🏥 ", "").replace("☕ ", "").replace("🅿️ ", "").replace("⭐ ", ""),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                    fontSize = 10.5.sp
                  ),
                  color = if (isSelected) Color.White else Color(0xFF334155)
                )
              }
            }
          }
        }
      }
    }

    // 3. LEFT-SIDE VERTICAL STACK: APPROACHING HAZARD & CAMERA QUEUE (Vietmap Live Style)
    VietmapLeftHazardQueue(
      camerasAhead = camerasAhead,
      activeWarning = activeWarning,
      modifier = Modifier
        .align(Alignment.TopStart)
        .padding(start = 14.dp, top = if (activeRoute?.isNavigating == true) 90.dp else 75.dp)
    )

    // 3.5. RIGHT-SIDE FLOATING ACTION CONTROLS (Voice, Test Voice, Floating HUD Bubble, Report Camera)
    val isBubbleRunning by FloatingSpeedBubbleService.isServiceRunning.collectAsState()
    val context = LocalContext.current

    Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(end = 14.dp, top = if (activeRoute?.isNavigating == true) 90.dp else 75.dp)
    ) {
      // 1. Toggle Voice Alert
      Surface(
        onClick = { onToggleVoice() },
        shape = CircleShape,
        color = if (voiceEnabled) Color(0xFF0284C7) else Color(0xFF1E293B).copy(alpha = 0.92f),
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(
          1.2.dp,
          if (voiceEnabled) Color(0xFF38BDF8) else Color(0xFF475569)
        ),
        modifier = Modifier.size(42.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (voiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
            contentDescription = "Giọng nói cảnh báo",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // 2. Test Voice Prompt
      Surface(
        onClick = { onTestSound() },
        shape = CircleShape,
        color = Color(0xFF0F172A).copy(alpha = 0.94f),
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF10B981)),
        modifier = Modifier.size(42.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Hearing,
            contentDescription = "Thử giọng nói",
            tint = Color(0xFF10B981),
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // 3. Floating Speed Bubble HUD Toggle (Over Google Maps)
      Surface(
        onClick = {
          if (!isBubbleRunning) {
            if (!FloatingSpeedBubbleService.canDrawOverlay(context)) {
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                  Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                  Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
              }
            } else {
              FloatingSpeedBubbleService.startService(context)
            }
          } else {
            FloatingSpeedBubbleService.stopService(context)
          }
        },
        shape = CircleShape,
        color = if (isBubbleRunning) Color(0xFFF59E0B) else Color(0xFF0F172A).copy(alpha = 0.94f),
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(
          1.2.dp,
          if (isBubbleRunning) Color(0xFFFBBF24) else Color(0xFF38BDF8)
        ),
        modifier = Modifier.size(42.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Layers,
            contentDescription = "Bong bóng nổi",
            tint = if (isBubbleRunning) Color(0xFF0F172A) else Color(0xFF38BDF8),
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    // 4. BOTTOM-LEFT STACKED SPEED HUD (Realtime Speed + Vietnam Limit Sign + Camera Countdown Bar)
    VietmapStackedSpeedHUD(
      currentSpeedKmh = locationState.speedKmh,
      speedLimitKmh = currentSpeedLimit,
      isOverspeeding = isOverspeed,
      nearestCameraDistance = trafficEvaluation.nearestCameraDistance,
      activeWarning = activeWarning,
      onSpeedLimitClick = { showSpeedLimitPicker = true },
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(start = 14.dp, bottom = 65.dp)
    )

    // 5. IN-NAVIGATION BOTTOM CONTROL DOCK OR BOTTOM ROAD PILL
    if (activeRoute != null && activeRoute.isNavigating) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.96f),
        shadowElevation = 18.dp,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF0284C7)),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(horizontal = 14.dp, vertical = 18.dp)
          .fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            // ETA & Remaining Distance
            val remainingKm = String.format(java.util.Locale.US, "%.1f", activeRoute.totalDistanceMeters / 1000f)
            val etaMillis = System.currentTimeMillis() + (activeRoute.estimatedDurationMinutes * 60000L)
            val etaCalendar = java.util.Calendar.getInstance().apply { timeInMillis = etaMillis }
            val etaTimeStr = String.format(java.util.Locale.US, "%02d:%02d", etaCalendar.get(java.util.Calendar.HOUR_OF_DAY), etaCalendar.get(java.util.Calendar.MINUTE))

            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "${activeRoute.estimatedDurationMinutes}",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, fontSize = 28.sp),
                color = Color(0xFF10B981)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("phút", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text("Dự kiến đến: $etaTimeStr", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                Text("Còn lại: $remainingKm km • ${activeRoute.destinationName}", color = Color(0xFF94A3B8), fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
              }
            }

            // Stop Navigation Button
            IconButton(
              onClick = { onCancelNavigation() },
              modifier = Modifier
                .background(Color(0xFFEF4444).copy(alpha = 0.2f), CircleShape)
                .size(40.dp)
            ) {
              Icon(Icons.Default.Close, contentDescription = "Dừng chỉ đường", tint = Color(0xFFEF4444), modifier = Modifier.size(22.dp))
            }
          }

          // Alternative route switcher chips if multiple routes exist
          if (activeRoute.alternativeRoutes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              item {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = Color(0xFF0284C7),
                  modifier = Modifier.clickable { /* Active */ }
                ) {
                  Text(
                    text = "✓ ${activeRoute.routeTag}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
              items(activeRoute.alternativeRoutes) { alt ->
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = Color(0xFF1E293B),
                  border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                  modifier = Modifier.clickable { onSwitchActiveRoute(alt) }
                ) {
                  Text(
                    text = "Đổi: ${alt.routeTag}",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            }
          }
        }
      }
    } else {
      // BOTTOM-CENTER FLOATING ROAD PILL (Vietmap Live Style)
      VietmapBottomRoadPill(
        roadName = trafficEvaluation.currentRoadName,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 16.dp)
          .clickable { showRoadSelectDialog = true }
      )
    }

    // 6. BOTTOM MULTI-ROUTE PREVIEW & VEHICLE SELECTION CARD
    if (previewTapPlace != null && (activeRoute == null || !activeRoute.isNavigating)) {
      val place = previewTapPlace!!
      val directDistKm = (VietnamTrafficData.calculateDistanceMeters(
        locationState.latitude, locationState.longitude,
        place.latitude, place.longitude
      ) / 1000f * 10).roundToInt() / 10f
      val durationMin = ((directDistKm * 1.25f) / (if (vehicleRoutingMode == VehicleRoutingMode.MOTORBIKE) 32f else 36f) * 60f).roundToInt().coerceAtLeast(2)

      var selectedAltIndex by remember { mutableIntStateOf(0) }

      Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.98f),
        shadowElevation = 20.dp,
        border = androidx.compose.foundation.BorderStroke(1.8.dp, Color(0xFF0284C7)),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(horizontal = 10.dp, vertical = 70.dp)
          .fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          // Destination Info Header
          Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.weight(1f)) {
              Surface(
                shape = CircleShape,
                color = Color(0xFF0284C7),
                modifier = Modifier.size(44.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(place.iconEmoji.ifBlank { "📍" }, fontSize = 22.sp)
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = place.name,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color.White
                )
                Text(
                  text = place.address,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFF94A3B8),
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  onSaveFavorite(place.name, place.address, place.category, place.latitude, place.longitude, place.iconEmoji)
                  showFavoriteToast = "Đã lưu \"${place.name}\" vào địa điểm yêu thích!"
                }
              ) {
                Icon(Icons.Default.Star, contentDescription = "Lưu yêu thích", tint = Color(0xFFFBBF24))
              }
              IconButton(onClick = { previewTapPlace = null }) {
                Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color(0xFF94A3B8))
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Vehicle Mode Switcher (Xe Máy 🏍️ vs Ô Tô 🚗)
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (vehicleRoutingMode == VehicleRoutingMode.MOTORBIKE) Color(0xFF0284C7) else Color(0xFF1E293B),
              border = if (vehicleRoutingMode == VehicleRoutingMode.MOTORBIKE) androidx.compose.foundation.BorderStroke(1.5.dp, Color.White) else null,
              modifier = Modifier
                .weight(1f)
                .clickable { onSetVehicleRoutingMode(VehicleRoutingMode.MOTORBIKE) }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 8.dp)
              ) {
                Text("🏍️", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Xe Máy (Tránh cao tốc)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = if (vehicleRoutingMode == VehicleRoutingMode.CAR) Color(0xFF0284C7) else Color(0xFF1E293B),
              border = if (vehicleRoutingMode == VehicleRoutingMode.CAR) androidx.compose.foundation.BorderStroke(1.5.dp, Color.White) else null,
              modifier = Modifier
                .weight(1f)
                .clickable { onSetVehicleRoutingMode(VehicleRoutingMode.CAR) }
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 8.dp)
              ) {
                Text("🚗", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ô Tô (Đường lớn)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Multi-Route Options (Tuyến 1: Tối ưu nhất, Tuyến 2: Ngắn nhất, Tuyến 3: Tránh BOT)
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            // Route Option 1
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (selectedAltIndex == 0) Color(0xFF0369A1) else Color(0xFF1E293B),
              border = if (selectedAltIndex == 0) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null,
              modifier = Modifier
                .weight(1f)
                .clickable { selectedAltIndex = 0 }
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text("🌟 Nhanh nhất", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("$durationMin phút", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(String.format(java.util.Locale.US, "%.1f km", directDistKm * 1.25f), color = Color(0xFF94A3B8), fontSize = 11.sp)
              }
            }

            // Route Option 2
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (selectedAltIndex == 1) Color(0xFF0369A1) else Color(0xFF1E293B),
              border = if (selectedAltIndex == 1) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null,
              modifier = Modifier
                .weight(1f)
                .clickable { selectedAltIndex = 1 }
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text("🌿 Ngắn nhất", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("${durationMin + 2} phút", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(String.format(java.util.Locale.US, "%.1f km", directDistKm * 1.12f), color = Color(0xFF94A3B8), fontSize = 11.sp)
              }
            }

            // Route Option 3
            Surface(
              shape = RoundedCornerShape(14.dp),
              color = if (selectedAltIndex == 2) Color(0xFF0369A1) else Color(0xFF1E293B),
              border = if (selectedAltIndex == 2) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)) else null,
              modifier = Modifier
                .weight(1f)
                .clickable { selectedAltIndex = 2 }
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(if (vehicleRoutingMode == VehicleRoutingMode.MOTORBIKE) "🏍️ Êm ái" else "🚧 Tránh BOT", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("${durationMin + 4} phút", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(String.format(java.util.Locale.US, "%.1f km", directDistKm * 1.38f), color = Color(0xFF94A3B8), fontSize = 11.sp)
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Start Navigation Big Action Button
          Button(
            onClick = {
              onStartNavigation(place)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Navigation, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Bắt đầu chỉ đường (${if (selectedAltIndex == 0) durationMin else if (selectedAltIndex == 1) durationMin + 2 else durationMin + 4} phút)",
              fontWeight = FontWeight.Black,
              color = Color(0xFF0F172A),
              fontSize = 15.5.sp
            )
          }
        }
      }
    }

    // 7. COMPREHENSIVE SEARCH BOTTOM SHEET & SEARCH HISTORY DIALOG
    if (showDestinationSearchDialog) {
      AlertDialog(
        onDismissRequest = { showDestinationSearchDialog = false },
        containerColor = Color(0xFF0F172A),
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Tìm kiếm điểm đến & địa chỉ",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White
            )
          }
        },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Search Input Field
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("Nhập địa chỉ, số nhà, ngõ ngách, tên đường...", color = Color(0xFF94A3B8), fontSize = 13.5.sp) },
              textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              ),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B),
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF475569),
                cursorColor = Color(0xFF38BDF8)
              ),
              leadingIcon = {
                if (isSearchingOnline) {
                  CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF38BDF8))
                } else {
                  Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF38BDF8))
                }
              },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { searchQuery = "" }) {
                    Icon(Icons.Default.Clear, contentDescription = "Xoá", tint = Color.White)
                  }
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            )

            // Quick 1-Tap Shortcut Cards: Nhà riêng, Công ty, Cây xăng, Cứu hộ
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              val homePlace = favorites.firstOrNull { it.category == "NHÀ" || it.name.contains("nhà", ignoreCase = true) }
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                  .weight(1f)
                  .clickable {
                    showDestinationSearchDialog = false
                    if (homePlace != null) {
                      onStartCustomNavigation(homePlace.name, homePlace.address, homePlace.latitude, homePlace.longitude)
                    } else {
                      onStartCustomNavigation("Nhà Riêng (Home)", "Điểm đến thường xuyên", 10.7769, 106.7009)
                    }
                  }
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                ) {
                  Text("🏠", fontSize = 16.sp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Column {
                    Text("Về Nhà", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(if (homePlace != null) homePlace.name else "1 chạm đi ngay", color = Color(0xFF94A3B8), fontSize = 9.5.sp, maxLines = 1)
                  }
                }
              }

              val workPlace = favorites.firstOrNull { it.category == "CÔNG TY" || it.name.contains("công ty", ignoreCase = true) }
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E293B),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7)),
                modifier = Modifier
                  .weight(1f)
                  .clickable {
                    showDestinationSearchDialog = false
                    if (workPlace != null) {
                      onStartCustomNavigation(workPlace.name, workPlace.address, workPlace.latitude, workPlace.longitude)
                    } else {
                      onStartCustomNavigation("Công Ty (Work)", "Nơi làm việc", 10.7725, 106.6980)
                    }
                  }
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                ) {
                  Text("🏢", fontSize = 16.sp)
                  Spacer(modifier = Modifier.width(6.dp))
                  Column {
                    Text("Công Ty", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(if (workPlace != null) workPlace.name else "1 chạm đi ngay", color = Color(0xFF94A3B8), fontSize = 9.5.sp, maxLines = 1)
                  }
                }
              }
            }

            // Quick Category Chips
            val categories = listOf("Tất cả", "Tuyến đường", "Cây xăng", "Sân bay", "Bệnh viện", "Trung tâm", "Bến xe")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              items(categories) { cat ->
                val isSelected = selectedCategoryFilter == cat
                FilterChip(
                  selected = isSelected,
                  onClick = { selectedCategoryFilter = cat },
                  label = { Text(cat, fontSize = 11.sp) },
                  colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF0284C7),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF1E293B),
                    labelColor = Color(0xFF94A3B8)
                  )
                )
              }
            }

            HorizontalDivider(color = Color(0xFF334155))

            // RECENT SEARCHES LIST (When Search Query is Empty)
            if (searchQuery.isBlank() && recentSearches.isNotEmpty()) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Lịch sử tìm kiếm gần đây",
                  color = Color(0xFF94A3B8),
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
                TextButton(onClick = { onClearAllRecentSearches() }) {
                  Text("Xoá tất cả", color = Color(0xFFEF4444), fontSize = 11.5.sp)
                }
              }

              LazyColumn(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(max = 160.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                items(recentSearches) { recent ->
                  Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable {
                        previewTapPlace = DestinationPlace(
                          id = recent.id,
                          name = recent.name,
                          address = recent.address,
                          category = recent.category,
                          latitude = recent.latitude,
                          longitude = recent.longitude,
                          iconEmoji = recent.iconEmoji
                        )
                        showDestinationSearchDialog = false
                      }
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                      Text("🕒", fontSize = 14.sp)
                      Spacer(modifier = Modifier.width(8.dp))
                      Column(modifier = Modifier.weight(1f)) {
                        Text(recent.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                        Text(recent.address, color = Color(0xFF64748B), fontSize = 10.5.sp, maxLines = 1)
                      }
                      IconButton(
                        onClick = { onDeleteRecentSearch(recent.id) },
                        modifier = Modifier.size(24.dp)
                      ) {
                        Icon(Icons.Default.Close, contentDescription = "Xoá", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                      }
                    }
                  }
                }
              }
              HorizontalDivider(color = Color(0xFF334155))
            }

            // Search Results List (Online Geocoded & Local with Distance Pill)
            if (searchResults.isEmpty() && !isSearchingOnline && searchQuery.isNotBlank()) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 24.dp)
              ) {
                Text(
                  text = "Không tìm thấy kết quả cho \"$searchQuery\".\nBạn hãy thử gõ tên đường hoặc chọn trên bản đồ.",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFF94A3B8),
                  textAlign = TextAlign.Center
                )
              }
            } else {
              LazyColumn(
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                items(searchResults) { place ->
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                    modifier = Modifier
                      .fillMaxWidth()
                      .clickable {
                        previewTapPlace = place
                        showDestinationSearchDialog = false
                      }
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(10.dp)
                    ) {
                      Surface(
                        shape = CircleShape,
                        color = Color(0xFF0369A1),
                        modifier = Modifier.size(36.dp)
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Text(place.iconEmoji.ifBlank { "📍" }, fontSize = 18.sp)
                        }
                      }

                      Spacer(modifier = Modifier.width(10.dp))

                      Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Text(
                            text = place.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                          )
                          if (place.distanceKm > 0f) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                              shape = RoundedCornerShape(6.dp),
                              color = Color(0xFF10B981).copy(alpha = 0.2f),
                              border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF10B981))
                            ) {
                              Text(
                                text = "${place.distanceKm} km",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                              )
                            }
                          }
                        }
                        Text(
                          text = place.address,
                          style = MaterialTheme.typography.labelSmall,
                          color = Color(0xFF94A3B8),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                      }

                      Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Chỉ đường",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(20.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { showDestinationSearchDialog = false }) {
            Text("Đóng", color = Color(0xFF94A3B8))
          }
        }
      )
    }

    // 7B. NEARBY UTILITY & FAVORITE PLACES MODAL DIALOG
    if (activeUtilityCategory != null) {
      val catTitle = activeUtilityCategory!!
      AlertDialog(
        onDismissRequest = { activeUtilityCategory = null },
        containerColor = Color(0xFF0F172A),
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            val iconEmoji = when {
              catTitle.contains("Cây xăng") -> "⛽"
              catTitle.contains("Ngân hàng") -> "🏦"
              catTitle.contains("Sửa xe") -> "🔧"
              catTitle.contains("Y tế") || catTitle.contains("Bệnh viện") -> "🏥"
              catTitle.contains("Ăn uống") || catTitle.contains("Cafe") -> "☕"
              catTitle.contains("Bãi đỗ") -> "🅿️"
              else -> "⭐"
            }
            Text(iconEmoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (catTitle.contains("Yêu thích")) "Địa điểm Yêu thích (${favorites.size})" else "$catTitle gần bạn (${utilityPlacesList.size})",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White
            )
          }
        },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (catTitle.contains("Yêu thích")) {
              if (favorites.isEmpty()) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                ) {
                  Text(
                    text = "Chưa có địa điểm yêu thích nào.\nKhi tìm kiếm hoặc chạm bản đồ, bấm biểu tượng ⭐ để lưu nhanh!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                  )
                }
              } else {
                LazyColumn(
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  items(favorites) { fav ->
                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = Color(0xFF1E293B),
                      border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                      ) {
                        Text(fav.iconEmoji.ifBlank { "⭐" }, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = fav.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                          Text(
                            text = fav.address,
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                        }
                        IconButton(
                          onClick = {
                            activeUtilityCategory = null
                            onStartCustomNavigation(fav.name, fav.address, fav.latitude, fav.longitude)
                          }
                        ) {
                          Icon(Icons.Default.Navigation, contentDescription = "Đi ngay", tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                        }
                        IconButton(
                          onClick = { onDeleteFavorite(fav.id) }
                        ) {
                          Icon(Icons.Default.DeleteOutline, contentDescription = "Xoá", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        }
                      }
                    }
                  }
                }
              }
            } else {
              // UTILITY LIST
              if (isUtilityLoading) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 30.dp)
                ) {
                  CircularProgressIndicator(color = Color(0xFF38BDF8))
                }
              } else if (utilityPlacesList.isEmpty()) {
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                ) {
                  Text(
                    text = "Đang quét dữ liệu $catTitle xung quanh vị trí xe của bạn...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                  )
                }
              } else {
                LazyColumn(
                  modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  items(utilityPlacesList) { place ->
                    val distMeters = VietnamTrafficData.calculateDistanceMeters(
                      locationState.latitude, locationState.longitude,
                      place.latitude, place.longitude
                    ).toInt()
                    val distStr = if (distMeters >= 1000) String.format(java.util.Locale.US, "%.1f km", distMeters / 1000f) else "$distMeters m"

                    Surface(
                      shape = RoundedCornerShape(12.dp),
                      color = Color(0xFF1E293B),
                      border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(10.dp)
                      ) {
                        Surface(
                          shape = RoundedCornerShape(8.dp),
                          color = Color(0xFF0369A1),
                          modifier = Modifier.padding(end = 8.dp)
                        ) {
                          Text(
                            text = distStr,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                          )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                          Text(
                            text = place.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                          Text(
                            text = place.address,
                            color = Color(0xFF94A3B8),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                          )
                        }
                        IconButton(
                          onClick = {
                            onSaveFavorite(place.name, place.address, place.category, place.latitude, place.longitude, "⭐")
                            showFavoriteToast = "Đã lưu \"${place.name}\" vào địa điểm yêu thích!"
                          }
                        ) {
                          Icon(Icons.Default.StarBorder, contentDescription = "Lưu", tint = Color(0xFFFBBF24), modifier = Modifier.size(20.dp))
                        }
                        Button(
                          onClick = {
                            activeUtilityCategory = null
                            onStartCustomNavigation(place.name, place.address, place.latitude, place.longitude)
                          },
                          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
                          shape = RoundedCornerShape(8.dp),
                          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                          Text("Đi ngay", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          TextButton(onClick = { activeUtilityCategory = null }) {
            Text("Đóng", color = Color(0xFF94A3B8))
          }
        }
      )
    }

    // 7C. FAVORITE SAVE CONFIRMATION TOAST
    if (showFavoriteToast != null) {
      LaunchedEffect(showFavoriteToast) {
        delay(2500)
        showFavoriteToast = null
      }
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.95f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
        shadowElevation = 8.dp,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(bottom = 80.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
          Text("⭐", fontSize = 16.sp)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = showFavoriteToast!!,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp
          )
        }
      }
    }

    // 8. SPEED LIMIT QUICK OVERVIEW & ADJUSTMENT DIALOG
    if (showSpeedLimitPicker) {
      AlertDialog(
        onDismissRequest = { showSpeedLimitPicker = false },
        containerColor = Color(0xFF0F172A),
        icon = {
          Surface(
            shape = CircleShape,
            color = SignBackgroundWhite,
            border = androidx.compose.foundation.BorderStroke(4.dp, SignBorderRed),
            modifier = Modifier.size(54.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = "$currentSpeedLimit",
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = SignTextBlack
              )
            }
          }
        },
        title = {
          Text(
            text = "Giới Hạn Tốc Độ Tuyến Đường",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
          )
        },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFF1E293B),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Tuyến đường đang chạy:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)
                Text(
                  trafficEvaluation.currentRoadName,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF38BDF8),
                  style = MaterialTheme.typography.bodyMedium
                )
              }
            }

            Text(
              text = "⚖️ Theo Thông tư 31/2019/TT-BGTVT & Thực tế Sở GTVT TP.HCM: Các trục đường nội thành (Lũy Bán Bích, Thoại Ngọc Hầu, Hòa Bình...) áp dụng tối đa 50 km/h để đảm bảo an toàn.",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
              color = Color(0xFFCBD5E1)
            )

            HorizontalDivider(color = Color(0xFF334155))

            Text("Tùy chỉnh giới hạn tốc độ tạm thời:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.labelSmall)

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              listOf(30, 40, 50, 60, 70, 80).forEach { limit ->
                val isSelected = currentSpeedLimit == limit
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B),
                  border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) Color(0xFF38BDF8) else Color(0xFF475569)
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .clickable {
                      onSetSpeed(limit.toFloat())
                      showSpeedLimitPicker = false
                    }
                ) {
                  Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                  ) {
                    Text(
                      text = "$limit",
                      fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                      color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                      fontSize = 13.sp
                    )
                  }
                }
              }
            }
          }
        },
        confirmButton = {
          Button(
            onClick = { showSpeedLimitPicker = false },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
          ) {
            Text("Đã hiểu")
          }
        }
      )
    }

    // 9. CUSTOM ROAD SELECT DIALOG
    if (showRoadSelectDialog) {
      AlertDialog(
        onDismissRequest = { showRoadSelectDialog = false },
        containerColor = Color(0xFF0F172A),
        title = { Text("Chọn hoặc nhập tên đường đang chạy", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = customRoadInput,
              onValueChange = { customRoadInput = it },
              placeholder = { Text("Ví dụ: Đường Nguyễn Trãi, Võ Văn Kiệt...", color = Color(0xFF94A3B8)) },
              textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              ),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF1E293B),
                unfocusedContainerColor = Color(0xFF1E293B),
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF475569),
                cursorColor = Color(0xFF38BDF8)
              ),
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            )
          }
        },
        confirmButton = {
          Button(
            onClick = {
              if (customRoadInput.isNotBlank()) {
                onSetCustomRoad(customRoadInput)
              }
              showRoadSelectDialog = false
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
          ) {
            Text("Áp dụng")
          }
        },
        dismissButton = {
          TextButton(onClick = { showRoadSelectDialog = false }) {
            Text("Huỷ", color = Color(0xFF94A3B8))
          }
        }
      )
    }

    // 10. TRIP SUMMARY POPUP
    if (lastFinishedTrip != null) {
      TripSummaryDialog(
        stats = lastFinishedTrip,
        onDismiss = onCloseTripSummary
      )
    }

    // 11. CAMERA DETAIL BOTTOM SHEET (Tapping camera on map)
    if (inspectingCamera != null) {
      CameraDetailBottomSheet(
        camera = inspectingCamera,
        currentLocation = locationState,
        onDismiss = { inspectingCamera = null },
        onStartNavigation = onStartNavigation,
        onSpeakCamera = onSpeakAlert
      )
    }

    // 12. POI DETAIL BOTTOM SHEET (Tapping Gas, BOT, Hospital, Rescue on map)
    if (inspectingPoi != null) {
      PoiDetailBottomSheet(
        poi = inspectingPoi,
        currentLocation = locationState,
        onDismiss = { inspectingPoi = null },
        onStartNavigation = onStartNavigation
      )
    }
  }
}

@Composable
fun TripSummaryDialog(
  stats: CurrentTripStats,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = Color(0xFF0F172A),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Flag, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Tổng kết lộ trình", fontWeight = FontWeight.Bold, color = Color.White)
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        val durationMin = stats.durationSeconds / 60
        val durationSec = stats.durationSeconds % 60

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          SummaryStatItem(
            icon = Icons.Default.Timeline,
            label = "Quãng đường",
            value = String.format(java.util.Locale.US, "%.1f km", stats.distanceKm),
            color = Color(0xFF38BDF8)
          )
          SummaryStatItem(
            icon = Icons.Default.Timer,
            label = "Thời gian",
            value = String.format(java.util.Locale.US, "%d p %02d s", durationMin, durationSec),
            color = Color(0xFF10B981)
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          SummaryStatItem(
            icon = Icons.Default.Speed,
            label = "Tốc độ TB",
            value = "${stats.avgSpeedKmh.toInt()} km/h",
            color = Color(0xFFF59E0B)
          )
          SummaryStatItem(
            icon = Icons.Default.Warning,
            label = "Cảnh báo vi phạm",
            value = "${stats.overspeedEvents} lần",
            color = if (stats.overspeedEvents > 0) Color(0xFFEF4444) else Color(0xFF10B981)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
      ) {
        Text("Đóng")
      }
    }
  )
}

@Composable
fun SummaryStatItem(icon: ImageVector, label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.Start, modifier = Modifier.width(110.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(4.dp))
      Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
    }
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      color = Color.White,
      modifier = Modifier.padding(top = 2.dp)
    )
  }
}

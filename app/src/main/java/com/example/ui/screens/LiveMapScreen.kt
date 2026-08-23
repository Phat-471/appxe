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
import com.example.data.VietnamTrafficData
import com.example.data.local.UserSettingsEntity
import com.example.data.model.*
import com.example.service.NavigationRoutingService
import com.example.service.WarningEvaluationResult
import com.example.ui.components.*
import com.example.ui.theme.*
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
  onToggleVoice: () -> Unit = {},
  onToggleTripRecording: () -> Unit = {},
  onCloseTripSummary: () -> Unit = {},
  onToggleGpsOrSimulation: (Boolean) -> Unit = {},
  onStartNavigation: (DestinationPlace) -> Unit = {},
  onStartCustomNavigation: (name: String, address: String, lat: Double, lng: Double) -> Unit = { _, _, _, _ -> },
  onCancelNavigation: () -> Unit = {},
  onSelectRoute: (Int) -> Unit = {},
  onSetSpeed: (Float) -> Unit = {},
  onSetCustomRoad: (String) -> Unit = {},
  onTestSound: () -> Unit = {},
  onSpeakAlert: (String) -> Unit = {},
  onOpenReportDialog: () -> Unit,
  onRefreshLocation: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()
  var isExpandedControls by remember { mutableStateOf(false) }
  var inspectingCamera by remember { mutableStateOf<TrafficCamera?>(null) }
  var showDestinationSearchDialog by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategoryFilter by remember { mutableStateOf("Tất cả") }
  var showRoadSelectDialog by remember { mutableStateOf(false) }
  var customRoadInput by remember { mutableStateOf("") }
  var previewTapPlace by remember { mutableStateOf<DestinationPlace?>(null) }

  // Online Geocoding Search States
  var searchResults by remember { mutableStateOf<List<DestinationPlace>>(emptyList()) }
  var isSearchingOnline by remember { mutableStateOf(false) }
  var searchJob by remember { mutableStateOf<Job?>(null) }

  val activeWarning = trafficEvaluation.activeWarning
  val nearestCam = trafficEvaluation.nearestCamera
  val currentSpeedLimit = trafficEvaluation.currentSpeedLimit.coerceAtLeast(50)
  val isOverspeed = trafficEvaluation.isOverspeeding
  val speedInt = locationState.speedKmh.toInt()

  val allPlaces = remember { VietnamTrafficData.POPULAR_PLACES }

  // Calculate upcoming cameras list ahead with distances
  val camerasAhead = remember(cameras, locationState) {
    cameras.mapNotNull { cam ->
      val dist = VietnamTrafficData.calculateDistanceMeters(
        locationState.latitude, locationState.longitude,
        cam.latitude, cam.longitude
      ).toInt()
      if (dist in 15..800) cam to dist else null
    }.sortedBy { it.second }
  }

  // Debounced live geocoding search for street names and addresses
  LaunchedEffect(searchQuery, selectedCategoryFilter) {
    searchJob?.cancel()
    if (searchQuery.isBlank()) {
      searchResults = allPlaces.filter {
        selectedCategoryFilter == "Tất cả" || it.category == selectedCategoryFilter
      }
      isSearchingOnline = false
    } else {
      isSearchingOnline = true
      searchJob = coroutineScope.launch {
        delay(350)
        val results = NavigationRoutingService.searchLocations(searchQuery)
        searchResults = if (selectedCategoryFilter == "Tất cả") {
          results
        } else {
          results.filter { it.category == selectedCategoryFilter }
        }
        isSearchingOnline = false
      }
    }
  }

  val filteredCameras = remember(cameras, userSettings) {
    cameras.filter { cam ->
      when (cam.type) {
        CameraType.SPEED_CAMERA, CameraType.COLD_FINE_SURVEILLANCE -> userSettings.showSpeedCamerasOnMap
        CameraType.RED_LIGHT_CAMERA -> userSettings.showRedLightCamerasOnMap
        CameraType.SPEED_LIMIT_SIGN, CameraType.ZONE_RESIDENTIAL_ENTRY, CameraType.ZONE_RESIDENTIAL_EXIT -> userSettings.showSpeedLimitsOnMap
        CameraType.COMMUNITY_REPORT -> userSettings.showCommunityReportsOnMap
        else -> true
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
      onSelectCamera = { inspectingCamera = it },
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
      modifier = Modifier.fillMaxSize()
    )

    // 2. TOP HEADER: VIETMAP LIVE LANE GUIDANCE OR SEARCH PILL
    if (activeRoute != null && activeRoute.isNavigating) {
      val currentStep = activeRoute.steps.getOrNull(activeRoute.currentStepIndex)
        ?: activeRoute.steps.lastOrNull()

      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .align(Alignment.TopCenter)
          .padding(top = 10.dp)
      ) {
        VietmapTopLaneGuidanceBanner(
          turnDistanceMeters = currentStep?.distanceMeters ?: activeRoute.totalDistanceMeters,
          turnInstruction = currentStep?.instruction ?: "Đi thẳng"
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
            IconButton(
              onClick = { onOpenReportDialog() },
              modifier = Modifier.size(32.dp)
            ) {
              Icon(
                imageVector = Icons.Default.AddLocationAlt,
                contentDescription = "Báo camera",
                tint = Color(0xFFDC2626),
                modifier = Modifier.size(20.dp)
              )
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

    // 4. BOTTOM-LEFT STACKED SPEED HUD (Realtime Speed + Vietnam Limit Sign)
    VietmapStackedSpeedHUD(
      currentSpeedKmh = locationState.speedKmh,
      speedLimitKmh = currentSpeedLimit,
      isOverspeeding = isOverspeed,
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(start = 14.dp, bottom = 65.dp)
    )

    // 5. BOTTOM-CENTER FLOATING ROAD PILL (Vietmap Live Style)
    VietmapBottomRoadPill(
      roadName = trafficEvaluation.currentRoadName,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 16.dp)
        .clickable { showRoadSelectDialog = true }
    )

    // 6. BOTTOM ROUTE PREVIEW CARD (When Place Selected)
    if (previewTapPlace != null && (activeRoute == null || !activeRoute.isNavigating)) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.96f),
        shadowElevation = 16.dp,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF38BDF8)),
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .padding(horizontal = 12.dp, vertical = 70.dp)
          .fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(modifier = Modifier.weight(1f)) {
              Surface(
                shape = CircleShape,
                color = Color(0xFF0284C7),
                modifier = Modifier.size(42.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = previewTapPlace!!.name,
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color.White
                )
                Text(
                  text = previewTapPlace!!.address,
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFF94A3B8),
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
            IconButton(onClick = { previewTapPlace = null }) {
              Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color(0xFF94A3B8))
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Route Option Chips (Vietmap Style: 1 giờ 55 phút - 102.6 km)
          val directDistKm = VietnamTrafficData.calculateDistanceMeters(
            locationState.latitude, locationState.longitude,
            previewTapPlace!!.latitude, previewTapPlace!!.longitude
          ) / 1000f
          val durationMinutes = ((directDistKm * 1.3f) / 35f * 60f).toInt().coerceAtLeast(3)

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFF0369A1),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(
                  text = if (durationMinutes >= 60) "${durationMinutes / 60} giờ ${durationMinutes % 60} phút" else "$durationMinutes phút",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                  color = Color.White
                )
                Text(
                  text = String.format(java.util.Locale.US, "%.1f km", directDistKm * 1.25f),
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFFBAE6FD)
                )
              }
            }

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = Color(0xFF1E293B),
              modifier = Modifier.weight(1f)
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                Text(
                  text = if (durationMinutes + 4 >= 60) "${(durationMinutes + 4) / 60} giờ ${(durationMinutes + 4) % 60} phút" else "${durationMinutes + 4} phút",
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFF94A3B8)
                )
                Text(
                  text = String.format(java.util.Locale.US, "%.1f km", directDistKm * 1.4f),
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF64748B)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Big Cyan Start Navigation Button
          Button(
            onClick = {
              onStartNavigation(previewTapPlace!!)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Navigation, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Bắt đầu • Đến sau $durationMinutes phút",
              fontWeight = FontWeight.Black,
              color = Color(0xFF0F172A),
              fontSize = 15.sp
            )
          }
        }
      }
    }

    // 7. DESTINATION SEARCH & REALTIME GEOCODING AUTOCOMPLETE DIALOG
    if (showDestinationSearchDialog) {
      AlertDialog(
        onDismissRequest = { showDestinationSearchDialog = false },
        containerColor = Color(0xFF0F172A),
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Directions, contentDescription = null, tint = Color(0xFF38BDF8))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Tìm kiếm vị trí & tên đường",
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
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("Nhập tên đường (Nguyễn Trãi, Lê Lợi, CMT8...)", color = Color(0xFF94A3B8), fontSize = 14.sp) },
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

            // Category Filter Row
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

            // Search Results List (Online Geocoded & Local)
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
                          val iconVec = when (place.category) {
                            "Cây xăng" -> Icons.Default.LocalGasStation
                            "Sân bay" -> Icons.Default.Flight
                            "Bệnh viện" -> Icons.Default.LocalHospital
                            "Tuyến đường" -> Icons.Default.AltRoute
                            else -> Icons.Default.LocationOn
                          }
                          Icon(iconVec, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                      }

                      Spacer(modifier = Modifier.width(10.dp))

                      Column(modifier = Modifier.weight(1f)) {
                        Text(
                          text = place.name,
                          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                          color = Color.White,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
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

    // 8. CAMERA INSPECTION DETAIL DIALOG
    if (inspectingCamera != null) {
      val cam = inspectingCamera!!
      AlertDialog(
        onDismissRequest = { inspectingCamera = null },
        containerColor = Color(0xFF0F172A),
        icon = {
          Surface(
            shape = CircleShape,
            color = Color(0xFF7F1D1D),
            modifier = Modifier.size(48.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Default.Videocam, contentDescription = null, tint = AlertCrimsonDanger, modifier = Modifier.size(26.dp))
            }
          }
        },
        title = {
          Text(
            text = cam.description,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
            textAlign = TextAlign.Center
          )
        },
        text = {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Tuyến đường:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
              Text(cam.roadName, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Khu vực:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
              Text(cam.districtCity, fontWeight = FontWeight.Bold, color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
            Row(
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Tốc độ quy định:", color = Color(0xFF94A3B8), style = MaterialTheme.typography.bodySmall)
              Text("${cam.speedLimit} km/h", fontWeight = FontWeight.Black, color = AlertCrimsonDanger, style = MaterialTheme.typography.bodySmall)
            }
          }
        },
        confirmButton = {
          Button(
            onClick = { inspectingCamera = null },
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

            Text("Gợi ý đường chính:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))

            val quickRoads = listOf("Đại lộ Võ Văn Kiệt (60 km/h)", "Đường Phạm Văn Đồng (60 km/h)", "Đường Nguyễn Văn Linh (60 km/h)", "Đường Nguyễn Trãi (50 km/h)", "Cao Tốc TP.HCM - Long Thành (80 km/h)")
            quickRoads.forEach { rd ->
              TextButton(
                onClick = {
                  val rawName = rd.substringBefore(" (")
                  onSetCustomRoad(rawName)
                  showRoadSelectDialog = false
                },
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(rd, color = Color(0xFF38BDF8), textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth())
              }
            }
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

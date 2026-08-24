package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.SpeedAlertViewModel

enum class NavigationTab(val title: String, val icon: ImageVector) {
  MAP("Bản Đồ & Cảnh Báo", Icons.Default.Map),
  COCKPIT("Đồng Hồ HUD", Icons.Default.Speed),
  SETTINGS("Cài Đặt", Icons.Default.Settings)
}

@Composable
fun MainAppContainer(
  viewModel: SpeedAlertViewModel,
  modifier: Modifier = Modifier
) {
  var currentTab by remember { mutableStateOf(NavigationTab.MAP) }
  var showReportDialog by remember { mutableStateOf(false) }

  val locationState by viewModel.locationState.collectAsStateWithLifecycle()
  val trafficEvaluation by viewModel.trafficEvaluation.collectAsStateWithLifecycle()
  val tripStats by viewModel.currentTripStats.collectAsStateWithLifecycle()
  val isRecordingTrip by viewModel.isRecordingTrip.collectAsStateWithLifecycle()
  val allCameras by viewModel.allCameras.collectAsStateWithLifecycle()
  val breadcrumbs by viewModel.breadcrumbs.collectAsStateWithLifecycle()
  val trips by viewModel.tripSummaries.collectAsStateWithLifecycle()
  val offlinePacks by viewModel.offlinePacks.collectAsStateWithLifecycle()
  val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
  val favoritePlaces by viewModel.favoritePlaces.collectAsStateWithLifecycle()
  val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()
  val activeRoute by viewModel.activeNavigationRoute.collectAsStateWithLifecycle()
  val lastFinishedTrip by viewModel.lastFinishedTrip.collectAsStateWithLifecycle()
  val compassHeading by viewModel.compassHeading.collectAsStateWithLifecycle()

  Scaffold(
    bottomBar = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 10.dp)
          .navigationBarsPadding()
      ) {
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = Color(0xFF0F172A).copy(alpha = 0.94f),
          border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
              colors = listOf(
                Color.White.copy(alpha = 0.25f),
                Color(0xFF38BDF8).copy(alpha = 0.35f),
                Color.White.copy(alpha = 0.08f)
              )
            )
          ),
          shadowElevation = 16.dp,
          tonalElevation = 6.dp,
          modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .testTag("main_navigation_bar")
        ) {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
          ) {
            NavigationTab.entries.forEach { tab ->
              val isSelected = currentTab == tab
              val tabTitle = when (tab) {
                NavigationTab.MAP -> com.example.ui.i18n.AppStrings.get("tab_map", userSettings.appLanguage)
                NavigationTab.COCKPIT -> com.example.ui.i18n.AppStrings.get("tab_hud", userSettings.appLanguage)
                NavigationTab.SETTINGS -> com.example.ui.i18n.AppStrings.get("tab_settings", userSettings.appLanguage)
              }

              Surface(
                onClick = { currentTab = tab },
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.25f) else Color.Transparent,
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)) else null,
                modifier = Modifier
                  .weight(1f)
                  .padding(horizontal = 4.dp, vertical = 6.dp)
                  .testTag("nav_tab_${tab.name.lowercase()}")
              ) {
                Row(
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 6.dp, horizontal = 4.dp)
                ) {
                  Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title,
                    tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = tabTitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      fontSize = 12.sp,
                      letterSpacing = (-0.2).sp
                    ),
                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                    maxLines = 1
                  )
                }
              }
            }
          }
        }
      }
    },
    containerColor = NavLightBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
    modifier = modifier.fillMaxSize()
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Crossfade(targetState = currentTab, label = "TabCrossfade") { tab ->
        when (tab) {
          NavigationTab.MAP -> {
            LiveMapScreen(
              locationState = locationState,
              trafficEvaluation = trafficEvaluation,
              cameras = allCameras,
              breadcrumbs = breadcrumbs,
              activeRoute = activeRoute,
              tripStats = tripStats,
              isRecordingTrip = isRecordingTrip,
              lastFinishedTrip = lastFinishedTrip,
              voiceEnabled = userSettings.voiceAlertsEnabled,
              compassHeading = compassHeading,
              userSettings = userSettings,
              favorites = favoritePlaces,
              onToggleVoice = { viewModel.toggleVoiceAlerts() },
              onToggleTripRecording = {
                if (isRecordingTrip) viewModel.stopTripRecording()
                else viewModel.startTripRecording()
              },
              onCloseTripSummary = { viewModel.clearLastFinishedTrip() },
              onToggleGpsOrSimulation = { useReal -> viewModel.toggleGpsOrSimulation(useReal) },
              onStartNavigation = { dest -> viewModel.startNavigationToDestination(dest) },
              onStartCustomNavigation = { name, addr, lat, lng -> viewModel.startNavigationToCustom(name, addr, lat, lng) },
              onCancelNavigation = { viewModel.cancelNavigation() },
              onSelectRoute = { idx -> viewModel.setSimulationRoute(idx) },
              onSetSpeed = { speed -> viewModel.setSimulatedSpeed(speed) },
              onSetCustomRoad = { road -> viewModel.setCustomTestRoad(road) },
              onTestSound = { viewModel.testVoice() },
              onSpeakAlert = { msg -> viewModel.speakCustom(msg) },
              onSaveFavorite = { name, addr, cat, lat, lng, icon -> viewModel.saveFavoritePlace(name, addr, cat, lat, lng, icon) },
              onDeleteFavorite = { id -> viewModel.deleteFavoritePlace(id) },
              onSearchNearbyUtilities = { cat -> viewModel.searchNearbyUtilities(cat) },
              onOpenReportDialog = { showReportDialog = true }
            )
          }

          NavigationTab.COCKPIT -> {
            CockpitDashboardScreen(
              locationState = locationState,
              trafficEvaluation = trafficEvaluation,
              tripStats = tripStats,
              isRecordingTrip = isRecordingTrip,
              voiceEnabled = userSettings.voiceAlertsEnabled,
              hudMirrorMode = userSettings.hudMirrorMode,
              onToggleVoice = { viewModel.toggleVoiceAlerts() },
              onToggleHudMirror = {
                viewModel.updateSettings(userSettings.copy(hudMirrorMode = !userSettings.hudMirrorMode))
              },
              onToggleTripRecording = {
                if (isRecordingTrip) viewModel.stopTripRecording()
                else viewModel.startTripRecording()
              },
              onToggleGpsOrSimulation = { useReal -> viewModel.toggleGpsOrSimulation(useReal) },
              onSelectSimulationRoute = { idx -> viewModel.setSimulationRoute(idx) },
              onSetSimulatedSpeed = { speed -> viewModel.setSimulatedSpeed(speed) },
              onSetCustomRoad = { road -> viewModel.setCustomTestRoad(road) },
              onOpenReportDialog = { showReportDialog = true }
            )
          }

          NavigationTab.SETTINGS -> {
            SettingsScreen(
              settings = userSettings,
              offlinePacks = offlinePacks,
              onUpdateSettings = { updated -> viewModel.updateSettings(updated) },
              onTestVoice = { viewModel.testVoice() },
              onDownloadPack = { pack -> viewModel.downloadOrUpdateOfflinePack(pack) }
            )
          }
        }
      }

      // Report Camera Dialog
      if (showReportDialog) {
        ReportCameraDialog(
          currentRoadName = trafficEvaluation.currentRoadName,
          currentSpeedLimit = trafficEvaluation.currentSpeedLimit,
          onDismiss = { showReportDialog = false },
          onSubmit = { type, road, limit, desc, city ->
            viewModel.reportCamera(type, road, limit, desc, city)
          }
        )
      }
    }
  }
}

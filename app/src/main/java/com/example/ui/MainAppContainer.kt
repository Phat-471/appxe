package com.example.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
  val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()
  val activeRoute by viewModel.activeNavigationRoute.collectAsStateWithLifecycle()
  val lastFinishedTrip by viewModel.lastFinishedTrip.collectAsStateWithLifecycle()
  val compassHeading by viewModel.compassHeading.collectAsStateWithLifecycle()

  Scaffold(
    bottomBar = {
      NavigationBar(
        containerColor = NavLightSurface,
        contentColor = NavLightTextPrimary,
        tonalElevation = 6.dp,
        modifier = Modifier
          .border(0.5.dp, NavLightCardBorder)
          .testTag("main_navigation_bar")
      ) {
        NavigationTab.entries.forEach { tab ->
          val isSelected = currentTab == tab
          NavigationBarItem(
            selected = isSelected,
            onClick = { currentTab = tab },
            icon = {
              Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                modifier = Modifier.size(22.dp)
              )
            },
            label = {
              val tabTitle = when (tab) {
                NavigationTab.MAP -> com.example.ui.i18n.AppStrings.get("tab_map", userSettings.appLanguage)
                NavigationTab.COCKPIT -> com.example.ui.i18n.AppStrings.get("tab_hud", userSettings.appLanguage)
                NavigationTab.SETTINGS -> com.example.ui.i18n.AppStrings.get("tab_settings", userSettings.appLanguage)
              }
              Text(
                text = tabTitle,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 11.sp
                )
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = Color.White,
              selectedTextColor = NavRouteBlue,
              indicatorColor = NavRouteBlue,
              unselectedIconColor = NavLightTextSecondary,
              unselectedTextColor = NavLightTextSecondary
            ),
            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
          )
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

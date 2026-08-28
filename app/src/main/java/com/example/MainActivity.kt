package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.service.OsmTileManager
import com.example.ui.MainAppContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.SpeedAlertViewModel

import android.os.Build
import android.view.WindowManager
import androidx.lifecycle.lifecycleScope
import com.example.service.SpeedLimitTrackingService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val viewModel: SpeedAlertViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    OsmTileManager.init(applicationContext)
    enableEdgeToEdge()

    val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (hasFine || hasCoarse) {
      viewModel.toggleGpsOrSimulation(useRealGps = true)
    }

    // Observe user settings for screen-on, background service & floating bubble
    lifecycleScope.launch {
      viewModel.userSettings.collect { settings ->
        try {
          if (settings.autoScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
          } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
          }

          val hasLoc = ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                       ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

          if (settings.backgroundServiceEnabled && hasLoc) {
            SpeedLimitTrackingService.startService(this@MainActivity)
          } else if (!settings.backgroundServiceEnabled) {
            SpeedLimitTrackingService.stopService(this@MainActivity)
          }

          if (settings.floatingBubbleEnabled) {
            if (com.example.service.FloatingSpeedBubbleService.canDrawOverlay(this@MainActivity)) {
              com.example.service.FloatingSpeedBubbleService.startService(this@MainActivity)
            }
          } else {
            com.example.service.FloatingSpeedBubbleService.stopService(this@MainActivity)
          }
        } catch (e: Exception) {
          android.util.Log.w("MainActivity", "Settings application error: ${e.message}")
        }
      }
    }

    setContent {
      MyApplicationTheme {
        LocationPermissionHandler(
          onPermissionGranted = {
            viewModel.toggleGpsOrSimulation(useRealGps = true)
            if (viewModel.userSettings.value.backgroundServiceEnabled) {
              SpeedLimitTrackingService.startService(this@MainActivity)
            }
          },
          onPermissionDenied = {
            viewModel.toggleGpsOrSimulation(useRealGps = false)
          }
        )

        MainAppContainer(
          viewModel = viewModel,
          modifier = Modifier.fillMaxSize()
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (hasFine || hasCoarse) {
      viewModel.toggleGpsOrSimulation(useRealGps = true)
    }

    val settings = viewModel.userSettings.value
    if (settings.floatingBubbleEnabled && com.example.service.FloatingSpeedBubbleService.canDrawOverlay(this)) {
      com.example.service.FloatingSpeedBubbleService.startService(this)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    val settings = viewModel.userSettings.value
    if (!settings.backgroundServiceEnabled) {
      SpeedLimitTrackingService.stopService(this)
    }
  }
}

@Composable
fun LocationPermissionHandler(
  onPermissionGranted: () -> Unit,
  onPermissionDenied: () -> Unit
) {
  val context = LocalContext.current
  val launcher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (fineGranted || coarseGranted) {
      onPermissionGranted()
    } else {
      onPermissionDenied()
    }
  }

  LaunchedEffect(Unit) {
    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (hasFine || hasCoarse) {
      onPermissionGranted()
    } else {
      val perms = mutableListOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
      )
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        perms.add(Manifest.permission.POST_NOTIFICATIONS)
      }
      launcher.launch(perms.toTypedArray())
    }
  }
}


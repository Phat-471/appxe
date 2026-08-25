package com.example.data.model

import java.io.File

data class AppUpdateInfo(
  val currentVersionName: String = "1.2.0",
  val currentVersionCode: Int = 120,
  val latestVersionName: String = "1.2.1",
  val latestVersionCode: Int = 121,
  val hasUpdate: Boolean = false,
  val releaseDate: String = "25/08/2026",
  val releaseNotes: List<String> = emptyList(),
  val apkDownloadUrl: String = "https://github.com/Phat-471/appxe/releases",
  val fileSizeMb: Float = 28.5f,
  val isMandatory: Boolean = false
)

sealed interface UpdateCheckState {
  data object Idle : UpdateCheckState
  data object Checking : UpdateCheckState
  data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckState
  data class Downloading(
    val info: AppUpdateInfo,
    val progressPercent: Int,
    val downloadedMb: Float,
    val totalMb: Float
  ) : UpdateCheckState
  data class ReadyToInstall(
    val info: AppUpdateInfo,
    val apkFile: File
  ) : UpdateCheckState
  data class UpToDate(val currentVersion: String, val lastCheckedTime: String) : UpdateCheckState
  data class Error(val message: String) : UpdateCheckState
}

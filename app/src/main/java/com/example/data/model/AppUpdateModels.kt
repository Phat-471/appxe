package com.example.data.model

data class AppUpdateInfo(
  val currentVersionName: String = "1.2.0",
  val currentVersionCode: Int = 120,
  val latestVersionName: String = "1.2.1",
  val latestVersionCode: Int = 121,
  val hasUpdate: Boolean = false,
  val releaseDate: String = "24/08/2026",
  val releaseNotes: List<String> = emptyList(),
  val apkDownloadUrl: String = "https://github.com/aistudio/speedalert-vngps/releases",
  val fileSizeMb: Float = 24.8f,
  val isMandatory: Boolean = false
)

sealed interface UpdateCheckState {
  data object Idle : UpdateCheckState
  data object Checking : UpdateCheckState
  data class UpdateAvailable(val info: AppUpdateInfo) : UpdateCheckState
  data class UpToDate(val currentVersion: String, val lastCheckedTime: String) : UpdateCheckState
  data class Error(val message: String) : UpdateCheckState
}

package com.example.data.model

import java.io.File

data class AppUpdateInfo(
  val currentVersionName: String = "",
  val currentVersionCode: Int = 0,
  val latestVersionName: String = "",
  val latestVersionCode: Int = 0,
  val hasUpdate: Boolean = false,
  val releaseDate: String = "",
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

data class AppReleaseHistoryItem(
  val tagName: String,
  val versionName: String,
  val versionCode: Int,
  val releaseDate: String,
  val releaseNotes: List<String>,
  val apkDownloadUrl: String,
  val sizeMb: Float,
  val isCurrentVersion: Boolean = false,
  val isOlderVersion: Boolean = false
)

data class RollbackBackupInfo(
  val hasLocalBackup: Boolean = false,
  val backupVersionName: String = "",
  val backupVersionCode: Int = 0,
  val backupFilePath: String = "",
  val backupTimestamp: Long = 0L
)


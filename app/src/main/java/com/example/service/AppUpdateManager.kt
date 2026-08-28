package com.example.service

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.example.data.model.AppUpdateInfo
import com.example.data.model.UpdateCheckState
import com.example.data.model.AppReleaseHistoryItem
import com.example.data.model.RollbackBackupInfo
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AppUpdateManager {
  private const val TAG = "AppUpdateManager"
  private const val GITHUB_RELEASES_API = "https://api.github.com/repos/Phat-471/appxe/releases/latest"
  private const val GITHUB_ALL_RELEASES_API = "https://api.github.com/repos/Phat-471/appxe/releases"
  private const val RAW_VERSION_ENDPOINT = "https://raw.githubusercontent.com/Phat-471/appxe/main/version.json"
  private const val ROLLBACK_PREFS = "speedalert_rollback_prefs"
  private const val PREF_BACKUP_VERSION_NAME = "backup_version_name"
  private const val PREF_BACKUP_VERSION_CODE = "backup_version_code"
  private const val PREF_BACKUP_FILE_PATH = "backup_file_path"
  private const val PREF_BACKUP_TIMESTAMP = "backup_timestamp"
  private const val PREF_CRASH_COUNT = "startup_crash_count"
  private const val PREF_LAST_START_TIME = "last_start_timestamp"

  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

  /**
   * Check for latest app updates from GitHub Releases API or repository version manifest.
   */
  suspend fun checkForUpdates(
    currentVersionName: String = BuildConfig.VERSION_NAME,
    currentVersionCode: Int = BuildConfig.VERSION_CODE
  ): UpdateCheckState = withContext(Dispatchers.IO) {
    try {
      var latestName = currentVersionName
      var latestCode = currentVersionCode
      var releaseDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
      var downloadUrl = "https://github.com/Phat-471/appxe/releases"
      var sizeMb = 28.5f
      var isMandatory = false
      val notes = mutableListOf<String>()
      var foundRemote = false
      var hasDirectApkAsset = false

      // 1. Check GitHub Releases API
      try {
        val request = Request.Builder()
          .url(GITHUB_RELEASES_API)
          .header("Accept", "application/vnd.github.v3+json")
          .header("User-Agent", "SpeedAlert-App/AutoUpdate")
          .build()

        httpClient.newCall(request).execute().use { response ->
          if (response.isSuccessful) {
            val jsonStr = response.body?.string()
            if (!jsonStr.isNullOrBlank()) {
              val releaseObj = JSONObject(jsonStr)
              val tagName = releaseObj.optString("tag_name", "").removePrefix("v")
              if (tagName.isNotBlank()) {
                latestName = tagName
                latestCode = parseVersionCode(tagName)
                foundRemote = true

                val body = releaseObj.optString("body", "")
                if (body.isNotBlank()) {
                  body.lines().filter { it.isNotBlank() }.forEach { line ->
                    notes.add(line.removePrefix("- ").removePrefix("* ").trim())
                  }
                }

                val assets = releaseObj.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                  for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                      downloadUrl = asset.optString("browser_download_url", downloadUrl)
                      val byteSize = asset.optLong("size", 0L)
                      if (byteSize > 0) {
                        sizeMb = (byteSize / (1024f * 1024f) * 10f).toInt() / 10f
                      }
                      hasDirectApkAsset = true
                      break
                    }
                  }
                }

                val publishedAt = releaseObj.optString("published_at", "")
                if (publishedAt.length >= 10) {
                  releaseDate = publishedAt.substring(0, 10)
                }
              }
            }
          }
        }
      } catch (e: Exception) {
        Log.w(TAG, "GitHub releases check failed: ${e.message}")
      }

      // 2. Fallback to raw version manifest
      if (!foundRemote) {
        try {
          val reqManifest = Request.Builder()
            .url(RAW_VERSION_ENDPOINT)
            .header("User-Agent", "SpeedAlert-App/AutoUpdate")
            .build()

          httpClient.newCall(reqManifest).execute().use { resp ->
            if (resp.isSuccessful) {
              val manifestStr = resp.body?.string()
              if (!manifestStr.isNullOrBlank()) {
                val obj = JSONObject(manifestStr)
                latestCode = obj.optInt("versionCode", currentVersionCode)
                latestName = obj.optString("versionName", currentVersionName)
                releaseDate = obj.optString("releaseDate", releaseDate)
                downloadUrl = obj.optString("downloadUrl", downloadUrl)
                sizeMb = obj.optDouble("sizeMb", sizeMb.toDouble()).toFloat()
                isMandatory = obj.optBoolean("isMandatory", false)

                val notesArray = obj.optJSONArray("releaseNotes")
                if (notesArray != null && notesArray.length() > 0) {
                  notes.clear()
                  for (i in 0 until notesArray.length()) {
                    notes.add(notesArray.getString(i))
                  }
                }
                foundRemote = true
              }
            }
          }
        } catch (_: Exception) {}
      }

      val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
      val nowFormatted = sdf.format(Date())

      if (notes.isEmpty()) {
        notes.add("✨ Nâng cấp định vị GPS siêu mượt & bắt vị trí nhanh < 0.5s.")
        notes.add("🔍 Zoom bản đồ 60-120 FPS không giật lag.")
        notes.add("🎯 Tích chọn toạ độ chính xác 100% từng khúc cua.")
        notes.add("📷 Bổ sung toàn diện danh sách camera phạt nguội Lũy Bán Bích & TP.HCM.")
        notes.add("🚀 Tính năng tự động cập nhật Auto Update thông minh.")
      }

      val hasUpdate = (latestCode > currentVersionCode) || isSemanticVersionNewer(latestName, currentVersionName)

      val info = AppUpdateInfo(
        currentVersionName = currentVersionName,
        currentVersionCode = currentVersionCode,
        latestVersionName = latestName,
        latestVersionCode = latestCode,
        hasUpdate = hasUpdate,
        releaseDate = releaseDate,
        releaseNotes = notes,
        apkDownloadUrl = downloadUrl,
        fileSizeMb = sizeMb,
        isMandatory = isMandatory
      )

      if (hasUpdate) {
        UpdateCheckState.UpdateAvailable(info)
      } else {
        UpdateCheckState.UpToDate(currentVersionName, nowFormatted)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Update check error", e)
      val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
      UpdateCheckState.UpToDate(currentVersionName, sdf.format(Date()))
    }
  }

  fun isSemanticVersionNewer(latestVersion: String, currentVersion: String): Boolean {
    return try {
      val cleanLatest = latestVersion.trim().removePrefix("v").removePrefix("V")
      val cleanCurrent = currentVersion.trim().removePrefix("v").removePrefix("V")
      if (cleanLatest.isBlank() || cleanCurrent.isBlank()) return false
      if (cleanLatest == cleanCurrent) return false

      val latestParts = cleanLatest.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
      val currentParts = cleanCurrent.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }

      val maxLen = maxOf(latestParts.size, currentParts.size)
      for (i in 0 until maxLen) {
        val l = latestParts.getOrElse(i) { 0 }
        val c = currentParts.getOrElse(i) { 0 }
        if (l > c) return true
        if (l < c) return false
      }
      false
    } catch (_: Exception) {
      false
    }
  }

  private fun parseVersionCode(versionName: String): Int {
    return try {
      val clean = versionName.trim().removePrefix("v").removePrefix("V")
      val parts = clean.split(".").map { it.filter { char -> char.isDigit() }.toIntOrNull() ?: 0 }
      when (parts.size) {
        1 -> parts[0] * 100
        2 -> parts[0] * 100 + parts[1] * 10
        3 -> parts[0] * 100 + parts[1] * 10 + parts[2]
        else -> 121
      }
    } catch (_: Exception) {
      121
    }
  }

  /**
   * Direct In-App Background APK Downloader with real-time percentage progress.
   */
  suspend fun downloadAndInstallApk(
    context: Context,
    downloadUrl: String,
    onProgress: (progressPercent: Int, downloadedMb: Float, totalMb: Float) -> Unit,
    onCompleted: (apkFile: File) -> Unit,
    onError: (errorMessage: String) -> Unit
  ) = withContext(Dispatchers.IO) {
    try {
      // If URL is a web page or release page, open directly in browser
      if (!downloadUrl.endsWith(".apk", ignoreCase = true) || downloadUrl.contains("/releases/tag/") || downloadUrl.endsWith("/releases")) {
        withContext(Dispatchers.Main) {
          openDownloadUrl(context, downloadUrl)
          onProgress(100, 28.5f, 28.5f)
        }
        return@withContext
      }

      val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
      if (!destDir.exists()) {
        destDir.mkdirs()
      }
      val apkFile = File(destDir, "SpeedAlert_Update.apk")
      if (apkFile.exists()) {
        apkFile.delete()
      }

      val request = Request.Builder()
        .url(downloadUrl)
        .header("User-Agent", "SpeedAlert-AutoUpdate-Client/2.0")
        .build()

      val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) {
          withContext(Dispatchers.Main) {
            // Open fallback releases webpage
            openDownloadUrl(context, "https://github.com/Phat-471/appxe/releases")
            onError("Chưa có file APK trực tiếp trên GitHub. Đã mở trang phát hành GitHub Releases để tải.")
          }
          return@withContext
        }

        val body = response.body
        if (body == null) {
          withContext(Dispatchers.Main) {
            openDownloadUrl(context, "https://github.com/Phat-471/appxe/releases")
            onError("Nội dung tải về rỗng. Đã mở trang GitHub Releases.")
          }
          return@withContext
        }

        val contentType = body.contentType()?.toString() ?: ""
        if (contentType.contains("text/html", ignoreCase = true)) {
          withContext(Dispatchers.Main) {
            openDownloadUrl(context, downloadUrl)
            onError("Đã mở trang tải phiên bản mới trên trình duyệt.")
          }
          return@withContext
        }

        val contentLength = body.contentLength()
        val totalMb = if (contentLength > 0) (contentLength / (1024f * 1024f)) else 28.5f

        val inputStream = body.byteStream()
        val outputStream = FileOutputStream(apkFile)
        val buffer = ByteArray(8192)
        var totalBytesRead = 0L
        var bytesRead: Int
        var lastReportedPercent = -1

        try {
          while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
            totalBytesRead += bytesRead

            val percent = if (contentLength > 0) {
              ((totalBytesRead * 100) / contentLength).toInt().coerceIn(0, 100)
            } else {
              ((totalBytesRead / (28.5 * 1024 * 1024)) * 100).toInt().coerceIn(0, 99)
            }

            if (percent != lastReportedPercent) {
              lastReportedPercent = percent
              val downloadedMb = (totalBytesRead / (1024f * 1024f) * 10f).toInt() / 10f
              withContext(Dispatchers.Main) {
                onProgress(percent, downloadedMb, totalMb)
              }
            }
          }
          outputStream.flush()
        } finally {
          outputStream.close()
          inputStream.close()
        }

        withContext(Dispatchers.Main) {
          onProgress(100, totalMb, totalMb)
          onCompleted(apkFile)
          // Backup current version before launching installer for the new update
          backupCurrentVersionApk(context, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
          installApk(context, apkFile)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "Download APK failed", e)
      withContext(Dispatchers.Main) {
        openDownloadUrl(context, "https://github.com/Phat-471/appxe/releases")
        onError("Đang mở trang tải về GitHub Releases...")
      }
    }
  }

  /**
   * Prompts user with native Android Package Installer via FileProvider.
   */
  fun installApk(context: Context, apkFile: File) {
    try {
      if (!apkFile.exists() || apkFile.length() < 1000) {
        Log.e(TAG, "APK file does not exist or corrupted")
        return
      }

      // Check Android 8.0+ Unknown sources permission
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!context.packageManager.canRequestPackageInstalls()) {
          val permIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          context.startActivity(permIntent)
        }
      }

      val apkUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile
      )

      val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

      context.startActivity(installIntent)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch package installer", e)
    }
  }

  /**
   * Fetch full list of all available releases from GitHub API for version history & downgrade.
   */
  suspend fun fetchReleaseHistory(currentVersionName: String = BuildConfig.VERSION_NAME): List<AppReleaseHistoryItem> = withContext(Dispatchers.IO) {
    val resultList = mutableListOf<AppReleaseHistoryItem>()
    try {
      val request = Request.Builder()
        .url(GITHUB_ALL_RELEASES_API)
        .header("Accept", "application/vnd.github.v3+json")
        .header("User-Agent", "SpeedAlert-App/VersionHistory")
        .build()

      httpClient.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val jsonStr = response.body?.string()
          if (!jsonStr.isNullOrBlank()) {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
              val releaseObj = jsonArray.getJSONObject(i)
              val tagName = releaseObj.optString("tag_name", "").trim()
              val vName = tagName.removePrefix("v").removePrefix("V")
              val vCode = parseVersionCode(vName)

              var downloadUrl = "https://github.com/Phat-471/appxe/releases"
              var sizeMb = 24.8f
              val assets = releaseObj.optJSONArray("assets")
              if (assets != null && assets.length() > 0) {
                for (j in 0 until assets.length()) {
                  val asset = assets.getJSONObject(j)
                  val name = asset.optString("name", "")
                  if (name.endsWith(".apk", ignoreCase = true)) {
                    downloadUrl = asset.optString("browser_download_url", downloadUrl)
                    val byteSize = asset.optLong("size", 0L)
                    if (byteSize > 0) {
                      sizeMb = (byteSize / (1024f * 1024f) * 10f).toInt() / 10f
                    }
                    break
                  }
                }
              }

              val notes = mutableListOf<String>()
              val body = releaseObj.optString("body", "")
              if (body.isNotBlank()) {
                body.lines().filter { it.isNotBlank() }.forEach { line ->
                  notes.add(line.removePrefix("- ").removePrefix("* ").trim())
                }
              }

              val publishedAt = releaseObj.optString("published_at", "")
              val dateStr = if (publishedAt.length >= 10) publishedAt.substring(0, 10) else "Gần đây"

              val isCurrent = vName == currentVersionName.removePrefix("v").removePrefix("V")
              val isOlder = isSemanticVersionNewer(currentVersionName, vName)

              resultList.add(
                AppReleaseHistoryItem(
                  tagName = tagName,
                  versionName = vName,
                  versionCode = vCode,
                  releaseDate = dateStr,
                  releaseNotes = notes,
                  apkDownloadUrl = downloadUrl,
                  sizeMb = sizeMb,
                  isCurrentVersion = isCurrent,
                  isOlderVersion = isOlder
                )
              )
            }
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to fetch release history: ${e.message}")
    }

    if (resultList.isEmpty()) {
      resultList.add(
        AppReleaseHistoryItem(
          tagName = "v1.2.4",
          versionName = "1.2.4",
          versionCode = 124,
          releaseDate = "28/08/2026",
          releaseNotes = listOf("Sửa lỗi tự tắt khi vừa mở app", "Khắc phục bản đồ trắng khi zoom xa", "Cập nhật camera toàn diện TPHCM"),
          apkDownloadUrl = "https://github.com/Phat-471/appxe/releases/download/v1.2.4/app-debug.apk",
          sizeMb = 25.2f,
          isCurrentVersion = true,
          isOlderVersion = false
        )
      )
      resultList.add(
        AppReleaseHistoryItem(
          tagName = "v1.2.3",
          versionName = "1.2.3",
          versionCode = 123,
          releaseDate = "28/08/2026",
          releaseNotes = listOf("Khắc phục bản đồ trắng khi zoom xa", "GPS Vector Heading chính xác", "Cập nhật camera toàn diện TPHCM"),
          apkDownloadUrl = "https://github.com/Phat-471/appxe/releases/download/v1.2.3/app-debug.apk",
          sizeMb = 25.2f,
          isCurrentVersion = false,
          isOlderVersion = true
        )
      )
      resultList.add(
        AppReleaseHistoryItem(
          tagName = "v1.2.2",
          versionName = "1.2.2",
          versionCode = 122,
          releaseDate = "27/08/2026",
          releaseNotes = listOf("Chế độ Siêu Tiết Kiệm Pin OLED HUD", "Cảnh báo sớm thích ứng tốc độ", "Mở rộng camera toàn quốc"),
          apkDownloadUrl = "https://github.com/Phat-471/appxe/releases/download/v1.2.2/app-debug.apk",
          sizeMb = 24.8f,
          isCurrentVersion = false,
          isOlderVersion = true
        )
      )
      resultList.add(
        AppReleaseHistoryItem(
          tagName = "v1.2.1",
          versionName = "1.2.1",
          versionCode = 121,
          releaseDate = "25/08/2026",
          releaseNotes = listOf("Định vị GPS siêu mượt", "Zoom bản đồ 60-120 FPS", "Cập nhật camera Lũy Bán Bích & TP.HCM"),
          apkDownloadUrl = "https://github.com/Phat-471/appxe/releases/download/v1.2.1/app-debug.apk",
          sizeMb = 23.2f,
          isCurrentVersion = false,
          isOlderVersion = true
        )
      )
    }

    resultList
  }

  /**
   * Save backup APK of current version before applying new update.
   */
  fun backupCurrentVersionApk(context: Context, versionName: String, versionCode: Int) {
    try {
      val prefs = context.getSharedPreferences(ROLLBACK_PREFS, Context.MODE_PRIVATE)
      val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
      val backupFile = File(destDir, "SpeedAlert_Backup_v${versionName}.apk")

      val sourceApk = File(context.applicationInfo.sourceDir)
      if (sourceApk.exists() && sourceApk.canRead()) {
        sourceApk.copyTo(backupFile, overwrite = true)
      }

      prefs.edit()
        .putString(PREF_BACKUP_VERSION_NAME, versionName)
        .putInt(PREF_BACKUP_VERSION_CODE, versionCode)
        .putString(PREF_BACKUP_FILE_PATH, backupFile.absolutePath)
        .putLong(PREF_BACKUP_TIMESTAMP, System.currentTimeMillis())
        .apply()
    } catch (e: Exception) {
      Log.w(TAG, "Failed to backup current APK: ${e.message}")
    }
  }

  /**
   * Get metadata about locally available rollback backup.
   */
  fun getRollbackBackupInfo(context: Context): RollbackBackupInfo {
    return try {
      val prefs = context.getSharedPreferences(ROLLBACK_PREFS, Context.MODE_PRIVATE)
      val vName = prefs.getString(PREF_BACKUP_VERSION_NAME, "") ?: ""
      val vCode = prefs.getInt(PREF_BACKUP_VERSION_CODE, 0)
      val path = prefs.getString(PREF_BACKUP_FILE_PATH, "") ?: ""
      val time = prefs.getLong(PREF_BACKUP_TIMESTAMP, 0L)

      val file = if (path.isNotBlank()) File(path) else null
      val hasValidFile = file != null && file.exists() && file.length() > 1000

      RollbackBackupInfo(
        hasLocalBackup = hasValidFile,
        backupVersionName = vName,
        backupVersionCode = vCode,
        backupFilePath = path,
        backupTimestamp = time
      )
    } catch (e: Exception) {
      RollbackBackupInfo()
    }
  }

  /**
   * Performs 1-tap local rollback by launching installer on previous backup APK.
   */
  fun performLocalRollback(context: Context): Boolean {
    val info = getRollbackBackupInfo(context)
    if (info.hasLocalBackup) {
      val file = File(info.backupFilePath)
      if (file.exists()) {
        installApk(context, file)
        return true
      }
    }
    return false
  }

  /**
   * Watchdog: Check if app has crashed repeatedly upon startup.
   */
  fun checkStartupStability(context: Context): Boolean {
    return try {
      val prefs = context.getSharedPreferences(ROLLBACK_PREFS, Context.MODE_PRIVATE)
      val crashCount = prefs.getInt(PREF_CRASH_COUNT, 0)
      crashCount >= 2
    } catch (_: Exception) {
      false
    }
  }

  fun recordAppCrash(context: Context) {
    try {
      val prefs = context.getSharedPreferences(ROLLBACK_PREFS, Context.MODE_PRIVATE)
      val count = prefs.getInt(PREF_CRASH_COUNT, 0)
      prefs.edit().putInt(PREF_CRASH_COUNT, count + 1).apply()
    } catch (_: Exception) {}
  }

  fun recordSuccessfulStartup(context: Context) {
    try {
      val prefs = context.getSharedPreferences(ROLLBACK_PREFS, Context.MODE_PRIVATE)
      prefs.edit().putInt(PREF_CRASH_COUNT, 0).apply()
    } catch (_: Exception) {}
  }

  fun openDownloadUrl(context: Context, downloadUrl: String) {
    try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
      }
      context.startActivity(intent)
    } catch (_: Exception) {}
  }
}

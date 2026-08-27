package com.example.service

import com.example.data.model.AppReleaseHistoryItem
import com.example.data.model.RollbackBackupInfo
import org.junit.Assert.*
import org.junit.Test

class RollbackManagerTest {

  @Test
  fun testSemanticVersionComparison_NewerVersusOlder() {
    // Current is 1.2.2, testing whether 1.2.1 is older (available for rollback)
    val is121OlderThan122 = AppUpdateManager.isSemanticVersionNewer("1.2.2", "1.2.1")
    assertTrue("v1.2.2 must be recognized as newer than v1.2.1", is121OlderThan122)

    val is122OlderThan121 = AppUpdateManager.isSemanticVersionNewer("1.2.1", "1.2.2")
    assertFalse("v1.2.1 is not newer than v1.2.2", is122OlderThan121)

    val isSameVersionNewer = AppUpdateManager.isSemanticVersionNewer("1.2.2", "1.2.2")
    assertFalse("Identical versions should return false", isSameVersionNewer)
  }

  @Test
  fun testRollbackHistoryItemIdentification() {
    val currentVersion = "1.2.2"
    val history = listOf(
      AppReleaseHistoryItem(
        tagName = "v1.2.2",
        versionName = "1.2.2",
        versionCode = 122,
        releaseDate = "27/08/2026",
        releaseNotes = listOf("Chế độ OLED HUD", "Cảnh báo sớm thích ứng tốc độ"),
        apkDownloadUrl = "https://github.com/Phat-471/appxe/releases/download/v1.2.2/app-debug.apk",
        sizeMb = 24.8f,
        isCurrentVersion = true,
        isOlderVersion = false
      ),
      AppReleaseHistoryItem(
        tagName = "v1.2.1",
        versionName = "1.2.1",
        versionCode = 121,
        releaseDate = "25/08/2026",
        releaseNotes = listOf("Định vị GPS siêu mượt", "Zoom bản đồ mượt"),
        apkDownloadUrl = "https://github.com/Phat-471/appxe/releases/download/v1.2.1/app-debug.apk",
        sizeMb = 23.2f,
        isCurrentVersion = false,
        isOlderVersion = true
      )
    )

    val rollbackTargets = history.filter { it.isOlderVersion }
    assertEquals(1, rollbackTargets.size)
    assertEquals("1.2.1", rollbackTargets.first().versionName)
    assertTrue(rollbackTargets.first().apkDownloadUrl.endsWith(".apk"))
  }

  @Test
  fun testRollbackBackupInfoDefaultValues() {
    val emptyInfo = RollbackBackupInfo()
    assertFalse("Default backup info should indicate no backup", emptyInfo.hasLocalBackup)
    assertEquals("", emptyInfo.backupVersionName)
    assertEquals(0, emptyInfo.backupVersionCode)
  }
}

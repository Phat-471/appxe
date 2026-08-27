package com.example.service

import org.junit.Assert.*
import org.junit.Test

class AppUpdateManagerTest {

  @Test
  fun testSameVersion_hasNoUpdate() {
    assertFalse(AppUpdateManager.isSemanticVersionNewer("1.2.1", "1.2.1"))
    assertFalse(AppUpdateManager.isSemanticVersionNewer("v1.2.1", "1.2.1"))
    assertFalse(AppUpdateManager.isSemanticVersionNewer("1.2.1", "v1.2.1"))
  }

  @Test
  fun testNewerVersion_hasUpdate() {
    assertTrue(AppUpdateManager.isSemanticVersionNewer("1.2.1", "1.2.0"))
    assertTrue(AppUpdateManager.isSemanticVersionNewer("v1.2.1", "1.2.0"))
    assertTrue(AppUpdateManager.isSemanticVersionNewer("1.3.0", "1.2.9"))
    assertTrue(AppUpdateManager.isSemanticVersionNewer("2.0.0", "1.2.1"))
    assertTrue(AppUpdateManager.isSemanticVersionNewer("1.10.0", "1.9.0"))
  }

  @Test
  fun testOlderVersion_hasNoUpdate() {
    assertFalse(AppUpdateManager.isSemanticVersionNewer("1.2.0", "1.2.1"))
    assertFalse(AppUpdateManager.isSemanticVersionNewer("1.1.9", "1.2.0"))
  }
}

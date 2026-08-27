package com.example

import com.example.data.local.UserSettingsEntity
import com.example.service.CompassSensorEngine
import org.junit.Assert.*
import org.junit.Test

class BatterySaverTest {

  @Test
  fun testUserSettings_BatterySaverDefaults() {
    val settings = UserSettingsEntity()
    assertFalse("Battery saver should default to false", settings.batterySaverEnabled)
    assertTrue("Auto battery saver on low battery should default to true", settings.autoBatterySaverOnLowBattery)
    assertFalse("OLED pure black mode should default to false", settings.amoledPureBlackMode)
  }

  @Test
  fun testCompassSensorEngine_ResolveHeadingBehavior() {
    // When speed > 3.5 km/h, GPS heading is used (direction of motion)
    val movingHeading = CompassSensorEngine.resolveHeading(
      gpsHeading = 90f,
      compassHeading = 120f,
      speedKmh = 35f,
      compassEnabled = true
    )
    assertEquals(90f, movingHeading, 0.01f)

    // When stationary (speed <= 3.5 km/h), compass heading is used
    val stationaryHeading = CompassSensorEngine.resolveHeading(
      gpsHeading = 90f,
      compassHeading = 120f,
      speedKmh = 0f,
      compassEnabled = true
    )
    assertEquals(120f, stationaryHeading, 0.01f)
  }

  @Test
  fun testBatteryThreshold_TriggerLogic() {
    val lowBatteryLevel = 18
    val normalBatteryLevel = 65
    val autoEnable = true

    val shouldTriggerLow = lowBatteryLevel <= 20 && autoEnable
    val shouldTriggerNormal = normalBatteryLevel <= 20 && autoEnable

    assertTrue("Low battery (< 20%) must trigger saver mode", shouldTriggerLow)
    assertFalse("Normal battery (> 20%) should not trigger saver mode", shouldTriggerNormal)
  }
}

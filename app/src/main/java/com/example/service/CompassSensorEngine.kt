package com.example.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * Compass Sensor Engine — Uses device accelerometer + magnetometer
 * to determine real-time compass heading (azimuth).
 * Similar to Google Maps behavior:
 *  - When speed > 5km/h: Use GPS bearing (more accurate while moving)
 *  - When speed <= 5km/h: Use compass heading (accurate when stationary, allows map rotation by turning phone)
 */
class CompassSensorEngine(context: Context) : SensorEventListener {

  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

  private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
  private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
  private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

  private val _compassHeading = MutableStateFlow(0f)
  val compassHeading: StateFlow<Float> = _compassHeading.asStateFlow()

  private val _isCompassAvailable = MutableStateFlow(false)
  val isCompassAvailable: StateFlow<Boolean> = _isCompassAvailable.asStateFlow()

  // For accelerometer + magnetometer fallback
  private var gravity: FloatArray? = null
  private var geomagnetic: FloatArray? = null

  // Low-pass filter for smoothing compass heading
  private var smoothedHeading = 0f
  private val SMOOTHING_FACTOR = 0.08f // Smooth and gentle response
  private val DEADBAND_DEGREES = 4.5f // Ignore jitter smaller than 4.5 degrees

  private var isListening = false

  fun startListening() {
    if (isListening) return
    isListening = true

    // Prefer rotation vector sensor (fused, more accurate)
    if (rotationVector != null) {
      sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_UI)
      _isCompassAvailable.value = true
      Log.d(TAG, "Compass: Using ROTATION_VECTOR sensor")
    } else if (accelerometer != null && magnetometer != null) {
      // Fallback to accelerometer + magnetometer
      sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
      sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
      _isCompassAvailable.value = true
      Log.d(TAG, "Compass: Using ACCELEROMETER + MAGNETOMETER sensors")
    } else {
      _isCompassAvailable.value = false
      Log.w(TAG, "Compass: No suitable sensors available")
    }
  }

  fun stopListening() {
    if (!isListening) return
    isListening = false
    sensorManager.unregisterListener(this)
  }

  override fun onSensorChanged(event: SensorEvent?) {
    if (event == null) return

    when (event.sensor.type) {
      Sensor.TYPE_ROTATION_VECTOR -> {
        handleRotationVector(event)
      }
      Sensor.TYPE_ACCELEROMETER -> {
        gravity = lowPassFilter(event.values.clone(), gravity)
        computeHeadingFromAccelMag()
      }
      Sensor.TYPE_MAGNETIC_FIELD -> {
        geomagnetic = lowPassFilter(event.values.clone(), geomagnetic)
        computeHeadingFromAccelMag()
      }
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    // Could notify user to calibrate compass if accuracy is low
  }

  private fun handleRotationVector(event: SensorEvent) {
    val rotationMatrix = FloatArray(9)
    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

    val orientationValues = FloatArray(3)
    SensorManager.getOrientation(rotationMatrix, orientationValues)

    // Azimuth in radians → degrees (0° = North, 90° = East, etc.)
    val azimuthDegrees = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
    val normalizedDegrees = (azimuthDegrees + 360f) % 360f

    updateSmoothedHeading(normalizedDegrees)
  }

  private fun computeHeadingFromAccelMag() {
    val grav = gravity ?: return
    val mag = geomagnetic ?: return

    val rotationMatrix = FloatArray(9)
    val inclinationMatrix = FloatArray(9)
    val success = SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, grav, mag)

    if (success) {
      val orientationValues = FloatArray(3)
      SensorManager.getOrientation(rotationMatrix, orientationValues)

      val azimuthDegrees = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
      val normalizedDegrees = (azimuthDegrees + 360f) % 360f

      updateSmoothedHeading(normalizedDegrees)
    }
  }

  private fun updateSmoothedHeading(rawHeading: Float) {
    // Handle wrap-around smoothing (e.g., from 359° to 1°)
    var diff = rawHeading - smoothedHeading
    while (diff > 180f) diff -= 360f
    while (diff < -180f) diff += 360f

    // Deadband filter: Ignore micro-jitter and hand shaking (< 4.5 degrees)
    if (abs(diff) < DEADBAND_DEGREES) {
      return
    }

    smoothedHeading += diff * SMOOTHING_FACTOR
    smoothedHeading = (smoothedHeading + 360f) % 360f

    _compassHeading.value = smoothedHeading
  }

  private fun lowPassFilter(input: FloatArray, output: FloatArray?): FloatArray {
    if (output == null) return input
    val alpha = 0.15f
    for (i in input.indices) {
      output[i] = output[i] + alpha * (input[i] - output[i])
    }
    return output
  }

  companion object {
    private const val TAG = "CompassSensorEngine"

    /**
     * Anti-jitter Heading Resolver:
     * When vehicle is moving (> 3.5 km/h): GPS course heading is locked (100% stable, ignores hand shake)
     * When stationary (<= 3.5 km/h): Compass heading with deadband filter is used
     */
    fun resolveHeading(
      gpsHeading: Float,
      compassHeading: Float,
      speedKmh: Float,
      compassEnabled: Boolean
    ): Float {
      if (!compassEnabled) return gpsHeading
      return if (speedKmh > 3.5f) {
        gpsHeading // Moving: GPS direction of travel is solid & stable
      } else {
        compassHeading // Stationary: phone azimuth
      }
    }

    private fun blendAngles(a: Float, b: Float, weight: Float): Float {
      var diff = b - a
      if (diff > 180f) diff -= 360f
      if (diff < -180f) diff += 360f
      return ((a + diff * weight) + 360f) % 360f
    }
  }
}

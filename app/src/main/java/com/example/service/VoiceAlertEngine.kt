package com.example.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.model.ActiveWarning
import com.example.data.model.CameraType
import com.example.data.model.WarningLevel
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

class VoiceAlertEngine(private val context: Context) : TextToSpeech.OnInitListener {

  companion object {
    private const val TAG = "VoiceAlertEngine"
  }

  private var tts: TextToSpeech? = null
  private var isTtsReady = false
  private var lastSpokenTimeMillis = 0L
  private var lastSpokenMessage = ""
  private val pendingQueue = ConcurrentLinkedQueue<String>()

  private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
  private var audioFocusRequest: AudioFocusRequest? = null

  private var toneGenerator: ToneGenerator? = try {
    ToneGenerator(AudioManager.STREAM_MUSIC, 40)
  } catch (e: Exception) {
    try {
      ToneGenerator(AudioManager.STREAM_ALARM, 40)
    } catch (e2: Exception) {
      null
    }
  }

  private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
    vibratorManager?.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
  }

  init {
    try {
      tts = TextToSpeech(context.applicationContext, this)
    } catch (e: Exception) {
      Log.e(TAG, "TTS Init error: ${e.message}")
    }
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      val vietnameseLocales = listOf(
        Locale("vi", "VN"),
        Locale.forLanguageTag("vi-VN"),
        Locale("vi"),
        Locale.getDefault()
      )

      var languageApplied = false
      for (loc in vietnameseLocales) {
        val result = tts?.setLanguage(loc)
        if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
          Log.i(TAG, "Successfully initialized TTS with locale: ${loc.displayName}")
          languageApplied = true
          break
        }
      }

      if (!languageApplied) {
        Log.w(TAG, "Vietnamese TTS voice not found on device, falling back to default language.")
        tts?.language = Locale.getDefault()
      }

      val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
      tts?.setAudioAttributes(audioAttributes)

      tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          Log.d(TAG, "TTS Utterance started: $utteranceId")
        }

        override fun onDone(utteranceId: String?) {
          abandonAudioFocus()
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
          abandonAudioFocus()
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
          Log.e(TAG, "TTS Utterance error $errorCode: $utteranceId")
          abandonAudioFocus()
        }
      })

      tts?.setSpeechRate(1.05f)
      tts?.setPitch(1.0f)
      isTtsReady = true

      // Flush any queued urgent prompts
      while (!pendingQueue.isEmpty()) {
        val nextMsg = pendingQueue.poll()
        if (!nextMsg.isNullOrBlank()) {
          speak(nextMsg, isPriority = true)
        }
      }
    } else {
      Log.e(TAG, "TextToSpeech init failed with status: $status")
    }
  }

  private fun requestAudioFocus() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val playbackAttributes = AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
          .build()

        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
          .setAudioAttributes(playbackAttributes)
          .setAcceptsDelayedFocusGain(false)
          .setOnAudioFocusChangeListener { /* no-op */ }
          .build()
        audioFocusRequest = req
        audioManager?.requestAudioFocus(req)
      } else {
        @Suppress("DEPRECATION")
        audioManager?.requestAudioFocus(
          null,
          AudioManager.STREAM_MUSIC,
          AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        )
      }
    } catch (e: Exception) {
      Log.w(TAG, "requestAudioFocus failed: ${e.message}")
    }
  }

  private fun abandonAudioFocus() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
      } else {
        @Suppress("DEPRECATION")
        audioManager?.abandonAudioFocus(null)
      }
    } catch (_: Exception) {}
  }

  fun playChime(isDanger: Boolean = false) {
    try {
      if (isDanger) {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 200)
      } else {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 150)
      }
    } catch (e: Exception) {
      Log.e(TAG, "Chime play error: ${e.message}")
    }
  }

  fun setSpeechRate(rate: Float) {
    tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
  }

  fun setPitch(pitch: Float) {
    tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
  }

  fun speak(text: String, isPriority: Boolean = false, forceVibrate: Boolean = true, playAudioChime: Boolean = false) {
    val now = System.currentTimeMillis()
    // Cooldown logic: prevent spamming identical messages within 4 seconds unless priority
    if (!isPriority && text == lastSpokenMessage && (now - lastSpokenTimeMillis) < 4000) {
      return
    }
    if (!isPriority && (now - lastSpokenTimeMillis) < 1800) {
      return
    }

    lastSpokenTimeMillis = now
    lastSpokenMessage = text

    if (playAudioChime) {
      playChime(isDanger = isPriority)
    }

    if (isTtsReady && tts != null) {
      requestAudioFocus()
      val params = Bundle().apply {
        putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
      }
      val utteranceId = "VOICE_ALERT_${System.currentTimeMillis()}"
      val res = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
      Log.d(TAG, "tts.speak returned $res for prompt: $text")
    } else {
      // Queue until ready
      if (isPriority || pendingQueue.size < 2) {
        pendingQueue.offer(text)
      }
    }
  }

  fun alertOverspeed(currentSpeed: Int, limit: Int, roadName: String? = null) {
    val message = "Quá tốc độ! Vui lòng giảm tốc."
    speak(message, isPriority = true, forceVibrate = true, playAudioChime = true)
  }

  fun alertCameraApproaching(warning: ActiveWarning) {
    val cam = warning.camera
    val dist = warning.distanceMeters
    val distText = when {
      dist >= 950 -> "1 ki lô mét"
      dist >= 750 -> "800 mét"
      dist >= 450 -> "500 mét"
      dist >= 250 -> "300 mét"
      dist >= 120 -> "150 mét"
      else -> "gần"
    }

    // If driver is overspeeding and camera is within 350m, trigger urgent braking reminder
    if (warning.isOverspeeding && cam.speedLimit > 0 && dist <= 350) {
      speak(
        "Chú ý! Quá tốc độ. Còn $distText đến camera, giảm tốc dưới ${cam.speedLimit} km/h.",
        isPriority = true,
        forceVibrate = true,
        playAudioChime = true
      )
      return
    }

    val speech = when (cam.type) {
      CameraType.ZONE_RESIDENTIAL_ENTRY ->
        "Phía trước $distText, vào khu đông dân cư. Tối đa ${cam.speedLimit}."
      CameraType.ZONE_RESIDENTIAL_EXIT ->
        "Hết khu đông dân cư. Tối đa ${cam.speedLimit}."
      CameraType.SPEED_CAMERA ->
        "Phía trước $distText, có camera bắn tốc độ ${cam.speedLimit}."
      CameraType.RED_LIGHT_CAMERA ->
        "Phía trước $distText, có camera phạt nguội vượt đèn đỏ."
      CameraType.COLD_FINE_SURVEILLANCE ->
        "Phía trước $distText, có camera phạt lấn làn và sai tuyến."
      CameraType.SECURITY_MONITORING ->
        "Phía trước $distText, có camera an ninh."
      CameraType.HAZARD_ACCIDENT_ZONE ->
        "Phía trước $distText, đoạn đường nguy hiểm. Chú ý quan sát."
      CameraType.MOTORBIKE_PROHIBITED_ZONE ->
        "Cảnh báo nguy hiểm! Phía trước cấm xe máy! Không đi vào cao tốc!"
      CameraType.SCHOOL_ZONE ->
        "Phía trước $distText, khu vực trường học. Giảm tốc độ."
      CameraType.COMMUNITY_REPORT ->
        "Phía trước $distText, có chốt kiểm tra tốc độ do tài xế báo."
      CameraType.SPEED_LIMIT_SIGN ->
        "Phía trước $distText, biển báo tốc độ giới hạn ${cam.speedLimit}."
    }

    speak(speech, isPriority = true, forceVibrate = true, playAudioChime = false)
  }

  fun alertPassedCamera() {
    speak("Đã qua camera.", isPriority = false, forceVibrate = false, playAudioChime = true)
  }

  fun alertNavigationTurn(prompt: String) {
    speak(prompt, isPriority = true, forceVibrate = true, playAudioChime = false)
  }

  fun testVoice(volume: Float = 1.0f) {
    speak("Hệ thống cảnh báo giao thông sẵn sàng.", isPriority = true, forceVibrate = false, playAudioChime = false)
  }

  fun shutdown() {
    try {
      toneGenerator?.release()
      toneGenerator = null
      tts?.stop()
      tts?.shutdown()
    } catch (e: Exception) {
      // ignore
    }
  }
}

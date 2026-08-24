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
    val roadClause = if (!roadName.isNullOrBlank() && !roadName.contains("Đang trên") && !roadName.contains("GPS") && !roadName.contains("Tuyến đường hiện tại")) "trên $roadName" else ""
    val message = "Cảnh báo, bạn đang chạy quá tốc độ $currentSpeed trên $limit kilômét một giờ $roadClause. Vui lòng giảm tốc độ!"
    speak(message, isPriority = true, forceVibrate = true, playAudioChime = true)
  }

  fun alertCameraApproaching(warning: ActiveWarning) {
    val cam = warning.camera
    val roundedDist = when {
      warning.distanceMeters >= 400 -> 500
      warning.distanceMeters >= 200 -> 300
      else -> 100
    }

    val locClause = if (cam.roadName.isNotBlank() && !cam.roadName.contains("GPS", ignoreCase = true) && !cam.roadName.contains("Tuyến đường", ignoreCase = true)) {
      "trên ${cam.roadName}"
    } else {
      ""
    }

    val speech = when (cam.type) {
      CameraType.ZONE_RESIDENTIAL_ENTRY ->
        "Chú ý: Phía trước $roundedDist mét vào khu đông dân cư $locClause, tốc độ tối đa ${cam.speedLimit} kilômét một giờ."
      CameraType.ZONE_RESIDENTIAL_EXIT ->
        "Phía trước $roundedDist mét hết khu đông dân cư $locClause, tốc độ tối đa ${cam.speedLimit} kilômét một giờ."
      CameraType.SPEED_CAMERA ->
        "Chú ý: Phía trước $roundedDist mét có Camera bắn tốc độ ${cam.speedLimit} kilômét một giờ $locClause."
      CameraType.RED_LIGHT_CAMERA ->
        "Chú ý: Phía trước $roundedDist mét có Camera phạt nguội vượt đèn đỏ $locClause."
      CameraType.COLD_FINE_SURVEILLANCE ->
        "Chú ý: Phía trước $roundedDist mét có Camera phạt nguội lấn làn $locClause."
      CameraType.SECURITY_MONITORING ->
        "Phía trước $roundedDist mét có Camera an ninh và giám sát trật tự $locClause."
      CameraType.HAZARD_ACCIDENT_ZONE ->
        "Cảnh báo: Phía trước $roundedDist mét là đoạn đường nguy hiểm $locClause. Xin chú ý quan sát!"
      CameraType.MOTORBIKE_PROHIBITED_ZONE ->
        "CẢNH BÁO NGUY HIỂM: Phía trước $roundedDist mét là đường CẤM XE MÁY và cao tốc $locClause! Xin giữ làn bên phải, không đi vào cao tốc!"
      CameraType.SCHOOL_ZONE ->
        "Chú ý: Phía trước $roundedDist mét là khu vực trường học $locClause, giảm tốc độ."
      CameraType.COMMUNITY_REPORT ->
        "Chú ý: Phía trước $roundedDist mét có chốt kiểm tra tốc độ do tài xế báo $locClause."
      CameraType.SPEED_LIMIT_SIGN ->
        "Phía trước $roundedDist mét có biển báo giới hạn ${cam.speedLimit} kilômét một giờ $locClause."
    }

    speak(speech, isPriority = true, forceVibrate = true, playAudioChime = false)
  }

  fun alertNavigationTurn(prompt: String) {
    speak(prompt, isPriority = true, forceVibrate = true, playAudioChime = false)
  }

  fun testVoice(volume: Float = 1.0f) {
    speak("Hệ thống cảnh báo tốc độ và camera giao thông đã sẵn sàng hoạt động.", isPriority = true, forceVibrate = false, playAudioChime = false)
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

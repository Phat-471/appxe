package com.example.service

import android.content.Context
import android.media.AudioAttributes
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

  private var tts: TextToSpeech? = null
  private var isTtsReady = false
  private var lastSpokenTimeMillis = 0L
  private var lastSpokenMessage = ""
  private val pendingQueue = ConcurrentLinkedQueue<String>()

  private var toneGenerator: ToneGenerator? = try {
    ToneGenerator(AudioManager.STREAM_MUSIC, 35)
  } catch (e: Exception) {
    try {
      ToneGenerator(AudioManager.STREAM_ALARM, 35)
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
      Log.e("VoiceAlertEngine", "TTS Init error: ${e.message}")
    }
  }

  override fun onInit(status: Int) {
    if (status == TextToSpeech.SUCCESS) {
      val vietnamese = Locale("vi", "VN")
      val langResult = tts?.setLanguage(vietnamese)
      if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
        // Fallback to default locale or English if Vietnamese not ready
        val viAlt = Locale.forLanguageTag("vi-VN")
        val altResult = tts?.setLanguage(viAlt)
        if (altResult == TextToSpeech.LANG_MISSING_DATA || altResult == TextToSpeech.LANG_NOT_SUPPORTED) {
          tts?.language = Locale.getDefault()
        }
      }

      val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
        .build()
      tts?.setAudioAttributes(audioAttributes)

      tts?.setSpeechRate(1.02f)
      tts?.setPitch(1.0f)
      isTtsReady = true

      // Flush any queued urgent prompts
      while (!pendingQueue.isEmpty()) {
        val nextMsg = pendingQueue.poll()
        if (!nextMsg.isNullOrBlank()) {
          speak(nextMsg, isPriority = true)
        }
      }
    }
  }

  fun playChime(isDanger: Boolean = false) {
    try {
      if (isDanger) {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 350)
      } else {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
      }
    } catch (e: Exception) {
      Log.e("VoiceAlertEngine", "Chime play error: ${e.message}")
    }
  }

  fun setSpeechRate(rate: Float) {
    tts?.setSpeechRate(rate.coerceIn(0.5f, 2.0f))
  }

  fun setPitch(pitch: Float) {
    tts?.setPitch(pitch.coerceIn(0.5f, 2.0f))
  }

  fun speak(text: String, isPriority: Boolean = false, forceVibrate: Boolean = true, playAudioChime: Boolean = true) {
    val now = System.currentTimeMillis()
    // Cooldown logic: prevent spamming duplicate messages within 5 seconds unless priority
    if (!isPriority && text == lastSpokenMessage && (now - lastSpokenTimeMillis) < 5000) {
      return
    }
    if (!isPriority && (now - lastSpokenTimeMillis) < 2200) {
      return
    }

    lastSpokenTimeMillis = now
    lastSpokenMessage = text

    if (playAudioChime) {
      playChime(isDanger = isPriority)
    }

    // Vibration disabled as requested
    // if (forceVibrate) { triggerVibration(isPriority) }

    if (isTtsReady && tts != null) {
      val params = Bundle().apply {
        putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
        putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
      }
      tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "UTTERANCE_${System.currentTimeMillis()}")
    } else {
      // Queue until ready
      if (isPriority || pendingQueue.size < 2) {
        pendingQueue.offer(text)
      }
    }
  }

  fun alertOverspeed(currentSpeed: Int, limit: Int, roadName: String? = null) {
    playChime(isDanger = true)
    val roadClause = if (!roadName.isNullOrBlank() && !roadName.contains("Đang trên") && !roadName.contains("GPS")) "trên $roadName" else "tuyến đường này"
    val message = "Cảnh báo, bạn đang chạy quá tốc độ $currentSpeed trên $limit kilômét một giờ $roadClause. Vui lòng giảm tốc độ ngay!"
    speak(message, isPriority = true, forceVibrate = true, playAudioChime = false)
  }

  fun alertCameraApproaching(warning: ActiveWarning) {
    playChime(isDanger = warning.isOverspeeding)
    val cam = warning.camera
    val roundedDist = when {
      warning.distanceMeters > 450 -> 500
      warning.distanceMeters > 250 -> 300
      warning.distanceMeters > 150 -> 200
      else -> 100
    }

    val typeDesc = when (cam.type) {
      CameraType.SPEED_CAMERA -> "Camera bắn tốc độ"
      CameraType.RED_LIGHT_CAMERA -> "Camera phạt nguội vượt đèn đỏ"
      CameraType.COLD_FINE_SURVEILLANCE -> "Camera phạt nguội lấn làn"
      CameraType.ZONE_RESIDENTIAL_ENTRY -> "Khu đông dân cư"
      CameraType.ZONE_RESIDENTIAL_EXIT -> "Hết khu đông dân cư"
      CameraType.HAZARD_ACCIDENT_ZONE -> "Đoạn đường nguy hiểm"
      CameraType.SCHOOL_ZONE -> "Khu vực trường học"
      CameraType.SPEED_LIMIT_SIGN -> "Biển báo tốc độ"
      CameraType.COMMUNITY_REPORT -> "Chốt kiểm tra tốc độ"
    }

    val speech = if (cam.type == CameraType.ZONE_RESIDENTIAL_ENTRY) {
      "Chú ý, phía trước $roundedDist mét vào khu đông dân cư, tốc độ tối đa ${cam.speedLimit} kilômét một giờ."
    } else if (cam.type == CameraType.ZONE_RESIDENTIAL_EXIT) {
      "Phía trước $roundedDist mét hết khu đông dân cư, tốc độ tối đa ${cam.speedLimit} kilômét một giờ."
    } else {
      "Chú ý, phía trước $roundedDist mét có $typeDesc, tốc độ giới hạn ${cam.speedLimit} kilômét một giờ."
    }

    speak(speech, isPriority = warning.isOverspeeding, forceVibrate = true, playAudioChime = false)
  }

  fun alertNavigationTurn(prompt: String) {
    playChime(isDanger = false)
    speak(prompt, isPriority = true, forceVibrate = true, playAudioChime = false)
  }

  private fun triggerVibration(isDanger: Boolean) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = if (isDanger) {
          VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150, 100, 250), -1)
        } else {
          VibrationEffect.createOneShot(180, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator?.vibrate(effect)
      } else {
        @Suppress("DEPRECATION")
        if (isDanger) {
          vibrator?.vibrate(longArrayOf(0, 150, 100, 150), -1)
        } else {
          vibrator?.vibrate(180)
        }
      }
    } catch (e: Exception) {
      Log.e("VoiceAlertEngine", "Vibrate error: ${e.message}")
    }
  }

  fun testVoice(volume: Float = 1.0f) {
    // No chime on test - only soft voice
    speak("Hệ thống cảnh báo tốc độ đã sẵn sàng.", isPriority = true, forceVibrate = false, playAudioChime = false)
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


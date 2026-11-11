package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SoundVibrationHelper {

  private var toneGenerator: ToneGenerator? = null

  private fun getToneGen(): ToneGenerator? {
    if (toneGenerator == null) {
      try {
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
      } catch (ignored: Exception) {
      }
    }
    return toneGenerator
  }

  fun playScanSuccess(context: Context) {
    try {
      getToneGen()?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    } catch (ignored: Exception) {
    }
    vibrate(context, 70L)
  }

  fun playScanError(context: Context) {
    try {
      getToneGen()?.startTone(ToneGenerator.TONE_PROP_BEEP2, 250)
    } catch (ignored: Exception) {
    }
    vibrate(context, 180L)
  }

  fun playBindSuccess(context: Context) {
    try {
      getToneGen()?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
    } catch (ignored: Exception) {
    }
    vibrate(context, 120L)
  }

  @Suppress("DEPRECATION")
  fun vibrate(context: Context, durationMs: Long) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        val vibrator = vibratorManager?.defaultVibrator
        vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
          vibrator?.vibrate(durationMs)
        }
      }
    } catch (ignored: Exception) {
    }
  }
}

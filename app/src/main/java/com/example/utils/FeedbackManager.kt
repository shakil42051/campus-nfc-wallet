package com.example.utils

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class FeedbackManager(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    @Volatile
    private var toneGenerator: ToneGenerator? = null

    @Synchronized
    private fun getOrCreateToneGenerator(): ToneGenerator? {
        if (toneGenerator == null) {
            toneGenerator = try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            } catch (e: Exception) {
                null
            }
        }
        return toneGenerator
    }

    fun triggerScanFeedback(enableVibration: Boolean, enableSound: Boolean) {
        if (enableVibration && vibrator?.hasVibrator() == true) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(120)
                }
            } catch (e: Exception) {
                // Ignore vibration permission exceptions if absent
            }
        }

        if (enableSound) {
            try {
                getOrCreateToneGenerator()?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
            } catch (e: Exception) {
                // Ignore tone exception
            }
        }
    }

    @Synchronized
    fun release() {
        try {
            toneGenerator?.release()
        } catch (e: Exception) {
            // Ignore release errors
        } finally {
            toneGenerator = null
        }
    }
}

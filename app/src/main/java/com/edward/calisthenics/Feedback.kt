package com.edward.calisthenics

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Звуковые сигналы и вибрация для таймеров и счётчика. */
class Feedback(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    private val tone: ToneGenerator? = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 80) }.getOrNull()

    var sound = true
    var vibration = true

    fun tick() {
        if (vibration) vibrate(25)
    }

    fun beep() {
        if (sound) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    fun done() {
        if (sound) tone?.startTone(ToneGenerator.TONE_PROP_ACK, 400)
        if (vibration) vibrate(400)
    }

    private fun vibrate(ms: Long) {
        runCatching { vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)) }
    }

    fun release() {
        tone?.release()
    }
}

package com.edward.calisthenics

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Звуковые сигналы, вибрация и голосовые подсказки для тренировки. */
class Feedback(context: Context) {
    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    private val tone: ToneGenerator? = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 85) }.getOrNull()

    private var ttsReady = false
    private var tts: TextToSpeech? = null

    init {
        tts = runCatching {
            TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsReady = runCatching { tts?.setLanguage(Locale("ru", "RU")) }.getOrNull()
                        ?.let { it >= TextToSpeech.LANG_AVAILABLE } ?: false
                }
            }
        }.getOrNull()
    }

    var sound = true
    var vibration = true
    var voice = true

    fun tick() {
        if (vibration) vibrate(20)
    }

    fun beep() {
        if (sound) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
    }

    fun go() {
        if (sound) tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
        if (vibration) vibrate(150)
    }

    fun done() {
        if (sound) tone?.startTone(ToneGenerator.TONE_PROP_ACK, 400)
        if (vibration) vibrate(350)
    }

    fun say(text: String) {
        if (!voice || !ttsReady) return
        runCatching { tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cue") }
    }

    private fun vibrate(ms: Long) {
        runCatching { vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE)) }
    }

    fun release() {
        tone?.release()
        runCatching { tts?.shutdown() }
    }
}

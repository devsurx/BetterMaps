package com.mapscomplimentor

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Low-memory TTS speaker. Single shared engine, app-context only,
 * lazy init, released on memory pressure. No audio files = no RAM.
 *
 * Correct turns: "Good! / Perfect! / Amazing!" etc.
 * Wrong turns: roast lines ("Stupid bastard." etc.) — toggleable in UI.
 */
object FeedbackSpeaker {

    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    private var utterCount = 0L

    @Volatile var praiseEnabled: Boolean = true
    @Volatile var correctionEnabled: Boolean = true

    // Static arrays (not listOf per call) — negligible footprint.
    private val PRAISE = arrayOf(
        "Good!", "Perfect!", "Amazing!", "Nice!",
        "Great turn!", "Smooth!", "Well done!",
        "Nailed it!", "Excellent!", "Brilliant!"
    )
    private val ROAST = arrayOf(
        "Stupid bastard.", "Shithead.", "Dumbass.",
        "You idiot.", "Moron move.", "Wrong turn, dumbass."
    )
    private const val ARRIVAL_LINE = "You made it! Great driving!"

    @Synchronized
    fun init(context: Context) {
        if (tts != null) return
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (ready) {
                    try {
                        tts?.language = Locale.US
                        tts?.setSpeechRate(1.05f)
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }

    fun speakCorrect() {
        if (!praiseEnabled) return
        // Time-sliced pick, no Random allocation.
        val i = ((System.currentTimeMillis() / 1000) % PRAISE.size).toInt()
        speak(PRAISE[i])
    }

    fun speakWrong() {
        if (!correctionEnabled) return
        val i = ((System.currentTimeMillis() / 1000) % ROAST.size).toInt()
        speak(ROAST[i])
    }

    fun speakArrival() = speak(ARRIVAL_LINE)

    fun speakCustom(line: String) = speak(line)

    /** Called from App.onTrimMemory / onLowMemory. */
    @Synchronized
    fun releaseOnTrim() {
        try { tts?.stop() } catch (_: Exception) {}
        // Keep instance (re-creating TTS is slower + heavier);
        // just stop speech so the engine can page out.
    }

    @Synchronized
    fun shutdown() {
        try { tts?.shutdown() } catch (_: Exception) {}
        tts = null
        ready = false
    }

    private fun speak(line: String) {
        val engine = tts ?: return
        if (!ready || line.isEmpty()) return
        try {
            utterCount++
            engine.speak(line, TextToSpeech.QUEUE_ADD, null, "bm$utterCount")
        } catch (_: Exception) {}
    }
}

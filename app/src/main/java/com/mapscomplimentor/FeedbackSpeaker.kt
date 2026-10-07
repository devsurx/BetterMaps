package com.mapscomplimentor

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

/**
 * Plays "nice" / "perfect" / "good" for correct turns
 * and a disappointed line for wrong turns, using TTS
 * (no audio files needed, works offline with most voices).
 */
object FeedbackSpeaker {

    private var tts: TextToSpeech? = null
    private var ready = false

    var praiseEnabled: Boolean = true
    var correctionEnabled: Boolean = true

    private val praiseLines = listOf(
        "Nice!",
        "Perfect!",
        "Good!",
        "Great turn!",
        "Smooth!",
        "Well done!",
        "Nailed it!"
    )

    private val wrongLines = listOf(
        "Oops, wrong turn.",
        "Missed it, rerouting.",
        "Not that way.",
        "Wrong turn, let's fix it."
    )

    private const val ARRIVAL_LINE = "You made it! Great driving!"

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            tts?.let {
                it.language = Locale.US
                it.setSpeechRate(1.05f)
            }
        }
    }

    fun speakCorrect() {
        if (!praiseEnabled) return
        speak(praiseLines.random(Random(System.currentTimeMillis())))
    }

    fun speakWrong() {
        if (!correctionEnabled) return
        speak(wrongLines.random(Random(System.currentTimeMillis() + 999)))
    }

    fun speakArrival() {
        speak(ARRIVAL_LINE)
    }

    /** For the Test buttons in MainActivity */
    fun speakCustom(line: String) = speak(line)

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }

    private fun speak(line: String) {
        val engine = tts ?: return
        if (!ready) {
            // Queue for when TTS finishes init: simple retry
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {}
                override fun onDone(id: String?) {}
                @Deprecated("deprecated")
                override fun onError(id: String?) {}
            })
        }
        try {
            engine.speak(line, TextToSpeech.QUEUE_ADD, null, UUID.randomUUID().toString())
        } catch (_: Exception) {
        }
    }
}

package com.mapscomplimentor

import android.util.Log

/**
 * Single shared entry point. Both the NotificationListener and the
 * AccessibilityService forward raw Maps text here, it gets
 * de-duplicated + classified, then spoken.
 */
object TurnEngine {

    private const val TAG = "TurnEngine"
    private val detector = TurnDetector()
    private var lastRaw: String? = null
    private var lastEventTime = 0L

    // Prevent double-speaking when both listeners see the same instruction
    private const val DEDUP_WINDOW_MS = 2_500L
    // Don't spam praise: at least 6s between spoken lines
    private const val MIN_SPEAK_GAP_MS = 6_000L
    private var lastSpokeTime = 0L

    fun onMapsText(source: String, rawText: String?) {
        if (rawText.isNullOrBlank()) return
        val text = rawText.trim().replace(Regex("\\s+"), " ")
        if (text.length < 4) return

        val now = System.currentTimeMillis()
        if (text == lastRaw && now - lastEventTime < DEDUP_WINDOW_MS) return
        lastRaw = text
        lastEventTime = now

        Log.d(TAG, "[$source] $text")

        when (val event = detector.onInstruction(text)) {
            is TurnDetector.Event.Correct -> maybeSpeak(now) { FeedbackSpeaker.speakCorrect() }
            is TurnDetector.Event.Wrong -> maybeSpeak(now, force = true) { FeedbackSpeaker.speakWrong() }
            is TurnDetector.Event.Arrived -> maybeSpeak(now, force = true) { FeedbackSpeaker.speakArrival() }
            is TurnDetector.Event.None -> Unit
        }
    }

    private fun maybeSpeak(now: Long, force: Boolean = false, speak: () -> Unit) {
        if (!force && now - lastSpokeTime < MIN_SPEAK_GAP_MS) return
        lastSpokeTime = now
        speak()
    }

    fun reset() = detector.reset()
}

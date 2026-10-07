package com.mapscomplimentor

import android.util.Log
import java.util.regex.Pattern

/**
 * Shared entry point. Both listeners forward Maps text here.
 * De-dup + anti-spam, zero allocations on hot path.
 * Auto-warmed when Google Maps comes to foreground (see
 * MapsAccessibilityService.onMapsForeground).
 */
object TurnEngine {

    private const val TAG = "BetterMaps"
    private val detector = TurnDetector()
    private var lastRaw: String? = null
    private var lastEventTime = 0L
    private var lastSpokeTime = 0L
    private var lastForeground = 0L

    private val SPACES = Pattern.compile("\\s+")
    private const val DEDUP_WINDOW_MS = 2_500L
    private const val MIN_SPEAK_GAP_MS = 6_000L

    /** Called when Maps enters foreground: fresh trip after long gap resets state. */
    fun onMapsForeground() {
        val now = System.currentTimeMillis()
        if (now - lastForeground > 10 * 60_000) detector.reset()
        lastForeground = now
    }

    fun onMapsText(rawText: String?) {
        onMapsText("maps", rawText)
    }

    fun onMapsText(source: String, rawText: String?) {
        if (rawText == null || rawText.length < 4) return
        val text = SPACES.matcher(rawText.trim()).replaceAll(" ")
        if (text.length < 4) return

        val now = System.currentTimeMillis()
        if (text == lastRaw && now - lastEventTime < DEDUP_WINDOW_MS) return
        lastRaw = text
        lastEventTime = now

        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "[$source] $text")

        when (detector.onInstruction(text)) {
            is TurnDetector.Event.Correct -> maybeSpeak(now, false) { FeedbackSpeaker.speakCorrect() }
            is TurnDetector.Event.Wrong -> maybeSpeak(now, true) { FeedbackSpeaker.speakWrong() }
            is TurnDetector.Event.Arrived -> maybeSpeak(now, true) { FeedbackSpeaker.speakArrival() }
            is TurnDetector.Event.None -> Unit
        }
    }

    private inline fun maybeSpeak(now: Long, force: Boolean, speak: () -> Unit) {
        if (!force && now - lastSpokeTime < MIN_SPEAK_GAP_MS) return
        lastSpokeTime = now
        speak()
    }

    fun reset() = detector.reset()
}

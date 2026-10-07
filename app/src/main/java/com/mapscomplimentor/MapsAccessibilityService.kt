package com.mapscomplimentor

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Two jobs, one service (low RAM):
 * 1. AUTO-START: TYPE_WINDOW_STATE_CHANGED with Maps package = user opened
 *    Google Maps → warm TTS + reset trip state. No UI is launched (Android 10+
 *    blocks background activity starts, and popping over navigation is unsafe).
 *    Detection is already live at this point via the notification listener.
 * 2. FALLBACK parsing of the on-screen nav card (throttled, node-budgeted).
 */
class MapsAccessibilityService : AccessibilityService() {

    companion object {
        const val MAPS_PACKAGE = "com.google.android.apps.maps"
        private const val THROTTLE_MS = 1500L
        private const val MAX_NODES = 40
    }

    private var lastForward = 0L
    private var visited = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        FeedbackSpeaker.init(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.packageName?.toString() != MAPS_PACKAGE) return
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // User opened / returned to Google Maps → auto-start running.
                FeedbackSpeaker.init(this)
                TurnEngine.onMapsForeground()
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> parseNavCard()
            else -> Unit
        }
    }

    override fun onInterrupt() = Unit

    private fun parseNavCard() {
        val now = System.currentTimeMillis()
        if (now - lastForward < THROTTLE_MS) return
        val root: AccessibilityNodeInfo = try {
            rootInActiveWindow ?: return
        } catch (_: Exception) { return }
        lastForward = now
        visited = 0
        var best: String? = null
        try {
            best = findNavText(root, 0, best)
        } catch (_: Exception) {}
        if (best != null) TurnEngine.onMapsText("accessibility", best)
    }

    /** Depth-first, single pass, keeps longest nav-like string. Recycles nodes. */
    private fun findNavText(node: AccessibilityNodeInfo, depth: Int, currentBest: String?): String? {
        if (depth > 8 || visited >= MAX_NODES) return currentBest
        visited++
        var best = currentBest
        try {
            val t = node.text?.toString()
            if (!t.isNullOrBlank() && t.length in 4..220 && looksNavish(t)) {
                if (best == null || t.length > best.length) best = t
            }
            val cd = node.contentDescription?.toString()
            if (!cd.isNullOrBlank() && cd.length in 4..220 && looksNavish(cd)) {
                if (best == null || cd.length > best.length) best = cd
            }
            for (i in 0 until node.childCount) {
                if (visited >= MAX_NODES) break
                val child = try { node.getChild(i) } catch (_: Exception) { null } ?: continue
                best = findNavText(child, depth + 1, best)
                try { child.recycle() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
        return best
    }

    private fun looksNavish(s: String): Boolean {
        val l = s.lowercase()
        return l.contains("turn") || l.contains("keep") || l.contains("exit") ||
                l.contains("merge") || l.contains("roundabout") || l.contains("continue") ||
                l.contains("rerouting") || l.contains("u-turn") || l.contains("arrived") ||
                l.contains("straight") || l.contains("onto") || l.contains("destination")
    }
}

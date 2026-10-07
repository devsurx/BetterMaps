package com.mapscomplimentor

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Fallback detector: reads the on-screen Google Maps navigation card.
 * Needed on devices where notification text is truncated.
 * Enable via: Settings > Accessibility > Maps Complimentor.
 */
class MapsAccessibilityService : AccessibilityService() {

    companion object {
        const val MAPS_PACKAGE = "com.google.android.apps.maps"
    }

    private var lastForward = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        FeedbackSpeaker.init(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.packageName?.toString() != MAPS_PACKAGE) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return

        val now = System.currentTimeMillis()
        if (now - lastForward < 1500) return // throttle

        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return
        val texts = ArrayList<String>()
        collectText(root, texts, 0)
        lastForward = now

        // Forward the most nav-like string (longest match wins)
        val best = texts
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.length in 4..220 }
            .filter { looksNavish(it) }
            .maxByOrNull { it.length }
        if (best != null) TurnEngine.onMapsText("accessibility", best)
    }

    override fun onInterrupt() = Unit

    private fun looksNavish(s: String): Boolean {
        val l = s.lowercase()
        return l.contains("turn") || l.contains("keep") || l.contains("exit") ||
                l.contains("merge") || l.contains("roundabout") || l.contains("continue") ||
                l.contains("head") || l.contains("destination") || l.contains("rerouting") ||
                l.contains("u-turn") || l.contains("u turn") || l.contains("arrived") ||
                l.contains("straight") || l.contains("onto") || l.contains("toward")
    }

    private fun collectText(node: AccessibilityNodeInfo, out: MutableList<String>, depth: Int) {
        if (depth > 12) return
        try {
            node.text?.toString()?.let { if (it.isNotBlank()) out.add(it) }
            node.contentDescription?.toString()?.let { if (it.isNotBlank()) out.add(it) }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let {
                    collectText(it, out, depth + 1)
                    try { it.recycle() } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }
}

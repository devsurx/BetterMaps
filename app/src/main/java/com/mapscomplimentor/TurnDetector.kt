package com.mapscomplimentor

/**
 * Pure logic, no Android dependencies so it is unit-testable.
 *
 * Strategy (no API key needed):
 * - Google Maps announces maneuvers sequentially.
 * - If we see Maneuver A, then later a DIFFERENT Maneuver B with no
 *   "rerouting" in between, Maneuver A was done correctly -> praise.
 * - If we see "Rerouting..." / "Make a U-turn" to get back on route -> wrong turn.
 */
class TurnDetector {

    sealed interface Event {
        data object None : Event
        data class Correct(val completedManeuver: String) : Event
        data class Wrong(val reason: String) : Event
        data class Arrived(val message: String) : Event
    }

    private var lastManeuverCore: String? = null
    private var lastManeuverFull: String? = null
    private var lastManeuverTime: Long = 0L

    fun reset() {
        lastManeuverCore = null
        lastManeuverFull = null
        lastManeuverTime = 0L
    }

    fun onInstruction(rawText: String?): Event {
        if (rawText.isNullOrBlank()) return Event.None
        val text = rawText.trim()
        val lower = text.lowercase()
        val now = System.currentTimeMillis()

        // 1. Arrival always counts as success
        if (lower.contains("you have arrived") ||
            lower.contains("you've arrived") ||
            lower.contains("arrived at your destination") ||
            lower.contains("destination is on")
        ) {
            val completed = lastManeuverFull
            reset()
            return Event.Arrived("You made it!")
        }

        // 2. Wrong-turn signals from Google Maps
        if (isReroute(lower)) {
            reset() // route was broken, start fresh after reroute
            return Event.Wrong(text)
        }

        // 3. Not a navigation instruction? Ignore.
        //    This filters out music notifications etc. that sometimes leak in.
        if (!looksLikeNavInstruction(lower)) return Event.None

        val core = maneuverCore(lower)

        // Same maneuver with updated distance ("in 500m" -> "in 200m" -> "now") -> just update, no praise yet
        if (core == lastManeuverCore) {
            lastManeuverFull = text
            lastManeuverTime = now
            return Event.None
        }

        // New maneuver core arrived.
        val previous = lastManeuverFull
        val previousTime = lastManeuverTime
        lastManeuverCore = core
        lastManeuverFull = text
        lastManeuverTime = now

        // If there was a previous maneuver and enough time passed for the user
        // to actually drive it (>4s, <10min), the previous one was done right.
        if (previous != null && now - previousTime > 4_000 && now - previousTime < 10 * 60_000) {
            // Only praise actual turns, not straight "continue/head" chains,
            // otherwise the app says "nice" every 10 seconds on a highway.
            if (wasTurnManeuver(previous.lowercase())) {
                return Event.Correct(previous)
            }
        }
        return Event.None
    }

    private fun isReroute(lower: String): Boolean {
        val markers = listOf(
            "rerouting",
            "recalculating",
            "re-calculating",
            "route updated",
            "finding new route",
            "make a u-turn",
            "make a legal u-turn",
            "make a u turn",
            "wrong way",
            "return to the route",
            "back on route",
            "missed your turn",
            "you missed"
        )
        return markers.any { lower.contains(it) }
    }

    private fun looksLikeNavInstruction(lower: String): Boolean {
        val keywords = listOf(
            "turn", "keep", "exit", "merge", "roundabout", "rotary",
            "continue", "head", "straight", "destination", "arrive",
            "u-turn", "u turn", "sharp", "slight", "take ", "onto",
            "toward", "towards", " in ", " m ", " ft ", " km ", " mi ",
            "now", "ahead"
        )
        return keywords.any { lower.contains(it) }
    }

    private fun wasTurnManeuver(lower: String): Boolean {
        // Straight cruising should NOT trigger praise, only real decisions.
        if (lower.contains("continue straight") ||
            lower.contains("continue onto") && !lower.contains("turn") ||
            lower.startsWith("head ") && !lower.contains("turn") ||
            lower.startsWith("go straight")
        ) {
            // Still praise occasionally? No - keep it meaningful.
            return false
        }
        return lower.contains("turn") ||
                lower.contains("keep") ||
                lower.contains("exit") ||
                lower.contains("merge") ||
                lower.contains("roundabout") ||
                lower.contains("rotary") ||
                lower.contains("u-turn") ||
                lower.contains("u turn") ||
                lower.contains("sharp") ||
                lower.contains("slight left") ||
                lower.contains("slight right")
    }

    /**
     * Strip distance countdown so "Turn left in 500m onto X"
     * and "Turn left in 200m onto X" map to the same core.
     */
    internal fun maneuverCore(lower: String): String {
        var s = lower
        // remove "in 500 m / 500 ft / 2 km / 1 mi" chunks
        s = s.replace(Regex("in\\s+\\d+[.,]?\\d*\\s*(m|ft|feet|km|mi|miles?)"), "")
        s = s.replace(Regex("\\d+[.,]?\\d*\\s*(m|ft|feet|km|mi|miles?)\\s*(ahead|away)?"), "")
        s = s.replace(Regex("\\s+"), " ").trim()
        return s
    }
}

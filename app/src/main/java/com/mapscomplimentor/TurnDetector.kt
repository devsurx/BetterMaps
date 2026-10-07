package com.mapscomplimentor

import java.util.regex.Pattern

/**
 * Pure logic, zero Android deps. Static keyword sets + precompiled
 * patterns: no per-call allocations → minimal GC / memory churn.
 */
class TurnDetector {

    sealed interface Event {
        data object None : Event
        data class Correct(val completedManeuver: String) : Event
        data class Wrong(val reason: String) : Event
        data class Arrived(val message: String) : Event
    }

    private var lastCore: String? = null
    private var lastFull: String? = null
    private var lastTime: Long = 0L

    fun reset() {
        lastCore = null
        lastFull = null
        lastTime = 0L
    }

    fun onInstruction(rawText: String?): Event {
        if (rawText.isNullOrBlank()) return Event.None
        val text = rawText.trim()
        if (text.length < 4) return Event.None
        val lower = text.lowercase()
        val now = System.currentTimeMillis()

        if (isArrival(lower)) {
            reset()
            return Event.Arrived("You made it!")
        }
        if (isReroute(lower)) {
            reset()
            return Event.Wrong(text)
        }
        if (!looksLikeNav(lower)) return Event.None

        val core = coreOf(lower)
        if (core == lastCore) {
            lastFull = text
            lastTime = now
            return Event.None
        }
        val prev = lastFull
        val prevTime = lastTime
        lastCore = core
        lastFull = text
        lastTime = now

        if (prev != null && now - prevTime > 4_000 && now - prevTime < 10 * 60_000) {
            if (wasTurn(prev.lowercase())) return Event.Correct(prev)
        }
        return Event.None
    }

    internal fun maneuverCore(lower: String): String = coreOf(lower)

    companion object {
        private val DIST_IN = Pattern.compile("in\\s+\\d+[.,]?\\d*\\s*(m|ft|feet|km|mi|miles?)")
        private val DIST_ANY = Pattern.compile("\\d+[.,]?\\d*\\s*(m|ft|feet|km|mi|miles?)\\s*(ahead|away)?")
        private val SPACES = Pattern.compile("\\s+")

        // HashSets: contains() without iterator garbage.
        private val REROUTE = hashSetOf(
            "rerouting", "recalculating", "route updated", "finding new route",
            "make a u-turn", "make a legal u-turn", "wrong way",
            "return to the route", "back on route", "missed your turn", "you missed"
        )
        private val NAV = hashSetOf(
            "turn", "keep", "exit", "merge", "roundabout", "continue",
            "straight", "destination", "arrive", "u-turn", "sharp",
            "slight", "onto", "toward", "ahead", "now"
        )
        private val ARRIVE = hashSetOf(
            "you have arrived", "you've arrived",
            "arrived at your destination", "destination is on"
        )

        private fun containsAny(hay: String, set: HashSet<String>): Boolean {
            for (s in set) if (hay.contains(s)) return true
            return false
        }

        private fun isReroute(l: String) = containsAny(l, REROUTE)
        private fun looksLikeNav(l: String) = containsAny(l, NAV)
        private fun isArrival(l: String) = containsAny(l, ARRIVE)

        private fun wasTurn(l: String): Boolean {
            if (l.contains("continue straight") || l.startsWith("go straight")) return false
            if (l.contains("continue onto") && !l.contains("turn")) return false
            if (l.startsWith("head ") && !l.contains("turn")) return false
            return l.contains("turn") || l.contains("keep") || l.contains("exit") ||
                    l.contains("merge") || l.contains("roundabout") || l.contains("rotary") ||
                    l.contains("u-turn") || l.contains("sharp") ||
                    l.contains("slight left") || l.contains("slight right")
        }

        private fun coreOf(lower: String): String {
            var s = DIST_IN.matcher(lower).replaceAll("")
            s = DIST_ANY.matcher(s).replaceAll("")
            return SPACES.matcher(s).replaceAll(" ").trim()
        }
    }
}

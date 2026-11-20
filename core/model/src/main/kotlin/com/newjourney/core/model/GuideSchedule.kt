package com.newjourney.core.model

/**
 * When the spoken guide plays in a session, in milliseconds since the session started.
 *
 * The opening part starts just after the start bell has sounded. The closing part is placed so it
 * finishes a moment before the end, right before the end bell. In between there is quiet.
 */
data class GuideSchedule(val introStartMs: Long, val outroStartMs: Long) {

    companion object {
        /** Lets the start bell sound first. */
        const val AFTER_BELL_MS = 4_500L
        const val WITHOUT_BELL_MS = 1_500L

        /** A quiet moment after the last words ("Namaste") before the end bell rings. */
        const val END_GAP_MS = 8_000L

        /** The least quiet time between the two parts; shorter sessions are not guided. */
        const val MIN_QUIET_MS = 30_000L

        /** The plan for a session of [totalMs], or null when it is too short for both parts. */
        fun plan(totalMs: Long, introMs: Long, outroMs: Long, startBell: Boolean): GuideSchedule? {
            val introStart = if (startBell) AFTER_BELL_MS else WITHOUT_BELL_MS
            val outroStart = totalMs - outroMs - END_GAP_MS
            if (introMs <= 0 || outroMs <= 0) return null
            if (outroStart < introStart + introMs + MIN_QUIET_MS) return null
            return GuideSchedule(introStart, outroStart)
        }
    }
}

package com.newjourney.core.model

/**
 * A saved meditation: a length with its bells and background sound, started with one tap.
 * Premium lengths still need Premium to start, exactly like the timer itself.
 */
data class Routine(
    val id: Long = 0,
    val name: String,
    val minutes: Int,
    val settings: TimerSettings,
) {
    fun isLocked(entitlement: Entitlement): Boolean =
        !entitlement.isPremium && TimerLimits.isPremiumDuration(minutes)

    companion object {
        /** Enough for a morning, evening and a few favourites, while the list stays easy to scan. */
        const val MAX_ROUTINES: Int = 10
        const val MAX_NAME_LENGTH: Int = 40

        /** Trims the name, folds repeated spaces and caps the length. Empty means "use the suggestion". */
        fun cleanName(raw: String): String =
            raw.trim().replace(Regex("\\s+"), " ").take(MAX_NAME_LENGTH).trim()
    }
}

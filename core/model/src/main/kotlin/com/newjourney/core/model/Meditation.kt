package com.newjourney.core.model

/** Free vs premium rules for the timer, kept in one place so they are easy to change. */
object TimerLimits {
    /** Durations shown as quick-start buttons, in minutes. */
    val quickStartMinutes: List<Int> = listOf(5, 10, 15, 20, 30, 45, 60)

    /** The longest session a free user can start. 15 minutes and longer is premium. */
    const val FREE_MAX_MINUTES: Int = 10

    const val DEFAULT_MINUTES: Int = 10

    fun isPremiumDuration(minutes: Int): Boolean = minutes > FREE_MAX_MINUTES
}

/** What the user currently has access to. Phase 4 fills this from verified Google Play purchases. */
data class Entitlement(val isPremium: Boolean = false)

enum class Mood { PEACEFUL, HAPPY, MOTIVATED, CALM, TIRED, STRESSED, ANXIOUS, SAD }

/** The moods offered on the home screen check-in, in display order. */
val HomeCheckInMoods: List<Mood> = listOf(Mood.PEACEFUL, Mood.HAPPY, Mood.STRESSED, Mood.SAD, Mood.TIRED)

enum class PartOfDay {
    MORNING, AFTERNOON, EVENING, NIGHT;

    companion object {
        fun fromHour(hour: Int): PartOfDay = when (hour) {
            in 5..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..21 -> EVENING
            else -> NIGHT
        }
    }
}

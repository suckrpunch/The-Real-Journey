package com.newjourney.core.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Background sound played during a timer session. All three are free. */
enum class BackgroundSound { SILENCE, RAIN, FOREST }

/** The user's timer choices, remembered between sessions. */
data class TimerSettings(
    val startBell: Boolean = true,
    val endBell: Boolean = true,
    val sound: BackgroundSound = BackgroundSound.SILENCE,
)

/** One meditation, saved when it ends. */
data class MeditationSession(
    val startedAt: Instant,
    val plannedSeconds: Int,
    val completedSeconds: Int,
    /** True when the timer ran to the end; false when the user ended early. */
    val completed: Boolean,
)

object DailyGoal {
    const val DEFAULT_MINUTES: Int = 10
}

/** A session must last at least this long to be saved. */
const val MIN_SAVED_SESSION_SECONDS: Int = 60

/** What the Home screen shows about this week. */
data class WeekProgress(
    val todayMinutes: Int,
    val goalMinutes: Int,
    /** Days of the current week (Monday to Sunday) with at least one saved session. */
    val daysMeditated: Set<DayOfWeek>,
    val today: DayOfWeek,
) {
    val goalReached: Boolean get() = todayMinutes >= goalMinutes
    val goalFraction: Float get() = (todayMinutes.toFloat() / goalMinutes).coerceIn(0f, 1f)

    companion object {
        /** Start of the current week (Monday 00:00) in [zone], used to load this week's sessions. */
        fun weekStart(today: LocalDate, zone: ZoneId): Instant =
            today.with(DayOfWeek.MONDAY).atStartOfDay(zone).toInstant()

        fun from(
            sessions: List<MeditationSession>,
            today: LocalDate,
            zone: ZoneId,
            goalMinutes: Int = DailyGoal.DEFAULT_MINUTES,
        ): WeekProgress {
            val monday = today.with(DayOfWeek.MONDAY)
            val thisWeek = sessions.filter {
                val day = it.startedAt.atZone(zone).toLocalDate()
                !day.isBefore(monday) && !day.isAfter(today)
            }
            val todaySeconds = thisWeek
                .filter { it.startedAt.atZone(zone).toLocalDate() == today }
                .sumOf { it.completedSeconds }
            return WeekProgress(
                todayMinutes = todaySeconds / 60,
                goalMinutes = goalMinutes,
                daysMeditated = thisWeek.map { it.startedAt.atZone(zone).dayOfWeek }.toSet(),
                today = today.dayOfWeek,
            )
        }
    }
}

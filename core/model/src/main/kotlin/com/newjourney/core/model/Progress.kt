package com.newjourney.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Daily goal choices offered in Profile, in minutes. */
val DailyGoalChoices: List<Int> = listOf(5, 10, 15, 20, 30)

/**
 * Personal milestones. Every one counts things that only ever go up (sessions, minutes,
 * days meditated in total), so a missed day never takes anything away.
 */
enum class Milestone(val kind: Kind, val threshold: Int) {
    FIRST_SESSION(Kind.SESSIONS, 1),
    DAYS_3(Kind.DAYS, 3),
    FIRST_HOUR(Kind.MINUTES, 60),
    DAYS_7(Kind.DAYS, 7),
    SESSIONS_10(Kind.SESSIONS, 10),
    HOURS_5(Kind.MINUTES, 300),
    SESSIONS_25(Kind.SESSIONS, 25),
    DAYS_30(Kind.DAYS, 30),
    HOURS_10(Kind.MINUTES, 600),
    SESSIONS_50(Kind.SESSIONS, 50),
    HOURS_25(Kind.MINUTES, 1500),
    DAYS_100(Kind.DAYS, 100),
    SESSIONS_100(Kind.SESSIONS, 100),
    HOURS_50(Kind.MINUTES, 3000),
    DAYS_365(Kind.DAYS, 365),
    ;

    enum class Kind { SESSIONS, MINUTES, DAYS }
}

data class MilestoneProgress(
    val milestone: Milestone,
    /** When it was reached: the start of the session that reached it. null = not yet. */
    val achievedAt: Instant?,
    /** How far along (sessions, minutes or days) towards [Milestone.threshold]. */
    val current: Int,
)

data class DayTotal(val date: LocalDate, val minutes: Int)

/** Everything the progress screens show, worked out from the saved sessions. */
data class ProgressStats(
    val totalMinutes: Int,
    val sessionCount: Int,
    val daysMeditated: Int,
    /** Days in a row up to today (or up to yesterday, if today has no session yet). 0 if neither. */
    val currentRun: Int,
    val longestRun: Int,
    /** Minutes per day for the last 7 days, oldest first, ending today. */
    val lastSevenDays: List<DayTotal>,
    /** Minutes for every day that has any. */
    val minutesByDay: Map<LocalDate, Int>,
    val milestones: List<MilestoneProgress>,
) {
    val achieved: List<MilestoneProgress> get() = milestones.filter { it.achievedAt != null }

    /** The next milestone to reach, nearest first by how far along it is. */
    val next: MilestoneProgress?
        get() = milestones.filter { it.achievedAt == null }
            .maxByOrNull { it.current.toFloat() / it.milestone.threshold }

    /** Milestones reached by the session that started at [sessionStart]. */
    fun reachedBy(sessionStart: Instant): List<Milestone> =
        milestones.filter { it.achievedAt == sessionStart }.map { it.milestone }

    companion object {
        fun from(sessions: List<MeditationSession>, today: LocalDate, zone: ZoneId): ProgressStats {
            val sorted = sessions.sortedBy { it.startedAt }
            val minutesByDay = sorted
                .groupBy { it.startedAt.atZone(zone).toLocalDate() }
                .mapValues { (_, list) -> list.sumOf { it.completedSeconds } / 60 }
            val days = minutesByDay.keys

            // Walk through the sessions in order to find when each milestone was reached.
            val achieved = mutableMapOf<Milestone, Instant>()
            var count = 0
            var seconds = 0L
            val seenDays = mutableSetOf<LocalDate>()
            for (session in sorted) {
                count++
                seconds += session.completedSeconds
                seenDays += session.startedAt.atZone(zone).toLocalDate()
                for (milestone in Milestone.entries) {
                    if (milestone in achieved) continue
                    if (valueOf(milestone.kind, count, seconds, seenDays.size) >= milestone.threshold) {
                        achieved[milestone] = session.startedAt
                    }
                }
            }
            val totalMinutes = (seconds / 60).toInt()

            return ProgressStats(
                totalMinutes = totalMinutes,
                sessionCount = count,
                daysMeditated = days.size,
                currentRun = currentRun(days, today),
                longestRun = longestRun(days),
                lastSevenDays = (6 downTo 0).map { back ->
                    val date = today.minusDays(back.toLong())
                    DayTotal(date, minutesByDay[date] ?: 0)
                },
                minutesByDay = minutesByDay,
                milestones = Milestone.entries.map { milestone ->
                    MilestoneProgress(
                        milestone = milestone,
                        achievedAt = achieved[milestone],
                        current = valueOf(milestone.kind, count, seconds, days.size).coerceAtMost(milestone.threshold),
                    )
                },
            )
        }

        private fun valueOf(kind: Milestone.Kind, sessions: Int, seconds: Long, days: Int): Int = when (kind) {
            Milestone.Kind.SESSIONS -> sessions
            Milestone.Kind.MINUTES -> (seconds / 60).toInt()
            Milestone.Kind.DAYS -> days
        }

        internal fun currentRun(days: Set<LocalDate>, today: LocalDate): Int {
            var day = if (today in days) today else today.minusDays(1)
            var run = 0
            while (day in days) {
                run++
                day = day.minusDays(1)
            }
            return run
        }

        internal fun longestRun(days: Set<LocalDate>): Int {
            var best = 0
            for (day in days) {
                if (day.minusDays(1) in days) continue // only count from the start of each run
                var length = 0
                var d = day
                while (d in days) {
                    length++
                    d = d.plusDays(1)
                }
                best = maxOf(best, length)
            }
            return best
        }
    }
}

/** The user's reminder choices. Everything is off until the user turns it on. */
data class ReminderSettings(
    val dailyOn: Boolean = false,
    val dailyMinuteOfDay: Int = 8 * 60 + 30,
    /** Skip the daily reminder when the user has already meditated that day. */
    val dailySkipIfDone: Boolean = true,
    val eveningOn: Boolean = false,
    val eveningMinuteOfDay: Int = 21 * 60,
    /** A short summary on Sunday evening. Not sent in a week without sessions. */
    val weeklyOn: Boolean = false,
    /** One kind note after three days without a session; not repeated until the next session. */
    val encourageOn: Boolean = false,
) {
    val anyOn: Boolean get() = dailyOn || eveningOn || weeklyOn || encourageOn
}

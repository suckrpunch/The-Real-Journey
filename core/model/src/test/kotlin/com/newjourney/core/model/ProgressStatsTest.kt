package com.newjourney.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ProgressStatsTest {
    private val zone = ZoneId.of("Europe/Amsterdam")
    private val today = LocalDate.of(2026, 11, 6)

    private fun session(daysAgo: Long, minutes: Int, hour: Int = 9) = MeditationSession(
        startedAt = today.minusDays(daysAgo).atTime(hour, 0).atZone(zone).toInstant(),
        plannedSeconds = minutes * 60,
        completedSeconds = minutes * 60,
        completed = true,
    )

    @Test
    fun totalsAndRuns() {
        val sessions = listOf(
            session(9, 10), session(8, 10), session(7, 10), // a 3-day run
            session(1, 20), session(0, 5), session(0, 15, hour = 21), // yesterday and today
        )
        val stats = ProgressStats.from(sessions, today, zone)
        assertEquals(70, stats.totalMinutes)
        assertEquals(6, stats.sessionCount)
        assertEquals(5, stats.daysMeditated)
        assertEquals(2, stats.currentRun)
        assertEquals(3, stats.longestRun)
        assertEquals(20, stats.lastSevenDays.last().minutes)
        assertEquals(today, stats.lastSevenDays.last().date)
    }

    @Test
    fun runStaysWhenTodayHasNoSessionYet() {
        val stats = ProgressStats.from(listOf(session(2, 10), session(1, 10)), today, zone)
        assertEquals(2, stats.currentRun)
    }

    @Test
    fun runIsZeroAfterABreakButNothingIsLost() {
        val stats = ProgressStats.from(listOf(session(5, 10), session(4, 10)), today, zone)
        assertEquals(0, stats.currentRun)
        assertEquals(2, stats.longestRun)
        assertEquals(2, stats.daysMeditated)
    }

    @Test
    fun milestonesRecordTheSessionThatReachedThem() {
        val sessions = listOf(session(3, 30), session(2, 30), session(1, 5))
        val stats = ProgressStats.from(sessions, today, zone)
        val first = stats.milestones.first { it.milestone == Milestone.FIRST_SESSION }
        assertEquals(sessions[0].startedAt, first.achievedAt)
        // 30 + 30 minutes reaches the first hour on the second session.
        assertEquals(listOf(Milestone.FIRST_HOUR), stats.reachedBy(sessions[1].startedAt))
        assertEquals(listOf(Milestone.DAYS_3), stats.reachedBy(sessions[2].startedAt))
        assertNull(stats.milestones.first { it.milestone == Milestone.DAYS_7 }.achievedAt)
        assertTrue(stats.next != null)
    }

    @Test
    fun emptyHistory() {
        val stats = ProgressStats.from(emptyList(), today, zone)
        assertEquals(0, stats.currentRun)
        assertEquals(0, stats.longestRun)
        assertTrue(stats.achieved.isEmpty())
        assertEquals(7, stats.lastSevenDays.size)
    }
}

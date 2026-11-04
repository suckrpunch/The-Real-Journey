package com.newjourney.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId

class WeekProgressTest {
    private val zone = ZoneId.of("Europe/Amsterdam")
    private val wednesday = LocalDate.of(2026, 11, 4)

    private fun session(date: LocalDate, hour: Int, seconds: Int) = MeditationSession(
        startedAt = date.atTime(hour, 0).atZone(zone).toInstant(),
        plannedSeconds = seconds,
        completedSeconds = seconds,
        completed = true,
    )

    @Test
    fun countsTodayMinutesAndDaysThisWeekOnly() {
        val sessions = listOf(
            session(wednesday, 8, 600),
            session(wednesday, 20, 330),
            session(wednesday.minusDays(2), 9, 300), // Monday
            session(wednesday.minusDays(3), 9, 300), // last Sunday: not this week
        )
        val progress = WeekProgress.from(sessions, wednesday, zone)
        assertEquals(15, progress.todayMinutes)
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), progress.daysMeditated)
        assertTrue(progress.goalReached)
    }
}

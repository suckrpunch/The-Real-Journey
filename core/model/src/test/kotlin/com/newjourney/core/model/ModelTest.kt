package com.newjourney.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ModelTest {

    @Test
    fun dailyQuoteChangesEachDayAndRepeatsAfterFullCycle() {
        val day1 = LocalDate.of(2026, 11, 4)
        val first = DailyQuote.pick(SampleQuotes, day1)
        val next = DailyQuote.pick(SampleQuotes, day1.plusDays(1))
        val cycled = DailyQuote.pick(SampleQuotes, day1.plusDays(SampleQuotes.size.toLong()))
        assertTrue(first != next)
        assertEquals(first, cycled)
    }

    @Test
    fun dailyQuoteWorksForDatesBeforeEpoch() {
        val quote = DailyQuote.pick(SampleQuotes, LocalDate.of(2025, 6, 1))
        assertTrue(quote != null)
    }

    @Test
    fun scheduledThemeCrossesMidnight() {
        val pref = ThemePreference(ThemeMode.SCHEDULED, nightStartHour = 20, nightEndHour = 7)
        assertTrue(pref.useNightTheme(23, systemIsDark = false))
        assertTrue(pref.useNightTheme(3, systemIsDark = false))
        assertFalse(pref.useNightTheme(12, systemIsDark = true))
    }

    @Test
    fun freeTimerStopsAtTenMinutes() {
        assertFalse(TimerLimits.isPremiumDuration(10))
        assertTrue(TimerLimits.isPremiumDuration(15))
    }
}

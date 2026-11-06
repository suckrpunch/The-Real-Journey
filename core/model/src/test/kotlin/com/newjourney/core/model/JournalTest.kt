package com.newjourney.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

class JournalTest {

    @Test
    fun november2026StartsOnSundayColumn() {
        // 1 November 2026 is a Sunday: six empty cells (Mon–Sat), then the 1st.
        val cells = calendarCells(YearMonth.of(2026, 11))
        assertEquals(6, cells.takeWhile { it == null }.size)
        assertEquals(LocalDate.of(2026, 11, 1), cells[6])
        assertEquals(0, cells.size % 7)
        assertEquals(30, cells.count { it != null })
    }

    @Test
    fun monthStartingOnMondayHasNoLeadingCells() {
        val cells = calendarCells(YearMonth.of(2026, 6)) // 1 June 2026 is a Monday
        assertEquals(LocalDate.of(2026, 6, 1), cells.first())
        assertEquals(35, cells.size) // 30 days + 5 empty cells to finish the last week
        assertNull(cells.last())
    }

    @Test
    fun entryWithOnlyAMoodIsNotEmpty() {
        assertTrue(JournalEntry(createdAt = Instant.EPOCH).isEmpty)
        assertTrue(!JournalEntry(createdAt = Instant.EPOCH, mood = Mood.CALM).isEmpty)
    }
}

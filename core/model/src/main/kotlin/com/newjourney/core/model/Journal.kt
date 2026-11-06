package com.newjourney.core.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** One private journal entry. Stored encrypted on the phone. */
data class JournalEntry(
    /** 0 for an entry that has not been saved yet. */
    val id: Long = 0,
    val createdAt: Instant,
    val updatedAt: Instant = createdAt,
    /** How the writer feels now. */
    val mood: Mood? = null,
    /** How they felt before meditating, when the entry follows a session. */
    val moodBefore: Mood? = null,
    val text: String = "",
    /** Length of the session this entry follows, if any. */
    val sessionMinutes: Int? = null,
) {
    val isEmpty: Boolean get() = text.isBlank() && mood == null && moodBefore == null
}

/** Every mood, in the order the journal offers them. Calm and pleasant first, harder ones after. */
val JournalMoods: List<Mood> = listOf(
    Mood.PEACEFUL, Mood.CALM, Mood.HAPPY, Mood.MOTIVATED,
    Mood.TIRED, Mood.STRESSED, Mood.ANXIOUS, Mood.SAD,
)

/** The days to draw for a month calendar, Monday first. null = an empty cell before the 1st or after the last day. */
fun calendarCells(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
    val cells = List(leading) { null } + days
    val trailing = (7 - cells.size % 7) % 7
    return cells + List(trailing) { null }
}

package com.newjourney.core.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class Quote(
    val id: String,
    val text: String,
    val author: String?,
    val theme: QuoteTheme,
)

enum class QuoteTheme { MINDFULNESS, SELF_LOVE, INNER_PEACE, AWARENESS, GROWTH, SPIRITUALITY }

/**
 * Picks the quote of the day. Everyone sees the same quote on the same date, it works offline,
 * and quotes added later simply join the rotation.
 */
object DailyQuote {
    private val EPOCH: LocalDate = LocalDate.of(2026, 1, 1)

    fun pick(quotes: List<Quote>, date: LocalDate): Quote? {
        if (quotes.isEmpty()) return null
        val sorted = quotes.sortedBy { it.id }
        val days = ChronoUnit.DAYS.between(EPOCH, date)
        val index = Math.floorMod(days, sorted.size.toLong()).toInt()
        return sorted[index]
    }
}

/**
 * Placeholder quotes written for development. Replace them with your own collection;
 * in Phase 3 quotes move to the online catalog so you can manage them without an app update.
 */
val SampleQuotes: List<Quote> = listOf(
    Quote("q001", "Peace is not something you find. It is something you return to.", null, QuoteTheme.INNER_PEACE),
    Quote("q002", "Every breath is a small beginning.", null, QuoteTheme.MINDFULNESS),
    Quote("q003", "Be as patient with yourself as you would be with a friend.", null, QuoteTheme.SELF_LOVE),
    Quote("q004", "Notice what is here before you wish it were different.", null, QuoteTheme.AWARENESS),
    Quote("q005", "Growth is quiet. Trust the roots you cannot see.", null, QuoteTheme.GROWTH),
    Quote("q006", "Stillness is where you meet yourself.", null, QuoteTheme.SPIRITUALITY),
    Quote("q007", "You do not have to do this perfectly. You only have to show up.", null, QuoteTheme.SELF_LOVE),
)

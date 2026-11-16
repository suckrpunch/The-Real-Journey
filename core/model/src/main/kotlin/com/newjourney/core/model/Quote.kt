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

/** The same quotes in the app's other languages, by id. Missing ones stay in English. */
private val QuoteTranslations: Map<String, Map<String, String>> = mapOf(
    "nl" to mapOf(
        "q001" to "Rust is niet iets wat je vindt. Het is iets waar je naar terugkeert.",
        "q002" to "Elke ademhaling is een klein begin.",
        "q003" to "Wees net zo geduldig met jezelf als met een vriend.",
        "q004" to "Merk op wat er is, voordat je wenst dat het anders was.",
        "q005" to "Groei is stil. Vertrouw op de wortels die je niet ziet.",
        "q006" to "In de stilte ontmoet je jezelf.",
        "q007" to "Je hoeft dit niet perfect te doen. Je hoeft er alleen te zijn.",
    ),
    "de" to mapOf(
        "q001" to "Frieden ist nichts, was man findet. Es ist etwas, zu dem man zurückkehrt.",
        "q002" to "Jeder Atemzug ist ein kleiner Anfang.",
        "q003" to "Sei so geduldig mit dir, wie du es mit einem Freund wärst.",
        "q004" to "Nimm wahr, was da ist, bevor du dir wünschst, es wäre anders.",
        "q005" to "Wachstum ist leise. Vertraue den Wurzeln, die du nicht siehst.",
        "q006" to "In der Stille begegnest du dir selbst.",
        "q007" to "Du musst es nicht perfekt machen. Du musst nur da sein.",
    ),
    "fr" to mapOf(
        "q001" to "La paix n'est pas quelque chose que l'on trouve. C'est un lieu où l'on revient.",
        "q002" to "Chaque respiration est un petit commencement.",
        "q003" to "Soyez aussi patient avec vous-même qu'avec un ami.",
        "q004" to "Remarquez ce qui est là avant de souhaiter que ce soit différent.",
        "q005" to "La croissance est silencieuse. Faites confiance aux racines que vous ne voyez pas.",
        "q006" to "C'est dans le calme que l'on se rencontre.",
        "q007" to "Inutile d'être parfait. Il suffit d'être là.",
    ),
    "es" to mapOf(
        "q001" to "La paz no es algo que se encuentra. Es algo a lo que se vuelve.",
        "q002" to "Cada respiración es un pequeño comienzo.",
        "q003" to "Ten contigo la misma paciencia que tendrías con un amigo.",
        "q004" to "Observa lo que hay antes de desear que fuera distinto.",
        "q005" to "El crecimiento es silencioso. Confía en las raíces que no ves.",
        "q006" to "En la quietud te encuentras contigo.",
        "q007" to "No tienes que hacerlo perfecto. Solo tienes que estar.",
    ),
)

/** [SampleQuotes] in [language] (a tag such as "nl"); same ids, so saved quotes stay saved. */
fun sampleQuotes(language: String): List<Quote> {
    val texts = QuoteTranslations[language] ?: return SampleQuotes
    return SampleQuotes.map { quote -> texts[quote.id]?.let { quote.copy(text = it) } ?: quote }
}

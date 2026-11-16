package com.newjourney.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LanguageContentTest {

    @Test
    fun everyLanguageHasEveryQuoteWithTheSameIds() {
        for (language in listOf("nl", "de", "fr", "es")) {
            val quotes = sampleQuotes(language)
            assertEquals(SampleQuotes.map { it.id }, quotes.map { it.id })
            quotes.zip(SampleQuotes).forEach { (translated, english) -> assertNotEquals(english.text, translated.text) }
        }
    }

    @Test
    fun unknownLanguageKeepsEnglish() {
        assertEquals(SampleQuotes, sampleQuotes("it"))
    }

    @Test
    fun timerDefaultsToTheClassicBellAndSilence() {
        val settings = TimerSettings()
        assertEquals(BellSound.SINGING_BOWL, settings.bell)
        assertEquals(BackgroundSound.SILENCE, settings.sound)
    }
}

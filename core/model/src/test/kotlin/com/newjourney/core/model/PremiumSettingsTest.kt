package com.newjourney.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumSettingsTest {

    private val everything = TimerSettings(
        startBell = true,
        endBell = false,
        sound = BackgroundSound.BEACH,
        bell = BellSound.TIBETAN,
        guide = VoiceGuide.FEMALE,
    )

    @Test
    fun premiumMembersHearEverythingTheyChose() {
        assertEquals(everything, everything.forPremium(isPremium = true))
    }

    @Test
    fun withoutPremiumTheChoicesFallBackToFreeOnes() {
        val free = everything.forPremium(isPremium = false)
        assertEquals(BellSound.SINGING_BOWL, free.bell)
        assertEquals(BackgroundSound.SILENCE, free.sound)
        assertEquals(VoiceGuide.NONE, free.guide)
        // Everything that is free stays exactly as chosen.
        assertEquals(everything.startBell, free.startBell)
        assertEquals(everything.endBell, free.endBell)
    }

    @Test
    fun rainAndTheClassicBellStayFree() {
        val settings = TimerSettings(sound = BackgroundSound.RAIN, bell = BellSound.SINGING_BOWL)
        assertEquals(settings, settings.forPremium(isPremium = false))
    }

    @Test
    fun onlyTheAgreedChoicesArePremium() {
        assertEquals(listOf(BackgroundSound.FOREST, BackgroundSound.BEACH), BackgroundSound.entries.filter { it.isPremium })
        assertEquals(listOf(BellSound.TIBETAN), BellSound.entries.filter { it.isPremium })
        assertEquals(listOf(VoiceGuide.FEMALE, VoiceGuide.MALE), VoiceGuide.entries.filter { it.isPremium })
    }
}

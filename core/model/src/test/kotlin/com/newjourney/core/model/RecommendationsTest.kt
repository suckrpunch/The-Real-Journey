package com.newjourney.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationsTest {
    private fun track(id: String, category: String, sub: String, tier: TrackTier = TrackTier.FREE) = Track(
        id = id, title = id, subtitle = null, description = null, categoryId = category, subcategoryId = sub,
        durationMs = 60_000, audioUri = "asset:///$id", tier = tier, loop = null,
    )

    private val catalog = Catalog(
        categories = emptyList(),
        tracks = listOf(
            track("sleep-premium", "sleep", "deep-sleep", TrackTier.PREMIUM),
            track("sleep-free", "sleep", "deep-sleep"),
            track("calm-free", "relaxation", "inner-peace"),
            track("stress-premium", "relaxation", "stress", TrackTier.PREMIUM),
            track("focus-free", "focus", "concentration"),
            track("chakra-free", "chakras", "root"),
        ),
    )

    @Test
    fun freeTracksFirstForFreeListeners() {
        assertEquals(listOf("sleep-free"), Recommendations.pick(catalog, setOf(Goal.SLEEP), isPremium = false, count = 1).map { it.id })
    }

    @Test
    fun premiumListenersGetPreferredSubcollectionFirst() {
        assertEquals("stress-premium", Recommendations.pick(catalog, setOf(Goal.STRESS), isPremium = true, count = 1).single().id)
    }

    @Test
    fun takesTurnsBetweenGoalsWithoutDuplicates() {
        val ids = Recommendations.pick(catalog, setOf(Goal.SLEEP, Goal.FOCUS, Goal.PEACE), isPremium = false).map { it.id }
        assertEquals(listOf("sleep-free", "calm-free", "focus-free"), ids)
    }

    @Test
    fun noGoalsUsesDefaultMix() {
        val ids = Recommendations.pick(catalog, emptySet(), isPremium = false).map { it.id }
        assertEquals(3, ids.size)
        assertTrue("chakra-free" in ids)
    }

    @Test
    fun firstSessionIsGentleForOlderOrUnknownAge() {
        assertEquals(5, Recommendations.firstSessionMinutes(AgeRange.AGE_75_PLUS))
        assertEquals(5, Recommendations.firstSessionMinutes(null))
        assertEquals(10, Recommendations.firstSessionMinutes(AgeRange.AGE_30_44))
    }

    @Test
    fun cleanNameKeepsItShort() {
        assertEquals("Romario", UserProfile.cleanName("  Romario  "))
        assertEquals(null, UserProfile(name = "   ").displayName)
    }
}

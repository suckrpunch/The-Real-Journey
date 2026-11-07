package com.newjourney.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class JourneyTest {
    private fun sessions(count: Int) = (1..count).map { i ->
        MeditationSession(Instant.ofEpochSecond(1_800_000_000L + i * 86_400L), 600, 600, true)
    }

    @Test
    fun noSessionsMeansNoStageYet() {
        val journey = JourneyProgress.from(emptyList())
        assertNull(journey.current)
        assertEquals(JourneyStage.AWAKENING, journey.next)
        assertEquals(0f, journey.towardsNext)
    }

    @Test
    fun stagesArriveOnTheExactSession() {
        val list = sessions(7)
        val journey = JourneyProgress.from(list.shuffled())
        assertEquals(JourneyStage.FOUNDATION, journey.current)
        assertEquals(list[0].startedAt, journey.stages[0].arrivedAt)
        assertEquals(listOf(JourneyStage.FOUNDATION), journey.reachedBy(list[6].startedAt))
        assertEquals(JourneyStage.INNER_BALANCE, journey.next)
        assertEquals(0f, journey.towardsNext)
    }

    @Test
    fun progressTowardsNextStage() {
        val journey = JourneyProgress.from(sessions(12))
        // From 7 to 20: 5 of 13 done.
        assertEquals(5f / 13f, journey.towardsNext, 0.0001f)
    }

    @Test
    fun premiumOnlyGatesReading() {
        assertTrue(JourneyStage.INNER_BALANCE.canRead(isPremium = false))
        assertFalse(JourneyStage.PATH_OF_AWARENESS.canRead(isPremium = false))
        assertTrue(JourneyStage.PATH_OF_AWARENESS.canRead(isPremium = true))
    }

    @Test
    fun allStagesReachedAfter200() {
        val journey = JourneyProgress.from(sessions(250))
        assertEquals(JourneyStage.INNER_SANCTUARY, journey.current)
        assertNull(journey.next)
        assertEquals(1f, journey.towardsNext)
    }
}

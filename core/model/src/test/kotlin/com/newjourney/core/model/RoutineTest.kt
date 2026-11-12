package com.newjourney.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineTest {
    private fun routine(minutes: Int) = Routine(name = "Morning", minutes = minutes, settings = TimerSettings())

    @Test
    fun cleanNameTrimsAndFoldsSpaces() {
        assertEquals("Morning calm", Routine.cleanName("  Morning    calm \n"))
    }

    @Test
    fun cleanNameCapsLength() {
        assertEquals(Routine.MAX_NAME_LENGTH, Routine.cleanName("a".repeat(100)).length)
    }

    @Test
    fun premiumLengthIsLockedForFreeOnly() {
        assertTrue(routine(20).isLocked(Entitlement(isPremium = false)))
        assertFalse(routine(20).isLocked(Entitlement(isPremium = true)))
        assertFalse(routine(10).isLocked(Entitlement(isPremium = false)))
    }
}

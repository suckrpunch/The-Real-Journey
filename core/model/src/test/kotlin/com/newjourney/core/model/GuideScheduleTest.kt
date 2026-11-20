package com.newjourney.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideScheduleTest {

    private val intro = 111_060L
    private val outro = 55_180L

    @Test
    fun closingWordsEndJustBeforeTheEnd() {
        val plan = GuideSchedule.plan(totalMs = 10 * 60_000L, introMs = intro, outroMs = outro, startBell = true)!!
        assertEquals(GuideSchedule.AFTER_BELL_MS, plan.introStartMs)
        assertEquals(10 * 60_000L - GuideSchedule.END_GAP_MS, plan.outroStartMs + outro)
    }

    @Test
    fun withoutAStartBellTheVoiceBeginsSooner() {
        val plan = GuideSchedule.plan(5 * 60_000L, intro, outro, startBell = false)!!
        assertEquals(GuideSchedule.WITHOUT_BELL_MS, plan.introStartMs)
    }

    @Test
    fun fiveMinutesIsLongEnoughForBothParts() {
        val plan = GuideSchedule.plan(5 * 60_000L, intro, outro, startBell = true)!!
        assertTrue(plan.outroStartMs - (plan.introStartMs + intro) >= GuideSchedule.MIN_QUIET_MS)
    }

    @Test
    fun tooShortASessionIsNotGuided() {
        assertNull(GuideSchedule.plan(2 * 60_000L, intro, outro, startBell = true))
    }

    @Test
    fun missingRecordingsAreNotGuided() {
        assertNull(GuideSchedule.plan(10 * 60_000L, 0, outro, startBell = true))
    }

    @Test
    fun guideIsOffByDefault() {
        assertEquals(VoiceGuide.NONE, TimerSettings().guide)
    }
}

package com.newjourney.core.catalog

import com.newjourney.core.model.JourneyStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class JourneyContentTest {
    /** Every stage needs its words, whatever the owner edits in journey.json. */
    @Test
    fun everyStageHasContent() {
        val content = JourneyContentParser.parse(File("src/main/assets/journey/journey.json").readText())
        assertEquals(JourneyStage.entries.toSet(), content.keys)
        content.values.forEach {
            assertTrue("${it.stage}: name", it.name.isNotBlank())
            assertTrue("${it.stage}: reflection", it.reflection.isNotBlank())
            assertTrue("${it.stage}: colour", it.color.matches(Regex("#[0-9A-Fa-f]{6}")))
        }
    }
}

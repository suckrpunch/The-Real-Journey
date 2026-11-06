package com.newjourney.core.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogParserTest {

    /** The real catalog bundled with the app must always parse and be consistent. */
    @Test
    fun bundledCatalogIsValid() {
        val text = File("src/main/assets/catalog/catalog.json").readText()
        val catalog = CatalogParser.parse(text)
        assertEquals(6, catalog.categories.size)
        assertEquals(7, catalog.tracksInCategory("chakras").size)

        val subcategoryIds = catalog.categories.flatMap { c -> c.subcategories.map { it.id } }.toSet()
        catalog.tracks.forEach { track ->
            assertNotNull("${track.id}: unknown category", catalog.category(track.categoryId))
            assertTrue("${track.id}: unknown subcategory", track.subcategoryId in subcategoryIds)
            val file = File("src/main/assets/" + track.audioUri.removePrefix("asset:///"))
            assertTrue("${track.id}: missing audio file ${file.path}", file.exists())
            val loop = track.loop
            assertNotNull("${track.id}: no loop points", loop)
            assertTrue("${track.id}: loop out after end", loop!!.outMs <= track.durationMs)
        }
        assertEquals(catalog.tracks.size, catalog.tracks.map { it.id }.toSet().size)
    }

    @Test
    fun onlineAddressesAreKept() {
        assertEquals("https://example.com/a.m4a", CatalogParser.toUri("https://example.com/a.m4a"))
        assertEquals("asset:///audio/a.m4a", CatalogParser.toUri("audio/a.m4a"))
    }

    @Test
    fun invalidLoopIsDropped() {
        val text = """{"categories":[],"tracks":[{"id":"t","title":"T","category":"c","subcategory":"s",
            "durationMs":1000,"audio":"a.m4a","loop":{"inMs":500,"outMs":600,"crossfadeMs":200}}]}"""
        assertNull(CatalogParser.parse(text).tracks.single().loop)
    }
}

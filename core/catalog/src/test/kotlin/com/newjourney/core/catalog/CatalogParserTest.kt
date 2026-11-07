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

    /** The online catalog kept in the repository (content/catalog/catalog.json) must parse and be consistent. */
    @Test
    fun onlineCatalogIsValid() {
        val text = File("../../content/catalog/catalog.json").readText()
        val catalog = CatalogParser.parse(text, CatalogParser.onlineResolver("example-bucket"))
        assertEquals(27, catalog.tracks.size)
        val subcategoryIds = catalog.categories.flatMap { c -> c.subcategories.map { it.id } }.toSet()
        catalog.tracks.forEach { track ->
            assertTrue("${track.id}: unknown subcategory", track.subcategoryId in subcategoryIds)
            assertNotNull("${track.id}: no loop points", track.loop)
            if (track.audioUri.startsWith("asset:///")) {
                val file = File("src/main/assets/" + track.audioUri.removePrefix("asset:///"))
                assertTrue("${track.id}: missing bundled file ${file.path}", file.exists())
            } else {
                assertTrue(track.audioUri.startsWith("https://firebasestorage.googleapis.com/v0/b/example-bucket/o/audio%2F"))
            }
        }
        assertEquals(catalog.tracks.size, catalog.tracks.map { it.id }.toSet().size)
    }

    @Test
    fun storageAddressIsEncoded() {
        assertEquals(
            "https://firebasestorage.googleapis.com/v0/b/b.firebasestorage.app/o/audio%2Fsleep%2Fdeep-sleep-drifter.m4a?alt=media",
            CatalogParser.storageUrl("b.firebasestorage.app", "audio/sleep/deep-sleep-drifter.m4a"),
        )
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

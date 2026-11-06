package com.newjourney.core.model

/**
 * The music library, built from catalog.json (core/catalog/src/main/assets/catalog/).
 * Nothing about categories or tracks is hard-coded in the screens.
 */
data class Catalog(
    val categories: List<Category>,
    val tracks: List<Track>,
) {
    private val byId: Map<String, Track> = tracks.associateBy { it.id }

    fun track(id: String): Track? = byId[id]

    fun tracksIn(subcategoryId: String): List<Track> = tracks.filter { it.subcategoryId == subcategoryId }

    fun tracksInCategory(categoryId: String): List<Track> = tracks.filter { it.categoryId == categoryId }

    fun category(id: String): Category? = categories.firstOrNull { it.id == id }

    companion object {
        val Empty = Catalog(emptyList(), emptyList())
    }
}

data class Category(
    val id: String,
    val name: String,
    val description: String?,
    /** Name of the icon shown on the category card, for example "chakras" or "sleep". */
    val icon: String,
    val subcategories: List<Subcategory>,
)

data class Subcategory(
    val id: String,
    val name: String,
    /** Accent colour for artwork, as "#RRGGBB". */
    val color: String?,
)

enum class TrackTier { FREE, PREMIUM }

data class Track(
    val id: String,
    val title: String,
    /** A short line under the title, for example "396 Hz". */
    val subtitle: String?,
    val description: String?,
    val categoryId: String,
    val subcategoryId: String,
    val durationMs: Long,
    /** Where the audio lives: "asset:///…" for files inside the app, later "https://…" for online files. */
    val audioUri: String,
    val tier: TrackTier,
    /** How the track repeats seamlessly in longer sessions; null if it should not be looped. */
    val loop: LoopPoints?,
)

/**
 * A seamless repeat: shortly before [outMs] a second copy starts at [inMs], and the two
 * crossfade over [crossfadeMs]. Both points sit inside the music, past the fade-in and
 * before the fade-out, chosen so the sound on either side matches.
 */
data class LoopPoints(
    val inMs: Long,
    val outMs: Long,
    val crossfadeMs: Long,
)

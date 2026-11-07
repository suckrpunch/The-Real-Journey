package com.newjourney.core.catalog

import com.newjourney.core.model.Catalog
import com.newjourney.core.model.Category
import com.newjourney.core.model.LoopPoints
import com.newjourney.core.model.Subcategory
import com.newjourney.core.model.Track
import com.newjourney.core.model.TrackTier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Turns catalog.json into the app's model. Unknown fields are ignored, so the file can grow. */
object CatalogParser {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * [resolveAudio] turns a track's "audio" value into a playable address. For the catalog bundled
     * in the app, relative paths are app files; for the online catalog they are files in online storage.
     */
    fun parse(text: String, resolveAudio: (String) -> String = ::toUri): Catalog {
        val file = json.decodeFromString<CatalogFile>(text)
        val categories = file.categories.map { category ->
            Category(
                id = category.id,
                name = category.name,
                description = category.description,
                icon = category.icon ?: category.id,
                subcategories = category.subcategories.map { Subcategory(it.id, it.name, it.color) },
            )
        }
        val tracks = file.tracks.map { track ->
            Track(
                id = track.id,
                title = track.title,
                subtitle = track.subtitle,
                description = track.description,
                categoryId = track.category,
                subcategoryId = track.subcategory,
                durationMs = track.durationMs,
                audioUri = resolveAudio(track.audio),
                tier = if (track.tier.equals("premium", ignoreCase = true)) TrackTier.PREMIUM else TrackTier.FREE,
                loop = track.loop?.takeIf { it.outMs > it.inMs + it.crossfadeMs }
                    ?.let { LoopPoints(it.inMs, it.outMs, it.crossfadeMs) },
            )
        }
        return Catalog(categories, tracks)
    }

    /** "audio/x.m4a" means a file inside the app; a full address (asset:///, https://) is used as is. */
    internal fun toUri(audio: String): String =
        if (audio.contains("://")) audio else "asset:///" + audio.trimStart('/')

    /**
     * For the online catalog: "audio/sleep/x.m4a" is a file in the Firebase Storage [bucket].
     * Full addresses (such as asset:/// for music bundled in the app) are used as is.
     */
    fun onlineResolver(bucket: String): (String) -> String = { audio ->
        if (audio.contains("://")) audio else storageUrl(bucket, audio.trimStart('/'))
    }

    /** The public download address of a file in Firebase Storage. */
    fun storageUrl(bucket: String, path: String): String =
        "https://firebasestorage.googleapis.com/v0/b/$bucket/o/" +
            java.net.URLEncoder.encode(path, "UTF-8").replace("+", "%20") + "?alt=media"

    @Serializable
    private data class CatalogFile(
        val version: Int = 1,
        val categories: List<CategoryDto> = emptyList(),
        val tracks: List<TrackDto> = emptyList(),
    )

    @Serializable
    private data class CategoryDto(
        val id: String,
        val name: String,
        val description: String? = null,
        val icon: String? = null,
        val subcategories: List<SubcategoryDto> = emptyList(),
    )

    @Serializable
    private data class SubcategoryDto(val id: String, val name: String, val color: String? = null)

    @Serializable
    private data class TrackDto(
        val id: String,
        val title: String,
        val subtitle: String? = null,
        val description: String? = null,
        val category: String,
        val subcategory: String,
        @SerialName("durationMs") val durationMs: Long,
        val audio: String,
        val tier: String = "free",
        val loop: LoopDto? = null,
    )

    @Serializable
    private data class LoopDto(val inMs: Long, val outMs: Long, val crossfadeMs: Long)
}

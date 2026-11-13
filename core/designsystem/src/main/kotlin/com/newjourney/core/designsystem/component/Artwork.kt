package com.newjourney.core.designsystem.component

import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import com.newjourney.core.designsystem.icon.NavIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The icon for a category, by the "icon" name in catalog.json. */
fun categoryIcon(name: String): ImageVector = when (name) {
    "chakras" -> NavIcons.Lotus
    "sleep" -> NavIcons.Moon
    "relaxation" -> NavIcons.Waves
    "focus" -> NavIcons.Target
    "deep" -> NavIcons.Meditator
    else -> NavIcons.Leaf
}

/** Reads "#RRGGBB" from the catalog; falls back to [fallback] when missing or invalid. */
fun parseCatalogColor(hex: String?, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)

/**
 * Cover images live in assets/covers: "<track id>.webp" for tracks and "category-<id>.webp" for
 * categories. A small memory cache keeps scrolling smooth; a missing file simply returns null.
 */
private object Covers {
    /** About 24 MB of decoded covers (a 512 px cover is 1 MB). */
    private val cache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    private val missing: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    fun cached(name: String): ImageBitmap? = cache.get(name)

    fun load(context: Context, name: String): ImageBitmap? {
        cache.get(name)?.let { return it }
        if (name in missing) return null
        val bitmap = runCatching {
            context.assets.open("covers/$name.webp").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()?.asImageBitmap()
        if (bitmap == null) missing += name else cache.put(name, bitmap)
        return bitmap
    }
}

/** The cover image named [name] (without ".webp"), loaded off the main thread; null while loading or missing. */
@Composable
fun rememberCover(name: String?): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val cover by produceState(initialValue = name?.let(Covers::cached), name) {
        value = name?.let { withContext(Dispatchers.IO) { Covers.load(context, it) } }
    }
    return cover
}

/**
 * The artwork for a track: its cover image when [coverId] has one, otherwise a calm, abstract glow
 * in the theme colour. Decorative, so screen readers skip it.
 */
@Composable
fun TrackArtwork(colorHex: String?, modifier: Modifier = Modifier, coverId: String? = null) {
    val cover = rememberCover(coverId)
    if (cover != null) {
        Image(cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        GlowArtwork(modifier)
    }
}

/** The cover for a category ("category-<id>.webp"), or nothing when there is none. */
@Composable
fun CategoryCover(categoryId: String, modifier: Modifier = Modifier, fallback: @Composable BoxScope.() -> Unit = {}) {
    val cover = rememberCover("category-$categoryId")
    if (cover != null) {
        Image(cover, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    } else {
        Box(modifier) { fallback() }
    }
}

/** Calm, abstract artwork: a soft glow with quiet rings in the theme colour. */
@Composable
private fun GlowArtwork(modifier: Modifier) {
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)
        drawRect(surface)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.75f), color.copy(alpha = 0.25f), Color.Transparent),
                center = center,
                radius = radius * 0.95f,
            ),
            radius = radius * 0.95f,
            center = center,
        )
        for (i in 1..3) {
            drawCircle(
                color = color.copy(alpha = 0.35f / i),
                radius = radius * (0.35f + 0.18f * i),
                center = center,
                style = Stroke(width = radius * 0.015f),
            )
        }
        drawCircle(color.copy(alpha = 0.9f), radius = radius * 0.16f, center = center)
    }
}

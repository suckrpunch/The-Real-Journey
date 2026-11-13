package com.newjourney.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Healing
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector

/** The icon for a category, by the "icon" name in catalog.json. */
fun categoryIcon(name: String): ImageVector = when (name) {
    "chakras" -> Icons.Outlined.SelfImprovement
    "sleep" -> Icons.Outlined.Bedtime
    "relaxation" -> Icons.Outlined.Spa
    "focus" -> Icons.Outlined.CenterFocusStrong
    "deep" -> Icons.Outlined.Waves
    else -> Icons.Outlined.Healing
}

/** Reads "#RRGGBB" from the catalog; falls back to [fallback] when missing or invalid. */
fun parseCatalogColor(hex: String?, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)

/**
 * Calm, abstract artwork for a track: a soft glow with quiet rings in the theme colour.
 * Used until the owner adds real artwork. Decorative, so screen readers skip it.
 */
@Composable
fun TrackArtwork(colorHex: String?, modifier: Modifier = Modifier) {
    // The approved design keeps artwork monochrome: every track glows in the theme's own colour.
    // [colorHex] stays in the signature so real artwork or colours can return later without changes.
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

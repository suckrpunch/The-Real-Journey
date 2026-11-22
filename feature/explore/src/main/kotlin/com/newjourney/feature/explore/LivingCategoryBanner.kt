package com.newjourney.feature.explore

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import com.newjourney.core.designsystem.component.rememberCover
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The banner on a category page, gently alive: the picture drifts and zooms very slowly, and each
 * category has its own quiet detail — the chakras light up one after another, stars twinkle over
 * the sleep moon, rings ripple out for focus. Still when animations are off. Decorative.
 */
@Composable
internal fun LivingCategoryBanner(categoryId: String, modifier: Modifier = Modifier) {
    val cover = rememberCover("category-$categoryId") ?: return Box(modifier)
    val reduceMotion = rememberReduceMotion()
    val seconds by rememberSeconds(!reduceMotion)
    val scene = remember(categoryId) { categoryScene(categoryId) }

    Canvas(modifier) {
        val t = seconds
        val wave = (1f - cos(t * 2f * PI.toFloat() / 18f)) / 2f
        val zoom = if (reduceMotion) 1f else 1.03f + 0.025f * wave
        val driftX = if (reduceMotion) 0f else size.width * 0.01f * sin(t * 2f * PI.toFloat() / 28f)
        withTransform({
            scale(zoom, zoom, pivot = center)
            translate(left = driftX)
        }) {
            drawLivingCover(cover, scene, t, animate = !reduceMotion)
        }
    }
}

/** True when the phone's animations are turned off: everything then stays still. */
@Composable
internal fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Seconds since this appeared, updated every frame while [running]. */
@Composable
internal fun rememberSeconds(running: Boolean): State<Float> = produceState(0f, running) {
    if (!running) return@produceState
    val start = withFrameNanos { it }
    while (true) withFrameNanos { value = (it - start) / 1_000_000_000f }
}

private fun categoryScene(categoryId: String): Scene = when (categoryId) {
    "chakras" -> Scene(focusY = 0.46f) { t, side, at ->
        // Root to crown, one after another, then a pause: like a breath rising through the body.
        CHAKRAS.forEachIndexed { i, (y, color) ->
            val pulse = bump(phase(t, period = 7f, offset = i / 9f))
            glow(at(0.497f, y), side * 0.04f * (1.5f + 1.3f * pulse), color, 0.18f + 0.5f * pulse)
            glow(at(0.497f, y), side * 0.03f, Color.White, 0.12f * pulse)
        }
    }
    "sleep" -> Scene(focusY = 0.42f) { t, side, at ->
        breathingGlow(at, side, t, 0.37f, 0.45f, 0.16f, MOONLIGHT, 0.10f, 0.20f)
        stars(at, side, t, STARS_SLEEP, r = 0.012f)
        glints(at, side, t, 0.5f, 0.68f, 0.06f, 0.09f, MOONLIGHT, count = 5)
    }
    "relaxation" -> Scene(focusY = 0.45f) { t, side, at ->
        breathingGlow(at, side, t, 0.293f, 0.332f, 0.14f, SUNLIGHT, 0.14f, 0.28f)
        stars(at, side, t, SPARKLES_SEA, r = 0.008f, color = Color(0xFFFFFBEF))
    }
    "focus" -> Scene(focusY = 0.49f) { t, side, at ->
        breathingGlow(at, side, t, 0.497f, 0.485f, 0.09f, Color(0xFFFFF6D8), 0.20f, 0.40f, period = 5f)
        ripples(at, side, t, 0.497f, 0.485f, 0.07f, 0.49f, Color(0xFFF3F6E4))
    }
    "deep" -> Scene(focusY = 0.52f) { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.62f, 0.26f, Color(0xFFFFE6B0), 0.10f, 0.22f, period = 7f)
        risingMotes(at, side, t, centerX = 0.5f, fromY = 0.72f, toY = 0.28f, color = Color(0xFFFFEDC4))
        twinkle(at(0.499f, 0.156f), side * 0.014f, t, 3)
    }
    "healing" -> Scene(focusY = 0.5f) { t, side, at ->
        breathingGlow(at, side, t, 0.62f, 0.5f, 0.2f, Color(0xFFF6FFD8), 0.10f, 0.24f)
        risingMotes(at, side, t, centerX = 0.55f, fromY = 0.8f, toY = 0.25f, color = Color(0xFFFFFBE6))
    }
    else -> Scene()
}

internal val MOONLIGHT = Color(0xFFFFF1C9)
internal val SUNLIGHT = Color(0xFFFFF4D6)

// The chakra circles, bottom (root) to top (crown), with their own colours.
private val CHAKRAS = listOf(
    0.781f to Color(0xFFF17857),
    0.671f to Color(0xFFFA9C27),
    0.565f to Color(0xFFFCD05F),
    0.456f to Color(0xFF89B759),
    0.349f to Color(0xFF63B6DB),
    0.239f to Color(0xFF7181C0),
    0.129f to Color(0xFFA67CBC),
)

private val STARS_SLEEP = listOf(
    0.219f to 0.172f, 0.809f to 0.184f, 0.12f to 0.3f, 0.63f to 0.12f, 0.9f to 0.33f, 0.27f to 0.07f, 0.72f to 0.27f,
)

private val SPARKLES_SEA = listOf(
    0.48f to 0.47f, 0.62f to 0.5f, 0.78f to 0.46f, 0.55f to 0.55f, 0.88f to 0.52f, 0.7f to 0.58f, 0.42f to 0.52f,
)

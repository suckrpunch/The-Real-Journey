package com.newjourney.feature.explore

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.newjourney.core.designsystem.component.rememberCover
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The banner on a category page, gently alive: the picture drifts and zooms very slowly, and each
 * category has its own quiet detail drawn on top — the chakras light up one after another, stars
 * twinkle over the sleep moon, rings ripple out for focus. Calm, never busy. Still when animations
 * are turned off on the phone. Decorative, so screen readers skip it.
 *
 * Positions below are fractions of the square cover image (0..1, from the top left), measured from
 * the artwork itself, so the light sits exactly on the chakras, the moon or the sun.
 */
@Composable
internal fun LivingCategoryBanner(categoryId: String, modifier: Modifier = Modifier) {
    val cover = rememberCover("category-$categoryId") ?: return Box(modifier)
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val seconds by produceState(0f, reduceMotion) {
        if (reduceMotion) return@produceState
        val start = withFrameNanos { it }
        while (true) withFrameNanos { value = (it - start) / 1_000_000_000f }
    }
    val scene = remember(categoryId) { sceneFor(categoryId) }

    Canvas(modifier) {
        val t = seconds
        // Show the part of the square picture where the subject is.
        val side = maxOf(size.width, size.height)
        val offX = (size.width - side) / 2f
        val offY = (size.height / 2f - scene.focusY * side).coerceIn(size.height - side, 0f)
        fun at(x: Float, y: Float) = Offset(offX + x * side, offY + y * side)

        // A very slow camera: always zoomed in a little more than it drifts, so no edge shows.
        val wave = (1f - cos(t * 2f * PI.toFloat() / ZOOM_S)) / 2f
        val zoom = if (reduceMotion) 1f else 1.03f + 0.025f * wave
        val driftX = if (reduceMotion) 0f else size.width * 0.01f * sin(t * 2f * PI.toFloat() / DRIFT_S)

        withTransform({
            scale(zoom, zoom, pivot = center)
            translate(left = driftX)
        }) {
            drawImage(
                cover,
                dstOffset = IntOffset(offX.roundToInt(), offY.roundToInt()),
                dstSize = IntSize(side.roundToInt(), side.roundToInt()),
                filterQuality = FilterQuality.High,
            )
            if (!reduceMotion) scene.draw(this, t, side, ::at)
        }
    }
}

private const val ZOOM_S = 18f
private const val DRIFT_S = 28f

/** One category's banner: where to look, and what moves. */
private class Scene(
    val focusY: Float,
    val draw: DrawScope.(t: Float, side: Float, at: (Float, Float) -> Offset) -> Unit,
)

private fun sceneFor(categoryId: String): Scene = when (categoryId) {
    "chakras" -> Scene(focusY = 0.46f) { t, side, at ->
        // Root to crown, one after another, then a pause: like a breath rising through the body.
        CHAKRAS.forEachIndexed { i, (y, color) ->
            val pulse = bump(phase(t, period = 7f, offset = i / 9f))
            glow(at(0.497f, y), side * 0.04f * (1.5f + 1.3f * pulse), color, 0.18f + 0.5f * pulse)
            glow(at(0.497f, y), side * 0.03f, Color.White, 0.12f * pulse)
        }
    }
    "sleep" -> Scene(focusY = 0.42f) { t, side, at ->
        glow(at(0.37f, 0.45f), side * 0.16f, Color(0xFFFFF1C9), 0.10f + 0.10f * breathe(t, 6f))
        STARS_SLEEP.forEachIndexed { i, (x, y) -> twinkle(at(x, y), side * 0.012f, t, i) }
        // Moonlight on the water: a few glints that come and go.
        repeat(5) { i ->
            val p = bump(phase(t, period = 4.5f, offset = i / 5f))
            glow(at(0.47f + 0.015f * i, 0.68f + 0.022f * i), side * 0.02f, Color(0xFFFFF1C9), 0.35f * p)
        }
    }
    "relaxation" -> Scene(focusY = 0.45f) { t, side, at ->
        glow(at(0.293f, 0.332f), side * 0.14f, Color(0xFFFFF4D6), 0.14f + 0.14f * breathe(t, 6f))
        SPARKLES_SEA.forEachIndexed { i, (x, y) -> twinkle(at(x, y), side * 0.008f, t, i, Color(0xFFFFFBEF)) }
    }
    "focus" -> Scene(focusY = 0.49f) { t, side, at ->
        val c = at(0.497f, 0.485f)
        glow(c, side * 0.09f, Color(0xFFFFF6D8), 0.20f + 0.20f * breathe(t, 5f))
        // Rings ripple out slowly from the centre and fade away.
        repeat(3) { i ->
            val p = phase(t, period = 7.5f, offset = i / 3f)
            drawCircle(
                Color(0xFFF3F6E4).copy(alpha = 0.32f * (1f - p) * (1f - p)),
                radius = side * (0.07f + 0.42f * p),
                center = c,
                style = Stroke(width = side * 0.006f),
            )
        }
    }
    "deep" -> Scene(focusY = 0.52f) { t, side, at ->
        glow(at(0.5f, 0.62f), side * 0.26f, Color(0xFFFFE6B0), 0.10f + 0.12f * breathe(t, 7f))
        risingMotes(t, side, at, centerX = 0.5f, fromY = 0.72f, toY = 0.28f, color = Color(0xFFFFEDC4))
        twinkle(at(0.499f, 0.156f), side * 0.014f, t, 3)
    }
    "healing" -> Scene(focusY = 0.5f) { t, side, at ->
        glow(at(0.62f, 0.5f), side * 0.2f, Color(0xFFF6FFD8), 0.10f + 0.14f * breathe(t, 6f))
        risingMotes(t, side, at, centerX = 0.55f, fromY = 0.8f, toY = 0.25f, color = Color(0xFFFFFBE6))
    }
    else -> Scene(focusY = 0.5f) { _, _, _ -> }
}

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

/** 0..1, repeating every [period] seconds; [offset] shifts it along (0..1). */
private fun phase(t: Float, period: Float, offset: Float): Float = ((t / period - offset) % 1f + 1f) % 1f

/** A soft bump at the start of each phase, quiet the rest of the time. */
private fun bump(p: Float): Float = exp(-((p - 0.08f) * (p - 0.08f)) / 0.006f)

/** 0..1, breathing in and out over [period] seconds. */
private fun breathe(t: Float, period: Float): Float = (1f - cos(t * 2f * PI.toFloat() / period)) / 2f

private fun DrawScope.glow(center: Offset, radius: Float, color: Color, alpha: Float) {
    if (alpha <= 0.005f) return
    drawCircle(
        Brush.radialGradient(listOf(color.copy(alpha = alpha), color.copy(alpha = alpha * 0.4f), Color.Transparent), center, radius),
        radius = radius,
        center = center,
    )
}

/** A star or glint that brightens and fades at its own rhythm. */
private fun DrawScope.twinkle(center: Offset, radius: Float, t: Float, seed: Int, color: Color = Color.White) {
    val p = breathe(t + seed * 1.37f, 3.2f + (seed % 3) * 0.9f)
    glow(center, radius * (2f + 2f * p), color, 0.15f + 0.55f * p * p)
}

/** A few specks of light rising slowly and fading, like dust in warm light. */
private fun DrawScope.risingMotes(
    t: Float,
    side: Float,
    at: (Float, Float) -> Offset,
    centerX: Float,
    fromY: Float,
    toY: Float,
    color: Color,
) {
    repeat(7) { i ->
        val p = phase(t, period = 9f + i % 3, offset = i / 7f)
        val x = centerX + (((i * 37) % 11) - 5) * 0.035f + 0.012f * sin(t * 0.6f + i)
        val y = fromY + (toY - fromY) * p
        val fade = sin(p * PI.toFloat())
        glow(at(x, y), side * 0.012f, color, 0.5f * fade)
    }
}

package com.newjourney.feature.meditate

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.newjourney.core.designsystem.component.isNightTheme
import com.newjourney.core.designsystem.theme.NewJourneyTheme
import kotlin.math.cos
import kotlin.math.sin

// Small hand-drawn touches from the approved Meditate design. Thin lines in the theme's own
// colour, purely decorative, so screen readers skip them.

/** The warm amber of the approved sun: a small, quiet touch of daylight. */
private val SunAmber = Color(0xFFF2A03D)

/**
 * A sun by day, a crescent moon with a few stars by night, with a very slow, subtle animation:
 * the sun's rays turn gently and its core breathes; the moon floats a little and the stars twinkle.
 * Purely decorative, so screen readers skip it.
 */
@Composable
internal fun SkyMark(modifier: Modifier = Modifier) {
    val night = isNightTheme()
    val moonColor = MaterialTheme.colorScheme.primary
    val motion = rememberInfiniteTransition(label = "sky")
    // One full turn of the rays every 60 seconds: you notice it only if you look.
    val turn by motion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(60_000, easing = LinearEasing)),
        label = "turn",
    )
    // A slow in-and-out breath, about four seconds each way.
    val breath by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath",
    )
    val twinkle by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2_600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "twinkle",
    )

    Canvas(modifier) {
        val c = Offset(size.width / 2, size.height / 2)
        if (!night) {
            val stroke = size.minDimension * 0.03f
            val r = size.minDimension * 0.2f * (1f + 0.05f * breath)
            drawCircle(SunAmber, radius = r, center = c, style = Stroke(width = stroke))
            rotate(turn, pivot = c) {
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0 - 90).toFloat()
                    val inner = size.minDimension * 0.31f
                    val outer = size.minDimension * (0.42f + 0.02f * breath)
                    drawLine(
                        SunAmber,
                        Offset(c.x + cos(a) * inner, c.y + sin(a) * inner),
                        Offset(c.x + cos(a) * outer, c.y + sin(a) * outer),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
            }
        } else {
            val r = size.minDimension * 0.24f
            val float = size.minDimension * 0.025f * (breath - 0.5f) * 2f
            val mc = Offset(c.x + size.minDimension * 0.05f, c.y + float)
            val moon = Path().apply { addOval(Rect(mc, r)) }
            val bite = Path().apply { addOval(Rect(Offset(mc.x - r * 0.55f, mc.y - r * 0.35f), r)) }
            val crescent = Path().apply { op(moon, bite, PathOperation.Difference) }
            drawPath(crescent, moonColor)
            // Four little stars, twinkling out of step with each other.
            val stars = listOf(
                Triple(Offset(size.width * 0.14f, size.height * 0.2f), 0.045f, 0f),
                Triple(Offset(size.width * 0.9f, size.height * 0.1f), 0.035f, 0.5f),
                Triple(Offset(size.width * 0.1f, size.height * 0.55f), 0.03f, 0.25f),
                Triple(Offset(size.width * 0.88f, size.height * 0.9f), 0.04f, 0.75f),
            )
            stars.forEach { (pos, arm, phase) ->
                val t = ((twinkle + phase) % 1f)
                val glow = 0.45f + 0.55f * (if (t < 0.5f) t * 2f else (1f - t) * 2f)
                star(pos, size.minDimension * arm, moonColor.copy(alpha = moonColor.alpha * glow))
            }
        }
    }
}

private fun DrawScope.star(center: Offset, arm: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - arm * 2)
        quadraticTo(center.x, center.y, center.x + arm * 2, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + arm * 2)
        quadraticTo(center.x, center.y, center.x - arm * 2, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - arm * 2)
        close()
    }
    drawPath(path, color)
}

/**
 * Two small pines with a few blades of grass, as in the approved design: thin outlines with a
 * soft green fill by day, outlines only by night. Drawn on a 100 × 76 grid and scaled to fit.
 */
@Composable
internal fun Pines(modifier: Modifier = Modifier) {
    val line = MaterialTheme.colorScheme.primary
    val fill = if (isNightTheme()) Color.Transparent else NewJourneyTheme.extraColors.breathMiddle
    Canvas(modifier) {
        val k = minOf(size.width / 100f, size.height / 76f)
        val ox = (size.width - 100f * k) / 2f
        val oy = size.height - 76f * k
        fun p(x: Float, y: Float) = Offset(ox + x * k, oy + y * k)
        val stroke = Stroke(width = 1.8f * k, cap = StrokeCap.Round, join = StrokeJoin.Round)
        pine(::p, cx = 36f, top = 24f, w = 22f, h = 46f, line = line, fill = fill, stroke = stroke)
        pine(::p, cx = 64f, top = 10f, w = 26f, h = 60f, line = line, fill = fill, stroke = stroke)
        grass(::p, 12f, 72f, line, stroke.width)
        grass(::p, 90f, 72f, line, stroke.width)
    }
}

/** A pine of three tiers with a trunk running up through it. */
private fun DrawScope.pine(
    p: (Float, Float) -> Offset,
    cx: Float,
    top: Float,
    w: Float,
    h: Float,
    line: Color,
    fill: Color,
    stroke: Stroke,
) {
    val tiers = listOf(0f to 0f, 0.24f to 0.3f, 0.1f to 0.3f, 0.38f to 0.58f, 0.13f to 0.58f, 0.5f to 0.86f)
    val outline = Path()
    tiers.forEachIndexed { i, (x, y) ->
        val pt = p(cx + x * w, top + y * h)
        if (i == 0) outline.moveTo(pt.x, pt.y) else outline.lineTo(pt.x, pt.y)
    }
    tiers.drop(1).reversed().forEach { (x, y) ->
        val pt = p(cx - x * w, top + y * h)
        outline.lineTo(pt.x, pt.y)
    }
    outline.close()
    if (fill != Color.Transparent) drawPath(outline, fill)
    drawPath(outline, line, style = stroke)
    drawLine(line, p(cx, top + 0.32f * h), p(cx, top + h), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

/** Three blades of grass fanning upwards. */
private fun DrawScope.grass(p: (Float, Float) -> Offset, x: Float, y: Float, color: Color, width: Float) {
    drawLine(color, p(x - 1.6f, y), p(x - 4.2f, y - 5f), strokeWidth = width, cap = StrokeCap.Round)
    drawLine(color, p(x, y), p(x, y - 7f), strokeWidth = width, cap = StrokeCap.Round)
    drawLine(color, p(x + 1.6f, y), p(x + 4.2f, y - 5f), strokeWidth = width, cap = StrokeCap.Round)
}

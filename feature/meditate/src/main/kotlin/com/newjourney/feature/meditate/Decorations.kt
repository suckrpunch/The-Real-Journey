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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.newjourney.core.designsystem.component.isNightTheme
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

/** Two small pines with a little grass, in thin lines. */
@Composable
internal fun Pines(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = h * 0.022f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        pine(Offset(w * 0.40f, h * 0.10f), Size(w * 0.24f, h * 0.70f), color, stroke)
        pine(Offset(w * 0.64f, h * 0.0f), Size(w * 0.30f, h * 0.82f), color, stroke)
        grass(Offset(w * 0.12f, h * 0.9f), h * 0.12f, color, stroke)
        grass(Offset(w * 0.94f, h * 0.88f), h * 0.12f, color, stroke)
    }
}

private fun DrawScope.pine(top: Offset, box: Size, color: Color, stroke: Stroke) {
    val cx = top.x + box.width / 2
    val trunkBottom = top.y + box.height * 1.15f
    val tiers = 3
    val path = Path()
    for (i in 0 until tiers) {
        val tierTop = top.y + box.height * (i / (tiers + 0.6f))
        val tierBottom = top.y + box.height * ((i + 1.6f) / (tiers + 0.6f))
        val half = box.width / 2 * ((i + 1f) / tiers)
        path.moveTo(cx, tierTop)
        path.lineTo(cx - half, tierBottom)
        path.lineTo(cx + half, tierBottom)
        path.close()
    }
    drawPath(path, color, style = stroke)
    drawLine(color, Offset(cx, top.y + box.height * 0.3f), Offset(cx, trunkBottom), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

private fun DrawScope.grass(base: Offset, height: Float, color: Color, stroke: Stroke) {
    listOf(-0.5f, 0f, 0.5f).forEach { lean ->
        drawLine(
            color,
            Offset(base.x + lean * height * 0.4f, base.y - height * (if (lean == 0f) 1f else 0.75f)),
            Offset(base.x + lean * height * 0.15f, base.y),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}

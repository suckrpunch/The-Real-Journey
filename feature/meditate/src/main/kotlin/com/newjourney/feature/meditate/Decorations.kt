package com.newjourney.feature.meditate

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.newjourney.core.designsystem.component.isNightTheme
import kotlin.math.cos
import kotlin.math.sin

// Small hand-drawn touches from the approved Meditate design. Thin lines in the theme's own
// colour, purely decorative, so screen readers skip them.

/** A sun by day, a crescent moon with a few stars by night. */
@Composable
internal fun SkyMark(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val night = isNightTheme()
    Canvas(modifier) {
        val stroke = Stroke(width = size.minDimension * 0.035f, cap = StrokeCap.Round)
        val c = Offset(size.width / 2, size.height / 2)
        if (!night) {
            val r = size.minDimension * 0.22f
            drawCircle(color, radius = r, center = c, style = stroke)
            for (i in 0 until 8) {
                val a = Math.toRadians(i * 45.0 - 90).toFloat()
                val inner = r * 1.45f
                val outer = r * 1.95f
                drawLine(
                    color,
                    Offset(c.x + cos(a) * inner, c.y + sin(a) * inner),
                    Offset(c.x + cos(a) * outer, c.y + sin(a) * outer),
                    strokeWidth = stroke.width,
                    cap = StrokeCap.Round,
                )
            }
        } else {
            val r = size.minDimension * 0.26f
            val moon = Path().apply {
                addOval(Rect(c, r))
            }
            val bite = Path().apply {
                addOval(Rect(Offset(c.x - r * 0.55f, c.y - r * 0.35f), r))
            }
            val crescent = Path().apply { op(moon, bite, PathOperation.Difference) }
            drawPath(crescent, color)
            star(Offset(size.width * 0.18f, size.height * 0.2f), size.minDimension * 0.045f, color)
            star(Offset(size.width * 0.84f, size.height * 0.12f), size.minDimension * 0.035f, color)
            star(Offset(size.width * 0.12f, size.height * 0.62f), size.minDimension * 0.03f, color)
            star(Offset(size.width * 0.86f, size.height * 0.86f), size.minDimension * 0.04f, color)
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

package com.newjourney.feature.explore

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * The small toolkit behind the living covers and banners. Every position is a fraction of the square
 * cover (0..1 from the top left); `at` turns it into a point on screen. Everything is soft and slow.
 */

/** Turns a position on the cover (fractions) into a point on screen. */
internal typealias At = (Float, Float) -> Offset

/**
 * A part of the picture that moves a little on its own (leaves, a cloud, a boat, a sleeping panda).
 * It is drawn again on top with a feathered edge, shifted, turned or scaled very slightly.
 */
internal class Patch(
    val x: Float,
    val y: Float,
    val radius: Float,
    val motion: (t: Float) -> Motion,
)

internal data class Motion(
    val dx: Float = 0f,
    val dy: Float = 0f,
    val degrees: Float = 0f,
    val scale: Float = 1f,
    val pivotX: Float,
    val pivotY: Float,
)

/** One cover's life: where to look, which parts move, and what is drawn on top. */
internal class Scene(
    val focusY: Float = 0.5f,
    val patches: List<Patch> = emptyList(),
    val draw: DrawScope.(t: Float, side: Float, at: At) -> Unit = { _, _, _ -> },
)

/** Draws [cover] filling the canvas (cropped to the scene's focus), its moving parts, then its light. */
internal fun DrawScope.drawLivingCover(cover: ImageBitmap, scene: Scene, t: Float, animate: Boolean) {
    val side = maxOf(size.width, size.height)
    val offX = (size.width - side) / 2f
    val offY = (size.height / 2f - scene.focusY * side).coerceIn(size.height - side, 0f)
    val at: At = { x, y -> Offset(offX + x * side, offY + y * side) }
    fun image() = drawImage(
        cover,
        dstOffset = IntOffset(offX.roundToInt(), offY.roundToInt()),
        dstSize = IntSize(side.roundToInt(), side.roundToInt()),
        filterQuality = FilterQuality.High,
    )
    image()
    if (!animate) return
    scene.patches.forEach { patch ->
        val m = patch.motion(t)
        val c = at(patch.x, patch.y)
        val r = patch.radius * side
        val pivot = at(m.pivotX, m.pivotY)
        val bounds = Rect(c.x - r, c.y - r, c.x + r, c.y + r)
        drawIntoCanvas { canvas ->
            canvas.saveLayer(bounds, Paint())
            withTransform({
                translate(m.dx * side, m.dy * side)
                rotate(m.degrees, pivot)
                scale(m.scale, m.scale, pivot)
            }) { image() }
            // Keep only a soft circle of the moved copy, so it melts into the picture.
            drawRect(
                Brush.radialGradient(listOf(Color.Black, Color.Black, Color.Transparent), c, r),
                topLeft = bounds.topLeft,
                size = Size(bounds.width, bounds.height),
                blendMode = BlendMode.DstIn,
            )
            canvas.restore()
        }
    }
    scene.draw(this, t, side, at)
}

// ---- Timing ----

/** 0..1, repeating every [period] seconds; [offset] shifts it along (0..1). */
internal fun phase(t: Float, period: Float, offset: Float = 0f): Float = ((t / period - offset) % 1f + 1f) % 1f

/** A soft bump at the start of each phase, quiet the rest of the time. */
internal fun bump(p: Float, width: Float = 0.006f): Float = exp(-((p - 0.08f) * (p - 0.08f)) / width)

/** 0..1, breathing in and out over [period] seconds. */
internal fun breathe(t: Float, period: Float, offset: Float = 0f): Float =
    (1f - cos((t / period + offset) * 2f * PI.toFloat())) / 2f

/** A slow sway between -1 and 1. */
internal fun sway(t: Float, period: Float, offset: Float = 0f): Float = sin((t / period + offset) * 2f * PI.toFloat())

// ---- Light ----

internal fun DrawScope.glow(center: Offset, radius: Float, color: Color, alpha: Float) {
    if (alpha <= 0.005f || radius <= 0f) return
    drawCircle(
        Brush.radialGradient(listOf(color.copy(alpha = alpha.coerceAtMost(1f)), color.copy(alpha = alpha * 0.4f), Color.Transparent), center, radius),
        radius = radius,
        center = center,
    )
}

/** A light that breathes in place. */
internal fun DrawScope.breathingGlow(at: At, side: Float, t: Float, x: Float, y: Float, r: Float, color: Color, low: Float, high: Float, period: Float = 6f, offset: Float = 0f) =
    glow(at(x, y), side * r, color, low + (high - low) * breathe(t, period, offset))

/** A star or glint that brightens and fades at its own rhythm. */
internal fun DrawScope.twinkle(center: Offset, radius: Float, t: Float, seed: Int, color: Color = Color.White) {
    val p = breathe(t + seed * 1.37f, 3.2f + (seed % 3) * 0.9f)
    glow(center, radius * (2f + 2f * p), color, 0.15f + 0.55f * p * p)
}

internal fun DrawScope.stars(at: At, side: Float, t: Float, points: List<Pair<Float, Float>>, r: Float = 0.01f, color: Color = Color.White) =
    points.forEachIndexed { i, (x, y) -> twinkle(at(x, y), side * r, t, i, color) }

/** Glints on water along a short band, coming and going. */
internal fun DrawScope.glints(at: At, side: Float, t: Float, x: Float, y: Float, width: Float, depth: Float, color: Color = Color(0xFFFFF4D6), count: Int = 6) =
    repeat(count) { i ->
        val p = bump(phase(t, period = 4.5f, offset = i / count.toFloat()))
        val gx = x + width * (((i * 7) % count) / (count - 1f) - 0.5f)
        val gy = y + depth * (i / (count - 1f))
        glow(at(gx, gy), side * 0.018f, color, 0.45f * p)
    }

/** A few specks of light rising slowly and fading, like dust in warm light. */
internal fun DrawScope.risingMotes(at: At, side: Float, t: Float, centerX: Float, fromY: Float, toY: Float, color: Color, spread: Float = 0.18f, count: Int = 7) =
    repeat(count) { i ->
        val p = phase(t, period = 9f + i % 3, offset = i / count.toFloat())
        val x = centerX + spread * ((((i * 37) % 11) - 5) / 5f) + 0.012f * sin(t * 0.6f + i)
        val y = fromY + (toY - fromY) * p
        glow(at(x, y), side * 0.012f, color, 0.5f * sin(p * PI.toFloat()))
    }

/** Soft steam curling up from a cup: puffs that rise, widen and fade. */
internal fun DrawScope.steam(at: At, side: Float, t: Float, x: Float, y: Float, height: Float) =
    repeat(3) { wisp ->
        repeat(6) { j ->
            val p = phase(t, period = 4f, offset = j / 6f + wisp * 0.11f)
            val px = x + (wisp - 1) * 0.012f + 0.018f * sin(p * 5f + t * 0.7f + wisp * 2f) * (0.4f + p)
            val py = y - height * p
            glow(at(px, py), side * 0.014f * (1f + 1.6f * p), Color.White, 0.2f * sin(p * PI.toFloat()) * (1f - 0.3f * p))
        }
    }

/** Beams of light from [x],[y] fanning around [angle] (degrees; 0 = right, 90 = down), slowly shifting. */
internal fun DrawScope.rays(at: At, side: Float, t: Float, x: Float, y: Float, angle: Float, spread: Float, length: Float, count: Int, color: Color, strength: Float = 0.16f) {
    val origin = at(x, y)
    repeat(count) { i ->
        val a = Math.toRadians((angle + spread * (i / (count - 1f).coerceAtLeast(1f) - 0.5f) + 1.5f * sway(t, 11f, i * 0.23f)).toDouble())
        val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
        val normal = Offset(-dir.y, dir.x)
        val len = length * side
        val w0 = side * 0.01f
        val w1 = side * (0.06f + 0.03f * (i % 2))
        val end = origin + dir * len
        val path = Path().apply {
            moveTo((origin + normal * w0).x, (origin + normal * w0).y)
            lineTo((end + normal * w1).x, (end + normal * w1).y)
            lineTo((end - normal * w1).x, (end - normal * w1).y)
            lineTo((origin - normal * w0).x, (origin - normal * w0).y)
            close()
        }
        val alpha = strength * (0.45f + 0.55f * breathe(t, 7f + i, i * 0.31f))
        drawPath(path, Brush.linearGradient(listOf(color.copy(alpha = alpha), Color.Transparent), origin, end))
    }
}

/** A soft patch of light (sun through a window or leaves) drifting slowly. */
internal fun DrawScope.dapple(at: At, side: Float, t: Float, x: Float, y: Float, r: Float, color: Color, alpha: Float, drift: Float = 0.03f, period: Float = 14f) =
    glow(at(x + drift * sway(t, period), y + drift * 0.5f * sway(t, period * 1.3f, 0.25f)), side * r, color, alpha * (0.7f + 0.3f * breathe(t, period * 0.6f)))

/** Rings spreading out from a point and fading. [flat] < 1 makes them lie down, like on water. */
internal fun DrawScope.ripples(at: At, side: Float, t: Float, x: Float, y: Float, from: Float, to: Float, color: Color, flat: Float = 1f, period: Float = 7.5f, count: Int = 3, strength: Float = 0.32f) {
    val c = at(x, y)
    repeat(count) { i ->
        val p = phase(t, period, i / count.toFloat())
        val rx = side * (from + (to - from) * p)
        drawOval(
            color.copy(alpha = strength * (1f - p) * (1f - p)),
            topLeft = Offset(c.x - rx, c.y - rx * flat),
            size = Size(rx * 2f, rx * 2f * flat),
            style = Stroke(width = side * 0.005f),
        )
    }
}

/** A light travelling along a line (a path, a wave, a comet trail), with a short soft tail. */
internal fun DrawScope.travel(at: At, side: Float, t: Float, points: List<Pair<Float, Float>>, color: Color, period: Float = 6f, size: Float = 0.02f, strength: Float = 0.6f, offset: Float = 0f) {
    val p = phase(t, period, offset)
    // Travel for the first 70 % of the period, then rest.
    val q = (p / 0.7f).coerceAtMost(1f)
    val fade = if (p > 0.7f) 0f else sin(q * PI.toFloat())
    repeat(4) { k ->
        val (x, y) = pointAlong(points, (q - k * 0.025f).coerceIn(0f, 1f))
        glow(at(x, y), side * size * (1f - 0.15f * k), color, strength * fade * (1f - 0.22f * k))
    }
}

private fun pointAlong(points: List<Pair<Float, Float>>, f: Float): Pair<Float, Float> {
    val lengths = points.zipWithNext { a, b -> sqrt((b.first - a.first).let { it * it } + (b.second - a.second).let { it * it }) }
    var target = lengths.sum() * f
    lengths.forEachIndexed { i, l ->
        if (target <= l || i == lengths.lastIndex) {
            val k = if (l == 0f) 0f else (target / l).coerceIn(0f, 1f)
            val a = points[i]
            val b = points[i + 1]
            return (a.first + (b.first - a.first) * k) to (a.second + (b.second - a.second) * k)
        }
        target -= l
    }
    return points.last()
}

/** A slow diagonal sheen sweeping across now and then, like light over paper. */
internal fun DrawScope.sheen(t: Float, period: Float = 10f, strength: Float = 0.12f) {
    val p = phase(t, period)
    if (p > 0.6f) return
    val q = p / 0.6f
    val w = size.width
    val x = -w + q * 3f * w
    drawRect(
        Brush.linearGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = strength * sin(q * PI.toFloat())), Color.Transparent),
            Offset(x, 0f),
            Offset(x + w * 0.6f, size.height),
        ),
    )
}

/** Soft rain: thin streaks falling through an area. */
internal fun DrawScope.rain(at: At, side: Float, t: Float, x0: Float, x1: Float, y0: Float, y1: Float, count: Int = 9) =
    repeat(count) { i ->
        val p = phase(t, period = 1.6f + (i % 3) * 0.3f, offset = i / count.toFloat())
        val x = x0 + (x1 - x0) * (((i * 5) % count) / (count - 1f))
        val y = y0 + (y1 - y0) * p
        drawLine(
            Color.White.copy(alpha = 0.35f * sin(p * PI.toFloat())),
            at(x, y),
            at(x - 0.004f, y + 0.035f),
            strokeWidth = side * 0.003f,
        )
    }

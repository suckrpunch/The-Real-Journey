package com.newjourney.feature.meditate

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import com.newjourney.core.designsystem.component.isNightTheme
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * The mark above "Well done": a soft circle with a check, and five short rays like a rising sun.
 * It plays once, calmly: the circle appears, the check draws itself, the rays reach outwards one
 * after another. Afterwards the rays glow very gently. Still when animations are off on the phone.
 */
@Composable
internal fun CompletionMark(modifier: Modifier = Modifier) {
    val night = isNightTheme()
    val colors = MaterialTheme.colorScheme
    // Day: pale sage circle with a green check. Night: cream circle with a navy check.
    val circleColor = if (night) colors.primary else colors.primaryContainer
    val checkColor = if (night) colors.onPrimary else colors.primary
    val rayColor = colors.primary

    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val start = if (reduceMotion) 1f else 0f
    val circle = remember { Animatable(start) }
    val check = remember { Animatable(start) }
    val rays = remember { List(RAYS) { Animatable(start) } }

    LaunchedEffect(Unit) {
        if (reduceMotion) return@LaunchedEffect
        circle.animateTo(1f, tween(550, easing = FastOutSlowInEasing))
        check.animateTo(1f, tween(450, easing = FastOutSlowInEasing))
        rays.mapIndexed { i, ray ->
            async {
                delay(i * 90L)
                ray.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
            }
        }.awaitAll()
    }

    // After it has played, the rays breathe very softly.
    val glow = if (reduceMotion) {
        1f
    } else {
        val pulse by rememberInfiniteTransition(label = "glow").animateFloat(
            initialValue = 1f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(tween(2_400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "glowAlpha",
        )
        if (rays.last().value >= 1f) pulse else 1f
    }

    Canvas(modifier) {
        val d = size.minDimension
        val r = d * 0.2f
        val c = Offset(size.width / 2f, size.height * 0.62f)

        // Circle: grows in softly.
        drawCircle(circleColor.copy(alpha = circleColor.alpha * circle.value), radius = r * (0.7f + 0.3f * circle.value), center = c)

        // Check: drawn from its first stroke to its last.
        if (check.value > 0f) {
            val full = Path().apply {
                moveTo(c.x - r * 0.42f, c.y + r * 0.02f)
                lineTo(c.x - r * 0.1f, c.y + r * 0.34f)
                lineTo(c.x + r * 0.46f, c.y - r * 0.3f)
            }
            val measure = PathMeasure().apply { setPath(full, false) }
            val part = Path()
            measure.getSegment(0f, measure.length * check.value, part, true)
            drawPath(part, checkColor, style = Stroke(width = d * 0.035f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // Rays: five short lines fanning above the circle, reaching outwards in turn.
        rays.forEachIndexed { i, ray ->
            val p = ray.value
            if (p <= 0f) return@forEachIndexed
            val angle = Math.toRadians(-150.0 + i * 30.0).toFloat()
            val inner = r * 1.45f
            val outer = inner + r * 0.55f * p
            drawLine(
                rayColor.copy(alpha = rayColor.alpha * p * glow),
                Offset(c.x + cos(angle) * inner, c.y + sin(angle) * inner),
                Offset(c.x + cos(angle) * outer, c.y + sin(angle) * outer),
                strokeWidth = d * 0.022f,
                cap = StrokeCap.Round,
            )
        }
    }
}

private const val RAYS = 5

package com.newjourney.feature.meditate

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.newjourney.core.designsystem.theme.NewJourneyTheme

/**
 * Three concentric circles that breathe with you: they swell over 4 seconds (breathe in),
 * rest for a moment at the top, and settle over 6 seconds (breathe out). The inner circle
 * moves most and leads; the outer rings follow a fraction later, like a soft ripple.
 * Everything stays still when [moving] is false or the phone's "Remove animations" setting is on.
 */
@Composable
internal fun BreathingCircle(moving: Boolean, modifier: Modifier = Modifier, size: Dp = 240.dp) {
    val reduceMotion = rememberReduceMotion()
    val cycle = if (moving && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "breathing")
        val t by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(CYCLE_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "breathCycle",
        )
        t
    } else {
        null
    }
    // 0 = fully out, 1 = fully in. Paused or still: a calm middle position.
    fun breath(lag: Float): Float = cycle?.let { breathAt((it - lag + 1f) % 1f) } ?: 0.55f
    val inner = breath(0f)
    val middle = breath(0.025f)
    val outer = breath(0.05f)

    val colors = NewJourneyTheme.extraColors
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .scale(0.9f + 0.1f * outer)
                .background(colors.breathOuter, CircleShape),
        )
        Box(
            Modifier
                .size(size * 0.7f)
                .scale(0.74f + 0.26f * middle)
                .background(colors.breathMiddle, CircleShape),
        )
        Box(
            Modifier
                .size(size * 0.38f)
                .scale(0.66f + 0.34f * inner)
                .background(colors.breathInner, CircleShape),
        )
    }
}

private const val CYCLE_MS = 11_000
private const val IN_END = 4f / 11f
private const val HOLD_END = 5f / 11f

/** Where the breath is at point [t] (0..1) of the cycle: in for 4 s, hold 1 s, out for 6 s. */
private fun breathAt(t: Float): Float = when {
    t < IN_END -> FastOutSlowInEasing.transform(t / IN_END)
    t < HOLD_END -> 1f
    else -> 1f - FastOutSlowInEasing.transform((t - HOLD_END) / (1f - HOLD_END))
}

@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

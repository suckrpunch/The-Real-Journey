package com.newjourney.feature.meditate

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
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
 * A soft circle that grows over 4 seconds (breathe in) and settles over 6 seconds (breathe out).
 * It stays still when [moving] is false, or when the phone's "Remove animations" setting is on.
 */
@Composable
internal fun BreathingCircle(moving: Boolean, modifier: Modifier = Modifier, size: Dp = 240.dp) {
    val reduceMotion = rememberReduceMotion()
    val scale = if (moving && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "breathing")
        val animated by transition.animateFloat(
            initialValue = REST,
            targetValue = REST,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 10_000
                    REST at 0 using FastOutSlowInEasing
                    1.0f at 4_000 using FastOutSlowInEasing
                    REST at 10_000 using LinearEasing
                },
                repeatMode = RepeatMode.Restart,
            ),
            label = "breathingScale",
        )
        animated
    } else {
        0.85f
    }
    // Three calm, concentric circles: the outer ring stays still, the inner two breathe.
    val colors = NewJourneyTheme.extraColors
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(Modifier.size(size).background(colors.breathOuter, CircleShape))
        Box(
            Modifier
                .size(size * 0.7f)
                .scale(0.8f + 0.2f * scale)
                .background(colors.breathMiddle, CircleShape),
        )
        Box(
            Modifier
                .size(size * 0.38f)
                .scale(0.85f + 0.15f * scale)
                .background(colors.breathInner, CircleShape),
        )
    }
}

private const val REST = 0.7f

@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

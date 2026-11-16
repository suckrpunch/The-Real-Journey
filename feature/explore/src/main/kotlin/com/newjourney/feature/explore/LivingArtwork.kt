package com.newjourney.feature.explore

import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import com.newjourney.core.designsystem.component.TrackArtwork
import kotlin.math.PI
import kotlin.math.sin

/**
 * The big cover on a track's page, slowly alive: it drifts and zooms very gently inside its frame,
 * like a calm camera. While the music plays the movement is fuller; when it stops, the cover settles
 * to a whisper of motion. Stays still when animations are turned off on the phone.
 *
 * [modifier] should set the size and clip the frame; the image moves inside it.
 */
@Composable
internal fun LivingArtwork(
    coverId: String,
    colorHex: String?,
    playing: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    if (reduceMotion) {
        TrackArtwork(colorHex, modifier, coverId = coverId)
        return
    }

    // How much it moves: full while playing, a little at rest. Changes over two seconds, never jumps.
    val amount by animateFloatAsState(
        targetValue = if (playing) 1f else 0.35f,
        animationSpec = tween(2_000, easing = FastOutSlowInEasing),
        label = "artworkAmount",
    )
    // Two slow cycles of different lengths, so the movement never visibly repeats.
    val transition = rememberInfiniteTransition(label = "artwork")
    val zoom by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(ZOOM_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "zoom",
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(DRIFT_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "drift",
    )

    Box(modifier) {
        TrackArtwork(
            colorHex,
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val wave = (1f - kotlin.math.cos(zoom * 2f * PI.toFloat())) / 2f // 0 → 1 → 0, eased
                    val angle = drift * 2f * PI.toFloat()
                    // Always zoomed in a little more than it drifts, so the edges never show.
                    val scale = 1f + amount * (0.04f + 0.05f * wave)
                    scaleX = scale
                    scaleY = scale
                    translationX = amount * size.width * 0.015f * sin(angle)
                    translationY = amount * size.height * 0.012f * sin(angle * 2f)
                },
            coverId = coverId,
        )
    }
}

private const val ZOOM_MS = 16_000
private const val DRIFT_MS = 26_000

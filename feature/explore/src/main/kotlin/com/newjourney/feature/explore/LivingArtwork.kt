package com.newjourney.feature.explore

import com.newjourney.core.designsystem.living.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.withTransform
import com.newjourney.core.designsystem.component.TrackArtwork
import com.newjourney.core.designsystem.component.rememberCover
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The big cover on a track's page, slowly alive. The picture drifts and zooms very gently inside its
 * frame, like a calm camera, fuller while the music plays. On top, each cover has its own quiet life
 * (see [trackScene]): steam from the coffee, sunlight that shifts, leaves that sway, stars that
 * twinkle. Stays still when animations are turned off on the phone.
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
    val cover = rememberCover(coverId)
    val reduceMotion = rememberReduceMotion()
    if (cover == null || reduceMotion) {
        TrackArtwork(colorHex, modifier, coverId = coverId)
        return
    }

    // How much the camera moves: full while playing, a little at rest. Changes over two seconds.
    val amount by animateFloatAsState(
        targetValue = if (playing) 1f else 0.35f,
        animationSpec = tween(2_000, easing = FastOutSlowInEasing),
        label = "artworkAmount",
    )
    val seconds by rememberSeconds(running = true)
    val scene = remember(coverId) { trackScene(coverId) }

    Canvas(modifier) {
        val t = seconds
        val wave = (1f - cos(t / ZOOM_S * 2f * PI.toFloat())) / 2f
        val angle = t / DRIFT_S * 2f * PI.toFloat()
        // Always zoomed in a little more than it drifts, so the edges never show.
        val scale = 1f + amount * (0.04f + 0.05f * wave)
        withTransform({
            scale(scale, scale, pivot = center)
            translate(amount * size.width * 0.015f * sin(angle), amount * size.height * 0.012f * sin(angle * 2f))
        }) {
            drawLivingCover(cover, scene, t, animate = true)
        }
    }
}

private const val ZOOM_S = 16f
private const val DRIFT_S = 26f

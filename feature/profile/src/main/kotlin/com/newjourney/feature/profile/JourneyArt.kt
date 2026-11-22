package com.newjourney.feature.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.newjourney.core.designsystem.component.rememberCover
import com.newjourney.core.designsystem.living.MOONLIGHT
import com.newjourney.core.designsystem.living.Motion
import com.newjourney.core.designsystem.living.Patch
import com.newjourney.core.designsystem.living.SUNLIGHT
import com.newjourney.core.designsystem.living.Scene
import com.newjourney.core.designsystem.living.breathingGlow
import com.newjourney.core.designsystem.living.drawLivingCover
import com.newjourney.core.designsystem.living.glints
import com.newjourney.core.designsystem.living.phase
import com.newjourney.core.designsystem.living.rays
import com.newjourney.core.designsystem.living.rememberReduceMotion
import com.newjourney.core.designsystem.living.rememberSeconds
import com.newjourney.core.designsystem.living.ripples
import com.newjourney.core.designsystem.living.risingMotes
import com.newjourney.core.designsystem.living.stars
import com.newjourney.core.designsystem.living.sway
import com.newjourney.core.designsystem.living.travel
import com.newjourney.core.designsystem.living.twinkle
import com.newjourney.core.model.JourneyStage
import com.newjourney.core.model.Milestone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * The artwork of The Journey Within: one illustration per stage, gently alive on its own page, and
 * one badge per milestone. What is still ahead is shown softly faded, never hidden.
 */

private fun stageCover(stage: JourneyStage) = "stage-" + stage.name.lowercase().replace('_', '-')
private fun badgeCover(milestone: Milestone) = "milestone-" + milestone.name.lowercase().replace('_', '-')

/** Grey and faded, for what is still ahead. */
private val Ahead = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.15f) })

/** A stage's picture in a small circle, ringed in its colour; faded while the stage is ahead. */
@Composable
internal fun StageThumbnail(stage: JourneyStage, arrived: Boolean, ring: Color, size: Dp, modifier: Modifier = Modifier) {
    val cover = rememberCover(stageCover(stage))
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, if (arrived) ring else ring.copy(alpha = 0.45f), CircleShape),
    ) {
        if (cover != null) {
            Image(
                cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = if (arrived) null else Ahead,
                modifier = Modifier
                    .size(size)
                    .alpha(if (arrived) 1f else 0.55f),
            )
        }
    }
}

/**
 * A stage's illustration on its page, slowly alive: a calm drift and zoom, and its own quiet detail
 * (the sprout sways, light flows into the roots, a light walks the path, the doorway glows). Faded
 * and still while the stage is ahead. Still when animations are off on the phone.
 */
@Composable
internal fun LivingStageArt(stage: JourneyStage, arrived: Boolean, modifier: Modifier = Modifier) {
    val cover = rememberCover(stageCover(stage)) ?: return Box(modifier)
    val reduceMotion = rememberReduceMotion()
    val animate = arrived && !reduceMotion
    val seconds by rememberSeconds(animate)
    val scene = remember(stage) { stageScene(stage) }
    Canvas(modifier.alpha(if (arrived) 1f else 0.6f)) {
        val t = seconds
        val wave = (1f - cos(t * 2f * PI.toFloat() / 18f)) / 2f
        val zoom = if (animate) 1.03f + 0.025f * wave else 1f
        val driftX = if (animate) size.width * 0.01f * sin(t * 2f * PI.toFloat() / 28f) else 0f
        withTransform({
            scale(zoom, zoom, pivot = center)
            translate(left = driftX)
        }) {
            drawLivingCover(cover, scene, t, animate = animate)
            if (!arrived) drawRect(Color.White.copy(alpha = 0.25f))
        }
    }
}

/**
 * A milestone's badge. Earned badges catch the light now and then (each at its own moment);
 * badges still ahead are shown softly faded.
 */
@Composable
internal fun MilestoneBadge(milestone: Milestone, earned: Boolean, size: Dp, modifier: Modifier = Modifier) {
    val cover = rememberCover(badgeCover(milestone))
    val reduceMotion = rememberReduceMotion()
    val shine = earned && !reduceMotion
    val seconds by rememberSeconds(shine)
    Box(modifier.size(size).clip(CircleShape)) {
        if (cover != null) {
            Image(
                cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = if (earned) null else Ahead,
                modifier = Modifier
                    .size(size)
                    .alpha(if (earned) 1f else 0.5f),
            )
        }
        if (shine) {
            Canvas(Modifier.size(size)) {
                // A soft band of light passes across, then rests for a while.
                val p = phase(seconds, period = 9f, offset = milestone.ordinal * 0.17f)
                if (p < 0.25f) {
                    val q = p / 0.25f
                    val w = this.size.width
                    val x = -w + q * w * 3f
                    drawRect(
                        Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.35f * sin(q * PI.toFloat())), Color.Transparent),
                            Offset(x, 0f),
                            Offset(x + w * 0.7f, this.size.height),
                        ),
                    )
                }
            }
        }
    }
}

private fun swaying(x: Float, y: Float, r: Float, px: Float, py: Float, degrees: Float, period: Float, offset: Float = 0f) =
    Patch(x, y, r) { t -> Motion(degrees = degrees * sway(t, period, offset), pivotX = px, pivotY = py) }

/** Each stage's own quiet life, matched to its illustration (positions are fractions of the picture). */
private fun stageScene(stage: JourneyStage): Scene = when (stage) {
    JourneyStage.AWAKENING -> Scene(
        focusY = 0.52f,
        patches = listOf(swaying(0.5f, 0.6f, 0.08f, 0.5f, 0.68f, 3f, 5f)),
    ) { t, side, at ->
        breathingGlow(at, side, t, 0.62f, 0.42f, 0.16f, SUNLIGHT, 0.14f, 0.3f)
        breathingGlow(at, side, t, 0.5f, 0.62f, 0.09f, Color(0xFFFFF6C8), 0.08f, 0.24f, period = 5f)
        travel(at, side, t, listOf(0.6f to 0.62f, 0.66f to 0.56f, 0.6f to 0.52f, 0.68f to 0.47f), Color(0xFFFFF4D6), period = 8f, size = 0.018f, strength = 0.45f)
        risingMotes(at, side, t, centerX = 0.5f, fromY = 0.66f, toY = 0.45f, color = Color(0xFFFFF8E0), spread = 0.08f, count = 5)
    }
    JourneyStage.FOUNDATION -> Scene(
        focusY = 0.55f,
        patches = listOf(
            swaying(0.22f, 0.68f, 0.07f, 0.22f, 0.72f, 3f, 5f),
            swaying(0.75f, 0.68f, 0.07f, 0.75f, 0.72f, 3f, 6f, 0.4f),
        ),
    ) { t, side, at ->
        breathingGlow(at, side, t, 0.65f, 0.41f, 0.16f, SUNLIGHT, 0.14f, 0.3f)
        // Light flowing down into the roots, steadying the stones.
        travel(at, side, t, listOf(0.5f to 0.7f, 0.5f to 0.8f, 0.49f to 0.9f, 0.5f to 0.97f), Color(0xFFFFF0B0), period = 6f, size = 0.022f, strength = 0.5f)
        travel(at, side, t, listOf(0.5f to 0.72f, 0.42f to 0.8f, 0.33f to 0.86f), Color(0xFFFFF0B0), period = 6f, size = 0.016f, strength = 0.35f, offset = 0.15f)
        travel(at, side, t, listOf(0.5f to 0.72f, 0.58f to 0.8f, 0.67f to 0.86f), Color(0xFFFFF0B0), period = 6f, size = 0.016f, strength = 0.35f, offset = 0.3f)
    }
    JourneyStage.INNER_BALANCE -> Scene(
        focusY = 0.55f,
        patches = listOf(swaying(0.1f, 0.6f, 0.1f, 0.1f, 0.68f, 2.5f, 5f)),
    ) { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.52f, 0.14f, Color(0xFFFFFBEA), 0.06f, 0.16f, period = 7f)
        ripples(at, side, t, 0.5f, 0.69f, 0.08f, 0.4f, Color.White, flat = 0.18f, period = 8f)
        glints(at, side, t, 0.5f, 0.76f, 0.2f, 0.08f, Color.White, count = 5)
    }
    JourneyStage.PATH_OF_AWARENESS -> Scene(
        focusY = 0.6f,
        patches = listOf(
            swaying(0.2f, 0.78f, 0.09f, 0.2f, 0.88f, 3f, 5f),
            swaying(0.72f, 0.73f, 0.07f, 0.72f, 0.8f, 3f, 6f, 0.3f),
        ),
    ) { t, side, at ->
        breathingGlow(at, side, t, 0.62f, 0.45f, 0.16f, SUNLIGHT, 0.14f, 0.3f)
        // A light walking the path towards the sun.
        travel(
            at, side, t,
            listOf(0.62f to 1.0f, 0.55f to 0.88f, 0.35f to 0.8f, 0.38f to 0.72f, 0.55f to 0.68f, 0.68f to 0.62f, 0.6f to 0.56f, 0.62f to 0.5f),
            Color(0xFFFFF6DC), period = 10f, size = 0.022f, strength = 0.5f,
        )
    }
    JourneyStage.DEEPER_SELF -> Scene(
        focusY = 0.5f,
        patches = listOf(
            swaying(0.17f, 0.68f, 0.12f, 0.17f, 0.82f, 2.5f, 6f),
            swaying(0.78f, 0.7f, 0.12f, 0.77f, 0.85f, 2.5f, 7f, 0.4f),
        ),
    ) { t, side, at ->
        breathingGlow(at, side, t, 0.64f, 0.29f, 0.1f, SUNLIGHT, 0.12f, 0.26f)
        breathingGlow(at, side, t, 0.5f, 0.39f, 0.1f, Color(0xFFFFF4E8), 0.14f, 0.34f, period = 5f)
        ripples(at, side, t, 0.5f, 0.43f, 0.06f, 0.36f, Color.White, flat = 0.16f, period = 7f)
        rays(at, side, t, 0.5f, 0.47f, angle = 90f, spread = 40f, length = 0.45f, count = 5, color = Color(0xFFFFF4D0), strength = 0.12f)
        risingMotes(at, side, t, centerX = 0.5f, fromY = 0.88f, toY = 0.5f, color = Color(0xFFFFE9A8), spread = 0.2f)
    }
    JourneyStage.INNER_SANCTUARY -> Scene(
        focusY = 0.45f,
        patches = listOf(
            swaying(0.27f, 0.35f, 0.2f, 0.27f, 0.6f, 0.8f, 7f),
            swaying(0.72f, 0.42f, 0.15f, 0.72f, 0.6f, 0.8f, 8f, 0.35f),
        ),
    ) { t, side, at ->
        // The doorway's warm light breathes; the moon glows; stars twinkle; fireflies drift.
        breathingGlow(at, side, t, 0.5f, 0.53f, 0.12f, Color(0xFFFFE6A0), 0.18f, 0.4f, period = 6f)
        breathingGlow(at, side, t, 0.74f, 0.17f, 0.07f, MOONLIGHT, 0.14f, 0.3f)
        stars(at, side, t, listOf(0.17f to 0.16f, 0.42f to 0.17f, 0.5f to 0.09f, 0.6f to 0.2f, 0.79f to 0.12f, 0.86f to 0.15f, 0.66f to 0.32f, 0.88f to 0.24f), r = 0.008f)
        risingMotes(at, side, t, centerX = 0.5f, fromY = 0.72f, toY = 0.56f, color = Color(0xFFFFF0B0), spread = 0.22f, count = 6)
        twinkle(at(0.34f, 0.71f), side * 0.008f, t, 7, Color(0xFFFFF0B0))
    }
}

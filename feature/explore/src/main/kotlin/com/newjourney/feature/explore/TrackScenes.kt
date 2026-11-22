package com.newjourney.feature.explore

import com.newjourney.core.designsystem.living.*
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Each track cover's own quiet life on the player screen, matched to its artwork: steam from the
 * coffee, sunlight through the window, leaves that sway, stars that twinkle, a panda that breathes.
 * Positions are fractions of the square cover, measured from the artwork.
 */

/** A part of the picture that sways (leaves, grass): turns back and forth around [px],[py]. */
private fun swaying(x: Float, y: Float, r: Float, px: Float, py: Float, degrees: Float, period: Float, offset: Float = 0f) =
    Patch(x, y, r) { t -> Motion(degrees = degrees * sway(t, period, offset), pivotX = px, pivotY = py) }

/** A part that drifts sideways and back (a cloud). */
private fun drifting(x: Float, y: Float, r: Float, dx: Float, period: Float) =
    Patch(x, y, r) { t -> Motion(dx = dx * sway(t, period), pivotX = x, pivotY = y) }

/** A part that rocks gently on water (a boat). */
private fun bobbing(x: Float, y: Float, r: Float) =
    Patch(x, y, r) { t -> Motion(dy = 0.005f * sway(t, 4.5f), degrees = 0.7f * sway(t, 6f, 0.2f), pivotX = x, pivotY = y) }

/** A part that slowly breathes, growing and settling (a sleeping panda). */
private fun breathing(x: Float, y: Float, r: Float, px: Float, py: Float, amount: Float, period: Float) =
    Patch(x, y, r) { t -> Motion(scale = 1f + amount * breathe(t, period), pivotX = px, pivotY = py) }

internal fun trackScene(trackId: String): Scene = when (trackId) {
    // ---- The Chakra Collection ----
    "chakra-root-grounding" -> Scene { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.4f, 0.24f, Color(0xFFF6C9B8), 0.10f, 0.22f)
        breathingGlow(at, side, t, 0.5f, 0.53f, 0.12f, Color(0xFFE86A50), 0.04f, 0.14f, period = 5f)
        twinkle(at(0.5f, 0.21f), side * 0.015f, t, 1)
        // Energy flowing down into the roots.
        travel(at, side, t, listOf(0.5f to 0.6f, 0.5f to 0.72f, 0.49f to 0.83f, 0.5f to 0.95f), Color(0xFFFFB39E), period = 6f, size = 0.025f, strength = 0.5f)
    }
    "chakra-sacral-stillness" -> Scene { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.37f, 0.13f, Color(0xFFFFC27A), 0.15f, 0.32f)
        breathingGlow(at, side, t, 0.5f, 0.62f, 0.14f, Color(0xFFFFB066), 0.05f, 0.15f, period = 5f, offset = 0.3f)
        ripples(at, side, t, 0.5f, 0.73f, 0.06f, 0.42f, Color.White, flat = 0.22f)
    }
    "chakra-solar-plexus-activation" -> Scene { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.32f, 0.3f, Color(0xFFFFD27A), 0.10f, 0.24f, period = 7f)
        // The flame flickers softly.
        val flicker = 0.5f + 0.25f * sin(t * 5.1f) + 0.15f * sin(t * 8.3f + 1f) + 0.1f * sin(t * 13.7f)
        glow(at(0.5f, 0.38f), side * 0.07f, Color(0xFFFFF2C0), 0.12f + 0.22f * flicker)
        breathingGlow(at, side, t, 0.5f, 0.6f, 0.14f, Color(0xFFFFE08A), 0.05f, 0.14f, period = 5f, offset = 0.4f)
    }
    "chakra-heart-cleansing" -> Scene(
        patches = listOf(swaying(0.12f, 0.1f, 0.2f, 0.0f, 0.02f, 1.4f, 6f)),
    ) { t, side, at ->
        rain(at, side, t, 0.3f, 0.92f, 0.0f, 0.35f)
        twinkle(at(0.07f, 0.27f), side * 0.008f, t, 2)
        twinkle(at(0.18f, 0.29f), side * 0.008f, t, 5)
        breathingGlow(at, side, t, 0.5f, 0.42f, 0.12f, Color(0xFFEFF8D2), 0.12f, 0.3f)
        glints(at, side, t, 0.5f, 0.78f, 0.2f, 0.08f)
    }
    "chakra-throat-circle" -> Scene { t, side, at ->
        ripples(at, side, t, 0.5f, 0.5f, 0.12f, 0.46f, Color.White, strength = 0.28f, period = 8f)
        ripples(at, side, t, 0.5f, 0.82f, 0.05f, 0.38f, Color.White, flat = 0.18f, period = 6f, count = 2)
        breathingGlow(at, side, t, 0.5f, 0.57f, 0.12f, Color(0xFFB8E2F5), 0.08f, 0.2f)
    }
    "chakra-third-eye-focus" -> Scene { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.43f, 0.2f, Color(0xFFD5CCF5), 0.06f, 0.18f, period = 7f)
        twinkle(at(0.5f, 0.45f), side * 0.02f, t, 0)
        stars(at, side, t, listOf(0.36f to 0.41f, 0.43f to 0.47f, 0.6f to 0.4f, 0.68f to 0.43f, 0.56f to 0.5f, 0.39f to 0.5f), r = 0.006f)
        glints(at, side, t, 0.5f, 0.78f, 0.25f, 0.06f, Color(0xFFE9E2FF))
    }
    "chakra-crown-bliss" -> Scene { t, side, at ->
        rays(at, side, t, 0.5f, -0.05f, angle = 90f, spread = 14f, length = 0.75f, count = 3, color = Color(0xFFFFF8E6), strength = 0.18f)
        risingMotes(at, side, t, centerX = 0.5f, fromY = 0.65f, toY = 0.1f, color = Color.White, spread = 0.08f)
        breathingGlow(at, side, t, 0.5f, 0.68f, 0.16f, Color(0xFFEEDDF8), 0.08f, 0.2f)
    }

    // ---- Sleep ----
    "deep-sleep-drifter" -> Scene(patches = listOf(bobbing(0.54f, 0.81f, 0.13f))) { t, side, at ->
        breathingGlow(at, side, t, 0.74f, 0.38f, 0.13f, MOONLIGHT, 0.10f, 0.22f)
        stars(at, side, t, listOf(0.15f to 0.15f, 0.25f to 0.32f, 0.42f to 0.12f, 0.55f to 0.25f, 0.88f to 0.2f, 0.07f to 0.38f, 0.95f to 0.43f))
        breathingGlow(at, side, t, 0.54f, 0.78f, 0.05f, Color(0xFFFFE7A8), 0.15f, 0.35f, period = 4f)
        glints(at, side, t, 0.52f, 0.86f, 0.08f, 0.08f, MOONLIGHT)
    }
    "sleeping-panda-dream" -> Scene(
        patches = listOf(
            breathing(0.47f, 0.82f, 0.3f, 0.47f, 0.98f, 0.012f, 4.5f),
            swaying(0.12f, 0.15f, 0.22f, 0.05f, 0.0f, 1.2f, 7f),
            swaying(0.87f, 0.15f, 0.2f, 0.95f, 0.0f, 1.0f, 8f, 0.4f),
        ),
    ) { t, side, at ->
        breathingGlow(at, side, t, 0.83f, 0.66f, 0.22f, Color(0xFFFFE3A0), 0.08f, 0.18f, period = 7f)
        stars(at, side, t, listOf(0.35f to 0.12f, 0.55f to 0.2f, 0.7f to 0.1f, 0.6f to 0.35f))
    }
    "partical-heaven-zone" -> Scene { t, side, at ->
        stars(
            at, side, t,
            listOf(0.68f to 0.08f, 0.81f to 0.21f, 0.34f to 0.3f, 0.75f to 0.34f, 0.52f to 0.47f, 0.65f to 0.42f, 0.43f to 0.52f, 0.6f to 0.58f, 0.3f to 0.6f, 0.22f to 0.65f, 0.48f to 0.68f),
            r = 0.016f,
            color = Color(0xFFFFE2A8),
        )
        risingMotes(at, side, t, centerX = 0.5f, fromY = 0.8f, toY = 0.1f, color = Color(0xFFFFE9C0), spread = 0.25f)
    }
    "pluto-planet-calmth" -> Scene { t, side, at ->
        breathingGlow(at, side, t, 0.6f, 0.45f, 0.3f, Color(0xFFEADCE8), 0.06f, 0.16f, period = 8f)
        twinkle(at(0.15f, 0.47f), side * 0.012f, t, 4, Color(0xFFE6E0F0))
        stars(at, side, t, listOf(0.14f to 0.12f, 0.3f to 0.25f, 0.85f to 0.1f, 0.92f to 0.3f, 0.2f to 0.35f, 0.45f to 0.08f))
    }
    "universe-calling-home" -> Scene(
        patches = listOf(Patch(0.52f, 0.3f, 0.27f) { t -> Motion(degrees = 2f * sway(t, 14f), pivotX = 0.52f, pivotY = 0.3f) }),
    ) { t, side, at ->
        breathingGlow(at, side, t, 0.52f, 0.3f, 0.12f, Color(0xFFFFE2F0), 0.12f, 0.3f)
        stars(at, side, t, listOf(0.15f to 0.05f, 0.1f to 0.25f, 0.85f to 0.12f, 0.92f to 0.4f, 0.3f to 0.45f))
        // A light walking the path home, towards the horizon.
        travel(at, side, t, listOf(0.3f to 0.97f, 0.3f to 0.88f, 0.33f to 0.81f, 0.45f to 0.75f, 0.6f to 0.7f, 0.65f to 0.65f, 0.6f to 0.6f, 0.68f to 0.56f), Color(0xFFFFE6C8), period = 9f, size = 0.022f, strength = 0.55f)
    }

    // ---- Relaxation ----
    "relaxed-calm-mind" -> Scene(patches = listOf(swaying(0.08f, 0.87f, 0.17f, 0.08f, 1.0f, 1.6f, 5f))) { t, side, at ->
        breathingGlow(at, side, t, 0.52f, 0.33f, 0.13f, SUNLIGHT, 0.14f, 0.3f)
        glints(at, side, t, 0.52f, 0.64f, 0.1f, 0.16f)
    }
    "lay-back-and-chill" -> Scene(
        patches = listOf(
            swaying(0.3f, 0.1f, 0.25f, 0.35f, 0.35f, 0.8f, 7f),
            swaying(0.82f, 0.12f, 0.2f, 0.8f, 0.35f, 0.8f, 8f, 0.3f),
            swaying(0.5f, 0.62f, 0.3f, 0.5f, 0.45f, 0.9f, 5f),
        ),
    ) { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.3f, 0.2f, SUNLIGHT, 0.08f, 0.18f, period = 8f)
        dapple(at, side, t, 0.45f, 0.86f, 0.1f, Color(0xFFFFF6D0), 0.25f)
        dapple(at, side, t, 0.7f, 0.9f, 0.07f, Color(0xFFFFF6D0), 0.2f, period = 11f)
    }
    "sleep-on-the-keys" -> Scene { t, side, at ->
        // Keys softly lit one after another, like a slow melody.
        listOf(0.14f to 0.63f, 0.31f to 0.62f, 0.47f to 0.62f, 0.62f to 0.64f, 0.77f to 0.66f, 0.9f to 0.69f, 0.55f to 0.63f, 0.24f to 0.62f)
            .forEachIndexed { i, (x, y) ->
                val p = bump(phase(t, period = 6f, offset = i * 0.13f), width = 0.004f)
                glow(at(x, y), side * 0.05f, Color(0xFFFFF8E6), 0.45f * p)
            }
        sheen(t, period = 12f, strength = 0.08f)
    }
    "will-be-right-with-you" -> Scene { t, side, at ->
        breathingGlow(at, side, t, 0.32f, 0.4f, 0.24f, Color(0xFFFFC878), 0.06f, 0.18f, period = 7f)
        breathingGlow(at, side, t, 0.57f, 0.52f, 0.22f, Color(0xFFE6F0DC), 0.06f, 0.18f, period = 7f, offset = 0.5f)
        breathingGlow(at, side, t, 0.43f, 0.46f, 0.1f, Color.White, 0.04f, 0.14f, period = 3.5f)
    }
    "vibe-says-it-all" -> Scene { t, side, at ->
        breathingGlow(at, side, t, 0.85f, 0.25f, 0.2f, Color(0xFFFFD2A0), 0.05f, 0.15f, period = 7f)
        breathingGlow(at, side, t, 0.2f, 0.78f, 0.2f, Color(0xFFFFD2A0), 0.05f, 0.15f, period = 7f, offset = 0.5f)
        sheen(t, period = 9f, strength = 0.12f)
    }

    // ---- Focus ----
    "study-with-me" -> Scene(patches = listOf(swaying(0.3f, 0.33f, 0.17f, 0.33f, 0.5f, 1.1f, 6f))) { t, side, at ->
        rays(at, side, t, 1.02f, 0.05f, angle = 139f, spread = 22f, length = 1.0f, count = 4, color = Color(0xFFFFF3D0), strength = 0.14f)
        dapple(at, side, t, 0.55f, 0.2f, 0.12f, Color(0xFFFFF3D0), 0.18f, period = 16f)
        dapple(at, side, t, 0.62f, 0.83f, 0.1f, Color(0xFFFFF3D0), 0.2f, period = 12f)
        steam(at, side, t, 0.79f, 0.6f, 0.17f)
    }
    "eyes-on-the-road" -> Scene { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.43f, 0.14f, SUNLIGHT, 0.14f, 0.3f)
        // Light running down the road towards you.
        travel(at, side, t, listOf(0.5f to 0.63f, 0.5f to 0.78f, 0.5f to 1.0f), Color(0xFFFFF6DC), period = 4f, size = 0.03f, strength = 0.35f)
        risingMotes(at, side, t, centerX = 0.5f, fromY = 0.6f, toY = 0.3f, color = Color.White, spread = 0.3f, count = 5)
    }
    "delta-for-your-brain-waves" -> Scene { t, side, at ->
        travel(at, side, t, WAVE_A, Color.White, period = 7f, size = 0.025f, strength = 0.6f)
        travel(at, side, t, WAVE_B, Color.White, period = 7f, size = 0.022f, strength = 0.5f, offset = 0.5f)
    }
    "relax-my-friend" -> Scene(patches = listOf(drifting(0.68f, 0.38f, 0.3f, 0.014f, 16f))) { t, side, at ->
        breathingGlow(at, side, t, 0.7f, 0.25f, 0.25f, Color.White, 0.04f, 0.12f, period = 9f)
        sheen(t, period = 14f, strength = 0.06f)
    }
    "dream-team-vibe" -> Scene { t, side, at ->
        breathingGlow(at, side, t, 0.8f, 0.48f, 0.15f, Color(0xFFFFD49A), 0.08f, 0.22f, period = 6f)
        breathingGlow(at, side, t, 0.37f, 0.8f, 0.17f, Color(0xFFFFE2B0), 0.08f, 0.2f, period = 6f, offset = 0.5f)
        sheen(t, period = 11f, strength = 0.08f)
    }

    // ---- Deep meditation & healing ----
    "come-and-find-me" -> Scene(
        patches = listOf(
            swaying(0.2f, 0.12f, 0.2f, 0.2f, 0.3f, 0.9f, 7f),
            swaying(0.8f, 0.1f, 0.2f, 0.8f, 0.3f, 0.9f, 8f, 0.35f),
        ),
    ) { t, side, at ->
        breathingGlow(at, side, t, 0.52f, 0.5f, 0.2f, SUNLIGHT, 0.12f, 0.26f)
        rays(at, side, t, 0.52f, 0.45f, angle = 90f, spread = 60f, length = 0.5f, count = 5, color = SUNLIGHT, strength = 0.1f)
        risingMotes(at, side, t, centerX = 0.52f, fromY = 0.75f, toY = 0.3f, color = Color(0xFFFFF8E0), spread = 0.15f)
    }
    "deep-inside-restoration" -> Scene { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.55f, 0.12f, Color(0xFFFFF6D8), 0.2f, 0.4f, period = 5f)
        ripples(at, side, t, 0.5f, 0.55f, 0.08f, 0.48f, Color(0xFFF3F6E4), period = 8f)
    }
    "positive-alpha" -> Scene { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.6f, 0.22f, Color(0xFFFFF1C8), 0.12f, 0.28f)
        ripples(at, side, t, 0.5f, 0.6f, 0.16f, 0.5f, Color.White, strength = 0.22f, period = 9f)
        glints(at, side, t, 0.5f, 0.9f, 0.12f, 0.06f)
    }
    "void-dancer" -> Scene { t, side, at ->
        twinkle(at(0.768f, 0.328f), side * 0.03f, t, 0, Color(0xFFFFF4DC))
        travel(at, side, t, VOID_ARC, Color(0xFFFFF4DC), period = 6f, size = 0.02f, strength = 0.55f)
        stars(at, side, t, listOf(0.12f to 0.12f, 0.2f to 0.38f, 0.35f to 0.2f, 0.6f to 0.1f, 0.88f to 0.55f, 0.55f to 0.68f))
    }
    "focused-on-healing" -> Scene { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.58f, 0.16f, Color(0xFFFFF6C8), 0.12f, 0.3f)
        ripples(at, side, t, 0.5f, 0.73f, 0.08f, 0.45f, Color.White, flat = 0.2f)
        glints(at, side, t, 0.5f, 0.8f, 0.1f, 0.1f)
    }

    // ---- Healing & Energy ----
    "all-is-well" -> Scene(
        patches = listOf(
            swaying(0.36f, 0.75f, 0.1f, 0.3f, 0.82f, 2f, 6f),
            swaying(0.06f, 0.76f, 0.09f, 0.06f, 0.86f, 2.5f, 5f, 0.3f),
        ),
    ) { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.37f, 0.16f, SUNLIGHT, 0.14f, 0.3f)
        glints(at, side, t, 0.5f, 0.54f, 0.16f, 0.1f)
    }
    "buddas-belly" -> Scene(
        patches = listOf(
            breathing(0.5f, 0.56f, 0.08f, 0.5f, 0.61f, 0.015f, 5f),
            swaying(0.12f, 0.73f, 0.1f, 0.12f, 0.84f, 2.5f, 5f),
        ),
    ) { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.35f, 0.14f, SUNLIGHT, 0.1f, 0.24f, period = 7f)
        // A warm glow round the middle, breathing with the figure.
        breathingGlow(at, side, t, 0.5f, 0.57f, 0.09f, Color(0xFFFFD98A), 0.12f, 0.32f, period = 5f)
        stars(at, side, t, listOf(0.42f to 0.47f, 0.6f to 0.5f, 0.38f to 0.55f, 0.66f to 0.45f, 0.56f to 0.43f), r = 0.008f, color = Color(0xFFFFEBB0))
    }
    "closer-to-self" -> Scene(patches = listOf(swaying(0.5f, 0.42f, 0.22f, 0.5f, 0.82f, 1f, 7f))) { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.33f, 0.16f, Color(0xFFFFD6DA), 0.12f, 0.3f, period = 6f)
        ripples(at, side, t, 0.5f, 0.31f, 0.1f, 0.4f, Color.White, strength = 0.2f, period = 9f)
        risingMotes(at, side, t, centerX = 0.5f, fromY = 0.7f, toY = 0.2f, color = Color(0xFFFFF0F0), spread = 0.15f, count = 5)
    }
    "i-am-safe-here" -> Scene(
        patches = listOf(
            swaying(0.2f, 0.63f, 0.09f, 0.2f, 0.7f, 2f, 6f),
            swaying(0.82f, 0.62f, 0.09f, 0.82f, 0.7f, 2f, 7f, 0.4f),
        ),
    ) { t, side, at ->
        breathingGlow(at, side, t, 0.6f, 0.35f, 0.16f, Color(0xFFFFE2C0), 0.08f, 0.2f, period = 8f)
        // The warm light inside the shelter breathes; fireflies drift around it.
        breathingGlow(at, side, t, 0.5f, 0.6f, 0.12f, Color(0xFFFFD9A0), 0.16f, 0.36f, period = 5f)
        risingMotes(at, side, t, centerX = 0.5f, fromY = 0.68f, toY = 0.5f, color = Color(0xFFFFF0B0), spread = 0.2f, count = 6)
    }
    "nature-is-calling" -> Scene(patches = listOf(swaying(0.07f, 0.66f, 0.1f, 0.05f, 0.8f, 2.5f, 5f))) { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.38f, 0.14f, SUNLIGHT, 0.16f, 0.32f)
        rays(at, side, t, 0.5f, 0.4f, angle = -90f, spread = 110f, length = 0.4f, count = 5, color = SUNLIGHT, strength = 0.12f)
        // A light walking the path towards the sun.
        travel(at, side, t, listOf(0.6f to 1.0f, 0.66f to 0.82f, 0.42f to 0.73f, 0.48f to 0.63f, 0.56f to 0.56f, 0.52f to 0.48f), Color(0xFFFFF6DC), period = 9f, size = 0.022f, strength = 0.5f)
    }
    "remember-to-heal" -> Scene { t, side, at ->
        breathingGlow(at, side, t, 0.63f, 0.17f, 0.12f, SUNLIGHT, 0.1f, 0.22f, period = 7f)
        breathingGlow(at, side, t, 0.5f, 0.53f, 0.15f, Color(0xFFFFF0C8), 0.14f, 0.34f, period = 5f)
        ripples(at, side, t, 0.5f, 0.6f, 0.08f, 0.45f, Color.White, flat = 0.2f, period = 7f)
        glints(at, side, t, 0.5f, 0.68f, 0.12f, 0.12f)
    }
    "soften-your-gaze" -> Scene { t, side, at ->
        breathingGlow(at, side, t, 0.5f, 0.32f, 0.16f, SUNLIGHT, 0.1f, 0.22f, period = 8f)
        // Soft mist drifting slowly over the lake.
        dapple(at, side, t, 0.3f, 0.52f, 0.22f, Color.White, 0.16f, drift = 0.08f, period = 20f)
        dapple(at, side, t, 0.72f, 0.72f, 0.24f, Color.White, 0.14f, drift = 0.08f, period = 24f)
        glints(at, side, t, 0.5f, 0.66f, 0.06f, 0.16f)
    }
    else -> Scene { t, side, at -> breathingGlow(at, side, t, 0.5f, 0.45f, 0.3f, Color.White, 0.0f, 0.1f, period = 8f) }
}

// The two white lines in the brain-waves cover, traced from the artwork.
private val WAVE_A = (0..24).map { i ->
    val x = i / 24f
    x to 0.585f + 0.115f * sin(2f * PI.toFloat() * (x - 0.355f) / 0.9f)
}
private val WAVE_B = (0..24).map { i ->
    val x = 1f - i / 24f
    x to 0.47f - 0.08f * cos(2f * PI.toFloat() * (x - 0.5f))
}

// The comet trail rising to the star in the void-dancer cover.
private val VOID_ARC = listOf(
    0.02f to 0.77f, 0.1f to 0.705f, 0.17f to 0.646f, 0.25f to 0.594f, 0.32f to 0.545f,
    0.4f to 0.5f, 0.47f to 0.459f, 0.55f to 0.422f, 0.62f to 0.389f, 0.7f to 0.357f, 0.768f to 0.328f,
)

package com.newjourney.feature.meditate

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.model.TimerLimits
import kotlinx.serialization.Serializable

/** Opens the Meditate tab, optionally with a duration chosen on Home. */
@Serializable
data class MeditateRoute(val minutes: Int = TimerLimits.DEFAULT_MINUTES)

fun NavGraphBuilder.meditateScreen() {
    composable<MeditateRoute> { entry ->
        MeditateScreen(minutes = entry.toRoute<MeditateRoute>().minutes)
    }
}

/**
 * Phase 1 starter: shows the chosen duration and the breathing circle.
 * The working timer (bells, background sound, lock-screen playback) is built next.
 */
@Composable
internal fun MeditateScreen(minutes: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        SectionTitle(stringResource(R.string.meditate_title), Modifier.align(Alignment.Start))
        BreathingCircle()
        Text(
            stringResource(R.string.meditate_ready, minutes),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.meditate_coming_next),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Expands over 4 seconds (breathe in) and contracts over 6 seconds (breathe out). */
@Composable
private fun BreathingCircle() {
    val transition = rememberInfiniteTransition(label = "breathing")
    val scale by transition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 10_000
                0.7f at 0 using FastOutSlowInEasing
                1.0f at 4_000 using FastOutSlowInEasing
                0.7f at 10_000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "breathingScale",
    )
    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(240.dp)
                .scale(scale)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
        )
        Box(
            Modifier
                .size(120.dp)
                .scale(scale)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape),
        )
    }
}

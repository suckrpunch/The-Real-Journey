package com.newjourney.feature.onboarding

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/**
 * The introduction: name, age range, goals, then a few suggestions to listen to.
 * Shown once on first launch; with [editing] it is opened from Profile to change the answers.
 */
@Serializable
data class OnboardingRoute(val editing: Boolean = false)

fun NavGraphBuilder.onboardingScreen(
    /** The introduction is over (finished, skipped, or the answers were changed). */
    onFinished: () -> Unit,
    /** Ends the introduction and opens a suggested track. */
    onOpenTrack: (trackId: String) -> Unit,
    /** Ends the introduction and starts a timer session of this length. */
    onStartMeditation: (minutes: Int) -> Unit,
) {
    composable<OnboardingRoute> {
        OnboardingRouteContent(
            onFinished = onFinished,
            onOpenTrack = onOpenTrack,
            onStartMeditation = onStartMeditation,
        )
    }
}

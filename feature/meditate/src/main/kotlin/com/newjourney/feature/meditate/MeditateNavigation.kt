package com.newjourney.feature.meditate

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.newjourney.core.model.TimerLimits
import kotlinx.serialization.Serializable

/** The Meditate tab: choose length, bells and sound. [minutes] pre-selects a length. */
@Serializable
data class MeditateRoute(val minutes: Int = TimerLimits.DEFAULT_MINUTES)

/**
 * The full-screen timer. With [autoStart] it starts a [minutes] session using the saved
 * bells and sound; without it, it shows the session that is already running.
 * With [trackId], that track loops seamlessly for the whole session instead of the background sound.
 */
@Serializable
data class MeditationSessionRoute(
    val minutes: Int = TimerLimits.DEFAULT_MINUTES,
    val autoStart: Boolean = true,
    val trackId: String? = null,
)

fun NavGraphBuilder.meditateScreen(
    onStartSession: (minutes: Int) -> Unit,
    onReturnToSession: () -> Unit,
) {
    composable<MeditateRoute> {
        MeditateSetupRoute(onStartSession = onStartSession, onReturnToSession = onReturnToSession)
    }
}

fun NavGraphBuilder.meditationSessionScreen(
    onClose: () -> Unit,
    /** Opens a journal entry for the session that just ended, with its length in minutes. */
    onOpenJournal: (sessionMinutes: Int) -> Unit,
    /** Opens a journey stage reached by the session that just ended. */
    onOpenStage: (stage: String) -> Unit,
) {
    composable<MeditationSessionRoute> {
        MeditationSessionRouteContent(onClose = onClose, onOpenJournal = onOpenJournal, onOpenStage = onOpenStage)
    }
}

package com.newjourney.feature.profile

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.newjourney.core.model.JourneyStage
import kotlinx.serialization.Serializable

/** The Profile tab: progress summary, daily goal, reminders and appearance. */
@Serializable
data object ProfileRoute

/** Full progress: totals, the week, a calendar of practice and milestones. */
@Serializable
data object ProgressRoute

/** Reminder choices. */
@Serializable
data object RemindersRoute

/** The Journey Within: the path of six stages. */
@Serializable
data object JourneyRoute

/** One stage of the journey, by its name (for example "FOUNDATION"). */
@Serializable
data class StageRoute(val stage: String)

/** What Premium offers, and subscribing through Google Play. Reachable from anywhere in the app. */
@Serializable
data object PremiumRoute

fun NavGraphBuilder.profileScreens(
    onOpenProgress: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenJourney: () -> Unit,
    onOpenStage: (stage: String) -> Unit,
    onOpenPremium: () -> Unit,
    onEditAboutYou: () -> Unit,
    onBack: () -> Unit,
) {
    composable<ProfileRoute> {
        ProfileRouteContent(
            onOpenProgress = onOpenProgress,
            onOpenReminders = onOpenReminders,
            onOpenJourney = onOpenJourney,
            onOpenPremium = onOpenPremium,
            onEditAboutYou = onEditAboutYou,
        )
    }
    composable<JourneyRoute> {
        JourneyRouteContent(onOpenStage = { onOpenStage(it.name) }, onBack = onBack)
    }
    composable<StageRoute> { entry ->
        val name = entry.toRoute<StageRoute>().stage
        StageRouteContent(
            stage = JourneyStage.entries.firstOrNull { it.name == name },
            onOpenPremium = onOpenPremium,
            onBack = onBack,
        )
    }
    composable<PremiumRoute> {
        PremiumRouteContent(onBack = onBack)
    }
    composable<ProgressRoute> {
        ProgressRouteContent(onBack = onBack)
    }
    composable<RemindersRoute> {
        RemindersRouteContent(onBack = onBack)
    }
}

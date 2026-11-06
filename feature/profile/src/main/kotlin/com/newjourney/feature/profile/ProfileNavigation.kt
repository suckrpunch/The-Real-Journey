package com.newjourney.feature.profile

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
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

fun NavGraphBuilder.profileScreens(
    onOpenProgress: () -> Unit,
    onOpenReminders: () -> Unit,
    onBack: () -> Unit,
) {
    composable<ProfileRoute> {
        ProfileRouteContent(onOpenProgress = onOpenProgress, onOpenReminders = onOpenReminders)
    }
    composable<ProgressRoute> {
        ProgressRouteContent(onBack = onBack)
    }
    composable<RemindersRoute> {
        RemindersRouteContent(onBack = onBack)
    }
}

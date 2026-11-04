package com.newjourney.feature.home

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.newjourney.core.model.Mood
import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

fun NavGraphBuilder.homeScreen(
    onBeginMeditation: (minutes: Int) -> Unit,
    onOpenExplore: () -> Unit,
    onMoodSelected: (Mood) -> Unit,
) {
    composable<HomeRoute> {
        HomeScreenRoute(
            onBeginMeditation = onBeginMeditation,
            onOpenExplore = onOpenExplore,
            onMoodSelected = onMoodSelected,
        )
    }
}

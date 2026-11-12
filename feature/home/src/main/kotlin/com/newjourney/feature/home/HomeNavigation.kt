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
    onOpenCategory: (categoryId: String) -> Unit,
    onOpenTrack: (trackId: String) -> Unit,
    onOpenStage: (stage: String) -> Unit,
    onMoodSelected: (Mood) -> Unit,
    onStartRoutine: (routineId: Long) -> Unit,
) {
    composable<HomeRoute> {
        HomeScreenRoute(
            onBeginMeditation = onBeginMeditation,
            onOpenExplore = onOpenExplore,
            onOpenCategory = onOpenCategory,
            onOpenTrack = onOpenTrack,
            onOpenStage = onOpenStage,
            onMoodSelected = onMoodSelected,
            onStartRoutine = onStartRoutine,
        )
    }
}

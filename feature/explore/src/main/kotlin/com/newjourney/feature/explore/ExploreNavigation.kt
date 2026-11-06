package com.newjourney.feature.explore

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** The Explore tab: favourites, recently played and the categories. */
@Serializable
data object ExploreRoute

/** One category with its sub-categories and tracks. */
@Serializable
data class CategoryRoute(val categoryId: String)

/** The player for one track, with "Meditate with this track". */
@Serializable
data class TrackRoute(val trackId: String)

fun NavGraphBuilder.exploreScreens(
    onOpenCategory: (categoryId: String) -> Unit,
    onOpenTrack: (trackId: String) -> Unit,
    onMeditateWithTrack: (trackId: String, minutes: Int) -> Unit,
    onBack: () -> Unit,
) {
    composable<ExploreRoute> {
        ExploreRouteContent(onOpenCategory = onOpenCategory, onOpenTrack = onOpenTrack)
    }
    composable<CategoryRoute> {
        CategoryRouteContent(onOpenTrack = onOpenTrack, onBack = onBack)
    }
    composable<TrackRoute> {
        TrackRouteContent(onMeditateWithTrack = onMeditateWithTrack, onBack = onBack)
    }
}

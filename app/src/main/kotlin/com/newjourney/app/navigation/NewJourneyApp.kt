package com.newjourney.app.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.newjourney.app.R
import com.newjourney.feature.explore.CategoryRoute
import com.newjourney.feature.explore.ExploreRoute
import com.newjourney.feature.explore.TrackRoute
import com.newjourney.feature.explore.exploreScreens
import com.newjourney.feature.home.HomeRoute
import com.newjourney.feature.home.homeScreen
import com.newjourney.feature.journal.JournalRoute
import com.newjourney.feature.journal.journalScreen
import com.newjourney.feature.meditate.MeditateRoute
import com.newjourney.feature.meditate.MeditationSessionRoute
import com.newjourney.feature.meditate.meditateScreen
import com.newjourney.feature.meditate.meditationSessionScreen
import com.newjourney.feature.profile.ProfileRoute
import com.newjourney.feature.profile.profileScreen
import kotlin.reflect.KClass

/** The five tabs, in order. Every tab has an icon and a text label. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    @StringRes val label: Int,
) {
    HOME(HomeRoute, HomeRoute::class, Icons.Outlined.Home, R.string.tab_home),
    EXPLORE(ExploreRoute, ExploreRoute::class, Icons.Outlined.Explore, R.string.tab_explore),
    MEDITATE(MeditateRoute(), MeditateRoute::class, Icons.Outlined.SelfImprovement, R.string.tab_meditate),
    JOURNAL(JournalRoute(), JournalRoute::class, Icons.AutoMirrored.Outlined.MenuBook, R.string.tab_journal),
    PROFILE(ProfileRoute, ProfileRoute::class, Icons.Outlined.Person, R.string.tab_profile),
}

@Composable
fun NewJourneyApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // The running timer is full screen: no tab bar, nothing else competing for attention.
    val showTabs = currentDestination?.hasRoute(MeditationSessionRoute::class) != true
    val inLibrary = currentDestination?.hasRoute(CategoryRoute::class) == true ||
        currentDestination?.hasRoute(TrackRoute::class) == true
    val onTrackScreen = currentDestination?.hasRoute(TrackRoute::class) == true
    val openTrack: (String) -> Unit = { id -> navController.navigate(TrackRoute(id)) { launchSingleTop = true } }

    Scaffold(
        bottomBar = {
            if (showTabs) {
                Column {
                    // The player screen has its own controls, so the mini player steps aside there.
                    if (!onTrackScreen) MiniPlayer(onOpen = openTrack)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest) {
                        TopLevelDestination.entries.forEach { destination ->
                            val selected = currentDestination?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true ||
                                (destination == TopLevelDestination.EXPLORE && inLibrary)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navController.navigateToTab(destination.route) },
                                icon = { Icon(destination.icon, contentDescription = null) },
                                label = { Text(stringResource(destination.label), style = MaterialTheme.typography.labelMedium) },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                ),
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            homeScreen(
                onBeginMeditation = { minutes -> navController.navigate(MeditationSessionRoute(minutes)) },
                onOpenExplore = { navController.navigateToTab(ExploreRoute) },
                onOpenCategory = { id -> navController.navigate(CategoryRoute(id)) },
                onOpenTrack = openTrack,
                onMoodSelected = { mood -> navController.navigateToTab(JournalRoute(mood.name), restore = false) },
            )
            exploreScreens(
                onOpenCategory = { id -> navController.navigate(CategoryRoute(id)) },
                onOpenTrack = openTrack,
                onMeditateWithTrack = { trackId, minutes ->
                    navController.navigate(MeditationSessionRoute(minutes = minutes, trackId = trackId))
                },
                onBack = { navController.popBackStack() },
            )
            meditateScreen(
                onStartSession = { minutes -> navController.navigate(MeditationSessionRoute(minutes)) },
                onReturnToSession = { navController.navigate(MeditationSessionRoute(autoStart = false)) },
            )
            meditationSessionScreen(
                onClose = {
                    // Only pop if the timer is still on top, so a second call does nothing.
                    if (navController.currentDestination?.hasRoute(MeditationSessionRoute::class) == true) {
                        navController.popBackStack()
                    }
                },
                onOpenJournal = { navController.navigateToTab(JournalRoute(), restore = false) },
            )
            journalScreen()
            profileScreen()
        }
    }
}

/**
 * Switches tabs without piling up screens: back always returns to Home, then exits.
 * [restore] brings a tab back as the user left it; off when opening a tab with new arguments.
 */
private fun NavHostController.navigateToTab(route: Any, restore: Boolean = true) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = restore }
        launchSingleTop = true
        restoreState = restore
    }
}

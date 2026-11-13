package com.newjourney.app.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.newjourney.app.R
import com.newjourney.core.designsystem.component.LocalOpenPremium
import com.newjourney.core.designsystem.component.LocalOpenReminders
import com.newjourney.core.designsystem.theme.NavigationLabelStyle
import com.newjourney.core.designsystem.theme.NewJourneyTheme
import com.newjourney.feature.explore.CategoryRoute
import com.newjourney.feature.explore.ExploreRoute
import com.newjourney.feature.explore.TrackRoute
import com.newjourney.feature.explore.exploreScreens
import com.newjourney.feature.home.HomeRoute
import com.newjourney.feature.home.homeScreen
import com.newjourney.feature.journal.JournalEditorRoute
import com.newjourney.feature.journal.JournalRoute
import com.newjourney.feature.journal.journalScreens
import com.newjourney.feature.meditate.MeditateRoute
import com.newjourney.feature.meditate.MeditationSessionRoute
import com.newjourney.feature.meditate.meditateScreen
import com.newjourney.feature.meditate.meditationSessionScreen
import com.newjourney.feature.onboarding.OnboardingRoute
import com.newjourney.feature.onboarding.onboardingScreen
import com.newjourney.feature.profile.JourneyRoute
import com.newjourney.feature.profile.PremiumRoute
import com.newjourney.feature.profile.ProfileRoute
import com.newjourney.feature.profile.ProgressRoute
import com.newjourney.feature.profile.RemindersRoute
import com.newjourney.feature.profile.StageRoute
import com.newjourney.feature.profile.profileScreens
import kotlin.reflect.KClass

/** The five tabs, in order. Every tab has an icon and a text label. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val icon: ImageVector,
    /** Shown when the tab is selected; the same outline icon unless a filled one reads better. */
    val selectedIcon: ImageVector,
    @StringRes val label: Int,
) {
    HOME(HomeRoute, HomeRoute::class, Icons.Outlined.Home, Icons.Filled.Home, R.string.tab_home),
    EXPLORE(ExploreRoute, ExploreRoute::class, Icons.Outlined.Search, Icons.Outlined.Search, R.string.tab_explore),
    MEDITATE(MeditateRoute(), MeditateRoute::class, Icons.Outlined.Spa, Icons.Outlined.Spa, R.string.tab_meditate),
    JOURNAL(JournalRoute, JournalRoute::class, Icons.AutoMirrored.Outlined.Article, Icons.AutoMirrored.Outlined.Article, R.string.tab_journal),
    PROFILE(ProfileRoute, ProfileRoute::class, Icons.Outlined.Person, Icons.Outlined.Person, R.string.tab_profile),
}

@Composable
fun NewJourneyApp(
    /** True on the very first launch: the app opens with the introduction. */
    startWithIntroduction: Boolean = false,
    navController: NavHostController = rememberNavController(),
) {
    // Chosen once; finishing the introduction moves on to Home without rebuilding the app.
    val startDestination: Any = remember { if (startWithIntroduction) OnboardingRoute() else HomeRoute }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // The running timer and journal writing are full screen: no tab bar, nothing else competing for attention.
    val showTabs = currentDestination?.hasRoute(MeditationSessionRoute::class) != true &&
        currentDestination?.hasRoute(JournalEditorRoute::class) != true &&
        currentDestination?.hasRoute(OnboardingRoute::class) != true
    val inLibrary = currentDestination?.hasRoute(CategoryRoute::class) == true ||
        currentDestination?.hasRoute(TrackRoute::class) == true
    val inProfile = listOf(ProgressRoute::class, RemindersRoute::class, JourneyRoute::class, StageRoute::class, PremiumRoute::class)
        .any { currentDestination?.hasRoute(it) == true }
    val onTrackScreen = currentDestination?.hasRoute(TrackRoute::class) == true
    val openTrack: (String) -> Unit = { id -> navController.navigate(TrackRoute(id)) { launchSingleTop = true } }
    val openStage: (String) -> Unit = { stage -> navController.navigate(StageRoute(stage)) { launchSingleTop = true } }
    val openPremium: () -> Unit = { navController.navigate(PremiumRoute) { launchSingleTop = true } }
    // From Profile the introduction just closes; on first launch it makes way for Home.
    val finishIntroduction: () -> Unit = {
        if (navController.currentDestination?.hasRoute(OnboardingRoute::class) == true) {
            if (navController.previousBackStackEntry != null) {
                navController.popBackStack()
            } else {
                navController.navigate(HomeRoute) { popUpTo<OnboardingRoute> { inclusive = true } }
            }
        }
    }

    CompositionLocalProvider(
        LocalOpenPremium provides openPremium,
        LocalOpenReminders provides { navController.navigate(RemindersRoute) { launchSingleTop = true } },
    ) {
    Scaffold(
        bottomBar = {
            if (showTabs) {
                Column {
                    // The player screen has its own controls, so the mini player steps aside there.
                    if (!onTrackScreen) MiniPlayer(onOpen = openTrack)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                        TopLevelDestination.entries.forEach { destination ->
                            val selected = currentDestination?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true ||
                                (destination == TopLevelDestination.EXPLORE && inLibrary) ||
                                (destination == TopLevelDestination.PROFILE && inProfile)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navController.navigateToTab(destination.route) },
                                icon = {
                                    Icon(if (selected) destination.selectedIcon else destination.icon, contentDescription = null)
                                },
                                label = {
                                    Text(
                                        stringResource(destination.label),
                                        style = NavigationLabelStyle,
                                        maxLines = 1,
                                    )
                                },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = NewJourneyTheme.extraColors.navIndicator,
                                    selectedIconColor = NewJourneyTheme.extraColors.onNavIndicator,
                                    selectedTextColor = MaterialTheme.colorScheme.onBackground,
                                    unselectedIconColor = NewJourneyTheme.extraColors.navInactive,
                                    unselectedTextColor = NewJourneyTheme.extraColors.navInactive,
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
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            onboardingScreen(
                onFinished = finishIntroduction,
                onOpenTrack = { id -> finishIntroduction(); openTrack(id) },
                onStartMeditation = { minutes ->
                    finishIntroduction()
                    navController.navigate(MeditationSessionRoute(minutes))
                },
            )
            homeScreen(
                onBeginMeditation = { minutes -> navController.navigate(MeditationSessionRoute(minutes)) },
                onOpenExplore = { navController.navigateToTab(ExploreRoute) },
                onOpenCategory = { id -> navController.navigate(CategoryRoute(id)) },
                onOpenTrack = openTrack,
                onOpenStage = openStage,
                onMoodSelected = { mood -> navController.navigate(JournalEditorRoute(mood = mood.name)) },
                onStartRoutine = { id -> navController.navigate(MeditationSessionRoute(routineId = id)) },
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
                onStartRoutine = { id -> navController.navigate(MeditationSessionRoute(routineId = id)) },
                onReturnToSession = { navController.navigate(MeditationSessionRoute(autoStart = false)) },
            )
            meditationSessionScreen(
                onClose = {
                    // Only pop if the timer is still on top, so a second call does nothing.
                    if (navController.currentDestination?.hasRoute(MeditationSessionRoute::class) == true) {
                        navController.popBackStack()
                    }
                },
                onOpenStage = { stage ->
                    // Like the journal: the finished timer makes way for the stage.
                    navController.navigate(StageRoute(stage)) {
                        popUpTo<MeditationSessionRoute> { inclusive = true }
                    }
                },
                onOpenJournal = { minutes ->
                    // The finished timer makes way for the journal; Back from writing returns to where the session began.
                    navController.navigate(JournalEditorRoute(afterSessionMinutes = minutes.takeIf { it > 0 })) {
                        popUpTo<MeditationSessionRoute> { inclusive = true }
                    }
                },
            )
            journalScreens(
                onWrite = { navController.navigate(JournalEditorRoute()) },
                onOpenEntry = { id -> navController.navigate(JournalEditorRoute(entryId = id)) },
                onCloseEditor = {
                    if (navController.currentDestination?.hasRoute(JournalEditorRoute::class) == true) {
                        navController.popBackStack()
                    }
                },
            )
            profileScreens(
                onOpenProgress = { navController.navigate(ProgressRoute) },
                onOpenReminders = { navController.navigate(RemindersRoute) },
                onOpenJourney = { navController.navigate(JourneyRoute) },
                onOpenStage = openStage,
                onOpenPremium = openPremium,
                onEditAboutYou = { navController.navigate(OnboardingRoute(editing = true)) { launchSingleTop = true } },
                onBack = { navController.popBackStack() },
            )
        }
    }
    }
}

/**
 * Switches tabs without piling up screens: back always returns to Home, then exits.
 * [restore] brings a tab back as the user left it; off when opening a tab with new arguments.
 */
private fun NavHostController.navigateToTab(route: Any, restore: Boolean = true) {
    navigate(route) {
        // Home is the base of every tab (also after the introduction, which was the first screen then).
        popUpTo<HomeRoute> { saveState = restore }
        launchSingleTop = true
        restoreState = restore
    }
}

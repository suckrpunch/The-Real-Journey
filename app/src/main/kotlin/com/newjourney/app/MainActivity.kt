package com.newjourney.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.newjourney.app.navigation.NewJourneyApp
import com.newjourney.core.datastore.UserSettingsRepository
import com.newjourney.core.designsystem.theme.NewJourneyTheme
import com.newjourney.core.model.ThemePreference
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    settings: UserSettingsRepository,
) : ViewModel() {
    val themePreference: StateFlow<ThemePreference> = settings.themePreference
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference())
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val preference by viewModel.themePreference.collectAsStateWithLifecycle()
            val hour by currentHour()
            val night = preference.useNightTheme(currentHour = hour, systemIsDark = isSystemInDarkTheme())

            // Status and navigation bar icons follow the app's theme, not only the phone's.
            DisposableEffect(night) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { night },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { night },
                )
                onDispose {}
            }

            NewJourneyTheme(nightTheme = night) {
                NewJourneyApp()
            }
        }
    }
}

/** The current hour, refreshed once a minute, so a scheduled theme switches on time. */
@Composable
private fun currentHour() = produceState(initialValue = LocalTime.now().hour) {
    while (true) {
        value = LocalTime.now().hour
        delay(60_000L - (System.currentTimeMillis() % 60_000L))
    }
}

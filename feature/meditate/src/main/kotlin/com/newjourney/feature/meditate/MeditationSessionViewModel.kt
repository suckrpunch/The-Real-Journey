package com.newjourney.feature.meditate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.newjourney.core.catalog.CatalogRepository
import com.newjourney.core.datastore.UserSettingsRepository
import com.newjourney.core.model.Entitlement
import com.newjourney.core.model.TimerLimits
import com.newjourney.feature.meditate.timer.MeditationTimer
import com.newjourney.feature.meditate.timer.TimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeditationSessionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val timer: MeditationTimer,
    settings: UserSettingsRepository,
    catalog: CatalogRepository,
) : ViewModel() {

    val state: StateFlow<TimerState> = timer.state

    /** The title of the track playing in this session, if any. */
    val trackTitle: StateFlow<String?> = combine(timer.state, catalog.catalog) { timerState, cat ->
        val id = when (timerState) {
            is TimerState.Running -> timerState.session.trackId
            is TimerState.Paused -> timerState.session.trackId
            is TimerState.Finished -> timerState.session.trackId
            TimerState.Idle -> null
        }
        id?.let { cat?.track(it)?.title }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** True once this screen has had a session, so it can close itself when the session is over. */
    val hadSession: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_HAD_SESSION, false)

    // Phase 4 replaces this with the verified entitlement from Google Play.
    private val entitlement = Entitlement(isPremium = false)

    init {
        val route = savedStateHandle.toRoute<MeditationSessionRoute>()
        val current = timer.state.value
        if (current !is TimerState.Idle) {
            savedStateHandle[KEY_HAD_SESSION] = true
        } else if (route.autoStart && !hadSession.value) {
            val allowed = entitlement.isPremium || !TimerLimits.isPremiumDuration(route.minutes)
            val minutes = if (allowed) route.minutes else TimerLimits.FREE_MAX_MINUTES
            viewModelScope.launch {
                timer.start(minutes, settings.timerSettings.first(), route.trackId)
                savedStateHandle[KEY_HAD_SESSION] = true
            }
        } else {
            // Nothing to show (for example the session ended while the app was closed).
            savedStateHandle[KEY_HAD_SESSION] = true
        }
    }

    fun pause() = timer.pause()
    fun resume() = timer.resume()
    fun endEarly() = timer.endEarly()
    fun dismissFinished() = timer.dismissFinished()

    private companion object {
        const val KEY_HAD_SESSION = "had_session"
    }
}

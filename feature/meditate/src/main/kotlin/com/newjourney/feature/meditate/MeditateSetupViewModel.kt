package com.newjourney.feature.meditate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.newjourney.core.database.RoutineRepository
import com.newjourney.core.datastore.UserSettingsRepository
import com.newjourney.core.model.BackgroundSound
import com.newjourney.core.billing.EntitlementRepository
import com.newjourney.core.model.Entitlement
import com.newjourney.core.model.Routine
import com.newjourney.core.model.TimerLimits
import com.newjourney.core.model.TimerSettings
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

data class MeditateSetupUiState(
    val durations: List<Int> = TimerLimits.quickStartMinutes,
    val selectedMinutes: Int = TimerLimits.DEFAULT_MINUTES,
    val settings: TimerSettings = TimerSettings(),
    val sessionInProgress: Boolean = false,
    val entitlement: Entitlement = Entitlement(),
    val routines: List<Routine> = emptyList(),
) {
    fun isLocked(minutes: Int): Boolean = !entitlement.isPremium && TimerLimits.isPremiumDuration(minutes)
    val canSaveRoutine: Boolean get() = routines.size < Routine.MAX_ROUTINES
}

@HiltViewModel
class MeditateSetupViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val settingsRepository: UserSettingsRepository,
    timer: MeditationTimer,
    private val entitlements: EntitlementRepository,
    private val routines: RoutineRepository,
) : ViewModel() {

    /** Premium as confirmed by Google Play (see EntitlementRepository). */
    private val entitlement: Entitlement get() = entitlements.entitlement.value

    private val selectedMinutes = savedStateHandle.getStateFlow(
        KEY_MINUTES,
        savedStateHandle.toRoute<MeditateRoute>().minutes.let { requested ->
            if (!entitlement.isPremium && TimerLimits.isPremiumDuration(requested)) TimerLimits.DEFAULT_MINUTES else requested
        },
    )

    val uiState: StateFlow<MeditateSetupUiState> = combine(
        selectedMinutes,
        settingsRepository.timerSettings,
        timer.state,
    ) { minutes, settings, timerState ->
        MeditateSetupUiState(
            selectedMinutes = minutes,
            settings = settings,
            sessionInProgress = timerState is TimerState.Running || timerState is TimerState.Paused,
            entitlement = entitlement,
        )
    }.combine(entitlements.entitlement) { state, current -> state.copy(entitlement = current) }
        .combine(routines.routines) { state, saved -> state.copy(routines = saved) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MeditateSetupUiState())

    fun selectDuration(minutes: Int) {
        if (!uiState.value.isLocked(minutes)) savedStateHandle[KEY_MINUTES] = minutes
    }

    fun setStartBell(on: Boolean) = updateSettings { it.copy(startBell = on) }
    fun setEndBell(on: Boolean) = updateSettings { it.copy(endBell = on) }
    fun setSound(sound: BackgroundSound) = updateSettings { it.copy(sound = sound) }

    /** Saves the current length, bells and sound as a routine called [name]. */
    fun saveRoutine(name: String) {
        val state = uiState.value
        val clean = Routine.cleanName(name)
        if (clean.isEmpty() || !state.canSaveRoutine) return
        viewModelScope.launch {
            routines.save(Routine(name = clean, minutes = state.selectedMinutes, settings = state.settings))
        }
    }

    fun deleteRoutine(id: Long) {
        viewModelScope.launch { routines.delete(id) }
    }

    /** Settings are saved straight away, so Home's Begin button uses them too. */
    private fun updateSettings(change: (TimerSettings) -> TimerSettings) {
        viewModelScope.launch {
            settingsRepository.setTimerSettings(change(settingsRepository.timerSettings.first()))
        }
    }

    private companion object {
        const val KEY_MINUTES = "setup_selected_minutes"
    }
}

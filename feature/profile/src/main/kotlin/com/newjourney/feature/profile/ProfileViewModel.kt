package com.newjourney.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newjourney.core.datastore.UserSettingsRepository
import com.newjourney.core.model.ThemeMode
import com.newjourney.core.model.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val settings: UserSettingsRepository,
) : ViewModel() {

    val themePreference: StateFlow<ThemePreference> = settings.themePreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemePreference())

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    /** Moves the night start or end by [deltaHours], wrapping around midnight. */
    fun shiftSchedule(start: Boolean, deltaHours: Int) {
        val current = themePreference.value
        val newStart = if (start) Math.floorMod(current.nightStartHour + deltaHours, 24) else current.nightStartHour
        val newEnd = if (!start) Math.floorMod(current.nightEndHour + deltaHours, 24) else current.nightEndHour
        viewModelScope.launch { settings.setNightSchedule(newStart, newEnd) }
    }
}

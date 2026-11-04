package com.newjourney.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newjourney.core.database.SessionRepository
import com.newjourney.core.datastore.UserSettingsRepository
import com.newjourney.core.model.DailyQuote
import com.newjourney.core.model.Entitlement
import com.newjourney.core.model.PartOfDay
import com.newjourney.core.model.Quote
import com.newjourney.core.model.SampleQuotes
import com.newjourney.core.model.TimerLimits
import com.newjourney.core.model.WeekProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val partOfDay: PartOfDay,
    val date: LocalDate,
    val quote: Quote?,
    val quoteSaved: Boolean,
    val durations: List<Int>,
    val selectedMinutes: Int,
    val entitlement: Entitlement,
    val week: WeekProgress,
) {
    fun isLocked(minutes: Int): Boolean = !entitlement.isPremium && TimerLimits.isPremiumDuration(minutes)
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val settings: UserSettingsRepository,
    sessions: SessionRepository,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    // Phase 4 replaces this with the verified entitlement from Google Play.
    private val entitlement = Entitlement(isPremium = false)

    private val selectedMinutes: StateFlow<Int> =
        savedStateHandle.getStateFlow(KEY_MINUTES, TimerLimits.DEFAULT_MINUTES)

    private val weekSessions = sessions.sessionsSince(WeekProgress.weekStart(LocalDate.now(), zone))

    val uiState: StateFlow<HomeUiState> = combine(
        selectedMinutes,
        settings.favoriteQuoteIds,
        weekSessions,
    ) { minutes, saved, sessionList ->
        val today = LocalDate.now()
        val quote = DailyQuote.pick(SampleQuotes, today)
        HomeUiState(
            partOfDay = PartOfDay.fromHour(LocalTime.now().hour),
            date = today,
            quote = quote,
            quoteSaved = quote != null && quote.id in saved,
            durations = TimerLimits.quickStartMinutes,
            selectedMinutes = minutes,
            entitlement = entitlement,
            week = WeekProgress.from(sessionList, today, zone),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState())

    /** Selects a free duration. Premium durations are handled by the screen, which explains premium. */
    fun selectDuration(minutes: Int) {
        if (!uiState.value.isLocked(minutes)) savedStateHandle[KEY_MINUTES] = minutes
    }

    fun toggleQuoteSaved() {
        val id = uiState.value.quote?.id ?: return
        viewModelScope.launch { settings.toggleFavoriteQuote(id) }
    }

    private fun initialState(): HomeUiState {
        val today = LocalDate.now()
        return HomeUiState(
            partOfDay = PartOfDay.fromHour(LocalTime.now().hour),
            date = today,
            quote = DailyQuote.pick(SampleQuotes, today),
            quoteSaved = false,
            durations = TimerLimits.quickStartMinutes,
            selectedMinutes = TimerLimits.DEFAULT_MINUTES,
            entitlement = entitlement,
            week = WeekProgress.from(emptyList(), today, zone),
        )
    }

    private companion object {
        const val KEY_MINUTES = "selected_minutes"
    }
}

package com.newjourney.feature.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newjourney.core.catalog.CatalogRepository
import com.newjourney.core.catalog.JourneyContentRepository
import com.newjourney.core.database.LibraryRepository
import com.newjourney.core.database.RoutineRepository
import com.newjourney.core.database.RecentTrack
import com.newjourney.core.database.SessionRepository
import com.newjourney.core.datastore.UserSettingsRepository
import com.newjourney.core.model.Catalog
import com.newjourney.core.model.Category
import com.newjourney.core.model.DailyQuote
import com.newjourney.core.billing.EntitlementRepository
import com.newjourney.core.model.Entitlement
import com.newjourney.core.model.JourneyProgress
import com.newjourney.core.model.PartOfDay
import com.newjourney.core.model.Recommendations
import com.newjourney.core.model.Goal
import com.newjourney.core.model.Routine
import com.newjourney.core.model.Quote
import com.newjourney.core.model.SampleQuotes
import com.newjourney.core.model.TimerLimits
import com.newjourney.core.model.Track
import com.newjourney.core.model.TrackTier
import com.newjourney.core.model.WeekProgress
import com.newjourney.core.playback.MusicPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
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
    val categories: List<Category> = emptyList(),
    val continueListening: ContinueListening? = null,
    /** The first few saved routines, as one-tap starts. */
    val routines: List<Routine> = emptyList(),
    /** The name from the introduction, for the greeting. */
    val userName: String? = null,
    /** Suggestions from the goals chosen in the introduction; empty when no goals were chosen. */
    val picks: List<HomePick> = emptyList(),
) {
    fun isLocked(minutes: Int): Boolean = !entitlement.isPremium && TimerLimits.isPremiumDuration(minutes)
}

/** A suggested track on Home, chosen from the listener's goals. */
data class HomePick(
    val trackId: String,
    val title: String,
    val place: String,
    val colorHex: String?,
    val minutes: Int,
    val locked: Boolean,
)

/** The track the listener stopped part-way through, shown as "Continue listening". */
data class ContinueListening(
    val track: Track,
    val colorHex: String?,
    val place: String,
    val positionMs: Long,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val settings: UserSettingsRepository,
    sessions: SessionRepository,
    private val catalogRepository: CatalogRepository,
    library: LibraryRepository,
    private val player: MusicPlayer,
    journeyContent: JourneyContentRepository,
    private val entitlements: EntitlementRepository,
    routines: RoutineRepository,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()

    /** Premium as confirmed by Google Play (see EntitlementRepository). */
    private val entitlement: Entitlement get() = entitlements.entitlement.value

    private val selectedMinutes: StateFlow<Int> =
        savedStateHandle.getStateFlow(KEY_MINUTES, TimerLimits.DEFAULT_MINUTES)

    private val weekSessions = sessions.sessionsSince(WeekProgress.weekStart(LocalDate.now(), zone))

    val uiState: StateFlow<HomeUiState> = combine(
        selectedMinutes,
        settings.favoriteQuoteIds,
        combine(weekSessions, settings.dailyGoalMinutes) { list, goal -> list to goal },
        catalogRepository.catalog,
        library.recent(limit = 1),
    ) { minutes, saved, (sessionList, goal), catalog, recent ->
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
            week = WeekProgress.from(sessionList, today, zone, goalMinutes = goal),
            categories = catalog?.categories.orEmpty(),
            continueListening = catalog?.let { continueListeningFrom(it, recent.firstOrNull()) },
        )
    }.combine(entitlements.entitlement) { state, current -> state.copy(entitlement = current) }
        .combine(routines.routines) { state, saved -> state.copy(routines = saved.take(HOME_ROUTINES)) }
        .combine(settings.userProfile) { state, profile ->
            state.copy(userName = profile.displayName, picks = picksFor(profile.goals, state.entitlement.isPremium))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialState())

    /** Selects a free duration. Premium durations are handled by the screen, which explains premium. */
    fun selectDuration(minutes: Int) {
        if (!uiState.value.isLocked(minutes)) savedStateHandle[KEY_MINUTES] = minutes
    }

    /**
     * The furthest journey stage reached that the user has not opened yet: (stage id, name).
     * Shown on Home until they read it.
     */
    val newStage: StateFlow<Pair<String, String>?> = combine(
        sessions.sessionsSince(Instant.EPOCH).map { JourneyProgress.from(it).current },
        settings.seenJourneyStages,
        flow { emit(journeyContent.content()) },
    ) { current, seen, content ->
        current?.takeIf { it.name !in seen }?.let { it.name to (content[it]?.name ?: it.name) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Resumes the last track where it stopped, with the rest of its category after it. */
    fun continueListening() {
        val item = uiState.value.continueListening ?: return
        viewModelScope.launch {
            val catalog = catalogRepository.get()
            val queue = catalog.tracksInCategory(item.track.categoryId).filter { it.tier == TrackTier.FREE || entitlement.isPremium }
            val index = queue.indexOfFirst { it.id == item.track.id }
            if (index >= 0) player.play(queue, index, item.positionMs) else player.play(listOf(item.track), 0, item.positionMs)
        }
    }

    private fun continueListeningFrom(catalog: Catalog, recent: RecentTrack?): ContinueListening? {
        recent ?: return null
        val track = catalog.track(recent.trackId) ?: return null
        // Only worth offering when the listener is part-way through.
        if (recent.positionMs < 15_000 || recent.positionMs > track.durationMs - 15_000) return null
        val category = catalog.category(track.categoryId)
        val sub = category?.subcategories?.firstOrNull { it.id == track.subcategoryId }
        return ContinueListening(
            track = track,
            colorHex = sub?.color,
            place = listOfNotNull(category?.name, sub?.name).joinToString(" · "),
            positionMs = recent.positionMs,
        )
    }

    private fun picksFor(goals: Set<Goal>, isPremium: Boolean): List<HomePick> {
        if (goals.isEmpty()) return emptyList()
        val catalog = catalogRepository.catalog.value ?: return emptyList()
        return Recommendations.pick(catalog, goals, isPremium).map { track ->
            val category = catalog.category(track.categoryId)
            val sub = category?.subcategories?.firstOrNull { it.id == track.subcategoryId }
            HomePick(
                trackId = track.id,
                title = track.title,
                place = listOfNotNull(category?.name, sub?.name).joinToString(" · "),
                colorHex = sub?.color,
                minutes = ((track.durationMs + 59_999) / 60_000).toInt(),
                locked = track.tier == TrackTier.PREMIUM && !isPremium,
            )
        }
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
        const val HOME_ROUTINES = 3
        const val KEY_MINUTES = "selected_minutes"
    }
}

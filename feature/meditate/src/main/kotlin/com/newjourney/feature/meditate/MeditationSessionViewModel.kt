package com.newjourney.feature.meditate

import com.newjourney.core.model.VoiceGuide
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.newjourney.core.catalog.CatalogRepository
import com.newjourney.core.catalog.JourneyContentRepository
import com.newjourney.core.database.RoutineRepository
import com.newjourney.core.database.SessionRepository
import com.newjourney.core.datastore.UserSettingsRepository
import com.newjourney.core.billing.EntitlementRepository
import com.newjourney.core.model.Entitlement
import com.newjourney.core.model.JourneyProgress
import com.newjourney.core.model.Milestone
import com.newjourney.core.model.ProgressStats
import com.newjourney.core.model.TimerLimits
import com.newjourney.feature.meditate.timer.MeditationTimer
import com.newjourney.feature.meditate.timer.TimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class MeditationSessionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val timer: MeditationTimer,
    private val userSettings: UserSettingsRepository,
    catalog: CatalogRepository,
    sessions: SessionRepository,
    journeyContent: JourneyContentRepository,
    private val entitlements: EntitlementRepository,
    routines: RoutineRepository,
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

    /** Milestones reached by the session that just finished, shown on the finish screen. */
    val newMilestones: StateFlow<List<Milestone>> = combine(timer.state, sessions.sessionsSince(Instant.EPOCH)) { timerState, all ->
        val finished = timerState as? TimerState.Finished ?: return@combine emptyList()
        val zone = ZoneId.systemDefault()
        ProgressStats.from(all, LocalDate.now(zone), zone).reachedBy(Instant.ofEpochMilli(finished.session.id))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Days in a row with at least one session, counting today, for the finish screen. */
    val currentRun: StateFlow<Int> = sessions.sessionsSince(Instant.EPOCH).map { all ->
        val zone = ZoneId.systemDefault()
        ProgressStats.from(all, LocalDate.now(zone), zone).currentRun
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Journey stages reached by the session that just finished: (stage id, stage name). */
    val newStages: StateFlow<List<Pair<String, String>>> = combine(
        timer.state,
        sessions.sessionsSince(Instant.EPOCH),
        flow { emit(journeyContent.content()) },
    ) { timerState, all, content ->
        val finished = timerState as? TimerState.Finished ?: return@combine emptyList()
        JourneyProgress.from(all).reachedBy(Instant.ofEpochMilli(finished.session.id))
            .map { it.name to (content[it]?.name ?: it.name) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** True once this screen has had a session, so it can close itself when the session is over. */
    val hadSession: StateFlow<Boolean> = savedStateHandle.getStateFlow(KEY_HAD_SESSION, false)

    /** Premium as confirmed by Google Play (see EntitlementRepository). */
    private val entitlement: Entitlement get() = entitlements.entitlement.value

    init {
        // The voice starts at the volume chosen last time.
        viewModelScope.launch { timer.setVoiceVolume(userSettings.voiceVolume.first()) }
        val route = savedStateHandle.toRoute<MeditationSessionRoute>()
        val current = timer.state.value
        if (current !is TimerState.Idle) {
            savedStateHandle[KEY_HAD_SESSION] = true
        } else if (route.autoStart && !hadSession.value) {
            viewModelScope.launch {
                // A routine brings its own length, bells and sound; otherwise the saved timer settings.
                val routine = route.routineId?.let { routines.get(it) }
                val requested = routine?.minutes ?: route.minutes
                val allowed = entitlement.isPremium || !TimerLimits.isPremiumDuration(requested)
                val minutes = if (allowed) requested else TimerLimits.FREE_MAX_MINUTES
                // The bell and the voice are personal choices, so routines use the ones chosen now.
                val saved = userSettings.timerSettings.first()
                val timerSettings = routine?.settings?.copy(bell = saved.bell, guide = saved.guide) ?: saved
                timer.start(minutes, timerSettings, if (routine != null) null else route.trackId)
                savedStateHandle[KEY_HAD_SESSION] = true
            }
        } else {
            // Nothing to show (for example the session ended while the app was closed).
            savedStateHandle[KEY_HAD_SESSION] = true
        }
    }

    /** The guiding voice's volume, or null when this session has no voice. */
    val voiceVolume: StateFlow<Float?> = combine(timer.state, timer.voiceVolume) { timerState, volume ->
        val guide = when (timerState) {
            is TimerState.Running -> timerState.session.settings.guide
            is TimerState.Paused -> timerState.session.settings.guide
            else -> VoiceGuide.NONE
        }
        volume.takeIf { guide != VoiceGuide.NONE }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Moves the voice's volume while the slider is dragged. */
    fun setVoiceVolume(volume: Float) = timer.setVoiceVolume(volume)

    /** Remembers the volume for next time, once the slider is let go. */
    fun saveVoiceVolume() {
        val volume = timer.voiceVolume.value
        viewModelScope.launch { userSettings.setVoiceVolume(volume) }
    }

    fun pause() = timer.pause()
    fun resume() = timer.resume()
    fun endEarly() = timer.endEarly()
    fun dismissFinished() = timer.dismissFinished()

    private companion object {
        const val KEY_HAD_SESSION = "had_session"
    }
}

package com.newjourney.feature.meditate.timer

import android.content.Context
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.newjourney.core.database.SessionRepository
import com.newjourney.core.model.MIN_SAVED_SESSION_SECONDS
import com.newjourney.core.model.MeditationSession
import com.newjourney.core.model.SessionKind
import com.newjourney.core.model.TimerSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** One meditation in progress. [id] is its start time in epoch milliseconds. */
data class ActiveSession(
    val id: Long,
    val minutes: Int,
    val settings: TimerSettings,
    /** A music track that loops for the whole session instead of the background sound. */
    val trackId: String? = null,
) {
    val totalMs: Long get() = minutes * 60_000L
    val totalSeconds: Int get() = minutes * 60
}

sealed interface TimerState {
    data object Idle : TimerState

    /** Counting down; the session ends at [endAtElapsedMs] on the phone's monotonic clock. */
    data class Running(val session: ActiveSession, val endAtElapsedMs: Long) : TimerState

    data class Paused(val session: ActiveSession, val remainingMs: Long) : TimerState

    /** Showing the finish screen. [endedEarly] is true when the user tapped End. */
    data class Finished(
        val session: ActiveSession,
        val completedSeconds: Int,
        val endedEarly: Boolean,
    ) : TimerState
}

/** Milliseconds left in this state, given the current [SystemClock.elapsedRealtime]. */
fun TimerState.remainingMs(nowElapsedMs: Long = SystemClock.elapsedRealtime()): Long = when (this) {
    is TimerState.Running -> (endAtElapsedMs - nowElapsedMs).coerceAtLeast(0)
    is TimerState.Paused -> remainingMs
    is TimerState.Finished, TimerState.Idle -> 0
}

/**
 * The single source of truth for the meditation timer. Screens read [state] and call the
 * actions; [MeditationService] follows [state] to play sound and keep running with the screen off.
 *
 * Time is measured with elapsedRealtime, which keeps counting while the phone sleeps and is not
 * affected by the user changing the clock.
 */
@Singleton
class MeditationTimer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessions: SessionRepository,
) {
    private val _state = MutableStateFlow<TimerState>(TimerState.Idle)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val saveScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun now() = SystemClock.elapsedRealtime()

    /** Starts a new session unless one is already running or paused. */
    fun start(minutes: Int, settings: TimerSettings, trackId: String? = null) {
        val current = _state.value
        if (current is TimerState.Running || current is TimerState.Paused) return
        val session = ActiveSession(
            id = System.currentTimeMillis(),
            minutes = minutes,
            settings = settings,
            trackId = trackId,
        )
        _state.value = TimerState.Running(session, endAtElapsedMs = now() + session.totalMs)
        ContextCompat.startForegroundService(context, MeditationService.startIntent(context))
    }

    fun pause() {
        val current = _state.value as? TimerState.Running ?: return
        _state.value = TimerState.Paused(current.session, current.remainingMs(now()))
    }

    fun resume() {
        val current = _state.value as? TimerState.Paused ?: return
        _state.value = TimerState.Running(current.session, endAtElapsedMs = now() + current.remainingMs)
    }

    /** Called by the service when the countdown reaches zero. */
    internal fun complete() {
        val session = activeSession() ?: return
        end(session, session.totalSeconds, endedEarly = false)
    }

    /**
     * The user tapped End. Sessions of a minute or more are kept, because every minute
     * of practice counts; shorter ones are dropped quietly.
     */
    fun endEarly() {
        val current = _state.value
        val session = activeSession() ?: return
        val completedSeconds = ((session.totalMs - current.remainingMs(now())) / 1000).toInt()
        if (completedSeconds < MIN_SAVED_SESSION_SECONDS) {
            _state.value = TimerState.Idle
        } else {
            end(session, completedSeconds, endedEarly = true)
        }
    }

    /** Leaves the finish screen. */
    fun dismissFinished() {
        if (_state.value is TimerState.Finished) _state.value = TimerState.Idle
    }

    private fun activeSession(): ActiveSession? = when (val current = _state.value) {
        is TimerState.Running -> current.session
        is TimerState.Paused -> current.session
        else -> null
    }

    private fun end(session: ActiveSession, completedSeconds: Int, endedEarly: Boolean) {
        _state.value = TimerState.Finished(session, completedSeconds, endedEarly)
        saveScope.launch {
            sessions.record(
                MeditationSession(
                    startedAt = Instant.ofEpochMilli(session.id),
                    plannedSeconds = session.totalSeconds,
                    completedSeconds = completedSeconds,
                    completed = !endedEarly,
                    kind = if (session.trackId != null) SessionKind.MUSIC else SessionKind.TIMER,
                    trackId = session.trackId,
                ),
            )
        }
    }
}

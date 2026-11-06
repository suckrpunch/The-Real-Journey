package com.newjourney.feature.meditate

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.newjourney.core.designsystem.component.PrimaryButton
import com.newjourney.core.designsystem.component.SecondaryButton
import com.newjourney.feature.meditate.timer.TimerState
import com.newjourney.feature.meditate.timer.remainingMs
import kotlinx.coroutines.delay

@Composable
internal fun MeditationSessionRouteContent(
    onClose: () -> Unit,
    onOpenJournal: () -> Unit,
    viewModel: MeditationSessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val hadSession by viewModel.hadSession.collectAsStateWithLifecycle()
    val trackTitle by viewModel.trackTitle.collectAsStateWithLifecycle()

    RequestNotificationPermissionOnce()

    // When the session is over (Done tapped, or ended within the first minute), leave this screen.
    LaunchedEffect(state, hadSession) {
        if (hadSession && state is TimerState.Idle) onClose()
    }

    // Back leaves the screen; the session keeps going and can be reopened from the Meditate tab.
    BackHandler(enabled = state is TimerState.Finished) {
        viewModel.dismissFinished()
    }

    MeditationSessionScreen(
        state = state,
        trackTitle = trackTitle,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onEnd = viewModel::endEarly,
        onDone = viewModel::dismissFinished,
        onOpenJournal = {
            viewModel.dismissFinished()
            onOpenJournal()
        },
    )
}

@Composable
internal fun MeditationSessionScreen(
    state: TimerState,
    trackTitle: String?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
    onDone: () -> Unit,
    onOpenJournal: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        when (state) {
            is TimerState.Running, is TimerState.Paused -> Countdown(state, trackTitle, onPause, onResume, onEnd)
            is TimerState.Finished -> Finished(state, onDone, onOpenJournal)
            TimerState.Idle -> Spacer(Modifier.height(1.dp))
        }
    }
}

@Composable
private fun Countdown(
    state: TimerState,
    trackTitle: String?,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
) {
    val running = state is TimerState.Running
    var confirmEnd by rememberSaveable { mutableStateOf(false) }

    // Ticks only while this screen is visible; the service keeps time when the screen is off.
    val remainingMs by produceState(initialValue = state.remainingMs(), state) {
        while (true) {
            value = state.remainingMs(SystemClock.elapsedRealtime())
            if (state !is TimerState.Running) break
            delay(250)
        }
    }
    val totalSeconds = ((remainingMs + 999) / 1000).toInt()
    val minutesLeft = (totalSeconds + 59) / 60
    val timeText = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    val spoken = pluralStringResource(R.plurals.session_minutes_left, minutesLeft, minutesLeft)

    Text(
        stringResource(if (running) R.string.session_status_running else R.string.session_status_paused),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (trackTitle != null) {
        Text(
            stringResource(R.string.session_with_track, trackTitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    BreathingCircle(moving = running)
    Text(
        timeText,
        style = MaterialTheme.typography.displaySmall.copy(fontSize = 64.sp, lineHeight = 72.sp),
        modifier = Modifier.semantics { contentDescription = spoken },
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (running) {
            PrimaryButton(
                text = stringResource(R.string.timer_pause),
                onClick = onPause,
                leading = { Icon(Icons.Filled.Pause, contentDescription = null) },
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.timer_resume),
                onClick = onResume,
                leading = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
            )
        }
        SecondaryButton(
            text = stringResource(R.string.session_end),
            onClick = { confirmEnd = true },
            modifier = Modifier.fillMaxWidth(),
        )
    }
    Text(
        stringResource(R.string.session_screen_off_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text(stringResource(R.string.session_end_confirm_title)) },
            text = { Text(stringResource(R.string.session_end_confirm_body), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = {
                    confirmEnd = false
                    onEnd()
                }) { Text(stringResource(R.string.session_end_confirm_yes), style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnd = false }) {
                    Text(stringResource(R.string.session_end_confirm_no), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

@Composable
private fun Finished(state: TimerState.Finished, onDone: () -> Unit, onOpenJournal: () -> Unit) {
    val minutes = state.completedSeconds / 60
    Spacer(Modifier.height(24.dp))
    BreathingCircle(moving = false, size = 160.dp)
    Text(
        stringResource(if (state.endedEarly) R.string.finished_title_early else R.string.finished_title),
        style = MaterialTheme.typography.displaySmall,
        textAlign = TextAlign.Center,
    )
    Text(
        pluralStringResource(R.plurals.finished_minutes, minutes, minutes),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryButton(text = stringResource(R.string.finished_done), onClick = onDone)
        SecondaryButton(
            text = stringResource(R.string.finished_journal),
            onClick = onOpenJournal,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Asks once (Android 13+) so Pause and End can appear on the lock screen. The timer works either way. */
@Composable
private fun RequestNotificationPermissionOnce() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted && !asked) {
            asked = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

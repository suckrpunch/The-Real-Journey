package com.newjourney.feature.meditate

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CropSquare
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.newjourney.core.designsystem.component.PrimaryButton
import com.newjourney.core.designsystem.component.ScreenPadding
import com.newjourney.core.designsystem.component.SecondaryButton
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.designsystem.component.milestoneTitle
import com.newjourney.core.designsystem.icon.NavIcons
import com.newjourney.core.designsystem.theme.NewJourneyTheme
import com.newjourney.core.model.Milestone
import com.newjourney.feature.meditate.timer.TimerState
import com.newjourney.feature.meditate.timer.remainingMs
import kotlinx.coroutines.delay

@Composable
internal fun MeditationSessionRouteContent(
    onClose: () -> Unit,
    onOpenJournal: (sessionMinutes: Int) -> Unit,
    onOpenStage: (stage: String) -> Unit,
    onGoHome: () -> Unit = onClose,
    viewModel: MeditationSessionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val hadSession by viewModel.hadSession.collectAsStateWithLifecycle()
    val trackTitle by viewModel.trackTitle.collectAsStateWithLifecycle()
    val newMilestones by viewModel.newMilestones.collectAsStateWithLifecycle()
    val newStages by viewModel.newStages.collectAsStateWithLifecycle()
    val currentRun by viewModel.currentRun.collectAsStateWithLifecycle()

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
        newMilestones = newMilestones,
        newStages = newStages,
        onOpenStage = { stage ->
            viewModel.dismissFinished()
            onOpenStage(stage)
        },
        onBack = onClose,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onEnd = viewModel::endEarly,
        onDone = {
            viewModel.dismissFinished()
            onGoHome()
        },
        currentRun = currentRun,
        onOpenJournal = {
            val minutes = ((state as? TimerState.Finished)?.completedSeconds ?: 0) / 60
            viewModel.dismissFinished()
            onOpenJournal(minutes)
        },
    )
}

@Composable
internal fun MeditationSessionScreen(
    state: TimerState,
    trackTitle: String?,
    newMilestones: List<Milestone>,
    newStages: List<Pair<String, String>>,
    onOpenStage: (String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit,
    onDone: () -> Unit,
    onOpenJournal: () -> Unit,
    onBack: () -> Unit = {},
    currentRun: Int = 0,
) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        when (state) {
            is TimerState.Running, is TimerState.Paused -> Countdown(state, trackTitle, onBack, onPause, onResume, onEnd)
            is TimerState.Finished -> Finished(state, newMilestones, newStages, currentRun, onOpenStage, onDone, onOpenJournal)
            TimerState.Idle -> Spacer(Modifier.height(1.dp))
        }
    }
}

@Composable
private fun Countdown(
    state: TimerState,
    trackTitle: String?,
    onBack: () -> Unit,
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
    val timeText = "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    val sessionMinutes = when (state) {
        is TimerState.Running -> state.session.minutes
        is TimerState.Paused -> state.session.minutes
        else -> 0
    }
    val spoken = pluralStringResource(R.plurals.session_minutes_left, minutesLeft, minutesLeft)

    // Back leaves the screen while the session keeps going (reopen it from the Meditate tab).
    Row(Modifier.fillMaxWidth()) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.size(48.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.session_back),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    // One breath cycle drives both the circles and the words, so they always match.
    val cycle = rememberBreathCycle(running)
    val status = when {
        !running -> R.string.session_status_paused
        cycle == null -> R.string.session_status_running
        else -> when (breathPhase(cycle)) {
            // The short rest at the top keeps "Breathe in…", so the words change only twice a breath.
            BreathPhase.IN, BreathPhase.HOLD -> R.string.session_breathe_in
            BreathPhase.OUT -> R.string.session_breathe_out
        }
    }
    // The words fade softly into each other; screen readers keep the calm "Breathe gently".
    val spokenStatus = stringResource(if (running) R.string.session_status_running else R.string.session_status_paused)
    Crossfade(
        targetState = status,
        animationSpec = tween(700),
        label = "breathWords",
        modifier = Modifier.semantics { contentDescription = spokenStatus },
    ) { res ->
        Text(
            stringResource(res),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    if (trackTitle != null) {
        Text(
            stringResource(R.string.session_with_track, trackTitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    BreathingCircle(moving = running, size = 260.dp, cycle = cycle)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = spoken },
    ) {
        Text(timeText, style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        if (sessionMinutes > 0) {
            Text(
                stringResource(R.string.session_of_total, "%02d:00".format(sessionMinutes)),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (running) {
            PrimaryButton(
                text = stringResource(R.string.timer_pause),
                onClick = onPause,
                modifier = Modifier.weight(1f),
                leading = { Icon(Icons.Filled.Pause, contentDescription = null) },
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.timer_resume),
                onClick = onResume,
                modifier = Modifier.weight(1f),
                leading = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
            )
        }
        SecondaryButton(
            text = stringResource(R.string.timer_end),
            onClick = { confirmEnd = true },
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            leading = { Icon(Icons.Outlined.CropSquare, contentDescription = null) },
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
private fun Finished(
    state: TimerState.Finished,
    newMilestones: List<Milestone>,
    newStages: List<Pair<String, String>>,
    currentRun: Int,
    onOpenStage: (String) -> Unit,
    onDone: () -> Unit,
    onOpenJournal: () -> Unit,
) {
    val minutes = state.completedSeconds / 60
    Spacer(Modifier.height(40.dp))
    CompletionMark(Modifier.size(150.dp))
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(if (state.endedEarly) R.string.finished_title_early else R.string.finished_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            if (state.endedEarly) {
                pluralStringResource(R.plurals.finished_minutes, minutes, minutes)
            } else {
                stringResource(R.string.finished_completed, maxOf(minutes, 1))
            },
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, lineHeight = 28.sp),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
    if (currentRun >= 1) {
        SoftCard(padding = 22.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Icon(
                    Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp),
                )
                Column {
                    Text(
                        pluralStringResource(R.plurals.finished_streak, currentRun, currentRun),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(stringResource(R.string.finished_keep_going), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    newStages.forEach { (stageId, stageName) ->
        SoftCard(raised = true) {
            Text(stringResource(R.string.finished_stage_label), style = MaterialTheme.typography.titleMedium)
            Text(stageName, style = MaterialTheme.typography.headlineSmall)
            SecondaryButton(
                text = stringResource(R.string.finished_stage_read),
                onClick = { onOpenStage(stageId) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
    if (newMilestones.isNotEmpty()) {
        SoftCard(raised = true) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = NewJourneyTheme.extraColors.premium)
                Text(stringResource(R.string.finished_milestone), style = MaterialTheme.typography.titleMedium)
            }
            newMilestones.forEach { milestone ->
                Text(stringResource(milestoneTitle(milestone)), style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryButton(
            text = stringResource(R.string.finished_journal),
            onClick = onOpenJournal,
            leading = { Icon(NavIcons.Journal, contentDescription = null, modifier = Modifier.size(24.dp)) },
        )
        SecondaryButton(
            text = stringResource(R.string.finished_home),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            leading = { Icon(NavIcons.Home, contentDescription = null, modifier = Modifier.size(24.dp)) },
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

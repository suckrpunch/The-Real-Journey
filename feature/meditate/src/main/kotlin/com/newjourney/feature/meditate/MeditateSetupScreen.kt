package com.newjourney.feature.meditate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.newjourney.core.designsystem.component.AppHeader
import com.newjourney.core.designsystem.component.DurationChip
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.PremiumDurationSheet
import com.newjourney.core.designsystem.component.PrimaryButton
import com.newjourney.core.designsystem.component.ScreenPadding
import com.newjourney.core.designsystem.component.ScreenTitle
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.designsystem.component.SwitchRow
import com.newjourney.core.model.BackgroundSound

@Composable
internal fun MeditateSetupRoute(
    onStartSession: (Int) -> Unit,
    onStartRoutine: (Long) -> Unit,
    onReturnToSession: () -> Unit,
    viewModel: MeditateSetupViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MeditateSetupScreen(
        state = state,
        onSelectDuration = viewModel::selectDuration,
        onStartBellChange = viewModel::setStartBell,
        onEndBellChange = viewModel::setEndBell,
        onSoundSelected = viewModel::setSound,
        onBegin = { onStartSession(state.selectedMinutes) },
        onReturnToSession = onReturnToSession,
        onStartRoutine = onStartRoutine,
        onSaveRoutine = viewModel::saveRoutine,
        onRemoveRoutine = viewModel::deleteRoutine,
    )
}

@Composable
internal fun MeditateSetupScreen(
    state: MeditateSetupUiState,
    onSelectDuration: (Int) -> Unit,
    onStartBellChange: (Boolean) -> Unit,
    onEndBellChange: (Boolean) -> Unit,
    onSoundSelected: (BackgroundSound) -> Unit,
    onBegin: () -> Unit,
    onReturnToSession: () -> Unit,
    onStartRoutine: (Long) -> Unit = {},
    onSaveRoutine: (String) -> Unit = {},
    onRemoveRoutine: (Long) -> Unit = {},
) {
    var premiumSheetMinutes by rememberSaveable { mutableStateOf<Int?>(null) }
    var showSaveRoutine by rememberSaveable { mutableStateOf(false) }
    var routineToRemove by rememberSaveable { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AppHeader()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenTitle(
                        title = stringResource(R.string.meditate_title),
                        subtitle = stringResource(R.string.meditate_subtitle),
                        modifier = Modifier.weight(1f),
                    )
                    SkyMark(Modifier.size(84.dp))
                }
            }
        }

        if (state.sessionInProgress) {
            item {
                SoftCard(raised = true) {
                    Text(stringResource(R.string.setup_in_progress_title), style = MaterialTheme.typography.titleLarge)
                    PrimaryButton(text = stringResource(R.string.setup_return_to_session), onClick = onReturnToSession)
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.setup_length),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.durations) { minutes ->
                        DurationChip(
                            minutes = minutes,
                            selected = minutes == state.selectedMinutes,
                            premium = state.isLocked(minutes),
                            onClick = {
                                if (state.isLocked(minutes)) premiumSheetMinutes = minutes else onSelectDuration(minutes)
                            },
                            modifier = Modifier.width(60.dp),
                        )
                    }
                }
            }
        }

        item {
            SoftCard {
                CardHeading(Icons.Outlined.Notifications, stringResource(R.string.setup_bells))
                SwitchRow(stringResource(R.string.setup_start_bell), state.settings.startBell, onStartBellChange)
                SwitchRow(stringResource(R.string.setup_end_bell), state.settings.endBell, onEndBellChange)
            }
        }

        item {
            SoftCard {
                CardHeading(Icons.Outlined.MusicNote, stringResource(R.string.setup_sound))
                Box {
                    Pines(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 4.dp)
                            .size(width = 96.dp, height = 72.dp),
                    )
                    Column(Modifier.selectableGroup()) {
                        BackgroundSound.entries.forEach { sound ->
                            val selected = sound == state.settings.sound
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = MinTouchTarget)
                                    .selectable(selected = selected, onClick = { onSoundSelected(sound) }, role = Role.RadioButton),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selected, onClick = null)
                                Text(
                                    stringResource(soundLabel(sound)),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            PrimaryButton(
                text = stringResource(R.string.setup_begin, state.selectedMinutes),
                onClick = onBegin,
                leading = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
            )
        }

        item {
            SaveRoutineRow(
                canSave = state.canSaveRoutine,
                hasRoutines = state.routines.isNotEmpty(),
                onSave = { showSaveRoutine = true },
            )
        }

        if (state.routines.isNotEmpty()) {
            item {
                SectionTitle(stringResource(R.string.routine_section_title), Modifier.padding(top = 12.dp))
            }
            items(state.routines, key = { "routine-${it.id}" }) { routine ->
                val locked = state.isLocked(routine.minutes)
                RoutineCard(
                    routine = routine,
                    locked = locked,
                    onBegin = { if (locked) premiumSheetMinutes = routine.minutes else onStartRoutine(routine.id) },
                    onRemove = { routineToRemove = routine.id },
                )
            }
        }
    }

    if (showSaveRoutine) {
        SaveRoutineDialog(
            summary = routineSummary(state.selectedMinutes, state.settings),
            suggestion = suggestedRoutineName(state.selectedMinutes, state.settings),
            onSave = onSaveRoutine,
            onDismiss = { showSaveRoutine = false },
        )
    }

    routineToRemove?.let { id -> state.routines.firstOrNull { it.id == id } }?.let { routine ->
        RemoveRoutineDialog(
            name = routine.name,
            onConfirm = { onRemoveRoutine(routine.id) },
            onDismiss = { routineToRemove = null },
        )
    }

    premiumSheetMinutes?.let { minutes ->
        PremiumDurationSheet(minutes = minutes, onDismiss = { premiumSheetMinutes = null })
    }
}

/** A card heading with a small icon in front, like "Bells" and "Background sound". */
@Composable
private fun CardHeading(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
    }
}

internal fun soundLabel(sound: BackgroundSound): Int = when (sound) {
    BackgroundSound.SILENCE -> R.string.sound_silence
    BackgroundSound.RAIN -> R.string.sound_rain
    BackgroundSound.FOREST -> R.string.sound_forest
}

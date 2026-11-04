package com.newjourney.feature.meditate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.newjourney.core.designsystem.component.DurationChip
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.PremiumDurationSheet
import com.newjourney.core.designsystem.component.PrimaryButton
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.model.BackgroundSound

@Composable
internal fun MeditateSetupRoute(
    onStartSession: (Int) -> Unit,
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
) {
    var premiumSheetMinutes by rememberSaveable { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        item { SectionTitle(stringResource(R.string.meditate_title)) }

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
                Text(stringResource(R.string.setup_length), style = MaterialTheme.typography.titleLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.durations) { minutes ->
                        DurationChip(
                            minutes = minutes,
                            selected = minutes == state.selectedMinutes,
                            premium = state.isLocked(minutes),
                            onClick = {
                                if (state.isLocked(minutes)) premiumSheetMinutes = minutes else onSelectDuration(minutes)
                            },
                        )
                    }
                }
            }
        }

        item {
            SoftCard {
                Text(stringResource(R.string.setup_bells), style = MaterialTheme.typography.titleLarge)
                SwitchRow(stringResource(R.string.setup_start_bell), state.settings.startBell, onStartBellChange)
                SwitchRow(stringResource(R.string.setup_end_bell), state.settings.endBell, onEndBellChange)
            }
        }

        item {
            SoftCard {
                Text(stringResource(R.string.setup_sound), style = MaterialTheme.typography.titleLarge)
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

        item {
            PrimaryButton(
                text = stringResource(R.string.setup_begin, state.selectedMinutes),
                onClick = onBegin,
                leading = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
            )
        }
    }

    premiumSheetMinutes?.let { minutes ->
        PremiumDurationSheet(minutes = minutes, onDismiss = { premiumSheetMinutes = null })
    }
}

/** A full-width row the user can tap anywhere to switch on or off. */
@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .toggleable(value = checked, onValueChange = onChange, role = Role.Switch),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

internal fun soundLabel(sound: BackgroundSound): Int = when (sound) {
    BackgroundSound.SILENCE -> R.string.sound_silence
    BackgroundSound.RAIN -> R.string.sound_rain
    BackgroundSound.FOREST -> R.string.sound_forest
}

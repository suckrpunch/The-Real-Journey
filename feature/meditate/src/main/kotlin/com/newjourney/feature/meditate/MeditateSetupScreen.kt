package com.newjourney.feature.meditate

import com.newjourney.core.model.TimerSettings
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.clickable
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.expandVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import com.newjourney.core.designsystem.component.PremiumDurationSheet
import com.newjourney.core.designsystem.component.PrimaryButton
import com.newjourney.core.designsystem.component.ScreenPadding
import com.newjourney.core.designsystem.component.ScreenTitle
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.designsystem.component.SwitchRow
import com.newjourney.core.designsystem.icon.NavIcons
import com.newjourney.core.model.BackgroundSound
import com.newjourney.core.model.BellSound
import com.newjourney.core.model.VoiceGuide

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
        onBellSelected = viewModel::setBell,
        onGuideSelected = viewModel::setGuide,
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
    onBellSelected: (BellSound) -> Unit = {},
    onGuideSelected: (VoiceGuide) -> Unit = {},
    onBegin: () -> Unit,
    onReturnToSession: () -> Unit,
    onStartRoutine: (Long) -> Unit = {},
    onSaveRoutine: (String) -> Unit = {},
    onRemoveRoutine: (Long) -> Unit = {},
) {
    var premiumSheetMinutes by rememberSaveable { mutableStateOf<Int?>(null) }
    var showSaveRoutine by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var premiumSetting by rememberSaveable { mutableStateOf(false) }
    var routineToRemove by rememberSaveable { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AppHeader()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScreenTitle(
                        title = stringResource(R.string.meditate_title),
                        subtitle = stringResource(R.string.meditate_subtitle),
                        modifier = Modifier.weight(1f),
                    )
                    SkyMark(Modifier.size(72.dp))
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.setup_length),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                // Five lengths fill the width exactly, as in the approved design; longer ones are a swipe away.
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val chipWidth = (maxWidth - 8.dp * 4) / 5
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.durations) { minutes ->
                            DurationChip(
                                minutes = minutes,
                                selected = minutes == state.selectedMinutes,
                                premium = state.isLocked(minutes),
                                onClick = {
                                    if (state.isLocked(minutes)) premiumSheetMinutes = minutes else onSelectDuration(minutes)
                                },
                                modifier = Modifier.width(chipWidth),
                            )
                        }
                    }
                }
            }
        }

        item {
            // Bells, background sound and the guiding voice, folded into one tidy panel.
            MeditationSettingsCard(
                settings = state.effectiveSettings,
                expanded = settingsOpen,
                onToggle = { settingsOpen = !settingsOpen },
            ) {
                CardHeading(NavIcons.Bell, stringResource(R.string.setup_bells))
                SwitchRow(stringResource(R.string.setup_start_bell), state.settings.startBell, onStartBellChange, compact = true)
                SwitchRow(stringResource(R.string.setup_end_bell), state.settings.endBell, onEndBellChange, compact = true)
                BellChoice(
                    state.effectiveSettings.bell,
                    onBellSelected,
                    Modifier.padding(top = 8.dp),
                    isPremium = state.isPremium,
                    onLocked = { premiumSetting = true },
                )

                SettingsDivider()
                CardHeading(NavIcons.Music, stringResource(R.string.setup_sound))
                Box {
                    Pines(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 4.dp)
                            .size(width = 84.dp, height = 64.dp),
                    )
                    Column(Modifier.selectableGroup()) {
                        BackgroundSound.entries.forEach { sound ->
                            val selected = sound == state.effectiveSettings.sound
                            val locked = sound.isPremium && !state.isPremium
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 38.dp)
                                    .selectable(
                                        selected = selected,
                                        onClick = { if (locked) premiumSetting = true else onSoundSelected(sound) },
                                        role = Role.RadioButton,
                                    ),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(selected = selected, onClick = null)
                                Text(
                                    stringResource(soundLabel(sound)),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp),
                                )
                                // Beside the name, clear of the trees in the corner.
                                if (locked) PremiumTag(Modifier.padding(start = 12.dp))
                            }
                        }
                    }
                }

                SettingsDivider()
                CardHeading(NavIcons.Meditator, stringResource(R.string.setup_guide))
                GuideChoice(
                    state.effectiveSettings.guide,
                    onGuideSelected,
                    isPremium = state.isPremium,
                    onLocked = { premiumSetting = true },
                )
            }
        }

        item {
            PrimaryButton(
                text = stringResource(R.string.setup_begin, state.selectedMinutes),
                onClick = onBegin,
                leading = { Icon(NavIcons.Play, contentDescription = null, modifier = Modifier.size(22.dp)) },
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
            summary = routineSummary(state.selectedMinutes, state.effectiveSettings),
            suggestion = suggestedRoutineName(state.selectedMinutes, state.effectiveSettings),
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
    if (premiumSetting) PremiumSettingSheet(onDismiss = { premiumSetting = false })
}

/** A card heading with a small icon in front, like "Bells" and "Background sound". */
@Composable
private fun CardHeading(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
    }
}

internal fun soundLabel(sound: BackgroundSound): Int = when (sound) {
    BackgroundSound.SILENCE -> R.string.sound_silence
    BackgroundSound.RAIN -> R.string.sound_rain
    BackgroundSound.FOREST -> R.string.sound_forest
    BackgroundSound.BEACH -> R.string.sound_beach
}

/**
 * "Meditation settings": closed, it shows a one-line summary of the choices (bells, sound, voice);
 * a tap opens it to change them. Keeps the Meditate page calm and short.
 */
@Composable
private fun MeditationSettingsCard(
    settings: TimerSettings,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    val bells = stringResource(
        when {
            settings.startBell && settings.endBell -> R.string.routine_bells_both
            settings.startBell -> R.string.routine_bells_start
            settings.endBell -> R.string.routine_bells_end
            else -> R.string.routine_bells_none
        },
    )
    val voice = when (settings.guide) {
        VoiceGuide.NONE -> null
        VoiceGuide.FEMALE -> "Gabriëlle"
        VoiceGuide.MALE -> "Amos"
    }
    val summary = listOfNotNull(bells, stringResource(soundLabel(settings.sound)), voice).joinToString(" · ")
    val stateText = stringResource(if (expanded) R.string.setup_settings_open else R.string.setup_settings_closed)

    SoftCard(padding = 16.dp, spacing = 0.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = stateText },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = null, tint = colors.primary, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.setup_settings_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() },
                )
                if (!expanded) {
                    Text(summary, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.onSurface,
                modifier = Modifier
                    .size(28.dp)
                    .rotate(rotation),
            )
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = content)
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

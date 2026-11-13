package com.newjourney.feature.meditate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.SecondaryButton
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.model.BackgroundSound
import com.newjourney.core.model.Routine
import com.newjourney.core.model.TimerSettings

/** "10 minutes · Rain · Bells at start and end" */
@Composable
internal fun routineSummary(minutes: Int, settings: TimerSettings): String {
    val bells = when {
        settings.startBell && settings.endBell -> R.string.routine_bells_both
        settings.startBell -> R.string.routine_bells_start
        settings.endBell -> R.string.routine_bells_end
        else -> R.string.routine_bells_none
    }
    return stringResource(R.string.routine_summary, minutes, stringResource(soundLabel(settings.sound)), stringResource(bells))
}

/** A friendly default name for the current choices, such as "10 minutes with Rain". */
@Composable
internal fun suggestedRoutineName(minutes: Int, settings: TimerSettings): String =
    if (settings.sound == BackgroundSound.SILENCE) {
        stringResource(R.string.routine_name_silence, minutes)
    } else {
        stringResource(R.string.routine_name_sound, minutes, stringResource(soundLabel(settings.sound)))
    }

@Composable
internal fun RoutineCard(
    routine: Routine,
    locked: Boolean,
    onBegin: () -> Unit,
    onRemove: () -> Unit,
) {
    SoftCard {
        Text(routine.name, style = MaterialTheme.typography.titleLarge)
        Text(
            routineSummary(routine.minutes, routine.settings),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SecondaryButton(
                text = stringResource(R.string.routine_begin),
                onClick = onBegin,
                modifier = Modifier.weight(1f),
                leading = {
                    Icon(if (locked) Icons.Outlined.Lock else Icons.Filled.PlayArrow, contentDescription = null)
                },
            )
            TextButton(onClick = onRemove, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.routine_remove), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
internal fun SaveRoutineDialog(
    summary: String,
    suggestion: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(suggestion) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.routine_save_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(summary, style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(Routine.MAX_NAME_LENGTH) },
                    label = { Text(stringResource(R.string.routine_name_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name); onDismiss() },
                enabled = Routine.cleanName(name).isNotEmpty(),
                modifier = Modifier.heightIn(min = MinTouchTarget),
            ) { Text(stringResource(R.string.routine_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.routine_cancel))
            }
        },
    )
}

@Composable
internal fun RemoveRoutineDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.routine_remove_title, name)) },
        text = { Text(stringResource(R.string.routine_remove_body), style = MaterialTheme.typography.bodyLarge) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.routine_remove))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.routine_keep))
            }
        },
    )
}

/** Shown under the Begin button: save these choices, or why that isn't possible right now. */
@Composable
internal fun SaveRoutineRow(canSave: Boolean, hasRoutines: Boolean, onSave: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!hasRoutines) {
            Text(
                stringResource(R.string.routine_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (canSave) {
            SecondaryButton(
                text = stringResource(R.string.routine_save_current),
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                leading = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null) },
            )
        } else {
            Text(
                stringResource(R.string.routine_full, Routine.MAX_ROUTINES),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


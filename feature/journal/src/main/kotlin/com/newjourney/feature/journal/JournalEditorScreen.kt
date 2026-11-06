package com.newjourney.feature.journal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.PrimaryButton
import com.newjourney.core.designsystem.component.SecondaryButton
import java.time.ZoneId

@Composable
internal fun JournalEditorRouteContent(
    onClose: () -> Unit,
    viewModel: JournalEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var askToSave by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.done) { if (state.done) onClose() }

    val leave: () -> Unit = {
        if (state.dirty) {
            askToSave = true
        } else {
            viewModel.discard()
        }
    }
    BackHandler(enabled = !state.done, onBack = leave)

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TextButton(onClick = leave, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Text(
                stringResource(R.string.journal_back),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (state.loading) return@Column

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val date = state.createdAt.atZone(ZoneId.systemDefault()).toLocalDate()
            Text(
                if (state.isNew) stringResource(R.string.journal_new_entry) else dayLabel(date),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.semantics { heading() },
            )
            state.sessionMinutes?.let {
                Text(
                    stringResource(R.string.journal_after_session, it),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.askBefore) {
            Question(stringResource(R.string.journal_mood_before))
            MoodPicker(selected = state.moodBefore, onSelect = viewModel::setMoodBefore)
        }

        Question(stringResource(if (state.askBefore) R.string.journal_mood_after else R.string.journal_mood_now))
        MoodPicker(selected = state.mood, onSelect = viewModel::setMood)

        if (!state.askBefore) {
            TextButton(onClick = viewModel::showBeforeQuestion, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(
                    stringResource(R.string.journal_add_before),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        Question(stringResource(R.string.journal_thoughts))
        OutlinedTextField(
            value = state.text,
            onValueChange = viewModel::setText,
            modifier = Modifier.fillMaxWidth(),
            minLines = 8,
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = { Text(stringResource(R.string.journal_thoughts_hint), style = MaterialTheme.typography.bodyLarge) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        )

        PrimaryButton(text = stringResource(R.string.journal_save), onClick = viewModel::save)
        if (!state.isNew) {
            SecondaryButton(
                text = stringResource(R.string.journal_delete),
                onClick = { confirmDelete = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (askToSave) {
        AlertDialog(
            onDismissRequest = { askToSave = false },
            title = { Text(stringResource(R.string.journal_unsaved_title)) },
            text = { Text(stringResource(R.string.journal_unsaved_body), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = {
                    askToSave = false
                    viewModel.save()
                }) { Text(stringResource(R.string.journal_save), style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                TextButton(onClick = {
                    askToSave = false
                    viewModel.discard()
                }) { Text(stringResource(R.string.journal_discard), style = MaterialTheme.typography.labelLarge) }
            },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.journal_delete_title)) },
            text = { Text(stringResource(R.string.journal_delete_body), style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete()
                }) { Text(stringResource(R.string.journal_delete_yes), style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.journal_delete_no), style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

@Composable
private fun Question(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

package com.newjourney.feature.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.newjourney.core.journal.JournalRepository
import com.newjourney.core.model.JournalEntry
import com.newjourney.core.model.Mood
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

internal data class JournalEditorUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val mood: Mood? = null,
    val moodBefore: Mood? = null,
    val text: String = "",
    val sessionMinutes: Int? = null,
    /** Shows the "before meditating" mood question. */
    val askBefore: Boolean = false,
    /** True when something changed since the entry was opened or last saved. */
    val dirty: Boolean = false,
    /** Set once the entry is saved, deleted or discarded: the screen then closes. */
    val done: Boolean = false,
)

@HiltViewModel
internal class JournalEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: JournalRepository,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<JournalEditorRoute>()
    private var entryId: Long = route.entryId

    private val _state = MutableStateFlow(JournalEditorUiState())
    val state: StateFlow<JournalEditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = if (entryId != 0L) runCatching { repository.entry(entryId) }.getOrNull() else null
            _state.value = if (existing != null) {
                JournalEditorUiState(
                    loading = false,
                    isNew = false,
                    createdAt = existing.createdAt,
                    mood = existing.mood,
                    moodBefore = existing.moodBefore,
                    text = existing.text,
                    sessionMinutes = existing.sessionMinutes,
                    askBefore = existing.moodBefore != null || existing.sessionMinutes != null,
                )
            } else {
                entryId = 0
                JournalEditorUiState(
                    loading = false,
                    mood = route.mood?.let { name -> Mood.entries.firstOrNull { it.name == name } },
                    sessionMinutes = route.afterSessionMinutes,
                    askBefore = route.afterSessionMinutes != null,
                )
            }
        }
    }

    fun setMood(mood: Mood?) = edit { it.copy(mood = mood) }
    fun setMoodBefore(mood: Mood?) = edit { it.copy(moodBefore = mood) }
    fun setText(text: String) = edit { it.copy(text = text) }
    fun showBeforeQuestion() = _state.update { it.copy(askBefore = true) }

    /** Saves and closes. An entry with no words and no mood is simply not kept. */
    fun save() {
        val current = _state.value
        val entry = JournalEntry(
            id = entryId,
            createdAt = current.createdAt,
            mood = current.mood,
            moodBefore = current.moodBefore,
            text = current.text.trim(),
            sessionMinutes = current.sessionMinutes,
        )
        viewModelScope.launch {
            when {
                !entry.isEmpty -> entryId = repository.save(entry)
                entryId != 0L -> repository.delete(entryId) // everything was cleared from an existing entry
            }
            _state.update { it.copy(dirty = false, done = true) }
        }
    }

    fun delete() {
        viewModelScope.launch {
            if (entryId != 0L) repository.delete(entryId)
            _state.update { it.copy(dirty = false, done = true) }
        }
    }

    fun discard() = _state.update { it.copy(dirty = false, done = true) }

    private fun edit(change: (JournalEditorUiState) -> JournalEditorUiState) =
        _state.update { change(it).copy(dirty = true) }
}

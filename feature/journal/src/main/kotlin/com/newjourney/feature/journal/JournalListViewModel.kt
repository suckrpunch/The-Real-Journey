package com.newjourney.feature.journal

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newjourney.core.journal.JournalRepository
import com.newjourney.core.model.JournalEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

internal enum class JournalView { LIST, CALENDAR }

internal data class JournalListUiState(
    val loading: Boolean = true,
    val failed: Boolean = false,
    val view: JournalView = JournalView.LIST,
    val query: String = "",
    /** Entries matching the search, newest first. */
    val entries: List<JournalEntry> = emptyList(),
    /** Every entry, for the calendar. */
    val all: List<JournalEntry> = emptyList(),
    val month: YearMonth = YearMonth.now(),
    val selectedDay: LocalDate = LocalDate.now(),
) {
    val hasAnyEntry: Boolean get() = all.isNotEmpty()
    val entriesByDay: Map<LocalDate, List<JournalEntry>> by lazy { all.groupBy { it.localDate() } }
}

private sealed interface Loaded {
    data class Ok(val matching: List<JournalEntry>, val all: List<JournalEntry>) : Loaded
    data object Failed : Loaded
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
internal class JournalListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    repository: JournalRepository,
) : ViewModel() {

    private val query = savedStateHandle.getStateFlow(KEY_QUERY, "")
    private val view = savedStateHandle.getStateFlow(KEY_VIEW, JournalView.LIST.name)
    private val month = savedStateHandle.getStateFlow(KEY_MONTH, YearMonth.now().toString())
    private val day = savedStateHandle.getStateFlow(KEY_DAY, LocalDate.now().toString())

    private val loaded = query.debounce(200).flatMapLatest { q ->
        val all = repository.entries
        if (q.isBlank()) {
            all.map { Loaded.Ok(it, it) as Loaded }
        } else {
            combine(repository.search(q), all) { matching, everything -> Loaded.Ok(matching, everything) as Loaded }
        }
    }.catch { emit(Loaded.Failed) }

    val uiState: StateFlow<JournalListUiState> = combine(loaded, query, view, month, day) { result, q, v, m, d ->
        val ok = result as? Loaded.Ok
        JournalListUiState(
            loading = false,
            failed = result is Loaded.Failed,
            view = JournalView.valueOf(v),
            query = q,
            entries = ok?.matching.orEmpty(),
            all = ok?.all.orEmpty(),
            month = YearMonth.parse(m),
            selectedDay = LocalDate.parse(d),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JournalListUiState())

    fun setQuery(text: String) {
        savedStateHandle[KEY_QUERY] = text
    }

    fun setView(newView: JournalView) {
        savedStateHandle[KEY_VIEW] = newView.name
    }

    fun shiftMonth(by: Long) {
        val next = YearMonth.parse(month.value).plusMonths(by)
        if (next.isAfter(YearMonth.now())) return
        savedStateHandle[KEY_MONTH] = next.toString()
    }

    fun selectDay(date: LocalDate) {
        savedStateHandle[KEY_DAY] = date.toString()
    }

    private companion object {
        const val KEY_QUERY = "journal_query"
        const val KEY_VIEW = "journal_view"
        const val KEY_MONTH = "journal_month"
        const val KEY_DAY = "journal_day"
    }
}

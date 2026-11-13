package com.newjourney.feature.journal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.newjourney.core.designsystem.component.AppHeader
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.PillShape
import com.newjourney.core.designsystem.component.PrimaryButton
import com.newjourney.core.designsystem.component.ScreenPadding
import com.newjourney.core.designsystem.component.ScreenTitle
import com.newjourney.core.designsystem.component.moodColor
import com.newjourney.core.model.JournalEntry
import com.newjourney.core.model.calendarCells
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
internal fun JournalListRoute(
    onWrite: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    viewModel: JournalListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    JournalListScreen(
        state = state,
        onWrite = onWrite,
        onOpenEntry = onOpenEntry,
        onQueryChange = viewModel::setQuery,
        onViewChange = viewModel::setView,
        onShiftMonth = viewModel::shiftMonth,
        onSelectDay = viewModel::selectDay,
    )
}

@Composable
internal fun JournalListScreen(
    state: JournalListUiState,
    onWrite: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onViewChange: (JournalView) -> Unit,
    onShiftMonth: (Long) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppHeader()
                ScreenTitle(stringResource(R.string.journal_title))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                    Text(
                        stringResource(R.string.journal_private_note),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
        item {
            PrimaryButton(
                text = stringResource(R.string.journal_write),
                onClick = onWrite,
                leading = { Icon(Icons.Outlined.Edit, contentDescription = null) },
            )
        }

        when {
            state.loading -> Unit
            state.failed -> item {
                Text(stringResource(R.string.journal_failed), style = MaterialTheme.typography.bodyLarge)
            }
            !state.hasAnyEntry -> item {
                Text(
                    stringResource(R.string.journal_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            else -> {
                item { ViewSwitch(state.view, onViewChange) }
                when (state.view) {
                    JournalView.LIST -> listItems(state, onQueryChange, onOpenEntry)
                    JournalView.CALENDAR -> calendarItems(state, onShiftMonth, onSelectDay, onOpenEntry)
                }
            }
        }
    }
}

/** List or Calendar: two halves of one pill; the chosen half is filled. */
@Composable
private fun ViewSwitch(view: JournalView, onChange: (JournalView) -> Unit) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().selectableGroup(),
    ) {
        Row {
            ViewOption(
                selected = view == JournalView.LIST,
                icon = Icons.AutoMirrored.Outlined.List,
                label = stringResource(R.string.journal_view_list),
                onClick = { onChange(JournalView.LIST) },
                modifier = Modifier.weight(1f),
            )
            ViewOption(
                selected = view == JournalView.CALENDAR,
                icon = Icons.Outlined.CalendarMonth,
                label = stringResource(R.string.journal_view_calendar),
                onClick = { onChange(JournalView.CALENDAR) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ViewOption(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = PillShape,
        color = if (selected) colors.secondaryContainer else Color.Transparent,
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Row(
            Modifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val tint = if (selected) colors.onSecondaryContainer else colors.onBackground
            Icon(icon, contentDescription = null, tint = tint)
            Text(label, style = MaterialTheme.typography.labelLarge, color = tint, modifier = Modifier.padding(start = 10.dp))
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.listItems(
    state: JournalListUiState,
    onQueryChange: (String) -> Unit,
    onOpenEntry: (Long) -> Unit,
) {
    item {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            placeholder = { Text(stringResource(R.string.journal_search), style = MaterialTheme.typography.bodyLarge) },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
            ),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (state.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.journal_search_clear))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        )
    }
    if (state.query.isNotBlank() && state.entries.isEmpty()) {
        item {
            Text(
                stringResource(R.string.journal_search_none, state.query.trim()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    val groups = state.entries.groupBy { it.localDate() }
    groups.forEach { (date, entries) ->
        item(key = "day-$date") { DayHeader(date) }
        items(entries, key = { it.id }) { entry -> EntryCard(entry, onClick = { onOpenEntry(entry.id) }) }
    }
}

@Composable
private fun DayHeader(date: LocalDate) {
    Text(
        dayLabel(date),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier
            .padding(top = 12.dp)
            .semantics { heading() },
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.calendarItems(
    state: JournalListUiState,
    onShiftMonth: (Long) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
    onOpenEntry: (Long) -> Unit,
) {
    item { MonthCalendar(state, onShiftMonth, onSelectDay) }
    val dayEntries = state.entriesByDay[state.selectedDay].orEmpty()
    item { DayHeader(state.selectedDay) }
    if (dayEntries.isEmpty()) {
        item {
            Text(
                stringResource(R.string.journal_day_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    items(dayEntries, key = { "cal-" + it.id }) { entry -> EntryCard(entry, onClick = { onOpenEntry(entry.id) }) }
}

@Composable
private fun MonthCalendar(
    state: JournalListUiState,
    onShiftMonth: (Long) -> Unit,
    onSelectDay: (LocalDate) -> Unit,
) {
    val formatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val today = LocalDate.now()
    val cells = remember(state.month) { calendarCells(state.month) }

    Surface(color = Color.Transparent) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MonthStepper(
                title = state.month.format(formatter).replaceFirstChar { it.titlecase(Locale.getDefault()) },
                onPrevious = { onShiftMonth(-1) },
                onNext = { onShiftMonth(1) },
                nextEnabled = state.month.isBefore(YearMonth.now()),
            )
            Row(Modifier.fillMaxWidth()) {
                DayOfWeek.entries.forEach { day ->
                    Text(
                        day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (date != null) {
                                DayCell(
                                    date = date,
                                    entries = state.entriesByDay[date].orEmpty(),
                                    isToday = date == today,
                                    isSelected = date == state.selectedDay,
                                    enabled = !date.isAfter(today),
                                    onClick = { onSelectDay(date) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    entries: List<JournalEntry>,
    isToday: Boolean,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val fullDate = remember(date) { date.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())) }
    val spoken = if (entries.isEmpty()) {
        fullDate
    } else {
        fullDate + ", " + pluralStringResource(R.plurals.journal_entry_count, entries.size, entries.size)
    }
    // The newest entry's mood colours the dot, so the month shows a quiet picture of how it felt.
    val dotColor = entries.firstNotNullOfOrNull { it.mood }?.let { moodColor(it) } ?: colors.primary
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) colors.secondaryContainer else Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .padding(2.dp)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, colors.primary, RoundedCornerShape(12.dp)) else Modifier)
            .semantics(mergeDescendants = true) {
                contentDescription = spoken
                selected = isSelected
            },
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "${date.dayOfMonth}",
                style = MaterialTheme.typography.titleSmall,
                color = when {
                    !enabled -> colors.onSurfaceVariant
                    isSelected -> colors.onSecondaryContainer
                    else -> colors.onSurface
                },
            )
            if (entries.isNotEmpty()) {
                Box(Modifier.padding(top = 2.dp).size(8.dp).background(dotColor, CircleShape))
            }
        }
    }
}

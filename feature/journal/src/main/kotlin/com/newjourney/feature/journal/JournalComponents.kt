package com.newjourney.feature.journal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.moodColor
import com.newjourney.core.designsystem.component.moodLabel
import com.newjourney.core.model.JournalEntry
import com.newjourney.core.model.JournalMoods
import com.newjourney.core.model.Mood
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal fun JournalEntry.localDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    createdAt.atZone(zone).toLocalDate()

/** "Today", "Yesterday" or "Wednesday 4 November". */
@Composable
internal fun dayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()) }
    return when (date) {
        today -> stringResource(R.string.journal_today)
        today.minusDays(1) -> stringResource(R.string.journal_yesterday)
        else -> {
            val text = date.format(formatter)
            if (date.year != today.year) "$text ${date.year}" else text
        }
    }
}

@Composable
internal fun timeLabel(entry: JournalEntry): String {
    val formatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()) }
    return entry.createdAt.atZone(ZoneId.systemDefault()).format(formatter)
}

/** A row of mood choices. Tapping the chosen mood again clears it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoodPicker(selected: Mood?, onSelect: (Mood?) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        JournalMoods.forEach { mood ->
            val isSelected = mood == selected
            val colors = MaterialTheme.colorScheme
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = if (isSelected) colors.primaryContainer else colors.surfaceContainerLowest,
                border = BorderStroke(if (isSelected) 2.dp else 1.5.dp, if (isSelected) colors.primary else colors.outline),
                modifier = Modifier
                    .heightIn(min = MinTouchTarget)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(if (isSelected) null else mood) },
                    ),
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(Modifier.size(18.dp).background(moodColor(mood), CircleShape))
                    Text(
                        stringResource(moodLabel(mood)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) colors.onPrimaryContainer else colors.onSurface,
                    )
                }
            }
        }
    }
}

/** A small "● Stressed → ● Peaceful" line. */
@Composable
internal fun MoodLine(entry: JournalEntry) {
    if (entry.mood == null && entry.moodBefore == null) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        entry.moodBefore?.let {
            MoodDot(it)
            Text("→", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        entry.mood?.let { MoodDot(it) }
    }
}

@Composable
private fun MoodDot(mood: Mood) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(14.dp).background(moodColor(mood), CircleShape))
        Text(stringResource(moodLabel(mood)), style = MaterialTheme.typography.bodyMedium)
    }
}

/** One entry in a list: time, moods and the first lines of text. */
@Composable
internal fun EntryCard(entry: JournalEntry, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val meta = buildList {
                add(timeLabel(entry))
                entry.sessionMinutes?.let { add(stringResource(R.string.journal_after_session_short, it)) }
            }.joinToString(" · ")
            Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            MoodLine(entry)
            if (entry.text.isNotBlank()) {
                Text(
                    entry.text.trim(),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The month's name, with large labelled buttons underneath to move between months. */
@Composable
internal fun MonthStepper(title: String, onPrevious: () -> Unit, onNext: () -> Unit, nextEnabled: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth()) {
            TextButton(onClick = onPrevious, modifier = Modifier.weight(1f).heightIn(min = MinTouchTarget)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text(
                    stringResource(R.string.journal_month_previous),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            TextButton(onClick = onNext, enabled = nextEnabled, modifier = Modifier.weight(1f).heightIn(min = MinTouchTarget)) {
                Text(
                    stringResource(R.string.journal_month_next),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

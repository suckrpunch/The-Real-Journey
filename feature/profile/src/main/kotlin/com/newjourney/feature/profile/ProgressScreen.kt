package com.newjourney.feature.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.newjourney.core.database.SessionRepository
import com.newjourney.core.datastore.UserSettingsRepository
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.designsystem.component.milestoneTitle
import com.newjourney.core.designsystem.theme.NewJourneyTheme
import com.newjourney.core.model.MilestoneProgress
import com.newjourney.core.model.ProgressStats
import com.newjourney.core.model.calendarCells
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

internal data class ProgressUiState(val stats: ProgressStats, val goalMinutes: Int)

@HiltViewModel
internal class ProgressViewModel @Inject constructor(
    sessions: SessionRepository,
    settings: UserSettingsRepository,
) : ViewModel() {
    val uiState: StateFlow<ProgressUiState?> = combine(sessions.progress(), settings.dailyGoalMinutes) { stats, goal ->
        ProgressUiState(stats, goal)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@Composable
internal fun ProgressRouteContent(onBack: () -> Unit, viewModel: ProgressViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BackButton(onBack)
        SectionTitle(stringResource(R.string.progress_title))
        val current = state ?: return@Column
        val stats = current.stats

        Totals(stats)
        if (stats.currentRun >= 2) {
            Text(pluralStringResource(R.plurals.run_current, stats.currentRun, stats.currentRun), style = MaterialTheme.typography.bodyLarge)
        }
        WeekCard(stats, current.goalMinutes)
        MonthCard(stats)
        MilestonesCard(stats)
    }
}

@Composable
private fun BackButton(onBack: () -> Unit) {
    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = MinTouchTarget)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        Text(stringResource(R.string.back), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun Totals(stats: ProgressStats) {
    val hour = stringResource(R.string.unit_hour_short)
    val minute = stringResource(R.string.unit_minute_short)
    SoftCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryNumber(formatMinutes(stats.totalMinutes, hour, minute), stringResource(R.string.stat_total_time), Modifier.weight(1f))
            SummaryNumber("${stats.sessionCount}", stringResource(R.string.stat_sessions), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryNumber("${stats.daysMeditated}", stringResource(R.string.stat_days), Modifier.weight(1f))
            SummaryNumber(
                pluralStringResource(R.plurals.days_short, stats.longestRun, stats.longestRun),
                stringResource(R.string.stat_longest_run),
                Modifier.weight(1f),
            )
        }
    }
}

/** Minutes per day for the last seven days, with a dashed line at the daily goal. */
@Composable
private fun WeekCard(stats: ProgressStats, goalMinutes: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val track = NewJourneyTheme.extraColors.progressTrack
    val goalColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxMinutes = maxOf(goalMinutes, stats.lastSevenDays.maxOf { it.minutes }, 1)
    val minuteShort = stringResource(R.string.unit_minute_short)
    SoftCard {
        Text(stringResource(R.string.progress_week), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        Text(
            stringResource(R.string.progress_week_goal, goalMinutes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            stats.lastSevenDays.forEach { day ->
                val name = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                val fullName = day.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                val spoken = stringResource(R.string.progress_day_spoken, fullName, day.minutes)
                Column(
                    Modifier
                        .weight(1f)
                        .semantics(mergeDescendants = true) { contentDescription = spoken },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        if (day.minutes > 0) "${day.minutes}" else "",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                        val goalY = size.height * (1f - goalMinutes.toFloat() / maxMinutes)
                        drawRoundRect(track, cornerRadius = CornerRadius(8.dp.toPx()))
                        val barHeight = size.height * day.minutes / maxMinutes
                        if (barHeight > 0f) {
                            drawRoundRect(
                                primary,
                                topLeft = Offset(0f, size.height - barHeight),
                                size = Size(size.width, barHeight),
                                cornerRadius = CornerRadius(8.dp.toPx()),
                            )
                        }
                        drawLine(
                            goalColor,
                            start = Offset(0f, goalY),
                            end = Offset(size.width, goalY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                        )
                    }
                    Text(
                        name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (day.date == LocalDate.now()) androidx.compose.ui.text.font.FontWeight.Bold else null,
                    )
                }
            }
        }
        Text(
            stringResource(R.string.progress_week_total, stats.lastSevenDays.sumOf { it.minutes }, minuteShort),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A month calendar; days with practice are filled. Empty days are simply empty, never marked. */
@Composable
private fun MonthCard(stats: ProgressStats) {
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthText)
    val cells = remember(month) { calendarCells(month) }
    val formatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }
    val dayFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.getDefault()) }
    val today = LocalDate.now()
    val colors = MaterialTheme.colorScheme
    val daysThisMonth = stats.minutesByDay.keys.count { YearMonth.from(it) == month }

    SoftCard {
        Text(
            month.format(formatter).replaceFirstChar { it.titlecase(Locale.getDefault()) },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().semantics { heading() },
        )
        Row(Modifier.fillMaxWidth()) {
            TextButton(
                onClick = { monthText = month.minusMonths(1).toString() },
                modifier = Modifier.weight(1f).heightIn(min = MinTouchTarget),
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Text(stringResource(R.string.month_previous), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 6.dp))
            }
            TextButton(
                onClick = { monthText = month.plusMonths(1).toString() },
                enabled = month.isBefore(YearMonth.now()),
                modifier = Modifier.weight(1f).heightIn(min = MinTouchTarget),
            ) {
                Text(stringResource(R.string.month_next), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(end = 6.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { day ->
                Text(
                    day.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            val minutes = stats.minutesByDay[date]
                            val spoken = if (minutes != null) {
                                stringResource(R.string.progress_calendar_day_done, date.format(dayFormatter), minutes)
                            } else {
                                date.format(dayFormatter)
                            }
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .then(if (minutes != null) Modifier.background(colors.primary, CircleShape) else Modifier)
                                    .then(if (date == today && minutes == null) Modifier.border(1.5.dp, colors.primary, CircleShape) else Modifier)
                                    .semantics { contentDescription = spoken },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${date.dayOfMonth}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (minutes != null) colors.onPrimary else colors.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            pluralStringResource(R.plurals.progress_month_days, daysThisMonth, daysThisMonth),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun MilestonesCard(stats: ProgressStats) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()) }
    val achieved = stats.achieved.sortedByDescending { it.achievedAt }
    val upcoming = stats.milestones.filter { it.achievedAt == null }
        .sortedByDescending { it.current.toFloat() / it.milestone.threshold }
        .take(3)
    SoftCard {
        Text(stringResource(R.string.milestones_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        if (achieved.isEmpty()) {
            Text(
                stringResource(R.string.milestones_none),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        achieved.forEach { item ->
            val date = item.achievedAt!!.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter)
            MilestoneRow(item, done = true, detail = date)
        }
        if (upcoming.isNotEmpty()) {
            Text(
                stringResource(R.string.milestones_ahead),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp).semantics { heading() },
            )
            upcoming.forEach { item -> MilestoneRow(item, done = false, detail = null) }
        }
    }
}

@Composable
private fun MilestoneRow(item: MilestoneProgress, done: Boolean, detail: String?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(
            if (done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(28.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(milestoneTitle(item.milestone)), style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LinearProgressIndicator(
                    progress = { item.current.toFloat() / item.milestone.threshold },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = NewJourneyTheme.extraColors.progressTrack,
                )
                Text(
                    "${item.current} / ${item.milestone.threshold}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}


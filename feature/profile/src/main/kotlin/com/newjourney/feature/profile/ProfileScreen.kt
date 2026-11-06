package com.newjourney.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.newjourney.core.database.SessionRepository
import com.newjourney.core.datastore.UserSettingsRepository
import com.newjourney.core.designsystem.component.DurationChip
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.SecondaryButton
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.designsystem.component.milestoneTitle
import com.newjourney.core.model.DailyGoal
import com.newjourney.core.model.DailyGoalChoices
import com.newjourney.core.model.ProgressStats
import com.newjourney.core.model.ReminderSettings
import com.newjourney.core.model.ThemeMode
import com.newjourney.core.model.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

internal data class ProfileUiState(
    val theme: ThemePreference = ThemePreference(),
    val stats: ProgressStats? = null,
    val goalMinutes: Int = DailyGoal.DEFAULT_MINUTES,
    val reminders: ReminderSettings = ReminderSettings(),
)

@HiltViewModel
internal class ProfileViewModel @Inject constructor(
    private val settings: UserSettingsRepository,
    sessions: SessionRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        settings.themePreference,
        sessions.progress(),
        settings.dailyGoalMinutes,
        settings.reminderSettings,
    ) { theme, stats, goal, reminders ->
        ProfileUiState(theme, stats, goal, reminders)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProfileUiState(stats = ProgressStats.from(emptyList(), LocalDate.now(), ZoneId.systemDefault())),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    /** Moves the night start or end by [deltaHours], wrapping around midnight. */
    fun shiftSchedule(start: Boolean, deltaHours: Int) {
        val current = uiState.value.theme
        val newStart = if (start) Math.floorMod(current.nightStartHour + deltaHours, 24) else current.nightStartHour
        val newEnd = if (!start) Math.floorMod(current.nightEndHour + deltaHours, 24) else current.nightEndHour
        viewModelScope.launch { settings.setNightSchedule(newStart, newEnd) }
    }

    fun setGoal(minutes: Int) {
        viewModelScope.launch { settings.setDailyGoalMinutes(minutes) }
    }
}

@Composable
internal fun ProfileRouteContent(
    onOpenProgress: () -> Unit,
    onOpenReminders: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SectionTitle(stringResource(R.string.profile_title))
        state.stats?.let { ProgressSummary(it, onOpenProgress) }
        GoalCard(state.goalMinutes, viewModel::setGoal)
        RemindersSummary(state.reminders, onOpenReminders)
        AppearanceCard(state.theme, viewModel::setThemeMode, viewModel::shiftSchedule)
        SoftCard {
            Text(stringResource(R.string.profile_coming_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.profile_coming_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProgressSummary(stats: ProgressStats, onOpenProgress: () -> Unit) {
    SoftCard {
        Text(stringResource(R.string.profile_progress_title), style = MaterialTheme.typography.titleLarge)
        if (stats.sessionCount == 0) {
            Text(
                stringResource(R.string.profile_progress_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryNumber(
                    value = formatMinutes(stats.totalMinutes, stringResource(R.string.unit_hour_short), stringResource(R.string.unit_minute_short)),
                    label = stringResource(R.string.stat_total_time),
                    modifier = Modifier.weight(1f),
                )
                SummaryNumber("${stats.sessionCount}", stringResource(R.string.stat_sessions), Modifier.weight(1f))
                SummaryNumber("${stats.daysMeditated}", stringResource(R.string.stat_days), Modifier.weight(1f))
            }
            if (stats.currentRun >= 2) {
                Text(
                    pluralStringResource(R.plurals.run_current, stats.currentRun, stats.currentRun),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            stats.next?.let { next ->
                Text(
                    stringResource(R.string.profile_next_milestone, stringResource(milestoneTitle(next.milestone))),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        SecondaryButton(
            text = stringResource(R.string.profile_see_progress),
            onClick = onOpenProgress,
            modifier = Modifier.fillMaxWidth(),
            leading = { Icon(Icons.Outlined.Insights, contentDescription = null) },
        )
    }
}

@Composable
internal fun SummaryNumber(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GoalCard(goal: Int, onGoal: (Int) -> Unit) {
    SoftCard {
        Text(stringResource(R.string.goal_title), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.goal_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.selectableGroup()) {
            items(DailyGoalChoices) { minutes ->
                DurationChip(minutes = minutes, selected = minutes == goal, premium = false, onClick = { onGoal(minutes) })
            }
        }
    }
}

@Composable
private fun RemindersSummary(reminders: ReminderSettings, onOpen: () -> Unit) {
    val lines = buildList {
        if (reminders.dailyOn) add(stringResource(R.string.reminders_summary_daily, formatMinuteOfDay(reminders.dailyMinuteOfDay)))
        if (reminders.eveningOn) add(stringResource(R.string.reminders_summary_evening, formatMinuteOfDay(reminders.eveningMinuteOfDay)))
        if (reminders.weeklyOn) add(stringResource(R.string.reminders_summary_weekly))
        if (reminders.encourageOn) add(stringResource(R.string.reminders_summary_return))
    }
    SoftCard {
        Text(stringResource(R.string.reminders_title), style = MaterialTheme.typography.titleLarge)
        Text(
            if (lines.isEmpty()) stringResource(R.string.reminders_summary_off) else lines.joinToString("\n"),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SecondaryButton(
            text = stringResource(R.string.reminders_open),
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth(),
            leading = { Icon(Icons.Outlined.NotificationsNone, contentDescription = null) },
        )
    }
}

@Composable
private fun AppearanceCard(
    preference: ThemePreference,
    onModeSelected: (ThemeMode) -> Unit,
    onShiftSchedule: (start: Boolean, deltaHours: Int) -> Unit,
) {
    SoftCard {
        Text(stringResource(R.string.profile_appearance), style = MaterialTheme.typography.titleLarge)
        Column(Modifier.selectableGroup()) {
            ThemeOption(ThemeMode.SYSTEM, R.string.theme_system, R.string.theme_system_hint, preference.mode, onModeSelected)
            ThemeOption(ThemeMode.DAY, R.string.theme_day, R.string.theme_day_hint, preference.mode, onModeSelected)
            ThemeOption(ThemeMode.NIGHT, R.string.theme_night, R.string.theme_night_hint, preference.mode, onModeSelected)
            ThemeOption(ThemeMode.SCHEDULED, R.string.theme_scheduled, R.string.theme_scheduled_hint, preference.mode, onModeSelected)
        }
        if (preference.mode == ThemeMode.SCHEDULED) {
            Stepper(
                label = stringResource(R.string.theme_night_starts),
                value = formatMinuteOfDay(preference.nightStartHour * 60),
                onShift = { onShiftSchedule(true, it) },
            )
            Stepper(
                label = stringResource(R.string.theme_day_returns),
                value = formatMinuteOfDay(preference.nightEndHour * 60),
                onShift = { onShiftSchedule(false, it) },
            )
        }
    }
}

@Composable
private fun ThemeOption(mode: ThemeMode, title: Int, hint: Int, current: ThemeMode, onSelected: (ThemeMode) -> Unit) {
    val selected = mode == current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget + 16.dp)
            .selectable(selected = selected, onClick = { onSelected(mode) }, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.padding(start = 16.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A value with large − and + buttons, used for times. [onShift] receives −1 or +1. */
@Composable
internal fun Stepper(label: String, value: String, onShift: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { onShift(-1) }, modifier = Modifier.size(MinTouchTarget)) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.stepper_earlier, label))
        }
        Text(value, style = MaterialTheme.typography.titleLarge)
        FilledTonalIconButton(onClick = { onShift(1) }, modifier = Modifier.size(MinTouchTarget)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.stepper_later, label))
        }
    }
}

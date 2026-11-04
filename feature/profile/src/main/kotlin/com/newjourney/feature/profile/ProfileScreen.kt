package com.newjourney.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.model.ThemeMode
import com.newjourney.core.model.ThemePreference
import kotlinx.serialization.Serializable

@Serializable
data object ProfileRoute

fun NavGraphBuilder.profileScreen() {
    composable<ProfileRoute> {
        val viewModel: ProfileViewModel = hiltViewModel()
        val preference by viewModel.themePreference.collectAsStateWithLifecycle()
        ProfileScreen(
            preference = preference,
            onModeSelected = viewModel::setThemeMode,
            onShiftSchedule = viewModel::shiftSchedule,
        )
    }
}

@Composable
internal fun ProfileScreen(
    preference: ThemePreference,
    onModeSelected: (ThemeMode) -> Unit,
    onShiftSchedule: (start: Boolean, deltaHours: Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        SectionTitle(stringResource(R.string.profile_title))

        SoftCard {
            Text(stringResource(R.string.profile_appearance), style = MaterialTheme.typography.titleLarge)
            Column(Modifier.selectableGroup()) {
                ThemeOption(ThemeMode.SYSTEM, R.string.theme_system, R.string.theme_system_hint, preference.mode, onModeSelected)
                ThemeOption(ThemeMode.DAY, R.string.theme_day, R.string.theme_day_hint, preference.mode, onModeSelected)
                ThemeOption(ThemeMode.NIGHT, R.string.theme_night, R.string.theme_night_hint, preference.mode, onModeSelected)
                ThemeOption(ThemeMode.SCHEDULED, R.string.theme_scheduled, R.string.theme_scheduled_hint, preference.mode, onModeSelected)
            }
            if (preference.mode == ThemeMode.SCHEDULED) {
                HourStepper(
                    label = stringResource(R.string.theme_night_starts),
                    hour = preference.nightStartHour,
                    onShift = { onShiftSchedule(true, it) },
                )
                HourStepper(
                    label = stringResource(R.string.theme_day_returns),
                    hour = preference.nightEndHour,
                    onShift = { onShiftSchedule(false, it) },
                )
            }
        }

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
private fun ThemeOption(
    mode: ThemeMode,
    title: Int,
    hint: Int,
    current: ThemeMode,
    onSelected: (ThemeMode) -> Unit,
) {
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
            Text(
                stringResource(hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HourStepper(label: String, hour: Int, onShift: (Int) -> Unit) {
    val time = "%02d:00".format(hour)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = { onShift(-1) }, modifier = Modifier.size(MinTouchTarget)) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.theme_earlier, label))
        }
        Text(time, style = MaterialTheme.typography.titleLarge)
        FilledTonalIconButton(onClick = { onShift(1) }, modifier = Modifier.size(MinTouchTarget)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.theme_later, label))
        }
    }
}

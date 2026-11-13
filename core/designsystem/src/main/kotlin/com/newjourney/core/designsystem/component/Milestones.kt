package com.newjourney.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.newjourney.core.designsystem.R
import com.newjourney.core.model.Milestone

/** The name of a milestone, shared by Profile and the session finish screen. */
@StringRes
fun milestoneTitle(milestone: Milestone): Int = when (milestone) {
    Milestone.FIRST_SESSION -> R.string.milestone_first_session
    Milestone.DAYS_3 -> R.string.milestone_days_3
    Milestone.FIRST_HOUR -> R.string.milestone_first_hour
    Milestone.DAYS_7 -> R.string.milestone_days_7
    Milestone.SESSIONS_10 -> R.string.milestone_sessions_10
    Milestone.HOURS_5 -> R.string.milestone_hours_5
    Milestone.SESSIONS_25 -> R.string.milestone_sessions_25
    Milestone.DAYS_30 -> R.string.milestone_days_30
    Milestone.HOURS_10 -> R.string.milestone_hours_10
    Milestone.SESSIONS_50 -> R.string.milestone_sessions_50
    Milestone.HOURS_25 -> R.string.milestone_hours_25
    Milestone.DAYS_100 -> R.string.milestone_days_100
    Milestone.SESSIONS_100 -> R.string.milestone_sessions_100
    Milestone.HOURS_50 -> R.string.milestone_hours_50
    Milestone.DAYS_365 -> R.string.milestone_days_365
}

/** A full-width row the user can tap anywhere to switch something on or off. */
@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .toggleable(value = checked, onValueChange = onChange, role = Role.Switch),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        // Day: green track with an ivory thumb. Night: cream track with a soft grey thumb.
        val night = isNightTheme()
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedThumbColor = if (night) NightSwitchThumb else MaterialTheme.colorScheme.onPrimary,
                checkedBorderColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

/** The thumb of a switch that is on, by night: a soft grey on the cream track. */
private val NightSwitchThumb = Color(0xFF8B959B)

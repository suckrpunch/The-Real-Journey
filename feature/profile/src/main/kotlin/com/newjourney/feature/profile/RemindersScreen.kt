package com.newjourney.feature.profile

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.newjourney.core.datastore.UserSettingsRepository
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.PrimaryButton
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.designsystem.component.SwitchRow
import com.newjourney.core.model.ReminderSettings
import com.newjourney.core.notifications.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class RemindersViewModel @Inject constructor(
    private val settings: UserSettingsRepository,
    private val scheduler: ReminderScheduler,
) : ViewModel() {

    val reminders: StateFlow<ReminderSettings> =
        settings.reminderSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderSettings())

    /** Saves the change, then plans or cancels the reminders to match. */
    fun update(change: (ReminderSettings) -> ReminderSettings) {
        viewModelScope.launch {
            settings.setReminderSettings(change(settings.reminderSettings.first()))
            scheduler.rescheduleAll()
        }
    }
}

@Composable
internal fun RemindersRouteContent(onBack: () -> Unit, viewModel: RemindersViewModel = hiltViewModel()) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-checked when returning from the phone's settings.
    var allowed by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) allowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        allowed = granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val askPermission = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
    /** Turning a reminder on asks for notification permission first, if needed. */
    fun turn(on: Boolean, change: (ReminderSettings) -> ReminderSettings) {
        if (on && !allowed) askPermission()
        viewModel.update(change)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = MinTouchTarget)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            Text(stringResource(R.string.back), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
        }
        SectionTitle(stringResource(R.string.reminders_title))
        Text(
            stringResource(R.string.reminders_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (reminders.anyOn && !allowed) {
            SoftCard(raised = true) {
                Text(stringResource(R.string.reminders_blocked_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.reminders_blocked_body), style = MaterialTheme.typography.bodyLarge)
                PrimaryButton(text = stringResource(R.string.reminders_allow), onClick = askPermission)
            }
        }

        SoftCard {
            SwitchRow(stringResource(R.string.reminders_daily), reminders.dailyOn, { on -> turn(on) { it.copy(dailyOn = on) } })
            if (reminders.dailyOn) {
                Stepper(
                    label = stringResource(R.string.reminders_at),
                    value = formatMinuteOfDay(reminders.dailyMinuteOfDay),
                    onShift = { step -> viewModel.update { it.copy(dailyMinuteOfDay = shift(it.dailyMinuteOfDay, step)) } },
                )
                SwitchRow(
                    stringResource(R.string.reminders_daily_skip),
                    reminders.dailySkipIfDone,
                    { on -> viewModel.update { it.copy(dailySkipIfDone = on) } },
                )
            }
        }

        SoftCard {
            SwitchRow(stringResource(R.string.reminders_evening), reminders.eveningOn, { on -> turn(on) { it.copy(eveningOn = on) } })
            if (reminders.eveningOn) {
                Stepper(
                    label = stringResource(R.string.reminders_at),
                    value = formatMinuteOfDay(reminders.eveningMinuteOfDay),
                    onShift = { step -> viewModel.update { it.copy(eveningMinuteOfDay = shift(it.eveningMinuteOfDay, step)) } },
                )
            }
        }

        SoftCard {
            SwitchRow(stringResource(R.string.reminders_weekly), reminders.weeklyOn, { on -> turn(on) { it.copy(weeklyOn = on) } })
            Hint(stringResource(R.string.reminders_weekly_hint))
        }

        SoftCard {
            SwitchRow(stringResource(R.string.reminders_return), reminders.encourageOn, { on -> turn(on) { it.copy(encourageOn = on) } })
            Hint(stringResource(R.string.reminders_return_hint))
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
}

/** Moves a time by 15 minutes, wrapping around midnight. */
private fun shift(minuteOfDay: Int, step: Int): Int = Math.floorMod(minuteOfDay + step * 15, 24 * 60)

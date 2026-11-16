package com.newjourney.app

import android.app.Application
import android.content.Context
import com.newjourney.core.datastore.AppLanguage
import com.newjourney.core.notifications.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NewJourneyApplication : Application() {

    @Inject lateinit var reminders: ReminderScheduler

    /** Notifications and reminders use the chosen language too (Android 12 and older). */
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLanguage.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        // Make sure the planned reminders match the saved settings (for example after an update).
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch { reminders.rescheduleAll() }
    }
}

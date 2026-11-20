package com.newjourney.feature.meditate.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.annotation.RawRes
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.newjourney.core.catalog.CatalogRepository
import com.newjourney.core.datastore.AppLanguage
import com.newjourney.core.model.BackgroundSound
import com.newjourney.core.model.BellSound
import com.newjourney.core.model.GuideSchedule
import com.newjourney.core.model.TimerSettings
import com.newjourney.core.model.VoiceGuide
import com.newjourney.core.playback.LoopingTrackPlayer
import com.newjourney.feature.meditate.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Keeps a meditation running while the screen is off or the app is in the background.
 * It follows [MeditationTimer.state]: plays the bells, the looping background sound and the spoken
 * guide, shows a notification with Pause and End on the lock screen, and rings the end bell on time.
 */
@AndroidEntryPoint
class MeditationService : Service() {

    /** The session notification uses the app's chosen language (Android 12 and older). */
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    @Inject lateinit var timer: MeditationTimer
    @Inject lateinit var catalog: CatalogRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var stateJob: Job? = null
    private var endJob: Job? = null
    private var fadeJob: Job? = null

    private var backgroundPlayer: ExoPlayer? = null
    private var musicPlayer: LoopingTrackPlayer? = null
    private var musicJob: Job? = null
    private var bellPlayer: ExoPlayer? = null

    // The spoken guide: an opening part after the start bell and a closing part before the end.
    private var guidePlayer: ExoPlayer? = null
    private var guideJob: Job? = null
    private var guidePlan: GuideSchedule? = null
    @RawRes private var guideIntro = 0
    @RawRes private var guideOutro = 0
    private var introStarted = false
    private var outroStarted = false
    /** True while a part of the guide is being spoken (also while paused mid-sentence). */
    private var guideSpeaking = false

    /** The level of the background sound or music: lowered while the voice speaks. */
    private var bedLevel = 1f

    private var wakeLock: PowerManager.WakeLock? = null

    private var currentSessionId: Long? = null
    private var isForeground = false
    private var finishing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> timer.pause()
            ACTION_RESUME -> timer.resume()
            ACTION_END -> timer.endEarly()
        }
        // Android requires a foreground notification within seconds of starting.
        if (!isForeground) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(timer.state.value),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0,
            )
            isForeground = true
        }
        if (stateJob == null) {
            stateJob = scope.launch { timer.state.collect { onState(it) } }
        }
        return START_NOT_STICKY
    }

    private fun onState(state: TimerState) {
        when (state) {
            is TimerState.Running -> {
                finishing = false
                if (currentSessionId != state.session.id) {
                    currentSessionId = state.session.id
                    beginAudio(state.session.settings, state.session.trackId)
                    beginGuide(state.session)
                } else {
                    resumeAudio()
                    runGuide()
                }
                val remaining = state.remainingMs()
                acquireWakeLock(remaining + WAKE_LOCK_MARGIN_MS)
                endJob?.cancel()
                endJob = scope.launch {
                    delay(remaining)
                    timer.complete()
                }
                showNotification(state)
            }

            is TimerState.Paused -> {
                endJob?.cancel()
                pauseGuide()
                pauseAudio()
                releaseWakeLock()
                showNotification(state)
            }

            is TimerState.Finished -> {
                endJob?.cancel()
                if (!finishing) {
                    finishing = true
                    stopGuide()
                    showNotification(state)
                    scope.launch {
                        finishAudio(
                            ringEndBell = !state.endedEarly && state.session.settings.endBell,
                            bell = state.session.settings.bell,
                        )
                        // A new session may have started while the bell rang; keep running for it.
                        val now = timer.state.value
                        if (now !is TimerState.Running && now !is TimerState.Paused) shutDown()
                    }
                }
            }

            TimerState.Idle -> {
                endJob?.cancel()
                // While finishing, let the end bell ring out; shutDown follows it.
                if (!finishing) shutDown()
            }
        }
    }

    // ---- Sound ----

    private fun beginAudio(settings: TimerSettings, trackId: String?) {
        bedLevel = 1f
        if (settings.startBell) playBell(settings.bell)
        if (trackId != null) {
            beginMusic(trackId)
            return
        }
        val sound = soundResource(settings.sound) ?: return
        val player = backgroundPlayer ?: createBackgroundPlayer().also { backgroundPlayer = it }
        player.setMediaItem(MediaItem.fromUri(rawUri(sound)))
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.volume = 0f
        player.prepare()
        player.play()
        fadeTo(player, bedLevel, FADE_IN_MS)
    }

    /** The track loops seamlessly for the whole session (see LoopingTrackPlayer). */
    private fun beginMusic(trackId: String) {
        musicJob?.cancel()
        musicJob = scope.launch {
            val track = catalog.get().track(trackId) ?: return@launch
            val player = musicPlayer ?: LoopingTrackPlayer(this@MeditationService, scope).also { musicPlayer = it }
            player.start(track)
            // The voice may already be speaking; the music then starts underneath it.
            if (bedLevel < 1f) player.fadeTo(bedLevel, 1L)
            // The session may have been paused while the catalog loaded.
            if (timer.state.value is TimerState.Paused) player.pause()
        }
    }

    private fun pauseAudio() {
        fadeJob?.cancel()
        backgroundPlayer?.pause()
        musicPlayer?.pause()
    }

    private fun resumeAudio() {
        musicPlayer?.resume()
        val player = backgroundPlayer ?: return
        player.play()
        fadeTo(player, bedLevel, RESUME_FADE_MS)
    }

    // ---- Spoken guide ----

    /** Prepares the guide for a new session: which voice, and when each part plays. */
    private fun beginGuide(session: ActiveSession) {
        stopGuide()
        introStarted = false
        outroStarted = false
        guidePlan = null
        val (intro, outro) = when (session.settings.guide) {
            VoiceGuide.NONE -> return
            VoiceGuide.FEMALE -> R.raw.guide_female_intro to R.raw.guide_female_outro
            VoiceGuide.MALE -> R.raw.guide_male_intro to R.raw.guide_male_outro
        }
        guideIntro = intro
        guideOutro = outro
        // Reading the two lengths takes a moment; then runGuide takes over (and replaces this job).
        guideJob = scope.launch {
            val (introMs, outroMs) = withContext(Dispatchers.IO) { durationMs(intro) to durationMs(outro) }
            guidePlan = GuideSchedule.plan(session.totalMs, introMs, outroMs, session.settings.startBell)
            if (timer.state.value is TimerState.Running) runGuide()
        }
    }

    /**
     * Plays whatever part of the guide is due, from where the session is now. Called when a session
     * starts and each time it resumes: a sentence that was paused carries on where it stopped.
     */
    private fun runGuide() {
        val plan = guidePlan ?: return
        val state = timer.state.value as? TimerState.Running ?: return
        guideJob?.cancel()
        val player = guidePlayer
        if (guideSpeaking && player != null) {
            player.play()
            setBedLevel(DUCK_LEVEL, DUCK_MS)
        }
        guideJob = scope.launch {
            fun elapsed() = state.session.totalMs - state.remainingMs()
            if (!introStarted) {
                delay(plan.introStartMs - elapsed())
                introStarted = true
                speak(guideIntro)
            }
            if (!outroStarted) {
                delay(plan.outroStartMs - elapsed())
                outroStarted = true
                speak(guideOutro)
            }
        }
    }

    private fun speak(@RawRes part: Int) {
        val player = guidePlayer ?: createGuidePlayer().also { guidePlayer = it }
        player.setMediaItem(MediaItem.fromUri(rawUri(part)))
        player.prepare()
        player.play()
        guideSpeaking = true
        setBedLevel(DUCK_LEVEL, DUCK_MS)
    }

    private fun pauseGuide() {
        guideJob?.cancel()
        guidePlayer?.pause()
    }

    /** Ends the guide at once, for example when the session is ended early. */
    private fun stopGuide() {
        guideJob?.cancel()
        guidePlayer?.stop()
        guideSpeaking = false
    }

    private fun createGuidePlayer(): ExoPlayer =
        ExoPlayer.Builder(this)
            // Like the bell, the voice does not take audio focus, so the background sound continues.
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                false,
            )
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED && guideSpeaking) {
                            guideSpeaking = false
                            setBedLevel(1f, UNDUCK_MS)
                        }
                    }
                })
            }

    /** Softens the background sound or music while the voice speaks, and brings it back after. */
    private fun setBedLevel(level: Float, durationMs: Long) {
        bedLevel = level
        backgroundPlayer?.let { fadeTo(it, level, durationMs) }
        musicPlayer?.fadeTo(level, durationMs)
    }

    private fun durationMs(@RawRes res: Int): Long = runCatching {
        resources.openRawResourceFd(res).use { fd ->
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } finally {
                retriever.release()
            }
        }
    }.getOrDefault(0L)

    private suspend fun finishAudio(ringEndBell: Boolean, bell: BellSound) {
        musicJob?.cancel()
        musicPlayer?.let { player ->
            player.fadeOut(MUSIC_FADE_OUT_MS).join()
            player.release()
            musicPlayer = null
        }
        backgroundPlayer?.let { player ->
            fadeTo(player, 0f, FADE_OUT_MS).join()
            player.stop()
        }
        if (ringEndBell) {
            playBell(bell)
            delay(BELL_RING_OUT_MS)
        }
    }

    private fun createBackgroundPlayer(): ExoPlayer =
        ExoPlayer.Builder(this)
            // The background sound manages audio focus, so it pauses for phone calls.
            .setAudioAttributes(mediaAttributes(), true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

    /** The bell that is loaded in [bellPlayer]. */
    private var loadedBell: BellSound? = null

    private fun bell(sound: BellSound): ExoPlayer {
        val player = bellPlayer ?: ExoPlayer.Builder(this)
            // The bell does not take audio focus, so it never interrupts the background sound.
            .setAudioAttributes(mediaAttributes(), false)
            .build()
            .also { bellPlayer = it }
        if (loadedBell != sound) {
            player.setMediaItem(MediaItem.fromUri(rawUri(bellResource(sound))))
            player.prepare()
            loadedBell = sound
        }
        return player
    }

    private fun playBell(sound: BellSound) {
        val player = bell(sound)
        player.seekTo(0)
        player.play()
    }

    private fun fadeTo(player: ExoPlayer, target: Float, durationMs: Long): Job {
        fadeJob?.cancel()
        val startVolume = player.volume
        val steps = (durationMs / FADE_STEP_MS).toInt().coerceAtLeast(1)
        return scope.launch {
            for (step in 1..steps) {
                player.volume = startVolume + (target - startVolume) * step / steps
                delay(FADE_STEP_MS)
            }
        }.also { fadeJob = it }
    }

    private fun mediaAttributes() = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    @RawRes
    private fun bellResource(sound: BellSound): Int = when (sound) {
        BellSound.SINGING_BOWL -> R.raw.bell
        BellSound.TIBETAN -> R.raw.bell_tibetan
    }

    @RawRes
    private fun soundResource(sound: BackgroundSound): Int? = when (sound) {
        BackgroundSound.SILENCE -> null
        BackgroundSound.RAIN -> R.raw.rain
        BackgroundSound.FOREST -> R.raw.forest
        BackgroundSound.BEACH -> R.raw.beach
    }

    private fun rawUri(@RawRes res: Int): Uri = Uri.parse("android.resource://$packageName/$res")

    // ---- Staying awake ----

    /** A partial wake lock keeps the CPU (not the screen) awake so the end bell rings on time. */
    private fun acquireWakeLock(timeoutMs: Long) {
        releaseWakeLock()
        val power = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NewJourney:meditation").apply {
            setReferenceCounted(false)
            acquire(timeoutMs)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    // ---- Notification ----

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun showNotification(state: TimerState) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: TimerState): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_meditation)
            .setContentTitle(getString(R.string.notification_title))
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openAppIntent())

        val remainingMinutes = ((state.remainingMs() + 59_999) / 60_000).toInt()
        when (state) {
            is TimerState.Running -> builder
                .setContentText(resources.getQuantityString(R.plurals.notification_minutes_left, remainingMinutes, remainingMinutes))
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() + state.remainingMs())
                .addAction(0, getString(R.string.timer_pause), actionIntent(ACTION_PAUSE))
                .addAction(0, getString(R.string.timer_end), actionIntent(ACTION_END))

            is TimerState.Paused -> builder
                .setContentText(resources.getQuantityString(R.plurals.notification_paused, remainingMinutes, remainingMinutes))
                .setShowWhen(false)
                .addAction(0, getString(R.string.timer_resume), actionIntent(ACTION_RESUME))
                .addAction(0, getString(R.string.timer_end), actionIntent(ACTION_END))

            else -> builder.setContentText(getString(R.string.notification_finishing)).setShowWhen(false)
        }
        return builder.build()
    }

    private fun openAppIntent(): PendingIntent? {
        val launch = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        launch.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        return PendingIntent.getActivity(this, 0, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun actionIntent(action: String): PendingIntent = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, MeditationService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    // ---- Shutting down ----

    private fun shutDown() {
        endJob?.cancel()
        fadeJob?.cancel()
        musicJob?.cancel()
        musicPlayer?.release()
        musicPlayer = null
        backgroundPlayer?.release()
        backgroundPlayer = null
        bellPlayer?.release()
        bellPlayer = null
        loadedBell = null
        guideJob?.cancel()
        guidePlayer?.release()
        guidePlayer = null
        guidePlan = null
        guideSpeaking = false
        releaseWakeLock()
        currentSessionId = null
        finishing = false
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        isForeground = false
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        musicPlayer?.release()
        backgroundPlayer?.release()
        bellPlayer?.release()
        guidePlayer?.release()
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "meditation_session"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_START = "com.newjourney.meditate.START"
        private const val ACTION_PAUSE = "com.newjourney.meditate.PAUSE"
        private const val ACTION_RESUME = "com.newjourney.meditate.RESUME"
        private const val ACTION_END = "com.newjourney.meditate.END"

        private const val FADE_IN_MS = 4_000L
        private const val RESUME_FADE_MS = 1_500L
        private const val FADE_OUT_MS = 3_000L
        /** Music gets a longer, gentler fade at the end of a session. */
        private const val MUSIC_FADE_OUT_MS = 6_000L
        private const val FADE_STEP_MS = 50L
        /** The bells are about 7.3 and 8.1 seconds long; this lets either ring out fully. */
        private const val BELL_RING_OUT_MS = 9_000L
        private const val WAKE_LOCK_MARGIN_MS = 60_000L
        /** The background sound or music while the voice speaks: softer, but still there. */
        private const val DUCK_LEVEL = 0.35f
        private const val DUCK_MS = 1_500L
        private const val UNDUCK_MS = 3_000L

        internal fun startIntent(context: Context): Intent =
            Intent(context, MeditationService::class.java).setAction(ACTION_START)
    }
}

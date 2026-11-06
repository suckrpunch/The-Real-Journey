package com.newjourney.core.playback

import android.content.Context
import android.media.AudioAttributes as PlatformAudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import com.newjourney.core.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Plays a track for as long as a session lasts, repeating it without a seam.
 *
 * It starts at 0:00 (with the artist's own fade-in). Shortly before the loop's out point a
 * second player starts the same track at the loop's in point, and the two crossfade with an
 * equal-power curve. The players then swap roles. Crossfade volumes follow the incoming
 * player's position, so pausing in the middle of a crossfade resumes exactly where it was.
 * File boundaries are never reached, so there is nothing to click.
 *
 * Audio focus is handled here once for both players, so they never pause each other.
 * Must be used from the main thread.
 */
class LoopingTrackPlayer(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private var track: Track? = null
    private var active: ExoPlayer? = null
    private var standby: ExoPlayer? = null
    private var crossfading = false
    private var paused = false
    private var pausedByFocusLoss = false

    /** Overall level, used for the session's fade-out. */
    private var master = 1f
    private var monitor: Job? = null
    private var masterFade: Job? = null

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(
            PlatformAudioAttributes.Builder()
                .setUsage(PlatformAudioAttributes.USAGE_MEDIA)
                .setContentType(PlatformAudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        .setOnAudioFocusChangeListener { change -> onFocusChange(change) }
        .build()

    fun start(track: Track) {
        release()
        this.track = track
        master = 1f
        paused = false
        audioManager.requestAudioFocus(focusRequest)

        active = newPlayer(track).apply {
            repeatMode = if (track.loop == null) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            playWhenReady = true
            prepare()
        }
        track.loop?.let { loop ->
            standby = newPlayer(track).apply {
                prepare()
                seekTo(loop.inMs)
                playWhenReady = false
                volume = 0f
            }
        }
        monitor = scope.launch { watch() }
    }

    fun pause() {
        paused = true
        active?.pause()
        if (crossfading) standby?.pause()
    }

    fun resume() {
        paused = false
        pausedByFocusLoss = false
        audioManager.requestAudioFocus(focusRequest)
        active?.play()
        if (crossfading) standby?.play()
    }

    /** Fades everything out over [durationMs], then stops. Returns the fade's job to wait on. */
    fun fadeOut(durationMs: Long): Job {
        masterFade?.cancel()
        val from = master
        return scope.launch {
            val steps = (durationMs / STEP_MS).toInt().coerceAtLeast(1)
            for (i in 1..steps) {
                master = from * (1f - i.toFloat() / steps)
                applyVolumes()
                delay(STEP_MS)
            }
            master = 0f
            applyVolumes()
            stopPlayers()
        }.also { masterFade = it }
    }

    fun release() {
        monitor?.cancel()
        masterFade?.cancel()
        monitor = null
        masterFade = null
        active?.release()
        standby?.release()
        active = null
        standby = null
        crossfading = false
        track = null
        audioManager.abandonAudioFocusRequest(focusRequest)
    }

    private fun stopPlayers() {
        // Marked as paused so the watcher does not start another crossfade after the fade-out.
        paused = true
        active?.pause()
        standby?.pause()
    }

    private suspend fun watch() {
        while (scope.isActive) {
            val loop = track?.loop
            val current = active
            val next = standby
            if (loop != null && current != null && next != null && !paused) {
                if (crossfading) {
                    val progress = ((next.currentPosition - loop.inMs).toFloat() / loop.crossfadeMs).coerceIn(0f, 1f)
                    applyVolumes(progress)
                    if (progress >= 1f) {
                        // The new copy has taken over: park the old one at the in point for next time.
                        current.pause()
                        current.volume = 0f
                        current.seekTo(loop.inMs)
                        active = next
                        standby = current
                        crossfading = false
                        applyVolumes()
                    }
                } else if (current.currentPosition >= loop.outMs - loop.crossfadeMs) {
                    next.volume = 0f
                    next.play()
                    crossfading = true
                }
            }
            delay(STEP_MS)
        }
    }

    private fun applyVolumes(progress: Float? = null) {
        if (crossfading && progress != null) {
            active?.volume = cos(progress * PI / 2).toFloat() * master
            standby?.volume = sin(progress * PI / 2).toFloat() * master
        } else if (!crossfading) {
            active?.volume = master
        }
        // During a crossfade with no new progress value the watcher sets the volumes on its next tick.
    }

    private fun onFocusChange(change: Int) {
        when (change) {
            AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (!paused) {
                    pausedByFocusLoss = true
                    pause()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> if (pausedByFocusLoss) resume()
        }
    }

    private fun newPlayer(track: Track): ExoPlayer =
        ExoPlayer.Builder(context)
            // Focus is handled above for both players together.
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                false,
            )
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
            .apply {
                setSeekParameters(SeekParameters.EXACT)
                setMediaItem(MediaItem.fromUri(track.audioUri))
            }

    private companion object {
        const val STEP_MS = 50L
    }
}

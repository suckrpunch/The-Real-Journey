package com.newjourney.feature.meditate

import android.os.SystemClock
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.newjourney.core.designsystem.icon.NavIcons
import com.newjourney.core.designsystem.theme.NewJourneyTheme
import com.newjourney.feature.meditate.timer.MeditationTimer
import com.newjourney.feature.meditate.timer.TimerState
import com.newjourney.feature.meditate.timer.remainingMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
internal class SessionBarViewModel @Inject constructor(private val timer: MeditationTimer) : ViewModel() {
    val state = timer.state
    fun pause() = timer.pause()
    fun resume() = timer.resume()
}

/**
 * While a meditation is running or paused and its screen is closed, this bar stays above the tabs
 * (like the mini player): the time left, Pause or Resume, and a tap takes you back to the session.
 *
 * Returns true when it is showing, so the music mini player can step aside.
 */
@Composable
fun ActiveSessionBar(onOpen: () -> Unit): Boolean {
    val viewModel: SessionBarViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state !is TimerState.Running && state !is TimerState.Paused) return false
    val running = state is TimerState.Running

    val remaining by produceState(initialValue = state.remainingMs(), state) {
        while (true) {
            value = state.remainingMs(SystemClock.elapsedRealtime())
            if (state !is TimerState.Running) break
            delay(500)
        }
    }
    val seconds = ((remaining + 999) / 1000).toInt()
    val time = "%02d:%02d".format(seconds / 60, seconds % 60)
    val detail = stringResource(if (running) R.string.session_bar_left else R.string.session_bar_paused, time)
    val openLabel = stringResource(R.string.session_bar_open)
    val toggleLabel = stringResource(if (running) R.string.timer_pause else R.string.timer_resume)
    val colors = MaterialTheme.colorScheme

    // The lotus breathes softly while the session runs.
    val breath by rememberInfiniteTransition(label = "sessionBar").animateFloat(
        initialValue = 0.9f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "lotus",
    )

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(24.dp),
        color = colors.primary,
        contentColor = colors.onPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp)
            .semantics { contentDescription = "$openLabel. $detail" },
    ) {
        Row(
            Modifier
                .heightIn(min = 72.dp)
                .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = NewJourneyTheme.extraColors.playerArt,
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        NavIcons.Lotus,
                        contentDescription = null,
                        tint = NewJourneyTheme.extraColors.onPlayerArt,
                        modifier = Modifier
                            .size(30.dp)
                            .scale(if (running) breath else 1f),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.session_bar_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimary.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = { if (running) viewModel.pause() else viewModel.resume() },
                modifier = Modifier
                    .size(56.dp)
                    .semantics { contentDescription = toggleLabel },
            ) {
                Icon(
                    if (running) NavIcons.Pause else NavIcons.Play,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
    return true
}

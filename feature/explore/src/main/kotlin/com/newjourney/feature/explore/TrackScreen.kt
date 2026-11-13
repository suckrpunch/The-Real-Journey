package com.newjourney.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.newjourney.core.designsystem.component.DurationChip
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.PremiumDurationSheet
import com.newjourney.core.designsystem.component.PrimaryButton
import com.newjourney.core.designsystem.component.SecondaryButton
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.designsystem.component.TrackArtwork
import kotlinx.coroutines.delay

@Composable
internal fun TrackRouteContent(
    onMeditateWithTrack: (trackId: String, minutes: Int) -> Unit,
    onBack: () -> Unit,
    viewModel: TrackViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val track = state.track
    var premiumMinutes by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BackButton(onBack)
        if (track == null) return@Column

        TrackArtwork(
            state.colorHex,
            Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .aspectRatio(1f)
                .clip(MaterialTheme.shapes.extraLarge)
                .align(Alignment.CenterHorizontally),
            coverId = track.id,
        )

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(track.title, style = MaterialTheme.typography.displaySmall)
            Text(
                listOfNotNull(track.subtitle, state.place.ifEmpty { null }).joinToString(" · "),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            track.description?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
            }
        }

        SecondaryButton(
            text = stringResource(if (state.isFavorite) R.string.track_favorite_saved else R.string.track_favorite_save),
            onClick = viewModel::toggleFavorite,
            leading = {
                Icon(if (state.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = null)
            },
        )

        PlayerControls(
            state = state,
            durationMs = state.nowPlaying?.durationMs?.takeIf { it > 0 } ?: track.durationMs,
            positionMs = { viewModel.positionMs() },
            onPlayPause = viewModel::playOrToggle,
            onSeek = viewModel::seekTo,
            onNext = viewModel::next,
            onPrevious = viewModel::previous,
        )

        if (state.nowPlaying?.failed == true) {
            SoftCard(raised = true) {
                Text(stringResource(R.string.track_offline_title), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.track_offline_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.SelfImprovement, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.track_meditate_title), style = MaterialTheme.typography.titleLarge)
            }
            Text(
                stringResource(R.string.track_meditate_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.durations) { minutes ->
                    DurationChip(
                        minutes = minutes,
                        selected = minutes == state.selectedMinutes,
                        premium = state.isLocked(minutes),
                        onClick = { if (state.isLocked(minutes)) premiumMinutes = minutes else viewModel.selectMinutes(minutes) },
                    )
                }
            }
            PrimaryButton(
                text = stringResource(R.string.track_meditate_begin, state.selectedMinutes),
                onClick = {
                    viewModel.prepareForSession()
                    onMeditateWithTrack(track.id, state.selectedMinutes)
                },
            )
        }
    }

    premiumMinutes?.let { PremiumDurationSheet(minutes = it, onDismiss = { premiumMinutes = null }) }
}

@Composable
private fun PlayerControls(
    state: TrackUiState,
    durationMs: Long,
    positionMs: () -> Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    // Follows the player while this screen is visible; zero when this track is not loaded.
    val position by produceState(initialValue = 0L, state.isCurrent, state.isPlaying) {
        while (true) {
            value = if (state.isCurrent) positionMs() else 0L
            if (!state.isPlaying) break
            delay(500)
        }
    }
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = dragging?.toLong() ?: position
    val positionText = formatDuration(shown)
    val durationText = formatDuration(durationMs)
    val progressSpoken = stringResource(R.string.track_progress_spoken, positionText, durationText)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Slider(
            value = shown.toFloat().coerceIn(0f, durationMs.toFloat().coerceAtLeast(1f)),
            onValueChange = { dragging = it },
            onValueChangeFinished = {
                dragging?.let { onSeek(it.toLong()) }
                dragging = null
            },
            valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
            enabled = state.isCurrent,
            modifier = Modifier.semantics { stateDescription = progressSpoken },
        )
        Row(Modifier.fillMaxWidth()) {
            Text(positionText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                durationText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val previousLabel = stringResource(R.string.track_previous)
            val nextLabel = stringResource(R.string.track_next)
            FilledTonalIconButton(
                onClick = onPrevious,
                enabled = state.isCurrent,
                modifier = Modifier.size(MinTouchTarget + 8.dp).semantics { contentDescription = previousLabel },
            ) { Icon(Icons.Filled.SkipPrevious, contentDescription = null) }
            val playLabel = stringResource(if (state.isPlaying) R.string.track_pause else R.string.track_play)
            FilledIconButton(
                onClick = onPlayPause,
                shape = CircleShape,
                modifier = Modifier.size(88.dp).semantics { contentDescription = playLabel },
            ) {
                Icon(
                    if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                )
            }
            FilledTonalIconButton(
                onClick = onNext,
                enabled = state.isCurrent && state.nowPlaying?.hasNext == true,
                modifier = Modifier.size(MinTouchTarget + 8.dp).semantics { contentDescription = nextLabel },
            ) { Icon(Icons.Filled.SkipNext, contentDescription = null) }
        }
    }
}

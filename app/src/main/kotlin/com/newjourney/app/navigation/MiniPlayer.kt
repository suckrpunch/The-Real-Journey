package com.newjourney.app.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.newjourney.app.R
import com.newjourney.core.playback.MusicPlayer
import com.newjourney.core.playback.NowPlaying
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MiniPlayerViewModel @Inject constructor(
    private val player: MusicPlayer,
) : ViewModel() {
    val nowPlaying: StateFlow<NowPlaying?> = player.nowPlaying

    init {
        // Connect at start-up so music that is already playing shows here straight away.
        player.connect()
    }

    fun togglePlayPause() = player.togglePlayPause()
}

/** A slim bar above the tabs while music is loaded. Tap it to open the player. */
@Composable
fun MiniPlayer(onOpen: (trackId: String) -> Unit, viewModel: MiniPlayerViewModel = hiltViewModel()) {
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val current = nowPlaying ?: return
    val openLabel = stringResource(R.string.mini_player_open, current.title)
    val toggleLabel = stringResource(if (current.isPlaying) R.string.mini_player_pause else R.string.mini_player_play)

    Surface(
        onClick = { onOpen(current.trackId) },
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = openLabel },
    ) {
        Row(
            Modifier
                .heightIn(min = 72.dp)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(current.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                current.subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            FilledIconButton(
                onClick = viewModel::togglePlayPause,
                modifier = Modifier.size(56.dp).semantics { contentDescription = toggleLabel },
            ) {
                Icon(if (current.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
            }
        }
    }
}

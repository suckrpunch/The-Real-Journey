package com.newjourney.app.navigation

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
import com.newjourney.core.designsystem.component.isNightTheme
import com.newjourney.core.designsystem.icon.NavIcons
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

/**
 * A floating pill above the tabs while music is loaded: artwork square, title, and a play/pause
 * button. In the primary colour, so it stands out calmly (forest green by day, cream by night).
 * Tap it to open the player.
 */
@Composable
fun MiniPlayer(onOpen: (trackId: String) -> Unit, viewModel: MiniPlayerViewModel = hiltViewModel()) {
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val current = nowPlaying ?: return
    val openLabel = stringResource(R.string.mini_player_open, current.title)
    val toggleLabel = stringResource(if (current.isPlaying) R.string.mini_player_pause else R.string.mini_player_play)
    val colors = MaterialTheme.colorScheme
    val night = isNightTheme()

    Surface(
        onClick = { onOpen(current.trackId) },
        shape = RoundedCornerShape(24.dp),
        color = colors.primary,
        contentColor = colors.onPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp)
            .semantics { contentDescription = openLabel },
    ) {
        Row(
            Modifier
                .heightIn(min = 72.dp)
                .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // By day a lighter green square with a white note; by night a navy square with a cream note.
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (night) colors.onPrimary else colors.onPrimary.copy(alpha = 0.18f),
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        NavIcons.Music,
                        contentDescription = null,
                        tint = if (night) colors.primary else colors.onPrimary,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    current.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                current.subtitle?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onPrimary.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            IconButton(
                onClick = viewModel::togglePlayPause,
                modifier = Modifier.size(56.dp).semantics { contentDescription = toggleLabel },
            ) {
                Icon(
                    if (current.isPlaying) NavIcons.Pause else NavIcons.Play,
                    contentDescription = null,
                    tint = colors.onPrimary,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

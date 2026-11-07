package com.newjourney.feature.explore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.newjourney.core.catalog.CatalogRepository
import com.newjourney.core.database.LibraryRepository
import com.newjourney.core.billing.EntitlementRepository
import com.newjourney.core.model.Entitlement
import com.newjourney.core.model.TimerLimits
import com.newjourney.core.model.Track
import com.newjourney.core.model.TrackTier
import com.newjourney.core.playback.MusicPlayer
import com.newjourney.core.playback.NowPlaying
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class TrackUiState(
    val track: Track? = null,
    val colorHex: String? = null,
    val place: String = "",
    val isFavorite: Boolean = false,
    /** Set when this track is the one in the music player. */
    val nowPlaying: NowPlaying? = null,
    val durations: List<Int> = TimerLimits.quickStartMinutes,
    val selectedMinutes: Int = TimerLimits.DEFAULT_MINUTES,
    val entitlement: Entitlement = Entitlement(),
) {
    val isCurrent: Boolean get() = nowPlaying != null
    val isPlaying: Boolean get() = nowPlaying?.isPlaying == true
    fun isLocked(minutes: Int): Boolean = !entitlement.isPremium && TimerLimits.isPremiumDuration(minutes)
}

@HiltViewModel
internal class TrackViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val catalogRepository: CatalogRepository,
    private val library: LibraryRepository,
    private val player: MusicPlayer,
    private val entitlements: EntitlementRepository,
) : ViewModel() {

    private val trackId = savedStateHandle.toRoute<TrackRoute>().trackId

    /** Premium as confirmed by Google Play (see EntitlementRepository). */
    private val entitlement: Entitlement get() = entitlements.entitlement.value

    private val selectedMinutes = savedStateHandle.getStateFlow(KEY_MINUTES, TimerLimits.DEFAULT_MINUTES)

    val uiState: StateFlow<TrackUiState> = combine(
        catalogRepository.catalog,
        library.favoriteIds,
        player.nowPlaying,
        selectedMinutes,
    ) { catalog, favorites, nowPlaying, minutes ->
        val track = catalog?.track(trackId)
        val category = if (catalog != null && track != null) catalog.category(track.categoryId) else null
        val sub = category?.subcategories?.firstOrNull { it.id == track?.subcategoryId }
        TrackUiState(
            track = track,
            colorHex = sub?.color,
            place = listOfNotNull(category?.name, sub?.name).joinToString(" · "),
            isFavorite = trackId in favorites,
            nowPlaying = nowPlaying?.takeIf { it.trackId == trackId },
            selectedMinutes = minutes,
            entitlement = entitlement,
        )
    }.combine(entitlements.entitlement) { state, current -> state.copy(entitlement = current) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TrackUiState())

    fun positionMs(): Long = player.positionMs()

    /** Plays this track, then the rest of its category in order. Toggles if it is already loaded. */
    fun playOrToggle() {
        val state = uiState.value
        if (state.isCurrent) {
            player.togglePlayPause()
            return
        }
        viewModelScope.launch {
            val catalog = catalogRepository.get()
            val track = catalog.track(trackId) ?: return@launch
            val queue = catalog.tracksInCategory(track.categoryId)
                .filter { it.tier == TrackTier.FREE || entitlement.isPremium }
            val index = queue.indexOfFirst { it.id == trackId }.coerceAtLeast(0)
            player.play(queue.ifEmpty { listOf(track) }, index)
        }
    }

    fun seekTo(positionMs: Long) = player.seekTo(positionMs)
    fun next() = player.next()
    fun previous() = player.previous()

    fun toggleFavorite() {
        val favorite = !uiState.value.isFavorite
        viewModelScope.launch { library.setFavorite(trackId, favorite) }
    }

    fun selectMinutes(minutes: Int) {
        if (!uiState.value.isLocked(minutes)) savedStateHandle[KEY_MINUTES] = minutes
    }

    /** Stops the music player before a session takes over, so the two never play together. */
    fun prepareForSession() = player.pause()

    private companion object {
        const val KEY_MINUTES = "track_session_minutes"
    }
}

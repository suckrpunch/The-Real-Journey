package com.newjourney.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.newjourney.core.catalog.CatalogRepository
import com.newjourney.core.database.LibraryRepository
import com.newjourney.core.designsystem.component.AppHeader
import com.newjourney.core.designsystem.component.ScreenPadding
import com.newjourney.core.designsystem.component.ScreenTitle
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.designsystem.component.categoryIcon
import com.newjourney.core.model.Category
import com.newjourney.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal data class CategorySummary(val category: Category, val trackCount: Int)

/** A track plus the colour of its sub-category, for artwork. */
internal data class TrackItem(val track: Track, val colorHex: String?)

internal data class ExploreUiState(
    val loading: Boolean = true,
    val favorites: List<TrackItem> = emptyList(),
    val recent: List<TrackItem> = emptyList(),
    val categories: List<CategorySummary> = emptyList(),
)

@HiltViewModel
internal class ExploreViewModel @Inject constructor(
    catalogRepository: CatalogRepository,
    library: LibraryRepository,
) : ViewModel() {

    val uiState: StateFlow<ExploreUiState> = combine(
        catalogRepository.catalog.filterNotNull(),
        library.favoriteIds,
        library.recent(limit = 5),
    ) { catalog, favoriteIds, recent ->
        val colors = catalog.categories.flatMap { it.subcategories }.associate { it.id to it.color }
        fun item(id: String) = catalog.track(id)?.let { TrackItem(it, colors[it.subcategoryId]) }
        ExploreUiState(
            loading = false,
            favorites = favoriteIds.mapNotNull(::item),
            recent = recent.mapNotNull { item(it.trackId) },
            categories = catalog.categories.map { CategorySummary(it, catalog.tracksInCategory(it.id).size) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreUiState())
}

@Composable
internal fun ExploreRouteContent(
    onOpenCategory: (String) -> Unit,
    onOpenTrack: (String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExploreScreen(state, onOpenCategory, onOpenTrack)
}

@Composable
internal fun ExploreScreen(
    state: ExploreUiState,
    onOpenCategory: (String) -> Unit,
    onOpenTrack: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppHeader()
                ScreenTitle(stringResource(R.string.explore_title))
            }
        }

        if (state.favorites.isNotEmpty()) {
            item { SubTitle(stringResource(R.string.explore_favorites)) }
            items(state.favorites, key = { "fav-" + it.track.id }) { item ->
                TrackRow(item.track, item.colorHex, locked = false, onClick = { onOpenTrack(item.track.id) })
            }
        }

        if (state.recent.isNotEmpty()) {
            item { SubTitle(stringResource(R.string.explore_recent)) }
            items(state.recent, key = { "recent-" + it.track.id }) { item ->
                TrackRow(item.track, item.colorHex, locked = false, onClick = { onOpenTrack(item.track.id) })
            }
        }

        item { SubTitle(stringResource(R.string.explore_categories)) }
        items(state.categories, key = { it.category.id }) { summary ->
            CategoryCard(summary, onClick = { onOpenCategory(summary.category.id) })
        }
    }
}

@Composable
private fun SubTitle(text: String) {
    SectionTitle(text, Modifier.padding(top = 16.dp))
}

@Composable
private fun CategoryCard(summary: CategorySummary, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            Modifier
                .heightIn(min = 80.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        categoryIcon(summary.category.icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(summary.category.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    if (summary.trackCount > 0) {
                        pluralStringResource(R.plurals.explore_track_count, summary.trackCount, summary.trackCount)
                    } else {
                        stringResource(R.string.explore_coming_soon)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

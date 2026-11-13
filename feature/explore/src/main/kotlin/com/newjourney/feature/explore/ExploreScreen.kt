package com.newjourney.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.newjourney.core.billing.EntitlementRepository
import com.newjourney.core.catalog.CatalogRepository
import com.newjourney.core.database.LibraryRepository
import com.newjourney.core.designsystem.component.AppHeader
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.ScreenPadding
import com.newjourney.core.designsystem.component.ScreenTitle
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.designsystem.component.categoryIcon
import com.newjourney.core.designsystem.icon.NavIcons
import com.newjourney.core.model.Category
import com.newjourney.core.model.Track
import com.newjourney.core.model.TrackTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import java.util.Locale
import javax.inject.Inject

internal data class CategorySummary(val category: Category, val trackCount: Int)

/** A track plus the colour of its sub-category, for artwork. */
internal data class TrackItem(val track: Track, val colorHex: String?, val locked: Boolean = false)

internal data class ExploreUiState(
    val loading: Boolean = true,
    val favorites: List<TrackItem> = emptyList(),
    val recent: List<TrackItem> = emptyList(),
    val categories: List<CategorySummary> = emptyList(),
    val query: String = "",
    /** Tracks matching [query]; only meaningful while a search is typed. */
    val results: List<TrackItem> = emptyList(),
) {
    val searching: Boolean get() = query.isNotBlank()
}

@HiltViewModel
internal class ExploreViewModel @Inject constructor(
    catalogRepository: CatalogRepository,
    library: LibraryRepository,
    entitlements: EntitlementRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")

    fun setQuery(value: String) {
        query.value = value
    }

    val uiState: StateFlow<ExploreUiState> = combine(
        catalogRepository.catalog.filterNotNull(),
        library.favoriteIds,
        library.recent(limit = 5),
        entitlements.entitlement,
        query,
    ) { catalog, favoriteIds, recent, entitlement, q ->
        val subs = catalog.categories.flatMap { it.subcategories }.associateBy { it.id }
        fun item(track: Track) = TrackItem(
            track = track,
            colorHex = subs[track.subcategoryId]?.color,
            locked = track.tier == TrackTier.PREMIUM && !entitlement.isPremium,
        )
        // Search looks at the title, subtitle, collection and category, ignoring case.
        val needle = q.trim().lowercase(Locale.getDefault())
        val results = if (needle.isEmpty()) {
            emptyList()
        } else {
            catalog.tracks.filter { track ->
                listOfNotNull(
                    track.title,
                    track.subtitle,
                    subs[track.subcategoryId]?.name,
                    catalog.category(track.categoryId)?.name,
                ).any { it.lowercase(Locale.getDefault()).contains(needle) }
            }.map(::item)
        }
        ExploreUiState(
            loading = false,
            favorites = favoriteIds.mapNotNull { catalog.track(it)?.let(::item) },
            recent = recent.mapNotNull { catalog.track(it.trackId)?.let(::item) },
            categories = catalog.categories.map { CategorySummary(it, catalog.tracksInCategory(it.id).size) },
            query = q,
            results = results,
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
    ExploreScreen(state, onOpenCategory, onOpenTrack, viewModel::setQuery)
}

@Composable
internal fun ExploreScreen(
    state: ExploreUiState,
    onOpenCategory: (String) -> Unit,
    onOpenTrack: (String) -> Unit,
    onQueryChange: (String) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AppHeader()
                ScreenTitle(
                    stringResource(R.string.explore_title),
                    subtitle = stringResource(R.string.explore_subtitle),
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
        item { SearchBar(state.query, onQueryChange, Modifier.padding(top = 12.dp, bottom = 6.dp)) }

        if (state.searching) {
            if (state.results.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.explore_search_none, state.query.trim()),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }
            items(state.results, key = { "result-" + it.track.id }) { item ->
                TrackRow(item.track, item.colorHex, locked = item.locked, onClick = { onOpenTrack(item.track.id) })
            }
            return@LazyColumn
        }

        items(state.categories, key = { it.category.id }) { summary ->
            CategoryRow(summary, onClick = { onOpenCategory(summary.category.id) })
        }

        if (state.favorites.isNotEmpty()) {
            item { SubTitle(stringResource(R.string.explore_favorites)) }
            items(state.favorites, key = { "fav-" + it.track.id }) { item ->
                TrackRow(item.track, item.colorHex, locked = item.locked, onClick = { onOpenTrack(item.track.id) })
            }
        }

        if (state.recent.isNotEmpty()) {
            item { SubTitle(stringResource(R.string.explore_recent)) }
            items(state.recent, key = { "recent-" + it.track.id }) { item ->
                TrackRow(item.track, item.colorHex, locked = item.locked, onClick = { onOpenTrack(item.track.id) })
            }
        }
    }
}

@Composable
private fun SubTitle(text: String) {
    SectionTitle(text, Modifier.padding(start = 4.dp, top = 20.dp))
}

/** A filled pill with a search icon, as in the approved design. */
@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    val hint = stringResource(R.string.explore_search_hint)
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .heightIn(min = 56.dp)
                .padding(start = 20.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(NavIcons.Explore, contentDescription = null, tint = colors.onSurface, modifier = Modifier.size(24.dp))
            Box(Modifier.weight(1f).padding(start = 14.dp), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(hint, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, maxLines = 1)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = hint },
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(MinTouchTarget)) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.explore_search_clear), tint = colors.onSurface)
                }
            }
        }
    }
}

/** A category: line icon, name and an arrow, on a softly rounded row. */
@Composable
private fun CategoryRow(summary: CategorySummary, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .heightIn(min = 58.dp)
                .padding(start = 20.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Icon(
                categoryIcon(summary.category.icon),
                contentDescription = null,
                tint = colors.onSurface,
                modifier = Modifier.size(28.dp),
            )
            Text(
                summary.category.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = colors.onSurface)
        }
    }
}

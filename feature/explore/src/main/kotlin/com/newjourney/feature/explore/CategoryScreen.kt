package com.newjourney.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.newjourney.core.catalog.CatalogRepository
import com.newjourney.core.designsystem.component.PremiumTrackSheet
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.billing.EntitlementRepository
import com.newjourney.core.model.Entitlement
import com.newjourney.core.model.Subcategory
import com.newjourney.core.model.TrackTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

internal data class SubcategorySection(val subcategory: Subcategory, val tracks: List<TrackItem>)

internal data class CategoryUiState(
    val name: String = "",
    val description: String? = null,
    val sections: List<SubcategorySection> = emptyList(),
    val entitlement: Entitlement = Entitlement(),
)

@HiltViewModel
internal class CategoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    catalogRepository: CatalogRepository,
    private val entitlements: EntitlementRepository,
) : ViewModel() {
    private val categoryId = savedStateHandle.toRoute<CategoryRoute>().categoryId

    /** Premium as confirmed by Google Play (see EntitlementRepository). */
    private val entitlement: Entitlement get() = entitlements.entitlement.value

    val uiState: StateFlow<CategoryUiState> = catalogRepository.catalog.filterNotNull().map { catalog ->
        val category = catalog.category(categoryId)
        CategoryUiState(
            name = category?.name.orEmpty(),
            description = category?.description,
            sections = category?.subcategories.orEmpty().map { sub ->
                SubcategorySection(sub, catalog.tracksIn(sub.id).map { TrackItem(it, sub.color) })
            },
            entitlement = entitlement,
        )
    }.combine(entitlements.entitlement) { state, current -> state.copy(entitlement = current) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryUiState())
}

@Composable
internal fun CategoryRouteContent(
    onOpenTrack: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPremium by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { BackButton(onBack) }
        item { SectionTitle(state.name) }
        state.description?.let { description ->
            item {
                Text(description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        state.sections.forEach { section ->
            item(key = "head-" + section.subcategory.id) {
                Text(
                    section.subcategory.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .semantics { heading() },
                )
            }
            if (section.tracks.isEmpty()) {
                item(key = "empty-" + section.subcategory.id) {
                    Text(
                        stringResource(R.string.category_coming_soon),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(section.tracks, key = { it.track.id }) { item ->
                val locked = item.track.tier == TrackTier.PREMIUM && !state.entitlement.isPremium
                TrackRow(
                    track = item.track,
                    colorHex = item.colorHex,
                    locked = locked,
                    onClick = { if (locked) showPremium = true else onOpenTrack(item.track.id) },
                )
            }
        }
    }

    if (showPremium) PremiumTrackSheet(onDismiss = { showPremium = false })
}

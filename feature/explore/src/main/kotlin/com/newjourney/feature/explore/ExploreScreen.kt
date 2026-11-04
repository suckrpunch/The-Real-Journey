package com.newjourney.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.model.Category
import com.newjourney.core.model.DefaultCategories
import kotlinx.serialization.Serializable

@Serializable
data object ExploreRoute

fun NavGraphBuilder.exploreScreen() {
    composable<ExploreRoute> { ExploreScreen(DefaultCategories) }
}

/**
 * Phase 1 starter: lists the categories and their sub-categories.
 * Tracks, artwork and the player arrive with the music catalog in Phase 3.
 */
@Composable
internal fun ExploreScreen(categories: List<Category>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SectionTitle(stringResource(R.string.explore_title)) }
        item {
            Text(
                stringResource(R.string.explore_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(categories, key = { it.id }) { category ->
            SoftCard {
                Text(category.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    category.subcategories.joinToString(" · ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

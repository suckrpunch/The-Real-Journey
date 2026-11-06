package com.newjourney.feature.journal

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** The Journal tab: entries as a list or a calendar, with search. */
@Serializable
data object JournalRoute

/**
 * Writing or reading one entry. [entryId] 0 = a new entry.
 * [mood] pre-selects how the writer feels (from Home); [afterSessionMinutes] marks an entry
 * written right after a meditation and asks how they felt before it too.
 */
@Serializable
data class JournalEditorRoute(
    val entryId: Long = 0,
    val mood: String? = null,
    val afterSessionMinutes: Int? = null,
)

fun NavGraphBuilder.journalScreens(
    onWrite: () -> Unit,
    onOpenEntry: (id: Long) -> Unit,
    onCloseEditor: () -> Unit,
) {
    composable<JournalRoute> {
        JournalListRoute(onWrite = onWrite, onOpenEntry = onOpenEntry)
    }
    composable<JournalEditorRoute> {
        JournalEditorRouteContent(onClose = onCloseEditor)
    }
}

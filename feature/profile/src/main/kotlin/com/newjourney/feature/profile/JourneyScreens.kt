package com.newjourney.feature.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.newjourney.core.billing.EntitlementRepository
import com.newjourney.core.catalog.JourneyContentRepository
import com.newjourney.core.database.SessionRepository
import com.newjourney.core.datastore.UserSettingsRepository
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.PrimaryButton
import com.newjourney.core.designsystem.component.SecondaryButton
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.designsystem.component.TrackArtwork
import com.newjourney.core.designsystem.component.parseCatalogColor
import com.newjourney.core.designsystem.theme.NewJourneyTheme
import com.newjourney.core.model.JourneyProgress
import com.newjourney.core.model.JourneyStage
import com.newjourney.core.model.StageContent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

internal data class JourneyUiState(
    val journey: JourneyProgress,
    val content: Map<JourneyStage, StageContent>,
    val isPremium: Boolean,
)

@HiltViewModel
internal class JourneyViewModel @Inject constructor(
    sessions: SessionRepository,
    contentRepository: JourneyContentRepository,
    entitlements: EntitlementRepository,
    private val settings: UserSettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<JourneyUiState?> = combine(
        sessions.sessionsSince(Instant.EPOCH).map { JourneyProgress.from(it) },
        flow { emit(contentRepository.content()) },
        entitlements.entitlement,
    ) { journey, content, entitlement ->
        JourneyUiState(journey, content, entitlement.isPremium)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun markSeen(stage: JourneyStage) {
        viewModelScope.launch { settings.markJourneyStagesSeen(setOf(stage.name)) }
    }
}

// ---- The path ----

@Composable
internal fun JourneyRouteContent(
    onOpenStage: (JourneyStage) -> Unit,
    onBack: () -> Unit,
    viewModel: JourneyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BackRow(onBack)
        SectionTitle(stringResource(R.string.journey_title))
        Text(
            stringResource(R.string.journey_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val current = state ?: return@Column
        NextStageCard(current)
        Column {
            JourneyStage.entries.forEachIndexed { index, stage ->
                StageRow(
                    stage = stage,
                    content = current.content[stage],
                    arrivedAt = current.journey.stages[index].arrivedAt,
                    isFirst = index == 0,
                    isLast = index == JourneyStage.entries.lastIndex,
                    isPremium = current.isPremium,
                    onClick = { onOpenStage(stage) },
                )
            }
        }
    }
}

@Composable
private fun NextStageCard(state: JourneyUiState) {
    val next = state.journey.next
    SoftCard {
        if (next == null) {
            Text(stringResource(R.string.journey_complete), style = MaterialTheme.typography.titleLarge)
        } else {
            val name = state.content[next]?.name.orEmpty()
            Text(stringResource(R.string.journey_next, name), style = MaterialTheme.typography.titleLarge)
            LinearProgressIndicator(
                progress = { state.journey.towardsNext },
                modifier = Modifier.fillMaxWidth(),
                trackColor = NewJourneyTheme.extraColors.progressTrack,
            )
            Text(
                stringResource(R.string.journey_next_count, state.journey.sessions, next.sessionsToArrive),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One stop on the path: a node on a quiet line, then the stage's name and where you are with it. */
@Composable
private fun StageRow(
    stage: JourneyStage,
    content: StageContent?,
    arrivedAt: Instant?,
    isFirst: Boolean,
    isLast: Boolean,
    isPremium: Boolean,
    onClick: () -> Unit,
) {
    val fallback = MaterialTheme.colorScheme.primary
    val color = remember(content?.color) { parseCatalogColor(content?.color, fallback) }
    val line = MaterialTheme.colorScheme.outline
    val arrived = arrivedAt != null
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()) }
    val status = when {
        arrived -> stringResource(R.string.journey_reached, arrivedAt!!.atZone(ZoneId.systemDefault()).toLocalDate().format(dateFormatter))
        else -> stringResource(R.string.journey_after, stage.sessionsToArrive)
    }
    val premiumNote = if (stage.access != JourneyStage.Access.FREE && !isPremium) stringResource(R.string.journey_premium_stage) else null

    Surface(onClick = onClick, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.heightIn(min = 96.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(
                Modifier
                    .width(48.dp)
                    .fillMaxHeight()
                    .heightIn(min = 96.dp),
            ) {
                val cx = size.width / 2
                val cy = size.height / 2
                val stroke = 2.dp.toPx()
                if (!isFirst) drawLine(line, start = Offset(cx, 0f), end = Offset(cx, cy), strokeWidth = stroke)
                if (!isLast) drawLine(line, start = Offset(cx, cy), end = Offset(cx, size.height), strokeWidth = stroke)
                val r = 14.dp.toPx()
                if (arrived) {
                    drawCircle(color, radius = r, center = Offset(cx, cy))
                } else {
                    drawCircle(color.copy(alpha = 0.15f), radius = r, center = Offset(cx, cy))
                    drawCircle(color, radius = r, center = Offset(cx, cy), style = Stroke(width = stroke))
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(content?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
                Text(content?.tagline.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (premiumNote != null) {
                    Text(premiumNote, style = MaterialTheme.typography.labelMedium, color = NewJourneyTheme.extraColors.premium)
                }
            }
        }
    }
}

// ---- One stage ----

@Composable
internal fun StageRouteContent(
    stage: JourneyStage?,
    onOpenPremium: () -> Unit,
    onBack: () -> Unit,
    viewModel: JourneyViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        BackRow(onBack)
        val current = state ?: return@Column
        if (stage == null) return@Column
        val content = current.content[stage]
        val progress = current.journey.stages.first { it.stage == stage }
        val readable = progress.arrived && stage.canRead(current.isPremium)

        LaunchedEffect(stage, progress.arrived) { if (progress.arrived) viewModel.markSeen(stage) }

        TrackArtwork(
            content?.color,
            Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .aspectRatio(1.4f)
                .clip(MaterialTheme.shapes.extraLarge)
                .align(Alignment.CenterHorizontally),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(content?.name.orEmpty(), style = MaterialTheme.typography.displaySmall, modifier = Modifier.semantics { heading() })
            Text(content?.tagline.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        when {
            readable -> {
                content?.reflection?.split("\n\n")?.forEach { paragraph ->
                    Text(paragraph.trim(), style = MaterialTheme.typography.headlineMedium.copy(fontSize = MaterialTheme.typography.bodyLarge.fontSize * 1.15f))
                }
            }
            !progress.arrived -> {
                Text(
                    stringResource(R.string.journey_stage_ahead, stage.sessionsToArrive, current.journey.sessions),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            else -> {
                SoftCard(raised = true) {
                    Text(stringResource(R.string.journey_invite_title), style = MaterialTheme.typography.titleLarge)
                    Text(
                        stringResource(R.string.journey_invite_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    PrimaryButton(text = stringResource(R.string.premium_discover_button), onClick = onOpenPremium)
                    SecondaryButton(
                        text = stringResource(R.string.premium_not_now_button),
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                    )
                }
            }
        }

        content?.opens?.takeIf { it.isNotBlank() }?.let { opens ->
            SoftCard {
                Text(stringResource(R.string.journey_opens), style = MaterialTheme.typography.titleMedium)
                Text(opens, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun BackRow(onBack: () -> Unit) {
    TextButton(onClick = onBack, modifier = Modifier.heightIn(min = MinTouchTarget)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        Text(stringResource(R.string.back), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

package com.newjourney.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.newjourney.core.designsystem.component.MinTouchTarget
import com.newjourney.core.designsystem.component.DurationChip
import com.newjourney.core.designsystem.component.PremiumDurationSheet
import com.newjourney.core.designsystem.component.PrimaryButton
import com.newjourney.core.designsystem.component.SecondaryButton
import com.newjourney.core.designsystem.component.SectionTitle
import com.newjourney.core.designsystem.component.SoftCard
import com.newjourney.core.designsystem.component.moodColor
import com.newjourney.core.designsystem.component.moodLabel
import com.newjourney.core.designsystem.component.TrackArtwork
import com.newjourney.core.designsystem.component.categoryIcon
import com.newjourney.core.designsystem.theme.NewJourneyTheme
import com.newjourney.core.model.Category
import com.newjourney.core.model.HomeCheckInMoods
import com.newjourney.core.model.Mood
import com.newjourney.core.model.PartOfDay
import com.newjourney.core.model.WeekProgress
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun HomeScreenRoute(
    onBeginMeditation: (Int) -> Unit,
    onOpenExplore: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onOpenTrack: (String) -> Unit,
    onOpenStage: (String) -> Unit,
    onMoodSelected: (Mood) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val newStage by viewModel.newStage.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        newStage = newStage,
        onOpenStage = onOpenStage,
        onSelectDuration = viewModel::selectDuration,
        onToggleQuoteSaved = viewModel::toggleQuoteSaved,
        onBeginMeditation = { onBeginMeditation(state.selectedMinutes) },
        onOpenExplore = onOpenExplore,
        onOpenCategory = onOpenCategory,
        onContinueListening = {
            viewModel.continueListening()
            state.continueListening?.let { onOpenTrack(it.track.id) }
        },
        onMoodSelected = onMoodSelected,
    )
}

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    newStage: Pair<String, String>?,
    onOpenStage: (String) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onToggleQuoteSaved: () -> Unit,
    onBeginMeditation: () -> Unit,
    onOpenExplore: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onContinueListening: () -> Unit,
    onMoodSelected: (Mood) -> Unit,
) {
    var premiumSheetMinutes by rememberSaveable { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 32.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        item { Greeting(state) }
        newStage?.let { (stageId, stageName) ->
            item {
                SoftCard(raised = true) {
                    Text(stringResource(R.string.home_new_stage), style = MaterialTheme.typography.titleMedium)
                    Text(stageName, style = MaterialTheme.typography.headlineSmall)
                    SecondaryButton(
                        text = stringResource(R.string.home_new_stage_read),
                        onClick = { onOpenStage(stageId) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        state.quote?.let { quote ->
            item {
                QuoteCard(
                    text = quote.text,
                    author = quote.author,
                    saved = state.quoteSaved,
                    onToggleSaved = onToggleQuoteSaved,
                )
            }
        }
        item {
            QuickStart(
                state = state,
                onChipTapped = { minutes ->
                    if (state.isLocked(minutes)) premiumSheetMinutes = minutes else onSelectDuration(minutes)
                },
                onBegin = onBeginMeditation,
            )
        }
        state.continueListening?.let { listening ->
            item { ContinueListeningCard(listening, onContinue = onContinueListening) }
        }
        if (state.categories.isNotEmpty()) {
            item {
                ExploreRow(
                    categories = state.categories,
                    onOpenExplore = onOpenExplore,
                    onOpenCategory = onOpenCategory,
                )
            }
        }
        item { TodayCard(state.week) }
        item { MoodCheckIn(onMoodSelected = onMoodSelected) }
    }

    premiumSheetMinutes?.let { minutes ->
        PremiumDurationSheet(minutes = minutes, onDismiss = { premiumSheetMinutes = null })
    }
}

@Composable
private fun Greeting(state: HomeUiState) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()) }
    val greeting = when (state.partOfDay) {
        PartOfDay.MORNING -> R.string.home_greeting_morning
        PartOfDay.AFTERNOON -> R.string.home_greeting_afternoon
        PartOfDay.EVENING -> R.string.home_greeting_evening
        PartOfDay.NIGHT -> R.string.home_greeting_night
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            state.date.format(formatter),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(stringResource(greeting), style = MaterialTheme.typography.displaySmall)
    }
}

@Composable
private fun QuoteCard(text: String, author: String?, saved: Boolean, onToggleSaved: () -> Unit) {
    SoftCard {
        Text(
            stringResource(R.string.home_quote_label).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text, style = MaterialTheme.typography.headlineMedium)
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                author.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = stringResource(if (saved) R.string.home_quote_saved else R.string.home_quote_save),
                onClick = onToggleSaved,
                leading = {
                    Icon(
                        if (saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@Composable
private fun QuickStart(state: HomeUiState, onChipTapped: (Int) -> Unit, onBegin: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionTitle(stringResource(R.string.home_start_title))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.durations) { minutes ->
                DurationChip(
                    minutes = minutes,
                    selected = minutes == state.selectedMinutes,
                    premium = state.isLocked(minutes),
                    onClick = { onChipTapped(minutes) },
                )
            }
        }
        PrimaryButton(
            text = stringResource(R.string.home_begin, state.selectedMinutes),
            onClick = onBegin,
            leading = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
        )
    }
}

@Composable
private fun ExploreRow(categories: List<Category>, onOpenExplore: () -> Unit, onOpenCategory: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(stringResource(R.string.home_explore_title), Modifier.weight(1f))
            TextButton(onClick = onOpenExplore, modifier = Modifier.heightIn(min = MinTouchTarget)) {
                Text(stringResource(R.string.home_explore_see_all), style = MaterialTheme.typography.labelLarge)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            items(categories, key = { it.id }) { category ->
                Surface(
                    onClick = { onOpenCategory(category.id) },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.width(150.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                        Icon(
                            categoryIcon(category.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(category.name, style = MaterialTheme.typography.titleMedium, minLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueListeningCard(item: ContinueListening, onContinue: () -> Unit) {
    val left = ((item.track.durationMs - item.positionMs).coerceAtLeast(0) + 59_999) / 60_000
    val subtitle = listOfNotNull(
        item.place.ifEmpty { null },
        pluralStringResource(R.plurals.home_continue_minutes_left, left.toInt(), left.toInt()),
    ).joinToString(" · ")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.home_continue_title))
        Surface(
            onClick = onContinue,
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = item.track.title + ", " + subtitle
            },
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TrackArtwork(item.colorHex, Modifier.size(72.dp).clip(MaterialTheme.shapes.medium))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.track.title, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(
                        progress = { (item.positionMs.toFloat() / item.track.durationMs).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        trackColor = NewJourneyTheme.extraColors.progressTrack,
                    )
                }
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(56.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayCard(week: WeekProgress) {
    val track = NewJourneyTheme.extraColors.progressTrack
    val progressColor = MaterialTheme.colorScheme.primary
    val ringDescription = stringResource(R.string.home_today_ring_description, week.todayMinutes, week.goalMinutes)
    val hasAnySession = week.daysMeditated.isNotEmpty()
    SoftCard {
        SectionTitle(stringResource(R.string.home_today_title))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(88.dp).semantics { contentDescription = ringDescription }) {
                    val stroke = 8.dp.toPx()
                    val inset = stroke / 2
                    val arcSize = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke)
                    val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                    drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(width = stroke))
                    if (week.goalFraction > 0f) {
                        drawArc(
                            progressColor, -90f, 360f * week.goalFraction, false, topLeft, arcSize,
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                Text("${week.todayMinutes}", style = MaterialTheme.typography.headlineSmall)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.home_today_minutes, week.todayMinutes, week.goalMinutes),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(
                        when {
                            week.goalReached -> R.string.home_today_goal_reached
                            week.todayMinutes > 0 -> R.string.home_today_almost
                            else -> R.string.home_today_not_yet
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (hasAnySession) {
            Text(
                pluralStringResource(R.plurals.home_week_days, week.daysMeditated.size, week.daysMeditated.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WeekDots(week)
        }
    }
}

/** Monday to Sunday. Filled for days with a session; an empty ring otherwise. Never red, never a broken streak. */
@Composable
private fun WeekDots(week: WeekProgress) {
    val filled = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.outline
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        DayOfWeek.entries.forEach { day ->
            val done = day in week.daysMeditated
            val name = day.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
            val fullName = day.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
            val spoken = stringResource(if (done) R.string.home_week_day_done else R.string.home_week_day_open, fullName)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = spoken },
            ) {
                Canvas(Modifier.size(30.dp)) {
                    if (done) {
                        drawCircle(filled)
                    } else {
                        drawCircle(empty, style = Stroke(width = 2.dp.toPx()))
                    }
                }
                Text(
                    name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (day == week.today) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MoodCheckIn(onMoodSelected: (Mood) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.home_mood_title))
        Text(
            stringResource(R.string.home_mood_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HomeCheckInMoods.forEach { mood ->
                Surface(
                    onClick = { onMoodSelected(mood) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f).heightIn(min = 76.dp),
                ) {
                    Column(
                        Modifier.padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(22.dp).background(moodColor(mood), CircleShape))
                        Text(
                            stringResource(moodLabel(mood)),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}


package com.newjourney.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newjourney.core.designsystem.R
import com.newjourney.core.designsystem.icon.NavIcons
import com.newjourney.core.designsystem.theme.NewJourneyTheme

/** Minimum touch size used across the app (Android's minimum is 48dp). */
val MinTouchTarget = 56.dp

/** Horizontal margin of every screen. */
val ScreenPadding = 16.dp

/** Main and secondary buttons are full pills. */
val PillShape = RoundedCornerShape(32.dp)

/** Opens Reminders from the bell in a screen header. Provided by the app's navigation. */
val LocalOpenReminders = staticCompositionLocalOf<(() -> Unit)?> { null }

/** True in the Night theme; decides which version of the logo to show. */
@Composable
fun isNightTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/** The main action on a screen: a full-width pill, 56dp tall, always with a text label. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.size(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** A secondary action: an outlined pill, at least 56dp tall. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable RowScope.() -> Unit)? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = MinTouchTarget),
        shape = PillShape,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.size(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** A softly rounded card. Surfaces are told apart by tone, not shadows. */
@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    raised: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (raised) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

/** Section heading (Start meditating, Today …), marked as a heading for screen readers. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.semantics { heading() },
    )
}

/** The New Journey logo, exactly as supplied: the day or night version to suit the theme. */
@Composable
fun NewJourneyLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(if (isNightTheme()) R.drawable.logo_night else R.drawable.logo_day),
        contentDescription = stringResource(R.string.logo_description),
        modifier = modifier.size(54.dp),
    )
}

/**
 * The top of the main screens: the logo on the left and the reminders bell on the right.
 * The bell only shows where the app can open Reminders.
 */
@Composable
fun AppHeader(modifier: Modifier = Modifier) {
    val openReminders = LocalOpenReminders.current
    Row(
        modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NewJourneyLogo()
        Spacer(Modifier.weight(1f))
        if (openReminders != null) {
            IconButton(onClick = openReminders, modifier = Modifier.size(48.dp)) {
                Icon(
                    NavIcons.Bell,
                    contentDescription = stringResource(R.string.header_reminders),
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}

/** A screen's big heading with an optional line below it ("Meditate" / "Create your moment of calm"). */
@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * A duration button. With [showTier] it reads "5 min / Free" or "15 min / Premium" (Home);
 * otherwise "5 / min" with a small lock on Premium lengths (Meditate). Selection is shown by
 * an outline as well as colour. Premium lengths stay tappable so the app can explain calmly.
 */
@Composable
fun DurationChip(
    minutes: Int,
    selected: Boolean,
    premium: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showTier: Boolean = false,
) {
    // Home (with tiers) marks the choice with an outline on the page colour; Meditate also tints it.
    val selectedFill = if (showTier) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.primaryContainer
    val colors = MaterialTheme.colorScheme
    val label = if (premium) {
        stringResource(R.string.duration_minutes_premium, minutes)
    } else {
        stringResource(R.string.duration_minutes, minutes)
    }
    val content = if (selected && !showTier) colors.onPrimaryContainer else colors.onSurface
    val secondary = if (selected && !showTier) colors.onPrimaryContainer else colors.onSurfaceVariant
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = if (showTier) 76.dp else 64.dp)
            .widthIn(min = 56.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                this.selected = selected
                role = Role.RadioButton
            },
        shape = RoundedCornerShape(18.dp),
        color = if (selected) selectedFill else NewJourneyTheme.extraColors.chip,
        border = if (selected) BorderStroke(2.dp, colors.primary) else null,
    ) {
        Box(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (showTier) {
                    Text(
                        stringResource(R.string.duration_short, minutes),
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, lineHeight = 24.sp),
                        color = content,
                        maxLines = 1,
                    )
                    Text(
                        stringResource(if (premium) R.string.duration_tier_premium else R.string.duration_tier_free),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = secondary,
                        maxLines = 1,
                    )
                } else {
                    Text("$minutes", style = MaterialTheme.typography.labelLarge, color = content)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.duration_min_short), style = MaterialTheme.typography.labelMedium, color = secondary)
                        if (premium) {
                            Icon(
                                Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = secondary,
                                modifier = Modifier.padding(start = 2.dp).size(12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A row of evenly spaced content with the standard gap. */
@Composable
fun SpacedRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}

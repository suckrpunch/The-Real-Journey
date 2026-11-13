package com.newjourney.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// The approved New Journey palette (Day: Warm Minimal, Night: Midnight Calm).
// Screens never use these directly; they use MaterialTheme.colorScheme or NewJourneyTheme.extraColors.

// Day — Warm Minimal
internal val WarmIvory = Color(0xFFFBF4EB)
internal val DeepForestGreen = Color(0xFF06452A)
internal val DarkForestGreen = Color(0xFF082F20)

// Approved Muted Green (#58765F) is 4.2:1 on the sage cards; this barely darker shade keeps
// the same look and passes WCAG AA (4.5:1) on every Day surface.
internal val MutedGreen = Color(0xFF4F6A56)
internal val WarmSageIvory = Color(0xFFECECDE)
internal val PaleLinen = Color(0xFFF2EDDF)
internal val PaleSage = Color(0xFFDCE9D7)
internal val WarmStone = Color(0xFFD8D6C8)
internal val WarmWhite = Color(0xFFFFF9F0)
internal val InactiveGreen = Color(0xFF214B38)

// Night — Midnight Calm
internal val MidnightNavy = Color(0xFF0E1C30)
internal val DeepNavy = Color(0xFF0B1829)
internal val WarmCream = Color(0xFFFFF1DE)
internal val SoftIvory = Color(0xFFF0EDE7)
internal val MutedNavySlate = Color(0xFF222F44)
internal val SoftSlateNavy = Color(0xFF28374B)
internal val MutedNavy = Color(0xFF263248)
internal val MutedSlate = Color(0xFF586679)

// Approved Muted Slate (#586679) is 2.9:1 on navy; borders that carry meaning (text fields,
// outlined buttons) use this lighter shade so they reach 3:1.
internal val SlateOutline = Color(0xFF6A7A91)

internal val DayColorScheme = lightColorScheme(
    primary = DeepForestGreen,
    onPrimary = WarmWhite,
    primaryContainer = PaleSage,
    onPrimaryContainer = DarkForestGreen,
    secondary = MutedGreen,
    onSecondary = WarmWhite,
    secondaryContainer = PaleSage,
    onSecondaryContainer = DarkForestGreen,
    tertiary = MutedGreen,
    onTertiary = WarmWhite,
    tertiaryContainer = PaleSage,
    onTertiaryContainer = DarkForestGreen,
    background = WarmIvory,
    onBackground = DarkForestGreen,
    surface = WarmIvory,
    onSurface = DarkForestGreen,
    surfaceVariant = PaleLinen,
    onSurfaceVariant = MutedGreen,
    surfaceContainerLowest = WarmIvory,
    surfaceContainerLow = PaleLinen,
    surfaceContainer = WarmSageIvory,
    surfaceContainerHigh = WarmSageIvory,
    surfaceContainerHighest = WarmSageIvory,
    inverseSurface = DarkForestGreen,
    inverseOnSurface = WarmIvory,
    outline = MutedGreen,
    outlineVariant = WarmStone,
)

internal val NightColorScheme = darkColorScheme(
    primary = WarmCream,
    onPrimary = MidnightNavy,
    primaryContainer = SoftSlateNavy,
    onPrimaryContainer = WarmCream,
    secondary = SoftIvory,
    onSecondary = MidnightNavy,
    secondaryContainer = MutedSlate,
    onSecondaryContainer = WarmCream,
    tertiary = SoftIvory,
    onTertiary = MidnightNavy,
    tertiaryContainer = MutedSlate,
    onTertiaryContainer = WarmCream,
    background = MidnightNavy,
    onBackground = WarmCream,
    surface = MidnightNavy,
    onSurface = WarmCream,
    surfaceVariant = MutedNavy,
    onSurfaceVariant = SoftIvory,
    surfaceContainerLowest = DeepNavy,
    surfaceContainerLow = MutedNavy,
    surfaceContainer = MutedNavySlate,
    surfaceContainerHigh = SoftSlateNavy,
    surfaceContainerHighest = SoftSlateNavy,
    inverseSurface = WarmCream,
    inverseOnSurface = MidnightNavy,
    outline = SlateOutline,
    outlineVariant = MutedSlate,
)

/** Colours Material 3 has no slot for. */
@Immutable
data class ExtraColors(
    /** Premium is shown with a lock and a label, never with a bright accent. */
    val premium: Color,
    val progressTrack: Color,
    /** Unselected duration chips. */
    val chip: Color,
    /** The selected tab's pill and the icon on it. */
    val navIndicator: Color,
    val onNavIndicator: Color,
    val navInactive: Color,
    /** The breathing circle, outside to inside. */
    val breathOuter: Color,
    val breathMiddle: Color,
    val breathInner: Color,
)

internal val DayExtraColors = ExtraColors(
    premium = MutedGreen,
    progressTrack = WarmStone,
    chip = WarmSageIvory,
    navIndicator = PaleSage,
    onNavIndicator = DeepForestGreen,
    navInactive = InactiveGreen,
    breathOuter = Color(0xFFE3ECDD),
    breathMiddle = Color(0xFFC9DBC5),
    breathInner = Color(0xFF7FA186),
)

internal val NightExtraColors = ExtraColors(
    premium = SoftIvory,
    progressTrack = MutedNavy,
    chip = MutedNavy,
    navIndicator = WarmCream,
    onNavIndicator = MidnightNavy,
    navInactive = SoftIvory,
    breathOuter = Color(0xFF1C2B40),
    breathMiddle = Color(0xFF3E5068),
    breathInner = WarmCream,
)

internal val LocalExtraColors = staticCompositionLocalOf { DayExtraColors }

package com.newjourney.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// The approved New Journey palette (Day: Warm Minimal, Night: Midnight Calm).
// Screens never use these directly; they use MaterialTheme.colorScheme or NewJourneyTheme.extraColors.

// Day — Warm Minimal
internal val WarmIvory = Color(0xFFFCF4EA)
internal val DeepForestGreen = Color(0xFF133F24)
internal val DarkForestGreen = Color(0xFF0A2716)

// Approved Muted Green (#58765F) is 4.2:1 on the sage cards; this barely darker shade keeps
// the same look and passes WCAG AA (4.5:1) on every Day surface.
internal val MutedGreen = Color(0xFF375C44)
internal val WarmSageIvory = Color(0xFFF0EDDD)
/** The search field: a touch deeper than the cards. */
internal val SearchSage = Color(0xFFECEADA)
internal val PaleLinen = Color(0xFFF2EDDF)
internal val PaleSage = Color(0xFFDCE9D7)
internal val WarmStone = Color(0xFFD8D6C8)
internal val WarmWhite = Color(0xFFFFFDF8)
internal val InactiveGreen = Color(0xFF1C3E2C)

// Night — Midnight Calm, matched to the approved Explore screens
internal val MidnightNavy = Color(0xFF0A172A)
internal val DeepNavy = Color(0xFF07111F)
internal val WarmCream = Color(0xFFFCF0E0)
/** Night text: near-white with a breath of warmth, as in the approved screens. */
internal val NightText = Color(0xFFFFFCF6)
internal val SoftIvory = Color(0xFFEEF0F2)
internal val MutedNavySlate = Color(0xFF1D2B3F)
internal val SoftSlateNavy = Color(0xFF2D3B4F)
internal val MutedNavy = Color(0xFF1D2B3F)
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
    surfaceContainerHigh = SearchSage,
    surfaceContainerHighest = SearchSage,
    inverseSurface = DarkForestGreen,
    inverseOnSurface = WarmIvory,
    outline = MutedGreen,
    outlineVariant = WarmStone,
)

internal val NightColorScheme = darkColorScheme(
    primary = WarmCream,
    onPrimary = MidnightNavy,
    primaryContainer = SoftSlateNavy,
    onPrimaryContainer = NightText,
    secondary = SoftIvory,
    onSecondary = MidnightNavy,
    secondaryContainer = MutedSlate,
    onSecondaryContainer = NightText,
    tertiary = SoftIvory,
    onTertiary = MidnightNavy,
    tertiaryContainer = MutedSlate,
    onTertiaryContainer = NightText,
    background = MidnightNavy,
    onBackground = NightText,
    surface = MidnightNavy,
    onSurface = NightText,
    surfaceVariant = MutedNavy,
    onSurfaceVariant = SoftIvory,
    surfaceContainerLowest = DeepNavy,
    surfaceContainerLow = MutedNavy,
    surfaceContainer = MutedNavySlate,
    surfaceContainerHigh = SoftSlateNavy,
    surfaceContainerHighest = SoftSlateNavy,
    inverseSurface = NightText,
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
    /** The artwork square in the mini player and its note. */
    val playerArt: Color,
    val onPlayerArt: Color,
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
    playerArt = Color(0xFF5C7F62),
    onPlayerArt = WarmWhite,
    breathOuter = Color(0xFFE6ECDC),
    breathMiddle = Color(0xFFCADAC4),
    breathInner = Color(0xFF5C7F62),
)

internal val NightExtraColors = ExtraColors(
    premium = SoftIvory,
    progressTrack = MutedNavy,
    chip = MutedNavy,
    navIndicator = WarmCream,
    onNavIndicator = MidnightNavy,
    navInactive = SoftIvory,
    playerArt = MidnightNavy,
    onPlayerArt = WarmCream,
    breathOuter = Color(0xFF16243A),
    breathMiddle = Color(0xFF384A62),
    breathInner = WarmCream,
)

internal val LocalExtraColors = staticCompositionLocalOf { DayExtraColors }

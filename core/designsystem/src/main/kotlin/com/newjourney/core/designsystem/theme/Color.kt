package com.newjourney.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Raw palette — the values from the design direction. Screens never use these directly;
// they use MaterialTheme.colorScheme or NewJourneyTheme.extraColors.

// Day
internal val Cream = Color(0xFFF6F1E7)
internal val WarmBeige = Color(0xFFEFE7D8)
internal val Linen = Color(0xFFFFFDF8)
internal val DeepForest = Color(0xFF3F5E4A)
internal val Sage = Color(0xFFA8B8A0)
internal val SageMist = Color(0xFFDDE5D6)
internal val ForestInk = Color(0xFF2B4434)
internal val Lavender = Color(0xFF9C8FB8)
internal val WarmInk = Color(0xFF2B2A26)
internal val WarmInkSecondary = Color(0xFF5E5A50)
internal val SandOutline = Color(0xFFC9BFAC)
internal val SandTrack = Color(0xFFE4DBCB)
internal val MutedGold = Color(0xFFB08D57)

// Night
internal val MidnightNavy = Color(0xFF121826)
internal val DeepSlate = Color(0xFF1C2333)
internal val CharcoalBlue = Color(0xFF252D40)
internal val MutedLavender = Color(0xFFB9AEDC)
internal val DuskViolet = Color(0xFF33335A)
internal val MoonBlue = Color(0xFF8FA8C8)
internal val WarmOffWhite = Color(0xFFE8E4DA)
internal val WarmGrey = Color(0xFFA9A69E)
internal val NightOutline = Color(0xFF3A4358)
internal val NightTrack = Color(0xFF323B50)
internal val SoftGold = Color(0xFFC9A66B)

internal val DayColorScheme = lightColorScheme(
    primary = DeepForest,
    onPrimary = Linen,
    primaryContainer = SageMist,
    onPrimaryContainer = ForestInk,
    secondary = Sage,
    onSecondary = WarmInk,
    tertiary = Lavender,
    onTertiary = Linen,
    background = Cream,
    onBackground = WarmInk,
    surface = Cream,
    onSurface = WarmInk,
    surfaceVariant = WarmBeige,
    onSurfaceVariant = WarmInkSecondary,
    surfaceContainerLowest = Linen,
    surfaceContainerLow = Linen,
    surfaceContainer = WarmBeige,
    surfaceContainerHigh = WarmBeige,
    outline = SandOutline,
    outlineVariant = SandTrack,
)

internal val NightColorScheme = darkColorScheme(
    primary = MutedLavender,
    onPrimary = MidnightNavy,
    primaryContainer = DuskViolet,
    onPrimaryContainer = WarmOffWhite,
    secondary = MoonBlue,
    onSecondary = MidnightNavy,
    tertiary = MoonBlue,
    onTertiary = MidnightNavy,
    background = MidnightNavy,
    onBackground = WarmOffWhite,
    surface = MidnightNavy,
    onSurface = WarmOffWhite,
    surfaceVariant = DeepSlate,
    onSurfaceVariant = WarmGrey,
    surfaceContainerLowest = DeepSlate,
    surfaceContainerLow = DeepSlate,
    surfaceContainer = DeepSlate,
    surfaceContainerHigh = CharcoalBlue,
    outline = NightOutline,
    outlineVariant = NightTrack,
)

/** Colours Material 3 has no slot for. */
@Immutable
data class ExtraColors(
    val premium: Color,
    val progressTrack: Color,
)

internal val DayExtraColors = ExtraColors(premium = MutedGold, progressTrack = SandTrack)
internal val NightExtraColors = ExtraColors(premium = SoftGold, progressTrack = NightTrack)

internal val LocalExtraColors = staticCompositionLocalOf { DayExtraColors }

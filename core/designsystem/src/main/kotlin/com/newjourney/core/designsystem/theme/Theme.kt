package com.newjourney.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.dp

internal val NewJourneyShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * The app theme. [nightTheme] is decided once in MainActivity from the user's
 * Day / Night / Follow device / Scheduled setting.
 */
@Composable
fun NewJourneyTheme(
    nightTheme: Boolean,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalExtraColors provides if (nightTheme) NightExtraColors else DayExtraColors,
    ) {
        MaterialTheme(
            colorScheme = if (nightTheme) NightColorScheme else DayColorScheme,
            typography = NewJourneyTypography,
            shapes = NewJourneyShapes,
            content = content,
        )
    }
}

object NewJourneyTheme {
    val extraColors: ExtraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtraColors.current
}

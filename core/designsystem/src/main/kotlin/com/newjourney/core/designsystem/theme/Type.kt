package com.newjourney.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.newjourney.core.designsystem.R

/** Fraunces: the editorial serif, only for the quote of the day and reflective text. */
val Fraunces = FontFamily(
    Font(R.font.fraunces_400, FontWeight.Normal),
    Font(R.font.fraunces_500, FontWeight.Medium),
    Font(R.font.fraunces_600, FontWeight.SemiBold),
)

/** Nunito Sans: the interface font for everything else, from bold headings to body text. */
val NunitoSans = FontFamily(
    Font(R.font.nunito_sans_400, FontWeight.Normal),
    Font(R.font.nunito_sans_500, FontWeight.Medium),
    Font(R.font.nunito_sans_600, FontWeight.SemiBold),
    Font(R.font.nunito_sans_700, FontWeight.Bold),
    Font(R.font.nunito_sans_800, FontWeight.ExtraBold),
)

/** Nunito: the rounded sister of Nunito Sans, used bold for the big headings. */
val Nunito = FontFamily(Font(R.font.nunito_700, FontWeight.Bold))

/** The quote of the day: Fraunces 24/32. */
val QuoteTextStyle = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 32.sp)

/** Reflective, editorial passages (journey reflections): Fraunces at reading size. */
val ReflectionTextStyle = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Normal, fontSize = 19.sp, lineHeight = 29.sp)

/** Bottom navigation labels: small and semi-bold, as in the approved Home design. */
val NavigationLabelStyle = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp)

// All sizes are in sp, so the phone's font-size setting scales them.
internal val NewJourneyTypography = Typography(
    // The greeting on Home, the big timer.
    displayMedium = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.ExtraBold, fontSize = 52.sp, lineHeight = 60.sp),
    displaySmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 38.sp, lineHeight = 44.sp),
    // Headings use rounded Nunito bold. Screen headings (Meditate, Journal, Profile …).
    headlineLarge = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp),
    // Section headings (Start meditating, Today …).
    headlineSmall = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    // Card titles.
    titleLarge = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    // Buttons and chip numbers.
    labelLarge = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp),
    labelMedium = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 18.sp),
    labelSmall = TextStyle(fontFamily = NunitoSans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 16.sp),
)

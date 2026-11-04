package com.newjourney.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.newjourney.core.designsystem.R
import com.newjourney.core.model.Mood

/** The translatable name of a mood, shared by Home and the Journal. */
@StringRes
fun moodLabel(mood: Mood): Int = when (mood) {
    Mood.PEACEFUL -> R.string.mood_peaceful
    Mood.HAPPY -> R.string.mood_happy
    Mood.MOTIVATED -> R.string.mood_motivated
    Mood.CALM -> R.string.mood_calm
    Mood.TIRED -> R.string.mood_tired
    Mood.STRESSED -> R.string.mood_stressed
    Mood.ANXIOUS -> R.string.mood_anxious
    Mood.SAD -> R.string.mood_sad
}

/** A soft colour for each mood; the same in both themes so a mood always looks the same. */
fun moodColor(mood: Mood): Color = when (mood) {
    Mood.PEACEFUL, Mood.CALM -> Color(0xFFA8B8A0)
    Mood.HAPPY, Mood.MOTIVATED -> Color(0xFFE3C27A)
    Mood.STRESSED, Mood.ANXIOUS -> Color(0xFFD9A38A)
    Mood.SAD -> Color(0xFF9FB3C8)
    Mood.TIRED -> Color(0xFF9C8FB8)
}

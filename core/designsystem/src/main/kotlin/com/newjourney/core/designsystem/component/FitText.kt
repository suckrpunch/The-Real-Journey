package com.newjourney.core.designsystem.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp

/**
 * Text for buttons and tabs, where words must never break in the middle ("Resum / e").
 *
 * It stays on one line and, when the words are longer than the space (other languages, larger
 * text in the phone's settings), gently shrinks down to [minFontSize]. If even that is too wide it
 * wraps onto [wrapLines] lines, always between whole words. Measured, not guessed, so it is exact.
 */
@Composable
fun FitText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Center,
    minFontSize: TextUnit = 13.sp,
    wrapLines: Int = 2,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val maxWidth = constraints.maxWidth
        val (fontSize, oneLine) = remember(text, style, maxWidth, measurer) {
            val start = if (style.fontSize.isSpecified) style.fontSize.value else 16f
            val min = if (minFontSize.isSpecified) minOf(minFontSize.value, start) else start
            if (maxWidth == Constraints.Infinity) return@remember start to true
            fun width(size: Float) =
                measurer.measure(text, style.copy(fontSize = size.sp), maxLines = 1, softWrap = false).size.width
            var size = start
            while (size > min && width(size) > maxWidth) size -= 0.5f
            size.coerceAtLeast(min) to (width(size.coerceAtLeast(min)) <= maxWidth)
        }
        val sized = style.copy(
            fontSize = fontSize.sp,
            // Keep the line height in step with a smaller font, so two lines stay compact.
            lineHeight = if (style.lineHeight.isSpecified && style.fontSize.isSpecified) {
                (style.lineHeight.value * fontSize / style.fontSize.value).sp
            } else {
                style.lineHeight
            },
        )
        Text(
            text,
            style = sized,
            color = if (color != Color.Unspecified) color else LocalContentColor.current,
            textAlign = textAlign,
            maxLines = if (oneLine) 1 else wrapLines,
            softWrap = !oneLine,
        )
    }
}

package com.newjourney.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The bottom-navigation icons from the approved design: thin, rounded line icons, with a filled
 * house for the selected Home tab. Drawn on a 24×24 grid; [androidx.compose.material3.Icon] tints them.
 */
object NavIcons {

    private const val LINE = 1.5f
    private const val LINE_BOLD = 2.2f

    private fun lineIcon(name: String, width: Float, vararg paths: String): ImageVector =
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .apply {
                paths.forEach { d ->
                    addPath(
                        pathData = addPathNodes(d),
                        fill = null,
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = width,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                    )
                }
            }
            .build()

    // A house with an arched door, softly rounded at every corner.
    private const val HOUSE = "M12 3.6 L20.4 9.6 V19.4 Q20.4 20.4 19.4 20.4 H15 V15.6 A3 3 0 0 0 9 15.6 V20.4 H4.6 Q3.6 20.4 3.6 19.4 V9.6 Z"
    private const val LENS = "M4 10.5 A6.5 6.5 0 1 0 17 10.5 A6.5 6.5 0 1 0 4 10.5 Z"
    private const val HANDLE = "M15.4 15.4 L20.5 20.5"
    private const val LOTUS_CENTER = "M12 4.5 C14.7 7.2 14.7 12.4 12 15.6 C9.3 12.4 9.3 7.2 12 4.5 Z"
    private const val LOTUS_LEFT = "M12 15.6 C10.6 11.6 7.4 9 4.6 8.8 C4.4 12.6 7.4 15.6 12 15.6 Z"
    private const val LOTUS_RIGHT = "M12 15.6 C13.4 11.6 16.6 9 19.4 8.8 C19.6 12.6 16.6 15.6 12 15.6 Z"
    private const val LOTUS_BASE = "M2.5 12.6 C3.6 15.8 7.4 17.8 12 17.8 C16.6 17.8 20.4 15.8 21.5 12.6"
    private const val PAGE = "M6.2 3.5 H17.8 A2 2 0 0 1 19.8 5.5 V18.5 A2 2 0 0 1 17.8 20.5 H6.2 A2 2 0 0 1 4.2 18.5 V5.5 A2 2 0 0 1 6.2 3.5 Z"
    private const val PAGE_LINES = "M8.2 8.5 H15.8 M8.2 12 H15.8 M8.2 15.5 H13"
    private const val HEAD = "M8 8 A4 4 0 1 0 16 8 A4 4 0 1 0 8 8 Z"
    private const val SHOULDERS = "M4.5 20.5 C4.5 16.4 7.8 13.9 12 13.9 C16.2 13.9 19.5 16.4 19.5 20.5"

    private const val BELL = "M6.2 16.8 V11.2 A5.8 5.8 0 0 1 17.8 11.2 V16.8 L19.3 18.3 H4.7 Z"
    private const val BELL_CLAPPER = "M10 20.6 A2 2 0 0 0 14 20.6"
    private const val BELL_TOP = "M12 3.6 V5.4"

    val Home: ImageVector = lineIcon("NavHome", LINE, HOUSE)

    /** The reminders bell in screen headers, in the same thin line style as the tab icons. */
    val Bell: ImageVector = lineIcon("HeaderBell", LINE, BELL, BELL_CLAPPER, BELL_TOP)

    /** The selected Home tab: a solid house with softly rounded corners. */
    val HomeFilled: ImageVector = ImageVector.Builder(
        name = "NavHomeFilled", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        addPath(
            pathData = addPathNodes(HOUSE),
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }.build()

    val Explore: ImageVector = lineIcon("NavExplore", LINE, LENS, HANDLE)
    val ExploreBold: ImageVector = lineIcon("NavExploreBold", LINE_BOLD, LENS, HANDLE)

    val Meditate: ImageVector = lineIcon("NavMeditate", LINE, LOTUS_CENTER, LOTUS_LEFT, LOTUS_RIGHT, LOTUS_BASE)
    val MeditateBold: ImageVector = lineIcon("NavMeditateBold", LINE_BOLD, LOTUS_CENTER, LOTUS_LEFT, LOTUS_RIGHT, LOTUS_BASE)

    val Journal: ImageVector = lineIcon("NavJournal", LINE, PAGE, PAGE_LINES)
    val JournalBold: ImageVector = lineIcon("NavJournalBold", LINE_BOLD, PAGE, PAGE_LINES)

    val Profile: ImageVector = lineIcon("NavProfile", LINE, HEAD, SHOULDERS)
    val ProfileBold: ImageVector = lineIcon("NavProfileBold", LINE_BOLD, HEAD, SHOULDERS)

    // Library and player icons, in the same thin line style.
    private const val MOON = "M19.5 15.2 A8.3 8.3 0 1 1 10.2 3.8 A6.6 6.6 0 0 0 19.5 15.2 Z"
    private const val WAVES = "M3 8 C5 6.4 7 6.4 9 8 C11 9.6 13 9.6 15 8 C17 6.4 19 6.4 21 8 " +
        "M3 12.5 C5 10.9 7 10.9 9 12.5 C11 14.1 13 14.1 15 12.5 C17 10.9 19 10.9 21 12.5 " +
        "M3 17 C5 15.4 7 15.4 9 17 C11 18.6 13 18.6 15 17 C17 15.4 19 15.4 21 17"
    private const val TARGET = "M3.5 12 A8.5 8.5 0 1 0 20.5 12 A8.5 8.5 0 1 0 3.5 12 Z " +
        "M7.5 12 A4.5 4.5 0 1 0 16.5 12 A4.5 4.5 0 1 0 7.5 12 Z M10.8 12 A1.2 1.2 0 1 0 13.2 12 A1.2 1.2 0 1 0 10.8 12 Z"
    private const val MEDITATOR = "M10 5 A2 2 0 1 0 14 5 A2 2 0 1 0 10 5 Z " +
        "M12 7.5 C10.2 7.5 9.4 9 9.2 11 L8.6 15.5 H15.4 L14.8 11 C14.6 9 13.8 7.5 12 7.5 Z " +
        "M9.4 10.2 L6 13.6 M14.6 10.2 L18 13.6 M4.5 19 C6.5 16.4 9.5 15.6 12 17.2 C14.5 15.6 17.5 16.4 19.5 19 Z"
    private const val LEAF = "M5 19.5 C4.4 11.5 9.6 5 19.5 4.5 C19.8 13.4 14.4 19.4 5 19.5 Z M5 19.5 C8 15.5 11 12.5 14.5 9.5"
    private const val NOTE_STEMS = "M9.5 17 V6.5 L18.5 4.8 V15.3 M9.5 9.5 L18.5 7.8"
    private const val NOTE_HEADS = "M5.5 17 A2 2 0 1 0 9.5 17 A2 2 0 1 0 5.5 17 Z M14.5 15.3 A2 2 0 1 0 18.5 15.3 A2 2 0 1 0 14.5 15.3 Z"
    private const val PAUSE = "M9 6.5 V17.5 M15 6.5 V17.5"
    private const val PLAY = "M8.5 6 L18 12 L8.5 18 Z"

    val Lotus: ImageVector = lineIcon("Lotus", LINE, LOTUS_CENTER, LOTUS_LEFT, LOTUS_RIGHT, LOTUS_BASE)
    val Moon: ImageVector = lineIcon("Moon", LINE, MOON)
    val Waves: ImageVector = lineIcon("Waves", LINE, WAVES)
    val Target: ImageVector = lineIcon("Target", LINE, TARGET)
    val Meditator: ImageVector = lineIcon("Meditator", LINE, MEDITATOR)
    val Leaf: ImageVector = lineIcon("Leaf", LINE, LEAF)

    /** Two beamed notes, for the mini player's artwork square. */
    val Music: ImageVector = ImageVector.Builder(
        name = "Music", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        addPath(addPathNodes(NOTE_STEMS), fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        addPath(addPathNodes(NOTE_HEADS), fill = SolidColor(Color.Black), stroke = SolidColor(Color.Black), strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
    }.build()

    /** Two rounded bars. */
    val Pause: ImageVector = lineIcon("Pause", 2.6f, PAUSE)

    /** A softly rounded triangle. */
    val Play: ImageVector = ImageVector.Builder(
        name = "Play", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
    ).apply {
        addPath(addPathNodes(PLAY), fill = SolidColor(Color.Black), stroke = SolidColor(Color.Black), strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
    }.build()
}

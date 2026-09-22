package com.saketkhundia.pocketserver.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Stroke icon set — one style for ALL icons.
 * 24x24 viewBox, stroke currentColor, stroke-width 1.6, round caps/joins,
 * minimal geometric outlines. Glyph color resolves to #EDEDEF at call site.
 */
private fun strokeIcon(name: String, data: List<String>): ImageVector {
    val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    data.forEach { d ->
        b.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            pathFillType = PathFillType.NonZero,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
    return b.build()
}

private fun strokedCircleIcon(name: String, strokes: List<String>, fills: List<String> = emptyList()): ImageVector {
    val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    strokes.forEach { d ->
        b.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        )
    }
    fills.forEach { d ->
        b.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            fill = SolidColor(Color.Black)
        )
    }
    return b.build()
}

object PsIcons {
    val Home: ImageVector by lazy {
        strokeIcon("home", listOf(
            "M3 10.5 12 3l9 7.5",
            "M5 9.8V20a1 1 0 0 0 1 1h4v-6h4v6h4a1 1 0 0 0 1-1V9.8"
        ))
    }
    val Folder: ImageVector by lazy {
        strokeIcon("folder", listOf(
            "M3 6a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6z"
        ))
    }
    val Files: ImageVector get() = Folder
    val Activity: ImageVector by lazy {
        strokeIcon("activity", listOf("M3 12h4l3 8 4-16 3 8h4"))
    }
    val Pulse: ImageVector get() = Activity
    val Settings: ImageVector by lazy {
        strokeIcon("gear", listOf(
            "M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z",
            "M19.4 15a1.7 1.7 0 0 0 0.3 1.9l0.1 0.1a2 2 0 1 1-2.9 2.9l-0.1-0.1a1.7 1.7 0 0 0-1.9-0.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-0.1a1.7 1.7 0 0 0-1-1.6 1.7 1.7 0 0 0-1.9 0.3l-0.1 0.1a2 2 0 1 1-2.9-2.9l0.1-0.1a1.7 1.7 0 0 0 0.3-1.9 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h0.1a1.7 1.7 0 0 0 1.6-1 1.7 1.7 0 0 0-0.3-1.9l-0.1-0.1a2 2 0 1 1 2.9-2.9l0.1 0.1a1.7 1.7 0 0 0 1.9 0.3 1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v0.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.9-0.3l0.1-0.1a2 2 0 1 1 2.9 2.9l-0.1 0.1a1.7 1.7 0 0 0-0.3 1.9 1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-0.1a1.7 1.7 0 0 0-1.5 1z"
        ))
    }
    val Gear: ImageVector get() = Settings
    val Server: ImageVector by lazy {
        strokeIcon("server", listOf(
            "M3 4h18v6H3z",
            "M3 14h18v6H3z",
            "M7 7h0.01",
            "M7 17h0.01"
        ))
    }
    val Image: ImageVector by lazy {
        strokeIcon("image", listOf(
            "M5 4h14a1 1 0 0 1 1 1v14a1 1 0 0 1-1 1H5a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1z",
            "M9 11h0.01",
            "M4 17l5-5 3.5 3.5L16 12l4 4"
        ))
    }
    val Photos: ImageVector by lazy {
        strokeIcon("camera", listOf(
            "M4 8h3l2-3h6l2 3h3v11H4V8z",
            "M12 18a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z"
        ))
    }
    val Camera: ImageVector get() = Photos
    /**
     * Media: rounded-rectangle video frame (rect x=2 y=5 w=20 h=14 rx=4,
     * stroked) with small SOLID play triangle centered inside
     * (points "10 9.2 15.5 12 10 14.8", fill currentColor, no stroke).
     */
    val Media: ImageVector by lazy {
        strokedCircleIcon(
            "media",
            strokes = listOf("M6 5h12a4 4 0 0 1 4 4v6a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V9a4 4 0 0 1 4-4z"),
            fills = listOf("M10 9.2 15.5 12 10 14.8V9.2z")
        )
    }
    val Globe: ImageVector by lazy {
        strokeIcon("globe", listOf(
            "M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z",
            "M3 12h18",
            "M12 3a15 15 0 0 1 0 18a15 15 0 0 1 0-18z"
        ))
    }
    val Share: ImageVector by lazy {
        strokeIcon("share", listOf(
            "M9 12a2.5 2.5 0 1 0 0-0.01",
            "M15.5 5.5a2.5 2.5 0 1 0 0-0.01",
            "M15.5 18.5a2.5 2.5 0 1 0 0-0.01",
            "M8.2 10.8l7.6-3.6",
            "M8.2 13.2l7.6 3.6"
        ))
    }
    val Storage: ImageVector by lazy {
        strokeIcon("database", listOf(
            "M12 3c4.4 0 8 1.1 8 2.5S16.4 8 12 8 4 6.9 4 5.5 7.6 3 12 3z",
            "M4 5.5V12c0 1.4 3.6 2.5 8 2.5s8-1.1 8-2.5V5.5",
            "M4 12v6.5c0 1.4 3.6 2.5 8 2.5s8-1.1 8-2.5V12"
        ))
    }
    val Database: ImageVector get() = Storage
    val Upload: ImageVector by lazy {
        strokeIcon("upload", listOf("M12 16V4", "M7 9l5-5 5 5", "M4 20h16"))
    }
    val Download: ImageVector by lazy {
        strokeIcon("download", listOf("M12 4v12", "M7 11l5 5 5-5", "M4 20h16"))
    }
    val Link: ImageVector by lazy {
        strokeIcon("link", listOf(
            "M10 14a4 4 0 0 0 6 0l3-3a4 4 0 0 0-6-6l-1.5 1.5",
            "M14 10a4 4 0 0 0-6 0l-3 3a4 4 0 0 0 6 6l1.5-1.5"
        ))
    }
    val Music: ImageVector by lazy {
        strokeIcon("music", listOf(
            "M9 18V6l10-2v12",
            "M11 18a2 2 0 1 1-4 0 2 2 0 1 1 4 0z",
            "M21 16a2 2 0 1 1-4 0 2 2 0 1 1 4 0z"
        ))
    }
    val ChevronRight: ImageVector by lazy {
        strokeIcon("chevron", listOf("M9 6l6 6-6 6"))
    }
    val Wifi: ImageVector by lazy {
        strokeIcon("wifi", listOf(
            "M5 10a10 10 0 0 1 14 0",
            "M8.5 13.5a5 5 0 0 1 7 0",
            "M12 18h0.01"
        ))
    }
    val Signal: ImageVector by lazy {
        strokeIcon("signal", listOf("M5 19v-6", "M10 19V9", "M15 19v-9", "M20 19V5"))
    }
    val Battery: ImageVector by lazy {
        strokeIcon("battery", listOf(
            "M3 8h16v8H3z",
            "M21 11v2",
            "M7 11v2"
        ))
    }
    val Bell: ImageVector by lazy {
        strokeIcon("bell", listOf(
            "M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6",
            "M10 20a2 2 0 0 0 4 0"
        ))
    }
    val Search: ImageVector by lazy {
        strokeIcon("search", listOf(
            "M11 18a7 7 0 1 0 0-14 7 7 0 0 0 0 14z",
            "M21 21l-4.3-4.3"
        ))
    }
    val Plus: ImageVector by lazy {
        strokeIcon("plus", listOf("M12 5v14", "M5 12h14"))
    }
    val Copy: ImageVector by lazy {
        strokeIcon("copy", listOf(
            "M9 9h11v11H9z",
            "M5 15V4h11"
        ))
    }
    val Play: ImageVector by lazy {
        strokeIcon("play", listOf("M7 4.5v15l13-7.5-13-7.5z"))
    }
    val Stop: ImageVector by lazy {
        strokeIcon("stop", listOf("M7 7h10v10H7z"))
    }
    val Qr: ImageVector by lazy {
        strokeIcon("qr", listOf(
            "M4 4h6v6H4z",
            "M14 4h6v6h-6z",
            "M4 14h6v6H4z",
            "M14 14h3v3h-3z",
            "M20 14v6h-6"
        ))
    }
    val Check: ImageVector by lazy {
        strokeIcon("check", listOf("M4 12.5l5 5L20 6.5"))
    }
    val Close: ImageVector by lazy {
        strokeIcon("close", listOf("M6 6l12 12", "M18 6L6 18"))
    }
    val Back: ImageVector by lazy {
        strokeIcon("back", listOf("M19 12H5", "M11 18l-6-6 6-6"))
    }
    val Refresh: ImageVector by lazy {
        strokeIcon("refresh", listOf("M21 12a9 9 0 1 1-2.6-6.4", "M21 3v6h-6"))
    }
    val Delete: ImageVector by lazy {
        strokeIcon("delete", listOf("M4 7h16", "M9 7V5h6v2", "M6 7l1 13h10l1-13", "M10 11v6", "M14 11v6"))
    }
}

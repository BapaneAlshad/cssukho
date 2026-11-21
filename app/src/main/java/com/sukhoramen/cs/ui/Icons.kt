package com.sukhoramen.cs.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object SukhoIcons {
    private fun outlined(name: String, data: String): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
        pathData = PathParser().parsePathString(data).toNodes(),
        fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.7f,
        strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
    ).build()
    val Inbox = outlined("Inbox", "M4 4h16v16H4z M4 13h5l1.5 3h3l1.5-3h5 M8 8h8")
    val Gear = outlined("Gear", "M9.5 3h5l.6 2.6 2 1.2 2.5-.8 2.5 4.3-1.9 1.8v2.3l1.9 1.8-2.5 4.3-2.5-.8-2 1.2-.6 2.6h-5l-.6-2.6-2-1.2-2.5.8L2 16.2l1.9-1.8v-2.3L2 10.3 4.5 6l2.5.8 2-1.2.5-2.6 M15.5 13.2a3.5 3.5 0 1 1-7 0 3.5 3.5 0 0 1 7 0")
    val Tune = outlined("Tune", "M4 7h5m4 0h7M4 17h9m4 0h3 M9 4v6 M17 14v6 M13 4v6 M13 14v6")
    val Back = outlined("Back", "M14 5l-7 7 7 7 M7 12h14")
    val Chevron = outlined("Chevron", "M9 5l7 7-7 7")
    val Check = outlined("Check", "M5 12l4 4L19 6")
    val CheckCircle = outlined("CheckCircle", "M21 12a9 9 0 1 1-5-8 M7 11l4 4 10-10")
    val Send = outlined("Send", "M3 3l19 9-19 9 3-9-3-9z M6 12h16")
    val Refresh = outlined("Refresh", "M20 7v5h-5 M19 12a7 7 0 1 0-1.7 4.5 M20 7l-2-2")
    val Chat = outlined("Chat", "M20 15a3 3 0 0 1-3 3H9l-5 3V6a3 3 0 0 1 3-3h10a3 3 0 0 1 3 3v9z M8 8h8 M8 12h5")
    val Comment = outlined("Comment", "M5 4h14a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H9l-6 3V6a2 2 0 0 1 2-2z M7 9h10 M7 13h6")
    val Instagram = outlined("Instagram", "M7 3h10a4 4 0 0 1 4 4v10a4 4 0 0 1-4 4H7a4 4 0 0 1-4-4V7a4 4 0 0 1 4-4z M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0 M17.5 6.5h.01")
    val TikTok = outlined("TikTok", "M14 3v12a5 5 0 1 1-5-5v3a2 2 0 1 0 2 2V3h3 M14 3c1 4 3 5 7 5v3c-3 0-5-1-7-3")
    val Info = outlined("Info", "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0 M12 11v6 M12 7h.01")
    val Offline = outlined("Offline", "M3 3l18 18 M8 6a15 15 0 0 1 13 3 M3 9a15 15 0 0 1 2-1 M6 13a10 10 0 0 1 3-2 M13 11a10 10 0 0 1 5 2 M9 17a5 5 0 0 1 5-1 M12 21h.01")
    val Link = outlined("Link", "M10 13a5 5 0 0 0 7 0l3-3a5 5 0 0 0-7-7l-2 2 M14 11a5 5 0 0 0-7 0l-3 3a5 5 0 0 0 7 7l2-2")
    val Bell = outlined("Bell", "M6 9a6 6 0 0 1 12 0v6l2 3H4l2-3V9 M10 21h4")
    val Sun = outlined("Sun", "M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0 M12 2v2 M12 20v2 M2 12h2 M20 12h2 M5 5l1.5 1.5 M17.5 17.5L19 19 M5 19l1.5-1.5 M17.5 6.5L19 5")
    val Shield = outlined("Shield", "M12 3l8 3v6c0 5-8 9-8 9s-8-4-8-9V6l8-3z M8 12l3 3 5-6")
    val Clock = outlined("Clock", "M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0 M12 7v5l3 2")
}


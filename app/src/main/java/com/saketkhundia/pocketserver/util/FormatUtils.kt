package com.saketkhundia.pocketserver.util

import java.util.Locale

object FormatUtils {
    fun formatBytes(bytes: Long): String {
        if (bytes < 0) return "—"
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes.toDouble() / 1024.0
        var unit = 0
        while (value >= 1024 && unit < units.size - 1) { value /= 1024; unit++ }
        return String.format(Locale.US, "%.1f %s", value, units[unit])
    }

    fun formatUptime(millis: Long): String {
        val s = millis / 1000
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, sec)
    }

    fun formatTime(ms: Long): String {
        val c = java.util.Calendar.getInstance()
        c.timeInMillis = ms
        return String.format(
            Locale.US, "%02d:%02d:%02d",
            c.get(java.util.Calendar.HOUR_OF_DAY),
            c.get(java.util.Calendar.MINUTE),
            c.get(java.util.Calendar.SECOND)
        )
    }

    /** Human relative time, e.g. "just now", "2 min ago". Pure formatting, no I/O. */
    fun relativeTime(ms: Long, now: Long = System.currentTimeMillis()): String {
        val diff = (now - ms).coerceAtLeast(0)
        val s = diff / 1000
        return when {
            s < 10 -> "just now"
            s < 60 -> "$s sec ago"
            s < 3600 -> {
                val m = s / 60
                if (m == 1L) "1 min ago" else "$m min ago"
            }
            s < 86400 -> {
                val h = s / 3600
                if (h == 1L) "1 hour ago" else "$h hours ago"
            }
            else -> {
                val d = s / 86400
                if (d == 1L) "yesterday" else "$d days ago"
            }
        }
    }
}

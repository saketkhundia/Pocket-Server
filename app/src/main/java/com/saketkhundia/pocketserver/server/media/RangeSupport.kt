package com.saketkhundia.pocketserver.server.media

data class Range(val start: Long, val endExclusive: Long)

object RangeSupport {
    /** Parse "bytes=0-1023" / "bytes=500-" / "bytes=-500". Returns null if malformed/unsatisfiable here. */
    fun parse(header: String?, total: Long): Range? {
        if (header == null || total <= 0) return null
        val h = header.trim()
        if (!h.startsWith("bytes=")) return null
        val spec = h.removePrefix("bytes=").split(",").firstOrNull()?.trim() ?: return null
        return try {
            if (spec.startsWith("-")) {
                val suffix = spec.removePrefix("-").toLong()
                if (suffix <= 0) return null
                val s = (total - suffix).coerceAtLeast(0)
                Range(s, total)
            } else {
                val parts = spec.split("-")
                val s = parts[0].toLong()
                val e = if (parts.size > 1 && parts[1].isNotEmpty()) parts[1].toLong() + 1 else total
                if (s < 0 || s >= total) return null
                Range(s, e.coerceAtMost(total).coerceAtLeast(s + 1))
            }
        } catch (_: Exception) { null }
    }
}

package com.saketkhundia.pocketserver.server.security

import java.net.URLDecoder

/**
 * Central path-security gate. All HTTP/FTP paths must pass through here.
 * Guarantees the resolved virtual path stays inside the shared roots.
 */
object PathSecurity {
    const val MAX_PATH_LEN = 4096
    const val MAX_SEGMENTS = 128

    sealed interface Check { data object Ok : Check; data class Rejected(val reason: String) : Check }

    fun sanitizeFileName(raw: String): Check {
        if (raw.isEmpty() || raw.length > 255) return Check.Rejected("Bad filename length")
        if (raw.contains('\u0000')) return Check.Rejected("Null byte")
        if (raw == "." || raw == "..") return Check.Rejected("Dot segment")
        if ('/' in raw || '\\' in raw) return Check.Rejected("Separator in filename")
        if (raw.endsWith('.') || raw.endsWith(' ')) return Check.Rejected("Trailing dot/space")
        // Reject control chars
        if (raw.any { it.code < 0x20 }) return Check.Rejected("Control char")
        return Check.Ok
    }

    /** Normalize a virtual path like "Downloads/a/../b". Returns null if unsafe. */
    fun normalizeVirtualPath(rawPath: String?): String? {
        if (rawPath == null) return ""
        if (rawPath.length > MAX_PATH_LEN) return null
        if ('\u0000' in rawPath) return null
        var decoded: String = rawPath
        // Double-decode to catch %252e tricks, bounded iterations
        repeat(3) {
            try {
                val next = URLDecoder.decode(decoded, "UTF-8")
                if (next == decoded) return@repeat
                decoded = next
            } catch (_: Exception) { return null }
        }
        if ('\u0000' in decoded) return null
        val unified = decoded.replace('\\', '/')
        if (unified.contains('\u0000')) return null
        val segments = unified.split('/')
        if (segments.size > MAX_SEGMENTS) return null
        val out = ArrayDeque<String>()
        for (seg in segments) {
            when {
                seg.isEmpty() || seg == "." -> Unit
                seg == ".." -> { if (out.isEmpty()) return null; out.removeLast() }
                seg.length > 255 -> return null
                seg.any { it.code < 0x20 } -> return null
                else -> out.add(seg)
            }
        }
        return out.joinToString("/")
    }

    fun splitRoot(virtualPath: String): Pair<String, String> {
        val idx = virtualPath.indexOf('/')
        return if (idx < 0) virtualPath to "" else virtualPath.substring(0, idx) to virtualPath.substring(idx + 1)
    }
}

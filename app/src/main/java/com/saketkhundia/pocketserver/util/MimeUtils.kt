package com.saketkhundia.pocketserver.util

import java.util.Locale

object MimeUtils {
    private val map = mapOf(
        "html" to "text/html; charset=utf-8",
        "htm" to "text/html; charset=utf-8",
        "css" to "text/css",
        "js" to "application/javascript",
        "mjs" to "application/javascript",
        "json" to "application/json",
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "webp" to "image/webp",
        "gif" to "image/gif",
        "svg" to "image/svg+xml",
        "ico" to "image/x-icon",
        "mp4" to "video/mp4",
        "webm" to "video/webm",
        "mkv" to "video/x-matroska",
        "mov" to "video/quicktime",
        "avi" to "video/x-msvideo",
        "mp3" to "audio/mpeg",
        "wav" to "audio/wav",
        "flac" to "audio/flac",
        "ogg" to "audio/ogg",
        "pdf" to "application/pdf",
        "txt" to "text/plain; charset=utf-8",
        "md" to "text/markdown; charset=utf-8",
        "zip" to "application/zip",
        "apk" to "application/vnd.android.package-archive",
        "ttf" to "font/ttf",
        "woff" to "font/woff",
        "woff2" to "font/woff2"
    )

    fun fromName(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.US)
        return map[ext] ?: "application/octet-stream"
    }

    fun isImage(name: String): Boolean {
        val e = name.substringAfterLast('.', "").lowercase(Locale.US)
        return e in setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "svg", "bmp")
    }

    fun isVideo(name: String): Boolean {
        val e = name.substringAfterLast('.', "").lowercase(Locale.US)
        return e in setOf("mp4", "mkv", "webm", "mov", "avi")
    }

    fun isAudio(name: String): Boolean {
        val e = name.substringAfterLast('.', "").lowercase(Locale.US)
        return e in setOf("mp3", "wav", "flac", "ogg", "m4a", "aac")
    }

    fun isMedia(name: String) = isImage(name) || isVideo(name) || isAudio(name)
}

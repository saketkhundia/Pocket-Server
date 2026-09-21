package com.saketkhundia.pocketserver.server.media

object MediaSupport {
    private val imageExt = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp", "svg")
    private val videoExt = setOf("mp4", "mkv", "webm", "mov", "avi")
    private val audioExt = setOf("mp3", "wav", "flac", "ogg", "m4a", "aac")

    fun kindOf(name: String): String {
        val e = name.substringAfterLast('.', "").lowercase()
        return when (e) {
            in imageExt -> "image"
            in videoExt -> "video"
            in audioExt -> "audio"
            else -> "other"
        }
    }
}

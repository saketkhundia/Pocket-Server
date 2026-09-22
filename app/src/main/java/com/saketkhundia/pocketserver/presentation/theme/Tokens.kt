package com.saketkhundia.pocketserver.presentation.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/** Consistent spacing scale. */
@Immutable
object PsSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp
}

/**
 * Consistent corner radii — spec table (exact).
 * Cards 24 · bottom tab bar 28 · sliding pill 20 · primary buttons 16 ·
 * icon tiles 32% of tile size.
 */
@Immutable
object PsRadius {
    val sm = 13.dp
    val md = 14.dp
    val lg = 20.dp
    val xl = 24.dp
    val hero = 24.dp
    val dock = 28.dp
    val pill = 20.dp
    val button = 16.dp
}

/** File kinds for the shared icon system (Android + Web use same mapping). */
enum class FileKind {
    Folder, Image, Video, Audio, Pdf, Archive, Code, Document, Unknown;

    companion object {
        fun of(name: String, isDir: Boolean): FileKind {
            if (isDir) return Folder
            val ext = name.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp", "svg" -> Image
                "mp4", "mkv", "webm", "mov", "avi" -> Video
                "mp3", "wav", "flac", "ogg", "m4a", "aac" -> Audio
                "pdf" -> Pdf
                "zip", "rar", "7z", "tar", "gz", "apk" -> Archive
                "html", "css", "js", "ts", "json", "xml", "kt", "java", "py", "sh", "md", "yml", "yaml" -> Code
                "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "csv", "rtf" -> Document
                else -> Unknown
            }
        }
    }
}

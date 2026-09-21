package com.saketkhundia.pocketserver.presentation.media

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.saketkhundia.pocketserver.PocketServerApp
import com.saketkhundia.pocketserver.domain.model.FileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A media file with a content URI the app can display/share. */
data class MediaItem(
    val entry: FileEntry,
    val uri: Uri,
    val mime: String?
)

data class MediaUiState(
    val photos: List<MediaItem> = emptyList(),
    val videos: List<MediaItem> = emptyList(),
    val audio: List<MediaItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "gif", "heic", "heif", "bmp")
private val VIDEO_EXT = setOf("mp4", "mkv", "webm", "mov", "avi")
private val AUDIO_EXT = setOf("mp3", "wav", "flac", "ogg", "m4a", "aac")

private const val MAX_DEPTH = 3
private const val MAX_ITEMS = 500

/**
 * Browses shared folders for viewable media. Read-only: listing + content
 * URIs only, reusing SharedFolderManager/StorageManager. No server changes.
 */
class MediaViewModel(private val app: PocketServerApp) : ViewModel() {

    private val _state = MutableStateFlow(MediaUiState())
    val state: StateFlow<MediaUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = MediaUiState(isLoading = true)
            try {
                // File traversal + SAF resolution are blocking I/O — never on Main.
                // listVirtual/resolveDoc already use Dispatchers.IO internally;
                // the walk + sort also runs on IO so 1k+ file trees stay smooth.
                val result = withContext(Dispatchers.IO) {
                    val folders = app.container.sharedFolderManager
                    val photos = mutableListOf<MediaItem>()
                    val videos = mutableListOf<MediaItem>()
                    val audio = mutableListOf<MediaItem>()

                    suspend fun walk(vpath: String, depth: Int) {
                        if (photos.size + videos.size + audio.size >= MAX_ITEMS || depth > MAX_DEPTH) return
                        val children = folders.listVirtual(vpath) ?: return
                        for (c in children) {
                            if (photos.size + videos.size + audio.size >= MAX_ITEMS) return
                            if (c.isDir) {
                                if (depth < MAX_DEPTH) walk(c.path, depth + 1)
                            } else {
                                val ext = c.name.substringAfterLast('.', "").lowercase()
                                val target = when (ext) {
                                    in IMAGE_EXT -> photos
                                    in VIDEO_EXT -> videos
                                    in AUDIO_EXT -> audio
                                    else -> null
                                }
                                if (target != null) {
                                    val resolved = folders.resolveDoc(c.path)
                                    val doc = resolved?.doc
                                    if (doc != null && !doc.isDirectory) {
                                        target.add(MediaItem(c, doc.uri, c.mime))
                                    }
                                }
                            }
                        }
                    }

                    val roots = folders.listVirtual("") ?: emptyList()
                    for (r in roots) walk(r.path, 0)
                    val cmp = compareByDescending<MediaItem> { it.entry.lastModified }
                    Triple(
                        photos.sortedWith(cmp),
                        videos.sortedWith(cmp),
                        audio.sortedWith(cmp)
                    )
                }
                _state.value = MediaUiState(
                    photos = result.first,
                    videos = result.second,
                    audio = result.third,
                    isLoading = false
                )
            } catch (e: Exception) {
                _state.value = MediaUiState(isLoading = false, error = e.message ?: "Couldn't load media")
            }
        }
    }

    companion object {
        fun factory(app: PocketServerApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MediaViewModel(app) as T
            }
        }
    }
}

package com.saketkhundia.pocketserver.presentation.media

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saketkhundia.pocketserver.PocketServerApp
import com.saketkhundia.pocketserver.presentation.components.AmoledBackground
import com.saketkhundia.pocketserver.presentation.components.EmptyState
import com.saketkhundia.pocketserver.presentation.components.ErrorState
import com.saketkhundia.pocketserver.presentation.components.Eyebrow
import com.saketkhundia.pocketserver.presentation.components.FileRow
import com.saketkhundia.pocketserver.presentation.components.LoadingRows
import com.saketkhundia.pocketserver.presentation.theme.PsSpacing
import com.saketkhundia.pocketserver.util.FormatUtils
import kotlinx.coroutines.launch

/**
 * In-app video/audio browser. Playback hands off to the device's own player
 * via ACTION_VIEW with a granted content URI — no new playback engine.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as PocketServerApp
    val vm: MediaViewModel = viewModel(factory = MediaViewModel.factory(app))
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun play(item: MediaItem) {
        try {
            val open = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(item.uri, item.mime ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(open)
        } catch (_: Exception) {
            scope.launch { snackbar.showSnackbar("No player found for this file") }
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Media", style = MaterialTheme.typography.headlineLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { vm.refresh() }) { Icon(Icons.Filled.Refresh, contentDescription = "Refresh") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            AmoledBackground(Modifier.fillMaxSize())
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                when {
                    state.isLoading -> item {
                        Spacer(Modifier.height(PsSpacing.lg))
                        LoadingRows(rows = 6)
                    }
                    state.error != null -> item {
                        ErrorState(message = state.error!!, onRetry = { vm.refresh() })
                    }
                    state.videos.isEmpty() && state.audio.isEmpty() -> item {
                        EmptyState(title = "No media", subtitle = "Shared video and audio will appear here.")
                    }
                    else -> {
                        if (state.videos.isNotEmpty()) {
                            item(key = "videos-header", contentType = "header") {
                                Eyebrow("Videos")
                                Spacer(Modifier.height(PsSpacing.sm))
                            }
                            items(state.videos, key = { it.entry.path }, contentType = { "media" }) { item ->
                                FileRow(
                                    name = item.entry.name,
                                    subtitle = "${FormatUtils.formatBytes(item.entry.size)} · ${item.entry.path.substringBefore('/')}",
                                    isDir = false,
                                    trailing = {
                                        IconButton(onClick = { play(item) }) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                                        }
                                    },
                                    onClick = { play(item) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                        if (state.audio.isNotEmpty()) {
                            item(key = "audio-header", contentType = "header") {
                                Spacer(Modifier.height(PsSpacing.lg))
                                Eyebrow("Audio")
                                Spacer(Modifier.height(PsSpacing.sm))
                            }
                            items(state.audio, key = { it.entry.path }, contentType = { "media" }) { item ->
                                FileRow(
                                    name = item.entry.name,
                                    subtitle = "${FormatUtils.formatBytes(item.entry.size)} · ${item.entry.path.substringBefore('/')}",
                                    isDir = false,
                                    trailing = {
                                        IconButton(onClick = { play(item) }) {
                                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                                        }
                                    },
                                    onClick = { play(item) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(110.dp)) }
            }
        }
    }
}

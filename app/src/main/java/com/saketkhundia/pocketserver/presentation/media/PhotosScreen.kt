package com.saketkhundia.pocketserver.presentation.media

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.saketkhundia.pocketserver.PocketServerApp
import com.saketkhundia.pocketserver.presentation.components.AmoledBackground
import com.saketkhundia.pocketserver.presentation.components.EmptyState
import com.saketkhundia.pocketserver.presentation.components.ErrorState
import com.saketkhundia.pocketserver.presentation.components.LoadingRows
import com.saketkhundia.pocketserver.presentation.theme.PsRadius
import com.saketkhundia.pocketserver.presentation.theme.PsSpacing
import kotlinx.coroutines.launch

/**
 * In-app photo grid over shared folders. Read-only browsing; tap for
 * full-screen viewer with share. Reuses the storage layer, no server impact.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as PocketServerApp
    val vm: MediaViewModel = viewModel(factory = MediaViewModel.factory(app))
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Photos", style = MaterialTheme.typography.headlineLarge)
                        if (!state.isLoading && state.photos.isNotEmpty()) {
                            Text(
                                "${state.photos.size} photos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
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
            when {
                state.isLoading -> Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(PsSpacing.lg))
                    LoadingRows(rows = 6)
                }
                state.error != null -> Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
                    ErrorState(message = state.error!!, onRetry = { vm.refresh() })
                }
                state.photos.isEmpty() -> Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
                    EmptyState(title = "No photos", subtitle = "Shared images will appear here.")
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Stable keys (virtual path) + animateItem: inserts/deletes glide,
                    // existing thumbs never reload. Thumbnails are 300px Coil requests
                    // with crossfade + memory/disk cache — full-res bitmaps are never
                    // decoded for the grid, only in the viewer.
                    itemsIndexed(
                        state.photos,
                        key = { _, item -> item.entry.path },
                        contentType = { _, _ -> "photo" }
                    ) { index, item ->
                        AsyncImage(
                            model = ImageRequest.Builder(ctx)
                                .data(item.uri)
                                .size(300)
                                .crossfade(180)
                                .memoryCacheKey("thumb-${item.entry.path}")
                                .build(),
                            contentDescription = item.entry.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .animateItem()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(PsRadius.md))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewerIndex = index }
                        )
                    }
                }
            }
        }
    }

    viewerIndex?.let { start ->
        Dialog(
            onDismissRequest = { viewerIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val pager = rememberPagerState(initialPage = start, pageCount = { state.photos.size })
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Black)) {
                HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                    val item = state.photos[page]
                    AsyncImage(
                        model = ImageRequest.Builder(ctx)
                            .data(item.uri)
                            .crossfade(200)
                            .build(),
                        contentDescription = item.entry.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                IconButton(
                    onClick = { viewerIndex = null },
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape)
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = androidx.compose.ui.graphics.Color.White) }
                IconButton(
                    onClick = {
                        val item = state.photos[pager.currentPage]
                        try {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = item.mime ?: "image/*"
                                putExtra(Intent.EXTRA_STREAM, item.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                clipData = ClipData.newRawUri("", item.uri)
                            }
                            ctx.startActivity(Intent.createChooser(send, "Share photo"))
                        } catch (_: Exception) {
                            scope.launchShareError(snackbar)
                        }
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape)
                ) { Icon(Icons.Filled.Share, null, tint = androidx.compose.ui.graphics.Color.White) }
                Text(
                    "${pager.currentPage + 1} / ${state.photos.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp)
                )
            }
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchShareError(snackbar: SnackbarHostState) {
    launch { snackbar.showSnackbar("Couldn't share this photo") }
}

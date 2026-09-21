package com.saketkhundia.pocketserver.presentation.files

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saketkhundia.pocketserver.presentation.components.AmoledBackground
import com.saketkhundia.pocketserver.presentation.components.EmptyState
import com.saketkhundia.pocketserver.presentation.components.FileRow
import com.saketkhundia.pocketserver.presentation.components.GlassCard
import com.saketkhundia.pocketserver.presentation.home.rememberSharedHomeViewModel
import com.saketkhundia.pocketserver.presentation.theme.LocalPsExtra
import com.saketkhundia.pocketserver.presentation.theme.PsRadius
import com.saketkhundia.pocketserver.presentation.theme.PsSpacing

/**
 * Shared folders manager — clean rows with icon, name, metadata, chevron.
 * Only folders explicitly selected via SAF are shown here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen() {
    val ctx = LocalContext.current
    // Activity-shared: zero per-visit init cost, one network observer total.
    val vm = rememberSharedHomeViewModel()
    // Narrow collectors: folders + errors only. The full uiState also carries
    // stats/logs, which emit per HTTP request during transfers — collecting it
    // here recomposed the whole screen (including the search field) per request.
    val folders by vm.foldersFlow.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var query by remember { mutableStateOf("") }
    // First-visit row stagger only (saveable across back-stack restore).
    var rowsSeen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { rowsSeen = true }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.addFolder(ctx, uri)
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbar.showSnackbar(it); vm.clearError() }
    }

    // derivedStateOf: filter result is cached and only recomputed when the
    // folder list or query actually changes — no redundant filtering per frame.
    // (Shared-folder count is tiny; huge directory browsing lives in the web
    // client and MediaViewModel, which filter on Dispatchers.Default/IO.)
    val visible by remember {
        derivedStateOf {
            val all = folders
            val q = query
            if (q.isBlank()) all
            else all.filter { it.name.contains(q, ignoreCase = true) }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Files", style = MaterialTheme.typography.headlineLarge)
                        if (visible.isNotEmpty()) {
                            Text(
                                "${visible.size} shared folder${if (visible.size == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { folderPicker.launch(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add folder")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            AmoledBackground(Modifier.fillMaxSize())
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(PsSpacing.md)
            ) {
                // Search smoothly expands in place; clear button fades rather
                // than popping. Results cross-fade without flashing the screen.
                item(key = "search", contentType = "search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search folders…") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            AnimatedVisibility(
                                visible = query.isNotEmpty(),
                                enter = fadeIn(tween(150, easing = FastOutSlowInEasing)),
                                exit = fadeOut(tween(120)),
                                label = "searchClear"
                            ) {
                                IconButton(onClick = { query = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(PsRadius.lg),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = LocalPsExtra.current.subtleBorder,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.fillMaxWidth().animateItem()
                    )
                }
                // Container cross-fades between empty / list — never flashes.
                item(key = "list-${visible.isEmpty()}", contentType = "list") {
                    AnimatedContent(
                        targetState = visible.isEmpty(),
                        transitionSpec = {
                            fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                                fadeOut(tween(150))
                        },
                        label = "folderList"
                    ) { empty ->
                        if (empty) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                EmptyState(
                                    title = if (query.isBlank()) "No shared folders" else "No matches",
                                    subtitle = if (query.isBlank())
                                        "Only folders you add here are visible on the network."
                                    else "Try a different search.",
                                    actionLabel = if (query.isBlank()) "Add folder" else null,
                                    onAction = if (query.isBlank()) ({ folderPicker.launch(null) }) else null
                                )
                            }
                        } else {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(horizontal = PsSpacing.md, vertical = 6.dp)) {
                                    // Small lists (a handful of shared folders): tiny
                                    // per-row stagger on FIRST visit only. Revisits via
                                    // back-stack restore show instantly — replaying the
                                    // stagger on every tab switch read as lag.
                                    visible.forEachIndexed { i, folder ->
                                        val rowDelay = (i.coerceAtMost(7)) * 25
                                        AnimatedVisibility(
                                            visible = true,
                                            enter = if (rowsSeen) EnterTransition.None else fadeIn(
                                                tween(180, delayMillis = rowDelay, easing = FastOutSlowInEasing)
                                            ) + slideInVertically(
                                                tween(180, delayMillis = rowDelay, easing = FastOutSlowInEasing)
                                            ) { it / 8 },
                                            label = "folderRow$i"
                                        ) {
                                            Column {
                                                FileRow(
                                                    name = folder.name,
                                                    subtitle = if (folder.readOnly) "Read only" else "Read · Write",
                                                    isDir = true,
                                                    trailing = {
                                                        TextButton(onClick = { vm.removeFolder(folder.id) }) {
                                                            Text("Remove", color = MaterialTheme.colorScheme.error)
                                                        }
                                                    }
                                                )
                                                if (i < visible.size - 1) {
                                                    Box(
                                                        Modifier.fillMaxWidth()
                                                            .padding(start = 40.dp + PsSpacing.md)
                                                            .height(1.dp)
                                                            .background(LocalPsExtra.current.subtleBorder)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "footer", contentType = "footer") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = PsSpacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Files stay on this phone and are served only while the server runs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(110.dp))
                }
            }
        }
    }
}

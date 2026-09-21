package com.saketkhundia.pocketserver.presentation.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saketkhundia.pocketserver.domain.model.ServerStatus
import com.saketkhundia.pocketserver.presentation.components.ActivityRow
import com.saketkhundia.pocketserver.presentation.components.AmoledBackground
import com.saketkhundia.pocketserver.presentation.components.EmptyState
import com.saketkhundia.pocketserver.presentation.components.GlassButton
import com.saketkhundia.pocketserver.presentation.components.GlassCard
import com.saketkhundia.pocketserver.presentation.components.GradientButton
import com.saketkhundia.pocketserver.presentation.components.MonoText
import com.saketkhundia.pocketserver.presentation.components.QuickAction
import com.saketkhundia.pocketserver.presentation.components.ServerGraphic
import com.saketkhundia.pocketserver.presentation.components.StatusPill
import com.saketkhundia.pocketserver.presentation.components.StorageBar
import com.saketkhundia.pocketserver.presentation.components.activityKindOf
import com.saketkhundia.pocketserver.presentation.theme.LocalPsExtra
import com.saketkhundia.pocketserver.presentation.theme.PsRadius
import com.saketkhundia.pocketserver.presentation.theme.PsSpacing
import com.saketkhundia.pocketserver.util.FormatUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenQr: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPhotos: () -> Unit,
    onOpenMedia: () -> Unit
) {
    val ctx = LocalContext.current
    // Activity-shared: zero per-visit init cost, one network observer total.
    val vm = rememberSharedHomeViewModel()
    // Isolated collectors — each slice recomposes only its section. Stats ticks,
    // upload progress and log lines never rebuild the whole screen.
    // Lifecycle-aware: collectors stop when the tab is in background, so rapid
    // tab switches never stack duplicate subscriptions. Slices stay isolated.
    val serverState by vm.serverState.collectAsStateWithLifecycle()
    val folders by vm.foldersFlow.collectAsStateWithLifecycle()
    val settings by vm.settingsFlow.collectAsStateWithLifecycle()
    val recentLogs by vm.recentLogs.collectAsStateWithLifecycle()
    val ipAddress by vm.localIpFlow.collectAsStateWithLifecycle()
    val storage by vm.storageStats.collectAsStateWithLifecycle()
    val troubleshoot by vm.troubleshootHint.collectAsStateWithLifecycle()
    val candidates by vm.allCandidates.collectAsStateWithLifecycle()
    val errorMessage by vm.errorMessage.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbar.showSnackbar(it); vm.clearError() }
    }

    // Coordinated entrance: single flag, tiny stagger (≤40ms) across sections.
    // Fades + 12dp rise, total under ~380ms — one gesture, not a presentation.
    // Saveable (not plain remember): tab switches restore the entry, so a
    // revisit must NOT replay the entrance — that replay churn read as lag
    // when tapping Home → Files → Home quickly.
    var entered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val status = serverState.status
    val running = status == ServerStatus.RUNNING
    val starting = status == ServerStatus.STARTING
    val url = serverState.url

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            AmoledBackground(Modifier.fillMaxSize())
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Header: identity first, 28-30sp tight, 44dp glass buttons ──
                item(key = "header", contentType = "header") {
                    Entrance(visible = entered, delay = 0) {
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Pocket Server",
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Your phone. Your server.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HeaderIconButton(
                                onClick = onOpenActivity,
                                dot = status == ServerStatus.RUNNING
                            ) {
                                Icon(Icons.Outlined.Notifications, contentDescription = "Activity")
                            }
                            Spacer(Modifier.width(10.dp))
                            HeaderIconButton(onClick = onOpenSettings) {
                                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // ── Hero ──
                item(key = "hero", contentType = "hero") {
                    Entrance(visible = entered, delay = 40) {
                        HeroCard(
                            status = status,
                            url = url,
                            error = serverState.error,
                            starting = starting,
                            running = running,
                            onStart = { vm.startServer(ctx) },
                            onStop = { vm.stopServer(ctx) },
                            onRetry = { vm.startServer(ctx); vm.clearError() },
                            onOpenServer = {
                                val u = url ?: return@HeroCard
                                try {
                                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                                } catch (_: Exception) {}
                            },
                            onShare = { vm.shareUrl(ctx) },
                            onQr = onOpenQr,
                            onCopy = {
                                val u = url ?: return@HeroCard
                                clipboard.setText(AnnotatedString(u))
                                scope.launch { snackbar.showSnackbar("Link copied") }
                            },
                            onOpenFiles = onOpenFiles,
                            onOpenPhotos = onOpenPhotos,
                            onOpenMedia = onOpenMedia,
                            onOpenSettings = onOpenSettings
                        )
                    }
                }

                // ── Folders + Storage ──
                item(key = "cards", contentType = "cards") {
                    Entrance(visible = entered, delay = 80) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(PsSpacing.md)
                        ) {
                            FoldersCard(
                                count = folders.size,
                                names = folders.take(2).joinToString { it.name },
                                onClick = onOpenFiles,
                                modifier = Modifier.weight(1f).fillMaxSize()
                            )
                            StorageCard(
                                free = storage?.freeBytes,
                                used = storage?.usedBytes,
                                total = storage?.totalBytes,
                                fraction = storage?.usedFraction ?: 0f,
                                modifier = Modifier.weight(1f).fillMaxSize()
                            )
                        }
                    }
                }

                // ── Recent activity: breathing room, 16-18sp header, 62dp rows ──
                item(key = "activity", contentType = "activity") {
                    Entrance(visible = entered, delay = 120) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Recent Activity", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            Text(
                                "View all  →",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(onClick = onOpenActivity).padding(8.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    if (recentLogs.isEmpty()) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                title = "No activity yet",
                                subtitle = "Connect another device to see it here."
                            )
                        }
                    } else {
                        // Standalone white rows (no nested card-in-card):
                        // each row owns its surface, borders never stack.
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            recentLogs.forEach { log ->
                                ActivityRow(
                                    title = activityTitle(log.method, log.path),
                                    time = FormatUtils.relativeTime(log.timestampMs),
                                    kind = activityKindOf(log.method, log.status),
                                    showDivider = false,
                                    onClick = onOpenActivity
                                )
                            }
                        }
                    }
                    }
                }

                // ── Connection help ──
                item(key = "help", contentType = "help") {
                    Entrance(visible = entered, delay = 160) {
                        var expanded by remember { mutableStateOf(false) }
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { expanded = !expanded }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(PsSpacing.lg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Wifi, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(PsSpacing.md))
                            Column(Modifier.weight(1f)) {
                                Text("Having trouble connecting?", style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (expanded) troubleshoot
                                    else "Make sure your device is on the same Wi-Fi network.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                AnimatedVisibility(
                                    visible = expanded,
                                    enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                                        expandVertically(tween(200, easing = FastOutSlowInEasing)),
                                    exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
                                    label = "helpExpand"
                                ) {
                                    Column {
                                        Spacer(Modifier.height(8.dp))
                                        if (candidates.isNotEmpty()) {
                                            MonoText(candidates.joinToString(), small = true, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Text(
                                                "Refresh",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.clickable { vm.refreshNetwork() }
                                            )
                                            if (ipAddress != null) {
                                                Text(
                                                    "Test in browser",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.clickable {
                                                        try {
                                                            ctx.startActivity(
                                                                Intent(
                                                                    Intent.ACTION_VIEW,
                                                                    Uri.parse("http://${ipAddress}:${settings.httpPort}/ping")
                                                                )
                                                            )
                                                        } catch (_: Exception) {}
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Icon(
                                Icons.Filled.ChevronRight, null,
                                tint = LocalPsExtra.current.tertiaryText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    }
                }

                item(key = "spacer", contentType = "spacer") { Spacer(Modifier.height(124.dp)) }
            }
        }
    }
}

/**
 * Coordinated entrance wrapper: fade + slight rise with a tiny stagger delay.
 * Sections share one gesture — header(0) → hero(40) → cards(80) →
 * activity(120) → help(160). Total stays under ~400ms.
 *
 * Content is wrapped in a Column: AnimatedVisibility lays its children out
 * stacked (Box-like), so multiple items would otherwise draw on top of each
 * other — that overlap bug is why this wrapper exists.
 */
@Composable
private fun Entrance(
    visible: Boolean,
    delay: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220, delayMillis = delay, easing = FastOutSlowInEasing)) +
            slideInVertically(tween(220, delayMillis = delay, easing = FastOutSlowInEasing)) { it / 14 },
        label = "entrance$delay"
    ) {
        Column { content() }
    }
}

@Composable
private fun HeaderIconButton(
    onClick: () -> Unit,
    dot: Boolean = false,
    icon: @Composable () -> Unit
) {
    val extra = LocalPsExtra.current
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(extra.glass)
            .border(1.dp, extra.subtleBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            icon()
            if (dot) {
                Box(
                    Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    status: ServerStatus,
    url: String?,
    error: String?,
    starting: Boolean,
    running: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onOpenServer: () -> Unit,
    onShare: () -> Unit,
    onQr: () -> Unit,
    onCopy: () -> Unit,
    onOpenFiles: () -> Unit,
    onOpenPhotos: () -> Unit,
    onOpenMedia: () -> Unit,
    onOpenSettings: () -> Unit
) {
    // Subtle "alive" response when running: illustration breathes 1.0 → 1.03
    // on a GPU layer. No particles, no bounce — professional, battery-safe.
    val graphicScale by animateFloatAsState(
        targetValue = if (running) 1.03f else 1f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "heroGlow"
    )
    GlassCard(modifier = Modifier.fillMaxWidth(), radius = PsRadius.hero, frosted = true) {
        Column(Modifier.padding(PsSpacing.xl)) {
            StatusPill(status)
            Spacer(Modifier.height(PsSpacing.lg))
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = status,
                        transitionSpec = {
                            (fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                                slideInVertically(tween(180, easing = FastOutSlowInEasing)) { it / 6 }) togetherWith
                                (fadeOut(tween(150)) + androidx.compose.animation.slideOutVertically(tween(150)) { -it / 8 })
                        },
                        label = "heroState"
                    ) { s ->
                        Column {
                            Text(
                                when (s) {
                                    ServerStatus.RUNNING -> "Your server is ready"
                                    ServerStatus.STARTING -> "Starting server…"
                                    ServerStatus.ERROR -> "Something went wrong"
                                    ServerStatus.STOPPED -> "Start your server"
                                },
                                style = MaterialTheme.typography.headlineLarge
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                when (s) {
                                    ServerStatus.RUNNING -> "Other devices on this Wi-Fi can connect now."
                                    ServerStatus.STARTING -> "Finding network and binding port…"
                                    ServerStatus.ERROR -> error ?: "The server could not start."
                                    ServerStatus.STOPPED -> "Your phone isn't sharing anything yet. Tap below to get started."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.width(PsSpacing.md))
                ServerGraphic(
                    running = running,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(width = 80.dp, height = 104.dp)
                        .graphicsLayer {
                            scaleX = graphicScale
                            scaleY = graphicScale
                            // Spec presence: subtle but visible in both themes.
                            alpha = 0.8f
                        }
                )
            }
            // URL row expands/collapses instead of popping in place.
            AnimatedVisibility(
                visible = running && url != null,
                enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                    expandVertically(tween(200, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
                label = "urlRow"
            ) {
                Column {
                    Spacer(Modifier.height(PsSpacing.lg))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MonoText(url ?: "", color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                        androidx.compose.material3.IconButton(onClick = onQr, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.QrCode2, "Show QR code", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                        androidx.compose.material3.IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.ContentCopy, "Copy link", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(PsSpacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassButton(label = "Open Server", onClick = onOpenServer, modifier = Modifier.weight(1f))
                        GlassButton(label = "Share", onClick = onShare, icon = Icons.Filled.Share, modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(PsSpacing.lg))
            // One morphing CTA, always full width: Start ↔ Starting ↔ Stop ↔ Retry.
            // AnimatedContent keeps the same slot — the button transforms, never pops.
            AnimatedContent(
                targetState = when {
                    starting -> 0
                    running -> 1
                    status == ServerStatus.ERROR -> 2
                    else -> 3
                },
                transitionSpec = {
                    (fadeIn(tween(180, easing = FastOutSlowInEasing)) +
                        scaleIn(tween(180, easing = FastOutSlowInEasing), initialScale = 0.98f)) togetherWith
                        fadeOut(tween(150))
                },
                label = "ctaMorph"
            ) { slot ->
                when (slot) {
                    0 -> GradientButton(
                        label = "Starting…", onClick = {}, enabled = false, loading = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    1 -> GradientButton(
                        label = "Stop Server",
                        onClick = onStop,
                        icon = Icons.Outlined.Stop,
                        modifier = Modifier.fillMaxWidth()
                    )
                    2 -> GradientButton(
                        label = "Try again", onClick = onRetry,
                        modifier = Modifier.fillMaxWidth()
                    )
                    else -> GradientButton(
                        label = "Start Server", onClick = onStart, icon = Icons.Outlined.PlayArrow,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(PsSpacing.lg))
            Box(
                Modifier.fillMaxWidth().height(1.dp)
                    .background(LocalPsExtra.current.subtleBorder)
            )
            Spacer(Modifier.height(PsSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickAction(
                    icon = Icons.Outlined.Folder, title = "Share Files", subtitle = "Anywhere",
                    onClick = onOpenFiles, modifier = Modifier.weight(1f)
                )
                QuickDivider()
                QuickAction(
                    icon = Icons.Outlined.Image, title = "Photos", subtitle = "Backup",
                    onClick = onOpenPhotos, modifier = Modifier.weight(1f)
                )
                QuickDivider()
                QuickAction(
                    icon = Icons.Outlined.PlayArrow, title = "Media", subtitle = "Instant",
                    onClick = onOpenMedia, modifier = Modifier.weight(1f)
                )
                QuickDivider()
                QuickAction(
                    icon = Icons.Outlined.Language, title = "Web Server", subtitle = "Website",
                    onClick = onOpenSettings, modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(64.dp)
            .background(LocalPsExtra.current.subtleBorder)
    )
}

@Composable
private fun FoldersCard(count: Int, names: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, onClick = onClick) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Shared Folders", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = LocalPsExtra.current.tertiaryText, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(PsSpacing.md))
            Text(
                "$count",
                // Spec hierarchy: section number 22sp semibold.
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (count == 0) "No folders yet" else "$count folder${if (count == 1) "" else "s"} · $names",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StorageCard(
    free: Long?,
    used: Long?,
    total: Long?,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.PieChart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Storage", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(PsSpacing.md))
            Text(
                if (free != null) "${FormatUtils.formatBytes(free)} free" else "—",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(PsSpacing.md))
            StorageBar(progress = fraction)
            Spacer(Modifier.height(PsSpacing.sm))
            if (used != null && total != null) {
                Text(
                    "${FormatUtils.formatBytes(used)} used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    "${FormatUtils.formatBytes(total)} total",
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalPsExtra.current.tertiaryText,
                    maxLines = 1
                )
            } else {
                Text(
                    "Device storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun activityTitle(method: String, path: String): String {
    val name = path.substringAfterLast('/').takeIf { it.isNotBlank() } ?: path.take(32)
    return when {
        method == "START" -> "Server started"
        method == "STOP" -> "Server stopped"
        method == "NETWORK" -> "Network changed"
        method.startsWith("FTP") -> "FTP $name"
        method == "POST" -> "Upload · $name"
        method == "DELETE" -> "Deleted · $name"
        method == "PUT" -> "Renamed · $name"
        else -> "Served · $name"
    }
}

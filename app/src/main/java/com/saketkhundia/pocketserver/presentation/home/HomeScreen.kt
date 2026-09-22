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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassAddressBar
import com.saketkhundia.pocketserver.presentation.glass.LiquidIconTile
import com.saketkhundia.pocketserver.presentation.motion.floatAmbient
import com.saketkhundia.pocketserver.presentation.theme.PsIcons
import com.saketkhundia.pocketserver.domain.model.MdnsStatus
import com.saketkhundia.pocketserver.domain.model.ServerStatus
import com.saketkhundia.pocketserver.server.mdns.loopbackUrl
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
    // Addressing single source of truth: friendly hostname primary, IP fallback.
    val address by vm.address.collectAsStateWithLifecycle()
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
    // Display: verified name → expected (checking) name → IP, instantly.
    // Actions (open/copy/share/QR) keep using the verified-or-IP primary.
    val url = address.primaryUrl
    val displayUrl = address.displayUrl
    val hostnameUrl = address.hostnameUrl
    val ipFallback = address.ipUrl?.takeIf { it != displayUrl }
    val mdnsChecking = address.mdnsChecking

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                        // Tight to the status bar: the Scaffold already applies
                        // the status-bar inset, so only a small gap is needed.
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Pocket Server",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = (-0.3).sp
                                    ),
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Your phone. Your server.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                                    color = Color(0xFF858585)
                                )
                            }
                            HeaderIconButton(
                                onClick = onOpenActivity,
                                dot = status == ServerStatus.RUNNING
                            ) {
                                Icon(PsIcons.Bell, contentDescription = "Activity", tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            HeaderIconButton(onClick = onOpenSettings) {
                                Icon(PsIcons.Settings, contentDescription = "Settings", tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                // ── Hero ──
                item(key = "hero", contentType = "hero") {
                    Entrance(visible = entered, delay = 40) {
                        HeroCard(
                            status = status,
                            url = displayUrl,
                            hostnameUrl = hostnameUrl,
                            ipFallback = ipFallback,
                            mdnsChecking = mdnsChecking,
                            mdnsUnavailable = address.mdnsStatus == MdnsStatus.UNAVAILABLE,
                            error = serverState.error,
                            starting = starting,
                            running = running,
                            onStart = { vm.startServer(ctx) },
                            onStop = { vm.stopServer(ctx) },
                            onRetry = { vm.startServer(ctx); vm.clearError() },
                            onOpenServer = {
                                // On-device preview: Android cannot resolve .local
                                // names (NXDOMAIN), so always use loopback here.
                                // The hostname URL is for OTHER devices.
                                val u = loopbackUrl(serverState.port)
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

                // ── Recent activity: one continuous glass list ──
                item(key = "activity", contentType = "activity") {
                    Entrance(visible = entered, delay = 120) {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "RECENT ACTIVITY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 1.6.sp
                                ),
                                color = Color.White.copy(alpha = 0.30f),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "View all  →",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White.copy(alpha = 0.60f),
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
                        // One continuous glass container, subtle internal
                        // dividers — not a card per row.
                        GlassCard(modifier = Modifier.fillMaxWidth(), radius = 20.dp) {
                            Column(Modifier.padding(vertical = 6.dp)) {
                                recentLogs.forEachIndexed { i, log ->
                                    ActivityRow(
                                        title = activityTitle(log.method, log.path),
                                        time = FormatUtils.relativeTime(log.timestampMs),
                                        kind = activityKindOf(log.method, log.status),
                                        showDivider = false,
                                        flat = true,
                                        onClick = onOpenActivity
                                    )
                                    if (i < recentLogs.size - 1) {
                                        Box(
                                            Modifier.fillMaxWidth()
                                                .padding(start = 62.dp, end = 14.dp)
                                                .height(1.dp)
                                                .background(Color.White.copy(alpha = 0.06f))
                                        )
                                    }
                                }
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
                            LiquidIconTile(icon = PsIcons.Wifi, tileSize = 44.dp, glyphSize = 20.dp)
                            Spacer(Modifier.width(PsSpacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Having trouble connecting?",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (expanded) troubleshoot
                                    else "Make sure your device is on the same Wi-Fi network.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.40f)
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
                                            MonoText(candidates.joinToString(), small = true, color = Color.White.copy(alpha = 0.40f))
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            Text(
                                                "Refresh",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = Color.White.copy(alpha = 0.95f),
                                                modifier = Modifier.clickable { vm.refreshNetwork() }
                                            )
                                            if (ipAddress != null) {
                                                Text(
                                                    "Test in browser",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = Color.White.copy(alpha = 0.95f),
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
                                PsIcons.ChevronRight, null,
                                tint = Color.White.copy(alpha = 0.30f),
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
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
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
                        .background(com.saketkhundia.pocketserver.presentation.theme.LiveDot)
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    status: ServerStatus,
    url: String?,
    hostnameUrl: String?,
    ipFallback: String?,
    mdnsChecking: Boolean,
    mdnsUnavailable: Boolean,
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
    GlassCard(modifier = Modifier.fillMaxWidth(), radius = PsRadius.hero, frosted = true) {
        Column(Modifier.padding(PsSpacing.xl)) {
            // Signature: status pill top-left, 52px liquid server icon top-right.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(status, modifier = Modifier.weight(1f).wrapContentWidth(Alignment.Start))
                LiquidIconTile(
                    icon = PsIcons.Server,
                    tileSize = 52.dp,
                    glyphSize = 24.dp,
                    modifier = Modifier.floatAmbient()
                )
            }
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
                                // Hero title 22-24sp semibold — the focal point.
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White.copy(alpha = 0.95f)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                when (s) {
                                    ServerStatus.RUNNING -> "Other devices on this Wi-Fi can connect now."
                                    ServerStatus.STARTING -> "Finding network and binding port…"
                                    ServerStatus.ERROR -> error ?: "The server could not start."
                                    ServerStatus.STOPPED -> "Your phone isn't sharing anything yet. Tap below to get started."
                                },
                                // Description 13sp, muted — never too bright.
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp
                                ),
                                color = Color(0xFF8A8A8A)
                            )
                        }
                    }
                }
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
                    // Dark inset address bar (0.28 fill, 0.09 border, r16) + COPY.
                    LiquidGlassAddressBar(
                        text = (url ?: "").removePrefix("http://"),
                        onCopy = onCopy
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.IconButton(onClick = onQr, modifier = Modifier.size(36.dp)) {
                            Icon(PsIcons.Qr, "Show QR code", tint = Color.White.copy(alpha = 0.60f), modifier = Modifier.size(20.dp))
                        }
                        MonoText(
                            "Show QR",
                            small = true,
                            color = Color.White.copy(alpha = 0.60f),
                            modifier = Modifier.clickable(onClick = onQr).padding(8.dp)
                        )
                    }
                    // Friendly name is primary; the working IP stays visible as
                    // fallback. UNAVAILABLE is stated honestly — never
                    // advertised as working. While checking, the expected
                    // name shows instantly with a marker; actions still use
                    // the verified-or-IP primary.
                    if (ipFallback != null && (hostnameUrl != null || mdnsChecking)) {
                        Spacer(Modifier.height(2.dp))
                        MonoText(
                            "Fallback: ${ipFallback.removePrefix("http://")}",
                            small = true,
                            color = Color.White.copy(alpha = 0.40f)
                        )
                    }
                    if (mdnsChecking) {
                        Text(
                            "Checking local name…",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.40f)
                        )
                    } else if (mdnsUnavailable) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Local name unavailable on this network.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.40f)
                        )
                    }
                    Spacer(Modifier.height(PsSpacing.md))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassButton(label = "Open Server", onClick = onOpenServer, modifier = Modifier.weight(1f))
                        GlassButton(label = "Share", onClick = onShare, icon = PsIcons.Share, modifier = Modifier.weight(1f))
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
                        icon = PsIcons.Stop,
                        modifier = Modifier.fillMaxWidth()
                    )
                    2 -> GradientButton(
                        label = "Try again", onClick = onRetry,
                        modifier = Modifier.fillMaxWidth()
                    )
                    else -> GradientButton(
                        label = "Start Server", onClick = onStart, icon = PsIcons.Play,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(PsSpacing.lg))
            Box(
                Modifier.fillMaxWidth().height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )
            Spacer(Modifier.height(PsSpacing.sm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickAction(
                    icon = PsIcons.Folder, title = "Share Files", subtitle = "Anywhere",
                    onClick = onOpenFiles, modifier = Modifier.weight(1f)
                )
                QuickDivider()
                QuickAction(
                    icon = PsIcons.Photos, title = "Photos", subtitle = "Backup",
                    onClick = onOpenPhotos, modifier = Modifier.weight(1f)
                )
                QuickDivider()
                QuickAction(
                    icon = PsIcons.Media, title = "Media", subtitle = "Instant",
                    onClick = onOpenMedia, modifier = Modifier.weight(1f)
                )
                QuickDivider()
                QuickAction(
                    icon = PsIcons.Globe, title = "Web Server", subtitle = "Website",
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
            .background(Color.White.copy(alpha = 0.08f))
    )
}

@Composable
private fun FoldersCard(count: Int, names: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, radius = 20.dp, onClick = onClick) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PsIcons.Folder, null, tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Shared Folders",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                    color = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.weight(1f)
                )
                Icon(PsIcons.ChevronRight, null, tint = Color.White.copy(alpha = 0.30f), modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "$count",
                // Card numeral: bold tabular-nums.
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                    color = Color.White.copy(alpha = 0.95f)
                )
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (count == 0) "No folders yet" else "$count folder${if (count == 1) "" else "s"} · $names",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color.White.copy(alpha = 0.40f),
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
    GlassCard(modifier = modifier, radius = 20.dp) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PsIcons.Storage, null, tint = Color.White.copy(alpha = 0.95f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Storage",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                    color = Color.White.copy(alpha = 0.95f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (free != null) "${FormatUtils.formatBytes(free)} free" else "—",
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = Color.White.copy(alpha = 0.95f)
            )
            Spacer(Modifier.height(10.dp))
            StorageBar(progress = fraction)
            Spacer(Modifier.height(8.dp))
            if (used != null && total != null) {
                Text(
                    "${FormatUtils.formatBytes(used)} used",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.40f),
                    maxLines = 1
                )
                Text(
                    "${FormatUtils.formatBytes(total)} total",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.30f),
                    maxLines = 1
                )
            } else {
                Text(
                    "Device storage",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.40f)
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

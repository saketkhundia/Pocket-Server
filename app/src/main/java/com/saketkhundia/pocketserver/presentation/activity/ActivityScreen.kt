package com.saketkhundia.pocketserver.presentation.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saketkhundia.pocketserver.PocketServerApp
import com.saketkhundia.pocketserver.domain.model.LogEntry
import com.saketkhundia.pocketserver.presentation.components.ActivityRow
import com.saketkhundia.pocketserver.presentation.components.AmoledBackground
import com.saketkhundia.pocketserver.presentation.components.EmptyState
import com.saketkhundia.pocketserver.presentation.components.Eyebrow
import com.saketkhundia.pocketserver.presentation.components.GlassCard
import com.saketkhundia.pocketserver.presentation.components.activityKindOf
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassChip
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassSearchBar
import com.saketkhundia.pocketserver.presentation.logs.LogsViewModel
import com.saketkhundia.pocketserver.presentation.theme.PsSpacing
import com.saketkhundia.pocketserver.util.FormatUtils
import java.util.Calendar

/**
 * Activity timeline grouped by day: Today / Yesterday / Older.
 * Full request log lives under Settings → Server logs.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ActivityScreen(onOpenLogs: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as PocketServerApp
    val vm: LogsViewModel = viewModel(factory = LogsViewModel.factory(app))
    val logs by vm.logs.collectAsStateWithLifecycle()

    // Filter + search — preserved across tab switches via saveable + back-stack
    // restore. Filtering is a cheap in-memory pass over capped rows.
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(0) }

    // Cap rendered rows: full history stays in Server logs. Rendering hundreds
    // of rows on first composition is what made tab switches feel laggy.
    val recent = remember(logs) { logs.take(MAX_ROWS) }
    val filtered = remember(recent, query, filter) {
        val q = query.trim()
        recent.filter { log ->
            val passFilter = when (filter) {
                1 -> log.method == "START" || log.method == "STOP" || log.method == "NETWORK" || log.method.startsWith("FTP_") && (log.method.endsWith("START") || log.method.endsWith("STOP"))
                2 -> log.method == "GET" || log.method == "HEAD" || log.method == "POST" || log.method == "DELETE" || log.method == "PUT" || log.method.startsWith("FTP")
                3 -> log.method == "POST" || (log.method.startsWith("FTP") && log.method.contains("STOR"))
                4 -> log.method == "GET" || log.method == "HEAD"
                5 -> log.path.contains(".jpg", true) || log.path.contains(".png", true) || log.path.contains(".jpeg", true) || log.path.contains(".webp", true) || log.path.contains(".heic", true) || log.method == "PHOTO"
                else -> true
            }
            val passQuery = q.isBlank() || log.path.contains(q, true) || log.clientIp.contains(q, true) || log.method.contains(q, true)
            passFilter && passQuery
        }
    }
    val groups = rememberGroups(filtered)
    val hasMore = logs.size > recent.size

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Activity", style = MaterialTheme.typography.headlineLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {
            AmoledBackground(Modifier.fillMaxSize())
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                // Standalone rows with even 8dp rhythm (headers included).
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "search", contentType = "search") {
                    LiquidGlassSearchBar(
                        value = query, onValue = { query = it },
                        placeholder = "Search activity…",
                        leading = Icons.Outlined.Search,
                        onClear = { query = "" }
                    )
                    Spacer(Modifier.height(4.dp))
                }
                item(key = "filters", contentType = "filters") {
                    val filters = listOf("All", "Server", "Files", "Uploads", "Downloads", "Photos")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(filters.size, key = { filters[it] }) { i ->
                            LiquidGlassChip(label = filters[i], selected = filter == i, onClick = { filter = i })
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
                if (filtered.isEmpty()) {
                    item(key = "empty", contentType = "empty") {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            EmptyState(
                                title = if (logs.isEmpty()) "No activity yet" else "No matches",
                                subtitle = if (logs.isEmpty()) "Start your server and connect another device."
                                else "Try a different search or filter."
                            )
                        }
                    }
                } else {
                    groups.forEach { (label, entries) ->
                        stickyHeader(key = "header-$label", contentType = "header") {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = PsSpacing.sm)
                            ) {
                                Eyebrow(label)
                            }
                        }
                        items(
                            entries,
                            key = { log -> rowKey(log) },
                            contentType = { "row" }
                        ) { log ->
                            val (title, _) = describe(log)
                            // animateItem: new rows fade/slide in while existing rows
                            // stay stable — the list never re-animates wholesale.
                            ActivityRow(
                                title = title,
                                time = FormatUtils.formatTime(log.timestampMs),
                                kind = activityKindOf(log.method, log.status),
                                showDivider = true,
                                onClick = onOpenLogs,
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                    if (hasMore) {
                        item(key = "more", contentType = "more") {
                            Text(
                                "Older entries live in Server logs  →",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onOpenLogs)
                                    .padding(vertical = PsSpacing.lg),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                item(key = "spacer", contentType = "spacer") { Spacer(Modifier.height(110.dp)) }
            }
        }
    }
}

private const val MAX_ROWS = 100

/** Stable key: DB id when present, else content hash (mem + persisted copies dedupe upstream). */
private fun rowKey(log: LogEntry): Any =
    if (log.id != 0L) log.id
    else "${log.timestampMs}-${log.method}-${log.path}-${log.status}-${log.clientIp}".hashCode()

@Composable
private fun rememberGroups(logs: List<LogEntry>): List<Pair<String, List<LogEntry>>> {
    return androidx.compose.runtime.remember(logs) {
        // Day boundaries computed ONCE per list emission — not per row.
        // The old dayBucket() built 2 Calendars per row (200+ allocations per
        // burst emission); now it's 1 Calendar total, then cheap comparisons.
        val dayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStartMs = dayStart.timeInMillis
        dayStart.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStartMs = dayStart.timeInMillis
        val today = mutableListOf<LogEntry>()
        val yesterday = mutableListOf<LogEntry>()
        val older = mutableListOf<LogEntry>()
        for (log in logs) {
            when {
                log.timestampMs >= todayStartMs -> today.add(log)
                log.timestampMs >= yesterdayStartMs -> yesterday.add(log)
                else -> older.add(log)
            }
        }
        buildList {
            if (today.isNotEmpty()) add("Today" to today)
            if (yesterday.isNotEmpty()) add("Yesterday" to yesterday)
            if (older.isNotEmpty()) add("Older" to older)
        }
    }
}

/** Human-friendly description without exposing secrets. */
private fun describe(log: LogEntry): Pair<String, String> {
    val path = log.path.take(48)
    return when {
        log.method == "START" -> "Server started" to path
        log.method == "STOP" -> "Server stopped" to "Sessions cleared"
        log.method == "NETWORK" -> "Network changed" to path
        log.method == "FTP_START" || log.method == "FTP_STOP" -> "FTP ${if (log.method.endsWith("START")) "started" else "stopped"}" to path
        log.method.startsWith("FTP") -> "FTP ${log.method.removePrefix("FTP_")}" to "${log.clientIp} · ${FormatUtils.formatTime(log.timestampMs)}"
        log.method == "POST" && path.contains("upload") -> "Files uploaded" to "${log.clientIp} · ${FormatUtils.formatTime(log.timestampMs)}"
        log.method == "POST" -> "Request" to "${log.clientIp} · $path"
        log.status == 401 || log.status == 429 -> "Blocked request" to "${log.clientIp} · ${log.status}"
        log.method == "GET" || log.method == "HEAD" -> "File served" to "$path · ${log.clientIp}"
        else -> "${log.method} $path" to "${log.clientIp} · ${log.status}"
    }
}

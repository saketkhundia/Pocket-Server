package com.saketkhundia.pocketserver.presentation.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saketkhundia.pocketserver.PocketServerApp
import com.saketkhundia.pocketserver.presentation.components.ActivityIcon
import com.saketkhundia.pocketserver.presentation.components.EmptyState
import com.saketkhundia.pocketserver.presentation.components.MonoText
import com.saketkhundia.pocketserver.presentation.components.activityKindOf
import com.saketkhundia.pocketserver.presentation.theme.PsSpacing
import com.saketkhundia.pocketserver.util.FormatUtils

/**
 * Full server log — technical rows with monospace paths and status tints.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as PocketServerApp
    val vm: LogsViewModel = viewModel(factory = LogsViewModel.factory(app))
    val logs by vm.logs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server logs", style = MaterialTheme.typography.headlineLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = { vm.clear() }) { Icon(Icons.Filled.Delete, null) }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (logs.isEmpty()) {
                item {
                    EmptyState(
                        title = "No logs yet",
                        subtitle = "Start the server and access files to see activity."
                    )
                }
            } else {
                items(logs.size, key = { i -> logs[i].id.takeIf { it != 0L } ?: ("t" + logs[i].timestampMs + "i$i").hashCode().toLong() }, contentType = { "log" }) { i ->
                    val log = logs[i]
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).animateItem(),
                        verticalAlignment = Alignment.Top
                    ) {
                        ActivityIcon(activityKindOf(log.method, log.status))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    FormatUtils.formatTime(log.timestampMs),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${log.status}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when (log.status) {
                                        in 200..299 -> MaterialTheme.colorScheme.secondary
                                        in 400..499 -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            MonoText(
                                "${log.method} ${log.path}",
                                small = true
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                log.clientIp,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (log.message.isNotBlank()) {
                                Text(
                                    log.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

package com.saketkhundia.pocketserver.presentation.developer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.saketkhundia.pocketserver.domain.model.ServerStatus
import com.saketkhundia.pocketserver.presentation.components.Eyebrow
import com.saketkhundia.pocketserver.presentation.components.MonoText
import com.saketkhundia.pocketserver.presentation.glass.GlassBackground
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassListItem
import com.saketkhundia.pocketserver.presentation.home.rememberSharedHomeViewModel
import com.saketkhundia.pocketserver.presentation.theme.PsSpacing
import kotlinx.coroutines.launch

/**
 * Developer mode — restrained key/value diagnostics with copy support.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(onBack: () -> Unit) {
    // Activity-shared: zero per-visit init cost, one network observer total.
    val vm = rememberSharedHomeViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val info = buildString {
        appendLine("Server Status: ${state.serverState.status}")
        appendLine("HTTP Server: ${if (state.serverState.status == ServerStatus.RUNNING) "Running" else "Stopped"}")
        appendLine("Local IP: ${state.localIp ?: "—"}")
        appendLine("HTTP Port: ${state.settings.httpPort}")
        appendLine("FTP Port: ${state.settings.ftpPort} (enabled=${state.settings.ftpEnabled})")
        appendLine("Server Name: ${state.settings.serverName}")
        appendLine("URL: ${state.serverState.url ?: "—"}")
        appendLine("Active Connections: ${state.stats.activeConnections}")
        appendLine("Requests: ${state.stats.requests}")
        appendLine("Connected Devices: ${state.stats.connectedDevices}")
        appendLine("Shared Folders: ${state.folders.joinToString { it.name }}")
        appendLine("Auth Required: ${state.settings.authRequired}")
        appendLine("Network: ${state.networkDesc}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer", style = MaterialTheme.typography.headlineLarge) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(info))
                        scope.launch { snackbar.showSnackbar("Technical info copied") }
                    }) { Icon(Icons.Filled.ContentCopy, null) }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            GlassBackground(Modifier.fillMaxSize())
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(PsSpacing.xxl)
        ) {
            item {
                Column {
                    Eyebrow("Status")
                    Spacer(Modifier.height(4.dp))
                    InfoRow("HTTP server", if (state.serverState.status == ServerStatus.RUNNING) "Running" else "Stopped")
                    InfoRow("Local IP", state.localIp ?: "—", mono = true)
                    InfoRow("HTTP port", state.settings.httpPort.toString(), mono = true)
                    InfoRow("FTP port", "${state.settings.ftpPort}${if (state.settings.ftpEnabled) " · on" else " · off"}", mono = true)
                    InfoRow("Network", state.networkDesc)
                }
            }
            item {
                Column {
                    Eyebrow("Traffic")
                    Spacer(Modifier.height(4.dp))
                    InfoRow("Active connections", state.stats.activeConnections.toString(), mono = true)
                    InfoRow("Requests", state.stats.requests.toString(), mono = true)
                    InfoRow("Connected devices", state.stats.connectedDevices.toString(), mono = true)
                }
            }
            item {
                Column {
                    Eyebrow("Configuration")
                    Spacer(Modifier.height(4.dp))
                    InfoRow("URL", state.serverState.url ?: "—", mono = true)
                    InfoRow("Shared folders", state.folders.joinToString { it.name }.ifBlank { "—" })
                    InfoRow("Auth", if (state.settings.authRequired) "Required" else "Disabled")
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, mono: Boolean = false) {
    Column {
        LiquidGlassListItem(
            title = label,
            subtitle = null,
            trailing = {
                if (mono) MonoText(value, small = true)
                else Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        )
        Spacer(Modifier.height(8.dp))
    }
}

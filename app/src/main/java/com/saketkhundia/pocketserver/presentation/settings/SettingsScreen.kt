package com.saketkhundia.pocketserver.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saketkhundia.pocketserver.PocketServerApp
import com.saketkhundia.pocketserver.domain.model.AppSettings
import com.saketkhundia.pocketserver.domain.model.AppThemeMode
import com.saketkhundia.pocketserver.presentation.components.DialogEntrance
import com.saketkhundia.pocketserver.presentation.components.Eyebrow
import com.saketkhundia.pocketserver.presentation.theme.PsSpacing

/**
 * Grouped settings with clear spacing — rows with dividers,
 * no giant cards. Edits happen in focused dialogs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenLogs: () -> Unit,
    onOpenDeveloper: () -> Unit,
    onOpenFiles: () -> Unit
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as PocketServerApp
    val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
    val settings by vm.settings.collectAsState()
    val folders by vm.folders.collectAsState()
    val msg by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    var dialog by remember { mutableStateOf<EditTarget?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(msg) { msg?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    if (showResetConfirm) {
        // Shared subtle entrance: fade + 0.96 → 1.0 scale, 180ms, no bounce.
        DialogEntrance {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text("Reset sign-in?") },
                text = {
                    Text(
                        "Restores admin / admin, signs out all browsers and clears login blocks. Change the password right after.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showResetConfirm = false; vm.resetSignIn() }) { Text("Reset") }
                },
                dismissButton = { TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") } },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }

    if (dialog != null) {
        DialogEntrance {
            EditDialog(
                target = dialog!!,
                settings = settings,
                onDismiss = { dialog = null },
                onConfirm = { value, extra ->
                    when (dialog!!) {
                        EditTarget.ServerName -> vm.updateServerName(value)
                        EditTarget.HttpPort -> vm.updatePort(value, true)
                        EditTarget.FtpPort -> vm.updatePort(value, false)
                        EditTarget.Username -> vm.changeCredentials(value, extra ?: "")
                        EditTarget.Timeout -> value.toLongOrNull()?.let { vm.setSessionTimeout(it) }
                    }
                    dialog = null
                }
            )
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineLarge) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            com.saketkhundia.pocketserver.presentation.components.AmoledBackground(Modifier.fillMaxSize())
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(PsSpacing.xxl)
            ) {
            item(key = "server", contentType = "group") {
                SettingsGroup(title = "Server") {
                    ValueRow("Server name", settings.serverName.ifBlank { "Pocket Server" }) { dialog = EditTarget.ServerName }
                    ValueRow("HTTP port", settings.httpPort.toString()) { dialog = EditTarget.HttpPort }
                    ValueRow("FTP port", settings.ftpPort.toString()) { dialog = EditTarget.FtpPort }
                    SwitchRow("Auto start", "Start the server when the app opens.", settings.autoStart) { vm.toggleAutoStart(it) }
                    SwitchRow("Background server", "Keep running when the app is closed.", settings.keepRunning) { vm.toggleKeepRunning(it) }
                }
            }
            item(key = "security", contentType = "group") {
                SettingsGroup(title = "Security") {
                    SwitchRow("Authentication", "Require a login from browsers.", settings.authRequired) { vm.toggleAuth(it) }
                    ValueRow("Username", settings.username.ifBlank { "admin" }) { dialog = EditTarget.Username }
                    ValueRow("Session timeout", "${settings.sessionTimeoutMin} min") { dialog = EditTarget.Timeout }
                    NavRow("Reset sign-in", destructive = true) { showResetConfirm = true }
                }
            }
            item(key = "storage", contentType = "group") {
                SettingsGroup(title = "Storage") {
                    ValueRow(
                        "Shared folders",
                        if (folders.isEmpty()) "None" else "${folders.size} · ${folders.take(2).joinToString { it.name }}"
                    ) { onOpenFiles() }
                    SwitchRow("Web server mode", "Serve a folder as a static website.", settings.webServerMode) { vm.toggleWebMode(it) }
                    // Sections expand/collapse instead of popping — AnimatedVisibility,
                    // not instant add/remove.
                    AnimatedVisibility(
                        visible = settings.webServerMode,
                        enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                            expandVertically(tween(200, easing = FastOutSlowInEasing)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
                        label = "webRoot"
                    ) {
                        Column {
                            if (folders.isEmpty()) {
                                Text(
                                    "Add a shared folder first.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            } else {
                                folders.forEach { f ->
                                    val selected = f.id == settings.webRootFolderId
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable { vm.setWebRoot(if (selected) null else f.id) }
                                            .padding(vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(f.name, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            if (selected) "Selected" else "",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                    SwitchRow("FTP server", "Independent file access over FTP.", settings.ftpEnabled) { vm.toggleFtp(it) }
                    AnimatedVisibility(
                        visible = settings.ftpEnabled,
                        enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                            expandVertically(tween(200, easing = FastOutSlowInEasing)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
                        label = "ftpHint"
                    ) {
                        Text(
                            "ftp://<phone-ip>:${settings.ftpPort}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
            item(key = "appearance", contentType = "group") {
                SettingsGroup(title = "Appearance") {
                    ThemeRow(current = settings.themeMode, onPick = { vm.setTheme(it) })
                }
            }
            item(key = "advanced", contentType = "group") {
                SettingsGroup(title = "Advanced") {
                    NavRow("Server logs") { onOpenLogs() }
                    NavRow("Developer mode") { onOpenDeveloper() }
                }
            }
            item(key = "spacer", contentType = "spacer") { Spacer(Modifier.height(110.dp)) }
            }
        }
    }

}

private enum class EditTarget { ServerName, HttpPort, FtpPort, Username, Timeout }

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        Eyebrow(title)
        Spacer(Modifier.height(4.dp))
        content()
    }
}

@Composable
private fun ValueRow(label: String, value: String, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SwitchRow(label: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onChange)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun NavRow(label: String, destructive: Boolean = false, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ThemeRow(current: AppThemeMode, onPick: (AppThemeMode) -> Unit) {
    Column {
        listOf(AppThemeMode.SYSTEM, AppThemeMode.DARK, AppThemeMode.LIGHT).forEach { mode ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clickable { onPick(mode) }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                if (mode == current) {
                    Text("Active", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun EditDialog(
    target: EditTarget,
    settings: AppSettings,
    onDismiss: () -> Unit,
    onConfirm: (value: String, extra: String?) -> Unit
) {
    val title: String
    val initial: String
    val numeric: Boolean
    val isPasswordChange: Boolean
    when (target) {
        EditTarget.ServerName -> { title = "Server name"; initial = settings.serverName; numeric = false; isPasswordChange = false }
        EditTarget.HttpPort -> { title = "HTTP port"; initial = settings.httpPort.toString(); numeric = true; isPasswordChange = false }
        EditTarget.FtpPort -> { title = "FTP port"; initial = settings.ftpPort.toString(); numeric = true; isPasswordChange = false }
        EditTarget.Username -> { title = "Username"; initial = settings.username; numeric = false; isPasswordChange = true }
        EditTarget.Timeout -> { title = "Session timeout (minutes)"; initial = settings.sessionTimeoutMin.toString(); numeric = true; isPasswordChange = false }
    }
    var value by remember { mutableStateOf(initial) }
    var extra by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = if (numeric) it.filter { c -> c.isDigit() }.take(5) else it.take(64)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isPasswordChange) {
                    OutlinedTextField(
                        value = extra,
                        onValueChange = { extra = it },
                        label = { Text("New password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Changing credentials signs out all browsers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (numeric && target != EditTarget.Timeout) {
                    Text(
                        "Use ports 1024–65535.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isPasswordChange) {
                        if (value.isBlank() || extra.length < 4) return@TextButton
                    } else if (value.isBlank()) return@TextButton
                    onConfirm(value.trim(), extra.ifBlank { null })
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        shape = RoundedCornerShape(20.dp)
    )
}

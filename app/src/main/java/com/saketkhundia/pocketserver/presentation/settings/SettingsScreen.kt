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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saketkhundia.pocketserver.PocketServerApp
import com.saketkhundia.pocketserver.domain.model.AppSettings
import com.saketkhundia.pocketserver.presentation.components.DialogEntrance
import com.saketkhundia.pocketserver.presentation.components.Eyebrow
import com.saketkhundia.pocketserver.presentation.glass.GlassLevel
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassDialog
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassListItem
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassSurface
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassSwitch
import com.saketkhundia.pocketserver.presentation.glass.LiquidGlassTextField
import com.saketkhundia.pocketserver.presentation.glass.LocalGlassColors
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
    val settings by vm.settings.collectAsStateWithLifecycle()
    val folders by vm.folders.collectAsStateWithLifecycle()
    val msg by vm.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var dialog by remember { mutableStateOf<EditTarget?>(null) }
    var showResetConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(msg) { msg?.let { snackbar.showSnackbar(it); vm.clearMessage() } }

    if (showResetConfirm) {
        // Real dialog window (dim + focus + back-press). The glass panel
        // alone renders inline — without Dialog it has no window/scrim.
        Dialog(
            onDismissRequest = { showResetConfirm = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                DialogEntrance {
                    LiquidGlassDialog(
                        title = "Reset sign-in?",
                        body = "Restores admin / admin, signs out all browsers and clears login blocks. Change the password right after.",
                        primaryLabel = "Reset",
                        onPrimary = { showResetConfirm = false; vm.resetSignIn() },
                        secondaryLabel = "Cancel",
                        onDismiss = { showResetConfirm = false }
                    )
                }
            }
        }
    }

    if (dialog != null) {
        Dialog(
            onDismissRequest = { dialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
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
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineLarge) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
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
                                            color = Color.White.copy(alpha = 0.95f)
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
    val g = LocalGlassColors.current
    Column {
        LiquidGlassListItem(
            title = label, subtitle = null,
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(value, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.60f))
                    Spacer(Modifier.width(6.dp))
                    androidx.compose.material3.Icon(com.saketkhundia.pocketserver.presentation.theme.PsIcons.ChevronRight, null, tint = Color.White.copy(alpha = 0.30f), modifier = Modifier.size(18.dp))
                }
            },
            onClick = onClick
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SwitchRow(label: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Column {
        LiquidGlassListItem(
            title = label, subtitle = subtitle,
            trailing = { LiquidGlassSwitch(checked = checked, onChange = onChange) },
            onClick = { onChange(!checked) }
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun NavRow(label: String, destructive: Boolean = false, onClick: () -> Unit) {
    Column {
        LiquidGlassListItem(
            title = label, subtitle = null,
            trailing = {
                androidx.compose.material3.Icon(
                    com.saketkhundia.pocketserver.presentation.theme.PsIcons.ChevronRight, null,
                    tint = Color.White.copy(alpha = 0.30f),
                    modifier = Modifier.size(18.dp)
                )
            },
            onClick = onClick
        )
        Spacer(Modifier.height(8.dp))
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
    val g = LocalGlassColors.current

    LiquidGlassSurface(level = GlassLevel.L3, radius = com.saketkhundia.pocketserver.presentation.glass.GlassShapes.large, glow = null, frosted = true, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = g.textPrimary)
            LiquidGlassTextField(
                value = value,
                onValue = { value = if (numeric) it.filter { c -> c.isDigit() }.take(5) else it.take(64) },
                placeholder = title
            )
            if (isPasswordChange) {
                LiquidGlassTextField(value = extra, onValue = { extra = it }, placeholder = "New password")
                Text("Changing credentials signs out all browsers.", style = MaterialTheme.typography.bodySmall, color = g.textSecondary)
            }
            if (numeric && target != EditTarget.Timeout) {
                Text("Use ports 1024–65535.", style = MaterialTheme.typography.bodySmall, color = g.textSecondary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                com.saketkhundia.pocketserver.presentation.glass.LiquidGlassSecondaryButton("Cancel", onDismiss, Modifier.weight(1f))
                com.saketkhundia.pocketserver.presentation.glass.LiquidGlassButton(
                    "Save",
                    onClick = {
                        if (isPasswordChange) {
                            if (value.isBlank() || extra.length < 4) return@LiquidGlassButton
                        } else if (value.isBlank()) return@LiquidGlassButton
                        onConfirm(value.trim(), extra.ifBlank { null })
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

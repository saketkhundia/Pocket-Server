package com.saketkhundia.pocketserver.presentation.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saketkhundia.pocketserver.PocketServerApp
import com.saketkhundia.pocketserver.domain.model.AppSettings
import com.saketkhundia.pocketserver.domain.model.LocalServerAddress
import com.saketkhundia.pocketserver.domain.model.LogEntry
import com.saketkhundia.pocketserver.domain.model.MdnsStatus
import com.saketkhundia.pocketserver.domain.model.ServerState
import com.saketkhundia.pocketserver.domain.model.ServerStats
import com.saketkhundia.pocketserver.domain.model.ServerStatus
import com.saketkhundia.pocketserver.domain.model.SharedFolder
import com.saketkhundia.pocketserver.network.LocalIpProvider
import com.saketkhundia.pocketserver.service.ServerForegroundService
import com.saketkhundia.pocketserver.util.FormatUtils
import com.saketkhundia.pocketserver.util.StorageStats
import com.saketkhundia.pocketserver.util.StorageStatsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val serverState: ServerState = ServerState(),
    val stats: ServerStats = ServerStats(),
    val logs: List<LogEntry> = emptyList(),
    val folders: List<SharedFolder> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val localIp: String? = null,
    val networkDesc: String = "No network",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val shareUrl: String? = null
)

class HomeViewModel(private val app: PocketServerApp) : ViewModel() {

    private val container = app.container
    private val _error = MutableStateFlow<String?>(null)
    // NOTE: these initializers run synchronously on the Main thread during
    // ViewModel creation (i.e. mid-navigation-transition when returning Home).
    // NetworkInterface enumeration can stall for seconds on stale/VPN/doze
    // interfaces — so anything that touches interfaces starts empty here and
    // loads async in refreshNetwork(). describeNetwork/troubleshootingHint are
    // cheap binder calls and stay synchronous.
    private val _localIp = MutableStateFlow<String?>(null)
    private val _networkDesc = MutableStateFlow(LocalIpProvider.describeNetwork(app))
    private val _troubleshoot = MutableStateFlow(LocalIpProvider.troubleshootingHint(app))
    val troubleshootHint: StateFlow<String> = _troubleshoot
    private val _candidates = MutableStateFlow<List<String>>(emptyList())
    val allCandidates: StateFlow<List<String>> = _candidates
    private val _storage = MutableStateFlow<StorageStats?>(null)
    val storageStats: StateFlow<StorageStats?> = _storage
    /** Coalesces overlapping refreshes (init + network flaps): latest wins. */
    private var refreshJob: Job? = null
    /** Dedicated error flow so snackbars react without recomposing the whole screen. */
    val errorMessage: StateFlow<String?> = _error
    /** Direct IP flow for isolated collection (avoids pulling the whole uiState). */
    val localIpFlow: StateFlow<String?> = _localIp
    val networkDescFlow: StateFlow<String> = _networkDesc

    // ── Granular flows for isolated collection ──────────────────
    // Collecting the whole uiState at the top of Home recomposes the entire
    // screen on every stats tick / log line. These slices let each section
    // (hero, storage, activity) recompose independently.
    val serverState: StateFlow<ServerState> = container.serverStateRepository.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServerState())
    val statsFlow: StateFlow<ServerStats> = container.serverStateRepository.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServerStats())
    val foldersFlow: StateFlow<List<SharedFolder>> = container.sharedFolderRepository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settingsFlow: StateFlow<AppSettings> = container.settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())
    val recentLogs: StateFlow<List<LogEntry>> = container.serverStateRepository.logs
        .map { it.take(4) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Single source of truth for addressing: friendly hostname first, IP
     * fallback second. Screens consume [LocalServerAddress.primaryUrl] and
     * never hand-build network URLs.
     */
    val address: StateFlow<LocalServerAddress> = container.serverStateRepository.state
        .map { LocalServerAddress(it.hostnameUrl, it.url, it.mdnsStatus, it.pendingHostnameUrl) }
        .stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000),
            LocalServerAddress(null, null, MdnsStatus.IDLE)
        )

    val uiState: StateFlow<HomeUiState> = combine(
        container.serverStateRepository.state,
        container.serverStateRepository.stats,
        container.serverStateRepository.logs,
        container.sharedFolderRepository.folders,
        container.settingsRepository.settings,
        _localIp,
        _networkDesc,
        _error
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val state = args[0] as ServerState
        val stats = args[1] as ServerStats
        val logs = args[2] as List<LogEntry>
        val folders = args[3] as List<SharedFolder>
        val settings = args[4] as AppSettings
        val ip = args[5] as String?
        val net = args[6] as String
        val err = args[7] as String?
        // Compute uptime
        val uptime = if (state.status == ServerStatus.RUNNING && state.startedAtMs != null) {
            System.currentTimeMillis() - state.startedAtMs
        } else 0L
        HomeUiState(
            serverState = state,
            stats = stats.copy(uptimeMs = uptime),
            logs = logs.take(20),
            folders = folders,
            settings = settings,
            localIp = ip,
            networkDesc = net,
            errorMessage = err
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        refreshNetwork()
        refreshStorage()
        // Observe network changes — auto-refresh displayed IP and server state if running
        viewModelScope.launch {
            container.networkManager.networkChanges.collect { _ ->
                refreshNetwork()
                // refreshIpOnNetworkChange() enumerates interfaces (blocking) —
                // never on Main; fire-and-forget so the collector stays responsive.
                viewModelScope.launch(Dispatchers.IO) {
                    try { container.httpServerManager.refreshIpOnNetworkChange() } catch (_: Exception) {}
                }
            }
        }
    }

    fun refreshNetwork() {
        // Interface enumeration + provider queries run on IO. Posting to
        // StateFlow is thread-safe; the UI fills in as results arrive instead
        // of blocking the navigation transition.
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            _localIp.value = LocalIpProvider.getLocalIpv4(app)
            _networkDesc.value = LocalIpProvider.describeNetwork(app)
            _troubleshoot.value = LocalIpProvider.troubleshootingHint(app)
            try { _candidates.value = LocalIpProvider.getAllCandidates(app) } catch (_: Exception) {}
            _storage.value = StorageStatsProvider.snapshot()
        }
    }

    fun refreshStorage() {
        viewModelScope.launch {
            _storage.value = StorageStatsProvider.snapshot()
        }
    }

    fun startServer(context: Context) {
        viewModelScope.launch {
            _error.value = null
            // Same rule as refreshNetwork: interface enumeration off Main.
            // Uses the app context (equivalent for interface lookup, no leak).
            val ip = withContext(Dispatchers.IO) { LocalIpProvider.getLocalIpv4(app) }
            if (ip == null) {
                val detail = withContext(Dispatchers.IO) {
                    "No local network. ${LocalIpProvider.troubleshootingHint(app)} " +
                        "Candidates: ${LocalIpProvider.getAllCandidates(app).joinToString()}"
                }
                _error.value = detail
                return@launch
            }
            if (container.settingsRepository.snapshot().httpPort < 1024) {
                _error.value = "Invalid port"
                return@launch
            }
            // Start via foreground service
            val intent = Intent(context, ServerForegroundService::class.java).apply {
                action = ServerForegroundService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent) else context.startService(intent)
        }
    }

    fun stopServer(context: Context) {
        viewModelScope.launch {
            _error.value = null
            val intent = Intent(context, ServerForegroundService::class.java).apply {
                action = ServerForegroundService.ACTION_STOP
            }
            context.startService(intent)
            // Also stop directly via manager for immediate UI feedback
            try { container.httpServerManager.stop() } catch (_: Exception) {}
            try { container.ftpServerManager.stop() } catch (_: Exception) {}
        }
    }

    fun shareUrl(context: Context) {
        // Friendly hostname first, IP fallback second — same priority as UI.
        val url = address.value.primaryUrl ?: run {
            val ip = _localIp.value ?: return
            val port = uiState.value.settings.httpPort
            "http://$ip:$port"
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
            putExtra(Intent.EXTRA_SUBJECT, "Pocket Server")
        }
        context.startActivity(Intent.createChooser(send, "Share Server URL"))
    }

    fun addFolder(context: Context, treeUri: Uri) {
        viewModelScope.launch {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    context.contentResolver.takePersistableUriPermission(treeUri, flags)
                } catch (_: Exception) {}
                val doc = DocumentFile.fromTreeUri(context, treeUri) ?: run {
                    _error.value = "Unable to access folder"
                    return@launch
                }
                val name = doc.name ?: "Folder ${System.currentTimeMillis()}"
                // Check feature gate for max folders
                val current = container.sharedFolderRepository.snapshot()
                val limit = container.featureAccess.maxSharedFolders()
                if (current.size >= limit) {
                    _error.value = "Free version limited to $limit folders. Upgrade to Pro for unlimited."
                    return@launch
                }
                if (current.any { it.uri == treeUri.toString() }) {
                    _error.value = "Folder already added"
                    return@launch
                }
                container.sharedFolderRepository.add(name.take(64), treeUri.toString(), false)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to add folder"
            }
        }
    }

    fun removeFolder(id: String) {
        viewModelScope.launch {
            container.sharedFolderRepository.remove(id)
        }
    }

    fun clearError() { _error.value = null }

    fun formatStats(): List<Pair<String, String>> {
        val s = uiState.value.stats
        return listOf(
            "Requests" to s.requests.toString(),
            "Uploaded" to FormatUtils.formatBytes(s.bytesUploaded),
            "Downloaded" to FormatUtils.formatBytes(s.bytesDownloaded),
            "Uptime" to FormatUtils.formatUptime(s.uptimeMs)
        )
    }

    companion object {
        fun factory(app: PocketServerApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(app) as T
            }
        }
    }
}

/**
 * Activity-shared HomeViewModel for every screen that reads server state
 * (Home, Files, QR, Developer).
 *
 * WHY: the default `viewModel()` is scoped to the NavBackStackEntry, and tab
 * switches pop entries (popUpTo + saveState). So each visit DESTROYED and
 * recreated the VM: re-registering a network callback, re-firing refresh
 * storms, re-subscribing every sharing coroutine and resetting UI-adjacent
 * state. After a few switches the churn piled up and tabs felt dead.
 * One shared instance = one network observer, permanently warm flows, and
 * zero per-visit initialization cost. Entry scoping stays for genuinely
 * screen-local VMs (Media, Logs, Settings).
 */
@Composable
fun rememberSharedHomeViewModel(): HomeViewModel {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as PocketServerApp
    // LocalContext inside MainActivity's content is the activity itself, which
    // is a ViewModelStoreOwner — so this instance survives tab switches.
    val owner = ctx as ViewModelStoreOwner
    return viewModel(viewModelStoreOwner = owner, factory = HomeViewModel.factory(app))
}

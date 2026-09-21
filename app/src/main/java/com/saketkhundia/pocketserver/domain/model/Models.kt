package com.saketkhundia.pocketserver.domain.model

import kotlinx.serialization.Serializable

enum class ServerStatus { STOPPED, STARTING, RUNNING, ERROR }

/** mDNS local-name advertisement state. Never blocks the HTTP server. */
enum class MdnsStatus { IDLE, REGISTERING, AVAILABLE, UNAVAILABLE }

data class ServerState(
    val status: ServerStatus = ServerStatus.STOPPED,
    val url: String? = null,
    val localIp: String? = null,
    val port: Int = 8080,
    val error: String? = null,
    val startedAtMs: Long? = null,
    /** Friendly URL (e.g. http://pocketserver.local:8080) once VERIFIED. Null otherwise — never faked. */
    val hostnameUrl: String? = null,
    /** Expected name shown instantly while VERIFYING (with a checking marker). */
    val pendingHostnameUrl: String? = null,
    val mdnsStatus: MdnsStatus = MdnsStatus.IDLE
)

/**
 * Single source of truth for "which address do we show/use".
 * UI must never hand-build network URLs — consume [displayUrl] for display
 * and [primaryUrl] for actions (open/copy/share/QR).
 */
data class LocalServerAddress(
    val hostnameUrl: String?,
    val ipUrl: String?,
    val mdnsStatus: MdnsStatus = MdnsStatus.IDLE,
    val pendingHostnameUrl: String? = null
) {
    /** Display: verified name → expected (checking) name → IP. */
    val displayUrl: String? get() = hostnameUrl ?: pendingHostnameUrl ?: ipUrl
    /** Functional: verified name → IP. Pending names never drive actions. */
    val primaryUrl: String? get() = hostnameUrl ?: ipUrl
    val mdnsAvailable: Boolean get() = mdnsStatus == MdnsStatus.AVAILABLE && hostnameUrl != null
    val mdnsChecking: Boolean get() = mdnsStatus == MdnsStatus.REGISTERING && hostnameUrl == null && pendingHostnameUrl != null
}

data class SharedFolder(
    val id: String,
    val name: String,
    val uri: String,
    val readOnly: Boolean = false
)

data class FileEntry(
    val name: String,
    val path: String, // virtual path like "Downloads/a/b.pdf" or "" for root
    val isDir: Boolean,
    val size: Long,
    val lastModified: Long,
    val mime: String? = null
)

data class ServerStats(
    val requests: Long = 0,
    val bytesUploaded: Long = 0,
    val bytesDownloaded: Long = 0,
    val activeConnections: Int = 0,
    val connectedDevices: Int = 0,
    val uptimeMs: Long = 0
)

data class LogEntry(
    val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val clientIp: String = "",
    val method: String = "",
    val path: String = "",
    val status: Int = 200,
    val message: String = ""
)

@Serializable
data class ApiFile(
    val name: String,
    val path: String,
    val isDir: Boolean,
    val size: Long,
    val lastModified: Long,
    val mime: String? = null
)

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val httpPort: Int = 8080,
    val ftpPort: Int = 2121,
    val ftpEnabled: Boolean = false,
    val ftpUser: String = "pocket",
    val serverName: String = "Pocket Server",
    val autoStart: Boolean = false,
    val keepRunning: Boolean = true,
    val authRequired: Boolean = true,
    val username: String = "admin",
    val sessionTimeoutMin: Long = 60,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val webRootFolderId: String? = null,
    val webServerMode: Boolean = false,
    val onboarded: Boolean = false
)

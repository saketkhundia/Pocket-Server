package com.saketkhundia.pocketserver.domain.model

import kotlinx.serialization.Serializable

enum class ServerStatus { STOPPED, STARTING, RUNNING, ERROR }

data class ServerState(
    val status: ServerStatus = ServerStatus.STOPPED,
    val url: String? = null,
    val localIp: String? = null,
    val port: Int = 8080,
    val error: String? = null,
    val startedAtMs: Long? = null
)

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

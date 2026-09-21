package com.saketkhundia.pocketserver.domain.repository

import com.saketkhundia.pocketserver.domain.model.AppSettings
import com.saketkhundia.pocketserver.domain.model.LogEntry
import com.saketkhundia.pocketserver.domain.model.ServerState
import com.saketkhundia.pocketserver.domain.model.ServerStats
import com.saketkhundia.pocketserver.domain.model.SharedFolder
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
    suspend fun snapshot(): AppSettings
}

interface SharedFolderRepository {
    val folders: Flow<List<SharedFolder>>
    suspend fun add(name: String, uri: String, readOnly: Boolean = false): SharedFolder
    suspend fun remove(id: String)
    suspend fun snapshot(): List<SharedFolder>
    suspend fun updateReadOnly(id: String, readOnly: Boolean)
}

interface ServerStateRepository {
    val state: Flow<ServerState>
    val stats: Flow<ServerStats>
    val logs: Flow<List<LogEntry>>
    suspend fun setState(s: ServerState)
    suspend fun updateStats(t: (ServerStats) -> ServerStats)
    suspend fun addLog(e: LogEntry)
    suspend fun clearLogs()
}

interface AuthCredentialRepository {
    suspend fun setCredentials(username: String, passwordHash: String, salt: String)
    suspend fun getUsername(): String
    suspend fun getPasswordHash(): String?
    suspend fun getSalt(): String?
    suspend fun hasCredentials(): Boolean
}

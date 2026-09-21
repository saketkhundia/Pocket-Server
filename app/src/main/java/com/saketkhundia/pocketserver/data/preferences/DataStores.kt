package com.saketkhundia.pocketserver.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.saketkhundia.pocketserver.domain.model.AppSettings
import com.saketkhundia.pocketserver.domain.model.AppThemeMode
import com.saketkhundia.pocketserver.domain.repository.AuthCredentialRepository
import com.saketkhundia.pocketserver.domain.repository.SettingsRepository
import com.saketkhundia.pocketserver.domain.repository.SharedFolderRepository
import com.saketkhundia.pocketserver.domain.model.SharedFolder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore("pocket_server_prefs")

class DataStoreSettingsRepository(private val ctx: Context) : SettingsRepository {
    private object K {
        val httpPort = intPreferencesKey("http_port")
        val ftpPort = intPreferencesKey("ftp_port")
        val ftpEnabled = booleanPreferencesKey("ftp_enabled")
        val ftpUser = stringPreferencesKey("ftp_user")
        val serverName = stringPreferencesKey("server_name")
        val autoStart = booleanPreferencesKey("auto_start")
        val keepRunning = booleanPreferencesKey("keep_running")
        val authRequired = booleanPreferencesKey("auth_required")
        val username = stringPreferencesKey("username")
        val sessionTimeout = longPreferencesKey("session_timeout_min")
        val theme = stringPreferencesKey("theme")
        val webRoot = stringPreferencesKey("web_root")
        val webMode = booleanPreferencesKey("web_mode")
        val onboarded = booleanPreferencesKey("onboarded")
    }

    override val settings: Flow<AppSettings> = ctx.dataStore.data.map { p ->
        AppSettings(
            httpPort = p[K.httpPort] ?: 8080,
            ftpPort = p[K.ftpPort] ?: 2121,
            ftpEnabled = p[K.ftpEnabled] ?: false,
            ftpUser = p[K.ftpUser] ?: "pocket",
            serverName = p[K.serverName] ?: "Pocket Server",
            autoStart = p[K.autoStart] ?: false,
            keepRunning = p[K.keepRunning] ?: true,
            authRequired = p[K.authRequired] ?: true,
            username = p[K.username] ?: "admin",
            sessionTimeoutMin = p[K.sessionTimeout] ?: 60,
            themeMode = runCatching { AppThemeMode.valueOf(p[K.theme] ?: "SYSTEM") }.getOrDefault(AppThemeMode.SYSTEM),
            webRootFolderId = p[K.webRoot],
            webServerMode = p[K.webMode] ?: false,
            onboarded = p[K.onboarded] ?: false
        )
    }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        val cur = settings.first()
        val next = transform(cur)
        ctx.dataStore.edit { e ->
            e[K.httpPort] = next.httpPort.coerceIn(1024, 65535)
            e[K.ftpPort] = next.ftpPort.coerceIn(1024, 65535)
            e[K.ftpEnabled] = next.ftpEnabled
            e[K.ftpUser] = next.ftpUser.take(64)
            e[K.serverName] = next.serverName.take(64).ifBlank { "Pocket Server" }
            e[K.autoStart] = next.autoStart
            e[K.keepRunning] = next.keepRunning
            e[K.authRequired] = next.authRequired
            e[K.username] = next.username.take(64).ifBlank { "admin" }
            e[K.sessionTimeout] = next.sessionTimeoutMin.coerceIn(5, 1440)
            e[K.theme] = next.themeMode.name
            if (next.webRootFolderId == null) e.remove(K.webRoot) else e[K.webRoot] = next.webRootFolderId
            e[K.webMode] = next.webServerMode
            e[K.onboarded] = next.onboarded
        }
    }

    override suspend fun snapshot(): AppSettings = settings.first()
}

class DataStoreAuthCredentials(private val ctx: Context) : AuthCredentialRepository {
    private object K {
        val user = stringPreferencesKey("auth_user")
        val hash = stringPreferencesKey("auth_hash")
        val salt = stringPreferencesKey("auth_salt")
    }
    override suspend fun setCredentials(username: String, passwordHash: String, salt: String) {
        ctx.dataStore.edit { it[K.user] = username; it[K.hash] = passwordHash; it[K.salt] = salt }
    }
    override suspend fun getUsername(): String = ctx.dataStore.data.map { it[K.user] ?: "admin" }.first()
    override suspend fun getPasswordHash(): String? = ctx.dataStore.data.map { it[K.hash] }.first()
    override suspend fun getSalt(): String? = ctx.dataStore.data.map { it[K.salt] }.first()
    override suspend fun hasCredentials(): Boolean = getPasswordHash() != null && getSalt() != null
}

class DataStoreSharedFolders(private val ctx: Context) : SharedFolderRepository {
    private val key = stringPreferencesKey("shared_folders_json")
    private val gson = org.json.JSONArray()

    override val folders: Flow<List<SharedFolder>> =
        ctx.dataStore.data.map { p -> decode(p[key]) }

    private fun decode(raw: String?): List<SharedFolder> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                SharedFolder(o.getString("id"), o.getString("name"), o.getString("uri"), o.optBoolean("ro", false))
            }
        } catch (_: Exception) { emptyList() }
    }

    private fun encode(list: List<SharedFolder>): String {
        val arr = org.json.JSONArray()
        list.forEach { arr.put(org.json.JSONObject().put("id", it.id).put("name", it.name).put("uri", it.uri).put("ro", it.readOnly)) }
        return arr.toString()
    }

    override suspend fun add(name: String, uri: String, readOnly: Boolean): SharedFolder {
        var created = SharedFolder(UUID.randomUUID().toString(), name.take(128), uri, readOnly)
        ctx.dataStore.edit { e ->
            val cur = decode(e[key]).toMutableList()
            // avoid duplicate URIs
            cur.find { it.uri == uri }?.let { created = it; return@edit }
            cur.add(created)
            e[key] = encode(cur)
        }
        return created
    }

    override suspend fun remove(id: String) {
        ctx.dataStore.edit { e -> e[key] = encode(decode(e[key]).filterNot { it.id == id }) }
    }

    override suspend fun snapshot(): List<SharedFolder> = folders.first()

    override suspend fun updateReadOnly(id: String, readOnly: Boolean) {
        ctx.dataStore.edit { e ->
            e[key] = encode(decode(e[key]).map { if (it.id == id) it.copy(readOnly = readOnly) else it })
        }
    }
}

package com.saketkhundia.pocketserver.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.saketkhundia.pocketserver.PocketServerApp
import com.saketkhundia.pocketserver.domain.model.AppSettings
import com.saketkhundia.pocketserver.domain.model.AppThemeMode
import com.saketkhundia.pocketserver.domain.model.SharedFolder
import com.saketkhundia.pocketserver.domain.usecase.ValidatePortUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(private val app: PocketServerApp) : ViewModel() {
    private val container = app.container
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    private val _folders = MutableStateFlow<List<SharedFolder>>(emptyList())
    val folders: StateFlow<List<SharedFolder>> = _folders.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    init {
        viewModelScope.launch {
            container.settingsRepository.settings.collectLatest { _settings.value = it }
        }
        viewModelScope.launch {
            container.sharedFolderRepository.folders.collectLatest { _folders.value = it }
        }
    }

    fun updatePort(raw: String, isHttp: Boolean) {
        val res = ValidatePortUseCase.validateString(raw)
        if (res is ValidatePortUseCase.Result.Invalid) {
            _message.value = res.reason
            return
        }
        val p = raw.toInt()
        viewModelScope.launch {
            container.settingsRepository.update {
                if (isHttp) it.copy(httpPort = p) else it.copy(ftpPort = p)
            }
            _message.value = "Port saved"
        }
    }

    fun updateServerName(name: String) {
        if (name.isBlank()) { _message.value = "Name cannot be empty"; return }
        viewModelScope.launch {
            container.settingsRepository.update { it.copy(serverName = name.take(64)) }
            _message.value = "Server name saved"
        }
    }

    fun toggleAutoStart(v: Boolean) = save { it.copy(autoStart = v) }
    fun toggleKeepRunning(v: Boolean) = save { it.copy(keepRunning = v) }
    fun toggleAuth(v: Boolean) {
        viewModelScope.launch {
            container.settingsRepository.update { it.copy(authRequired = v) }
            container.authManager.authRequired = v
            _message.value = if (v) "Authentication enabled" else "Authentication disabled"
        }
    }
    fun toggleFtp(v: Boolean) = save { it.copy(ftpEnabled = v) }
    fun toggleWebMode(v: Boolean) = save { it.copy(webServerMode = v) }
    fun setWebRoot(id: String?) = save { it.copy(webRootFolderId = id) }

    fun setTheme(mode: AppThemeMode) = save { it.copy(themeMode = mode) }

    fun setSessionTimeout(min: Long) {
        val v = min.coerceIn(5, 1440)
        // Persist AND live-apply: previously only the repo was updated, so a
        // running server kept the old session expiry until restart.
        container.authManager.sessions.setTimeoutMin(v)
        save { it.copy(sessionTimeoutMin = v) }
        _message.value = "Session timeout saved"
    }

    fun changeCredentials(newUser: String, newPass: String) {
        if (newUser.length !in 1..64) { _message.value = "Username must be 1–64 chars"; return }
        if (newPass.length < 4 || newPass.length > 256) { _message.value = "Password must be 4–256 chars"; return }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                container.authManager.changeCredentials(newUser, newPass)
                container.settingsRepository.update { it.copy(username = newUser) }
                _message.value = "Credentials updated"
            } catch (e: Exception) {
                _message.value = e.message ?: "Failed"
            } finally { _isSaving.value = false }
        }
    }

    fun resetSignIn() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                container.authManager.resetCredentials()
                container.settingsRepository.update { it.copy(username = "admin") }
                _message.value = "Sign-in reset to admin / admin"
            } catch (e: Exception) {
                _message.value = e.message ?: "Failed"
            } finally { _isSaving.value = false }
        }
    }

    fun clearMessage() { _message.value = null }

    private fun save(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            container.settingsRepository.update(transform)
        }
    }

    companion object {
        fun factory(app: PocketServerApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(app) as T
        }
    }
}

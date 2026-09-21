package com.saketkhundia.pocketserver

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.saketkhundia.pocketserver.di.AppContainer
import com.saketkhundia.pocketserver.util.Constants
import kotlinx.coroutines.launch

class PocketServerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        createNotificationChannel()

        // Initialize default credentials non-blocking; secure default is admin/admin,
        // user is prompted to change it when auth is enabled.
        container.appScope.launch {
            try {
                val settings = container.settingsRepository.snapshot()
                container.authManager.authRequired = settings.authRequired
                container.authManager.sessions.setTimeoutMin(settings.sessionTimeoutMin)
                // Ensure default creds exist (admin/admin). Password is hashed.
                val has = container.authCredentialsRepository.hasCredentials()
                if (!has) {
                    container.authManager.ensureDefaultCredentials(
                        settings.username.ifBlank { "admin" },
                        "admin"
                    )
                }
            } catch (_: Exception) {
                // Startup must never crash app.
            }
        }
    }

    private fun createNotificationChannel() {
        val mgr = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            "Pocket Server",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows Pocket Server running status"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        mgr.createNotificationChannel(channel)
    }
}

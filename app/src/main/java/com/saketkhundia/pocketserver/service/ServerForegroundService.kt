package com.saketkhundia.pocketserver.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.saketkhundia.pocketserver.MainActivity
import com.saketkhundia.pocketserver.PocketServerApp
import com.saketkhundia.pocketserver.R
import com.saketkhundia.pocketserver.network.LocalIpProvider
import com.saketkhundia.pocketserver.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerForegroundService : Service() {

    companion object {
        const val ACTION_START = "com.saketkhundia.pocketserver.START"
        const val ACTION_STOP = "com.saketkhundia.pocketserver.STOP"
        const val EXTRA_PORT = "port"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var stateJob: kotlinx.coroutines.Job? = null

    override fun onCreate() {
        super.onCreate()
        // Observe server state to keep notification in sync with IP changes (e.g., Wi-Fi reconnect)
        val app = application as PocketServerApp
        stateJob = scope.launch {
            app.container.serverStateRepository.state.collect { st ->
                if (st.status == com.saketkhundia.pocketserver.domain.model.ServerStatus.RUNNING && st.url != null) {
                    updateNotification("Server running", st.url.removePrefix("http://"))
                } else if (st.status == com.saketkhundia.pocketserver.domain.model.ServerStatus.ERROR && st.error != null) {
                    updateNotification("Failed to start", st.error)
                }
            }
        }
        // Also refresh IP when network changes while running. refreshIpOnNetworkChange()
        // enumerates interfaces (blocking) — never on Main, even though this
        // service lives on the main thread. NotificationManager is thread-safe.
        scope.launch {
            val app2 = application as PocketServerApp
            app2.container.networkManager.networkChanges.collect {
                withContext(Dispatchers.IO) {
                    try { app2.container.httpServerManager.refreshIpOnNetworkChange() } catch (_: Exception) {}
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                scope.launch { stopServerAndSelf() }
                return START_NOT_STICKY
            }
            else -> {
                // Default start
                val notification = buildNotification("Starting...", "Pocket Server is starting")
                try {
                    if (Build.VERSION.SDK_INT >= 34) {
                        startForeground(Constants.NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                    } else {
                        startForeground(Constants.NOTIFICATION_ID, notification)
                    }
                } catch (e: SecurityException) {
                    // POST_NOTIFICATIONS denied on Android 13+ — foreground service still allowed, but try compat
                    try { ServiceCompat.startForeground(this, Constants.NOTIFICATION_ID, notification, if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0) } catch (_: Exception) {}
                } catch (_: Exception) {
                    try { startForeground(Constants.NOTIFICATION_ID, notification) } catch (_: Exception) {}
                }
                // Server start does interface enumeration + socket binds (blocking) —
                // off Main even though this service lives on the main thread.
                scope.launch(Dispatchers.IO) {
                    startServers()
                }
                return START_STICKY
            }
        }
    }

    private suspend fun startServers() {
        val app = application as PocketServerApp
        val http = app.container.httpServerManager
        val ftp = app.container.ftpServerManager

        val result = http.start()
        if (result.isSuccess) {
            val state = result.getOrNull()
            val url = state?.url ?: "http://${LocalIpProvider.getLocalIpv4(this@ServerForegroundService) ?: "0.0.0.0"}:${state?.port ?: 8080}"
            val ipPort = url.removePrefix("http://")
            updateNotification("Server running", ipPort)

            // Start FTP if enabled (non-blocking, don't fail HTTP if FTP fails)
            try {
                val settings = app.container.settingsRepository.snapshot()
                if (settings.ftpEnabled) {
                    ftp.start()
                }
            } catch (_: Exception) {
                // FTP failure should not stop HTTP
            }
        } else {
            val err = result.exceptionOrNull()?.message ?: "Failed to start"
            updateNotification("Failed to start", err)
            // Keep service alive briefly to show error, then stop?
            // Don't auto-stop; let user see error in app.
            // Update state already set to ERROR by manager.
        }
    }

    private suspend fun stopServerAndSelf() {
        val app = application as? PocketServerApp ?: run { stopSelf(); return }
        try { app.container.httpServerManager.stop() } catch (_: Exception) {}
        try { app.container.ftpServerManager.stop() } catch (_: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(title: String, text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, ServerForegroundService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_server)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setContentIntent(openPending)
            .addAction(R.drawable.ic_server, "STOP", stopPending)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(Constants.NOTIFICATION_ID, buildNotification(title, text))
    }

    override fun onDestroy() {
        stateJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

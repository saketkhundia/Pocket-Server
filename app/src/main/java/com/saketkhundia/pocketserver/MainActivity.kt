package com.saketkhundia.pocketserver

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.saketkhundia.pocketserver.presentation.navigation.AppNavGraph
import com.saketkhundia.pocketserver.presentation.navigation.Screen
import com.saketkhundia.pocketserver.presentation.theme.PocketServerTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            val has = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!has) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val app = application as PocketServerApp
        // Auto-start if configured
        lifecycleScope.launch {
            val settings = app.container.settingsRepository.snapshot()
            if (settings.autoStart) {
                // Start server via service; UI will reflect via StateFlow
                val intent = android.content.Intent(this@MainActivity, com.saketkhundia.pocketserver.service.ServerForegroundService::class.java).apply {
                    action = com.saketkhundia.pocketserver.service.ServerForegroundService.ACTION_START
                }
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
            }
        }

        setContent {
            PocketServerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val appContainer = (application as PocketServerApp).container
                    // Null until DataStore emits: while loading, hold the system
                    // splash frame (black = splash_background) instead of booting
                    // NavHost on a guessed destination. The old
                    // initial=AppSettings() defaulted onboarded=false, so every
                    // returning user saw Onboarding flash for ~a second before
                    // a corrective navigation jumped to Home.
                    val settings by appContainer.settingsRepository.settings.collectAsState(
                        initial = null
                    )
                    val loaded = settings
                    if (loaded == null) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        )
                    } else {
                        val startRoute =
                            if (!loaded.onboarded) Screen.Onboarding.route else Screen.Home.route
                        // If onboarding completed during session, navigate away
                        LaunchedEffect(loaded.onboarded) {
                            if (loaded.onboarded && navController.currentDestination?.route == Screen.Onboarding.route) {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            }
                        }
                        AppNavGraph(navController = navController, startRoute = startRoute)
                    }
                }
            }
        }
    }
}

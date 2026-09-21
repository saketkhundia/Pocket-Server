package com.saketkhundia.pocketserver.di

import android.content.Context
import androidx.room.Room
import com.saketkhundia.pocketserver.data.database.AppDatabase
import com.saketkhundia.pocketserver.data.preferences.DataStoreAuthCredentials
import com.saketkhundia.pocketserver.data.preferences.DataStoreSettingsRepository
import com.saketkhundia.pocketserver.data.preferences.DataStoreSharedFolders
import com.saketkhundia.pocketserver.data.repository.AppServerStateRepository
import com.saketkhundia.pocketserver.domain.monetization.FeatureAccessManager
import com.saketkhundia.pocketserver.domain.monetization.FreeFeatureAccessManager
import com.saketkhundia.pocketserver.domain.monetization.ProFeatureAccessManager
import com.saketkhundia.pocketserver.network.NetworkManager
import com.saketkhundia.pocketserver.server.auth.AuthManager
import com.saketkhundia.pocketserver.server.ftp.FtpServerManager
import com.saketkhundia.pocketserver.server.mdns.MdnsManager
import com.saketkhundia.pocketserver.server.HttpServerManager
import com.saketkhundia.pocketserver.storage.SharedFolderManager
import com.saketkhundia.pocketserver.storage.StorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(private val ctx: Context) {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Database (Room — only for logs metadata). No sensitive data stored.
    private val db: AppDatabase by lazy {
        Room.databaseBuilder(ctx.applicationContext, AppDatabase::class.java, "pocket_server.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val settingsRepository by lazy { DataStoreSettingsRepository(ctx) }
    val authCredentialsRepository by lazy { DataStoreAuthCredentials(ctx) }
    val sharedFolderRepository by lazy { DataStoreSharedFolders(ctx) }

    val serverStateRepository by lazy {
        AppServerStateRepository(db.logs(), appScope)
    }

    val storageManager by lazy { StorageManager(ctx.applicationContext) }
    val sharedFolderManager by lazy { SharedFolderManager(sharedFolderRepository, storageManager) }
    val authManager by lazy { AuthManager(authCredentialsRepository) }

    // Monetization gate — swap to ProFeatureAccessManager for paid build.
    val featureAccess: FeatureAccessManager by lazy {
        // Check a flag in settings? For now free tier; pro unlock via settings.
        // We read a persisted flag elsewhere; default Free.
        FreeFeatureAccessManager()
    }

    fun proFeatureAccess(): FeatureAccessManager = ProFeatureAccessManager()

    val networkManager by lazy { NetworkManager(ctx.applicationContext) }

    val mdnsManager by lazy { MdnsManager(ctx.applicationContext, appScope) }

    val httpServerManager by lazy {
        HttpServerManager(
            context = ctx.applicationContext,
            auth = authManager,
            folders = sharedFolderManager,
            storage = storageManager,
            settingsRepo = settingsRepository,
            sharedFolderRepo = sharedFolderRepository,
            stateRepo = serverStateRepository,
            mdns = mdnsManager,
            appScope = appScope
        )
    }

    val ftpServerManager by lazy {
        FtpServerManager(
            context = ctx.applicationContext,
            auth = authManager,
            folders = sharedFolderManager,
            storage = storageManager,
            settingsRepo = settingsRepository,
            stateRepo = serverStateRepository,
            scope = appScope
        )
    }
}

package com.saketkhundia.pocketserver.domain.monetization

/** Feature gate so monetization can be added without rewriting the server. */
enum class Feature { HTTP_FILES, UNLIMITED_FOLDERS, FTP, WEB_HOSTING, MEDIA, ADVANCED_AUTH, ADVANCED_LOGS, PROFILES, CUSTOM_SETTINGS }

interface FeatureAccessManager {
    fun isAllowed(feature: Feature): Boolean
    fun maxSharedFolders(): Int
}

class FreeFeatureAccessManager : FeatureAccessManager {
    override fun isAllowed(feature: Feature): Boolean = when (feature) {
        Feature.HTTP_FILES -> true
        Feature.MEDIA -> true
        else -> false
    }
    override fun maxSharedFolders(): Int = 3
}

class ProFeatureAccessManager : FeatureAccessManager {
    override fun isAllowed(feature: Feature) = true
    override fun maxSharedFolders() = Int.MAX_VALUE
}

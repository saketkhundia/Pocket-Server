package com.saketkhundia.pocketserver.util

import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Device storage snapshot for the dashboard Storage card. Read-only, no backend impact. */
data class StorageStats(
    val totalBytes: Long,
    val freeBytes: Long
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0)
    val usedFraction: Float get() = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
}

object StorageStatsProvider {
    suspend fun snapshot(): StorageStats? = withContext(Dispatchers.IO) {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            StorageStats(
                totalBytes = stat.blockCountLong * blockSize,
                freeBytes = stat.availableBlocksLong * blockSize
            )
        } catch (_: Exception) {
            null
        }
    }
}

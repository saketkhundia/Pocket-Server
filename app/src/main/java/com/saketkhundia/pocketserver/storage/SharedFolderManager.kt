package com.saketkhundia.pocketserver.storage

import com.saketkhundia.pocketserver.domain.model.FileEntry
import com.saketkhundia.pocketserver.domain.model.SharedFolder
import com.saketkhundia.pocketserver.server.security.PathSecurity
import kotlinx.coroutines.flow.first

/**
 * Maps virtual paths ("Downloads/a.pdf") to SAF documents.
 * The split is always: first segment = shared folder display name.
 */
class SharedFolderManager(
    private val folders: com.saketkhundia.pocketserver.domain.repository.SharedFolderRepository,
    private val storage: StorageManager
) {
    data class Target(val folder: SharedFolder, val relPath: String)

    suspend fun listRoots(): List<FileEntry> {
        return folders.snapshot().map {
            FileEntry(it.name, it.name, true, 0, 0, null)
        }.sortedBy { it.name.lowercase() }
    }

    /** Returns null when path is invalid or folder not configured (no escape possible). */
    suspend fun parse(virtualPath: String): Target? {
        val norm = PathSecurity.normalizeVirtualPath(virtualPath) ?: return null
        if (norm.isEmpty()) return null
        val (root, rel) = PathSecurity.splitRoot(norm)
        val folder = folders.snapshot().find { it.name == root } ?: return null
        return Target(folder, rel)
    }

    suspend fun resolveDoc(virtualPath: String): StorageManager.Resolved? {
        val norm = PathSecurity.normalizeVirtualPath(virtualPath) ?: return null
        if (norm.isEmpty()) return null
        val t = parse(norm) ?: return null
        return storage.resolve(t.folder.uri, t.folder.name, t.relPath)
    }

    suspend fun listVirtual(virtualPath: String): List<FileEntry>? {
        val norm = PathSecurity.normalizeVirtualPath(virtualPath) ?: return null
        if (norm.isEmpty()) return listRoots()
        val t = parse(norm) ?: return null
        val r = storage.resolve(t.folder.uri, t.folder.name, t.relPath) ?: return null
        if (!r.doc.isDirectory) return null
        // Children paths are prefixed with the folder name
        val prefix = if (t.relPath.isEmpty()) t.folder.name else "${t.folder.name}/${t.relPath}"
        return storage.listChildren(r.doc, prefix)
    }
}

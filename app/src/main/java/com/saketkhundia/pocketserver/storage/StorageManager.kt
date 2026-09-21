package com.saketkhundia.pocketserver.storage

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.saketkhundia.pocketserver.domain.model.FileEntry
import com.saketkhundia.pocketserver.util.MimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

/**
 * SAF-backed storage. The server NEVER sees raw filesystem paths —
 * it only works with virtual paths resolved through configured tree URIs.
 */
class StorageManager(private val ctx: Context) {

    data class Resolved(val doc: DocumentFile, val folderName: String)

    fun rootDoc(treeUri: String): DocumentFile? {
        return try {
            val uri = Uri.parse(treeUri)
            // Persisted permission must already be held; DocumentFile handles tree
            DocumentFile.fromTreeUri(ctx, uri)
        } catch (_: Exception) { null }
    }

    /** Resolve "FolderName/a/b" -> DocumentFile, or null if missing/escaped. */
    suspend fun resolve(folderUri: String, folderName: String, relPath: String): Resolved? =
        withContext(Dispatchers.IO) {
            val root = rootDoc(folderUri) ?: return@withContext null
            if (relPath.isBlank()) return@withContext Resolved(root, folderName)
            var cur: DocumentFile = root
            for (seg in relPath.split('/')) {
                if (seg.isEmpty() || seg == "." || seg == "..") return@withContext null
                cur = cur.listFiles().find { it.name == seg } ?: return@withContext null
            }
            Resolved(cur, folderName)
        }

    suspend fun listChildren(doc: DocumentFile, virtualPrefix: String): List<FileEntry> =
        withContext(Dispatchers.IO) {
            val files = try { doc.listFiles().toList() } catch (_: Exception) { emptyList() }
            files.mapNotNull { d ->
                val name = d.name ?: return@mapNotNull null
                FileEntry(
                    name = name,
                    path = if (virtualPrefix.isEmpty()) name else "$virtualPrefix/$name",
                    isDir = d.isDirectory,
                    size = if (d.isDirectory) 0 else d.length(),
                    lastModified = d.lastModified(),
                    mime = if (d.isDirectory) null else MimeUtils.fromName(name)
                )
            }.sortedWith(compareBy({ !it.isDir }, { it.name.lowercase() }))
        }

    fun openInput(doc: DocumentFile): InputStream? = try {
        ctx.contentResolver.openInputStream(doc.uri)
    } catch (_: Exception) { null }

    fun openOutput(parent: DocumentFile, name: String, mime: String): OutputStream? = try {
        val existing = parent.listFiles().find { it.name == name }
        existing?.delete()
        val created = parent.createFile(mime, name) ?: return null
        ctx.contentResolver.openOutputStream(created.uri, "w")
    } catch (_: Exception) { null }

    suspend fun createFolder(parent: DocumentFile, name: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (parent.listFiles().any { it.name == name }) return@withContext false
            parent.createDirectory(name) != null
        } catch (_: Exception) { false }
    }

    suspend fun rename(doc: DocumentFile, newName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // DocumentsContract.renameDocument works for tree docs
            DocumentsContract.renameDocument(ctx.contentResolver, doc.uri, newName) != null
        } catch (_: Exception) {
            try { doc.renameTo(newName) } catch (_: Exception) { false }
        }
    }

    suspend fun delete(doc: DocumentFile): Boolean = withContext(Dispatchers.IO) {
        try { doc.delete() } catch (_: Exception) { false }
    }

    suspend fun writeStream(parent: DocumentFile, name: String, mime: String, input: InputStream): Long =
        withContext(Dispatchers.IO) {
            val existing = parent.listFiles().find { it.name == name }
            existing?.delete()
            val created = parent.createFile(mime, name) ?: throw FileNotFoundException("Cannot create $name")
            ctx.contentResolver.openOutputStream(created.uri, "w")!!.use { out ->
                input.use { `in` ->
                    val buf = ByteArray(64 * 1024)
                    var total = 0L
                    while (true) {
                        val n = `in`.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        total += n
                    }
                    out.flush()
                    total
                }
            }
        }

    /**
     * Streaming write with hard byte cap. Returns bytes written. Throws if cap exceeded.
     * Never loads entire file into RAM.
     */
    suspend fun writeStreamCapped(
        parent: DocumentFile,
        name: String,
        mime: String,
        input: InputStream,
        cap: Long
    ): Long = withContext(Dispatchers.IO) {
        val existing = parent.listFiles().find { it.name == name }
        existing?.delete()
        val created = parent.createFile(mime, name) ?: throw FileNotFoundException("Cannot create $name")
        ctx.contentResolver.openOutputStream(created.uri, "w")!!.use { out ->
            val buf = ByteArray(64 * 1024)
            var total = 0L
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > cap) {
                    try { ctx.contentResolver.delete(created.uri, null, null) } catch (_: Exception) {}
                    try { created.delete() } catch (_: Exception) {}
                    throw IllegalStateException("Upload exceeds limit (${cap} bytes)")
                }
                out.write(buf, 0, n)
            }
            out.flush()
            total
        }
    }

    fun openOutputStreamForDoc(doc: DocumentFile, mode: String = "w"): OutputStream? =
        try { ctx.contentResolver.openOutputStream(doc.uri, mode) } catch (_: Exception) { null }
}

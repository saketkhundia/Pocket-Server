package com.saketkhundia.pocketserver.server.ftp

import android.content.Context
import com.saketkhundia.pocketserver.data.repository.AppServerStateRepository
import com.saketkhundia.pocketserver.domain.model.LogEntry
import com.saketkhundia.pocketserver.domain.repository.SettingsRepository
import com.saketkhundia.pocketserver.network.LocalIpProvider
import com.saketkhundia.pocketserver.server.auth.AuthManager
import com.saketkhundia.pocketserver.server.security.PathSecurity
import com.saketkhundia.pocketserver.storage.SharedFolderManager
import com.saketkhundia.pocketserver.storage.StorageManager
import com.saketkhundia.pocketserver.util.Constants
import com.saketkhundia.pocketserver.util.MimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lightweight FTP server as optional plugin.
 * Loosely coupled from HTTP server; can be enabled/disabled independently.
 */
class FtpServerManager(
    private val context: Context,
    private val auth: AuthManager,
    private val folders: SharedFolderManager,
    private val storage: StorageManager,
    private val settingsRepo: SettingsRepository,
    private val stateRepo: AppServerStateRepository,
    private val scope: CoroutineScope
) {
    private var serverSocket: ServerSocket? = null
    private var acceptJob: Job? = null
    private val mutex = Mutex()
    private val running = AtomicBoolean(false)
    private val activeConnections = AtomicInteger(0)

    suspend fun start(): Result<Unit> = mutex.withLock {
        if (running.get()) return Result.success(Unit)
        val settings = settingsRepo.snapshot()
        if (!settings.ftpEnabled) return Result.failure(IllegalStateException("FTP disabled in settings"))
        val port = settings.ftpPort
        if (port < Constants.MIN_PORT) return Result.failure(IllegalArgumentException("FTP port must be ≥ ${Constants.MIN_PORT}"))
        val ip = LocalIpProvider.getLocalIpv4(context)
        if (ip == null) return Result.failure(IllegalStateException("No local network for FTP: ${LocalIpProvider.troubleshootingHint(context)}"))

        try {
            val ss = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))
            serverSocket = ss
            running.set(true)
            acceptJob = scope.launch(Dispatchers.IO) {
                while (isActive && running.get()) {
                    try {
                        val client = ss.accept()
                        activeConnections.incrementAndGet()
                        launch(Dispatchers.IO) {
                            try { handleClient(client) } catch (_: Exception) {} finally {
                                activeConnections.decrementAndGet()
                                try { client.close() } catch (_: Exception) {}
                            }
                        }
                    } catch (e: Exception) {
                        if (!running.get()) break
                    }
                }
            }
            scope.launch {
                stateRepo.addLog(LogEntry(clientIp = "system", method = "FTP_START", path = "ftp://$ip:$port", status = 200, message = "FTP server started"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val msg = if (e.message?.contains("Address already in use") == true) "FTP port $port already in use" else e.message ?: "FTP start failed"
            Result.failure(IllegalStateException(msg, e))
        }
    }

    suspend fun stop(): Result<Unit> = mutex.withLock {
        try {
            running.set(false)
            acceptJob?.cancel()
            acceptJob = null
            serverSocket?.close()
            serverSocket = null
            scope.launch { stateRepo.addLog(LogEntry(clientIp = "system", method = "FTP_STOP", path = "ftp", status = 200, message = "FTP stopped")) }
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    fun isRunning(): Boolean = running.get()

    private suspend fun handleClient(socket: Socket) {
        val remoteIp = socket.inetAddress?.hostAddress ?: "ftp-client"
        socket.soTimeout = 60_000
        val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
        val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))

        fun reply(code: Int, msg: String) {
            try { writer.write("$code $msg\r\n"); writer.flush() } catch (_: Exception) {}
        }

        var authenticated = false
        var pendingUser: String? = null
        var renameFrom: String? = null
        var currentDir = "/"
        var pasvServer: ServerSocket? = null
        var activeHost: String? = null
        var activePort: Int? = null

        reply(220, "Pocket Server FTP ready")

        suspend fun listVirtual(vpath: String): List<com.saketkhundia.pocketserver.domain.model.FileEntry>? {
            val norm = PathSecurity.normalizeVirtualPath(vpath.removePrefix("/")) ?: return null
            return folders.listVirtual(norm)
        }

        // One formatter per connection (used sequentially below — SimpleDateFormat
        // is not thread-safe, but this instance never escapes this connection).
        // Was previously allocated per directory entry (10k-file LIST = 10k instances).
        val listDateFmt = SimpleDateFormat("MMM dd HH:mm", Locale.US)
        fun formatFtpListEntry(e: com.saketkhundia.pocketserver.domain.model.FileEntry): String {
            val date = listDateFmt.format(Date(e.lastModified.takeIf { it > 0 } ?: System.currentTimeMillis()))
            val type = if (e.isDir) "d" else "-"
            val size = if (e.isDir) "4096" else e.size.toString()
            return "$type${"rwxr-xr-x"} 1 owner group $size $date ${e.name}"
        }

        suspend fun openDataConnection(): Socket? {
            return when {
                pasvServer != null -> {
                    val ss = pasvServer
                    if (ss == null || ss.isClosed) return null
                    ss.soTimeout = 20_000
                    try { ss.accept().also { pasvServer = null; try { ss.close() } catch (_: Exception) {} } } catch (_: Exception) { null }
                }
                activeHost != null && activePort != null -> {
                    try { Socket(activeHost, activePort!!).apply { soTimeout = 30_000 } } catch (_: Exception) { null }
                }
                else -> null
            }
        }

        suspend fun awaitResolveDir(vpath: String): Boolean {
            val norm = PathSecurity.normalizeVirtualPath(vpath.removePrefix("/")) ?: return false
            if (norm.isEmpty()) return true
            val resolved = folders.resolveDoc(norm) ?: return false
            return resolved.doc.isDirectory
        }

        while (true) {
            val line = try { reader.readLine() } catch (_: Exception) { break } ?: break
            if (line.isBlank()) continue
            val spaceIdx = line.indexOf(' ')
            val cmd = (if (spaceIdx >= 0) line.substring(0, spaceIdx) else line).trim().uppercase(Locale.US)
            val arg = if (spaceIdx >= 0) line.substring(spaceIdx + 1).trim() else ""
            val safeLine = if (cmd == "PASS") "PASS ***" else line.take(256)
            scope.launch { stateRepo.addLog(LogEntry(clientIp = remoteIp, method = "FTP_$cmd", path = safeLine, status = 200, message = "")) }

            when (cmd) {
                "USER" -> {
                    pendingUser = arg.take(64)
                    reply(331, "User name okay, need password")
                }
                "PASS" -> {
                    val user = pendingUser ?: ""
                    val pass = arg.take(256)
                    val settings = try { settingsRepo.snapshot() } catch (_: Exception) { null }
                    // No reflection, no runBlocking: plain suspend credential reads.
                    // Outcome matrix identical to before: settings ftpUser
                    // override, else stored user (fails closed when unset), else
                    // "admin" only when the credential read itself throws.
                    val storedUser = try { auth.storedUsername() } catch (_: Exception) { settings?.ftpUser ?: "admin" }
                    val ftpUser: String? = settings?.ftpUser?.takeIf { it.isNotBlank() } ?: storedUser
                    val ok = user == ftpUser && try {
                        auth.verifyPassword(pass)
                    } catch (_: Exception) { false }
                    if (ok) { authenticated = true; reply(230, "User logged in") }
                    else { kotlinx.coroutines.delay(500); reply(530, "Not logged in") }
                }
                "QUIT" -> { reply(221, "Goodbye"); break }
                "SYST" -> reply(215, "UNIX Type: L8")
                "FEAT" -> {
                    writer.write("211-Features:\r\n"); writer.write(" PASV\r\n"); writer.write(" UTF8\r\n"); writer.write(" SIZE\r\n"); writer.write(" MDTM\r\n"); writer.write("211 End\r\n"); writer.flush()
                }
                "OPTS" -> reply(200, "OK")
                "PWD" -> {
                    if (!authenticated) { reply(530, "Not logged in"); continue }
                    reply(257, "\"$currentDir\" is current directory")
                }
                "CWD" -> {
                    if (!authenticated) { reply(530, "Not logged in"); continue }
                    val target = if (arg.startsWith("/")) arg else if (currentDir == "/") "/$arg" else "$currentDir/$arg"
                    val norm = PathSecurity.normalizeVirtualPath(target.removePrefix("/"))
                    if (norm == null) { reply(550, "Invalid path"); continue }
                    val vpath = if (norm.isEmpty()) "/" else "/$norm"
                    if (vpath == "/" || awaitResolveDir(vpath)) { currentDir = vpath; reply(250, "Directory changed to $currentDir") } else reply(550, "No such directory")
                }
                "CDUP" -> {
                    if (!authenticated) { reply(530, "Not logged in"); continue }
                    currentDir = if (currentDir == "/") "/" else { val p = currentDir.substringBeforeLast('/', ""); if (p.isEmpty()) "/" else p }
                    reply(250, "Up to $currentDir")
                }
                "TYPE" -> reply(200, "Type set to ${arg.uppercase()}")
                "MODE" -> reply(200, "Mode S")
                "STRU" -> reply(200, "Structure F")
                "PASV" -> {
                    if (!authenticated) { reply(530, "Not logged in"); continue }
                    try { pasvServer?.close() } catch (_: Exception) {}
                    val ss = ServerSocket(0, 1, InetAddress.getByName("0.0.0.0"))
                    pasvServer = ss
                    val localIp = LocalIpProvider.getLocalIpv4(context) ?: "127.0.0.1"
                    val port = ss.localPort
                    val p1 = port / 256; val p2 = port % 256
                    val ipParts = localIp.split(".").joinToString(",")
                    reply(227, "Entering Passive Mode ($ipParts,$p1,$p2)")
                }
                "PORT" -> {
                    if (!authenticated) { reply(530, "Not logged in"); continue }
                    try {
                        val parts = arg.split(",")
                        if (parts.size == 6) { activeHost = parts.take(4).joinToString("."); activePort = parts[4].toInt() * 256 + parts[5].toInt(); reply(200, "PORT command successful") }
                        else reply(501, "Bad PORT")
                    } catch (_: Exception) { reply(501, "Bad PORT") }
                }
                "LIST", "NLST" -> {
                    if (!authenticated) { reply(530, "Not logged in"); continue }
                    val listArg = arg.takeIf { it.isNotBlank() } ?: currentDir
                    val target = if (listArg.startsWith("/")) listArg else if (currentDir == "/") "/$listArg" else "$currentDir/$listArg"
                    val norm = PathSecurity.normalizeVirtualPath(target.removePrefix("/"))
                    if (norm == null) { reply(550, "Invalid path"); continue }
                    val entries: List<com.saketkhundia.pocketserver.domain.model.FileEntry>? = if (norm.isEmpty()) listVirtual("/") else {
                        val resolved = folders.resolveDoc(norm)
                        if (resolved == null) { reply(550, "Not found"); continue }
                        if (!resolved.doc.isDirectory) {
                            val name = resolved.doc.name ?: norm.substringAfterLast('/')
                            val size = resolved.doc.length(); val lm = resolved.doc.lastModified()
                            listOf(com.saketkhundia.pocketserver.domain.model.FileEntry(name, norm, false, size, lm, MimeUtils.fromName(name)))
                        } else listVirtual(norm)
                    }
                    if (entries == null) { reply(550, "Failed"); continue }
                    val dataSock = openDataConnection()
                    if (dataSock == null) { reply(425, "Can't open data connection"); continue }
                    reply(150, "Opening data connection")
                    try {
                        withContext(Dispatchers.IO) {
                            val out = dataSock.getOutputStream().bufferedWriter(Charsets.UTF_8)
                            for (e in entries) { if (cmd == "NLST") out.write(e.name + "\r\n") else out.write(formatFtpListEntry(e) + "\r\n") }
                            out.flush(); dataSock.close()
                        }
                        reply(226, "Transfer complete")
                    } catch (e: Exception) { try { dataSock.close() } catch (_: Exception) {}; reply(426, "Transfer failed") }
                }
                "RETR" -> {
                    if (!authenticated) { reply(530, "Not logged in"); continue }
                    val target = if (arg.startsWith("/")) arg else if (currentDir == "/") "/$arg" else "$currentDir/$arg"
                    val norm = PathSecurity.normalizeVirtualPath(target.removePrefix("/"))
                    if (norm == null) { reply(550, "Invalid path"); continue }
                    val resolved = folders.resolveDoc(norm)
                    if (resolved == null) { reply(550, "Not found"); continue }
                    val doc = resolved.doc
                    if (doc.isDirectory) { reply(550, "Is directory"); continue }
                    val dataSock = openDataConnection()
                    if (dataSock == null) { reply(425, "No data connection"); continue }
                    reply(150, "Opening data connection for ${doc.name}")
                    try {
                        withContext(Dispatchers.IO) {
                            val input = storage.openInput(doc) ?: throw FileNotFoundException()
                            dataSock.getOutputStream().use { out -> input.use { ins -> val buf = ByteArray(64*1024); while (true) { val n = ins.read(buf); if (n<0) break; out.write(buf,0,n) }; out.flush() } }
                            dataSock.close()
                        }
                        reply(226, "Transfer complete")
                    } catch (e: Exception) { try { dataSock.close() } catch (_: Exception) {}; reply(551, "Transfer failed") }
                }
                "STOR" -> {
                    if (!authenticated) { reply(530, "Not logged in"); continue }
                    val target = if (arg.startsWith("/")) arg else if (currentDir == "/") "/$arg" else "$currentDir/$arg"
                    val norm = PathSecurity.normalizeVirtualPath(target.removePrefix("/"))
                    if (norm == null) { reply(550, "Invalid path"); continue }
                    val slash = norm.lastIndexOf('/')
                    val dirNorm = if (slash < 0) "" else norm.substring(0, slash)
                    val fileName = if (slash < 0) norm else norm.substring(slash+1)
                    if (PathSecurity.sanitizeFileName(fileName) is PathSecurity.Check.Rejected) { reply(553, "Bad filename"); continue }
                    if (dirNorm.isEmpty()) { reply(553, "Choose a shared folder"); continue }
                    val dirResolved = folders.resolveDoc(dirNorm)
                    if (dirResolved == null) { reply(550, "No such directory"); continue }
                    val dirDoc = dirResolved.doc
                    if (!dirDoc.isDirectory) { reply(550, "No such directory"); continue }
                    val dataSock = openDataConnection()
                    if (dataSock == null) { reply(425, "No data connection"); continue }
                    reply(150, "Opening data connection for upload")
                    try {
                        withContext(Dispatchers.IO) {
                            val input = dataSock.getInputStream()
                            var total = 0L
                            val existing = dirDoc.listFiles().find { it.name == fileName }; existing?.delete()
                            val mime = MimeUtils.fromName(fileName)
                            val created = dirDoc.createFile(mime, fileName) ?: throw FileNotFoundException()
                            context.contentResolver.openOutputStream(created.uri, "w")!!.use { out ->
                                val buf = ByteArray(64*1024)
                                while (true) { val n = input.read(buf); if (n<0) break; total+=n; if (total>Constants.MAX_UPLOAD_BYTES) throw IllegalStateException("Too large"); out.write(buf,0,n) }
                            }
                            dataSock.close()
                        }
                        reply(226, "Upload complete")
                    } catch (e: Exception) { try { dataSock.close() } catch (_: Exception) {}; reply(552, "Upload failed: ${e.message}") }
                }
                "DELE" -> {
                    if (!authenticated) { reply(530, "Not logged in"); continue }
                    val target = if (arg.startsWith("/")) arg else if (currentDir == "/") "/$arg" else "$currentDir/$arg"
                    val norm = PathSecurity.normalizeVirtualPath(target.removePrefix("/"))
                    if (norm == null) { reply(550, "Invalid path"); continue }
                    val resolved = folders.resolveDoc(norm)
                    if (resolved == null) { reply(550, "Not found"); continue }
                    val ok = storage.delete(resolved.doc)
                    if (ok) reply(250, "Deleted") else reply(550, "Delete failed")
                }
                "MKD" -> {
                    if (!authenticated) { reply(530, "Not logged in"); continue }
                    val target = if (arg.startsWith("/")) arg else if (currentDir == "/") "/$arg" else "$currentDir/$arg"
                    val norm = PathSecurity.normalizeVirtualPath(target.removePrefix("/"))
                    if (norm == null) { reply(550, "Invalid path"); continue }
                    val slash = norm.lastIndexOf('/'); val parentNorm = if (slash<0) "" else norm.substring(0, slash); val name = if (slash<0) norm else norm.substring(slash+1)
                    if (PathSecurity.sanitizeFileName(name) is PathSecurity.Check.Rejected) { reply(553, "Bad name"); continue }
                    if (parentNorm.isEmpty()) { reply(553, "Cannot create at root"); continue }
                    val parentResolved = folders.resolveDoc(parentNorm)
                    if (parentResolved == null) { reply(550, "Parent not found"); continue }
                    val ok = storage.createFolder(parentResolved.doc, name)
                    if (ok) reply(257, "\"/$norm\" created") else reply(550, "Create failed")
                }
                "RMD" -> {
                    if (!authenticated) { reply(530, "Not logged in"); continue }
                    val target = if (arg.startsWith("/")) arg else if (currentDir == "/") "/$arg" else "$currentDir/$arg"
                    val norm = PathSecurity.normalizeVirtualPath(target.removePrefix("/"))
                    if (norm == null) { reply(550, "Invalid path"); continue }
                    val resolved = folders.resolveDoc(norm)
                    if (resolved == null) { reply(550, "Not found"); continue }
                    if (!resolved.doc.isDirectory) { reply(550, "Not a directory"); continue }
                    val ok = resolved.doc.delete()
                    if (ok) reply(250, "Removed") else reply(550, "Remove failed (not empty?)")
                }
                "RNFR" -> {
                    if (!authenticated) { reply(530, "Not logged in"); continue }
                    val target = if (arg.startsWith("/")) arg else if (currentDir == "/") "/$arg" else "$currentDir/$arg"
                    val norm = PathSecurity.normalizeVirtualPath(target.removePrefix("/"))
                    if (norm == null) { reply(550, "Invalid path"); continue }
                    val resolved = folders.resolveDoc(norm)
                    if (resolved == null) { reply(550, "Not found"); continue }
                    renameFrom = norm; reply(350, "Need RNTO")
                }
                "RNTO" -> {
                    if (!authenticated) { reply(530, "Not logged in"); continue }
                    val from = renameFrom
                    if (from == null) { reply(503, "Need RNFR"); continue }
                    val target = if (arg.startsWith("/")) arg else if (currentDir == "/") "/$arg" else "$currentDir/$arg"
                    val toNorm = PathSecurity.normalizeVirtualPath(target.removePrefix("/"))
                    if (toNorm == null) { reply(550, "Invalid path"); continue }
                    val newName = toNorm.substringAfterLast('/')
                    if (PathSecurity.sanitizeFileName(newName) is PathSecurity.Check.Rejected) { reply(553, "Bad name"); continue }
                    val fromResolved = folders.resolveDoc(from)
                    if (fromResolved == null) { reply(550, "Not found"); continue }
                    val ok = storage.rename(fromResolved.doc, newName)
                    renameFrom = null
                    if (ok) reply(250, "Renamed") else reply(550, "Rename failed")
                }
                "SIZE" -> {
                    if (!authenticated) { reply(530, "Not logged in"); continue }
                    val target = if (arg.startsWith("/")) arg else if (currentDir == "/") "/$arg" else "$currentDir/$arg"
                    val norm = PathSecurity.normalizeVirtualPath(target.removePrefix("/"))
                    if (norm == null) { reply(550, "Invalid path"); continue }
                    val resolved = folders.resolveDoc(norm)
                    if (resolved == null) { reply(550, "Not found"); continue }
                    if (resolved.doc.isDirectory) { reply(550, "Is directory"); continue }
                    reply(213, "${resolved.doc.length()}")
                }
                "MDTM" -> {
                    if (!authenticated) { reply(530, "Not logged in"); continue }
                    val target = if (arg.startsWith("/")) arg else if (currentDir == "/") "/$arg" else "$currentDir/$arg"
                    val norm = PathSecurity.normalizeVirtualPath(target.removePrefix("/"))
                    if (norm == null) { reply(550, "Invalid path"); continue }
                    val resolved = folders.resolveDoc(norm)
                    if (resolved == null) { reply(550, "Not found"); continue }
                    val lm = resolved.doc.lastModified()
                    val fmt = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date(lm))
                    reply(213, fmt)
                }
                "NOOP" -> reply(200, "OK")
                "REST" -> reply(350, "Restart not supported")
                "ABOR" -> reply(226, "Aborted")
                else -> reply(502, "Command not implemented")
            }
        }
        try { pasvServer?.close() } catch (_: Exception) {}
    }

    private sealed interface DataMode { data object PasvPending : DataMode }
}

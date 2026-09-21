package com.saketkhundia.pocketserver.server

import android.content.Context
import com.saketkhundia.pocketserver.data.repository.AppServerStateRepository
import com.saketkhundia.pocketserver.domain.model.LogEntry
import com.saketkhundia.pocketserver.domain.model.MdnsStatus
import com.saketkhundia.pocketserver.domain.model.ServerState
import com.saketkhundia.pocketserver.domain.model.ServerStatus
import com.saketkhundia.pocketserver.domain.repository.SharedFolderRepository
import com.saketkhundia.pocketserver.domain.repository.SettingsRepository
import com.saketkhundia.pocketserver.network.LocalIpProvider
import com.saketkhundia.pocketserver.server.auth.AuthManager
import com.saketkhundia.pocketserver.server.mdns.MDNS_BASE_HOST
import com.saketkhundia.pocketserver.server.mdns.MdnsManager
import com.saketkhundia.pocketserver.server.mdns.mdnsHostnameUrl
import com.saketkhundia.pocketserver.server.media.RangeSupport
import com.saketkhundia.pocketserver.server.routing.RouteDeps
import com.saketkhundia.pocketserver.server.routing.WebUiAssets
import com.saketkhundia.pocketserver.server.routing.pocketApi
import com.saketkhundia.pocketserver.server.security.PathSecurity
import com.saketkhundia.pocketserver.storage.SharedFolderManager
import com.saketkhundia.pocketserver.storage.StorageManager
import com.saketkhundia.pocketserver.util.Constants
import com.saketkhundia.pocketserver.util.MimeUtils
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.net.BindException
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/** Pre-compiled: onRequest sanitizes every request path; compiling per request wasted CPU per hit. */
private val SESSION_COOKIE_REGEX = Regex("ps_session=[^;]+")

class HttpServerManager(
    private val context: Context,
    private val auth: AuthManager,
    private val folders: SharedFolderManager,
    private val storage: StorageManager,
    private val settingsRepo: SettingsRepository,
    private val sharedFolderRepo: SharedFolderRepository,
    private val stateRepo: AppServerStateRepository,
    private val mdns: MdnsManager,
    private val appScope: CoroutineScope
) {
    private var engine: EmbeddedServer<*, *>? = null
    private val mutex = Mutex()
    private val seenIps = ConcurrentHashMap.newKeySet<String>()
    private var startedAt: Long? = null

    // Live-cached settings/folders for per-request reads.
    // Previously each HTTP request ran runBlocking { DataStore.snapshot() },
    // blocking a Ktor event-loop thread on disk IO per request — during a
    // gallery load (hundreds of requests) this stalled the server and janked
    // the UI observers. Now collectors keep volatile copies fresh; request
    // handlers read plain fields with zero blocking.
    @Volatile private var cachedName = "Pocket Server"
    @Volatile private var cachedWebMode = false
    @Volatile private var cachedWebRoot: String? = null
    @Volatile private var cachedPort = 8080
    @Volatile private var cachedFolders: List<com.saketkhundia.pocketserver.domain.model.SharedFolder> = emptyList()

    init {
        appScope.launch {
            try { settingsRepo.settings.collect { s ->
                cachedName = s.serverName.ifBlank { "Pocket Server" }
                cachedWebMode = s.webServerMode
                cachedWebRoot = s.webRootFolderId
                cachedPort = s.httpPort
            } } catch (_: Exception) {}
        }
        appScope.launch {
            try { sharedFolderRepo.folders.collect { cachedFolders = it } } catch (_: Exception) {}
        }
    }

    val isRunning: Boolean get() = engine != null

    /** Non-blocking: uses cached port; IP enumeration is caller-thread work. */
    suspend fun currentUrl(): String? = withContext(Dispatchers.IO) {
        val ip = LocalIpProvider.getLocalIpv4(context) ?: return@withContext null
        "http://$ip:$cachedPort"
    }

    /** Returns all candidates for diagnostics (useful when primary IP fails). */
    fun currentCandidates(): List<String> = LocalIpProvider.getAllCandidates(context)

    /** Called when network changes while running — updates displayed URL without restart (bind is 0.0.0.0). */
    suspend fun refreshIpOnNetworkChange() {
        if (engine == null) return
        val newIp = LocalIpProvider.getLocalIpv4(context) ?: return
        val prev = stateRepo.state.first()
        if (prev.localIp != newIp) {
            val url = "http://$newIp:${prev.port}"
            stateRepo.setState(prev.copy(localIp = newIp, url = url))
            stateRepo.addLog(LogEntry(clientIp = "system", method = "NETWORK", path = url, status = 200, message = "IP updated after network change: $newIp"))
            // JmDNS binds the old address — re-advertise on the new network
            // so no stale record lingers. Fire-and-forget; IP works meanwhile.
            val port = prev.port
            appScope.launch {
                try {
                    val cur = stateRepo.state.first()
                    if (cur.status != ServerStatus.RUNNING) return@launch
                    stateRepo.setState(cur.copy(mdnsStatus = MdnsStatus.REGISTERING, hostnameUrl = null, pendingHostnameUrl = mdnsHostnameUrl(MDNS_BASE_HOST, port)))
                    val verified = try { mdns.restart(port, cur.localIp) } catch (_: Exception) { null }
                    val latest = stateRepo.state.first()
                    if (latest.status != ServerStatus.RUNNING) return@launch
                    if (verified != null) {
                        stateRepo.setState(latest.copy(mdnsStatus = MdnsStatus.AVAILABLE, hostnameUrl = verified, pendingHostnameUrl = null))
                    } else {
                        stateRepo.setState(latest.copy(mdnsStatus = MdnsStatus.UNAVAILABLE, hostnameUrl = null, pendingHostnameUrl = null))
                    }
                } catch (_: Exception) { /* never break the server */ }
            }
        }
    }

    private fun isPortAvailable(port: Int): Boolean {
        return try { java.net.ServerSocket(port).use { true } } catch (_: Exception) { false }
    }

    suspend fun start(): Result<ServerState> = mutex.withLock {
        if (engine != null) {
            return Result.failure(IllegalStateException("Server already running"))
        }
        val settings = settingsRepo.snapshot()
        cachedName = settings.serverName.ifBlank { "Pocket Server" }
        cachedWebMode = settings.webServerMode
        cachedWebRoot = settings.webRootFolderId
        cachedPort = settings.httpPort
        val port = settings.httpPort
        val validation = com.saketkhundia.pocketserver.domain.usecase.ValidatePortUseCase.validate(port)
        if (validation is com.saketkhundia.pocketserver.domain.usecase.ValidatePortUseCase.Result.Invalid) {
            return Result.failure(IllegalArgumentException(validation.reason))
        }
        // Check network — allow start even without site-local if user forces? But guide user.
        val ip = LocalIpProvider.getLocalIpv4(context)
        if (ip == null) {
            val hint = LocalIpProvider.troubleshootingHint(context)
            return Result.failure(IllegalStateException("No local network. $hint"))
        }
        val networkDesc = LocalIpProvider.describeNetwork(context)
        // Pre-check port
        if (!isPortAvailable(port)) {
            val msg = "Port $port is already in use. Try another port."
            stateRepo.setState(ServerState(ServerStatus.ERROR, null, ip, port, msg, null))
            return Result.failure(IllegalStateException(msg))
        }

        // Configure auth
        auth.authRequired = settings.authRequired
        auth.sessions.setTimeoutMin(settings.sessionTimeoutMin)
        if (!authCredentialsReady()) {
            auth.ensureDefaultCredentials(settings.username.ifBlank { "admin" }, "admin")
        }

        stateRepo.setState(ServerState(ServerStatus.STARTING, null, ip, port, null, System.currentTimeMillis()))
        seenIps.clear()
        stateRepo.updateStats { it.copy(activeConnections = 0, connectedDevices = 0, requests = 0, bytesUploaded = 0, bytesDownloaded = 0) }

        // Build Ktor server
        val deps = RouteDeps(
            auth = auth,
            folders = folders,
            storage = storage,
            serverName = { cachedName },
            webMode = { cachedWebMode },
            webRootId = { cachedWebRoot },
            sharedSnapshot = { cachedFolders },
            onRequest = { method, path, status, clientIp, bytesDown, bytesUp ->
                // sanitize: never log tokens, passwords, or file contents
                val safePath = path.take(512).replace(SESSION_COOKIE_REGEX, "ps_session=***")
                // Rate limiting is handled inside AuthManager; here just stats
                if (clientIp.isNotBlank() && clientIp != "lan-client") seenIps.add(clientIp)
                appScope.launch {
                    stateRepo.updateStats {
                        it.copy(
                            requests = it.requests + 1,
                            bytesDownloaded = it.bytesDownloaded + bytesDown,
                            bytesUploaded = it.bytesUploaded + bytesUp,
                            connectedDevices = seenIps.size
                        )
                    }
                    val entry = LogEntry(
                        timestampMs = System.currentTimeMillis(),
                        clientIp = clientIp.take(64),
                        method = method.take(16),
                        path = safePath,
                        status = status,
                        message = ""
                    )
                    stateRepo.addLog(entry)
                }
            }
        )

        try {
            val eng = embeddedServer(CIO, host = "0.0.0.0", port = port) {
                configureServer(deps)
            }
            eng.start(wait = false)
            engine = eng
            startedAt = System.currentTimeMillis()
            // Re-resolve IP after bind (Wi-Fi may have refreshed)
            val resolvedIp = LocalIpProvider.getLocalIpv4(context) ?: ip
            val url = "http://$resolvedIp:$port"
            val state = ServerState(ServerStatus.RUNNING, url, resolvedIp, port, null, startedAt)
            stateRepo.setState(state)
            stateRepo.addLog(
                LogEntry(
                    clientIp = "system",
                    method = "START",
                    path = url,
                    status = 200,
                    message = "HTTP server started on $networkDesc. Candidates: ${currentCandidates().joinToString()} — try ping $resolvedIp"
                )
            )
            // Advertise pocketserver.local asynchronously — mDNS must never
            // delay or fail the start itself; the IP URL works regardless.
            advertiseHostname(port)
            Result.success(state)
        } catch (e: BindException) {
            val msg = "Port $port is already in use. Try another port."
            stateRepo.setState(ServerState(ServerStatus.ERROR, null, ip, port, msg, null))
            Result.failure(IllegalStateException(msg, e))
        } catch (e: Exception) {
            val msg = e.message ?: "Failed to start server"
            stateRepo.setState(ServerState(ServerStatus.ERROR, null, ip, port, msg, null))
            Result.failure(e)
        }
    }

    suspend fun stop(): Result<Unit> = mutex.withLock {
        try {
            // Hand the engine off under lock, then tear down OFF the caller's
            // thread: stop() is routinely invoked on Main (UI toggle, service
            // action) and both engine grace-stop and the mDNS goodbye block
            // for seconds — that was an ANR-grade freeze.
            val eng = engine
            engine = null
            withContext(Dispatchers.IO) {
                try { eng?.stop(300, 500) } catch (_: Exception) {}
            }
            // Fire-and-forget: the goodbye is still sent (process stays alive
            // via appScope), but stop() never waits on an in-flight ~20s
            // registration or a slow close. Restart ordering stays correct —
            // register() always cleans stale state first under its own mutex.
            mdns.unregisterAsync()
            // Clear sessions and reset stats
            auth.sessions.clear()
            seenIps.clear()
            val prev = stateRepo.state.first()
            stateRepo.setState(ServerState(ServerStatus.STOPPED, null, prev.localIp, prev.port, null, null))
            stateRepo.updateStats { it.copy(activeConnections = 0) }
            stateRepo.addLog(
                LogEntry(clientIp = "system", method = "STOP", path = "server", status = 200, message = "HTTP server stopped")
            )
            startedAt = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Best-effort local-name advertisement after a successful start.
     * Mirrors REGISTERING → AVAILABLE/UNAVAILABLE + verified URL into the
     * single ServerState so UI never has to guess or hand-build URLs.
     */
    private fun advertiseHostname(port: Int) {
        appScope.launch {
            try {
                val cur = stateRepo.state.first()
                if (cur.status != ServerStatus.RUNNING) return@launch
                // Publish the expected name INSTANTLY (with a checking marker
                // in UI) while verification runs in the background.
                val pending = mdnsHostnameUrl(MDNS_BASE_HOST, port)
                stateRepo.setState(cur.copy(mdnsStatus = MdnsStatus.REGISTERING, hostnameUrl = null, pendingHostnameUrl = pending))
                stateRepo.addLog(LogEntry(clientIp = "system", method = "MDNS", path = "register", status = 200, message = "Local name registration started"))
                // Bind the exact working IP so the A record can never point
                // somewhere the IP URL doesn't reach.
                val verified = try { mdns.register(port, cur.localIp) } catch (_: Exception) { null }
                val latest = stateRepo.state.first()
                if (latest.status != ServerStatus.RUNNING) return@launch
                if (verified != null) {
                    stateRepo.setState(latest.copy(mdnsStatus = MdnsStatus.AVAILABLE, hostnameUrl = verified, pendingHostnameUrl = null))
                    stateRepo.addLog(LogEntry(clientIp = "system", method = "MDNS", path = verified, status = 200, message = "Local name available"))
                } else {
                    stateRepo.setState(latest.copy(mdnsStatus = MdnsStatus.UNAVAILABLE, hostnameUrl = null, pendingHostnameUrl = null))
                    stateRepo.addLog(LogEntry(clientIp = "system", method = "MDNS", path = "unavailable", status = 200, message = "Local name unavailable on this network — IP fallback only"))
                }
            } catch (_: Exception) { /* mDNS must never break the server */ }
        }
    }

    suspend fun restart(): Result<ServerState> {
        stop()
        return start()
    }

    private suspend fun authCredentialsReady(): Boolean {
        return try { authCredentialsAvailable() } catch (_: Exception) { false }
    }

    private suspend fun authCredentialsAvailable(): Boolean {
        // Check via AuthManager's repo
        // Use reflection-free: call hasCredentials if available
        return try {
            val f = auth::class.java.getDeclaredField("creds")
            f.isAccessible = true
            val repo = f.get(auth) as com.saketkhundia.pocketserver.domain.repository.AuthCredentialRepository
            repo.hasCredentials()
        } catch (_: Exception) { true }
    }

    private fun Application.configureServer(deps: RouteDeps) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; isLenient = true })
        }
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowHeader(HttpHeaders.Range)
            allowHeader(HttpHeaders.Cookie)
            exposeHeader(HttpHeaders.ContentRange)
            exposeHeader(HttpHeaders.AcceptRanges)
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                val msg = if (cause is IllegalArgumentException) cause.message ?: "Bad request" else "Internal error"
                val code = if (cause is IllegalArgumentException) HttpStatusCode.BadRequest else HttpStatusCode.InternalServerError
                try {
                    call.respondText("""{"error":"${msg.replace("\"", "")}"}""", ContentType.Application.Json, code)
                } catch (_: Exception) {}
            }
        }
        routing {
            // Health endpoints (no auth) — for browser timeout diagnostics
            get("/ping") {
                call.respondText("pong", ContentType.Text.Plain)
            }
            get("/health") {
                val ip = LocalIpProvider.getLocalIpv4(context) ?: "unknown"
                call.respondText("""{"status":"ok","ip":"$ip"}""", ContentType.Application.Json)
            }
            get("/api/ping") {
                call.respondText("""{"pong":true}""", ContentType.Application.Json)
            }
            // API routes under /api/*
            route("/") {
                pocketApi(deps)
            }

            // Web UI + Web Server mode
            get("/") {
                // Check auth if required
                if (deps.auth.authRequired) {
                    val token = com.saketkhundia.pocketserver.server.auth.SessionManager.extractToken(
                        call.request.header(HttpHeaders.Cookie),
                        call.request.header(HttpHeaders.Authorization)
                    )
                    // Allow the status and login API without auth, but root page should show login via JS
                    // So we still serve indexHtml; JS will handle login. Don't block here.
                }
                val settings = runCatching { deps.sharedSnapshot() }.getOrNull()
                val webMode = deps.webMode()
                val webRootId = deps.webRootId()
                if (webMode && webRootId != null) {
                    val folder = settings?.find { it.id == webRootId } ?: deps.sharedSnapshot().find { it.id == webRootId }
                    if (folder != null) {
                        // Try to serve index.html from web root
                        val resolved = folders.resolveDoc("${folder.name}/index.html") ?: folders.resolveDoc(folder.name)
                        val doc = resolved?.doc
                        if (doc != null && doc.isDirectory) {
                            // directory without index — fallback to file browser
                            call.respondText(WebUiAssets.indexHtml(deps.serverName()), ContentType.Text.Html)
                        } else if (doc != null && !doc.isDirectory) {
                            // serve index.html with streaming
                            val mime = MimeUtils.fromName("index.html")
                            call.response.header(HttpHeaders.ContentType, mime)
                            call.respondOutputStream(ContentType.Text.Html, HttpStatusCode.OK) {
                                withContext(Dispatchers.IO) {
                                    val input = storage.openInput(doc) ?: throw FileNotFoundException()
                                    input.use { ins ->
                                        val buf = ByteArray(64 * 1024)
                                        while (true) {
                                            val n = ins.read(buf)
                                            if (n < 0) break
                                            this@respondOutputStream.write(buf, 0, n)
                                        }
                                    }
                                }
                            }
                        } else {
                            call.respondText(WebUiAssets.indexHtml(deps.serverName()), ContentType.Text.Html)
                        }
                    } else {
                        call.respondText(WebUiAssets.indexHtml(deps.serverName()), ContentType.Text.Html)
                    }
                } else {
                    call.respondText(WebUiAssets.indexHtml(deps.serverName()), ContentType.Text.Html)
                }
                // Log serving root
                deps.onRequest("GET", "/", 200, call.request.local.remoteHost, 0, 0)
            }

            // Static web hosting: serve any file under webRoot folder as static site when webMode is on.
            // This catches /style.css, /assets/* etc.
            get("/{path...}") {
                val raw = call.parameters["path"] ?: ""
                // If it's an API path already handled, don't re-handle (but Ktor routing prioritizes).
                if (raw.startsWith("api/")) {
                    call.respondText("""{"error":"not found"}""", ContentType.Application.Json, HttpStatusCode.NotFound)
                    return@get
                }
                val webMode = deps.webMode()
                val webRootId = deps.webRootId()
                if (webMode && webRootId != null) {
                    // Need auth check for static hosting as well if authRequired
                    if (deps.auth.authRequired) {
                        val token = com.saketkhundia.pocketserver.server.auth.SessionManager.extractToken(
                            call.request.header(HttpHeaders.Cookie),
                            call.request.header(HttpHeaders.Authorization)
                        )
                        if (deps.auth.sessions.validate(token) == null) {
                            // Serve login page instead of 401 for browser navigation — keep UX simple
                            call.respondText(WebUiAssets.indexHtml(deps.serverName()), ContentType.Text.Html)
                            return@get
                        }
                    }
                    val folder = deps.sharedSnapshot().find { it.id == webRootId }
                    if (folder != null) {
                        val sanitized = PathSecurity.normalizeVirtualPath(raw) ?: run {
                            call.respondText("Bad path", ContentType.Text.Plain, HttpStatusCode.BadRequest)
                            return@get
                        }
                        if (sanitized.isEmpty()) {
                            call.respondText(WebUiAssets.indexHtml(deps.serverName()), ContentType.Text.Html)
                            return@get
                        }
                        val virtual = "${folder.name}/$sanitized"
                        val resolved = folders.resolveDoc(virtual)
                        val doc = resolved?.doc
                        if (doc != null && !doc.isDirectory) {
                            val mime = MimeUtils.fromName(sanitized.substringAfterLast('/'))
                            val total = doc.length().takeIf { it >= 0 } ?: -1L
                            val rangeHeader = call.request.header(HttpHeaders.Range)
                            val range = if (total > 0) RangeSupport.parse(rangeHeader, total) else null
                            if (rangeHeader != null && total > 0 && range == null && rangeHeader.startsWith("bytes=")) {
                                call.response.header("Content-Range", "bytes */$total")
                                call.respondText("Range not satisfiable", ContentType.Text.Plain, HttpStatusCode.RequestedRangeNotSatisfiable)
                                return@get
                            }
                            val start = range?.start ?: 0L
                            val endEx = range?.endExclusive ?: total
                            val outLen = if (total >= 0 && endEx >= 0) (endEx - start).coerceAtLeast(0) else -1L
                            val status = if (range != null) HttpStatusCode.PartialContent else HttpStatusCode.OK
                            call.response.header(HttpHeaders.AcceptRanges, "bytes")
                            call.response.header(HttpHeaders.ContentType, mime)
                            if (outLen >= 0) call.response.header(HttpHeaders.ContentLength, outLen.toString())
                            if (range != null && total > 0) call.response.header(HttpHeaders.ContentRange, "bytes $start-${endEx - 1}/$total")
                            if (call.request.httpMethod.value == "HEAD") {
                                call.respondText("", ContentType.Text.Plain, status)
                                return@get
                            }
                            var sent = 0L
                            call.respondOutputStream(ContentType.parse(mime.substringBefore(';')), status) {
                                withContext(Dispatchers.IO) {
                                    val input = storage.openInput(doc) ?: throw FileNotFoundException()
                                    input.use { ins ->
                                        var toSkip = start
                                        val buf = ByteArray(64 * 1024)
                                        while (toSkip > 0) {
                                            val s = ins.skip(toSkip)
                                            if (s == 0L) {
                                                val b = ins.read()
                                                if (b == -1) break
                                                toSkip--
                                            } else toSkip -= s
                                        }
                                        var remaining = if (range != null) (endEx - start) else Long.MAX_VALUE
                                        while (remaining > 0) {
                                            val want = minOf(buf.size.toLong(), remaining).toInt()
                                            val n = ins.read(buf, 0, want)
                                            if (n < 0) break
                                            this@respondOutputStream.write(buf, 0, n)
                                            sent += n
                                            remaining -= n
                                        }
                                    }
                                }
                            }
                            deps.onRequest("GET", "/$raw", status.value, call.request.local.remoteHost, sent, 0)
                            return@get
                        }
                    }
                }
                // Fallback — if no static file matched, serve Web UI for SPA handling or 404 for assets?
                // For asset requests (css/js), return 404 if not found when webMode enabled; otherwise serve app.
                if (webMode && raw.substringAfterLast('.', "") in setOf("css", "js", "png", "jpg", "svg", "woff", "woff2")) {
                    call.respondText("Not found", ContentType.Text.Plain, HttpStatusCode.NotFound)
                } else {
                    call.respondText(WebUiAssets.indexHtml(deps.serverName()), ContentType.Text.Html)
                }
            }
        }
    }
}

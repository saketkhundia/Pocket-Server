package com.saketkhundia.pocketserver.server.routing

import com.saketkhundia.pocketserver.domain.model.SharedFolder
import com.saketkhundia.pocketserver.server.auth.AuthManager
import com.saketkhundia.pocketserver.server.media.MediaSupport
import com.saketkhundia.pocketserver.server.media.RangeSupport
import com.saketkhundia.pocketserver.server.security.PathSecurity
import com.saketkhundia.pocketserver.server.security.RequestValidation
import com.saketkhundia.pocketserver.storage.SharedFolderManager
import com.saketkhundia.pocketserver.storage.StorageManager
import com.saketkhundia.pocketserver.util.Constants
import com.saketkhundia.pocketserver.util.MimeUtils
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.request.header
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.head
import io.ktor.server.routing.options
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class RouteDeps(
    val auth: AuthManager,
    val folders: SharedFolderManager,
    val storage: StorageManager,
    val serverName: () -> String,
    val webMode: () -> Boolean,
    val webRootId: () -> String?,
    val sharedSnapshot: suspend () -> List<SharedFolder>,
    val onRequest: suspend (method: String, path: String, status: Int, ip: String, bytesDown: Long, bytesUp: Long) -> Unit
)

private fun ApplicationCall.clientIp(): String =
    request.header("X-Forwarded-For")?.split(",")?.firstOrNull()?.trim()
        ?: request.local.remoteHost.ifBlank { "lan-client" }

private fun ApplicationCall.sessionToken(auth: AuthManager): String? =
    com.saketkhundia.pocketserver.server.auth.SessionManager.extractToken(
        request.header(HttpHeaders.Cookie), request.header(HttpHeaders.Authorization)
    ).let { t -> t?.let { auth.sessions.validate(it)?.token } }

private suspend fun ApplicationCall.requireAuth(deps: RouteDeps): Boolean {
    if (!deps.auth.authRequired) return true
    val token = com.saketkhundia.pocketserver.server.auth.SessionManager.extractToken(
        request.header(HttpHeaders.Cookie), request.header(HttpHeaders.Authorization)
    )
    if (deps.auth.sessions.validate(token) != null) return true
    deps.onRequest(request.local.method.value, request.local.uri, 401, clientIp(), 0, 0)
    respondText(JSONObject().put("error", "unauthorized").toString(), ContentType.Application.Json, HttpStatusCode.Unauthorized)
    return false
}

private fun jsonFiles(files: List<com.saketkhundia.pocketserver.domain.model.FileEntry>, path: String): String {
    val arr = JSONArray()
    files.forEach { f ->
        arr.put(JSONObject().put("name", f.name).put("path", f.path).put("isDir", f.isDir).put("size", f.size).put("lastModified", f.lastModified).put("mime", f.mime))
    }
    return JSONObject().put("path", path).put("files", arr).toString()
}

private fun sortFiles(files: List<com.saketkhundia.pocketserver.domain.model.FileEntry>, sort: String?): List<com.saketkhundia.pocketserver.domain.model.FileEntry> {
    return when (sort) {
        "size" -> files.sortedWith(compareBy({ !it.isDir }, { it.size }))
        "size_desc" -> files.sortedWith(compareBy({ !it.isDir }, { -it.size }))
        "date" -> files.sortedWith(compareBy({ !it.isDir }, { it.lastModified }))
        "date_desc" -> files.sortedWith(compareBy({ !it.isDir }, { -it.lastModified }))
        "name_desc" -> files.sortedWith(compareBy({ !it.isDir }, { it.name.lowercase() })).reversed().sortedBy { !it.isDir }
        else -> files.sortedWith(compareBy({ !it.isDir }, { it.name.lowercase() }))
    }
}

fun Route.pocketApi(deps: RouteDeps) {
    get("/api/status") {
        val token = com.saketkhundia.pocketserver.server.auth.SessionManager.extractToken(
            call.request.header(HttpHeaders.Cookie), call.request.header(HttpHeaders.Authorization)
        )
        val authed = !deps.auth.authRequired || deps.auth.sessions.validate(token) != null
        val json = JSONObject()
            .put("name", deps.serverName())
            .put("authRequired", deps.auth.authRequired)
            .put("authenticated", authed)
            .put("version", "1.0.0").toString()
        call.respondText(json, ContentType.Application.Json)
        deps.onRequest("GET", "/api/status", 200, call.clientIp(), json.length.toLong(), 0)
    }

    post("/api/auth/login") {
        val ip = call.clientIp()
        if (deps.auth.rateLimiter.isBlocked(ip)) {
            call.response.header("Retry-After", "300")
            call.respondText(JSONObject().put("error", "too many attempts").toString(), ContentType.Application.Json, HttpStatusCode.TooManyRequests)
            deps.onRequest("POST", "/api/auth/login", 429, ip, 0, 0)
            return@post
        }
        val body = try { call.receiveText() } catch (_: Exception) { "" }
        val obj = try { JSONObject(body) } catch (_: Exception) { JSONObject() }
        val u = obj.optString("username", "").take(128)
        val p = obj.optString("password", "").take(512)
        val session = deps.auth.login(ip, u, p)
        if (session == null) {
            // Distinguish blocked vs bad creds
            val code = if (deps.auth.rateLimiter.isBlocked(ip)) HttpStatusCode.TooManyRequests else HttpStatusCode.Unauthorized
            if (code == HttpStatusCode.TooManyRequests) call.response.header("Retry-After", "300")
            call.respondText(JSONObject().put("error", "invalid credentials").toString(), ContentType.Application.Json, code)
            deps.onRequest("POST", "/api/auth/login", code.value, ip, 0, 0)
        } else {
            call.response.header(HttpHeaders.SetCookie, "ps_session=${session.token}; Path=/; HttpOnly; SameSite=Lax; Max-Age=${60 * 60 * 24}")
            call.respondText(JSONObject().put("ok", true).put("token", session.token).toString(), ContentType.Application.Json)
            deps.onRequest("POST", "/api/auth/login", 200, ip, 0, 0)
        }
    }

    post("/api/auth/logout") {
        val token = com.saketkhundia.pocketserver.server.auth.SessionManager.extractToken(
            call.request.header(HttpHeaders.Cookie), call.request.header(HttpHeaders.Authorization)
        )
        if (token != null) deps.auth.sessions.invalidate(token)
        call.response.header(HttpHeaders.SetCookie, "ps_session=; Path=/; HttpOnly; Max-Age=0")
        call.respondText(JSONObject().put("ok", true).toString(), ContentType.Application.Json)
    }

    get("/api/files") {
        if (!call.requireAuth(deps)) return@get
        if (!RequestValidation.validateSearchQuery(call.request.queryParameters["search"]) ||
            !RequestValidation.validateSortParam(call.request.queryParameters["sort"])
        ) {
            call.respondText(JSONObject().put("error", "bad query").toString(), ContentType.Application.Json, HttpStatusCode.BadRequest)
            return@get
        }
        val rawPath = call.request.queryParameters["path"] ?: ""
        val norm = PathSecurity.normalizeVirtualPath(rawPath)
        if (norm == null) {
            call.respondText(JSONObject().put("error", "invalid path").toString(), ContentType.Application.Json, HttpStatusCode.BadRequest)
            deps.onRequest("GET", "/api/files", 400, call.clientIp(), 0, 0)
            return@get
        }
        val search = call.request.queryParameters["search"]?.lowercase()?.take(256)
        val sort = call.request.queryParameters["sort"]
        val list = deps.folders.listVirtual(norm)
        if (list == null) {
            call.respondText(JSONObject().put("error", "not found").toString(), ContentType.Application.Json, HttpStatusCode.NotFound)
            deps.onRequest("GET", "/api/files", 404, call.clientIp(), 0, 0)
            return@get
        }
        var out = if (search.isNullOrBlank()) list else list.filter { it.name.lowercase().contains(search) }
        out = sortFiles(out, sort)
        // Pagination guard: cap at 2000 entries per response
        if (out.size > 2000) out = out.take(2000)
        val json = jsonFiles(out, norm)
        call.respondText(json, ContentType.Application.Json)
        deps.onRequest("GET", "/api/files", 200, call.clientIp(), json.length.toLong(), 0)
    }

    get("/api/media") {
        if (!call.requireAuth(deps)) return@get
        val type = call.request.queryParameters["type"] ?: "image" // image|video|audio
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 1000) ?: 200
        val roots = deps.folders.listVirtual("") ?: emptyList()
        val acc = mutableListOf<com.saketkhundia.pocketserver.domain.model.FileEntry>()
        suspend fun walk(vpath: String, depth: Int) {
            if (acc.size >= limit || depth > 4) return
            val children = deps.folders.listVirtual(vpath) ?: return
            for (c in children) {
                if (acc.size >= limit) return
                if (c.isDir) { if (depth < 4) walk(c.path, depth + 1) }
                else {
                    val kind = MediaSupport.kindOf(c.name)
                    if ((type == "image" && kind == "image") || (type == "video" && (kind == "video" || kind == "audio")) || (type == "audio" && kind == "audio")) {
                        acc.add(c)
                    }
                }
            }
        }
        for (r in roots) walk(r.path, 0)
        val json = jsonFiles(acc.take(limit), "")
        call.respondText(json, ContentType.Application.Json)
        deps.onRequest("GET", "/api/media", 200, call.clientIp(), json.length.toLong(), 0)
    }

    // Download with streaming + range support. Shared by GET and HEAD.
    suspend fun serveDownload(appCall: ApplicationCall, headOnly: Boolean) {
        if (!appCall.requireAuth(deps)) return
        val rawPath = appCall.request.queryParameters["path"] ?: ""
        val norm = PathSecurity.normalizeVirtualPath(rawPath)
        if (norm == null || norm.isEmpty()) {
            if (!headOnly) appCall.respondText(JSONObject().put("error", "invalid path").toString(), ContentType.Application.Json, HttpStatusCode.BadRequest)
            else appCall.respondText("", ContentType.Text.Plain, HttpStatusCode.BadRequest)
            return
        }
        val resolved = deps.folders.resolveDoc(norm)
        val doc = resolved?.doc
        if (doc == null || doc.isDirectory) {
            if (!headOnly) appCall.respondText(JSONObject().put("error", "not found").toString(), ContentType.Application.Json, HttpStatusCode.NotFound)
            else appCall.respondText("", ContentType.Text.Plain, HttpStatusCode.NotFound)
            deps.onRequest(if (headOnly) "HEAD" else "GET", "/api/download", 404, appCall.clientIp(), 0, 0)
            return
        }
        val total = doc.length().takeIf { it >= 0 } ?: -1L
        val mime = MimeUtils.fromName(doc.name ?: norm.substringAfterLast('/'))
        val rangeHeader = appCall.request.header(HttpHeaders.Range)
        val range = if (total > 0) RangeSupport.parse(rangeHeader, total) else null
        // If Range header present but unsatisfiable:
        if (rangeHeader != null && total > 0 && range == null && rangeHeader.startsWith("bytes=")) {
            appCall.response.header("Content-Range", "bytes */$total")
            appCall.respondText("Range not satisfiable", ContentType.Text.Plain, HttpStatusCode.RequestedRangeNotSatisfiable)
            deps.onRequest("GET", "/api/download", 416, appCall.clientIp(), 0, 0)
            return
        }
        val start = range?.start ?: 0L
        val endEx = range?.endExclusive ?: total
        val outLen = if (total >= 0 && endEx >= 0) (endEx - start).coerceAtLeast(0) else -1L
        val status = if (range != null) HttpStatusCode.PartialContent else HttpStatusCode.OK
        appCall.response.header(HttpHeaders.AcceptRanges, "bytes")
        appCall.response.header("Content-Type", mime)
        val safeName = (doc.name ?: "file").replace("\"", "")
        appCall.response.header(HttpHeaders.ContentDisposition, "inline; filename*=UTF-8''${URLEncoder.encode(safeName, "UTF-8")}")
        if (outLen >= 0) appCall.response.header(HttpHeaders.ContentLength, outLen.toString())
        if (range != null && total > 0) appCall.response.header(HttpHeaders.ContentRange, "bytes $start-${endEx - 1}/$total")
        if (headOnly) {
            appCall.respondText("", ContentType.Text.Plain, status)
            deps.onRequest("HEAD", "/api/download", status.value, appCall.clientIp(), 0, 0)
            return
        }
        var sent = 0L
        appCall.respondOutputStream(ContentType.parse(mime.substringBefore(';')), status) {
            withContext(Dispatchers.IO) {
                val input = deps.storage.openInput(doc) ?: throw java.io.FileNotFoundException()
                input.use { ins ->
                    // skip to start without loading into RAM
                    var toSkip = start
                    val buf = ByteArray(64 * 1024)
                    while (toSkip > 0) {
                        val skipped = ins.skip(toSkip)
                        if (skipped == 0L) {
                            val b = ins.read()
                            if (b == -1) break
                            toSkip--
                        } else {
                            toSkip -= skipped
                        }
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
                    this@respondOutputStream.flush()
                }
            }
        }
        deps.onRequest("GET", "/api/download", status.value, appCall.clientIp(), sent, 0)
    }

    get("/api/download") { serveDownload(call, false) }
    head("/api/download") { serveDownload(call, true) }
    // Legacy alias
    get("/api/file") { serveDownload(call, false) }

    post("/api/upload") {
        if (!call.requireAuth(deps)) return@post
        val rawPath = call.request.queryParameters["path"] ?: ""
        val norm = PathSecurity.normalizeVirtualPath(rawPath)
        if (norm == null) {
            call.respondText(JSONObject().put("error", "invalid path").toString(), ContentType.Application.Json, HttpStatusCode.BadRequest)
            return@post
        }
        // Resolve target dir: root "" means must pick a folder — reject upload to virtual root
        if (norm.isEmpty()) {
            call.respondText(JSONObject().put("error", "choose a folder").toString(), ContentType.Application.Json, HttpStatusCode.BadRequest)
            return@post
        }
        val target = deps.folders.parse(norm)
        if (target == null) {
            call.respondText(JSONObject().put("error", "not found").toString(), ContentType.Application.Json, HttpStatusCode.NotFound)
            return@post
        }
        val folder = deps.sharedSnapshot().find { it.name == target.folder.name }
        if (folder == null || folder.readOnly) {
            call.respondText(JSONObject().put("error", "read-only").toString(), ContentType.Application.Json, HttpStatusCode.Forbidden)
            return@post
        }
        val resolved = deps.folders.resolveDoc(norm)
        val dirDoc = resolved?.doc
        if (dirDoc == null || !dirDoc.isDirectory) {
            call.respondText(JSONObject().put("error", "not found").toString(), ContentType.Application.Json, HttpStatusCode.NotFound)
            return@post
        }
        val uploaded = mutableListOf<String>()
        var totalUp = 0L
        try {
            val multipart = call.receiveMultipart()
            var count = 0
            multipart.forEachPart { part ->
                try {
                    if (part is PartData.FileItem && part.name == "files") {
                        if (++count > 20) return@forEachPart // cap files per request
                        var fname = part.originalFileName?.substringAfterLast('/')?.substringAfterLast('\\') ?: "upload.bin"
                        fname = fname.take(255)
                        if (PathSecurity.sanitizeFileName(fname) is PathSecurity.Check.Rejected) {
                            fname = "upload_${System.currentTimeMillis()}.bin"
                        }
                        // Duplicate handling: append (1), (2)…
                        var finalName = fname
                        val existing = withContext(Dispatchers.IO) { dirDoc.listFiles().mapNotNull { it.name }.toSet() } + uploaded.toSet()
                        if (finalName in existing) {
                            val base = fname.substringBeforeLast('.', fname)
                            val ext = fname.substringAfterLast('.', "")
                            var i = 1
                            do {
                                finalName = if (ext.isEmpty() || ext == fname) "$base ($i)" else "$base ($i).$ext"
                                i++
                            } while (finalName in existing && i < 1000)
                        }
                        val mime = MimeUtils.fromName(finalName)
                        val bytes = withContext(Dispatchers.IO) {
                            deps.storage.writeStreamCapped(dirDoc, finalName, mime, part.provider().toInputStream(), Constants.MAX_UPLOAD_BYTES)
                        }
                        totalUp += bytes
                        uploaded.add(finalName)
                    }
                } finally { part.dispose() }
            }
        } catch (e: Exception) {
            call.respondText(JSONObject().put("error", "upload failed").toString(), ContentType.Application.Json, HttpStatusCode.BadRequest)
            deps.onRequest("POST", "/api/upload", 400, call.clientIp(), 0, totalUp)
            return@post
        }
        if (uploaded.isEmpty()) {
            call.respondText(JSONObject().put("error", "no files").toString(), ContentType.Application.Json, HttpStatusCode.BadRequest)
            deps.onRequest("POST", "/api/upload", 400, call.clientIp(), 0, 0)
        } else {
            call.respondText(JSONObject().put("uploaded", JSONArray(uploaded)).toString(), ContentType.Application.Json, HttpStatusCode.Created)
            deps.onRequest("POST", "/api/upload", 201, call.clientIp(), 0, totalUp)
        }
    }

    post("/api/folder") {
        if (!call.requireAuth(deps)) return@post
        val body = try { JSONObject(call.receiveText()) } catch (_: Exception) { JSONObject() }
        val rawPath = body.optString("path", "")
        val name = body.optString("name", "").trim()
        if (PathSecurity.sanitizeFileName(name) is PathSecurity.Check.Rejected) {
            call.respondText(JSONObject().put("error", "invalid name").toString(), ContentType.Application.Json, HttpStatusCode.BadRequest)
            return@post
        }
        val norm = PathSecurity.normalizeVirtualPath(rawPath) ?: run {
            call.respondText(JSONObject().put("error", "invalid path").toString(), ContentType.Application.Json, HttpStatusCode.BadRequest)
            return@post
        }
        if (norm.isEmpty()) {
            call.respondText(JSONObject().put("error", "cannot create at root").toString(), ContentType.Application.Json, HttpStatusCode.BadRequest)
            return@post
        }
        val resolved = deps.folders.resolveDoc(norm) ?: run {
            call.respondText(JSONObject().put("error", "not found").toString(), ContentType.Application.Json, HttpStatusCode.NotFound)
            return@post
        }
        val ok = deps.storage.createFolder(resolved.doc, name)
        if (ok) {
            call.respondText(JSONObject().put("ok", true).toString(), ContentType.Application.Json, HttpStatusCode.Created)
            deps.onRequest("POST", "/api/folder", 201, call.clientIp(), 0, 0)
        } else {
            call.respondText(JSONObject().put("error", "exists or failed").toString(), ContentType.Application.Json, HttpStatusCode.Conflict)
            deps.onRequest("POST", "/api/folder", 409, call.clientIp(), 0, 0)
        }
    }

    put("/api/file") {
        if (!call.requireAuth(deps)) return@put
        val body = try { JSONObject(call.receiveText()) } catch (_: Exception) { JSONObject() }
        val rawPath = body.optString("path", "")
        val newName = body.optString("newName", "").trim()
        if (PathSecurity.sanitizeFileName(newName) is PathSecurity.Check.Rejected) {
            call.respondText(JSONObject().put("error", "invalid name").toString(), ContentType.Application.Json, HttpStatusCode.BadRequest)
            return@put
        }
        val norm = PathSecurity.normalizeVirtualPath(rawPath)
        if (norm == null || norm.isEmpty()) {
            call.respondText(JSONObject().put("error", "invalid path").toString(), ContentType.Application.Json, HttpStatusCode.BadRequest)
            return@put
        }
        val resolved = deps.folders.resolveDoc(norm) ?: run {
            call.respondText(JSONObject().put("error", "not found").toString(), ContentType.Application.Json, HttpStatusCode.NotFound)
            return@put
        }
        val ok = deps.storage.rename(resolved.doc, newName)
        if (ok) {
            call.respondText(JSONObject().put("ok", true).toString(), ContentType.Application.Json)
            deps.onRequest("PUT", "/api/file", 200, call.clientIp(), 0, 0)
        } else {
            call.respondText(JSONObject().put("error", "rename failed").toString(), ContentType.Application.Json, HttpStatusCode.Conflict)
            deps.onRequest("PUT", "/api/file", 409, call.clientIp(), 0, 0)
        }
    }

    delete("/api/file") {
        if (!call.requireAuth(deps)) return@delete
        val rawPath = call.request.queryParameters["path"] ?: ""
        val norm = PathSecurity.normalizeVirtualPath(rawPath)
        if (norm == null || norm.isEmpty()) {
            call.respondText(JSONObject().put("error", "invalid path").toString(), ContentType.Application.Json, HttpStatusCode.BadRequest)
            return@delete
        }
        val resolved = deps.folders.resolveDoc(norm) ?: run {
            call.respondText(JSONObject().put("error", "not found").toString(), ContentType.Application.Json, HttpStatusCode.NotFound)
            return@delete
        }
        val ok = deps.storage.delete(resolved.doc)
        if (ok) {
            call.respondText(JSONObject().put("ok", true).toString(), ContentType.Application.Json)
            deps.onRequest("DELETE", "/api/file", 200, call.clientIp(), 0, 0)
        } else {
            call.respondText(JSONObject().put("error", "delete failed").toString(), ContentType.Application.Json, HttpStatusCode.Conflict)
            deps.onRequest("DELETE", "/api/file", 409, call.clientIp(), 0, 0)
        }
    }

    options("/api/{...}") {
        call.response.header(HttpHeaders.Allow, "GET, HEAD, POST, PUT, DELETE, OPTIONS")
        call.respondText("", ContentType.Text.Plain, HttpStatusCode.NoContent)
    }
}

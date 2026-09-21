package com.saketkhundia.pocketserver

import com.saketkhundia.pocketserver.domain.repository.AuthCredentialRepository
import com.saketkhundia.pocketserver.server.auth.AuthManager
import com.saketkhundia.pocketserver.server.auth.PasswordHasher
import com.saketkhundia.pocketserver.server.routing.RouteDeps
import com.saketkhundia.pocketserver.server.routing.pocketApi
import com.saketkhundia.pocketserver.storage.SharedFolderManager
import com.saketkhundia.pocketserver.storage.StorageManager
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL

/**
 * End-to-end regression test for sign-in against the REAL route code:
 * real Ktor server + real pocketApi + real AuthManager/SessionManager/RateLimiter.
 * Android-bound storage/folders are mocked (login never touches them).
 */
class LoginFlowTest {

    /** In-memory credential repo mirroring DataStore behavior. */
    private class FakeCreds : AuthCredentialRepository {
        var user = "admin"
        var hash: String? = null
        var salt: String? = null
        override suspend fun setCredentials(username: String, passwordHash: String, salt: String) {
            user = username; hash = passwordHash; this.salt = salt
        }
        override suspend fun getUsername(): String = user
        override suspend fun getPasswordHash(): String? = hash
        override suspend fun getSalt(): String? = salt
        override suspend fun hasCredentials(): Boolean = hash != null && salt != null
    }

    private data class Server(val port: Int, val auth: AuthManager, val stop: () -> Unit)

    private fun startTestServer(authRequired: Boolean, seed: Boolean = true): Server {
        val creds = FakeCreds()
        if (seed) {
            val salt = PasswordHasher.generateSalt()
            runBlocking { creds.setCredentials("admin", PasswordHasher.hash("admin", salt), salt) }
        }
        val auth = AuthManager(creds)
        auth.authRequired = authRequired
        val deps = RouteDeps(
            auth = auth,
            folders = mockk<SharedFolderManager>(relaxed = true),
            storage = mockk<StorageManager>(relaxed = true),
            serverName = { "Pocket Server" },
            webMode = { false },
            webRootId = { null },
            sharedSnapshot = { emptyList() },
            onRequest = { _, _, _, _, _, _ -> }
        )
        val port = 18924
        val engine = embeddedServer(CIO, host = "127.0.0.1", port = port) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; isLenient = true }) }
            routing { route("/") { pocketApi(deps) } }
        }
        engine.start(wait = false)
        Thread.sleep(800)
        return Server(port, auth) { engine.stop(100, 200) }
    }

    private data class Resp(val code: Int, val body: String, val headers: Map<String, List<String>>)

    private fun header(resp: Resp, name: String): String? =
        resp.headers.entries.firstOrNull { it.key?.equals(name, ignoreCase = true) == true }
            ?.value?.firstOrNull()

    private fun http(
        port: Int,
        method: String,
        path: String,
        body: String? = null,
        cookie: String? = null
    ): Resp {
        val conn = (URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            doOutput = body != null
            connectTimeout = 5000
            readTimeout = 5000
            if (cookie != null) setRequestProperty("Cookie", cookie)
            if (body != null) {
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
        }
        val code = try {
            conn.responseCode
        } catch (e: Exception) {
            println("DIAG connect failed: ${e::class.simpleName}: ${e.message}")
            conn.disconnect()
            throw e
        }
        println("DIAG $method $path -> $code ${conn.responseMessage}")
        val stream = if (code in 200..299) {
            try { conn.inputStream } catch (_: Exception) { null }
        } else {
            try { conn.errorStream } catch (_: Exception) { null }
        }
        val resp = try { stream?.bufferedReader()?.readText() ?: "" } catch (_: Exception) { "" }
        val headers = conn.headerFields.filterKeys { it != null }.mapKeys { it.key!! }
        conn.disconnect()
        return Resp(code, resp, headers)
    }

    @Test
    fun `login with correct credentials succeeds and sets cookie`() {
        val s = startTestServer(authRequired = true)
        try {
            val r =
                http(s.port, "POST", "/api/auth/login", """{"username":"admin","password":"admin"}""")
            val setCookie = header(r, "Set-Cookie")
            println("LOGIN-OK code=${r.code} resp=${r.body} cookie=$setCookie")
            assertEquals(200, r.code)
            assertTrue(JSONObject(r.body).optBoolean("ok"))
            assertTrue(setCookie != null && setCookie.contains("ps_session="))

            // Authenticated request with the cookie works
            val cookie = setCookie!!.split(";").first()
            val r2 = http(s.port, "GET", "/api/status", cookie = cookie)
            assertEquals(200, r2.code)
            assertTrue(JSONObject(r2.body).optBoolean("authenticated"))
        } finally {
            s.stop()
        }
    }

    @Test
    fun `login with wrong password returns 401`() {
        val s = startTestServer(authRequired = true)
        try {
            val r =
                http(s.port, "POST", "/api/auth/login", """{"username":"admin","password":"wrong"}""")
            println("LOGIN-WRONG code=${r.code}")
            assertEquals(401, r.code)
        } finally {
            s.stop()
        }
    }

    @Test
    fun `repeated failures trigger 429 rate limit with Retry-After`() {
        val s = startTestServer(authRequired = true)
        try {
            repeat(5) {
                http(s.port, "POST", "/api/auth/login", """{"username":"admin","password":"wrong$it"}""")
            }
            val r =
                http(s.port, "POST", "/api/auth/login", """{"username":"admin","password":"wrong"}""")
            println("LOGIN-RATELIMIT code=${r.code}")
            assertEquals(429, r.code)
            assertTrue(header(r, "Retry-After")?.toIntOrNull() ?: 0 > 0)
        } finally {
            s.stop()
        }
    }

    @Test
    fun `status reflects authRequired flag and auth-off bypasses protection`() {
        val s = startTestServer(authRequired = true)
        try {
            val statusOn = http(s.port, "GET", "/api/status")
            assertTrue(JSONObject(statusOn.body).optBoolean("authRequired"))
            assertTrue(!JSONObject(statusOn.body).optBoolean("authenticated"))

            // Flip the flag live, exactly like Settings toggle does
            s.auth.authRequired = false
            val statusOff = http(s.port, "GET", "/api/status")
            assertEquals(200, statusOff.code)
            assertTrue(!JSONObject(statusOff.body).optBoolean("authRequired"))
            assertTrue(JSONObject(statusOff.body).optBoolean("authenticated"))
        } finally {
            s.stop()
        }
    }
}

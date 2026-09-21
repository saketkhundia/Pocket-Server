package com.saketkhundia.pocketserver.server.auth

import com.saketkhundia.pocketserver.util.Constants
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class Session(val token: String, val username: String, val expiresAtMs: Long)

class SessionManager(private var timeoutMin: Long = 60) {
    private val sessions = ConcurrentHashMap<String, Session>()
    private val rng = SecureRandom()

    fun setTimeoutMin(m: Long) { timeoutMin = m.coerceIn(5, 60 * 24) }

    fun create(username: String): Session {
        purge()
        val bytes = ByteArray(Constants.SESSION_TOKEN_BYTES)
        rng.nextBytes(bytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + UUID.randomUUID().toString().replace("-", "")
        val s = Session(token, username, System.currentTimeMillis() + timeoutMin * 60_000)
        sessions[token] = s
        return s
    }

    fun validate(token: String?): Session? {
        if (token.isNullOrBlank()) return null
        val s = sessions[token] ?: return null
        if (System.currentTimeMillis() > s.expiresAtMs) { sessions.remove(token); return null }
        return s
    }

    fun invalidate(token: String) { sessions.remove(token) }
    fun clear() = sessions.clear()
    fun count() = sessions.size.also { purge() }

    private fun purge() {
        val now = System.currentTimeMillis()
        sessions.entries.removeIf { it.value.expiresAtMs < now }
    }

    companion object {
        fun extractToken(cookieHeader: String?, authHeader: String?): String? {
            if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ")) {
                return authHeader.removePrefix("Bearer ").trim().takeIf { it.isNotEmpty() }
            }
            if (!cookieHeader.isNullOrBlank()) {
                for (part in cookieHeader.split(";")) {
                    val kv = part.trim().split("=", limit = 2)
                    if (kv.size == 2 && kv[0].trim() == "ps_session" && kv[1].isNotBlank()) return kv[1].trim()
                }
            }
            return null
        }
    }
}

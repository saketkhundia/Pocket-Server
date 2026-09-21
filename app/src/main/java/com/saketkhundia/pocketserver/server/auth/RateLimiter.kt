package com.saketkhundia.pocketserver.server.auth

import com.saketkhundia.pocketserver.util.Constants
import java.util.concurrent.ConcurrentHashMap

/** Basic brute-force protection for /api/auth/login. In-memory per-IP window. */
class RateLimiter {
    private data class State(var attempts: Int = 0, var windowStart: Long = 0, var blockedUntil: Long = 0)
    private val map = ConcurrentHashMap<String, State>()

    @Synchronized
    fun isBlocked(ip: String): Boolean {
        val s = map[ip] ?: return false
        return System.currentTimeMillis() < s.blockedUntil
    }

    @Synchronized
    fun recordFailure(ip: String) {
        val now = System.currentTimeMillis()
        val s = map.getOrPut(ip) { State(windowStart = now) }
        if (now - s.windowStart > Constants.LOGIN_WINDOW_MS) { s.attempts = 0; s.windowStart = now }
        s.attempts++
        if (s.attempts >= Constants.MAX_LOGIN_ATTEMPTS) {
            s.blockedUntil = now + Constants.LOGIN_BLOCK_MS
            s.attempts = 0
        }
    }

    @Synchronized
    fun recordSuccess(ip: String) { map.remove(ip) }

    /** Clears all throttle state (used by on-device credential reset recovery). */
    @Synchronized
    fun clear() { map.clear() }
}

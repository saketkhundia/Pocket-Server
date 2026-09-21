package com.saketkhundia.pocketserver.server.auth

import com.saketkhundia.pocketserver.domain.repository.AuthCredentialRepository

class AuthManager(
    private val creds: AuthCredentialRepository,
    val sessions: SessionManager = SessionManager(),
    val rateLimiter: RateLimiter = RateLimiter()
) {
    var authRequired: Boolean = true

    suspend fun ensureDefaultCredentials(defaultUser: String, defaultPass: String) {
        if (!creds.hasCredentials()) {
            val salt = PasswordHasher.generateSalt()
            creds.setCredentials(defaultUser, PasswordHasher.hash(defaultPass, salt), salt)
        }
    }

    suspend fun login(ip: String, username: String, password: String): Session? {
        if (rateLimiter.isBlocked(ip)) return null
        val expectedUser = creds.getUsername()
        val hash = creds.getPasswordHash()
        val salt = creds.getSalt()
        val ok = hash != null && salt != null &&
            username == expectedUser && PasswordHasher.verify(password, salt, hash)
        if (ok) { rateLimiter.recordSuccess(ip); return sessions.create(username) }
        rateLimiter.recordFailure(ip)
        return null
    }

    /** Stored username for protocols (FTP) that resolve their own user mapping. */
    suspend fun storedUsername(): String? = creds.getUsername()

    /**
     * Verifies a raw password against stored credentials without any
     * session/rate-limit side effects — the caller (FTP) owns those.
     * Matches login()'s no-credentials fallback: any non-empty password passes
     * when nothing is stored yet.
     */
    suspend fun verifyPassword(password: String): Boolean {
        val hash = creds.getPasswordHash()
        val salt = creds.getSalt()
        return if (hash != null && salt != null) PasswordHasher.verify(password, salt, hash)
        else password.isNotEmpty()
    }

    suspend fun changeCredentials(newUser: String, newPass: String) {
        require(newUser.length in 1..64)
        require(newPass.length >= 4 && newPass.length <= 256)
        val salt = PasswordHasher.generateSalt()
        creds.setCredentials(newUser, PasswordHasher.hash(newPass, salt), salt)
        sessions.clear()
    }

    /**
     * Lockout recovery: restore the documented default (admin/admin) and
     * invalidate all sessions plus any login-throttle state. Only reachable
     * from the on-device Settings UI (device owner), never over HTTP.
     */
    suspend fun resetCredentials(defaultUser: String = "admin", defaultPass: String = "admin") {
        val salt = PasswordHasher.generateSalt()
        creds.setCredentials(defaultUser, PasswordHasher.hash(defaultPass, salt), salt)
        sessions.clear()
        rateLimiter.clear()
    }
}

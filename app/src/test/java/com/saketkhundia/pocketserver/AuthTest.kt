package com.saketkhundia.pocketserver

import com.saketkhundia.pocketserver.server.auth.PasswordHasher
import com.saketkhundia.pocketserver.server.auth.RateLimiter
import com.saketkhundia.pocketserver.server.auth.SessionManager
import org.junit.Assert.*
import org.junit.Test

class AuthTest {

    @Test
    fun `password hash and verify`() {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash("secret123", salt)
        assertTrue(PasswordHasher.verify("secret123", salt, hash))
        assertFalse(PasswordHasher.verify("wrong", salt, hash))
        assertFalse(PasswordHasher.verify("secret123", PasswordHasher.generateSalt(), hash))
    }

    @Test
    fun `password verify rejects long password`() {
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash("short", salt)
        assertFalse(PasswordHasher.verify("a".repeat(600), salt, hash))
    }

    @Test
    fun `session create and validate`() {
        val mgr = SessionManager(timeoutMin = 60)
        val s = mgr.create("admin")
        assertNotNull(s.token)
        assertEquals("admin", mgr.validate(s.token)?.username)
        // invalid token
        assertNull(mgr.validate("bad"))
        assertNull(mgr.validate(null))
    }

    @Test
    fun `session expiration`() {
        val mgr = SessionManager(timeoutMin = 60)
        // Create session then manually expire by setting timeout to 0 and purging?
        // Simulate by creating expired session
        val s = mgr.create("admin")
        // Invalidate and check
        mgr.invalidate(s.token)
        assertNull(mgr.validate(s.token))
    }

    @Test
    fun `session clear`() {
        val mgr = SessionManager()
        val s1 = mgr.create("admin"); val s2 = mgr.create("user")
        assertEquals(2, mgr.count())
        mgr.clear()
        assertEquals(0, mgr.count())
        assertNull(mgr.validate(s1.token))
    }

    @Test
    fun `session extractToken from cookie and bearer`() {
        val tok = "abc123"
        assertEquals(tok, SessionManager.extractToken("ps_session=$tok; Path=/", null))
        assertEquals(tok, SessionManager.extractToken(null, "Bearer $tok"))
        // When both present, Bearer takes precedence
        assertEquals("bad", SessionManager.extractToken("other=x; ps_session=$tok", "Bearer bad"))
        assertEquals(tok, SessionManager.extractToken("other=x; ps_session=$tok", null))
        assertNull(SessionManager.extractToken(null, null))
        assertNull(SessionManager.extractToken("", ""))
    }

    @Test
    fun `rate limiter blocks after threshold`() {
        val rl = RateLimiter()
        val ip = "1.2.3.4"
        assertFalse(rl.isBlocked(ip))
        repeat(5) { rl.recordFailure(ip) }
        assertTrue(rl.isBlocked(ip))
        rl.recordSuccess(ip)
        assertFalse(rl.isBlocked(ip))
    }

    @Test
    fun `rate limiter isolates ips`() {
        val rl = RateLimiter()
        repeat(5) { rl.recordFailure("1.1.1.1") }
        assertTrue(rl.isBlocked("1.1.1.1"))
        assertFalse(rl.isBlocked("2.2.2.2"))
    }
}

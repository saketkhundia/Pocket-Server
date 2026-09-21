package com.saketkhundia.pocketserver.server.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object PasswordHasher {
    private const val ITERATIONS = 120_000
    fun generateSalt(): String {
        val b = ByteArray(16)
        SecureRandom().nextBytes(b)
        return Base64.getEncoder().encodeToString(b)
    }
    fun hash(password: String, saltB64: String): String {
        val salt = Base64.getDecoder().decode(saltB64)
        var data = salt + password.toByteArray(Charsets.UTF_8)
        val md = MessageDigest.getInstance("SHA-256")
        repeat(ITERATIONS) { data = md.digest(data) }
        return Base64.getEncoder().encodeToString(data)
    }
    fun verify(password: String, saltB64: String, expectedHash: String): Boolean {
        if (password.length > 512) return false
        val actual = try { hash(password, saltB64) } catch (_: Exception) { return false }
        return MessageDigest.isEqual(actual.toByteArray(), expectedHash.toByteArray())
    }
}

package com.saketkhundia.pocketserver

import com.saketkhundia.pocketserver.domain.model.LocalServerAddress
import com.saketkhundia.pocketserver.domain.model.MdnsStatus
import com.saketkhundia.pocketserver.server.mdns.loopbackUrl
import com.saketkhundia.pocketserver.server.mdns.mdnsHostnameUrl
import com.saketkhundia.pocketserver.server.mdns.sanitizeMdnsHost
import com.saketkhundia.pocketserver.server.mdns.stripTrailingDot
import org.junit.Assert.*
import org.junit.Test

class MdnsAddressTest {

    @Test
    fun `hostname url keeps dynamic port`() {
        assertEquals("http://pocketserver.local:8080", mdnsHostnameUrl("pocketserver", 8080))
        assertEquals("http://pocketserver.local:9090", mdnsHostnameUrl("pocketserver", 9090))
    }

    @Test
    fun `hostname url tolerates fqdn and case`() {
        assertEquals("http://pocketserver.local:8080", mdnsHostnameUrl("PocketServer.local.", 8080))
    }

    @Test
    fun `sanitize keeps dns-safe labels`() {
        assertEquals("pocketserver", sanitizeMdnsHost("pocketserver"))
        assertEquals("my-phone", sanitizeMdnsHost("My Phone!"))
        assertEquals("pocketserver", sanitizeMdnsHost(""))
        assertEquals("pocketserver", sanitizeMdnsHost("!!!"))
        assertTrue(sanitizeMdnsHost("a".repeat(100)).length <= 63)
    }

    @Test
    fun `strip trailing dot`() {
        assertEquals("pocketserver.local", stripTrailingDot("pocketserver.local."))
        assertEquals("pocketserver.local", stripTrailingDot("pocketserver.local"))
    }

    @Test
    fun `primary prefers verified hostname over ip`() {
        val a = LocalServerAddress("http://pocketserver.local:8080", "http://192.168.1.20:8080", MdnsStatus.AVAILABLE)
        assertEquals("http://pocketserver.local:8080", a.primaryUrl)
        assertTrue(a.mdnsAvailable)
    }

    @Test
    fun `primary falls back to ip when mdns unavailable`() {
        val a = LocalServerAddress(null, "http://192.168.1.20:8080", MdnsStatus.UNAVAILABLE)
        assertEquals("http://192.168.1.20:8080", a.primaryUrl)
        assertFalse(a.mdnsAvailable)
    }

    @Test
    fun `primary null when offline`() {
        val a = LocalServerAddress(null, null, MdnsStatus.IDLE)
        assertNull(a.primaryUrl)
        assertFalse(a.mdnsAvailable)
    }

    @Test
    fun `display shows pending name instantly while checking`() {
        val a = LocalServerAddress(null, "http://192.168.1.20:8080", MdnsStatus.REGISTERING, "http://pocketserver.local:8080")
        assertEquals("http://pocketserver.local:8080", a.displayUrl)
        // Actions stay on the working IP until verified.
        assertEquals("http://192.168.1.20:8080", a.primaryUrl)
        assertTrue(a.mdnsChecking)
        assertFalse(a.mdnsAvailable)
    }

    @Test
    fun `display prefers verified hostname over pending`() {
        val a = LocalServerAddress("http://pocketserver-2.local:8080", "http://192.168.1.20:8080", MdnsStatus.AVAILABLE, "http://pocketserver.local:8080")
        assertEquals("http://pocketserver-2.local:8080", a.displayUrl)
        assertEquals("http://pocketserver-2.local:8080", a.primaryUrl)
        assertFalse(a.mdnsChecking)
    }

    @Test
    fun `loopback preview ignores network state`() {
        assertEquals("http://127.0.0.1:8080", loopbackUrl(8080))
        assertEquals("http://127.0.0.1:9090", loopbackUrl(9090))
    }
}

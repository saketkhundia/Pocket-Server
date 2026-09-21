package com.saketkhundia.pocketserver

import com.saketkhundia.pocketserver.server.security.PathSecurity
import org.junit.Assert.*
import org.junit.Test

class PathSecurityTest {

    @Test
    fun `normalize empty and root`() {
        assertEquals("", PathSecurity.normalizeVirtualPath(""))
        assertEquals("", PathSecurity.normalizeVirtualPath("/"))
        assertEquals("", PathSecurity.normalizeVirtualPath(null))
        assertEquals("a/b", PathSecurity.normalizeVirtualPath("a/b"))
        assertEquals("a/b", PathSecurity.normalizeVirtualPath("/a/b/"))
        assertEquals("a/b", PathSecurity.normalizeVirtualPath("a//b"))
    }

    @Test
    fun `reject traversal outside root`() {
        assertNull(PathSecurity.normalizeVirtualPath("../etc/passwd"))
        assertNull(PathSecurity.normalizeVirtualPath("a/../../b"))
        assertNull(PathSecurity.normalizeVirtualPath(".."))
        assertNull(PathSecurity.normalizeVirtualPath("/../"))
        // a/.. resolves to root (empty), not traversal outside
        assertEquals("", PathSecurity.normalizeVirtualPath("a/.."))
        assertEquals("", PathSecurity.normalizeVirtualPath("a/b/../../"))
    }

    @Test
    fun `reject encoded traversal`() {
        assertNull(PathSecurity.normalizeVirtualPath("%2e%2e%2fetc%2fpasswd"))
        // a%2F..%2Fb decodes to a/../b -> b (valid within root)
        assertEquals("b", PathSecurity.normalizeVirtualPath("a%2F..%2Fb"))
        assertNull(PathSecurity.normalizeVirtualPath("%252e%252e%2f")) // double encoded
        assertNull(PathSecurity.normalizeVirtualPath("..%2F"))
        assertNull(PathSecurity.normalizeVirtualPath("%2e%2e/%2e%2e"))
    }

    @Test
    fun `reject backslash traversal`() {
        assertEquals(null, PathSecurity.normalizeVirtualPath("..\\etc\\passwd"))
        // a\..\b -> a/../b -> b (valid)
        assertEquals("b", PathSecurity.normalizeVirtualPath("a\\..\\b"))
        // but pure traversal via backslash outside root is rejected
        assertNull(PathSecurity.normalizeVirtualPath("..\\..\\b"))
    }

    @Test
    fun `reject null bytes`() {
        assertNull(PathSecurity.normalizeVirtualPath("a\u0000b"))
        assertNull(PathSecurity.normalizeVirtualPath("a%00b"))
    }

    @Test
    fun `sanitize file names`() {
        assertTrue(PathSecurity.sanitizeFileName("file.txt") is PathSecurity.Check.Ok)
        assertTrue(PathSecurity.sanitizeFileName("my document.pdf") is PathSecurity.Check.Ok)
        assertTrue(PathSecurity.sanitizeFileName("../") is PathSecurity.Check.Rejected)
        assertTrue(PathSecurity.sanitizeFileName("a/b") is PathSecurity.Check.Rejected)
        assertTrue(PathSecurity.sanitizeFileName("a\\b") is PathSecurity.Check.Rejected)
        assertTrue(PathSecurity.sanitizeFileName(".") is PathSecurity.Check.Rejected)
        assertTrue(PathSecurity.sanitizeFileName("..") is PathSecurity.Check.Rejected)
        assertTrue(PathSecurity.sanitizeFileName("") is PathSecurity.Check.Rejected)
        assertTrue(PathSecurity.sanitizeFileName("a\u0000b") is PathSecurity.Check.Rejected)
        assertTrue(PathSecurity.sanitizeFileName("a\u001F") is PathSecurity.Check.Rejected)
        assertTrue(PathSecurity.sanitizeFileName("a.".repeat(130)) is PathSecurity.Check.Rejected) // >255
    }

    @Test
    fun `normalize handles dot segments`() {
        assertEquals("a/c", PathSecurity.normalizeVirtualPath("a/b/../c"))
        assertEquals("a/c", PathSecurity.normalizeVirtualPath("a/./c"))
        assertEquals("a", PathSecurity.normalizeVirtualPath("a/././"))
    }

    @Test
    fun `splitRoot`() {
        assertEquals("Docs" to "", PathSecurity.splitRoot("Docs"))
        assertEquals("Docs" to "a/b", PathSecurity.splitRoot("Docs/a/b"))
    }

    @Test
    fun `reject oversized path`() {
        val long = "a/".repeat(3000) // >4096
        assertNull(PathSecurity.normalizeVirtualPath(long))
    }

    @Test
    fun `allow unicode filenames`() {
        assertEquals("文档/图片", PathSecurity.normalizeVirtualPath("文档/图片"))
        assertTrue(PathSecurity.sanitizeFileName("照片_2024.jpg") is PathSecurity.Check.Ok)
    }
}

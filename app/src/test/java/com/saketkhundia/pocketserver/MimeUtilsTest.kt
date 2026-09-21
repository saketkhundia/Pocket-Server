package com.saketkhundia.pocketserver

import com.saketkhundia.pocketserver.util.MimeUtils
import org.junit.Assert.*
import org.junit.Test

class MimeUtilsTest {
    @Test
    fun `mime detection`() {
        assertEquals("text/html; charset=utf-8", MimeUtils.fromName("index.html"))
        assertEquals("image/jpeg", MimeUtils.fromName("photo.jpg"))
        assertEquals("video/mp4", MimeUtils.fromName("movie.mp4"))
        assertEquals("application/octet-stream", MimeUtils.fromName("unknown.xyz"))
        assertEquals("text/css", MimeUtils.fromName("style.CSS")) // case insensitive
    }

    @Test
    fun `media detection`() {
        assertTrue(MimeUtils.isImage("pic.png"))
        assertTrue(MimeUtils.isVideo("video.mkv"))
        assertTrue(MimeUtils.isAudio("song.mp3"))
        assertTrue(MimeUtils.isMedia("a.jpg"))
        assertFalse(MimeUtils.isMedia("doc.pdf"))
    }
}

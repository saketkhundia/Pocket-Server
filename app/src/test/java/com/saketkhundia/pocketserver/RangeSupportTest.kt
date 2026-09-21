package com.saketkhundia.pocketserver

import com.saketkhundia.pocketserver.server.media.RangeSupport
import org.junit.Assert.*
import org.junit.Test

class RangeSupportTest {
    @Test
    fun `parse simple range`() {
        val r = RangeSupport.parse("bytes=0-1023", 5000)
        assertNotNull(r); assertEquals(0, r!!.start); assertEquals(1024, r.endExclusive)
    }

    @Test
    fun `parse open ended`() {
        val r = RangeSupport.parse("bytes=500-", 1000)
        assertNotNull(r); assertEquals(500, r!!.start); assertEquals(1000, r.endExclusive)
    }

    @Test
    fun `parse suffix`() {
        val r = RangeSupport.parse("bytes=-500", 1000)
        assertNotNull(r); assertEquals(500, r!!.start); assertEquals(1000, r.endExclusive)
    }

    @Test
    fun `reject malformed`() {
        assertNull(RangeSupport.parse("bytes=abc-def", 1000))
        assertNull(RangeSupport.parse("bytes=2000-3000", 1000)) // beyond total
        assertNull(RangeSupport.parse(null, 1000))
        assertNull(RangeSupport.parse("bytes=0-10", 0))
        assertNull(RangeSupport.parse("invalid", 1000))
    }

    @Test
    fun `suffix larger than total`() {
        val r = RangeSupport.parse("bytes=-2000", 1000)
        assertNotNull(r); assertEquals(0, r!!.start)
    }
}

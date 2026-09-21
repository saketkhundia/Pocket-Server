package com.saketkhundia.pocketserver

import com.saketkhundia.pocketserver.domain.monetization.Feature
import com.saketkhundia.pocketserver.domain.monetization.FreeFeatureAccessManager
import com.saketkhundia.pocketserver.domain.monetization.ProFeatureAccessManager
import org.junit.Assert.*
import org.junit.Test

class FeatureAccessTest {
    @Test
    fun `free tier limits`() {
        val free = FreeFeatureAccessManager()
        assertTrue(free.isAllowed(Feature.HTTP_FILES))
        assertFalse(free.isAllowed(Feature.FTP))
        assertFalse(free.isAllowed(Feature.WEB_HOSTING))
        assertEquals(3, free.maxSharedFolders())
    }

    @Test
    fun `pro allows all`() {
        val pro = ProFeatureAccessManager()
        Feature.entries.forEach { assertTrue(pro.isAllowed(it)) }
        assertEquals(Int.MAX_VALUE, pro.maxSharedFolders())
    }
}

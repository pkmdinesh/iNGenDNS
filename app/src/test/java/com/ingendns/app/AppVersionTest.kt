package com.ingendns.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AppVersionTest {
    @Test
    fun `extracts versions from GitHub and flavor names`() {
        assertEquals("11.2", extractAppVersion("v11.2-fdroid"))
        assertEquals("11.1", extractAppVersion("11.1-fdroid"))
    }

    @Test
    fun `compares semantic versions numerically`() {
        assertEquals(1, compareAppVersions("11.10", "11.2-fdroid"))
        assertEquals(0, compareAppVersions("v11.1-fdroid", "11.1-fdroid"))
        assertEquals(-1, compareAppVersions("10.9", "11.1"))
    }
}

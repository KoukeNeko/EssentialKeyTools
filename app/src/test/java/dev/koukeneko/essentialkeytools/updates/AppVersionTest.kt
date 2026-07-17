package dev.koukeneko.essentialkeytools.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppVersionTest {

    @Test
    fun `parses stable and preview project versions`() {
        assertEquals(AppVersion.parse("1.0.8"), AppVersion.parse("v1.0.8"))
        assertEquals(
            AppVersion.parse("1.0.9-pre.37"),
            AppVersion.parse("preview-v1.0.9-pre.37")
        )
    }

    @Test
    fun `orders numeric versions and preview builds correctly`() {
        assertTrue(AppVersion.parse("1.1.0")!! > AppVersion.parse("1.0.99")!!)
        assertTrue(AppVersion.parse("1.0.9-pre.38")!! > AppVersion.parse("1.0.9-pre.37")!!)
        assertTrue(AppVersion.parse("1.0.9")!! > AppVersion.parse("1.0.9-pre.99")!!)
    }

    @Test
    fun `rejects unrelated tag formats`() {
        assertNull(AppVersion.parse("nightly"))
        assertNull(AppVersion.parse("release-v1.0.9"))
        assertNull(AppVersion.parse("v1.0.9-beta.1"))
    }
}

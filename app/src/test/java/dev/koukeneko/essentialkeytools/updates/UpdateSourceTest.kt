package dev.koukeneko.essentialkeytools.updates

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateSourceTest {

    @Test
    fun `preview package always follows GitHub pre-releases`() {
        assertEquals(
            UpdateSource.GITHUB_PREVIEW,
            chooseUpdateSource(
                packageName = "dev.koukeneko.essentialkeytools.preview",
                installingPackageName = "com.android.vending"
            )
        )
    }

    @Test
    fun `Play installed production package uses Google Play`() {
        assertEquals(
            UpdateSource.PLAY_STORE,
            chooseUpdateSource(
                packageName = "dev.koukeneko.essentialkeytools",
                installingPackageName = "com.android.vending"
            )
        )
    }

    @Test
    fun `sideloaded production package follows stable GitHub releases`() {
        assertEquals(
            UpdateSource.GITHUB_STABLE,
            chooseUpdateSource(
                packageName = "dev.koukeneko.essentialkeytools",
                installingPackageName = null
            )
        )
    }
}

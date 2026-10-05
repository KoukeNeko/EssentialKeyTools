package dev.koukeneko.essentialkeytools.unlock

import org.junit.Assert.assertEquals
import org.junit.Test

class PackageToggleCommandTest {

    @Test
    fun disabling_usesTheReversibleDisableUser() {
        val command = packageToggleCommand("com.nothing.ntessentialspace", enabled = false, userId = 0)

        assertEquals(
            listOf("pm", "disable-user", "--user", "0", "com.nothing.ntessentialspace"),
            command
        )
    }

    @Test
    fun enabling_targetsTheGivenUser() {
        val command = packageToggleCommand("com.nothing.ntessentialspace", enabled = true, userId = 10)

        assertEquals(
            listOf("pm", "enable", "--user", "10", "com.nothing.ntessentialspace"),
            command
        )
    }
}

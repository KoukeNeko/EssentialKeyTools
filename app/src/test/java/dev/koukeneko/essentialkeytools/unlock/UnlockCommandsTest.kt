package dev.koukeneko.essentialkeytools.unlock

import org.junit.Assert.assertEquals
import org.junit.Test

class UnlockCommandsTest {

    @Test
    fun disable_usesDisableUserForCurrentUser() {
        val command = UnlockCommands.disable("com.nothing.ntessentialspace")

        assertEquals(
            listOf("pm", "disable-user", "--user", "0", "com.nothing.ntessentialspace"),
            command
        )
    }

    @Test
    fun enable_usesPlainEnable() {
        val command = UnlockCommands.enable("com.nothing.ntessentialrecorder")

        assertEquals(listOf("pm", "enable", "com.nothing.ntessentialrecorder"), command)
    }

    @Test
    fun listPackages_carriesPrefix() {
        val command = UnlockCommands.listPackages("com.nothing")

        assertEquals(listOf("pm", "list", "packages", "com.nothing"), command)
    }

    @Test
    fun parsePackageList_stripsPackagePrefixAndBlankLines() {
        val output = """
            package:com.nothing.ntessentialspace
            package:com.nothing.ntessentialrecorder

        """.trimIndent()

        val packages = UnlockCommands.parsePackageList(output)

        assertEquals(
            listOf("com.nothing.ntessentialspace", "com.nothing.ntessentialrecorder"),
            packages
        )
    }

    @Test
    fun parsePackageList_emptyOutput_isEmptyList() {
        assertEquals(emptyList<String>(), UnlockCommands.parsePackageList(""))
    }
}

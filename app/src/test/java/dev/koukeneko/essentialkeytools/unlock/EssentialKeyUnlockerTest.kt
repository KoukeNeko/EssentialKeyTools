package dev.koukeneko.essentialkeytools.unlock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EssentialKeyUnlockerTest {

    private val space = "com.nothing.ntessentialspace"
    private val recorder = "com.nothing.ntessentialrecorder"

    /** Records the commands it was asked to run and returns a scripted [ShellResult] per package. */
    private class FakeShellRunner(
        private val available: Boolean = true,
        private val resultFor: (List<String>) -> ShellResult = { ShellResult(0, "ok") }
    ) : ShellCommandRunner {
        val commands = mutableListOf<List<String>>()

        override fun isAvailable(): Boolean = available

        override fun run(arguments: List<String>): ShellResult {
            commands.add(arguments)
            return resultFor(arguments)
        }
    }

    /** Serves fixed package states from a map, defaulting to "not installed". */
    private class FakeStateReader(
        private val states: Map<String, PackageState>
    ) : PackageStateReader {
        override fun stateOf(packageName: String): PackageState =
            states[packageName] ?: PackageState.notInstalled(packageName)
    }

    private fun unlocker(
        states: Map<String, PackageState>,
        runner: ShellCommandRunner
    ): EssentialKeyUnlocker =
        EssentialKeyUnlocker(
            stateReader = FakeStateReader(states),
            shellRunner = runner,
            candidatePackages = listOf(space, recorder)
        )

    @Test
    fun freeSinglePress_disablesOnlyEnabledInstalledConsumers() {
        val states = mapOf(
            space to PackageState(space, installed = true, enabled = true),
            recorder to PackageState(recorder, installed = true, enabled = false)
        )
        val runner = FakeShellRunner()
        val result = unlocker(states, runner).freeSinglePress()

        // Only the still-enabled package should be touched, and with disable-user.
        assertEquals(listOf(UnlockCommands.disable(space)), runner.commands)
        assertEquals(1, result.perPackage.size)
        assertTrue(result.allSucceeded)
    }

    @Test
    fun freeSinglePress_skipsUninstalledCandidates() {
        val states = mapOf(space to PackageState(space, installed = true, enabled = true))
        val runner = FakeShellRunner()
        val result = unlocker(states, runner).freeSinglePress()

        assertEquals(listOf(UnlockCommands.disable(space)), runner.commands)
        assertEquals(1, result.perPackage.size)
    }

    @Test
    fun freeSinglePress_reportsPerPackageFailure() {
        val states = mapOf(
            space to PackageState(space, installed = true, enabled = true),
            recorder to PackageState(recorder, installed = true, enabled = true)
        )
        val runner = FakeShellRunner(resultFor = { command ->
            if (command.contains(space)) ShellResult(1, "denied") else ShellResult(0, "ok")
        })
        val result = unlocker(states, runner).freeSinglePress()

        assertTrue(result.anyFailed)
        assertFalse(result.allSucceeded)
        assertEquals(2, result.perPackage.size)
        val spaceResult = result.perPackage.first { it.packageName == space }
        assertFalse(spaceResult.succeeded)
        assertEquals("denied", spaceResult.detail)
    }

    @Test
    fun restoreSinglePress_enablesOnlyDisabledConsumers() {
        val states = mapOf(
            space to PackageState(space, installed = true, enabled = false),
            recorder to PackageState(recorder, installed = true, enabled = true)
        )
        val runner = FakeShellRunner()
        val result = unlocker(states, runner).restoreSinglePress()

        assertEquals(listOf(UnlockCommands.enable(space)), runner.commands)
        assertEquals(1, result.perPackage.size)
        assertTrue(result.allSucceeded)
    }

    @Test
    fun readStatus_reflectsPackageStates() {
        val states = mapOf(
            space to PackageState(space, installed = true, enabled = false),
            recorder to PackageState(recorder, installed = true, enabled = false)
        )
        val result = unlocker(states, FakeShellRunner()).readStatus()

        assertEquals(UnlockStatus.FREED, result)
    }

    @Test
    fun canRunPrivileged_reflectsShellAvailability() {
        val states = emptyMap<String, PackageState>()

        assertTrue(unlocker(states, FakeShellRunner(available = true)).canRunPrivileged())
        assertFalse(unlocker(states, FakeShellRunner(available = false)).canRunPrivileged())
    }
}

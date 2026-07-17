package dev.koukeneko.essentialkeytools.unlock

import org.junit.Assert.assertEquals
import org.junit.Test

class EssentialKeyUnlockerTest {

    private val space = "com.nothing.ntessentialspace"
    private val recorder = "com.nothing.ntessentialrecorder"

    /** Serves fixed package states from a map, defaulting to "not installed". */
    private class FakeStateReader(
        private val states: Map<String, PackageState>
    ) : PackageStateReader {
        override fun stateOf(packageName: String): PackageState =
            states[packageName] ?: PackageState.notInstalled(packageName)
    }

    private fun unlocker(states: Map<String, PackageState>): EssentialKeyUnlocker =
        EssentialKeyUnlocker(
            stateReader = FakeStateReader(states),
            candidatePackages = listOf(space, recorder)
        )

    @Test
    fun readPackageStates_preservesCandidateOrderAndIncludesAbsentPackages() {
        val states = mapOf(
            space to PackageState(space, installed = true, enabled = true),
            recorder to PackageState(recorder, installed = true, enabled = false)
        )

        assertEquals(
            listOf(states.getValue(space), states.getValue(recorder)),
            unlocker(states).readPackageStates()
        )
    }

    @Test
    fun readStatus_reflectsPackageStates() {
        val states = mapOf(
            space to PackageState(space, installed = true, enabled = false),
            recorder to PackageState(recorder, installed = true, enabled = false)
        )
        val result = unlocker(states).readStatus()

        assertEquals(UnlockStatus.FREED, result)
    }
}

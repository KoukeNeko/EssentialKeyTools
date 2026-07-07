package dev.koukeneko.essentialkeytools.unlock

import org.junit.Assert.assertEquals
import org.junit.Test

class UnlockStatusTest {

    private fun consuming(name: String) = PackageState(name, installed = true, enabled = true)
    private fun freed(name: String) = PackageState(name, installed = true, enabled = false)
    private fun absent(name: String) = PackageState.notInstalled(name)

    @Test
    fun noInstalledConsumers_isNoConsumers() {
        val status = UnlockStatus.fromPackageStates(listOf(absent("a"), absent("b")))

        assertEquals(UnlockStatus.NO_CONSUMERS, status)
    }

    @Test
    fun emptyList_isNoConsumers() {
        assertEquals(UnlockStatus.NO_CONSUMERS, UnlockStatus.fromPackageStates(emptyList()))
    }

    @Test
    fun allInstalledConsumersEnabled_isLocked() {
        val status = UnlockStatus.fromPackageStates(listOf(consuming("a"), consuming("b")))

        assertEquals(UnlockStatus.LOCKED, status)
    }

    @Test
    fun allInstalledConsumersDisabled_isFreed() {
        val status = UnlockStatus.fromPackageStates(listOf(freed("a"), freed("b")))

        assertEquals(UnlockStatus.FREED, status)
    }

    @Test
    fun mixedEnabledAndDisabled_isPartiallyFreed() {
        val status = UnlockStatus.fromPackageStates(listOf(freed("a"), consuming("b")))

        assertEquals(UnlockStatus.PARTIALLY_FREED, status)
    }

    @Test
    fun notInstalledPackagesAreIgnoredWhenClassifying() {
        // One freed consumer plus an uninstalled candidate should still read as fully freed.
        val status = UnlockStatus.fromPackageStates(listOf(freed("a"), absent("b")))

        assertEquals(UnlockStatus.FREED, status)
    }
}

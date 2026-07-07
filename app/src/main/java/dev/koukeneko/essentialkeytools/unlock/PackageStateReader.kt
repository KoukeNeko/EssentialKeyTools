package dev.koukeneko.essentialkeytools.unlock

/**
 * Reads the install/enabled state of a package. The production implementation asks
 * [android.content.pm.PackageManager] (no Shizuku needed for status), so the home-screen unlock
 * card works even when Shizuku is absent. Kept as an interface so the unlocker is unit-testable.
 */
interface PackageStateReader {
    fun stateOf(packageName: String): PackageState
}

package dev.koukeneko.essentialkeytools.unlock

/**
 * Reads the install/enabled state of a package. The production implementation asks
 * [android.content.pm.PackageManager]. Kept as an interface so the unlocker is unit-testable.
 */
interface PackageStateReader {
    fun stateOf(packageName: String): PackageState
}

package dev.koukeneko.essentialkeytools.unlock

/**
 * The install/enabled state of one Nothing consumer package on the device, gathered from
 * [android.content.pm.PackageManager] without needing Shizuku. Only [installed] packages carry a
 * meaningful [enabled] value.
 *
 * A package still installed but not enabled has been "freed" — its handler no longer intercepts the
 * Essential Key. A package not installed at all was never a consumer on this model.
 */
data class PackageState(
    val packageName: String,
    val installed: Boolean,
    val enabled: Boolean
) {
    /** True when this package is installed and still actively intercepting the key. */
    val isConsuming: Boolean get() = installed && enabled

    /** True when this package is installed but has been disabled (freed). */
    val isFreed: Boolean get() = installed && !enabled

    companion object {
        fun notInstalled(packageName: String): PackageState =
            PackageState(packageName = packageName, installed = false, enabled = false)
    }
}

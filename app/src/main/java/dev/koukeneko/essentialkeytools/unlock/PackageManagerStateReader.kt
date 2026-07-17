package dev.koukeneko.essentialkeytools.unlock

import android.content.Context
import android.content.pm.PackageManager

/**
 * Reads consumer-package state from the system [PackageManager], allowing the home-screen unlock
 * card to show live status without invoking a privileged command.
 *
 * A package disabled by another app (`disable-user`) reports its enabled setting as one of the
 * disabled states, which is exactly what we treat as "freed".
 */
class PackageManagerStateReader(context: Context) : PackageStateReader {

    private val packageManager: PackageManager = context.applicationContext.packageManager

    override fun stateOf(packageName: String): PackageState {
        if (!isInstalled(packageName)) {
            return PackageState.notInstalled(packageName)
        }
        return PackageState(
            packageName = packageName,
            installed = true,
            enabled = isEnabled(packageName)
        )
    }

    private fun isInstalled(packageName: String): Boolean =
        try {
            packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (error: PackageManager.NameNotFoundException) {
            false
        }

    private fun isEnabled(packageName: String): Boolean =
        when (packageManager.getApplicationEnabledSetting(packageName)) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED -> false
            // DEFAULT and ENABLED both mean the package's own manifest state applies: still active.
            else -> true
        }
}

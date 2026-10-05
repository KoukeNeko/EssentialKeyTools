package dev.koukeneko.essentialkeytools.unlock

import kotlin.system.exitProcess

/**
 * Enables or disables a Nothing consumer package through `pm`. Shizuku instantiates this class by
 * name in its own shell-privileged process, which is why the system-app Disable button being greyed
 * out in App Info does not apply here.
 */
class PackageToggleService : IPackageToggleService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun setPackageEnabled(packageName: String, enabled: Boolean, userId: Int): Boolean {
        // The process holds shell privileges, so it only ever touches the packages this app manages.
        if (packageName !in NothingConsumerPackages.CANDIDATES) {
            return false
        }
        val process = ProcessBuilder(packageToggleCommand(packageName, enabled, userId))
            .redirectErrorStream(true)
            .start()
        // Drain the output so the child cannot block on a full pipe before it exits.
        process.inputStream.bufferedReader().use { reader -> reader.readText() }
        return process.waitFor() == 0
    }
}

/** `disable-user` is the reversible disable that App Info's Disable button performs. */
internal fun packageToggleCommand(packageName: String, enabled: Boolean, userId: Int): List<String> =
    listOf("pm", if (enabled) "enable" else "disable-user", "--user", userId.toString(), packageName)

package dev.koukeneko.essentialkeytools.unlock

/**
 * The outcome of trying to toggle the accessibility service through the privileged shell, so the UI
 * can distinguish "done" from "shell unavailable, fall back to Settings" from "a command failed".
 */
enum class ServiceToggleResult {
    /** The service was enabled/disabled (or was already in the requested state). */
    SUCCEEDED,

    /** No privileged shell is available; the caller should route the user to the settings path. */
    SHELL_UNAVAILABLE,

    /** A shell command ran but reported failure; the caller should offer the settings fallback. */
    COMMAND_FAILED
}

/**
 * Enables or disables our accessibility service by editing the secure settings through a
 * [ShellCommandRunner] (Shizuku-backed at runtime). Reads the current service list first and merges
 * ours in or out, preserving every service the user already enabled.
 *
 * The runner is an interface, so the read-merge-write sequence is fully unit-testable with a fake:
 * the command strings come from [AccessibilityServiceCommands] and the merge from its pure helpers.
 */
class AccessibilityServiceController(
    private val shellRunner: ShellCommandRunner,
    private val serviceComponent: String = OUR_SERVICE_COMPONENT
) {
    /** Whether a privileged shell is available to write the secure settings without a PC. */
    fun canRunPrivileged(): Boolean = shellRunner.isAvailable()

    /**
     * Enables the service: reads the current list, appends ours if absent, writes it back, then
     * flips the master accessibility flag. A no-op merge (already enabled) still reports success.
     */
    fun enable(): ServiceToggleResult {
        if (!shellRunner.isAvailable()) {
            return ServiceToggleResult.SHELL_UNAVAILABLE
        }
        val currentList = readCurrentList() ?: return ServiceToggleResult.COMMAND_FAILED
        val merged = AccessibilityServiceCommands.mergedEnableValue(currentList, serviceComponent)
        // A null merge means our component is already in the list; only the master flag remains.
        if (merged != null && !writeEnabledServices(merged)) {
            return ServiceToggleResult.COMMAND_FAILED
        }
        return if (writeAccessibilityEnabled()) {
            ServiceToggleResult.SUCCEEDED
        } else {
            ServiceToggleResult.COMMAND_FAILED
        }
    }

    /**
     * Disables the service: reads the current list, removes ours if present, and writes it back. A
     * no-op merge (already disabled) still reports success. The master flag is left untouched so any
     * other accessibility service the user relies on keeps working.
     */
    fun disable(): ServiceToggleResult {
        if (!shellRunner.isAvailable()) {
            return ServiceToggleResult.SHELL_UNAVAILABLE
        }
        val currentList = readCurrentList() ?: return ServiceToggleResult.COMMAND_FAILED
        val merged = AccessibilityServiceCommands.mergedDisableValue(currentList, serviceComponent)
            ?: return ServiceToggleResult.SUCCEEDED
        return if (writeEnabledServices(merged)) {
            ServiceToggleResult.SUCCEEDED
        } else {
            ServiceToggleResult.COMMAND_FAILED
        }
    }

    private fun readCurrentList(): String? {
        val result = shellRunner.run(AccessibilityServiceCommands.getEnabledServices())
        return if (result.isSuccess) result.output else null
    }

    private fun writeEnabledServices(value: String): Boolean =
        shellRunner.run(AccessibilityServiceCommands.putEnabledServices(value)).isSuccess

    private fun writeAccessibilityEnabled(): Boolean =
        shellRunner.run(AccessibilityServiceCommands.putAccessibilityEnabled()).isSuccess

    companion object {
        private const val APP_ID = "dev.koukeneko.essentialkeytools"
        private const val SERVICE_CLASS =
            "dev.koukeneko.essentialkeytools.service.EssentialKeyDetectionService"

        /** The flattened `package/class` component id Android stores in the enabled-services list. */
        const val OUR_SERVICE_COMPONENT = "$APP_ID/$SERVICE_CLASS"
    }
}

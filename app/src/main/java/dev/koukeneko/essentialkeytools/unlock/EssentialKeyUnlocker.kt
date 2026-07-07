package dev.koukeneko.essentialkeytools.unlock

/**
 * Orchestrates freeing and restoring the Essential Key's single press. Reads consumer-package state
 * through a [PackageStateReader] (works without Shizuku) and runs the privileged `pm` commands
 * through a [ShellCommandRunner] (Shizuku-backed at runtime).
 *
 * Both collaborators are interfaces, so this class holds the pure decision logic and is fully
 * unit-testable with fakes. Android and Shizuku live only in the concrete implementations.
 */
class EssentialKeyUnlocker(
    private val stateReader: PackageStateReader,
    private val shellRunner: ShellCommandRunner,
    private val candidatePackages: List<String> = NothingConsumerPackages.CANDIDATES
) {
    /** State of every candidate consumer package, in candidate order. */
    fun readPackageStates(): List<PackageState> =
        candidatePackages.map { packageName -> stateReader.stateOf(packageName) }

    /** The overall unlock status derived from the current package states. */
    fun readStatus(): UnlockStatus =
        UnlockStatus.fromPackageStates(readPackageStates())

    /** Whether a privileged shell is available to actually free or restore packages. */
    fun canRunPrivileged(): Boolean = shellRunner.isAvailable()

    /**
     * Frees every installed consumer that is still enabled. Packages already freed or not installed
     * are skipped rather than reported as failures.
     */
    fun freeSinglePress(): UnlockRunResult =
        runOverInstalledConsumers(shouldAct = { state -> state.isConsuming }) { packageName ->
            shellRunner.run(UnlockCommands.disable(packageName))
        }

    /**
     * Restores every installed consumer that is currently disabled. Packages already enabled or not
     * installed are skipped.
     */
    fun restoreSinglePress(): UnlockRunResult =
        runOverInstalledConsumers(shouldAct = { state -> state.isFreed }) { packageName ->
            shellRunner.run(UnlockCommands.enable(packageName))
        }

    private fun runOverInstalledConsumers(
        shouldAct: (PackageState) -> Boolean,
        command: (String) -> ShellResult
    ): UnlockRunResult {
        val results = readPackageStates()
            .filter { state -> state.installed && shouldAct(state) }
            .map { state -> toResult(state.packageName, command(state.packageName)) }
        return UnlockRunResult(perPackage = results)
    }

    private fun toResult(packageName: String, shellResult: ShellResult): PackageActionResult =
        PackageActionResult(
            packageName = packageName,
            succeeded = shellResult.isSuccess,
            detail = shellResult.output.trim()
        )
}

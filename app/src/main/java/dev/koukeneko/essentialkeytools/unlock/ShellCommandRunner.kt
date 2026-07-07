package dev.koukeneko.essentialkeytools.unlock

/** The outcome of running one shell command: its exit code and captured output. */
data class ShellResult(
    val exitCode: Int,
    val output: String
) {
    val isSuccess: Boolean get() = exitCode == SUCCESS_EXIT_CODE

    companion object {
        const val SUCCESS_EXIT_CODE = 0
    }
}

/**
 * Runs a privileged shell command (e.g. `pm disable-user`). The Shizuku-backed implementation lives
 * in [ShizukuShellCommandRunner]; keeping this an interface lets the unlock orchestration be tested
 * with a fake runner and no Shizuku on the classpath at test time.
 */
interface ShellCommandRunner {

    /** Whether a privileged shell is currently available (Shizuku running and permission granted). */
    fun isAvailable(): Boolean

    /**
     * Executes [command] split into its already-tokenised [arguments] and returns the result.
     * Implementations must not throw for a non-zero exit; they surface it via [ShellResult].
     */
    fun run(arguments: List<String>): ShellResult
}

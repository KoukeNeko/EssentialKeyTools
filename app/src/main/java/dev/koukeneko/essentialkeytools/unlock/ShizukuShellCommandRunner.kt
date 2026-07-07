package dev.koukeneko.essentialkeytools.unlock

import android.util.Log

/**
 * Runs privileged shell commands through Shizuku's `newProcess`, which starts the process with the
 * ADB-shell (or root) identity Shizuku holds. That identity is what lets `pm disable-user` touch a
 * system package this app does not own.
 *
 * `Shizuku.newProcess` is a hidden static method, so it is invoked reflectively. This keeps the app
 * compiling against the public API artifact while still reaching the one entry point we need, and
 * degrades to a failed [ShellResult] (never a crash) if the method is missing on some Shizuku build.
 */
class ShizukuShellCommandRunner : ShellCommandRunner {

    override fun isAvailable(): Boolean =
        ShizukuGate.availability() == ShizukuAvailability.READY

    override fun run(arguments: List<String>): ShellResult {
        if (!isAvailable()) {
            return ShellResult(exitCode = SHELL_UNAVAILABLE_EXIT_CODE, output = "Shizuku not ready")
        }
        return try {
            executeViaShizuku(arguments)
        } catch (error: ReflectiveOperationException) {
            Log.w(TAG, "Shizuku newProcess unavailable", error)
            ShellResult(exitCode = SHELL_UNAVAILABLE_EXIT_CODE, output = describe(error))
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            Log.w(TAG, "Interrupted waiting for shell command", error)
            ShellResult(exitCode = SHELL_UNAVAILABLE_EXIT_CODE, output = describe(error))
        } catch (error: java.io.IOException) {
            Log.w(TAG, "Failed reading shell command output", error)
            ShellResult(exitCode = SHELL_UNAVAILABLE_EXIT_CODE, output = describe(error))
        }
    }

    private fun executeViaShizuku(arguments: List<String>): ShellResult {
        val process = newShizukuProcess(arguments)
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val errorOutput = process.errorStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        val combined = listOf(output, errorOutput)
            .filter { text -> text.isNotBlank() }
            .joinToString(separator = "\n")
            .trim()
        return ShellResult(exitCode = exitCode, output = combined)
    }

    /**
     * Reflectively calls `Shizuku.newProcess(String[] cmd, String[] env, String dir)`. Returns a
     * [Process] because the concrete `ShizukuRemoteProcess` extends it and we only need the base API.
     */
    private fun newShizukuProcess(arguments: List<String>): Process {
        val shizukuClass = Class.forName(SHIZUKU_CLASS_NAME)
        val newProcess = shizukuClass.getDeclaredMethod(
            NEW_PROCESS_METHOD,
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        newProcess.isAccessible = true
        val result = newProcess.invoke(null, arguments.toTypedArray(), null, null)
            ?: throw ReflectiveOperationException("newProcess returned null")
        return result as Process
    }

    private fun describe(error: Throwable): String =
        error.message ?: error.javaClass.simpleName

    private companion object {
        const val TAG = "ShizukuShellRunner"
        const val SHIZUKU_CLASS_NAME = "rikka.shizuku.Shizuku"
        const val NEW_PROCESS_METHOD = "newProcess"

        // Non-zero, distinct from any real pm exit code, marks "we never reached the shell".
        const val SHELL_UNAVAILABLE_EXIT_CODE = -1
    }
}

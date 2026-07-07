package dev.koukeneko.essentialkeytools.unlock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityServiceControllerTest {

    private val ourService = AccessibilityServiceController.OUR_SERVICE_COMPONENT
    private val otherService = "com.example.other/com.example.other.SomeService"

    /** Serves scripted results per command and records every command it was asked to run. */
    private class FakeShellRunner(
        private val available: Boolean = true,
        private val resultFor: (List<String>) -> ShellResult
    ) : ShellCommandRunner {
        val commands = mutableListOf<List<String>>()

        override fun isAvailable(): Boolean = available

        override fun run(arguments: List<String>): ShellResult {
            commands.add(arguments)
            return resultFor(arguments)
        }
    }

    private fun isGet(command: List<String>): Boolean = command.contains("get")
    private fun isPutServices(command: List<String>): Boolean =
        command.contains("put") && command.contains(AccessibilityServiceCommands.ENABLED_SERVICES_KEY)
    private fun isPutFlag(command: List<String>): Boolean =
        command.contains("put") && command.contains(AccessibilityServiceCommands.ACCESSIBILITY_ENABLED_KEY)

    private fun writtenServiceValue(runner: FakeShellRunner): String? =
        runner.commands.firstOrNull { isPutServices(it) }?.last()

    @Test
    fun enable_appendsToEmptyListAndSetsFlag() {
        val runner = FakeShellRunner(resultFor = { command ->
            if (isGet(command)) ShellResult(0, "null") else ShellResult(0, "")
        })

        val result = AccessibilityServiceController(runner).enable()

        assertEquals(ServiceToggleResult.SUCCEEDED, result)
        assertEquals(ourService, writtenServiceValue(runner))
        assertTrue(runner.commands.any { isPutFlag(it) })
    }

    @Test
    fun enable_preservesExistingServices() {
        val runner = FakeShellRunner(resultFor = { command ->
            if (isGet(command)) ShellResult(0, otherService) else ShellResult(0, "")
        })

        AccessibilityServiceController(runner).enable()

        assertEquals("$otherService:$ourService", writtenServiceValue(runner))
    }

    @Test
    fun enable_alreadyPresentSkipsListWriteButStillSetsFlag() {
        val runner = FakeShellRunner(resultFor = { command ->
            if (isGet(command)) ShellResult(0, "$otherService:$ourService") else ShellResult(0, "")
        })

        val result = AccessibilityServiceController(runner).enable()

        assertEquals(ServiceToggleResult.SUCCEEDED, result)
        // Our component was already enabled, so no list write should have happened.
        assertTrue(runner.commands.none { isPutServices(it) })
        assertTrue(runner.commands.any { isPutFlag(it) })
    }

    @Test
    fun enable_shellUnavailableReportsFallback() {
        val runner = FakeShellRunner(available = false, resultFor = { ShellResult(0, "") })

        assertEquals(ServiceToggleResult.SHELL_UNAVAILABLE, AccessibilityServiceController(runner).enable())
        assertTrue(runner.commands.isEmpty())
    }

    @Test
    fun enable_getFailureReportsCommandFailed() {
        val runner = FakeShellRunner(resultFor = { command ->
            if (isGet(command)) ShellResult(1, "denied") else ShellResult(0, "")
        })

        assertEquals(ServiceToggleResult.COMMAND_FAILED, AccessibilityServiceController(runner).enable())
    }

    @Test
    fun enable_putFailureReportsCommandFailed() {
        val runner = FakeShellRunner(resultFor = { command ->
            when {
                isGet(command) -> ShellResult(0, "null")
                isPutServices(command) -> ShellResult(1, "denied")
                else -> ShellResult(0, "")
            }
        })

        assertEquals(ServiceToggleResult.COMMAND_FAILED, AccessibilityServiceController(runner).enable())
    }

    @Test
    fun disable_removesOurServiceAndLeavesOthers() {
        val runner = FakeShellRunner(resultFor = { command ->
            if (isGet(command)) ShellResult(0, "$otherService:$ourService") else ShellResult(0, "")
        })

        val result = AccessibilityServiceController(runner).disable()

        assertEquals(ServiceToggleResult.SUCCEEDED, result)
        assertEquals(otherService, writtenServiceValue(runner))
        // Disable must not touch the master flag — other services may still need it on.
        assertTrue(runner.commands.none { isPutFlag(it) })
    }

    @Test
    fun disable_notPresentSucceedsWithoutWrite() {
        val runner = FakeShellRunner(resultFor = { command ->
            if (isGet(command)) ShellResult(0, otherService) else ShellResult(0, "")
        })

        val result = AccessibilityServiceController(runner).disable()

        assertEquals(ServiceToggleResult.SUCCEEDED, result)
        assertTrue(runner.commands.none { isPutServices(it) })
    }

    @Test
    fun disable_shellUnavailableReportsFallback() {
        val runner = FakeShellRunner(available = false, resultFor = { ShellResult(0, "") })

        assertEquals(ServiceToggleResult.SHELL_UNAVAILABLE, AccessibilityServiceController(runner).disable())
    }
}

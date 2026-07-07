package dev.koukeneko.essentialkeytools.unlock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccessibilityServiceCommandsTest {

    private val ourService =
        "dev.koukeneko.essentialkeytools/dev.koukeneko.essentialkeytools.service.EssentialKeyDetectionService"
    private val otherService = "com.example.other/com.example.other.SomeService"

    @Test
    fun getEnabledServices_readsSecureKey() {
        assertEquals(
            listOf("settings", "get", "secure", "enabled_accessibility_services"),
            AccessibilityServiceCommands.getEnabledServices()
        )
    }

    @Test
    fun putEnabledServices_writesGivenValue() {
        assertEquals(
            listOf("settings", "put", "secure", "enabled_accessibility_services", ourService),
            AccessibilityServiceCommands.putEnabledServices(ourService)
        )
    }

    @Test
    fun putAccessibilityEnabled_flipsMasterFlag() {
        assertEquals(
            listOf("settings", "put", "secure", "accessibility_enabled", "1"),
            AccessibilityServiceCommands.putAccessibilityEnabled()
        )
    }

    @Test
    fun parseEnabledServices_nullLiteralIsEmpty() {
        assertEquals(emptyList<String>(), AccessibilityServiceCommands.parseEnabledServices("null"))
    }

    @Test
    fun parseEnabledServices_blankIsEmpty() {
        assertEquals(emptyList<String>(), AccessibilityServiceCommands.parseEnabledServices("  \n"))
    }

    @Test
    fun parseEnabledServices_splitsAndTrims() {
        val raw = " $otherService : $ourService \n"

        assertEquals(
            listOf(otherService, ourService),
            AccessibilityServiceCommands.parseEnabledServices(raw)
        )
    }

    @Test
    fun parseEnabledServices_dropsEmptyEntries() {
        assertEquals(
            listOf(otherService),
            AccessibilityServiceCommands.parseEnabledServices("$otherService::")
        )
    }

    @Test
    fun appendService_appendsToEmpty() {
        assertEquals(
            listOf(ourService),
            AccessibilityServiceCommands.appendService(emptyList(), ourService)
        )
    }

    @Test
    fun appendService_preservesExistingEntries() {
        assertEquals(
            listOf(otherService, ourService),
            AccessibilityServiceCommands.appendService(listOf(otherService), ourService)
        )
    }

    @Test
    fun appendService_alreadyPresentIsNoOp() {
        val current = listOf(otherService, ourService)

        assertEquals(current, AccessibilityServiceCommands.appendService(current, ourService))
    }

    @Test
    fun removeService_removesOnlyTarget() {
        assertEquals(
            listOf(otherService),
            AccessibilityServiceCommands.removeService(listOf(otherService, ourService), ourService)
        )
    }

    @Test
    fun mergedEnableValue_appendToEmptyNull() {
        assertEquals(
            ourService,
            AccessibilityServiceCommands.mergedEnableValue("null", ourService)
        )
    }

    @Test
    fun mergedEnableValue_appendToExistingPreservesList() {
        assertEquals(
            "$otherService:$ourService",
            AccessibilityServiceCommands.mergedEnableValue(otherService, ourService)
        )
    }

    @Test
    fun mergedEnableValue_alreadyPresentReturnsNull() {
        assertNull(
            AccessibilityServiceCommands.mergedEnableValue("$otherService:$ourService", ourService)
        )
    }

    @Test
    fun mergedDisableValue_removesFromList() {
        assertEquals(
            otherService,
            AccessibilityServiceCommands.mergedDisableValue("$otherService:$ourService", ourService)
        )
    }

    @Test
    fun mergedDisableValue_lastEntryLeavesEmptyString() {
        assertEquals(
            "",
            AccessibilityServiceCommands.mergedDisableValue(ourService, ourService)
        )
    }

    @Test
    fun mergedDisableValue_notPresentReturnsNull() {
        assertNull(AccessibilityServiceCommands.mergedDisableValue(otherService, ourService))
    }
}

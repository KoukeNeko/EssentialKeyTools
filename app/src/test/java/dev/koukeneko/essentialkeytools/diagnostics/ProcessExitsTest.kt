package dev.koukeneko.essentialkeytools.diagnostics

import android.app.ApplicationExitInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessExitsTest {

    @Test
    fun namesTheReasonsWorthActingOn() {
        assertEquals("ANR", exitReasonLabel(ApplicationExitInfo.REASON_ANR))
        assertEquals("Crash", exitReasonLabel(ApplicationExitInfo.REASON_CRASH))
        assertEquals("Native crash", exitReasonLabel(ApplicationExitInfo.REASON_CRASH_NATIVE))
        assertEquals("Low memory", exitReasonLabel(ApplicationExitInfo.REASON_LOW_MEMORY))
    }

    @Test
    fun keepsAnUnrecognisedReasonReadable() {
        val label = exitReasonLabel(9999)

        assertTrue(label.contains("9999"))
    }

    @Test
    fun formatsWithAndWithoutADescription() {
        val described = ProcessExit("2026-09-01T13:42:03Z", "ANR", "Input dispatching timed out")
        val bare = ProcessExit("2026-09-01T13:42:03Z", "Crash", null)

        assertEquals(
            "2026-09-01T13:42:03Z  ANR  —  Input dispatching timed out",
            formatProcessExit(described)
        )
        assertEquals("2026-09-01T13:42:03Z  Crash", formatProcessExit(bare))
    }
}

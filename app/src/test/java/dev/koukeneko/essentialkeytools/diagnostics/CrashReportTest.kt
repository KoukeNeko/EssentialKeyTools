package dev.koukeneko.essentialkeytools.diagnostics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportTest {

    private fun reportWith(stackTrace: String) = CrashReport(
        appVersionName = "1.0.10",
        appVersionCode = 10010,
        installSource = "github_stable",
        manufacturer = "Nothing",
        model = "A063",
        androidRelease = "16",
        androidSdk = 36,
        occurredAt = "2026-09-01T13:42:03Z",
        stackTrace = stackTrace
    )

    @Test
    fun carriesEveryFactNeededToReproduce() {
        val formatted = formatCrashReport(reportWith("java.lang.IllegalStateException"))

        for (fact in listOf("1.0.10", "10010", "github_stable", "Nothing A063", "SDK 36",
            "2026-09-01T13:42:03Z", "java.lang.IllegalStateException")) {
            assertTrue("report is missing $fact", formatted.contains(fact))
        }
    }

    @Test
    fun longStackTraceIsCappedAndMarked() {
        val formatted = formatCrashReport(reportWith("x".repeat(100_000)))

        assertTrue(formatted.length < 30_000)
        assertTrue(formatted.contains("truncated"))
    }

    @Test
    fun shortStackTraceIsLeftIntact() {
        val trace = "java.lang.SecurityException: denied\n\tat Foo.bar(Foo.kt:12)"

        assertTrue(formatCrashReport(reportWith(trace)).contains(trace))
        assertFalse(formatCrashReport(reportWith(trace)).contains("truncated"))
    }
}

package dev.koukeneko.essentialkeytools.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ISSUES_URL = "https://github.com/KoukeNeko/EssentialKeyTools/issues"

class CrashIssueTest {

    private fun reportOf(stackTrace: String) = formatCrashReport(
        CrashReport(
            appVersionName = "1.0.11",
            appVersionCode = 10011,
            installSource = "github_stable",
            manufacturer = "Nothing",
            model = "A063",
            androidRelease = "16",
            androidSdk = 36,
            occurredAt = "2026-09-01T13:42:03Z",
            stackTrace = stackTrace
        )
    )

    @Test
    fun titlesTheIssueWithTheException() {
        val report = reportOf("java.lang.SecurityException: denied\n\tat Foo.bar(Foo.kt:12)")

        val url = buildCrashIssueUrl(ISSUES_URL, report)

        assertTrue(url.startsWith("$ISSUES_URL/new?title="))
        assertTrue(url.contains("SecurityException"))
    }

    @Test
    fun bodyCarriesTheBuildAndDeviceFacts() {
        val url = buildCrashIssueUrl(ISSUES_URL, reportOf("java.lang.IllegalStateException"))

        for (fact in listOf("1.0.11", "Nothing", "A063", "36")) {
            assertTrue("issue body is missing $fact", url.contains(fact))
        }
    }

    @Test
    fun dropsTheBodyRatherThanEmitAnOverlongUrl() {
        val hugeHeader = "x".repeat(20_000)

        val url = buildCrashIssueUrl(ISSUES_URL, "title\n\n$hugeHeader\n\njava.lang.Error")

        assertTrue(url.length <= 6_000)
        assertFalse(url.contains("body="))
    }

    @Test
    fun fallsBackToAPlainTitleWhenTheReportIsUnrecognisable() {
        val url = buildCrashIssueUrl(ISSUES_URL, "not a report this app wrote")

        assertTrue(url.contains("Crash+report"))
    }

    @Test
    fun readsTheSummaryAndHeaderBackOutOfItsOwnFormat() {
        val report = reportOf("java.lang.SecurityException: denied\n\tat Foo.bar(Foo.kt:12)")

        assertEquals("java.lang.SecurityException: denied", crashSummary(report))
        assertTrue(crashHeader(report).contains("App:      1.0.11 (10011)"))
        assertNull(crashSummary("no blank lines here"))
    }
}

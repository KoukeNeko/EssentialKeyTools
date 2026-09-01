package dev.koukeneko.essentialkeytools.diagnostics

import java.net.URLEncoder

/**
 * Query strings are not a transport for a stack trace: browsers and servers cut long URLs off, and
 * a truncated report is worse than none. The full report travels on the clipboard, so this stays
 * small enough to survive any client.
 */
private const val MAX_ISSUE_URL_CHARS = 6_000
private const val MAX_TITLE_CHARS = 90
private const val DEFAULT_TITLE = "Crash report"
private const val UTF_8 = "UTF-8"

// The issue tracker is English, so the template a reporter lands in is too.
private const val ISSUE_BODY_TEMPLATE = """## What happened

<!-- What were you doing when the app crashed? -->

## Diagnostic report

%s

<!-- The full report was copied to your clipboard. Paste it below this line. -->
"""

/**
 * Builds the prefilled "new issue" URL for a crash. The title names the exception so similar
 * reports group visibly, and the body carries the short build and device facts, which are useful
 * even from a reporter who never pastes the rest.
 */
fun buildCrashIssueUrl(issuesBaseUrl: String, report: String): String {
    val title = crashSummary(report)?.take(MAX_TITLE_CHARS) ?: DEFAULT_TITLE
    val body = ISSUE_BODY_TEMPLATE.format(crashHeader(report))
    val url = "$issuesBaseUrl/new?title=${encode(title)}&body=${encode(body)}"
    return if (url.length <= MAX_ISSUE_URL_CHARS) {
        url
    } else {
        "$issuesBaseUrl/new?title=${encode(title)}"
    }
}

private fun encode(value: String): String = URLEncoder.encode(value, UTF_8)

/**
 * The first line of the stack trace — the exception class and message — or null when [report] does
 * not look like something this app formatted.
 */
internal fun crashSummary(report: String): String? =
    report.split(REPORT_BLOCK_SEPARATOR)
        .getOrNull(STACK_TRACE_BLOCK)
        ?.lineSequence()
        ?.firstOrNull()
        ?.trim()
        ?.takeIf { line -> line.isNotEmpty() }

/** The build and device block: short, safe to put in a URL, and enough to triage from. */
internal fun crashHeader(report: String): String =
    report.split(REPORT_BLOCK_SEPARATOR).getOrNull(HEADER_BLOCK).orEmpty().trim()

private const val REPORT_BLOCK_SEPARATOR = "\n\n"
private const val HEADER_BLOCK = 1
private const val STACK_TRACE_BLOCK = 2

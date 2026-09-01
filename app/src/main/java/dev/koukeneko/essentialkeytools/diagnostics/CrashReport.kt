package dev.koukeneko.essentialkeytools.diagnostics

/** Longest stack trace kept. A dying process should write a small file, not a megabyte of text. */
private const val MAX_STACK_TRACE_CHARS = 16_000
private const val TRUNCATION_MARKER = "\n… stack trace truncated"

/**
 * The facts a crash report carries: fixed build and device fields plus the throwable's own stack.
 *
 * The field list is deliberately closed. This app runs an accessibility service that observes window
 * content and key presses, and none of that may ever reach a report the user is invited to paste
 * into a public issue — so nothing here is sourced from the service.
 */
data class CrashReport(
    val appVersionName: String,
    val appVersionCode: Long,
    val installSource: String,
    val manufacturer: String,
    val model: String,
    val androidRelease: String,
    val androidSdk: Int,
    val occurredAt: String,
    val stackTrace: String
)

/** Renders [report] as the plain text block the user copies into a bug report. */
fun formatCrashReport(report: CrashReport): String = buildString {
    appendLine("Essential Key Tools — crash report")
    appendLine()
    appendLine("App:      ${report.appVersionName} (${report.appVersionCode})")
    appendLine("Source:   ${report.installSource}")
    appendLine("Device:   ${report.manufacturer} ${report.model}")
    appendLine("Android:  ${report.androidRelease} (SDK ${report.androidSdk})")
    appendLine("Occurred: ${report.occurredAt}")
    appendLine()
    append(truncateStackTrace(report.stackTrace))
}

/** Caps a stack trace at [MAX_STACK_TRACE_CHARS], marking the cut so nobody reads it as complete. */
internal fun truncateStackTrace(stackTrace: String): String =
    if (stackTrace.length <= MAX_STACK_TRACE_CHARS) {
        stackTrace
    } else {
        stackTrace.take(MAX_STACK_TRACE_CHARS) + TRUNCATION_MARKER
    }

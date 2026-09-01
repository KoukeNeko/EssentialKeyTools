package dev.koukeneko.essentialkeytools.diagnostics

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import java.time.Instant

/** How many past exits to show. Enough to cover a bad afternoon, not a history. */
private const val MAX_EXITS_SHOWN = 5

/**
 * One process exit as recorded by Android itself. This is the half of the picture the app's own
 * handler cannot see: a Java crash arrives through [CrashReportHandler], but ANRs and kills never
 * run app code on the way out.
 */
data class ProcessExit(
    val occurredAt: String,
    val reason: String,
    val description: String?
)

/** Renders one exit as a single line for the diagnostics screen. */
fun formatProcessExit(exit: ProcessExit): String {
    val description = exit.description
    return if (description.isNullOrBlank()) {
        "${exit.occurredAt}  ${exit.reason}"
    } else {
        "${exit.occurredAt}  ${exit.reason}  —  $description"
    }
}

/**
 * Reads the exits Android recorded for this app. Involves a binder call, so callers should keep it
 * off the main thread. Returns an empty list rather than throwing: a diagnostics screen that
 * crashes while reporting crashes would be a poor joke.
 */
fun readRecentProcessExits(context: Context): List<ProcessExit> {
    val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return emptyList()
    return runCatching {
        activityManager
            .getHistoricalProcessExitReasons(context.packageName, 0, MAX_EXITS_SHOWN)
            .map { exitInfo ->
                ProcessExit(
                    occurredAt = Instant.ofEpochMilli(exitInfo.timestamp).toString(),
                    reason = exitReasonLabel(exitInfo.reason),
                    description = exitInfo.description
                )
            }
    }.getOrDefault(emptyList())
}

/**
 * Names an [ApplicationExitInfo] reason. The constants are compile-time ints, so the mapping is
 * unit-testable without a device.
 */
internal fun exitReasonLabel(reason: Int): String = when (reason) {
    ApplicationExitInfo.REASON_ANR -> "ANR"
    ApplicationExitInfo.REASON_CRASH -> "Crash"
    ApplicationExitInfo.REASON_CRASH_NATIVE -> "Native crash"
    ApplicationExitInfo.REASON_LOW_MEMORY -> "Low memory"
    ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "Excessive resource usage"
    ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "Permission change"
    ApplicationExitInfo.REASON_USER_REQUESTED -> "User requested"
    ApplicationExitInfo.REASON_USER_STOPPED -> "User stopped"
    ApplicationExitInfo.REASON_SIGNALED -> "Signalled"
    ApplicationExitInfo.REASON_EXIT_SELF -> "Exited itself"
    ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "Dependency died"
    ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "Initialisation failure"
    ApplicationExitInfo.REASON_OTHER -> "Other"
    else -> "Unknown ($reason)"
}

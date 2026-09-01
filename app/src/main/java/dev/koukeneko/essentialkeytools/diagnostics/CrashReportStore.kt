package dev.koukeneko.essentialkeytools.diagnostics

import android.content.Context
import java.io.File

private const val CRASH_REPORT_FILE_NAME = "last_crash_report.txt"

/**
 * Keeps the most recent crash report in app-private storage. Only the last one is kept: this exists
 * so a user can hand over what just happened, not as a history.
 *
 * A plain file rather than the DataStore the rest of the app uses, because the writer runs inside a
 * process that is already dying — there is no time for an asynchronous, coroutine-based write.
 *
 * Every operation reports success rather than throwing, so neither a crashing process nor the
 * diagnostics screen can be brought down by a storage failure.
 */
class CrashReportStore(private val reportFile: File) {

    fun write(report: String): Boolean = runCatching { reportFile.writeText(report) }.isSuccess

    fun read(): String? =
        runCatching { if (reportFile.exists()) reportFile.readText() else null }.getOrNull()

    fun clear(): Boolean = runCatching { !reportFile.exists() || reportFile.delete() }
        .getOrDefault(false)

    companion object {
        fun create(context: Context): CrashReportStore =
            CrashReportStore(File(context.filesDir, CRASH_REPORT_FILE_NAME))
    }
}

package dev.koukeneko.essentialkeytools.diagnostics

import android.content.Context
import android.os.Build
import android.util.Log
import dev.koukeneko.essentialkeytools.updates.chooseUpdateSource
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val TAG = "CrashReporting"
private const val UNKNOWN_VERSION_NAME = "unknown"

/**
 * Installs on-device crash capture. Everything stays on the device: the report is written to
 * app-private storage and only leaves if the user chooses to share it from the diagnostics screen.
 * That is what keeps this free of any consent, disclosure, or Play data-safety obligation.
 *
 * Build and device facts are snapshotted here, at startup, rather than looked up while the process
 * is dying — leaving only the timestamp and the stack trace to be gathered at crash time.
 */
fun installCrashReporting(context: Context) {
    val appContext = context.applicationContext
    val store = CrashReportStore.create(appContext)
    val snapshot = readBuildSnapshot(appContext)

    Thread.setDefaultUncaughtExceptionHandler(
        CrashReportHandler(
            recordCrash = { error -> store.write(formatCrashReport(snapshot.toReport(error))) },
            onRecordingFailed = { failure ->
                Log.w(TAG, "Could not record the crash report", failure)
            },
            delegate = Thread.getDefaultUncaughtExceptionHandler()
        )
    )
}

/** The parts of a report that never change while the process lives. */
private data class BuildSnapshot(
    val appVersionName: String,
    val appVersionCode: Long,
    val installSource: String
) {
    fun toReport(error: Throwable) = CrashReport(
        appVersionName = appVersionName,
        appVersionCode = appVersionCode,
        installSource = installSource,
        manufacturer = Build.MANUFACTURER,
        model = Build.MODEL,
        androidRelease = Build.VERSION.RELEASE,
        androidSdk = Build.VERSION.SDK_INT,
        occurredAt = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
        stackTrace = error.stackTraceToString()
    )
}

private fun readBuildSnapshot(context: Context): BuildSnapshot {
    val packageInfo = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }.getOrNull()
    val installingPackageName = runCatching {
        context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
    }.getOrNull()

    return BuildSnapshot(
        appVersionName = packageInfo?.versionName ?: UNKNOWN_VERSION_NAME,
        appVersionCode = packageInfo?.longVersionCode ?: 0L,
        // Reuses the update checker's mapping so "where did this build come from" has one answer.
        installSource = chooseUpdateSource(context.packageName, installingPackageName)
            .name
            .lowercase()
    )
}

package dev.koukeneko.essentialkeytools.diagnostics

/**
 * Records a crash on its way out, then hands the throwable to [delegate].
 *
 * Delegating is mandatory, not politeness: the platform handler is what terminates the process and
 * what feeds Google Play's Android vitals. Swallowing the exception here would leave the app hung
 * and blind the one crash channel that already works.
 */
class CrashReportHandler(
    private val recordCrash: (Throwable) -> Unit,
    private val onRecordingFailed: (Throwable) -> Unit,
    private val delegate: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, error: Throwable) {
        try {
            recordCrash(error)
        } catch (recordingFailure: Throwable) {
            // Catching Throwable is deliberate: an OutOfMemoryError while writing diagnostics must
            // not stop the platform handler from running.
            onRecordingFailed(recordingFailure)
        }
        delegate?.uncaughtException(thread, error)
    }
}

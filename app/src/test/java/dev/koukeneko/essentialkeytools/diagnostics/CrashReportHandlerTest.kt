package dev.koukeneko.essentialkeytools.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportHandlerTest {

    private class RecordingDelegate : Thread.UncaughtExceptionHandler {
        var received: Throwable? = null
        override fun uncaughtException(thread: Thread, error: Throwable) {
            received = error
        }
    }

    @Test
    fun recordsTheCrashThenDelegates() {
        val recorded = mutableListOf<Throwable>()
        val delegate = RecordingDelegate()
        val crash = IllegalStateException("boom")
        val handler = CrashReportHandler(
            recordCrash = { error -> recorded.add(error) },
            onRecordingFailed = { failure -> throw AssertionError("unexpected", failure) },
            delegate = delegate
        )

        handler.uncaughtException(Thread.currentThread(), crash)

        assertEquals(listOf<Throwable>(crash), recorded)
        assertSame(crash, delegate.received)
    }

    @Test
    fun stillDelegatesWhenRecordingFails() {
        val delegate = RecordingDelegate()
        val crash = IllegalStateException("boom")
        val failures = mutableListOf<Throwable>()
        val handler = CrashReportHandler(
            recordCrash = { throw OutOfMemoryError("no room for diagnostics") },
            onRecordingFailed = { failure -> failures.add(failure) },
            delegate = delegate
        )

        handler.uncaughtException(Thread.currentThread(), crash)

        assertSame("the platform handler must still run", crash, delegate.received)
        assertTrue(failures.single() is OutOfMemoryError)
    }

    @Test
    fun survivesAMissingPlatformHandler() {
        val handler = CrashReportHandler(
            recordCrash = {},
            onRecordingFailed = {},
            delegate = null
        )

        handler.uncaughtException(Thread.currentThread(), IllegalStateException("boom"))
    }
}

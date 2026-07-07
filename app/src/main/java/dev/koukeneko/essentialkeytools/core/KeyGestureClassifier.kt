package dev.koukeneko.essentialkeytools.core

/**
 * Pure-Kotlin state machine that turns raw key up/down events for a single physical key into a
 * [KeyGesture]. It has zero Android dependencies so the timing logic is unit-testable on the JVM;
 * the accessibility service in a later phase feeds it events after scanCode filtering.
 *
 * Timing is driven through an injected [GestureScheduler] rather than real timers so tests can
 * advance a virtual clock deterministically. Results are delivered via [onGesture].
 *
 * @param enabledGestures which gestures the current configuration cares about. Disabling the
 *   multi-tap gestures lets the classifier resolve simpler gestures immediately, cutting latency.
 */
class KeyGestureClassifier(
    private val enabledGestures: Set<KeyGesture>,
    private val scheduler: GestureScheduler,
    private val onGesture: (KeyGesture) -> Unit
) {

    companion object {
        /** A press held at least this long is a long press and never counts as a tap. */
        const val LONG_PRESS_THRESHOLD_MS = 500L

        /** After a key-up, wait this long for a follow-up tap before resolving the gesture. */
        const val MULTI_TAP_WINDOW_MS = 400L

        /** Tap counts above this collapse to [KeyGesture.TRIPLE_PRESS]. */
        private const val MAX_TAP_COUNT = 3
    }

    private var tapCount = 0
    private var pressInProgress = false
    private var longPressTimer: Cancellable? = null
    private var multiTapTimer: Cancellable? = null

    fun onKeyDown(timestampMs: Long) {
        // A new press cancels any in-flight multi-tap decision: the sequence is still growing.
        cancelMultiTapTimer()
        pressInProgress = true
        scheduleLongPressTimer()
    }

    fun onKeyUp(timestampMs: Long) {
        // If the long-press timer already fired, this key-up closes a long press and is not a tap.
        if (!pressInProgress) {
            return
        }
        pressInProgress = false
        cancelLongPressTimer()

        tapCount = (tapCount + 1).coerceAtMost(MAX_TAP_COUNT)
        resolveOrWaitForMoreTaps()
    }

    /** Cancels any pending decision and returns the machine to its initial state. */
    fun reset() {
        cancelLongPressTimer()
        cancelMultiTapTimer()
        tapCount = 0
        pressInProgress = false
    }

    private fun resolveOrWaitForMoreTaps() {
        val immediate = immediateGestureOrNull()
        if (immediate != null) {
            emitAndReset(immediate)
            return
        }
        scheduleMultiTapWindow()
    }

    /**
     * Returns the gesture that can be emitted without waiting for the multi-tap window, or null if
     * a follow-up tap is still possible under the current configuration.
     */
    private fun immediateGestureOrNull(): KeyGesture? {
        val doubleEnabled = KeyGesture.DOUBLE_PRESS in enabledGestures
        val tripleEnabled = KeyGesture.TRIPLE_PRESS in enabledGestures
        return when (tapCount) {
            2 -> if (!tripleEnabled) KeyGesture.DOUBLE_PRESS else null
            1 -> if (!doubleEnabled && !tripleEnabled) KeyGesture.SINGLE_PRESS else null
            // When triple is enabled we still wait the window after the third tap so extra taps
            // are absorbed (they cap back to triple) rather than starting a new sequence.
            else -> null
        }
    }

    private fun scheduleMultiTapWindow() {
        cancelMultiTapTimer()
        multiTapTimer = scheduler.schedule(MULTI_TAP_WINDOW_MS) {
            emitAndReset(gestureForTapCount(tapCount))
        }
    }

    private fun scheduleLongPressTimer() {
        cancelLongPressTimer()
        longPressTimer = scheduler.schedule(LONG_PRESS_THRESHOLD_MS) {
            // Held long enough: the release will be swallowed by the pressInProgress guard.
            pressInProgress = false
            emitAndReset(KeyGesture.LONG_PRESS)
        }
    }

    private fun gestureForTapCount(count: Int): KeyGesture = when (count) {
        1 -> KeyGesture.SINGLE_PRESS
        2 -> KeyGesture.DOUBLE_PRESS
        else -> KeyGesture.TRIPLE_PRESS
    }

    private fun emitAndReset(gesture: KeyGesture) {
        reset()
        onGesture(gesture)
    }

    private fun cancelLongPressTimer() {
        longPressTimer?.cancel()
        longPressTimer = null
    }

    private fun cancelMultiTapTimer() {
        multiTapTimer?.cancel()
        multiTapTimer = null
    }
}

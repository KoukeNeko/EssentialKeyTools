package dev.koukeneko.essentialkeytools.service

import android.view.KeyEvent

/**
 * Pure decision logic for which key events the service acts on, extracted from the Android service
 * so it can be unit-tested on the JVM. Holds no state and no Android service dependency.
 */
object KeyEventFilter {

    /** True when a scanCode is a volume key we must never treat as an Essential-Key candidate. */
    fun isVolumeKey(keyCode: Int): Boolean =
        keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN

    /**
     * True when an event should feed the gesture classifier: its scanCode is the learned Essential
     * Key. Detection (learning) mode bypasses classification entirely, so this only applies to
     * normal operation.
     */
    fun matchesLearnedKey(eventScanCode: Int, learnedScanCode: Int): Boolean =
        eventScanCode == learnedScanCode

    /**
     * True when a captured event is a valid candidate for scanCode learning. Volume keys are
     * excluded so the user cannot accidentally learn a volume rocker as the Essential Key.
     */
    fun isLearnableCandidate(keyCode: Int): Boolean = !isVolumeKey(keyCode)
}

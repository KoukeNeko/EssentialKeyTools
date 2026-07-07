package dev.koukeneko.essentialkeytools.service

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyEventFilterTest {

    @Test
    fun matchesLearnedKey_trueWhenScanCodesEqual() {
        assertTrue(KeyEventFilter.matchesLearnedKey(eventScanCode = 250, learnedScanCode = 250))
    }

    @Test
    fun matchesLearnedKey_falseWhenScanCodesDiffer() {
        assertFalse(KeyEventFilter.matchesLearnedKey(eventScanCode = 42, learnedScanCode = 250))
    }

    @Test
    fun isVolumeKey_trueForVolumeUpAndDown() {
        assertTrue(KeyEventFilter.isVolumeKey(KeyEvent.KEYCODE_VOLUME_UP))
        assertTrue(KeyEventFilter.isVolumeKey(KeyEvent.KEYCODE_VOLUME_DOWN))
    }

    @Test
    fun isVolumeKey_falseForUnknownKeyCode() {
        // The Essential Key arrives as KEYCODE_UNKNOWN (0), which must not be filtered out.
        assertFalse(KeyEventFilter.isVolumeKey(KeyEvent.KEYCODE_UNKNOWN))
    }

    @Test
    fun isLearnableCandidate_rejectsVolumeKeys() {
        assertFalse(KeyEventFilter.isLearnableCandidate(KeyEvent.KEYCODE_VOLUME_UP))
        assertFalse(KeyEventFilter.isLearnableCandidate(KeyEvent.KEYCODE_VOLUME_DOWN))
    }

    @Test
    fun isLearnableCandidate_acceptsEssentialKey() {
        assertTrue(KeyEventFilter.isLearnableCandidate(KeyEvent.KEYCODE_UNKNOWN))
    }
}

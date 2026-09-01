package dev.koukeneko.essentialkeytools.actions

import android.media.AudioManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The ringer constants are compile-time literals, so the cycle is exercisable on the JVM without a
 * device. The without-access cases are the regression guard for the crash where leaving silent threw
 * SecurityException ("Not allowed to change Do Not Disturb state").
 */
class RingerModeCycleTest {

    @Test
    fun normalGoesToVibrateWithoutPolicyAccess() {
        assertEquals(
            AudioManager.RINGER_MODE_VIBRATE,
            nextRingerMode(AudioManager.RINGER_MODE_NORMAL, policyAccessGranted = false)
        )
    }

    @Test
    fun vibrateReturnsToNormalWithoutPolicyAccess() {
        assertEquals(
            AudioManager.RINGER_MODE_NORMAL,
            nextRingerMode(AudioManager.RINGER_MODE_VIBRATE, policyAccessGranted = false)
        )
    }

    @Test
    fun silentHasNoLegalStepWithoutPolicyAccess() {
        assertNull(nextRingerMode(AudioManager.RINGER_MODE_SILENT, policyAccessGranted = false))
    }

    @Test
    fun vibrateGoesToSilentWithPolicyAccess() {
        assertEquals(
            AudioManager.RINGER_MODE_SILENT,
            nextRingerMode(AudioManager.RINGER_MODE_VIBRATE, policyAccessGranted = true)
        )
    }

    @Test
    fun silentReturnsToNormalWithPolicyAccess() {
        assertEquals(
            AudioManager.RINGER_MODE_NORMAL,
            nextRingerMode(AudioManager.RINGER_MODE_SILENT, policyAccessGranted = true)
        )
    }

    @Test
    fun unknownModeRestartsAtNormal() {
        val unknownMode = -1
        assertEquals(
            AudioManager.RINGER_MODE_NORMAL,
            nextRingerMode(unknownMode, policyAccessGranted = true)
        )
    }
}

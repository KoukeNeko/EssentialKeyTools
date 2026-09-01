package dev.koukeneko.essentialkeytools.actions

import android.media.AudioManager

/**
 * The mode one step of the ringer cycle should land on, or null when no step is legal.
 *
 * The platform treats every transition that enters or leaves silent as a Do-Not-Disturb change and
 * rejects it with a SecurityException unless the app holds Notification-Policy access. Without that
 * access the cycle therefore collapses to normal <-> vibrate, and a device already sitting in silent
 * has no reachable target at all — hence the null.
 */
internal fun nextRingerMode(currentMode: Int, policyAccessGranted: Boolean): Int? = when {
    currentMode == AudioManager.RINGER_MODE_SILENT && !policyAccessGranted -> null
    currentMode == AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
    currentMode == AudioManager.RINGER_MODE_VIBRATE ->
        if (policyAccessGranted) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
    else -> AudioManager.RINGER_MODE_NORMAL
}

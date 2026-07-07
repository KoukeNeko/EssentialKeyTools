package dev.koukeneko.essentialkeytools.unlock

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku

/** Why the privileged shell cannot be used right now, so the wizard can guide the user precisely. */
enum class ShizukuAvailability {
    /** Shizuku is running and this app already holds the permission. */
    READY,

    /** Shizuku is running but the app has not been granted permission yet. */
    PERMISSION_REQUIRED,

    /** Shizuku is installed but its service is not running (not started via ADB/root). */
    NOT_RUNNING,

    /** Shizuku is not installed at all, or too old to talk to. */
    NOT_INSTALLED
}

/**
 * Thin wrapper over the static [Shizuku] API for the pieces the UI and runner need: current
 * availability and permission. Isolating the Shizuku calls here keeps the rest of the unlock code
 * free of the binder details and makes the availability logic easy to reason about.
 */
object ShizukuGate {

    /** Shizuku pre-v11 could not report permission per-app; we require a modern build. */
    private const val LEGACY_UID_UNKNOWN = -1

    fun availability(): ShizukuAvailability {
        if (!isBinderAlive()) {
            return if (Shizuku.pingBinder()) ShizukuAvailability.NOT_RUNNING
            else ShizukuAvailability.NOT_INSTALLED
        }
        if (Shizuku.isPreV11() || Shizuku.getUid() == LEGACY_UID_UNKNOWN) {
            // A pre-v11 Shizuku cannot grant runtime permission the way we need.
            return ShizukuAvailability.NOT_INSTALLED
        }
        return if (hasPermission()) ShizukuAvailability.READY
        else ShizukuAvailability.PERMISSION_REQUIRED
    }

    fun hasPermission(): Boolean =
        isBinderAlive() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    fun requestPermission(requestCode: Int) {
        if (isBinderAlive()) {
            Shizuku.requestPermission(requestCode)
        }
    }

    private fun isBinderAlive(): Boolean =
        try {
            Shizuku.pingBinder()
        } catch (error: IllegalStateException) {
            // Thrown when the binder was never received (Shizuku app absent).
            false
        }
}

package dev.koukeneko.essentialkeytools.actions

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import dev.koukeneko.essentialkeytools.R

/**
 * Runs a [KeyAction]. Actions that need system-level reach (screenshot, lock screen) require the
 * owning [AccessibilityService]; the rest only need a [Context]. Every handler fails soft: it logs
 * and surfaces a toast rather than throwing, because an action fires from a hardware key press with
 * no UI in the foreground.
 */
class ActionExecutor(
    private val context: Context,
    private val accessibilityService: AccessibilityService?
) {
    private companion object {
        const val TAG = "ActionExecutor"

        // Tracks the torch state ourselves: CameraManager has no synchronous "is torch on" query
        // that is reliable across devices, so we mirror the last toggle we issued.
    }

    private var torchEnabled = false
    private var silentUnavailableHintShown = false

    fun execute(action: KeyAction) {
        when (action) {
            is KeyAction.None -> Unit
            is KeyAction.LaunchApp -> launchApp(action.packageName)
            is KeyAction.ToggleFlashlight -> toggleFlashlight()
            is KeyAction.TakeScreenshot -> performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT,
                R.string.error_screenshot_failed
            )
            is KeyAction.LockScreen -> performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN,
                R.string.error_lock_screen_failed
            )
            is KeyAction.MediaPlayPause ->
                dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            is KeyAction.MediaNext -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            is KeyAction.MediaPrevious -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            is KeyAction.RingerCycle -> cycleRingerMode()
        }
    }

    private fun launchApp(packageName: String) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Log.w(TAG, "No launch intent for package $packageName")
            toast(R.string.error_app_not_found)
            return
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(launchIntent)
        } catch (error: android.content.ActivityNotFoundException) {
            Log.w(TAG, "Activity not found launching $packageName", error)
            toast(R.string.error_app_not_found)
        }
    }

    private fun toggleFlashlight() {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (cameraManager == null) {
            toast(R.string.error_flashlight_unavailable)
            return
        }
        val cameraId = backCameraWithFlashOrNull(cameraManager)
        if (cameraId == null) {
            toast(R.string.error_flashlight_unavailable)
            return
        }
        try {
            val nextState = !torchEnabled
            cameraManager.setTorchMode(cameraId, nextState)
            torchEnabled = nextState
        } catch (error: CameraAccessException) {
            Log.w(TAG, "Torch toggle failed", error)
            toast(R.string.error_flashlight_unavailable)
        } catch (error: IllegalArgumentException) {
            Log.w(TAG, "Torch toggle rejected camera id", error)
            toast(R.string.error_flashlight_unavailable)
        }
    }

    private fun backCameraWithFlashOrNull(cameraManager: CameraManager): String? {
        return try {
            cameraManager.cameraIdList.firstOrNull { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val hasFlash =
                    characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            }
        } catch (error: CameraAccessException) {
            Log.w(TAG, "Could not enumerate cameras", error)
            null
        }
    }

    private fun performGlobalAction(globalAction: Int, failureMessageRes: Int) {
        val service = accessibilityService
        if (service == null) {
            Log.w(TAG, "Global action $globalAction needs the accessibility service")
            toast(failureMessageRes)
            return
        }
        val dispatched = service.performGlobalAction(globalAction)
        if (!dispatched) {
            Log.w(TAG, "Global action $globalAction was not dispatched")
            toast(failureMessageRes)
        }
    }

    /**
     * Sends one media key to whichever session currently owns media buttons. The down/up pair is
     * load-bearing rather than ceremony: the framework treats play/pause as a voice key and holds
     * its down event until the up arrives, so a lone down would never be delivered. Nothing is
     * reported back — the platform offers no permission-free acknowledgement, so a press with no
     * player listening is a silent no-op, exactly like a headset button.
     */
    private fun dispatchMediaKey(keyCode: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager == null) {
            toast(R.string.error_media_unavailable)
            return
        }
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun cycleRingerMode() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager == null) {
            toast(R.string.error_ringer_unavailable)
            return
        }
        val policyAccessGranted = notificationPolicyAccessGranted()
        val nextMode = nextRingerMode(audioManager.ringerMode, policyAccessGranted)
        if (nextMode == null) {
            toast(R.string.hint_ringer_silent_exit_needs_policy)
            return
        }
        if (!applyRingerMode(audioManager, nextMode)) {
            toast(R.string.error_ringer_change_denied)
            return
        }
        if (!policyAccessGranted) {
            showSilentUnavailableHintOnce()
        }
    }

    /**
     * Writes [mode] and reports whether the system accepted it. OEM builds draw the
     * Do-Not-Disturb line in slightly different places, so the write stays guarded even for
     * transitions [nextRingerMode] believes are legal — a denied write must never escape, because
     * the action runs on the accessibility service's main thread.
     */
    private fun applyRingerMode(audioManager: AudioManager, mode: Int): Boolean = try {
        audioManager.ringerMode = mode
        true
    } catch (error: SecurityException) {
        Log.w(TAG, "Ringer mode change to $mode denied", error)
        false
    }

    private fun notificationPolicyAccessGranted(): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return notificationManager?.isNotificationPolicyAccessGranted == true
    }

    /** Explains the shortened cycle once per process rather than on every key press. */
    private fun showSilentUnavailableHintOnce() {
        if (silentUnavailableHintShown) {
            return
        }
        silentUnavailableHintShown = true
        toast(R.string.hint_ringer_silent_needs_policy)
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
    }
}

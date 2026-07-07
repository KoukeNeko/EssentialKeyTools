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
            is KeyAction.MediaPlayPause -> dispatchMediaPlayPause()
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

    private fun dispatchMediaPlayPause() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager == null) {
            toast(R.string.error_media_unavailable)
            return
        }
        val keyCode = KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun cycleRingerMode() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager == null) {
            toast(R.string.error_ringer_unavailable)
            return
        }
        val nextMode = nextRingerMode(audioManager.ringerMode, silentAllowed())
        try {
            audioManager.ringerMode = nextMode
        } catch (error: SecurityException) {
            // Setting SILENT without policy access throws even when we pre-check; degrade to vibrate.
            Log.w(TAG, "Ringer mode change denied, falling back to vibrate", error)
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
        }
    }

    /**
     * Chooses the next ringer mode. When silent is unavailable (no Notification-Policy access) the
     * cycle collapses to normal <-> vibrate so the action still does something.
     */
    private fun nextRingerMode(currentMode: Int, silentAllowed: Boolean): Int = when (currentMode) {
        AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
        AudioManager.RINGER_MODE_VIBRATE ->
            if (silentAllowed) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
        else -> AudioManager.RINGER_MODE_NORMAL
    }

    private fun silentAllowed(): Boolean {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val granted = notificationManager?.isNotificationPolicyAccessGranted == true
        if (!granted && !ringerHintShown) {
            // Surface the missing-permission hint exactly once per process, not on every cycle.
            ringerHintShown = true
            toast(R.string.hint_ringer_silent_needs_policy)
        }
        return granted
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
    }

    private var ringerHintShown = false
}

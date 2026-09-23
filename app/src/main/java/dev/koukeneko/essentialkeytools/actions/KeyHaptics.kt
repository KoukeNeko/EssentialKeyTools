package dev.koukeneko.essentialkeytools.actions

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import dev.koukeneko.essentialkeytools.settings.HapticStrength

/**
 * Plays the confirmation vibration for an Essential Key press. Uses the system's predefined
 * effects so the feel matches the device's own button feedback, and tags it as hardware feedback
 * so it is treated like a physical button press rather than touch feedback.
 */
class KeyHaptics(context: Context) {

    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
    private val attributes = VibrationAttributes.createForUsage(
        VibrationAttributes.USAGE_HARDWARE_FEEDBACK
    )

    fun perform(strength: HapticStrength) {
        val effectId = when (strength) {
            HapticStrength.OFF -> return
            HapticStrength.LIGHT -> VibrationEffect.EFFECT_TICK
            HapticStrength.MEDIUM -> VibrationEffect.EFFECT_CLICK
            HapticStrength.STRONG -> VibrationEffect.EFFECT_HEAVY_CLICK
        }
        vibrator.vibrate(VibrationEffect.createPredefined(effectId), attributes)
    }
}

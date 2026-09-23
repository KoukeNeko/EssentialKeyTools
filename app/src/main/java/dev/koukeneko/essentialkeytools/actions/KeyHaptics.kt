package dev.koukeneko.essentialkeytools.actions

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import dev.koukeneko.essentialkeytools.settings.HapticStrength

/**
 * Plays the confirmation vibration for an Essential Key press. Tagged as hardware feedback so it is
 * treated like a physical button press rather than touch feedback.
 *
 * Medium uses the system's own click effect, which keeps that level matching the feel of the
 * device's physical buttons. Light and Strong are built as explicit pulses instead, because the
 * predefined tick and heavy-click effects are patterns for unrelated UI (a clock tick, a long
 * press) rather than points on one intensity scale, and on a vibrator that reports no supported
 * effects every predefined effect falls back to the same generic pattern — which left all three
 * levels feeling identical. Pulse length is the only dimension available to separate them.
 */
class KeyHaptics(context: Context) {

    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
    private val attributes = VibrationAttributes.createForUsage(
        VibrationAttributes.USAGE_HARDWARE_FEEDBACK
    )

    fun perform(strength: HapticStrength) {
        val effect = when (strength) {
            HapticStrength.OFF -> return
            HapticStrength.LIGHT -> VibrationEffect.createWaveform(LIGHT_TIMINGS, NO_REPEAT)
            HapticStrength.MEDIUM -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            HapticStrength.STRONG -> VibrationEffect.createWaveform(STRONG_TIMINGS, NO_REPEAT)
        }
        vibrator.vibrate(effect, attributes)
    }

    private companion object {
        const val NO_REPEAT = -1

        // Waveform timings alternate off/on and begin with the wait before the motor starts.
        val LIGHT_TIMINGS = longArrayOf(0, 15)
        val STRONG_TIMINGS = longArrayOf(0, 80)
    }
}

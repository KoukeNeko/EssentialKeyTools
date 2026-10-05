package dev.koukeneko.essentialkeytools.actions

import android.content.Context
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.VibratorManager
import dev.koukeneko.essentialkeytools.settings.HapticPattern
import dev.koukeneko.essentialkeytools.settings.HapticStrength

/**
 * Plays the confirmation vibration for an Essential Key press. Tagged as hardware feedback so it is
 * treated like a physical button press rather than touch feedback.
 *
 * Every level is an explicit pulse rather than a predefined system effect. On a vibrator that
 * reports no supported effects, each predefined effect falls back to a device-specific pattern:
 * the same generic one for every effect on the Phone (3), which left all three levels feeling
 * identical, and nothing at all on the Phone (4a). The Phone (3) vibrator also has no amplitude
 * control, so pulse length is the only dimension available to separate the levels.
 */
class KeyHaptics(context: Context) {

    private val vibrator = context.getSystemService(VibratorManager::class.java).defaultVibrator
    private val attributes = VibrationAttributes.createForUsage(
        VibrationAttributes.USAGE_HARDWARE_FEEDBACK
    )

    val hasAmplitudeControl: Boolean get() = vibrator.hasAmplitudeControl()

    /** Plays [customPattern] for [HapticStrength.CUSTOM]; the other levels are fixed pulses. */
    fun perform(strength: HapticStrength, customPattern: HapticPattern) {
        val effect = when (strength) {
            HapticStrength.OFF -> return
            HapticStrength.CUSTOM -> return play(customPattern)
            HapticStrength.LIGHT -> VibrationEffect.createWaveform(LIGHT_TIMINGS, NO_REPEAT)
            HapticStrength.MEDIUM -> VibrationEffect.createWaveform(MEDIUM_TIMINGS, NO_REPEAT)
            HapticStrength.STRONG -> VibrationEffect.createWaveform(STRONG_TIMINGS, NO_REPEAT)
        }
        vibrator.vibrate(effect, attributes)
    }

    fun play(pattern: HapticPattern) {
        val waveform = pattern.toWaveform(hasAmplitudeControl)
        vibrator.vibrate(
            VibrationEffect.createWaveform(waveform.timings, waveform.amplitudes, NO_REPEAT),
            attributes
        )
    }

    private companion object {
        const val NO_REPEAT = -1

        // Waveform timings alternate off/on and begin with the wait before the motor starts.
        val LIGHT_TIMINGS = longArrayOf(0, 15)
        // 40ms was indistinguishable from Light on the Phone (4a); 50ms is clearly longer there.
        val MEDIUM_TIMINGS = longArrayOf(0, 50)
        val STRONG_TIMINGS = longArrayOf(0, 80)
    }
}

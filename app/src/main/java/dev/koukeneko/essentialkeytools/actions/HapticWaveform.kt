package dev.koukeneko.essentialkeytools.actions

import dev.koukeneko.essentialkeytools.settings.HapticPattern
import kotlin.math.roundToInt

/** Parallel arrays in the shape [android.os.VibrationEffect.createWaveform] takes. */
class HapticWaveform(val timings: LongArray, val amplitudes: IntArray)

private const val MAX_AMPLITUDE = 255

// Length of one on/off cycle when amplitude is approximated on a motor with no amplitude control.
private const val DUTY_CYCLE_MILLIS = 10

/**
 * Expands the pattern into a waveform. A vibrator with amplitude control plays each pulse at the
 * requested strength. One without it can only switch fully on or off, so a pulse below full
 * strength is chopped into short on/off cycles whose on-share is the amplitude; the motor's inertia
 * averages them into a weaker vibration.
 */
fun HapticPattern.toWaveform(hasAmplitudeControl: Boolean): HapticWaveform {
    // Work from the coerced pattern so no input can make pulses run into each other.
    val (pulseCount, pulseMillis, frequencyHz, amplitudePercent) = coerced()
    val period = 1000 / frequencyHz
    val timings = mutableListOf<Long>()
    val amplitudes = mutableListOf<Int>()

    fun add(durationMillis: Int, amplitude: Int) {
        if (durationMillis <= 0) return
        if (amplitudes.isNotEmpty() && amplitudes.last() == amplitude) {
            timings[timings.lastIndex] += durationMillis.toLong()
        } else {
            timings.add(durationMillis.toLong())
            amplitudes.add(amplitude)
        }
    }

    repeat(pulseCount) { index ->
        if (index > 0) add(period - pulseMillis, 0)
        if (hasAmplitudeControl) {
            add(pulseMillis, amplitudePercent * MAX_AMPLITUDE / 100)
        } else {
            val onMillis = (DUTY_CYCLE_MILLIS * amplitudePercent / 100.0).roundToInt().coerceAtLeast(1)
            var remaining = pulseMillis
            while (remaining > 0) {
                val on = minOf(onMillis, remaining)
                add(on, MAX_AMPLITUDE)
                remaining -= on
                val off = minOf(DUTY_CYCLE_MILLIS - onMillis, remaining)
                add(off, 0)
                remaining -= off
            }
        }
    }
    return HapticWaveform(timings.toLongArray(), amplitudes.toIntArray())
}

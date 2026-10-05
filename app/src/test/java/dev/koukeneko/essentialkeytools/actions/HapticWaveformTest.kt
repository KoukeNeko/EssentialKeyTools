package dev.koukeneko.essentialkeytools.actions

import dev.koukeneko.essentialkeytools.settings.HapticPattern
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class HapticWaveformTest {

    private fun pattern(
        count: Int = 1,
        pulseMillis: Int = 50,
        frequencyHz: Int = 10,
        amplitudePercent: Int = 100
    ) = HapticPattern(count, pulseMillis, frequencyHz, amplitudePercent)

    @Test
    fun singleFullPulse_isOneOnSegment() {
        val waveform = pattern().toWaveform(hasAmplitudeControl = false)

        assertArrayEquals(longArrayOf(50), waveform.timings)
        assertArrayEquals(intArrayOf(255), waveform.amplitudes)
    }

    @Test
    fun pulsesRepeatAtTheRequestedFrequency() {
        // 5Hz is a 200ms period: a 50ms pulse then a 150ms gap, with no trailing gap.
        val waveform = pattern(count = 3, frequencyHz = 5).toWaveform(hasAmplitudeControl = false)

        assertArrayEquals(longArrayOf(50, 150, 50, 150, 50), waveform.timings)
        assertArrayEquals(intArrayOf(255, 0, 255, 0, 255), waveform.amplitudes)
    }

    @Test
    fun frequencyTooHighForThePulse_isLoweredSoPulsesStayApart() {
        // 25Hz would be a 40ms period, shorter than the 100ms pulse; 9Hz is the highest that fits.
        val waveform = pattern(count = 2, pulseMillis = 100, frequencyHz = 25)
            .toWaveform(hasAmplitudeControl = false)

        assertArrayEquals(longArrayOf(100, 11, 100), waveform.timings)
    }

    @Test
    fun hardwareAmplitude_scalesEachPulse() {
        val waveform = pattern(count = 2, amplitudePercent = 50).toWaveform(hasAmplitudeControl = true)

        assertArrayEquals(intArrayOf(127, 0, 127), waveform.amplitudes)
        assertArrayEquals(longArrayOf(50, 50, 50), waveform.timings)
    }

    @Test
    fun withoutAmplitudeControl_dutyCyclingKeepsThePulseLength() {
        val waveform = pattern(pulseMillis = 45, amplitudePercent = 30)
            .toWaveform(hasAmplitudeControl = false)

        assertEquals(45L, waveform.timings.sum())
        // Only fully on or fully off: the motor cannot do anything in between.
        assertEquals(setOf(0, 255), waveform.amplitudes.toSet())
        // 30% of a 10ms cycle is 3ms on.
        assertEquals(3L, waveform.timings[0])
        assertEquals(7L, waveform.timings[1])
    }

    @Test
    fun lowestAmplitude_neverRoundsToSilence() {
        val waveform = pattern(pulseMillis = 20, amplitudePercent = 10)
            .toWaveform(hasAmplitudeControl = false)

        assertEquals(1L, waveform.timings[0])
        assertEquals(255, waveform.amplitudes[0])
    }
}

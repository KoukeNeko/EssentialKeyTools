package dev.koukeneko.essentialkeytools.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class HapticPatternTest {

    @Test
    fun longerPulses_lowerTheHighestFrequency() {
        assertEquals(25, HapticPattern.maxFrequencyHz(pulseMillis = 5))
        assertEquals(9, HapticPattern.maxFrequencyHz(pulseMillis = 100))
        assertEquals(4, HapticPattern.maxFrequencyHz(pulseMillis = 200))
    }

    @Test
    fun coerced_lowersTheFrequencyToWhatTheLongPulseAllows() {
        val pattern = HapticPattern(pulseCount = 3, pulseMillis = 200, frequencyHz = 20, amplitudePercent = 100)

        assertEquals(4, pattern.coerced().frequencyHz)
    }

    @Test
    fun coerced_keepsAFrequencyThatAlreadyFits() {
        val pattern = HapticPattern(pulseCount = 3, pulseMillis = 30, frequencyHz = 8, amplitudePercent = 60)

        assertEquals(pattern, pattern.coerced())
    }
}

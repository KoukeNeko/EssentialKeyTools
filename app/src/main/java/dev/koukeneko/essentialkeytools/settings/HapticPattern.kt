package dev.koukeneko.essentialkeytools.settings

/**
 * A user-designed vibration: [pulseCount] pulses of [pulseMillis] each, started [frequencyHz] times
 * per second, at [amplitudePercent] of the motor's full strength. The frequency only spaces
 * consecutive pulses, so it has no effect on a single pulse.
 */
data class HapticPattern(
    val pulseCount: Int,
    val pulseMillis: Int,
    val frequencyHz: Int,
    val amplitudePercent: Int
) {
    /** The same pattern with every field pulled inside its supported range. */
    fun coerced(): HapticPattern {
        val boundedPulseMillis = pulseMillis.coerceIn(PULSE_MILLIS_RANGE)
        return HapticPattern(
            pulseCount = pulseCount.coerceIn(PULSE_COUNT_RANGE),
            pulseMillis = boundedPulseMillis,
            frequencyHz = frequencyHz.coerceIn(
                FREQUENCY_HZ_RANGE.first,
                maxFrequencyHz(boundedPulseMillis)
            ),
            amplitudePercent = amplitudePercent.coerceIn(AMPLITUDE_PERCENT_RANGE)
        )
    }

    companion object {
        val PULSE_COUNT_RANGE = 1..10
        val PULSE_MILLIS_RANGE = 5..200
        val FREQUENCY_HZ_RANGE = 1..25
        val AMPLITUDE_PERCENT_RANGE = 10..100

        // Pulses never merge into one long buzz, however high the frequency or long the pulse.
        const val MIN_GAP_MILLIS = 10

        /**
         * The highest frequency that still leaves [MIN_GAP_MILLIS] between pulses of [pulseMillis].
         * Anything above it would sound the same as it, so it is not offered.
         */
        fun maxFrequencyHz(pulseMillis: Int): Int =
            (1000 / (pulseMillis + MIN_GAP_MILLIS)).coerceAtMost(FREQUENCY_HZ_RANGE.last)

        val DEFAULT = HapticPattern(
            pulseCount = 1,
            pulseMillis = 50,
            frequencyHz = 10,
            amplitudePercent = 100
        )
    }
}

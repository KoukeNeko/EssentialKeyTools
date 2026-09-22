package dev.koukeneko.essentialkeytools.glyph

/**
 * A Nothing device with Glyph Matrix hardware. Pure mapping from `Build.DEVICE` so it stays
 * unit-testable on the JVM; callers pass `Build.DEVICE` in rather than this file reading it, the
 * same separation [dev.koukeneko.essentialkeytools.service.KeyEventFilter] uses.
 *
 * Nothing Phone (4a) Pro also ships a Glyph Matrix (the SDK's `Glyph.DEVICE_25111p`), but its
 * `Build.DEVICE` codename has not been confirmed against real hardware, so it is deliberately left
 * undetected here rather than guessed: a wrong match would mean sending Glyph Matrix calls the SDK
 * was never registered to receive.
 */
enum class GlyphDevice {
    /** Nothing Phone (3): 25x25 Glyph Matrix. `Build.DEVICE` confirmed as "Metroid". */
    PHONE_3;

    companion object {
        /** Resolves the [GlyphDevice] for a `Build.DEVICE` codename, or null if it has none. */
        fun forCodename(codename: String?): GlyphDevice? = when (codename) {
            "Metroid" -> PHONE_3
            else -> null
        }
    }
}

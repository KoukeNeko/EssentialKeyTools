package dev.koukeneko.essentialkeytools.glyph

import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph

/**
 * A Nothing device with Glyph Matrix hardware. Detection is delegated entirely to the Glyph Matrix
 * SDK's own `Common.isXXXX()` checks rather than matching `Build.DEVICE` ourselves: only Nothing's
 * SDK can say for certain which codename maps to which supported model.
 */
enum class GlyphDevice(internal val sdkDeviceId: String) {
    /** Nothing Phone (3): 25x25 Glyph Matrix. */
    PHONE_3(Glyph.DEVICE_23112),

    /** Nothing Phone (4a) Pro: 13x13 Glyph Matrix. */
    PHONE_4A_PRO(Glyph.DEVICE_25111p);

    companion object {
        /** The current device's [GlyphDevice], or null when it has no Glyph Matrix. */
        fun current(): GlyphDevice? = when {
            Common.is23112() -> PHONE_3
            Common.is25111p() -> PHONE_4A_PRO
            else -> null
        }
    }
}

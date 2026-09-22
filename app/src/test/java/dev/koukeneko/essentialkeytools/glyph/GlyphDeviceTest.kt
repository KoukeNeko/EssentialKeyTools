package dev.koukeneko.essentialkeytools.glyph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GlyphDeviceTest {

    @Test
    fun forCodename_metroidIsPhone3() {
        assertEquals(GlyphDevice.PHONE_3, GlyphDevice.forCodename("Metroid"))
    }

    @Test
    fun forCodename_unknownCodenameIsNull() {
        assertNull(GlyphDevice.forCodename("some_other_phone"))
    }

    @Test
    fun forCodename_nullIsNull() {
        assertNull(GlyphDevice.forCodename(null))
    }
}

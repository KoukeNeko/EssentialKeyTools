package dev.koukeneko.essentialkeytools.glyph

import android.content.Context

/**
 * Controls a device's Glyph Matrix as a steady, all-on light -- the closest equivalent to Nothing
 * OS's own "Glyph Torch" quick-settings tile, which the Glyph Matrix SDK does not expose as a
 * single call. An implementation owns its own on/off state, mirroring how
 * [dev.koukeneko.essentialkeytools.actions.ActionExecutor] already tracks the camera torch:
 * neither SDK offers a reliable synchronous "is it on" query.
 */
interface GlyphLight {
    /** Binds to the Glyph service. Calls [onReady] once bound; never called if binding fails. */
    fun bind(context: Context, onReady: () -> Unit)

    /** Turns the whole matrix on or off at full brightness. */
    fun setAllOn(on: Boolean)

    /** Releases the service binding. */
    fun release()
}

package dev.koukeneko.essentialkeytools.glyph

import android.content.Context
import android.util.Log

/**
 * [GlyphLight] backed by Nothing's Glyph Matrix SDK.
 *
 * TODO: not yet implemented. The SDK ships as a manually-vendored AAR (`app/libs/glyph-matrix-sdk-2.0.aar`,
 * not on Maven Central) that is not in this checkout yet, so this class cannot reference
 * `com.nothing.ketchum.GlyphMatrixManager` until it is added. Once the AAR is vendored, replace this
 * body with: bind `GlyphMatrixManager`, `register(Glyph.DEVICE_23112)` in its connected callback,
 * and drive `setMatrixFrame` with a full-coverage, full-brightness frame for [setAllOn].
 *
 * Kept out of [dev.koukeneko.essentialkeytools.ui.UiLabels.builtInActions] until this is real, so
 * the action picker never offers a gesture mapping that silently does nothing.
 */
class MatrixGlyphLight : GlyphLight {
    override fun bind(context: Context, onReady: () -> Unit) {
        Log.w(TAG, "Glyph Matrix support is not implemented yet")
    }

    override fun setAllOn(on: Boolean) = Unit

    override fun release() = Unit

    private companion object {
        const val TAG = "MatrixGlyphLight"
    }
}

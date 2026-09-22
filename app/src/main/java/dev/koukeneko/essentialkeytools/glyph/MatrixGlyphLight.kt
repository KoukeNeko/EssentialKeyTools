package dev.koukeneko.essentialkeytools.glyph

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.Log
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphMatrixObject

/**
 * [GlyphLight] backed by Nothing's Glyph Matrix SDK. The SDK has no single "turn everything on"
 * call, so [setAllOn] renders a full-white [GlyphMatrixObject] instead -- the closest equivalent
 * to Nothing OS's own "Glyph Torch" quick-settings tile.
 *
 * `GlyphMatrixManager.setMatrixFrame(int[])` also accepts a raw array directly, but its expected
 * encoding is undocumented; the SDK's own example project always goes through
 * [GlyphMatrixObject]/[GlyphMatrixFrame.Builder]/`render()`, which is the path used here.
 */
class MatrixGlyphLight(private val deviceId: String) : GlyphLight {

    private var manager: GlyphMatrixManager? = null
    private var appContext: Context? = null

    override fun bind(context: Context, onReady: () -> Unit) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        val instance = GlyphMatrixManager.getInstance(applicationContext)
        instance.init(object : GlyphMatrixManager.Callback {
            override fun onServiceConnected(name: ComponentName?) {
                if (!instance.register(deviceId)) {
                    Log.w(TAG, "GlyphMatrixManager.register($deviceId) was refused")
                    return
                }
                manager = instance
                onReady()
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                manager = null
            }
        })
    }

    override fun setAllOn(on: Boolean) {
        val active = manager ?: return
        val context = appContext ?: return
        try {
            if (on) {
                active.setMatrixFrame(fullWhiteFrame(context))
            } else {
                active.turnOff()
            }
        } catch (error: GlyphException) {
            Log.w(TAG, "Glyph Matrix frame update failed", error)
        }
    }

    override fun release() {
        manager?.unInit()
        manager = null
        appContext = null
    }

    /** A [GlyphMatrixObject] covering the whole matrix at maximum brightness, rendered to a frame. */
    private fun fullWhiteFrame(context: Context): IntArray {
        val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).drawColor(Color.WHITE)
        val fullMatrixObject = GlyphMatrixObject.Builder()
            .setImageSource(bitmap)
            .setScale(MAX_SCALE)
            .setPosition(0, 0)
            .setBrightness(MAX_BRIGHTNESS)
            .setReverse(false)
            .build()
        return GlyphMatrixFrame.Builder()
            .addTop(fullMatrixObject)
            .build(context)
            .render()
    }

    private companion object {
        const val TAG = "MatrixGlyphLight"
        const val MAX_BRIGHTNESS = 255
        const val MAX_SCALE = 100

        // Source bitmap for the solid fill; the SDK downsamples it to the device's actual matrix
        // resolution, so this only needs to be large enough to avoid sampling artifacts.
        const val BITMAP_SIZE = 64
    }
}

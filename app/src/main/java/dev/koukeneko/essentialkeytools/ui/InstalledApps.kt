package dev.koukeneko.essentialkeytools.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

private const val TAG = "InstalledApps"

/** A launchable app shown in the action picker, with its icon already sized for the row. */
data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap?
)

/**
 * Queries the launcher-visible apps declared reachable by the manifest `<queries>` element. Runs
 * off the main thread by the caller; sorting is by display label so the list is human-scannable.
 */
object InstalledApps {

    /**
     * Loads every launchable app, each icon rasterised to an [iconSizePx] square. Icons are
     * converted here rather than carried as drawables so the loaded drawables — one per installed
     * app, some of them very large — are collectable as soon as this returns, instead of the whole
     * set staying resident for as long as the picker is open.
     */
    fun loadLaunchable(context: Context, iconSizePx: Int): List<LaunchableApp> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager.queryIntentActivities(launcherIntent, 0)
            // Dedupe before loading anything: a package exposing several launcher activities would
            // otherwise pay for a label and an icon per activity, only for the extras to be dropped.
            .distinctBy { resolveInfo -> resolveInfo.activityInfo.packageName }
            .map { resolveInfo ->
                LaunchableApp(
                    packageName = resolveInfo.activityInfo.packageName,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    icon = rasterizeIcon(resolveInfo, packageManager, iconSizePx)
                )
            }
            .sortedBy { app -> app.label.lowercase() }
    }

    /**
     * Rasterises a launcher icon at exactly the size the row draws it, never at the drawable's own
     * intrinsic size: icons come from arbitrary third-party packages, and one that reports a huge
     * intrinsic size produces a bitmap the render thread refuses to draw ("trying to draw too large
     * bitmap"). Returns null when a foreign package's icon cannot be loaded or rasterised at all,
     * so one bad icon costs its own row's icon rather than the whole picker.
     */
    private fun rasterizeIcon(
        resolveInfo: ResolveInfo,
        packageManager: PackageManager,
        iconSizePx: Int
    ): ImageBitmap? = try {
        resolveInfo.loadIcon(packageManager)
            .toBitmap(width = iconSizePx, height = iconSizePx)
            .asImageBitmap()
    } catch (error: RuntimeException) {
        val packageName = resolveInfo.activityInfo.packageName
        Log.w(TAG, "Could not rasterise the launcher icon for $packageName", error)
        null
    }
}

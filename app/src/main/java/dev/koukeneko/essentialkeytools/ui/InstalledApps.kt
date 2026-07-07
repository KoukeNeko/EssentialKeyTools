package dev.koukeneko.essentialkeytools.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

/** A launchable app shown in the action picker. */
data class LaunchableApp(
    val packageName: String,
    val label: String,
    val icon: Drawable
)

/**
 * Queries the launcher-visible apps declared reachable by the manifest `<queries>` element. Runs
 * off the main thread by the caller; sorting is by display label so the list is human-scannable.
 */
object InstalledApps {

    fun loadLaunchable(context: Context): List<LaunchableApp> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        return packageManager.queryIntentActivities(launcherIntent, 0)
            .map { resolveInfo ->
                LaunchableApp(
                    packageName = resolveInfo.activityInfo.packageName,
                    label = resolveInfo.loadLabel(packageManager).toString(),
                    icon = resolveInfo.loadIcon(packageManager)
                )
            }
            .distinctBy { app -> app.packageName }
            .sortedBy { app -> app.label.lowercase() }
    }
}

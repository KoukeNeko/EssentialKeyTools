package dev.koukeneko.essentialkeytools.ui

import android.content.Context
import android.content.pm.PackageManager

/** Resolves an installed app's display label from its package name for the home gesture cards. */
object AppLabelResolver {

    /** The app's label, or the package name itself if the app can no longer be resolved. */
    fun labelFor(context: Context, packageName: String): String {
        val packageManager = context.packageManager
        return try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(info).toString()
        } catch (error: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}

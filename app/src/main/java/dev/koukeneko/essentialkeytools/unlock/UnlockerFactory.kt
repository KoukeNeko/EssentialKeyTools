package dev.koukeneko.essentialkeytools.unlock

import android.content.Context

/**
 * Builds an [EssentialKeyUnlocker] wired to the real Android [PackageManagerStateReader].
 */
object UnlockerFactory {

    fun create(context: Context): EssentialKeyUnlocker =
        EssentialKeyUnlocker(
            stateReader = PackageManagerStateReader(context)
        )
}

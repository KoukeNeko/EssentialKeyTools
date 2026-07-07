package dev.koukeneko.essentialkeytools.unlock

import android.content.Context

/**
 * Builds an [EssentialKeyUnlocker] wired to the real Android [PackageManagerStateReader] and the
 * [ShizukuShellCommandRunner]. Screens use this so they never touch the concrete collaborators,
 * keeping the Shizuku dependency behind the unlock package boundary.
 */
object UnlockerFactory {

    fun create(context: Context): EssentialKeyUnlocker =
        EssentialKeyUnlocker(
            stateReader = PackageManagerStateReader(context),
            shellRunner = ShizukuShellCommandRunner()
        )

    /**
     * Builds an [AccessibilityServiceController] wired to the same [ShizukuShellCommandRunner], so
     * the home screen can enable/disable the accessibility service through secure settings without a
     * PC. Kept here so the Shizuku dependency stays behind the unlock package boundary.
     */
    fun createServiceController(): AccessibilityServiceController =
        AccessibilityServiceController(shellRunner = ShizukuShellCommandRunner())
}

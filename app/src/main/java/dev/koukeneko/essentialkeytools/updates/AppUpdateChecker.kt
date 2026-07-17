package dev.koukeneko.essentialkeytools.updates

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class UpdateSource {
    PLAY_STORE,
    GITHUB_STABLE,
    GITHUB_PREVIEW
}

sealed interface UpdateDestination {
    data object PlayStore : UpdateDestination
    data class GitHubRelease(val url: String) : UpdateDestination
}

sealed interface UpdateCheckResult {
    data object UpToDate : UpdateCheckResult
    data class Available(
        val versionName: String?,
        val destination: UpdateDestination
    ) : UpdateCheckResult
}

interface AppUpdateChecker {
    val currentVersionName: String
    val source: UpdateSource

    suspend fun check(): Result<UpdateCheckResult>
}

/** Chooses a policy-safe update source from the package name and verified installation source. */
internal fun chooseUpdateSource(packageName: String, installingPackageName: String?): UpdateSource =
    when {
        packageName.endsWith(PREVIEW_APPLICATION_ID_SUFFIX) -> UpdateSource.GITHUB_PREVIEW
        installingPackageName == PLAY_STORE_PACKAGE -> UpdateSource.PLAY_STORE
        else -> UpdateSource.GITHUB_STABLE
    }

object AppUpdateCheckerFactory {
    fun create(context: Context): AppUpdateChecker {
        val appContext = context.applicationContext
        val currentVersionName = readCurrentVersionName(appContext)
        val installingPackageName = runCatching {
            appContext.packageManager
                .getInstallSourceInfo(appContext.packageName)
                .installingPackageName
        }.getOrNull()

        return when (chooseUpdateSource(appContext.packageName, installingPackageName)) {
            UpdateSource.PLAY_STORE -> PlayStoreUpdateChecker(appContext, currentVersionName)
            UpdateSource.GITHUB_STABLE -> GitHubAppUpdateChecker(
                currentVersionName = currentVersionName,
                source = UpdateSource.GITHUB_STABLE,
                channel = GitHubReleaseChannel.STABLE
            )
            UpdateSource.GITHUB_PREVIEW -> GitHubAppUpdateChecker(
                currentVersionName = currentVersionName,
                source = UpdateSource.GITHUB_PREVIEW,
                channel = GitHubReleaseChannel.PREVIEW
            )
        }
    }

    private fun readCurrentVersionName(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(0)
        ).versionName
    }.getOrNull().orEmpty().ifEmpty { UNKNOWN_VERSION_NAME }
}

private class GitHubAppUpdateChecker(
    override val currentVersionName: String,
    override val source: UpdateSource,
    private val channel: GitHubReleaseChannel,
    private val service: GitHubReleasesService = GitHubReleasesService()
) : AppUpdateChecker {

    override suspend fun check(): Result<UpdateCheckResult> = service.fetchLatest(channel).mapCatching {
        latestRelease ->
        val currentVersion = AppVersion.parse(currentVersionName)
            ?: throw IOException("Unsupported installed version: $currentVersionName")

        if (latestRelease.version > currentVersion) {
            UpdateCheckResult.Available(
                versionName = latestRelease.versionName,
                destination = UpdateDestination.GitHubRelease(latestRelease.pageUrl)
            )
        } else {
            UpdateCheckResult.UpToDate
        }
    }
}

/**
 * Play-installed builds use Play's update availability API and never fall back to an external APK.
 * The UI opens the Play listing when an update is reported, leaving installation to Google Play.
 */
private class PlayStoreUpdateChecker(
    context: Context,
    override val currentVersionName: String
) : AppUpdateChecker {
    override val source = UpdateSource.PLAY_STORE
    private val appUpdateManager = AppUpdateManagerFactory.create(context)

    override suspend fun check(): Result<UpdateCheckResult> = runCatching {
        suspendCancellableCoroutine { continuation ->
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    if (!continuation.isActive) return@addOnSuccessListener
                    val updateAvailable = when (info.updateAvailability()) {
                        UpdateAvailability.UPDATE_AVAILABLE,
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> true
                        else -> false
                    }
                    continuation.resume(
                        if (updateAvailable) {
                            UpdateCheckResult.Available(
                                versionName = null,
                                destination = UpdateDestination.PlayStore
                            )
                        } else {
                            UpdateCheckResult.UpToDate
                        }
                    )
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                }
        }
    }
}

private const val PLAY_STORE_PACKAGE = "com.android.vending"
private const val PREVIEW_APPLICATION_ID_SUFFIX = ".preview"
private const val UNKNOWN_VERSION_NAME = "—"

package dev.koukeneko.essentialkeytools.updates

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

enum class GitHubReleaseChannel {
    STABLE,
    PREVIEW
}

data class GitHubAppRelease(
    val versionName: String,
    val version: AppVersion,
    val pageUrl: String
)

/** Parses published GitHub releases and selects the greatest valid version for one channel. */
object GitHubReleasesParser {
    fun latestForChannel(json: String, channel: GitHubReleaseChannel): GitHubAppRelease? {
        val entries = JSONArray(json)
        val releases = mutableListOf<GitHubAppRelease>()
        for (index in 0 until entries.length()) {
            val entry = entries.optJSONObject(index) ?: continue
            if (entry.optBoolean(DRAFT_FIELD, false)) continue

            val tagName = entry.optString(TAG_NAME_FIELD)
            val prerelease = entry.optBoolean(PRERELEASE_FIELD, false)
            val matchesChannel = when (channel) {
                GitHubReleaseChannel.STABLE -> !prerelease && tagName.startsWith(STABLE_TAG_PREFIX)
                GitHubReleaseChannel.PREVIEW ->
                    prerelease && tagName.startsWith(PREVIEW_TAG_PREFIX)
            }
            if (!matchesChannel) continue

            val version = AppVersion.parse(tagName) ?: continue
            val pageUrl = entry.optString(PAGE_URL_FIELD)
            if (!pageUrl.startsWith(TRUSTED_RELEASE_URL_PREFIX)) continue
            releases += GitHubAppRelease(
                versionName = tagName.removePrefix(PREVIEW_TAG_PREFIX).removePrefix(STABLE_TAG_PREFIX),
                version = version,
                pageUrl = pageUrl
            )
        }
        return releases.maxByOrNull { it.version }
    }

    private const val DRAFT_FIELD = "draft"
    private const val TAG_NAME_FIELD = "tag_name"
    private const val PRERELEASE_FIELD = "prerelease"
    private const val PAGE_URL_FIELD = "html_url"
    private const val STABLE_TAG_PREFIX = "v"
    private const val PREVIEW_TAG_PREFIX = "preview-v"
    private const val TRUSTED_RELEASE_URL_PREFIX =
        "https://github.com/KoukeNeko/EssentialKeyTools/releases/"
}

/** Fetches public release metadata only; it never downloads an APK or executable content. */
class GitHubReleasesService(private val endpoint: String = DEFAULT_ENDPOINT) {
    suspend fun fetchLatest(channel: GitHubReleaseChannel): Result<GitHubAppRelease> =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection = openConnection()
                try {
                    val status = connection.responseCode
                    if (status != HttpURLConnection.HTTP_OK) {
                        throw IOException("GitHub API returned HTTP $status")
                    }
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    GitHubReleasesParser.latestForChannel(body, channel)
                        ?: throw IOException("No published release found for $channel")
                } finally {
                    connection.disconnect()
                }
            }
        }

    private fun openConnection(): HttpURLConnection =
        (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", ACCEPT_HEADER)
        }

    companion object {
        private const val DEFAULT_ENDPOINT =
            "https://api.github.com/repos/KoukeNeko/EssentialKeyTools/releases?per_page=30"
        private const val USER_AGENT = "EssentialKeyTools-App"
        private const val ACCEPT_HEADER = "application/vnd.github+json"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
    }
}

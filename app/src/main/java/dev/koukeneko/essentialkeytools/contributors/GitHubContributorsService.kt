package dev.koukeneko.essentialkeytools.contributors

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches the repository's contributor list from the GitHub REST API over HTTPS. Network, HTTP, and
 * parsing failures are folded into a [Result] so the UI can fall back to a muted caption instead of
 * crashing when the device is offline or the API is unavailable.
 *
 * The endpoint is injectable so the fetch can be pointed at a stub in instrumentation tests; the
 * pure JSON mapping is covered separately by [GitHubContributorsParser]'s unit tests.
 */
class GitHubContributorsService(private val endpoint: String = DEFAULT_ENDPOINT) {

    suspend fun fetchContributors(): Result<List<Contributor>> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = openConnection()
            try {
                val status = connection.responseCode
                if (status != HttpURLConnection.HTTP_OK) {
                    throw IOException("GitHub API returned HTTP $status")
                }
                val body = connection.inputStream.bufferedReader().use { reader -> reader.readText() }
                GitHubContributorsParser.parse(body)
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun openConnection(): HttpURLConnection {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        // GitHub rejects requests without a User-Agent and versions its API through the Accept header.
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", ACCEPT_HEADER)
        return connection
    }

    companion object {
        private const val DEFAULT_ENDPOINT =
            "https://api.github.com/repos/KoukeNeko/EssentialKeyTools/contributors"
        private const val USER_AGENT = "EssentialKeyTools-App"
        private const val ACCEPT_HEADER = "application/vnd.github+json"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
    }
}

package dev.koukeneko.essentialkeytools.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GitHubReleasesParserTest {

    @Test
    fun `selects greatest published stable release`() {
        val latest = GitHubReleasesParser.latestForChannel(RELEASES, GitHubReleaseChannel.STABLE)

        assertEquals("1.0.8", latest?.versionName)
        assertEquals(
            "https://github.com/KoukeNeko/EssentialKeyTools/releases/tag/v1.0.8",
            latest?.pageUrl
        )
    }

    @Test
    fun `selects greatest preview and ignores draft or untrusted entries`() {
        val latest = GitHubReleasesParser.latestForChannel(RELEASES, GitHubReleaseChannel.PREVIEW)

        assertEquals("1.0.9-pre.37", latest?.versionName)
    }

    @Test
    fun `returns null when channel has no valid release`() {
        val json = """
            [
              {
                "tag_name": "preview-v1.0.9-pre.99",
                "html_url": "https://example.com/fake.apk",
                "draft": false,
                "prerelease": true
              }
            ]
        """.trimIndent()

        assertNull(GitHubReleasesParser.latestForChannel(json, GitHubReleaseChannel.PREVIEW))
    }

    private companion object {
        val RELEASES = """
            [
              {
                "tag_name": "v1.0.7",
                "html_url": "https://github.com/KoukeNeko/EssentialKeyTools/releases/tag/v1.0.7",
                "draft": false,
                "prerelease": false
              },
              {
                "tag_name": "preview-v1.0.9-pre.37",
                "html_url": "https://github.com/KoukeNeko/EssentialKeyTools/releases/tag/preview-v1.0.9-pre.37",
                "draft": false,
                "prerelease": true
              },
              {
                "tag_name": "v1.0.8",
                "html_url": "https://github.com/KoukeNeko/EssentialKeyTools/releases/tag/v1.0.8",
                "draft": false,
                "prerelease": false
              },
              {
                "tag_name": "preview-v1.0.9-pre.38",
                "html_url": "https://github.com/KoukeNeko/EssentialKeyTools/releases/tag/preview-v1.0.9-pre.38",
                "draft": true,
                "prerelease": true
              },
              {
                "tag_name": "preview-v9.0.0-pre.1",
                "html_url": "https://example.com/fake.apk",
                "draft": false,
                "prerelease": true
              }
            ]
        """.trimIndent()
    }
}

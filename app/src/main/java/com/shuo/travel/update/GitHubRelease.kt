package com.shuo.travel.update

import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable
import java.net.URI

/** Stable numeric versions only; preview/draft releases never replace production. */
@Serializable data class ReleaseVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion): Int = compareValuesBy(this, other,
        ReleaseVersion::major, ReleaseVersion::minor, ReleaseVersion::patch)
    override fun toString() = "$major.$minor.$patch"
    companion object {
        fun parse(text: String): ReleaseVersion? {
            val match = Regex("v?(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)").matchEntire(text) ?: return null
            val numbers = match.groupValues.drop(1).map { it.toIntOrNull() ?: return null }
            return ReleaseVersion(numbers[0], numbers[1], numbers[2])
        }
    }
}

@Serializable data class GitHubRelease(val version: ReleaseVersion, val url: String, val size: Long, val sha256: String?) {
    companion object {
        const val MAX_APK_BYTES = 150L * 1024 * 1024
        fun parse(json: String, repository: String, installed: String): GitHubRelease? {
            val root = Json.parseToJsonElement(json).jsonObject
            require(root["draft"]?.jsonPrimitive?.boolean == false)
            if (root["prerelease"]?.jsonPrimitive?.boolean != false) return null
            val tag = root.getValue("tag_name").jsonPrimitive.content
            val version = ReleaseVersion.parse(tag) ?: return null
            val current = ReleaseVersion.parse(installed) ?: return null
            if (version <= current) return null
            val name = "Shuo-$version-release.apk"
            val asset = root.getValue("assets").jsonArray.map { it.jsonObject }
                .single { it["name"]?.jsonPrimitive?.content == name }
            require(asset["state"]?.jsonPrimitive?.content == "uploaded")
            val size = asset.getValue("size").jsonPrimitive.long
            require(size in 1..MAX_APK_BYTES)
            val url = asset.getValue("browser_download_url").jsonPrimitive.content
            val expected = "https://github.com/$repository/releases/download/$tag/$name"
            require(url == expected)
            val digest = asset["digest"]?.jsonPrimitive?.contentOrNull
            require(digest == null || digest.matches(Regex("sha256:[a-fA-F0-9]{64}")))
            return GitHubRelease(version, url, size, digest?.substringAfter(':')?.lowercase())
        }

        fun allowedUrl(url: String): Boolean = runCatching {
            val uri = URI(url)
            uri.scheme == "https" && uri.userInfo == null && uri.port in listOf(-1, 443) &&
                uri.host in setOf("api.github.com", "github.com", "release-assets.githubusercontent.com", "objects.githubusercontent.com")
        }.getOrDefault(false)
    }
}

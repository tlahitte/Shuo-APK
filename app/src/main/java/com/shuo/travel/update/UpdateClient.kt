package com.shuo.travel.update

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

enum class UpdateFailure { NETWORK, UNAVAILABLE, INVALID, INSTALL }
class UpdateException(val failure: UpdateFailure) : IOException()

interface UpdateClient {
    suspend fun latest(): GitHubRelease?
    suspend fun download(release: GitHubRelease, progress: (Float) -> Unit): File
    suspend fun verify(file: File, release: GitHubRelease)
}

class GitHubUpdateClient(private val context: Context, private val repository: String,
    private val installedVersion: String,
    private val openConnection: (URL) -> HttpURLConnection = { it.openConnection() as HttpURLConnection }) : UpdateClient {
    private fun connection(url: String): HttpURLConnection {
        var address = url
        repeat(5) {
            if (!GitHubRelease.allowedUrl(address)) throw UpdateException(UpdateFailure.INVALID)
            val connection = openConnection(URL(address)).apply {
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Shuo/$installedVersion")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            }
            try {
                when (connection.responseCode) {
                    200 -> return connection
                    301, 302, 303, 307, 308 -> {
                        val location = connection.getHeaderField("Location") ?: throw UpdateException(UpdateFailure.INVALID)
                        address = URL(URL(address), location).toString()
                    }
                    401, 403, 404, 429 -> throw UpdateException(UpdateFailure.UNAVAILABLE)
                    else -> throw UpdateException(UpdateFailure.NETWORK)
                }
            } catch (error: Exception) { connection.disconnect(); throw error }
            connection.disconnect()
        }
        throw UpdateException(UpdateFailure.INVALID)
    }

    override suspend fun latest(): GitHubRelease? = withContext(Dispatchers.IO) {
        if (repository.isBlank()) throw UpdateException(UpdateFailure.UNAVAILABLE)
        val connection = connection("https://api.github.com/repos/$repository/releases/latest")
        try {
            val bytes = connection.inputStream.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = stream.read(buffer)
                    if (read < 0) break
                    if (output.size() + read > 1024 * 1024) throw UpdateException(UpdateFailure.INVALID)
                    output.write(buffer, 0, read)
                }
                output.toByteArray()
            }
            try { GitHubRelease.parse(bytes.toString(Charsets.UTF_8), repository, installedVersion) }
            catch (_: Exception) { throw UpdateException(UpdateFailure.INVALID) }
        } finally { connection.disconnect() }
    }

    override suspend fun download(release: GitHubRelease, progress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        directory.listFiles()?.forEach { it.delete() }
        val partial = File(directory, "download.part")
        val apk = File(directory, "Shuo-${release.version}.apk")
        try {
            val connection = connection(release.url)
            try {
                if (connection.contentLengthLong > 0 && connection.contentLengthLong != release.size)
                    throw UpdateException(UpdateFailure.INVALID)
                connection.inputStream.use { input -> partial.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var size = 0L
                    var lastPercent = -1
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        size += read
                        if (size > release.size) throw UpdateException(UpdateFailure.INVALID)
                        output.write(buffer, 0, read)
                        val percent = (size * 100 / release.size).toInt()
                        if (percent != lastPercent) { progress(size.toFloat() / release.size); lastPercent = percent }
                    }
                    if (size != release.size) throw UpdateException(UpdateFailure.INVALID)
                } }
            } finally { connection.disconnect() }
            verify(partial, release)
            currentCoroutineContext().ensureActive()
            if (!partial.renameTo(apk)) throw UpdateException(UpdateFailure.INVALID)
            apk
        } finally { partial.delete() }
    }

    @Suppress("DEPRECATION")
    override suspend fun verify(file: File, release: GitHubRelease): Unit = withContext(Dispatchers.IO) {
        if (!file.isFile || file.length() != release.size) throw UpdateException(UpdateFailure.INVALID)
        if (release.sha256 != null) {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            if (hash != release.sha256) throw UpdateException(UpdateFailure.INVALID)
        }
        val manager = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val installed = manager.getPackageInfo(context.packageName, flags)
        val archive = manager.getPackageArchiveInfo(file.absolutePath, flags) ?: throw UpdateException(UpdateFailure.INVALID)
        fun code(info: PackageInfo) = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
        fun signers(info: PackageInfo): Set<String> = (if (Build.VERSION.SDK_INT >= 28)
            info.signingInfo?.apkContentsSigners else info.signatures)?.map { it.toCharsString() }?.toSet().orEmpty()
        if (archive.packageName != context.packageName || archive.versionName != release.version.toString() ||
            code(archive) <= code(installed) || signers(archive).isEmpty() || signers(archive) != signers(installed))
            throw UpdateException(UpdateFailure.INVALID)
    }
}

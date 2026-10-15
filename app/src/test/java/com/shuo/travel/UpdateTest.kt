package com.shuo.travel

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.Signature
import androidx.test.core.app.ApplicationProvider
import com.shuo.travel.update.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [27], application = android.app.Application::class)
class UpdateTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val release = GitHubRelease(ReleaseVersion(0, 3, 0),
        "https://github.com/tlahitte/Shuo-APK/releases/download/v0.3.0/Shuo-0.3.0-release.apk", 3, null)
    private fun metadata(tag: String = "v0.3.0", version: String = "0.3.0") = """{
        "draft":false,"prerelease":false,"tag_name":"$tag","assets":[{
        "name":"Shuo-$version-release.apk","state":"uploaded","size":3,"digest":null,
        "browser_download_url":"https://github.com/tlahitte/Shuo-APK/releases/download/$tag/Shuo-$version-release.apk"}]}"""
    private fun parse(json: String) = GitHubRelease.parse(json, "tlahitte/Shuo-APK", "0.2.0")
    @Before fun clear() { context.getSharedPreferences("updates", Context.MODE_PRIVATE).edit().clear().commit() }

    @Test fun versionsCompareNumericallyAndRejectPreviews() {
        assertTrue(ReleaseVersion.parse("v0.10.0")!! > ReleaseVersion.parse("0.9.9")!!)
        assertTrue(ReleaseVersion.parse("1.0.0")!! > ReleaseVersion.parse("0.99.99")!!)
        for (value in listOf("0.2.0-beta", "v0.2", "0.02.0", "0.2.0/evil", "2147483648.0.0")) assertNull(value, ReleaseVersion.parse(value))
        assertNull(parse(metadata("v0.2.0", "0.2.0")))
        assertNull(parse(metadata("v0.1.0", "0.1.0")))
        assertNull(parse(metadata().replace("\"prerelease\":false", "\"prerelease\":true")))
    }

    @Test fun releaseSelectsOnlyExpectedAssetAndRepository() {
        assertEquals(release, parse(metadata()))
        for (json in listOf(metadata().replace("tlahitte/Shuo-APK/releases", "attacker/Shuo-APK/releases"),
            metadata().replace("https://github.com", "http://github.com"),
            metadata().replace("\"size\":3", "\"size\":0"),
            metadata().replace("\"size\":3", "\"size\":999999999"),
            metadata().replace("\"digest\":null", "\"digest\":\"sha256:invalid\""),
            metadata().replace("\"state\":\"uploaded\"", "\"state\":\"new\""),
            metadata().replace("\"draft\":false", "\"draft\":true"))) {
            assertThrows(Exception::class.java) { parse(json) }
        }
    }

    @Test fun redirectsStayOnGitHubHttpsHosts() {
        for (url in listOf(release.url, "https://release-assets.githubusercontent.com/path?signature=public-download"))
            assertTrue(GitHubRelease.allowedUrl(url))
        for (url in listOf("http://github.com/a", "https://github.com.evil.test/a", "https://github.com@evil.test/a",
            "https://user:password@github.com/a", "https://github.com:8080/a", "file:///etc/passwd"))
            assertFalse(url, GitHubRelease.allowedUrl(url))
    }

    private class FakeClient(var result: GitHubRelease? = null) : UpdateClient {
        var checks = 0
        var failure: Exception? = null
        var waitForDownload = false
        override suspend fun latest(): GitHubRelease? { checks++; failure?.let { throw it }; return result }
        override suspend fun download(release: GitHubRelease, progress: (Float) -> Unit): File {
            progress(.5f)
            if (waitForDownload) awaitCancellation()
            failure?.let { throw it }
            return File("fake.apk")
        }
        override suspend fun verify(file: File, release: GitHubRelease) { failure?.let { throw it } }
    }

    @Test fun autoCheckIsThrottledPersistentAndOptional() = runTest {
        var now = 100_000_000L
        val client = FakeClient()
        val controller = UpdateController(context, this, client, { now })
        controller.check(false); advanceUntilIdle()
        assertEquals(UpdatePhase.CURRENT, controller.state.value.phase)
        controller.check(false); advanceUntilIdle()
        UpdateController(context, this, client, { now }).check(false); advanceUntilIdle()
        assertEquals(1, client.checks)
        now += 86_400_000
        controller.automatic(false)
        val reopened = UpdateController(context, this, client, { now })
        assertFalse(reopened.autoCheck.value)
        reopened.check(false); advanceUntilIdle(); assertEquals(1, client.checks)
        reopened.check(); advanceUntilIdle(); assertEquals(2, client.checks)
        reopened.automatic(true); now += 86_400_000
        reopened.check(false); advanceUntilIdle(); assertEquals(3, client.checks)
    }

    @Test fun availableReleaseSurvivesRestartAndClearsAfterUpgrade() = runTest {
        val controller = UpdateController(context, this, FakeClient(release))
        controller.check(); advanceUntilIdle()
        assertEquals(release, UpdateController(context, this, FakeClient()).state.value.release)
        assertNull(UpdateController(context, this, FakeClient(), installedVersion = "0.3.0").state.value.release)
    }

    @Test fun offlineAutoCheckIsQuietAndManualCheckCanRetry() = runTest {
        val client = FakeClient().apply { failure = IOException() }
        val controller = UpdateController(context, this, client)
        controller.check(false); advanceUntilIdle()
        assertEquals(UpdatePhase.IDLE, controller.state.value.phase)
        controller.check(); advanceUntilIdle()
        assertEquals(UpdateFailure.NETWORK, controller.state.value.failure)
        client.failure = null
        controller.check(); advanceUntilIdle()
        assertEquals(UpdatePhase.CURRENT, controller.state.value.phase)
    }

    @Test fun downloadCanCancelRetryAndRejectInvalidInstall() = runTest {
        val client = FakeClient(release).apply { waitForDownload = true }
        val controller = UpdateController(context, this, client)
        controller.check(); advanceUntilIdle()
        controller.download(); runCurrent()
        assertEquals(.5f, controller.state.value.progress)
        controller.check(); assertEquals(1, client.checks)
        controller.cancel(); advanceUntilIdle()
        assertEquals(UpdatePhase.AVAILABLE, controller.state.value.phase)
        client.waitForDownload = false
        controller.download(); advanceUntilIdle()
        assertEquals(UpdatePhase.READY, controller.state.value.phase)
        client.failure = UpdateException(UpdateFailure.INVALID)
        var installed = false
        controller.install { installed = true }; advanceUntilIdle()
        assertFalse(installed)
        assertEquals(UpdateFailure.INVALID, controller.state.value.failure)
        client.failure = null
        controller.download(); advanceUntilIdle()
        controller.install { installed = true }; advanceUntilIdle()
        assertTrue(installed)
    }

    @Suppress("DEPRECATION")
    @Test fun downloadValidatesLengthAndDeletesPartialFilesOnFailure() = runTest {
        val manager = shadowOf(context.packageManager)
        val installed = PackageInfo().apply {
            packageName = context.packageName; versionCode = 2; versionName = "0.2.0"; signatures = arrayOf(Signature("1234"))
        }
        val archive = PackageInfo().apply {
            packageName = context.packageName; versionCode = 3; versionName = "0.3.0"; signatures = arrayOf(Signature("1234"))
        }
        manager.addPackage(installed)
        manager.setPackageArchiveInfo(File(context.cacheDir, "updates/download.part").absolutePath, archive)
        var bytes = "abc".toByteArray()
        var disconnected = false
        val client = GitHubUpdateClient(context, "tlahitte/Shuo-APK", "0.2.0") { url ->
            object : HttpURLConnection(url) {
                override fun connect() = Unit
                override fun usingProxy() = false
                override fun disconnect() { disconnected = true }
                override fun getResponseCode() = 200
                override fun getContentLengthLong() = -1L
                override fun getInputStream() = bytes.inputStream()
            }
        }
        var progress = 0f
        val result = client.download(release) { progress = it }
        assertEquals("abc", result.readText())
        assertEquals(1f, progress)
        assertTrue(disconnected)
        for (invalid in listOf("a", "abcd")) {
            bytes = invalid.toByteArray()
            try { client.download(release) { }; fail("Incomplete or oversized APK accepted") }
            catch (error: UpdateException) { assertEquals(UpdateFailure.INVALID, error.failure) }
            assertTrue(File(context.cacheDir, "updates").listFiles().orEmpty().isEmpty())
        }
    }

    @Test fun apiRateLimitsAndUnsafeRedirectsAreHandled() = runTest {
        var status = 403
        var opens = 0
        val client = GitHubUpdateClient(context, "tlahitte/Shuo-APK", "0.2.0") { url ->
            opens++
            object : HttpURLConnection(url) {
                override fun connect() = Unit
                override fun usingProxy() = false
                override fun disconnect() = Unit
                override fun getResponseCode() = status
                override fun getHeaderField(name: String) = "https://attacker.invalid/payload"
            }
        }
        try { client.latest(); fail("Rate limit ignored") }
        catch (error: UpdateException) { assertEquals(UpdateFailure.UNAVAILABLE, error.failure) }
        status = 302
        try { client.latest(); fail("Untrusted redirect followed") }
        catch (error: UpdateException) { assertEquals(UpdateFailure.INVALID, error.failure) }
        assertEquals(2, opens)
    }

    @Suppress("DEPRECATION")
    @Test fun apkRequiresMatchingPackageSignatureVersionAndDigest() = runTest {
        val manager = shadowOf(context.packageManager)
        fun info(name: String = context.packageName, version: Int = 3, signature: String = "1234") = PackageInfo().apply {
            packageName = name; versionCode = version; versionName = "0.3.0"; signatures = arrayOf(Signature(signature))
        }
        manager.addPackage(info(version = 2))
        val file = File(context.cacheDir, "candidate.apk").apply { writeText("abc") }
        val client = GitHubUpdateClient(context, "tlahitte/Shuo-APK", "0.2.0")
        manager.setPackageArchiveInfo(file.absolutePath, info())
        client.verify(file, release)
        client.verify(file, release.copy(sha256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"))
        suspend fun rejected(block: suspend () -> Unit) {
            try { block(); fail("Untrusted APK accepted") } catch (error: UpdateException) { assertEquals(UpdateFailure.INVALID, error.failure) }
        }
        rejected { client.verify(file, release.copy(sha256 = "0".repeat(64))) }
        for (invalid in listOf(info(name = "com.other"), info(version = 2), info(signature = "5678"), info().apply { versionName = "0.4.0" }, info().apply { signatures = emptyArray() })) {
            manager.setPackageArchiveInfo(file.absolutePath, invalid)
            rejected { client.verify(file, release) }
        }
        manager.setPackageArchiveInfo(file.absolutePath, info())
        file.writeText("truncated")
        rejected { client.verify(file, release) }
    }
}

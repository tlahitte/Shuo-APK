package com.shuo.travel

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.shuo.travel.domain.Language
import com.shuo.travel.ui.inLanguage
import com.shuo.travel.ui.screens.UpdateSection
import com.shuo.travel.ui.theme.ShuoTheme
import com.shuo.travel.data.Appearance
import com.shuo.travel.update.*
import kotlinx.coroutines.*
import org.junit.*
import java.io.File

class UpdateFlowTest {
    @get:Rule val compose = createComposeRule()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    @After fun stop() { scope.cancel() }

    @Test fun availableDownloadCancelRetryAndInstallPrompt() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("updates", Context.MODE_PRIVATE).edit().clear().commit()
        var finish = false
        var reject = true
        val release = GitHubRelease(ReleaseVersion(0, 3, 0), "https://github.com/tlahitte/Shuo-APK/releases/download/v0.3.0/Shuo-0.3.0-release.apk", 3, null)
        val client = object : UpdateClient {
            override suspend fun latest() = release
            override suspend fun download(release: GitHubRelease, progress: (Float) -> Unit): File {
                progress(.5f)
                if (!finish) awaitCancellation()
                if (reject) throw UpdateException(UpdateFailure.INVALID)
                return File(context.cacheDir, "test.apk")
            }
            override suspend fun verify(file: File, release: GitHubRelease) = Unit
        }
        val controller = UpdateController(context, scope, client)
        compose.setContent {
            val activityContext = LocalContext.current
            CompositionLocalProvider(LocalContext provides activityContext.inLanguage(Language.FRENCH)) {
                ShuoTheme(Appearance.LIGHT) { UpdateSection(controller) }
            }
        }
        compose.onNodeWithTag("update-check").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("update-download").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("La version 0.3.0 est disponible.").assertIsDisplayed()
        compose.onNodeWithTag("update-download").performClick()
        compose.onNodeWithText("50 % téléchargés").assertIsDisplayed()
        compose.onNodeWithText("Annuler").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("update-download").fetchSemanticsNodes().isNotEmpty() }
        compose.runOnIdle { finish = true }
        compose.onNodeWithTag("update-download").performClick()
        compose.waitUntil(5000) { controller.state.value.phase == UpdatePhase.ERROR }
        compose.onNodeWithText("Impossible de valider cette mise à jour.", substring = true).assertIsDisplayed()
        compose.runOnIdle { reject = false }
        compose.onNodeWithTag("update-download").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithTag("update-install").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("La mise à jour est prête à installer.").assertIsDisplayed()
        compose.onNodeWithText("Android peut demander", substring = true).assertIsDisplayed()
    }
}

package com.shuo.travel

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.shuo.travel.data.Appearance
import com.shuo.travel.domain.Phrase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.Assert.*
import java.io.File

class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application as ShuoApplication
    @Before fun reset() {
        runBlocking {
            app.database.clearAllTables()
            app.library.seedOnce(app.assets.open("starter-library.json").bufferedReader().use { it.readText() })
            app.preferences.appearance(Appearance.LIGHT)
            app.preferences.scale(1f)
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Bonjour, la Chine !").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun search(query: String) {
        compose.onNodeWithText("Chercher une phrase en français").performClick()
        compose.onNodeWithTag("search-input").performTextReplacement(query)
        compose.activityRule.scenario.onActivity { activity ->
            androidx.core.view.WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
        }
        compose.waitForIdle()
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        val image = instrumentation.uiAutomation.takeScreenshot()
        val folder = File(app.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        val file = File(folder, "$name.png")
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        // The connected test runner may uninstall the app after the suite.
        // Keep evidence in the emulator shell's temporary directory before teardown.
        for (command in listOf("mkdir -p /data/local/tmp/shuo-verification",
            "cp ${file.absolutePath} /data/local/tmp/shuo-verification/$name.png")) {
            val output = android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
                .use { it.readBytes().toString(Charsets.UTF_8) }
            assertTrue("Screenshot retention failed: $output", output.isBlank())
        }
    }

    @Test fun searchPresentationFavoritesRotationAndHistory() {
        search("bu yao")
        compose.onNodeWithTag("phrase-no-spice").assertIsDisplayed().performClick()
        compose.onNodeWithText("Pas de piment, s’il vous plaît.").assertDoesNotExist()
        compose.onNodeWithText("不要辣").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tourner la carte à 180 degrés").performClick()
        assertTrue(compose.activity.window.decorView.findViewById<android.view.View>(android.R.id.content).keepScreenOn ||
            compose.activity.window.decorView.keepScreenOn || run { var found = false
                fun walk(v: android.view.View) { if (v.keepScreenOn) found = true; if (v is android.view.ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i)) }
                walk(compose.activity.window.decorView); found })
        compose.onNodeWithContentDescription("Retirer des favoris").performClick()
        compose.onNodeWithContentDescription("Ajouter aux favoris").assertExists()
        compose.onNodeWithText("Afficher la traduction").performClick()
        compose.onNodeWithText("Pas de piment, s’il vous plaît.").assertIsDisplayed()
        compose.onNodeWithContentDescription("Masquer la traduction").performClick()
        compose.onNodeWithText("Pas de piment, s’il vous plaît.").assertDoesNotExist()
        compose.onNodeWithContentDescription("Retour").performClick()
        compose.onNodeWithContentDescription("Retour").performClick()
        compose.onNodeWithText("Historique").performClick()
        compose.onNodeWithTag("phrase-no-spice").assertExists()
        runBlocking { assertNotNull(app.database.libraryDao().phrase("no-spice")!!.content.lastUsedAt) }
    }

    @Test fun addEditDeleteAndActivityRecreation() {
        compose.onNodeWithContentDescription("Ajouter une carte").performClick()
        compose.onNodeWithTag("french-input").performTextInput("Ma carte de test")
        compose.onNodeWithTag("chinese-input").performTextInput("谢谢你的帮助")
        compose.onNodeWithTag("pinyin-input").performTextInput("Xièxie nǐ de bāngzhù")
        compose.onNodeWithTag("save-phrase").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Bonjour, la Chine !").fetchSemanticsNodes().isNotEmpty() }
        search("Ma carte de test")
        val id = runBlocking { app.database.libraryDao().phrases().first { it.content.french == "Ma carte de test" }.content.id }
        compose.onNodeWithTag("phrase-$id").performClick()
        compose.onNodeWithText("Modifier").performClick()
        compose.onNodeWithTag("french-input").performTextReplacement("Carte modifiée")
        compose.onNodeWithTag("save-phrase").performClick()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("MONTRER LA CARTE").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Afficher la traduction").performClick()
        compose.onNodeWithText("Carte modifiée").assertIsDisplayed()
        compose.onNodeWithText("Modifier").performClick()
        compose.onNodeWithText("Supprimer cette carte").performScrollTo().performClick()
        compose.onNodeWithText("Supprimer", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        runBlocking { assertNull(app.database.libraryDao().phrase(id)) }
    }

    @Test fun capturesScreensInBothThemes() {
        runBlocking { app.library.markUsed("price") }
        for ((appearance, suffix) in listOf(Appearance.LIGHT to "light", Appearance.DARK to "dark")) {
            runBlocking { app.preferences.appearance(appearance) }
            compose.waitForIdle()
            screenshot("home-$suffix")
            search("restaurant")
            screenshot("search-$suffix")
            compose.onNodeWithTag("search-list").performScrollToNode(hasTestTag("phrase-no-spice"))
            compose.onNodeWithTag("phrase-no-spice").performClick()
            screenshot("presentation-$suffix")
            compose.onNodeWithText("Modifier").performClick()
            screenshot("edit-$suffix")
            compose.onNodeWithContentDescription("Retour").performClick()
            compose.onNodeWithContentDescription("Retour").performClick()
            compose.onNodeWithTag("nav-favorites").performClick()
            screenshot("favorites-$suffix")
            compose.onNodeWithTag("nav-settings").performClick()
            screenshot("settings-$suffix")
            compose.onNodeWithTag("nav-home").performClick()
        }
    }

    @Test fun longChinesePhraseAndIncreasedTextSize() {
        val longText = "你好，我不会说中文。请问你可以帮我找到最近的地铁站吗？非常感谢你的帮助。"
        runBlocking {
            app.library.savePhrase(Phrase("long-test", longText, "Nǐ hǎo, wǒ bú huì shuō Zhōngwén. Qǐngwèn nǐ kěyǐ bāng wǒ zhǎodào zuìjìn de dìtiězhàn ma? Fēicháng gǎnxiè nǐ de bāngzhù.", "Longue phrase de test", categoryId = "communication"))
            app.preferences.scale(1.5f)
        }
        search("Longue phrase de test")
        compose.onNodeWithTag("phrase-long-test").performClick()
        compose.onNodeWithText(longText).assertIsDisplayed()
        screenshot("presentation-long-150-percent")
    }

    @Test fun categoryManagementThemePersistenceAndUnsavedFormGuard() {
        compose.onNodeWithTag("nav-settings").performClick()
        compose.onNodeWithContentDescription("Créer une catégorie").performClick()
        compose.onNodeWithText("Nom").performTextInput("Personnel")
        compose.onNodeWithText("Enregistrer", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        val id = runBlocking { app.database.libraryDao().categories().first { it.content.name == "Personnel" }.content.id }
        compose.onNodeWithText("Personnel").performScrollTo().performClick()
        compose.onNodeWithText("Nom").performTextReplacement("Mes cartes")
        compose.onNodeWithText("Enregistrer", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Sombre").performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Votre bibliothèque").fetchSemanticsNodes().isNotEmpty() }
        runBlocking { assertEquals(Appearance.DARK, app.preferences.preferences.first().appearance) }
        compose.onNodeWithText("Mes cartes").performScrollTo().performClick()
        compose.onNodeWithText("Supprimer la catégorie").performScrollTo().performClick()
        compose.onNodeWithText("Supprimer", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        runBlocking { assertTrue(app.database.libraryDao().categories().none { it.content.id == id }) }
        compose.onNodeWithTag("nav-home").performClick()
        compose.onNodeWithContentDescription("Ajouter une carte").performClick()
        compose.onNodeWithTag("french-input").performTextInput("Brouillon")
        compose.onNodeWithTag("nav-favorites").performClick()
        compose.onNodeWithText("Quitter sans enregistrer ?").assertIsDisplayed()
        compose.onNodeWithText("Continuer").performClick()
        compose.onNodeWithTag("french-input").assertTextEquals("Brouillon")
        compose.onNodeWithTag("nav-favorites").performClick()
        compose.onNodeWithText("Quitter", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Vos favoris").assertIsDisplayed()
    }
}

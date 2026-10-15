package com.shuo.travel

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.ReportDrawnWhen
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shuo.travel.navigation.ShuoNavigation
import com.shuo.travel.ui.LibraryViewModel
import com.shuo.travel.ui.ShuoLocale
import com.shuo.travel.ui.inLanguage
import com.shuo.travel.data.PreferencesRepository
import com.shuo.travel.domain.Language
import com.shuo.travel.ui.theme.*

class MainActivity : ComponentActivity() {
    private var activityLanguage = Language.FRENCH
    override fun attachBaseContext(newBase: Context) {
        activityLanguage = PreferencesRepository.savedLanguage(newBase)
        super.attachBaseContext(newBase.inLanguage(activityLanguage))
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: LibraryViewModel = viewModel()
            val state by model.state.collectAsStateWithLifecycle()
            LaunchedEffect(state.ready, state.preferences.language) {
                if (state.ready && state.preferences.language != activityLanguage) recreate()
            }
            ReportDrawnWhen { state.ready }
            LaunchedEffect(state.ready) {
                if (state.ready && !BuildConfig.DEBUG) {
                    kotlinx.coroutines.delay(2_000)
                    model.updates.check(manual = false)
                }
            }
            ShuoLocale(state.preferences.language) {
            ShuoTheme(state.preferences.appearance) {
                val dark = LocalDark.current
                val view = LocalView.current
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !dark
                        isAppearanceLightNavigationBars = !dark
                    }
                }
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
                    ShuoNavigation(model, state)
                }
            }
            }
        }
    }
}

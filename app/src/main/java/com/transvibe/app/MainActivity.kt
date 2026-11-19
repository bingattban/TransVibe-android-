package com.transvibe.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.transvibe.app.persistence.UserPreferences
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.TransVibeViewModelFactory
import com.transvibe.app.presentation.navigation.TransVibeNavGraph
import com.transvibe.app.presentation.theme.TransVibeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TransVibeApplication
        val initialJobId = intent?.getStringExtra("extra_job_id")

        setContent {
            val vm: TransVibeViewModel = viewModel(
                factory = TransVibeViewModelFactory(app.container)
            )
            val prefs by vm.preferences.collectAsState(initial = UserPreferences())
            val layoutDir = if (prefs.uiLanguage == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
                TransVibeTheme(themeMode = prefs.themeMode) {
                    TransVibeNavGraph(
                        viewModel = vm,
                        isArabic = prefs.uiLanguage == "ar",
                        onboardingDone = prefs.onboardingCompleted,
                        initialNotificationJobId = initialJobId
                    )
                }
            }
        }
    }
}

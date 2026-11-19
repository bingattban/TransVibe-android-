package com.transvibe.app.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.screens.BenchmarkScreen
import com.transvibe.app.presentation.screens.HistoryScreen
import com.transvibe.app.presentation.screens.HomeScreen
import com.transvibe.app.presentation.screens.ModelDetailsScreen
import com.transvibe.app.presentation.screens.ModelLibraryScreen
import com.transvibe.app.presentation.screens.PlayerPreviewScreen
import com.transvibe.app.presentation.screens.ProcessingScreen
import com.transvibe.app.presentation.screens.ProcessingSetupScreen
import com.transvibe.app.presentation.screens.RecoveryDialog
import com.transvibe.app.presentation.screens.ResultsEditorScreen
import com.transvibe.app.presentation.screens.SettingsScreen
import com.transvibe.app.presentation.screens.TranslationPacksScreen
import com.transvibe.app.presentation.screens.VideoPickerScreen
import com.transvibe.app.presentation.screens.WelcomeScreen
import com.transvibe.app.presentation.theme.TransVibeBackground

object Routes {
    const val WELCOME = "welcome"
    const val HOME = "home"
    const val MODELS = "models"
    const val MODEL_DETAILS = "model_details/{modelId}"
    const val TRANSLATION_PACKS = "translation_packs"
    const val PICKER = "picker"
    const val SETUP = "setup"
    const val PROCESSING = "processing"
    const val RESULTS = "results/{jobId}"
    const val PLAYER = "player/{jobId}"
    const val HISTORY = "history"
    const val BENCHMARK = "benchmark"
    const val SETTINGS = "settings"
}

@Composable
fun TransVibeNavGraph(
    viewModel: TransVibeViewModel,
    isArabic: Boolean,
    onboardingDone: Boolean,
    initialNotificationJobId: String?
) {
    val navController = rememberNavController()
    val errorMsg by viewModel.recoveryError.collectAsState()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val topLevelRoutes = setOf(Routes.HOME, Routes.MODELS, Routes.HISTORY, Routes.SETTINGS)
    val openSection: (String) -> Unit = { destination ->
        if (currentRoute != destination) {
            navController.navigate(destination) {
                popUpTo(Routes.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
    val startDest = when {
        !initialNotificationJobId.isNullOrBlank() -> "results/$initialNotificationJobId"
        onboardingDone -> Routes.HOME
        else -> Routes.WELCOME
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AnimatedVisibility(
                visible = currentRoute in topLevelRoutes,
                enter = fadeIn(animationSpec = tween(200)),
                exit = fadeOut(animationSpec = tween(160))
            ) {
                TransVibeBottomNavigation(
                    currentRoute = currentRoute,
                    isArabic = isArabic,
                    onNavigate = openSection
                )
            }
        }
    ) { innerPadding ->
        TransVibeBackground {
        NavHost(
            navController = navController,
            startDestination = startDest,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.WELCOME) {
                WelcomeScreen(
                    isArabic = isArabic,
                    onSelectLanguage = { viewModel.setUiLanguage(it) },
                    onContinue = {
                        viewModel.completeOnboarding()
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.WELCOME) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = viewModel,
                    isArabic = isArabic,
                    onNewVideo = { navController.navigate(Routes.PICKER) },
                    onModels = { navController.navigate(Routes.MODELS) },
                    onHistory = { navController.navigate(Routes.HISTORY) },
                    onSettings = { navController.navigate(Routes.SETTINGS) },
                    onBenchmark = { navController.navigate(Routes.BENCHMARK) },
                    onOpenJob = { jobId ->
                        viewModel.loadJobCues(jobId)
                        navController.navigate("results/$jobId")
                    }
                )
            }
            composable(Routes.MODELS) {
                ModelLibraryScreen(
                    viewModel = viewModel,
                    isArabic = isArabic,
                    onBack = { navController.popBackStack() },
                    onModelClick = { modelId -> navController.navigate("model_details/$modelId") }
                )
            }
            composable(
                Routes.MODEL_DETAILS,
                arguments = listOf(navArgument("modelId") { type = NavType.StringType })
            ) { backStack ->
                val modelId = backStack.arguments?.getString("modelId") ?: ""
                ModelDetailsScreen(
                    modelId = modelId,
                    viewModel = viewModel,
                    isArabic = isArabic,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.TRANSLATION_PACKS) {
                TranslationPacksScreen(
                    viewModel = viewModel,
                    isArabic = isArabic,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.PICKER) {
                VideoPickerScreen(
                    viewModel = viewModel,
                    isArabic = isArabic,
                    onBack = { navController.popBackStack() },
                    onProceedToSetup = { navController.navigate(Routes.SETUP) }
                )
            }
            composable(Routes.SETUP) {
                ProcessingSetupScreen(
                    viewModel = viewModel,
                    isArabic = isArabic,
                    onBack = { navController.popBackStack() },
                    onOpenModels = { navController.navigate(Routes.MODELS) },
                    onOpenTranslationPacks = { navController.navigate(Routes.TRANSLATION_PACKS) },
                    onStartProcessing = { navController.navigate(Routes.PROCESSING) }
                )
            }
            composable(Routes.PROCESSING) {
                ProcessingScreen(
                    viewModel = viewModel,
                    isArabic = isArabic,
                    onBackHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    },
                    onOpenResults = { jobId ->
                        navController.navigate("results/$jobId") {
                            popUpTo(Routes.HOME)
                        }
                    }
                )
            }
            composable(
                Routes.RESULTS,
                arguments = listOf(navArgument("jobId") { type = NavType.StringType })
            ) { backStack ->
                val jobId = backStack.arguments?.getString("jobId") ?: ""
                ResultsEditorScreen(
                    jobId = jobId,
                    viewModel = viewModel,
                    isArabic = isArabic,
                    onBack = { navController.popBackStack() },
                    onPreviewPlayer = { navController.navigate("player/$jobId") }
                )
            }
            composable(
                Routes.PLAYER,
                arguments = listOf(navArgument("jobId") { type = NavType.StringType })
            ) {
                PlayerPreviewScreen(
                    viewModel = viewModel,
                    isArabic = isArabic,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    viewModel = viewModel,
                    isArabic = isArabic,
                    onBack = { navController.popBackStack() },
                    onOpenJob = { jobId ->
                        viewModel.loadJobCues(jobId)
                        navController.navigate("results/$jobId")
                    }
                )
            }
            composable(Routes.BENCHMARK) {
                BenchmarkScreen(
                    viewModel = viewModel,
                    isArabic = isArabic,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    viewModel = viewModel,
                    isArabic = isArabic,
                    onBack = { navController.popBackStack() },
                    onOpenModels = { navController.navigate(Routes.MODELS) },
                    onOpenTranslationPacks = { navController.navigate(Routes.TRANSLATION_PACKS) }
                )
            }
        }
        }
    }

    errorMsg?.let { msg ->
        RecoveryDialog(
            message = msg,
            isArabic = isArabic,
            onDismiss = { viewModel.dismissError() }
        )
    }
}

/** Persistent four-section navigation; hidden during processing and subtitle editing. */
@Composable
private fun TransVibeBottomNavigation(
    currentRoute: String?,
    isArabic: Boolean,
    onNavigate: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
        shape = RoundedCornerShape(26.dp),
        color = scheme.surfaceContainer,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.65f))
    ) {
        NavigationBar(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            tonalElevation = 0.dp
        ) {
            val destinations = listOf(
                Triple(Routes.HOME, if (isArabic) "الرئيسية" else "Home", Icons.Filled.Home),
                Triple(Routes.MODELS, if (isArabic) "النماذج" else "Models", Icons.Filled.Tune),
                Triple(Routes.HISTORY, if (isArabic) "مشاريعي" else "History", Icons.Filled.History),
                Triple(Routes.SETTINGS, if (isArabic) "الإعدادات" else "Settings", Icons.Filled.Settings)
            )
            destinations.forEach { (route, label, icon) ->
                NavigationBarItem(
                    selected = route == currentRoute,
                    onClick = { onNavigate(route) },
                    icon = { Icon(icon, contentDescription = label) },
                    label = { Text(label, maxLines = 1) },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = scheme.onPrimaryContainer,
                        selectedTextColor = scheme.primary,
                        indicatorColor = scheme.primaryContainer,
                        unselectedIconColor = scheme.onSurfaceVariant,
                        unselectedTextColor = scheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

package com.transvibe.app.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.History as FilledHistory
import androidx.compose.material.icons.filled.Home as FilledHome
import androidx.compose.material.icons.filled.Settings as FilledSettings
import androidx.compose.material.icons.filled.Tune as FilledTune
import androidx.compose.material.icons.outlined.History as OutlinedHistory
import androidx.compose.material.icons.outlined.Home as OutlinedHome
import androidx.compose.material.icons.outlined.Settings as OutlinedSettings
import androidx.compose.material.icons.outlined.Tune as OutlinedTune
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
            ) { backStack ->
                val previewJobId = backStack.arguments?.getString("jobId") ?: ""
                PlayerPreviewScreen(
                    jobId = previewJobId,
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

/** Original owner's four-section navigation silhouette and animated tabs. */
@Composable
private fun TransVibeBottomNavigation(
    currentRoute: String?,
    isArabic: Boolean,
    onNavigate: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(26.dp),
        color = scheme.surfaceContainer,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppNavItem(currentRoute == Routes.HOME, { onNavigate(Routes.HOME) }, Icons.Filled.FilledHome, Icons.Outlined.OutlinedHome, if (isArabic) "الرئيسية" else "Home")
            AppNavItem(currentRoute == Routes.MODELS, { onNavigate(Routes.MODELS) }, Icons.Filled.FilledTune, Icons.Outlined.OutlinedTune, if (isArabic) "النماذج" else "Models")
            AppNavItem(currentRoute == Routes.HISTORY, { onNavigate(Routes.HISTORY) }, Icons.Filled.FilledHistory, Icons.Outlined.OutlinedHistory, if (isArabic) "السجل" else "History")
            AppNavItem(currentRoute == Routes.SETTINGS, { onNavigate(Routes.SETTINGS) }, Icons.Filled.FilledSettings, Icons.Outlined.OutlinedSettings, if (isArabic) "الإعدادات" else "Settings")
        }
    }
}

@Composable
private fun RowScope.AppNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String,
    hasBadge: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val pillColor by animateColorAsState(
        targetValue = if (selected) scheme.primaryContainer else Color.Transparent,
        animationSpec = tween(220),
        label = "navPill"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "navIcon"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .sizeIn(minHeight = 60.dp)
            .clip(RoundedCornerShape(18.dp))
            .semantics { this.selected = selected; role = Role.Tab }
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(pillColor)
                .padding(horizontal = 18.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(scheme.tertiary)
                )
            }
        }
        Text(
            text = label,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


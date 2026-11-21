package com.transvibe.app.presentation.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History as FilledHistory
import androidx.compose.material.icons.filled.Home as FilledHome
import androidx.compose.material.icons.filled.Settings as FilledSettings
import androidx.compose.material.icons.filled.Tune as FilledTune
import androidx.compose.material.icons.outlined.History as OutlinedHistory
import androidx.compose.material.icons.outlined.Home as OutlinedHome
import androidx.compose.material.icons.outlined.Settings as OutlinedSettings
import androidx.compose.material.icons.outlined.Tune as OutlinedTune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.transvibe.app.domain.JobState
import com.transvibe.app.presentation.TransVibeViewModel
import com.transvibe.app.presentation.screens.AboutScreen
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
    const val ABOUT = "about"
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
    val progress by viewModel.processingProgress.collectAsState()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val activity = LocalContext.current as? Activity
    var showExitConfirmation by remember { mutableStateOf(false) }

    val activeProcessing = progress?.state in setOf(
        JobState.CREATED,
        JobState.QUEUED,
        JobState.INSPECTING,
        JobState.DECODING,
        JobState.RECOGNIZING,
        JobState.TRANSLATING,
        JobState.FINALIZING
    )

    val topLevelRoutes = setOf(Routes.HOME, Routes.MODELS, Routes.HISTORY, Routes.SETTINGS, Routes.ABOUT)
    val openSection: (String) -> Unit = { destination ->
        if (currentRoute == Routes.ABOUT && destination == Routes.SETTINGS) {
            navController.popBackStack(Routes.SETTINGS, false)
        } else if (currentRoute != destination) {
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

    BackHandler(enabled = currentRoute == Routes.HOME && !showExitConfirmation) {
        showExitConfirmation = true
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = {
                Text(
                    text = if (isArabic) "تأكيد الخروج" else "Exit TransVibe?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (isArabic) "هل تريد إغلاق التطبيق الآن؟" else "Do you want to close the app now?"
                )
            },
            confirmButton = {
                TextButton(onClick = { showExitConfirmation = false; activity?.finish() }) {
                    Text(if (isArabic) "خروج" else "Exit", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    TransVibeBackground {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                AnimatedVisibility(
                    visible = currentRoute in topLevelRoutes,
                    enter = expandVertically(animationSpec = tween(250), expandFrom = Alignment.Bottom) + fadeIn(animationSpec = tween(180)),
                    exit = shrinkVertically(animationSpec = tween(250), shrinkTowards = Alignment.Bottom) + fadeOut(animationSpec = tween(180))
                ) {
                    TransVibeBottomNavigation(
                        currentRoute = currentRoute,
                        isArabic = isArabic,
                        hasProcessingBadge = activeProcessing && currentRoute != Routes.HOME,
                        onNavigate = openSection
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDest,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { defaultEnterTransition() },
                exitTransition = { defaultExitTransition() },
                popEnterTransition = { defaultPopEnterTransition() },
                popExitTransition = { defaultPopExitTransition() }
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
                        onSetup = { navController.navigate(Routes.SETUP) },
                        onModels = { openSection(Routes.MODELS) },
                        onHistory = { openSection(Routes.HISTORY) },
                        onSettings = { openSection(Routes.SETTINGS) },
                        onBenchmark = { navController.navigate(Routes.BENCHMARK) },
                        onOpenProcessing = { navController.navigate(Routes.PROCESSING) { launchSingleTop = true } },
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
                ) { entry ->
                    val modelId = entry.arguments?.getString("modelId") ?: ""
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
                        onOpenModels = { openSection(Routes.MODELS) },
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
                ) { entry ->
                    val jobId = entry.arguments?.getString("jobId") ?: ""
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
                ) { entry ->
                    val previewJobId = entry.arguments?.getString("jobId") ?: ""
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
                        onOpenModels = { openSection(Routes.MODELS) },
                        onOpenTranslationPacks = { navController.navigate(Routes.TRANSLATION_PACKS) },
                        onAbout = { navController.navigate(Routes.ABOUT) { launchSingleTop = true } }
                    )
                }
                composable(Routes.ABOUT) {
                    AboutScreen(
                        isArabic = isArabic,
                        onBack = { navController.popBackStack() }
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

@Composable
private fun TransVibeBottomNavigation(
    currentRoute: String?,
    isArabic: Boolean,
    hasProcessingBadge: Boolean,
    onNavigate: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(26.dp),
        color = scheme.surfaceContainer,
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppNavItem(
                selected = currentRoute == Routes.HOME,
                onClick = { onNavigate(Routes.HOME) },
                selectedIcon = Icons.Filled.FilledHome,
                unselectedIcon = Icons.Outlined.OutlinedHome,
                label = if (isArabic) "الرئيسية" else "Home",
                hasBadge = hasProcessingBadge
            )
            AppNavItem(
                selected = currentRoute == Routes.MODELS,
                onClick = { onNavigate(Routes.MODELS) },
                selectedIcon = Icons.Filled.FilledTune,
                unselectedIcon = Icons.Outlined.OutlinedTune,
                label = if (isArabic) "النماذج" else "Models"
            )
            AppNavItem(
                selected = currentRoute == Routes.HISTORY,
                onClick = { onNavigate(Routes.HISTORY) },
                selectedIcon = Icons.Filled.FilledHistory,
                unselectedIcon = Icons.Outlined.OutlinedHistory,
                label = if (isArabic) "السجل" else "History"
            )
            AppNavItem(
                selected = currentRoute == Routes.SETTINGS || currentRoute == Routes.ABOUT,
                onClick = { onNavigate(Routes.SETTINGS) },
                selectedIcon = Icons.Filled.FilledSettings,
                unselectedIcon = Icons.Outlined.OutlinedSettings,
                label = if (isArabic) "الإعدادات" else "Settings"
            )
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

private fun defaultEnterTransition(): EnterTransition =
    fadeIn(tween(220)) + slideInHorizontally(tween(220), initialOffsetX = { fullWidth -> fullWidth / 14 })

private fun defaultExitTransition(): ExitTransition =
    fadeOut(tween(180)) + slideOutHorizontally(tween(220), targetOffsetX = { fullWidth -> -fullWidth / 14 })

private fun defaultPopEnterTransition(): EnterTransition =
    fadeIn(tween(220)) + slideInHorizontally(tween(220), initialOffsetX = { fullWidth -> -fullWidth / 14 })

private fun defaultPopExitTransition(): ExitTransition =
    fadeOut(tween(180)) + slideOutHorizontally(tween(220), targetOffsetX = { fullWidth -> fullWidth / 14 })

package com.flowtap.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.flowtap.app.ui.theme.LocalFocusPalette
import com.flowtap.app.MainTab
import com.flowtap.app.FlowtapApp
import com.flowtap.app.di.AppContainer
import com.flowtap.app.ui.history.HistoryScreen
import com.flowtap.app.ui.home.HomeScreen
import com.flowtap.app.ui.modes.ModeEditorScreen
import com.flowtap.app.ui.modes.ModesScreen
import com.flowtap.app.ui.onboarding.OnboardingScreen
import com.flowtap.app.ui.settings.SettingsScreen
import com.flowtap.app.ui.tags.TagsScreen
import kotlinx.coroutines.launch

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val TAGS = "tags"
    const val MODES = "modes"
    const val MODE_EDITOR = "modeEditor"
    const val SETTINGS = "settings"

    /** Null [modeId] opens the editor for creating a new mode. */
    fun modeEditor(modeId: Long?): String =
        if (modeId == null) MODE_EDITOR else "$MODE_EDITOR?modeId=$modeId"
}

val MainTab.route: String
    get() = when (this) {
        MainTab.HOME -> Routes.HOME
        MainTab.HISTORY -> Routes.HISTORY
        MainTab.TAGS -> Routes.TAGS
        MainTab.MODES -> Routes.MODES
        MainTab.SETTINGS -> Routes.SETTINGS
    }

@Composable
fun MainApp() {
    val context = LocalContext.current
    val app = context.applicationContext as FlowtapApp
    val container = app.container
    val scope = rememberCoroutineScope()

    val onboardingDone by container.settingsDataStore.onboardingDone
        .collectAsStateWithLifecycle(initialValue = false)

    if (!onboardingDone) {
        OnboardingScreen(
            container = container,
            onDone = { scope.launch { container.settingsDataStore.setOnboardingDone() } },
        )
        return
    }

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    // The mode editor is a full-screen detail of Modes: keep Modes highlighted
    // while editing, and hide the bottom bar there for a focused settings page.
    val onModeEditor = currentRoute?.startsWith(Routes.MODE_EDITOR) == true
    val selected: MainTab =
        if (onModeEditor) {
            MainTab.MODES
        } else {
            MainTab.entries.firstOrNull { it.route == currentRoute } ?: MainTab.HOME
        }
    val palette = LocalFocusPalette.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (!onModeEditor) {
            NavigationBar(containerColor = palette.card) {
                MainTab.entries.forEach { tab ->
                    val isSelected = selected == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.iconSelected else tab.icon,
                                contentDescription = stringResource(tab.labelRes),
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        label = {
                            Text(
                                stringResource(tab.labelRes),
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = palette.accentText,
                            selectedTextColor = palette.textPrimary,
                            unselectedIconColor = palette.textTertiary,
                            unselectedTextColor = palette.textSecondary,
                            indicatorColor = palette.accent.copy(alpha = 0.16f),
                        ),
                    )
                }
            }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    container = container,
                    onOpenHistory = { navController.navigate(Routes.HISTORY) },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    container = container,
                    onOpenHome = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Routes.TAGS) { TagsScreen(container) }
            composable(Routes.MODES) {
                ModesScreen(
                    container = container,
                    onCreateMode = { navController.navigate(Routes.modeEditor(null)) },
                    onEditMode = { modeId -> navController.navigate(Routes.modeEditor(modeId)) },
                )
            }
            composable(
                route = "${Routes.MODE_EDITOR}?modeId={modeId}",
                arguments = listOf(
                    navArgument("modeId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) { entry ->
                val arg = entry.arguments?.getLong("modeId") ?: -1L
                ModeEditorScreen(
                    container = container,
                    modeId = arg.takeIf { it >= 0 },
                    onDone = { navController.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) { SettingsScreen(container) }
        }
    }
}

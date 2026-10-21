package com.masterski.ui

import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import com.masterski.R
import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.masterski.MasterSkiApp
import com.masterski.data.Lesson
import com.masterski.ui.backup.BackupsScreen
import com.masterski.ui.backup.RestoreScreen
import com.masterski.ui.edit.EditLessonScreen
import com.masterski.ui.lessons.LessonsScreen
import com.masterski.ui.overview.MonthDetailScreen
import com.masterski.ui.overview.OverviewScreen
import com.masterski.ui.settings.SettingsScreen
import java.time.YearMonth
import kotlinx.coroutines.launch

const val ROUTE_LESSONS = "lessons"
const val ROUTE_OVERVIEW = "overview"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_EDIT = "edit?id={id}"
const val ROUTE_MONTH = "month/{year}/{month}"
const val ROUTE_BACKUPS = "backups"
const val ROUTE_RESTORE = "restore/{fileName}"

fun editRoute(id: Long? = null): String = "edit?id=${id ?: -1L}"

fun monthRoute(m: YearMonth): String = "month/${m.year}/${m.monthValue}"

fun restoreRoute(fileName: String): String = "restore/${Uri.encode(fileName)}"

private data class Tab(val route: String, @StringRes val labelRes: Int, val icon: ImageVector)

private val tabs = listOf(
    Tab(ROUTE_LESSONS, R.string.lessons, Icons.AutoMirrored.Filled.List),
    Tab(ROUTE_OVERVIEW, R.string.overview, Icons.Filled.BarChart),
    Tab(ROUTE_SETTINGS, R.string.settings, Icons.Filled.Settings),
)

/** Switches to a top-level tab, keeping each tab's state and never popping the start destination. */
private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        popUpTo(ROUTE_LESSONS) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun MasterSkiNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBar = currentRoute in tabs.map { it.route }

    // Hoisted here so the Undo snackbar survives leaving the Lessons screen.
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val context = LocalContext.current
    val lessonRepository = (context.applicationContext as MasterSkiApp).container.lessonRepository
    val showMessage: (String) -> Unit = { message ->
        snackbarScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }
    val showUndo: (Lesson) -> Unit = { lesson ->
        snackbarScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                context.getString(R.string.lesson_deleted),
                actionLabel = context.getString(R.string.undo),
                duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) lessonRepository.restore(lesson)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = {
            SnackbarHost(snackbarHostState, if (showBar) Modifier else Modifier.navigationBarsPadding())
        },
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        // Every navigating callback is wrapped in dropUnlessResumed so double taps and taps during a
        // transition are ignored; back/done use navigateUp, which never pops the start destination.
        NavHost(navController, startDestination = ROUTE_LESSONS, modifier = Modifier.padding(padding)) {
            composable(ROUTE_LESSONS) { entry ->
                LessonsScreen(
                    onAdd = dropUnlessResumed { navController.navigate(editRoute()) },
                    onEdit = { id -> dropUnlessResumedNow(entry) { navController.navigate(editRoute(id)) } },
                    onDeleted = showUndo,
                )
            }
            composable(ROUTE_OVERVIEW) { entry ->
                OverviewScreen(
                    onOpenMonth = { month -> dropUnlessResumedNow(entry) { navController.navigate(monthRoute(month)) } },
                    onOpenSettings = dropUnlessResumed { navController.navigateToTab(ROUTE_SETTINGS) },
                )
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(onOpenBackups = dropUnlessResumed { navController.navigate(ROUTE_BACKUPS) })
            }
            composable(ROUTE_BACKUPS) { entry ->
                BackupsScreen(
                    onBack = dropUnlessResumed { navController.navigateUp() },
                    onRestore = { fileName -> dropUnlessResumedNow(entry) { navController.navigate(restoreRoute(fileName)) } },
                    showMessage = showMessage,
                )
            }
            composable(ROUTE_RESTORE, arguments = listOf(navArgument("fileName") { type = NavType.StringType })) { entry ->
                val fileName = entry.arguments?.getString("fileName") ?: return@composable
                RestoreScreen(
                    fileName = fileName,
                    onBack = dropUnlessResumed { navController.navigateUp() },
                    onRestored = { count ->
                        navController.popBackStack(ROUTE_BACKUPS, inclusive = false)
                        showMessage(context.resources.getQuantityString(R.plurals.restored_lessons, count, count))
                    },
                )
            }
            composable(
                ROUTE_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                EditLessonScreen(lessonId = id.takeIf { it != -1L }, onDone = dropUnlessResumed { navController.navigateUp() })
            }
            composable(
                ROUTE_MONTH,
                arguments = listOf(
                    navArgument("year") { type = NavType.IntType },
                    navArgument("month") { type = NavType.IntType },
                ),
            ) { entry ->
                val year = entry.arguments?.getInt("year") ?: return@composable
                val month = entry.arguments?.getInt("month") ?: return@composable
                MonthDetailScreen(
                    month = YearMonth.of(year, month),
                    onEdit = { id -> dropUnlessResumedNow(entry) { navController.navigate(editRoute(id)) } },
                    onBack = dropUnlessResumed { navController.navigateUp() },
                )
            }
        }
    }
}

/** Runs [block] only if [entry] is RESUMED; for callbacks that take an argument (dropUnlessResumed takes none). */
private inline fun dropUnlessResumedNow(entry: NavBackStackEntry, block: () -> Unit) {
    if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) block()
}

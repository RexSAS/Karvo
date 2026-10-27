package com.karvo.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.karvo.BuildConfig
import com.karvo.KarvoApp
import com.karvo.R
import com.karvo.data.Lesson
import com.karvo.ui.backup.BackupsScreen
import com.karvo.ui.backup.RestoreScreen
import com.karvo.ui.edit.EditLessonScreen
import com.karvo.ui.lessons.LessonsScreen
import com.karvo.ui.moments.CameraScreen
import com.karvo.ui.moments.DayRecapScreen
import com.karvo.ui.moments.LocalNavAnimation
import com.karvo.ui.moments.LocalSharedTransition
import com.karvo.ui.moments.MomentViewerScreen
import com.karvo.ui.moments.MomentsScreen
import com.karvo.ui.overview.MonthDetailScreen
import com.karvo.ui.overview.OverviewScreen
import com.karvo.ui.settings.SettingsScreen
import com.karvo.ui.theme.AlpineBackdrop
import com.karvo.ui.theme.AlpineNight
import com.karvo.ui.theme.BackdropFigure
import com.karvo.ui.theme.BackdropFigureLayer
import com.karvo.ui.theme.isAlpineNight
import com.karvo.ui.update.UpdateDialogs
import com.karvo.ui.update.UpdateViewModel
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

/** Lessons, Overview and Settings live side by side in one swipeable pager. */
const val ROUTE_HOME = "home"
const val ROUTE_EDIT = "edit?id={id}"
const val ROUTE_MONTH = "month/{year}/{month}"
const val ROUTE_BACKUPS = "backups"
const val ROUTE_RESTORE = "restore/{fileName}"
const val ROUTE_CAMERA = "camera"
const val ROUTE_MOMENTS = "moments"
const val ROUTE_MOMENT = "moment/{date}/{index}"
const val ROUTE_DAY = "day/{date}"
fun dayRoute(date: LocalDate): String = "day/$date"
fun momentRoute(date: LocalDate, index: Int): String = "moment/$date/$index"

fun editRoute(id: Long? = null): String = "edit?id=${id ?: -1L}"

fun monthRoute(m: YearMonth): String = "month/${m.year}/${m.monthValue}"

fun restoreRoute(fileName: String): String = "restore/${Uri.encode(fileName)}"

private data class Tab(@StringRes val labelRes: Int, val icon: ImageVector)

/** Pager pages, in order. */
private val tabs = listOf(
    Tab(R.string.lessons, Icons.AutoMirrored.Filled.List),
    Tab(R.string.overview, Icons.Filled.BarChart),
    Tab(R.string.settings, Icons.Filled.Settings),
)

private const val PAGE_LESSONS = 0
private const val PAGE_OVERVIEW = 1
private const val PAGE_SETTINGS = 2

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun KarvoNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBar = currentRoute == ROUTE_HOME
    // Hoisted so the selected page survives opening a lesson, a month or Backups and coming back.
    val pagerState = rememberPagerState { tabs.size }
    val pagerScope = rememberCoroutineScope()
    val goToPage: (Int) -> Unit = { page -> pagerScope.launch { pagerState.animateScrollToPage(page) } }

    // Outside the NavHost, so this ViewModel is scoped to the activity and the update flow survives navigation.
    val updateViewModel: UpdateViewModel? =
        if (BuildConfig.UPDATER) viewModel<UpdateViewModel>(factory = UpdateViewModel.Factory).also { UpdateDialogs(it) } else null

    // Hoisted here so the Undo snackbar survives leaving the Lessons screen.
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val context = LocalContext.current
    val lessonRepository = (context.applicationContext as KarvoApp).container.lessonRepository
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
                NavigationBar(containerColor = if (isAlpineNight()) AlpineNight.NavBar else NavigationBarDefaults.containerColor) {
                    tabs.forEachIndexed { page, tab ->
                        NavigationBarItem(
                            selected = pagerState.targetPage == page,
                            onClick = { goToPage(page) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isAlpineNight()) 0.16f else 0.24f),
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        // Every navigating callback is wrapped in dropUnlessResumed so double taps and taps during a
        // transition are ignored; back/done use navigateUp, which never pops the start destination.
        // Shared elements let a moment's photo and Share button fly between the day page and the viewer.
        SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransition provides this) {
        NavHost(navController, startDestination = ROUTE_HOME, modifier = Modifier.padding(padding)) {
            composable(ROUTE_HOME) { entry ->
                // Back on Overview or Settings returns to Lessons; on Lessons it leaves the app as before.
                BackHandler(enabled = pagerState.currentPage != PAGE_LESSONS) { goToPage(PAGE_LESSONS) }
                // The sky stays still while the pages slide over it.
                AlpineBackdrop(BackdropFigure.None) {
                    HorizontalPager(pagerState, beyondViewportPageCount = 1) { page ->
                        val isCurrent = pagerState.settledPage == page
                        when (page) {
                            PAGE_LESSONS -> LessonsScreen(
                                onAdd = dropUnlessResumed { navController.navigate(editRoute()) },
                                onEdit = { id -> dropUnlessResumedNow(entry) { navController.navigate(editRoute(id)) } },
                                onDeleted = showUndo,
                                onOpenCamera = dropUnlessResumed { navController.navigate(ROUTE_CAMERA) },
                                onOpenMoments = dropUnlessResumed { navController.navigate(ROUTE_MOMENTS) },
                            )
                            PAGE_OVERVIEW -> OverviewScreen(
                                onOpenMonth = { month -> dropUnlessResumedNow(entry) { navController.navigate(monthRoute(month)) } },
                                onOpenSettings = { goToPage(PAGE_SETTINGS) },
                                isCurrentPage = isCurrent,
                            )
                            else -> Box {
                                BackdropFigureLayer(BackdropFigure.Snowflakes)
                                SettingsScreen(
                                    onOpenBackups = dropUnlessResumed { navController.navigate(ROUTE_BACKUPS) },
                                    onCheckForUpdates = updateViewModel?.let { it::checkNow },
                                    isCurrentPage = isCurrent,
                                )
                            }
                        }
                    }
                }
            }
            composable(ROUTE_CAMERA) {
                CameraScreen(
                    // Not dropUnlessResumed: the camera also closes by itself right after a capture.
                    onClose = { navController.navigateUp() },
                )
            }
            composable(ROUTE_MOMENTS) { entry ->
                AlpineBackdrop(BackdropFigure.None) {
                    MomentsScreen(
                        onBack = dropUnlessResumed { navController.navigateUp() },
                        onOpenCamera = dropUnlessResumed { navController.navigate(ROUTE_CAMERA) },
                        onOpenDay = { date -> dropUnlessResumedNow(entry) { navController.navigate(dayRoute(date)) } },
                    )
                }
            }
            composable(ROUTE_DAY, arguments = listOf(navArgument("date") { type = NavType.StringType })) { entry ->
                val date = entry.arguments?.getString("date")?.let(LocalDate::parse) ?: return@composable
                CompositionLocalProvider(LocalNavAnimation provides this) {
                AlpineBackdrop(BackdropFigure.None) {
                    DayRecapScreen(
                        date,
                        onBack = dropUnlessResumed { navController.navigateUp() },
                        onOpenMoment = { day, index -> dropUnlessResumedNow(entry) { navController.navigate(momentRoute(day, index)) } },
                        onEditLesson = { id -> dropUnlessResumedNow(entry) { navController.navigate(editRoute(id)) } },
                        onOpenCamera = dropUnlessResumed { navController.navigate(ROUTE_CAMERA) },
                    )
                }
                }
            }
            composable(
                ROUTE_MOMENT,
                arguments = listOf(
                    navArgument("date") { type = NavType.StringType },
                    navArgument("index") { type = NavType.IntType },
                ),
            ) { entry ->
                val date = entry.arguments?.getString("date")?.let(LocalDate::parse) ?: return@composable
                CompositionLocalProvider(LocalNavAnimation provides this) {
                    MomentViewerScreen(date, entry.arguments?.getInt("index") ?: 0, onBack = dropUnlessResumed { navController.navigateUp() })
                }
            }
            composable(ROUTE_BACKUPS) { entry ->
                AlpineBackdrop(BackdropFigure.None) {
                    BackupsScreen(
                        onBack = dropUnlessResumed { navController.navigateUp() },
                        onRestore = { fileName -> dropUnlessResumedNow(entry) { navController.navigate(restoreRoute(fileName)) } },
                        showMessage = showMessage,
                    )
                }
            }
            composable(ROUTE_RESTORE, arguments = listOf(navArgument("fileName") { type = NavType.StringType })) { entry ->
                val fileName = entry.arguments?.getString("fileName") ?: return@composable
                AlpineBackdrop(BackdropFigure.None) {
                    RestoreScreen(
                        fileName = fileName,
                        onBack = dropUnlessResumed { navController.navigateUp() },
                        onRestored = { count ->
                            navController.popBackStack(ROUTE_BACKUPS, inclusive = false)
                            showMessage(context.resources.getQuantityString(R.plurals.restored_lessons, count, count))
                        },
                    )
                }
            }
            composable(
                ROUTE_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                AlpineBackdrop(BackdropFigure.None) {
                    EditLessonScreen(lessonId = id.takeIf { it != -1L }, onDone = dropUnlessResumed { navController.navigateUp() })
                }
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
                AlpineBackdrop(BackdropFigure.None) {
                    MonthDetailScreen(
                        month = YearMonth.of(year, month),
                        onEdit = { id -> dropUnlessResumedNow(entry) { navController.navigate(editRoute(id)) } },
                        onBack = dropUnlessResumed { navController.navigateUp() },
                    )
                }
            }
        }
        }
        }
    }
}

/** Runs [block] only if [entry] is RESUMED; for callbacks that take an argument (dropUnlessResumed takes none). */
private inline fun dropUnlessResumedNow(entry: NavBackStackEntry, block: () -> Unit) {
    if (entry.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) block()
}

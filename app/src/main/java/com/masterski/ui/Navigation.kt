package com.masterski.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.masterski.ui.edit.EditLessonScreen
import com.masterski.ui.lessons.LessonsScreen
import java.time.YearMonth

const val ROUTE_LESSONS = "lessons"
const val ROUTE_OVERVIEW = "overview"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_EDIT = "edit?id={id}"
const val ROUTE_MONTH = "month/{year}/{month}"

fun editRoute(id: Long? = null): String = "edit?id=${id ?: -1L}"

fun monthRoute(m: YearMonth): String = "month/${m.year}/${m.monthValue}"

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(ROUTE_LESSONS, "Lessons", Icons.AutoMirrored.Filled.List),
    Tab(ROUTE_OVERVIEW, "Overview", Icons.Filled.BarChart),
    Tab(ROUTE_SETTINGS, "Settings", Icons.Filled.Settings),
)

@Composable
fun MasterSkiNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBar = currentRoute in tabs.map { it.route }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(ROUTE_LESSONS) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = ROUTE_LESSONS, modifier = Modifier.padding(padding)) {
            composable(ROUTE_LESSONS) {
                LessonsScreen(
                    onAdd = { navController.navigate(editRoute()) },
                    onEdit = { navController.navigate(editRoute(it)) },
                )
            }
            composable(ROUTE_OVERVIEW) { Text("Overview") }
            composable(ROUTE_SETTINGS) { Text("Settings") }
            composable(
                ROUTE_EDIT,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                EditLessonScreen(lessonId = id.takeIf { it != -1L }, onDone = { navController.popBackStack() })
            }
            composable(
                ROUTE_MONTH,
                arguments = listOf(
                    navArgument("year") { type = NavType.IntType },
                    navArgument("month") { type = NavType.IntType },
                ),
            ) { Text("Month") }
        }
    }
}

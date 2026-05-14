package com.yourapp.timemanagement.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yourapp.timemanagement.core.TaskDraft
import com.yourapp.timemanagement.core.TimeManagementUiState
import com.yourapp.timemanagement.core.TimeManagementViewModel
import com.yourapp.timemanagement.domain.TaskPriority
import com.yourapp.timemanagement.ui.screens.AnalyticsScreen
import com.yourapp.timemanagement.ui.screens.DashboardScreen
import com.yourapp.timemanagement.ui.screens.FocusScreen
import com.yourapp.timemanagement.ui.screens.OnboardingScreen
import com.yourapp.timemanagement.ui.screens.SettingsScreen
import com.yourapp.timemanagement.ui.screens.SplashScreen
import com.yourapp.timemanagement.ui.screens.TaskEditorScreen
import com.yourapp.timemanagement.ui.screens.TaskListScreen
import com.yourapp.timemanagement.ui.screens.WidgetConfigurationScreen

object Routes {
    const val Splash = "splash"
    const val Onboarding = "onboarding"
    const val Dashboard = "dashboard"
    const val Tasks = "tasks"
    const val Focus = "focus"
    const val Analytics = "analytics"
    const val Settings = "settings"
    const val TaskEditor = "task_editor"
    const val WidgetConfig = "widget_config"
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val bottomDestinations = listOf(
    BottomDestination(Routes.Dashboard, "Today", Icons.Rounded.Dashboard),
    BottomDestination(Routes.Tasks, "Tasks", Icons.Rounded.TaskAlt),
    BottomDestination(Routes.Focus, "Focus", Icons.Rounded.Timer),
    BottomDestination(Routes.Analytics, "Trends", Icons.Rounded.Analytics),
    BottomDestination(Routes.Settings, "Settings", Icons.Rounded.Settings),
)

@Composable
fun TimeManagementApp(
    uiState: TimeManagementUiState,
    viewModel: TimeManagementViewModel,
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = bottomDestinations.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(navController = navController, startDestination = Routes.Splash) {
            composable(Routes.Splash) {
                SplashScreen(uiState = uiState)
                LaunchedEffect(uiState.isLoading, uiState.settings.onboardingComplete) {
                    if (!uiState.isLoading) {
                        navController.navigate(
                            if (uiState.settings.onboardingComplete) Routes.Dashboard else Routes.Onboarding,
                        ) {
                            popUpTo(Routes.Splash) { inclusive = true }
                        }
                    }
                }
            }
            composable(Routes.Onboarding) {
                OnboardingScreen(
                    settings = uiState.settings,
                    onFinish = {
                        viewModel.completeOnboarding()
                        navController.navigate(Routes.Dashboard) {
                            popUpTo(Routes.Onboarding) { inclusive = true }
                        }
                    },
                    onAccentSelected = viewModel::setAccentColor,
                )
            }
            composable(Routes.Dashboard) {
                DashboardScreen(
                    contentPadding = innerPadding,
                    uiState = uiState,
                    onQuickAdd = viewModel::quickAdd,
                    onStartFocus = { viewModel.startFocus(uiState.currentTask) },
                    onOpenTasks = { navController.navigate(Routes.Tasks) },
                    onOpenEditor = { navController.navigate(Routes.TaskEditor) },
                    onStatusChange = viewModel::updateTaskStatus,
                    onRefreshCalendar = viewModel::refreshCalendarEvents,
                    onDismissAchievement = viewModel::dismissAchievementDialog,
                )
            }
            composable(Routes.Tasks) {
                TaskListScreen(
                    contentPadding = innerPadding,
                    uiState = uiState,
                    onAddTask = { navController.navigate(Routes.TaskEditor) },
                    onEditTask = { taskId -> navController.navigate("${Routes.TaskEditor}?taskId=$taskId") },
                    onMoveTask = viewModel::moveTask,
                    onDeleteTask = viewModel::deleteTask,
                    onStatusChange = viewModel::updateTaskStatus,
                    onToggleTagFilter = viewModel::toggleTagFilter,
                    onClearTagFilters = viewModel::clearTagFilters,
                )
            }
            composable(Routes.TaskEditor) {
                TaskEditorScreen(
                    contentPadding = innerPadding,
                    uiState = uiState,
                    initialDraft = TaskDraft(
                        categoryId = uiState.categories.firstOrNull()?.id ?: 0,
                        priority = TaskPriority.Medium,
                    ),
                    onSave = {
                        viewModel.saveTask(it)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() },
                    onAddSubTask = viewModel::addSubTask,
                    onSubTaskChecked = viewModel::setSubTaskCompleted,
                    onDeleteSubTask = viewModel::deleteSubTask,
                    onAddTag = viewModel::addTag,
                    onTaskTagSelected = viewModel::setTaskTag,
                )
            }
            composable("${Routes.TaskEditor}?taskId={taskId}") { entry ->
                val taskId = entry.arguments?.getString("taskId")?.toLongOrNull()
                val task = uiState.allTasks.firstOrNull { it.id == taskId }
                TaskEditorScreen(
                    contentPadding = innerPadding,
                    uiState = uiState,
                    initialDraft = task?.let {
                        TaskDraft(
                            id = it.id,
                            title = it.title,
                            notes = it.notes,
                            date = it.date,
                            startTime = it.startTime,
                            estimateMinutes = it.estimateMinutes,
                            priority = it.priority,
                            categoryId = it.categoryId,
                            tag = it.tag,
                            recurrence = it.recurrence,
                            reminderMinutesBefore = it.reminderMinutesBefore,
                            status = it.status,
                        )
                    } ?: TaskDraft(
                        categoryId = uiState.categories.firstOrNull()?.id ?: 0,
                        priority = TaskPriority.Medium,
                    ),
                    onSave = {
                        viewModel.saveTask(it)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() },
                    onAddSubTask = viewModel::addSubTask,
                    onSubTaskChecked = viewModel::setSubTaskCompleted,
                    onDeleteSubTask = viewModel::deleteSubTask,
                    onAddTag = viewModel::addTag,
                    onTaskTagSelected = viewModel::setTaskTag,
                )
            }
            composable(Routes.Focus) {
                FocusScreen(
                    contentPadding = innerPadding,
                    uiState = uiState,
                    onStartFocus = { viewModel.startFocus(uiState.currentTask) },
                    onPauseFocus = viewModel::pauseFocus,
                    onCompleteFocus = viewModel::completeFocus,
                    onInterruption = viewModel::recordInterruption,
                    onBreak = viewModel::logBreak,
                    onSavePreset = viewModel::saveFocusPreset,
                    onDeletePreset = viewModel::deleteFocusPreset,
                    onBlockCalendar = viewModel::blockCurrentTaskOnCalendar,
                )
            }
            composable(Routes.Analytics) {
                AnalyticsScreen(contentPadding = innerPadding, uiState = uiState)
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    contentPadding = innerPadding,
                    uiState = uiState,
                    onThemeMode = viewModel::setThemeMode,
                    onAccentColor = viewModel::setAccentColor,
                    onCardStyle = viewModel::setCardStyle,
                    onDensity = viewModel::setDensity,
                    onScoringStyle = viewModel::setScoringStyle,
                    onToggleModule = viewModel::toggleDashboardModule,
                    onAddCategory = viewModel::addCategory,
                    onNotificationTone = viewModel::setNotificationTone,
                    onWidgetConfig = { navController.navigate(Routes.WidgetConfig) },
                )
            }
            composable(Routes.WidgetConfig) {
                WidgetConfigurationScreen(
                    contentPadding = innerPadding,
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}

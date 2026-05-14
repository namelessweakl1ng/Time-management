package com.yourapp.timemanagement.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourapp.timemanagement.core.TimeManagementUiState
import com.yourapp.timemanagement.domain.InsightSeverity
import com.yourapp.timemanagement.domain.Task
import com.yourapp.timemanagement.domain.TaskStatus
import com.yourapp.timemanagement.ui.common.PremiumCard
import com.yourapp.timemanagement.ui.common.ProgressRing
import com.yourapp.timemanagement.ui.common.SectionHeader
import com.yourapp.timemanagement.ui.common.StatPill
import com.yourapp.timemanagement.ui.common.StatusChip
import com.yourapp.timemanagement.ui.common.formatMinutes
import com.yourapp.timemanagement.ui.common.priorityColor
import com.yourapp.timemanagement.ui.common.timeLabel
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun DashboardScreen(
    contentPadding: PaddingValues,
    uiState: TimeManagementUiState,
    onQuickAdd: () -> Unit,
    onStartFocus: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenEditor: () -> Unit,
    onStatusChange: (Long, TaskStatus) -> Unit,
    onRefreshCalendar: () -> Unit,
    onDismissAchievement: () -> Unit,
) {
    LaunchedEffect(uiState.hasCalendarPermission) {
        if (uiState.hasCalendarPermission) onRefreshCalendar()
    }
    uiState.recentlyUnlockedAchievement?.let { achievement ->
        AlertDialog(
            onDismissRequest = onDismissAchievement,
            confirmButton = { Button(onClick = onDismissAchievement) { Text("Nice") } },
            title = { Text("Achievement unlocked") },
            text = { Text("${achievement.title}: ${achievement.description}\n+${achievement.xpReward} XP") },
        )
    }
    LazyColumn(
        modifier = Modifier.padding(contentPadding),
        contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Today", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    Text("A calm plan with enough room to finish.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FloatingActionButton(onClick = onQuickAdd) {
                    Icon(Icons.Rounded.Add, contentDescription = "Quick add task")
                }
            }
        }
        item {
            DailyGoalCard(uiState = uiState)
        }
        item {
            GamificationCard(uiState)
        }
        item {
            FocusNowCard(uiState = uiState, onStartFocus = onStartFocus, onOpenEditor = onOpenEditor)
        }
        item {
            SummaryRow(uiState = uiState)
        }
        item {
            CalendarOverviewCard(uiState)
        }
        item {
            InsightCard(uiState = uiState)
        }
        item {
            SmartPredictionCard(uiState)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Timeline", "Tap into the day without hunting around.", Modifier.weight(1f))
                FilledTonalButton(onClick = onOpenTasks) { Text("Manage") }
            }
        }
        items(uiState.todayTasks, key = { it.id }) { task ->
            TimelineTaskRow(
                task = task,
                uiState = uiState,
                onComplete = { onStatusChange(task.id, TaskStatus.Completed) },
                onSkip = { onStatusChange(task.id, TaskStatus.Skipped) },
            )
        }
        if (uiState.todayTasks.isEmpty()) {
            item {
                PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("No tasks planned", style = MaterialTheme.typography.titleLarge)
                        Text("Add a focused block and the dashboard will light up with progress, scoring, and suggestions.")
                        Button(onClick = onOpenEditor) { Text("Add first task") }
                    }
                }
            }
        }
    }
}

@Composable
private fun GamificationCard(uiState: TimeManagementUiState) {
    PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                ProgressRing(
                    progress = uiState.gamification.levelProgress,
                    label = "level",
                    value = "${uiState.gamification.level}",
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Level ${uiState.gamification.level}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${uiState.gamification.xp} XP  |  next at ${uiState.gamification.nextLevelXp} XP",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Streak: ${uiState.gamification.streakDays} day${if (uiState.gamification.streakDays == 1) "" else "s"}",
                        color = Color(0xFFE07A5F),
                    )
                }
            }
            val unlocked = uiState.gamification.unlockedAchievements.takeLast(3)
            if (unlocked.isNotEmpty()) {
                Text(
                    "Badges: ${unlocked.joinToString("  |  ") { it.title }}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SmartPredictionCard(uiState: TimeManagementUiState) {
    PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Smart timing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (uiState.flowState.active) {
                Text(
                    "Flow State badge active: ${uiState.flowState.consecutiveSessions} sessions, ${formatMinutes(uiState.flowState.totalMinutes)} focused.",
                    color = Color(0xFF1F8A70),
                )
            }
            val prediction = uiState.predictions.firstOrNull()
            if (prediction == null) {
                Text("Complete a few focus sessions and local timing predictions will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val task = uiState.todayTasks.firstOrNull { it.id == prediction.taskId }
                Text(
                    "${task?.title ?: "Next task"} looks strongest around ${prediction.suggestedHour}:00 (${(prediction.confidence * 100).toInt()}% confidence).",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CalendarOverviewCard(uiState: TimeManagementUiState) {
    PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Calendar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (!uiState.hasCalendarPermission) {
                Text("Grant calendar permission to see upcoming events beside your focus blocks.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            if (uiState.calendarEvents.isEmpty()) {
                Text("No calendar events left today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                uiState.calendarEvents.take(3).forEach { event ->
                    val start = Instant.ofEpochMilli(event.startMillis).atZone(ZoneId.systemDefault()).toLocalTime()
                    Text("${timeLabel(start.hour, start.minute)}  ${event.title}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DailyGoalCard(uiState: TimeManagementUiState) {
    PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            ProgressRing(
                progress = uiState.completedPercent,
                label = "complete",
                value = "${(uiState.completedPercent * 100).toInt()}%",
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Daily goal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${uiState.stats.completedTasks}/${uiState.stats.totalTasks} tasks finished with ${formatMinutes(uiState.stats.actualProductiveMinutes)} focused.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text("Score ${uiState.stats.score}") })
                    AssistChip(onClick = {}, label = { Text("${formatMinutes(uiState.stats.plannedMinutes)} planned") })
                }
            }
        }
    }
}

@Composable
private fun FocusNowCard(
    uiState: TimeManagementUiState,
    onStartFocus: () -> Unit,
    onOpenEditor: () -> Unit,
) {
    val task = uiState.currentTask
    var remaining by remember(task?.id, task?.endTime) { mutableIntStateOf(0) }
    LaunchedEffect(task?.id, task?.endTime) {
        while (true) {
            remaining = task?.let { Duration.between(LocalTime.now(), it.endTime).toMinutes().toInt().coerceAtLeast(0) } ?: 0
            delay(1_000)
        }
    }
    PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Focus mode", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    AnimatedContent(task?.title ?: "No active task", label = "current-task") { title ->
                        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text(formatMinutes(remaining), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = if (task == null) onOpenEditor else onStartFocus, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (task == null) "Plan a focus block" else "Start focus")
            }
        }
    }
}

@Composable
private fun SummaryRow(uiState: TimeManagementUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatPill("Productive", formatMinutes(uiState.stats.actualProductiveMinutes), Modifier.weight(1f), MaterialTheme.colorScheme.primary)
        StatPill("Breaks", formatMinutes(uiState.stats.breakMinutes), Modifier.weight(1f), Color(0xFFE07A5F))
        StatPill("Streak", formatMinutes(uiState.stats.longestFocusStreakMinutes), Modifier.weight(1f), Color(0xFF536DFE))
    }
}

@Composable
private fun InsightCard(uiState: TimeManagementUiState) {
    val insight = uiState.insights.firstOrNull() ?: return
    val color = when (insight.severity) {
        InsightSeverity.Info -> MaterialTheme.colorScheme.primary
        InsightSeverity.Warning -> Color(0xFFC2410C)
        InsightSeverity.Success -> Color(0xFF1F8A70)
    }
    PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(insight.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
            Text(insight.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TimelineTaskRow(
    task: Task,
    uiState: TimeManagementUiState,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
) {
    PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(timeLabel(task.startTime.hour, task.startTime.minute), style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))
                Text(formatMinutes(task.estimateMinutes), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusChip(task.status)
                    Text(task.priority.name, color = priorityColor(task.priority), style = MaterialTheme.typography.labelMedium)
                }
            }
            IconButton(onClick = onComplete) {
                Icon(Icons.Rounded.Check, contentDescription = "Complete task", tint = Color(0xFF1F8A70))
            }
            IconButton(onClick = onSkip) {
                Icon(Icons.Rounded.SkipNext, contentDescription = "Skip task")
            }
        }
    }
}

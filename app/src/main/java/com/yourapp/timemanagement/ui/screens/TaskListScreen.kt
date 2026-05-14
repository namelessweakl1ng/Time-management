package com.yourapp.timemanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourapp.timemanagement.core.TimeManagementUiState
import com.yourapp.timemanagement.domain.Task
import com.yourapp.timemanagement.domain.TaskStatus
import com.yourapp.timemanagement.ui.common.PremiumCard
import com.yourapp.timemanagement.ui.common.SectionHeader
import com.yourapp.timemanagement.ui.common.StatusChip
import com.yourapp.timemanagement.ui.common.formatMinutes
import com.yourapp.timemanagement.ui.common.priorityColor
import com.yourapp.timemanagement.ui.common.timeLabel

private enum class TaskFilter { All, Active, Completed, Issues }

@Composable
fun TaskListScreen(
    contentPadding: PaddingValues,
    uiState: TimeManagementUiState,
    onAddTask: () -> Unit,
    onEditTask: (Long) -> Unit,
    onMoveTask: (Long, Int) -> Unit,
    onDeleteTask: (Long) -> Unit,
    onStatusChange: (Long, TaskStatus) -> Unit,
    onToggleTagFilter: (Long) -> Unit,
    onClearTagFilters: () -> Unit,
) {
    var filter by remember { mutableStateOf(TaskFilter.All) }
    val tasks = remember(uiState.todayTasks, filter) {
        uiState.todayTasks.filter {
            when (filter) {
                TaskFilter.All -> true
                TaskFilter.Active -> it.status == TaskStatus.Planned || it.status == TaskStatus.InProgress
                TaskFilter.Completed -> it.status == TaskStatus.Completed
                TaskFilter.Issues -> it.status == TaskStatus.Skipped || it.status == TaskStatus.Overdue
            }
        }
    }

    LazyColumn(
        modifier = Modifier.padding(contentPadding),
        contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Task timeline", "Reorder, finish, skip, or refine your plan.", Modifier.weight(1f))
                Button(onClick = onAddTask) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("Add")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                TaskFilter.values().forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.name) },
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = uiState.selectedTagIds.isEmpty(),
                    onClick = onClearTagFilters,
                    label = { Text("All labels") },
                )
                uiState.tags.take(5).forEach { tag ->
                    FilterChip(
                        selected = tag.id in uiState.selectedTagIds,
                        onClick = { onToggleTagFilter(tag.id) },
                        label = { Text("#${tag.name}") },
                    )
                }
            }
        }
        if (tasks.isEmpty()) {
            item {
                PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Nothing in this view", style = MaterialTheme.typography.titleLarge)
                        Text("Change the filter or add a block for today.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledTonalButton(onClick = onAddTask) { Text("Add task") }
                    }
                }
            }
        }
        items(tasks, key = { it.id }) { task ->
            TaskRow(
                task = task,
                uiState = uiState,
                onEdit = { onEditTask(task.id) },
                onMoveUp = { onMoveTask(task.id, -1) },
                onMoveDown = { onMoveTask(task.id, 1) },
                onDelete = { onDeleteTask(task.id) },
                onComplete = { onStatusChange(task.id, TaskStatus.Completed) },
                onSkip = { onStatusChange(task.id, TaskStatus.Skipped) },
            )
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    uiState: TimeManagementUiState,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
) {
    var dragOffset by remember(task.id) { mutableStateOf(0f) }
    PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .pointerInput(task.id) {
                    detectDragGesturesAfterLongPress(
                        onDragEnd = {
                            when {
                                dragOffset < -36f -> onMoveUp()
                                dragOffset > 36f -> onMoveDown()
                            }
                            dragOffset = 0f
                        },
                        onDragCancel = { dragOffset = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount.y
                        },
                    )
                }
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${timeLabel(task.startTime.hour, task.startTime.minute)} - ${formatMinutes(task.estimateMinutes)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(task.status)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(task.priority.name, color = priorityColor(task.priority), style = MaterialTheme.typography.labelLarge)
                if (task.tag.isNotBlank()) Text("#${task.tag}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                uiState.taskTagIds[task.id].orEmpty().mapNotNull { tagId -> uiState.tags.firstOrNull { it.id == tagId } }.take(2).forEach { tag ->
                    Text("#${tag.name}", color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row {
                    IconButton(onClick = onMoveUp) { Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move task earlier") }
                    IconButton(onClick = onMoveDown) { Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move task later") }
                    IconButton(onClick = onEdit) { Icon(Icons.Rounded.Edit, contentDescription = "Edit task") }
                    IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete task", tint = Color(0xFFC2410C)) }
                }
                Row {
                    IconButton(onClick = onComplete) { Icon(Icons.Rounded.Add, contentDescription = "Complete task", tint = Color(0xFF1F8A70)) }
                    IconButton(onClick = onSkip) { Icon(Icons.Rounded.SkipNext, contentDescription = "Skip task") }
                }
            }
        }
    }
}

package com.yourapp.timemanagement.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.yourapp.timemanagement.core.TimeManagementUiState
import com.yourapp.timemanagement.domain.Task
import com.yourapp.timemanagement.domain.TaskPriority
import com.yourapp.timemanagement.domain.TaskStatus
import com.yourapp.timemanagement.domain.RecurrenceRule
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class TaskListScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun taskList_displaysTodayTasksAndFilters() {
        composeRule.setContent {
            TaskListScreen(
                contentPadding = PaddingValues(0.dp),
                uiState = TimeManagementUiState(todayTasks = listOf(task())),
                onAddTask = {},
                onEditTask = {},
                onMoveTask = { _, _ -> },
                onDeleteTask = {},
                onStatusChange = { _, _ -> },
                onToggleTagFilter = {},
                onClearTagFilters = {},
            )
        }

        composeRule.onNodeWithText("Task timeline").assertIsDisplayed()
        composeRule.onNodeWithText("Write production tests").assertIsDisplayed()
        composeRule.onNodeWithText("#quality").assertIsDisplayed()
    }

    private fun task() = Task(
        id = 1,
        title = "Write production tests",
        notes = "",
        date = LocalDate.now(),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        estimateMinutes = 60,
        priority = TaskPriority.High,
        categoryId = 1,
        tag = "quality",
        recurrence = RecurrenceRule.None,
        reminderMinutesBefore = null,
        status = TaskStatus.Planned,
        sortOrder = 0,
        createdAtMillis = 0,
    )
}

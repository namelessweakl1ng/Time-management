package com.yourapp.timemanagement.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yourapp.timemanagement.appContainer
import com.yourapp.timemanagement.domain.RecurrenceRule
import com.yourapp.timemanagement.domain.TaskPriority
import com.yourapp.timemanagement.domain.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class TimeWidgetActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = context.appContainer()
                when (intent.action) {
                    ACTION_WIDGET_QUICK_ADD -> quickAdd(container)
                    ACTION_WIDGET_START_FOCUS -> startFocus(container)
                    ACTION_WIDGET_PAUSE_FOCUS -> container.timeRepository.finishActiveFocus(completeTask = false, note = "Paused from widget")
                    ACTION_WIDGET_REFRESH -> Unit
                }
                container.widgetUpdater.updateAll()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun quickAdd(container: com.yourapp.timemanagement.core.AppContainer) {
        val categoryId = container.timeRepository.categories.first().firstOrNull()?.id ?: 0L
        container.timeRepository.createTask(
            title = "Quick capture",
            notes = "Added from home screen widget.",
            date = LocalDate.now(),
            startTime = LocalTime.now().plusMinutes(15).withSecond(0).withNano(0),
            estimateMinutes = 25,
            priority = TaskPriority.Medium,
            categoryId = categoryId,
            tag = "widget",
            recurrence = RecurrenceRule.None,
            reminderMinutesBefore = null,
            sortOrder = 999,
        )
    }

    private suspend fun startFocus(container: com.yourapp.timemanagement.core.AppContainer) {
        val task = container.timeRepository.tasksForDate(LocalDate.now()).first()
            .firstOrNull { it.status == TaskStatus.InProgress || it.status == TaskStatus.Planned }
        container.timeRepository.startFocus(task)
    }
}

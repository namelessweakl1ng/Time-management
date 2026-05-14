package com.yourapp.timemanagement.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yourapp.timemanagement.data.repository.TimeRepository
import com.yourapp.timemanagement.domain.RecurrenceRule
import com.yourapp.timemanagement.domain.TaskPriority
import com.yourapp.timemanagement.domain.TaskStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class TimeWidgetActionReceiver : BroadcastReceiver() {
    @Inject lateinit var timeRepository: TimeRepository
    @Inject lateinit var widgetUpdater: TimeWidgetUpdater

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_WIDGET_QUICK_ADD -> quickAdd()
                    ACTION_WIDGET_START_FOCUS -> startFocus()
                    ACTION_WIDGET_PAUSE_FOCUS -> timeRepository.finishActiveFocus(completeTask = false, note = "Paused from widget")
                    ACTION_WIDGET_REFRESH -> Unit
                }
                widgetUpdater.updateAll()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun quickAdd() {
        val categoryId = timeRepository.categories.first().firstOrNull()?.id ?: 0L
        timeRepository.createTask(
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

    private suspend fun startFocus() {
        val task = timeRepository.tasksForDate(LocalDate.now()).first()
            .firstOrNull { it.status == TaskStatus.InProgress || it.status == TaskStatus.Planned }
        timeRepository.startFocus(task)
    }
}

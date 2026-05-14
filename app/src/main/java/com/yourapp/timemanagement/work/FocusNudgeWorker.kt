package com.yourapp.timemanagement.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourapp.timemanagement.data.repository.TimeRepository
import com.yourapp.timemanagement.domain.TaskStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

@HiltWorker
class FocusNudgeWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val timeRepository: TimeRepository,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val now = LocalTime.now()
        val todayTasks = timeRepository.tasksForDate(LocalDate.now()).first()
        val active = timeRepository.activeSession.first()
        val missedTask = todayTasks.firstOrNull { task ->
            task.status == TaskStatus.Planned &&
                now.isAfter(task.startTime.plusMinutes(10)) &&
                now.isBefore(task.endTime.plusMinutes(30))
        }
        if (active == null && missedTask != null) {
            NotificationHelper.showReminder(
                applicationContext,
                "Gentle focus nudge",
                "${missedTask.title} was planned for ${missedTask.startTime}. Start a short block or reschedule it.",
                NotificationHelper.NUDGE_NOTIFICATION_ID,
            )
        }
        return Result.success()
    }

    companion object {
        const val TAG = "focus_nudge_check"
    }
}

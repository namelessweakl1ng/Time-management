package com.yourapp.timemanagement.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class TaskReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, 0L)
        val title = inputData.getString(KEY_TITLE).orEmpty().ifBlank { "Upcoming task" }
        NotificationHelper.showReminder(
            applicationContext,
            title,
            "Your next planned block is about to start.",
            taskId.toInt().coerceAtLeast(1),
        )
        return Result.success()
    }

    companion object {
        const val KEY_TASK_ID = "task_id"
        const val KEY_TITLE = "title"
    }
}

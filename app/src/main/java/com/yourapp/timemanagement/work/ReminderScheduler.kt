package com.yourapp.timemanagement.work

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    fun schedule(taskId: Long, title: String, triggerAtMillis: Long) {
        val delay = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<TaskReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putLong(TaskReminderWorker.KEY_TASK_ID, taskId)
                    .putString(TaskReminderWorker.KEY_TITLE, title)
                    .build(),
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "task_reminder_$taskId",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}

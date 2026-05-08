package com.yourapp.timemanagement.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourapp.timemanagement.appContainer

class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        applicationContext.appContainer().widgetUpdater.updateAll()
        return Result.success()
    }

    companion object {
        const val TAG = "time_widget_refresh"
    }
}

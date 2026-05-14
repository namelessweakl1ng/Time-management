package com.yourapp.timemanagement.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yourapp.timemanagement.widgets.TimeWidgetUpdater
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class WidgetRefreshWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val widgetUpdater: TimeWidgetUpdater,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            widgetUpdater.updateAll()
            Result.success()
        } catch (throwable: Throwable) {
            Result.retry()
        }
    }

    companion object {
        const val TAG = "time_widget_refresh"
    }
}

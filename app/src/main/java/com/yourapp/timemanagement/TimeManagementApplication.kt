package com.yourapp.timemanagement

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.yourapp.timemanagement.di.WidgetRefreshScheduler
import com.yourapp.timemanagement.work.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TimeManagementApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var widgetRefreshScheduler: WidgetRefreshScheduler

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannels(this)
        widgetRefreshScheduler.schedule()
        widgetRefreshScheduler.scheduleFocusNudges()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}

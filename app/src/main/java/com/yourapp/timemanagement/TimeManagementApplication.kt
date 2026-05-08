package com.yourapp.timemanagement

import android.app.Application
import com.yourapp.timemanagement.core.AppContainer
import com.yourapp.timemanagement.work.NotificationHelper

class TimeManagementApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.ensureChannels(this)
        container.scheduleWidgetRefresh()
    }
}

fun android.content.Context.appContainer(): AppContainer =
    (applicationContext as TimeManagementApplication).container

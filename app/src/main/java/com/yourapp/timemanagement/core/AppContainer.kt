package com.yourapp.timemanagement.core

import android.content.Context
import androidx.room.Room
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yourapp.timemanagement.data.local.AppDatabase
import com.yourapp.timemanagement.data.repository.SettingsRepository
import com.yourapp.timemanagement.data.repository.TimeRepository
import com.yourapp.timemanagement.data.repository.settingsDataStore
import com.yourapp.timemanagement.domain.ProductivityCalculator
import com.yourapp.timemanagement.domain.SmartSuggestionEngine
import com.yourapp.timemanagement.widgets.TimeWidgetUpdater
import com.yourapp.timemanagement.work.ReminderScheduler
import com.yourapp.timemanagement.work.WidgetRefreshWorker
import java.util.concurrent.TimeUnit

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "time_management.db",
    ).build()

    val settingsRepository = SettingsRepository(appContext.settingsDataStore)
    val timeRepository = TimeRepository(
        taskDao = database.taskDao(),
        categoryDao = database.categoryDao(),
        sessionDao = database.focusSessionDao(),
        interruptionDao = database.interruptionDao(),
        widgetPreferenceDao = database.widgetPreferenceDao(),
    )
    val productivityCalculator = ProductivityCalculator()
    val smartSuggestionEngine = SmartSuggestionEngine()
    val widgetUpdater = TimeWidgetUpdater(appContext, timeRepository, settingsRepository, productivityCalculator, smartSuggestionEngine)
    val reminderScheduler = ReminderScheduler(appContext)

    fun scheduleWidgetRefresh() {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES)
            .addTag(WidgetRefreshWorker.TAG)
            .build()
        WorkManager.getInstance(appContext).enqueueUniquePeriodicWork(
            WidgetRefreshWorker.TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}

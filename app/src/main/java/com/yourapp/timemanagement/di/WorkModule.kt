package com.yourapp.timemanagement.di

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yourapp.timemanagement.work.WidgetRefreshWorker
import com.yourapp.timemanagement.work.FocusNudgeWorker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkModule {
    @Provides
    @Singleton
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
        WorkManager.getInstance(context)

    @Provides
    fun provideWidgetRefreshScheduler(workManager: WorkManager): WidgetRefreshScheduler =
        WidgetRefreshScheduler(workManager)
}

class WidgetRefreshScheduler(private val workManager: WorkManager) {
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES)
            .addTag(WidgetRefreshWorker.TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            WidgetRefreshWorker.TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun scheduleFocusNudges() {
        val request = PeriodicWorkRequestBuilder<FocusNudgeWorker>(2, TimeUnit.HOURS)
            .addTag(FocusNudgeWorker.TAG)
            .build()
        workManager.enqueueUniquePeriodicWork(
            FocusNudgeWorker.TAG,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}

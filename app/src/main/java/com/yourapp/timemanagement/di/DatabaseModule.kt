package com.yourapp.timemanagement.di

import android.content.Context
import androidx.room.Room
import com.yourapp.timemanagement.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "time_management.db",
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

    @Provides
    fun provideTaskDao(database: AppDatabase) = database.taskDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase) = database.categoryDao()

    @Provides
    fun provideFocusSessionDao(database: AppDatabase) = database.focusSessionDao()

    @Provides
    fun provideInterruptionDao(database: AppDatabase) = database.interruptionDao()

    @Provides
    fun provideDailyStatsDao(database: AppDatabase) = database.dailyStatsDao()

    @Provides
    fun provideWidgetPreferenceDao(database: AppDatabase) = database.widgetPreferenceDao()

    @Provides
    fun provideSubTaskDao(database: AppDatabase) = database.subTaskDao()

    @Provides
    fun provideTagDao(database: AppDatabase) = database.tagDao()
}

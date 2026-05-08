package com.yourapp.timemanagement.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        TaskEntity::class,
        CategoryEntity::class,
        FocusSessionEntity::class,
        InterruptionEntity::class,
        DailyStatsEntity::class,
        WidgetPreferenceEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun interruptionDao(): InterruptionDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun widgetPreferenceDao(): WidgetPreferenceDao
}

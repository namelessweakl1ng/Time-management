package com.yourapp.timemanagement.data.local

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TaskEntity::class,
        CategoryEntity::class,
        FocusSessionEntity::class,
        InterruptionEntity::class,
        DailyStatsEntity::class,
        WidgetPreferenceEntity::class,
        SubTaskEntity::class,
        TagEntity::class,
        TaskTagCrossRefEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun categoryDao(): CategoryDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun interruptionDao(): InterruptionDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun widgetPreferenceDao(): WidgetPreferenceDao
    abstract fun subTaskDao(): SubTaskDao
    abstract fun tagDao(): TagDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sub_tasks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        parentTaskId INTEGER NOT NULL,
                        title TEXT NOT NULL,
                        isCompleted INTEGER NOT NULL,
                        FOREIGN KEY(parentTaskId) REFERENCES tasks(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_sub_tasks_parentTaskId ON sub_tasks(parentTaskId)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tags (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        color INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS task_tag_cross_refs (
                        taskId INTEGER NOT NULL,
                        tagId INTEGER NOT NULL,
                        PRIMARY KEY(taskId, tagId),
                        FOREIGN KEY(taskId) REFERENCES tasks(id) ON DELETE CASCADE,
                        FOREIGN KEY(tagId) REFERENCES tags(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_tag_cross_refs_taskId ON task_tag_cross_refs(taskId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_task_tag_cross_refs_tagId ON task_tag_cross_refs(tagId)")
            }
        }
    }
}

package com.yourapp.timemanagement.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long,
    val iconName: String,
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String,
    val dateEpochDay: Long,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val estimateMinutes: Int,
    val priority: String,
    val categoryId: Long,
    val tag: String,
    val recurrence: String,
    val reminderMinutesBefore: Int?,
    val status: String,
    val sortOrder: Int,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "sub_tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTaskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("parentTaskId")],
)
data class SubTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentTaskId: Long,
    val title: String,
    val isCompleted: Boolean,
)

@Entity(tableName = "tags", indices = [Index(value = ["name"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long,
)

@Entity(
    tableName = "task_tag_cross_refs",
    primaryKeys = ["taskId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId"), Index("tagId")],
)
data class TaskTagCrossRefEntity(
    val taskId: Long,
    val tagId: Long,
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long?,
    val categoryId: Long?,
    val type: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val productiveMinutes: Int,
    val distractedMinutes: Int,
    val breakMinutes: Int,
    val interruptionCount: Int,
    val note: String,
)

@Entity(tableName = "interruptions")
data class InterruptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val startedAtMillis: Long,
    val durationMinutes: Int,
    val reason: String,
)

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey val dateEpochDay: Long,
    val plannedMinutes: Int,
    val productiveMinutes: Int,
    val distractedMinutes: Int,
    val breakMinutes: Int,
    val score: Int,
)

@Entity(tableName = "widget_preferences")
data class WidgetPreferenceEntity(
    @PrimaryKey val widgetId: Int,
    val widgetType: String,
    val accentColor: Long,
    val compact: Boolean,
)

package com.yourapp.timemanagement.data.local

import com.yourapp.timemanagement.domain.Category
import com.yourapp.timemanagement.domain.FocusSession
import com.yourapp.timemanagement.domain.RecurrenceRule
import com.yourapp.timemanagement.domain.SessionType
import com.yourapp.timemanagement.domain.SubTask
import com.yourapp.timemanagement.domain.Tag
import com.yourapp.timemanagement.domain.Task
import com.yourapp.timemanagement.domain.TaskPriority
import com.yourapp.timemanagement.domain.TaskStatus
import com.yourapp.timemanagement.domain.WidgetPreferences
import java.time.LocalDate
import java.time.LocalTime

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    notes = notes,
    date = LocalDate.ofEpochDay(dateEpochDay),
    startTime = LocalTime.of(startMinuteOfDay / 60, startMinuteOfDay % 60),
    endTime = LocalTime.of((endMinuteOfDay.coerceAtMost(23 * 60 + 59)) / 60, endMinuteOfDay % 60),
    estimateMinutes = estimateMinutes,
    priority = runCatching { TaskPriority.valueOf(priority) }.getOrDefault(TaskPriority.Medium),
    categoryId = categoryId,
    tag = tag,
    recurrence = runCatching { RecurrenceRule.valueOf(recurrence) }.getOrDefault(RecurrenceRule.None),
    reminderMinutesBefore = reminderMinutesBefore,
    status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.Planned),
    sortOrder = sortOrder,
    createdAtMillis = createdAtMillis,
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    notes = notes,
    dateEpochDay = date.toEpochDay(),
    startMinuteOfDay = startTime.hour * 60 + startTime.minute,
    endMinuteOfDay = endTime.hour * 60 + endTime.minute,
    estimateMinutes = estimateMinutes,
    priority = priority.name,
    categoryId = categoryId,
    tag = tag,
    recurrence = recurrence.name,
    reminderMinutesBefore = reminderMinutesBefore,
    status = status.name,
    sortOrder = sortOrder,
    createdAtMillis = createdAtMillis,
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    color = color,
    iconName = iconName,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    color = color,
    iconName = iconName,
)

fun SubTaskEntity.toDomain(): SubTask = SubTask(
    id = id,
    parentTaskId = parentTaskId,
    title = title,
    isCompleted = isCompleted,
)

fun SubTask.toEntity(): SubTaskEntity = SubTaskEntity(
    id = id,
    parentTaskId = parentTaskId,
    title = title,
    isCompleted = isCompleted,
)

fun TagEntity.toDomain(): Tag = Tag(
    id = id,
    name = name,
    color = color,
)

fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name.trim().lowercase(),
    color = color,
)

fun FocusSessionEntity.toDomain(): FocusSession = FocusSession(
    id = id,
    taskId = taskId,
    categoryId = categoryId,
    type = runCatching { SessionType.valueOf(type) }.getOrDefault(SessionType.Focus),
    startedAtMillis = startedAtMillis,
    endedAtMillis = endedAtMillis,
    productiveMinutes = productiveMinutes,
    distractedMinutes = distractedMinutes,
    breakMinutes = breakMinutes,
    interruptionCount = interruptionCount,
    note = note,
)

fun FocusSession.toEntity(): FocusSessionEntity = FocusSessionEntity(
    id = id,
    taskId = taskId,
    categoryId = categoryId,
    type = type.name,
    startedAtMillis = startedAtMillis,
    endedAtMillis = endedAtMillis,
    productiveMinutes = productiveMinutes,
    distractedMinutes = distractedMinutes,
    breakMinutes = breakMinutes,
    interruptionCount = interruptionCount,
    note = note,
)

fun WidgetPreferenceEntity.toDomain(): WidgetPreferences = WidgetPreferences(
    widgetId = widgetId,
    widgetType = widgetType,
    accentColor = accentColor,
    compact = compact,
)

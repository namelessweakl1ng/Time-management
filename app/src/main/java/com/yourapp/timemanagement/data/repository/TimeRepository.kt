package com.yourapp.timemanagement.data.repository

import com.yourapp.timemanagement.data.local.CategoryDao
import com.yourapp.timemanagement.data.local.CategoryEntity
import com.yourapp.timemanagement.data.local.FocusSessionDao
import com.yourapp.timemanagement.data.local.FocusSessionEntity
import com.yourapp.timemanagement.data.local.InterruptionDao
import com.yourapp.timemanagement.data.local.InterruptionEntity
import com.yourapp.timemanagement.data.local.TaskDao
import com.yourapp.timemanagement.data.local.TaskEntity
import com.yourapp.timemanagement.data.local.WidgetPreferenceDao
import com.yourapp.timemanagement.data.local.WidgetPreferenceEntity
import com.yourapp.timemanagement.data.local.toDomain
import com.yourapp.timemanagement.data.local.toEntity
import com.yourapp.timemanagement.domain.Category
import com.yourapp.timemanagement.domain.FocusSession
import com.yourapp.timemanagement.domain.RecurrenceRule
import com.yourapp.timemanagement.domain.SessionType
import com.yourapp.timemanagement.domain.Task
import com.yourapp.timemanagement.domain.TaskPriority
import com.yourapp.timemanagement.domain.TaskStatus
import com.yourapp.timemanagement.domain.WidgetPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TimeRepository(
    private val taskDao: TaskDao,
    private val categoryDao: CategoryDao,
    private val sessionDao: FocusSessionDao,
    private val interruptionDao: InterruptionDao,
    private val widgetPreferenceDao: WidgetPreferenceDao,
) {
    val allTasks: Flow<List<Task>> = taskDao.observeAll().map { tasks -> tasks.map { it.toDomain() } }
    val categories: Flow<List<Category>> = categoryDao.observeAll().map { rows -> rows.map { it.toDomain() } }
    val sessions: Flow<List<FocusSession>> = sessionDao.observeAll().map { rows -> rows.map { it.toDomain() } }
    val activeSession: Flow<FocusSession?> = sessionDao.observeActive().map { it?.toDomain() }
    val widgetPreferences: Flow<List<WidgetPreferences>> =
        widgetPreferenceDao.observeAll().map { rows -> rows.map { it.toDomain() } }

    fun tasksForDate(date: LocalDate): Flow<List<Task>> =
        taskDao.observeByDate(date.toEpochDay()).map { rows -> rows.map { it.toDomain() } }

    fun task(id: Long): Flow<Task?> = taskDao.observeById(id).map { it?.toDomain() }

    fun sessionsForDate(date: LocalDate): Flow<List<FocusSession>> =
        sessionDao.observeBetween(date.startMillis(), date.plusDays(1).startMillis())
            .map { rows -> rows.map { it.toDomain() } }

    suspend fun saveTask(task: Task): Long = taskDao.upsert(task.toEntity())

    suspend fun createTask(
        title: String,
        notes: String,
        date: LocalDate,
        startTime: LocalTime,
        estimateMinutes: Int,
        priority: TaskPriority,
        categoryId: Long,
        tag: String,
        recurrence: RecurrenceRule,
        reminderMinutesBefore: Int?,
        sortOrder: Int,
    ): Long {
        val endTime = startTime.plusMinutes(estimateMinutes.toLong().coerceAtLeast(15))
        return taskDao.upsert(
            TaskEntity(
                title = title.ifBlank { "Untitled task" },
                notes = notes,
                dateEpochDay = date.toEpochDay(),
                startMinuteOfDay = startTime.hour * 60 + startTime.minute,
                endMinuteOfDay = endTime.hour * 60 + endTime.minute,
                estimateMinutes = estimateMinutes.coerceAtLeast(5),
                priority = priority.name,
                categoryId = categoryId,
                tag = tag,
                recurrence = recurrence.name,
                reminderMinutesBefore = reminderMinutesBefore,
                status = TaskStatus.Planned.name,
                sortOrder = sortOrder,
                createdAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun updateStatus(taskId: Long, status: TaskStatus) {
        taskDao.updateStatus(taskId, status.name)
    }

    suspend fun moveTask(taskId: Long, direction: Int) {
        val task = taskDao.getById(taskId) ?: return
        val dayTasks = taskDao.getByDate(task.dateEpochDay)
        val index = dayTasks.indexOfFirst { it.id == taskId }
        val targetIndex = (index + direction).coerceIn(dayTasks.indices)
        if (index == -1 || index == targetIndex) return
        val reordered = dayTasks.toMutableList().apply {
            add(targetIndex, removeAt(index))
        }
        reordered.forEachIndexed { order, row -> taskDao.updateSortOrder(row.id, order) }
    }

    suspend fun deleteTask(taskId: Long) {
        taskDao.deleteById(taskId)
    }

    suspend fun saveCategory(category: Category): Long = categoryDao.upsert(category.toEntity())

    suspend fun startFocus(task: Task?): Long {
        val active = sessionDao.getActive()
        if (active != null) return active.id
        task?.let { taskDao.updateStatus(it.id, TaskStatus.InProgress.name) }
        return sessionDao.upsert(
            FocusSessionEntity(
                taskId = task?.id,
                categoryId = task?.categoryId,
                type = SessionType.Focus.name,
                startedAtMillis = System.currentTimeMillis(),
                endedAtMillis = null,
                productiveMinutes = 0,
                distractedMinutes = 0,
                breakMinutes = 0,
                interruptionCount = 0,
                note = "",
            ),
        )
    }

    suspend fun finishActiveFocus(completeTask: Boolean = false, note: String = "") {
        val active = sessionDao.getActive() ?: return
        val now = System.currentTimeMillis()
        val duration = ((now - active.startedAtMillis) / 60_000L).coerceAtLeast(1).toInt()
        sessionDao.finish(
            id = active.id,
            endedAtMillis = now,
            productiveMinutes = duration,
            distractedMinutes = active.distractedMinutes,
            breakMinutes = active.breakMinutes,
            interruptionCount = active.interruptionCount,
            note = note,
        )
        if (completeTask && active.taskId != null) {
            taskDao.updateStatus(active.taskId, TaskStatus.Completed.name)
        } else if (active.taskId != null) {
            taskDao.updateStatus(active.taskId, TaskStatus.Planned.name)
        }
    }

    suspend fun logBreak(minutes: Int) {
        sessionDao.upsert(
            FocusSessionEntity(
                taskId = null,
                categoryId = null,
                type = SessionType.Break.name,
                startedAtMillis = System.currentTimeMillis() - minutes.coerceAtLeast(1) * 60_000L,
                endedAtMillis = System.currentTimeMillis(),
                productiveMinutes = 0,
                distractedMinutes = 0,
                breakMinutes = minutes.coerceAtLeast(1),
                interruptionCount = 0,
                note = "Manual break",
            ),
        )
    }

    suspend fun recordInterruption(reason: String, minutes: Int) {
        val active = sessionDao.getActive()
        val now = System.currentTimeMillis()
        if (active == null) {
            sessionDao.upsert(
                FocusSessionEntity(
                    taskId = null,
                    categoryId = null,
                    type = SessionType.Distracted.name,
                    startedAtMillis = now - minutes.coerceAtLeast(1) * 60_000L,
                    endedAtMillis = now,
                    productiveMinutes = 0,
                    distractedMinutes = minutes.coerceAtLeast(1),
                    breakMinutes = 0,
                    interruptionCount = 1,
                    note = reason,
                ),
            )
            return
        }
        interruptionDao.insert(
            InterruptionEntity(
                sessionId = active.id,
                startedAtMillis = now - minutes.coerceAtLeast(1) * 60_000L,
                durationMinutes = minutes.coerceAtLeast(1),
                reason = reason.ifBlank { "Interruption" },
            ),
        )
        sessionDao.update(
            active.copy(
                distractedMinutes = active.distractedMinutes + minutes.coerceAtLeast(1),
                interruptionCount = active.interruptionCount + 1,
            ),
        )
    }

    suspend fun upsertWidgetPreference(preference: WidgetPreferenceEntity) {
        widgetPreferenceDao.upsert(preference)
    }

    suspend fun getWidgetPreference(widgetId: Int): WidgetPreferences? =
        widgetPreferenceDao.get(widgetId)?.toDomain()

    suspend fun seedSampleDataIfEmpty() {
        if (taskDao.count() > 0) return
        val categories = listOf(
            CategoryEntity(name = "Deep Work", color = 0xFF1F8A70, iconName = "bolt"),
            CategoryEntity(name = "Admin", color = 0xFF536DFE, iconName = "inbox"),
            CategoryEntity(name = "Learning", color = 0xFFE07A5F, iconName = "book"),
            CategoryEntity(name = "Wellness", color = 0xFF7B61FF, iconName = "heart"),
        )
        val ids = categories.map { categoryDao.upsert(it) }
        val today = LocalDate.now()
        val now = System.currentTimeMillis()
        val sampleTasks = listOf(
            sampleTask("Plan launch sprint", "Map milestones and reduce uncertainty.", today, 8, 45, 75, TaskPriority.High, ids[0], "strategy", 0),
            sampleTask("Inbox zero sprint", "Process mail and admin queue.", today, 10, 30, 35, TaskPriority.Medium, ids[1], "admin", 1),
            sampleTask("Prototype focus timer", "Polish interactions for the demo.", today, 12, 0, 90, TaskPriority.Critical, ids[0], "build", 2),
            sampleTask("Walk and reset", "Log a proper break before analytics review.", today, 15, 0, 25, TaskPriority.Low, ids[3], "break", 3),
            sampleTask("Review productivity trends", "Look at planned versus actual time.", today, 16, 0, 45, TaskPriority.Medium, ids[2], "review", 4),
            sampleTask("Weekly learning block", "Read notes and capture next actions.", today.minusDays(1), 9, 0, 60, TaskPriority.Medium, ids[2], "learn", 0, TaskStatus.Completed),
            sampleTask("Design critique", "Review dashboard hierarchy.", today.minusDays(1), 11, 0, 50, TaskPriority.High, ids[0], "design", 1, TaskStatus.Completed),
        )
        sampleTasks.forEach { taskDao.upsert(it) }
        listOf(
            FocusSessionEntity(taskId = null, categoryId = ids[0], type = SessionType.Focus.name, startedAtMillis = now - 5 * 60 * 60_000L, endedAtMillis = now - 4 * 60 * 60_000L, productiveMinutes = 58, distractedMinutes = 4, breakMinutes = 0, interruptionCount = 1, note = "Morning deep work"),
            FocusSessionEntity(taskId = null, categoryId = ids[1], type = SessionType.Focus.name, startedAtMillis = now - 3 * 60 * 60_000L, endedAtMillis = now - 2 * 60 * 60_000L, productiveMinutes = 38, distractedMinutes = 12, breakMinutes = 0, interruptionCount = 2, note = "Admin sprint"),
            FocusSessionEntity(taskId = null, categoryId = ids[3], type = SessionType.Break.name, startedAtMillis = now - 90 * 60_000L, endedAtMillis = now - 70 * 60_000L, productiveMinutes = 0, distractedMinutes = 0, breakMinutes = 20, interruptionCount = 0, note = "Reset break"),
        ).forEach { sessionDao.upsert(it) }
    }

    private fun sampleTask(
        title: String,
        notes: String,
        date: LocalDate,
        hour: Int,
        minute: Int,
        estimate: Int,
        priority: TaskPriority,
        categoryId: Long,
        tag: String,
        order: Int,
        status: TaskStatus = TaskStatus.Planned,
    ): TaskEntity {
        val start = LocalTime.of(hour, minute)
        val end = start.plusMinutes(estimate.toLong())
        return TaskEntity(
            title = title,
            notes = notes,
            dateEpochDay = date.toEpochDay(),
            startMinuteOfDay = start.hour * 60 + start.minute,
            endMinuteOfDay = end.hour * 60 + end.minute,
            estimateMinutes = estimate,
            priority = priority.name,
            categoryId = categoryId,
            tag = tag,
            recurrence = RecurrenceRule.None.name,
            reminderMinutesBefore = 10,
            status = status.name,
            sortOrder = order,
            createdAtMillis = System.currentTimeMillis(),
        )
    }
}

fun LocalDate.startMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

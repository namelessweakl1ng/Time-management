package com.yourapp.timemanagement.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourapp.timemanagement.data.repository.SettingsRepository
import com.yourapp.timemanagement.data.repository.TimeRepository
import com.yourapp.timemanagement.data.repository.CalendarRepository
import com.yourapp.timemanagement.data.repository.startMillis
import com.yourapp.timemanagement.domain.AnalyticsSnapshot
import com.yourapp.timemanagement.domain.CalendarEvent
import com.yourapp.timemanagement.domain.CardStyle
import com.yourapp.timemanagement.domain.Category
import com.yourapp.timemanagement.domain.CategoryBreakdown
import com.yourapp.timemanagement.domain.FocusSession
import com.yourapp.timemanagement.domain.FocusPreset
import com.yourapp.timemanagement.domain.FlowState
import com.yourapp.timemanagement.domain.HourProductivity
import com.yourapp.timemanagement.domain.LayoutDensity
import com.yourapp.timemanagement.domain.OnDeviceTaskTimePredictor
import com.yourapp.timemanagement.domain.Achievement
import com.yourapp.timemanagement.domain.GamificationEngine
import com.yourapp.timemanagement.domain.GamificationState
import com.yourapp.timemanagement.domain.ProductivityCalculator
import com.yourapp.timemanagement.domain.RecurrenceRule
import com.yourapp.timemanagement.domain.ScoringStyle
import com.yourapp.timemanagement.domain.SessionType
import com.yourapp.timemanagement.domain.SmartInsight
import com.yourapp.timemanagement.domain.SmartSuggestionEngine
import com.yourapp.timemanagement.domain.SubTask
import com.yourapp.timemanagement.domain.Tag
import com.yourapp.timemanagement.domain.Task
import com.yourapp.timemanagement.domain.TaskTimePrediction
import com.yourapp.timemanagement.domain.TaskPriority
import com.yourapp.timemanagement.domain.TaskStatus
import com.yourapp.timemanagement.domain.ThemeMode
import com.yourapp.timemanagement.domain.UserSettings
import com.yourapp.timemanagement.work.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class TimeManagementUiState(
    val isLoading: Boolean = true,
    val settings: UserSettings = UserSettings(),
    val todayTasks: List<Task> = emptyList(),
    val allTasks: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
    val todaySessions: List<FocusSession> = emptyList(),
    val allSessions: List<FocusSession> = emptyList(),
    val subTasks: List<SubTask> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val taskTagIds: Map<Long, Set<Long>> = emptyMap(),
    val selectedTagIds: Set<Long> = emptySet(),
    val calendarEvents: List<CalendarEvent> = emptyList(),
    val hasCalendarPermission: Boolean = false,
    val predictions: List<TaskTimePrediction> = emptyList(),
    val flowState: FlowState = FlowState(false, 0, 0, 25),
    val gamification: GamificationState = GamificationState(),
    val recentlyUnlockedAchievement: Achievement? = null,
    val activeSession: FocusSession? = null,
    val stats: com.yourapp.timemanagement.domain.ProductivityStats = com.yourapp.timemanagement.domain.ProductivityStats(),
    val insights: List<SmartInsight> = emptyList(),
    val analytics: AnalyticsSnapshot = AnalyticsSnapshot(
        stats = com.yourapp.timemanagement.domain.ProductivityStats(),
        categoryBreakdown = emptyList(),
        mostProductiveHours = emptyList(),
        weeklyScores = emptyList(),
        plannedVsActualPercent = 0,
        insights = emptyList(),
    ),
    val operationState: Result<Unit> = Result.Success(Unit),
) {
    val currentTask: Task?
        get() = todayTasks.firstOrNull { it.status == TaskStatus.InProgress }
            ?: todayTasks.firstOrNull { it.isCurrent() }
            ?: todayTasks.firstOrNull { it.status == TaskStatus.Planned }

    val completedPercent: Float
        get() = if (stats.totalTasks == 0) 0f else stats.completedTasks.toFloat() / stats.totalTasks
}

data class TaskDraft(
    val id: Long = 0,
    val title: String = "",
    val notes: String = "",
    val date: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.now().withMinute(0).plusHours(1),
    val estimateMinutes: Int = 45,
    val priority: TaskPriority = TaskPriority.Medium,
    val categoryId: Long = 0,
    val tag: String = "",
    val recurrence: RecurrenceRule = RecurrenceRule.None,
    val reminderMinutesBefore: Int? = 10,
    val status: TaskStatus = TaskStatus.Planned,
)

@HiltViewModel
class TimeManagementViewModel @Inject constructor(
    private val timeRepository: TimeRepository,
    private val settingsRepository: SettingsRepository,
    private val calendarRepository: CalendarRepository,
    private val calculator: ProductivityCalculator,
    private val suggestionEngine: SmartSuggestionEngine,
    private val taskTimePredictor: OnDeviceTaskTimePredictor,
    private val flowStateDetector: com.yourapp.timemanagement.domain.FlowStateDetector,
    private val gamificationEngine: GamificationEngine,
    private val reminderScheduler: ReminderScheduler,
    private val widgetUpdater: com.yourapp.timemanagement.widgets.TimeWidgetUpdater,
) : ViewModel() {
    private val loading = MutableStateFlow(true)
    private val today = MutableStateFlow(LocalDate.now())
    private val operationState = MutableStateFlow<Result<Unit>>(Result.Success(Unit))
    private val selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    private val calendarEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    private val recentlyUnlockedAchievement = MutableStateFlow<Achievement?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TimeManagementUiState> = today.flatMapLatest { currentDate ->
        combine(
            loading,
            settingsRepository.settings,
            timeRepository.tasksForDate(currentDate),
            timeRepository.allTasks,
            timeRepository.categories,
            timeRepository.sessionsForDate(currentDate),
            timeRepository.sessions,
            timeRepository.subTasks,
            timeRepository.tags,
            timeRepository.taskTagIds,
            timeRepository.activeSession,
            selectedTagIds,
            calendarEvents,
            recentlyUnlockedAchievement,
            operationState,
        ) { values ->
            val isLoading = values[0] as Boolean
            val settings = values[1] as UserSettings
            val todayTasks = (values[2] as List<*>).filterIsInstance<Task>().withOverdueState()
            val allTasks = (values[3] as List<*>).filterIsInstance<Task>()
            val categories = (values[4] as List<*>).filterIsInstance<Category>()
            val todaySessions = (values[5] as List<*>).filterIsInstance<FocusSession>()
            val allSessions = (values[6] as List<*>).filterIsInstance<FocusSession>()
            val subTasks = (values[7] as List<*>).filterIsInstance<SubTask>()
            val tags = (values[8] as List<*>).filterIsInstance<Tag>()
            val taskTagIds = values[9] as Map<Long, Set<Long>>
            val activeSession = values[10] as FocusSession?
            val selectedTagIds = values[11] as Set<Long>
            val calendarEvents = (values[12] as List<*>).filterIsInstance<CalendarEvent>()
            val recentlyUnlockedAchievement = values[13] as Achievement?
            val operationState = values[14] as Result<Unit>
            val filteredTodayTasks = if (selectedTagIds.isEmpty()) {
                todayTasks
            } else {
                todayTasks.filter { task -> taskTagIds[task.id].orEmpty().intersect(selectedTagIds).isNotEmpty() }
            }
            val stats = calculator.calculate(todayTasks, todaySessions, settings)
            val predictions = taskTimePredictor.predict(filteredTodayTasks, allSessions)
            val flowState = flowStateDetector.detect(todaySessions)
            val insights = suggestionEngine.insights(filteredTodayTasks, todaySessions, categories, stats, predictions, flowState)
            val gamification = gamificationEngine.calculate(allTasks, allSessions, settings.unlockedAchievementIds)
            TimeManagementUiState(
                isLoading = isLoading,
                settings = settings,
                todayTasks = filteredTodayTasks,
                allTasks = allTasks,
                categories = categories,
                todaySessions = todaySessions,
                allSessions = allSessions,
                subTasks = subTasks,
                tags = tags,
                taskTagIds = taskTagIds,
                selectedTagIds = selectedTagIds,
                calendarEvents = calendarEvents,
                hasCalendarPermission = calendarRepository.hasReadPermission(),
                predictions = predictions,
                flowState = flowState,
                gamification = gamification,
                recentlyUnlockedAchievement = recentlyUnlockedAchievement,
                activeSession = activeSession,
                stats = stats,
                insights = insights,
                analytics = buildAnalytics(settings, allTasks, allSessions, categories, insights),
                operationState = operationState,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimeManagementUiState())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runMutation {
                timeRepository.seedSampleDataIfEmpty()
                seedTagsFromLegacyTaskLabels()
                refreshGamification()
                settingsRepository.setSeededSampleData(true)
                refreshCalendarEvents()
                widgetUpdater.updateAll()
            }
            loading.value = false
        }
    }

    fun completeOnboarding() = viewModelScope.launch {
        runMutation { settingsRepository.setOnboardingComplete(true) }
    }

    fun saveTask(draft: TaskDraft) = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            val categories = uiState.value.categories
            val categoryId = draft.categoryId.takeIf { id -> id != 0L } ?: categories.firstOrNull()?.id ?: 0L
            val task = Task(
                id = draft.id,
                title = draft.title.ifBlank { "New focus block" },
                notes = draft.notes,
                date = draft.date,
                startTime = draft.startTime,
                endTime = draft.startTime.plusMinutes(draft.estimateMinutes.toLong().coerceAtLeast(5)),
                estimateMinutes = draft.estimateMinutes.coerceAtLeast(5),
                priority = draft.priority,
                categoryId = categoryId,
                tag = draft.tag,
                recurrence = draft.recurrence,
                reminderMinutesBefore = draft.reminderMinutesBefore,
                status = draft.status,
                sortOrder = if (draft.id == 0L) uiState.value.todayTasks.size + 1 else uiState.value.todayTasks.firstOrNull { it.id == draft.id }?.sortOrder ?: 0,
                createdAtMillis = System.currentTimeMillis(),
            )
            val id = timeRepository.saveTask(task)
            scheduleReminder(task.copy(id = id))
            afterMutation()
        }
    }

    fun quickAdd() = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            val category = uiState.value.categories.firstOrNull()
            val start = LocalTime.now().plusMinutes(15).withSecond(0).withNano(0)
            timeRepository.createTask(
                title = "Quick capture",
                notes = "Refine this task when you have a minute.",
                date = LocalDate.now(),
                startTime = start,
                estimateMinutes = 25,
                priority = TaskPriority.Medium,
                categoryId = category?.id ?: 0,
                tag = "quick",
                recurrence = RecurrenceRule.None,
                reminderMinutesBefore = 10,
                sortOrder = uiState.value.todayTasks.size + 1,
            )
            afterMutation()
        }
    }

    fun updateTaskStatus(taskId: Long, status: TaskStatus) = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            timeRepository.updateStatus(taskId, status)
            afterMutation()
        }
    }

    fun moveTask(taskId: Long, direction: Int) = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            timeRepository.moveTask(taskId, direction)
            afterMutation()
        }
    }

    fun deleteTask(taskId: Long) = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            timeRepository.deleteTask(taskId)
            afterMutation()
        }
    }

    fun startFocus(task: Task?) = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            timeRepository.startFocus(task)
            afterMutation()
        }
    }

    fun pauseFocus() = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            timeRepository.finishActiveFocus(completeTask = false, note = "Paused from focus mode")
            afterMutation()
        }
    }

    fun completeFocus() = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            timeRepository.finishActiveFocus(completeTask = true, note = "Completed focus session")
            afterMutation()
        }
    }

    fun recordInterruption(reason: String = "Context switch", minutes: Int = 5) = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            timeRepository.recordInterruption(reason, minutes)
            afterMutation()
        }
    }

    fun logBreak(minutes: Int = 10) = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            timeRepository.logBreak(minutes)
            afterMutation()
        }
    }

    fun setThemeMode(value: ThemeMode) = viewModelScope.launch { runMutation { settingsRepository.setThemeMode(value) } }
    fun setAccentColor(value: Long) = viewModelScope.launch { runMutation { settingsRepository.setAccentColor(value) } }
    fun setCardStyle(value: CardStyle) = viewModelScope.launch { runMutation { settingsRepository.setCardStyle(value) } }
    fun setDensity(value: LayoutDensity) = viewModelScope.launch { runMutation { settingsRepository.setDensity(value) } }
    fun setScoringStyle(value: ScoringStyle) = viewModelScope.launch { runMutation { settingsRepository.setScoringStyle(value) } }
    fun setNotificationTone(value: String) = viewModelScope.launch { runMutation { settingsRepository.setNotificationTone(value) } }

    fun toggleDashboardModule(key: String) = viewModelScope.launch {
        runMutation {
            val modules = uiState.value.settings.dashboardModules.map { module ->
                if (module.key == key) module.copy(visible = !module.visible) else module
            }
            settingsRepository.setDashboardModules(modules)
        }
    }

    fun addCategory(name: String, color: Long) = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            timeRepository.saveCategory(Category(0, name.ifBlank { "Custom" }, color, "label"))
            afterMutation()
        }
    }

    fun addSubTask(parentTaskId: Long, title: String) = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            if (parentTaskId != 0L) timeRepository.saveSubTask(parentTaskId, title)
            afterMutation()
        }
    }

    fun setSubTaskCompleted(subTaskId: Long, completed: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            timeRepository.setSubTaskCompleted(subTaskId, completed)
            afterMutation()
        }
    }

    fun deleteSubTask(subTaskId: Long) = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            timeRepository.deleteSubTask(subTaskId)
            afterMutation()
        }
    }

    fun addTag(name: String, color: Long) = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            if (name.isNotBlank()) timeRepository.saveTag(name, color)
            afterMutation()
        }
    }

    fun setTaskTag(taskId: Long, tagId: Long, selected: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            if (taskId != 0L) timeRepository.setTaskTag(taskId, tagId, selected)
            afterMutation()
        }
    }

    fun toggleTagFilter(tagId: Long) {
        selectedTagIds.value = selectedTagIds.value.toMutableSet().apply {
            if (!add(tagId)) remove(tagId)
        }
    }

    fun clearTagFilters() {
        selectedTagIds.value = emptySet()
    }

    fun saveFocusPreset(preset: FocusPreset) = viewModelScope.launch {
        runMutation {
            val existing = uiState.value.settings.focusPresets
            val id = if (preset.id == 0L) (existing.maxOfOrNull(FocusPreset::id) ?: 0L) + 1 else preset.id
            val normalized = preset.copy(
                id = id,
                name = preset.name.ifBlank { "${preset.focusMinutes}m Focus" },
                focusMinutes = preset.focusMinutes.coerceIn(5, 180),
                breakMinutes = preset.breakMinutes.coerceIn(1, 60),
            )
            settingsRepository.setFocusPresets(existing.filterNot { it.id == id } + normalized)
        }
    }

    fun deleteFocusPreset(presetId: Long) = viewModelScope.launch {
        runMutation {
            val remaining = uiState.value.settings.focusPresets.filterNot { it.id == presetId }
            settingsRepository.setFocusPresets(remaining.ifEmpty { com.yourapp.timemanagement.domain.defaultFocusPresets })
        }
    }

    fun refreshCalendarEvents() = viewModelScope.launch(Dispatchers.IO) {
        val date = LocalDate.now()
        calendarEvents.value = calendarRepository.upcomingEvents(date.startMillis(), date.plusDays(1).startMillis())
    }

    fun blockCurrentTaskOnCalendar() = viewModelScope.launch(Dispatchers.IO) {
        runMutation {
            val task = uiState.value.currentTask ?: return@runMutation
            calendarRepository.createFocusBlock(
                title = "Focus: ${task.title}",
                startMillis = task.date.startMillis() + (task.startTime.hour * 60L + task.startTime.minute) * 60_000L,
                endMillis = task.date.startMillis() + (task.endTime.hour * 60L + task.endTime.minute) * 60_000L,
            )
            refreshCalendarEvents()
        }
    }

    fun dismissAchievementDialog() {
        recentlyUnlockedAchievement.value = null
    }

    private fun scheduleReminder(task: Task) {
        val minutes = task.reminderMinutesBefore ?: return
        val trigger = task.date.startMillis() +
            (task.startTime.hour * 60L + task.startTime.minute - minutes) * 60_000L
        if (trigger > System.currentTimeMillis()) {
            reminderScheduler.schedule(task.id, task.title, trigger)
        }
    }

    private suspend fun afterMutation() {
        refreshGamification()
        widgetUpdater.updateAll()
    }

    private suspend fun runMutation(block: suspend () -> Unit) {
        operationState.value = Result.Loading
        operationState.value = try {
            block()
            Result.Success(Unit)
        } catch (throwable: Throwable) {
            Result.Error(throwable)
        }
    }

    private suspend fun seedTagsFromLegacyTaskLabels() {
        uiState.value.allTasks.forEach { task ->
            val label = task.tag.trim()
            if (label.isNotBlank()) {
                val id = timeRepository.saveTag(label, 0xFF1F8A70)
                if (id > 0) timeRepository.setTaskTag(task.id, id, selected = true)
            }
        }
    }

    private suspend fun refreshGamification() {
        val state = uiState.value
        val gamification = gamificationEngine.calculate(
            tasks = state.allTasks,
            sessions = state.allSessions,
            unlockedIds = state.settings.unlockedAchievementIds,
        )
        val newlyUnlocked = gamificationEngine.newlyUnlocked(state.settings.unlockedAchievementIds, gamification)
        if (newlyUnlocked.isNotEmpty()) {
            recentlyUnlockedAchievement.value = newlyUnlocked.first()
        }
        val ids = gamification.unlockedAchievements.map(Achievement::id).toSet()
        if (
            state.settings.xp != gamification.xp ||
            state.settings.level != gamification.level ||
            state.settings.streakDays != gamification.streakDays ||
            state.settings.unlockedAchievementIds != ids
        ) {
            settingsRepository.setGamification(
                xp = gamification.xp,
                level = gamification.level,
                streakDays = gamification.streakDays,
                unlockedAchievementIds = ids,
            )
        }
    }

    private fun buildAnalytics(
        settings: UserSettings,
        allTasks: List<Task>,
        allSessions: List<FocusSession>,
        categories: List<Category>,
        insights: List<SmartInsight>,
    ): AnalyticsSnapshot {
        val today = LocalDate.now()
        val weekStart = today.minusDays(6)
        val weekTasks = allTasks.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(today) }
        val weekSessions = allSessions.filter {
            val date = Instant.ofEpochMilli(it.startedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate()
            !date.isBefore(weekStart) && !date.isAfter(today)
        }
        val stats = calculator.calculate(weekTasks, weekSessions, settings)
        val breakdown = weekSessions
            .groupBy { it.categoryId }
            .mapNotNull { (categoryId, sessions) ->
                val category = categories.firstOrNull { it.id == categoryId } ?: return@mapNotNull null
                CategoryBreakdown(category, sessions.sumOf { it.productiveMinutes + it.distractedMinutes + it.breakMinutes })
            }
            .sortedByDescending { it.minutes }
        val hours = weekSessions
            .groupBy { Instant.ofEpochMilli(it.startedAtMillis).atZone(ZoneId.systemDefault()).hour }
            .map { (hour, sessions) -> HourProductivity(hour, sessions.sumOf { it.productiveMinutes }) }
            .sortedByDescending { it.productiveMinutes }
            .take(6)
        return AnalyticsSnapshot(
            stats = stats,
            categoryBreakdown = breakdown,
            mostProductiveHours = hours,
            weeklyScores = calculator.weeklyScores(allTasks, allSessions, settings, today),
            plannedVsActualPercent = calculator.plannedVsActualPercent(weekTasks, weekSessions),
            insights = insights,
        )
    }

    private fun List<Task>.withOverdueState(): List<Task> {
        val now = LocalTime.now()
        val today = LocalDate.now()
        return map { task ->
            if (task.date == today && task.status == TaskStatus.Planned && now.isAfter(task.endTime)) {
                task.copy(status = TaskStatus.Overdue)
            } else {
                task
            }
        }
    }
}

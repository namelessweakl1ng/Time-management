package com.yourapp.timemanagement.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yourapp.timemanagement.data.repository.TimeRepository
import com.yourapp.timemanagement.data.repository.startMillis
import com.yourapp.timemanagement.domain.AnalyticsSnapshot
import com.yourapp.timemanagement.domain.CardStyle
import com.yourapp.timemanagement.domain.Category
import com.yourapp.timemanagement.domain.CategoryBreakdown
import com.yourapp.timemanagement.domain.FocusSession
import com.yourapp.timemanagement.domain.HourProductivity
import com.yourapp.timemanagement.domain.LayoutDensity
import com.yourapp.timemanagement.domain.ProductivityCalculator
import com.yourapp.timemanagement.domain.RecurrenceRule
import com.yourapp.timemanagement.domain.ScoringStyle
import com.yourapp.timemanagement.domain.SessionType
import com.yourapp.timemanagement.domain.SmartInsight
import com.yourapp.timemanagement.domain.SmartSuggestionEngine
import com.yourapp.timemanagement.domain.Task
import com.yourapp.timemanagement.domain.TaskPriority
import com.yourapp.timemanagement.domain.TaskStatus
import com.yourapp.timemanagement.domain.ThemeMode
import com.yourapp.timemanagement.domain.UserSettings
import com.yourapp.timemanagement.work.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class TimeManagementUiState(
    val isLoading: Boolean = true,
    val settings: UserSettings = UserSettings(),
    val todayTasks: List<Task> = emptyList(),
    val allTasks: List<Task> = emptyList(),
    val categories: List<Category> = emptyList(),
    val todaySessions: List<FocusSession> = emptyList(),
    val allSessions: List<FocusSession> = emptyList(),
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

class TimeManagementViewModel(
    private val timeRepository: TimeRepository,
    private val settingsRepository: com.yourapp.timemanagement.data.repository.SettingsRepository,
    private val calculator: ProductivityCalculator,
    private val suggestionEngine: SmartSuggestionEngine,
    private val reminderScheduler: ReminderScheduler,
    private val widgetUpdater: com.yourapp.timemanagement.widgets.TimeWidgetUpdater,
) : ViewModel() {
    private val loading = MutableStateFlow(true)
    private val today = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<TimeManagementUiState> = combine(
        loading,
        settingsRepository.settings,
        timeRepository.tasksForDate(today.value),
        timeRepository.allTasks,
        timeRepository.categories,
        timeRepository.sessionsForDate(today.value),
        timeRepository.sessions,
        timeRepository.activeSession,
    ) { values ->
        val isLoading = values[0] as Boolean
        val settings = values[1] as UserSettings
        val todayTasks = (values[2] as List<*>).filterIsInstance<Task>().withOverdueState()
        val allTasks = (values[3] as List<*>).filterIsInstance<Task>()
        val categories = (values[4] as List<*>).filterIsInstance<Category>()
        val todaySessions = (values[5] as List<*>).filterIsInstance<FocusSession>()
        val allSessions = (values[6] as List<*>).filterIsInstance<FocusSession>()
        val activeSession = values[7] as FocusSession?
        val stats = calculator.calculate(todayTasks, todaySessions, settings)
        val insights = suggestionEngine.insights(todayTasks, todaySessions, categories, stats)
        TimeManagementUiState(
            isLoading = isLoading,
            settings = settings,
            todayTasks = todayTasks,
            allTasks = allTasks,
            categories = categories,
            todaySessions = todaySessions,
            allSessions = allSessions,
            activeSession = activeSession,
            stats = stats,
            insights = insights,
            analytics = buildAnalytics(settings, allTasks, allSessions, categories, insights),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimeManagementUiState())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            timeRepository.seedSampleDataIfEmpty()
            settingsRepository.setSeededSampleData(true)
            loading.value = false
            widgetUpdater.updateAll()
        }
    }

    fun completeOnboarding() = viewModelScope.launch {
        settingsRepository.setOnboardingComplete(true)
    }

    fun saveTask(draft: TaskDraft) = viewModelScope.launch(Dispatchers.IO) {
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

    fun quickAdd() = viewModelScope.launch(Dispatchers.IO) {
        val category = uiState.value.categories.firstOrNull()
        val start = LocalTime.now().plusMinutes(15).withSecond(0).withNano(0)
        val id = timeRepository.createTask(
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
        timeRepository.task(id)
        afterMutation()
    }

    fun updateTaskStatus(taskId: Long, status: TaskStatus) = viewModelScope.launch(Dispatchers.IO) {
        timeRepository.updateStatus(taskId, status)
        afterMutation()
    }

    fun moveTask(taskId: Long, direction: Int) = viewModelScope.launch(Dispatchers.IO) {
        timeRepository.moveTask(taskId, direction)
        afterMutation()
    }

    fun deleteTask(taskId: Long) = viewModelScope.launch(Dispatchers.IO) {
        timeRepository.deleteTask(taskId)
        afterMutation()
    }

    fun startFocus(task: Task?) = viewModelScope.launch(Dispatchers.IO) {
        timeRepository.startFocus(task)
        afterMutation()
    }

    fun pauseFocus() = viewModelScope.launch(Dispatchers.IO) {
        timeRepository.finishActiveFocus(completeTask = false, note = "Paused from focus mode")
        afterMutation()
    }

    fun completeFocus() = viewModelScope.launch(Dispatchers.IO) {
        timeRepository.finishActiveFocus(completeTask = true, note = "Completed focus session")
        afterMutation()
    }

    fun recordInterruption(reason: String = "Context switch", minutes: Int = 5) = viewModelScope.launch(Dispatchers.IO) {
        timeRepository.recordInterruption(reason, minutes)
        afterMutation()
    }

    fun logBreak(minutes: Int = 10) = viewModelScope.launch(Dispatchers.IO) {
        timeRepository.logBreak(minutes)
        afterMutation()
    }

    fun setThemeMode(value: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(value) }
    fun setAccentColor(value: Long) = viewModelScope.launch { settingsRepository.setAccentColor(value) }
    fun setCardStyle(value: CardStyle) = viewModelScope.launch { settingsRepository.setCardStyle(value) }
    fun setDensity(value: LayoutDensity) = viewModelScope.launch { settingsRepository.setDensity(value) }
    fun setScoringStyle(value: ScoringStyle) = viewModelScope.launch { settingsRepository.setScoringStyle(value) }
    fun setNotificationTone(value: String) = viewModelScope.launch { settingsRepository.setNotificationTone(value) }

    fun toggleDashboardModule(key: String) = viewModelScope.launch {
        val modules = uiState.value.settings.dashboardModules.map { module ->
            if (module.key == key) module.copy(visible = !module.visible) else module
        }
        settingsRepository.setDashboardModules(modules)
    }

    fun addCategory(name: String, color: Long) = viewModelScope.launch(Dispatchers.IO) {
        timeRepository.saveCategory(Category(0, name.ifBlank { "Custom" }, color, "label"))
        afterMutation()
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
        widgetUpdater.updateAll()
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

class TimeManagementViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TimeManagementViewModel(
            timeRepository = container.timeRepository,
            settingsRepository = container.settingsRepository,
            calculator = container.productivityCalculator,
            suggestionEngine = container.smartSuggestionEngine,
            reminderScheduler = container.reminderScheduler,
            widgetUpdater = container.widgetUpdater,
        ) as T
    }
}

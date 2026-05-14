package com.yourapp.timemanagement.domain

import java.time.LocalDate
import java.time.LocalTime

enum class TaskPriority { Low, Medium, High, Critical }
enum class TaskStatus { Planned, InProgress, Completed, Skipped, Overdue }
enum class RecurrenceRule { None, Daily, Weekdays, Weekly }
enum class SessionType { Focus, Break, Distracted }
enum class ThemeMode { System, Light, Dark, Amoled }
enum class CardStyle { Rounded, Sharp }
enum class LayoutDensity { Compact, Spacious }
enum class ScoringStyle { Balanced, FocusHeavy, CompletionHeavy }

data class SubTask(
    val id: Long,
    val parentTaskId: Long,
    val title: String,
    val isCompleted: Boolean,
)

data class Tag(
    val id: Long,
    val name: String,
    val color: Long,
)

data class FocusPreset(
    val id: Long,
    val name: String,
    val focusMinutes: Int,
    val breakMinutes: Int,
)

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val calendarName: String,
)

data class TaskTimePrediction(
    val taskId: Long,
    val suggestedHour: Int,
    val confidence: Float,
    val reason: String,
)

data class FlowState(
    val active: Boolean,
    val consecutiveSessions: Int,
    val totalMinutes: Int,
    val suggestedNextBlockMinutes: Int,
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val xpReward: Int,
)

data class GamificationState(
    val xp: Int = 0,
    val level: Int = 1,
    val levelProgress: Float = 0f,
    val currentLevelXp: Int = 0,
    val nextLevelXp: Int = 100,
    val streakDays: Int = 0,
    val unlockedAchievements: List<Achievement> = emptyList(),
)

data class Category(
    val id: Long,
    val name: String,
    val color: Long,
    val iconName: String,
)

data class Task(
    val id: Long,
    val title: String,
    val notes: String,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val estimateMinutes: Int,
    val priority: TaskPriority,
    val categoryId: Long,
    val tag: String,
    val recurrence: RecurrenceRule,
    val reminderMinutesBefore: Int?,
    val status: TaskStatus,
    val sortOrder: Int,
    val createdAtMillis: Long,
) {
    val durationMinutes: Int
        get() = java.time.Duration.between(startTime, endTime).toMinutes().coerceAtLeast(0).toInt()

    fun isCurrent(now: LocalTime = LocalTime.now()): Boolean =
        status == TaskStatus.Planned && !now.isBefore(startTime) && now.isBefore(endTime)
}

data class FocusSession(
    val id: Long,
    val taskId: Long?,
    val categoryId: Long?,
    val type: SessionType,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val productiveMinutes: Int,
    val distractedMinutes: Int,
    val breakMinutes: Int,
    val interruptionCount: Int,
    val note: String,
) {
    val durationMinutes: Int
        get() {
            val end = endedAtMillis ?: System.currentTimeMillis()
            return ((end - startedAtMillis) / 60_000L).coerceAtLeast(0).toInt()
        }
}

data class ProductivityStats(
    val plannedMinutes: Int = 0,
    val actualProductiveMinutes: Int = 0,
    val distractedMinutes: Int = 0,
    val breakMinutes: Int = 0,
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val skippedTasks: Int = 0,
    val overdueTasks: Int = 0,
    val longestFocusStreakMinutes: Int = 0,
    val score: Int = 0,
)

data class DashboardModule(
    val key: String,
    val label: String,
    val visible: Boolean = true,
)

data class UserSettings(
    val onboardingComplete: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.System,
    val accentColor: Long = 0xFF1F8A70,
    val cardStyle: CardStyle = CardStyle.Rounded,
    val density: LayoutDensity = LayoutDensity.Spacious,
    val dashboardModules: List<DashboardModule> = defaultDashboardModules,
    val scoringStyle: ScoringStyle = ScoringStyle.Balanced,
    val focusPresets: List<FocusPreset> = defaultFocusPresets,
    val notificationTone: String = "Gentle chime",
    val seededSampleData: Boolean = false,
    val xp: Int = 0,
    val level: Int = 1,
    val streakDays: Int = 0,
    val unlockedAchievementIds: Set<String> = emptySet(),
) {
    val focusPresetMinutes: List<Int>
        get() = focusPresets.map(FocusPreset::focusMinutes)
}

data class WidgetPreferences(
    val widgetId: Int,
    val widgetType: String,
    val accentColor: Long,
    val compact: Boolean,
)

data class SmartInsight(
    val title: String,
    val message: String,
    val severity: InsightSeverity = InsightSeverity.Info,
)

enum class InsightSeverity { Info, Warning, Success }

data class CategoryBreakdown(
    val category: Category,
    val minutes: Int,
)

data class HourProductivity(
    val hour: Int,
    val productiveMinutes: Int,
)

data class AnalyticsSnapshot(
    val stats: ProductivityStats,
    val categoryBreakdown: List<CategoryBreakdown>,
    val mostProductiveHours: List<HourProductivity>,
    val weeklyScores: List<Int>,
    val plannedVsActualPercent: Int,
    val insights: List<SmartInsight>,
)

val defaultDashboardModules = listOf(
    DashboardModule("goal", "Daily goal"),
    DashboardModule("timeline", "Timeline"),
    DashboardModule("focus", "Focus"),
    DashboardModule("score", "Score"),
    DashboardModule("streak", "Streak"),
    DashboardModule("insight", "Insight"),
)

val defaultFocusPresets = listOf(
    FocusPreset(1, "Pomodoro", 25, 5),
    FocusPreset(2, "Deep Work", 50, 10),
    FocusPreset(3, "Sprint", 15, 3),
)

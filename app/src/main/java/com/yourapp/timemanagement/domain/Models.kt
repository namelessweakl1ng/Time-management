package com.yourapp.timemanagement.domain

import java.time.LocalDate
import java.time.LocalTime

enum class TaskPriority { Low, Medium, High, Critical }
enum class TaskStatus { Planned, InProgress, Completed, Skipped, Overdue }
enum class RecurrenceRule { None, Daily, Weekdays, Weekly }
enum class SessionType { Focus, Break, Distracted }
enum class ThemeMode { System, Light, Dark }
enum class CardStyle { Rounded, Sharp }
enum class LayoutDensity { Compact, Spacious }
enum class ScoringStyle { Balanced, FocusHeavy, CompletionHeavy }

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
    val focusPresetMinutes: List<Int> = listOf(25, 45, 60),
    val notificationTone: String = "Gentle chime",
    val seededSampleData: Boolean = false,
)

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

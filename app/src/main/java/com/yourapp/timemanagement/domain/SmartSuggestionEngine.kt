package com.yourapp.timemanagement.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SmartSuggestionEngine {
    fun insights(
        tasks: List<Task>,
        sessions: List<FocusSession>,
        categories: List<Category>,
        stats: ProductivityStats,
    ): List<SmartInsight> {
        val result = mutableListOf<SmartInsight>()
        val plannedMinutes = tasks.sumOf { it.estimateMinutes }

        if (plannedMinutes > 9 * 60) {
            result += SmartInsight(
                title = "Today is overbooked",
                message = "You planned ${plannedMinutes / 60}h ${plannedMinutes % 60}m. Move one low priority task to protect focus time.",
                severity = InsightSeverity.Warning,
            )
        }

        val underEstimated = tasks.filter { task ->
            val actual = sessions
                .filter { it.taskId == task.id }
                .sumOf { it.productiveMinutes + it.distractedMinutes }
            actual > task.estimateMinutes * 1.35f && actual > 15
        }
        if (underEstimated.isNotEmpty()) {
            result += SmartInsight(
                title = "Estimates need padding",
                message = "${underEstimated.first().title} tends to run long. Add a 15 minute buffer next time.",
                severity = InsightSeverity.Warning,
            )
        }

        if (stats.distractedMinutes > stats.actualProductiveMinutes / 3 && stats.distractedMinutes > 20) {
            result += SmartInsight(
                title = "Distractions are climbing",
                message = "You logged ${stats.distractedMinutes} distracted minutes. Try a shorter focus preset and one planned break.",
                severity = InsightSeverity.Warning,
            )
        }

        bestFocusWindow(sessions)?.let { window ->
            result += SmartInsight(
                title = "Best focus window",
                message = "Your strongest focus usually starts around ${window}:00. Put deep work there first.",
                severity = InsightSeverity.Success,
            )
        }

        if (stats.completedTasks == stats.totalTasks && stats.totalTasks > 0) {
            result += SmartInsight(
                title = "Clean finish",
                message = "All planned tasks are done. Keep tomorrow lighter and repeat the same rhythm.",
                severity = InsightSeverity.Success,
            )
        }

        val categoryMinutes = sessions.groupBy { it.categoryId }.mapValues { entry ->
            entry.value.sumOf { it.productiveMinutes + it.distractedMinutes }
        }
        val topCategory = categoryMinutes.maxByOrNull { it.value }
        if (topCategory != null && topCategory.value > 180) {
            val name = categories.firstOrNull { it.id == topCategory.key }?.name ?: "one category"
            result += SmartInsight(
                title = "Category balance",
                message = "$name used ${topCategory.value / 60}h today. Add a recovery block if that was heavy work.",
            )
        }

        return if (result.isEmpty()) {
            listOf(
                SmartInsight(
                    title = "Ready for a focused day",
                    message = "Your plan is realistic. Start with the highest priority task and protect the first focus block.",
                    severity = InsightSeverity.Success,
                ),
            )
        } else {
            result.take(4)
        }
    }

    fun bestFocusWindow(sessions: List<FocusSession>): Int? {
        return sessions
            .filter { it.type == SessionType.Focus && it.productiveMinutes > 0 }
            .groupBy {
                Instant.ofEpochMilli(it.startedAtMillis)
                    .atZone(ZoneId.systemDefault())
                    .hour
            }
            .mapValues { it.value.sumOf(FocusSession::productiveMinutes) }
            .maxByOrNull { it.value }
            ?.key
    }

    fun isOverbooked(tasks: List<Task>): Boolean = tasks.sumOf { it.estimateMinutes } > 9 * 60

    fun shouldTakeBreak(sessions: List<FocusSession>): Boolean {
        val today = LocalDate.now()
        val recentFocus = sessions
            .filter {
                it.type == SessionType.Focus &&
                    Instant.ofEpochMilli(it.startedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate() == today
            }
            .sortedByDescending { it.startedAtMillis }
            .take(2)
            .sumOf { it.productiveMinutes }
        return recentFocus >= 75
    }
}

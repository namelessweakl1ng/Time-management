package com.yourapp.timemanagement.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductivityCalculator @Inject constructor() {
    fun calculate(
        tasks: List<Task>,
        sessions: List<FocusSession>,
        settings: UserSettings = UserSettings(),
    ): ProductivityStats {
        val completed = tasks.count { it.status == TaskStatus.Completed }
        val total = tasks.size.coerceAtLeast(1)
        val productive = sessions.sumOf { it.productiveMinutes }
        val distracted = sessions.sumOf { it.distractedMinutes }
        val breaks = sessions.sumOf { it.breakMinutes }
        val planned = tasks.sumOf { it.estimateMinutes.coerceAtLeast(it.durationMinutes) }
        val completionScore = completed * 100 / total
        val focusScore = if (planned == 0) 0 else (productive * 100 / planned).coerceAtMost(120)
        val distractionPenalty = (distracted * 100 / (productive + distracted + 1)).coerceAtMost(35)

        val weighted = when (settings.scoringStyle) {
            ScoringStyle.Balanced -> (completionScore * 0.45f + focusScore * 0.55f)
            ScoringStyle.FocusHeavy -> (completionScore * 0.25f + focusScore * 0.75f)
            ScoringStyle.CompletionHeavy -> (completionScore * 0.70f + focusScore * 0.30f)
        }

        return ProductivityStats(
            plannedMinutes = planned,
            actualProductiveMinutes = productive,
            distractedMinutes = distracted,
            breakMinutes = breaks,
            completedTasks = completed,
            totalTasks = tasks.size,
            skippedTasks = tasks.count { it.status == TaskStatus.Skipped },
            overdueTasks = tasks.count { it.status == TaskStatus.Overdue },
            longestFocusStreakMinutes = longestFocusStreak(sessions),
            score = (weighted.toInt() - distractionPenalty).coerceIn(0, 100),
        )
    }

    fun plannedVsActualPercent(tasks: List<Task>, sessions: List<FocusSession>): Int {
        val planned = tasks.sumOf { it.estimateMinutes.coerceAtLeast(1) }
        val actual = sessions.sumOf { it.productiveMinutes }
        return if (planned == 0) 0 else (actual * 100 / planned).coerceIn(0, 200)
    }

    fun longestFocusStreak(sessions: List<FocusSession>): Int {
        return sessions
            .filter { it.type == SessionType.Focus }
            .maxOfOrNull { it.productiveMinutes }
            ?: 0
    }

    fun weeklyScores(
        tasks: List<Task>,
        sessions: List<FocusSession>,
        settings: UserSettings,
        today: LocalDate = LocalDate.now(),
    ): List<Int> {
        return (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val dayTasks = tasks.filter { it.date == date }
            val daySessions = sessions.filter { session ->
                Instant.ofEpochMilli(session.startedAtMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate() == date
            }
            calculate(dayTasks, daySessions, settings).score
        }
    }
}

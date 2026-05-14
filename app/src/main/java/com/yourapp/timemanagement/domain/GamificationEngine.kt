package com.yourapp.timemanagement.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class GamificationEngine @Inject constructor() {
    val achievements = listOf(
        Achievement("early_bird", "Early Bird", "Complete 5 tasks scheduled before 8 AM.", 150),
        Achievement("focus_master", "Focus Master", "Reach 100 hours of focused work.", 500),
        Achievement("streak_starter", "Streak Starter", "Build a 3 day productivity streak.", 120),
        Achievement("finisher", "Finisher", "Complete 50 planned tasks.", 250),
        Achievement("flow_rider", "Flow Rider", "Complete two strong focus sessions back-to-back.", 180),
    )

    fun calculate(tasks: List<Task>, sessions: List<FocusSession>, unlockedIds: Set<String>): GamificationState {
        val completedTasks = tasks.count { it.status == TaskStatus.Completed }
        val productiveMinutes = sessions.sumOf(FocusSession::productiveMinutes)
        val streak = streakDays(tasks, sessions)
        val earnedAchievements = achievements.filter { achievement ->
            achievement.id in unlockedIds || qualifies(achievement.id, tasks, sessions, streak)
        }
        val baseXp = completedTasks * 20 + productiveMinutes * 2 + streak * 25
        val achievementXp = earnedAchievements.sumOf(Achievement::xpReward)
        val xp = baseXp + achievementXp
        val level = levelForXp(xp)
        val current = xpForLevel(level)
        val next = xpForLevel(level + 1)
        val progress = if (next == current) 1f else ((xp - current).toFloat() / (next - current)).coerceIn(0f, 1f)
        return GamificationState(
            xp = xp,
            level = level,
            levelProgress = progress,
            currentLevelXp = current,
            nextLevelXp = next,
            streakDays = streak,
            unlockedAchievements = earnedAchievements,
        )
    }

    fun newlyUnlocked(previousIds: Set<String>, state: GamificationState): List<Achievement> =
        state.unlockedAchievements.filterNot { it.id in previousIds }

    private fun qualifies(id: String, tasks: List<Task>, sessions: List<FocusSession>, streak: Int): Boolean =
        when (id) {
            "early_bird" -> tasks.count { it.status == TaskStatus.Completed && it.startTime.hour < 8 } >= 5
            "focus_master" -> sessions.sumOf(FocusSession::productiveMinutes) >= 6_000
            "streak_starter" -> streak >= 3
            "finisher" -> tasks.count { it.status == TaskStatus.Completed } >= 50
            "flow_rider" -> FlowStateDetector().detect(sessions).active
            else -> false
        }

    private fun streakDays(tasks: List<Task>, sessions: List<FocusSession>): Int {
        val completedTaskDates = tasks
            .filter { it.status == TaskStatus.Completed }
            .map(Task::date)
            .toSet()
        val focusDates = sessions
            .filter { it.productiveMinutes >= 25 }
            .map { Instant.ofEpochMilli(it.startedAtMillis).atZone(ZoneId.systemDefault()).toLocalDate() }
            .toSet()
        val activeDates = completedTaskDates + focusDates
        var cursor = LocalDate.now()
        var count = 0
        while (cursor in activeDates) {
            count += 1
            cursor = cursor.minusDays(1)
        }
        return count
    }

    private fun levelForXp(xp: Int): Int =
        (sqrt(xp.coerceAtLeast(0) / 100.0).toInt() + 1).coerceAtLeast(1)

    private fun xpForLevel(level: Int): Int {
        val completedLevels = (level - 1).coerceAtLeast(0)
        return completedLevels * completedLevels * 100
    }
}

package com.yourapp.timemanagement.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class GamificationEngineTest {
    private val engine = GamificationEngine()

    @Test
    fun calculate_awardsXpLevelsAndFinisherAchievement() {
        val tasks = List(50) { index -> task(index.toLong() + 1, TaskStatus.Completed) }
        val state = engine.calculate(tasks, emptyList(), emptySet())

        assertTrue(state.xp >= 1_250)
        assertTrue(state.level > 1)
        assertTrue(state.unlockedAchievements.any { it.id == "finisher" })
    }

    @Test
    fun calculate_tracksCurrentDayStreak() {
        val tasks = listOf(task(1, TaskStatus.Completed, LocalDate.now()))

        val state = engine.calculate(tasks, emptyList(), emptySet())

        assertEquals(1, state.streakDays)
    }

    private fun task(
        id: Long,
        status: TaskStatus,
        date: LocalDate = LocalDate.now(),
    ) = Task(
        id = id,
        title = "Task $id",
        notes = "",
        date = date,
        startTime = LocalTime.of(7, 30),
        endTime = LocalTime.of(8, 0),
        estimateMinutes = 30,
        priority = TaskPriority.Medium,
        categoryId = 1,
        tag = "",
        recurrence = RecurrenceRule.None,
        reminderMinutesBefore = null,
        status = status,
        sortOrder = id.toInt(),
        createdAtMillis = 0,
    )
}

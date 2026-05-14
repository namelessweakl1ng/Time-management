package com.yourapp.timemanagement.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ProductivityCalculatorTest {
    private val calculator = ProductivityCalculator()

    @Test
    fun calculate_balancedScoreRewardsCompletedFocusedWork() {
        val tasks = listOf(
            task(id = 1, status = TaskStatus.Completed, estimate = 60),
            task(id = 2, status = TaskStatus.Completed, estimate = 60),
        )
        val sessions = listOf(session(productive = 115, distracted = 5))

        val stats = calculator.calculate(tasks, sessions)

        assertEquals(2, stats.completedTasks)
        assertEquals(115, stats.actualProductiveMinutes)
        assertTrue(stats.score >= 85)
    }

    @Test
    fun plannedVsActualPercent_clampsExtremeOverruns() {
        val tasks = listOf(task(estimate = 30))
        val sessions = listOf(session(productive = 120))

        assertEquals(200, calculator.plannedVsActualPercent(tasks, sessions))
    }

    @Test
    fun weeklyScores_returnsSevenDaysInChronologicalWindow() {
        val today = LocalDate.of(2026, 5, 14)
        val tasks = listOf(task(date = today, status = TaskStatus.Completed))

        assertEquals(7, calculator.weeklyScores(tasks, emptyList(), UserSettings(), today).size)
    }

    private fun task(
        id: Long = 1,
        date: LocalDate = LocalDate.now(),
        status: TaskStatus = TaskStatus.Planned,
        estimate: Int = 60,
    ) = Task(
        id = id,
        title = "Task $id",
        notes = "",
        date = date,
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        estimateMinutes = estimate,
        priority = TaskPriority.Medium,
        categoryId = 1,
        tag = "",
        recurrence = RecurrenceRule.None,
        reminderMinutesBefore = null,
        status = status,
        sortOrder = id.toInt(),
        createdAtMillis = 0,
    )

    private fun session(productive: Int, distracted: Int = 0) = FocusSession(
        id = 1,
        taskId = 1,
        categoryId = 1,
        type = SessionType.Focus,
        startedAtMillis = 0,
        endedAtMillis = 60_000,
        productiveMinutes = productive,
        distractedMinutes = distracted,
        breakMinutes = 0,
        interruptionCount = if (distracted > 0) 1 else 0,
        note = "",
    )
}

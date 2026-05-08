package com.yourapp.timemanagement.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ProductivityDomainTest {
    private val calculator = ProductivityCalculator()
    private val suggestions = SmartSuggestionEngine()

    @Test
    fun scoringRewardsCompletionAndFocus() {
        val tasks = listOf(
            task(status = TaskStatus.Completed, estimate = 60),
            task(id = 2, status = TaskStatus.Completed, estimate = 60),
        )
        val sessions = listOf(session(productive = 110, distracted = 5))

        val stats = calculator.calculate(tasks, sessions)

        assertTrue(stats.score >= 85)
        assertEquals(2, stats.completedTasks)
        assertEquals(110, stats.actualProductiveMinutes)
    }

    @Test
    fun plannedVsActualIsClampedPercentage() {
        val tasks = listOf(task(estimate = 60), task(id = 2, estimate = 60))
        val sessions = listOf(session(productive = 90))

        assertEquals(75, calculator.plannedVsActualPercent(tasks, sessions))
    }

    @Test
    fun detectsOverbookedDay() {
        val tasks = List(10) { index -> task(id = index.toLong() + 1, estimate = 60) }

        assertTrue(suggestions.isOverbooked(tasks))
    }

    @Test
    fun bestFocusWindowUsesMostProductiveHour() {
        val nine = LocalDate.now().atTime(9, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val eleven = LocalDate.now().atTime(11, 0).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val sessions = listOf(
            session(start = nine, productive = 90),
            session(start = eleven, productive = 30),
        )

        assertEquals(9, suggestions.bestFocusWindow(sessions))
    }

    @Test
    fun recurrenceSkipsWeekendsForWeekdayRule() {
        val planner = RecurrencePlanner()
        val friday = LocalDate.of(2026, 5, 8)

        val dates = planner.dates(RecurrenceRule.Weekdays, friday, 3)

        assertEquals(
            listOf(LocalDate.of(2026, 5, 8), LocalDate.of(2026, 5, 11), LocalDate.of(2026, 5, 12)),
            dates,
        )
    }

    @Test
    fun activeSessionDurationComesFromTimestamps() {
        val start = 1_000_000L
        val end = start + 42 * 60_000L

        assertEquals(42, session(start = start, end = end).durationMinutes)
    }

    private fun task(
        id: Long = 1,
        status: TaskStatus = TaskStatus.Planned,
        estimate: Int = 60,
    ) = Task(
        id = id,
        title = "Task $id",
        notes = "",
        date = LocalDate.now(),
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

    private fun session(
        start: Long = 0,
        end: Long? = start + 60_000L,
        productive: Int = 60,
        distracted: Int = 0,
    ) = FocusSession(
        id = 1,
        taskId = 1,
        categoryId = 1,
        type = SessionType.Focus,
        startedAtMillis = start,
        endedAtMillis = end,
        productiveMinutes = productive,
        distractedMinutes = distracted,
        breakMinutes = 0,
        interruptionCount = if (distracted > 0) 1 else 0,
        note = "",
    )
}

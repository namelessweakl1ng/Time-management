package com.yourapp.timemanagement.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class SmartSuggestionEngineTest {
    private val engine = SmartSuggestionEngine()

    @Test
    fun insights_warnsWhenDayIsOverbooked() {
        val tasks = List(10) { index -> task(id = index + 1L, estimate = 60) }

        val insights = engine.insights(tasks, emptyList(), emptyList(), ProductivityStats(totalTasks = tasks.size))

        assertTrue(insights.any { it.title == "Today is overbooked" })
    }

    @Test
    fun bestFocusWindow_returnsMostProductiveStartingHour() {
        val nine = LocalDate.of(2026, 5, 14).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val fourteen = LocalDate.of(2026, 5, 14).atTime(14, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        assertEquals(14, engine.bestFocusWindow(listOf(session(nine, 20), session(fourteen, 90))))
    }

    @Test
    fun defaultInsight_isReturnedForRealisticPlan() {
        val insights = engine.insights(listOf(task()), emptyList(), emptyList(), ProductivityStats(totalTasks = 1))

        assertNotNull(insights.singleOrNull { it.title == "Ready for a focused day" })
    }

    private fun task(id: Long = 1, estimate: Int = 45) = Task(
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
        status = TaskStatus.Planned,
        sortOrder = id.toInt(),
        createdAtMillis = 0,
    )

    private fun session(start: Long, productive: Int) = FocusSession(
        id = start,
        taskId = null,
        categoryId = null,
        type = SessionType.Focus,
        startedAtMillis = start,
        endedAtMillis = start + productive * 60_000L,
        productiveMinutes = productive,
        distractedMinutes = 0,
        breakMinutes = 0,
        interruptionCount = 0,
        note = "",
    )
}

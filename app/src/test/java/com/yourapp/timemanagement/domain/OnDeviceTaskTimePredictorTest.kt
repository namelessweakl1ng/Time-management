package com.yourapp.timemanagement.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class OnDeviceTaskTimePredictorTest {
    private val predictor = OnDeviceTaskTimePredictor()
    private val flowDetector = FlowStateDetector()

    @Test
    fun predict_prefersMostProductiveHistoricalHour() {
        val task = task()
        val predictions = predictor.predict(
            tasks = listOf(task),
            sessions = listOf(session(hour = 9, productive = 25), session(hour = 14, productive = 90)),
        )

        assertEquals(14, predictions.single().suggestedHour)
        assertTrue(predictions.single().confidence > 0.5f)
    }

    @Test
    fun detect_returnsActiveFlowAfterConsecutiveSessions() {
        val now = System.currentTimeMillis()
        val sessions = listOf(
            focus(start = now - 50 * 60_000L, end = now - 5 * 60_000L, minutes = 45),
            focus(start = now - 105 * 60_000L, end = now - 60 * 60_000L, minutes = 45),
        )

        val state = flowDetector.detect(sessions)

        assertTrue(state.active)
        assertEquals(2, state.consecutiveSessions)
    }

    private fun task() = Task(
        id = 1,
        title = "Design review",
        notes = "",
        date = LocalDate.now(),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
        estimateMinutes = 60,
        priority = TaskPriority.High,
        categoryId = 4,
        tag = "",
        recurrence = RecurrenceRule.None,
        reminderMinutesBefore = null,
        status = TaskStatus.Planned,
        sortOrder = 0,
        createdAtMillis = 0,
    )

    private fun session(hour: Int, productive: Int): FocusSession {
        val start = LocalDate.of(2026, 5, 14).atTime(hour, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return focus(start, start + productive * 60_000L, productive)
    }

    private fun focus(start: Long, end: Long, minutes: Int) = FocusSession(
        id = start,
        taskId = null,
        categoryId = 4,
        type = SessionType.Focus,
        startedAtMillis = start,
        endedAtMillis = end,
        productiveMinutes = minutes,
        distractedMinutes = 0,
        breakMinutes = 0,
        interruptionCount = 0,
        note = "",
    )
}

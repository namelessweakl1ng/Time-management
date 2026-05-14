package com.yourapp.timemanagement.domain

import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

interface TaskTimePredictor {
    fun predict(tasks: List<Task>, sessions: List<FocusSession>): List<TaskTimePrediction>
}

@Singleton
class OnDeviceTaskTimePredictor @Inject constructor() : TaskTimePredictor {
    override fun predict(tasks: List<Task>, sessions: List<FocusSession>): List<TaskTimePrediction> {
        val hourScores = sessions
            .filter { it.type == SessionType.Focus && it.productiveMinutes > 0 }
            .groupBy { Instant.ofEpochMilli(it.startedAtMillis).atZone(ZoneId.systemDefault()).hour }
            .mapValues { (_, bucket) ->
                bucket.sumOf { session ->
                    session.productiveMinutes - session.distractedMinutes.coerceAtMost(session.productiveMinutes / 2)
                }.coerceAtLeast(0)
            }
        val fallbackHour = hourScores.maxByOrNull { it.value }?.key ?: 9
        val maxScore = hourScores.values.maxOrNull()?.coerceAtLeast(1) ?: 1

        return tasks
            .filter { it.status == TaskStatus.Planned || it.status == TaskStatus.Overdue }
            .map { task ->
                val preferredHour = categoryHour(task, sessions) ?: fallbackHour
                val hourScore = hourScores[preferredHour] ?: maxScore / 2
                val distancePenalty = (abs(task.startTime.hour - preferredHour) * 0.04f).coerceAtMost(0.24f)
                val confidence = ((hourScore.toFloat() / maxScore) - distancePenalty).coerceIn(0.35f, 0.96f)
                TaskTimePrediction(
                    taskId = task.id,
                    suggestedHour = preferredHour,
                    confidence = confidence,
                    reason = if (sessions.isEmpty()) {
                        "No training history yet; starting with a calm morning focus default."
                    } else {
                        "Learned from your recent local focus sessions and category rhythm."
                    },
                )
            }
            .sortedByDescending(TaskTimePrediction::confidence)
    }

    private fun categoryHour(task: Task, sessions: List<FocusSession>): Int? =
        sessions
            .filter { it.categoryId == task.categoryId && it.type == SessionType.Focus && it.productiveMinutes > 0 }
            .groupBy { Instant.ofEpochMilli(it.startedAtMillis).atZone(ZoneId.systemDefault()).hour }
            .mapValues { it.value.sumOf(FocusSession::productiveMinutes) }
            .maxByOrNull { it.value }
            ?.key
}

@Singleton
class FlowStateDetector @Inject constructor() {
    fun detect(sessions: List<FocusSession>): FlowState {
        val recent = sessions
            .filter { it.type == SessionType.Focus && it.endedAtMillis != null && it.productiveMinutes >= 20 }
            .sortedByDescending { it.endedAtMillis }
        if (recent.isEmpty()) return FlowState(false, 0, 0, 25)

        var consecutive = 0
        var totalMinutes = 0
        var previousStart: Long? = null
        for (session in recent) {
            val ended = session.endedAtMillis ?: break
            val gapMinutes = previousStart?.let { (it - ended) / 60_000L } ?: 0L
            if (consecutive > 0 && gapMinutes > 15) break
            consecutive += 1
            totalMinutes += session.productiveMinutes
            previousStart = session.startedAtMillis
        }
        return FlowState(
            active = consecutive >= 2 && totalMinutes >= 75,
            consecutiveSessions = consecutive,
            totalMinutes = totalMinutes,
            suggestedNextBlockMinutes = when {
                totalMinutes >= 120 -> 50
                totalMinutes >= 75 -> 25
                else -> 20
            },
        )
    }
}

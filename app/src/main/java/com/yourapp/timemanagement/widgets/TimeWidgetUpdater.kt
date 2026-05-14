package com.yourapp.timemanagement.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.yourapp.timemanagement.MainActivity
import com.yourapp.timemanagement.R
import com.yourapp.timemanagement.data.repository.SettingsRepository
import com.yourapp.timemanagement.data.repository.TimeRepository
import com.yourapp.timemanagement.domain.ProductivityCalculator
import com.yourapp.timemanagement.domain.SmartSuggestionEngine
import com.yourapp.timemanagement.domain.TaskStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeWidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val timeRepository: TimeRepository,
    private val settingsRepository: SettingsRepository,
    private val calculator: ProductivityCalculator,
    private val suggestionEngine: SmartSuggestionEngine,
) {
    private val manager: AppWidgetManager
        get() = AppWidgetManager.getInstance(context)

    suspend fun updateAll() {
        updateProvider(TodayWidgetProvider::class.java, WIDGET_TODAY)
        updateProvider(FocusWidgetProvider::class.java, WIDGET_FOCUS)
        updateProvider(ProgressWidgetProvider::class.java, WIDGET_PROGRESS)
        updateProvider(QuickAddWidgetProvider::class.java, WIDGET_QUICK_ADD)
        updateProvider(MotivationWidgetProvider::class.java, WIDGET_MOTIVATION)
    }

    suspend fun update(widgetType: String, ids: IntArray) {
        ids.forEach { widgetId -> manager.updateAppWidget(widgetId, render(widgetType, widgetId)) }
    }

    private suspend fun updateProvider(providerClass: Class<*>, widgetType: String) {
        val ids = manager.getAppWidgetIds(ComponentName(context, providerClass))
        update(widgetType, ids)
    }

    private suspend fun render(widgetType: String, widgetId: Int): RemoteViews {
        val todayTasks = timeRepository.tasksForDate(LocalDate.now()).first()
        val sessions = timeRepository.sessionsForDate(LocalDate.now()).first()
        val categories = timeRepository.categories.first()
        val settings = settingsRepository.settings.first()
        val stats = calculator.calculate(todayTasks, sessions, settings)
        val insights = suggestionEngine.insights(todayTasks, sessions, categories, stats)
        val preference = timeRepository.getWidgetPreference(widgetId)
        return when (widgetType) {
            WIDGET_FOCUS -> focusWidget(todayTasks, stats)
            WIDGET_PROGRESS -> progressWidget(stats, todayTasks, preference?.widgetType ?: METRIC_PRODUCTIVITY_SCORE)
            WIDGET_QUICK_ADD -> quickAddWidget()
            WIDGET_MOTIVATION -> motivationWidget(insights.firstOrNull()?.message ?: "Protect one focused block today.")
            else -> todayWidget(todayTasks)
        }
    }

    private fun todayWidget(tasks: List<com.yourapp.timemanagement.domain.Task>): RemoteViews {
        val next = tasks.firstOrNull { it.status == TaskStatus.Planned || it.status == TaskStatus.InProgress }
        val body = tasks.take(3).joinToString("\n") { "${it.startTime.label()}  ${it.title}" }.ifBlank { "No tasks planned yet" }
        return RemoteViews(context.packageName, R.layout.widget_today).apply {
            setTextViewText(R.id.widget_title, "Today")
            setTextViewText(R.id.widget_subtitle, next?.let { "Next: ${it.title}" } ?: "Plan a calm day")
            setTextViewText(R.id.widget_body, body)
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(10))
        }
    }

    private fun focusWidget(
        tasks: List<com.yourapp.timemanagement.domain.Task>,
        stats: com.yourapp.timemanagement.domain.ProductivityStats,
    ): RemoteViews {
        val current = tasks.firstOrNull { it.status == TaskStatus.InProgress }
            ?: tasks.firstOrNull { it.status == TaskStatus.Planned }
        return RemoteViews(context.packageName, R.layout.widget_focus).apply {
            setTextViewText(R.id.widget_title, "Focus")
            setTextViewText(R.id.widget_subtitle, current?.title ?: "No active block")
            setTextViewText(R.id.widget_body, "${stats.actualProductiveMinutes} productive minutes today")
            setTextViewText(R.id.widget_button, "Start")
            setOnClickPendingIntent(R.id.widget_button, actionIntent(ACTION_WIDGET_START_FOCUS, 20))
            setOnClickPendingIntent(R.id.widget_secondary_button, actionIntent(ACTION_WIDGET_PAUSE_FOCUS, 21))
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(22))
        }
    }

    private fun progressWidget(
        stats: com.yourapp.timemanagement.domain.ProductivityStats,
        tasks: List<com.yourapp.timemanagement.domain.Task>,
        metric: String,
    ): RemoteViews {
        val (score, body) = when (metric) {
            METRIC_TODAY_TASKS -> tasks.count { it.status == TaskStatus.Completed }.toString() to "${tasks.size} tasks planned today"
            METRIC_FOCUS_MINUTES -> stats.actualProductiveMinutes.toString() to "focused minutes today"
            else -> stats.score.toString() to "${stats.completedTasks}/${stats.totalTasks} done  |  ${stats.actualProductiveMinutes}m focused"
        }
        return RemoteViews(context.packageName, R.layout.widget_progress).apply {
            setTextViewText(R.id.widget_title, "Daily progress")
            setTextViewText(R.id.widget_score, score)
            setTextViewText(R.id.widget_body, body)
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(30))
        }
    }

    private fun quickAddWidget(): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_quick_add).apply {
            setTextViewText(R.id.widget_title, "Quick add")
            setTextViewText(R.id.widget_body, "Capture a 25 minute task for today.")
            setOnClickPendingIntent(R.id.widget_button, actionIntent(ACTION_WIDGET_QUICK_ADD, 40))
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(41))
        }
    }

    private fun motivationWidget(message: String): RemoteViews {
        return RemoteViews(context.packageName, R.layout.widget_motivation).apply {
            setTextViewText(R.id.widget_title, "Nudge")
            setTextViewText(R.id.widget_body, message)
            setOnClickPendingIntent(R.id.widget_root, openAppIntent(50))
        }
    }

    private fun openAppIntent(requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, TimeWidgetActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun LocalTime.label(): String {
        val suffix = if (hour >= 12) "PM" else "AM"
        val hourLabel = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$hourLabel:${minute.toString().padStart(2, '0')} $suffix"
    }
}

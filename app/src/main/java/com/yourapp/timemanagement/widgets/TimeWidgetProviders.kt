package com.yourapp.timemanagement.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.yourapp.timemanagement.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseTimeWidgetProvider(private val widgetType: String) : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            context.appContainer().widgetUpdater.update(widgetType, appWidgetIds)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_REFRESH) {
            CoroutineScope(Dispatchers.IO).launch {
                context.appContainer().widgetUpdater.updateAll()
            }
        }
    }
}

class TodayWidgetProvider : BaseTimeWidgetProvider(WIDGET_TODAY)
class FocusWidgetProvider : BaseTimeWidgetProvider(WIDGET_FOCUS)
class ProgressWidgetProvider : BaseTimeWidgetProvider(WIDGET_PROGRESS)
class QuickAddWidgetProvider : BaseTimeWidgetProvider(WIDGET_QUICK_ADD)
class MotivationWidgetProvider : BaseTimeWidgetProvider(WIDGET_MOTIVATION)

package com.yourapp.timemanagement.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseTimeWidgetProvider(private val widgetType: String) : AppWidgetProvider() {
    @Inject lateinit var widgetUpdater: TimeWidgetUpdater

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        CoroutineScope(Dispatchers.IO).launch {
            widgetUpdater.update(widgetType, appWidgetIds)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_WIDGET_REFRESH) {
            CoroutineScope(Dispatchers.IO).launch {
                widgetUpdater.updateAll()
            }
        }
    }
}

@AndroidEntryPoint
class TodayWidgetProvider : BaseTimeWidgetProvider(WIDGET_TODAY)
@AndroidEntryPoint
class FocusWidgetProvider : BaseTimeWidgetProvider(WIDGET_FOCUS)
@AndroidEntryPoint
class ProgressWidgetProvider : BaseTimeWidgetProvider(WIDGET_PROGRESS)
@AndroidEntryPoint
class QuickAddWidgetProvider : BaseTimeWidgetProvider(WIDGET_QUICK_ADD)
@AndroidEntryPoint
class MotivationWidgetProvider : BaseTimeWidgetProvider(WIDGET_MOTIVATION)

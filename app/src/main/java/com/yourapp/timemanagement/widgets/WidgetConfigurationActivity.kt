package com.yourapp.timemanagement.widgets

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.yourapp.timemanagement.data.local.WidgetPreferenceEntity
import com.yourapp.timemanagement.data.repository.TimeRepository
import com.yourapp.timemanagement.ui.theme.TimeManagementTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WidgetConfigurationActivity : ComponentActivity() {
    @Inject lateinit var timeRepository: TimeRepository
    @Inject lateinit var widgetUpdater: TimeWidgetUpdater

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        setResult(Activity.RESULT_CANCELED)
        setContent {
            TimeManagementTheme {
                var selectedMetric by remember { mutableStateOf(METRIC_PRODUCTIVITY_SCORE) }
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    Text("Choose widget metric", style = MaterialTheme.typography.headlineMedium)
                    Text("This controls what the progress widget emphasizes on your launcher.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    WidgetMetricChoice("Productivity score", METRIC_PRODUCTIVITY_SCORE, selectedMetric) { selectedMetric = it }
                    WidgetMetricChoice("Today's completed tasks", METRIC_TODAY_TASKS, selectedMetric) { selectedMetric = it }
                    WidgetMetricChoice("Focus minutes", METRIC_FOCUS_MINUTES, selectedMetric) { selectedMetric = it }
                    Button(
                        onClick = {
                            lifecycleScope.launch {
                                timeRepository.upsertWidgetPreference(
                                    WidgetPreferenceEntity(
                                        widgetId = widgetId,
                                        widgetType = selectedMetric,
                                        accentColor = 0xFF1F8A70,
                                        compact = false,
                                    ),
                                )
                                widgetUpdater.update(WIDGET_PROGRESS, intArrayOf(widgetId))
                                val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                                setResult(Activity.RESULT_OK, result)
                                finish()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Save widget") }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun WidgetMetricChoice(label: String, value: String, selected: String, onSelected: (String) -> Unit) {
    FilterChip(
        selected = selected == value,
        onClick = { onSelected(value) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
    )
}

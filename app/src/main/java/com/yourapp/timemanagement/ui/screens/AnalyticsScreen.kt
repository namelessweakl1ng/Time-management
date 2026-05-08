package com.yourapp.timemanagement.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourapp.timemanagement.core.TimeManagementUiState
import com.yourapp.timemanagement.domain.CategoryBreakdown
import com.yourapp.timemanagement.ui.common.PremiumCard
import com.yourapp.timemanagement.ui.common.SectionHeader
import com.yourapp.timemanagement.ui.common.StatPill
import com.yourapp.timemanagement.ui.common.formatMinutes

@Composable
fun AnalyticsScreen(
    contentPadding: PaddingValues,
    uiState: TimeManagementUiState,
) {
    LazyColumn(
        modifier = Modifier.padding(contentPadding),
        contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SectionHeader("Productivity trends", "What your week says about your rhythm.")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatPill("Score", "${uiState.analytics.stats.score}", Modifier.weight(1f), MaterialTheme.colorScheme.primary)
                StatPill("Planned", "${uiState.analytics.plannedVsActualPercent}%", Modifier.weight(1f), Color(0xFF536DFE))
                StatPill("Rate", "${completionRate(uiState)}%", Modifier.weight(1f), Color(0xFF1F8A70))
            }
        }
        item {
            PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Time by category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    DonutChart(uiState.analytics.categoryBreakdown)
                    uiState.analytics.categoryBreakdown.take(4).forEach {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text(it.category.name)
                            Text(formatMinutes(it.minutes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item {
            PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Weekly score", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    WeeklyBars(uiState.analytics.weeklyScores)
                }
            }
        }
        item {
            PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Most productive hours", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (uiState.analytics.mostProductiveHours.isEmpty()) {
                        Text("Track a few sessions and your best focus windows will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        uiState.analytics.mostProductiveHours.forEach { hour ->
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("${hour.hour}:00")
                                Text(formatMinutes(hour.productiveMinutes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item {
            PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Insights", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    uiState.analytics.insights.forEach { insight ->
                        Text("${insight.title}: ${insight.message}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(data: List<CategoryBreakdown>) {
    val fallback = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    Canvas(Modifier.fillMaxWidth().height(180.dp)) {
        val total = data.sumOf { it.minutes }.coerceAtLeast(1)
        val side = size.height.coerceAtMost(size.width)
        val topLeft = Offset((size.width - side) / 2f, 0f)
        var start = -90f
        if (data.isEmpty()) {
            drawArc(fallback, 0f, 360f, false, topLeft = topLeft, size = Size(side, side), style = Stroke(28.dp.toPx()))
        } else {
            data.forEach { item ->
                val sweep = item.minutes * 360f / total
                drawArc(Color(item.category.color), start, sweep, false, topLeft = topLeft, size = Size(side, side), style = Stroke(28.dp.toPx()))
                start += sweep
            }
        }
    }
}

@Composable
private fun WeeklyBars(scores: List<Int>) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val gap = 10.dp.toPx()
        val barWidth = (size.width - gap * 6) / 7f
        val values = if (scores.size == 7) scores else List(7) { 0 }
        values.forEachIndexed { index, score ->
            val left = index * (barWidth + gap)
            drawRoundRect(track, topLeft = Offset(left, 0f), size = Size(barWidth, size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()))
            val h = (score.coerceIn(0, 100) / 100f) * size.height
            drawRoundRect(primary, topLeft = Offset(left, size.height - h), size = Size(barWidth, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx()))
        }
    }
}

private fun completionRate(uiState: TimeManagementUiState): Int {
    val total = uiState.analytics.stats.totalTasks.coerceAtLeast(1)
    return uiState.analytics.stats.completedTasks * 100 / total
}

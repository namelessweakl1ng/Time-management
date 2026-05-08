package com.yourapp.timemanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourapp.timemanagement.core.TimeManagementUiState
import com.yourapp.timemanagement.ui.common.PremiumCard
import com.yourapp.timemanagement.ui.common.SectionHeader

@Composable
fun WidgetConfigurationScreen(
    contentPadding: PaddingValues,
    uiState: TimeManagementUiState,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(contentPadding),
        contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                    Text("Back")
                }
            }
        }
        item { SectionHeader("Widget setup", "Long-press the launcher, add a Time Management widget, then pick the layout you want.") }
        item { WidgetInfoCard(uiState, "Today widget", "Upcoming blocks, statuses, and next start time.", Icons.Rounded.TaskAlt) }
        item { WidgetInfoCard(uiState, "Focus widget", "Current focus state with start and pause actions.", Icons.Rounded.PlayCircle) }
        item { WidgetInfoCard(uiState, "Daily progress widget", "Completed tasks and productivity score at a glance.", Icons.Rounded.TrendingUp) }
        item { WidgetInfoCard(uiState, "Quick add widget", "Instantly captures a default 25 minute task.", Icons.Rounded.Bolt) }
        item { WidgetInfoCard(uiState, "Motivation widget", "Shows the best current insight or streak nudge.", Icons.Rounded.Insights) }
    }
}

@Composable
private fun WidgetInfoCard(uiState: TimeManagementUiState, title: String, body: String, icon: ImageVector) {
    PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

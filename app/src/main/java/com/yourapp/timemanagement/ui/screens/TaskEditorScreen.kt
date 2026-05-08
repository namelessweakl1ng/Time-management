package com.yourapp.timemanagement.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yourapp.timemanagement.core.TaskDraft
import com.yourapp.timemanagement.core.TimeManagementUiState
import com.yourapp.timemanagement.domain.RecurrenceRule
import com.yourapp.timemanagement.domain.TaskPriority
import com.yourapp.timemanagement.ui.common.PremiumCard
import com.yourapp.timemanagement.ui.common.SectionHeader
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditorScreen(
    contentPadding: PaddingValues,
    uiState: TimeManagementUiState,
    initialDraft: TaskDraft,
    onSave: (TaskDraft) -> Unit,
    onCancel: () -> Unit,
) {
    var draft by remember(initialDraft.id) { mutableStateOf(initialDraft) }
    var timeText by remember(initialDraft.id) {
        mutableStateOf("${initialDraft.startTime.hour.toString().padStart(2, '0')}:${initialDraft.startTime.minute.toString().padStart(2, '0')}")
    }
    var estimateText by remember(initialDraft.id) { mutableStateOf(initialDraft.estimateMinutes.toString()) }
    var reminderEnabled by remember(initialDraft.id) { mutableStateOf(initialDraft.reminderMinutesBefore != null) }

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            SectionHeader(if (draft.id == 0L) "New task" else "Edit task", "Make the block realistic and easy to start.", Modifier.weight(1f))
            OutlinedButton(onClick = onCancel) {
                Icon(Icons.Rounded.Close, contentDescription = null)
                Text("Cancel")
            }
        }
        PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    label = { Text("Task title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = draft.notes,
                    onValueChange = { draft = draft.copy(notes = it) },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = timeText,
                        onValueChange = { timeText = it },
                        label = { Text("Start HH:MM") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = estimateText,
                        onValueChange = { estimateText = it.filter(Char::isDigit).take(3) },
                        label = { Text("Minutes") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
                Text("Date", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateOption("Today", LocalDate.now(), draft.date) { draft = draft.copy(date = it) }
                    DateOption("Tomorrow", LocalDate.now().plusDays(1), draft.date) { draft = draft.copy(date = it) }
                    DateOption("+2 days", LocalDate.now().plusDays(2), draft.date) { draft = draft.copy(date = it) }
                }
                Text("Priority", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TaskPriority.values().forEach { priority ->
                        FilterChip(
                            selected = draft.priority == priority,
                            onClick = { draft = draft.copy(priority = priority) },
                            label = { Text(priority.name) },
                        )
                    }
                }
                Text("Category", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.categories.forEach { category ->
                        FilterChip(
                            selected = draft.categoryId == category.id,
                            onClick = { draft = draft.copy(categoryId = category.id) },
                            label = { Text(category.name) },
                        )
                    }
                }
                OutlinedTextField(
                    value = draft.tag,
                    onValueChange = { draft = draft.copy(tag = it.take(20)) },
                    label = { Text("Tag") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text("Repeat", style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RecurrenceRule.values().forEach { rule ->
                        FilterChip(
                            selected = draft.recurrence == rule,
                            onClick = { draft = draft.copy(recurrence = rule) },
                            label = { Text(rule.name) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Reminder", style = MaterialTheme.typography.titleMedium)
                        Text("Notify 10 minutes before start", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = reminderEnabled, onCheckedChange = { reminderEnabled = it })
                }
            }
        }
        Button(
            onClick = {
                val parsedTime = parseTime(timeText) ?: draft.startTime
                onSave(
                    draft.copy(
                        startTime = parsedTime,
                        estimateMinutes = estimateText.toIntOrNull()?.coerceIn(5, 480) ?: 45,
                        reminderMinutesBefore = if (reminderEnabled) 10 else null,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null)
            Spacer(Modifier.padding(4.dp))
            Text("Save task")
        }
    }
}

@Composable
private fun DateOption(label: String, date: LocalDate, selected: LocalDate, onSelected: (LocalDate) -> Unit) {
    FilterChip(
        selected = date == selected,
        onClick = { onSelected(date) },
        label = { Text(label) },
    )
}

private fun parseTime(raw: String): LocalTime? {
    val parts = raw.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return if (hour in 0..23 && minute in 0..59) LocalTime.of(hour, minute) else null
}

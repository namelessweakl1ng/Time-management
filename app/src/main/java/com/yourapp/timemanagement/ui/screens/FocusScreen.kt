package com.yourapp.timemanagement.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourapp.timemanagement.core.TimeManagementUiState
import com.yourapp.timemanagement.domain.FocusPreset
import com.yourapp.timemanagement.ui.common.PremiumCard
import com.yourapp.timemanagement.ui.common.SectionHeader
import com.yourapp.timemanagement.ui.common.StatPill
import com.yourapp.timemanagement.ui.common.formatMinutes
import kotlinx.coroutines.delay

@Composable
fun FocusScreen(
    contentPadding: PaddingValues,
    uiState: TimeManagementUiState,
    onStartFocus: () -> Unit,
    onPauseFocus: () -> Unit,
    onCompleteFocus: () -> Unit,
    onInterruption: (String, Int) -> Unit,
    onBreak: (Int) -> Unit,
    onSavePreset: (FocusPreset) -> Unit,
    onDeletePreset: (Long) -> Unit,
    onBlockCalendar: () -> Unit,
) {
    val active = uiState.activeSession
    var elapsed by remember(active?.id) { mutableIntStateOf(active?.durationMinutes ?: 0) }
    var selectedPresetId by remember(uiState.settings.focusPresets) { mutableStateOf(uiState.settings.focusPresets.firstOrNull()?.id ?: 0L) }
    var presetName by remember { mutableStateOf("") }
    var presetFocus by remember { mutableStateOf("25") }
    var presetBreak by remember { mutableStateOf("5") }
    LaunchedEffect(active?.id, active?.startedAtMillis) {
        while (active != null) {
            elapsed = ((System.currentTimeMillis() - active.startedAtMillis) / 60_000L).coerceAtLeast(0).toInt()
            delay(1_000)
        }
    }

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SectionHeader("Focus timer", "Timestamp based tracking keeps sessions accurate.")
        PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                FocusOrb(
                    active = active != null,
                    elapsed = elapsed,
                    targetMinutes = uiState.settings.focusPresets.firstOrNull { it.id == selectedPresetId }?.focusMinutes ?: 25,
                )
                Text(
                    uiState.currentTask?.title ?: "Choose a task from the dashboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    if (active == null) "Ready when you are." else "Focused for ${formatMinutes(elapsed)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    if (active == null) {
                        Button(onClick = onStartFocus, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Text("Start")
                        }
                    } else {
                        FilledTonalButton(onClick = onPauseFocus, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Pause, contentDescription = null)
                            Text("Pause")
                        }
                        Button(onClick = onCompleteFocus, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Flag, contentDescription = null)
                            Text("Done")
                        }
                    }
                }
                OutlinedButton(onClick = onBlockCalendar, enabled = uiState.hasCalendarPermission && uiState.currentTask != null) {
                    Icon(Icons.Rounded.Event, contentDescription = null)
                    Text("Block on calendar")
                }
            }
        }
        AnimatedVisibility(active != null) {
            PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Session controls", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = { onInterruption("Context switch", 5) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Bolt, contentDescription = null)
                            Text("Interrupt")
                        }
                        OutlinedButton(onClick = { onBreak(10) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Coffee, contentDescription = null)
                            Text("Break")
                        }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatPill("Focus", formatMinutes(uiState.stats.actualProductiveMinutes), Modifier.weight(1f), MaterialTheme.colorScheme.primary)
            StatPill("Distracted", formatMinutes(uiState.stats.distractedMinutes), Modifier.weight(1f), Color(0xFFC2410C))
            StatPill("Break", formatMinutes(uiState.stats.breakMinutes), Modifier.weight(1f), Color(0xFFE07A5F))
        }
        PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Focus presets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    uiState.settings.focusPresets.forEach { preset ->
                        FilterChip(
                            selected = selectedPresetId == preset.id,
                            onClick = { selectedPresetId = preset.id },
                            label = { Text("${preset.name} ${preset.focusMinutes}/${preset.breakMinutes}") },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = presetName, onValueChange = { presetName = it.take(20) }, label = { Text("Name") }, modifier = Modifier.weight(1.2f), singleLine = true)
                    OutlinedTextField(value = presetFocus, onValueChange = { presetFocus = it.filter(Char::isDigit).take(3) }, label = { Text("Focus") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = presetBreak, onValueChange = { presetBreak = it.filter(Char::isDigit).take(2) }, label = { Text("Break") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            onSavePreset(
                                FocusPreset(
                                    id = 0,
                                    name = presetName,
                                    focusMinutes = presetFocus.toIntOrNull() ?: 25,
                                    breakMinutes = presetBreak.toIntOrNull() ?: 5,
                                ),
                            )
                            presetName = ""
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("Save preset") }
                    OutlinedButton(
                        onClick = { onDeletePreset(selectedPresetId) },
                        enabled = uiState.settings.focusPresets.size > 1,
                        modifier = Modifier.weight(1f),
                    ) { Text("Delete selected") }
                }
            }
        }
    }
}

@Composable
private fun FocusOrb(active: Boolean, elapsed: Int, targetMinutes: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val progress by animateFloatAsState(
        targetValue = if (targetMinutes <= 0) 0f else (elapsed.toFloat() / targetMinutes).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "focus-progress",
    )
    Canvas(
        Modifier
            .size(210.dp)
            .semantics {
                contentDescription = if (active) {
                    "Focus timer active, ${elapsed} minutes elapsed out of ${targetMinutes}"
                } else {
                    "Focus timer idle"
                }
            },
    ) {
        val stroke = Stroke(13.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(color = primary.copy(alpha = 0.10f))
        drawCircle(color = primary.copy(alpha = 0.18f), radius = size.minDimension * 0.34f)
        drawArc(
            color = if (active) primary else primary.copy(alpha = 0.35f),
            startAngle = -90f,
            sweepAngle = progress * 360f,
            useCenter = false,
            topLeft = Offset(14.dp.toPx(), 14.dp.toPx()),
            size = androidx.compose.ui.geometry.Size(size.width - 28.dp.toPx(), size.height - 28.dp.toPx()),
            style = stroke,
        )
    }
}

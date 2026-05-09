package com.yourapp.timemanagement.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourapp.timemanagement.core.TimeManagementUiState
import com.yourapp.timemanagement.domain.CardStyle
import com.yourapp.timemanagement.domain.LayoutDensity
import com.yourapp.timemanagement.domain.ScoringStyle
import com.yourapp.timemanagement.domain.ThemeMode
import com.yourapp.timemanagement.ui.common.PremiumCard
import com.yourapp.timemanagement.ui.common.SectionHeader

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    uiState: TimeManagementUiState,
    onThemeMode: (ThemeMode) -> Unit,
    onAccentColor: (Long) -> Unit,
    onCardStyle: (CardStyle) -> Unit,
    onDensity: (LayoutDensity) -> Unit,
    onScoringStyle: (ScoringStyle) -> Unit,
    onToggleModule: (String) -> Unit,
    onAddCategory: (String, Long) -> Unit,
    onNotificationTone: (String) -> Unit,
    onWidgetConfig: () -> Unit,
) {
    var categoryName by remember { mutableStateOf("") }
    val accents = listOf(0xFF1F8A70, 0xFF536DFE, 0xFFE07A5F, 0xFF7B61FF, 0xFF0F766E, 0xFFBE123C)

    LazyColumn(
        modifier = Modifier,
        contentPadding = PaddingValues(
            start = 20.dp,
            top = contentPadding.calculateTopPadding() + 18.dp,
            end = 20.dp,
            bottom = contentPadding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SectionHeader("Customize", "Shape the cockpit around your work style.") }
        item {
            PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            FilterChip(selected = uiState.settings.themeMode == mode, onClick = { onThemeMode(mode) }, label = { Text(mode.name) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        accents.forEach { accent ->
                            Surface(
                                onClick = { onAccentColor(accent) },
                                shape = CircleShape,
                                color = Color(accent),
                                modifier = Modifier.size(38.dp),
                                border = BorderStroke(
                                    2.dp,
                                    if (uiState.settings.accentColor == accent) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                ),
                            ) {}
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CardStyle.entries.forEach { style ->
                                FilterChip(selected = uiState.settings.cardStyle == style, onClick = { onCardStyle(style) }, label = { Text("${style.name} cards") })
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LayoutDensity.entries.forEach { density ->
                                FilterChip(selected = uiState.settings.density == density, onClick = { onDensity(density) }, label = { Text(density.name) })
                            }
                        }
                    }
                }
            }
        }
        item {
            PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Dashboard modules", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    uiState.settings.dashboardModules.forEach { module ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text(module.label, style = MaterialTheme.typography.titleMedium)
                                Text(module.key, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = module.visible, onCheckedChange = { onToggleModule(module.key) })
                        }
                    }
                }
            }
        }
        item {
            PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Productivity scoring", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ScoringStyle.entries.forEach { style ->
                            FilterChip(selected = uiState.settings.scoringStyle == style, onClick = { onScoringStyle(style) }, label = { Text(style.name) })
                        }
                    }
                    Text("Focus presets: ${uiState.settings.focusPresetMinutes.joinToString(", ") { "${it}m" }}")
                    OutlinedTextField(
                        value = uiState.settings.notificationTone,
                        onValueChange = onNotificationTone,
                        label = { Text("Notification tone label") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        }
        item {
            PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Categories and labels", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(uiState.categories.joinToString("  |  ") { it.name }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = categoryName,
                            onValueChange = { categoryName = it.take(18) },
                            label = { Text("New category") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Button(onClick = {
                            onAddCategory(categoryName, accents.random())
                            categoryName = ""
                        }) {
                            Text("Add")
                        }
                    }
                }
            }
        }
        item {
            PremiumCard(settings = uiState.settings, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Widgets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Today, focus, progress, quick add, and motivation widgets share the same local data source.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onWidgetConfig, modifier = Modifier.fillMaxWidth()) { Text("Configure widgets") }
                }
            }
        }
    }
}

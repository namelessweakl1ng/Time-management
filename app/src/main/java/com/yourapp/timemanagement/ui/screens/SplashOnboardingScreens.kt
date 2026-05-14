package com.yourapp.timemanagement.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourapp.timemanagement.core.TimeManagementUiState
import com.yourapp.timemanagement.domain.ThemeMode
import com.yourapp.timemanagement.domain.UserSettings
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun SplashScreen(uiState: TimeManagementUiState) {
    val transition = rememberInfiniteTransition(label = "splash")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "sweep",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(120.dp)) {
                    drawCircle(color = Color.White.copy(alpha = 0.18f))
                    drawArc(
                        color = Color(0xFF1F8A70),
                        startAngle = sweep,
                        sweepAngle = 110f,
                        useCenter = false,
                        style = Stroke(width = 10.dp.toPx()),
                    )
                }
                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(54.dp))
            }
            Text("Time Management", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                if (uiState.isLoading) "Preparing your calm productivity cockpit" else "Ready",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    settings: UserSettings,
    onFinish: () -> Unit,
    onAccentSelected: (Long) -> Unit,
) {
    val accents = listOf(0xFF1F8A70, 0xFF536DFE, 0xFFE07A5F, 0xFF7B61FF)
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()
    val goals = listOf("Finish planned tasks", "Protect deep work", "Reduce distractions")
    var selectedGoal by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(goals.first()) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            Column(verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)) {
                    Icon(
                        when (page) {
                            1 -> Icons.Rounded.CheckCircle
                            2 -> Icons.Rounded.DarkMode
                            3 -> Icons.Rounded.EmojiEvents
                            else -> Icons.Rounded.AutoAwesome
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(18.dp).size(34.dp),
                    )
                }
                when (page) {
                    0 -> {
                        Text("Design a day you can actually finish", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text("Plan blocks, track real focus, spot drift, and keep the dashboard tuned to how you work.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                            OnboardingFeature("Live focus", "Timers and pauses", Icons.Rounded.Timer, Modifier.weight(1f))
                            OnboardingFeature("Smart rhythm", "Private insights", Icons.Rounded.AutoAwesome, Modifier.weight(1f))
                        }
                    }
                    1 -> {
                        Text("Pick your main goal", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text("We will tune nudges, achievements, and the dashboard around this direction.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        goals.forEach { goal ->
                            FilterChip(selected = selectedGoal == goal, onClick = { selectedGoal = goal }, label = { Text(goal) })
                        }
                    }
                    2 -> {
                        Text("Make it feel like yours", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text("Choose an accent now. You can switch to the new AMOLED black theme from Settings anytime.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            accents.forEach { color ->
                                Surface(
                                    onClick = { onAccentSelected(color) },
                                    shape = CircleShape,
                                    color = Color(color),
                                    modifier = Modifier.size(if (settings.accentColor == color) 46.dp else 38.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        2.dp,
                                        if (settings.accentColor == color) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    ),
                                ) {}
                            }
                        }
                    }
                    else -> {
                        Text("Ready to build momentum", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                        Text("You will earn XP, keep streaks, unlock achievements, and get private on-device timing insights.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OnboardingFeature("Selected goal", selectedGoal, Icons.Rounded.EmojiEvents, Modifier.fillMaxWidth())
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) } },
                enabled = pagerState.currentPage > 0,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(18.dp),
            ) { Text("Back") }
            Button(
                onClick = {
                    if (pagerState.currentPage == pagerState.pageCount - 1) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                Text(if (pagerState.currentPage == pagerState.pageCount - 1) "Start planning" else "Next")
            }
        }
    }
}

@Composable
private fun OnboardingFeature(
    title: String,
    body: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Start)
        }
    }
}

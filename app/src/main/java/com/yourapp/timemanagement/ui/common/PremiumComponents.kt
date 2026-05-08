package com.yourapp.timemanagement.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yourapp.timemanagement.domain.CardStyle
import com.yourapp.timemanagement.domain.LayoutDensity
import com.yourapp.timemanagement.domain.TaskPriority
import com.yourapp.timemanagement.domain.TaskStatus
import com.yourapp.timemanagement.domain.UserSettings

@Composable
fun premiumShape(settings: UserSettings) =
    if (settings.cardStyle == CardStyle.Sharp) RoundedCornerShape(8.dp) else RoundedCornerShape(24.dp)

@Composable
fun PremiumCard(
    settings: UserSettings,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = premiumShape(settings)
    Card(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f), shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
                        ),
                    ),
                )
                .then(
                    if (settings.density == LayoutDensity.Compact) {
                        Modifier
                    } else {
                        Modifier
                    },
                ),
            content = content,
        )
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (!subtitle.isNullOrBlank()) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.height(58.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 12.dp,
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), label = "progress-ring")
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(132.dp)) {
            val stroke = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2
            val size = Size(size.width - strokeWidth.toPx(), size.height - strokeWidth.toPx())
            drawArc(track, -90f, 360f, false, topLeft = Offset(inset, inset), size = size, style = stroke)
            drawArc(primary, -90f, animated * 360f, false, topLeft = Offset(inset, inset), size = size, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatusChip(status: TaskStatus) {
    val color = when (status) {
        TaskStatus.Completed -> Color(0xFF1F8A70)
        TaskStatus.InProgress -> MaterialTheme.colorScheme.primary
        TaskStatus.Overdue -> Color(0xFFC2410C)
        TaskStatus.Skipped -> Color(0xFF64748B)
        TaskStatus.Planned -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(999.dp), color = color.copy(alpha = 0.12f), contentColor = color) {
        Row(
            modifier = Modifier.size(width = 108.dp, height = 32.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (status == TaskStatus.Completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(5.dp))
            Text(status.name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

fun priorityColor(priority: TaskPriority): Color = when (priority) {
    TaskPriority.Low -> Color(0xFF64748B)
    TaskPriority.Medium -> Color(0xFF536DFE)
    TaskPriority.High -> Color(0xFFE07A5F)
    TaskPriority.Critical -> Color(0xFFBE123C)
}

fun formatMinutes(minutes: Int): String {
    val safe = minutes.coerceAtLeast(0)
    val hours = safe / 60
    val mins = safe % 60
    return if (hours == 0) "${mins}m" else "${hours}h ${mins}m"
}

fun timeLabel(hour: Int, minute: Int): String {
    val suffix = if (hour >= 12) "PM" else "AM"
    val h = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return "$h:${minute.toString().padStart(2, '0')} $suffix"
}

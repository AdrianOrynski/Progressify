package com.example.progressify.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progressify.*
import com.example.progressify.ui.theme.*
import com.example.progressify.viewmodel.TaskViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import kotlin.math.max

@Composable
fun StatisticsScreen(taskViewModel: TaskViewModel, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkWood, ShadowBlack.copy(alpha = 0.6f), DarkWood)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = FantasyGold
                )
            }
            Text(
                "📊 CHRONICLES",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    color = FantasyGold
                )
            )
        }

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            XpPerWeekChart(taskViewModel)
            CategoryBreakdownChart(taskViewModel)
            RecordsCard(taskViewModel)
            OverallStatsCard(taskViewModel)
        }
    }
}

// ── XP per week (bar chart) ──────────────────────────────────────────────────

@Composable
private fun XpPerWeekChart(taskViewModel: TaskViewModel) {
    val weeklyXp = remember(taskViewModel.tasks) {
        buildWeeklyXpMap(taskViewModel.tasks)
    }

    val animProgress by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label         = "xpBars"
    )

    StatCard(title = "XP GAINED PER WEEK") {
        if (weeklyXp.isEmpty()) {
            Text("Complete tasks to see your weekly XP history.",
                style = MaterialTheme.typography.bodyMedium, color = ParchmentDim)
        } else {
            val weeks   = weeklyXp.keys.sorted().takeLast(8)
            val maxXp   = weeklyXp.values.maxOrNull()?.toFloat() ?: 1f
            val barColor = FantasyGold

            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val chartWidth  = size.width
                val chartHeight = size.height - 32.dp.toPx()
                val barGap      = 8.dp.toPx()
                val barWidth    = (chartWidth - barGap * (weeks.size + 1)) / weeks.size

                weeks.forEachIndexed { i, week ->
                    val xp       = (weeklyXp[week] ?: 0).toFloat()
                    val barH     = (xp / maxXp) * chartHeight * animProgress
                    val left     = barGap + i * (barWidth + barGap)
                    val top      = chartHeight - barH

                    drawRoundRect(
                        color        = barColor.copy(alpha = 0.25f),
                        topLeft      = Offset(left, 0f),
                        size         = Size(barWidth, chartHeight),
                        cornerRadius = CornerRadius(4.dp.toPx())
                    )
                    if (barH > 0f) {
                        drawRoundRect(
                            brush        = Brush.verticalGradient(
                                colors   = listOf(barColor, barColor.copy(alpha = 0.6f)),
                                startY   = top,
                                endY     = chartHeight
                            ),
                            topLeft      = Offset(left, top),
                            size         = Size(barWidth, barH),
                            cornerRadius = CornerRadius(4.dp.toPx())
                        )
                    }

                    val label = week.substringAfter("W")
                    drawContext.canvas.nativeCanvas.drawText(
                        "W$label",
                        left + barWidth / 2f,
                        size.height,
                        android.graphics.Paint().apply {
                            color     = ParchmentDim.copy(alpha = 0.6f).toArgb()
                            textSize  = 22f
                            textAlign = android.graphics.Paint.Align.CENTER
        }
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val total = weeklyXp.values.sum()
                val avg   = if (weeklyXp.isNotEmpty()) total / weeklyXp.size else 0
                MiniStat("Total XP", "$total")
                MiniStat("Avg/Week", "$avg")
                MiniStat("Best Week", "${weeklyXp.values.maxOrNull() ?: 0}")
            }
        }
    }
}

private fun buildWeeklyXpMap(tasks: List<Task>): Map<String, Int> {
    val weekFields = WeekFields.of(Locale.getDefault())
    val result = mutableMapOf<String, Int>()
    tasks.filter { it.isCompleted && it.completedAt != null }.forEach { task ->
        val date = task.completedAt!!.toDate().toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val year = date.get(weekFields.weekBasedYear())
        val week = date.get(weekFields.weekOfWeekBasedYear())
        val key  = "${year}W${week.toString().padStart(2, '0')}"
        result[key] = (result[key] ?: 0) + task.xpAwarded
    }
    return result
}

// ── Category breakdown (horizontal bars) ────────────────────────────────────

@Composable
private fun CategoryBreakdownChart(taskViewModel: TaskViewModel) {
    val stats = taskViewModel.categoryStats
    val totalXp = stats.values.sumOf { it.exp }.toFloat().let { if (it == 0f) 1f else it }

    StatCard(title = "XP BY DISCIPLINE") {
        if (stats.isEmpty() || stats.values.all { it.exp == 0 }) {
            Text("Complete tasks to see your discipline breakdown.",
                style = MaterialTheme.typography.bodyMedium, color = ParchmentDim)
        } else {
            TaskCategory.entries.forEach { cat ->
                val exp      = stats[cat]?.exp ?: 0
                val fraction = exp / totalXp
                val color    = categoryBarColor(cat)

                val animatedFraction by animateFloatAsState(
                    targetValue   = fraction,
                    animationSpec = tween(800, easing = FastOutSlowInEasing),
                    label         = "bar_${cat.name}"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        skillIcon(cat),
                        fontSize = 14.sp,
                        modifier = Modifier.width(24.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        cat.label,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = ParchmentDim,
                        modifier = Modifier.width(110.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(10.dp)
                            .background(color.copy(alpha = 0.15f), RoundedCornerShape(5.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFraction)
                                .background(
                                    Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.6f))),
                                    RoundedCornerShape(5.dp)
                                )
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$exp XP",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = color,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(52.dp)
                    )
                }
            }
        }
    }
}

// ── Records card ─────────────────────────────────────────────────────────────

@Composable
private fun RecordsCard(taskViewModel: TaskViewModel) {
    StatCard(title = "⚡ PERSONAL RECORDS") {
        val durationH = taskViewModel.longestQuestMinutes / 60
        val durationM = taskViewModel.longestQuestMinutes % 60
        val durationStr = when {
            durationH > 0 -> "${durationH}h ${durationM}m"
            durationM > 0 -> "${durationM}m"
            else          -> "—"
        }
        RecordRow("Max XP in one task",   "${taskViewModel.maxXpGained} XP",   FantasyGold)
        RecordRow("Longest quest",         durationStr,                          DragonRedLight)
        RecordRow("Best streak",           "${taskViewModel.longestStreak} days", Color(0xFFFF6D00))
        RecordRow("Tasks completed",       "${taskViewModel.completedTasksCount}", Color(0xFF4CAF50))
    }
}

@Composable
private fun RecordRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = ParchmentDim)
        Box(
            modifier = Modifier
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.labelLarge,
                color = color, fontWeight = FontWeight.ExtraBold)
        }
    }
}

// ── Overall stats card ───────────────────────────────────────────────────────

@Composable
private fun OverallStatsCard(taskViewModel: TaskViewModel) {
    StatCard(title = "OVERVIEW") {
        val activeTasks    = taskViewModel.tasks.count { !it.isCompleted }
        val overdueTasks   = taskViewModel.tasks.count { it.isOverdue }
        val topCategory    = taskViewModel.categoryStats.entries
            .maxByOrNull { it.value.exp }?.key

        StatRow("Current level",     "${taskViewModel.currentLevel}")
        StatRow("Current XP",        "${taskViewModel.currentXp} / ${taskViewModel.xpToNextLevel}")
        StatRow("Active tasks",      "$activeTasks")
        StatRow("Overdue tasks",     "$overdueTasks")
        StatRow("Current streak",    "${taskViewModel.currentStreak} days")
        if (topCategory != null)
            StatRow("Top discipline", topCategory.label)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = ParchmentDim)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Parchment,
            fontWeight = FontWeight.SemiBold)
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun StatCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape     = RoundedCornerShape(8.dp),
        border    = androidx.compose.foundation.BorderStroke(1.dp, FantasyGoldDim.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(AncientBrownLight, AncientBrown)))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    title,
                    style    = MaterialTheme.typography.labelLarge,
                    color    = FantasyGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                content()
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge,
            color = FantasyGold, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = ParchmentDim)
    }
}

private fun categoryBarColor(category: TaskCategory): Color = when (category) {
    TaskCategory.SPELLCRAFT       -> Color(0xFF9C27B0)
    TaskCategory.TASKFORGE        -> Color(0xFFFF6D00)
    TaskCategory.BARDS_DELIGHT    -> Color(0xFF03A9F4)
    TaskCategory.SCHOLARS_SANCTUM -> Color(0xFF4CAF50)
    TaskCategory.CYCLE_OF_ORDER   -> Color(0xFF9E9E9E)
    TaskCategory.BODYFORGE        -> Color(0xFFF44336)
}


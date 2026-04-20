package com.example.progressify.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.progressify.*
import com.example.progressify.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskCard(
    task      : Task,
    onComplete: () -> Unit,
    onDelete  : () -> Unit,
    showDelete: Boolean,
    modifier  : Modifier = Modifier
) {
    val isOverdue = task.isOverdue
    val bgColor by animateColorAsState(
        targetValue = when {
            task.isCompleted -> Color(0xFF1A1410)
            isOverdue        -> Color(0xFF2A0E0E)
            else             -> AncientBrown
        }, animationSpec = tween(300), label = "bg")
    val borderColor by animateColorAsState(
        targetValue = when {
            task.isCompleted -> IronGray
            isOverdue        -> DragonRedLight
            else             -> FantasyGold
        }, animationSpec = tween(300), label = "border")

    Card(modifier  = modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
        border     = BorderStroke(1.dp, borderColor),
        colors     = CardDefaults.cardColors(containerColor = bgColor),
        elevation  = CardDefaults.cardElevation(if (task.isCompleted) 1.dp else 4.dp)) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically) {

            IconButton(onClick = onComplete, enabled = !task.isCompleted,
                modifier = Modifier.size(36.dp)) {
                AnimatedContent(targetState = task.isCompleted,
                    transitionSpec = { scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut() },
                    label = "check") { completed ->
                    Icon(if (completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null,
                        tint     = if (completed) Color.Gray else if (isOverdue) DragonRedLight else FantasyGold,
                        modifier = Modifier.size(24.dp))
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(task.title,
                    style          = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color          = if (task.isCompleted) Color.Gray else Parchment,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)

                if (task.description.isNotBlank())
                    Text(task.description,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = if (task.isCompleted) Color.Gray else ParchmentDim,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)

                if (task.startTime != null && task.endTime != null) {
                    val fmt           = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                    val deadlineColor = when {
                        task.isCompleted -> Color.Gray
                        isOverdue        -> DragonRedLight
                        else             -> ParchmentDim
                    }
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Schedule, null, tint = deadlineColor, modifier = Modifier.size(12.dp))
                        Text(
                            text = if (isOverdue)
                                "OVERDUE — ${fmt.format(task.startTime.toDate())} – ${fmt.format(task.endTime.toDate())}"
                            else
                                "${fmt.format(task.startTime.toDate())} – ${fmt.format(task.endTime.toDate())}",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = deadlineColor,
                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)) {
                    if (task.category.isNotBlank()) CategoryChip(task.category, task.isCompleted)
                    DifficultyChip(task.difficulty, task.isCompleted)
                    if (task.isRecurring) RecurringChip(task.isCompleted)
                    if (task.isCompleted && task.xpAwarded > 0)
                        Text("+${task.xpAwarded} XP",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }

            if (showDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, null,
                        tint     = DeepDragonRed.copy(alpha = if (task.isCompleted) 0.4f else 1f),
                        modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
internal fun CategoryChip(category: String, completed: Boolean) {
    Box(modifier = Modifier
        .background(
            if (completed) IronGray.copy(alpha = 0.3f) else DeepDragonRed.copy(alpha = 0.3f),
            RoundedCornerShape(50.dp))
        .padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(category, style = MaterialTheme.typography.labelSmall,
            color = if (completed) Color.Gray else DragonRedLight)
    }
}

@Composable
internal fun DifficultyChip(difficulty: String, completed: Boolean) {
    val color = if (completed) Color.Gray else difficultyColor(difficulty)
    val label = try { Difficulty.valueOf(difficulty).label } catch (e: Exception) { difficulty }
    Box(modifier = Modifier
        .background(color.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
        .padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun RecurringChip(completed: Boolean) {
    Box(modifier = Modifier
        .background(
            FantasyGold.copy(alpha = if (completed) 0.1f else 0.15f),
            RoundedCornerShape(50.dp))
        .padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text("↻ recurring", style = MaterialTheme.typography.labelSmall,
            color = if (completed) Color.Gray else FantasyGoldDim)
    }
}

internal fun difficultyColor(difficulty: String): Color = when (difficulty) {
    Difficulty.EASY.name -> Color(0xFF4CAF50)
    Difficulty.HARD.name -> DragonRedLight
    else                 -> FantasyGold
}

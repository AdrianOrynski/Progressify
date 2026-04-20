package com.example.progressify.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progressify.*
import com.example.progressify.ui.theme.*
import com.example.progressify.viewmodel.SkillStat
import com.example.progressify.viewmodel.TaskViewModel

@Composable
fun SkillsScreen(taskViewModel: TaskViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkWood, ShadowBlack.copy(alpha = 0.6f), DarkWood)))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("⚗️ SKILL GRIMOIRE",
            style    = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = FantasyGold),
            modifier = Modifier.padding(bottom = 6.dp, top = 16.dp))
        Text("Hone your disciplines to unleash greater power",
            style    = MaterialTheme.typography.bodyMedium, color = ParchmentDim,
            modifier = Modifier.padding(bottom = 24.dp))

        taskViewModel.skillStats.forEach { stat ->
            SkillCard(stat)
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SkillCard(stat: SkillStat) {
    val color  = skillColor(stat.category)
    val emoji  = skillIcon(stat.category)
    val animatedProgress by animateFloatAsState(
        targetValue   = (stat.currentXp.toFloat() / stat.xpToNextLevel).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label         = "skill_${stat.category.name}"
    )
    FantasyCard(borderColor = color.copy(alpha = 0.6f), borderWidth = 1.dp,
        gradient = listOf(AncientBrownLight, AncientBrown)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 14.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            ) {
                Text(emoji, fontSize = 26.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stat.category.label,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Parchment)
                    Box(
                        modifier = Modifier
                            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("LVL ${stat.level}", style = MaterialTheme.typography.labelLarge,
                            color = color, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(skillSubtitle(stat.category),
                    style = MaterialTheme.typography.bodyMedium, color = ParchmentDim)
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("${stat.completedTasks} tasks completed",
                style = MaterialTheme.typography.labelSmall, color = ParchmentDim)
            Text("${stat.currentXp} / ${stat.xpToNextLevel} XP",
                style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress   = { animatedProgress },
            modifier   = Modifier.fillMaxWidth().height(10.dp),
            color      = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap  = StrokeCap.Round
        )
    }
}

private fun skillColor(category: TaskCategory): Color = when (category) {
    TaskCategory.SPELLCRAFT       -> Color(0xFF9C27B0)
    TaskCategory.TASKFORGE        -> Color(0xFFFF6D00)
    TaskCategory.BARDS_DELIGHT    -> Color(0xFF03A9F4)
    TaskCategory.SCHOLARS_SANCTUM -> Color(0xFF4CAF50)
    TaskCategory.CYCLE_OF_ORDER   -> Color(0xFF9E9E9E)
    TaskCategory.BODYFORGE        -> Color(0xFFF44336)
}

private fun skillIcon(category: TaskCategory): String = when (category) {
    TaskCategory.SPELLCRAFT       -> "✨"
    TaskCategory.TASKFORGE        -> "⚒️"
    TaskCategory.BARDS_DELIGHT    -> "🎵"
    TaskCategory.SCHOLARS_SANCTUM -> "📚"
    TaskCategory.CYCLE_OF_ORDER   -> "⚙️"
    TaskCategory.BODYFORGE        -> "💪"
}

private fun skillSubtitle(category: TaskCategory): String = when (category) {
    TaskCategory.SPELLCRAFT       -> "Innovation & Design"
    TaskCategory.TASKFORGE        -> "Execution & Work"
    TaskCategory.BARDS_DELIGHT    -> "Expression & Leisure"
    TaskCategory.SCHOLARS_SANCTUM -> "Learning & Research"
    TaskCategory.CYCLE_OF_ORDER   -> "Routine & Structure"
    TaskCategory.BODYFORGE        -> "Strength & Vitality"
}

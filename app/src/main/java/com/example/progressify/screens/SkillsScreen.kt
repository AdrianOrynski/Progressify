package com.example.progressify.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
        Text("Tap a discipline to reveal your hero class",
            style    = MaterialTheme.typography.bodyMedium, color = ParchmentDim,
            modifier = Modifier.padding(bottom = 24.dp))

        taskViewModel.skillStats.forEach { stat ->
            SkillCard(stat, activeClasses = taskViewModel.heroClasses)
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SkillCard(stat: SkillStat, activeClasses: List<String>) {
    val color     = skillColor(stat.category)
    val emoji     = skillIcon(stat.category)
    val heroClass = stat.category.toHeroClass()
    val isActive  = heroClass.name in activeClasses

    var expanded by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue   = (stat.currentXp.toFloat() / stat.xpToNextLevel).coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label         = "skill_${stat.category.name}"
    )
    val chevronRotation by animateFloatAsState(
        targetValue   = if (expanded) 180f else 0f,
        animationSpec = tween(250),
        label         = "chevron_${stat.category.name}"
    )

    val borderColor = if (isActive && expanded) color else color.copy(alpha = 0.6f)
    val borderWidth = if (isActive && expanded) 1.5.dp else 1.dp

    Card(
        shape     = RoundedCornerShape(8.dp),
        border    = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier  = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(AncientBrownLight, AncientBrown)))
                .padding(16.dp)
        ) {
            Column {
                // ── Header row ──────────────────────────────────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stat.category.label,
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Parchment)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("LVL ${stat.level}", style = MaterialTheme.typography.labelLarge,
                                        color = color, fontWeight = FontWeight.ExtraBold)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "›",
                                    fontSize = 20.sp,
                                    color = color.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.rotate(chevronRotation)
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(skillSubtitle(stat.category),
                            style = MaterialTheme.typography.bodyMedium, color = ParchmentDim)
                        Spacer(Modifier.height(2.dp))
                        Text("${heroClass.icon} ${heroClass.label.uppercase()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = color)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── XP bar ───────────────────────────────────────────────────
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

                // ── Expandable class panel ───────────────────────────────────
                AnimatedVisibility(
                    visible = expanded,
                    enter   = expandVertically(tween(280)),
                    exit    = shrinkVertically(tween(220))
                ) {
                    ClassPanel(heroClass = heroClass, color = color, isActive = isActive)
                }
            }
        }
    }
}

@Composable
private fun ClassPanel(heroClass: HeroClass, color: Color, isActive: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        HorizontalDivider(color = color.copy(alpha = 0.3f))
        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(color.copy(alpha = if (isActive) 0.2f else 0.1f), RoundedCornerShape(12.dp))
                    .border(1.dp, color.copy(alpha = if (isActive) 0.7f else 0.3f), RoundedCornerShape(12.dp))
            ) {
                Text(heroClass.icon, fontSize = 30.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        heroClass.label.uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = color
                    )
                    if (isActive) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("ACTIVE", style = MaterialTheme.typography.labelSmall,
                                color = color, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Text(
                    classDescription(heroClass),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ParchmentDim
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                "✦ ${classBonus(heroClass)}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = color.copy(alpha = if (isActive) 1f else 0.6f)
            )
        }
    }
}

private fun classDescription(heroClass: HeroClass): String = when (heroClass) {
    HeroClass.ARCHMAGE   -> "Masters of Spellcraft, bending reality through creativity and design."
    HeroClass.MERCENARY  -> "Relentless executors who turn raw tasks into gold. No deadline too close."
    HeroClass.BARD       -> "Free spirits who weave art and leisure into legend."
    HeroClass.LOREKEEPER -> "Scholars who amass knowledge like treasure, growing stronger with every truth."
    HeroClass.PALADIN    -> "Champions of routine and order, forging an unbreakable shield against chaos."
    HeroClass.BARBARIAN  -> "Warriors of the body who forge strength through relentless training."
}

private fun classBonus(heroClass: HeroClass): String = when (heroClass) {
    HeroClass.ARCHMAGE   -> "Spellcraft XP bonus — creative tasks yield greater rewards"
    HeroClass.MERCENARY  -> "Taskforge XP bonus — execution tasks yield greater rewards"
    HeroClass.BARD       -> "Bard's Delight XP bonus — creative leisure yields greater rewards"
    HeroClass.LOREKEEPER -> "Scholars' Sanctum XP bonus — learning yields greater rewards"
    HeroClass.PALADIN    -> "Cycle of Order XP bonus — routine tasks yield greater rewards"
    HeroClass.BARBARIAN  -> "Bodyforge XP bonus — fitness tasks yield greater rewards"
}

internal fun skillColor(category: TaskCategory): Color = when (category) {
    TaskCategory.SPELLCRAFT       -> Color(0xFF9C27B0)
    TaskCategory.TASKFORGE        -> Color(0xFFFF6D00)
    TaskCategory.BARDS_DELIGHT    -> Color(0xFF03A9F4)
    TaskCategory.SCHOLARS_SANCTUM -> Color(0xFF4CAF50)
    TaskCategory.CYCLE_OF_ORDER   -> Color(0xFF9E9E9E)
    TaskCategory.BODYFORGE        -> Color(0xFFF44336)
}

internal fun skillIcon(category: TaskCategory): String = when (category) {
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

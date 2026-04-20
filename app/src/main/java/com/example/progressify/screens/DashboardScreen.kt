package com.example.progressify.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progressify.*
import com.example.progressify.ui.theme.*
import com.example.progressify.viewmodel.TaskViewModel

@Composable
fun DashboardScreen(user: User?, taskViewModel: TaskViewModel) {
    val activeTasks  = taskViewModel.tasks.filter { !it.isCompleted }.take(3)
    val overdueTasks = taskViewModel.tasks.filter { it.isOverdue }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val avatarScale by infiniteTransition.animateFloat(
        initialValue  = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkWood, ShadowBlack.copy(alpha = 0.7f), DarkWood)))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("⚔️ THE TAVERN",
            style    = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = FantasyGold),
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp))

        FantasyCard(borderColor = FantasyGold, borderWidth = 2.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(avatarScale)) {
                    Icon(Icons.Default.AccountCircle, null,
                        modifier = Modifier.size(72.dp), tint = FantasyGold)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(user?.nickname?.uppercase() ?: "ADVENTURER",
                        style = MaterialTheme.typography.titleLarge,
                        color = Parchment, fontWeight = FontWeight.ExtraBold)
                    if (user?.name?.isNotBlank() == true || user?.surname?.isNotBlank() == true)
                        Text("${user.name} ${user.surname}".trim(),
                            style = MaterialTheme.typography.bodyMedium, color = ParchmentDim)
                    Text("LVL ${taskViewModel.currentLevel} HERO",
                        color = FantasyGold, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(20.dp))
            XpProgressBar(currentXp = taskViewModel.currentXp, maxXp = taskViewModel.xpToNextLevel)
        }

        if (overdueTasks.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(8.dp),
                border   = BorderStroke(1.dp, DragonRedLight),
                colors   = CardDefaults.cardColors(containerColor = DeepDragonRed.copy(alpha = 0.15f))
            ) {
                Row(modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, null, tint = DragonRedLight, modifier = Modifier.size(20.dp))
                    Text("${overdueTasks.size} overdue ${if (overdueTasks.size > 1) "bounties" else "bounty"}! Complete them to avoid XP penalty.",
                        style = MaterialTheme.typography.bodyMedium, color = DragonRedLight)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        GoldDivider(label = "HERO STATS")
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Level", "${taskViewModel.currentLevel}", modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Star, null, tint = FantasyGold, modifier = Modifier.size(26.dp))
            }
            StatCard("Done", "${taskViewModel.tasks.count { it.isCompleted }}", modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CheckCircle, null, tint = FantasyGold, modifier = Modifier.size(26.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        GoldDivider(label = "ACTIVE BOUNTIES")
        Spacer(Modifier.height(12.dp))

        if (activeTasks.isEmpty()) {
            FantasyCard(borderColor = FantasyGoldDim.copy(alpha = 0.5f),
                gradient = listOf(AncientBrown.copy(alpha = 0.6f), DarkWood)) {
                Text("No active bounties.\nVisit the Bounty Board to add tasks!",
                    style = MaterialTheme.typography.bodyMedium, color = ParchmentDim,
                    modifier = Modifier.padding(8.dp))
            }
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                items(activeTasks, key = { it.id }) { task ->
                    TaskCard(task = task, onComplete = { taskViewModel.completeTask(task) },
                        onDelete = { taskViewModel.deleteTask(task.id) }, showDelete = false)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, icon: @Composable () -> Unit) {
    FantasyCard(modifier = modifier, borderColor = FantasyGoldDim, borderWidth = 1.dp,
        gradient = listOf(AncientBrownLight, AncientBrown)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            icon()
            Column {
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = ParchmentDim)
                Text(value, style = MaterialTheme.typography.titleLarge,
                    color = FantasyGold, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

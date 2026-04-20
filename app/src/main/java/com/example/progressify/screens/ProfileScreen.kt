package com.example.progressify.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progressify.*
import com.example.progressify.ui.theme.*
import com.example.progressify.viewmodel.TaskViewModel

@Composable
fun ProfileScreen(user: User?, taskViewModel: TaskViewModel) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatarPulse")
    val avatarScale by infiniteTransition.animateFloat(
        initialValue  = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkWood, ShadowBlack.copy(alpha = 0.6f), DarkWood)))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🛡️ HERO SHEET",
            style    = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = FantasyGold),
            modifier = Modifier.align(Alignment.Start).padding(bottom = 24.dp, top = 16.dp))

        Box(contentAlignment = Alignment.Center,
            modifier = Modifier
                .scale(avatarScale)
                .size(120.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(AncientBrownLight, AncientBrown)))) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(80.dp), tint = FantasyGold)
        }

        Spacer(Modifier.height(16.dp))
        Text(user?.nickname?.uppercase() ?: "ADVENTURER",
            style = MaterialTheme.typography.headlineMedium, color = Parchment, fontWeight = FontWeight.ExtraBold)
        if (user?.name?.isNotBlank() == true || user?.surname?.isNotBlank() == true)
            Text("${user.name} ${user.surname}".trim(),
                style = MaterialTheme.typography.bodyMedium, color = ParchmentDim)
        Text("CLASS: LEVEL ${taskViewModel.currentLevel} EXPLORER",
            color = FantasyGold, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(28.dp))

        FantasyCard(borderColor = FantasyGold) {
            Text("EXPERIENCE", style = MaterialTheme.typography.labelLarge,
                color = FantasyGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            XpProgressBar(currentXp = taskViewModel.currentXp, maxXp = taskViewModel.xpToNextLevel)
        }

        Spacer(Modifier.height(16.dp))

        FantasyCard(borderColor = FantasyGoldDim, borderWidth = 1.dp,
            gradient = listOf(AncientBrownLight, AncientBrown)) {
            Text("CHRONICLES", style = MaterialTheme.typography.labelLarge,
                color = FantasyGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            GoldDivider()
            Spacer(Modifier.height(12.dp))
            ProfileRow("Level",           "${taskViewModel.currentLevel}")
            ProfileRow("Experience",      "${taskViewModel.currentXp} / ${taskViewModel.xpToNextLevel} XP")
            ProfileRow("Tasks Completed", "${user?.completedTasksCount ?: 0}")
            ProfileRow("Tasks Active",    "${taskViewModel.tasks.count { !it.isCompleted }}")
            ProfileRow("Overdue",         "${taskViewModel.tasks.count { it.isOverdue }}")
            user?.createdAt?.let { ProfileRow("Member since", it.toDate().toLocaleString()) }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = ParchmentDim)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Parchment, fontWeight = FontWeight.SemiBold)
    }
}

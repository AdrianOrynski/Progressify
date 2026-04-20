package com.example.progressify.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.progressify.*
import com.example.progressify.ui.theme.*
import com.example.progressify.viewmodel.TaskViewModel

@Composable
fun TaskListScreen(user: User?, taskViewModel: TaskViewModel) {
    var showAddDialog    by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var showXpPopup      by remember { mutableStateOf(false) }

    LaunchedEffect(taskViewModel.showXpPopup) {
        if (taskViewModel.showXpPopup) {
            showXpPopup = true
            kotlinx.coroutines.delay(2000)
            showXpPopup = false
            taskViewModel.clearXpAnim()
            kotlinx.coroutines.delay(500)
            taskViewModel.resetXpAfterAnimation()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = DarkWood,
            floatingActionButton = {
                FloatingActionButton(
                    onClick        = { showAddDialog = true },
                    containerColor = FantasyGold, contentColor = DarkWood,
                    shape          = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Bounty")
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(DarkWood, ShadowBlack.copy(alpha = 0.5f), DarkWood)))
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Text("📜 BOUNTY BOARD",
                    style    = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = FantasyGold),
                    modifier = Modifier.padding(bottom = 6.dp, top = 16.dp))
                Text("Hero: ${user?.nickname ?: "Adventurer"}",
                    style = MaterialTheme.typography.labelLarge, color = ParchmentDim,
                    modifier = Modifier.padding(bottom = 16.dp))

                val categories = listOf("All") + TaskCategory.entries.map { it.label }
                    .filter { cat -> taskViewModel.tasks.any { it.category == cat } }
                if (taskViewModel.tasks.isNotEmpty()) {
                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach { cat ->
                            CategorySelectChip(
                                label      = cat,
                                isSelected = selectedCategory == cat,
                                onClick    = { selectedCategory = cat }
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                GoldDivider(label = "BOUNTIES")
                Spacer(Modifier.height(12.dp))

                when {
                    taskViewModel.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        FantasyLoadingIndicator()
                    }
                    taskViewModel.tasks.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                        FantasyCard(borderColor = FantasyGoldDim.copy(alpha = 0.5f),
                            gradient = listOf(AncientBrown.copy(alpha = 0.7f), DarkWood)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)) {
                                Text("⚔️", fontSize = 48.sp, modifier = Modifier.padding(bottom = 12.dp))
                                Text("BOUNTY BOARD EMPTY", style = MaterialTheme.typography.titleLarge,
                                    color = FantasyGold, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Text("Press + to post your first bounty!",
                                    style = MaterialTheme.typography.bodyMedium, color = ParchmentDim)
                            }
                        }
                    }
                    else -> {
                        val filtered = if (selectedCategory == "All") taskViewModel.tasks
                        else taskViewModel.tasks.filter { it.category == selectedCategory }
                        val sorted = filtered.sortedWith(
                            compareByDescending<Task> { it.isOverdue }
                                .thenBy { it.isCompleted }
                                .thenBy { it.startTime?.toDate() }
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(sorted, key = { it.id }) { task ->
                                TaskCard(task = task,
                                    onComplete = { taskViewModel.completeTask(task) },
                                    onDelete   = { taskViewModel.deleteTask(task) },
                                    showDelete = true)
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible  = showXpPopup,
            enter    = fadeIn() + slideInVertically { it },
            exit     = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 100.dp)
        ) {
            Card(shape  = RoundedCornerShape(50.dp),
                colors  = CardDefaults.cardColors(containerColor = AncientBrown),
                border  = BorderStroke(1.dp, FantasyGold)) {
                Text("+${taskViewModel.xpGainedAnim} XP ✨",
                    color      = FantasyGold, fontWeight = FontWeight.ExtraBold,
                    style      = MaterialTheme.typography.titleLarge,
                    modifier   = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, category, difficulty, startTime, endTime, recurrence ->
                taskViewModel.addTask(title, desc, category, difficulty, startTime, endTime, recurrence)
                showAddDialog = false
            }
        )
    }
}

package com.example.progressify

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.progressify.screens.*
import com.example.progressify.ui.theme.*
import com.example.progressify.viewmodel.TaskViewModel

private sealed class NavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : NavItem("dashboard", "Tavern",   Icons.Default.Home)
    object Tasks     : NavItem("tasks",     "Bounties", Icons.Default.List)
    object Skills    : NavItem("skills",    "Skills",   Icons.Default.Star)
    object Profile   : NavItem("profile",   "Hero",     Icons.Default.Person)
}

private val navItems = listOf(NavItem.Dashboard, NavItem.Tasks, NavItem.Skills, NavItem.Profile)

@Composable
fun MainApp(user: User?) {
    val navController                = rememberNavController()
    val taskViewModel: TaskViewModel = viewModel()
    val entry   by navController.currentBackStackEntryAsState()
    val current = entry?.destination?.route

    LaunchedEffect(user) { taskViewModel.syncXpFromUser(user) }

    Scaffold(
        containerColor = DarkWood,
        bottomBar = {
            NavigationBar(containerColor = androidx.compose.ui.graphics.Color(0xFF0A0704), tonalElevation = 0.dp) {
                navItems.forEach { item ->
                    NavigationBarItem(
                        icon     = { Icon(item.icon, contentDescription = item.title) },
                        label    = { Text(item.title, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall) },
                        selected = current == item.route,
                        colors   = NavigationBarItemDefaults.colors(
                            selectedIconColor   = FantasyGold,
                            selectedTextColor   = FantasyGold,
                            unselectedIconColor = ParchmentDim.copy(alpha = 0.5f),
                            unselectedTextColor = ParchmentDim.copy(alpha = 0.5f),
                            indicatorColor      = DeepDragonRed.copy(alpha = 0.35f)
                        ),
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true; restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = NavItem.Dashboard.route,
            modifier         = Modifier
                .padding(innerPadding)
                .pointerInput(current) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val idx = navItems.indexOfFirst { it.route == current }
                            if (totalDrag < -200 && idx in 0 until navItems.size - 1) {
                                navController.navigate(navItems[idx + 1].route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            } else if (totalDrag > 200 && idx > 0) {
                                navController.navigate(navItems[idx - 1].route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            }
                            totalDrag = 0f
                        },
                        onDragCancel = { totalDrag = 0f }
                    ) { _, dragAmount -> totalDrag += dragAmount }
                },
            enterTransition  = { fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 6 } },
            exitTransition   = { fadeOut(tween(250)) + slideOutHorizontally(tween(250)) { -it / 6 } }
        ) {
            composable(NavItem.Dashboard.route) { DashboardScreen(user, taskViewModel) }
            composable(NavItem.Tasks.route)     { TaskListScreen(user, taskViewModel) }
            composable(NavItem.Skills.route)    { SkillsScreen(taskViewModel) }
            composable(NavItem.Profile.route)   { ProfileScreen(user, taskViewModel) }
        }
    }
}

@Composable
internal fun CategorySelectChip(
    label      : String,
    isSelected : Boolean,
    onClick    : () -> Unit,
    modifier   : Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(if (isSelected) DeepDragonRed else AncientBrownLight, RoundedCornerShape(50.dp))
            .border(1.dp,
                if (isSelected) FantasyGold else ParchmentDim.copy(alpha = 0.3f),
                RoundedCornerShape(50.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label,
            style    = MaterialTheme.typography.labelSmall,
            color    = if (isSelected) Parchment else ParchmentDim,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis)
    }
}

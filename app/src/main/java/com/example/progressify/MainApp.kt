package com.example.progressify

import androidx.compose.animation.core.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.example.progressify.User
import com.example.progressify.ui.theme.*

// ─────────────────────────────────────────────────────────────────
// Bottom Nav setup
// ─────────────────────────────────────────────────────────────────

private sealed class NavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : NavItem("dashboard", "Tavern",   Icons.Default.Home)
    object Tasks     : NavItem("tasks",     "Bounties", Icons.Default.List)
    object Profile   : NavItem("profile",   "Hero",     Icons.Default.Person)
}

@Composable
fun MainApp(user: User?) {
    val navController = rememberNavController()

    Scaffold(
        containerColor = DarkWood,
        bottomBar = {
            val entry by navController.currentBackStackEntryAsState()
            val current = entry?.destination?.route
            val items = listOf(NavItem.Dashboard, NavItem.Tasks, NavItem.Profile)

            NavigationBar(containerColor = Color(0xFF0A0704), tonalElevation = 0.dp) {
                items.forEach { item ->
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
                        onClick  = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
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
            modifier         = Modifier.padding(innerPadding),
            enterTransition  = { fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 6 } },
            exitTransition   = { fadeOut(tween(250)) + slideOutHorizontally(tween(250)) { -it / 6 } }
        ) {
            composable(NavItem.Dashboard.route) { DashboardScreen(user) }
            composable(NavItem.Tasks.route)     { TaskListScreen(user) }
            composable(NavItem.Profile.route)   { ProfileScreen(user) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Dashboard
// ─────────────────────────────────────────────────────────────────

@Composable
fun DashboardScreen(user: User?) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val avatarScale by infiniteTransition.animateFloat(
        initialValue  = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkWood, ShadowBlack.copy(alpha = 0.7f), DarkWood)))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "⚔️ THE TAVERN",
            style    = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = FantasyGold),
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )

        // Karta bohatera
        FantasyCard(borderColor = FantasyGold, borderWidth = 2.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.scale(avatarScale)) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null,
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
                    Text("LVL ${user?.level ?: 1} HERO", color = FantasyGold,
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(20.dp))
            XpProgressBar(currentXp = user?.experiencePoints ?: 0, maxXp = 1000)
        }

        Spacer(Modifier.height(24.dp))
        GoldDivider(label = "HERO STATS")
        Spacer(Modifier.height(16.dp))

        // Statystyki
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("Level", "${user?.level ?: 1}", modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Star, null, tint = FantasyGold, modifier = Modifier.size(26.dp))
            }
            StatCard("Total XP", "${user?.experiencePoints ?: 0}", modifier = Modifier.weight(1f)) {
                Text("✨", fontSize = 20.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
        GoldDivider(label = "ACTIVE BOUNTIES")
        Spacer(Modifier.height(12.dp))

        FantasyCard(borderColor = FantasyGoldDim.copy(alpha = 0.5f),
            gradient = listOf(AncientBrown.copy(alpha = 0.6f), DarkWood)) {
            Text("No bounties posted yet.\nVisit the Bounty Board to begin your quest!",
                style = MaterialTheme.typography.bodyMedium, color = ParchmentDim,
                modifier = Modifier.padding(8.dp))
        }

        user?.createdAt?.let {
            Spacer(Modifier.height(20.dp))
            GoldDivider()
            Spacer(Modifier.height(10.dp))
            Text("At Progressify since: ${it.toDate().toLocaleString()}",
                style = MaterialTheme.typography.labelSmall,
                color = ParchmentDim.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        Spacer(Modifier.height(16.dp))
    }
}

// ── StatCard helper ───────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────────
// Task List (placeholder — logika w Tygodniu 3)
// ─────────────────────────────────────────────────────────────────

@Composable
fun TaskListScreen(user: User?) {
    Scaffold(
        containerColor = DarkWood,
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { /* TODO Week 3 */ },
                containerColor = FantasyGold,
                contentColor   = DarkWood,
                shape          = RoundedCornerShape(8.dp)
            ) { Icon(Icons.Default.Add, contentDescription = "New Bounty") }
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
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = FantasyGold),
                modifier = Modifier.padding(bottom = 6.dp, top = 16.dp))
            Text("Hero: ${user?.nickname ?: "Adventurer"}",
                style = MaterialTheme.typography.labelLarge, color = ParchmentDim,
                modifier = Modifier.padding(bottom = 24.dp))

            GoldDivider(label = "AVAILABLE BOUNTIES")
            Spacer(Modifier.height(24.dp))

            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                FantasyCard(borderColor = FantasyGoldDim.copy(alpha = 0.5f),
                    gradient = listOf(AncientBrown.copy(alpha = 0.7f), DarkWood)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Text("⚔️", fontSize = 48.sp, modifier = Modifier.padding(bottom = 12.dp))
                        Text("BOUNTY BOARD EMPTY", style = MaterialTheme.typography.titleLarge,
                            color = FantasyGold, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Task system arriving in Week 3.\nPress + to prepare for battle!",
                            style = MaterialTheme.typography.bodyMedium, color = ParchmentDim)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Profile
// ─────────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(user: User?) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatarPulse")
    val avatarScale by infiniteTransition.animateFloat(
        initialValue  = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label         = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkWood, ShadowBlack.copy(alpha = 0.6f), DarkWood)))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🛡️ HERO SHEET",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = FantasyGold),
            modifier = Modifier.align(Alignment.Start).padding(bottom = 24.dp, top = 16.dp))

        // Avatar
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.scale(avatarScale).size(120.dp).clip(CircleShape)
                .background(Brush.radialGradient(listOf(AncientBrownLight, AncientBrown)))
        ) {
            Icon(Icons.Default.Person, null, modifier = Modifier.size(80.dp), tint = FantasyGold)
        }

        Spacer(Modifier.height(16.dp))
        Text(user?.nickname?.uppercase() ?: "ADVENTURER",
            style = MaterialTheme.typography.headlineMedium, color = Parchment, fontWeight = FontWeight.ExtraBold)
        if (user?.name?.isNotBlank() == true || user?.surname?.isNotBlank() == true)
            Text("${user.name} ${user.surname}".trim(),
                style = MaterialTheme.typography.bodyMedium, color = ParchmentDim)
        Text("CLASS: LEVEL ${user?.level ?: 1} EXPLORER",
            color = FantasyGold, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))

        Spacer(Modifier.height(28.dp))

        // Pasek XP
        FantasyCard(borderColor = FantasyGold) {
            Text("EXPERIENCE", style = MaterialTheme.typography.labelLarge,
                color = FantasyGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            XpProgressBar(currentXp = user?.experiencePoints ?: 0, maxXp = 1000)
        }

        Spacer(Modifier.height(16.dp))

        // Chronicles
        FantasyCard(borderColor = FantasyGoldDim, borderWidth = 1.dp,
            gradient = listOf(AncientBrownLight, AncientBrown)) {
            Text("CHRONICLES", style = MaterialTheme.typography.labelLarge,
                color = FantasyGold, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
            GoldDivider()
            Spacer(Modifier.height(12.dp))
            ProfileRow("Level",      "${user?.level ?: 1}")
            ProfileRow("Experience", "${user?.experiencePoints ?: 0} XP")
            user?.createdAt?.let { ProfileRow("Member since", it.toDate().toLocaleString()) }
        }

        Spacer(Modifier.height(16.dp))

        FantasyCard(borderColor = FantasyGoldDim.copy(alpha = 0.4f), borderWidth = 1.dp,
            gradient = listOf(AncientBrown.copy(alpha = 0.5f), DarkWood)) {
            Text("🏆 Achievements  ·  Skills  ·  Classes — coming soon",
                style = MaterialTheme.typography.bodyMedium,
                color = ParchmentDim.copy(alpha = 0.7f), modifier = Modifier.padding(4.dp))
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = ParchmentDim)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Parchment, fontWeight = FontWeight.SemiBold)
    }
}
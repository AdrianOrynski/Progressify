package com.example.progressify

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.progressify.ui.theme.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────
// TaskViewModel
// ─────────────────────────────────────────────────────────────────

class TaskViewModel : ViewModel() {
    private val taskRepository = TaskRepository()
    private val db             = FirebaseFirestore.getInstance()
    private val uid get()      = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var tasks      by mutableStateOf<List<Task>>(emptyList())
        private set
    var isLoading  by mutableStateOf(false)
        private set
    var error      by mutableStateOf<String?>(null)
        private set
    var currentXp    by mutableIntStateOf(0)
        private set
    var currentLevel by mutableIntStateOf(1)
        private set
    var xpGainedAnim by mutableIntStateOf(0)
        private set

    val xpToNextLevel get() = currentLevel * 1000

    init { loadTasks() }

    fun loadTasks() {
        if (uid.isBlank()) return
        isLoading = true
        taskRepository.getTasks(
            uid       = uid,
            onSuccess = { tasks = it; isLoading = false },
            onFailure = { error = "Failed to load tasks"; isLoading = false }
        )
    }

    fun syncXpFromUser(user: User?) {
        user?.let { currentXp = it.experiencePoints; currentLevel = it.level }
    }

    fun addTask(title: String, description: String, category: String,
                difficulty: String, startTime: Timestamp, endTime: Timestamp,
                recurrence: RecurrenceRule) {
        val task = Task(
            uid         = uid,
            title       = title,
            description = description,
            category    = category,
            difficulty  = difficulty,
            startTime   = startTime,
            endTime     = endTime,
            recurrence  = recurrence
        )
        taskRepository.addTask(task) { success ->
            if (success) loadTasks() else error = "Failed to add task"
        }
    }

    fun deleteTask(taskId: String) {
        taskRepository.deleteTask(taskId) { success ->
            if (success) tasks = tasks.filter { it.id != taskId }
            else error = "Failed to delete task"
        }
    }

    fun completeTask(task: Task) {
        if (task.isCompleted) return
        val completedTask = task.copy(isCompleted = true, completedAt = Timestamp.now())
        val xp = completedTask.calculateXp()
        taskRepository.completeTask(task.id, xp) { success ->
            if (success) {
                tasks = tasks.map { if (it.id == task.id) completedTask.copy(xpAwarded = xp) else it }
                xpGainedAnim = xp
                currentXp += xp
                while (currentXp >= xpToNextLevel) { currentXp -= xpToNextLevel; currentLevel++ }
                saveXpToFirestore()
            } else error = "Failed to complete task"
        }
    }

    private fun saveXpToFirestore() {
        if (uid.isBlank()) return
        db.collection("users").document(uid)
            .update(mapOf("experiencePoints" to currentXp, "level" to currentLevel))
    }

    fun clearXpAnim() { xpGainedAnim = 0 }
    fun clearError()  { error = null }
}

// ─────────────────────────────────────────────────────────────────
// Bottom Nav
// ─────────────────────────────────────────────────────────────────

private sealed class NavItem(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : NavItem("dashboard", "Tavern",   Icons.Default.Home)
    object Tasks     : NavItem("tasks",     "Bounties", Icons.Default.List)
    object Profile   : NavItem("profile",   "Hero",     Icons.Default.Person)
}

@Composable
fun MainApp(user: User?) {
    val navController                = rememberNavController()
    val taskViewModel: TaskViewModel = viewModel()

    LaunchedEffect(user) { taskViewModel.syncXpFromUser(user) }

    Scaffold(
        containerColor = DarkWood,
        bottomBar = {
            val entry   by navController.currentBackStackEntryAsState()
            val current = entry?.destination?.route
            NavigationBar(containerColor = Color(0xFF0A0704), tonalElevation = 0.dp) {
                listOf(NavItem.Dashboard, NavItem.Tasks, NavItem.Profile).forEach { item ->
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
            modifier         = Modifier.padding(innerPadding),
            enterTransition  = { fadeIn(tween(250)) + slideInHorizontally(tween(250)) { it / 6 } },
            exitTransition   = { fadeOut(tween(250)) + slideOutHorizontally(tween(250)) { -it / 6 } }
        ) {
            composable(NavItem.Dashboard.route) { DashboardScreen(user, taskViewModel) }
            composable(NavItem.Tasks.route)     { TaskListScreen(user, taskViewModel) }
            composable(NavItem.Profile.route)   { ProfileScreen(user, taskViewModel) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Dashboard
// ─────────────────────────────────────────────────────────────────

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

        // Character card
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


        // TODO: Remove — for XP testing only
        /*Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                taskViewModel.currentXp += 50
                while (taskViewModel.currentXp >= taskViewModel.xpToNextLevel) {
                    taskViewModel.currentXp -= taskViewModel.xpToNextLevel
                    taskViewModel.currentLevel++
                }
                taskViewModel.xpGainedAnim = 50
                taskViewModel.saveXpToFirestore()
            },
            colors   = ButtonDefaults.buttonColors(containerColor = DeepDragonRed),
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(4.dp)
        ) { Text("+XP TEST — REMOVE BEFORE COMMIT", fontWeight = FontWeight.Bold, fontSize = 11.sp) }*/
        // TODO: Remove — for XP testing only


        // Warning about expired tasks
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

// ─────────────────────────────────────────────────────────────────
// Task List
// ─────────────────────────────────────────────────────────────────

@Composable
fun TaskListScreen(user: User?, taskViewModel: TaskViewModel) {
    var showAddDialog    by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var showXpPopup      by remember { mutableStateOf(false) }

    LaunchedEffect(taskViewModel.xpGainedAnim) {
        if (taskViewModel.xpGainedAnim > 0) {
            showXpPopup = true
            kotlinx.coroutines.delay(2000)
            showXpPopup = false
            taskViewModel.clearXpAnim()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = DarkWood,
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true },
                    containerColor = FantasyGold, contentColor = DarkWood,
                    shape = RoundedCornerShape(8.dp)) {
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

                // Filtry kategorii
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
                                    onDelete   = { taskViewModel.deleteTask(task.id) },
                                    showDelete = true)
                            }
                        }
                    }
                }
            }
        }

        // Popup +XP
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

// ─────────────────────────────────────────────────────────────────
// Task card
// ─────────────────────────────────────────────────────────────────

@Composable
fun TaskCard(task: Task, onComplete: () -> Unit, onDelete: () -> Unit,
             showDelete: Boolean, modifier: Modifier = Modifier) {
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
        Row(modifier = Modifier
            .padding(12.dp)
            .fillMaxWidth(),
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
                    val fmt       = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                    val timeFmt   = SimpleDateFormat("HH:mm", Locale.getDefault())
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

// ─────────────────────────────────────────────────────────────────
// Add task
// ─────────────────────────────────────────────────────────────────

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, Timestamp, Timestamp, RecurrenceRule) -> Unit
) {
    var title        by remember { mutableStateOf("") }
    var desc         by remember { mutableStateOf("") }
    var selectedCat  by remember { mutableStateOf("") }
    var selectedDiff by remember { mutableStateOf(Difficulty.MEDIUM.name) }

    var startDate   by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var startHour   by remember { mutableStateOf<Int?>(null) }
    var startMinute by remember { mutableStateOf<Int?>(null) }
    var endDate     by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    var endHour     by remember { mutableStateOf<Int?>(null) }
    var endMinute   by remember { mutableStateOf<Int?>(null) }

    var titleError by remember { mutableStateOf(false) }
    var timeError  by remember { mutableStateOf(false) }

    var recurrenceType by remember { mutableStateOf(RecurrenceType.NONE.name) }
    var selectedDays   by remember { mutableStateOf(setOf<Int>()) }
    var interval       by remember { mutableIntStateOf(1) }

    val context  = LocalContext.current
    val dateFmt  = SimpleDateFormat("dd MMM", Locale.getDefault())

    fun buildTimestamp(date: Triple<Int, Int, Int>?, hour: Int?, minute: Int?): Timestamp? {
        if (date == null || hour == null || minute == null) return null
        val cal = Calendar.getInstance()
        cal.set(date.first, date.second, date.third, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return Timestamp(cal.time)
    }

    fun dateLabel(date: Triple<Int, Int, Int>?): String {
        if (date == null) return "Date"
        val cal = Calendar.getInstance().apply { set(date.first, date.second, date.third) }
        return dateFmt.format(cal.time)
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(AncientBrown, DarkWood)),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(1.5.dp, FantasyGold, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {

                Text("POST A BOUNTY",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold, color = FantasyGold))
                Spacer(Modifier.height(20.dp))

                // ── Title ──────────────────────────────────────────────
                FantasyTextField(
                    value         = title,
                    onValueChange = { title = it; titleError = false },
                    label         = "Task Title *",
                    isError       = titleError
                )
                if (titleError) Text("Title is required",
                    color = DragonRedLight, style = MaterialTheme.typography.labelSmall)

                Spacer(Modifier.height(12.dp))
                FantasyTextField(value = desc, onValueChange = { desc = it }, label = "Description")
                Spacer(Modifier.height(16.dp))

                // ── Category – 2-row grid ───────────────────────
                Text("CATEGORY", style = MaterialTheme.typography.labelMedium, color = ParchmentDim)
                Spacer(Modifier.height(8.dp))

                val cats      = TaskCategory.entries
                val firstRow  = cats.take(3)
                val secondRow = cats.drop(3)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(firstRow, secondRow).forEach { row ->
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { cat ->
                                CategorySelectChip(
                                    label      = cat.label,
                                    isSelected = selectedCat == cat.label,
                                    onClick    = {
                                        selectedCat = if (selectedCat == cat.label) "" else cat.label
                                    },
                                    modifier   = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Difficulty ───────────────────────────────────────────
                Text("DIFFICULTY", style = MaterialTheme.typography.labelMedium, color = ParchmentDim)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { diff ->
                        val isSelected = selectedDiff == diff.name
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) difficultyColor(diff.name).copy(alpha = 0.3f)
                                    else AncientBrownLight,
                                    RoundedCornerShape(6.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) difficultyColor(diff.name)
                                    else ParchmentDim.copy(alpha = 0.3f),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { selectedDiff = diff.name }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(diff.label,
                                style      = MaterialTheme.typography.labelMedium,
                                color      = if (isSelected) difficultyColor(diff.name) else ParchmentDim,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Time Window – Start / End ──────────────────────────
                Text("TIME WINDOW *",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (timeError) DragonRedLight else ParchmentDim)
                Spacer(Modifier.height(8.dp))

                @Composable
                fun TimeRow(
                    label     : String,
                    date      : Triple<Int, Int, Int>?,
                    hour      : Int?,
                    minute    : Int?,
                    onPickDate: () -> Unit,
                    onPickTime: () -> Unit
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(label,
                            style    = MaterialTheme.typography.labelMedium,
                            color    = ParchmentDim,
                            modifier = Modifier.width(34.dp))

                        OutlinedButton(
                            onClick        = onPickDate,
                            modifier       = Modifier.weight(1f),
                            shape          = RoundedCornerShape(6.dp),
                            border         = BorderStroke(1.dp,
                                if (date != null) FantasyGold else ParchmentDim.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.DateRange, null,
                                tint     = if (date != null) FantasyGold else ParchmentDim,
                                modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(dateLabel(date),
                                color = if (date != null) FantasyGold else ParchmentDim,
                                style = MaterialTheme.typography.bodyMedium)
                        }

                        OutlinedButton(
                            onClick        = onPickTime,
                            modifier       = Modifier.weight(1f),
                            shape          = RoundedCornerShape(6.dp),
                            border         = BorderStroke(1.dp,
                                if (hour != null) FantasyGold else ParchmentDim.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Schedule, null,
                                tint     = if (hour != null) FantasyGold else ParchmentDim,
                                modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (hour != null && minute != null)
                                    String.format("%02d:%02d", hour, minute)
                                else "Time",
                                color = if (hour != null) FantasyGold else ParchmentDim,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                TimeRow(
                    label      = "Start",
                    date       = startDate,
                    hour       = startHour,
                    minute     = startMinute,
                    onPickDate = {
                        showDatePicker(context) { y, m, d ->
                            startDate = Triple(y, m, d); timeError = false
                        }
                    },
                    onPickTime = {
                        showTimePicker(context) { h, min ->
                            startHour = h; startMinute = min; timeError = false
                        }
                    }
                )

                Spacer(Modifier.height(6.dp))

                TimeRow(
                    label      = "End",
                    date       = endDate,
                    hour       = endHour,
                    minute     = endMinute,
                    onPickDate = {
                        showDatePicker(context) { y, m, d ->
                            endDate = Triple(y, m, d); timeError = false
                        }
                    },
                    onPickTime = {
                        showTimePicker(context) { h, min ->
                            endHour = h; endMinute = min; timeError = false
                        }
                    }
                )

                if (timeError) Text("Set start and end date & time",
                    color = DragonRedLight, style = MaterialTheme.typography.labelSmall)

                Spacer(Modifier.height(16.dp))

                // ── Recurrence ────────────────────────────────────────
                Text("RECURRENCE", style = MaterialTheme.typography.labelMedium, color = ParchmentDim)
                Spacer(Modifier.height(8.dp))

                Row(Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RecurrenceType.entries.forEach { type ->
                        CategorySelectChip(
                            label      = type.label,
                            isSelected = recurrenceType == type.name,
                            onClick    = { recurrenceType = type.name; selectedDays = setOf() }
                        )
                    }
                }

                if (recurrenceType == RecurrenceType.SELECTED_DAYS.name) {
                    Spacer(Modifier.height(10.dp))
                    val dayLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dayLabels.forEachIndexed { index, label ->
                            val dayNum     = index + 1
                            val isSelected = selectedDays.contains(dayNum)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isSelected) DeepDragonRed else AncientBrownLight,
                                        CircleShape)
                                    .border(1.dp,
                                        if (isSelected) FantasyGold else ParchmentDim.copy(alpha = 0.3f),
                                        CircleShape)
                                    .clickable {
                                        selectedDays =
                                            if (isSelected) selectedDays - dayNum
                                            else selectedDays + dayNum
                                    }
                            ) {
                                Text(label,
                                    style      = MaterialTheme.typography.labelSmall,
                                    color      = if (isSelected) Parchment else ParchmentDim,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                if (recurrenceType != RecurrenceType.NONE.name
                    && recurrenceType != RecurrenceType.DAILY.name
                    && recurrenceType != RecurrenceType.SELECTED_DAYS.name
                ) {
                    Spacer(Modifier.height(10.dp))
                    val intervalLabel = when (recurrenceType) {
                        RecurrenceType.WEEKLY.name  -> "Every X weeks"
                        RecurrenceType.MONTHLY.name -> "Every X months"
                        RecurrenceType.YEARLY.name  -> "Every X years"
                        else                        -> "Interval"
                    }
                    Row(verticalAlignment    = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(intervalLabel, style = MaterialTheme.typography.labelMedium, color = ParchmentDim)
                        Row(verticalAlignment    = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { if (interval > 1) interval-- },
                                modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Remove, null, tint = FantasyGold, modifier = Modifier.size(16.dp))
                            }
                            Text("$interval", style = MaterialTheme.typography.titleLarge,
                                color = FantasyGold, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { interval++ }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Add, null, tint = FantasyGold, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── Buttons ──────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(4.dp), border = BorderStroke(1.dp, ParchmentDim)) {
                        Text("CANCEL", color = ParchmentDim)
                    }
                    Button(
                        onClick = {
                            val startTs = buildTimestamp(startDate, startHour, startMinute)
                            val endTs   = buildTimestamp(endDate, endHour, endMinute)
                            titleError  = title.isBlank()
                            timeError   = startTs == null || endTs == null
                            if (!titleError && !timeError) {
                                onConfirm(
                                    title, desc, selectedCat, selectedDiff,
                                    startTs!!, endTs!!,
                                    RecurrenceRule(
                                        type         = recurrenceType,
                                        selectedDays = selectedDays.toList().sorted(),
                                        interval     = interval
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(4.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = DeepDragonRed)
                    ) { Text("CONFIRM", color = Parchment, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Profiles
// ─────────────────────────────────────────────────────────────────

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
            ProfileRow("Tasks Completed", "${taskViewModel.tasks.count { it.isCompleted }}")
            ProfileRow("Tasks Active",    "${taskViewModel.tasks.count { !it.isCompleted }}")
            ProfileRow("Overdue",         "${taskViewModel.tasks.count { it.isOverdue }}")
            user?.createdAt?.let { ProfileRow("Member since", it.toDate().toLocaleString()) }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────
// Picker helpers
// ─────────────────────────────────────────────────────────────────

private fun showTimePicker(context: Context, onResult: (Int, Int) -> Unit) {
    val now = Calendar.getInstance()
    TimePickerDialog(context, { _, hour, minute ->
        onResult(hour, minute)
    }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
}

private fun showDatePicker(context: Context, onResult: (Int, Int, Int) -> Unit) {
    val now = Calendar.getInstance()
    DatePickerDialog(context, { _, year, month, day ->
        onResult(year, month, day)
    }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
}

// ─────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────

private fun difficultyColor(difficulty: String): Color = when (difficulty) {
    Difficulty.EASY.name -> Color(0xFF4CAF50)
    Difficulty.HARD.name -> DragonRedLight
    else                 -> FantasyGold
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

@Composable
private fun CategoryChip(category: String, completed: Boolean) {
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
private fun DifficultyChip(difficulty: String, completed: Boolean) {
    val color = if (completed) Color.Gray else difficultyColor(difficulty)
    val label = try { Difficulty.valueOf(difficulty).label } catch (e: Exception) { difficulty }
    Box(modifier = Modifier
        .background(color.copy(alpha = 0.15f), RoundedCornerShape(50.dp))
        .padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RecurringChip(completed: Boolean) {
    Box(modifier = Modifier
        .background(
            FantasyGold.copy(alpha = if (completed) 0.1f else 0.15f),
            RoundedCornerShape(50.dp))
        .padding(horizontal = 8.dp, vertical = 2.dp)) {
        Text("↻ recurring", style = MaterialTheme.typography.labelSmall,
            color = if (completed) Color.Gray else FantasyGoldDim)
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
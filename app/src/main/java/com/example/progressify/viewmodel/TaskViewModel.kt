package com.example.progressify.viewmodel

import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import com.example.progressify.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TaskViewModel(app: Application) : AndroidViewModel(app) {
    private val taskRepository = TaskRepository()
    private val db             = FirebaseFirestore.getInstance()
    private val uid get()      = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    var tasks      by mutableStateOf<List<Task>>(emptyList())
        private set
    var isLoading  by mutableStateOf(false)
        private set
    var error      by mutableStateOf<String?>(null)
        private set
    var currentXp     by mutableIntStateOf(0)
        private set
    var currentLevel  by mutableIntStateOf(1)
        private set
    var xpGainedAnim  by mutableIntStateOf(0)
        private set
    var currentStreak  by mutableIntStateOf(0)
        private set
    var longestStreak  by mutableIntStateOf(0)
        private set
    var streakDates    by mutableStateOf<Set<String>>(emptySet())
        private set

    var categoryStats by mutableStateOf<Map<TaskCategory, CategoryStats>>(emptyMap())
        private set

    val xpToNextLevel get() = currentLevel * 1000

    init { loadTasks()
           loadAllCategoryStats() }

    // ── Loading Tasks ───────────────────────────────────────────

    fun loadTasks() {
        if (uid.isBlank()) return
        isLoading = true
        taskRepository.getTasks(
            uid       = uid,
            onSuccess = { tasks = it; isLoading = false },
            onFailure = { error = "Failed to load tasks"; isLoading = false }
        )
    }

    fun loadTasksForCategory(category: TaskCategory) {
        if (uid.isBlank()) return
        taskRepository.getTasksByCategory(
            uid = uid,
            category = category.name,
            onSuccess = { loaded ->
                tasks = tasks.filter { it.category != category.label } + loaded
            },
            onFailure = { error = "Failed to load tasks for ${category.label}" }
        )
    }

    fun loadAllCategoryStats() {
        if (uid.isBlank()) return
        val result = mutableMapOf<TaskCategory, CategoryStats>()
        var remaining = TaskCategory.entries.size

        for (cat in TaskCategory.entries) {
            taskRepository.getCategoryStats(
                uid = uid,
                category = cat.name,
                onSuccess = { stats ->
                    result[cat] = stats
                    if (--remaining == 0) categoryStats = result.toMap()
                },
                onFailure = {
                    result[cat] = CategoryStats()
                    if (--remaining == 0) categoryStats = result.toMap()
                }
            )
        }
    }

    // ── User data synchronization ─────────────────────────

    fun syncXpFromUser(user: User?) {
        user?.let {
            currentXp     = it.experiencePoints
            currentLevel  = it.level
            currentStreak = it.currentStreak
            longestStreak = it.longestStreak
            streakDates   = it.streakDates.toSet()
        }
    }

    // ── Add Task ─────────────────────────────────────────

    fun addTask(
        title: String,
        description: String,
        category: String,
        difficulty: String,
        startTime: Timestamp,
        endTime: Timestamp,
        recurrence: RecurrenceRule
    ) {
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
        taskRepository.addTask(task) { createdTask ->
            if (createdTask != null) {
                loadTasks()
                NotificationScheduler.schedule(getApplication(), createdTask)
            } else {
                error = "Failed to add task"
            }
        }
    }

    // ── Delete Task ──────────────────────────────────────────

    fun deleteTask(task: Task) {
        NotificationScheduler.cancel(getApplication(), task)
        taskRepository.deleteTask(uid, task.id, task.category) { success ->
            if (success) tasks = tasks.filter { it.id != task.id }
            else error = "Failed to delete task"
        }
    }

    // ── Complete Task ─────────────────────────────────────────

    fun completeTask(task: Task) {
        if (task.isCompleted) return

        NotificationScheduler.cancel(getApplication(), task)

        taskRepository.completeTask(uid, task.id, task.category) { success, xpFromDb ->
            if (success) {
                loadTasks()
                triggerXpPopup(xpFromDb)
                currentXp += xpFromDb
                while (currentXp >= xpToNextLevel) { currentXp -= xpToNextLevel; currentLevel++ }
                saveXpToFirestore()

                val cat = TaskCategory.entries.firstOrNull { it.label == task.category }
                if (cat != null) refreshCategoryStat(cat)

                if (task.isRecurring) {
                    taskRepository.scheduleNextOccurrence(uid, task) { nextTask ->
                        if (nextTask != null) {
                            loadTasksForCategory(TaskCategory.entries.first { it.label == task.category })
                            NotificationScheduler.schedule(getApplication(), nextTask)
                        }
                    }
                }
            } else {
                error = "Failed to complete task"
            }
        }
    }

    // ── Private Helpers ──────────────────────────────────────────

    private fun refreshCategoryStat(cat: TaskCategory) {
        taskRepository.getCategoryStats(
            uid      = uid,
            category = cat.name,
            onSuccess = { stats ->
                categoryStats = categoryStats.toMutableMap().also { it[cat] = stats }
            },
            onFailure = {}
        )
    }

    private fun saveXpToFirestore() {
        if (uid.isBlank()) return
        db.collection("users").document(uid)
            .update(mapOf("experiencePoints" to currentXp, "level" to currentLevel))
    }

    // ── SkillStats (categoryStats from Firestore) ────────

    val skillStats: List<SkillStat>
        get() = TaskCategory.entries.map { cat ->
            val stats    = categoryStats[cat] ?: CategoryStats()
            val catTasks = tasks.filter { it.category == cat.label }
            SkillStat(
                category        = cat,
                level           = stats.level,
                currentXp       = stats.exp - xpForLevel(stats.level),
                xpToNextLevel   = stats.level * 100,
                completedTasks  = stats.completedTasksCount,
                totalTasks      = catTasks.size + stats.completedTasksCount
            )
        }

    private fun xpForLevel(level: Int): Int {
        var total = 0
        for (l in 1 until level) total += l * 100
        return total
    }

    // ── XP popup ─────────────────────────────────────────────────

    var showXpPopup by mutableStateOf(false)
        private set

    fun triggerXpPopup(xp: Int) { xpGainedAnim = xp; showXpPopup = true }
    fun clearXpAnim()            { showXpPopup = false }
    fun resetXpAfterAnimation()  { xpGainedAnim = -1 }
    fun clearError()             { error = null }
}

data class SkillStat(
    val category: TaskCategory,
    val level: Int,
    val currentXp: Int,
    val xpToNextLevel: Int,
    val completedTasks: Int,
    val totalTasks: Int
)
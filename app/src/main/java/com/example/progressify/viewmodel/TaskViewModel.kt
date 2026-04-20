package com.example.progressify.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.example.progressify.*
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        taskRepository.deleteTask(currentUserId, taskId) { success ->
            if (success) tasks = tasks.filter { it.id != taskId }
            else error = "Failed to delete task"
        }
    }

    fun completeTask(task: Task) {
        if (task.isCompleted) return
        taskRepository.completeTask(task.uid, task.id) { success, xpFromDb ->
            if (success) {
                tasks = tasks.map {
                    if (it.id == task.id) it.copy(
                        isCompleted = true,
                        completedAt = Timestamp.now(),
                        xpAwarded   = xpFromDb
                    ) else it
                }
                triggerXpPopup(xpFromDb)
                currentXp += xpFromDb
                while (currentXp >= xpToNextLevel) { currentXp -= xpToNextLevel; currentLevel++ }
                saveXpToFirestore()
                if (task.isRecurring) loadTasks()
            } else {
                error = "Failed to complete task"
            }
        }
    }

    private fun saveXpToFirestore() {
        if (uid.isBlank()) return
        db.collection("users").document(uid)
            .update(mapOf("experiencePoints" to currentXp, "level" to currentLevel))
    }

    var showXpPopup by mutableStateOf(false)
        private set

    fun triggerXpPopup(xp: Int) { xpGainedAnim = xp; showXpPopup = true }
    fun clearXpAnim() { showXpPopup = false }
    fun resetXpAfterAnimation() { xpGainedAnim = -1 }
    fun clearError() { error = null }

    val skillStats: List<SkillStat>
        get() = TaskCategory.entries.map { cat ->
            val catTasks = tasks.filter { it.category == cat.label }
            val done     = catTasks.filter { it.isCompleted }
            val totalXp  = done.sumOf { it.xpAwarded }
            var lvl      = 1
            var xpLeft   = totalXp
            while (xpLeft >= lvl * 100) { xpLeft -= lvl * 100; lvl++ }
            SkillStat(cat, lvl, xpLeft, lvl * 100, done.size, catTasks.size)
        }
}

data class SkillStat(
    val category: TaskCategory,
    val level: Int,
    val currentXp: Int,
    val xpToNextLevel: Int,
    val completedTasks: Int,
    val totalTasks: Int
)

package com.example.progressify

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore

// ── Difficulty level ─────────────────────────────────────────────
enum class Difficulty(val label: String, val xpBase: Int) {
    EASY("Easy", 50),
    MEDIUM("Medium", 100),
    HARD("Hard", 200)
}

// ── Categories ────────────────────────────────────────────────────
enum class TaskCategory(val label: String) {
    SPELLCRAFT("Spellcraft"),
    TASKFORGE("Taskforge"),
    BARDS_DELIGHT("Bard's Delight"),
    SCHOLARS_SANCTUM("Scholars' Sanctum"),
    CYCLE_OF_ORDER("Cycle of Order"),
    BODYFORGE("Bodyforge")
}

// ── Repetition type ───────────────────────────────────────────────
enum class RecurrenceType(val label: String) {
    NONE("None"),
    DAILY("Daily"),
    WEEKLY("Weekly"),
    SELECTED_DAYS("Selected days"),
    MONTHLY("Monthly"),
    YEARLY("Yearly")
}

// ── Repetition type ───────────────────────────────────────────
data class RecurrenceRule(
    val type        : String    = RecurrenceType.NONE.name,
    val selectedDays: List<Int> = emptyList(),
    val interval    : Int       = 1
)

// ── Task model ────────────────────────────────────────────────
data class Task(
    val id          : String         = "",
    val uid         : String         = "",
    val title       : String         = "",
    val description : String         = "",
    val category    : String         = "",
    val difficulty  : String         = Difficulty.MEDIUM.name,
    val startTime   : Timestamp?     = null,
    val endTime     : Timestamp?     = null,
    val isCompleted : Boolean        = false,
    val completedAt : Timestamp?     = null,
    val xpAwarded   : Int            = 0,
    val recurrence  : RecurrenceRule = RecurrenceRule()
) {
    val isOverdue: Boolean
        get() = endTime != null &&
                !isCompleted &&
                endTime.toDate().before(java.util.Date())

    val completedOnTime: Boolean
        get() = isCompleted &&
                endTime != null &&
                completedAt != null &&
                completedAt.toDate().before(endTime.toDate())

    val isRecurring: Boolean
        get() = recurrence.type != RecurrenceType.NONE.name

    fun calculateXp(): Int {
        if (!isCompleted) return 0
        val diff        = try { Difficulty.valueOf(difficulty) } catch (e: Exception) { Difficulty.MEDIUM }
        val base        = diff.xpBase
        val timeBonus   = if (completedOnTime) (base * 0.5).toInt() else 0
        val latePenalty = if (isCompleted && endTime != null && completedAt != null &&
            completedAt.toDate().after(endTime.toDate())) (base * 0.25).toInt() else 0
        return (base + timeBonus - latePenalty).coerceAtLeast(10)
    }
}

// ── Repository ───────────────────────────────────────────────────
class TaskRepository {
    private val db = FirebaseFirestore.getInstance()

    fun getTasks(uid: String, onSuccess: (List<Task>) -> Unit, onFailure: (Exception) -> Unit) {
        db.collection("tasks")
            .whereEqualTo("uid", uid)
            .get()
            .addOnSuccessListener { documents ->
                val tasks = documents.mapNotNull { it.toObject(Task::class.java) }
                onSuccess(tasks)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun addTask(task: Task, onComplete: (Boolean) -> Unit) {
        val id        = db.collection("tasks").document().id
        val taskWithId = task.copy(id = id)
        db.collection("tasks").document(id).set(taskWithId)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun deleteTask(taskId: String, onComplete: (Boolean) -> Unit) {
        db.collection("tasks").document(taskId).delete()
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun completeTask(taskId: String, xpAwarded: Int, onComplete: (Boolean) -> Unit) {
        db.collection("tasks").document(taskId)
            .update(mapOf(
                "completed"   to true,
                "completedAt" to Timestamp.now(),
                "xpAwarded"   to xpAwarded
            ))
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }
}
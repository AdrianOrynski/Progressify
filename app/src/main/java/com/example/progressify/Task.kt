package com.example.progressify

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PropertyName
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import kotlin.math.round

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

    @get:PropertyName("isCompleted")
    @PropertyName("isCompleted")
    val isCompleted : Boolean        = false,

    val completedAt : Timestamp?     = null,
    val xpAwarded   : Int            = 0,
    val recurrence  : RecurrenceRule = RecurrenceRule()
) {
    @get:Exclude
    val isOverdue: Boolean
        get() = endTime != null &&
                !isCompleted &&
                endTime.toDate().before(java.util.Date())

    @get:Exclude
    val completedOnTime: Boolean
        get() = isCompleted &&
                endTime != null &&
                completedAt != null &&
                completedAt.toDate().before(endTime.toDate())

    @get:Exclude
    val isRecurring: Boolean
        get() = recurrence.type != RecurrenceType.NONE.name

    fun calculateXp(now: Timestamp = Timestamp.now()): Int {
        val diff = try { Difficulty.valueOf(difficulty) } catch (e: Exception) { Difficulty.MEDIUM }
        val base = diff.xpBase.toFloat()
        val durationMinutes = getDuration()?.toMinutes()?.toFloat() ?: 0f
        val scaler = durationMinutes / 60f
        val rawXP = (base * scaler * getTimeDiff(now)).toInt()
        val roundedXp = (Math.round(rawXP / 10.0) * 10).toInt()
        return roundedXp
    }

    fun getDuration(): Duration? {
        val start = startTime?.toInstant() ?: return null
        val end = endTime?.toInstant()   ?: return null
        return Duration.between(start, end)
    }

    fun getTimeDiff(now: Timestamp): Float {
        val start = startTime?.toInstant() ?: return 0f
        val end = endTime?.toInstant() ?: return 0f
        val completed = now.toInstant()
        val spentMillis = Duration.between(start, completed).toMillis().toFloat()
        val totalTaskMillis = Duration.between(start, end).toMillis().toFloat()
        if (totalTaskMillis <= 0f) return 1f
        val ratio = (spentMillis / totalTaskMillis).coerceIn(0f, 1f)
        return round(ratio * 10) / 10f
    }

    fun createNextOccurrence(): Task? {
        if (!isRecurring) return null
        val startDate = startTime?.toDate() ?: return null
        val endDate = endTime?.toDate()
        val zoneId = ZoneId.systemDefault()
        val startZdt = startDate.toInstant().atZone(zoneId)

        val nextStartZdt: ZonedDateTime = when (recurrence.type) {
            RecurrenceType.DAILY.name -> startZdt.plusDays(recurrence.interval.toLong())
            RecurrenceType.WEEKLY.name -> startZdt.plusWeeks(recurrence.interval.toLong())
            RecurrenceType.MONTHLY.name -> startZdt.plusMonths(recurrence.interval.toLong())
            RecurrenceType.YEARLY.name -> startZdt.plusYears(recurrence.interval.toLong())
            RecurrenceType.SELECTED_DAYS.name -> {
                if (recurrence.selectedDays.isEmpty()) return null
                val currentDayOfWeek = startZdt.dayOfWeek.value
                val sortedDays = recurrence.selectedDays.sorted()
                val nextDay = sortedDays.firstOrNull { it > currentDayOfWeek }
                val daysToAdd = if (nextDay != null) nextDay - currentDayOfWeek
                else (7 - currentDayOfWeek) + sortedDays.first()
                startZdt.plusDays(daysToAdd.toLong())
            }
            else -> return null
        }

        val newStartTime = Timestamp(Date.from(nextStartZdt.toInstant()))
        val newEndTime = endDate?.let {
            val endZdt = it.toInstant().atZone(zoneId)
            val durationMs = ChronoUnit.MILLIS.between(startZdt, endZdt)
            Timestamp(Date.from(nextStartZdt.plus(durationMs, ChronoUnit.MILLIS).toInstant()))
        }

        return this.copy(
            id = "",
            startTime = newStartTime,
            endTime = newEndTime,
            isCompleted = false,
            completedAt = null,
            xpAwarded = 0
        )
    }
}

// ── CategoryStats model ───────────────────────────────────────────
//
// Storage in: users/{uid}/categories/{categoryName}
//
data class CategoryStats(
    val level              : Int = 1,
    val exp                : Int = 0,
    val completedTasksCount: Int = 0
)

// ── TaskRepository ────────────────────────────────────────────────
//
// New Firestore Structure:
//
//   users/{uid}
//     ├── completedTasksCount: Int
//     └── categories/{categoryName}
//           ├── active/{taskId}
//           ├── completed/{taskId}
//           └── deleted/{taskId}
//
class TaskRepository {
    private val db = FirebaseFirestore.getInstance()

    // ── Path Helpers ─────────────────────────────────────────

    private fun categoryDoc(uid: String, category: String) =
        db.collection("users").document(uid)
            .collection("categories").document(category)

    private fun activeColl(uid: String, category: String) =
        categoryDoc(uid, category).collection("active")

    private fun completedColl(uid: String, category: String) =
        categoryDoc(uid, category).collection("completed")

    private fun deletedColl(uid: String, category: String) =
        categoryDoc(uid, category).collection("deleted")

    // ── Loading tasks ──────────────────────────────────────────

    fun getTasks(
        uid      : String,
        onSuccess: (List<Task>) -> Unit,
        onFailure: (Exception)  -> Unit
    ) {
        val categories = TaskCategory.entries.map { it.name }
        val allTasks   = mutableListOf<Task>()
        var remaining  = categories.size * 2
        if (categories.isEmpty()) { onSuccess(emptyList()); return }
        for (cat in categories) {
            activeColl(uid, cat).get()
                .addOnSuccessListener { docs ->
                    synchronized(allTasks) {
                        val tasks = docs.mapNotNull { doc ->
                            doc.toObject(Task::class.java)?.copy(id = doc.id)  // ← fix
                        }
                        allTasks += tasks
                        if (--remaining == 0) onSuccess(allTasks)
                    }
                }
                .addOnFailureListener {
                    synchronized(allTasks) {
                        if (--remaining == 0) onSuccess(allTasks)
                    }
                }
            completedColl(uid, cat).get()
                .addOnSuccessListener { docs ->
                    synchronized(allTasks) {
                        val tasks = docs.mapNotNull { doc ->
                            doc.toObject(Task::class.java)?.copy(isCompleted = true, id = doc.id)  // ← fix
                        }
                        allTasks += tasks
                        if (--remaining == 0) onSuccess(allTasks)
                    }
                }
                .addOnFailureListener {
                    synchronized(allTasks) {
                        if (--remaining == 0) onSuccess(allTasks)
                    }
                }
        }
    }

    fun getTasksByCategory(
        uid      : String,
        category : String,
        onSuccess: (List<Task>) -> Unit,
        onFailure: (Exception)  -> Unit
    ) {
        activeColl(uid, category).get()
            .addOnSuccessListener { docs ->
                onSuccess(docs.mapNotNull { doc ->
                    doc.toObject(Task::class.java)?.copy(id = doc.id)  // ← fix
                })
            }
            .addOnFailureListener(onFailure)
    }

    fun getCategoryStats(
        uid      : String,
        category : String,
        onSuccess: (CategoryStats) -> Unit,
        onFailure: (Exception)     -> Unit
    ) {
        categoryDoc(uid, category).get()
            .addOnSuccessListener { doc ->
                onSuccess(doc.toObject(CategoryStats::class.java) ?: CategoryStats())
            }
            .addOnFailureListener(onFailure)
    }

    // ── Add Task ─────────────────────────────────────────

    fun addTask(task: Task, onComplete: (Boolean) -> Unit) {
        if (task.uid.isEmpty() || task.category.isEmpty()) { onComplete(false); return }

        val catKey  = TaskCategory.entries.firstOrNull { it.label == task.category }?.name
            ?: task.category
        val taskRef = activeColl(task.uid, catKey).document()
        val taskWithId = task.copy(id = taskRef.id)

        val catRef = categoryDoc(task.uid, catKey)
        db.runTransaction { tx ->
            val snap = tx.get(catRef)
            if (!snap.exists()) tx.set(catRef, CategoryStats())
            tx.set(taskRef, taskWithId)
        }.addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // ── Delete Task  (soft-delete to deleted collection) ─────────

    fun deleteTask(
        uid      : String,
        taskId   : String,
        category : String,
        onComplete: (Boolean) -> Unit
    ) {
        val catKey    = TaskCategory.entries.firstOrNull { it.label == category }?.name ?: category
        val activeRef = activeColl(uid, catKey).document(taskId)
        val backupRef = deletedColl(uid, catKey).document(taskId)

        activeRef.get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val task = doc.toObject(Task::class.java)
                if (task == null) { onComplete(false); return@addOnSuccessListener }
                val batch = db.batch()
                batch.set(backupRef, task)
                batch.delete(activeRef)
                batch.commit().addOnCompleteListener { onComplete(it.isSuccessful) }
            } else {
                val completedRef = completedColl(uid, catKey).document(taskId)
                completedRef.get().addOnSuccessListener { completedDoc ->
                    val task = completedDoc.toObject(Task::class.java)
                    if (task == null) { onComplete(false); return@addOnSuccessListener }
                    val batch = db.batch()
                    batch.set(backupRef, task)
                    batch.delete(completedRef)
                    batch.commit().addOnCompleteListener { onComplete(it.isSuccessful) }
                }.addOnFailureListener { onComplete(false) }
            }
        }.addOnFailureListener { onComplete(false) }
    }

    // ── Completing Task ─────────────────────────────────────────

    fun completeTask(
        uid       : String,
        taskId    : String,
        category  : String,
        onComplete: (Boolean, Int) -> Unit
    ) {
        if (taskId.isBlank()) { onComplete(false, 0); return }

        val catKey       = TaskCategory.entries.firstOrNull { it.label == category }?.name ?: category
        val activeRef    = activeColl(uid, catKey).document(taskId)
        val completedRef = completedColl(uid, catKey).document(taskId)
        val catRef       = categoryDoc(uid, catKey)
        val userRef      = db.collection("users").document(uid)

        db.runTransaction { tx ->
            val taskSnap = tx.get(activeRef)
            val catSnap  = tx.get(catRef)
            val userSnap = tx.get(userRef)

            val task  = taskSnap.toObject(Task::class.java) ?: throw Exception("Task not found")
            val stats = catSnap.toObject(CategoryStats::class.java) ?: CategoryStats()

            val now          = Timestamp.now()
            val calculatedXp = task.calculateXp(now)
            val completedTask = task.copy(
                isCompleted = true,
                completedAt = now,
                xpAwarded   = calculatedXp
            )

            // 1. Move Task: active to completed
            tx.set(completedRef, completedTask)
            tx.delete(activeRef)

            // 2. Update category stats
            val newExp   = stats.exp + calculatedXp
            val newLevel = computeLevel(newExp)
            tx.set(catRef, CategoryStats(
                level               = newLevel,
                exp                 = newExp,
                completedTasksCount = stats.completedTasksCount + 1
            ))

            // 3. Update global tasks data
            val currentCount = userSnap.getLong("completedTasksCount")?.toInt() ?: 0
            tx.update(userRef, "completedTasksCount", currentCount + 1)

            calculatedXp
        }
            .addOnSuccessListener { xp -> onComplete(true, xp as Int) }
            .addOnFailureListener  { onComplete(false, 0) }
    }

    // ── Recursion─────────────────────────

    fun scheduleNextOccurrence(uid: String, task: Task, onComplete: (Boolean) -> Unit) {
        val next = task.createNextOccurrence() ?: run { onComplete(false); return }
        addTask(next.copy(uid = uid), onComplete)
    }

    // ── Private Helpers ──────────────────────────────────────────

    private fun computeLevel(totalExp: Int): Int {
        var lvl  = 1
        var left = totalExp
        while (left >= lvl * 100) { left -= lvl * 100; lvl++ }
        return lvl
    }
}

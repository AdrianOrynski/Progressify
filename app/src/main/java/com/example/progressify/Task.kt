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
        val end = endTime?.toInstant() ?: return null

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

                val daysToAdd = if (nextDay != null) {
                    nextDay - currentDayOfWeek
                } else {
                    (7 - currentDayOfWeek) + sortedDays.first()
                }

                startZdt.plusDays(daysToAdd.toLong())
            }
            else -> return null
        }

        val newStartTime = Timestamp(Date.from(nextStartZdt.toInstant()))
        var newEndTime: Timestamp? = null

        if (endDate != null) {
            val endZdt = endDate.toInstant().atZone(zoneId)
            val durationMillis = ChronoUnit.MILLIS.between(startZdt, endZdt)
            val nextEndZdt = nextStartZdt.plus(durationMillis, ChronoUnit.MILLIS)
            newEndTime = Timestamp(Date.from(nextEndZdt.toInstant()))
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

// ── Repository ───────────────────────────────────────────────────
class TaskRepository {
    private val db = FirebaseFirestore.getInstance()

    private fun getUserTasksCollection(uid: String) =
        db.collection("users").document(uid).collection("tasks")

    fun getTasks(uid: String, onSuccess: (List<Task>) -> Unit, onFailure: (Exception) -> Unit) {
        getUserTasksCollection(uid)
            .get()
            .addOnSuccessListener { documents ->
                val tasks = documents.mapNotNull { it.toObject(Task::class.java) }
                onSuccess(tasks)
            }
            .addOnFailureListener { onFailure(it) }
    }

    fun addTask(task: Task, onComplete: (Boolean) -> Unit) {
        if (task.uid.isEmpty()) {
            onComplete(false)
            return
        }

        val taskRef = getUserTasksCollection(task.uid).document()
        val taskWithId = task.copy(id = taskRef.id)

        taskRef.set(taskWithId)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun deleteTask(uid: String, taskId: String, onComplete: (Boolean) -> Unit) {
        getUserTasksCollection(uid).document(taskId).delete()
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun completeTask(uid: String, taskId: String, onComplete: (Boolean, Int) -> Unit) {
        if (taskId.isBlank()) {
            onComplete(false, 0)
            return
        }

        val taskRef = getUserTasksCollection(uid).document(taskId)

        taskRef.get().addOnSuccessListener { document ->
            val task =
                document.toObject(Task::class.java) ?: return@addOnSuccessListener onComplete(
                    false,
                    0
                )

            val batch = db.batch()
            val now = Timestamp.now()
            val calculatedXp = task.calculateXp(now)
            batch.update(
                taskRef, mapOf(
                    "isCompleted" to true,
                    "completedAt" to now,
                    "xpAwarded" to calculatedXp
                )
            )

            try {
                val nextTask = task.createNextOccurrence()
                if (nextTask != null) {
                    val nextTaskRef = getUserTasksCollection(uid).document()
                    batch.set(nextTaskRef, nextTask.copy(id = nextTaskRef.id, uid = uid))
                }
            } catch (e: Exception) { /* log error */
            }

            batch.commit().addOnCompleteListener {
                onComplete(it.isSuccessful, if (it.isSuccessful) calculatedXp else 0)
            }
        }.addOnFailureListener {
            onComplete(false, 0)
        }
    }
}
package com.example.progressify

import com.google.firebase.Timestamp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Unit tests for the Task model.
 */
class TaskTest {

    // ── Helpers — create ready Task objects ─────────────────────────

    private fun overdueTask() = Task(
        title       = "Overdue task",
        endTime     = Timestamp(Date(System.currentTimeMillis() - 3_600_000)), // 1h ago
        isCompleted = false
    )

    private fun futureTask() = Task(
        title       = "Future task",
        endTime     = Timestamp(Date(System.currentTimeMillis() + 3_600_000)), // 1h in the future
        isCompleted = false
    )

    private fun completedOverdueTask() = Task(
        title       = "Done task",
        endTime     = Timestamp(Date(System.currentTimeMillis() - 3_600_000)), // deadline passed
        isCompleted = true                                                       // but completed
    )

    // ── isOverdue tests ──────────────────────────────────────────────

    @Test
    fun `isOverdue is true when deadline passed and task not completed`() {
        // Given: task with a passed deadline, not completed
        val task = overdueTask()

        // Then: should be overdue
        assertTrue(task.isOverdue)
    }

    @Test
    fun `isOverdue is false when task is completed even if deadline passed`() {
        // Given: task with a passed deadline, but already completed
        val task = completedOverdueTask()

        // Then: a completed task is never overdue
        assertFalse(task.isOverdue)
    }

    @Test
    fun `isOverdue is false when deadline is in the future`() {
        // Given: task with a future deadline
        val task = futureTask()

        // Then: not overdue
        assertFalse(task.isOverdue)
    }

    @Test
    fun `isOverdue is false when task has no endTime`() {
        // Given: task with no end time set
        val task = Task(title = "No deadline", endTime = null, isCompleted = false)

        // Then: without a deadline it cannot be overdue
        assertFalse(task.isOverdue)
    }

    // ── isRecurring tests ────────────────────────────────────────────

    @Test
    fun `isRecurring is true for DAILY recurrence`() {
        // Given: task with daily recurrence
        val task = Task(
            title      = "Daily task",
            recurrence = RecurrenceRule(type = RecurrenceType.DAILY.name)
        )

        // Then: should be marked as recurring
        assertTrue(task.isRecurring)
    }

    @Test
    fun `isRecurring is true for WEEKLY recurrence`() {
        val task = Task(
            title      = "Weekly task",
            recurrence = RecurrenceRule(type = RecurrenceType.WEEKLY.name)
        )
        assertTrue(task.isRecurring)
    }

    @Test
    fun `isRecurring is false when recurrence type is NONE`() {
        // Given: task without recurrence (default)
        val task = Task(
            title      = "One-time task",
            recurrence = RecurrenceRule(type = RecurrenceType.NONE.name)
        )

        // Then: not recurring
        assertFalse(task.isRecurring)
    }

    @Test
    fun `isRecurring is false by default`() {
        // Given: task created without specifying recurrence
        val task = Task(title = "Default task")

        // Then: not recurring by default
        assertFalse(task.isRecurring)
    }

    @Test
    fun `isRecurring is true for SELECTED_DAYS recurrence`() {
        val task = Task(
            recurrence = RecurrenceRule(
                type         = RecurrenceType.SELECTED_DAYS.name,
                selectedDays = listOf(1, 3, 5)
            )
        )
        assertTrue(task.isRecurring)
    }

    @Test
    fun `isRecurring is true for MONTHLY recurrence`() {
        val task = Task(recurrence = RecurrenceRule(type = RecurrenceType.MONTHLY.name))
        assertTrue(task.isRecurring)
    }

    @Test
    fun `isRecurring is true for YEARLY recurrence`() {
        val task = Task(recurrence = RecurrenceRule(type = RecurrenceType.YEARLY.name))
        assertTrue(task.isRecurring)
    }

    // ── Edge cases ──────────────────────────────────────────────────

    @Test
    fun `isOverdue is false when completed and no endTime`() {
        val task = Task(isCompleted = true, endTime = null)
        assertFalse(task.isOverdue)
    }

    @Test
    fun `isOverdue is false when not completed and no endTime`() {
        val task = Task(isCompleted = false, endTime = null)
        assertFalse(task.isOverdue)
    }
}

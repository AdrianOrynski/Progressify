package com.example.progressify

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.progressify.screens.TaskCard
import com.example.progressify.ui.theme.ProgressifyTheme
import com.google.firebase.Timestamp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * UI tests for TaskCard — delete dialog and card states.
 */
@RunWith(AndroidJUnit4::class)
class TaskCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Sample task used in tests
    private val sampleTask = Task(
        id          = "test-1",
        title       = "Slay the dragon",
        description = "A mighty quest",
        category    = TaskCategory.TASKFORGE.label,
        difficulty  = Difficulty.HARD.name,
        startTime   = Timestamp(Date(System.currentTimeMillis() + 3_600_000)),
        endTime     = Timestamp(Date(System.currentTimeMillis() + 7_200_000)),
        isCompleted = false
    )

    private val completedTask = sampleTask.copy(isCompleted = true)

    // ── Delete confirmation dialog ───────────────────────────────────

    @Test
    fun deleteButton_showsConfirmationDialog() {
        // Given: task card with delete button enabled
        composeTestRule.setContent {
            ProgressifyTheme {
                TaskCard(
                    task       = sampleTask,
                    onComplete = {},
                    onDelete   = {},
                    showDelete = true
                )
            }
        }

        // When: clicking the trash icon
        composeTestRule.onNodeWithContentDescription("Delete").performClick()

        // Then: dialog with a question appears
        composeTestRule.onNodeWithText("Abandon Bounty?").assertIsDisplayed()
        composeTestRule.onNodeWithText("\"Slay the dragon\" will be lost forever.").assertIsDisplayed()
    }

    @Test
    fun keepItButton_dismissesDialog_withoutCallingOnDelete() {
        // Given: delete confirmation dialog is open
        var deleteWasCalled = false

        composeTestRule.setContent {
            ProgressifyTheme {
                TaskCard(
                    task       = sampleTask,
                    onComplete = {},
                    onDelete   = { deleteWasCalled = true },
                    showDelete = true
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        composeTestRule.onNodeWithText("Abandon Bounty?").assertIsDisplayed()

        // When: clicking KEEP IT
        composeTestRule.onNodeWithText("KEEP IT").performClick()

        // Then: dialog disappears and onDelete was NOT called
        composeTestRule.onNodeWithText("Abandon Bounty?").assertDoesNotExist()
        assertFalse(deleteWasCalled)
    }

    @Test
    fun abandonButton_callsOnDelete() {
        // Given: delete confirmation dialog is open
        var deleteWasCalled = false

        composeTestRule.setContent {
            ProgressifyTheme {
                TaskCard(
                    task       = sampleTask,
                    onComplete = {},
                    onDelete   = { deleteWasCalled = true },
                    showDelete = true
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Delete").performClick()

        // When: clicking ABANDON
        composeTestRule.onNodeWithText("ABANDON").performClick()

        // Then: onDelete was called
        assertTrue(deleteWasCalled)
    }

    // ── Delete button hidden when showDelete = false ─────────────────

    @Test
    fun deleteButton_notVisible_whenShowDeleteIsFalse() {
        // Given: card without delete button (e.g. on Dashboard)
        composeTestRule.setContent {
            ProgressifyTheme {
                TaskCard(
                    task       = sampleTask,
                    onComplete = {},
                    onDelete   = {},
                    showDelete = false
                )
            }
        }

        // Then: trash icon does not exist on screen
        composeTestRule.onNodeWithContentDescription("Delete").assertDoesNotExist()
    }

    // ── Completed task states ────────────────────────────────────────

    @Test
    fun completedTask_showsTaskTitle_withCorrectState() {
        // Given: completed task card
        composeTestRule.setContent {
            ProgressifyTheme {
                TaskCard(
                    task       = completedTask,
                    onComplete = {},
                    onDelete   = {},
                    showDelete = true
                )
            }
        }

        // Then: title is visible
        composeTestRule.onNodeWithText("Slay the dragon").assertIsDisplayed()
    }

    @Test
    fun taskTitle_isDisplayed() {
        // Given: regular task card
        composeTestRule.setContent {
            ProgressifyTheme {
                TaskCard(
                    task       = sampleTask,
                    onComplete = {},
                    onDelete   = {},
                    showDelete = true
                )
            }
        }

        // Then: title visible
        composeTestRule.onNodeWithText("Slay the dragon").assertIsDisplayed()
    }

    @Test
    fun overdueTask_showsOverdueText() {
        // Given: task with a passed deadline
        val overdueTask = Task(
            title       = "Late quest",
            startTime   = Timestamp(Date(System.currentTimeMillis() - 7_200_000)),
            endTime     = Timestamp(Date(System.currentTimeMillis() - 3_600_000)),
            isCompleted = false
        )
        composeTestRule.setContent {
            ProgressifyTheme {
                TaskCard(task = overdueTask, onComplete = {}, onDelete = {}, showDelete = false)
            }
        }

        // Then: "OVERDUE" text is visible
        composeTestRule.onNodeWithText("OVERDUE", substring = true).assertIsDisplayed()
    }

    @Test
    fun completedTask_showsCompletedIcon_andHidesCompleteButton() {
        // Given: completed task card
        composeTestRule.setContent {
            ProgressifyTheme {
                TaskCard(task = completedTask, onComplete = {}, onDelete = {}, showDelete = false)
            }
        }

        // Then: "Completed" icon visible, "Complete task" button does not exist
        composeTestRule.onNodeWithContentDescription("Completed").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Complete task").assertDoesNotExist()
    }

    @Test
    fun completeButton_callsOnComplete() {
        // Given: incomplete task card
        var completeWasCalled = false

        composeTestRule.setContent {
            ProgressifyTheme {
                TaskCard(
                    task       = sampleTask,
                    onComplete = { completeWasCalled = true },
                    onDelete   = {},
                    showDelete = true
                )
            }
        }

        // When: clicking the complete button
        composeTestRule.onNodeWithContentDescription("Complete task").performClick()

        // Then: onComplete was called
        assertTrue(completeWasCalled)
    }
}

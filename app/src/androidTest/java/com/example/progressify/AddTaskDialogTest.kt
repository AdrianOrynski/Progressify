package com.example.progressify

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.progressify.screens.AddTaskDialog
import com.example.progressify.ui.theme.ProgressifyTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for AddTaskDialog — form validation.
 *
 * Require an emulator or connected device.
 * Run > Run 'AddTaskDialogTest' in Android Studio.
 */
@RunWith(AndroidJUnit4::class)
class AddTaskDialogTest {

    // composeTestRule is the test engine — renders Compose and allows
    // finding elements on screen and clicking them
    @get:Rule
    val composeTestRule = createComposeRule()

    // Helper — renders the dialog with default (empty) settings
    private fun showDialog() {
        composeTestRule.setContent {
            ProgressifyTheme {
                AddTaskDialog(
                    skillPoints = 0,
                    heroClasses = emptyList(),
                    onDismiss   = {},
                    onConfirm   = { _, _, _, _, _, _, _, _, _ -> }
                )
            }
        }
    }

    // ── Title validation ─────────────────────────────────────────────

    @Test
    fun confirmWithoutTitle_showsTitleError() {
        // Given: dialog is displayed, title empty
        showDialog()

        // When: clicking CONFIRM without entering a title
        composeTestRule.onNodeWithText("CONFIRM").performClick()

        // Then: error message appears
        composeTestRule.onNodeWithText("Title is required").assertIsDisplayed()
    }

    @Test
    fun typingTitle_clearsTitleError() {
        // Given: first we trigger the title error
        showDialog()
        composeTestRule.onNodeWithText("CONFIRM").performClick()
        composeTestRule.onNodeWithText("Title is required").assertIsDisplayed()

        // When: typing a title
        composeTestRule.onNodeWithText("Task Title *").performTextInput("My task")

        // Then: error disappears
        composeTestRule.onNodeWithText("Title is required").assertDoesNotExist()
    }

    // ── Category validation ──────────────────────────────────────────

    @Test
    fun confirmWithoutCategory_showsCategoryError() {
        // Given: dialog is displayed, category not selected
        showDialog()

        // When: clicking CONFIRM
        composeTestRule.onNodeWithText("CONFIRM").performClick()

        // Then: category error message appears
        composeTestRule.onNodeWithText("Category is required").assertIsDisplayed()
    }

    @Test
    fun selectingCategory_clearsCategoryError() {
        // Given: category error is visible
        showDialog()
        composeTestRule.onNodeWithText("CONFIRM").performClick()
        composeTestRule.onNodeWithText("Category is required").assertIsDisplayed()

        // When: selecting any category
        composeTestRule.onNodeWithText("Spellcraft").performClick()

        // Then: error disappears
        composeTestRule.onNodeWithText("Category is required").assertDoesNotExist()
    }

    // ── Time validation ──────────────────────────────────────────────

    @Test
    fun confirmWithoutTime_showsTimeError() {
        // Given: dialog is displayed, time not set
        showDialog()

        // When: clicking CONFIRM
        composeTestRule.onNodeWithText("CONFIRM").performClick()

        // Then: time error message appears
        composeTestRule.onNodeWithText("Set start and end date & time").assertIsDisplayed()
    }

    // ── Multiple errors at once ──────────────────────────────────────

    @Test
    fun confirmWithEmptyForm_showsAllErrors() {
        // Given: dialog is displayed, nothing filled in
        showDialog()

        // When: clicking CONFIRM
        composeTestRule.onNodeWithText("CONFIRM").performClick()

        // Then: all errors at once
        composeTestRule.onNodeWithText("Title is required").assertIsDisplayed()
        composeTestRule.onNodeWithText("Category is required").assertIsDisplayed()
        composeTestRule.onNodeWithText("Set start and end date & time").assertIsDisplayed()
    }

    // ── Error independence ───────────────────────────────────────────

    @Test
    fun fixingTitle_doesNotClearCategoryError() {
        // Given: clicking CONFIRM — both errors appear
        showDialog()
        composeTestRule.onNodeWithText("CONFIRM").performClick()
        composeTestRule.onNodeWithText("Title is required").assertIsDisplayed()
        composeTestRule.onNodeWithText("Category is required").assertIsDisplayed()

        // When: typing a title (fixing only the title)
        composeTestRule.onNodeWithText("Task Title *").performTextInput("My task")

        // Then: title error disappears, but category error remains
        composeTestRule.onNodeWithText("Title is required").assertDoesNotExist()
        composeTestRule.onNodeWithText("Category is required").assertIsDisplayed()
    }

    @Test
    fun categoryChip_worksAsToggle_restoringErrorOnDeselect() {
        // Given: triggering the category error
        showDialog()
        composeTestRule.onNodeWithText("CONFIRM").performClick()
        composeTestRule.onNodeWithText("Category is required").assertIsDisplayed()

        // When: selecting a category — error disappears
        composeTestRule.onNodeWithText("Spellcraft").performClick()
        composeTestRule.onNodeWithText("Category is required").assertDoesNotExist()

        // When: deselecting the same category (chip acts as a toggle)
        composeTestRule.onNodeWithText("Spellcraft").performClick()

        // Then: after clicking CONFIRM again, error returns
        composeTestRule.onNodeWithText("CONFIRM").performClick()
        composeTestRule.onNodeWithText("Category is required").assertIsDisplayed()
    }

    // ── Cancel button ────────────────────────────────────────────────

    @Test
    fun cancelButton_doesNotShowErrors() {
        // Given: dialog is displayed
        showDialog()

        // When: clicking CANCEL (not CONFIRM)
        composeTestRule.onNodeWithText("CANCEL").performClick()

        // Then: no errors
        composeTestRule.onNodeWithText("Title is required").assertDoesNotExist()
        composeTestRule.onNodeWithText("Category is required").assertDoesNotExist()
    }
}

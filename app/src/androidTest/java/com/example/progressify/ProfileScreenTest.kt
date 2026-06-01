package com.example.progressify

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.progressify.screens.ProfileScreen
import com.example.progressify.ui.theme.ProgressifyTheme
import com.example.progressify.viewmodel.TaskViewModel
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for ProfileScreen — logout dialog.
 *
 * ViewModel is created with a real context, but without
 * a logged-in user — Firebase will not execute any queries.
 */
@RunWith(AndroidJUnit4::class)
class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // Minimal test user
    private val testUser = User(
        uid      = "test-uid",
        nickname = "TestHero",
        name     = "Jan",
        surname  = "Kowalski"
    )

    // ViewModel with default state (uid empty → loadTasks() does nothing)
    private val viewModel: TaskViewModel by lazy {
        TaskViewModel(ApplicationProvider.getApplicationContext<Application>())
    }

    private fun showProfileScreen(onLogout: () -> Unit = {}) {
        composeTestRule.setContent {
            ProgressifyTheme {
                ProfileScreen(
                    user              = testUser,
                    taskViewModel     = viewModel,
                    onLogout          = onLogout,
                    onNavigateToStats = {}
                )
            }
        }
    }

    // ── Logout dialog ────────────────────────────────────────────────

    @Test
    fun leaveTavernButton_showsLogoutDialog() {
        // Given: profile screen is displayed
        showProfileScreen()

        // When: clicking "LEAVE TAVERN"
        composeTestRule.onNodeWithText("LEAVE TAVERN").performScrollTo().performClick()

        // Then: confirmation dialog appears
        composeTestRule.onNodeWithText("Leave the Tavern?").assertIsDisplayed()
    }

    @Test
    fun stayButton_dismissesDialog_withoutLoggingOut() {
        // Given: logout dialog is open
        var logoutWasCalled = false
        showProfileScreen(onLogout = { logoutWasCalled = true })

        composeTestRule.onNodeWithText("LEAVE TAVERN").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Leave the Tavern?").assertIsDisplayed()

        // When: clicking STAY
        composeTestRule.onNodeWithText("STAY").performClick()

        // Then: dialog disappears and logout did NOT happen
        composeTestRule.onNodeWithText("Leave the Tavern?").assertDoesNotExist()
        assertFalse(logoutWasCalled)
    }

    @Test
    fun leaveButton_callsOnLogout() {
        // Given: logout dialog is open
        var logoutWasCalled = false
        showProfileScreen(onLogout = { logoutWasCalled = true })

        composeTestRule.onNodeWithText("LEAVE TAVERN").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Leave the Tavern?").assertIsDisplayed() // wait for dialog to appear

        // When: clicking LEAVE
        composeTestRule.onNodeWithText("LEAVE").performClick()

        // Then: onLogout was called
        assertTrue(logoutWasCalled)
    }

    // ── Navigation ───────────────────────────────────────────────────

    @Test
    fun statisticsButton_callsOnNavigateToStats() {
        // Given: profile screen
        var navigated = false
        composeTestRule.setContent {
            ProgressifyTheme {
                ProfileScreen(
                    user              = testUser,
                    taskViewModel     = viewModel,
                    onLogout          = {},
                    onNavigateToStats = { navigated = true }
                )
            }
        }

        // When: clicking STATISTICS (emoji prefix, off-screen — scroll + click)
        composeTestRule.onNodeWithText("STATISTICS", substring = true)
            .performScrollTo()
            .performClick()

        // Then: navigation callback was called
        assertTrue(navigated)
    }

    // ── Basic screen content ─────────────────────────────────────────

    @Test
    fun profileScreen_displaysNickname() {
        // Given / When: profile screen with user
        showProfileScreen()

        // Then: user nickname is visible (uppercase)
        composeTestRule.onNodeWithText("TESTHERO").assertIsDisplayed()
    }

    @Test
    fun profileScreen_displaysSections() {
        // Given / When: profile screen
        showProfileScreen()

        // "🛡️ HERO SHEET" — emoji prefix, using substring
        composeTestRule.onNodeWithText("HERO SHEET", substring = true).assertIsDisplayed()
        // Sections below are off-screen (scrollable column) — checking they exist in the tree
        composeTestRule.onNodeWithText("DAILY VOW").assertExists()
        composeTestRule.onNodeWithText("CHRONICLES").assertExists()
    }
}

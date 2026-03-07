package com.sarmaya.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.sarmaya.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testBottomNavigationAndPagerSync() {
        // App starts on Dashboard
        composeTestRule.onNodeWithText("Portfolio Flow").assertIsDisplayed()

        // Click on Holdings Tab
        composeTestRule.onNodeWithText("Holdings").performClick()
        composeTestRule.onNodeWithText("Active Positions").assertIsDisplayed()

        // Click on Transactions Tab
        composeTestRule.onNodeWithText("Transactions").performClick()
        composeTestRule.onNodeWithText("Log Transaction").assertDoesNotExist() // Ensure sheet is not open by default

        // Click on Settings Tab
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Theme Preference").assertIsDisplayed()

        // Swipe back to Transactions
        composeTestRule.onRoot().performTouchInput { swipeRight() }
        // Verify we are back on Transactions
        composeTestRule.onNodeWithText("Transaction History", substring = true).assertExists()
    }
    
    @Test
    fun testFabOpensAddTransactionSheet() {
        // Ensure on Dashboard
        composeTestRule.onNodeWithText("Dashboard").performClick()
        
        // Find and click FAB (Content Description: Track New Stock)
        composeTestRule.onNodeWithContentDescription("Track New Stock").performClick()
        
        // Verify sheet opens
        composeTestRule.onNodeWithText("Log Transaction").assertIsDisplayed()
        composeTestRule.onNodeWithText("Save Transaction").assertIsDisplayed()
    }
}

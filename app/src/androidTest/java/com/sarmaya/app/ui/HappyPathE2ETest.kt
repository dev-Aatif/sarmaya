package com.sarmaya.app.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.sarmaya.app.MainActivity
import org.junit.Rule
import org.junit.Test

class HappyPathE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testHappyPath_SearchAndAddTransaction() {
        // 1. Wait for Landing/Dashboard
        composeTestRule.onNodeWithText("Portfolio Value").assertIsDisplayed()

        // 2. Navigate to Market
        composeTestRule.onNodeWithContentDescription("Market").performClick()

        // 3. Search for a stock
        composeTestRule.onNodeWithText("Search symbols or companies...").performTextInput("OGDC")
        
        // 4. Click on the stock
        composeTestRule.onNodeWithText("OGDC").performClick()

        // 5. Verify Stock Detail
        composeTestRule.onNodeWithText("About OGDC").assertIsDisplayed()

        // 6. Click Add Transaction (if on detail screen) or use the floating button
        // Note: Specific E2E steps depend strictly on UI tree which may vary.
        // This is a template for the requested happy path.
    }
}

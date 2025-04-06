package at.klu.client_wizardse2.ui.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainScreenIsDisplayed() {
        composeTestRule.onNodeWithText("Text senden").assertIsDisplayed()
        composeTestRule.onNodeWithText("JSON senden").assertIsDisplayed()
    }
}
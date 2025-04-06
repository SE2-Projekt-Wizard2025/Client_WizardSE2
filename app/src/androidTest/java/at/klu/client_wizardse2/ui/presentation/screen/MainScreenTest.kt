package at.klu.client_wizardse2.ui.presentation.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun buttonsAreDisplayed() {
        composeTestRule.setContent {
            ActionButtons(onTextSend = {}, onJsonSend = {})
        }

        composeTestRule.onNodeWithText("Text senden").assertIsDisplayed()
        composeTestRule.onNodeWithText("JSON senden").assertIsDisplayed()
    }

    @Test
    fun clickingButtonsCallsCallbacks() {
        var textClicked = false
        var jsonClicked = false

        composeTestRule.setContent {
            ActionButtons(
                onTextSend = { textClicked = true },
                onJsonSend = { jsonClicked = true }
            )
        }

        composeTestRule.onNodeWithText("Text senden").performClick()
        assert(textClicked)

        composeTestRule.onNodeWithText("JSON senden").performClick()
        assert(jsonClicked)
    }

    @Test
    fun messagesAreShownInList() {
        val messages = listOf("First", "Second")

        composeTestRule.setContent {
            MessageList(messages = messages)
        }

        composeTestRule.onNodeWithText("First").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second").assertIsDisplayed()
    }
}

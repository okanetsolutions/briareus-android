package com.okanetsolutions.briareus.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class PairingTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun refusesHttpBeforeSendingCredentials() {
        compose.onNodeWithText("Connect").assertIsNotEnabled()
        compose.onNode(hasSetTextAction() and hasText("Server address")).performTextInput("http://example.invalid")
        compose.onNode(hasSetTextAction() and hasText("Device token")).performTextInput("brm_test")
        compose.onNodeWithText("Connect").performScrollTo().performClick()
        compose.onNodeWithText("Enter an HTTPS server address", substring = true).performScrollTo().assertIsDisplayed()
    }

    @Test fun restoresAddressButDoesNotSaveTokenOnActivityRecreation() {
        compose.onNode(hasSetTextAction() and hasText("Server address")).performTextInput("https://example.invalid")
        compose.onNode(hasSetTextAction() and hasText("Device token")).performTextInput("brm_not_a_real_token")
        compose.activityRule.scenario.recreate()
        compose.onNode(hasSetTextAction() and hasText("Server address")).assertTextContains("https://example.invalid")
        compose.onNodeWithText("Connect").assertIsNotEnabled()
    }
}

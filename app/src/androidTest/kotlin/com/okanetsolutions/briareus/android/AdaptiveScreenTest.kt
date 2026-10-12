package com.okanetsolutions.briareus.android

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.okanetsolutions.briareus.android.ui.BriareusTheme
import com.okanetsolutions.briareus.android.ui.HomeScreen
import com.okanetsolutions.briareus.android.ui.Pane
import com.okanetsolutions.briareus.android.ui.VoiceField
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AdaptiveScreenTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun rendersSidebarOnlyInTabletWindow() {
        val expectedWide = InstrumentationRegistry.getArguments().getString("expectedWide")?.toBooleanStrict()
            ?: error("Pass expectedWide to testInstrumentationRunnerArguments for the chosen device.")
        assertEquals("The emulator must exercise the configured layout", expectedWide, context.resources.configuration.screenWidthDp >= 1024)
        compose.setContent {
            BriareusTheme {
                HomeScreen(
                    context.store, Pane.New(), {}, {}, {}, false, {}, {}, rememberSaveableStateHolder(),
                )
            }
        }
        compose.onNodeWithText("Welcome back").assertIsDisplayed()
        if (expectedWide) {
            compose.onNodeWithText("Briareus").assertIsDisplayed()
            compose.onNodeWithText("← Conversations").assertDoesNotExist()
        } else {
            compose.onNodeWithText("Briareus").assertDoesNotExist()
            compose.onNodeWithText("← Conversations").assertIsDisplayed()
        }
    }

    @Test fun acceptsKeyboardInputWithoutTranscriptionCapability() {
        compose.setContent {
            var draft by rememberSaveable { mutableStateOf("") }
            BriareusTheme { VoiceField(context.store, draft, { draft = it }, Modifier.fillMaxSize(), placeholder = "Message") }
        }
        compose.onNode(hasSetTextAction()).performTextInput("Fix the Android build")
        compose.onNode(hasSetTextAction()).assertTextContains("Fix the Android build")
    }
}

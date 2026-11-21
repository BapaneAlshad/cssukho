package com.sukhoramen.cs

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.sukhoramen.cs.data.DeviceVault
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PinEntryFlowTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val vault by lazy { DeviceVault(InstrumentationRegistry.getInstrumentation().targetContext) }

    @Before fun prepare() {
        vault.clear()
        vault.store("local-test-token", "123456")
    }
    @After fun clean() { vault.clear() }

    @Test fun pinIsRequiredAgainAfterColdStartAndDemoRemainsSeparate() {
        ActivityScenario.launch(MainActivity::class.java).use { first ->
            compose.onNodeWithTag("entry_pin").assertIsDisplayed()
            compose.onNodeWithTag("entry_pin").performTextInput("000000")
            compose.onNodeWithTag("unlock_live").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("PIN salah", substring = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("PIN salah", substring = true).assertExists()
            compose.onNodeWithTag("entry_pin").performTextClearance()
            compose.onNodeWithTag("entry_pin").performTextInput("123456")
            compose.onNodeWithTag("unlock_live").performClick()
            compose.waitUntil(5_000) { compose.onAllNodesWithText("MODE CLOUD").fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("MODE CLOUD").assertIsDisplayed()
            first.close()
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            compose.onNodeWithTag("entry_pin").assertIsDisplayed()
            compose.onNodeWithTag("try_demo").performClick()
            compose.onNodeWithText("MODE DEMO").assertIsDisplayed()
        }
    }
}


package com.sukhoramen.cs

import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.sukhoramen.cs.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import android.view.inputmethod.InputMethodManager
import android.content.Context
import java.io.File
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class InboxFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val repo get() = (compose.activity.application as SukhoApplication).demoRepository

    @Before fun reset() {
        runBlocking { repo.initialize(); repo.reset(); repo.setTheme(ThemeMode.LIGHT) }
        compose.waitUntil(5_000) { repo.state.value.ready }
        compose.onNodeWithTag("try_demo").performClick()
        compose.waitForIdle()
    }

    @Test fun replyCompleteAndReopen() {
        compose.onNodeWithTag("inbox_list").assertIsDisplayed()
        capture("inbox-light")
        compose.onNodeWithTag("inbox_list").performScrollToNode(hasTestTag("thread_ig-dm-nadia"))
        compose.onNodeWithTag("thread_ig-dm-nadia").performClick()
        compose.onNodeWithTag("reply_input").performTextInput("Bisa reservasi, Kak. Untuk jam berapa?")
        compose.onNodeWithTag("send_reply").performClick()
        compose.waitUntil(5_000) { repo.state.value.interactions.first { it.id == "ig-dm-nadia" }.messages.last().status == MessageStatus.SENT }
        hideKeyboard()
        capture("detail-dm")
        compose.onNodeWithTag("detail_messages").performScrollToNode(hasTestTag("toggle_complete"))
        compose.onNodeWithTag("toggle_complete").performClick()
        compose.onNodeWithTag("detail_back").performClick()
        compose.onNodeWithTag("inbox_list").performScrollToNode(hasTestTag("status_done"))
        compose.onNodeWithTag("status_done").performClick()
        compose.onNodeWithTag("inbox_list").performScrollToNode(hasTestTag("thread_ig-dm-nadia"))
        compose.onNodeWithTag("thread_ig-dm-nadia").performClick()
        Thread.sleep(250) // Let the initial jump to the latest reply finish.
        compose.waitForIdle()
        compose.onNodeWithTag("detail_messages").performScrollToNode(hasTestTag("toggle_complete"))
        compose.onNodeWithTag("toggle_complete").performClick()
        compose.waitUntil(5_000) { !repo.state.value.interactions.first { it.id == "ig-dm-nadia" }.completed }
    }

    @Test fun failedReplyCanRetryWithoutDuplicating() {
        runBlocking { repo.setScenario(DemoScenario.SEND_FAILURE, Platform.INSTAGRAM) }
        compose.onNodeWithTag("inbox_list").performScrollToNode(hasTestTag("thread_ig-dm-nadia"))
        compose.onNodeWithTag("thread_ig-dm-nadia").performClick()
        compose.onNodeWithTag("reply_input").performTextInput("Terima kasih, Kak!")
        compose.onNodeWithTag("send_reply").performClick()
        compose.waitUntil(5_000) { repo.state.value.interactions.first { it.id == "ig-dm-nadia" }.messages.last().status == MessageStatus.FAILED }
        val thread = repo.state.value.interactions.first { it.id == "ig-dm-nadia" }
        val message = thread.messages.last()
        hideKeyboard()
        compose.onNodeWithTag("detail_messages").performScrollToNode(hasTestTag("retry_${message.id}"))
        capture("detail-failed")
        compose.onNodeWithTag("retry_${message.id}").performClick()
        compose.waitUntil(5_000) { repo.state.value.interactions.first { it.id == thread.id }.messages.last().status == MessageStatus.SENT }
        assertEquals(thread.messages.size, repo.state.value.interactions.first { it.id == thread.id }.messages.size)
    }

    @Test fun commentShowsPostContextAndOfflineDisablesComposer() {
        compose.onNodeWithTag("nav_comment").performClick()
        compose.onNodeWithTag("inbox_list").performScrollToNode(hasTestTag("thread_tt-comment-rizky"))
        compose.onNodeWithTag("thread_tt-comment-rizky").performClick()
        compose.onNodeWithText("KONTEKS KOMENTAR").assertIsDisplayed()
        compose.onNodeWithText("Video TikTok · Contoh").assertIsDisplayed()
        capture("detail-comment")
        runBlocking { repo.setScenario(DemoScenario.OFFLINE, Platform.TIKTOK) }
        compose.onNodeWithTag("reply_input").assertIsNotEnabled()
        compose.onNodeWithTag("send_reply").assertIsNotEnabled()
        capture("detail-offline")
    }

    @Test fun filtersAndThemeControlsWork() {
        compose.onNodeWithTag("nav_comment").performClick()
        compose.onNodeWithTag("inbox_list").performScrollToNode(hasTestTag("filter_TIKTOK"))
        compose.onNodeWithTag("filter_TIKTOK").performClick()
        compose.onNodeWithTag("thread_ig-dm-nadia").assertDoesNotExist()
        compose.onNodeWithTag("thread_tt-comment-rizky").assertIsDisplayed()
        compose.onNodeWithTag("inbox_list").performScrollToIndex(0)
        compose.onNodeWithTag("nav_settings").performClick()
        capture("settings-light")
        clickScrolledTag("theme_DARK")
        compose.waitUntil(10_000) { repo.state.value.theme == ThemeMode.DARK }
        compose.onNodeWithTag("nav_dm").performClick()
        capture("inbox-dark")
        compose.onNodeWithTag("nav_settings").performClick()
        clickScrolledTag("theme_LIGHT")
        compose.onNodeWithTag("nav_dm").performClick()
        compose.onNodeWithTag("thread_ig-dm-nadia").assertIsDisplayed()
    }

    @Test fun choosingTabFromSettingsAlwaysClosesSettingsAndKeepsTabFilter() {
        compose.onNodeWithTag("nav_comment").performClick()
        compose.onNodeWithTag("inbox_list").performScrollToNode(hasTestTag("filter_TIKTOK"))
        compose.onNodeWithTag("filter_TIKTOK").performClick()
        compose.onNodeWithTag("thread_tt-comment-rizky").assertIsDisplayed()
        compose.onNodeWithTag("thread_ig-comment-dinda").assertDoesNotExist()

        repeat(4) { index ->
            compose.onNodeWithTag("inbox_list").performScrollToIndex(0)
            compose.onNodeWithTag("nav_settings").performClick()
            compose.onNodeWithTag("settings_scroll").assertIsDisplayed()
            val destination = if (index % 2 == 0) "nav_comment" else "nav_dm"
            compose.onNodeWithTag(destination).performClick()
            compose.onNodeWithTag("settings_scroll").assertDoesNotExist()
            if (destination == "nav_comment") {
                compose.onNodeWithText("Komentar terbaru").assertIsDisplayed()
                compose.onNodeWithTag("thread_tt-comment-rizky").assertIsDisplayed()
                compose.onNodeWithTag("thread_ig-comment-dinda").assertDoesNotExist()
            } else {
                compose.onNodeWithText("Pesan terbaru").assertIsDisplayed()
                compose.onNodeWithTag("thread_ig-dm-nadia").assertIsDisplayed()
                compose.onNodeWithTag("thread_tt-comment-rizky").assertDoesNotExist()
            }
        }
    }

    @Test fun systemBackFromSettingsReturnsToTheCommentTab() {
        compose.onNodeWithTag("nav_comment").performClick()
        compose.onNodeWithTag("inbox_list").performScrollToIndex(0)
        compose.onNodeWithTag("nav_settings").performClick()
        compose.onNodeWithTag("settings_scroll").assertIsDisplayed()
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithTag("settings_scroll").assertDoesNotExist()
        compose.onNodeWithText("Komentar terbaru").assertIsDisplayed()
        compose.onNodeWithTag("thread_tt-comment-rizky").assertIsDisplayed()
    }

    @Test fun realDataStoreRoundTripKeepsEmojiDraftThemeAndCompletion() {
        runBlocking {
            repo.sendReply("ig-dm-nadia", "Siap, Kak! 🍜", "codec-test")
            repo.setDraft("tt-dm-arya", "Draft untuk besok 😊")
            repo.setCompleted("ig-dm-nadia", true)
            repo.setTheme(ThemeMode.SYSTEM)
            val saved = PreferenceDemoStorage(compose.activity).load()!!
            val restored = saved.interactions.first { it.id == "ig-dm-nadia" }
            assertTrue(restored.completed)
            assertEquals("Siap, Kak! 🍜", restored.messages.last().text)
            assertEquals(MessageStatus.SENT, restored.messages.last().status)
            assertEquals("Draft untuk besok 😊", saved.drafts["tt-dm-arya"])
            assertEquals(ThemeMode.SYSTEM, saved.theme)
            assertEquals(repo.state.value.interactions, saved.interactions)
        }
    }

    @Test fun fastNativeKeyboardInputKeepsEveryCharacter() {
        val payload = "Halo Kak reservasi bisa"
        compose.onNodeWithTag("inbox_list").performScrollToNode(hasTestTag("thread_ig-dm-nadia"))
        compose.onNodeWithTag("thread_ig-dm-nadia").performClick()
        compose.onNodeWithTag("reply_input").performClick()
        // Native key events require a connected IME, unlike Compose's edit semantics.
        compose.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(compose.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        compose.waitForIdle()
        compose.onNodeWithTag("reply_input").assertIsFocused()
        val command = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("input text Halo%sKak%sreservasi%sbisa")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(command).bufferedReader().use { it.readText() }
        compose.onNodeWithTag("reply_input").assertTextContains(payload)
        compose.onNodeWithTag("send_reply").performClick()
        compose.waitUntil(5_000) {
            repo.state.value.interactions.first { it.id == "ig-dm-nadia" }.messages.last().let {
                it.text == payload && it.status == MessageStatus.SENT
            }
        }
    }

    private fun hideKeyboard() {
        compose.runOnUiThread {
            val activity = compose.activity
            (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        Thread.sleep(350)
        compose.waitForIdle()
    }

    private fun clickScrolledTag(tag: String) {
        compose.onNodeWithTag(tag).performScrollTo()
        compose.waitForIdle()
        // Give the platform scroll position one real frame before native touch injection.
        Thread.sleep(250)
        // Exercise the accessible click action; native theme touch is also smoke-tested on APK.
        compose.onNodeWithTag(tag).assertIsDisplayed().performSemanticsAction(SemanticsActions.OnClick) { it() }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(compose.activity.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}


package com.sukhoramen.cs.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DemoInboxRepositoryTest {
    private class MemoryStorage(var snapshot: PersistedDemo? = null) : DemoStorage {
        override suspend fun load() = snapshot
        override suspend fun save(data: PersistedDemo) { snapshot = data }
    }
    private val instagram = "ig-dm-nadia"
    private val tiktok = "tt-dm-arya"
    private fun InboxState.thread(id: String = instagram) = interactions.first { it.id == id }

    @Test fun `seed covers both platforms and interaction types and filters sort newest first`() = runTest {
        val repo = DemoInboxRepository(MemoryStorage())
        repo.initialize()
        val all = repo.state.value.interactions
        assertEquals(12, all.size)
        Platform.entries.forEach { platform -> InteractionType.entries.forEach { type ->
            assertTrue(all.any { it.platform == platform && it.type == type })
        } }
        val result = filterInteractions(all.reversed(), Platform.INSTAGRAM, false)
        assertTrue(result.all { it.platform == Platform.INSTAGRAM && !it.completed })
        assertEquals(result.sortedByDescending { it.updatedAt }, result)
        assertEquals(3, filterInteractions(all, null, true).size)
    }

    @Test fun `message stays sending until simulator confirms and fast taps cannot duplicate`() = runTest {
        val repo = DemoInboxRepository(MemoryStorage())
        repo.initialize()
        val before = repo.state.value.thread().messages.size
        launch { repo.sendReply(instagram, "Bisa, Kak!", "request-1") }
        launch { repo.sendReply(instagram, "Bisa, Kak!", "request-2") }
        runCurrent()
        assertEquals(MessageStatus.SENDING, repo.state.value.thread().messages.last().status)
        assertEquals(before + 1, repo.state.value.thread().messages.size)
        advanceTimeBy(649)
        assertEquals(MessageStatus.SENDING, repo.state.value.thread().messages.last().status)
        advanceUntilIdle()
        assertEquals(MessageStatus.SENT, repo.state.value.thread().messages.last().status)
        repo.sendReply(instagram, "Bisa, Kak!", "request-1")
        assertEquals(before + 1, repo.state.value.thread().messages.size)
    }

    @Test fun `failed message retains payload and retry reuses its identity`() = runTest {
        val repo = DemoInboxRepository(MemoryStorage())
        repo.initialize()
        repo.setScenario(DemoScenario.SEND_FAILURE, Platform.INSTAGRAM)
        repo.sendReply(instagram, "Silakan reservasi, Kak.", "request-1")
        val failed = repo.state.value.thread().messages.last()
        assertEquals(MessageStatus.FAILED, failed.status)
        assertEquals("Silakan reservasi, Kak.", failed.text)
        val count = repo.state.value.thread().messages.size
        repo.retryReply(instagram, failed.id)
        assertEquals(count, repo.state.value.thread().messages.size)
        assertEquals(MessageStatus.SENT, repo.state.value.thread().messages.last().status)
    }

    @Test fun `rate limit enforces retry delay then succeeds`() = runTest {
        val repo = DemoInboxRepository(MemoryStorage(), now = { 1_000_000L + testScheduler.currentTime })
        repo.initialize()
        repo.setScenario(DemoScenario.RATE_LIMIT, Platform.INSTAGRAM)
        repo.sendReply(instagram, "Terima kasih", "rate-1")
        assertEquals(MessageStatus.FAILED, repo.state.value.thread().messages.last().status)
        repo.retryReply(instagram, "rate-1")
        assertEquals(MessageStatus.FAILED, repo.state.value.thread().messages.last().status)
        advanceTimeBy(15_000)
        repo.retryReply(instagram, "rate-1")
        assertEquals(MessageStatus.SENT, repo.state.value.thread().messages.last().status)
    }

    @Test fun `expired or unsupported account does not block another platform`() = runTest {
        val repo = DemoInboxRepository(MemoryStorage())
        repo.initialize()
        listOf(DemoScenario.EXPIRED, DemoScenario.UNSUPPORTED).forEach { scenario ->
            repo.setScenario(scenario, Platform.INSTAGRAM)
            val before = repo.state.value.thread().messages.size
            repo.sendReply(instagram, "Tidak boleh terkirim", "blocked-${scenario.name}")
            assertEquals(before, repo.state.value.thread().messages.size)
            assertNotNull(repo.state.value.replyBlocked(repo.state.value.thread()))
            repo.sendReply(tiktok, "Halo, Kak!", "allowed-${scenario.name}")
            assertEquals(MessageStatus.SENT, repo.state.value.thread(tiktok).messages.last().status)
        }
    }

    @Test fun `offline retains cached inbox and draft while disabling sends`() = runTest {
        val repo = DemoInboxRepository(MemoryStorage())
        repo.initialize()
        repo.setDraft(instagram, "Draft yang belum terkirim")
        repo.setScenario(DemoScenario.OFFLINE, Platform.INSTAGRAM)
        val before = repo.state.value.thread().messages.size
        repo.sendReply(instagram, "Draft yang belum terkirim", "offline-1")
        repo.refresh()
        assertEquals(12, repo.state.value.interactions.size)
        assertEquals(before, repo.state.value.thread().messages.size)
        assertEquals("Draft yang belum terkirim", repo.state.value.drafts[instagram])
        assertNotNull(repo.state.value.refreshError)
        assertFalse(repo.state.value.refreshing)
    }

    @Test fun `completed statuses replies drafts and theme survive a new session`() = runTest {
        val storage = MemoryStorage()
        val repo = DemoInboxRepository(storage)
        repo.initialize()
        repo.setCompleted(instagram, true)
        repo.sendReply(tiktok, "Buka sampai jam 22.00, Kak.", "persist-1")
        repo.setDraft(instagram, "Draft reservasi")
        repo.setTheme(ThemeMode.DARK)
        val restored = DemoInboxRepository(storage)
        restored.initialize()
        assertTrue(restored.state.value.thread().completed)
        assertEquals("Draft reservasi", restored.state.value.drafts[instagram])
        assertEquals(ThemeMode.DARK, restored.state.value.theme)
        assertEquals("persist-1", restored.state.value.thread(tiktok).messages.last().id)
        restored.setCompleted(instagram, false)
        assertFalse(restored.state.value.thread().completed)
    }

    @Test fun `repeated incoming events do not create duplicate interactions`() = runTest {
        val repo = DemoInboxRepository(MemoryStorage())
        repo.initialize()
        val incoming = DemoFixtures.incoming()
        assertTrue(repo.ingest(incoming))
        assertFalse(repo.ingest(incoming))
        assertFalse(repo.ingest(incoming))
        assertEquals(13, repo.state.value.interactions.size)
        assertEquals(incoming.id, filterInteractions(repo.state.value.interactions, null, false).first().id)
    }

    @Test fun `process interruption recovers pending sends as retryable failures`() = runTest {
        val thread = DemoFixtures.interactions().first().let {
            it.copy(messages = it.messages + Message("interrupted", "Halo", true, 10, MessageStatus.SENDING))
        }
        val repo = DemoInboxRepository(MemoryStorage(PersistedDemo(listOf(thread))))
        repo.initialize()
        assertEquals(MessageStatus.FAILED, repo.state.value.thread().messages.last().status)
        assertNotNull(repo.state.value.thread().messages.last().error)
        repo.retryReply(instagram, "interrupted")
        assertEquals(MessageStatus.SENT, repo.state.value.thread().messages.last().status)
    }

    @Test fun `reset while sending discards old callbacks and preserves theme`() = runTest {
        val repo = DemoInboxRepository(MemoryStorage())
        repo.initialize()
        repo.setTheme(ThemeMode.DARK)
        launch { repo.sendReply(instagram, "Balasan lama", "old-request") }
        runCurrent()
        repo.reset()
        advanceUntilIdle()
        assertEquals(12, repo.state.value.interactions.size)
        assertFalse(repo.state.value.interactions.flatMap { it.messages }.any { it.id == "old-request" })
        assertEquals(ThemeMode.DARK, repo.state.value.theme)
    }

    @Test fun `expired reply window blocks only the affected interaction`() = runTest {
        val repo = DemoInboxRepository(MemoryStorage())
        repo.initialize()
        val expired = "ig-dm-rafi"
        assertNotNull(repo.state.value.replyBlocked(repo.state.value.thread(expired)))
        val count = repo.state.value.thread(expired).messages.size
        repo.sendReply(expired, "Tidak dikirim", "window-1")
        assertEquals(count, repo.state.value.thread(expired).messages.size)
        assertNull(repo.state.value.replyBlocked(repo.state.value.thread(instagram)))
    }

    @Test fun `storage failure is visible and a later refresh retries persistence`() = runTest {
        var fail = false
        val backing = MemoryStorage()
        val storage = object : DemoStorage {
            override suspend fun load() = backing.load()
            override suspend fun save(data: PersistedDemo) {
                if (fail) error("Disk full")
                backing.save(data)
            }
        }
        val repo = DemoInboxRepository(storage)
        repo.initialize()
        fail = true
        repo.setCompleted(instagram, true)
        assertNotNull(repo.state.value.storageError)
        fail = false
        repo.refresh()
        assertNull(repo.state.value.storageError)
        assertTrue(backing.snapshot!!.interactions.first { it.id == instagram }.completed)
    }
}


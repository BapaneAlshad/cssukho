package com.sukhoramen.cs.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DemoInboxRepository(
    private val storage: DemoStorage,
    private val now: () -> Long = System::currentTimeMillis,
    private val normalDelay: Long = 650,
    private val slowDelay: Long = 3_000,
) : InboxRepository, DemoControls {
    private val mutableState = MutableStateFlow(InboxState())
    override val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    private val attempts = mutableMapOf<String, Int>()
    private var generation = 0

    override suspend fun initialize() = mutex.withLock {
        if (state.value.ready) return@withLock
        var loadFailed = false
        val saved = try { storage.load() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { loadFailed = true; null }
        val threads = (saved?.interactions ?: DemoFixtures.interactions(now())).map { thread ->
            thread.copy(messages = thread.messages.map { message ->
                if (message.status == MessageStatus.SENDING) message.copy(
                    status = MessageStatus.FAILED, error = "Pengiriman demo terhenti. Coba kirim ulang.",
                ) else message
            })
        }
        mutableState.value = InboxState(
            interactions = threads, drafts = saved?.drafts.orEmpty(),
            theme = saved?.theme ?: ThemeMode.LIGHT, ready = true,
        )
        if (loadFailed) mutableState.value = state.value.copy(storageError = "Data lokal gagal dibaca. Menampilkan data contoh awal. Ketuk Coba lagi untuk menyimpannya.")
        else persist()
    }

    private suspend fun persist() {
        val value = state.value
        try {
            storage.save(PersistedDemo(value.interactions, value.drafts, value.theme))
            mutableState.value = state.value.copy(storageError = null)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) {
            mutableState.value = state.value.copy(storageError = "Perubahan belum tersimpan di perangkat. Coba lagi.")
        }
    }

    override suspend fun refresh() {
        val revision = mutex.withLock {
            if (state.value.refreshing) return
            mutableState.value = state.value.copy(refreshing = true, refreshError = null)
            generation
        }
        try {
            delay(if (state.value.scenario == DemoScenario.SLOW) slowDelay else normalDelay)
            mutex.withLock {
                if (revision != generation) return@withLock
                val error = when (state.value.scenario) {
                    DemoScenario.OFFLINE -> "Sedang offline (simulasi). Menampilkan pesan tersimpan."
                    DemoScenario.INBOX_ERROR -> "Inbox gagal diperbarui (simulasi). Silakan coba lagi."
                    else -> null
                }
                mutableState.value = state.value.copy(refreshing = false, refreshError = error)
                persist()
            }
        } finally {
            mutex.withLock {
                if (revision == generation) mutableState.value = state.value.copy(refreshing = false)
            }
        }
    }

    override suspend fun sendReply(interactionId: String, text: String, requestId: String) {
        if (text.isBlank()) return
        send(interactionId, text.trim(), requestId)
    }

    override suspend fun retryReply(interactionId: String, messageId: String) {
        val message = state.value.interactions.find { it.id == interactionId }
            ?.messages?.find { it.id == messageId } ?: return
        if (message.status != MessageStatus.FAILED || now() < message.retryAfter) return
        send(interactionId, message.text, messageId)
    }

    private suspend fun send(interactionId: String, text: String, requestId: String) {
        val revision = mutex.withLock {
            val current = state.value
            val thread = current.interactions.find { it.id == interactionId } ?: return
            if (current.replyBlocked(thread) != null) return
            if (thread.messages.any { it.status == MessageStatus.SENDING }) return
            val existing = thread.messages.find { it.id == requestId }
            if (existing?.status == MessageStatus.SENT || (existing?.retryAfter ?: 0) > now()) return
            val message = (existing ?: Message(requestId, text, true, now())).copy(
                status = MessageStatus.SENDING, error = null, retryAfter = 0,
            )
            val messages = if (existing == null) thread.messages + message
                else thread.messages.map { if (it.id == requestId) message else it }
            attempts[requestId] = (attempts[requestId] ?: 0) + 1
            mutableState.value = current.copy(
                interactions = current.interactions.map { if (it.id == interactionId) it.copy(messages = messages) else it },
                drafts = if (existing == null) current.drafts - interactionId else current.drafts,
            )
            persist()
            generation
        }
        try {
            delay(if (state.value.scenario == DemoScenario.SLOW) slowDelay else normalDelay)
            mutex.withLock {
                if (generation != revision) return@withLock
                val current = state.value
                val thread = current.interactions.find { it.id == interactionId } ?: return@withLock
                val affected = thread.platform == current.affectedPlatform
                val firstAttempt = attempts[requestId] == 1
                val error = current.replyBlocked(thread) ?: when {
                    affected && current.scenario == DemoScenario.SEND_FAILURE && firstAttempt ->
                        "Pesan gagal dikirim (simulasi). Ketuk coba lagi."
                    affected && current.scenario == DemoScenario.RATE_LIMIT && firstAttempt ->
                        "Batas kirim tercapai (simulasi). Tunggu 15 detik lalu coba lagi."
                    else -> null
                }
                updateMessage(interactionId, requestId) {
                    it.copy(
                        status = if (error == null) MessageStatus.SENT else MessageStatus.FAILED,
                        error = error,
                        retryAfter = if (error != null && affected && current.scenario == DemoScenario.RATE_LIMIT && firstAttempt) now() + 15_000 else 0,
                    )
                }
                persist()
            }
        } catch (cancelled: CancellationException) {
            withContext(NonCancellable) {
                mutex.withLock {
                    if (generation == revision) {
                        updateMessage(interactionId, requestId) { it.copy(status = MessageStatus.FAILED, error = "Pengiriman terhenti. Coba lagi.") }
                        persist()
                    }
                }
            }
            throw cancelled
        }
    }

    private fun updateMessage(threadId: String, messageId: String, transform: (Message) -> Message) {
        mutableState.value = state.value.copy(interactions = state.value.interactions.map { thread ->
            if (thread.id == threadId) thread.copy(messages = thread.messages.map { if (it.id == messageId) transform(it) else it }) else thread
        })
    }

    override suspend fun setCompleted(interactionId: String, completed: Boolean) = mutex.withLock {
        mutableState.value = state.value.copy(interactions = state.value.interactions.map {
            if (it.id == interactionId) it.copy(completed = completed) else it
        })
        persist()
    }

    override suspend fun setDraft(interactionId: String, text: String) = mutex.withLock {
        if (state.value.interactions.none { it.id == interactionId }) return@withLock
        mutableState.value = state.value.copy(drafts = if (text.isBlank()) state.value.drafts - interactionId else state.value.drafts + (interactionId to text))
        persist()
    }

    override suspend fun setTheme(theme: ThemeMode) = mutex.withLock {
        mutableState.value = state.value.copy(theme = theme)
        persist()
    }

    override suspend fun setScenario(scenario: DemoScenario, platform: Platform) = mutex.withLock {
        mutableState.value = state.value.copy(scenario = scenario, affectedPlatform = platform, refreshError = null)
    }

    override suspend fun ingest(interaction: Interaction): Boolean = mutex.withLock {
        if (state.value.interactions.any { it.id == interaction.id }) return@withLock false
        mutableState.value = state.value.copy(interactions = listOf(interaction) + state.value.interactions)
        persist()
        true
    }

    override suspend fun reset() = mutex.withLock {
        generation++
        attempts.clear()
        mutableState.value = InboxState(interactions = DemoFixtures.interactions(now()), theme = state.value.theme, ready = true)
        persist()
    }
}


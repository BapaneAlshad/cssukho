package com.sukhoramen.cs.data

import kotlinx.coroutines.flow.StateFlow

interface DemoStorage {
    suspend fun load(): PersistedDemo?
    suspend fun save(data: PersistedDemo)
}

interface InboxRepository {
    val state: StateFlow<InboxState>
    suspend fun initialize()
    suspend fun refresh()
    suspend fun loadDetail(interactionId: String) {}
    suspend fun sendReply(interactionId: String, text: String, requestId: String)
    suspend fun retryReply(interactionId: String, messageId: String)
    suspend fun setCompleted(interactionId: String, completed: Boolean)
    suspend fun setDraft(interactionId: String, text: String)
    suspend fun setTheme(theme: ThemeMode)
}

interface DemoControls {
    suspend fun setScenario(scenario: DemoScenario, platform: Platform)
    suspend fun ingest(interaction: Interaction): Boolean
    suspend fun reset()
}


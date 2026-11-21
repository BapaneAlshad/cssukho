package com.sukhoramen.cs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sukhoramen.cs.data.*
import java.util.UUID
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

sealed interface UiEvent {
    data class Notice(val text: String) : UiEvent
    data class Incoming(val id: String, val added: Boolean) : UiEvent
    data class OpenUrl(val url: String) : UiEvent
}

class MainViewModel(private val repository: InboxRepository, private val liveApi: LiveApi? = null) : ViewModel() {
    val state = repository.state
    private val eventFlow = MutableSharedFlow<UiEvent>()
    val events = eventFlow.asSharedFlow()
    private val draftJobs = mutableMapOf<String, Job>()
    private val pendingDrafts = mutableMapOf<String, String>()
    private val activeSendTokens = mutableMapOf<String, Any>()

    init { viewModelScope.launch { repository.initialize() } }

    fun refresh() { viewModelScope.launch { repository.refresh() } }
    fun connect(platform: Platform) {
        val api = liveApi ?: return
        viewModelScope.launch {
            try {
                eventFlow.emit(UiEvent.OpenUrl(api.connect(platform).authorizationUrl))
            } catch (error: Exception) {
                eventFlow.emit(UiEvent.Notice(error.message ?: "Koneksi akun gagal. Coba lagi."))
            }
        }
    }
    fun open(id: String) { viewModelScope.launch { repository.loadDetail(id) } }
    fun draft(id: String, text: String) {
        pendingDrafts[id] = text
        draftJobs.remove(id)?.cancel()
        draftJobs[id] = viewModelScope.launch {
            delay(250)
            repository.setDraft(id, text)
            if (pendingDrafts[id] == text) pendingDrafts.remove(id)
        }
    }
    fun flushDrafts() {
        val pending = pendingDrafts.toMap()
        pending.forEach { (id, text) ->
            draftJobs.remove(id)?.cancel()
            pendingDrafts.remove(id)
            viewModelScope.launch { repository.setDraft(id, text) }
        }
    }
    fun send(id: String, text: String) {
        if (!state.value.isDemo) return
        if (activeSendTokens.containsKey(id)) return
        val ticket = Any()
        activeSendTokens[id] = ticket
        draftJobs.remove(id)?.cancel()
        pendingDrafts.remove(id)
        viewModelScope.launch {
            try {
                repository.setDraft(id, text)
                repository.sendReply(id, text, UUID.randomUUID().toString())
            } finally {
                if (activeSendTokens[id] === ticket) activeSendTokens.remove(id)
            }
        }
    }
    fun retry(id: String, messageId: String) {
        if (!state.value.isDemo) return
        viewModelScope.launch { repository.retryReply(id, messageId) }
    }
    fun complete(id: String, completed: Boolean) {
        viewModelScope.launch {
            try {
                repository.setCompleted(id, completed)
                eventFlow.emit(UiEvent.Notice(if (completed) "Interaksi ditandai selesai" else "Interaksi dibuka kembali"))
            } catch (error: Exception) {
                eventFlow.emit(UiEvent.Notice(error.message ?: "Status gagal disimpan. Coba lagi."))
            }
        }
    }
    fun theme(theme: ThemeMode) { viewModelScope.launch { repository.setTheme(theme) } }
    fun scenario(scenario: DemoScenario, platform: Platform) {
        viewModelScope.launch { (repository as? DemoControls)?.setScenario(scenario, platform) }
    }
    fun incoming() {
        viewModelScope.launch {
            val item = DemoFixtures.incoming()
            val added = (repository as? DemoControls)?.ingest(item) ?: false
            eventFlow.emit(UiEvent.Incoming(item.id, added))
        }
    }
    fun duplicate() {
        viewModelScope.launch {
            val item = DemoFixtures.incoming()
            val controls = repository as? DemoControls ?: return@launch
            controls.ingest(item)
            val repeated = controls.ingest(item)
            eventFlow.emit(UiEvent.Notice(if (!repeated) "Event berulang diabaikan. Tidak ada pesan ganda." else "Event ditambahkan"))
        }
    }
    fun reset() {
        activeSendTokens.clear()
        draftJobs.values.forEach { it.cancel() }
        draftJobs.clear()
        pendingDrafts.clear()
        viewModelScope.launch {
            (repository as? DemoControls)?.reset()
            eventFlow.emit(UiEvent.Notice("Data demo dikembalikan ke awal"))
        }
    }

    class Factory(private val repository: InboxRepository, private val liveApi: LiveApi? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(repository, liveApi) as T
    }
}

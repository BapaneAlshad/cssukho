package com.sukhoramen.cs.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LiveInboxRepository(private val api: LiveApi, private val session: LiveSession, context: Context) : InboxRepository {
    private val prefs = context.applicationContext.getSharedPreferences("sukho_live_display_v1", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(InboxState(isDemo = false))
    override val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    @Volatile private var epoch = 0L

    fun clear() {
        epoch++
        mutableState.value = InboxState(isDemo = false, theme = state.value.theme)
    }

    override suspend fun initialize() {
        if (state.value.ready) return
        mutableState.value = state.value.copy(theme = ThemeMode.entries.find { it.name == prefs.getString("theme", "LIGHT") } ?: ThemeMode.LIGHT)
        refresh()
    }

    override suspend fun refresh() {
        mutex.withLock {
            val startedAt = epoch
            mutableState.value = state.value.copy(refreshing = true, refreshError = null)
            try {
                val accounts = api.accounts()
                val items = mutableListOf<Interaction>()
                var cursor: String? = null
                do {
                    val page = api.inbox(cursor)
                    items += page.items
                    cursor = page.nextCursor
                } while (cursor != null && items.size < 1_000)
                if (startedAt != epoch || session.token == null) return@withLock
                val old = state.value.interactions.associateBy { it.id }
                val merged = items.map { item ->
                    old[item.id]?.takeIf { it.messages.isNotEmpty() && it.messages.first().id != "preview-${item.id}" }
                        ?.copy(completed = item.completed) ?: item
                }
                mutableState.value = state.value.copy(interactions = merged, connections = accounts, ready = true, refreshing = false,
                    refreshError = if (cursor != null) "Menampilkan 1.000 interaksi terbaru." else null)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: ApiUnauthorized) { if (startedAt == epoch) session.revoke() }
            catch (error: Exception) { if (startedAt == epoch && session.token != null) mutableState.value = state.value.copy(ready = true, refreshing = false, refreshError = error.message ?: "Gagal memuat inbox.") }
        }
    }

    override suspend fun loadDetail(interactionId: String) {
        val startedAt = epoch
        try {
            val detail = api.detail(interactionId)
            if (startedAt == epoch && session.token != null) mutableState.value = state.value.copy(interactions = state.value.interactions.map { if (it.id == interactionId) detail else it })
        } catch (_: ApiUnauthorized) { if (startedAt == epoch) session.revoke() }
        catch (error: Exception) { if (startedAt == epoch && session.token != null) mutableState.value = state.value.copy(refreshError = error.message ?: "Detail gagal dimuat.") }
    }

    override suspend fun sendReply(interactionId: String, text: String, requestId: String): Unit =
        throw ApiError("Balasan live belum tersedia sebelum API sosial terhubung.")

    override suspend fun retryReply(interactionId: String, messageId: String): Unit =
        throw ApiError("Balasan live belum tersedia sebelum API sosial terhubung.")

    override suspend fun setCompleted(interactionId: String, completed: Boolean) {
        val startedAt = epoch
        try { api.complete(interactionId, completed) }
        catch (unauthorized: ApiUnauthorized) { session.revoke(); throw ApiError("Perangkat perlu aktivasi ulang.") }
        if (startedAt != epoch || session.token == null) return
        mutableState.value = state.value.copy(interactions = state.value.interactions.map { if (it.id == interactionId) it.copy(completed = completed) else it })
    }

    override suspend fun setDraft(interactionId: String, text: String) { /* Sending is unavailable in live mode. */ }

    override suspend fun setTheme(theme: ThemeMode) {
        prefs.edit().putString("theme", theme.name).apply()
        mutableState.value = state.value.copy(theme = theme)
    }
}


package com.sukhoramen.cs

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sukhoramen.cs.data.ApiError
import com.sukhoramen.cs.data.UnlockResult
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class EntryPage { SETUP, LOCKED, DEMO, LIVE }
data class EntryState(val page: EntryPage, val busy: Boolean = false, val error: String? = null, val cooldownUntil: Long = 0)

class EntryViewModel(private val app: SukhoApplication) : ViewModel() {
    private val mutableState = MutableStateFlow(EntryState(if (app.vault.hasCredential()) EntryPage.LOCKED else EntryPage.SETUP))
    val state = mutableState.asStateFlow()
    private var backgroundAt: Long? = null
    private var pendingToken: String? = null
    private var pendingDeviceId: String? = null

    init {
        viewModelScope.launch {
            app.session.revoked.collect { revoked ->
                if (revoked) {
                    app.vault.clear()
                    app.liveRepository.clear()
                    mutableState.value = EntryState(EntryPage.SETUP, error = "Akses perangkat dicabut. Aktifkan ulang dengan kode baru.")
                }
            }
        }
    }

    fun enterDemo() {
        app.session.lock()
        app.liveRepository.clear()
        mutableState.value = EntryState(EntryPage.DEMO)
    }

    fun showGate() {
        app.session.lock()
        app.liveRepository.clear()
        mutableState.value = EntryState(if (app.vault.hasCredential()) EntryPage.LOCKED else EntryPage.SETUP)
    }

    fun activate(code: String, pin: String, repeat: String) {
        if (mutableState.value.busy) return
        if (!pin.matches(Regex("[0-9]{6}"))) {
            mutableState.value = mutableState.value.copy(error = "PIN harus tepat 6 angka.")
            return
        }
        if (pin != repeat) {
            mutableState.value = mutableState.value.copy(error = "Konfirmasi PIN belum sama.")
            return
        }
        val normalized = code.uppercase().replace(Regex("[\\s-]"), "")
        if (normalized.length != 16) {
            mutableState.value = mutableState.value.copy(error = "Kode aktivasi harus 16 karakter.")
            return
        }
        mutableState.value = mutableState.value.copy(busy = true, error = null)
        viewModelScope.launch {
            try {
                val token = pendingToken ?: app.liveApi.newDeviceToken().also { pendingToken = it }
                val deviceId = pendingDeviceId ?: UUID.randomUUID().toString().also { pendingDeviceId = it }
                app.liveApi.activate(normalized, token, deviceId)
                withContext(Dispatchers.Default) { app.vault.store(token, pin) }
                app.session.unlock(token)
                pendingToken = null
                pendingDeviceId = null
                mutableState.value = EntryState(EntryPage.LIVE)
                app.liveRepository.refresh()
            } catch (error: Exception) {
                mutableState.value = EntryState(EntryPage.SETUP, error = error.message ?: "Aktivasi gagal. Coba lagi.")
            }
        }
    }

    fun unlock(pin: String) {
        if (mutableState.value.busy) return
        mutableState.value = mutableState.value.copy(busy = true, error = null)
        viewModelScope.launch {
            when (val result = withContext(Dispatchers.Default) { app.vault.unlock(pin) }) {
                is UnlockResult.Success -> {
                    app.session.unlock(result.token)
                    mutableState.value = EntryState(EntryPage.LIVE)
                    app.liveRepository.refresh()
                }
                is UnlockResult.Wrong -> mutableState.value = EntryState(EntryPage.LOCKED, error = "PIN salah. ${result.remaining} percobaan sebelum jeda.")
                is UnlockResult.Cooldown -> mutableState.value = EntryState(EntryPage.LOCKED, error = "Terlalu banyak percobaan. Coba lagi nanti.", cooldownUntil = result.until)
                UnlockResult.Damaged -> mutableState.value = EntryState(EntryPage.LOCKED, error = "Kunci perangkat tidak dapat dibaca. Aktifkan ulang.")
            }
        }
    }

    fun forgetPin() {
        app.session.lock()
        app.liveRepository.clear()
        app.vault.clear()
        pendingToken = null
        pendingDeviceId = null
        mutableState.value = EntryState(EntryPage.SETUP)
    }

    fun onBackground() {
        if (mutableState.value.page == EntryPage.LIVE) backgroundAt = SystemClock.elapsedRealtime()
    }

    fun onForeground() {
        val elapsed = backgroundAt?.let { SystemClock.elapsedRealtime() - it } ?: return
        backgroundAt = null
        if (elapsed >= 300_000 && mutableState.value.page == EntryPage.LIVE) showGate()
    }

    class Factory(private val app: SukhoApplication) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = EntryViewModel(app) as T
    }
}


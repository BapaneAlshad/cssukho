package com.sukhoramen.cs.data

enum class Platform(val label: String) { INSTAGRAM("Instagram"), TIKTOK("TikTok") }
enum class InteractionType(val label: String) { DM("DM"), COMMENT("Komentar") }
enum class MessageStatus { SENDING, SENT, FAILED }
enum class ThemeMode(val label: String) { LIGHT("Terang"), DARK("Gelap"), SYSTEM("Ikuti sistem") }
enum class DemoScenario(val label: String, val description: String) {
    NORMAL("Normal", "Semua alur demo berjalan lancar."),
    SLOW("Koneksi lambat", "Refresh dan pengiriman memerlukan sekitar 3 detik."),
    OFFLINE("Offline", "Pesan tersimpan tetap dapat dibaca. Pengiriman dinonaktifkan."),
    EXPIRED("Login kedaluwarsa", "Akun platform pilihan perlu login ulang."),
    RATE_LIMIT("Batas kirim", "Percobaan pertama dibatasi selama 15 detik."),
    SEND_FAILURE("Gagal kirim", "Percobaan pertama gagal. Coba ulang untuk berhasil."),
    UNSUPPORTED("Belum didukung", "Balasan platform pilihan belum tersedia melalui API."),
    INBOX_ERROR("Gagal memuat", "Refresh gagal; percakapan tersimpan tetap aman."),
    EMPTY("Inbox kosong", "Tampilkan kondisi ketika belum ada interaksi.")
}

data class ConnectionStatus(
    val status: String,
    val handle: String? = null,
    val canRead: Boolean = false,
    val canReply: Boolean = false,
)

data class Message(
    val id: String,
    val text: String,
    val outgoing: Boolean,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.SENT,
    val error: String? = null,
    val retryAfter: Long = 0,
)

data class PostContext(val caption: String, val kind: String, val url: String? = null)

data class Interaction(
    val id: String,
    val sender: String,
    val handle: String,
    val initials: String,
    val platform: Platform,
    val type: InteractionType,
    val messages: List<Message>,
    val completed: Boolean = false,
    val post: PostContext? = null,
    val sourceUrl: String? = null,
    val replyRestriction: String? = null,
) {
    val updatedAt: Long get() = messages.lastOrNull()?.timestamp ?: 0
    val preview: String get() = messages.lastOrNull()?.text.orEmpty()
}

data class PersistedDemo(
    val interactions: List<Interaction>,
    val drafts: Map<String, String> = emptyMap(),
    val theme: ThemeMode = ThemeMode.LIGHT,
)

data class InboxState(
    val interactions: List<Interaction> = emptyList(),
    val drafts: Map<String, String> = emptyMap(),
    val theme: ThemeMode = ThemeMode.LIGHT,
    val ready: Boolean = false,
    val refreshing: Boolean = false,
    val refreshError: String? = null,
    val storageError: String? = null,
    val scenario: DemoScenario = DemoScenario.NORMAL,
    val affectedPlatform: Platform = Platform.INSTAGRAM,
    val isDemo: Boolean = true,
    val connections: Map<Platform, ConnectionStatus> = emptyMap(),
) {
    fun replyBlocked(interaction: Interaction): String? = when {
        !isDemo -> "Balasan live belum diaktifkan pada tahap ini. Sinkronisasi dan izin kirim masih dalam persiapan."
        scenario == DemoScenario.OFFLINE -> "Sedang offline (simulasi). Hubungkan kembali sebelum membalas."
        interaction.replyRestriction != null -> interaction.replyRestriction
        interaction.platform == affectedPlatform && scenario == DemoScenario.EXPIRED ->
            "Sesi ${interaction.platform.label} kedaluwarsa (simulasi). Akun perlu login ulang."
        interaction.platform == affectedPlatform && scenario == DemoScenario.UNSUPPORTED ->
            "Balasan ${interaction.platform.label} belum didukung (simulasi akses API)."
        else -> null
    }
}

fun filterInteractions(
    interactions: List<Interaction>, platform: Platform?, completed: Boolean,
): List<Interaction> = interactions.filter {
    (platform == null || it.platform == platform) && it.completed == completed
}.sortedByDescending { it.updatedAt }


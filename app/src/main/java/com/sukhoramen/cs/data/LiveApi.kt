package com.sukhoramen.cs.data

import android.util.Base64
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ApiError(message: String) : Exception(message)
class ApiUnauthorized : Exception()

class LiveSession {
    @Volatile var token: String? = null
        private set
    private val _revoked = kotlinx.coroutines.flow.MutableStateFlow(false)
    val revoked = _revoked.asStateFlow()
    fun unlock(value: String) { token = value; _revoked.value = false }
    fun lock() { token = null }
    fun revoke() { token = null; _revoked.value = true }
}

data class LiveAccount(val platform: Platform, val status: ConnectionStatus)
data class InboxPage(val items: List<Interaction>, val nextCursor: String?)
data class OAuthConnection(val authorizationUrl: String)

class LiveApi(private val baseUrl: String, private val session: LiveSession) {
    fun newDeviceToken(): String = ByteArray(32).also(SecureRandom()::nextBytes)
        .let { Base64.encodeToString(it, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE) }

    suspend fun activate(code: String, token: String, deviceId: String) {
        request("POST", "/api/activate", JSONObject().put("code", code).put("deviceToken", token).put("deviceId", deviceId), authenticated = false)
    }

    suspend fun accounts(): Map<Platform, ConnectionStatus> {
        val array = request("GET", "/api/bootstrap").getJSONArray("accounts")
        return (0 until array.length()).mapNotNull { index ->
            val item = array.getJSONObject(index)
            val platform = parsePlatform(item.optString("platform")) ?: return@mapNotNull null
            platform to ConnectionStatus(item.optString("status"), item.optString("handle").takeIf(String::isNotBlank), item.optBoolean("canRead"), item.optBoolean("canReply"))
        }.toMap()
    }

    suspend fun connect(platform: Platform): OAuthConnection {
        val root = request("GET", "/api/connect?platform=${encode(platform.apiValue)}")
        val url = root.optString("authorizationUrl").takeIf { it.startsWith("https://") }
            ?: throw ApiError("Tautan koneksi Instagram tidak valid.")
        return OAuthConnection(url)
    }

    suspend fun inbox(cursor: String? = null): InboxPage {
        val path = "/api/inbox" + if (cursor == null) "" else "?cursor=${encode(cursor)}"
        val root = request("GET", path)
        val array = root.getJSONArray("items")
        val items = (0 until array.length()).mapNotNull { index ->
            val item = array.getJSONObject(index)
            val platform = parsePlatform(item.optString("platform")) ?: return@mapNotNull null
            val kind = parseKind(item.optString("kind")) ?: return@mapNotNull null
            val updated = timestamp(item.getString("updatedAt"))
            val id = item.getString("id")
            Interaction(id, item.getString("sender"), item.optString("handle"), item.optString("initials"),
                platform, kind, listOf(Message("preview-$id", item.optString("preview"), false, updated)),
                item.optBoolean("completed"), replyRestriction = item.optNullable("replyRestriction"))
        }
        return InboxPage(items, root.optNullable("nextCursor"))
    }

    suspend fun detail(id: String): Interaction {
        val root = request("GET", "/api/interaction?id=${encode(id)}")
        val item = root.getJSONObject("interaction")
        val array = root.getJSONArray("messages")
        val messages = (0 until array.length()).map { index ->
            val message = array.getJSONObject(index)
            Message(message.getString("id"), message.getString("text"), message.getString("direction") == "outgoing",
                timestamp(message.getString("occurredAt")),
                when (message.getString("status")) { "sending" -> MessageStatus.SENDING; "failed" -> MessageStatus.FAILED; else -> MessageStatus.SENT },
                message.optNullable("error"))
        }
        val post = item.optNullable("postCaption")?.let { caption -> PostContext(caption, item.optNullable("postKind") ?: "Posting", item.optNullable("postUrl")) }
        return Interaction(id, item.getString("sender"), item.optString("handle"), item.optString("initials"),
            parsePlatform(item.getString("platform")) ?: throw ApiError("Platform tidak dikenal."),
            parseKind(item.getString("kind")) ?: throw ApiError("Jenis interaksi tidak dikenal."),
            messages, item.optBoolean("completed"), post, item.optNullable("sourceUrl"), item.optNullable("replyRestriction"))
    }

    suspend fun complete(id: String, completed: Boolean) {
        request("PATCH", "/api/interaction?id=${encode(id)}", JSONObject().put("completed", completed))
    }

    private suspend fun request(method: String, path: String, body: JSONObject? = null, authenticated: Boolean = true): JSONObject = withContext(Dispatchers.IO) {
        if (baseUrl.isBlank()) throw ApiError("Backend belum dikonfigurasi pada APK ini.")
        if (!baseUrl.startsWith("https://")) throw ApiError("Backend harus memakai HTTPS.")
        val connection = URL(baseUrl.trimEnd('/') + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.useCaches = false
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-store")
            if (authenticated) {
                val token = session.token ?: throw ApiUnauthorized()
                connection.setRequestProperty("Authorization", "Bearer $token")
            }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val raw = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status == 401) throw ApiUnauthorized()
            val result = try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
            if (status !in 200..299) throw ApiError(result.optString("message").ifBlank { "Layanan gagal ($status). Coba lagi." })
            result
        } catch (error: java.io.IOException) {
            throw ApiError("Koneksi gagal. Periksa internet lalu coba lagi.")
        } finally { connection.disconnect() }
    }

    private fun parsePlatform(value: String): Platform? = when (value) { "instagram" -> Platform.INSTAGRAM; "tiktok" -> Platform.TIKTOK; else -> null }
    private fun parseKind(value: String): InteractionType? = when (value) { "dm" -> InteractionType.DM; "comment" -> InteractionType.COMMENT; else -> null }
    private fun timestamp(value: String): Long = try { Instant.parse(value).toEpochMilli() } catch (_: Exception) { System.currentTimeMillis() }
    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8")
    private fun JSONObject.optNullable(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}

private val Platform.apiValue: String
    get() = when (this) {
        Platform.INSTAGRAM -> "instagram"
        Platform.TIKTOK -> "tiktok"
    }

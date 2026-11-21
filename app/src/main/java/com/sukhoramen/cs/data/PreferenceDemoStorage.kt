package com.sukhoramen.cs.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.demoDataStore by preferencesDataStore("sukho_demo_v1")

class PreferenceDemoStorage(context: Context) : DemoStorage {
    private val store = context.applicationContext.demoDataStore
    private val snapshotKey = stringPreferencesKey("snapshot")

    override suspend fun load(): PersistedDemo? = withContext(Dispatchers.IO) {
        val raw = store.data.first()[snapshotKey] ?: return@withContext null
        val root = JSONObject(raw)
        val threads = root.getJSONArray("interactions").objects().map { thread ->
            Interaction(
                id = thread.getString("id"), sender = thread.getString("sender"),
                handle = thread.getString("handle"), initials = thread.getString("initials"),
                platform = Platform.valueOf(thread.getString("platform")),
                type = InteractionType.valueOf(thread.getString("type")),
                completed = thread.optBoolean("completed"),
                sourceUrl = thread.optionalString("sourceUrl"),
                replyRestriction = thread.optionalString("restriction"),
                post = thread.optJSONObject("post")?.let {
                    PostContext(it.getString("caption"), it.getString("kind"), it.optionalString("url"))
                },
                messages = thread.getJSONArray("messages").objects().map {
                    Message(
                        id = it.getString("id"), text = it.getString("text"),
                        outgoing = it.getBoolean("outgoing"), timestamp = it.getLong("timestamp"),
                        status = MessageStatus.valueOf(it.getString("status")),
                        error = it.optionalString("error"), retryAfter = it.optLong("retryAfter"),
                    )
                },
            )
        }
        val drafts = root.optJSONObject("drafts") ?: JSONObject()
        PersistedDemo(
            threads, drafts.keys().asSequence().associateWith { drafts.getString(it) },
            ThemeMode.entries.find { it.name == root.optString("theme") } ?: ThemeMode.LIGHT,
        )
    }

    override suspend fun save(data: PersistedDemo) = withContext(Dispatchers.IO) {
        val root = JSONObject().apply {
            put("version", 1)
            put("theme", data.theme.name)
            put("drafts", JSONObject(data.drafts))
            put("interactions", JSONArray().apply {
                data.interactions.forEach { thread ->
                    put(JSONObject().apply {
                        put("id", thread.id); put("sender", thread.sender)
                        put("handle", thread.handle); put("initials", thread.initials)
                        put("platform", thread.platform.name); put("type", thread.type.name)
                        put("completed", thread.completed); put("sourceUrl", thread.sourceUrl)
                        put("restriction", thread.replyRestriction)
                        thread.post?.let { post -> put("post", JSONObject().apply {
                            put("caption", post.caption); put("kind", post.kind); put("url", post.url)
                        }) }
                        put("messages", JSONArray().apply { thread.messages.forEach { message ->
                            put(JSONObject().apply {
                                put("id", message.id); put("text", message.text)
                                put("outgoing", message.outgoing); put("timestamp", message.timestamp)
                                put("status", message.status.name); put("error", message.error)
                                put("retryAfter", message.retryAfter)
                            })
                        } })
                    })
                }
            })
        }.toString()
        store.edit { it[snapshotKey] = root }
        Unit
    }
}

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
private fun JSONObject.optionalString(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }


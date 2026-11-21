package com.sukhoramen.cs.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.core.net.toUri
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukhoramen.cs.R
import com.sukhoramen.cs.data.*
import kotlinx.coroutines.delay

@Composable
fun DetailScreen(
    id: String, state: InboxState, onBack: () -> Unit,
    onDraft: (String, String) -> Unit, onSend: (String, String) -> Unit,
    onRetry: (String, String) -> Unit, onComplete: (String, Boolean) -> Unit,
) {
    BackHandler(onBack = onBack)
    val thread = state.interactions.find { it.id == id }
    val colors = MaterialTheme.colorScheme
    if (thread == null) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            Text("Interaksi tidak ditemukan", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Button(onClick = onBack) { Text("Kembali ke inbox") }
        }
        return
    }
    val listState = rememberLazyListState()
    val blocked = state.replyBlocked(thread)
    val sending = thread.messages.any { it.status == MessageStatus.SENDING }
    // Editing is synchronous local UI state. Disk writes must never gate IME input.
    var draft by rememberSaveable(id) { mutableStateOf(state.drafts[id].orEmpty()) }
    LaunchedEffect(thread.messages.size) {
        val last = thread.messages.lastOrNull()
        if (last?.outgoing == true && last.status == MessageStatus.SENDING && last.text == draft.trim()) draft = ""
    }
    val context = LocalContext.current
    var sourceError by remember { mutableStateOf<String?>(null) }
    val source = thread.sourceUrl ?: thread.post?.url
    val validSource = source?.toUri()?.takeIf { it.scheme in listOf("https", "http") && !it.host.isNullOrBlank() }

    LaunchedEffect(thread.messages.lastOrNull()?.id) {
        if (thread.messages.lastOrNull()?.outgoing == true) {
            delay(100)
            val count = listState.layoutInfo.totalItemsCount
            if (count > 0) listState.animateScrollToItem(count - 1)
        }
    }

    Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("detail_back"), shape = RoundedCornerShape(16.dp),
                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = colors.surface, contentColor = colors.onSurface)) {
                Icon(SukhoIcons.Back, "Kembali", Modifier.size(22.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(thread.sender, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${thread.platform.label} · ${thread.type.label}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            if (state.isDemo) DemoBadge() else LiveBadge()
        }
        LazyColumn(
            state = listState, modifier = Modifier.weight(1f).testTag("detail_messages"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SoftCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(thread.initials)
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(thread.sender, style = MaterialTheme.typography.titleMedium)
                            Text("@${thread.handle}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        PlatformIcon(thread.platform, Modifier.size(21.dp))
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 17.dp), color = colors.outlineVariant.copy(alpha = .65f))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(if (thread.completed) colors.tertiary else colors.primary, RoundedCornerShape(50)))
                        Spacer(Modifier.width(6.dp))
                        Text(if (thread.completed) "Selesai" else "Belum ditangani", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                            color = if (thread.completed) colors.tertiary else colors.onSurfaceVariant)
                        TextButton(onClick = { onComplete(id, !thread.completed) }, modifier = Modifier.testTag("toggle_complete"), contentPadding = PaddingValues(horizontal = 6.dp)) {
                            Icon(if (thread.completed) SukhoIcons.Refresh else SukhoIcons.Check, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(if (thread.completed) "Buka kembali" else "Tandai selesai", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            thread.post?.let { post -> item { PostContextCard(post, state.isDemo) } }
            thread.messages.firstOrNull()?.let { first -> item {
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.Center) {
                    Text(conversationDate(first.timestamp), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
            } }
            items(thread.messages, key = { it.id }) { message ->
                MessageBubble(message, retryEnabled = blocked == null && !sending, isDemo = state.isDemo, onRetry = { onRetry(id, message.id) })
            }
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (validSource != null) TextButton(onClick = {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, validSource)) }
                        catch (_: ActivityNotFoundException) { sourceError = "Tidak ada aplikasi untuk membuka tautan ini." }
                    }) {
                        Icon(SukhoIcons.Link, null, Modifier.size(15.dp)); Spacer(Modifier.width(6.dp))
                        Text("Buka konten asal", style = MaterialTheme.typography.labelMedium)
                    }
                    else Text(if (state.isDemo) "Konten contoh · Tautan asli belum tersedia" else "Tautan asal belum tersedia",
                        style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    sourceError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.error) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        state.storageError?.let { NoticeCard(it, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), error = true) }
        if (!state.isDemo) state.refreshError?.let { NoticeCard(it, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), error = true) }
        if (blocked != null) NoticeCard(blocked, Modifier.padding(horizontal = 16.dp, vertical = 6.dp), error = true)
        if (state.isDemo) Surface(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 5.dp, bottom = 10.dp), shape = RoundedCornerShape(23.dp), shadowElevation = 3.dp, color = colors.surface) {
            Column(Modifier.padding(10.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(
                        value = draft, onValueChange = { if (it.length <= 2_000) { draft = it; onDraft(id, it) } },
                        modifier = Modifier.weight(1f).testTag("reply_input"),
                        placeholder = { Text(if (blocked == null) "Tulis balasan hangat…" else "Balasan tidak tersedia", style = MaterialTheme.typography.bodyMedium) },
                        enabled = blocked == null && !sending, maxLines = 4, minLines = 1,
                        shape = RoundedCornerShape(16.dp), textStyle = MaterialTheme.typography.bodyLarge,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent, focusedBorderColor = colors.primaryContainer,
                            disabledBorderColor = Color.Transparent, unfocusedContainerColor = colors.surface,
                        ),
                    )
                    Spacer(Modifier.width(6.dp))
                    FilledIconButton(
                        onClick = { onSend(id, draft) }, enabled = draft.isNotBlank() && blocked == null && !sending,
                        modifier = Modifier.size(50.dp).testTag("send_reply"), shape = RoundedCornerShape(16.dp),
                    ) {
                        if (sending) CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp, color = colors.onPrimary)
                        else Icon(SukhoIcons.Send, "Kirim balasan", Modifier.size(21.dp))
                    }
                }
                Text("Balasan demo tidak dikirim ke akun sosial asli.", Modifier.padding(start = 12.dp, top = 3.dp, bottom = 2.dp), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PostContextCard(post: PostContext, isDemo: Boolean) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(20.dp), color = colors.primaryContainer.copy(alpha = .7f)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("KONTEKS KOMENTAR", fontSize = 9.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = .8.sp, color = colors.onPrimaryContainer)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(55.dp).background(colors.surface.copy(alpha = .8f), RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Image(painterResource(R.drawable.sukho_logo), "Logo SUKHO RAMEN", Modifier.size(39.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("SUKHO RAMEN", style = MaterialTheme.typography.labelMedium, color = colors.onPrimaryContainer)
                    Text(post.caption, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
                    Text(post.kind + if (isDemo) " · Contoh" else "", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.labelSmall, color = colors.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, retryEnabled: Boolean, isDemo: Boolean, onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val failed = message.status == MessageStatus.FAILED
    val bubbleColor = if (failed) colors.errorContainer else if (message.outgoing) colors.primary else colors.surface
    val textColor = if (failed) colors.onErrorContainer else if (message.outgoing) colors.onPrimary else colors.onSurface
    var clock by remember(message.retryAfter) { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(message.retryAfter) {
        while (clock < message.retryAfter) { delay(500); clock = System.currentTimeMillis() }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (message.outgoing) Alignment.End else Alignment.Start) {
        Surface(
            Modifier.fillMaxWidth(.88f), shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = if (message.outgoing) 20.dp else 5.dp, bottomEnd = if (message.outgoing) 5.dp else 20.dp),
            color = bubbleColor, shadowElevation = if (message.outgoing) 0.dp else 1.dp,
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
                Text(message.text, style = MaterialTheme.typography.bodyLarge, color = textColor)
                Spacer(Modifier.height(7.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Text(messageTime(message.timestamp), style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = .85f))
                    if (message.outgoing) {
                        Text("  ·  ", color = textColor.copy(alpha = .8f), fontSize = 10.sp)
                        if (message.status == MessageStatus.SENDING) {
                            CircularProgressIndicator(Modifier.size(10.dp), strokeWidth = 1.dp, color = textColor)
                            Spacer(Modifier.width(5.dp))
                        } else if (message.status == MessageStatus.SENT) { Icon(SukhoIcons.Check, null, Modifier.size(12.dp), tint = textColor); Spacer(Modifier.width(3.dp)) }
                        Text(when (message.status) { MessageStatus.SENDING -> "Mengirim"; MessageStatus.SENT -> if (isDemo) "Terkirim · demo" else "Terkirim"; MessageStatus.FAILED -> "Gagal" },
                            modifier = Modifier.testTag("message_status_${message.id}"), style = MaterialTheme.typography.labelSmall, color = textColor)
                    }
                }
            }
        }
        if (failed) {
            Text(message.error ?: "Pengiriman gagal", Modifier.fillMaxWidth(.88f).padding(top = 6.dp, start = 3.dp), style = MaterialTheme.typography.bodySmall, color = colors.error)
            TextButton(onClick = onRetry, enabled = retryEnabled && clock >= message.retryAfter, modifier = Modifier.testTag("retry_${message.id}")) {
                Icon(SukhoIcons.Refresh, null, Modifier.size(14.dp)); Spacer(Modifier.width(5.dp))
                Text(if (clock < message.retryAfter) "Coba lagi (${((message.retryAfter - clock) / 1000) + 1} dtk)" else "Coba kirim lagi", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}


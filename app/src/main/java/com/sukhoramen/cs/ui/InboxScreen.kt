package com.sukhoramen.cs.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukhoramen.cs.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    state: InboxState,
    type: InteractionType,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onOpen: (String) -> Unit,
) {
    var platformName by rememberSaveable { mutableStateOf("ALL") }
    var completed by rememberSaveable { mutableStateOf(false) }
    val platform = Platform.entries.find { it.name == platformName }
    val all = if (state.scenario == DemoScenario.EMPTY) emptyList() else state.interactions.filter { it.type == type }
    val items = remember(all, platform, completed) { filterInteractions(all, platform, completed) }
    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().testTag("inbox_list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                BrandHeader(onRefresh = onRefresh, onSettings = onSettings, isRefreshing = state.refreshing, isDemo = state.isDemo)
            }
            if (state.isDemo && state.scenario != DemoScenario.NORMAL) item {
                NoticeCard("Simulasi: ${state.scenario.label}" + if (state.scenario in listOf(DemoScenario.EXPIRED, DemoScenario.UNSUPPORTED, DemoScenario.RATE_LIMIT, DemoScenario.SEND_FAILURE)) " · ${state.affectedPlatform.label}" else "")
            }
            if (!state.isDemo && state.connections.values.none { it.canRead }) item {
                NoticeCard("Akun sosial belum terhubung. Inbox cloud akan terisi setelah integrasi API resmi tersedia.")
            }
            state.storageError?.let { error -> item { NoticeCard(error, error = true, action = "Coba lagi", onAction = onRefresh) } }
            state.refreshError?.let { error -> item { NoticeCard(error, error = true, action = "Ulangi", onAction = onRefresh) } }
            item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChoiceChip("Semua", platform == null, { platformName = "ALL" }, modifier = Modifier.testTag("filter_all"))
                    Platform.entries.forEach { candidate ->
                        ChoiceChip(candidate.label, platform == candidate, { platformName = candidate.name },
                            modifier = Modifier.testTag("filter_${candidate.name}"),
                            icon = if (candidate == Platform.INSTAGRAM) SukhoIcons.Instagram else SukhoIcons.TikTok)
                    }
                }
            }
            item {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(false to "Belum Ditangani", true to "Selesai").forEach { (done, label) ->
                            val count = all.count { (platform == null || platform == it.platform) && it.completed == done }
                            Surface(
                                onClick = { completed = done }, modifier = Modifier.weight(if (done) 0.85f else 1.4f).heightIn(min = 48.dp).testTag(if (done) "status_done" else "status_open"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (completed == done) MaterialTheme.colorScheme.surface else Color.Transparent,
                                shadowElevation = if (completed == done) 1.dp else 0.dp,
                            ) {
                                Row(Modifier.padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = if (completed == done) FontWeight.Bold else FontWeight.Medium,
                                        color = if (completed == done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(Modifier.width(6.dp))
                                    Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            completed -> "Sudah ditangani"
                            type == InteractionType.DM -> "Pesan terbaru"
                            else -> "Komentar terbaru"
                        },
                        Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                    )
                    Text("${items.size} interaksi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (items.isEmpty()) item {
                EmptyInbox(completed, type)
            } else itemsIndexed(items, key = { _, it -> it.id }) { index, thread ->
                InteractionCard(thread, alternate = index % 3 == 1, onClick = { onOpen(thread.id) })
            }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(SukhoIcons.Shield, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(5.dp))
                    Text(if (state.isDemo) "Data contoh · Tidak terhubung ke akun asli" else "Data cloud · Akun sosial belum terhubung",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun InteractionCard(thread: Interaction, alternate: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick, modifier = Modifier.fillMaxWidth().testTag("thread_${thread.id}"),
        shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp,
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box {
                Avatar(thread.initials, alternate = alternate)
                if (!thread.completed) Box(Modifier.align(Alignment.BottomEnd).offset(2.dp, 2.dp).size(11.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).padding(2.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(thread.sender, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.width(6.dp))
                    Text(timeLabel(thread.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(5.dp))
                Text(thread.preview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformIcon(thread.platform)
                    Spacer(Modifier.width(5.dp))
                    Text(thread.platform.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text("  ·  ${thread.type.label}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.weight(1f))
                    if (thread.completed) Icon(SukhoIcons.CheckCircle, "Selesai", Modifier.size(15.dp), tint = MaterialTheme.colorScheme.tertiary)
                    else Icon(SukhoIcons.Chevron, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}

@Composable
private fun EmptyInbox(completed: Boolean, type: InteractionType) {
    val category = if (type == InteractionType.DM) "DM" else "komentar"
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 38.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IconDisc(if (completed) SukhoIcons.CheckCircle else SukhoIcons.Inbox, Modifier.size(64.dp))
            Spacer(Modifier.height(18.dp))
            Text(
                when {
                    completed -> "Belum ada $category yang selesai"
                    type == InteractionType.DM -> "DM sedang tenang"
                    else -> "Belum ada komentar baru"
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                if (completed) "Yang ditandai selesai pada halaman ini akan tampil di sini."
                else "${if (type == InteractionType.DM) "Pesan" else "Komentar"} baru akan muncul di sini. Coba juga filter platform lainnya.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


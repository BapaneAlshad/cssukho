package com.sukhoramen.cs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sukhoramen.cs.BuildConfig
import com.sukhoramen.cs.MainViewModel
import com.sukhoramen.cs.data.*

@Composable
fun SettingsScreen(state: InboxState, model: MainViewModel, onExit: () -> Unit = {}) {
    if (!state.isDemo) {
        LiveSettingsScreen(state, model, onExit)
        return
    }
    val colors = MaterialTheme.colorScheme
    var resetDialog by remember { mutableStateOf(false) }
    var accountDialog by remember { mutableStateOf<Platform?>(null) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("settings_scroll").padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BrandHeader()
        Column(Modifier.padding(top = 10.dp, bottom = 4.dp)) {
            Text("Pengaturan", style = MaterialTheme.typography.headlineLarge)
            Text("Ruang kerja yang nyaman untukmu.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        SoftCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar("AD")
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("Admin Sukho", style = MaterialTheme.typography.titleMedium)
                    Text("Satu admin · Ruang kerja demo", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Icon(SukhoIcons.Shield, null, Modifier.size(22.dp), tint = colors.primary)
            }
        }
        SectionHeading("Akun terhubung", "Status di bawah adalah simulasi.")
        SoftCard(Modifier.fillMaxWidth()) {
            Platform.entries.forEachIndexed { index, platform ->
                val affected = state.affectedPlatform == platform
                val expired = affected && state.scenario == DemoScenario.EXPIRED
                val unsupported = affected && state.scenario == DemoScenario.UNSUPPORTED
                val status = if (expired) "Perlu login ulang · demo" else if (unsupported) "Belum didukung · demo" else "Terhubung · demo"
                Surface(onClick = { accountDialog = platform }, color = colors.surface, modifier = Modifier.testTag("account_${platform.name}")) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconDisc(if (platform == Platform.INSTAGRAM) SukhoIcons.Instagram else SukhoIcons.TikTok)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(platform.label, style = MaterialTheme.typography.titleSmall)
                            Text("@sukhoramen · akun contoh", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            Spacer(Modifier.height(5.dp))
                            Text(status, style = MaterialTheme.typography.labelSmall, color = if (expired || unsupported) colors.error else colors.tertiary)
                        }
                        Icon(SukhoIcons.Chevron, "Informasi akun ${platform.label}", Modifier.size(16.dp), tint = colors.outline)
                    }
                }
                if (index == 0) HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = colors.outlineVariant)
            }
        }
        SectionHeading("Tampilan", "Biru yang sama, suasana pilihanmu.")
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconDisc(SukhoIcons.Sun)
                    Spacer(Modifier.width(12.dp))
                    Column { Text("Tema aplikasi", style = MaterialTheme.typography.titleSmall); Text("Tersimpan di perangkat ini", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        ChoiceChip(mode.label, state.theme == mode, { model.theme(mode) }, Modifier.testTag("theme_${mode.name}"))
                    }
                }
            }
        }
        SoftCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
                IconDisc(SukhoIcons.Bell)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Notifikasi", style = MaterialTheme.typography.titleSmall)
                    Text("Push belum aktif", Modifier.padding(top = 4.dp), style = MaterialTheme.typography.labelMedium, color = colors.primary)
                    Text("Notifikasi asli tersedia setelah backend dan izin API terhubung. Coba interaksi baru melalui simulasi di bawah.", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
        SectionHeading("Skenario demo", "Coba kondisi aplikasi tanpa akun asli.")
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                NoticeCard("Semua tindakan hanya mengubah data contoh di perangkat ini.")
                Text("Platform untuk simulasi akses & pengiriman", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Platform.entries.forEach { platform ->
                        ChoiceChip(platform.label, state.affectedPlatform == platform, { model.scenario(state.scenario, platform) },
                            Modifier.testTag("scenario_platform_${platform.name}"))
                    }
                }
                DemoScenario.entries.forEach { scenario ->
                    Surface(
                        onClick = { model.scenario(scenario, state.affectedPlatform) }, modifier = Modifier.fillMaxWidth().testTag("scenario_${scenario.name}"),
                        shape = RoundedCornerShape(15.dp), color = if (state.scenario == scenario) colors.primaryContainer else colors.surface,
                    ) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp).heightIn(min = 38.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = state.scenario == scenario, onClick = null, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(scenario.label, style = MaterialTheme.typography.titleSmall)
                                Text(scenario.description, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                        }
                    }
                }
                HorizontalDivider(color = colors.outlineVariant)
                OutlinedButton(onClick = model::incoming, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("simulate_incoming"), shape = RoundedCornerShape(15.dp)) {
                    Icon(SukhoIcons.Bell, null, Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)); Text("Simulasikan interaksi baru")
                }
                OutlinedButton(onClick = model::duplicate, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(15.dp)) {
                    Icon(SukhoIcons.Refresh, null, Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)); Text("Uji event berulang")
                }
            }
        }
        TextButton(onClick = { resetDialog = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("reset_demo")) {
            Icon(SukhoIcons.Refresh, null, Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)); Text("Reset data demo", color = colors.error)
        }
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("open_live_mode")) {
            Text("Buka mode cloud")
        }
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Dibuat untuk melayani dengan hangat.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Text("SUKHO RAMEN · ${BuildConfig.VERSION_NAME}", Modifier.padding(top = 5.dp), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
    }
    if (resetDialog) AlertDialog(
        onDismissRequest = { resetDialog = false },
        title = { Text("Reset data demo?") },
        text = { Text("Balasan, draft, dan status selesai akan dikembalikan ke data contoh awal. Pilihan tema tetap disimpan.") },
        confirmButton = { TextButton(onClick = { model.reset(); resetDialog = false }, modifier = Modifier.testTag("confirm_reset")) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { resetDialog = false }) { Text("Batal") } },
    )
    accountDialog?.let { platform ->
        AlertDialog(
            onDismissRequest = { accountDialog = null },
            icon = { PlatformIcon(platform, Modifier.size(30.dp)) },
            title = { Text("${platform.label} dalam mode demo") },
            text = { Text("Belum ada akun sosial asli yang terhubung. Status dan percakapan di aplikasi ini adalah simulasi. Autentikasi resmi, hubungkan/putuskan akun, dan pengecekan izin API ditambahkan pada tahap backend.") },
            confirmButton = { TextButton(onClick = { accountDialog = null }) { Text("Mengerti") } },
        )
    }
}

@Composable
private fun LiveSettingsScreen(state: InboxState, model: MainViewModel, onExit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        BrandHeader(isDemo = false)
        Column(Modifier.padding(top = 10.dp, bottom = 4.dp)) {
            Text("Pengaturan", style = MaterialTheme.typography.headlineLarge)
            Text("Akses cloud pada perangkat ini.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
        SoftCard(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar("AD")
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("Admin Sukho", style = MaterialTheme.typography.titleMedium)
                    Text("Satu perangkat aktif · dilindungi PIN", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Icon(SukhoIcons.Shield, null, Modifier.size(22.dp), tint = colors.primary)
            }
        }
        SectionHeading("Akun sosial", "Status aktual dari backend, tanpa simulasi koneksi.")
        SoftCard(Modifier.fillMaxWidth()) {
            Platform.entries.forEachIndexed { index, platform ->
                val connection = state.connections[platform]
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconDisc(if (platform == Platform.INSTAGRAM) SukhoIcons.Instagram else SukhoIcons.TikTok)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(platform.label, style = MaterialTheme.typography.titleSmall)
                        Text(when (connection?.status) {
                            "connected" -> "Terhubung"
                            "reauth_required" -> "Perlu login ulang"
                            "pending_access" -> "Menunggu akses API"
                            "unsupported" -> "Belum didukung"
                            else -> "Belum terhubung"
                        }, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
                if (index == 0) HorizontalDivider(Modifier.padding(horizontal = 18.dp), color = colors.outlineVariant)
            }
        }
        SectionHeading("Tampilan", "Biru yang sama, suasana pilihanmu.")
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Tema aplikasi", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    ThemeMode.entries.forEach { mode -> ChoiceChip(mode.label, state.theme == mode, { model.theme(mode) }) }
                }
            }
        }
        NoticeCard("Balasan dan notifikasi asli tersedia setelah akun sosial terhubung melalui API resmi.")
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("lock_live")) {
            Text("Kunci aplikasi / Coba Demo")
        }
        Text("SUKHO RAMEN · ${BuildConfig.VERSION_NAME}", Modifier.fillMaxWidth().padding(bottom = 16.dp),
            style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(Modifier.padding(top = 3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


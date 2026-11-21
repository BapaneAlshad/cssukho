package com.sukhoramen.cs.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sukhoramen.cs.EntryPage
import com.sukhoramen.cs.EntryState

@Composable
fun EntryScreen(state: EntryState, onActivate: (String, String, String) -> Unit,
                onUnlock: (String) -> Unit, onDemo: () -> Unit, onForget: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var confirmForget by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val setup = state.page == EntryPage.SETUP
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 30.dp),
            verticalArrangement = Arrangement.Center) {
            BrandHeader(showBadge = false)
            Spacer(Modifier.height(38.dp))
            Text(if (setup) "Aktivasi perangkat" else "Masukkan PIN", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(8.dp))
            Text(if (setup) "Masukkan kode sekali pakai, lalu buat PIN 6 angka untuk membuka aplikasi."
                 else "Buka ruang kerja SUKHO RAMEN dengan PIN 6 angka.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            SoftCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (setup) OutlinedTextField(code, { code = it.uppercase().take(19) },
                        label = { Text("Kode aktivasi") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("activation_code"))
                    OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(6) },
                        label = { Text(if (setup) "Buat PIN 6 angka" else "PIN 6 angka") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().testTag("entry_pin"))
                    if (setup) OutlinedTextField(repeat, { repeat = it.filter(Char::isDigit).take(6) },
                        label = { Text("Ulangi PIN") }, singleLine = true,
                        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth().testTag("entry_pin_repeat"))
                    state.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = { keyboard?.hide(); if (setup) onActivate(code, pin, repeat) else onUnlock(pin) },
                        enabled = !state.busy && pin.length == 6 && (!setup || repeat.length == 6),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag(if (setup) "activate_device" else "unlock_live"),
                        shape = RoundedCornerShape(15.dp)) {
                        if (state.busy) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp)
                        else Text(if (setup) "Aktifkan perangkat" else "Buka aplikasi")
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onDemo, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("try_demo"), shape = RoundedCornerShape(15.dp)) {
                Text("Coba Demo")
            }
            if (!setup) TextButton(onClick = { confirmForget = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Lupa PIN? Aktifkan ulang")
            }
        }
    }
    if (confirmForget) AlertDialog(onDismissRequest = { confirmForget = false },
        title = { Text("Aktifkan ulang perangkat?") },
        text = { Text("PIN dan akses di ponsel ini akan dihapus. Anda memerlukan kode aktivasi baru untuk mode cloud.") },
        confirmButton = { TextButton(onClick = { confirmForget = false; onForget() }) { Text("Aktifkan ulang") } },
        dismissButton = { TextButton(onClick = { confirmForget = false }) { Text("Batal") } })
}


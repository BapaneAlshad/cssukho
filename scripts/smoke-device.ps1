param([Parameter(Mandatory)][string]$Serial)
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
$sdkPath = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adbPath = Join-Path $sdkPath 'platform-tools\adb.exe'
$apkPath = Join-Path $projectPath 'artifacts\CS-Sukho-Ramen-demo.apk'

function Read-Hierarchy {
    & $adbPath -s $Serial shell uiautomator dump /sdcard/sukho-smoke.xml | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'Tidak dapat membaca UI perangkat.' }
    return [xml]((& $adbPath -s $Serial shell cat /sdcard/sukho-smoke.xml) -join "`n")
}

function Tap-Label([string]$Label, [bool]$Description = $false, [bool]$ScrollUp = $false) {
    for ($attempt = 0; $attempt -lt 3; $attempt++) {
        $tree = Read-Hierarchy
        $attribute = if ($Description) { 'content-desc' } else { 'text' }
        $node = $tree.SelectNodes('//node') | Where-Object { $_.GetAttribute($attribute) -eq $Label } | Select-Object -First 1
        if ($node) {
            $bounds = [regex]::Match($node.GetAttribute('bounds'), '\[(\d+),(\d+)\]\[(\d+),(\d+)\]')
            $tapX = [int](([int]$bounds.Groups[1].Value + [int]$bounds.Groups[3].Value) / 2)
            $tapY = [int](([int]$bounds.Groups[2].Value + [int]$bounds.Groups[4].Value) / 2)
            & $adbPath -s $Serial shell input tap $tapX $tapY
            Write-Output "Tapped: $Label"
            return
        }
        if ($ScrollUp) { & $adbPath -s $Serial shell input swipe 540 500 540 1600 300 }
        Start-Sleep -Milliseconds 500
    }
    throw "Kontrol tidak ditemukan: $Label"
}

& $adbPath -s $Serial install -r $apkPath
if ($LASTEXITCODE -ne 0) { throw 'Pemasangan APK gagal.' }
& $adbPath -s $Serial shell am force-stop com.sukhoramen.cs
& $adbPath -s $Serial shell am start -W -n com.sukhoramen.cs/com.sukhoramen.cs.MainActivity
Tap-Label 'Coba Demo'
Start-Sleep -Milliseconds 700
$tree = Read-Hierarchy
if (-not $tree.OuterXml.Contains('MODE DEMO')) { throw 'Pengujian ini hanya untuk APK Mode Demo.' }
if (-not $tree.OuterXml.Contains('Nadia Putri')) {
    Tap-Label 'Selesai'
    Tap-Label 'Nadia Putri'
    Tap-Label 'Buka kembali' $false $true
    & $adbPath -s $Serial shell am force-stop com.sukhoramen.cs
    & $adbPath -s $Serial shell am start -W -n com.sukhoramen.cs/com.sukhoramen.cs.MainActivity | Out-Null
    Tap-Label 'Coba Demo'
}

Tap-Label 'Nadia Putri'
Tap-Label 'Tulis balasan hangat…'
# Wait for the real keyboard/window to be ready before injecting hardware events.
$null = Read-Hierarchy
& $adbPath -s $Serial shell input text 'Halo%sKak%sreservasi%sbisa'
$tree = Read-Hierarchy
$editor = $tree.SelectNodes('//node') | Where-Object { $_.GetAttribute('class') -eq 'android.widget.EditText' } | Select-Object -First 1
if ($editor.GetAttribute('text') -ne 'Halo Kak reservasi bisa') { throw "Input berubah: $($editor.GetAttribute('text'))" }
Write-Output 'PASS: native input preserves exact payload.'

Tap-Label 'Kirim balasan' $true
Start-Sleep -Seconds 2
$tree = Read-Hierarchy
if (-not $tree.OuterXml.Contains('Terkirim')) { throw 'Konfirmasi pengiriman tidak terlihat.' }
if (-not $tree.OuterXml.Contains('Halo Kak reservasi bisa')) { throw 'Teks balasan terkirim tidak cocok.' }
Tap-Label 'Tandai selesai' $false $true
$tree = Read-Hierarchy
if (-not $tree.OuterXml.Contains('Buka kembali')) { throw 'Status selesai belum diperbarui.' }
Start-Sleep -Milliseconds 700

& $adbPath -s $Serial shell am force-stop com.sukhoramen.cs
& $adbPath -s $Serial shell am start -W -n com.sukhoramen.cs/com.sukhoramen.cs.MainActivity
Tap-Label 'Coba Demo'
Tap-Label 'Selesai'
Tap-Label 'Nadia Putri'
Tap-Label 'Buka kembali' $false $true
Tap-Label 'Kembali' $true
Write-Output 'PASS: optimized APK reply, completion across process restart, and reopen.'


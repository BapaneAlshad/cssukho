param([switch]$SkipChecks)
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $PSScriptRoot
if (-not $env:JAVA_HOME) {
    $studioJbr = Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'
    if (Test-Path -LiteralPath $studioJbr) { $env:JAVA_HOME = $studioJbr }
}
if (-not (Test-Path -LiteralPath (Join-Path $projectPath 'local.properties'))) {
    $sdkPath = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
    if (-not (Test-Path -LiteralPath $sdkPath)) { throw 'Android SDK tidak ditemukan. Atur ANDROID_HOME atau buka proyek lewat Android Studio.' }
    [IO.File]::WriteAllText((Join-Path $projectPath 'local.properties'), "sdk.dir=$($sdkPath.Replace('\', '/').Replace(':', '\:'))`n")
}
Push-Location $projectPath
try {
    [string[]]$buildTasks = @(':app:assembleDemo')
    if (-not $SkipChecks) { $buildTasks = @(':app:testDebugUnitTest', ':app:lintDebug', ':app:assembleDemo') }
    & (Join-Path $projectPath 'gradlew.bat') @buildTasks --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle gagal dengan kode $LASTEXITCODE" }
    New-Item -ItemType Directory -Path (Join-Path $projectPath 'artifacts') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectPath 'app\build\outputs\apk\demo\app-demo.apk') -Destination (Join-Path $projectPath 'artifacts\CS-Sukho-Ramen-demo.apk')
    Write-Output "APK: $(Join-Path $projectPath 'artifacts\CS-Sukho-Ramen-demo.apk')"
} finally { Pop-Location }


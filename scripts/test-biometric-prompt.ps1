param(
    [string]$Adb = '',
    [string]$Serial = 'emulator-5554',
    [Parameter(Mandatory = $true)][string]$DevicePin,
    [string]$TestClass = 'com.personalfinance.tracker.BiometricPromptAcceptanceTest'
)
$ErrorActionPreference = 'Stop'
if (-not $Adb) {
    $sdkRoot = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT } elseif ($env:LOCALAPPDATA) { Join-Path $env:LOCALAPPDATA 'Android/Sdk' }
    if ($sdkRoot) { $Adb = Join-Path $sdkRoot 'platform-tools/adb.exe' }
    if (-not $Adb -or -not (Test-Path -LiteralPath $Adb -PathType Leaf)) {
        $adbCommand = Get-Command adb -ErrorAction SilentlyContinue
        if (-not $adbCommand) { throw 'Set ANDROID_HOME or ANDROID_SDK_ROOT, add adb to PATH, or pass -Adb.' }
        $Adb = $adbCommand.Source
    }
}
if ($Serial -notmatch '^emulator-\d+$') { throw 'This driver is for disposable emulators only.' }
if ($DevicePin -notmatch '^\d{4,10}$') { throw 'Expected an emulator-only numeric PIN.' }
$log = Join-Path $env:TEMP "biometric-prompt-$([guid]::NewGuid()).log"
$err = "$log.err"
$run = Start-Process -FilePath $Adb -ArgumentList @('-s', $Serial, 'shell', 'am', 'instrument', '-w',
    '-e', 'emulatorFingerDriver', 'true', '-e', 'devicePin', $DevicePin, '-e', 'class', $TestClass,
    'com.personalfinance.tracker.test/androidx.test.runner.AndroidJUnitRunner') -PassThru -WindowStyle Hidden `
    -RedirectStandardOutput $log -RedirectStandardError $err
$handled = 0
$deadline = [DateTime]::UtcNow.AddMinutes(4)
while (-not $run.HasExited) {
    if ([DateTime]::UtcNow -gt $deadline) { $run.Kill(); throw "Instrumentation timed out; log: $log" }
    $content = Get-Content -LiteralPath $log -Raw -ErrorAction SilentlyContinue
    $events = @()
    if ($content) { $events = @([regex]::Matches($content, 'BIOMETRIC_FINGER:(\d+)')) }
    while ($handled -lt $events.Count) {
        & $Adb -s $Serial emu finger touch $events[$handled].Groups[1].Value
        if ($LASTEXITCODE -ne 0) { throw 'Emulator finger injection failed.' }
        $handled++
    }
    Start-Sleep -Milliseconds 100
    $run.Refresh()
}
$output = Get-Content -LiteralPath $log -Raw
Write-Output $output
Get-Content -LiteralPath $err
Write-Output "Raw instrumentation log: $log"
if ($output -notmatch 'OK \(\d+ tests?\)' -or $output -match 'FAILURES!!!|INSTRUMENTATION_FAILED') {
    throw 'Biometric prompt acceptance did not pass.'
}

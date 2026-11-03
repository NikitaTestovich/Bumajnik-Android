param(
    [string]$Keystore = 'H:\AndroidKeys\FinanceTracker-release.jks',
    [string]$Alias = 'finance-tracker'
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $Keystore -PathType Leaf)) {
    throw "Keystore not found: $Keystore"
}
$Keystore = (Resolve-Path -LiteralPath $Keystore).Path
$androidDir = Join-Path (Split-Path $PSScriptRoot -Parent) 'android'
$localJdk = Join-Path $androidDir '.tooling\jdk-21'
if (Test-Path -LiteralPath (Join-Path $localJdk 'bin\java.exe') -PathType Leaf) {
    $env:JAVA_HOME = $localJdk
}
$localGradle = Join-Path $androidDir '.tooling\gradle-home'
if (Test-Path -LiteralPath $localGradle -PathType Container) {
    $env:GRADLE_USER_HOME = $localGradle
}
$java = Join-Path $env:JAVA_HOME 'bin\java.exe'
$keytool = Join-Path $env:JAVA_HOME 'bin\keytool.exe'
& $java (Join-Path $PSScriptRoot 'ReleaseBuild.java') $keytool $Keystore $Alias $androidDir
if ($LASTEXITCODE -ne 0) { throw "Release build failed with exit code $LASTEXITCODE" }

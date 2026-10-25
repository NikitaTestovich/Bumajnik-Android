# Finance Tracker for Android

[English user guide](../docs/user-guide-en.md) · [Руководство на русском](../docs/user-guide-ru.md)

The app supports Android 8.0 (API 26) and newer. Package ID: `com.personalfinance.tracker`.
Data stays on the device; account setup and a network connection are not needed for app use.

## Build a debug APK on Windows

Prerequisites:

- JDK 21. The project's Gradle 8.13 wrapper and Android Gradle Plugin 8.13.2 were built with JDK 21 on this workstation. Its Android Studio Java 25 runtime cannot run this Gradle Kotlin DSL build; select JDK 21 explicitly.
- Android SDK with API 37 platform, compatible Android SDK Build-Tools, and Platform-Tools (`adb`). Install them with Android Studio's SDK Manager and accept SDK licenses. `compileSdk` and `targetSdk` are 37; Java/Kotlin bytecode targets 17.
- Internet access for the first Gradle/dependency download, or a populated local cache.

Set your SDK path in an untracked `android/local.properties`, for example:

```properties
sdk.dir=C:/Users/YOUR_NAME/AppData/Local/Android/Sdk
```

Install/extract JDK 21 so that `android/.tooling/jdk-21/bin/java.exe` exists. This local tools directory is ignored by Git. Alternatively, set `JAVA_HOME` below to your existing JDK 21 installation. Open PowerShell in this `android/` directory:

```powershell
$env:JAVA_HOME = (Resolve-Path '.tooling/jdk-21').Path
$env:GRADLE_USER_HOME = Join-Path (Get-Location).Path '.tooling/gradle-home'
& "$env:JAVA_HOME/bin/java.exe" -version
.\gradlew.bat :app:archiveDebugApk --no-daemon --console=plain
```

Wait for `BUILD SUCCESSFUL`. The APK is written to `android/app/build/outputs/apk/debug/app-debug.apk`, signed with the local debug key and installable without release signing credentials. The same build refreshes `AppAndroidBudjets.zip` in the project root; it contains `AppAndroidBudjets/app-debug.apk`. Run `:app:archiveDebugApk` for each new build to keep the archive current. The generated archive stays local and is excluded from Git. Use the wrapper supplied with this project; a separate Gradle installation is unnecessary.

This configuration produced a debug APK on 2026-10-21. The build reported SDK XML-version and compileSdk 37 / AGP compatibility warnings, plus a native-library stripping warning; these did not prevent packaging. Updating local SDK tools may change the warnings. No device runtime result is implied by a successful build.

## Install on a device or emulator

For USB installation, enable Developer options and USB debugging on an Android 8.0+ phone, connect it, unlock it, and accept the debugging authorization. Alternatively, start a compatible Android emulator. In PowerShell, still in `android/`, set the Platform-Tools path for your SDK:

```powershell
$adb = 'C:/Users/YOUR_NAME/AppData/Local/Android/Sdk/platform-tools/adb.exe'
& $adb devices -l
& $adb install -r '.\app\build\outputs\apk\debug\app-debug.apk'
& $adb shell am start -n 'com.personalfinance.tracker/.MainActivity'
```

Proceed with installation once the device list shows an authorized target in state `device`; resolve `unauthorized` on the phone and reconnect an `offline` device. With several targets, insert `-s DEVICE_SERIAL` before `install` and `shell` in the commands. Installation should report `Success`. You can also launch **Finance Tracker** / **Учёт финансов** from the launcher.

To install directly on a phone, transfer the APK, open it in the phone's file manager, and allow installation from that source if Android asks. Names and locations of these controls depend on the device. Complete the installation, open the app, and enter your name and currency.

`install -r` updates an existing installation while retaining local data when package and signing certificate match. A debug APK built with another machine's key may fail with a signature conflict. Export your personal CSV backup before uninstalling an existing installation: uninstalling or clearing storage removes app data, and CSV excludes profile, settings, goal, rates, and family imports. Automatic Android app backup is disabled.

## Optional developer checks

With the same JDK and Gradle environment:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin --no-daemon --console=plain
```

With a connected, authorized Android device/emulator, run the instrumented checks separately:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --no-daemon --console=plain
```

On 2026-10-21, `:app:testDebugUnitTest` passed all 60 JVM tests. The Android instrumentation suite passed all 20 tests on the `Medium_Phone_API_37.0` Android 17 emulator, including CSV export/import flows and date-picker/manual-entry coverage. The Xiaomi M2101K6G (Android 13) is authorized, but MIUI terminated the instrumentation process before its UI tests could start; the built app itself was launched successfully on that device. The APK path is `android/app/build/outputs/apk/debug/app-debug.apk`; the refreshed root archive is `AppAndroidBudjets.zip`. The guides describe the implemented screens and localized strings; they contain no fabricated screenshots.

On 2026-10-22, the visual redesign uses gray-blue (#52697F), teal (#247A75), neutral cards, a pale teal navigation indicator, and accessible text colors. Home includes monthly totals and goal progress; reports and family use comparison columns and category bars. All 74 JVM tests passed, including a regression check for Material 3 container colors. Both app and test APKs compiled, and the complete FinanceTrackerAcceptanceTest passed 18 tests on emulator-5554. The archive task refreshes the root AppAndroidBudjets.zip with AppAndroidBudjets/app-debug.apk. Physical-device testing was not repeated for this redesign.

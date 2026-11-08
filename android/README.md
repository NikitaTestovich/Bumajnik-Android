# Finance Tracker for Android

## Version 1.22 — development build

This checkout builds versionName 1.22 / versionCode 7. Debug builds use a local test key; only an owner-signed release is suitable for updating the published release. See [verification](../docs/testing-1.22.md).

## Version 1.21 — released

The 1.21 release used versionName 1.21 / versionCode 6. Use the existing owner release key for production and name the verified release asset `finance-tracker-vc6.apk`. Debug output has a different signature and must not be installed over the release on a personal phone. See [verification](../docs/testing-1.21.md).

> **1.20 released (2026-11-02):** The 1.20 release used versionName 1.20 and versionCode 5. Download the owner-signed APK from [GitHub Releases](https://github.com/NikitaTestovich/Bumajnik-Android/releases/latest). Local debug APKs are test/bridge builds with a different signature. Public download and the phone's live no-newer-version check passed; the subsequent 1.20-to-1.21 phone upgrade passed with fictional personal records. See [test status](../docs/testing-1.20.md).

## Version 1.17

Version 1.17 (code 4) adds four app-wide themes, segmented savings progress, remembered Operations tabs and optional icon labels. It preserves enabled biometric preference during temporary unavailability when settings are saved. Package, schema 3 and debug signer remain compatible. Install the matching APK with `adb -s <disposable-emulator-serial> install -r` to retain data. `:app:archiveDebugApk` builds the ignored root ZIP. Full verification, upgrade evidence and remaining device limitations: [release report](../docs/testing-1.17.md).

## Version 1.15 verification (2026-10-28)

Version 1.15 uses `versionCode=3` and a non-destructive database migration to schema 3. It adds biometric-first entry, optional income comments and compatible CSV formats, editable/archivable categories with bundled icons, saved Home customization and an optional monthly expense doughnut. The package ID remains `com.personalfinance.tracker`; install the matching debug APK with `adb install -r` to retain local records. The generated APK is `app/build/outputs/apk/debug/app-debug.apk`; `:app:archiveDebugApk` refreshes the local root `AppAndroidBudjets.zip` containing the same APK. These outputs and signing keys are excluded from Git.

Run the full JVM and Android suites on a disposable emulator. The Android acceptance tests include cases that clear the local database and must not target a phone containing personal data. See [version 1.15 test results](../docs/testing-1.15.md) and the [English](../docs/user-guide-en.md) or [Russian](../docs/user-guide-ru.md) guide for the current behavior. The version 1.1 section below remains historical evidence.

## Version 1.1 verification (2026-10-26)

The 1.1 debug APK (`versionCode=2`) was built with the existing toolchain and the same signing certificate as the saved 1.0 APK. Both APK signatures were verified. Installing 1.1 over 1.0 with `adb install -r` preserved the seeded profile, personal transactions, standard/custom categories, goal, family records and exchange rates. The database schema remains version 2. The root archive was checked to contain the exact same APK as the build output.

All 89 JVM tests passed. The Android 17 emulator run reported 36 tests with no failures: 35 checks executed successfully, while the two-stage upgrade test was intentionally skipped in that generic run and passed separately before and after the update. Coverage includes six protection checks, a real system PIN recovery flow, actual CSV open/save dialogs, CSV data export/import and duplicate prevention. Earlier picker skips were caused by incomplete MIME-type package-visibility declarations; those declarations are corrected in 1.1.

An independent code review identified lifecycle/cancellation and category grouping issues. Regression tests reproduced them before fixes; the complete suites then passed. Physical fingerprint/face authentication has not been tested on a connected phone for this version. A supported enrolled biometric is required to exercise that path on a real device.

For the real credential test on a disposable emulator, configure a screen-lock PIN first and pass it as the `devicePin` instrumentation argument. Never run this suite against a phone containing real app data: the acceptance tests reset their test database.

[English user guide](../docs/user-guide-en.md) · [Руководство на русском](../docs/user-guide-ru.md)

The app supports Android 8.0 (API 26) and newer. Package ID: `com.personalfinance.tracker`.
Data stays on the device; account setup and a network connection are not needed for app use.

## Build a debug APK on Windows

Prerequisites:

- JDK 21. Version 1.20 uses the Gradle 9.6.0 wrapper, Android Gradle Plugin 9.4.0 and KSP 2.3.6. Select JDK 21 explicitly for a reproducible build; Android Studio may use a separate runtime.
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

The version 1.15 regression suite covers unchanged category translations, selected chart text at 1.5x font scale, zero-conversion expense cards, and an opt-in seed/force-stop/verify Home layout persistence test (HomeProcessRestartTest).

## Version 1.17 Home and export update

Monthly expenses is visible for new installations, missing preference entries and Restore defaults. Users can hide it; saved visibility (including previously hidden expenses) and order are retained. Reports always export the current eight-column CSV including income comments; the version 1.1 checkbox is removed. Version 1.1 cannot read these exports. Seven-column import remains supported. A pending export retains its chosen period through recreation and writes current format.

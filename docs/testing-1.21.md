# Version 1.21 verification

## Scope

Maintenance release: Android metadata 1.21/code 6 and documentation updates. App implementation, package and database remain unchanged from 1.20. No new behavior is claimed. See [specification](specs/version-1.21.md).

## Results

Preparation started 2026-11-02. Debug build, local archive and Android test APK assembled successfully with JDK 21 and Gradle 9.6. Full JVM suite: 158 tests, zero failures/errors. Build tools report package `com.personalfinance.tracker`, versionName 1.21 and versionCode 6. The ZIP entry matches the built debug APK by SHA-256. Full Android instrumentation completed on the guarded disposable API 37 emulator: 129 cases, 110 passed, 19 assumption/ignored skips, zero failures. Skips are not passes and include staged migration, biometric and credential scenarios requiring special arguments. No destructive tests ran on the phone.

## Release gates

- Completed: owner-signed artifact verified as recorded below. Keep the same certificate for future builds.
- Publish only the verified `finance-tracker-vc6.apk` with stable tag `v1.21` and release notes.
- On a phone with release 1.20/code 5, check discovery, download, installation confirmation and retained personal records. Do not uninstall or clear storage.
- After installation, verify 1.21/code 6 and that another check does not offer the same release again.
- Real personal-file recovery and broader provider/device coverage remain scoped as in [1.20 verification](testing-1.20.md).

## Review

Independent diff review confirmed that executable changes are limited to versionName/versionCode; no application logic, storage or signing configuration changed. A contradictory historical build-version sentence in android/README.md was corrected. Production signing subsequently passed; the real upgrade gate remains open.

## Owner-signed artifact, 2026-11-02

The owner ran the interactive release build successfully. Android build tools verified the resulting APK: package `com.personalfinance.tracker`, versionName `1.21`, versionCode `6`. Certificate SHA-256 matches public 1.20: `65f686e6930483501232d5bdc0d6cae8a2f62b5e1520e316b780a94ffb463d69`. APK SHA-256: `544020a98005569928938ba957567f86c05906ecb720dc00363f6df140e43f5d`. Staged asset: `finance-tracker-vc6.apk` (excluded from Git). This is artifact verification, not proof of an installed in-app upgrade. The physical phone remains on release 1.20/code 5 for that later check.

The root local ZIP was refreshed with the verified debug 1.21 APK; the previous archive was backed up outside Git. This debug archive is separate from the owner-signed release asset.

## Physical release upgrade, 2026-11-03

- Public GitHub `releases/latest` returned stable `v1.21` with `finance-tracker-vc6.apk`. Downloaded asset SHA-256 `544020a98005569928938ba957567f86c05906ecb720dc00363f6df140e43f5d` matched the staged owner-signed APK.
- The connected Android 13 test phone initially had release 1.20/code 5 and a fictional November balance of +1075.00 BYN (income 1200.00, expense 125.00). The in-app Updates screen discovered `v1.21` and showed its notes and Update button.
- The owner completed Android installation on the phone. Package manager then reported release 1.21/code 6. The Home screen retained the same fictional balance and income/expense totals. A repeat live update check reported no newer compatible update.
- This confirms the normal release-to-release upgrade on this phone with those sample personal records. It does not establish every preference, family import, biometric enrollment, interrupted download/retry behavior or other Android versions.

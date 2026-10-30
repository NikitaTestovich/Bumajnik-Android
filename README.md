# Finance Tracker for Android

## Version 1.17

Version 1.17 (code 4) adds four app-wide themes, segmented savings progress, remembered Operations tabs and optional icon labels. It preserves enabled biometric preference during temporary unavailability when settings are saved. Package, schema 3 and debug signer remain compatible. Install the matching APK with `adb -s <disposable-emulator-serial> install -r` to retain data. `:app:archiveDebugApk` builds the ignored root ZIP. Full verification, upgrade evidence and remaining device limitations: [release report](docs/testing-1.17.md).

A private income and expense tracker for Android. Record everyday transactions, understand where money goes, and estimate how long a savings goal may take. Your records stay on your device; family reporting works through CSV files you choose to exchange.

> **Status:** Active testing. Feedback and bug fixes are welcome.

## Version 1.17 theme examples

Fictional-data Reports at 280dp with Russian text at 1.6 scale (the chart can scroll horizontally):

| Financial journal | Electric graphite | Color mosaic | Night analytics |
| :---: | :---: | :---: | :---: |
| <img src="docs/screenshots/theme-2-ru.png" alt="Financial journal Russian report" width="180"> | <img src="docs/screenshots/theme-6-ru.png" alt="Electric graphite Russian report" width="180"> | <img src="docs/screenshots/theme-8-ru.png" alt="Color mosaic Russian report" width="180"> | <img src="docs/screenshots/theme-9-ru.png" alt="Night analytics Russian report" width="180"> |

## Earlier screenshots

These examples were captured on an Android emulator using fictional demo profiles and contain no personal financial records.

| Home | Reports |
| :---: | :---: |
| <img src="docs/screenshots/home-en.png" alt="Monthly balance, income, expenses, savings goal, and latest transaction" width="240"> | <img src="docs/screenshots/reports-en.png" alt="Report period, balance, prior-period comparison, and expense categories" width="240"> |
| **Family** | **Savings goal** |
| <img src="docs/screenshots/family-en.png" alt="Signed family balance, member comparison and combined expense categories" width="240"> | <img src="docs/screenshots/goal-en.png" alt="Savings goal target and already-saved amount" width="240"> |

Version 1.15 optional monthly expense widget, captured from the emulator with fictional data: [English chart](docs/screenshots/expenses-en.png) · [Russian chart with large text](docs/screenshots/expenses-ru.png).

## What it does

- Add, edit, and delete income and expense entries. Choose dates with a calendar or type them as `DD-MM-YYYY`. Add optional income comments of up to 150 Unicode characters.
- Start with standard expense categories, create your own, rename or archive them, and choose from 20 bundled icons. Archived categories remain on older expenses; icon-only edits preserve standard translations.
- Customize Home with hide/show controls and drag or arrow ordering. The monthly expense breakdown (a doughnut in themes 2, 6 and 8; horizontal bars in theme 9) is visible for fresh, missing and reset settings; previously saved visibility and order, including a hidden widget, remain. Hide it in Home settings or explore category amounts, including cards rounded to zero after conversion. Selected long names are bounded so the amount remains visible.
- Review monthly totals, category breakdowns, and comparisons with a previous period.
- Export current eight-column CSV reports/backups with income comments. The version 1.1 export checkbox is removed; version 1.1 cannot read current exports. Import either seven-column or eight-column files. A family member's report shows a signed balance, income/expense comparisons by member and combined expense categories; repeated records are skipped.
- Optionally protect access with a 4–10-character app code and available biometrics. When enabled, the biometric prompt opens automatically; the code appears after failure or cancellation. Recover a forgotten code through the phone screen lock without deleting records.
- Enter exchange rates manually when a report contains multiple currencies. Supported currencies: USD, EUR, RUB, and BYN.
- Plan one savings goal using your saved amount and the average surplus from up to three completed months.
- Switch between English and Russian in the profile.

## Privacy and backups

The app has no account or live synchronization. Financial records are stored locally. CSV export and import use Android's file picker; the app does not fetch exchange rates. A personal CSV backup contains your transaction history, so store and share it carefully. Export a backup before uninstalling the app or clearing its storage. The backup does not include every setting or saved family import; see the guide for details.

No real financial data, local SDK paths, signing keys, APKs, or backup files belong in this repository. The screenshots above show demo screens.

## Install or build

The project supports **Android 8.0 (API 26) and newer**. The generated debug APK is kept locally at `android/app/build/outputs/apk/debug/app-debug.apk`; it is not committed to Git. The root `AppAndroidBudjets.zip` archive is also a local build output.

See [build and installation instructions](android/README.md) for JDK 21, Android SDK, and Gradle wrapper setup.

To update, install a newer APK over the existing app without uninstalling it. Updates must use the same package ID and signing certificate to retain data. Current builds are for testing; a differently signed release requires a planned migration. See the [update guide](docs/user-guide-en.md#updating-without-losing-records).

## Guides and development

- [English user guide](docs/user-guide-en.md)
- [Руководство пользователя на русском](docs/user-guide-ru.md)
- [Development and bug reports](docs/CONTRIBUTING.md)
- [Current product specification](docs/specification.md)
- [Version 1.15 verification and device-test limits](docs/testing-1.15.md)

The project remains open for iterative testing and fixes. When reporting a bug, use sample data and remove names, amounts, and files that reveal real finances.

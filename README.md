# Finance Tracker for Android

A private income and expense tracker for Android. Record everyday transactions, understand where money goes, and estimate how long a savings goal may take. Your records stay on your device; family reporting works through CSV files you choose to exchange.

> **Status:** Active testing. Feedback and bug fixes are welcome.

## Screenshots

These examples were captured on an Android emulator with an empty demo profile and contain no personal financial records.

| Home | Reports |
| :---: | :---: |
| <img src="docs/screenshots/home-en.png" alt="Monthly balance, income, expenses, savings goal, and latest transaction" width="240"> | <img src="docs/screenshots/reports-en.png" alt="Report period, balance, prior-period comparison, and expense categories" width="240"> |
| **Family** | **Savings goal** |
| <img src="docs/screenshots/family-en.png" alt="Family CSV import and report period" width="240"> | <img src="docs/screenshots/goal-en.png" alt="Savings goal target and already-saved amount" width="240"> |

## What it does

- Add, edit, and delete income and expense entries. Choose dates with a calendar or type them as `DD-MM-YYYY`.
- Start with standard expense categories and create your own.
- Review monthly totals, category breakdowns, and comparisons with a previous period.
- Export personal reports and backups as CSV. Import a family member's report to see combined totals and per-member categories. Repeated records are skipped.
- Enter exchange rates manually when a report contains multiple currencies. Supported currencies: USD, EUR, RUB, and BYN.
- Plan one savings goal using your saved amount and the average surplus from up to three completed months.
- Switch between English and Russian in the profile.

## Privacy and backups

The app has no account or live synchronization. Financial records are stored locally. CSV export and import use Android's file picker; the app does not fetch exchange rates. A personal CSV backup contains your transaction history, so store and share it carefully. Export a backup before uninstalling the app or clearing its storage. The backup does not include every setting or saved family import; see the guide for details.

No real financial data, local SDK paths, signing keys, APKs, or backup files belong in this repository. The screenshots above show demo screens.

## Install or build

The project supports **Android 8.0 (API 26) and newer**. The generated debug APK is kept locally at `android/app/build/outputs/apk/debug/app-debug.apk`; it is not committed to Git. The root `AppAndroidBudjets.zip` archive is also a local build output.

See [build and installation instructions](android/README.md) for JDK 21, Android SDK, and Gradle wrapper setup.

## Guides and development

- [English user guide](docs/user-guide-en.md)
- [Руководство пользователя на русском](docs/user-guide-ru.md)
- [Development and bug reports](docs/CONTRIBUTING.md)

The project remains open for iterative testing and fixes. When reporting a bug, use sample data and remove names, amounts, and files that reveal real finances.

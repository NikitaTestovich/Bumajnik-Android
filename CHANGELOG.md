# Changelog

## 1.21 — unreleased

- Refresh README, both user guides, product specification and published-release verification notes.
- Increase Android versionName to 1.21 and versionCode to 6; application features and data formats are unchanged.
- Prepare a same-certificate update from public 1.20. The owner-signed APK has been verified; real in-app upgrade verification is pending.

## 1.20 — 2026-11-02

- Add operations search and expense comments, expandable report categories, distinct settings pages and chart palette improvements; display the expense doughnut in all four themes.
- Add full backup and GitHub update flows. Personal-data restoration to the owner-signed release was verified with synthetic data on an emulator; the public APK download and same-version phone check passed. A higher-version in-app installation and a user's actual data transfer remain unverified.
- Add fictional three-month CSV fixtures and a test report with remaining limitations.
- Update the Android build toolchain to Gradle 9.6.0, Android Gradle Plugin 9.4.0 and KSP 2.3.6 for API 37 support.

## 1.17 — 2026-10-29

- Monthly expenses is visible for fresh/missing/reset Home settings; saved visibility and order, including a previously hidden widget, remain. Users can hide it.
- Removed the version 1.1 report checkbox. Reports always export eight columns with income comments, including after picker recreation. Seven-column import remains; version 1.1 cannot read current exports.

- Four persistent app-wide themes with previews and accessible controls; Night analytics uses labelled horizontal breakdown bars.
- Four-segment savings goal with partial fill, safe milestone arithmetic and factual progress independent of forecast exchange rates.
- Persistent full-width Operations tabs and a persistent category icon-label switch.
- Preserve enabled biometric preference when saving settings during sensor unavailability; the personal-phone update symptom remains unproven.
- Version code 4, unchanged package, schema and compatible signer. See [release evidence](docs/testing-1.17.md) for checks and limitations.

## 1.15 — 2026-10-28

- Biometric-first unlock with automatic Android prompt, code fallback, explicit retry and stale-response protection.
- Optional income comments up to 150 Unicode code points, including in history, latest operation and current CSV export. Import accepts both current eight-column and version 1.1 seven-column files; reports offer an explicit old-format export.
- Rename or archive every expense category and choose from 20 built-in icons. Archived categories remain on historical entries; renamed personal categories display the new name in reports and export.
- Customize Home: show or hide blocks, reorder with drag handles or accessible arrows, and restore defaults. The monthly expense doughnut and category cards are optional and hidden initially.
- Personal operations sort by transaction date and then local creation order; editing does not change the original order. Storage upgrades non-destructively to schema 3.
- Updated English and Russian guides, specifications, test evidence and local APK archive. The package ID and debug signing certificate remain compatible with previous local builds.
- Verification fixes: preserve standard category translations on icon-only edits; fit selected chart names and amounts at enlarged font sizes; retain category cards for expenses rounded to zero after currency conversion.

## 1.1 — 2026-10-26

- Optional application protection with a 4–10-character code, supported device biometrics, and recovery using the phone screen lock.
- Automatic lock on launch, after 30 seconds in the background, and when the screen is turned off. Five incorrect codes trigger a 30-second delay that survives process restarts and clock changes.
- Protected app contents are hidden in screenshots and recent-app previews. Resetting the app code preserves financial records.
- Family reports now show a signed balance, one comparison chart grouped by income then expenses, and shared category totals across members. Standard Russian/English category names are combined.
- Imported-member cleanup is available even when the current report period contains no imported records.
- Removed the redundant Home page heading.
- Corrected Android package-visibility declarations for discovering CSV document pickers.
- Updated English and Russian guides with protection and update instructions.

This test update keeps the package ID, signing certificate and database schema of version 1.0. Install it over the existing compatible build to retain local records.

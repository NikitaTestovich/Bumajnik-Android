# Changelog

## 1.15 — 2026-10-28

- Biometric-first unlock with automatic Android prompt, code fallback, explicit retry and stale-response protection.
- Optional income comments up to 150 Unicode code points, including in history, latest operation and current CSV export. Import accepts both current eight-column and version 1.1 seven-column files; reports offer an explicit old-format export.
- Rename or archive every expense category and choose from 20 built-in icons. Archived categories remain on historical entries; renamed personal categories display the new name in reports and export.
- Customize Home: show or hide blocks, reorder with drag handles or accessible arrows, and restore defaults. The monthly expense doughnut and category cards are optional and hidden initially.
- Personal operations sort by transaction date and then local creation order; editing does not change the original order. Storage upgrades non-destructively to schema 3.
- Updated English and Russian guides, specifications, test evidence and local APK archive. The package ID and debug signing certificate remain compatible with previous local builds.

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

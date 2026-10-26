# Changelog

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

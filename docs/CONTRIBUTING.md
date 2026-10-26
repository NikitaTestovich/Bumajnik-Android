# Development and bug reports

Finance Tracker is being tested with everyday use. Small, focused changes are welcome.

## Reporting a bug

Include the app version, Android version, the screen involved, steps to reproduce, and what you expected to happen. If useful, attach a screenshot made with demo data. Do not attach a personal CSV backup, a real family report, or screenshots showing private financial details.

## Making a change

1. Work from the current `main` branch and create a short branch for a fix or feature.
2. Keep the English and Russian interface text in sync.
3. Check affected calculations and CSV compatibility when changing data behavior.
4. Build the debug APK and run the checks described in [the Android build guide](../android/README.md).
5. Review the GitHub Desktop **Changes** list before committing. Keep SDK files, local builds, exports, and credentials out of the commit.

## Checklist for every version

Before publishing any new version:

1. Update the [current product specification](specification.md) and each affected detailed specification in `docs/specs/` to match the delivered behavior.
2. Update **both** the [English](user-guide-en.md) and [Russian](user-guide-ru.md) user guides. Refresh `README.md`, `android/README.md`, screenshots, and `CHANGELOG.md` when their content changes.
3. Add meaningful automated tests for each new feature and behavior change. Run the relevant tests and the complete available suite. Record results and any untested physical-device behavior in `docs/testing-<version>.md`.
4. Verify that an update over the previous compatible APK preserves data when the version, storage, package ID, or signing changes. Review the commit for personal information, secrets, local builds, and backup files.

CSV files under `android/app/src/androidTest/assets/` are synthetic test fixtures; real exported CSV files are ignored by Git.

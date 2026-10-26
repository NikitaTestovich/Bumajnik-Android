# Version 1.1 verification

Verified on 25–26 October 2026 using an Android 17 emulator. All records used for testing and screenshots were fictional.

## Build and updates

- APK metadata: `com.personalfinance.tracker`, version name `1.1`, version code `2`, minimum Android API `26`.
- Gradle unit tests: 89 passed, no failures.
- APK and instrumentation APK compile successfully with the existing toolchain.
- Saved 1.0 APK and 1.1 APK pass signature verification and use the same signing certificate.
- Two-stage update test: seed on 1.0, install 1.1 using `adb install -r`, verify on 1.1. Profile, personal transactions, standard/custom categories, savings goal, imported family records and saved exchange rates remained identical. App protection remained disabled by default.
- The root ZIP contains the same APK bytes as the build output.

## Android runtime checks

The full runner reported 36 tests with zero failures. There were 35 successful checks and one intentional skip: the two-stage update test needs its explicit `seed`/`verify` argument and passed in separate invocations.

- Protection: wrong/correct app code, enable/change/disable from Profile, private encrypted verifier reload, canceled/confirmed credential callbacks, and the real Android system PIN recovery flow. Financial records were retained during recovery.
- CSV: actual system open/save dialogs and cancellation, export content, family import with conversion, duplicate prevention, and personal backup restore.
- UI and storage: onboarding, languages, date picker/manual dates, operations, reports, charts, family cleanup, goals and Room migration.
- A package-visibility regression test reproduces and covers the previously incorrect “no document picker” detection. CSV MIME types are now declared in manifest queries.

## Review and visual checks

An independent reviewer identified cancellation during persistence, screen-off during protection setup, and a standard/custom category identifier collision. Regression tests failed before fixes and passed afterward. The cooldown now uses elapsed time and a boot identifier so changing the wall clock cannot bypass it. Pending authentication is retained across configuration recreation and rejected after a process restart.

Home was checked without the redundant heading. Family was checked with two members, a positive signed balance, income columns followed by expense columns, and a shared category total. The chart remains horizontally scrollable for larger families. Both guides and README screenshots were updated.

After narrowing the family chart columns, the focused Android family comparison test passed again (1 test). The refreshed family screenshot shows all four columns without clipped labels.

## Remaining device check

No physical phone was connected for this verification. Actual fingerprint/face authentication and manufacturer-specific system prompts should be checked on a phone with enrolled biometrics. Emulator system PIN recovery was tested without replacing Android's authentication result.

This is a compatible debug test update. A future release using a different signing certificate will require a separate migration plan; uninstalling the test app would erase local data. CSV backup currently covers personal transactions, not every setting or family import.

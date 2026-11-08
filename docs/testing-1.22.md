# Version 1.22 verification — in progress

Tests use fictional data. Public release remains 1.21; 1.22 signing and upgrade verification are pending.

- JUnit checks added for all-time personal balance, missing rates, edits and deletion; report date validation; calculator arithmetic and invalid input; icon-key compatibility; and theme registry.
- Full JVM run: 165 tests, zero failures or skips. The initial full Android run encountered one outdated assertion (equal monthly and overall balance produced duplicate text), then stopped on a Compose layout exception during a report test. The assertion was corrected; five targeted UI tests, including the report, historical exchange-rate prompt and calculator, passed on rerun. The full Android rerun completed: 145 cases, 123 passed, 22 assumption-based skips, zero failures. The earlier Compose layout exception did not recur, although its precise cause was not established. Two subsequent targeted checks passed for the calculator equals key and persistence of the three new themes.
- New Android UI tests and expanded theme/language coverage run only on a disposable emulator; never run destructive database-reset fixtures on a personal device.
- App data, package ID and owner-managed signing key must be preserved on the production update; compare certificates and verify a staged 1.21 → 1.22 upgrade before release.

- Staged upgrade on the disposable Finance122 emulator: a debug-signed 1.21/code 6 APK was updated in place to debug-signed 1.22/code 7. Synthetic profile, transaction, category icon, goal, family record, rate, theme, selected tab, layout preferences and app code were retained. This does not establish release-key signing or physical-phone behaviour.
- Owner-signed APK verification, real release upgrade, complete skips and visual review on a physical phone remain outstanding.

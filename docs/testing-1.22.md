# Version 1.22 verification — in progress

Tests use fictional data. Public release remains 1.21; 1.22 signing and upgrade verification are pending.

- JUnit checks added for all-time personal balance, missing rates, edits and deletion; report date validation; calculator arithmetic and invalid input; icon-key compatibility; and theme registry.
- Full JVM run: 165 tests, zero failures or skips. The initial full Android run encountered one outdated assertion (equal monthly and overall balance produced duplicate text), then stopped on a Compose layout exception during a report test. The assertion was corrected; five targeted UI tests, including the report, historical exchange-rate prompt and calculator, passed on rerun. A complete Android rerun is in progress; no claim is made that the exception is resolved until the full result is known.
- New Android UI tests and expanded theme/language coverage run only on a disposable emulator; never run destructive database-reset fixtures on a personal device.
- App data, package ID and owner-managed signing key must be preserved on the production update; compare certificates and verify a staged 1.21 → 1.22 upgrade before release.

# Release rules

For every new app version, before publishing:

1. Update `docs/specification.md` and every affected detailed specification in `docs/specs/` so they describe the shipped behavior.
2. Update **both** `docs/user-guide-en.md` and `docs/user-guide-ru.md`. Update `README.md`, `android/README.md`, and `CHANGELOG.md` where the release changes their content.
3. Cover each new feature or behavior change with meaningful automated tests. Run the relevant tests and the complete available test suite; record results and device limitations in `docs/testing-<version>.md`.
4. Check compatible upgrades and retained data when changing storage, signing, package ID, or version metadata. Keep APKs, local data, credentials, and signing keys out of Git.

The contributor checklist is in `docs/CONTRIBUTING.md`.

# Version 1.21 — maintenance release

Status: released 2026-11-03. Version name 1.21, versionCode 6.

This release incorporates the current release documentation and increases the Android version metadata. Application features and storage formats remain those specified for 1.20. There are no new financial calculations, UI behavior changes or database migrations.

- Package remains `com.personalfinance.tracker`.
- Production signing must use the same owner-managed certificate as public 1.20.
- GitHub release tag: `v1.21`; asset: `finance-tracker-vc6.apk`.
- Version 1.20/code 5 should discover code 6; 1.21/code 6 must not offer itself as an update.
- A compatible in-place update must preserve personal records and preferences; never uninstall the existing phone app to test this path.
- Checking and downloading require internet and installation requires Android confirmation.

See [verification](../testing-1.21.md). Publication and signed-build verification are separate gates; a debug build does not prove release-signature compatibility.

Release description: [1.21 notes](../releases/1.21.md). These notes are prepared for publication, not evidence that the release is already public.

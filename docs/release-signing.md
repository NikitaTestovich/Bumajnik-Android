# Release signing and updates (work in progress)

Do not publish a release until the migration and update checks in [testing-1.20.md](testing-1.20.md) pass. Existing installations use a different debug certificate; a new release certificate cannot update them directly. Do not uninstall an existing installation until a full-data transfer and recovery have been independently verified.

For a new owner-managed key, see the [Russian setup guide](release-key-setup-ru.md). Keep the keystore and password outside Git and out of chat.

## Build configuration

The release build reads its keystore and credentials from the following process environment variables:

- `FINANCE_RELEASE_KEYSTORE`: absolute path to the keystore, stored outside the repository.
- `FINANCE_RELEASE_STORE_PASSWORD`: keystore password.
- `FINANCE_RELEASE_KEY_ALIAS`: alias of the release key.
- `FINANCE_RELEASE_KEY_PASSWORD`: key password.

Supply these securely in the build environment, not in source files, commits, chat, or build logs. Keep protected, recoverable copies of the keystore and credentials separately. Loss of the key can prevent users from installing subsequent updates. Do not use the debug key or any disposable testing key for the public release.

From `android`, run `gradlew.bat :app:assembleRelease` with JDK 21 and the variables configured. The release signing preflight rejects missing variables or a missing keystore. Verify the resulting APK's package, versionCode and signing certificate with Android build tools before distributing it. The certificate must match the installed production app for an in-place update.

## GitHub Releases

The in-app checker currently reads the latest non-prerelease of `NikitaTestovich/AppAndroidBudjets`. Its APK asset must be named `finance-tracker-vc<versionCode>.apk`; the numeric versionCode must increase and GitHub must expose the asset's SHA-256 digest. The release description is shown as the list of changes. The downloaded file is checked against the expected digest, package, version and certificate before Android's installation confirmation.

On an emulator, a full backup was transferred from the 1.17-compatible debug signer to a disposable second signer through the system document picker; see the test report for scope and limitations. An owner-signed release APK was also built, verified, installed and identically reinstalled with a synthetic profile retained. The GitHub-to-phone installation flow and migration to that release certificate on the target phone remain unverified. A test build signed with a disposable key is not a production release and must not be published or used as an upgrade.

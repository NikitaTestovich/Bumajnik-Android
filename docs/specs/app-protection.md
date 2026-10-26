# Optional app protection — version 1.1

Protection is disabled until the user enables it in Profile. A device screen lock must already be configured. The user sets and confirms a masked application code of 4–10 characters; enrolled device biometrics may be enabled as an alternative unlock method.

The app requires authentication on a fresh launch, after at least 30 seconds outside the app, and when the phone locks. A brief visit to the Android file picker does not require another unlock. Locked users cannot access financial screens or bypass the gate with Back. When protection is active, app content is hidden from screenshots and recent-app previews.

Five incorrect code attempts cause a 30-second pause. Restarting the app or changing the wall clock does not clear it. A reboot during a pending pause may start a fresh 30-second pause. Changing or disabling protection requires the current code. If the code is forgotten, successful confirmation of the phone's system PIN, password, or pattern disables the forgotten app protection while preserving financial records; cancellation changes nothing.

The code is never stored in plaintext. A salted, slow verifier is held in app-private storage protected with Android Keystore. Biometric matching takes place in Android's system prompt. Neither the code verifier nor protection preferences are included in CSV exports. This feature is an application access lock; it does not encrypt exported CSV files or the finance database.

Automated coverage includes code rules, retry policy, lock lifecycle, settings changes, cancellation, blocked navigation, and emulator system-PIN recovery without data deletion. Physical fingerprint and face authentication still require verification on an enrolled device.

# Version 1.23 security changes

Status: in development; verification and release are incomplete. This document supersedes older version-specific behavior where it differs.

- Newly created full backups require a separate password and use the versioned authenticated encrypted format. The user must retain the password; app PIN and biometric settings are not included. Existing unencrypted FTB1 files remain readable, with a warning.
- CSV remains the exact exchange and personal restore format, supporting the existing seven- and eight-column variants. Input/output is limited to 10 MiB, 100,000 records and 65,536 characters per field. Oversized old CSVs require splitting with a CSV-aware tool; preserve the original file. A single field above the limit cannot be fixed by splitting records.
- Reports additionally offer XLSX for viewing in spreadsheet applications. Untrusted text uses text cells; large amounts that would exceed spreadsheet numeric precision use text. XLSX is not an import or backup format.
- CSV input checks limits while reading and parsing; family preview runs off the UI thread. Cancellation is cooperative: a provider blocked in a synchronous read may not return immediately.
- Gradle wrapper uses 9.8.2 with a pinned distribution SHA-256. Dependency locking and artifact verification metadata are enabled; generated hashes represent the observed artifacts and are not independently verified provenance.
- Pull requests have a debug build and unit-test workflow without release credentials. Branch protection and private vulnerability reporting require separate owner configuration and verification on GitHub.
- Application version is 1.23 / versionCode 8; the existing package ID and release signing key are retained for compatible updates.

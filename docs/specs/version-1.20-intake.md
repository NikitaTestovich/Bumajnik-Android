# Version 1.20 — requirements intake and review notes

Date: 2026-11-01. This document preserves the initial requirements and review notes as a historical snapshot. The user subsequently approved implementation; work is in progress and release-transition verification remains incomplete. For current test status see [testing-1.20.md](../testing-1.20.md).

## Requested scope

1. Distinguishable adjacent expense chart segments; investigate intermittent chart visibility across all four themes.
2. Search personal Operations in both tabs by comment, amount and date; optional expense category filter.
3. Lower the full-width add-operation button to increase usable list height without covering records or system navigation.
4. Expand expense categories into contributing transactions in personal and family reports.
5. Keep profile editing in More; move themes, protection, personal backups and category management into separate destinations with Back. Add current version, update checking and an Update action when a newer release exists.
6. Optional expense comments, analogous to income comments, including CSV export/import and both report drilldowns.
7. Automated unit/integration/UI and end-to-end checks; fictional three-month datasets with many operations; screenshot-based verification report.
8. Split work into small independently reviewable tasks.
9. Prepare signed release distribution and in-app internet updates from GitHub Releases.

## Evidence from current code

- Current versionName 1.17, versionCode 4; no release signing configuration in app/build.gradle.kts.
- ProfileScreen currently contains all settings inline.
- Expense CSV comments are explicitly rejected on both encode and decode; the existing eighth column is comment.
- PersonalBackupManager exports personal transactions only, not a full application backup.
- MonthlyExpensesWidget maps category IDs to a short palette with hash modulo, permitting collisions.
- Theme 9 intentionally shows a segmented horizontal strip instead of a doughnut. Other disappearance causes remain unverified.
- Operations has a nested Scaffold bottom bar and navigationBarsPadding; inset handling needs inspection on actual devices before changing.

## Reviewable prototype

Local file: .superpowers/designs/v1.20-settings-preview.html.
Nine screen mockups: More, Themes, Protection, Backup, Categories, Updates, personal report, family report, Operations search.
Actions are explicitly simulations. Browser preview was blocked for the file URL; visual rendering has not been verified in this session.
Profile remains editable on More. The proposed Updates destination keeps version/check/download state together; user may prefer the check action directly on More.

## Proposed behavior, not yet approved

### User clarifications

- Preserve and display each category's currently selected icon. Colored dots in the first mockup are not replacements for category icons.
- Each More action opens only its own destination, with Back to More. The side-by-side mockup gallery is for review only and must not become a combined settings screen.
- Clarify personal CSV restore coverage before choosing the release-signature migration; restoring personal records must not be described as restoring the entire application.

- Search all personal records in the selected tab, newest first. Case-insensitive comments; accept decimal comma/dot and displayed DD-MM-YYYY dates. Category filter combines with search and includes archived categories with historical records. Clear/empty states required. Monthly totals remain monthly totals; label filtered result counts separately.
- Expense comments use the existing 150 Unicode-character limit. Reuse the eighth CSV column; accept old seven-column and current eight-column files. Older app versions reject expense comments: do not promise backward import into 1.17; update family devices before sharing these files.
- Category drilldowns show date, comment if present, amount, and owner in family reports. Use exactly the same period, category grouping, exchange rates and per-record rounding as the aggregate. Show original and converted amounts where needed; test that details sum to category total. Preserve combined categories across owners.
- Use an adjacency-aware chart palette including the first/last seam, with visible separators and matching legend colors. Handle more categories than palette entries, tiny/zero slices and theme changes. Decide whether theme 9 should retain its strip or use a doughnut too.
- Keep existing security behavior when extracting the settings page. Never silently reset code or biometric preference.
- Updates: public GitHub Releases over HTTPS, stable releases only, monotonic numeric versionCode comparison rather than decimal versionName comparison. Validate downloaded package ID, version, certificate and checksum before handing off to the Android installer. Handle offline, rate limits, no release, cancellation, wrong signatures and interrupted download. No embedded GitHub token; no financial data in requests.

## Release transition decision

Recommended: dedicated protected release signing key with a verified migration from the installed debug-signed build. Preserve personal/family records, categories, goals, rates and UI settings; device-bound biometric keys must not be exported and may require re-enrollment. Existing CSV is insufficient for this migration. Determine whether a tested signing lineage is viable on all actual devices; otherwise provide a full transfer mechanism available in an old-key-signed bridge build before replacing the installation. Do not uninstall any personal installation before a verified recovery path exists.

Alternative: temporarily continue signing with the existing debug certificate for in-place family testing. Simpler data continuity, but unsuitable as the long-term release key strategy.

## Candidate work packages (sequence to finalize after approval)

1. Reproduce chart issues and record baseline across themes 2/6/8/9.
2. Expense-comment domain validation, form, persistence and CSV compatibility.
3. Search/filter logic and UI, preserving remembered operation type.
4. Insets/button layout and small-screen/keyboard checks.
5. Shared report drilldown data with exact aggregate reconciliation.
6. Personal report expansion.
7. Family report expansion and ownership display.
8. More navigation and extraction of each existing settings section.
9. Chart color allocation and rendering fixes.
10. Signing/migration proof on disposable data, followed by agreed migration implementation.
11. Release metadata and update checker/downloader/installer UI.
12. Fixture generator and independent expected totals for three months, at least three fictional owners and 1,000 operations; duplicate IDs, equal dates, comments with CSV quoting, empty comments, currencies, archived/renamed categories and many slices.
13. Integration/UI/E2E runs: CSV import/export round trip, family deduplication, search, drilldowns, scrolling, all themes/languages, large font, update failure/success and retained data. Produce screenshot report from fictional data only; label unavailable device checks.
14. Update specification, detailed specs, both guides, both READMEs, CHANGELOG, testing-1.20.md and release archive. Review changes before publication.

## Open decisions

- Approve or adjust settings mockups, especially Updates as a separate page.
- Choose release-signature transition approach.
- Clarify where the intermittent chart disappearance occurs if code/device reproduction does not reveal it.

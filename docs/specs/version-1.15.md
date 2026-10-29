# Version 1.15 — implementation under verification

Status: approved by the user on 27 October 2026 and implemented in the version 1.15 build on 28 October 2026, with a 150-character comment limit and required drag-and-drop ordering alongside arrow controls. Emulator system-PIN recovery is verified; physical biometric match and recovery remain device checks. The [verification report](../testing-1.15.md) records executed checks and remaining physical-device checks.

Builder check completed on 27 October 2026: no important unanswered implementation questions.

## Purpose and scope

Improve everyday entry, category management and the personal dashboard, while retaining records and the existing optional app lock. Deliver versionName `1.15`, versionCode `3`, the same application ID and signing certificate, and an updated root `AppAndroidBudjets.zip`. A separate APK in the project root is not requested.

## Chosen approaches

- Dashboard controls/category presentation: user chose B, a labelled "Моя главная" / "Customize home" control and individually outlined category cards. Compact icon-only controls and a single divided list were considered and not selected.
- Expense chart: user chose option 2, a broad doughnut ring with percentages on segments and the amount in its centre. A thin rounded ring and a pie were considered and not selected.
- Category removal: user explicitly chose to retain removed categories on historical records. Renaming changes the name shown for existing personal records too.
- CSV: approved two export formats because the existing 1.1 parser requires exactly seven columns. New-format export preserves comments; an explicit legacy report export omits comments for 1.1 readers.

Visual references (local mockups): `.superpowers/designs/v1.15-options.html` (B) and `.superpowers/designs/v1.15-chart-options.html` (2).

## 1. Biometric-first access

When app protection and biometric unlock are enabled, every new locked session automatically requests Android biometric authentication. Do not display the app-code field behind the prompt. On success unlock; on a failed match, cancellation, terminal error, unavailable sensor or missing enrollment, allow the app-code screen. Cancel the outstanding biometric request before falling back; late callbacks cannot unlock a newer session. Avoid repeated automatic prompts caused by recomposition or rotation. Provide an explicit biometric retry from the fallback screen when available.

If biometric unlock is disabled, show the existing code screen immediately. Preserve system-credential recovery, code policy, five-attempt cooldown, background timeout, screen-off locking and protected previews. Canceling authentication does not disable protection or expose finance screens.

## 2. Income comments and CSV

Add an optional plain-text comment to income entry and editing, initially empty. Limit: 150 Unicode code points, enforced consistently in entry and import. Examples are salary, dividends and gifts; these remain text, not income categories.

Display the comment in income operation rows and the latest-transaction block when relevant; long text must wrap or show a readable preview with the full value available when editing. Existing records receive an empty comment during migration. Comments do not change financial calculations.

The new CSV header is `owner,record_id,date,category,income,expense,currency,comment`. Reports and personal backups preserve comments and proper CSV escaping. Both family import and personal restore accept the existing seven-column format (empty comment) and the new eight-column format. Save imported comments, even where the aggregate family chart does not show individual rows.

Report export offers the current format by default and "For version 1.1" as an explicit legacy option. Explain that legacy files omit comments. Personal backups use the current format so comments are not silently lost. Deduplication by record ID remains unchanged; importing an already known ID does not update it. Categories and comments remain separate columns. Dates retain DD-MM-YYYY output and existing legacy input support.

## 3. Editable categories and icons

All standard and custom categories support rename, icon selection and removal with confirmation. Use stable category IDs. Renaming updates the display for every existing personal transaction, chart and subsequent export referencing that ID. Imported family rows remain snapshots of received CSV data, consistent with the absence of synchronization.

Removal archives the category: exclude it from new-entry selectors but keep its ID, name and icon for existing transactions and reports. Existing expenses can retain their archived category during editing or move to an active category. Do not reseed archived standard categories or restore their old names on restart/language change. When no active category remains, guide users to add one before saving an expense.

Unedited standard names remain localized, including unchanged saves and icon-only changes. Proposed behavior for a renamed category: show the user-entered name in both languages, as with custom names. Reject empty names and duplicate active names after trimming and case normalization. Old archived names do not block creating a new category; identity remains separate.

Provide 20 bundled icons: groceries, home, transport, phone, health, education, clothing, dining, entertainment, household goods, travel, gifts, pets, sport, beauty, children, insurance, taxes, tools and other. Store stable icon keys with a safe fallback. Show them in category management, expense selection and category breakdowns. No remote image downloads.

## 4. Home customization

Place the labelled settings control above the blocks; do not reinstate the redundant Home heading. The settings control remains reachable even when all blocks are hidden.

Initial configurable blocks: monthly summary (balance/income/expenses together), quick entry actions, savings goal, latest transaction and the new expense breakdown. Support show/hide, change order and restore defaults. Both drag-and-drop ordering and accessible move up/down buttons are required. Give each row a visible drag handle, visibly lift/highlight the dragged row and show its destination, support scrolling near the list edges, and persist the resulting order. Ordinary list scrolling and visibility switches must not start a drag. Arrow and drag actions use the same ordering logic.

Persist choices across restart and upgrade, using stable block identifiers. Existing blocks keep the current default order and visibility. The new expense breakdown starts hidden. Future optional blocks can be added without resetting user order or visibility. Offer a useful empty state when all blocks are hidden.

## 5. Monthly expense breakdown

Use personal expenses for the current calendar month, converted with the same period/base-currency rates as the main monthly summary. Family imports are excluded. Request missing rates before displaying a converted total; do not mix unconverted currencies.

Render the selected broad doughnut with the total and currency in the centre. Category cards show icon, name, amount, percentage and a share bar, sorted by expense amount descending with a stable tie-break. Use matching segment/card colours but always include text labels.

Tapping a category highlights its segment, dims others and changes the centre to its amount and name. Repeating the tap restores the total. If a selected category disappears after editing, clear selection. Proposed small-segment rule: omit a percentage label on the ring when it would overlap; keep its exact rounded percentage on the card. Never inflate a segment for visibility. Keep every category in the scrollable list, including archived ones that have expenses in the month and positive expenses that round to zero after conversion. Zero totals retain category cards with zero shares and no painted segments. Bound the selected centre name to two lines and fit the amount on one line; the full name remains on its card and accessibility description.

Show a clear no-expenses state instead of an empty chart. Single-category, many-category, small amounts, long names, large text and screen-reader descriptions must remain usable. Refresh the month when the app resumes across a calendar boundary.

## 6. Sorting investigation and intended order

Reported bug: the user's wife sees expenses out of order. Code inspection found ISO date storage and date-descending SQL, with record-ID ordering for ties; this is evidence, not a reproduced diagnosis.

Reproduce date order across days, months, years, manual entry, edit, import and relaunch. The intended order is transaction date descending, then newest creation first within a date, with an ID tie-break. If creation metadata is introduced, preserve it when editing. For existing records whose creation time is unknown, use deterministic ordering rather than invent historical times. Imports without creation metadata receive a stable local insertion order. Apply consistent ordering to income and latest-transaction selection too. Document the verified cause or any reproduction limitation.

## 7. Storage, tests and release

Use a non-destructive Room migration for comments, category state/icons and any ordering metadata. Persist home preferences without overwriting existing profile/lock settings. Verify the upgrade from a populated 1.1 installation, including enabled protection and archived/renamed category behavior after relaunch.

Required automated checks:

- Unit tests: biometric presentation/fallback state and stale callbacks; comment validation and CSV round trips/legacy compatibility; category naming/archive rules; dashboard order/visibility defaults; category shares and date ordering rules.
- Android UI tests: biometric/code screens, comment entry/edit, category management and icon picker, dashboard settings/reordering/hiding, chart selection/empty state, and expense order on screen.
- End-to-end tests: add/edit income with comment, export through the system picker, import/restore, deduplicate, archive/rename a category with existing expenses, restart with saved widgets, and upgrade 1.1 to 1.15 without losing data.
- Run the complete available suite. Real biometric sensor behavior requires an enrolled physical device; do not report a simulated callback as a physical-device test.

Update the current specification, protection specification, English and Russian guides, README/build instructions, changelog and version-specific test report. Refresh affected screenshots using fictional data. Build the APK and root ZIP, verify actual APK version/signature and byte equality of the archived APK. Publish only under the user's separate GitHub instructions.

## Builder discretion

Use the existing Kotlin/Compose/Room architecture. Choose migration structure, icon implementation, chart drawing mechanics and preference storage without introducing a new framework. Resolve minor layout details within selected B/2 designs. Do not add cloud synchronization, arbitrary income categories or data deletion as part of this release.

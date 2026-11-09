# Finance Tracker — current product specification

## Version 1.23 — in development, not released

The 1.23 work introduces encrypted full backups protected by a separate backup password. When creating one, enter and confirm the password and keep it safe: the backup cannot be restored without it. This password is separate from the app access code. Older unencrypted full backups remain supported for restore; they remain readable files. Full restore replaces current data after confirmation and does not include app protection credentials. These changes and recovery scenarios still require release verification.

The development build offers a separate XLSX report for spreadsheet viewing alongside exact CSV exchange and personal restore. Use CSV for family import or personal restore; spreadsheet editing can change its data. CSV and spreadsheet reports are not full backups. XLSX behavior and compatibility still require release verification.


## Version 1.22 — released

See [approved specification](specs/version-1.22-proposal-ru.md) and [verification](testing-1.22.md). The new overall personal balance uses exchange rates entered for the current month and includes dated personal history. The release includes the calculator, report first-date guidance, expanded icon registry and three themes. Version 1.22 is the published baseline for 1.23.

## Version 1.21 — published maintenance release

VersionName 1.21 / versionCode 6; unchanged package, storage and app behavior from 1.20. Release documentation is refreshed. Same-certificate in-place updating is required. See [1.21 specification](specs/version-1.21.md) and [verification](testing-1.21.md).

> **1.20 released (2026-11-02):** Expense comments and CSV compatibility, operations search and category filtering, expandable personal/family reports, separate settings pages, improved charts, full backups and GitHub updates are shipped. The monthly expense doughnut appears in all four themes. Public download and the current-version live check are verified; the 1.20-to-1.21 in-app update was subsequently verified on a phone with fictional data. See [testing-1.20.md](testing-1.20.md). The 1.17 sections below document the inherited baseline.

## Version 1.20 shipped changes

- Personal income and expense comments are optional and limited to 150 Unicode code points; eight-column CSV export includes both. Seven-column legacy and eight-column CSV imports remain supported.
- Operations search covers comments, amounts and displayed dates for the selected transaction type; expense search additionally filters by category. The add-operation control stays below the scrollable history.
- Personal and family expense categories expand to show contributing dated transactions and comments; family details identify the owner and show conversion where applicable. Totals and details use the same report period and conversion rules.
- More retains profile editing and opens themes, protection, backups, categories and updates as separate screens. Existing category icons remain visible. The expense doughnut is available in every active theme and adjacent segments receive distinguishable colors where possible.
- In the 1.20 baseline, a personal CSV backup restores transactions by identifier without duplicates; it excludes profile, settings, goals, rates and family imports. The separate full backup in that baseline is unencrypted, includes those data and excludes app-lock secrets; full restoration replaces current data after confirmation. See the 1.23 development note above for the new format.
- Updates check the configured public GitHub release endpoint, display release notes and require a newer versionCode. Before Android's installer opens, the APK must match the expected SHA-256, package and signing certificate. Installation requires the user's confirmation. Actual GitHub end-to-end verification remains outstanding.

## Released 1.17 baseline

**Historical baseline:** 1.17 (29 October 2026), with the 1.20 changes above taking precedence. The [English](user-guide-en.md) and [Russian](user-guide-ru.md) guides explain current usage. Update this specification with each app version.

## Profile and navigation

- On first launch, require a name and a main currency: USD, EUR, RUB, or BYN. Choose Russian or English from the device language, falling back to English; allow later changes in Profile.
- Show Home, Operations, Reports, and More in bottom navigation. More leads to Profile, family reports, savings goal, categories, backups, and optional protection.
- Store data locally without an account or automatic family synchronization.

## Transactions and personal reporting

- Add, edit, and delete dated income or expenses. Require a positive amount and an expense category for expenses. An income may have an optional comment of at most 150 Unicode code points. Accept manually entered `DD-MM-YYYY` dates and a date picker.
- Rename or archive any standard or custom expense category and choose from 30 bundled icons. Archived categories stay in historical records and reports but cannot be selected for a new expense; renaming updates the name in personal history. Standard categories are not recreated after archival. Icon-only edits and unchanged saves preserve their translations.
- Take the currency for a new transaction from Profile. Keep each saved transaction's original currency and amount after later profile changes.
- Show current-month income, expenses, signed balance, goal progress, and latest transaction on Home. The labelled Customize home / Моя главная control can hide or reorder blocks by drag handle or accessible arrows; settings remain reachable when all blocks are hidden. The monthly expense breakdown is visible for new installations, missing preference entries and reset defaults; saved visibility and order, including an explicitly hidden widget, are retained. Users can hide it. Themes 2, 6 and 8 show a broad doughnut with selectable segments; theme 9 shows a horizontal breakdown. All themes provide selectable category cards. It uses the same current-month currency rates and rounding as the summary. Cards include expenses rounded to zero, with zero shares and no invented segments. In the doughnut, selected long names are bounded in the centre with the amount fitted on one line; full names remain on cards and in accessibility descriptions.
- Operations separates income and expenses and shows their current-month totals. Entries sort by transaction date descending and newest local creation order within a day; editing retains creation order.
- Reports support a date range from the first personal transaction through the selected end date, defaulting to the current month. Show income, expenses, signed balance, expense categories, and a comparison with the equivalent prior-month period. Export the selected personal entries as CSV.
- Request manual conversion rates when mixed currencies need a single displayed total. Keep source records unchanged; reuse saved rates for the matching report period and target currency.

## Family and goals

- Import another member's CSV report, validate it before saving, and skip duplicate record IDs. Save imported data separately from personal transactions. Request a manual conversion rate when currencies differ. Allow clearing one imported member's data without affecting personal records.
- Show family income, expenses, and signed net balance for the selected period. Compare incomes of all members first, then expenses of all members on one scale; allow horizontal scrolling for larger families. Combine matching expense categories across members, including standard Russian and English category names.
- Support one savings goal with a target and an already saved amount. Estimate whole months remaining from personal income minus expenses averaged over up to the last three completed calendar months. Exclude the incomplete current month and family imports.

## Import, export, and protection

- Current CSV reports and personal backups use UTF-8 fields `owner,record_id,date,category,income,expense,currency,comment`, with `DD-MM-YYYY` dates. Both seven-column 1.1 files and eight-column current files import. Report export and backups always use the current eight-column format with comments; there is no version 1.1 checkbox. Version 1.1 cannot read current exports. Pending report requests retain their chosen period across recreation and use the current format. Personal backups include all personal transactions, but not profile settings, goal, rates, or family imports.
- Optional app protection uses a separate code of 4–10 characters and available enrolled biometrics. If enabled, a locked session requests biometrics automatically; after failure, cancellation or unavailability the code screen appears, with an explicit biometric retry where possible. Require a configured system screen lock so a forgotten code can be reset with the phone PIN, password, or pattern without deleting financial records. Lock on fresh launch, after 30 seconds in background, and when the phone locks; pause code attempts for 30 seconds after five failures. See the [detailed protection specification](specs/app-protection.md).

## Compatibility and verification

- Android 8.0 (API 26) or newer. English and Russian UI and guides.
- A compatible APK update keeps the package ID and signing certificate, increases the version code, and preserves local records. Version 1.15 uses a non-destructive schema-2-to-3 migration and preserves 1.1 data.
- Every feature or changed behavior needs automated coverage. Before each release, run the full available suite and document its results and any device-specific checks still pending.

## Version 1.17 behavior

In Profile, open **Themes** and select Financial journal (2, default), Electric graphite (6), Color mosaic (8), or Night analytics (9). The choice applies immediately throughout the app, including protection, and survives restart and compatible updates. Unknown or retired choices use the available default. Night analytics uses labelled horizontal breakdown bars. Theme settings are local and excluded from CSV backups.

The goal screen shows four segments and 0/25/50/75/100% milestones, actual savings, target, remaining amount and the amount to the next milestone. Partial segments follow actual progress. Over-target savings remain visible; remaining amounts stop at zero. Factual same-currency progress remains available when forecast rates are missing. Forecasts still use completed months; no historical increases or dates are invented. After changing the main currency, the app automatically converts and saves the target and saved amount when all required rates match the forecast period and new currency. Missing rates are requested; the original goal remains until they are supplied. If conversion cannot be calculated (for example, no completed forecast period exists or the converted amount is invalid), re-enter both amounts in the new currency and save the goal.

Operations remembers Income or Expenses across navigation and restart. Saving an operation selects its type for the next Operations visit; canceling preserves the previous choice. The tabs share the available width. In the category editor, switch icon labels on or off; labels are on initially, the choice persists, and accessible icon names remain available. Icons do not restrict category use.

Saving security settings during temporary biometric unavailability now preserves an enabled biometric choice. App-code fallback and explicit disabling remain available. The reported personal-phone upgrade symptom has not been proven to share this cause; see [release verification](testing-1.17.md).

# Finance Tracker — current product specification

**Current version:** 1.15 (28 October 2026). This document describes the release behavior; the [English](user-guide-en.md) and [Russian](user-guide-ru.md) guides explain how to use it. Update this specification with each app version.

## Profile and navigation

- On first launch, require a name and a main currency: USD, EUR, RUB, or BYN. Choose Russian or English from the device language, falling back to English; allow later changes in Profile.
- Show Home, Operations, Reports, and More in bottom navigation. More leads to Profile, family reports, savings goal, categories, backups, and optional protection.
- Store data locally without an account or automatic family synchronization.

## Transactions and personal reporting

- Add, edit, and delete dated income or expenses. Require a positive amount and an expense category for expenses. An income may have an optional comment of at most 150 Unicode code points. Accept manually entered `DD-MM-YYYY` dates and a date picker.
- Rename or archive any standard or custom expense category and choose from 20 bundled icons. Archived categories stay in historical records and reports but cannot be selected for a new expense; renaming updates the name in personal history. Standard categories are not recreated after archival. Icon-only edits and unchanged saves preserve their translations.
- Take the currency for a new transaction from Profile. Keep each saved transaction's original currency and amount after later profile changes.
- Show current-month income, expenses, signed balance, goal progress, and latest transaction on Home. The labelled Customize home / Моя главная control can hide or reorder blocks by drag handle or accessible arrows; settings remain reachable when all blocks are hidden. The monthly expense breakdown is initially hidden and, when enabled, shows a broad doughnut and category cards with selectable segments. It uses the same current-month currency rates and rounding as the summary. Cards include expenses rounded to zero, with zero shares and no invented segments. Selected long names are bounded in the centre with the amount fitted on one line; full names remain on cards and in accessibility descriptions.
- Operations separates income and expenses and shows their current-month totals. Entries sort by transaction date descending and newest local creation order within a day; editing retains creation order.
- Reports support a date range from the first personal transaction through the selected end date, defaulting to the current month. Show income, expenses, signed balance, expense categories, and a comparison with the equivalent prior-month period. Export the selected personal entries as CSV.
- Request manual conversion rates when mixed currencies need a single displayed total. Keep source records unchanged; reuse saved rates for the matching report period and target currency.

## Family and goals

- Import another member's CSV report, validate it before saving, and skip duplicate record IDs. Save imported data separately from personal transactions. Request a manual conversion rate when currencies differ. Allow clearing one imported member's data without affecting personal records.
- Show family income, expenses, and signed net balance for the selected period. Compare incomes of all members first, then expenses of all members on one scale; allow horizontal scrolling for larger families. Combine matching expense categories across members, including standard Russian and English category names.
- Support one savings goal with a target and an already saved amount. Estimate whole months remaining from personal income minus expenses averaged over up to the last three completed calendar months. Exclude the incomplete current month and family imports.

## Import, export, and protection

- Current CSV reports and personal backups use UTF-8 fields `owner,record_id,date,category,income,expense,currency,comment`, with `DD-MM-YYYY` dates. Both seven-column 1.1 files and eight-column current files import. Report export has an explicit 1.1-compatible seven-column option that omits comments; backups always use the current format. Personal backups include all personal transactions, but not profile settings, goal, rates, or family imports.
- Optional app protection uses a separate code of 4–10 characters and available enrolled biometrics. If enabled, a locked session requests biometrics automatically; after failure, cancellation or unavailability the code screen appears, with an explicit biometric retry where possible. Require a configured system screen lock so a forgotten code can be reset with the phone PIN, password, or pattern without deleting financial records. Lock on fresh launch, after 30 seconds in background, and when the phone locks; pause code attempts for 30 seconds after five failures. See the [detailed protection specification](specs/app-protection.md).

## Compatibility and verification

- Android 8.0 (API 26) or newer. English and Russian UI and guides.
- A compatible APK update keeps the package ID and signing certificate, increases the version code, and preserves local records. Version 1.15 uses a non-destructive schema-2-to-3 migration and preserves 1.1 data.
- Every feature or changed behavior needs automated coverage. Before each release, run the full available suite and document its results and any device-specific checks still pending.

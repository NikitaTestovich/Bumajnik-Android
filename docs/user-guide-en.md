# Finance Tracker — user guide

[Русская версия](user-guide-ru.md) · [Build and install](../android/README.md)

Finance Tracker stores your data locally on your Android phone. It needs no account and has no automatic synchronization or cloud backup. Family sharing uses files that you exchange yourself. Export a personal backup before clearing app data, uninstalling, or moving to another phone.

## Updating without losing records

Download the newer APK, open it on your phone and confirm **Update**. Keep the existing app installed and do not clear its storage. A compatible update retains your profile, transactions, categories, goal, exchange rates and saved family imports. The new APK must have the same application ID and signing certificate; the developer increases its version for each release. Version 1.15 upgrades the database without removing existing records.

Export a personal CSV backup before an update as an extra precaution. It covers personal transactions, not all settings or family imports. If Android reports a signature conflict, ask for a compatible APK instead of uninstalling: an uninstall deletes local data. The current test builds use the same local debug signing key. Moving to a release signed with a different key requires a planned data transfer; it cannot be installed directly over this test build. GitHub does not automatically update the app on your phone.

## First launch and navigation

1. Enter **Your name**, choose a **Main currency** (USD, EUR, RUB, or BYN), and tap **Continue**. A name is required.
2. The initial language follows the device: Russian or English, with English as the fallback for other languages.
3. Use the four bottom tabs: **Home**, **Operations**, **Reports**, and **More**. **More** opens **Profile**, which also contains the family report, goal, backup actions, and categories. Scroll to see lower controls.

**Home** shows the current calendar month's income, expenses, and balance (income minus expenses), quick entry buttons, saved goal progress, and the latest transaction. Tap **Customize home** to show or hide blocks and change their order. Drag a block by its handle, use the accessible up/down buttons, or reset to defaults. The settings button remains visible when every block is hidden. **Monthly expenses** starts hidden; enable it to see a category doughnut and cards. Tap a segment or card to focus on one category; tap it again to restore the total. **All operations** opens the personal transaction list. Open **Savings goal planner** for the forecast.

Changing only a standard category icon preserves its translated names when you switch languages. The expense doughnut shortens long selected names in its centre to keep the amount visible; the category card retains the full name. Expenses that convert to 0.00 still have cards with 0% share; when all amounts convert to zero, no segments are drawn.

## Add, edit, and delete transactions

1. On **Home**, tap **Add income** or **Add expense**. Alternatively, in **Operations**, select **Expenses** or **Income**, then **Add transaction**.
2. Enter **Date** as `DD-MM-YYYY`, for example `21-10-2026`. Tap the calendar icon to pick a date, or type it manually. Enter a positive **Amount** with at most two decimal places; a decimal point or comma works.
3. The entry currency comes from the profile and cannot be changed in this form. Change **Main currency** in the profile to use another currency for new entries. Existing entries keep their original currencies and amounts, including when you edit an entry after changing the profile currency.
4. For an expense, select an **Expense category** from the full-width list. If a category is missing, add it in **More → Profile → Expense categories**. Income has no category; you may add an optional **Comment** of up to 150 Unicode characters (for example, salary or dividends). Tap **Save**; **Cancel** leaves the form without saving.

In **Operations**, the list contains all saved personal entries of the selected type, newest transaction dates first and newest creation first within a date. Editing an old entry does not move it above a newer entry on the same date. The full-width add button stays below the list instead of covering it. **Total this month** covers only the current month. Each row shows its original amount and currency code; income comments also appear on the row and the Home latest-entry card.

Tap a row or **Edit** to change it, then **Save**. Editing retains its record identifier. Tap the row's **Delete** and confirm **Delete transaction?** to remove it. **Cancel** keeps it. Deletion is permanent in the app; an earlier CSV backup may still contain the record.

## Profile and expense categories

In **More → Profile**, edit **Your name**, select **Русский** or **English** under **App language**, choose **Main currency**, and tap **Save changes**. The interface may reload after changing language.

The initial categories are Groceries, Housing and utilities, Transport, Communications, Healthcare, Education, Clothing, Cafes and restaurants, Entertainment, Household goods, and Other. Under **Expense categories**, add your own, or edit the name and one of 20 built-in icons on any standard or custom category. Names must be unique among active categories. A renamed category displays your chosen name in both languages, including on older personal expenses and reports. **Delete** archives a category after confirmation: existing expenses retain it, while new expenses cannot select it. If no active category remains, create one before saving another expense.

Changing the main currency changes the currency used for totals and new entries. Existing transactions retain their original amounts and currencies. Saved family imports and a saved goal may need additional conversion; see below.

## Currencies and exchange rates

Rates are entered manually; the app does not download market rates. When conversion is needed, use **Enter rates** or complete **Enter exchange rates**. Enter a positive rate for each requested currency, then **Save rates**. A field `1 EUR = ? USD` asks how many USD equal one EUR: if your chosen rate is 1.10, enter `1.10`.

Personal totals store rates for the exact date range and main currency. Screens reuse rates only when both match. Home and Operations use the full current calendar month; a report through today can have a different range and request rates again. The goal uses its own forecast range. If you cancel a rate dialog, the affected converted totals or forecast remain unavailable; reopen it with **Enter rates**. Conversion rounds to two decimal places and does not rewrite source transactions.

## Personal reports and export

1. Open **Reports**. Normally the initial period runs from the start of this month through today; it starts later if your first personal entry is later. Check the displayed dates.
2. Enter **From** and **Through** in `DD-MM-YYYY`, then tap **Apply period**. Both dates are included. The start cannot precede your first personal transaction or follow the end.
3. Supply missing rates. The report shows income, expenses, and net balance, plus category amounts with share bars. A comparison card gives the absolute and percentage change from an equal-length period shifted back one calendar month. The entered report rates are reused for both periods. An empty expense breakdown says **No expenses in this period.**
4. Tap **Export CSV**, choose a name and location in Android's file picker, and save. Wait for **CSV report saved.** The current eight-column format includes income comments. Select the clearly labelled **For version 1.1** option when sending a report to an older app; that seven-column format omits comments. Export uses the applied period and requires a valid period and any requested rates.

The CSV contains personal entries in that period, original amounts and currencies, and category names in the current app language. It is suitable for family import on another phone. For a copy of all personal transactions, use the separate personal backup action.

## Family report and saved imports

There is no shared account or live family sync. Each person exports a personal report CSV and sends the file themselves. A later edit or deletion on their phone does not update your imported copy.

1. Open **More → Family report**, then **Import family CSV**.
2. Choose the received CSV in Android's file picker. The app validates the complete file and displays its members, currencies, and record count. A malformed file or unsupported value is rejected before records are saved.
3. If currencies differ from your main currency, enter the requested rates and tap **Save rates** to finish the import. If all currencies already match, a valid file imports immediately. There is no separate confirmation button. Canceling the rate dialog leaves that import unfinished; select the file again to retry.
4. Read **Added** and **Skipped duplicates**. Existing imported `record_id` values and repeated IDs within the file are skipped. Importing a revised row with the same ID does not update the saved row.
5. Set **From** and **Through** (inclusive, `DD-MM-YYYY`) and tap **Apply period**. The start cannot precede the first available personal or imported record. Check the dates to include the records you want.

**Family totals** include your personal entries and saved family entries for the period. **Family balance** shows income minus expenses: a positive amount has a green plus, a negative amount has a red minus, and zero is neutral. **By family member** compares all income columns first, followed by all expense columns on the same scale. Swipe the chart horizontally to see every member. Members are grouped by `owner`; use distinct names to keep members distinct.

Expense categories are combined across the family. Matching names ignore case and extra spaces; Russian and English names of standard categories are also combined. Custom categories with different names remain separate. **Imported family members** lists saved imports even outside the selected period. **Clear member data** removes only that member's imported entries after confirmation; personal entries and other members' records remain. If the same record ID exists in both personal and family data for the period, the personal entry is counted once in preference to the imported copy.

Imported entries remain saved locally for later family reports, separately from personal entries. They do not enter Operations, personal reports, the savings forecast, or personal backups. The original amount, currency, owner, and import rate are preserved. Imported amounts use their saved import rates. If you later change your main currency, the family report can request rates from the old import base currency to the current currency; foreign personal entries may also require rates for the selected family period. Tap **Enter rates** and save them to see totals.

## One savings goal

1. Open **More → Savings goal planner** (also available on Home).
2. Enter a positive **Target** and a nonnegative **Already saved** amount in the displayed currency, with at most two decimal places. Enter `0` if nothing is saved yet, then tap **Save goal**. Saving again updates the single active goal.
3. Supply any requested rates to view **Progress and forecast**. Update **Already saved** yourself as your savings change; transactions do not automatically add to this field.

The forecast uses personal income minus personal expenses for up to the latest three completed calendar months, ending before the current month. If your first personal entry is more recent, it uses the completed months from that entry's month onward. Months without entries inside that range count as zero. The current incomplete month and family imports are excluded. For example, in October, an account with older records uses July–September; one whose first entry is in September uses September only.

The remaining amount is `max(target − already saved, 0)`. With a positive average monthly surplus, the estimate is the remaining amount divided by that average, rounded up to whole months. Thus a target of 1,000, saved amount of 400, and average surplus of 200 gives 3 months. Rates for the entire forecast range convert foreign entries to the main currency before calculating monthly results.

The screen explains when there are no completed months, or when the average surplus is zero or negative and no finish time can be estimated. If the saved amount reaches the target, it shows **Goal reached**. The estimate assumes the past average continues.

After changing the main currency, the app can convert the saved target and saved amount using the forecast-period rate for the old goal currency. Supply requested rates. If conversion cannot be calculated (for example, no completed forecast period exists), follow the prompt to re-enter both amounts in the new currency and **Save goal**.

## App protection

Open **More → Profile → App protection**. Protection is optional and starts disabled for a new or unprotected installation. An update preserves the existing protection setting. First configure a PIN, password or pattern in your phone settings so you can recover access if you forget the app code.

1. Enter and repeat a code of **4–10 characters**. Letters, digits and symbols are allowed; case and spaces matter.
2. Optionally enable **Biometric unlock** if your phone has an available, enrolled fingerprint or face method. Android performs biometric authentication; the app does not receive biometric data.
3. Tap **Enable protection**. The app asks for authentication on a fresh launch, after 30 seconds in the background, or when the phone is locked. A short visit to the CSV picker does not require another code.
4. When biometrics are enabled and available, Android asks for a fingerprint or face automatically. If it fails, is canceled, or is unavailable, enter the app code; an explicit biometric retry is available when the sensor is ready. If biometrics are disabled, enter the code directly. After five wrong codes, wait 30 seconds. Restarting the app or changing the clock does not remove the delay. If you reboot the phone during a pending delay, the app may require a fresh 30 seconds.

To change the code or biometric setting, enter the current code and save the protection settings. Leave both new-code fields empty to keep the code. To disable protection, enter the current code and tap **Disable protection**.

If you forget the code, tap **Forgot app code?** and confirm the phone PIN, password or pattern in Android's dialog. This removes the application code and biometric setting while keeping all financial records. Set up protection again in your profile. Canceling the system dialog keeps protection enabled.

When protection is enabled, screenshots and recent-app previews hide app content. This feature controls access to the app; it does not encrypt exported CSV files or add database encryption. Code verification data is protected in private app storage and excluded from CSV exports.

## Personal backup and restore

In **More → Profile**, scroll to **Personal CSV backup**.

- **Export personal backup**: choose a writable destination in Android's file picker. Wait for **Personal backup saved.** This exports all personal transactions, their original amounts/currencies, and category names, regardless of report dates.
- **Restore personal backup**: select the CSV. A successful restore shows added and skipped counts. It merges records into the existing personal list, skips IDs already present (including repeats within the file), and never deletes or replaces local records. A changed row with an existing ID is skipped. Categories are matched by name or created as needed; a blank expense category is restored as uncategorized.

The backup excludes profile, language/settings, savings goal, exchange rates, family imports, and categories unused by exported transactions. On a new installation, complete onboarding first, restore the file, then set your preferences, goal, and rates again. Automatic Android app backup is disabled. Keep the exported file somewhere you can access independently of the app.

An invalid backup is rejected as a whole with no records saved. For read/write failures, choose an accessible source or writable destination and retry. Canceling a system picker cancels that action. Restoring does not alter the family import list; importing a family CSV does not restore personal data.

## CSV format and troubleshooting

Current report exports and personal backups use UTF-8, comma-separated fields, and this exact header and order:

```csv
owner,record_id,date,category,income,expense,currency,comment
```

| Field | Meaning |
| --- | --- |
| `owner` | Nonempty owner name. Exports use the current profile name. Family reports group by this text. |
| `record_id` | App-generated UUID retained when editing/exporting; used to skip duplicates. Preserve it. |
| `date` | `DD-MM-YYYY`, for example `21-10-2026`. Imports also accept the ISO format `2026-10-21` from older files. |
| `category` | Expense category name, not an internal ID. Blank for income. A blank expense category can be imported. |
| `income` | Positive income amount with a decimal point, e.g. `1250.00`; blank for an expense. |
| `expense` | Positive expense amount with a decimal point, e.g. `45.70`; blank for income. Exactly one amount field is filled. |
| `currency` | Original `USD`, `EUR`, `RUB`, or `BYN`, uppercase. |
| `comment` | Optional income comment, up to 150 Unicode code points. Empty for expenses. Quoting protects commas and line breaks. |

Family import and personal restore also accept the old seven-column header without `comment`; those rows have an empty comment. The **For version 1.1** report export writes exactly seven columns and omits comments. Personal backups always use eight columns so comments are preserved.

Amounts must be representable to two decimal places. Fields with commas, quotes, or line breaks are quoted, and embedded quotes doubled. Use app exports where possible; spreadsheet tools can change dates, separators, encoding, or identifiers. Check these fields if an import fails. CSV files are readable financial data; share them only with intended recipients.

Android picker wording and installation prompts vary by device. The example screenshots in the project README were captured on an emulator with fictional demo data.

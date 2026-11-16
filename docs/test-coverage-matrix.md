# Test coverage inventory (work in progress)

This inventory is not a claim of complete coverage. References identify existing tests, not verified CI execution for every test type. Unit tests run in the current PR workflow. The PR workflow runs a three-class Android smoke suite on an emulator; it does not run the complete Android suite. Add negative cases, device requirements and remaining feature areas before treating this inventory as complete.

| Behavior | Existing automated checks | Level | Known limitation / follow-up |
| --- | --- | --- | --- |
| Welcome, locale and profile setup | `WelcomeViewModelTest#resolvesRussianAndEnglishDeviceLocalesAndFallsBackToEnglish`, `#trimsNameAndPersistsProfileAndDefaultCategoriesOnce`, `#rejectsBlankNamesAndUnsupportedCurrencyWithoutSaving` | JVM | Map full UI and change-in-profile paths separately. |
| Home layout | `HomeLayoutTest#defaultsAndMissingBlocksPreserveChoices`, `#movePreservesVisibilityAndClampsBoundaries`, `#allHiddenRemainsHiddenAndResetRestoresDefaults`, `#explicitlyHiddenExpensesAndSavedOrderSurviveNormalization` | JVM | Drag interaction and process restart need Android evidence. |
| Amount calculator | `AmountCalculatorTest#calculatesPrecedenceAndDecimalSeparators`, `#rejectsInvalidUnsafeAndNonPositiveResults` | JVM | Confirm both operation screens and large text on Android. |
| App lock policy | `AppLockPolicyTest#fiveFailuresStartThirtySecondDelayAndExpiredDelayResetsCount`, `#persistedDelayIsBoundedWhenWallClockMovesBackward`, `#locksAfterTimeoutOrScreenOffButAllowsShortPickerRoundTrip` | JVM | Real biometrics and system credential prompts require separate device/emulator tests. |
| Update validation | `UpdateValidationTest#updateMustBeNewerAndMatchIdentitySignatureAndDigest` | JVM | Signed download and upgrade are separate physical-device checks. |
| Period totals and conversions | `SummaryServiceTest#aggregatesBaseCurrencyIncomeExpensesAndBalance`, `#convertsMultipleForeignCurrenciesUsingRatesToBase`, `#refusesToShowTotalsWhenForeignCurrencyRateIsMissing` | JVM | UI and device upgrade coverage to be mapped. |
| Search by comment, date and amount; category filter | `OperationSearchTest#matchesCommentDateAndDecimalComma`, `#categoryAndQueryAreCombined`, `#commentSearchPreservesCommaWhileAmountAcceptsDecimalComma` | JVM | Verify real-screen search for both transaction types in the UI inventory. |
| Transaction form validation and editing | `TransactionFormViewModelTest#expenseCannotBeSavedWithoutCategory`, `#incomeCanBeSavedWithoutCategory`, `#editingKeepsTheExistingTransactionId`, `#deleteOnlyOccursAfterConfirmation` | JVM | Add corresponding real-screen and persistence evidence. |
| Expense categories | `CategoryManagementTest#renameAndArchiveRetainIdentityAndRejectInvalidNames`, `#duplicateActiveNameIsRejectedAfterNormalization`, `#iconRegistryHasThirtyStableKeysAndFallback` | JVM | Real-screen persistence and historical operations require their own Android checks. |
| CSV compatibility | `FinanceCsvCodecTest#encodeUsesExactHeaderAndKeepsStableIdsAndCurrencies`, `#roundTripPreservesQuotesCommasAndRussianText`, `#importsLegacyIsoDate`, `#malformedQuotePlacementRejectsTheWholeFile` | JVM | The document-picker and full import/export UI must be verified separately. |
| Spreadsheet export | `SpreadsheetReportCodecTest#spreadsheetStoresFormulaLikeTextLiterallyAndAmountsAsNumbers`, `#largeAmountsRemainExactTextWhileFifteenDigitAmountsRemainNumeric` | JVM | Coverage of actual spreadsheet apps is separate; Microsoft Excel remains unverified. |
| Savings forecast | `GoalForecastServiceTest#averagesThreeCompletedMonths`, `#accountsForAlreadySavedAmountAndReportsGoalReached`, `#zeroOrNegativeAverageSurplusHasNoFinishEstimate` | JVM | UI interaction and upgrade persistence to be mapped. |
| Expanding a category | `CategoryDrilldownTest#tappingCategoryOpensAndClosesDetails` | Android UI | This checks the reusable control with synthetic details; it does not alone test the complete reports screen. |
| Family CSV import | `FamilyImportManagerTest#malformedFileIsRejectedWithoutWritingAnyRows`, `#repeatedRecordIdsAreReportedAsDuplicatesAndWrittenOnce`, `#appliesOneSuppliedRatePerSourceCurrencyAndPersistsItsBase` | JVM | Full UI flow and multi-month reports to be mapped. |
| Encrypted backup envelope | `EncryptedBackupTest#encryptedRoundTripAndWrongPassword`, `#randomSaltAndNonceProduceDifferentFiles`, `#damagedHeaderCiphertextAndTagAreRejected` | JVM | Device restore and legacy-format flows need separate evidence. |
| Existing database migration | `MigrationTest#populatedVersionTwoPreservesValuesAndBackfillsMetadata`, `#migrationBackfillsTheProfileCurrencyForExistingFamilyRows` | Android | Does not establish every future migration or signed APK upgrade. |
| Personal report expense details | `ReportExpenseDetailTest#dateAndAmountShareRowWithCommentBelow`, `#blankCommentIsNotDisplayedAndOriginalCurrencyRemainsVisible` | Android UI | Full report integration, long text and large font still require tests. |
| Comparison chart at narrow width | `ResponsiveChartTest#allFamilyValuesFitWithoutHorizontalScrolling` | Android UI | More locales, large values, large fonts and real-screen rendering require additional verification. |
| Forest palette | `ThemeSurfaceTest#forest theme uses fresh conifer surfaces and ink`, `#every theme has readable normal text and semantic errors on every used surface` | JVM | Visual inspection and other text/icon combinations remain necessary. |

The inventory below maps additional tests, but is not a complete audit of every behavior and failure case. A high line-coverage percentage cannot replace this review.

## Additional Android test inventory for 1.24

These references identify test scenarios; see `testing-1.24.md` for execution results and device limitations. They do not establish complete coverage.

| Behavior | Android test evidence in the source |
| --- | --- |
| Onboarding and navigation | `FinanceTrackerAcceptanceTest#onboardingPersistsProfileAndBottomNavigationOpensEveryDestination` |
| Income creation, editing and confirmed deletion | `FinanceTrackerAcceptanceTest#incomeCanBeCreatedEditedAndDeletedOnlyAfterConfirmation` |
| Expense category and validation | `FinanceTrackerAcceptanceTest#expenseRequiresCategoryAndSavesSelectedCategory`; `CategoryAcceptanceTest#renameIconArchiveAndEditHistoricalExpense` |
| Profile and locales | `FinanceTrackerAcceptanceTest#profileSavesNameCurrencyAndBothLanguagesAcrossRecreation`; `ProfileSectionsTest#selectingCategoriesShowsOnlyThatPageAndBackReturnsToMore` |
| Calculator and goals | `FinanceTrackerAcceptanceTest#calculatorAppliesToIncomeAndCancellationPreservesExpenseAmount`, `#goalShowsEmptyStateThenPersistsProgressWithoutInventingForecast` |
| Reports and family | `FinanceTrackerAcceptanceTest#reportScreenShowsPairedPeriodColumnsAndCategoryShares`, `#familyReportShowsPerMemberComparisonAndCategoryShares`, `#familyThreeMonthCsvUsesRealSystemDocumentPicker` |
| Backup and failure handling | `FinanceTrackerAcceptanceTest#fullBackupUiRejectsCorruptionAndRestoresOnlyAfterConfirmation`, `#cancelBackupFilePickerClearsPasswordBeforeRetry` |
| App lock and biometric prompt | `AppLockAcceptanceTest#wrongCodeCannotOpenNavigationAndCorrectCodePreservesRecords`; `BiometricPromptAcceptanceTest#realNonmatchFallsBackAndExplicitRetrySucceeds` |
| Themes across screens | `ReleaseThemeJourneyTest#populatedRussianReportsFamilyAndIncomeSaveAcrossAllThemes` |

Historical 1.1 to 1.15 and 1.15 to 1.17 upgrades and cross-signer migration are excluded from the 1.24 release scope by the owner; these scenarios were not run as part of this release verification. The 1.23 to 1.24 upgrade remains separately documented in the test report.

### Further evidence and remaining gaps

- Home expense widget: `MonthlyExpensesWidgetTest` covers theme availability, hiding, reset and long labels. `HomeProcessRestartTest` checks layout after a process restart.
- Accessibility: `OperationsTabAccessibilityTest#tabsAreAccessibleAtLargeFont` covers the operations tabs in both languages and seven themes; `GoalProgressScreenTest#actualRussianGoalScreenFitsNarrowLargeFontInEveryTheme` covers the goal screen. These do not constitute a full accessibility audit.
- CSV: `ReportExportRecreationTest#pickerResultAfterSavedStateRestoreKeepsCommentsAndEightColumns` checks export after recreation; `PersonalBackupManagerTest#writeFailureRollsBackTransactionsAndNewCategories` checks rollback.
- Themes: `ThemeRealScreensTest#allDesignsKeepRealFormsReportsFamilyAndLockUsableAtLargeText` checks populated real screens with large text. `ThemeRecreationTest#profileChoiceSurvivesActivityRecreationAndAppliesToLock` checks recreation.
- Search: `OperationSearchTest` covers query logic at JVM level; `FinanceTrackerAcceptanceTest#searchFiltersIncomeAndExpenseCommentsAndAmountsOnOperationsScreen` covers the screen flow on an emulator.
- Family data removal: UI cancellation/confirmation and data isolation are covered by separate tests, not one end-to-end test.
- Android execution counts and separately executed scenarios are recorded in `testing-1.24.md`; the existence of a test alone does not prove its execution result.
Family removal is covered at two complementary levels: `FinanceTrackerAcceptanceTest#familyReportShowsPerMemberComparisonAndCategoryShares` exercises cancel and confirm in the UI, while Room integration test `FamilyImportManagerTest#deletingImportedOwnerRowsLeavesOtherOwnersAndPersonalTransactionsUntouched` checks isolation. These are separate tests, not one full end-to-end proof of all three outcomes.

Search screen follow-up: `FinanceTrackerAcceptanceTest#searchFiltersIncomeAndExpenseCommentsAndAmountsOnOperationsScreen` was added and compiled. Its emulator run on 2026-11-15 did not execute: ADB timed out obtaining emulator properties and Gradle reported unknown API level. It is not a passing test. Category-filter and date UI flows still require verification.

Search UI test follow-up: the emulator responded again after restart, but Gradle produced an empty XML suite (zero tests), so its successful status is not a pass. Direct instrumentation then failed before the test because Android user 0 was locked and credential-encrypted preferences were unavailable. A subsequent clean emulator startup attempt did not resolve that condition. That particular attempt did not verify the test; later successful runs are recorded below.

On 2026-11-15 the search screen test above ran on a clean, unlocked FinanceSearch124 API 37 emulator and reported `OK (1 test)` via Android instrumentation. It covers comment and amount searches, both operation types, and no matches; date and category filter UI remain unverified by that test.

On 2026-11-15 the same search UI test was extended and rerun on FinanceSearch124, reporting `OK (1 test)`: comment, amount and date searches, income and expenses, category combined with a search query, and no results. The first extended run timed out because it waited for an offscreen lazy-list row; after checking the visible row before selecting the category, the rerun passed. This is one emulator configuration, not a full accessibility or device matrix.

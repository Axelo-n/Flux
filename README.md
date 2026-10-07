# Flux

Android personal finance tracker for one blu account. Kotlin, Compose Material 3,
Room and coroutines. All financial data stays on the device; notification access
is optional and only `com.bcadigital.blu` is processed.

## Starting the revamped system

Build/install the debug APK, enter the actual blu opening balance, choose a time
zone (default Asia/Jakarta), and set Monday–Sunday budgets. The first day receives
a full budget. Extra starts at zero. **Confirming “Hapus & mulai” deletes the old
transactions, rules, adjustments and notification log.** Opening the upgraded
app does not delete existing data. Version 2 databases migrate to version 3;
old records remain until the user explicitly confirms a fresh start.

Budget changes take effect on a selected future date, earliest tomorrow. Future
schedules can be updated/cancelled; past policies are immutable. The active policy
for a date is the most recent policy on or before it. The time zone is locked for
the recording period so historical day boundaries remain consistent.

## Money rules

- Account balance = opening balance + income − effective expenses + balance corrections.
- Effective expense = original amount − total refund. A full refund remains in
  history at zero with a required explanation. Normal transaction notes are optional.
- Daily remaining = that day's budget − that day's effective expenses.
- Extra = extra corrections + the sum of budget minus expenses on completed days
  + today's overspending (only the negative part of daily remaining).
- Income affects account balance only. Transfers/top-ups/withdrawals are ordinary expenses.
- Today's unused budget becomes extra after the day ends; it is not deposited twice.
- Extra can be negative. Edits/deletes/refunds recompute the affected history.
- Days without spending accumulate their full budget from the setup date onward.
- Balance and extra corrections are separate, signed and have a required note.
- Today's displayed spending room is capped by recorded account balance. Extra is
  a budget accounting figure; a warning appears when it exceeds actual recorded funds.

Amounts entered in the app are whole rupiah. Calculation uses Long values;
legacy-compatible transaction storage uses Double restricted to integers no greater
than 9 trillion rupiah (within exact integer representation).

## Automatic recording

The listener uses a persistent foreground notification, requests rebind after
disconnect, exposes actual connection health, and checks currently available
notifications after reconnect. Event logging and transaction insertion are atomic.
Persistent event/occurrence identifiers prevent reconnects and notification updates
from recording the same occurrence again. Identical content/key within two minutes
is logged for review instead of silently counted twice; genuinely separate identical
payments in that window need manual confirmation/entry.

Rules match case-insensitive keyword containment. Blacklist rules run first;
enabled custom parsers then select category/optional note, newest rule first.
Rules never override amount or income/expense direction. The rule screen has search,
type filters, enabled toggles, editing, deletion and a non-writing parser preview.

The parser deliberately refuses messages with no explicit Rp/IDR amount, ambiguous
multiple currency amounts, non-whole-rupiah amounts, failures, pending transactions,
promotional signals and refunds. Unrecognised messages appear in the notification
log for manual review. Refund notifications need manual association with the original
expense; they never become automatic income. The latest 100 log entries are displayed.

Android/OEM battery policies and notification wording still require testing on the
actual phone. Notifications removed while the listener is unavailable cannot be
recovered from bank history. No bank API or account scraping is used.

## Backup

JSON backup version 2 contains opening configuration, effective-dated budgets,
transactions (including original/refunded values), rules and adjustments. Restore
requires confirmation, validates the data and runs in a Room transaction. Invalid
backup files leave existing data intact. The old backup format is rejected rather
than guessing historical budget settings. Import size is capped at 5 MB.
Notification logs are excluded from backup; restore starts a new notification
capture boundary to avoid replaying pre-restore notifications.

## Development

Stay on the existing `develop` branch. JDK 17+ and Android SDK 36 are required;
minimum Android API is 24. Java time APIs are desugared for older Android devices.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

The second command requires a running emulator/test device. Tests use isolated
in-memory databases; the migration test uses its own temporary database. UI flow
tests capture screenshots inside the emulator's external app files directory.

Core files:

- `model/BudgetEngine.kt`: deterministic money calculations independent of Android UI.
- `data/TransactionRepository.kt`: validation, atomic setup/restore/notification writes.
- `notification/`: blu parser and Android listener connection lifecycle.
- `viewmodel/DashboardViewModel.kt`: reactive state and periodic day-boundary recalculation.
- `ui/settings/`: system settings, budget scheduling and automatic rules.

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Appearance and notification notes

System → Preferences offers Indonesian/English and White/Black/Teal/Violet. Preferences persist independently of the financial database; changing language or theme never rewrites transaction notes or categories. Each theme changes the full palette, including cards, inputs and the budget hero.

Recognized blu notifications use the merchant/recipient name as the default note when present. An enabled custom rule with a non-empty note overrides it. Confirmation notifications are sent only after the database transaction commits successfully; duplicates, blacklist matches and messages requiring review do not send a success notification. Android notification permission and channel settings control their delivery.

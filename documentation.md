# Money Manager — Android App Spec

Personal finance tracker built in Jetpack Compose. This doc is the single source of truth
for building this app with AI assistance (Claude Code, etc.) — paste it in as context whenever
starting a new module.

## Tech Stack

- **UI:** Jetpack Compose, Material 3
- **Architecture:** MVVM (ViewModel + StateFlow, Room DAOs expose Flow)
- **DB:** Room (current schema version: 6)
- **Preferences:** DataStore (Preferences) — used for App Lock settings, Home section
  visibility, theme color, and profile (name/photo path)
- **Serialization:** `kotlinx.serialization` (JSON) — used only for the Local Backup/Restore
  export format; entities are annotated `@Serializable` directly, no separate DTO layer
- **Currency:** fixed to Rupee (₹) app-wide, no user-facing switcher — see `util/AppCurrency.kt`
  (`formatCurrency()`). There used to be a Rupee/Dollar/Dinar picker; it was deliberately removed
- **DI:** Hilt (including field injection into `BroadcastReceiver`s via `@AndroidEntryPoint`)
- **Navigation:** Navigation Compose
- **Biometrics:** `androidx.biometric` — requires `MainActivity` to be a `FragmentActivity`, not
  a bare `ComponentActivity`
- **Alarms/Notifications:** `AlarmManager` (inexact, `setAndAllowWhileIdle` — no exact-alarm
  permission needed) + `NotificationCompat`
- **Min SDK:** 26 (Android 8.0+)
- **Package:** `com.alwin.moneymanager`

## Status

MVP is built and working: a Home dashboard (with profile avatar/greeting header and
per-section show/hide customization), EMI tracker (with edit, start/end dates, due-date
reminders, loan amount/interest tracking, closed-loan archive, payoff celebration), Expenses
(user-defined categories with day/month/year filtering, daily subtotals, pick-any-date lookup
that opens a full day-detail page, editable after the fact), a Profile screen (name + photo),
a curated theme-color picker (shadcn/ui-style neutral surfaces + one accent color, applied
app-wide), Local Backup/Restore (export/import a full JSON snapshot via Android's file picker),
and App Lock (biometric/PIN). See "Done" section below. Bank Balance and Currency selection
(Rupee/Dollar/Dinar toggle) were both built and then removed — do not re-add either without
being asked; currency is now fixed to Rupee. Everything else is planned but not yet built.

---

## Feature List

### ✅ Done (MVP)

1. **Home Dashboard**
   - Landing tab (`ui/home/HomeScreen.kt`), first in the bottom nav. Current layout: a `HeroCard`
     (one big number — "This month, incl. EMI" — in a `primaryContainer`-tinted card, with
     "Expenses only: ₹X" as a muted sub-line), a `QuickStatsGrid` below it (2-column grid of small
     `QuickStatTile`s, each a label + bold value), then two list-style blocks, **Active loans** and
     **Recent activity**, each its own card with a "View all" link in the header
   - This is the **third** layout this screen has had in one session — worth reading in full if
     touching Home again, since the history explains why certain options are already ruled out.
     v1: four separate `ElevatedCard`s (one per stat group, 2–3 big numbers each) — rejected as
     too tall for what's fundamentally a short list of numbers, with nothing to *act on*. Two
     replacement directions were mocked up as an HTML/CSS artifact before writing any Compose code
     (**A**: hero number + quick-stat grid + loan/activity previews; **B**: one dense ledger list,
     no cards) — the user initially picked **B** plus asked for Recent Activity folded in from A
     (v2, `OverviewCard`/`LedgerRow`), then after using it, asked to switch to **A** instead — the
     current v3. **Mock up options as an artifact before writing Compose code for open-ended
     "make Home better" asks** — that's what got v2 and v3 both built in one pass each, versus
     several blind-iteration rounds for v1. Don't be surprised if this layout changes again; keep
     `HomeRepository`/`HomeViewModel`'s data shape (`HomeSummary`, `getActiveEmis`,
     `getRecentActivity`) decoupled from whichever composables currently render it, since that
     data layer has been stable across all three UI rewrites
   - **Active loans**: up to 4 active (`!isCompleted`) EMIs, soonest-`nextDueDateMillis`-first
     (`HomeRepository.getActiveEmis()`), each row a name + "Next due d MMM" next to the same
     `EmiProgressRing` used on the EMI list/detail screens (not a linear bar — a linear
     `LinearProgressIndicator` was tried here first and was a regression from the app's own
     established "count out of total reads as a ring" convention, see below). Tapping a row opens
     that EMI's detail screen directly (`onEmiClick`); "View all" navigates to the EMI tab. Hidden
     entirely when there are no active EMIs, and gated behind the same `HomeSection.EMI` toggle
     as the EMI quick-stat tiles
     — hiding EMI info hides this preview too, on the assumption that's what "hide EMI" means
   - **Recent activity**: last 5 expenses across every category (`HomeRepository.getRecentActivity`,
     `ExpenseDao.getRecentExpenses` ordered `dateMillis DESC, id DESC`). Each row is a 40dp circular
     avatar — category color at 15% alpha as the fill, the category name's first letter (bold, full
     category color) as the "glyph" — next to a two-line label (category name + credit-card icon
     inline if `isCreditCard`, then a relative day label below) and the amount on the right. This
     replaced an 8dp color dot + single cramped line of name/date/amount, which read as "boring."
     Tapping a row navigates to that expense's day-detail page (`onExpenseDateClick`,
     reuses the existing `expense_day/{dateMillis}` route — no new screen needed) rather than being
     purely decorative; "View all" still navigates to the Expenses tab. This is a genuinely new
     section (not a re-skin of an old one) so it got its own `HomeSection.RECENT_ACTIVITY` toggle
     entry rather than being unconditionally shown
   - The hero number and quick-stat tiles are still grouped by the same four concepts as before
     (This Month, Payment Method, EMI, Monthly Average) and each group is still individually
     hideable via the existing `HomeSection` toggles — `HeroCard` only shows when `THIS_MONTH` is
     on, and `QuickStatsGrid`'s tile list is assembled by conditionally appending per enabled
     section (`buildList { if (showX) add(QuickStat(...)) }` in `HomeScreen.kt`), so it silently
     shrinks to fewer tiles (or disappears if every section is off) rather than showing gaps
   - Nine stats plus the two list previews, all sourced from `HomeRepository`. **This Month**:
     **today's spend** (tile), **expenses only** (hero sub-line), **including EMI paid** (hero
     headline number). **Payment Method** tiles: **credit card**, **savings account** — split of
     this month's expenses by `Expense.isCreditCard`. **EMI** tiles: **EMI still due** (error-color
     value when > 0), **total outstanding (all loans)**. **Monthly Average** tiles: **expenses
     only**, **including EMI**
   - "EMI paid this month/window" uses `List<EmiWithProgress>.paidAmountInRange()` (an extension
     function in `EmiRepository.kt`), which sums each `EmiPayment`'s **due date** —
     `addMonths(emi.startDateMillis, payment.monthNumber - 1)` — falling in the range, multiplied
     by that EMI's *current* `monthlyAmount`. **This is deliberately keyed off the installment's
     due month, not `EmiPayment.paidDateMillis`** (when it was actually marked paid). It used to be
     paid-date based; that was a real bug found from live data — catching up on backlogged months
     (e.g. marking 23 old installments paid in one sitting) dumped the *entire backlog* into
     whichever single month you happened to click "mark paid" in, producing a wildly inflated
     "this month" figure (₹250k+ instead of the ~₹14k actually due that month). Do not revert to
     `paidDateMillis` for this calculation. If the monthly amount is edited later, historical
     totals still reflect the current amount (same simplification `EmiWithProgress.remainingAmount`
     already makes elsewhere). "EMI still to pay this month" is the sibling
     `dueAmountInRange()` extension, over `EmiWithProgress.nextDueDateMillis`
   - "EMI still to pay this month" uses `EmiWithProgress.nextDueDateMillis` (see Data Model) —
     sums `monthlyAmount` for every EMI whose next unpaid installment's due date falls within the
     current calendar month, regardless of whether it's already been paid *early* or not yet paid
   - "Total EMI outstanding (all loans)" is just `sum(EmiWithProgress.remainingAmount)` across
     every EMI — the full remaining balance across all loans, not scoped to any period
   - "Monthly average" is a fixed trailing 6-month window (`AVERAGE_WINDOW_MONTHS` in
     `HomeRepository.kt`) divided by a constant 6, not by number-of-months-with-data — avoids a
     divide-by-zero for brand new installs and matches how most finance apps define "average"
   - Settings gear lives in this screen's top bar (moved from the EMI screen, since Home is the
     new primary entry point)
   - Top bar title is a tappable header instead of the static "Money Manager" text: a small
     `ProfileAvatar` (photo, or a placeholder `AccountCircle` icon if none is set) followed by
     `"<time-of-day greeting>, <name>"` (`util.timeOfDayGreeting()` + `ProfileViewModel.name`,
     falling back to `"Welcome"` when no name has been set yet — see Profile System below).
     Tapping the avatar navigates to the Profile screen; long-pressing it (via
     `combinedClickable`, only when a photo exists) opens `ProfilePhotoViewerDialog`, a full-size
     view of the photo. This replaced the plain app-name title once a Profile system existed —
     don't revert to a static title
   - **Customize Home**: each of the five `HomeSection`s (This Month, Payment Method, EMI,
     Monthly Average, Recent Activity) can be individually hidden via toggles in Settings →
     "Customize Home" (`HomeSectionsRepository`, DataStore keys per `HomeSection` enum entry,
     default visible). This was chosen over a free-form draggable widget grid — a full widget
     engine (reordering, resizing) was considered and rejected as overkill for a handful of
     fixed, well-understood sections; simple show/hide toggles give the "remove what I don't want
     to see" benefit for a fraction of the complexity. `SettingsScreen`'s "Customize Home" list
     iterates `HomeSection.entries` generically, so adding a new entry is enough to get a toggle
     for it there automatically. If more sections are added later, add a matching `HomeSection`
     entry
     rather than inventing a second toggle mechanism

2. **EMI Tracker**
   - Add EMI: name, monthly amount, start date, end date, notes, optional due-date reminder
   - `totalMonths` is derived from `(endDate - startDate)`, **inclusive of both ends, counted by
     calendar month only** — `util.calendarMonthsInclusive()` compares `(year*12 + month)` for
     start and end and deliberately ignores day-of-month. So Sept → next Aug is always 12 months,
     and July 3 2026 → Feb 2 2028 is always 20 months, no matter which day of the month either
     date falls on.
     **Do not swap this for `ChronoUnit.MONTHS.between(...) + 1`** — that was tried and is
     subtly wrong: `ChronoUnit.MONTHS` is day-aware and *floors* whenever the end date's
     day-of-month is earlier than the start date's (e.g. start day 3, end day 2), silently
     undercounting by a full month. That bug shipped once already and showed up as the EMI detail
     grid's last box landing one calendar month before the actual end date (e.g. end date "Feb 2"
     but the grid stopped at "Jan").
   - Edit EMI: same fields as add; editing is blocked from reducing total months below the
     number of months already paid
   - Mark next month paid (sequential, can't skip months) / undo last payment. If the EMI is
     already `isCompleted` (i.e. undoing would *reopen* a closed loan, not just walk back one of
     several remaining months), `EmiDetailScreen` shows an extra "Reopen this loan?" warning
     `AlertDialog` before calling `undoLastPayment()` — a plain undo is a routine correction users
     make often, but un-completing an already-closed, already-celebrated loan is a bigger deal and
     gets its own confirmation on top of that
   - Live remaining balance = `(totalMonths - paidMonths) * monthlyAmount`
   - Auto-marks EMI `isCompleted = true` when last month is paid
   - Month-by-month grid view (visual paid/unpaid per month) — each box shows both the
     installment number (small, top-start corner, muted) **and** that month's actual due date as
     a short "MMM\nyy" label centered in the box (`util.shortMonthYearLabel`, due date computed
     via `addMonths(startDateMillis, monthIndex)`) — not just a bare sequential number 1..N, so
     it's clear which calendar month each box represents while still keeping the installment count
   - The EMI detail screen's info block (monthly amount, remaining, progress ring, duration,
     notes, loan/interest stats) lives in one `EmiOverviewCard` — an `ElevatedCard` with the two
     headline numbers (monthly amount, remaining) as big bold "label above, value below" stats
     next to the ring, then `HorizontalDivider`-separated `DetailRow`s (label left, value right,
     `SpaceBetween`) for everything else. Replaced a looser stack of plain `Text(...)` lines that
     had no consistent label/value alignment — reuse `DetailRow`/`OverviewStat` for any new field
     added to this card rather than dropping in another bare `Text`
   - Paid-months count (e.g. "16/47") is shown as a circular progress ring
     (`ui/emi/EmiProgressRing.kt` — a `CircularProgressIndicator` with the "paid/total" text
     centered inside it) on both the list card and the detail screen, instead of a plain sentence
     + separate linear bar. Reuse this composable for any future "count out of total" display
     rather than inventing another style
   - Delete EMI (with confirmation, via the shared `ui/common/ConfirmDeleteDialog.kt` — see
     App-wide delete confirmations below)
   - EMI list screen's top bar shows a subtitle under the "EMIs" title: "This month: ₹X paid ·
     ₹Y due" — paid/due totals across every EMI for the current calendar month, from
     `EmiViewModel.monthSummary` (`EmiRepository.getCurrentMonthSummary()`). Hidden when there are
     no EMIs yet. Uses the same due-month-based `paidAmountInRange`/`dueAmountInRange` extension
     functions on `List<EmiWithProgress>` (in `EmiRepository.kt`) that `HomeRepository` uses for
     its EMI stats — **this is the one canonical place these two calculations live**; don't
     re-derive them a third time for a future screen, call these extensions instead
   - **Closed loans are split out of the main list.** `EmiViewModel.emiList` filters to
     `!isCompleted` only; a 3-dot overflow menu in the EMI list top bar → "Closed loans" opens
     `EmiClosedListScreen.kt` (route `emi_closed_list`), which shows `EmiViewModel.closedEmiList`
     (`isCompleted` only) using the same shared `EmiCard`. This keeps the primary list focused on
     loans still being paid off instead of mixing in old, finished ones — if a future screen needs
     "all EMIs regardless of status," call `EmiRepository.getAllEmisWithProgress()` directly rather
     than combining the two filtered lists
   - **Loan amount (optional) → total interest.** `Emi.loanAmount` (`Double`, default `0.0`) is
     the original principal borrowed, entered in the Add/Edit EMI dialog as an optional field.
     `0.0` means "not entered," not "an interest-free loan" — `EmiWithProgress.totalInterest`
     (`= totalPayable - loanAmount`, where `totalPayable = monthlyAmount * totalMonths`) is `null`
     whenever `loanAmount <= 0`, and the EMI detail screen only shows the "Loan amount / Total
     payable / Total interest" block when it's non-null. This matters for the four EMIs that
     already existed on-device before this field was added (migration `MIGRATION_4_5` backfills
     `loanAmount = 0`) — they simply show no interest breakdown until the user fills it in via
     Edit EMI, rather than showing a misleading "₹0 interest"
   - **Congratulations dialog on payoff.** `EmiRepository.markNextMonthPaid` returns `true` when
     the payment just marked was the final installment; `EmiDetailViewModel.markNextMonthPaid`
     takes an `onCompleted: () -> Unit` callback that only fires on that transition, and
     `EmiDetailScreen` uses it to show `ui/emi/PayoffCelebrationDialog.kt`. Kept as a one-shot
     callback rather than a persisted flag — there's no need to show it again on screen
     rotation/reopen since it only matters at the exact moment of payoff
   - `PayoffCelebrationDialog` is a custom `Dialog` (not a plain `AlertDialog`) with a one-shot
     Canvas confetti burst (~30 falling/rotating rects, driven by a single `Animatable(0f→1f)`
     over 1.5s, no animation library needed) behind a spring-bounced celebration icon
     (`Animatable` + `spring(dampingRatio = DampingRatioMediumBouncy)`). A plain icon+text
     `AlertDialog` was tried first and felt flat for a "you just finished paying off a loan"
     moment — reuse this composable (or copy its confetti/bounce technique) for any future
     one-off milestone celebration (e.g. a Desire reaching its target) rather than a static dialog

3. **EMI Due Reminders**
   - Per-EMI opt-in toggle + "days before due date" field, set in the same Add/Edit EMI dialog
     (`Emi.notificationEnabled`, `Emi.reminderDaysBefore`)
   - `EmiReminderScheduler` (`reminder/EmiReminderScheduler.kt`) computes the next unpaid
     month's due date as `startDateMillis + paidMonths` months, then schedules an
     `AlarmManager.setAndAllowWhileIdle` alarm at `dueDate - reminderDaysBefore` days
   - Scheduling/cancellation is centralized in `EmiRepository` — every mutation that can change
     the next due date (add, edit, mark paid, undo, delete) calls the scheduler at the end of
     the same suspend function. Don't schedule from ViewModels; keep it in the repository.
   - `EmiReminderReceiver` re-checks the EMI is still unpaid/enabled at fire time (in case it was
     paid/deleted since scheduling) before showing the notification, since a stale alarm may
     still be pending
   - `BootCompletedReceiver` (`RECEIVE_BOOT_COMPLETED`) re-schedules all enabled, incomplete
     EMIs' reminders after a device reboot, since `AlarmManager` alarms don't survive reboot
   - Notification channel `emi_reminders` created once in `MoneyManagerApp.onCreate()`
   - `POST_NOTIFICATIONS` (API 33+) requested at app launch from `MainActivity`

4. **Expenses (user-defined categories)**
   - Categories are rows the user creates themselves (e.g. Food, Travel, Film, ...), not a
     fixed enum — see `ExpenseCategory` in the data model
   - `FilterPanel` (`ExpenseScreen.kt`) is a plain `Column`, **not** wrapped in a `Card`/gray
     background — a boxed "gray wrapper" around the category chips was tried and explicitly
     rejected as unwanted chrome. Keep it as bare chips + (conditionally) the period nav row,
     no container background
   - Category picker is a horizontal row of plain `FilterChip`s (name only — **no leading colored
     dot**; that was tried and explicitly rejected) plus a "+" chip to add a new one
   - Add expense: amount, note, date (defaults to today, editable via `ui/common/DateField.kt`
     so you can log something you forgot to add on the actual day), payment method (Credit Card /
     Savings Account toggle, `Expense.isCreditCard: Boolean`, defaults off = savings), attached to
     the selected category. Credit-card expenses show a small credit-card icon next to the amount
     in the expense list
   - Delete expense
   - Period filter granularity (All / Day / Month / Year) is chosen from a 3-dot overflow menu
     (`DropdownMenu` behind a `MoreVert` icon) **in the top app bar**, not always-visible segmented
     buttons in the body — the granularity is changed rarely, so it doesn't need permanent screen
     real estate or a dedicated row. When granularity is `ALL`, `FilterPanel` shows **nothing**
     below the chips (no "Showing: All time" text — also tried and rejected as unnecessary); the
     prev/next date nav row only appears for Day/Month/Year — see `ui/expense/PeriodFilter.kt`
     for the filtering/labeling/shifting logic
   - Running total for the currently selected category + period, shown in a highlighted
     summary card at the top of the list
   - For Month/Year/All views, the list is grouped under per-day headers (date + that day's
     subtotal) via `groupExpensesByDay()` in `PeriodFilter.kt` — Day view stays a flat list
     since there's only one day to show
   - **Pick-any-date lookup**: calendar icon in the top bar opens a `DatePickerDialog`; picking a
     date navigates to `DayDetailScreen.kt` (route `expense_day/{dateMillis}`, own
     `DayDetailViewModel` reading the date out of `SavedStateHandle` — same pattern as
     `EmiDetailViewModel`) — a full page (not a modal) showing that day's total **across every
     category** (not just the currently-selected one) plus a list of every expense that day.
     A full page was chosen over the modal it replaced (`DaySummaryDialog`, now deleted) since a
     day can contain many expenses and the modal was cramped and un-navigable (no back-stack entry,
     couldn't be deep-linked). Each row shows category as a small colored dot + name
     (`ui/expense/CategoryColor.kt#categoryColor(categoryId)` — a deterministic pick from the
     dataviz skill's validated 8-hue palette, `categoryId % 8`, light/dark variants swapped via
     `isSystemInDarkTheme()`). Note this dot treatment is `DayDetailScreen`-only — it was also
     tried on the category filter chips and rejected there (see above), so don't reintroduce it on
     the chips. Dividers between entries instead of tight spacing keep the list from feeling dense.
     `DayDetailViewModel` queries `ExpenseRepository.getExpensesForPeriod` directly for that one
     day rather than reusing the category-scoped `filteredExpenses` flow, since it needs to ignore
     the category filter

5. **App Lock (Biometric / PIN)**
   - Settings screen (`ui/settings/SettingsScreen.kt`, gear icon in the Home screen's top bar)
     has two toggles: "App Lock" and "Lock when backgrounded" (the latter disabled unless App
     Lock is on) — backed by `AppLockRepository` (DataStore keys `app_lock_enabled`,
     `lock_on_background`)
   - `AppLockGate` (`ui/applock/AppLockGate.kt`) wraps the entire nav host in `MainActivity`.
     Unlock state is a plain `remember` (not `rememberSaveable`/persisted) so a killed-and
     -restarted process always re-locks
   - Uses `ProcessLifecycleOwner` to detect `ON_STOP` (app backgrounded) and re-lock if
     "lock when backgrounded" is on
   - `LockScreen` triggers `BiometricPrompt` with
     `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` authenticators (biometric with automatic PIN/pattern
     fallback, no separate negative-button flow needed)
   - If the device has no screen lock configured at all, shows an explanatory error instead of
     silently failing

6. **Theme Color**
   - Settings → "Theme color" shows a row of `ColorSwatch` circles, one per `AppThemeColor` enum
     entry (`ui/theme/AppThemeColor.kt` — 8 fixed presets: Indigo, Sky, Teal, Green, Violet, Red,
     Magenta, Orange), each a seed `Color`. Tapping a swatch calls `ThemeViewModel.setThemeColor`,
     persisted via `ThemeRepository` (DataStore) and applied instantly (`MainActivity` collects
     `ThemeViewModel.themeColor` and passes its `.seed` into `MoneyManagerTheme`)
   - **Preset hex values are the Tailwind-500 shade for each color**, not the original
     dataviz-categorical-palette hues the first version shipped with — those looked flat/dull once
     stretched across a full app theme (bottom bar, chips, buttons) rather than small chart dots.
     Swapping the *hex value* under an unchanged *enum constant name* (`PURPLE`, `BLUE`, etc.) was
     deliberate: the user's selection is persisted as `AppThemeColor.name` in DataStore, so
     renaming the constants would have silently reset anyone who'd already picked a color. Only
     `label` and `seed` changed, so don't rename the enum constants when retuning these hexes
     again — same reasoning applies to `HomeSection`, `AppThemeColor`, or any other enum whose
     `.name` is persisted directly
   - **This is a curated preset picker, not a free hex-code input.** A hex field + algorithmic
     full-Material-palette generation (Material Color Utilities-style) was proposed and explicitly
     rejected — "just give option to choose primary color that we given" was the direction: pick
     from colors already vetted to look good in this app, not type an arbitrary code that might
     produce poor contrast. Don't add a hex input or color wheel without being asked again
   - The full `ColorScheme` (light and dark) is generated from one seed color via a small manual
     HSV-based tonal generator (`ui/theme/Theme.kt#tonalColorScheme` — `android.graphics.Color
     .colorToHSV`/`HSVToColor`, varying saturation/value at the seed's hue), not a full Material
     Color Utilities dependency — a lighter-weight approach since only 8 known seeds need to look
     good, not an unbounded user-chosen color. Dynamic (wallpaper-based) color was intentionally
     not wired up, to keep the picker the single source of truth for app color
   - **Surfaces are a true neutral zinc gray scale (white in light mode, near-black in dark),
     not derived from the seed hue — only `primary`/`onPrimary`/`primaryContainer`/
     `onPrimaryContainer` carry the seed color** (shadcn/ui-style "white/near-black background,
     one accent color," an explicit user request). `secondary`/`tertiary` and their containers are
     also neutral zinc grays for the same reason. This replaced an earlier version where
     `secondaryContainer`/`tertiaryContainer` were also seed-tinted (at different
     saturation/value from `primaryContainer`) — that made every card/badge/chip a *different*
     shade of the theme color instead of one consistent accent, which read as "unimpressive"/busy
     rather than clean. Components that specifically want the accent color (bottom nav selected
     tab, selected category `FilterChip`) now set `primary`/`onPrimary` directly via their
     `colors = …Defaults.colors(...)` parameter instead of relying on `secondaryContainer` —
     do the same for any new "selected state" component rather than reintroducing a colorful
     `secondaryContainer`/`tertiaryContainer`. The `Zinc` object at the top of `Theme.kt` is the
     one place these gray values live; retune the app's neutral palette there, not per-screen

7. **Profile System**
   - `ui/profile/ProfileScreen.kt` (route `profile`): shows `ProfileAvatar` (photo or placeholder
     icon), a "Change photo" button (`ActivityResultContracts.GetContent()` image picker, the
     picked `Uri` copied into app-internal storage at `filesDir/profile_photo.jpg` by
     `ProfileRepository.setPhoto` so it survives across launches/reboots without needing persisted
     Uri permissions), and a name field with its own Save button (enabled only once the field
     differs from the saved value). Backed by `ProfileRepository` (DataStore keys for name and
     photo file path) and `ProfileViewModel`
   - **No mandatory first-run setup wizard.** A first-launch "enter your details, with a skip
     option" flow was considered and rejected in favor of the simpler always-optional path: the
     Home header just shows "Welcome" until a name is saved, and the Profile screen is reachable
     (and editable) at any time by tapping the avatar. This avoids interrupting the very first
     app-open with a form for a single-user personal-finance app
   - Long-pressing the Home header avatar (only when a photo is set) opens
     `ProfilePhotoViewerDialog.kt` — a full-size, non-cropped view of the photo in a `Dialog`
     (`ContentScale.Fit`, not `Crop` like the small avatar), for reviewing the photo without
     navigating away from Home

8. **Local Backup / Restore**
   - Settings → "Backup & Restore" has two rows: "Export data" and "Import data"
     (`ui/settings/SettingsScreen.kt`, backed by `ui/settings/BackupViewModel.kt` and
     `data/backup/BackupRepository.kt`)
   - Export: `ActivityResultContracts.CreateDocument("application/json")` → user picks the save
     location via Android's own file picker (Downloads, a Drive-synced folder, USB storage,
     wherever — no automatic cloud upload) → writes one JSON file
     (`data/backup/MoneyManagerExport.kt`, `kotlinx.serialization`) containing every `Emi`,
     `EmiPayment`, `ExpenseCategory`, and `Expense` row, with original row `id`s preserved so
     `EmiPayment.emiId`/`Expense.categoryId` foreign keys still resolve after a restore. Desires
     aren't in the export yet since that feature doesn't exist; add it to `MoneyManagerExport`
     when Desires is built
   - Import: `ActivityResultContracts.OpenDocument()` → picks a file → shows a "Replace all
     data?" confirmation (`AlertDialog`, red "Replace" button, matching `ConfirmDeleteDialog`'s
     styling) before doing anything, since this is destructive
   - **Import always replaces, never merges** — this was an explicit choice over the phone: merge
     logic (matching existing rows against imported ones to avoid duplicates) is real complexity
     for a benefit that doesn't apply to either real use case (new phone or reinstall both start
     from an empty database anyway). If a future request needs merge semantics, treat it as a
     distinct new feature rather than retrofitting this one — don't quietly change replace to merge
   - `BackupRepository.importFrom` wraps the whole delete+insert sequence in one
     `MoneyManagerDatabase.withTransaction { ... }` (from `room-ktx`) so a bad file or mid-import
     crash rolls back instead of leaving a half-restored DB. Delete order relies on `ON DELETE
     CASCADE` (deleting all `Emi` rows cascades `EmiPayment`; deleting all `ExpenseCategory` rows
     cascades `Expense`) — insert order is the reverse (categories before expenses, EMIs before
     payments) since those FKs must resolve on the way in
   - EMI due-date reminders are cancelled for every EMI that existed *before* the import and
     rescheduled for every enabled, incomplete EMI *after* it (same pattern as
     `BootCompletedReceiver`) — `AlarmManager` alarms aren't tied to DB rows, so a plain DB wipe
     would otherwise leave stale alarms referencing EMIs that may no longer exist
   - The four entities (`Emi`, `EmiPayment`, `Expense`, `ExpenseCategory`) are directly annotated
     `@Serializable` rather than mapped to separate DTO classes — their shape already matches what
     should be exported, so a parallel set of "export model" classes would be pure duplication
   - **Build this before WhatsApp export** — the WhatsApp feature reuses this exact
     `MoneyManagerExport` format, just swaps the destination `Intent`

### 🔲 To Build

9. **Desires (Bucket List)**
   - Add desire: name, target amount
   - Track saved amount toward each desire (add contribution over time, like a mini savings goal)
   - Progress bar per desire (saved / target)
   - Mark desire as achieved when saved >= target

10. **Expense Charts**
   - Use **Vico** (Compose-native charting library) — do not hand-roll Canvas charts
   - Monthly bar chart: spend per category, using the now-dynamic `ExpenseCategory` table
   - Aggregation via Room query: `SELECT SUM(amount) ... GROUP BY categoryId, strftime('%Y-%m', date)`
   - No new tables needed — pure aggregation over existing `Expense`/`ExpenseCategory` tables

11. **WhatsApp Export / Import ("Share with family")**
   - Same JSON snapshot format as Local Backup
   - Export: `Intent.ACTION_SEND` (generic share sheet, or target WhatsApp package directly) with the JSON file attached via `FileProvider`
   - Import: recipient opens the file from WhatsApp ("Open with" → this app) or picks it via file picker inside the app
   - **Imported data becomes a new read-only `Profile`** — see Profile data model below. The importing user's own data is untouched; they switch between "My Data" and imported profiles (e.g. "Son") via a profile selector
   - Re-importing the same source name **updates/overwrites** that profile's data (not a duplicate) — match on a stable profile identifier included in the export (e.g. device owner's chosen display name)
   - Imported profile screens hide all add/edit/delete actions — view-only

12. **Android Home Screen Widget** (launcher widget — not to be confused with the in-app
    "Home Dashboard" tab above, which already exists)
    - **Glance API** (`GlanceAppWidget`)
    - Shows: next EMI due (name, amount, due date)
    - Refresh via `WorkManager` periodic worker (simplest) or a `ContentObserver`/`Flow` bridge for live updates
    - Depends on EMI module data only

---

## Data Model

### Existing (MVP, DB schema version 6)

```
Emi
├── id: Long (PK, autoGenerate)
├── name: String
├── monthlyAmount: Double
├── totalMonths: Int              // derived from (endDateMillis - startDateMillis) at add/edit time
├── startDateMillis: Long
├── endDateMillis: Long
├── notes: String
├── isCompleted: Boolean
├── notificationEnabled: Boolean  // default false
├── reminderDaysBefore: Int       // default 3
└── loanAmount: Double            // default 0.0 = "not entered", not a real zero-interest loan

EmiPayment
├── id: Long (PK, autoGenerate)
├── emiId: Long (FK → Emi.id, CASCADE delete)
├── monthNumber: Int          // 1-indexed; presence of row = that month is paid
└── paidDateMillis: Long

ExpenseCategory                  // user-managed, no longer a fixed enum
├── id: Long (PK, autoGenerate)
└── name: String                 // unique

Expense
├── id: Long (PK, autoGenerate)
├── categoryId: Long (FK → ExpenseCategory.id, CASCADE delete, indexed)
├── amount: Double
├── note: String
├── dateMillis: Long              // indexed — nearly every query filters/sorts by this
└── isCreditCard: Boolean         // default false; false = paid from savings account
```

`EmiWithProgress` (in `EmiRepository.kt`, not a table) also exposes a computed
`nextDueDateMillis: Long?` = `startDateMillis + paidMonths` months, or `null` if fully paid —
the single source of truth for "when is this EMI's next payment due", reused by both
`EmiReminderScheduler` and `HomeRepository`'s "EMI still to pay this month" stat.

App Lock is DataStore-only, not a table: Preferences keys `app_lock_enabled` and
`lock_on_background`, read/written by `AppLockRepository`.

The Home Dashboard has no table of its own either — `HomeRepository` is a read-only aggregation
layer that composes `EmiRepository` + `ExpenseRepository` (see Architecture Notes below).

Bank Balance (`BankBalance` entity, single-row table) was removed in migration `MIGRATION_1_2`
(`data/local/Migrations.kt`) — do not reintroduce it without being asked.

### To Add

```
Desire
├── id: Long (PK, autoGenerate)
├── name: String
├── targetAmount: Double
├── isAchieved: Boolean
└── createdAtMillis: Long

DesireContribution               // like EmiPayment but for savings-toward-a-goal
├── id: Long (PK, autoGenerate)
├── desireId: Long (FK → Desire.id, CASCADE delete)
├── amount: Double
└── contributedAtMillis: Long
// savedAmount for a Desire = SUM(amount) from its contributions

Profile                          // needed for WhatsApp import / multi-person view
├── id: Long (PK, autoGenerate)
├── name: String                 // e.g. "Me", "Son"
├── isLocal: Boolean             // true = this device's own editable data
└── lastImportedAtMillis: Long?  // null for isLocal = true

// NOTE: once Profile exists, every entity above needs a `profileId: Long` column
// added and all DAO queries scoped `WHERE profileId = :profileId`.
// The MVP entities currently have no profileId — this is a schema migration to do
// when starting the WhatsApp export/import feature (step 11), not before.
//
// Naming collision to be aware of: this `Profile` table (multi-person, for
// WhatsApp import) is unrelated to the already-built "Profile System" (feature 7
// above) — the single DataStore-backed name+photo for this device's one user.
// When this table is added, the existing Profile screen becomes the editor for
// the `isLocal = true` row; it does not need to be rebuilt.
```

---

## Key Architecture Notes (for AI context)

- **Balance/progress calculations live in the Repository layer**, not the ViewModel or UI —
  see `EmiRepository.EmiWithProgress` as the pattern to follow for `Desire` too
  (e.g. a `DesireWithProgress` wrapper with `savedAmount`, `remainingAmount`, `progressPercent`).
- **Reactivity pattern:** Room DAOs return `Flow`. ViewModels combine/flatMapLatest these into
  `StateFlow` exposed to Compose via `collectAsState()`. When adding a module that has a
  "list of X, each with live computed sub-data" (like Desires + Contributions, same shape as
  EMI + Payments), copy the `flatMapLatest` + `combine` pattern from `EmiRepository.getAllEmisWithProgress`.
- **All new features should follow the existing package structure:**
  `data/local/entity/`, `data/local/dao/`, `data/repository/`, `ui/<feature>/`. Platform-facing
  side-effect code (alarms, notifications) gets its own top-level package, e.g. `reminder/`.
- **Room migrations are hand-written, not destructive.** This app has a real user's live data
  on-device; every schema change needs a `Migration` in `data/local/Migrations.kt` that
  preserves existing rows (see `MIGRATION_1_2` for the pattern: `ALTER TABLE ... ADD COLUMN`
  with a backfill `UPDATE` for additive changes, full `CREATE new / copy / DROP old / RENAME`
  for anything that changes column types or adds foreign keys; `MIGRATION_2_3`/`MIGRATION_3_4`/
  `MIGRATION_4_5`/`MIGRATION_5_6` for simple single/multi-column/index additive cases). Never reach for
  `fallbackToDestructiveMigration()`. **Always double-check the new `Migration` object is actually
  passed to `.addMigrations(...)` in `DatabaseModule.kt`** — a migration that exists but isn't
  registered there shipped once already and crashed on launch for anyone upgrading from an older
  schema version with `IllegalStateException: A migration from N to N+1 was required but not
  found`. Also remember to bump `@Database(version = ...)` in `MoneyManagerDatabase.kt` itself.
- **Currency formatting is fixed to Rupee, not per-record or user-selectable.** Use
  `formatCurrency()` from `util/AppCurrency.kt` everywhere amounts are displayed — don't use
  `NumberFormat.getCurrencyInstance()` (follows device locale) and don't reintroduce a
  currency-selection ViewModel/repository/DataStore key; that was tried and explicitly removed.
- **Side-effecting platform calls (alarms) live in the repository, next to the DB write they
  accompany**, not in ViewModels — see `EmiRepository.markNextMonthPaid`/`undoLastPayment`
  calling `EmiReminderScheduler` right after the DB mutation. This keeps "what triggers a
  reminder recalculation" in one place instead of scattered across ViewModels.
- **`BroadcastReceiver`s use Hilt field injection** (`@AndroidEntryPoint` + `@Inject lateinit var`),
  not constructor injection — Android instantiates receivers itself. Always pair `goAsync()` with
  a `try/finally { pendingResult.finish() }` around the coroutine work so the receiver doesn't
  get killed mid-work and doesn't leak the wakelock.
- **`MainActivity` must stay a `FragmentActivity`** (not `ComponentActivity`) — `BiometricPrompt`
  requires it. `setContent {}` and the rest of the Compose setup work identically on
  `FragmentActivity`, so this is a safe, low-risk base class.
- **Cross-domain read-only aggregation (dashboards/summaries) gets its own repository**, not a
  method bolted onto an existing domain repository — see `HomeRepository`, which composes
  `EmiRepository` (for `EmiWithProgress`, including `nextDueDateMillis`) and `ExpenseRepository`
  (for simple period-total aggregates) rather than talking to DAOs directly. Reuse the domain
  repository's already-correct business logic (e.g. due-date calculation) instead of
  re-deriving it with a raw SQL join.
- **Nested `Scaffold`s and status-bar padding: only one `Scaffold` in the hierarchy should own
  the top inset.** The outer `Scaffold` in `MoneyManagerNavHost` has no `topBar`, so it must set
  `contentWindowInsets = WindowInsets(0, 0, 0, 0)` — otherwise its default `contentWindowInsets`
  (safeDrawing) leaks the status-bar height into `innerPadding`, which get applied to the
  `NavHost`, and then *each* inner screen's own `Scaffold`+`TopAppBar` pads for the status bar
  *again*, doubling it. This was a real bug (very large top padding on every screen) fixed once —
  don't add a `topBar` to the outer Scaffold or remove that `contentWindowInsets` override without
  re-checking this.
- **The Home/dashboard screen's layout has changed twice already — see "Home Dashboard" in the
  Feature List above for the full history before touching it a third time.** Current shape (v3):
  one `HeroCard` (the single headline number) + a `QuickStatsGrid` of small tiles, not a tile grid
  of big cards (v1) and not a dense ledger list (v2, `LedgerRow`s in one `OverviewCard` — that
  composable no longer exists). If asked to add a stat, add a `QuickStat` to the `buildList {}` in
  `HomeScreen.kt` rather than reintroducing per-stat cards or a chart — but also don't be
  surprised if the layout changes again; that's why `HomeRepository`'s data shape is kept
  independent of any of these three UIs.
- **Categorical color for identity (not magnitude) uses the dataviz skill's validated 8-hue
  palette** — see `ui/expense/CategoryColor.kt#categoryColor()`. It's assigned by
  `categoryId % 8`, not by creation order or a user-visible legend, since the category count is
  unbounded and user-defined. If another feature needs to color-code an unbounded user-defined
  set (e.g. Desires later), copy this pattern — invoke the `dataviz` skill again if the palette
  needs to grow past 8 slots or a different design system is involved, don't hand-pick colors.
- **A "count out of total" (paid months, progress) reads better as a ring than a sentence.**
  `ui/emi/EmiProgressRing.kt` wraps `CircularProgressIndicator` with the "x/y" label centered
  inside it — this replaced a plain "16/47 months paid" `Text` + separate `LinearProgressIndicator`
  bar on both the EMI list card and detail screen. Reuse this composable rather than reintroducing
  the sentence+bar pattern for any future paid/total or saved/target style stat (e.g. Desires).
- **`ui/common/DateField.kt` is the shared "tap to open a date picker" text field** — a read-only
  `OutlinedTextField` showing a formatted date, with an invisible clickable overlay (`Box` +
  `matchParentSize`) that opens a `DatePickerDialog`. Used by `EmiFormDialog` (start/end date) and
  `AddExpenseDialog` (expense date, defaults to today but editable so a forgotten entry can be
  backdated). Use this for any future "pick a date" field rather than re-copying the
  overlay-Box-on-a-readOnly-field pattern a third time.
- **Local Backup/Restore and WhatsApp Export/Import share one serializer** — build the JSON
  model once (a top-level `MoneyManagerExport` data class wrapping all entities) and reuse it
  for both features.
- **Profile scoping is the biggest structural change** in the remaining roadmap — it touches
  every table. Plan to do it as a dedicated Room migration + a global "current profile" concept
  (likely a `DataStore`-backed `currentProfileId` read by every repository), not something to
  retrofit casually.
- **A detail screen reached via a nav argument reads that argument in its ViewModel's
  `SavedStateHandle`, not a shared "selected item" field on the parent screen's ViewModel.**
  `DayDetailViewModel` (date, via `expense_day/{dateMillis}`) follows the same pattern as
  `EmiDetailViewModel` (EMI id). This is what let the day-detail modal become a real navigable
  page with its own back-stack entry (see Expenses → Pick-any-date lookup) — copy this pattern
  for any future "tap something in a list, see a full page about just that thing" screen rather
  than passing the selected item through a shared ViewModel field.
- **Any small piece of persisted user preference (a flag, an enum choice, a short string) is a
  DataStore-backed `XxxRepository` + `XxxViewModel` pair, not a Room table.** Home section
  visibility (`HomeSectionsRepository`), theme color (`ThemeRepository`), and profile name/photo
  path (`ProfileRepository`) all follow this shape: a `Singleton` repository wrapping
  `DataStore<Preferences>` exposing `Flow`s, and a thin `HiltViewModel` turning those into
  `StateFlow`s for Compose. Reach for Room only when the data is a genuine list/relation (like
  `Emi`/`Expense`), not a single app-wide setting.
- **An unbounded, user-created set (categories) gets a computed color; a small, fixed, curated
  set (theme color) gets hand-picked preset values — don't conflate the two patterns.**
  `CategoryColor.kt` deterministically derives a color from an unbounded `categoryId` via the
  dataviz palette (see above). `AppThemeColor` is the opposite shape: a fixed enum of 8
  hand-picked seed colors the user chooses from directly in Settings, explicitly *not* a
  hex-input/algorithmic-generation feature (see Theme Color above) — don't blur these into one
  "generate colors" utility.
- **Every delete action requires confirmation — no direct/instant deletes anywhere in the app.**
  `ui/common/ConfirmDeleteDialog.kt` is the one shared "Delete X? / This will permanently delete…"
  `AlertDialog` (title, message, red "Delete" confirm button, "Cancel" dismiss); EMI delete,
  expense delete (both `ExpenseScreen` and `DayDetailScreen`) all route through it. When adding
  a new delete action anywhere (Desires, categories, etc.), reuse this composable instead of
  writing another inline `AlertDialog` — keeps the confirmation wording and button styling
  consistent app-wide.
- **A list screen with a "done/closed/archived" concept splits into two screens, not one filtered
  toggle.** EMI does this: `EmiViewModel.emiList` is active-only (`!isCompleted`), and a separate
  `EmiClosedListScreen` (reached via a 3-dot overflow menu → "Closed loans") shows
  `EmiViewModel.closedEmiList` (`isCompleted` only), reusing the same `EmiCard`. This keeps the
  primary list scoped to "things I still need to act on" without a visible filter control — copy
  this shape (separate screen + overflow menu entry point) for any future entity with a similar
  active/closed split (e.g. achieved Desires) rather than adding a toggle to the main list.
- **Never decode an image file at full resolution just to display it small.** A picked profile
  photo can be 4000px+ per side straight off a phone camera; `util/BitmapUtils.kt` provides
  `downsampleImageToJpeg()` (used once, in `ProfileRepository.setPhoto`, to shrink+compress to
  `PROFILE_PHOTO_MAX_DIMENSION` = 720px before it's ever written to disk) and
  `decodeSampledBitmapFromFile()` (used in `ProfileAvatar`/`ProfilePhotoViewerDialog`, decoding
  with a `BitmapFactory.Options.inSampleSize` computed from the actual target size instead of a
  bare `decodeFile`). Both matter: downsampling at save-time keeps the stored file small forever;
  bounded decode at display-time is cheap insurance for any file that predates this change. Use
  these two functions for any future feature that stores or displays a user-picked image, rather
  than a raw `contentResolver` byte copy or a bare `BitmapFactory.decodeFile`.
- **`expense.dateMillis` is indexed** (`MIGRATION_5_6`) because nearly every expense query filters
  or sorts by it — period totals, the day-detail lookup, Home's recent-activity preview. If a new
  query is added on a column that's filtered/sorted on the full table (not just small tables like
  `expense_category`), check whether it needs an index rather than assuming Room/SQLite handles it
  for free.

## Recommended Build Order

Local Backup/Restore (previously step 4 here) is done — see Feature List above. It turned out not
to need the multi-person `Profile` data model as a prerequisite after all: it's a single-user
full-DB snapshot, so it only needed the JSON serializer (`data/backup/MoneyManagerExport.kt`).
That serializer is what WhatsApp export/import reuses below.

1. Desires module (reuses EMI's list+progress pattern almost exactly)
2. Expense charts (Vico)
3. **Profile data model + migration** (prerequisite for #4 — multi-person scoping, not needed by
   Local Backup/Restore)
4. WhatsApp export/import (reuses `MoneyManagerExport`, the serializer built for Local
   Backup/Restore)
5. Home screen widget (Glance)

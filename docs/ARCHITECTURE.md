# Artha tracker — Architecture Plan

Design source: [Baby Tracker canvas](https://claude.ai/artifact/UEkiiESeSGJnMnkkceZmrg)

| Artboard | Screen |
|---|---|
| `Main.dc.html` | Today |
| `AddActivity.dc.html` | Log activity sheet (Bottle / Solids / Diaper; `editMode` variant) |
| `LogGrowth.dc.html` | Log growth sheet |
| `History.dc.html` | History (day groups, swipe-delete, Undo snackbar) |
| `Summary.dc.html` | Summary (today's totals + the five previous days). `Main.dc.html` gained its entry point: a Summary icon button in the header, left of Settings |
| `Onboarding.dc.html` | Onboarding (also reused as Edit profile from Settings) |
| `Settings.dc.html` | Settings |
| `LogPump.dc.html` | Log pumping sheet (Mom; `editMode` variant) |

### Where the plan overrides the mockups
The mockups contain placeholder values. Where they differ from this plan, **the plan wins**:
- Settings has no mock for **Mom's name** (§5 Settings). It's specced here and needs a design pass.
- Wording: Today says "Height" and the growth sheet says "Length". Both are kept as designed, and they refer to the same `length_mm` value.

## 1. Scope (v1)

| Area | Decision |
|---|---|
| Purpose | Real daily use + portfolio. Production-grade, not over-engineered |
| Users / sync | One device, **local only**. Data layer is sync-ready (UUIDs, `updatedAt`, soft deletes) so a backend can be added later without a rewrite |
| Babies | One in the UI. Every row carries `babyId` |
| Feed | **Bottle** (amount only; slider 1–10 oz, default 3) and **Solids** (food + amount + reaction) |
| Diaper | **Pee / Potty** (as designed) |
| Growth | Logged measurements with history, **date only (no time)**. Today shows the latest weight and height. Entered by tapping the stats on Today |
| Units | Stored metric (ml, g, mm). Settings toggle for Imperial/Metric display |
| Photo | Android Photo Picker → copied into app-private storage → Coil |
| Onboarding | One screen: name, birth date, optional photo. The same screen is reused as **Edit profile** from Settings |
| About | Version, an in-app Privacy policy screen, and Send feedback (opens an email draft). Personal install only, not on the Play Store |
| Time entry | Defaults to now; tap to open M3 date + time pickers; no future times, nothing before the birth date |
| History ("See all") | Grouped by day with daily totals; tap to edit in the same sheet; swipe to delete with Undo |
| Summary | A screen reached from an icon button in Today's header. **Today's totals** are the hero card (fed oz with bottle/solids counts, pumped oz with session count, diapers with pee/potty split); the **5 previous calendar days** are a quiet table (fed, pumped, diaper count). Tapping a day opens History **scrolled to that day** |
| Theme | Brand theme only (dark forest green + gold); Fraunces + Figtree; no dynamic color |
| Backup | Android Auto Backup (DB + DataStore + photos) |
| Mom: pumping | Log pumping sessions: amount, optional side (Left/Right/Both), optional duration, when, note. Pumping belongs to a **caregiver (Mom)**, not the baby. It's a separate table, and Mom's name is editable in Settings. **No milk stash**: sessions are a log only, not linked to bottle feeds |
| Extras | None in v1 (no reminders or widget). M11 adds Trends: a line chart per category over 30 days, linked from Summary |
| Quality | Unit tests + GitHub Actions CI |
| Platform | minSdk 29, target/compile the latest stable SDK, Kotlin, Compose, Hilt, Navigation 3 |

## 2. Screens & navigation

```
Profile(Create) ──(baby created)──► Today ──► History
                                      │  │         │
                                      │  └► Settings ──► Profile(Edit)
                                      │        │   └───► PrivacyPolicy
                                      │        └───────► mailto: intent (Send feedback)
            (sheet) LogActivity ◄─────┘ ◄── tap an activity row in History (edit)
            (sheet) LogGrowth   ◄── tap Weight/Height on Today, or a growth row in History
            (sheet) LogPump     ◄── tap the Pumping card on Today, or a pump row in History
```

| Nav key | Type | Args |
|---|---|---|
| `ProfileKey` | screen | `mode: Create \| Edit`. Create is the start destination when no baby exists (title "Tell us about your baby", button "Continue"). Edit comes from Settings (back arrow, "Save") |
| `TodayKey` | screen (root) | — |
| `HistoryKey` | screen | `focusEpochDay: Long?` (null = top; else scroll to that day) |
| `SummaryKey` | screen | — |
| `TrendsKey` | screen | — (opened from Summary's "View trends" link) |
| `SettingsKey` | screen | — |
| `PrivacyPolicyKey` | screen | — (static text: "all data stays on this device + Google backup") |
| `LogActivityKey` | bottom sheet | `entryId: String?` (null = new), `initialType: ActivityType?` (Feed or Diaper tile preselects it) |
| `LogGrowthKey` | bottom sheet | `entryId: String?` |
| `LogPumpKey` | bottom sheet | `sessionId: String?` (null = new) |

**Navigation 3 setup (in `:app`)**
- `rememberNavBackStack(...)`. The start key is decided by `MainActivityViewModel` (`Loading` / `NeedsOnboarding` / `Ready`), and the splash screen stays up while the state is `Loading`.
- `NavDisplay` with `entryDecorators = [rememberSaveableStateHolderNavEntryDecorator(), rememberViewModelStoreNavEntryDecorator()]`, so each entry gets its own ViewModel scope.
- A `BottomSheetSceneStrategy` (Nav3 recipe) renders the keys marked as sheets in `ModalBottomSheet`.
- **Feature modules never depend on each other.** Each feature exposes its `NavKey`s and an `EntryProviderScope<NavKey>.xxxEntries(onNavigate… lambdas)` extension. `:app` connects the lambdas to back stack operations.
- ViewModels that take nav args use Hilt assisted injection: `@HiltViewModel(assistedFactory = …)` + `hiltViewModel<VM, VM.Factory> { it.create(key) }`.

## 3. Module graph

```mermaid
graph TD
  app --> feature_profile & feature_today & feature_log & feature_growth & feature_pump & feature_summary & feature_trends & feature_history & feature_settings
  feature_profile & feature_today & feature_log & feature_growth & feature_pump & feature_summary & feature_trends & feature_history & feature_settings --> core_ui & core_designsystem & core_domain
  core_ui --> core_designsystem & core_model
  core_domain --> core_data
  core_data --> core_database & core_datastore & core_common & core_model
  core_database --> core_model
  core_testing -.test only.-> core_data & core_model
```

| Module | Contents |
|---|---|
| `build-logic/convention` | Plugins: `babystatus.android.application`, `.android.library`, `.android.feature` (library + compose + hilt + core:ui/designsystem/domain), `.android.compose`, `.hilt`, `.android.room`, `.jvm.library` |
| `:app` | `@HiltAndroidApp`, `MainActivity` (edge-to-edge, splash), `NavDisplay`, sheet scene strategy, backup rules |
| `:core:model` | **Pure Kotlin.** `Baby`, `Caregiver`, the `TimelineEntry` hierarchy (`LogEntry` for the baby, `PumpSession` for Mom), value types (`Volume`, `Mass`, `Length`), enums, `UnitSystem`, `UserPreferences` |
| `:core:common` | `@Dispatcher` qualifiers, injectable `java.time.Clock`, `IdGenerator` (UUID) |
| `:core:database` | Room DB, entities, DAOs, converters, exported schemas + migrations |
| `:core:datastore` | Preferences DataStore wrapper (`unitSystem`) |
| `:core:data` | Repository interfaces + implementations (`Baby`, `Log`, `Pump`, `Caregiver`, `Timeline`, `UserPreferences`), `PhotoStore` (copy/downscale/delete) |
| `:core:domain` | Use cases that combine repositories or hold rules: `GetTodayStatusUseCase`, `CalculateAgeUseCase`, `ValidateLogEntryUseCase`, `GetHistoryUseCase` |
| `:core:designsystem` | `BabyStatusTheme` (colors, bundled Fraunces/Figtree fonts, shapes), stateless components: `ActivityTile`, `StatLabel`, `BabyAvatar`, `SelectableTile`, `LogFab`, `PrimaryButton`, icons |
| `:core:ui` | UI that knows the model: `LogEntryRow`, `UnitFormatter`, `RelativeTimeFormatter`, `DateTimeField` (picker dialogs) |
| `:core:testing` | Fake repositories, `MainDispatcherRule`, test data builders |
| `:feature:profile` | Profile screen + VM, in Create mode (onboarding) or Edit mode (from Settings) |
| `:feature:today` | Today screen + VM |
| `:feature:log` | Log activity sheet (Bottle / Solids / Diaper; create + edit + delete) |
| `:feature:growth` | Log growth sheet (create + edit + delete) |
| `:feature:pump` | Log pumping sheet (create + edit + delete) |
| `:feature:summary` | Summary screen + VM (today's totals, previous days) |
| `:feature:trends` | Trends screen + VM (a line chart per category over 30 days) and its Canvas `LineChart` |
| `:feature:history` | Paged list grouped by day, swipe to delete + Undo |
| `:feature:settings` | Settings (profile card → Profile(Edit), units toggle, About) + Privacy policy screen |

## 4. Data layer

### 4.1 Domain model (`:core:model`)

```kotlin
data class Baby(val id: String, val name: String, val birthDate: LocalDate, val photoPath: String?)

/** Anything that shows on the History timeline. */
sealed interface TimelineEntry { val id: String; val date: LocalDate; val note: String? }

/** The baby's entries (log_entry table). */
sealed interface LogEntry : TimelineEntry
sealed interface TimedEntry : LogEntry { val occurredAt: Instant }       // date = occurredAt in local zone
data class Bottle(..., val volume: Volume) : TimedEntry
data class Solids(..., val food: String, val amount: SolidsAmount, val reaction: Reaction?) : TimedEntry
data class Diaper(..., val kind: DiaperKind) : TimedEntry
data class Growth(..., val weight: Mass?, val length: Length?) : LogEntry  // date only; at least one of weight/length

/** Mom's entries (pump_session table). Not a LogEntry: it isn't about the baby. */
data class Caregiver(val id: String, val role: CaregiverRole, val name: String)
enum class CaregiverRole { MOTHER }
data class PumpSession(              // no caregiverId: the data layer attaches Mom, as it does the baby
    override val id: String, val occurredAt: Instant, override val date: LocalDate,
    override val note: String?, val volume: Volume, val side: BreastSide?, val duration: Duration?,
) : TimelineEntry
enum class BreastSide { LEFT, RIGHT, BOTH }
enum class ActivityType { FEED, DIAPER }
enum class SolidsAmount { TASTE, SOME, LOTS }
enum class Reaction { LIKED, DISLIKED, ALLERGIC }
enum class DiaperKind { PEE, POTTY }
enum class UnitSystem { IMPERIAL, METRIC }

@JvmInline value class Volume(val ml: Double)   // oz = ml / 29.5735
@JvmInline value class Mass(val grams: Int)
@JvmInline value class Length(val mm: Int)
```

### 4.2 Room schema (`:core:database`)

The baby's entries share one `log_entry` table with a type discriminator, so "last feed/diaper" and "latest growth" stay simple indexed queries. Mom's pumping lives in its own `pump_session` table, owned by a `caregiver`. History merges the two with one `UNION ALL` (§4.2.1).

**`baby`**: `id TEXT PK`, `name`, `birth_epoch_day INTEGER`, `photo_path TEXT?`, `created_at`, `updated_at`, `deleted_at?`

**`log_entry`**

| Column | Type | Notes |
|---|---|---|
| `id` | TEXT PK | UUID, generated on the device (sync-safe) |
| `baby_id` | TEXT FK → baby | indexed |
| `type` | TEXT | `BOTTLE` / `SOLIDS` / `DIAPER` / `GROWTH` |
| `occurred_at` | INTEGER? | epoch millis UTC. **NULL for GROWTH** (date only) |
| `local_epoch_day` | INTEGER | local day at write time, used for grouping and daily totals without timezone math in SQL. For GROWTH it is the chosen date |
| `note` | TEXT? | |
| `volume_ml` | REAL? | BOTTLE |
| `food`, `solids_amount`, `reaction` | TEXT? | SOLIDS |
| `diaper_kind` | TEXT? | DIAPER |
| `weight_g`, `length_mm` | INTEGER? | GROWTH |
| `created_at`, `updated_at` | INTEGER | |
| `deleted_at` | INTEGER? | soft delete: powers Undo now, becomes a sync tombstone later |

Indexes: `(baby_id, deleted_at, local_epoch_day DESC, occurred_at DESC)`, `(baby_id, type, occurred_at DESC)`.

**`caregiver`** (schema v2): `id TEXT PK`, `role TEXT` (`MOTHER`), `name TEXT` (default "Mom"), `created_at`, `updated_at`, `deleted_at?`. Household-level, with no `baby_id`: with twins, one mother pumps for both. Only the pumping owner for now; a "logged by" link on every entry waits for sync and accounts.

**`pump_session`** (schema v2)

| Column | Type | Notes |
|---|---|---|
| `id` | TEXT PK | UUID |
| `caregiver_id` | TEXT FK → caregiver | indexed |
| `occurred_at` | INTEGER | epoch millis UTC (always timed) |
| `local_epoch_day` | INTEGER | same role as in `log_entry` |
| `volume_ml` | REAL | required |
| `side` | TEXT? | `LEFT` / `RIGHT` / `BOTH` |
| `duration_min` | INTEGER? | 1–180 |
| `note` | TEXT? | |
| `created_at`, `updated_at`, `deleted_at?` | INTEGER | same sync-ready conventions |

Index: `(caregiver_id, deleted_at, local_epoch_day DESC, occurred_at DESC)`.

**Migration 1 → 2** only creates the two tables, so it's a Room `AutoMigration(from = 1, to = 2)` generated from the committed `1.json` / `2.json` schemas. The Mom row is created lazily by `CaregiverRepository.mother()` (get-or-insert, in a transaction). That one code path covers fresh installs, upgrades and restored backups, and nothing extra is needed at onboarding.

#### 4.2.1 The merged timeline

`TimelineDao.pagingSource(babyId)` is a raw `@Query` returning `TimelineRow`, a POJO with the union of both tables' columns plus `source` (`'LOG'` / `'PUMP'`):

```sql
SELECT 'LOG' AS source, id, type, occurred_at, local_epoch_day, created_at, note, volume_ml, food, …, NULL AS side, NULL AS duration_min
  FROM log_entry WHERE baby_id = :babyId AND deleted_at IS NULL
UNION ALL
SELECT 'PUMP', id, 'PUMP', occurred_at, local_epoch_day, created_at, note, volume_ml, NULL, …, side, duration_min
  FROM pump_session WHERE deleted_at IS NULL
ORDER BY local_epoch_day DESC, occurred_at IS NULL DESC, occurred_at DESC, created_at DESC
```

Room's `PagingSource` for a multi-table query observes both tables, so any write to either invalidates History. `observeDailyTotals` is a second grouped `UNION ALL` that adds `pumped_ml` next to bottle ml and the pee/potty counts. Malformed rows of either kind are skipped by the mapper, as today.

**Timeline order** (History and anything mixing types): `ORDER BY local_epoch_day DESC, occurred_at IS NULL DESC, occurred_at DESC, created_at DESC`. Growth rows (NULL time) sort to the top of their day.

Key DAO queries (all return `Flow` unless noted):
- `observeLatest(babyId, types, limit)`: today's recent list and last feed/diaper
- `observeLatestWeight(babyId)` / `observeLatestLength(babyId)`: the most recent non-null value of each by `local_epoch_day DESC, updated_at DESC`. They may come from different measurements
- `pagingSource(babyId)`: `PagingSource<Int, LogEntryEntity>` for History, in timeline order
- `observeDailyTotals(babyId)` (now on `TimelineDao`): `GROUP BY local_epoch_day` over the union → bottle ml, **pumped ml**, pee count, potty count (header: "18 oz fed · 14 oz pumped · 4 pee · 2 potty"; solids and growth aren't totalled)
- `PumpDao`: `observeTotalOn(day)` (sum ml), `observeLatest()`, `getById`, `upsert`, `softDelete`, `restore`
- `observeFoodSuggestions(babyId, prefix)`: `SELECT food … GROUP BY food ORDER BY MAX(occurred_at) DESC` (most recently eaten first), merged in the repository with a built-in starter list (Banana, Sweet potato, Avocado, Rice cereal, Peas, Applesauce) so suggestions appear before any solids have been logged
- `upsert`, `softDelete(id, now)`, `restore(id)`

Mapping entity ↔ `LogEntry` happens in `:core:data`. A malformed row (for example BOTTLE with a null volume) is logged and skipped, never allowed to crash the app.

### 4.3 Repositories (`:core:data`)

```kotlin
interface BabyRepository { fun observeBaby(): Flow<Baby?>; suspend fun create(...); suspend fun update(baby: Baby) }
interface LogRepository {
    fun observeRecent(limit: Int): Flow<List<LogEntry>>
    fun observeLast(type: ActivityType): Flow<TimedEntry?>
    fun observeLatestGrowth(): Flow<LatestGrowth>
    fun foodSuggestions(prefix: String): Flow<List<String>>
    suspend fun get(id: String): LogEntry?
    suspend fun save(entry: LogEntry)          // upsert, stamps updatedAt
}
interface PumpRepository {
    fun observeSummary(today: LocalDate): Flow<PumpSummary>   // total pumped today + last session
    suspend fun get(id: String): PumpSession?
    suspend fun save(session: PumpSession)
}
interface CaregiverRepository {
    fun observeMother(): Flow<Caregiver>                       // emits the default "Mom" until the row exists
    suspend fun mother(): Caregiver                            // get-or-create
    suspend fun rename(id: String, name: String)
}
/** History and delete/undo span both tables. */
interface TimelineRepository {
    fun historyDays(): Flow<PagingData<TimelineDay>>           // one item per day that has entries, newest first
    suspend fun dayIndex(date: LocalDate): Int                 // that day's position in historyDays(); used to scroll
    fun observeSummaries(from: LocalDate): Flow<Map<LocalDate, DaySummary>>   // from `from` to today; days with no entries are absent
    suspend fun delete(id: String)                             // soft; routes by which table holds the id
    suspend fun undoDelete(id: String)
}
interface UserPreferencesRepository { val preferences: Flow<UserPreferences>; suspend fun setUnitSystem(u: UnitSystem) }
interface PhotoStore { suspend fun importFromPicker(uri: Uri): String /* relative path */; suspend fun delete(path: String) }
```

`babyId` resolution stays inside the repositories ("the single baby"), so the UI never passes it around. Supporting several babies later becomes a repository change plus a `activeBabyId` preference.

### 4.4 Units

- Always stored metric. The conversion happens once, in `UnitFormatter` (display) and in the slider state (input).
- Bottle **and pump** sliders share one `VolumeScale` (formerly `BottleScale`): Imperial 1–10 oz in 0.5 steps, default 3 oz. Metric 30–300 ml in 10 ml steps, default 90 ml. When editing, the stored value snaps to the nearest step.
- Display: weight `13.2 lb` / `6.0 kg`; length `23.5 in` / `59.7 cm`; volume `3 oz` / `90 ml`.

### 4.5 Photos

`PickVisualMedia(ImageOnly)` → `PhotoStore` decodes with `ImageDecoder`, downsizes to ≤1024 px, writes `filesDir/photos/<uuid>.jpg`, updates `baby.photoPath` and deletes the previous file. No runtime permissions needed.

### 4.6 Days and summaries (M10)

One idea replaces two: **a day's entries are the source, and its totals are computed from them in Kotlin.** That removes the SQL `GROUP BY` (`TimelineDao.observeDailyTotals`, `DailyTotalsRow`, `DailyTotals`) and its mapper, so History's header and the Summary screen can never disagree.

```kotlin
// :core:model
data class DaySummary(
    val date: LocalDate,
    val fed: Volume, val bottleCount: Int, val solidsCount: Int,     // fed = bottles only: solids have no volume
    val pumped: Volume, val pumpSessions: Int,
    val pee: Int, val potty: Int,
    val entryCount: Int,                                             // every entry, growth included (counted nowhere else)
    val weight: Mass?, val length: Length?,                          // M11: the day's most recent growth values, null if none
) { val diapers get() = pee + potty; val hasEntries get() = entryCount > 0 }
data class TimelineDay(val date: LocalDate, val entries: List<TimelineEntry>) { val summary = summarize(date, entries) }   // timeline order inside the day
fun summarize(date: LocalDate, entries: List<TimelineEntry>): DaySummary
```

`TimelineDao` changes (queries only, **no schema change, no migration**):
- `pagingSourceDays(babyId): PagingSource<Int, TimelineDayRow>` (Room can't page a bare `Long`, so the row is a one-column POJO): `SELECT DISTINCT local_epoch_day` over the same union, newest first. Room's source counts the rows, so Paging has **placeholders**: the list knows its full length before pages load.
- `entriesOnDay(babyId, day): List<TimelineRow>` (suspend, one query per day, run inside `PagingData.map`), mapped into a `TimelineDay`; days whose entries all just vanished (a delete racing the page) are filtered out.
- `countDaysAfter(babyId, day): Int` = `COUNT(DISTINCT local_epoch_day) WHERE local_epoch_day > :day`, which is exactly a day's index in the list. That powers `dayIndex`.
- `observeEntriesSince(babyId, fromDay): Flow<List<TimelineRow>>`, a few dozen rows at most, for the Summary screen.

`GetSummaryUseCase(today)` (`:core:domain`) combines the baby (for the birth date) with `observeSummaries(today - 5 days)` into `Summary(today: DaySummary, previous: List<DaySummary>)`: five calendar days, newest first. A day with nothing logged is `summarize(day, emptyList())` (so `hasEntries` is false), never missing. Days before the baby's birth date are left out.

**M11 (Trends):** `summarize` also fills `weight` and `length` from the day's `GrowthMeasurement` entries. A day's entries arrive newest first, so the first non-null value of each wins (the day's latest measurement), and they can come from different entries. A day with no growth entry has `null` for both; `entryCount` still counts growth as before. `GetTrendsUseCase(today)` combines the baby with `observeSummaries(today - 29 days)` into `Trends(days: List<DaySummary>)`: `TREND_DAYS = 30` calendar days ending today, **oldest first**, empty days filled with `summarize(day, emptyList())`, days before the birth date left out. No new query, no schema change.

## 5. UI layer pattern

Every screen follows the same structure:

```
XxxRoute(viewModel)  ── collectAsStateWithLifecycle ──►  XxxScreen(uiState, onAction)   // stateless, previewable
XxxViewModel: StateFlow<XxxUiState> (stateIn WhileSubscribed(5s)), fun onAction(XxxAction)
One-off events (e.g. "saved → dismiss", "deleted → show Undo") exposed as state flags consumed by the UI, not Channels.
```

### Today
- `TodayUiState.Loading | Ready(name, dateLabel, photoPath, age, weight?, length?, lastFeed: RelativeTime?, lastDiaper: RelativeTime?, recent: List<LogEntryUi>)`
- Combines baby, latest growth, last feed, last diaper, recent entries, unit preference and a **per-minute ticker** (`flow { while(true) { emit(clock.instant()); delay(untilNextMinute) } }`) so "2h 25m ago" stays current.
- Both tiles show relative time ("2h 25m ago"). The design mixed relative and clock time.
- The recent list shows the 5 most recent entries from today, with an empty state when there are none.
- **Pumping card** (under the Feed/Diaper tiles): pump icon on `#F6E1DA`, "Pumping" + a **MOM** badge, subtitle "14 oz today · last 3h ago" ("Nothing today · last 1d ago" / "Not logged yet"), and a rust `#8E4436` "+ Log" pill. The whole card is one button → `LogPumpKey()`. Data: `PumpRepository.observeSummary(today)` combined in `TodayViewModel` (it's Mom's, so it stays out of `GetTodayStatusUseCase`).
- The **Recent list stays baby-only**: pumps don't appear there. History shows them.
- **Header (M10):** an icon-only **Summary** button (bar-chart icon, content description "Summary") sits left of the Settings button, drawn exactly like it (44 dp `CircleIconButton`, `goldLight` fill, forest icon, 8 dp gap), opening `SummaryKey`. The name column keeps `weight(1f)` and ellipsizes, so a long name never pushes the buttons off screen.
- Layout per the new mock: photo 150 dp (was 168) and the status block 284 dp tall, so the extra card fits without scrolling on a 390×844 screen.
- Age comes from `CalculateAgeUseCase`: under 4 weeks `"12 days"`, under 24 months `"3 mo, 1 wk"`, after that `"2 yr, 3 mo"`.

### Log activity sheet
- Form state: `type (FEED|DIAPER)`, `feedKind (BOTTLE|SOLIDS)`, `bottle: Volume`, `food`, `solidsAmount`, `reaction?`, `diaperKind`, `occurredAt`, `note`, `mode (New|Edit)`.
- Feed shows a Bottle | Solids segmented control. Solids shows:
  - a food text field with suggestions
  - required Taste / Some / Lots chips with nothing preselected, so Save stays disabled until one is picked
  - optional Liked / Disliked / Allergic reaction chips; tapping the selected chip clears it
- Save button text: "Save feed" / "Save diaper change".
- Edit mode changes the title to "Edit activity", adds a "Delete entry" text button under Save, and keeps the type locked. Deleting closes the sheet and the underlying screen shows the Undo snackbar.
- `ValidateLogEntryUseCase`: Solids needs a food and an amount; time can't be in the future or before the birth date.

### Log growth sheet
- Date (date picker only), Weight and Length fields in the current unit (lb/in or kg/cm), a hint box ("Enter weight, length, or both"), an optional note, and "Save growth".
- At least one measurement is required. The date can't be in the future or before the birth date.
- Edit mode follows the activity sheet: the title becomes "Edit growth" and a "Delete entry" button appears. (The mockup has no edit variant, so this mirrors AddActivity.)

### Log pumping sheet (`:feature:pump`)
- Header: a small **MOM** badge (`#F6E1DA` / `#7A2F22`) above the title "Log pumping" / "Edit pumping", and the close button.
- "Amount pumped": the same slider card as the bottle (`VolumeScale`, rust `#8E4436` accent), default 3 oz / 90 ml.
- "Side (optional)": Left / Right / Both option buttons (rust accent); tapping the selected one clears it.
- Two columns: "Duration (optional)" (number field, "min" suffix, 1–180) and "When" (date + time picker, **no future time and no birth-date lower bound**, since mothers may pump before the birth).
- Note (placeholder "Froze 2 oz"), "Save pumping", and "Delete entry" in edit mode. Delete goes through the shared Undo snackbar.
- `LogPumpViewModel` resolves `CaregiverRepository.mother()` on save, so the first-ever save creates the Mom row.
- New design tokens: an `Accent` `mom` (`#7A2F22` fg / `#F6E1DA` bg / `#8E4436` border), `momIcon` `#9A4535`, and a `Pump` stroke icon from the mock. A small shared `MomBadge` component goes in `:core:designsystem`.

### History
- A top bar with a back arrow and a light background (`#EEF5EC`). Each day is a header plus one white rounded card of rows.
- **Restructured in M10: one list item per day, not per entry.** `Pager` → `PagingData<TimelineDay>` (§4.6). A day item draws its own title (`Today`, `Yesterday`, `Mon, Sep 22`), the totals line, and the card with all of that day's rows. `insertSeparators`, `HistoryItem.DayHeader` and the first/last-row corner logic go away: the card clips its rows. The totals line comes from `TimelineDay.summary()`: "18 oz fed · 14 oz pumped · 4 pee · 2 potty" in current units, with "· N oz pumped" left out on days with no pumping.
- **Scroll to a day:** `HistoryKey(focusEpochDay)` goes to `HistoryViewModel` through assisted injection. It reads `dayIndex(date)` once and exposes it as `initialScrollIndex`. The screen calls `LazyListState.scrollToItem(index)` once. Placeholders make that index valid before the pages load; reading items near it makes Paging load them. No `initialKey` is needed.
- **Why per-day items:** with `insertSeparators`, headers aren't counted in placeholders, so a day's list index couldn't be computed without loading everything above it.

| Entry | Icon (circle bg / fg) | Title | Subtitle | Time |
|---|---|---|---|---|
| Bottle | bottle, gold `#F7E9B8` | Feed · Bottle | `5 oz` | `1:40p` |
| Solids | bowl, `#F0E4B8` | Solids · Sweet potato | `Some · Liked it` | `12:30p` |
| Diaper | drop, green `#D9ECD9` | Diaper · Pee / Potty | — | `2:05p` |
| Growth | trend, dark `#1B4332` | Growth | `13.2 lb · 23.5 in` (or just one) | *(none)* |
| Pump | pump, rust `#F6E1DA` / `#9A4535` | Pump + **MOM** badge | `4 oz · Both · 15 min` (missing parts left out) | `1:10p` |

- `SwipeToDismissBox` (end-to-start, red `#8B3A2E` "Delete" background) → soft delete → snackbar "Entry deleted" + **UNDO** → `undoDelete`.
- Tapping a row opens LogActivity, LogGrowth or LogPump in edit mode. Swipe-delete and Undo go through `TimelineRepository`, so pumps behave the same.

### Summary (new in M10, `:feature:summary`)
- **Top bar:** the shared forest `ForestTopBar` (back arrow + "Summary").
- **"View trends" link (M11):** the first item of the scrolling content, above "Today": the `Growth` trend-line icon + "View trends", styled like "Full history" (14 sp bold, leaf green), a `TextButton` (`Role.Button`) at least 48 dp tall. It opens `TrendsKey`.
- **Today** (hero): the title "Today" with "so far · Sep 24" beside it, then one white 24 dp card (soft shadow) of three rows, each a 48 dp icon badge, a title with a subtitle, and a big Fraunces number:
  - **Fed**: bottle badge; subtitle "4 bottles · 1 solids"; number `18` + "oz" (bottles only).
  - **Pumped** + **MOM** badge: pump badge (rust); subtitle "3 sessions"; number + unit.
  - **Diapers**: drop badge; subtitle "4 pee · 2 potty"; number `6` with no unit.
  - Counts use plurals ("1 bottle", "1 session"). Zero values still show ("0 oz", "0 sessions"). All three rows always appear.
- **Previous days** (quiet): a small caps title with a "Full history" link (leaf green, opens History at the top). Under it a four-column table, **Day · Fed · Pumped · Diapers**, with the numbers right-aligned in muted ink (`#5E574F`).
  - Rows are **the 5 calendar days before today**; the label is `Yesterday`, then `Tue, Sep 22`. Fed and pumped use the current units ("27 oz"); Diapers is the total count.
  - A day with nothing logged shows "—" in every cell and **isn't tappable**; the others open `HistoryKey(focusEpochDay)`.
  - If the baby is less than five days old, only days from the birth date show. With no previous days yet, the section shows "Earlier days will show up here."
- **Live:** `SummaryViewModel` combines `GetSummaryUseCase`, the unit preference and the same per-minute ticker as Today, so the totals update the moment you log something and roll over at local midnight (also when the time zone changes).
- **Design tokens:** `displayMedium` (Fraunces 500, 40 sp) for the big numbers, `BabyIcons.BarChart` (paths `M5 20V12`, `M12 20V5`, `M19 20v-9`). The badges reuse `IconBadge`, `MomBadge` and the Feed/Diaper/`mom` accents. `UnitFormatter.volumeParts(volume, units)` returns the number and unit separately ("18", "oz").
- **Accessibility:** the table has no Compose role, so each previous-day row is one `Role.Button` with a spoken description: "Yesterday. Fed 27 ounces. Pumped 16 ounces. 8 diapers. Opens history." Empty days read "Mon, Sep 22. Nothing logged.", and each hero row reads as one sentence.

### Trends (new in M11, `:feature:trends`)
- **Top bar:** `ForestTopBar` (back arrow + "Trends") on the sage background.
- **Category toggle:** a `SegmentedToggle` of **Growth · Feeding · Diapers · Pumping** (a radio group). Its segments use 4 dp side padding (the toggle's new `segmentPadding`, default 16 dp) so all four labels fit on a 390 dp screen. Feeding is selected on arrival; the choice lives in `TrendsViewModel`, so it survives rotation.
- **One white card** for the selected category: a title (heading), one figure, the line chart, and the window's first and last dates under it.

  | Category | One point per | Value | Title / subtitle |
  |---|---|---|---|
  | Growth | day with a weight | `day.weight` | "Weight" · "Now 13.2 lb" (the newest point) |
  | Feeding | every day in the window | `day.fed` (bottles only) | "Fed per day" · "Avg 24 oz / day" |
  | Diapers | every day in the window | `day.diapers` | "Diapers per day" · "Avg 6 / day" |
  | Pumping | every day in the window | `day.pumped` | "Pumped per day" · "Avg 16 oz / day" |

- **`LineChart`** (in `:feature:trends`, its only consumer; it moves to `:core:designsystem` if a second screen needs it): stateless, Compose `Canvas` only, no chart library. A polyline with a dot per point, a faint gridline at the top and bottom of the plotted range with its min and max as y labels, and the dates under the plot. Points are spaced **by date** across the window, so a sparse Growth series isn't stretched. The y range is `0 .. max` for Feeding, Diapers and Pumping (a quiet day reads as low) and `min .. max` for Growth, inset so the end points clear the labels. Values stay metric in the model; `UnitFormatter` formats the labels.
- **Empty state:** fewer than two points → "Not enough logged yet to show a trend." instead of the chart.
- **Live:** `TrendsViewModel` combines `GetTrendsUseCase` (driven by `clock.todayDates()`, so the window rolls over at midnight), the selected category and the unit preference into `TrendsUiState.Loading | Ready(trends, category, unitSystem)`.
- **Accessibility:** the chart is one node (`clearAndSetSemantics`) that reads "Fed per day, last 30 days. From 2 oz to 32 oz. Average 24 oz per day." The screen and card titles are headings.

### Settings
- A profile card (avatar, name, "Born Jun 12, 2026", chevron) → `ProfileKey(Edit)`.
- **Mom's name** (no mock yet): a white card row under the baby's profile card with the pump icon, "Mom" as the title, the caregiver's name as the subtitle, and a chevron. Tapping it opens an M3 dialog with one text field (1–40 chars) → `CaregiverRepository.rename`. The badge on Today/History/sheet stays the fixed word **MOM**; the name is for Settings and future sync.
- Units: an Imperial | Metric segmented toggle, with the helper text "Imperial shows oz, lb and in · Metric shows ml, kg and cm".
- About: Version (from `BuildConfig.VERSION_NAME`), Privacy policy → `PrivacyPolicyKey` (in-app text), Send feedback → `Intent(ACTION_SENDTO, "mailto:")` with the subject "Artha tracker feedback (v1.0.0)" and the address from `BuildConfig.FEEDBACK_EMAIL`.

### Profile (onboarding / edit)
- A green header band ("Welcome" / "Tell us about your baby"), a photo button overlapping the band (dashed gold circle; shows the photo once one is chosen), the name field, and a birth date field (tapping it opens the M3 DatePicker; future dates blocked).
- Create mode: "Continue" is enabled when the name isn't blank and a birth date is set. It creates the baby, then **replaces** the back stack with `[TodayKey]`.
- Edit mode: a back arrow, "Save" instead of Continue, and the fields are prefilled.

## 6. Cross-cutting

| Concern | Approach |
|---|---|
| DI | Hilt. `@Binds` repository interfaces in `:core:data`, DB/DAO providers in `:core:database`, `Clock` + dispatchers in `:core:common` |
| Time | `java.time` (native at minSdk 29). `Clock` is injected everywhere, so tests control "now" |
| Threading | Room/DataStore are main-safe. `PhotoStore` runs on the `@Dispatcher(IO)` dispatcher |
| Edge-to-edge | Required at target SDK 35+. Insets are handled per screen, and the status bar sits over the dark green |
| Fonts | Fraunces + Figtree TTFs bundled in `:core:designsystem/res/font` (OFL), not downloaded, so there's no GMS dependency |
| Backup | `android:fullBackupContent` (API 29–30) + `android:dataExtractionRules` (31+) including `database/`, `datastore/`, `files/photos/` |
| Permissions | None |
| Build | Version catalog `gradle/libs.versions.toml`, KSP for Room and Hilt, Compose BOM, convention plugins |
| Libraries | Compose + Material3, Navigation 3 (+ lifecycle-viewmodel-navigation3), Hilt, Room, Paging 3, DataStore, Coil 3, kotlinx-serialization (NavKeys), core-splashscreen |

## 7. Testing & CI

- **Unit tests (JVM)** with JUnit, `kotlinx-coroutines-test`, Turbine and fakes from `:core:testing`:
  - Every ViewModel (state mapping, validation, save/delete/undo flows)
  - Use cases (`CalculateAgeUseCase` edge cases: leap years, month ends; `ValidateLogEntryUseCase`)
  - `UnitFormatter`/conversions (round-trip snapping), `RelativeTimeFormatter`
  - Entity ↔ model mappers, including malformed rows
- Room schema JSON is exported and committed, so migrations stay reviewable (migration tests can come later).
- **GitHub Actions** (`.github/workflows/ci.yml`) on each PR and push to `main`: `./gradlew assembleDebug lintDebug testDebugUnitTest`, Gradle cache, JDK 21. Reports are uploaded when it fails.

## 8. Backup & restore (M8)

**Goal: lossless backup/restore only.** Human readability is a non-goal. Occasional **hand edits** (add or remove a row) must work, and the one-time Baby Tracker import (§8.1) depends on that.

### Format: a `.bsbackup` file (a ZIP)

```
babystatus-2026-09-26.bsbackup
├── manifest.json                   format, formatVersion, appVersion, roomSchemaVersion, exportedAt
├── baby-status.db                  snapshot via SQLite `VACUUM INTO` (consistent, no -wal/-shm)
├── user_preferences.preferences_pb DataStore file
└── photos/<uuid>.jpg
```

- **A snapshot of the database, not DTOs.** It's lossless by construction: new tables such as M7's are included automatically, with no mapping layer to forget.
- **Older backups are upgraded by Room's own migrations** when they're opened. There's no second versioning scheme. A backup whose `roomSchemaVersion` is newer than the app's is refused ("Update the app to restore this backup").
- **No content checksums in the manifest, so hand-edited files are accepted.** Corruption is caught by ZIP CRC and `PRAGMA integrity_check`.

### Restore (replaces everything; there is no merge)
1. Pick the file with `OpenDocument` (the system file picker; no permissions, still offline).
2. Unzip to cache. Open the `.db` as a **temporary** Room database: this runs any migrations and `integrity_check`, and the live data is still untouched.
3. **Validate every row** with the normal mappers. Repair `local_epoch_day` from `occurred_at` in the phone's zone when they disagree (the column hand edits get wrong most). Count rows that can't be read.
4. **Preview:** baby name, date range, counts per kind, and "N rows can't be read and will be skipped". The button says "Replace all current data".
5. **Swap:**
   - close the live DB, then copy in the temp DB, preferences and photos
   - delete photos nothing references any more
   - restart the app process so every Flow and cache reloads

### Backup
`CreateDocument("application/zip")` suggests `babystatus-<date>.bsbackup`. It writes the snapshot, preferences, photos and manifest to the chosen file, with a one-line warning: "This file contains your baby's data. Keep it somewhere private."

### Where it lives
- **`:core:backup`**: `BackupExporter`, `BackupRestorer` (validate → preview model → swap) and `BackupManifest`. It depends on `:core:database`, the DataStore file and the photo directory.
- **`:feature:settings`**: a "Backup" section with "Back up to file" and "Restore from file", plus the restore preview dialog.
- **`:feature:profile` (onboarding)**: a "Restore from a backup" text button under Continue. A new phone is exactly when restore is needed. (Proposed default.)
- **`docs/EDITING.md`**: the column rules for hand edits (UUID ids, epoch-ms UTC, `local_epoch_day`, metric units, exact enum strings, `deleted_at`) plus example `INSERT`s.

### 8.1 One-time Baby Tracker import (M9, built: a dev tool, not app code)

**Built** as `tools/babytracker_import/babytracker_csv.py` (+ `test_babytracker_csv.py`, 20 tests). The source is Baby Tracker's (nighp.com) **CSV export** (`csv.zip`: `Ari_nursing`, `Ari_diaper`, `Ari_formula`, `Ari_growth`, `pump`). The PDF report was dropped: the CSV is the source of truth, and it's cleaner (decimal pounds, per-side pump amounts). It's imported once, so there's **no in-app importer**.

Because the M8 backup/restore feature isn't built yet, the script writes **straight into the app's SQLite file over adb** (debug build only; `run-as` opens debuggable apps):

```
python tools/babytracker_import/babytracker_csv.py csv.zip --adb              # dry run
python tools/babytracker_import/babytracker_csv.py csv.zip --adb --write      # import
python tools/babytracker_import/babytracker_csv.py csv.zip --db some.db ...   # a local file instead
```
`--write` force-stops the app, saves the untouched `baby-status.db` (+ `-wal`/`-shm`) to a backup folder (the undo), merges the WAL locally, inserts in one transaction, runs `integrity_check` + `foreign_key_check`, pushes the file back and deletes the stale `-wal`/`-shm`. Once M8 exists the same script could edit a `.bsbackup`'s database instead.

**Mapping (decided):**

| Baby Tracker CSV | Result |
|---|---|
| `Ari_nursing.csv` (335 rows) | **Skipped.** No breastfeeding type, by decision |
| `Ari_formula.csv` (3) | `BOTTLE`, `volume_ml` = oz x 29.5735 (the formula label is lost, by decision) |
| `Ari_diaper.csv` Wet / Dirty / Mixed / Dry | `PEE` / `POTTY` / `POTTY` / skipped |
| `Ari_growth.csv` (3) | `GROWTH`, weight only (decimal lb -> g), date only. Head size isn't stored |
| `pump.csv` (64 rows) | `pump_session`; rules below |

**Pump rules:**
- **Rows with the same timestamp merge into one session** (the 10:50 AM session is an amount row plus a minutes row).
- **Volume** = left + right when given, else the total; summed across merged rows.
- **Side:** `Start side` is empty in the export, so it comes from the breasts that have an amount or minutes: LEFT / RIGHT / BOTH, else null.
- **Duration** = the longer of the two sides' minutes (their "total" adds the sides, double-counting a double pump). Outside 1-180 is dropped with a warning.
- **Skipped and reported:** a session with no amount (minutes only), and any amount over 20 oz (the export has one `53`). Override a typo with `--pump-fix "9/15/26 12:26 AM=5.3"`.

**Script behavior:**
- Times are wall-clock in `--zone` (default: the device's zone). `local_epoch_day` = the local date.
- Rows go to the app's existing baby (a name difference is reported) and to the mother row, created if missing.
- **Deterministic ids:** UUIDv5 of kind, local time and values **plus an occurrence number**, so two identical rows at the same minute (two Dirty diapers at 7:44 PM) stay two entries. Written with `INSERT OR IGNORE`: re-running adds nothing (verified: 0 to add, all "already").
- **Overlap:** a row is skipped if you already logged the same kind (same diaper kind) within +/-5 min (`--overlap-min`), or a growth entry on the same day. Deleted entries don't count.
- **Baby entries before the app's birth date** are skipped and reported; pumping has no such bound.
- **Dry run is the default.** `--preview file.md` writes a per-day table and every entry.

**Result on the export (Jul 9 - Aug 26, 2026):** 3 bottles, 15 diapers (12 potty, 3 pee), 3 growth, 61 pump sessions imported; 335 nursing rows, one 53 oz row and one minutes-only session skipped. Verified on an emulator: the app opened the edited database, and Today/History matched the preview.

## 9. Path to sync (not v1, design is ready)

1. Add `:core:network` (Firestore or Supabase) + `:sync` with a WorkManager `SyncWorker`.
2. Migration: add a `sync_state`/`server_version` column. Rows already have UUIDs, `updatedAt` and `deletedAt` tombstones.
3. Repositories become offline-first: write locally, mark dirty, push/pull deltas, resolve conflicts with last-write-wins on `updatedAt`.
4. Add auth + a family/household concept. `babyId` scoping already exists.

## 10. Build order

| Milestone | Deliverable |
|---|---|
| (status) | **M0–M5 built** and exercised on an emulator (Sep 24, 2026). **M7 built** (Sep 26, 2026): the upgrade from schema 1 was run on the emulator over real v1 data. M6 polish is partly done; what's left is listed in §12 |
| M0 | Gradle skeleton, build-logic, version catalog, empty modules, theme + fonts, CI green |
| M1 | `:core:model/database/datastore/data/domain` + tests; Profile (Create) end-to-end |
| M2 | Today screen (live data, ticker, age) + Log activity sheet (Bottle / Solids / Diaper) |
| M3 | Growth sheet + latest stats on Today |
| M4 | History (paging, day headers, totals, edit, swipe-delete + Undo) |
| M5 | Settings (units, About, privacy screen, feedback intent) + Profile (Edit) + photo + Auto Backup rules |
| M7 | **Pumping**: schema v2 + migration test, `caregiver`/`pump_session`, `TimelineDao` union, `TimelineRepository` (history, totals, delete/undo move here), `:feature:pump` sheet, Today card, History rows/totals, Settings Mom's name, privacy text mentions pumping |
| M8 | **Backup & restore** (§8): `:core:backup`, `.bsbackup` snapshot format, validated restore with preview + day repair, Settings rows, onboarding "Restore from a backup", `docs/EDITING.md` |
| M9 | **One-time Baby Tracker import** (§8.1): **built**, `tools/babytracker_import/`; run against the emulator, not yet against the real device |
| M10 | **Summary screen** (§4.6, §5): `DaySummary`/`TimelineDay` + `summarize`; `TimelineDao` day queries and removal of the SQL daily totals; **History restructured to one item per day** with scroll-to-day; `GetSummaryUseCase`; `:feature:summary`; the Today header button; design tokens; tests; a check on the phone against its real data |
| M11 | **Trends** (§4.6, §5): `DaySummary.weight`/`length`; `GetTrendsUseCase` (30 days, oldest first); `:feature:trends` with a Canvas `LineChart` per category (Growth · Feeding · Diapers · Pumping); the "View trends" link on Summary; tests; docs |
| M6 | Polish: empty states, accessibility (TalkBack labels, 48dp targets), app icon, release build config |

## 11. Defaults chosen (override if wrong)

- Package / applicationId: `com.sagar.babystatus`; app name **"Artha tracker"** (renamed from "Baby Status"; the applicationId stays `com.sagar.babystatus` so installed data survives).
- Feed/Diaper tiles on Today open the sheet preselected to that type; the FAB opens it on Feed.
- The Recent list shows only today's entries (max 5); "See all" shows everything.
- Deleted entries are kept as tombstones and never hard-deleted (small data, and sync needs them).
- The bottle slider always defaults to 3 oz / 90 ml, not the last amount logged.
- History has no filtering in v1.
- `FEEDBACK_EMAIL` is a `buildConfigField` read from `local.properties` (not committed), so no address is hard-coded in the repo.
- Solids amount is required and has no default. Reaction is optional.
- Pumping: amount required (default 3 oz / 90 ml); side and duration optional; no birth-date lower bound on the time.
- History totals leave out "oz pumped" on days with no pumping.
- The "Log activity" FAB and sheet don't include pumping; the Today card is the way in.
- Pump rows use the History title "Pump" (as mocked) while the card and sheet say "Pumping".
- Summary: the "day" is the local calendar day (midnight to midnight), the same as History. "Fed" counts bottle volume only (solids show as a count). The Diapers column is pee + potty.
- Summary: the Pumped row always shows, even for a day with no pumping. The previous-days window is fixed at 5 calendar days.
- History: unchanged to look at. Its day cards now come from per-day items, so they can be scrolled to by date.
- Trends: a fixed 30-calendar-day window ending today (no 7/30/90 picker). Growth plots **weight only** (one line per chart; length isn't charted). The default category is **Feeding**, while the toggle still lists Growth first. Growth's figure is the latest weight; the other three show a daily average.

## 12. As built: notes and deviations

Toolchain: Gradle 9.7.1, AGP 9.4.1 (built-in Kotlin, new DSL), Kotlin 2.4.20, KSP 2.3.12, Hilt 2.60.1, Room 2.8.5, Compose BOM 2026.09.00 (Material 3 1.4), Navigation 3 1.2.0, lifecycle 2.11. compileSdk/targetSdk 37, minSdk 29.

Where the code differs from sections 3–5:

- **No `GetHistoryUseCase`.** History's ViewModel reads `TimelineRepository.historyDays()` directly (M7 moved it off `LogRepository`; M10 made it one item per day); a use case would only have forwarded it.
- **`DeleteLogEntryUseCase`** (a `@Singleton`; with M7 it becomes `DeleteEntryUseCase` on top of `TimelineRepository`) soft-deletes and emits the id. `:app` collects it and shows the single "Entry deleted · UNDO" snackbar, so a History swipe and the sheets' "Delete entry" share one Undo path.
- **Food suggestions** are filtered in the ViewModel (`LogActivityViewModel.suggestions`): past foods (most recent first), then the starter list, matching the typed text and never the exact text. The DAO query just returns distinct foods.
- **Today's Recent list** shows timed entries only (growth has no time); tapping a row opens it for editing, as in History.
- **Sheets' `onDone` passes its key back** so `:app` pops only if that sheet is still on top (Save and Close can both fire). Every Back is guarded so the root is never popped.
- **The sheet scene** passes `ModalBottomSheetProperties` for light status-bar icons over the scrim, pads the sheet below the status bar, and implements `equals`/`hashCode` (NavDisplay compares scenes).
- **The History swipe state** uses plain `remember`, not `rememberSwipeToDismissBoxState` (which is saveable). LazyColumn restores saved item state by key, so an undone row came back already dismissed and was deleted again.
- **Dates** use `instant.atZone(zone).toLocalDate()`: `LocalDate.ofInstant` needs API 34 and lint (NewApi) flags it at minSdk 29.

M7 (pumping) as built:
- **`VolumeSliderCard`** moved to `:core:ui`, and the bottle and pump sheets share it with their own accent.
- **Delete/undo** routes by trying `log_entry` first, then `pump_session`. The DAOs' soft-delete queries return the row count, and ids are UUIDs, so only one table can match.
- **The Mom row** is created by `CaregiverDao.getOrCreateMother` (`@Transaction`) on the first pump save or rename. Until then `CaregiverRepository.mother` emits a default "Mom".
- **`PumpSession` has no caregiver id** in the model. `DefaultPumpRepository` attaches the mother on save, like `babyId` for the baby's entries.
- **Pumping card copy:** "Just now" reads as "last just now" on the card, since it's lowercased after "last". This is a copy nit to fix if it bothers anyone.

M10 (Summary) as built:
- **Totals are computed in Kotlin from a day's entries** (`summarize`), as planned; the SQL daily totals are gone. History's header and the Summary screen call the same function.
- **`entriesOnDay` is per day**, not per page (`entriesOnDays`, as the plan said). One small query per day item keeps `PagingData.map` simple, and a page is only ~20 days.
- **History scroll-to-day** uses `TimelineRepository.dayIndex(date)` (`COUNT(DISTINCT day) WHERE day > :date`) and `listState.scrollToItem(index)` once; `HistoryViewModel.initialScrollIndex` is cleared by `onScrolled()` so a rotation doesn't jump again. A day with no entries lands on the next older day that has some. `HistoryViewModel` is assisted-injected with its `HistoryKey`.
- **A day's rows are `key(entry.id)`ed** inside the day item. The swipe state still lives in plain `remember`, so it has to follow the entry, not its position, or the row after a deleted one inherits "dismissed".
- **The minute ticker moved to `:core:common`** (`Clock.minuteTicks()`, `Clock.todayDates()`), shared by Today and Summary so both roll over at local midnight the same way.
- **The previous-days table shows "—" for any zero figure**, not only on empty days: a day with no pumping reads quieter as a dash than as "0 oz". The hero card still shows real zeros ("0 oz", "0 sessions").
- **Checked on the Pixel_9a emulator, not the phone** (it wasn't connected): the Baby Tracker export plus a seeded week. Summary's Today card, the previous-days table and History's day headers agreed on every figure; a tap on a day opened History scrolled to it; the empty day did nothing; "Full history" opened History at the top; a History swipe-delete updated the Today header live and Undo brought the row back undismissed. Still worth a look on the phone against its real data.
- **Paging test tooling:** `FakeTimelineRepository.historyDays()` emits `PagingData.from(days, SETTLED)`. Without load states, `asSnapshot` waits for a load that never comes and times out. `:feature:history` tests use `paging-testing`.

Still open (M6 + M7):
- An automated migration test (`MigrationTestHelper`). It needs instrumented or Robolectric tests, which v1 skipped. The 1 → 2 upgrade was verified by installing over a v1 build on the emulator.
- Instrumented/UI tests and screenshot tests (out of v1 scope by choice).
- A release signing config. `assembleRelease` builds a minified, unsigned APK today.
- A11y pass with TalkBack on a device (semantics are in place: headings, roles, custom delete action, labels).

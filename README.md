# Artha tracker

A calm, one-handed baby tracker for Android: feeds (bottle and solids), diaper changes, and growth, with the baby's age and latest measurements front and center.

Everything stays on the phone (no account, no server, no internet permission). Android Auto Backup carries the data to a new phone.

## Features

- **Today**: age, photo, latest weight and height, time since the last feed and diaper (updated every minute), and today's entries.
- **Log activity** (bottom sheet): bottle (1–10 oz / 30–300 ml slider), solids (food with suggestions, amount, optional reaction), diaper (pee / potty), when, and a note. Tap an entry to edit or delete it.
- **Log growth**: weight and/or length for a date.
- **Log pumping** (Mom): amount, optional side and duration; a Pumping card on Today and pump rows in History.
- **Summary**: today's totals (fed, pumped, diapers) and a quiet table of the five days before it; tap a day to open it in History.
- **History**: every entry grouped by day with daily totals; swipe to delete with Undo.
- **Settings**: edit the baby's profile and photo, imperial / metric units, privacy policy, feedback.

## Tech

Kotlin · Jetpack Compose + Material 3 · Navigation 3 · Hilt · Room · Paging 3 · DataStore · Coil 3 · Coroutines/Flow · `java.time`

Multi-module, Now-in-Android style, with convention plugins in `build-logic/`:

```
app                      NavDisplay, sheets, undo snackbar, Hilt entry point
feature/{today,log,growth,pump,history,summary,profile,settings}
core/model               pure Kotlin domain types
core/domain              use cases (age, validation, today status, delete + undo)
core/data                repositories, photo storage
core/database            Room (log_entry, caregiver, pump_session; soft deletes)
core/datastore           preferences
core/designsystem        theme, fonts, components, icons
core/ui                  formatters, date/time pickers, bottom-sheet scene strategy
core/common, core/testing
```

The design and the reasoning behind it are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Build

Requires JDK 17+ (Android Studio's bundled JBR works) and the Android SDK with platform 37.

```bash
./gradlew assembleDebug          # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # unit tests
./gradlew lintDebug
```

Optional: set `feedback.email=you@example.com` in `local.properties` to prefill the Settings › Send feedback email.

## Licenses

Fonts: [Fraunces](licenses/Fraunces-OFL.txt) and [Figtree](licenses/Figtree-OFL.txt), SIL Open Font License 1.1.

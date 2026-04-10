# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
./gradlew assembleDebug          # Build debug APK → app/build/outputs/apk/debug/
./gradlew test                   # Run all unit tests
./gradlew test --tests "com.weyya.app.domain.CallDecisionEngineTest"           # Single test class
./gradlew test --tests "com.weyya.app.domain.CallDecisionEngineTest.first*"    # Single test method
```

## What This App Does

¡Wey Ya! is an offline Android call blocker. **Zero internet access** — no INTERNET permission, no analytics, no ads. Everything runs locally. It intercepts calls via Android's `CallScreeningService` (API 29+), decides whether to block based on configurable rules, and stores all data in Room/DataStore on-device.

## Architecture

**MVVM + Hilt** with three layers:

- **domain/** — Pure business logic, no Android dependencies. `CallDecisionEngine` is the central decision point: takes a phone number + current state → returns Allow/Disallow. `ScheduleChecker` evaluates time-based rules. `CallAttemptTracker` counts rapid repeat calls for urgency bypass.
- **data/** — Room database (4 migrations, schema in `/schemas`), DataStore preferences, contacts resolver. DAOs return `Flow<List<T>>` for reactivity. `ScheduleDao.getEnabledSync()` exists specifically for the synchronous `CallScreeningService` context.
- **ui/** — Compose screens with ViewModels exposing `StateFlow`. Navigation via Jetpack Navigation Compose (`WeyYaNavGraph.kt`).

**Key service**: `WeyYaScreeningService` extends `CallScreeningService`. Its `onScreenCall()` is synchronous by Android design — `runBlocking(Dispatchers.IO)` is the correct and standard pattern here, not an anti-pattern.

## Call Decision Flow

`WeyYaScreeningService.onScreenCall()` → `CallDecisionEngine.decide(phoneNumber, isContact, isWithinSchedule, blockingMode, threshold, windowMinutes)`:
1. If protection is off → Allow
2. If outside schedule → Allow
3. If number is in whitelist or contacts (for UNKNOWN_CALLERS mode) → Allow
4. If rapid repeat calls exceed threshold within window → Allow (urgency bypass)
5. Otherwise → Disallow

## Important Patterns

- **BigToggle colors**: Green = blocking unknown, Red = blocking all, Orange = active but outside schedule (calls pass), Gray = inactive
- **Schedule midnight crossing**: Schedules like "22:00-06:00" span two days. `ScheduleChecker` handles this by checking both current day and previous day
- **Minute slider**: `valueRange = 0f..55f, steps = 10` gives 12 positions at 5-minute increments (0, 5, 10...55). This is intentional UX, not a bug
- **i18n**: Spanish (default in `values/strings.xml`) + English (`values-en/strings.xml`)
- **Widget**: Jetpack Glance (`WeyYaWidgetReceiver`), not RemoteViews
- **Tests**: JUnit + Google Truth + Turbine. Tests live in `app/src/test/`. Domain layer has 90%+ coverage. No UI tests needed per project guidelines.

## DI Setup

Single Hilt module `DatabaseModule` provides Room DB singleton + all DAOs. `CallDecisionEngine`, `CallAttemptTracker`, `ScheduleChecker` are `@Singleton`. Services use `@EntryPoint` for injection (Android services don't support constructor injection).

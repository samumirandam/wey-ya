# Changelog

All notable changes to this project are documented here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
versioning follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.1] - 2026-06-04

### Fixed
- Edge-to-edge on Android 15 (SDK 35) no longer triggers Play Console "deprecated window API" warnings. The 1.2.2 fix (upgrading `androidx.activity` to 1.10.1) **did not** resolve this: that library's `enableEdgeToEdge()` still calls `Window.setStatusBarColor` / `Window.setNavigationBarColor` and sets `LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES` internally (verified in 1.10.1 bytecode). Replaced `enableEdgeToEdge()` in `MainActivity` with `WindowCompat.setDecorFitsSystemWindows(window, false)`, which enables edge-to-edge without the deprecated calls. System-bar icon contrast is now managed in `WeyYaTheme` via `WindowInsetsControllerCompat` (`isAppearanceLight*Bars`), and cutout drawing is preserved by the `android:windowLayoutInDisplayCutoutMode` theme attribute. With `enableEdgeToEdge()` unreferenced, R8 strips the offending `EdgeToEdge` classes from the release bundle. No dependency changes.

## [1.3.0] - 2026-06-04

### Changed
- Default app language switched from Spanish to English. `values/` now holds the English strings; the Spanish translation moved to the new `values-es/`. Devices with no matching locale now fall back to English instead of Spanish. The other locales (`pt`, `hi`, `in`) are unchanged.
- README, release-notes documentation, and the bundled privacy policy (`docs/privacy-policy.html`) translated to English.

### Added
- New `dedication` string resource, localized in all five languages. Replaces the single hardcoded Spanish string previously in `SettingsScreen`.

### Removed
- Redundant `values-en/strings.xml`, now superseded by the English default in `values/`.

## [1.2.2] - 2026-06-04

### Fixed
- Updated `androidx.activity` 1.9.3 → 1.10.1. **Note:** this was intended to clear Play Console's deprecated-window-API warnings, but it did not — `enableEdgeToEdge()` still calls those APIs internally even in 1.10.1. The actual fix landed in 1.3.1.
- Removed redundant `android:statusBarColor` / `android:navigationBarColor` declarations from `Theme.WeyYa.Main` in `themes.xml`; transparent system bars are handled at runtime by `enableEdgeToEdge()`. No visual change — content already drew behind the bars and all screens handle insets via `WindowInsets.navigationBars`.

## [1.2.1] - 2026-05-02

### Fixed
- "Activar" button on the role request card no longer silently no-ops when `RoleManager.isRoleAvailable()` returns `false` (some OEMs / non-telephony devices) or when the user denies the system role dialog. After the first attempt, the button switches to "Open Settings" and opens the system default-apps screen so users can pick ¡Wey Ya! manually, with a fallback to the app details screen if needed. Role and settings launches are now wrapped in `try/catch` for `ActivityNotFoundException`.

### Added
- New strings `role_denied_hint` and `role_open_settings` in all five locales (es, en, pt, hi, in).

## [1.2.0] - 2026-04-23

### Added
- Dual-SIM support: each schedule can apply to a specific SIM or to both.
- Lazy `READ_PHONE_STATE` runtime permission request, shown only on dual-SIM devices when the user opens Settings.
- SIM chip UI in schedule list and edit dialog, labeled with the carrier name resolved live from `SubscriptionManager`.
- Read-only "orphan" SIM chip when a schedule references a slot that is no longer active (SIM removed or permission revoked), so the user can clear the restriction.
- `SimResolver` with four resolution strategies for mapping `Call.Details.accountHandle` to a SIM slot (API 30+ direct lookup, iccId/subId heuristics, single-SIM shortcut, phone-number fallback).
- 9 new unit tests covering dual-SIM schedule evaluation and the `simSlot` entity default.

### Changed
- `ScheduleChecker.isBlockingActive` now accepts an optional `callSimSlot` and implements three-state semantics: empty schedule list blocks 24/7, no applicable schedule for the incoming SIM leaves that SIM unrestricted, otherwise the usual time window check runs.
- Room database version 4 → 5: `schedules` table gains a nullable `simSlot INTEGER` column. Existing rows migrate to `null` (applies to every SIM), preserving current behavior on upgrade.

### Notes
- Mono-SIM devices see zero UI changes. The permission dialog is never shown when `activeModemCount <= 1`.

## [1.1.0] - 2026-04

### Added
- About section in Settings with GitHub Issues and Play Store links (uses `Intent(ACTION_VIEW)`, no INTERNET permission required).
- Portuguese, Hindi, and Indonesian translations.
- Release signing configuration (optional when keystore is absent, so debug builds keep working).
- GitHub Actions workflow running unit tests on every push.
- Privacy policy bundled with the app for Play Store compliance.

### Changed
- Mode selector tab text now wraps correctly on narrow screens.
- `CallScreeningService` skips non-incoming and VoIP calls early, avoiding unnecessary work.

### Fixed
- Contact email in privacy policy.

## [1.0.0] - Initial release

- Offline call blocker built on `CallScreeningService` (API 29+).
- Three blocking modes: off, unknown callers, all calls.
- Configurable schedules with multi-day support and midnight crossing.
- Whitelist, blocked-call log, urgency bypass (rapid repeat calls within a time window).
- Privacy Dashboard, Jetpack Glance widget, Quick Settings tile.
- Spanish and English UI.
- Zero internet access — no INTERNET permission declared.

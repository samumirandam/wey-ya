# Coding Standards

Tooling (detekt + formatting rules, `StringsParityTest`, Room schema export) enforces style, string parity and formatting. This file covers only what tooling cannot check.

## Layers

- `domain/` imports no Android or Room. Map at the boundary with `entity.toDomain()`.
- No Repository layer: ViewModels and services inject DAOs directly.
- DAOs: reactive reads return `Flow<…>`; writes and one-shot lookups are `suspend`. Suffix `Sync` marks one-shot reads used by the screening service.

## ViewModels

- Expose individual `StateFlow<T>` properties via `stateIn(viewModelScope, WhileSubscribed(5_000), default)`. No `UiState` class, no loading/error state.
- Local state: `private val _x = MutableStateFlow(...)` + `val x = _x.asStateFlow()`.
- Screens collect with `collectAsStateWithLifecycle()`.

## Compose

- One `XxxScreen(navController, viewModel = hiltViewModel())` per feature in `ui/<feature>/`; sub-composables are `private fun` in the same file.
- Only `ui/components/` composables take a `modifier` parameter.
- No hardcoded strings. Enum → label via a `@StringRes` extension in `ui/common/`.
- New composables trip detekt `FunctionNaming`/`MagicNumber`: run `./gradlew detektBaseline` and commit the baseline in the same PR.

## Screening service

- `onScreenCall` is synchronous: `runBlocking(Dispatchers.IO)` + `withTimeoutOrNull` is intentional. Blocking non-suspend calls go inside `runInterruptible`.
- Fail open: any failure or timeout allows the call. Never crash the service.

## Data

- A new migration touches 4 places: `MIGRATION_X_Y` + `version` in `WeyYaDatabase`, `DatabaseModule.addMigrations`, `openWithRoom()` in the migration test, and a test opening the old DB. Index names: `index_<table>_<column>`.
- A new string goes into all 5 locales (`values`, `-es`, `-pt`, `-hi`, `-in`), same position, same section comment. `values-in` is Indonesian (Android legacy code, not `id`).

## Network

- No INTERNET permission. External URLs only via `Intent(ACTION_VIEW)`, which opens the system browser.

## Errors and logging

- Catch specific exceptions; discard with `_` when the fallback is obvious. `catch (e: Exception)` + `Log.e` only at boundaries that must not propagate.

## Comments

- English, explain why, `//` above the block. KDoc only for non-trivial contracts.
- `// TODO: YYYY-MM <reason>`.

## Tests

- Unit tests only, in `app/src/test/`. JUnit4 + Truth; names in backticks as a phrase.
- ViewModel tests extend `ViewModelTest`, MockK `relaxed = true`, stub every flow before constructing the VM (VMs read flows in initializers). With Turbine, the first item is the `stateIn` default.
- Robolectric only for migration tests, pinned to `@Config(sdk = [34])`: Robolectric needs Java 21 to sandbox SDK 36 and CI runs JDK 17.

## Releases

- Branch `chore/release-X.Y.Z`; commit `chore(release): X.Y.Z (versionCode N)`.
- Bump `versionCode`/`versionName`, add `## [X.Y.Z] - YYYY-MM-DD` to `CHANGELOG.md` (Keep a Changelog, English), add `release-notes/X.Y.Z.md`.
- Commits: Conventional Commits, English. PR bodies: Spanish.

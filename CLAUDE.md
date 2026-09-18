`domain/` has no Android imports: pure Kotlin, unit-tested in isolation.

`WeyYaScreeningService.onScreenCall()` is synchronous by Android design; the `runBlocking` there is intentional, keep it.

## Gotchas

- Indonesian resources live in `values-in/` (Android legacy code), not `values-id/`.
- External links use `Intent(ACTION_VIEW)` so they open the system browser and work without the INTERNET permission. The app declares no INTERNET permission; keep it that way.
- Only unit tests, under `app/src/test/`. No instrumented/UI tests by design.
- `./gradlew test` rejects `--tests`; filter with `./gradlew testDebugUnitTest --tests "com.weyya.app.domain.*"`.

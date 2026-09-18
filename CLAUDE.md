Conventions tooling cannot check (layers, ViewModel state, migrations, strings, tests, releases): `CODING_STANDARDS.md`. Read it before writing or reviewing code.

`./gradlew test` rejects `--tests`; filter with `./gradlew testDebugUnitTest --tests "com.weyya.app.domain.*"`.

## Agent skills

### Issue tracker

GitHub Issues (`samumirandam/wey-ya`) via `gh`. See `docs/agents/issue-tracker.md`.

### Triage labels

Default vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.

# Development documentation

This directory stores current development state, future direction, version planning, and chronological engineering history.

## Files

- [CURRENT.md](CURRENT.md) — concise current source of truth: active branch/baseline, current architecture boundary, blockers, validation state, and immediate next step.
- [ROADMAP.md](ROADMAP.md) — macro phases, planned work, deferred directions, prerequisites, and future-compatible seams.
- [VERSIONING.md](VERSIONING.md) — current development display-version policy and the first formal-release target.
- [DEVLOG.md](DEVLOG.md) — chronological engineering history.

## Historical integrity

`DEVLOG.md` is historical evidence.

When later evidence invalidates an older conclusion or implementation route:
- do not rewrite the old entry;
- append a later correction/invalidation;
- update `CURRENT.md` so the current source of truth no longer points at the invalidated route;
- update `ROADMAP.md` when the future direction changes.

An old Build can remain valid evidence about one SystemUI behavior while its overall architecture is no longer approved for new development.

## Relationship to reference evidence

Reusable implementation patterns are stored separately under [../reference/](../reference/).

Reference evidence is not development history and is not automatic permission to mutate SystemUI. Any adopted pattern still requires exact-target verification and the ownership/fail-native rules in `CONTRIBUTING.md`.

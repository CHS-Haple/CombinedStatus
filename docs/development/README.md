# Development documentation

This directory stores current development state, future direction, version planning, and chronological engineering history.

## Files

- [CURRENT.md](CURRENT.md) — concise current source of truth: active branch/baseline, current architecture boundary, blockers, validation state, and immediate next step.
- [ROADMAP.md](ROADMAP.md) — macro phases, planned work, deferred directions, prerequisites, and future-compatible seams.
- [VERSIONING.md](VERSIONING.md) — current development display-version policy and the first formal-release target.
- [DEVLOG.md](DEVLOG.md) — chronological engineering history.
- [RECORDING.md](RECORDING.md) — file-level writing conventions, templates, evidence language, and cross-file synchronization rules.

## CI routing quick reference

The normative CI contract lives in [CONTRIBUTING.md](../../CONTRIBUTING.md). The Build workflow keeps four scopes:

- **Light** — Draft and proven repository-only work.
- **Fast** — ordinary ready `feat/*` / `fix/* -> dev` app/runtime validation.
- **Integration** — trusted runtime integration on `dev`, including the signed Canary artifact.
- **Full** — build/dependency/CI/tooling changes and stable boundaries.

Routing is evaluated from the current base-to-head diff. The Build summary reports the selected scope, routing reason, detected runtime/build/CI/tooling/docs surfaces, and a mixed-surface warning when runtime work still carries a Full-triggering engineering surface. That warning is guidance, not permission to split an inseparable change or to weaken Full validation.

Signed work-branch Canary remains explicit and demand-driven after a successful trusted checkpoint. Changed paths alone do not prove that device testing is needed; runtime/device acceptance remains an engineering/maintainer decision.

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

# Current Development State

This file is the concise recovery point for active Combined Status development. Read it after `CONTRIBUTING.md`. Keep chronological investigation/build history in `DEVLOG.md`, future direction in `ROADMAP.md`, version semantics in `VERSIONING.md`, and record-writing rules in `RECORDING.md`.

## Repository baseline

- Last refreshed: 2026-09-27
- Stable branch: `main`
- Stable runtime baseline: Build 351, commit `2477867278483b76b80ed0884de3a07c7ede668a`
- Integration branch: `dev`
- Integration runtime baseline: Build 377, commit `f64fe0e3992eab4dd62ff479c3765d834ec7dfa4`
- Active architecture work: PR #100, `feat/native-panel-transition -> dev`
- Active stacked feature work: PR #104, `feat/battery-semantic-colors`, based on the current Phase-2A source line
- Active development display line: **0.0.2**
- First planned formal release: **1.0.0**
- Target profile: HyperOS SystemUI `17.03.260226.r`
- Modern Xposed API: 102
- Application ID: `com.chaners.combinedstatus`

`main` and `dev` runtime baselines remain unchanged by documentation-only commits. Work-branch checkpoints are not accepted integration baselines until their required validation and maintainer acceptance are complete.

## Current phase

The project is in **Phase 2A — 0.0.2 Home carrier / presentation architecture**.

The current direction replaces the superseded permanent extra-participant / occupancy-handoff approach with an existing-host Home composition model. Home -> shade / Control Center projection is Phase 2B and must not reopen the steady Home carrier once Phase 2A closes.

Current Home direction on the active work line:

`MiuiNotificationStatusContainer / system_icon_area -> host-scoped overlay -> resolved Home layout -> Combined Status renderer`

SystemUI remains authoritative for native peer layout, Battery scene/hide behavior, tint authority, and island motion. Combined Status owns only its compact composition plus narrowly scoped, reversible Home presentation state that has been explicitly verified.

## Current evidence / checkpoints

- Builds 386-393 are retained as historical evidence for the superseded permanent extra-participant route; they are not the current architecture premise.
- Build 397 is the first device-accepted checkpoint for the tested charging-carrier scenarios on the current Home direction.
- Build 398 strengthens stable width authority by using the live native `battery_icon_container` rather than the charging-expanded Battery root.
- Build 399 separates active and inactive battery-ring arc compositing without reopening Home carrier ownership.
- The active battery-semantic-color branch has advanced to Build 403 (`20260927-403`) on display version `0.0.2`.
- Fast Build #1063 for the current PR #104 head succeeded. Signed-Canary/device acceptance for the latest semantic-color checkpoint is not established here and remains a runtime gate.

## Active boundaries

- Home is the only Combined Status rendering surface currently treated as runtime-verified.
- Notification shade / Control Center, keyguard, and AOD remain native until separately implemented and validated.
- Native HyperOS/SystemUI state and resources should be reused when a verified source exists; project-local state machines or visual substitutions require a real compatibility boundary.
- Native peer geometry, Battery translation/alpha/visibility, and island animation remain SystemUI-owned.
- No per-frame follower, timing retry, magic translation/margin correction, or duplicate layout-occupancy owner should be introduced to repair a scene handoff.
- Unsupported or incomplete integration must fail toward native SystemUI presentation.

## Current blockers / validation

- PR #100 remains an active architecture branch rather than an accepted `dev` baseline.
- PR #104 is stacked on the active Phase-2A source line and must not overwrite newer documentation-governance files when it is synchronized.
- PR #99 (`fix/native-visual-intensity-normalization`) remains separate historical/open work and is not an accepted baseline; any reuse must be reconciled with the current line.
- The latest battery-semantic-color checkpoint still requires the applicable signed-Canary and focused device scenarios before it can be treated as accepted runtime behavior.

## Immediate next step

1. Keep the documentation-governance baseline independent of runtime PRs and synchronize active branches to it without duplicating equivalent commits.
2. Validate the latest PR #104 battery-semantic-color checkpoint against the pinned target, including normal, charging/quick-charging, power-save, performance, low-battery, tint inversion, Hot Reload, and fail-native fallback behavior as applicable.
3. Preserve the accepted Phase-2A Home carrier/spacing behavior while validating the color-only work.
4. After the Phase-2A Home carrier is explicitly closed, begin Phase 2B Home -> shade / Control Center projection without reopening steady Home ownership.

## Reference priority

1. `CONTRIBUTING.md`
2. this `CURRENT.md`
3. `ROADMAP.md`
4. recent/relevant `DEVLOG.md` entries
5. applicable `docs/architecture/` policy
6. applicable `docs/reference/` evidence
7. `VERSIONING.md` for display/release semantics and `RECORDING.md` for documentation maintenance

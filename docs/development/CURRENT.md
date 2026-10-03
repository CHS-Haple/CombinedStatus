# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.5.
- `main` remains on the promoted 0.0.5 / Build 618 stable checkpoint.
- `dev` has advanced to accepted Build 619: `0.0.5` / versionCode `261002419` / Build `20261002-619`.
- Build 619 is the latest accepted runtime-affecting development baseline.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- GPL-3.0-only remains the project license.

## Integrated Build 619 scope

PR #195 added two independent network-state visual controls:
- Airplane mode size: 40%-125%, default 100%.
- No-SIM size: 40%-125%, default 100%.

Both controls are profile-scoped like Wi-Fi/mobile-type sizing, so Network centered and Battery centered remember separate values. Wi-Fi and mobile-type sizing remain independent.

Renderer ownership remains narrow:
- HyperOS/native resources remain the drawable source.
- Each new setting changes only the matching resource draw-size constraint and the same resolved geometry consumed by optical avoidance / transition-source rendering.
- No state source, slot ownership, native peer layout, gesture timeline, timing, alpha, visibility, or translation writer was added.

## Validation evidence

- PR #195 merged to `dev` as `d59b7452cfe7abcad9a48f8ddbf00812adc00da5`.
- Work Branch Canary #650 validated Build 619 on the exact requested work-branch source; trusted checkout/build/signature/non-debuggable checks succeeded.
- Maintainer device validation on Xiaomi 15 Pro accepted the Airplane / No-SIM sizing behavior and independent content-layout memory with no visible regression requiring another runtime change.
- Returned detailed diagnostics report `overall=healthy` on Build 619; the durable device conclusion and the diagnostic-summary limitation are recorded in `DEVLOG.md`.
- Final PR Runtime CI #2230 succeeded after synchronizing latest `dev` ancestry and recording device evidence.
- Trusted `dev` integration CI #2231 succeeded on merge commit `d59b7452cfe7abcad9a48f8ddbf00812adc00da5`, including tests/build, pinned HyperOS target verification, Modern Xposed metadata, Haple APK signature, non-debuggable Canary verification, and Canary artifact upload.

## Active objective

Branch: `feat/battery-fill-retract-follow` / PR #197.

Build 658 is the current focused diagnostic candidate after Build 656 device evidence rejected the participant-translation hypothesis and post-commit review found a more authoritative state-read seam than the first Build-657 probe.

Build 656 device evidence:
- the reported island pull-down defect is unchanged: native icons beyond the island capacity disappear immediately, while the surviving native row then continues its pull motion;
- `reservationMode=native-island-authority` remains active and `nativeReservation=-1`, so Guiyuan transition padding is not driving the cutover;
- at the first captured expansion bucket (about fraction 0.11), QS_FAKE already has `network_speed` / `vpn` in native visibleState 2 with alpha 0 while the final QS row still reports those slots visible;
- QS_FAKE `system_icon_area` / `MiuiStatusIconContainer` remain effectively fixed at the island layout width while the top-level fake root continues native X/Y translation;
- the returned detailed log contains no Build-656 `slotTranslation ... bypass=true` marker, so the corrected Combined-participant Folme path is not established as the writer that performs this early native icon-state cutover.

The remaining causal boundary is therefore inside the QS_FAKE `MiuiStatusIconContainer` island-state / visible-state calculation, before the later fake-root motion can change what the user sees.

Build 658 is observation-only:
- no geometry, alpha, visibility, padding, ignored-slot, translation, timing, island width, or animation state is written;
- the existing bounded transition diagnostic records the fake/final status-row `islandWidth`, `islandWidthChanged`, `ignoredSlots`, plus native panel-expansion flags;
- child island state is read through the project's already-verified `MiuiStatusIconContainer$Companion.access$getViewStateFromChild(View)` seam instead of guessing equivalent fields on the View object;
- the native state object supplies `visibleState`, `inIslandState`, `beforeInIslandState`, `islandChanged`, `supportAnim`, `forceAppear`, and `layoutTranslationX`;
- the probe runs only where the existing detailed transition diagnostic is emitted and is tagged `islandProbe=v2`.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003658` / Build `20261003-658`.
- Runtime behavior is intentionally identical to Build 656.
- Review boundary: reflection is read-only, bounded to existing diagnostic emission, and introduces no new hook/listener/state machine.
- Exact-HEAD Runtime CI is required before Canary.
- Device evidence is mandatory because the next engineering decision depends on whether QS_FAKE receives a terminal `islandWidth` immediately or whether child island state changes independently of that width.

## Device gate

Validate one signed Build-658 Canary:

1. Trigger any island event and keep it active.
2. From Home, perform one slow Control Center pull-down through the point where the native icons disappear / move.
3. Close Control Center and export one detailed diagnostic.

Expected visual result is intentionally unchanged from Build 656. The diagnostic must contain `islandProbe=v2`; the captured fake-row values will decide the next runtime correction.

No Keyguard/AOD validation is required for this checkpoint.

## Immediate next step

Run exact-HEAD Runtime CI for Build 658. If green, request one signed Canary and freeze runtime until the focused island diagnostic returns.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

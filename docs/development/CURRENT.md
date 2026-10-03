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

Build 663 device evidence refines the island failure again. The progress-synchronous compact-to-final reservation is working as designed, but the fake row still applies HyperOS island-hide as a one-dimensional status-layout constraint. At the first captured bucket (fraction about 0.115) the project padding is only 2 px, yet QS_FAKE `network_speed` is already `visibleState=2 / inIslandState=10` while VPN remains visible. As the semantic reservation grows, additional peers disappear one by one even after the moving fake row is visually below the island.

The exact-target reference explains the mismatch:
- `IslandMonitor.RealContainerIslandMonitor.updateContainerSize(...)` derives Home `statusContainerSpace` from the live island rectangle plus the real Home container's screen location.
- `FakeContainerIslandMonitor` consumes that already-reduced horizontal space and writes it into the fake `MiuiStatusIconContainer` as island width/layout state.
- The fake carrier itself then moves in both X and Y under `ControlCenterHeaderExpandController`, but the inherited Home status-layout space is not a true 2D collision test for the translated fake row.

Build 664 is observation-only and keeps Build 663 runtime behavior unchanged. It extends the existing bounded island owner snapshot to locate the native 2D geometry source:
- record X/Y/width/height and translationX/translationY for the existing Home island-owner Views;
- inspect the exact `HomeStatusBarViewBinderInjector.islandController` object graph only inside existing diagnostic buckets;
- report bounded `Rect` / `RectF` / `View` geometry candidates plus relevant island/space/translation/monitor fields and safely-readable `StateFlow.getValue()` values;
- maximum depth 2, maximum 12 objects / 32 entries, no polling or new listener.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003664` / Build `20261003-664`.
- Runtime presentation must remain byte-for-byte equivalent in ownership to Build 663 outside diagnostics.
- No `getIslandShowing()` override, no island-width mutation, no peer state/geometry writer, no carrier-width lease, no new animation/timer/layout request.
- Exact-HEAD Runtime CI is required before one signed Canary.

## Device gate

One active-island slow Home -> Control Center pull and return is sufficient. Detailed diagnostics must contain `homeMotion=...islandGeometry=...`.

Decision:
- if a live island `Rect/RectF` or equivalent View bounds is exposed, Build 665 will gate the fake island constraint by actual 2D overlap while retaining Build 663 semantic progress reservation;
- if only scalar `statusContainerSpace` / translation values exist, do not invent a Y threshold: trace the `RealContainerIslandMonitor.updateContainerSize(...)` Rect input directly next.

No AOD / Keyguard / no-island regression pass is required for Build 664 because it is diagnostic-only.

## Immediate next step

Review Build 664, run exact-HEAD Runtime CI, then one signed Canary and freeze runtime for the single 2D-geometry diagnostic pass.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. Build 663 device video/diagnostic;
4. exact-target `scene-host-motion.md` island-monitor contract;
5. Build 652-663 island DEVLOG history.

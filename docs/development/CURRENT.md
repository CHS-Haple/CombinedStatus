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

Build 624 is the maintainer-accepted device baseline for the battery-ring retract behavior. Its ring/fill path must not be altered while refining charging-glyph handoff.

Build 627 adds an independent charging-glyph transition participant:
- the charging glyph no longer shares the Battery-number participant, so hiding/moving it cannot reflow or reposition the percentage text;
- while the ring retracts, glyph alpha follows the same ring-retained fraction: full above 60%, smooth fade from 60% to 50%, fully hidden at 50%;
- target motion begins only after the glyph is fully hidden;
- when a reliable native target exists, hidden travel follows the native target geometry and `TARGET` scale policy, using the exact `mBatteryChargingView` ImageView/drawable optical geometry like the existing Wi-Fi target path;
- the glyph begins to reappear only near the end of that hidden target travel;
- when no reliable charging target exists, no guessed/fallback target geometry is used: only the source-side fade executes and the glyph stays hidden until native handoff;
- reverse gesture reuses the same progress mapping; no second animator or clock is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003627` / Build `20261003-627`.
- Maintainer device feedback accepts Build 624 ring behavior as the required baseline.
- Unit coverage locks source fade, hidden-before-motion ordering, target-required reappearance, and target completion.
- Percentage layout remains based on the original readout group for the transition frame; glyph alpha changes do not remove its layout slot, preventing percentage jumps.
- Runtime CI for Build 627 is the current automated gate.

## Device gate

Focused charging validation for Build 627:
- charging Control Center transition: ring reaches the glyph visually, glyph fades smoothly and is fully gone around the 50% retained-ring point;
- the percentage number must not jump when the glyph becomes fully transparent;
- after full disappearance, glyph motion must remain invisible until close to the native charging target;
- near the target, the glyph should fade in while converging to the native target position and optical size;
- reverse gesture should remain continuous;
- if target resolution is unavailable, the glyph should only disappear and must not drift toward a guessed location;
- Build-624 ring/fill retract appearance must remain unchanged.

## Immediate next step

Finish Runtime CI and review. If clean, runtime is frozen and a signed Work Branch Canary is required because the new behavior depends on live native charging-target geometry.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

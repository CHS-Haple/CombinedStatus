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

Build 638 is the current visual correction candidate, based on the Build-635 device video plus the matching detailed diagnostic.

Confirmed Build-635 root causes:
- native target tint acquisition is healthy: the pull-down repeatedly resolves `nativeTint=e6ffffff` through `final-battery-tint`;
- non-colorized projected participants were nevertheless forced directly to that final tint instead of preserving their live source tint through the shared tint phase;
- latent additional-mobile reveal clipped against the source four-dot bounds even though exact native target-axis compensation can expand the rendered signal bars beyond those bounds;
- charging source clipping at retained-ring 60% -> 50% begins visibly before the accepted retract path reaches the charging-glyph region.

Build 638 corrections:
- non-colorized participants now follow the same source -> native 35%-65% tint phase instead of jumping to white at transition start;
- the pull-down tint switch still only freezes battery-colorized participants when disabled;
- latent additional-mobile Clip bounds are expanded from the same target-axis compensation used by the exact signal-bar morph;
- charging source Clip moves to retained-ring 26% -> 20% while preserving the accepted ring curve, number-relative follower transform, hidden target travel, 85% target reveal start, exact native target geometry, and no-target fail-native behavior;
- Build-637 tint-decision diagnostics remain available and now verify the corrected behavior rather than being the next experimental step.

No native geometry/alpha/visibility/tint writer, animator, timer, guessed pixel offset, or second transition clock is added.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003638` / Build `20261003-638`.
- Work branch remains based on current `dev` with no behind commits at the correction checkpoint.
- Focused unit coverage now locks:
  - source-preserving native tint handoff for non-colorized participants;
  - switch-OFF source retention for colorized participants;
  - exact-target latent-mobile Clip-envelope expansion;
  - charging source Clip at retained-ring 26% -> 20%.
- Exact-HEAD Runtime CI is required before producing a signed Canary.
- Device evidence is required because Build 638 changes visible transition tint/Clip timing and latent mobile reveal geometry.

## Device gate

After exact-HEAD Runtime CI passes, validate one signed Build-638 Canary with a slow Home -> Control Center pull-down in the same representative scene.

Acceptance:
- icons that are not battery-colorized retain their real Home tint at the start and transition smoothly to the native Control Center tint only through the middle phase; they must not start white;
- the later-appearing additional mobile signal is fully visible throughout its reveal and final bar morph, with no edge or bar clipping;
- the charging glyph stays fully visible until the retracting ring is visually close to its region, then clips away without alpha fade, follows the percentage while source-visible, travels only while hidden, and reveals near the exact native target;
- no regression in ring/fill retract path, native peer reservation, target alignment, or fail-native behavior.

## Immediate next step

Run exact-HEAD Runtime CI for Build 638. If green, request one signed Work Branch Canary and freeze runtime for focused device validation.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

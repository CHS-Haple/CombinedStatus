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

Build 624 remains the accepted main battery-ring/fill retract baseline. Build 629 is device-rejected for charging-glyph handoff semantics:
- “no movement while visible” was interpreted as freezing the glyph in screen/root coordinates, but the intended behavior is to remain fixed **relative to the battery percentage** and move with that number;
- the glyph still remained visible after the ring had passed the 50% retained point;
- target-side reappearance happened too soon after ring completion.

Build 631 corrects the handoff contract while preserving the accepted ring path:
- derive source fade timing from the existing ring policy's real 60% -> 50% retained interval;
- source glyph is fully invisible by 50% retained ring;
- while source alpha is non-zero, target motion is still forbidden, but glyph geometry follows the battery-number participant through the same affine transform so their relative offset/scale stays fixed;
- after source alpha reaches zero, the glyph may travel invisibly to exact native `mBatteryChargingView` target geometry;
- target travel completes before the late reveal phase;
- target reveal begins only near overall handoff progress 92%;
- reveal duration is exactly the same progress span as source fade and uses the same smoothstep easing, giving symmetric fade-out/fade-in speed;
- no reliable target still means source fade only, with no guessed motion or reveal.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003631` / Build `20261003-631`.
- Build 629 device video confirms the incorrect root-coordinate freeze, source visibility beyond the 50% ring point, and target reappearance too close to ring completion.
- Unit coverage locks:
  - ring 60% -> 50% as the source fade window;
  - full source invisibility at the 50% retained point;
  - zero charging-target motion while any source alpha remains;
  - battery-number-relative follower geometry during the visible/fading source phase;
  - equal fade-out/fade-in progress duration;
  - no target-side reveal before the late 92% handoff phase;
  - no-target fail-native behavior.
- The accepted Build-624 main ring/fill curve remains unchanged; the terminal ROUND-cap cleanup from 629 remains intact.

## Device gate

Focused Build-631 charging validation:
- before and during fade-out, charging glyph must move with the percentage number and keep the same relative offset; it must not be screen/root locked;
- glyph should begin fading around retained ring 60% and be completely invisible by 50%;
- after full invisibility it may separate from the number and travel invisibly to the native charging target;
- no target-side glyph should appear immediately after ring completion;
- only near the end of the whole transition should the glyph smoothly fade back in;
- fade-in and fade-out should feel equally fast and use the same smooth character;
- percentage number must not jump;
- target position/size must still converge to exact native charging geometry;
- missing target must produce fade-out only;
- reverse gesture should remain continuous.

## Immediate next step

Finish exact-HEAD Runtime CI and final geometry review for Build 631. If clean, freeze runtime and produce one signed Work Branch Canary for focused device validation.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

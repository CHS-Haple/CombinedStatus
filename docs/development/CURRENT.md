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

Build 624 remains the accepted main battery-ring/fill retract baseline. Build 627 device evidence exposes two refinement issues rather than invalidating that baseline:
- the final very short arc retracts visually too slowly because ROUND stroke caps dominate once arc length approaches one stroke width;
- the charging glyph begins disappearing too early and its exit feels too soft.

Build 629 corrects only those terminal/handoff details:
- the Build-624 ring progress/easing remains unchanged;
- transition ring drawing ends once the retained arc length is no greater than the actual ring stroke width, removing only the ROUND-cap-dominated terminal dot;
- the cutoff is derived from current drawable sweep, ring radius and resolved stroke width, so weight scaling/top-gap geometry remain authoritative;
- charging-glyph fade is moved later to retained ring 26% -> 20% and remains smooth but short;
- target motion begins only after 20% retained ring, completes by 4%, and remains invisible until the final target-reveal phase;
- exact `mBatteryChargingView` drawable optical geometry remains the only target; missing target still means fade-out only;
- charging glyph remains separate from percentage text, so the number cannot reflow when the glyph reaches alpha zero.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003629` / Build `20261003-629`.
- Build 627 device video confirms the terminal slow-looking remnant and that the glyph handoff starts earlier/softer than desired.
- At default 50px ring radius / 8.25px stroke / 240-degree full sweep, the geometry-derived terminal threshold is about 3.94% retained ring; earlier main retract geometry is untouched.
- Unit coverage locks the cap-dominated threshold and the later, shorter charging-glyph fade/hidden-travel sequence.
- Runtime CI passed the source changes before final identity/docs closure.
- Final Runtime CI on exact Build-629 HEAD is required before Canary.

## Device gate

Focused Build-629 charging validation:
- the accepted Build-624 main ring/fill retract pace must remain visually unchanged;
- the former lingering final ring dot should disappear cleanly instead of slowing at the end;
- charging glyph should stay visible noticeably longer, then disappear quickly but smoothly as the ring approaches it;
- glyph must be fully invisible before any target movement;
- percentage text must not jump when the glyph disappears;
- with a reliable target, hidden travel should converge to native position/size and only reappear near the target;
- without a reliable target, the glyph must only disappear and never drift to a guessed point;
- reverse gesture must remain continuous.

## Immediate next step

Finish exact-HEAD Runtime CI and final geometry/ownership review. If clean, freeze runtime and produce one signed Work Branch Canary for focused device evidence.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

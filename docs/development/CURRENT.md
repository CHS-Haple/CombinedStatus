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

Build 635 is the current visual candidate:
- opaque Clip hide/reveal for charging, unmatched exits, and latent reveals;
- charging reveal begins at 85% while retaining the Build-633 full-display completion time;
- selective pull-down reverse-tint transition remains default ON;
- native reverse tint now resolves from final status-icons peer, then final Battery tint, then last valid cached tint.

Build-635 device diagnostics confirm the native tint authority problem from Build 633 is fixed:
- repeated transition samples resolve `nativeTint=e6ffffff`;
- the authority is consistently `final-battery-tint` when status-icons peer tint is unavailable;
- no crash/exception is present in the supplied session.

The remaining uncertainty is purely the participant/color-decision layer: the 635 log did not expose whether the current battery semantic state was classified as colorized, whether the switch was enabled at draw time, or what source -> resolved tint each participant received.

Build 637 is a diagnostic-only follow-up:
- no visual, geometry, alpha, Clip, timing, target, or tint-selection behavior changes;
- transition diagnostics now include:
  - `batteryTinted`;
  - `controlCenterTintTransitionEnabled`;
  - handoff motion progress and tint phase;
  - native target tint;
  - source -> resolved tint for battery ring, number, charging glyph, center, and mobile.
- diagnostic values are evidence-only; no assumed target availability or synthetic visibility values are logged.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003637` / Build `20261003-637`.
- Build 635 Runtime #2361 and signed Canary #668 passed.
- Build-635 supplied trace proves the final Battery fallback provides `e6ffffff` through the active pull-down.
- Build 637 requires exact-HEAD Runtime CI before a diagnostic Canary.

## Device gate

Only needed if the visible reverse-tint behavior is still questionable:
- perform one slow pull-down while a visibly colorized battery state is active;
- export the detailed diagnostic from the same session;
- inspect `tintTransition={...}` at progress buckets around 0.35 / 0.50 / 0.65.

Expected when enabled:
- source colors remain unchanged before tint phase start;
- resolved colors move toward native target through the middle phase;
- resolved colors equal native target after the phase completes.

## Immediate next step

Run exact-HEAD Runtime CI for Build 637. If clean, produce a signed diagnostic Canary only if another device trace is required.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

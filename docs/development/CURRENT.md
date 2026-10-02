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

Build 650 is the current device candidate after Build-638 feedback.

Confirmed Build-638 root causes:
- `FOLLOW_SYSTEM` is correctly classified as non-colorized; only resolved `Custom` semantic sources are treated as battery-colorized;
- the pull-down was sampling `finalStatusIcons`, the fully-expanded QS destination, as native tint authority. That destination is commonly white and is not the native transition carrier visible beside Guiyuan during the gesture;
- Build 638 also attempted to preserve source tint for non-colorized participants, which diverged from the actual native QS_FAKE peer tint path;
- supplemental Airplane / No-SIM reveal resolved real native optical target geometry but used `SHRINK_ONLY`, preventing growth when the native target drawable is larger than the Guiyuan source;
- the Build-638 charging Clip window (retained ring 26% -> 20%) kept the source charging glyph visible too long.

Build 650 correction:
- pull-down native tint now reads the already-applied tint from visible, non-represented native peers in `QS_FAKE / fakeStatusIcons`, the same native transition presentation moving beside Guiyuan;
- non-colorized / FOLLOW_SYSTEM participants directly follow that live native peer tint; custom battery-colorized participants alone use the optional 35%-65% source -> native interpolation;
- final Battery tint is removed as a generic status-icon tint fallback; if a live QS_FAKE peer is temporarily unavailable, only the last valid QS_FAKE peer tint is retained;
- supplemental Airplane and No-SIM use `TARGET` scale with the existing native drawable optical target geometry, matching the Wi-Fi exact-target principle without per-icon scale constants;
- latent additional-mobile Clip remains expanded from exact target-axis compensation;
- source charging-glyph Clip starts when ring retract starts (100% remaining) and completes exactly when 50% remains; while source-visible it keeps following the number, then travels only while hidden;
- charging target reveal now keeps the same 0.85 start but completes within the first 35% of the former 0.85-0.98 reveal window, matching the accelerated latent-resource cadence instead of taking the full late window;
- custom-color pull-down tint no longer uses an independent fixed window: it starts with battery-ring retract and completes exactly when the ring retract completes; the color phase uses its own smoothstep over the shared ring lifetime so it stays visually gentler than the ring's front-loaded shrink. FOLLOW_SYSTEM still directly follows live QS_FAKE native tint.

No new animator, timer, native tint/geometry writer, guessed pixel offset, or second transition clock is added.

Build 648 Runtime failed only because a pre-change unit assertion still expected custom tint to remain fully at source color at progress 0.20. The runtime implementation already followed the approved ring-synced rule; Build 650 updates that test to hold source only at retract start (progress 0) and keeps the runtime behavior unchanged.


Build 650 device-feedback correction:
- Build 649 tied source charging-glyph disappearance to 50% **remaining ring arc**, but the ring retract curve is intentionally front-loaded; device video therefore showed the lightning fully gone before the retract animation itself reached halfway.
- Build 650 instead uses the ring retract **lifetime** as the authority: clipping starts at lifetime 0%, is exactly 50% visible at lifetime 25%, and is fully clipped at lifetime 50%.
- The source glyph remains number-relative while any source clip remains; only after complete clipping can hidden target travel begin.
- The mechanism remains opaque horizontal Clip only (`opacity=1`), not alpha fade or scale.
- Target reveal remains independently fixed at 0.85 -> 0.90.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003650` / Build `20261003-650`.
- Work branch remains based on current `dev` with no behind commits at the latest checkpoint.
- Focused coverage locks:
  - FOLLOW_SYSTEM -> live native target tint and custom-color switch semantics;
  - no Battery fallback in native transition tint selection;
  - latent-mobile target Clip envelope;
  - charging source Clip endpoints at ring remaining 100% and 50%;
  - existing native optical target resolution for single-icon Airplane / No-SIM witnesses.
- Exact-HEAD Runtime CI is required before a signed Canary.
- Device evidence is required for color, Airplane / No-SIM target-size continuity, and charging Clip timing.

## Device gate

After exact-HEAD Runtime CI passes, validate one signed Build-644 Canary:

1. Pull-down tint
   - use a scene where adjacent native status icons visibly change tint during pull-down;
   - all Guiyuan participants configured as FOLLOW_SYSTEM must match those adjacent native QS_FAKE icons throughout the gesture, not default to white;
   - custom battery-linked colors should transition only when the existing pull-down tint switch is enabled.

2. Airplane / No-SIM
   - trigger states where the icon appears only during Control Center expansion;
   - projected icon size must converge continuously to the fully-expanded native icon with no final size jump;
   - compare directly against the already-accepted Wi-Fi size continuity.

3. Charging glyph
   - Clip begins immediately with ring retract;
   - source charging glyph is exactly gone when retained ring reaches 50%;
   - it keeps its relative position to the battery number while source-visible, does not independently fade/shrink, travels only while hidden, and late-reveals at the native target.

4. Regression
   - additional mobile signal remains unclipped;
   - no regression in ring/fill retract, reservation, target alignment, or fail-native behavior.

## Immediate next step

Run exact-HEAD Runtime CI for Build 650. If green, request one signed Canary and freeze #197 runtime for focused device validation.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

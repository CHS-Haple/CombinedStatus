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

Build 624 remains the accepted ring/fill retract baseline.

Build 633 established:
- charging glyph remains number-relative while visible;
- source hide boundary is tied to retained ring 60% -> 50%;
- hidden target travel begins only after complete source hide;
- final reveal started at 88%;
- selective pull-down reverse-tint transition was added behind a default-ON global switch.

Device feedback on Build 633 identified two follow-ups:
1. charging-glyph reappearance still looked too much like an alpha flash;
2. the pull-down reverse-tint transition was effectively not visible.

Build 635 changes the transition language without changing the accepted geometry/path contracts.

### Opaque clip transition

- Charging glyph no longer changes alpha for its semantic hide/reveal.
- Source remains fully opaque and number-relative while a horizontal clip consumes it over the existing retained-ring 60% -> 50% window.
- After complete clipping, independent target travel remains hidden.
- Target reveal now begins at 85% rather than 88%, but the **fully visible completion time remains the same as Build 633**, producing a longer/slower reveal.
- The reveal is an inverse clip, not a fade.

The same opaque visual language now applies to generic transition participants:
- source exists, no target: fast clip-out replaces cubic alpha fade;
- no source, target appears later (second mobile, airplane, no-SIM): existing space-separation and target-distance gates remain, but reveal uses clip-in rather than alpha fade;
- alpha remains reserved for the transition layer/global fail-native opacity, not semantic participant disappearance.

### Reverse-tint correction

Build 633 depended only on the final `statusIcons` peer-tint cache. Device video showed native peers reaching the correct reverse tint while Guiyuan's colorized ring/readout remained colored, consistent with that cache being unavailable on the active path.

Build 635 resolves the target tint from:
1. live final status-icons peer tint;
2. final native battery's current `SystemUiTintStateSource` status/applied tint;
3. last valid cached native tint.

Transparent/invalid candidates are rejected. No black/white tint is guessed.

The existing **下拉反色过渡 / Pull-down tint transition** switch remains default ON:
- ON: only currently colorized participants hold source color through 35%, smoothstep to the native target tint over 35%-65%, then hold target tint;
- OFF: colorized participants keep their source color throughout the pull-down;
- participants already following native tint remain on the native tint path.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003635` / Build `20261003-635`.
- Existing Build-624 ring/fill motion remains untouched.
- Charging exact-target geometry and number-relative follower transform remain unchanged.
- Tests cover:
  - opaque charging clip hide/reveal;
  - 85% reveal start with Build-633 completion time retained;
  - hidden-only target travel;
  - generic unmatched clip-out;
  - clip edge anchoring;
  - latent reveal gating;
  - final-battery tint fallback selection;
  - selective tint switch and 35%-65% color curve.
- The runtime source before final identity/docs closure already passed Runtime CI #2355.

## Device gate

Focused Build-635 validation:
- charging glyph should look physically clipped/ revealed rather than faded;
- final charging reveal should begin slightly earlier but finish at the same time as 633;
- unmatched and latent participants should stay visually opaque while being clipped, with no obvious scale collapse;
- **下拉反色过渡 ON** must visibly transition colorized ring/dots/center/readout/charging participants to the actual native reverse tint;
- switch OFF must preserve their source color;
- no wrong-side wipe, clipping of matched target participants, color jump, guessed tint, or geometry regression.

## Immediate next step

Run final exact-HEAD Runtime CI for Build 635 after docs/version closure. If clean, produce one signed Canary for device validation.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

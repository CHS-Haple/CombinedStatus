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

Build 624 remains the accepted battery-ring/fill retract baseline. Build 629 was device-rejected because the charging glyph was frozen in root coordinates, remained visible beyond the 50% retained-ring point, and reappeared too early. Build 631 corrected the source-side contract by keeping the visible/fading glyph fixed relative to the battery percentage, hiding it fully by 50% retained ring, moving it only while hidden, and revealing it only near the end. Build 632 moves the final reveal slightly earlier from 92% to 88%.

Build 633 adds the requested Control Center color handoff:
- only elements that are **actually colorized** by the current battery semantic color source participate;
- elements already following the system/status-icon tint do not get a second artificial color animation;
- when enabled, colorized participants keep their source color through the first 35% of handoff, transition quickly with smoothstep from 35% -> 65%, then hold the final native status-icon tint through the last 35%;
- the target tint comes from the real final `statusIcons` native peer tint and is refreshed read-only during the transition;
- when no reliable final tint is available, source color is retained rather than guessing black/white;
- the behavior is controlled by the new global **Pull-down tint transition / 下拉反色过渡** switch, default ON;
- when the switch is OFF, colorized participants keep their source color throughout the pull-down; this switch does not alter steady-state color policy.

Colorized participants are selected semantically:
- battery ring whenever the active battery color source resolves to a preset/custom color;
- mobile dots/unavailable mark only when Mobile follows battery color;
- center Wi-Fi/mobile type/airplane/no-SIM/hotspot only when Center follows battery color;
- battery number only when Battery number follows battery color;
- charging glyph only when Charging icon follows battery color.

Charging geometry/alpha contract remains unchanged:
- source glyph fades over retained ring 60% -> 50% and is fully invisible by 50%;
- while visible/fading it follows the percentage-number affine transform;
- independent target motion starts only after source alpha reaches zero;
- target travel remains invisible;
- final target reveal starts at overall handoff progress 88%;
- fade-in reuses the same progress duration and smoothstep as fade-out;
- exact native `mBatteryChargingView` geometry and no-target fail-native behavior remain unchanged.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003633` / Build `20261003-633`.
- Unit coverage locks:
  - 60% -> 50% charging source fade and 50% full invisibility;
  - battery-number-relative charging-glyph geometry;
  - hidden-only target travel and 88% late reveal;
  - equal fade-out/fade-in duration;
  - ARGB transition endpoints and 35% / 50% / 65% middle-only tint curve;
  - actual tinted-state semantics versus FOLLOW_SYSTEM;
  - switch OFF keeps colorized participants at source color;
  - non-colorized participants stay on the native tint path;
  - switch default is true and participates in visual-runtime sync.
- Build-624 main ring/fill curve remains unchanged; terminal ROUND-cap cleanup remains intact.

## Device gate

Focused Build-633 validation:
- charging glyph remains relative to the percentage number before full hide;
- glyph is fully hidden by 50% retained ring;
- glyph does not reappear immediately after ring completion and starts final reveal around 88% handoff;
- fade-in/fade-out speed remains symmetric;
- with **下拉反色过渡 ON**, colorized elements hold their color initially, change mainly through the middle 35%-65%, then remain on system reverse tint;
- with the switch OFF, the same colorized elements remain in their source color during pull-down;
- elements already following system tint should not show a second visible color transition;
- no color jump, guessed black/white, geometry regression, percentage jump, or target mismatch.

## Immediate next step

Run exact-HEAD Runtime CI for Build 633 after final review. If clean, freeze runtime and produce one signed Work Branch Canary for combined charging/tint validation.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

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

Build 655 is the current device candidate, based on Build 654 device evidence.

Build 654 device feedback:
- the native-fallback latch solved ownership contention by removing Guiyuan from the gesture, but that is not an acceptable product behavior;
- the returned Build-654 diagnostic shows the island event was detected and latched before pull-down, then every Control Center transition sample reported `transitionOwner=inactive`;
- this proves the all-native pull-down was the direct consequence of the Build-654 fallback design, not a missed island event or timing race;
- Build 654 therefore remains a diagnostic checkpoint only and is rejected as the runtime solution.

Build 655 correction:
- keep the event-driven island latch, but reinterpret it as a Control Center native-layout-authority mode instead of a Guiyuan/native fallback switch;
- island mode no longer participates in `projectionReady()`; Guiyuan overlay and `CombinedStatusControlCenterTransitionOwner` stay active;
- while island native-layout authority is latched, the QS fake presentation does not add `wifi/mobile/stacked_mobile/airplane/no_sim` to native ignored slots;
- island mode does not write the QS fake `paddingEnd` reservation or fake-carrier capacity lease;
- the represented Wi-Fi/mobile/battery views remain in HyperOS native measure/layout so dual-SIM structure, island collision/knife avoidance, and native target geometry stay authoritative;
- Guiyuan only applies clip visual masks to the represented native views, retaining their measured geometry as transition targets without exposing duplicate icons;
- if an island disappears while Control Center remains visible, native-layout authority stays latched until the panel closes, preventing a second layout-mode switch during one gesture;
- after the panel closes and the island is gone, the existing compact Control Center presentation is prepared again;
- Build-654 Home charging-island `mIsHideBattery -> combined_status` occupancy reconciliation and diagnostics are retained;
- Build-652 charging Clip/tint/target timing and no-island transition behavior remain unchanged.

No timer, polling, delay, replacement island classifier, native peer translation writer, alpha writer, or island geometry patch is added.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003655` / Build `20261003-655`.
- Focused unit coverage now locks:
  - island state latches native-layout authority for the active gesture;
  - island native-layout authority does not block Guiyuan Control Center projection;
  - island Control Center presentation cannot apply persistent ignored slots;
  - island Control Center presentation cannot apply end reservation / fake-carrier capacity ownership;
  - ordinary no-island Control Center still uses the existing compact ownership path.
- Lifecycle review verified mode replacement restores the old session's end reservation, ignored slots, and clip state before the new session starts.
- Exact-HEAD Runtime CI is required before Canary.
- Device evidence is mandatory because this changes QS fake ownership while preserving the existing transition pipeline.

## Device gate

Validate one signed Build-655 Canary:

1. Any active island event
   - pull down Control Center and confirm Guiyuan transition is present again; it must not regress to Build-654 all-native pull-down;
   - non-represented native status icons must keep HyperOS island avoidance/knife-hide behavior instead of being dragged by Guiyuan's fake-row ownership.

2. Charging-only island + dual SIM
   - the expected dual-SIM / dual-signal native structure must remain available throughout layout;
   - left-side native status icons must not overlap the mobile presentation;
   - Guiyuan mobile/Wi-Fi/battery transition should still animate toward the native targets.

3. Island dismissal during an open gesture
   - no second layout jump or compact re-entry during the same gesture;
   - close Control Center, then reopen with no island and verify normal compact/no-island Build-652 behavior returns.

4. Regression
   - ordinary no-island pull-down unchanged from Build 652/653;
   - charging lightning Clip timing, tint, target reveal and target geometry unchanged;
   - Hot Reload first pull and Keyguard/AOD behavior unchanged.

## Immediate next step

Run exact-HEAD Runtime CI for Build 655. If green, request one signed Canary and freeze runtime for the focused island/native-layout device gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

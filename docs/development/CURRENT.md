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

Build 653 is the current device candidate, based directly on accepted Build 652 behavior.

Build 652 device feedback:
- normal Build-652 pull-down/tint/charging behavior showed no new major regression;
- with an island event active, HyperOS native status-icon island avoidance no longer behaved authoritatively;
- with charging-only island active, pull-down could collapse the expected dual-signal presentation to a single mobile presentation and allow left-side native status icons to overlap it.

Root cause confirmed from Build-652 diagnostics and source review:
- the transition session was allowed to expand native `paddingEnd` while a generic island was showing;
- a Guiyuan hook on `MiuiStatusIconContainer.getIslandTranslationX()` then subtracted the same transition padding delta from HyperOS' native island collision boundary;
- Build-652 diagnostics recorded `reservationMode=native-progress-fake-island-projected` with large active deltas while HyperOS' separate `isAddBatteryIsland` callback still reported false. The two island authorities could therefore diverge;
- this violated the native-layout ownership rule: Guiyuan transition width and overlay geometry are project-owned, while native status-icon capacity, dual-SIM layout decisions, and island collision/avoidance must remain HyperOS-owned.

Build 653 correction:
- removes the fake island-boundary hook entirely; Guiyuan no longer intercepts or modifies `getIslandTranslationX()`;
- removes the fake island-width compensation state/bridge and its lifecycle state;
- while `genericIslandShowing == true`, native transition padding expansion is always blocked for both Home and Keyguard;
- if an island appears after a pull-down transition already expanded native padding, the next native panel sample clears Guiyuan's transition reservation immediately;
- ordinary no-island pull-down keeps the existing Build-652 transition reservation behavior;
- transition overlay geometry, battery-ring/charging timing, tint behavior, target geometry, and 0.85 -> 0.90 charging target reveal are unchanged;
- panel runtime hook count is reduced from 5 to 4 because the only native island-boundary writer was removed.

No timer, delay, polling, new geometry patch, island-width writer, alpha patch, or second state machine is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003653` / Build `20261003-653`.
- Work branch is based on current `dev`; exact ahead/behind must be rechecked before Canary.
- Focused unit coverage now locks:
  - generic island => no native padding expansion on Home;
  - generic island => no native padding expansion on Keyguard;
  - ordinary no-island Home/Keyguard can retain transition reservation;
  - panel runtime hook count is 4 and contains no fake island-boundary hook.
- Exact-HEAD Runtime CI is required before a signed Canary.
- Device evidence is required because this changes native layout/ownership behavior during island transitions.

## Device gate

After exact-HEAD Runtime CI passes, validate one signed Build-653 Canary:

1. Charging-only island
   - with dual SIM / dual-signal presentation active, pull down Control Center;
   - the native dual-signal presentation must remain intact instead of collapsing to one mobile presentation;
   - left-side native status icons must not overlap the mobile presentation;
   - native icon hiding/knife-avoidance must follow HyperOS' island boundary.

2. Other island events
   - repeat pull-down with at least one non-charging island event;
   - HyperOS native status-icon island avoidance must work normally throughout the gesture;
   - Guiyuan must not widen native status-icon capacity or defeat native hiding.

3. Mixed charging + another island event
   - if available, verify the same native-authority behavior;
   - lack of an easy mixed-event reproduction is not a blocker if charging-only and another island type both pass.

4. Regression
   - ordinary no-island pull-down remains Build-652 behavior;
   - Build-652 charging lightning Clip/tint/target reveal remains unchanged;
   - no new first-pull, lockscreen/AOD, Hot Reload, or target-alignment regression.

## Immediate next step

Run exact-HEAD Runtime CI for Build 653. If green, request one signed Canary and freeze #197 runtime for the focused island device gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

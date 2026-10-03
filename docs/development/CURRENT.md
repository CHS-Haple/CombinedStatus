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

Build 662 device evidence closes the source/fake timing question. During the first captured active-island bucket (fraction about 0.12), the Home source row has already switched peers such as `network_speed` into terminal island-hide state while the QS_FAKE root is still early in its native motion. The fake row exposes the corresponding terminal horizontal arrangement immediately. Source child state reports native animation support; QS_FAKE child state does not. The visible symptom is therefore not a missing fake-root trajectory: semantic occupancy is already final before the fake carrier finishes moving.

The structural cause is the Build-655 island-native-layout compromise. To preserve a complete native participant set, represented Wi-Fi/mobile/Battery participants were reintroduced into QS_FAKE measure/layout from the first island frame and both Guiyuan end reservation and the fixed carrier lease were disabled. That restores HyperOS collision authority, but it also exposes full final represented width immediately, so surrounding peers are laid out against pull-down-end occupancy at gesture entry.

Build 663 tests the missing isolated combination:
- restore session-scoped native `ignoredSlots` for represented QS_FAKE slots so fake peer layout starts from compact occupancy;
- restore the already-accepted Build-507 semantic end reservation from compact width toward final total width using the same raw HyperOS expansion progress;
- keep the Build-653 removal of the `getIslandTranslationX()` compensation permanent;
- keep the Build-660-rejected fixed fake-carrier capacity lease disabled under island;
- keep all peer `NewStatusIconState`, alpha, visibility and translation writes absent;
- keep final target geometry read-only from the native final QS surface and retain the Build-662 source/fake/final diagnostics.

This combination was not previously isolated. Build 652 combined progress occupancy with a project island-boundary compensation and was rejected. Build 653 removed both the compensation and island progress reservation at the same time. Build 655 then reintroduced full represented measurement. Build 663 keeps only the progress occupancy part while leaving the native collision boundary untouched.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003663` / Build `20261003-663`.
- Required review: no `getIslandTranslationX()` or `getIslandShowing()` return-value hook; no island carrier-width lease; no per-peer geometry/state writer; one existing `statusIcons.paddingEnd` reservation writer only.
- Exact-HEAD Runtime CI is required before one signed Canary.
- Device evidence is mandatory because the candidate intentionally changes island QS_FAKE occupancy ownership.

## Device gate

Validate one active-island slow Home -> Control Center pull and reverse, preferably charging island + dual SIM:

1. At gesture entry, non-represented native peers must no longer jump immediately to their pull-down-end horizontal positions; horizontal avoidance should open with the gesture.
2. HyperOS knife-hide / island collision must remain active. No peer may overlap the island.
3. Charging island + dual SIM must keep the second mobile transition/presentation rather than consuming it at entry.
4. No Build-611/660 full-row left jump or full-width carrier behavior.
5. Recheck one ordinary no-island pull to ensure the accepted 611/612 capacity lease path is unchanged.
6. Export one detailed diagnostic.

Expected diagnostic:
- `reservationMode=native-island-progress-padding`;
- island `fakeCarrierWidth=-1` / `fakeCarrierCapacityDelta=-1`;
- QS_FAKE `ignoredSlots` contains represented slots;
- `nativeReservation` evolves with native progress;
- native island child states remain SystemUI-owned.

No Keyguard/AOD validation is required for this checkpoint.

## Immediate next step

Review Build 663, run exact-HEAD Runtime CI, then one signed Canary. Freeze runtime after Canary for the focused island gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. Build 652-663 island DEVLOG;
5. Build 507 and Build 611/612 occupancy/capacity history.

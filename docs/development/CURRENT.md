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

Build 654 is the current device candidate, based directly on Build 653.

Build 653 device feedback:
- removing the fake island-boundary hook and blocking island-time transition padding did not resolve the visible island regressions;
- generic island state was in fact detected correctly: diagnostics switched to `reservationMode=native-island-authority` and `nativeReservation=-1`;
- despite that, the Control Center fake presentation still retained represented-slot ownership/masks, so dual-SIM could collapse to one visible mobile participant during the gesture and remaining native status icons followed the fake carrier motion;
- charging-island steady avoidance also remained incorrect, so the accepted Build-321 `mIsHideBattery -> combined_status` occupancy handoff must be revalidated at runtime.

Build 654 correction:
- island status becomes an event-driven functional input to the Control Center presentation owner, not only a transition-padding guard;
- once any island becomes active, the current Control Center gesture latches Native fallback: Guiyuan releases the fake presentation ownership and cannot re-enter during the same visible gesture;
- if the island disappears while the panel is still visible, Native fallback remains latched until the panel closes; only then can Guiyuan prearm again;
- the existing HyperOS `mIsHideBattery` field remains the single charging-island layout authority;
- on each island-status event, Guiyuan re-reads that native field and idempotently replays the already-accepted Build-321 slot-occupancy policy to `combined_status`;
- bounded diagnostics now report native battery-hide state, occupancy reconciliation result, combined root width, visual width, and Control Center fallback state;
- Build-652/653 tint, charging Clip timing, target reveal, target geometry, and no-island transition behavior remain unchanged.

No timer, delay, polling, new island classifier, native peer translation writer, or replacement island geometry is added.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003654` / Build `20261003-654`.
- Focused unit coverage locks the island fallback latch:
  - island active immediately blocks Guiyuan Control Center projection;
  - island dismissal cannot re-enter Guiyuan while the same Control Center gesture remains visible;
  - fallback clears only after the panel is no longer visible and the island is gone.
- Charging-island occupancy continues to use the existing `resolveNativeSlotOccupancyWidth` / `setIsHideBattery` authority; Build 654 only reconciles stale/missed state from the authoritative native field.
- Exact-HEAD Runtime CI is required before Canary.
- Device evidence is mandatory because this changes Control Center ownership and revalidates Home charging-island occupancy.

## Device gate

Validate one signed Build-654 Canary:

1. Charging island steady state
   - verify native island avoidance behaves normally before any pull-down;
   - right-side status icons must not jump, overlap, or ignore HyperOS island hiding;
   - if still wrong, export diagnostics and inspect `islandPresentation reconcile` for `nativeBatteryHide`, root width, and visual width.

2. Charging island pull-down
   - dual-SIM must remain native/complete throughout the gesture instead of collapsing to a single mobile participant;
   - native status icons must follow HyperOS' island behavior, with no Guiyuan fake-row mask ownership.

3. Other island events
   - pull down while any non-charging island is active;
   - native status icons must keep HyperOS avoidance/folding behavior; Guiyuan must not drag a partially-masked fake row through the gesture.

4. Regression
   - no-island pull-down remains Build-652/653 behavior;
   - charging lightning/tint/target reveal are unchanged;
   - close/reopen Control Center after island dismissal and confirm Guiyuan prearms again;
   - Hot Reload first pull remains functional.

## Immediate next step

Run exact-HEAD Runtime CI for Build 654. If green, request a signed Canary and keep runtime frozen for the focused island gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

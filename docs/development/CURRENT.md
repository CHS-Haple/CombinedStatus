# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.3.
- `main` and `dev` are synchronized at Build 511 / `20260930-511`.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- Build 510 non-charging Home transition and Build 511 charging Home transition are device-accepted.
- The current transition ownership, reservation, Fail-native, and release-parity boundaries from Build 511 remain protected.

## Active objective

PR #181 / `feat/battery-top-readout` adds an optional battery percentage readout in the top opening of the Guiyuan battery ring while preserving the accepted Home -> Control Center transition contract.

Current checkpoint:
- Build 526 / `20261001-526`;
- branch remains based on current `dev` and is not behind it;
- Build 524 improved the final native Battery-number target for HyperOS hollow-battery presentation;
- Build 525 corrected two Build-523 device defects:
  - positive offset now uses real render-View headroom instead of treating canonical `y=0` as a physical clip edge;
  - charging glyph Y aligns its alpha-weighted visible-ink center to the percentage text optical center;
- Build 525 Runtime CI #1966 passed, including the new physical-headroom and alpha-centroid tests;
- the pending Build-525 Canary request is superseded by Build 526 and must not be used as the integration checkpoint.

Build-523 device evidence also identified an independent Control Center peer-motion regression:
- Super-Island alone is normal;
- charging without that combined condition is not the reported failure;
- the failure is specifically Super-Island + charging, where surrounding native status icons do not follow the expected endpoint rule;
- the same Build-523 diagnostic reports `addBatteryIsland=false / batteryWidthDiff=0` during the affected pull while Guiyuan selected `reservationMode=native-peer-motion`.

Build 526 root-cause correction:
- the old reservation policy used `charging && SystemUiIslandMotionSource.currentIslandShowing()` as a proxy for HyperOS Battery-Island ownership;
- that proxy is too broad: a generic Super-Island can be showing while charging even when `ControlCenterHeaderExpandController.isAddBatteryIsland == false`;
- Build 526 carries the exact native `isAddBatteryIsland` Boolean through the existing Control Center callback/update path;
- Home semantic reservation is enabled only when charging is false or exact `isAddBatteryIsland == false`;
- generic island + charging with exact `isAddBatteryIsland=false`, non-island charging, and island-only keep semantic reservation;
- an unknown Battery-Island read fails native and does not claim semantic reservation;
- no local `batteryWidthDiff`, translation, endpoint, duration, or trajectory compensation is introduced.

## Validation state

Confirmed:
- Build 523 focused device evidence reproduced the battery-top vertical ceiling and charging bolt/number Y mismatch.
- Build 523 device evidence isolates the native-peer endpoint regression to the island + charging combination; island-only behavior is normal.
- The affected diagnostic showed `addBatteryIsland=false / batteryWidthDiff=0` while the old policy had already switched to `native-peer-motion`.
- Build 524 Runtime CI #1961: green.
- Build 525 Runtime CI #1966: green.
- Build 526 static review:
  - exact HyperOS `isAddBatteryIsland` is read from the already-resolved `ControlCenterHeaderExpandController` contract;
  - no new hook count, listener, polling path, timer, animator, native translation writer, or layout writer is added;
  - `statusIcons.paddingEnd` remains the sole Guiyuan peer-layout writer;
  - the generic island callback remains available only for its existing island-owner diagnostics/motion evidence and no longer decides Battery-Island reservation authority;
  - expansion samples clear a stale prior Battery-Island value if the exact native read becomes unavailable;
  - unknown exact authority remains native-peer-motion rather than guessing `false`.

Pending:
- Build 526 Runtime CI.
- if green, one exact-head signed Build-526 Canary.
- focused device validation:
  - island-only pull remains unchanged and reaches the expected final native icon endpoints;
  - charging without an active generic island remains unchanged;
  - island + charging now moves surrounding native icons to the same native endpoint rule indicated by `addBatteryIsland=false / batteryWidthDiff=0` when HyperOS does not activate Battery Island;
  - if HyperOS actually reports `isAddBatteryIsland=true`, native Battery-Island peer motion remains authoritative and Guiyuan does not double-apply semantic reservation;
  - + battery-top offset continues upward through real View headroom;
  - charging lightning visible-ink vertical center matches the percentage visible-text center;
  - Home -> Control Center percentage morph still reaches the native Battery-number target;
  - accepted Build-510/511 Battery-body transition remains unchanged.

## Runtime / rendering contract

- HyperOS remains authoritative for battery state, charging-glyph resource selection, Control Center expansion/motion, and Battery-Island activation.
- `ControlCenterHeaderExpandController.isAddBatteryIsland` is the only Battery-Island reservation-authority signal; generic Super-Island visibility is not equivalent.
- Guiyuan only observes native state and native resources; it does not write HyperOS island translations or `batteryWidthDiff`.
- The existing Guiyuan painter remains the only writer of Guiyuan pixels.
- The existing Battery transition component remains the only Guiyuan owner of battery-component transition rendering.
- `statusIcons.paddingEnd` remains the sole Guiyuan native peer-layout writer.
- Native drawable visual geometry is obtained from one bounded cached probe; envelope geometry and alpha-weighted ink center are read-only measurements from the same probe.
- No polling, delayed state inference, new frame hook, duplicate charge-speed observer, or second gesture animator is introduced.
- Feature default-off preserves the accepted Build-511 visual path until the user enables the readout.

## Non-negotiable boundaries

- Root-cause first; no screenshot-fitted timing, fixed resource Y offsets, or Battery-Island X compensation.
- Preserve accepted Build-510/511 transition behavior unless contradictory device evidence appears.
- One mutable runtime property has one writer.
- Cleanup / Hot Reload restores only Guiyuan-owned state.
- Compatibility uncertainty must not impersonate Battery-Island state.
- HyperOS resources / state / motion are preferred over project-local copies or guesses.
- Do not revive the previously rejected local `batteryWidthDiff` normalization route without new exact-frame evidence that Guiyuan diverges from native QS_FAKE peers.

## Immediate next step

1. finish Build-526 Runtime CI and automated review;
2. if green, freeze runtime at exact Build 526 and request one signed work-branch Canary;
3. validate the island-only / charging-only / island+charging matrix plus the two battery-top geometry corrections;
4. change runtime again only if that device evidence identifies a concrete remaining defect;
5. merge to `dev` only after the combined checkpoint is device-accepted.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. task-specific architecture / reference docs;
4. current source and exact-target SystemUI evidence;
5. relevant `DEVLOG.md` history when needed.

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
- Build 529 / `20261001-529`;
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

Build 527 mobile-type typography correction:
- maintainer video evidence shows the compact Guiyuan `5G` visible ink is still about 1.35–1.40× the final native HyperOS `mobile_type` ink on the pinned target;
- compact mobile-type main text is rebased from 39 to 29 canonical px; suffix size/rise are reduced proportionally so 5G-A/4G suffix composition follows the same scale;
- target position/size convergence remains owned by the existing native `mobile_type_single/mobile_type` witness and transition matrix;
- when that exact native target is a TextView (or exposes one directly), its Typeface weight is read and the Guiyuan text weight interpolates continuously from the compact source weight to the native target weight using the existing HyperOS motion progress;
- if native target typography is unavailable, weight remains unchanged rather than guessing a target;
- no second animator, timing curve, native text writer, or screenshot-specific endpoint is added.

Build 528 latent-reveal timing correction:
- new device feedback confirms no-source / latent participants now wait for real occupancy correctly, but their opacity still reaches 100% too late on fast pulls, leaving a visibly empty target slot until the final part of expansion;
- root cause is the Build-509 rule `min(targetProximity, reservationProgress)`: both terms had to approach 1, so full opacity was mathematically tied to near-terminal target convergence;
- Build 528 preserves both existing safety gates: zero reservation still means invisible, and content outside one real target visual extent remains invisible;
- after both gates open, opacity now completes over the first 35% of the existing spatial reveal windows, using the same stateless smoothstep mapping for occupancy and target proximity;
- no delay, timer, Animator, new gesture curve, target coordinate, or native visibility writer is added;
- reverse collapse stays symmetric: occupancy/proximity falling back through the same window hides latent pixels before the slot fully closes.


Build 529 Battery ring -> native Battery shape-local morph:
- Build 528 Runtime CI #1982 is green and remains the previous rollback checkpoint.
- The old `BATTERY_FOLD` presentation only applied a whole-component Y squash (`1.0 -> 0.72`), which visually produced a flattened ring rather than a ring becoming a battery.
- Build 529 removes that whole-component squash and keeps the existing native `BatteryIcon` witness/motion matrix as position + outer similarity authority.
- The ring itself now morphs locally:
  - the source 240° open ring is sampled as one continuous perimeter parameter;
  - its two lower source ends map to the same final bottom-center point, so they gather inward while the native target height pulls them upward;
  - the source top midpoint maps to the final top midpoint;
  - the final local outline is a rounded battery silhouette whose aspect ratio is derived from the exact native Battery target width/height;
  - local axis compensation only restores the aspect ratio lost by the outer similarity matrix, so final root-space width/height remain native-owned.
- When the top percentage cutout exists, the cutout closes during the first half of the shape morph while the separately-owned Battery-number component leaves for its native target.
- The charging glyph does not receive an independent trajectory; when a native Battery target is available it fades out early, otherwise it preserves the prior Fail-native behavior.
- No native Battery View property, translation, alpha, visibility, or drawable is written by the morph.

## Validation state

Confirmed:
- Build 523 focused device evidence reproduced the battery-top vertical ceiling and charging bolt/number Y mismatch.
- Build 523 device evidence isolates the native-peer endpoint regression to the island + charging combination; island-only behavior is normal.
- The affected diagnostic showed `addBatteryIsland=false / batteryWidthDiff=0` while the old policy had already switched to `native-peer-motion`.
- Build 524 Runtime CI #1961: green.
- Build 525 Runtime CI #1966: green.
- Build 526 exact-head Runtime CI #1975: green; signed Work Branch Canary #583: green at exact SHA `9c833f79fff222b8551485349a0361812f6ba897`.
- Build 526 static review:
  - exact HyperOS `isAddBatteryIsland` is read from the already-resolved `ControlCenterHeaderExpandController` contract;
  - no new hook count, listener, polling path, timer, animator, native translation writer, or layout writer is added;
  - `statusIcons.paddingEnd` remains the sole Guiyuan peer-layout writer;
  - the generic island callback remains available only for its existing island-owner diagnostics/motion evidence and no longer decides Battery-Island reservation authority;
  - expansion samples clear a stale prior Battery-Island value if the exact native read becomes unavailable;
  - unknown exact authority remains native-peer-motion rather than guessing `false`.

Pending:
- Build 529 Runtime CI.
- if green, one exact-head signed Build-529 Canary.
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

1. finish Build-529 Runtime CI and automated review;
2. if green, freeze runtime at exact Build 529 and request one signed work-branch Canary;
3. validate Battery ring shape morph + charging-glyph fade, compact 5G/4G scale + native-target size/weight convergence + quick latent reveal, together with the Build-526 island/charging matrix and battery-top checks;
4. change runtime again only if that device evidence identifies a concrete remaining defect;
5. merge to `dev` only after the combined checkpoint is device-accepted.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. task-specific architecture / reference docs;
4. current source and exact-target SystemUI evidence;
5. relevant `DEVLOG.md` history when needed.

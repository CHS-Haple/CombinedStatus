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

Build 659 is rejected by device evidence: changing only `MiuiStatusIconContainer.getIslandShowing()` for the current QS_FAKE island-native-layout Session produced no visible change. Charging island still consumed the second mobile presentation because the longer island exposed the same early peer-capacity failure more strongly.

Historical evidence now reconnects this regression to the accepted Build 610-612 peer-capacity fix:
- Build 609 proved native QS_FAKE underflow removes `network_speed` while the fake surface is still visually authoritative;
- Build 611/612 fixed that by leasing already-unused leading width on the end-anchored fake carrier, while transition motion continued to use the frozen native logical carrier;
- Build 655 later disabled **both** transition end padding and the fake-carrier capacity lease for island-native-layout mode so HyperOS could recover native island layout authority;
- current source still returns from `syncEndReservation()` before `ensureFakeCarrierCapacityLease()` whenever `nativeLayoutAuthority=true`, so the old peer-capacity protection is absent only in the island path;
- Build 658 simultaneously shows the island QS_FAKE row is narrow and peers are already terminal from the first captured bucket, while the top-level fake root is still moving.

Build 660 therefore separates the two responsibilities that Build 655 disabled together:
- keep island-native-layout `ignoredSlots` disabled;
- keep project-owned island `statusIcons.paddingEnd` disabled;
- restore only the fixed, session-scoped QS_FAKE fake-carrier capacity lease;
- keep Build 612's end-anchored logical source carrier, so the extra leading capacity does not alter Guiyuan motion geometry;
- remove the rejected Build-659 `getIslandShowing()` Hook entirely;
- do not write peer `NewStatusIconState`, visibleState, alpha, visibility, translation, island width, progress or timing.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003660` / Build `20261003-660`.
- Code review must verify that the island exception restores only fixed peer capacity and does not revive Build-653's rejected island-boundary projection or project-owned island end padding.
- Exact-HEAD Runtime CI is required before one signed Canary.
- Device evidence is mandatory because this changes QS_FAKE native measurement capacity under an active island.

## Device gate

Validate one signed Build-660 Canary:

1. With a normal island active, slowly pull Home -> Control Center and return. Network-speed / VPN / neighboring native peers should not settle into the final island arrangement on first movement.
2. Repeat with charging-only island + dual SIM. The second mobile presentation must remain available until HyperOS' native fake-to-final handoff/avoidance actually requires a change; it must not be consumed immediately by the fake row.
3. Confirm the whole Guiyuan transition does not regain Build-611's initial left jump.
4. Confirm island collision/knife-hide still works and native peers do not overlap the island.
5. Recheck one ordinary no-island pull and export one detailed diagnostic.

Expected diagnostic evidence:
- `controlCenterPresentation peerCapacity authority=island-native-layout` appears for the island Session;
- `fakeCarrierCapacity lease=active` is present while island project end-padding remains disabled;
- early QS_FAKE peer state should no longer be forced terminal solely by lack of fake-row capacity.

No Keyguard/AOD validation is required for this checkpoint.

## Immediate next step

Review Build 660 as one bounded ownership split, run exact-HEAD Runtime CI, then request one signed Canary and freeze runtime for the focused island device gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

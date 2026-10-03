# Current Development State

## 2026-10-03 — Build 668: persist QS_FAKE island monitor contract evidence

Build 667 is visually unchanged as intended because it restored the accepted Build-663/664 runtime and only probed the production island-layout seam. The returned detailed diagnostic still proves fake peers can remain island-hidden after vertical separation, but the one-shot `islandContractProbe` event was pushed out of the 600-line runtime-log window by dense transition reservation logs.

Build 668 changes diagnostics only:
- cache the bounded Build-667 QS_FAKE island/monitor/space/delegate/controller contract probe result in the presentation owner;
- append that cached snapshot to the existing bucketed `controlCenterTransitionGeometry` diagnostic as `islandContract=...`;
- perform no additional per-frame reflection: transition buckets only read the cached string;
- no Hook, layout, padding, island-width/state, translation, alpha/visibility, timer, animator or requestLayout behavior changes.

Device gate: one active-island Home -> Control Center pull and one detailed diagnostic. The snapshot must expose whether the real fake-row contract is reachable as a setter/method/delegate around `MiuiStatusIconContainer` / its fake parent. The next functional build will target that exact seam; do not return to `getIslandShowing()`.


## 2026-10-03 — Build 667: trace the real QS_FAKE island-constraint seam

Build 665 is rejected by device evidence. Its fake-only `getIslandShowing()` 2D gate produced no `island2DGate` event and the visual result was unchanged. The detailed log kept `network_speed` / `vpn` at native `inIslandState=10` after the fake row had already moved vertically below the island, while the transition diagnostic reported the gate geometry/state unavailable. This proves the getter seam is not the production input used by QS_FAKE island layout on this target.

Exact-target reference remains authoritative: `FakeContainerIslandMonitor` collects `StatusBarIslandControllerImpl.statusContainerSpace`, feeds the fake `MiuiStatusIconContainer` island-width contract, marks the island-width change and requests native layout. Build 667 restores Build-664/663 runtime behavior and adds only a bounded, read-only contract probe for the live QS_FAKE view/delegate/object graph. It records island/monitor/space/delegate/controller-related fields and methods once when the fake Session starts under detailed diagnostics. The probe is not part of the pull-down hot path and performs no native geometry/state writes.

Device gate: one active-island Home -> Control Center pull and one detailed diagnostic. The next functional correction must target the verified monitor/statusContainerSpace seam; do not revive the rejected getter gate, island-boundary compensation, fixed carrier lease, or per-peer geometry writes.


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

Build 664 closes the remaining dimensionality question. On the pinned target, the native controller exposes the exact live island rectangle through:

`HomeStatusBarViewBinderInjector.islandController -> StatusBarIslandControllerImpl.islandStateHandler.islandRect`.

The returned device log shows `islandRect=Rect(522,31,918,156)`. Near fraction 0.50 the QS_FAKE peer row is already below that rectangle: fake `network_speed` / `vpn` are around screen Y 204 while the island bottom is 156, yet both still report native `inIslandState=10` and alpha 0. HyperOS' fake island constraint therefore remains a one-dimensional status-layout constraint after the translated fake peers are physically clear of the island.

Build 665 applies the smallest functional correction:
- retain Build-663 compact-to-final `statusIcons.paddingEnd` reservation; Guiyuan's split transition still consumes real horizontal volume;
- retain native island behavior while the actual QS_FAKE peer band overlaps the live island rectangle;
- once the fake peer band is 2D-separated from the live island rectangle, expose `getIslandShowing()=false` only to that exact current QS_FAKE `MiuiStatusIconContainer`;
- Home, Keyguard, final QS and unrelated fake rows keep the native return unchanged;
- if the live rectangle cannot be read, fail native and return the HyperOS value unchanged.

Performance boundary:
- Build-664 diagnostic object-graph traversal remains diagnostic-only and is not used by the functional path;
- exact `islandStateHandler` / `islandRect` Fields are discovered once and cached;
- the current fake peer vertical band is sampled read-only after the already-existing native `onLayout` Hook, not by a new listener;
- the `getIslandShowing()` hot path reuses one `Rect` and one `IntArray` per Session and performs cached field reads, one `getLocationOnScreen`, and integer overlap comparisons;
- no timer, poller, new animator, requestLayout, per-peer traversal, per-peer state write or per-frame diagnostic event is added.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003665` / Build `20261003-665`.
- Required review: one fake-only getter seam; fail-native on unavailable rectangle; no `islandWidth`, peer state, alpha, visibility, translation or island-boundary geometry writes.
- Exact-HEAD Runtime CI is required before one signed Canary.
- Device evidence is mandatory because native QS_FAKE island semantics now release on real 2D separation.

## Device gate

One active-island slow Home -> Control Center pull and reverse is the primary test:

1. While the fake peer row still vertically overlaps the island, native island avoidance must remain active and no peer may collide with the island.
2. Once the row is visually below the island, network speed / VPN / other peers must stop disappearing solely because of island projection.
3. The remaining peer movement/hiding, if any, must correspond only to Guiyuan's real compact-to-final horizontal reservation.
4. On reverse pull, island avoidance must re-engage when the fake peer band intersects the island again.
5. Recheck charging island + dual SIM and one ordinary no-island pull.
6. Export one detailed diagnostic.

Expected diagnostic transitions:
- `island2DGate ... overlap=true ... exposedShowing=true` near the overlapping phase;
- later `overlap=false ... exposedShowing=false` after vertical separation;
- reverse should return to `overlap=true`;
- no `fallback=native` on the pinned target.

No AOD / Keyguard validation is required for this checkpoint.

## Immediate next step

Review Build 665, run exact-head Runtime CI, then one signed Canary and freeze runtime for the focused 2D island gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. Build-664 device diagnostic;
4. exact-target `scene-host-motion.md` island-monitor contract;
5. Build 652-664 island DEVLOG history.

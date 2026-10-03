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

Build 659 is the focused island-transition correction candidate produced from Build-658 device evidence.

Build 658 proves the early cutover is inside the QS_FAKE native icon-state calculation:
- at the first captured island pull bucket (fraction about 0.13), the QS_FAKE root is still in native X/Y motion, but `network_speed` is already `visibleState=2`, `inIslandState=10`, alpha 0, and `layoutTranslationX=196`;
- the final QS row keeps the same slot visible with `visibleState=0`, `inIslandState=20`, and `layoutTranslationX=6`;
- the fake state stays terminal through later forward/reverse buckets, while no-island samples keep the fake slot at `visibleState=0 / inIslandState=20`;
- Build-655 island-native-layout mode intentionally stops excluding represented Wi-Fi/mobile/Battery slots so HyperOS can keep their geometry measured, while Guiyuan clips only their pixels.

The resulting mismatch is now bounded: QS_FAKE reintroduces those represented native participants into its own island calculation even though the visible source presentation is still Guiyuan's compact slot. HyperOS then settles non-represented peer icons against that wider fake-row island state before the top-level fake carrier has completed its transition.

Build 659 keeps the Build-655 geometry contract but releases only this stale QS_FAKE island constraint:
- `MiuiStatusIconContainer.getIslandShowing(): boolean` is hooked through the existing presentation owner;
- the native return is changed from `true` to `false` only when the exact status-icon group belongs to the current Control Center fake Session and that Session is in latched island-native-layout mode;
- represented native Wi-Fi/mobile/Battery Views stay measured and laid out, so transition/motion witnesses remain valid;
- native `onMeasure/onLayout` still owns child visible state, island state, positions, and animations; Guiyuan does not write child state, alpha, visibility, translation, island width, padding, or ignored slots;
- Home, Keyguard, final QS, ordinary no-island Control Center, and island detection/latching remain unchanged;
- the functional hook is lifecycle-owned by `SystemUiHomePresentationOwner` and removed with the existing presentation Hook set on runtime reset / Hot Reload.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003659` / Build `20261003-659`.
- Code review must confirm one writer, exact fake-row identity scoping, lifecycle cleanup, and no geometry/timing patch.
- Exact-HEAD Runtime CI is required before one signed Canary.
- Device evidence remains mandatory because the correction changes native QS_FAKE island semantics.

## Device gate

Validate one signed Build-659 Canary:

1. Keep any island event active and perform one slow Home -> Control Center pull, then return.
2. Confirm network-speed / VPN / other native peer icons no longer disappear on the first pull sample and do not overlap the island or Guiyuan transition.
3. Confirm surviving native peers follow the normal Control Center carrier motion rather than an early terminal island arrangement.
4. Recheck charging-only island with dual SIM and one ordinary no-island pull.
5. Export one detailed diagnostic.

Expected diagnostic evidence:
- one bounded `controlCenterPresentation islandConstraint nativeShowing=true exposedShowing=false` event during the island-native-layout Session;
- QS_FAKE peer states should no longer stay terminal `visibleState=2 / inIslandState=10` from the first transition bucket;
- no-island behavior must remain Build-652-equivalent.

No Keyguard/AOD validation is required for this checkpoint.

## Immediate next step

Run exact-HEAD Runtime CI for Build 659. If green, request one signed Canary and freeze runtime for focused device evidence.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.5.
- `main` remains on the promoted 0.0.5 / Build 618 stable checkpoint.
- `dev` is accepted through Build 619: `0.0.5` / versionCode `261002419` / Build `20261002-619`.
- Build 619 is the latest accepted runtime-affecting development baseline.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- GPL-3.0-only remains the project license.

## Active objective

Branch: `feat/aod-display-control` / PR #196.

Build 645 is the current Keyguard/AOD child-ownership correction candidate after Build-643 device feedback.

Build-643 device evidence:
- Keyguard ON + AOD ON: AOD -> Keyguard followed immediately by a fast Control Center pull can begin with no Guiyuan transition; if the gesture pauses at a partial fraction, the transition can jump in later rather than remaining continuous from the first frame.
- Keyguard ON + AOD OFF: Keyguard -> native AOD keeps the Guiyuan Keyguard presentation visibly too long before native AOD takes over.
- Keyguard OFF + AOD ON: disabling the Keyguard child cleans up Keyguard correctly, but the enabled AOD child can immediately attach to the same steady Keyguard host, making the Keyguard switch appear ineffective.

The Build-643 diagnostic proves the third defect is not preference transport failure: after `keyguardRenderFeature enabled=false` and Keyguard presentation cleanup, the module attaches `aodRenderer` from `feature-settings` and activates `aodPresentation` on the same host.

Root cause:
- animating family routing still treated existing presentation ownership as a reason to keep the old child, rather than as evidence of which child is outgoing;
- this allowed an old AOD claim to outlive the verified steady Keyguard boundary and allowed AOD to substitute for a disabled Keyguard child;
- conversely, an owned Keyguard child with AOD disabled was retained during Keyguard -> AOD until the late animation-tail callback.

Build 645 correction:
- existing child ownership is now interpreted as outgoing-child evidence, without using the rejected `toAod/animToAod` direction fields;
- steady Keyguard + Home still owned + AOD enabled remains the only Home -> AOD prearm exception;
- steady Keyguard + outgoing AOD ownership transfers immediately to Keyguard when the Keyguard child is enabled, otherwise Native;
- steady Keyguard + outgoing Keyguard ownership transfers immediately to AOD when the AOD child is enabled, otherwise Native;
- steady Keyguard with no family child yet acquires Keyguard immediately when enabled, covering native-AOD -> Keyguard without waiting for the animation tail;
- UNKNOWN scene preserves only an already-owned enabled child and otherwise fails native.

The family RenderSession remains single-writer and is retargeted in-place; no timer, delay, polling, native geometry writer, or transition-direction guess is added.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003645` / Build `20261003-645`.
- Work branch is based on current `dev` with no behind commits.
- Focused ScenePolicy coverage locks:
  - dual-enabled outgoing Keyguard -> AOD and outgoing AOD -> Keyguard ownership routing;
  - Keyguard-only native-AOD -> Keyguard early acquisition;
  - Keyguard-only Keyguard -> AOD immediate native release;
  - AOD-only cannot replace a disabled Keyguard child on steady Keyguard;
  - Home-owned AOD prearm remains valid across the transient Keyguard source.
- Exact-HEAD Runtime CI is required before a signed Canary.
- Device evidence is required because this changes visible family cutover timing and the Keyguard Control Center source boundary.

## Device gate

After exact-HEAD Runtime passes, validate one signed Build-645 Canary:

1. Keyguard ON / AOD ON
   - enter AOD, wake to Keyguard and immediately pull Control Center quickly;
   - Guiyuan transition must exist from the first non-zero fraction and stay continuous if the gesture pauses halfway;
   - no late jump from native/no-transition into the middle of the Guiyuan transition.

2. Keyguard ON / AOD OFF
   - Keyguard -> AOD must release Guiyuan at the outgoing-Keyguard AOD-animation boundary rather than lingering until the animation tail;
   - AOD -> Keyguard must still acquire Guiyuan promptly with no delayed entrance.

3. Keyguard OFF / AOD ON
   - steady Keyguard must remain native;
   - AOD Guiyuan must appear only for real AOD / valid Home -> AOD prearm, never as a substitute Keyguard presentation.

4. Dual-enabled Keyguard <-> AOD steady continuity and ordinary Home pull-down remain regression checks.

## Immediate next step

Run exact-HEAD Runtime CI for Build 645. If green, request one signed Canary and freeze #196 runtime for this three-part device gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

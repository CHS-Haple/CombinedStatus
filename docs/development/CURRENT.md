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

Build 655 is the current Keyguard/AOD timing candidate after Build-654 device validation.

Build-654 result:
- the large Build-653 regressions are closed: steady Keyguard is restored and the previous unlock/immediate-Control-Center failure is no longer the reported blocker;
- dual-enabled Home -> AOD can still show one brief screen/status flash, but the tester considers it acceptable for this gate;
- Keyguard OFF / AOD ON: AOD -> Keyguard releases Guiyuan about half a beat late;
- Keyguard ON / AOD OFF: AOD -> Keyguard acquires Guiyuan about half a beat late, while Keyguard -> AOD releases Guiyuan about half a beat early.

The 654 diagnostic explains the directional mismatch:
- Keyguard -> AOD cleanup occurs at the first `setIsAodAnimate(true)` callback, before the later native AOD-mode transition callback, so `isAodAnimate` is too early to be the single-child cutover authority;
- AOD -> Keyguard does not reacquire until the late `setIsAodAnimate(false)`/stable-family edge, so animation completion is too late;
- once that late stable edge arrives, renderer attach and native-layout cutover complete within only a few milliseconds. The delay is therefore event authority, not layout/readiness cost.

Build-655 candidate:
- keep Build 654's QS_FAKE visible-cycle capacity-lease correction unchanged;
- keep Build 654's structural steady-scene authority and dual-enabled Keyguard/AOD family ownership unchanged;
- add one read-only hook on exact-target `KeyguardStatusBarViewControllerInject.animateFullAod(boolean, boolean)`;
- after that native callback returns, re-evaluate the existing family owner from `MiuiKeyguardStatusBarView.mToLockScreen`, which is a persistent native target witness rather than an animation-progress clock;
- use that target only when exactly one of Keyguard/AOD is enabled and a stable Keyguard-family origin already exists;
- preserve the 654 status-icons alpha logic as fallback if the new target source is unavailable;
- preserve Home -> AOD UNKNOWN-origin prearm priority and all dual-enabled routing.

No timer, delay, polling, copied native duration/interpolator, native alpha/visibility/translation writer, or geometry patch is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003655` / Build `20261003-655`.
- PR #196 is 0 behind `dev` before Build-655 authoring.
- Build 655 source/tests/docs passed final diff review against the Build-654 head; the branch is ready for exact-HEAD Runtime validation.
- Focused tests cover the exact `animateFullAod(Boolean, Boolean)` signature and all four single-child target outcomes; tests also preserve UNKNOWN-origin Home -> AOD prearm and dual-enabled family routing.
- Exact-HEAD Runtime CI is required before Canary.
- Real-device validation is mandatory because the new source is an exact-target native lifecycle event.

## Device gate

1. Keyguard OFF / AOD ON
   - AOD -> Keyguard should yield to native Keyguard at the native full-AOD target switch, removing the current half-beat late handoff;
   - Keyguard -> AOD should acquire AOD Guiyuan at the corresponding native target switch;
   - stable Keyguard remains native and stable AOD remains Guiyuan.

2. Keyguard ON / AOD OFF
   - AOD -> Keyguard should acquire Guiyuan at the native target switch rather than animation end;
   - Keyguard -> AOD should retain Guiyuan until the native full-AOD target switch instead of dropping at the earlier `setIsAodAnimate(true)` edge;
   - stable Keyguard remains Guiyuan and stable AOD remains native.

3. Both ON
   - Keyguard <-> AOD remains one continuous family owner/RenderView;
   - Home -> AOD may retain the already-accepted brief native screen flash, but Build 655 must not add a second represented-icon flash.

4. Regression
   - AOD/Keyguard -> Home immediate Control Center pull;
   - repeated Home Control Center open -> close -> open;
   - no `fake-carrier-width-writer-conflict`;
   - Hot Reload first pull;
   - no attach -> cleanup -> attach oscillation around one `animateFullAod` event.

## Immediate next step

Fast-forward the reviewed Build 655 onto #196, run exact-HEAD Runtime, then one signed Canary for the directional timing gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. `SystemUI-Reference` exact-target findings and task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

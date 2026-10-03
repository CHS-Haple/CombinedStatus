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

Build 656 is the current Keyguard/AOD timing candidate after Build-655 device validation.

Build-654 result:
- the large Build-653 regressions are closed: steady Keyguard is restored and the previous unlock/immediate-Control-Center failure is no longer the reported blocker;
- dual-enabled Home -> AOD can still show one brief screen/status flash, but the tester considers it acceptable for this gate;
- Keyguard OFF / AOD ON: AOD -> Keyguard releases Guiyuan about half a beat late;
- Keyguard ON / AOD OFF: AOD -> Keyguard acquires Guiyuan about half a beat late, while Keyguard -> AOD releases Guiyuan about half a beat early.

The 654 diagnostic explains the directional mismatch:
- Keyguard -> AOD cleanup occurs at the first `setIsAodAnimate(true)` callback, before the later native AOD-mode transition callback, so `isAodAnimate` is too early to be the single-child cutover authority;
- AOD -> Keyguard does not reacquire until the late `setIsAodAnimate(false)`/stable-family edge, so animation completion is too late;
- once that late stable edge arrives, renderer attach and native-layout cutover complete within only a few milliseconds. The delay is therefore event authority, not layout/readiness cost.

Build-655 device result:
- the native full-AOD target removed the previous late handoff, but both tested single-child directions now cut about half a beat early;
- therefore `mToLockScreen` is correct direction evidence but too early to serve as the visual cutover itself.

Build-656 candidate:
- keep Build 654's QS_FAKE visible-cycle capacity-lease correction unchanged;
- keep structural steady-scene authority and dual-enabled Keyguard/AOD family ownership unchanged;
- keep `animateFullAod` / `mToLockScreen` as read-only direction evidence only;
- add one read-only hook on exact-target `MiuiKeyguardStatusBarView.animateIconContainer(boolean)`; its Boolean remains diagnostic-only;
- entering the native full-AOD call opens a bounded pending transition scope before HyperOS runs, so an `animateIconContainer` callback occurring inside that call cannot be missed;
- while that scope is pending, retain the enabled outgoing child (or Native when that outgoing child is disabled); only the observed native status-icon animation event may consume the committed `mToLockScreen` target;
- if the status-icon animation source is unavailable, fall back to the Build-654 status-icons-alpha path rather than inventing timing.

No timer, delay, polling, copied native duration/interpolator, native alpha/visibility/translation writer, or geometry patch is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003656` / Build `20261003-656`.
- PR #196 is 0 behind `dev` before Build-655 authoring.
- Build 656 source/tests/docs passed final diff review against the Build-655 head; the branch is ready for exact-HEAD Runtime validation.
- Focused tests cover the exact `animateFullAod(Boolean, Boolean)` and `animateIconContainer(Boolean)` contracts, pending-outgoing retention, native visual-boundary cutover, UNKNOWN-origin Home -> AOD prearm, and dual-enabled family routing.
- Exact-HEAD Runtime CI is required before Canary.
- Real-device validation is mandatory because the new source is an exact-target native lifecycle event.

## Device gate

1. Keyguard OFF / AOD ON
   - AOD -> Keyguard should yield to native Keyguard at the native status-icon animation boundary, removing the current half-beat late handoff;
   - Keyguard -> AOD should acquire AOD Guiyuan at the corresponding native status-icon animation boundary;
   - stable Keyguard remains native and stable AOD remains Guiyuan.

2. Keyguard ON / AOD OFF
   - AOD -> Keyguard should acquire Guiyuan at the native status-icon animation boundary rather than animation end;
   - Keyguard -> AOD should retain Guiyuan until the native status-icon animation boundary instead of dropping at the earlier full-AOD target edge;
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

Fast-forward the reviewed Build 656 onto #196, run exact-HEAD Runtime, then one signed Canary for the directional timing gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. `SystemUI-Reference` exact-target findings and task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

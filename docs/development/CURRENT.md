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

Build 657 is the current Keyguard/AOD lifecycle candidate after Build-656 device validation.

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

Build-656 device result:
- Keyguard -> AOD timing is now accepted in the tested single-child path;
- AOD -> Keyguard became late because the pending target lease is cleared immediately after `animateFullAod` returns while native `animateIconContainer(true)` arrives later; the later visual event is therefore ignored and acquisition/release falls back to the final stable-family edge;
- Home -> AOD is also late, and with both family children enabled the composed indicator can visibly disappear and then return;
- device logs show the Home origin can still report `UNLOCKED_STATUS_BAR` when the full-AOD target sequence begins, while the status-icon callback may occur before `isAodAnimate=true`. Build 656 therefore waits too long to prepare the AOD owner.

Build-657 candidate:
- preserve the accepted Keyguard -> AOD `animateIconContainer` cutover;
- do not clear the single-child full-AOD pending lease merely because the post-`animateFullAod` snapshot still reports `isAodAnimate=false`; retain it until the native status-icon event consumes it or a later non-animating AOD state closes the transition;
- add one bounded Home -> AOD target-prearm lease. It may arm only when native target is AOD, the observed origin is steady Home / UNKNOWN family, AOD projection is enabled, and Home still owns represented slots at arm time;
- once armed, the AOD owner may remain prepared across transient Keyguard ancestry until native AOD state catches up, so the outgoing Home visual does not disappear before the incoming AOD host is ready;
- use the existing AOD pre-mask / compact-layout cutover contract; no new visual writer is introduced.

No timer, delay, polling, copied native duration/interpolator, native alpha/visibility/translation writer, or geometry patch is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003657` / Build `20261003-657`.
- PR #196 is 0 behind `dev` before Build-655 authoring.
- Build 657 source/tests/docs are staged off-branch for final diff review against the Build-656 head.
- Focused tests preserve the accepted native status-icon cutover and add native-target Home -> AOD prearm eligibility / bounded-latch coverage.
- Exact-HEAD Runtime CI is required before Canary.
- Real-device validation is mandatory because the new source is an exact-target native lifecycle event.

## Device gate

1. Keyguard OFF / AOD ON
   - AOD -> Keyguard must consume the later native `animateIconContainer(true)` boundary instead of falling through to stable-family completion;
   - Keyguard -> AOD must remain at the Build-656 accepted timing;
   - Home -> AOD should prepare AOD before the outgoing Home visual disappears; stable Keyguard remains native and stable AOD remains Guiyuan.

2. Keyguard ON / AOD OFF
   - AOD -> Keyguard must acquire Guiyuan at the later native status-icon event rather than the final stable edge;
   - Keyguard -> AOD must remain unchanged from the Build-656 accepted timing;
   - stable Keyguard remains Guiyuan and stable AOD remains native.

3. Both ON
   - Keyguard <-> AOD remains one continuous family owner/RenderView;
   - Home -> AOD must not show a new represented-icon disappearance/reappearance interval; the previously accepted native screen flash is evaluated separately.

4. Regression
   - AOD/Keyguard -> Home immediate Control Center pull;
   - repeated Home Control Center open -> close -> open;
   - no `fake-carrier-width-writer-conflict`;
   - Hot Reload first pull;
   - no attach -> cleanup -> attach oscillation around one `animateFullAod` event.

## Immediate next step

Review Build 657 against Build 656, then fast-forward #196, run exact-HEAD Runtime, and issue one signed Canary for the focused lifecycle gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. `SystemUI-Reference` exact-target findings and task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

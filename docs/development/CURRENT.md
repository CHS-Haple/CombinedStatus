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

Build 660 is the current Keyguard/AOD lifecycle candidate after Build-657 device validation.

Build-657 device result:
- Keyguard OFF / AOD ON and both ON: Home -> AOD still loses Guiyuan briefly and then reacquires it;
- Keyguard ON / AOD OFF: Home -> AOD loses Guiyuan, briefly reacquires Guiyuan on transient Keyguard ancestry, then correctly settles to native AOD; AOD -> Keyguard still acquires Guiyuan late;
- the 657 diagnostic contains no successful `aod.homePrearm` event;
- `animateIconContainer(false)` can run inside `animateFullAod` before the post-call `mToLockScreen=AOD` snapshot is processed. By the time 657 re-checks Home origin, mutable scene/ownership evidence may already have changed;
- the reverse pending lease also records only a Boolean lifetime, so a stale non-animating state cannot be distinguished from the pending target's actual stable endpoint.

Build-660 candidate:
- latch the verified Home/UNKNOWN origin and Home represented-slot ownership at `animateFullAod` entry, before HyperOS mutates scene ancestry; the latch is origin evidence only and does not infer direction;
- keep native `mToLockScreen` as direction authority and `animateIconContainer` as the visual boundary;
- when the native target confirms AOD, the latched Home origin may arm the existing AOD pre-mask/compact owner even if current scene ancestry or Home ownership has already moved;
- when AOD projection is disabled, the same latched Home origin forces Native instead of allowing transient Keyguard ancestry to momentarily acquire Guiyuan;
- store the pending native target explicitly and close its lease only when a later non-animating state reaches that same endpoint. An old AOD state can no longer cancel a pending AOD -> Keyguard visual-boundary handoff.

No timer, delay, polling, copied native duration/interpolator, native alpha/visibility/translation writer, or geometry patch is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003660` / Build `20261003-660`.
- PR #196 is 0 behind `dev` before Build-655 authoring.
- Build 660 source/tests/docs encode the Build-657 device evidence: entry-time Home origin latching plus target-matched pending closure.
- Focused tests cover lost current Home ownership, AOD-disabled transient-Keyguard suppression, and target-matched stable-endpoint closure.
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

Review Build 660 against Build 657, fast-forward #196, run exact-HEAD Runtime, then issue one signed Canary for the same three-mode lifecycle gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. `SystemUI-Reference` exact-target findings and task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

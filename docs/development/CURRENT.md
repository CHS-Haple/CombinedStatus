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

Build 664 is the focused follow-up to Build-663 device evidence.

Build-663 device result:
- Keyguard ON / AOD OFF no longer performs the Build-662 early ignored-slot/end-reservation takeover. The diagnostic proves visual-only handoff writes zero native layout geometry, and ignoredSlots/end reservation are committed only at stable Keyguard.
- AOD -> Keyguard still exposes native represented icons at the beginning of the transition. Video shows the native row appearing left of the compact endpoint before Guiyuan takes over.
- The exact ordering explains the gap: `animateFullAod:after` already reports native target Keyguard at 11:10:22.014, but the visual-only lease is not armed until `animateIconContainer(true)` at 11:10:22.304, about 290 ms later.
- Once armed, the 663 visual-only lease itself is structurally correct: clip masking / Guiyuan renderer become active with `ignoredSlotsWrites=0 paddingWrites=0`; stable Keyguard at 11:10:22.700 then commits the deferred layout and completes native layout at 11:10:22.705.

Build-664 correction:
- keep the Build-663 two-phase ownership split unchanged;
- prearm the same visual-only Keyguard lease as soon as `animateFullAod` has committed authoritative `mToLockScreen=true` from a stable AOD origin;
- allow that explicitly armed visual lease to project Keyguard even in the short interval before `setIsAodAnimate(true)` reaches the battery state source;
- when `animateIconContainer(true)` later arrives, it observes the already-active lease instead of recreating it;
- ignored slots, end reservation, compact layout ownership and stable-cutover timing remain deferred exactly as in Build 663.

No timer, delay, polling, custom duration/interpolator, native alpha/visibility/translation writer, or geometry compensation is introduced. The only earlier action is the existing reversible clip mask / Guiyuan renderer lease, driven by the already-authoritative native target commit.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003664` / Build `20261003-664`.
- PR #196 is 0 behind `dev` before Build-664 authoring.
- Unit coverage proves the explicitly armed visual lease may override still-stable AOD only for the incoming enabled-Keyguard target, while the same stable AOD state remains native without that lease.
- Runtime CI and one signed Canary are required before focused device validation.

## Device gate

1. Keyguard ON / AOD OFF — primary
   - AOD -> Keyguard must not show the raw native row at the left/start position before Guiyuan appears.
   - Guiyuan should already be prepared when the native Keyguard carrier becomes visible.
   - No ignored-slot/padding/layout jump may occur before stable Keyguard.
   - Keyguard -> AOD remains unchanged.

2. Keyguard OFF / AOD ON — regression
   - preserve the accepted Build-660 behavior.

3. Both ON / Home -> AOD
   - remains intentionally outside this checkpoint; the existing flash is still open.

## Immediate next step

Review Build 664 for writer/lifecycle isolation, run exact-HEAD Runtime, then issue one signed Canary if automated validation is clean.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. `SystemUI-Reference` exact-target findings and task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

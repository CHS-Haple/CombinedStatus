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

Build 665 addresses two Build-664 device findings without changing the accepted Keyguard -> AOD direction.

Build-664 device evidence:
- AOD -> Keyguard now keeps Guiyuan present from the beginning, proving the earlier target-commit visual prearm is useful.
- The remaining motion defect is peer layout: at 11:30:58.147 the visual-only lease masks represented native views with zero native layout writes, while persistent ignored slots / end reservation are not committed until stable Keyguard at 11:30:58.846. Non-represented native peers therefore begin from the full-row layout and only collapse beside Guiyuan at the stable edge, producing the visible "merge in from the middle" motion.
- The native Keyguard host is still hidden when the authoritative Keyguard target is committed, leaving a native lifecycle window in which final compact Keyguard occupancy can be prepared before the host is revealed.
- Home -> AOD with AOD disabled has a separate transient-owner bug: the full-AOD entry reports Home presentation ownership, but a transient KEYGUARD scene attaches Guiyuan before the native target is known. Once `mToLockScreen=false` confirms native AOD, that temporary Keyguard owner survives until a later AOD callback, so the screen flashes, shows Guiyuan again, and only then returns to native Battery.

Build-665 correction:
- for stable-AOD -> enabled-Keyguard / disabled-AOD only, if the incoming Keyguard host is still not shown when `mToLockScreen=true` commits, precommit the existing native ignored-slot/end-reservation contract while the host is hidden;
- keep the Guiyuan renderer hidden until the native `animateIconContainer(true)` boundary; at that boundary reveal only after compact layout is ready;
- if the host is already shown, retain the Build-664 deferred-layout behavior rather than mutating a visible row;
- for Home-origin full-AOD transitions with AOD disabled, latch Home ownership at `animateFullAod:before`; when `mToLockScreen=false` commits with no stable Keyguard/AOD family, release only the transient Keyguard owner immediately;
- Keyguard -> AOD is not changed.

No timer, copied animation duration, custom interpolator, peer translation, alpha/visibility writer, or geometry compensation is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003665` / Build `20261003-665`.
- PR #196 is 0 behind `dev` before authoring.
- Unit coverage constrains hidden-host prelayout to the same stable-AOD -> single enabled Keyguard path and constrains transient Keyguard release to Home-owned / family-UNKNOWN / native-AOD target.
- Exact-head Runtime CI and one signed Canary are required.

## Device gate

1. Keyguard ON / AOD OFF — AOD -> Keyguard:
   - Guiyuan remains continuous.
   - adjacent native icons must no longer collapse inward from the middle at the end; their relative row layout should already be compact when the native Keyguard reveal begins.
   - no visible layout jump is allowed at stable Keyguard.

2. Keyguard ON / AOD OFF — Home/Desktop -> AOD:
   - the native/system flash itself may remain;
   - after the flash, a transient Keyguard Guiyuan must not linger before native AOD Battery appears.

3. Keyguard -> AOD:
   - regression only; preserve Build-664 behavior reported as normal.

## Immediate next step

Run post-commit ownership review and exact-head Runtime. If clean, issue one signed Canary and freeze runtime for this focused gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. `SystemUI-Reference` exact-target findings and task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

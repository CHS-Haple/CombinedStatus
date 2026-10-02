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

Build 626 is device-rejected. The callback-order correction was necessary but insufficient: detailed device evidence shows both family roles can still be released during one native AOD transition because RenderSession/readiness code independently re-derives eligibility from raw AOD state after ScenePolicy has already selected the continuous family projection.

Build 628 removes those duplicate eligibility writers:
- `CombinedStatusScenePolicy.resolveKeyguardAodProjection()` is the sole KEYGUARD / AOD / NATIVE projection authority;
- RenderSession receives the selected scene eligibility from Module and no longer derives it again from `toAod`, `isAodAnimate` or `blocksProjection`;
- raw AOD updates may refresh inherited visibility/alpha diagnostics but cannot change presentation readiness ownership;
- Keyguard and AOD readiness/cutover validate against the same current ScenePolicy projection instead of applying a second stable-AOD rule;
- same-host family retarget, role-specific cleanup guards, one presentation Session and one RenderView remain unchanged;
- no timer, delay, polling, direction guess or duplicate native geometry/visibility writer is added.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003628` / Build `20261003-628`.
- Build 626 device video still shows temporary native battery/status presentation during both directions of AOD switching.
- Build-626 diagnostics prove two remaining cleanup paths:
  - Keyguard scene-transfer becomes ineligible during `isAodAnimate=true`, then `readiness-lost:aod:setIsAodAnimate` restores ignored slots, clip masks and reservation;
  - an already-active AOD presentation is later cleaned with `aod-not-eligible` when the raw animation state changes.
- The Build-626 “retarget before readiness update” hypothesis is therefore rejected as a complete fix; the surviving root cause is duplicated eligibility authority.
- Runtime CI passed the Build-628 source changes before the final identity/docs bump.
- Final Runtime CI on the exact Build-628 HEAD is required before Canary.

## Device gate

Validate Build 628 with emphasis on:
- repeated Home -> AOD -> Home switching: no temporary native battery/network set, blank interval or duplicate set;
- repeated Keyguard -> AOD -> Keyguard switching with both child switches enabled: no native represented-icon flash and no cleanup/reacquire gap;
- AOD-only and Keyguard-only child settings still fall native for the disabled scene;
- global Guiyuan off still releases family ownership immediately;
- no stuck outgoing frame, stale AOD alpha on Keyguard or AOD leakage into Control Center.

## Immediate next step

Finish exact-HEAD Runtime CI and final ownership review. If clean, freeze runtime and produce one signed Work Branch Canary for focused device evidence.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

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

Build 628 fixed the original native represented-icon flash by making ScenePolicy the single Keyguard/AOD family eligibility authority. Build 630 then removed the invalid Battery-alpha -> whole-family-alpha copy, and device validation confirms Keyguard <-> AOD is now visually normal.

Build 634 was used as a diagnostic checkpoint for the remaining Home -> AOD single flash. The returned device trace shows:
- Home -> AOD traverses `HOME -> KEYGUARD -> AOD`;
- the Keyguard host is legitimately not yet shown during the early transfer window, while `system_icons` itself remains alpha 1;
- by the native AOD transition callbacks the full Keyguard host/content/system-icons ancestry is visible with effective alpha 1;
- there is no Keyguard/AOD renderer detach, no family presentation cleanup/inactive, no host replacement, no fail-native path, and no native-suppression release/reacquire in the captured session;
- the maintainer also confirmed that stock HyperOS exhibits a corresponding Home -> AOD visual flash because the status-bar style changes across that boundary.

Therefore the Home -> AOD flash is no longer treated as a Guiyuan defect merely because it exists. The remaining engineering question is whether Guiyuan adds any lifecycle churn on top of the native transition.

One lifecycle inefficiency was found:
- repeated resolver/AOD callbacks targeting the **same family scene** reused the same RenderSession correctly, but `retarget()` always dispatched presentation readiness with `force=true`;
- this did not recreate the RenderView or duplicate listeners, but it re-ran presentation cutover, reservation sync, and clip-mask refresh unnecessarily;
- true KEYGUARD <-> AOD retargets still require a forced readiness dispatch because the presentation role changes while readiness may remain true.

Build 636 removes that same-scene churn:
- same-scene retargets only dispatch when readiness actually changes;
- actual KEYGUARD <-> AOD scene changes still force presentation handoff;
- no alpha, visibility, timing, geometry, delay, bridge overlay, or native motion behavior is changed.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003636` / Build `20261003-636`.
- Build 634 device diagnostics establish that the early Home -> Keyguard host invisibility is part of the native cross-host lifecycle and not evidence of a Guiyuan detach/recreate loop.
- Full log audit found no family presentation cleanup/inactive, renderer detach, host replacement, fail-native, or suppression-release event during the captured Home -> AOD transitions.
- Keyguard/AOD render ownership remains one family Session / one RenderView with same-host retarget.
- Build 636 only deduplicates redundant same-scene readiness/cutover work.
- Unit coverage locks that readiness is forced for scene changes and not forced for same-scene retargets.

## Device gate

Build 636 does **not** target zero visual flash. Validate instead that:
- Home -> AOD looks no worse than Build 634 / stock HyperOS behavior;
- there is no extra second flash, represented-icon leak, duplicate Guiyuan frame, or prolonged blank interval;
- Keyguard <-> AOD remains normal;
- detailed diagnostics no longer show repeated same-scene presentation cutover churn for every AOD callback.

## Immediate next step

Run exact-HEAD Runtime CI for Build 636. If clean, produce one signed Canary for lifecycle regression validation; do not add a cross-host bridge unless future evidence shows a Guiyuan-specific extra artifact beyond the native transition.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

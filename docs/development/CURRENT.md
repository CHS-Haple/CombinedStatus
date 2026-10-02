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

Device evidence now separates the AOD problem into three successive layers:
- Build 628 fixed the original **native represented-icon flash** by making ScenePolicy the single Keyguard/AOD family eligibility authority.
- Build 630 fixed the **Keyguard <-> AOD Guiyuan self-flash** by removing the invalid `MiuiBatteryMeterView.alpha -> whole Guiyuan child alpha` copy.
- Build 630 device validation is now good for Keyguard <-> AOD. The only remaining visible defect is **Home -> AOD**, which still flashes once.

The Build-630 trace shows that Home -> AOD actually crosses `HOME -> KEYGUARD -> AOD`:
- the steady scene changes to Keyguard;
- Keyguard family readiness/presentation becomes combined within milliseconds and remains logically owned;
- later the same family session retargets to AOD;
- throughout the AOD callbacks, `childAlpha=1`, `ownerReady=true`, `sceneEligible=true`, and `systemIconsAlphaReadOnly=1`;
- therefore the remaining flash is not the already-fixed Battery alpha bug and not another family ownership cleanup.

Exact-target reference evidence identifies `MiuiKeyguardStatusBarView`, its `mKeyguardStatusBarContent`, and native `animateIconContainer(...)` / `animateFullAod(...)` ownership above the `mSystemIconsContainer` child. Since Android ViewOverlay/children inherit ancestor alpha/visibility, an ancestor-level transition can blank the Guiyuan child even when `system_icons.alpha == 1`.

Build 634 is a **diagnostic-only candidate** to identify that exact ancestor:
- log the full visual ancestry of the Keyguard/AOD host, `mKeyguardStatusBarContent`, and `system_icons`;
- record per-level visibility, alpha, `isShown`, and accumulated/effective alpha at the existing AOD state callbacks;
- do not change rendering, ownership, scene projection, alpha, visibility, geometry, timing, or native state.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003634` / Build `20261003-634`.
- Build 630 device result:
  - Keyguard -> AOD -> Keyguard: visually normal;
  - native represented icons remain suppressed correctly;
  - Home -> AOD still has one Guiyuan-only flash.
- The matching trace proves family readiness and ownership stay continuous through the failing edge.
- `system_icons` itself reports alpha 1 during the failing AOD transition sample, so the remaining visibility owner must be checked above that child rather than patched with another child alpha rule.
- Build 634 adds read-only ancestor visibility diagnostics only.

## Device gate

Build 634 requires one focused Home -> AOD capture with detailed diagnostics:
- reproduce Home -> AOD at least twice;
- no need to re-test Keyguard <-> AOD unless a regression is visible;
- return the diagnostic report from the same session;
- the decisive fields are `hostVisual`, `contentVisual`, and `systemIconsVisual` around the visible flash.

If one ancestor becomes hidden / alpha 0 while `system_icons` remains logically ready, the next runtime change should bridge only that proven cross-host visibility gap and must continue to preserve HyperOS-owned motion.

## Immediate next step

Run exact-HEAD Runtime CI for Build 634. If clean, produce one signed diagnostic Canary and use the resulting Home -> AOD ancestor trace to choose the narrowest cross-host bridge.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

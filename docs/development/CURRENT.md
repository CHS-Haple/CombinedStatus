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

Build 628 is partially accepted by device evidence: it fixes the native represented-icon flash during Keyguard/AOD scene switching, confirming that the single ScenePolicy ownership authority and one family Session/RenderView are correct. A visible self-flash remains: Guiyuan itself disappears briefly during the family scene change and then returns, without native icons taking over.

Build 630 isolates that remaining defect to child alpha ownership:
- the family RenderView remains `ownerReady=true`, `sceneEligible=true` and presentation ownership stays continuous during the flash;
- Build 628 nevertheless copies `MiuiBatteryMeterView.alpha` onto the entire Guiyuan child whenever the role is AOD;
- HyperOS independently animates Battery alpha and status-icon alpha during `animateFullAod()`; Battery may legitimately reach alpha 0 while the family/status-icons surface remains the correct Guiyuan carrier;
- therefore Battery child alpha is not a valid animation clock for the whole combined visual.

Build 630 keeps Guiyuan child alpha at 1 and inherits only the verified `system_icons` family host's native parent visibility/motion. Battery/status-icons/system-icons alpha are now read-only diagnostics and do not become Guiyuan alpha writers.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003630` / Build `20261003-630`.
- Build 628 device evidence confirms the native represented-icon flash is fixed but a roughly 0.4 s Guiyuan-only blank interval remains.
- Matching diagnostics keep `ownerReady=true`, `sceneEligible=true`, `overlayVisible=true` across the same scene transfers, excluding presentation ownership loss as the remaining root cause.
- Existing architecture/reference evidence states that HyperOS AOD independently animates Battery and status-icon alpha; copying Battery alpha onto the whole Guiyuan child collapses distinct native animation layers.
- Build 630 removes only that derived child-alpha write; no new animation clock, timer, delay, native alpha/visibility writer or ownership path is added.
- Exact Build-630 Runtime CI #2322 passed.

## Device gate

Focused Build-630 validation:
- repeated Keyguard -> AOD -> Keyguard and Home -> AOD -> Home: no Guiyuan blank/flash interval and no native represented-icon flash;
- child alpha should remain visually continuous while HyperOS controls the parent family host transition;
- AOD steady state still follows native parent visibility and scene lifecycle;
- disabled AOD/Keyguard child settings and global Guiyuan off still fail native immediately;
- watch for any stale frame, duplicate set, or AOD content leaking into Control Center.

## Immediate next step

Record Build-628 rejection/Build-630 alpha root cause, then freeze runtime and produce one signed Work Branch Canary from the exact reviewed Build-630 branch.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

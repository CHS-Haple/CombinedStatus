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

Build 666 is a focused lifecycle validation of the remaining Build-665 AOD -> Keyguard peer-motion defect.

Build-665 device evidence:
- the defect still reproduces;
- each incoming-Keyguard attempt reports `hostShownAtArm=true`, so Build 665 does not precommit compact occupancy;
- the same transition then reports native `statusIconsAlpha=0.0`, proving the enclosing Keyguard host can already be shown while the animated native status-icon layer is still fully hidden;
- therefore `View.isShown` is not the native presentation lifecycle authority for this transition.

Build-666 correction:
- keep the same stable-AOD -> enabled-Keyguard / disabled-AOD eligibility;
- replace the hidden-host guard with the exact Keyguard status-icon presentation state returned by `statusIconsPresentationAlpha()`;
- allow precommit only when that exact native layer is attached and fully hidden (`alpha == 0f`);
- unavailable or partially visible native status icons fail back to the existing deferred path.

No timer, copied animation duration, custom interpolator, peer translation, native alpha/visibility writer, or geometry compensation is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003666` / Build `20261003-666`.
- PR #196 was 0 behind `dev` before authoring.
- Unit coverage now requires exact native status-icons hidden state for Keyguard boundary precommit.
- Exact-head Runtime CI is running; one signed Canary follows only if Runtime is clean.
- Home -> AOD transient-owner handling is intentionally unchanged in this minimal lifecycle checkpoint.

## Device gate

1. Keyguard ON / AOD OFF — AOD -> Keyguard:
   - diagnostics should show `statusIconsAlphaAtArm=0.0` and `nativeLayoutOwnership=precommit-before-reveal`;
   - adjacent native icons must no longer collapse inward from the middle;
   - Guiyuan continuity must not regress.

2. Keyguard -> AOD:
   - regression only; preserve accepted behavior.

3. Home/Desktop -> AOD:
   - observe only in this build; do not judge the separate transient-owner issue as fixed yet.

## Immediate next step

Complete exact-head Runtime review. If green, issue one signed Canary and freeze runtime for the focused AOD -> Keyguard lifecycle gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. `SystemUI-Reference` exact-target findings and task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

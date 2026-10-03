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

Build 668 follows successful Build-667 device validation. Build 667 closed the known Keyguard/AOD lifecycle defects:
- AOD -> Keyguard no longer collapses adjacent peers inward;
- AOD -> Keyguard fast/partial Control Center pull no longer falls back to native in the reproduced path;
- Keyguard -> AOD no longer returns to native at fade start and now keeps the outgoing Keyguard owner until the native status-icon lifetime ends.

The remaining device issue is only **Home/Desktop -> AOD when AOD projection is disabled**: Guiyuan hands back to native AOD later than desired.

Build-667 evidence for the remaining path:
- Home is authoritative immediately before screen-off and Home still owns represented slots;
- HyperOS does not go directly Home -> AOD. The first Full-AOD target is Keyguard, then roughly one native callback later `toAod=true / isAodAnimate=true` begins without a stable Keyguard endpoint;
- therefore this is a direct Home -> native-AOD lifecycle routed through a transient Keyguard target, not an ordinary stable Keyguard -> AOD transition.

Build-668 lifecycle correction:
- arm a Home-native-AOD fallback candidate only at Full-AOD entry while the authoritative steady source is HOME, Home still owns represented slots, Keyguard projection is enabled, and AOD projection is disabled;
- a transient `target=keyguard` does not consume or clear that candidate;
- if native AOD animation then begins before stable Keyguard, promote the candidate to an active native-AOD fallback, tear down the transient Keyguard presentation, and keep native authoritative until stable AOD;
- if stable Keyguard is reached first, clear the candidate so later Keyguard -> AOD keeps the accepted Build-667 alpha-lifetime behavior;
- direct native target=AOD may consume the same candidate immediately;
- reverse Keyguard target, resolver failure, stable endpoints, feature-setting changes, Hot Reload and full feature teardown fail closed.

AOD -> Keyguard Control Center risk review:
- Build 667 fixed the observed fast-pull failure with incoming-boundary presentation readiness;
- a residual callback-order race remained possible if panel expansion fraction arrived before visible/source reconciliation while the cached source was still HOME;
- Build 668 bridges that exact case: once the existing incoming Keyguard presentation-ready fact is true, fraction > 0 promotes the Control Center source to KEYGUARD before lease acquisition;
- visible/source conflict resolution also prefers KEYGUARD only when the same incoming-ready fact is true and at least one native source witness explicitly reports KEYGUARD;
- normal unlock cannot use this guard because incoming-boundary readiness is absent.

No timer, delay, copied duration/interpolator, native alpha/visibility/translation writer, peer-motion writer, geometry compensation, or second presentation owner is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003668` / Build `20261003-668`.
- PR #196 was 0 behind `dev` before authoring.
- Pre-commit lifecycle review completed for Home -> transient Keyguard -> native AOD and AOD -> incoming Keyguard -> Control Center.
- Unit coverage includes Home fallback arming, AOD-animation consumption, direct native-AOD target, stale family override, incoming Keyguard source conflict, and non-Keyguard unlock rejection.
- Exact-head Runtime CI and one signed Canary are required.

## Device gate

1. Keyguard ON / AOD OFF — Home/Desktop -> AOD:
   - the native/system flash may remain;
   - transient Keyguard Guiyuan must hand back as soon as native AOD animation begins, not hundreds of milliseconds later;
   - no second Guiyuan interval after native AOD takeover.

2. Keyguard ON / AOD OFF — ordinary Keyguard -> AOD:
   - preserve Build-667 behavior: Guiyuan stays until native Keyguard status-icons actually reach their hidden endpoint.

3. AOD -> Keyguard, immediate/fast/partial pull:
   - no native status row / native QS fake even if expansion fraction arrives before visible/source callback;
   - aborting or holding the partial pull remains combined.

4. AOD -> Keyguard normal path:
   - preserve no-peer-merge fix.

## Immediate next step

Freeze runtime at Build 668, complete exact-head Runtime CI and review, then issue one signed Canary for the focused lifecycle gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. `SystemUI-Reference` exact-target findings and task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

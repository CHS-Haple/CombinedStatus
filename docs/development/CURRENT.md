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

Build 667 follows Build-666 device validation and fixes two lifecycle-boundary defects without changing native motion ownership.

Build-666 device evidence:
- AOD -> Keyguard peer icons no longer merge inward, validating native status-icon presentation alpha as the correct incoming-layout lifecycle authority.
- Keyguard -> AOD returns to native too early: `animateIconContainer(false)` starts native status-icon fade, but the first `isAodAnimate=true` callback releases the Keyguard presentation only milliseconds later while the native transition is still active.
- AOD -> Keyguard can briefly show native QS/status icons when pulled down quickly. The incoming Keyguard visual handoff is already revealed and compact layout is ready, but stable `keyguardRuntimeReady` is still false; Control Center therefore temporarily treats the source as native until stable Keyguard commits.

Build-667 lifecycle correction:
- outgoing Keyguard visual ownership is retained while the exact native Keyguard status-icon layer still has visible presentation alpha (`alpha > 0`); release occurs only at `alpha == 0` or the stable AOD endpoint;
- `animateIconContainer(false)` is treated as fade-start, not the outgoing owner cleanup boundary;
- stable Keyguard readiness remains unchanged;
- a derived incoming-boundary presentation-ready state is available only when visual handoff is active, compact prelayout is complete, the native visual boundary has been reached, the host is attached, Keyguard projection is enabled, and AOD projection is disabled;
- Keyguard-originated Control Center may use that already-valid incoming presentation before stable-family commit, and a temporary AOD-blocked flag cannot revoke an active lease during that bounded incoming handoff;
- fraction-zero cleanup and transient readiness loss do not tear down the incoming Keyguard owner while that derived presentation-ready state remains valid.

No timer, delay, copied duration/interpolator, native alpha/visibility/translation writer, peer-motion writer, or second presentation owner is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003667` / Build `20261003-667`.
- PR #196 was 0 behind `dev` before authoring.
- Pre-commit lifecycle review completed across Home / Keyguard / AOD / Keyguard Control Center ownership.
- Unit coverage includes outgoing Keyguard alpha lifetime, native visual-boundary semantics, incoming boundary readiness, host detach failure, and AOD-blocked lease retention only during the verified incoming handoff.
- Exact-head Runtime CI and one signed Canary are required.

## Device gate

1. Keyguard ON / AOD OFF — Keyguard -> AOD:
   - Guiyuan must not switch to native immediately at fade start;
   - it should remain the outgoing owner until the native Keyguard status-icon presentation actually reaches its hidden endpoint;
   - stable AOD remains native.

2. Keyguard ON / AOD OFF — AOD -> Keyguard, immediate/fast partial pull:
   - no transient native status row / native QS fake;
   - holding a partial pull must not wait for stable Keyguard to recover Guiyuan;
   - aborting the pull must not tear down the incoming Keyguard handoff.

3. AOD -> Keyguard normal path:
   - preserve Build-666 fix: no peer icons merging inward.

4. Home/Desktop -> AOD:
   - still observe only; the separate Home-origin transient-owner defect is not part of Build 667.

## Immediate next step

Freeze runtime at Build 667, run exact-head Runtime CI, then issue one signed Canary for the lifecycle gate above.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. `SystemUI-Reference` exact-target findings and task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

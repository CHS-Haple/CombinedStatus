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

Build 639 addresses the independent Keyguard/AOD child-switch handoff asymmetry exposed by Build 636 device testing.

Build-636 device evidence:
- with Keyguard enabled and AOD disabled, AOD -> Keyguard waits until the native AOD animation state fully clears before Guiyuan reacquires Keyguard, producing a delayed Guiyuan entrance;
- with Keyguard disabled and AOD enabled, AOD -> Keyguard retains the AOD-owned Guiyuan presentation after the steady source has already returned to Keyguard, producing a delayed Guiyuan release/exit;
- Home -> AOD with Keyguard disabled can expose native represented icons during the transient native Keyguard source interval before the AOD owner is attached;
- the two AOD -> Keyguard delays are the same ownership-boundary defect in opposite directions.

Historical constraint remains authoritative: Build 621 proved `toAod` / `animToAod` are not reliable transition-direction authorities on the pinned HyperOS target. Build 639 does not use them to guess direction.

Build 639 correction:
- dual-enabled Keyguard/AOD transitions retain the accepted ownership-driven continuity behavior unchanged;
- Keyguard-only mode may acquire Keyguard as soon as the verified steady source is Keyguard, without waiting for the later AOD-animation teardown callback;
- AOD-only mode releases an already-owned AOD presentation when the verified steady source is Keyguard and Home is no longer the still-owned source;
- AOD-only Home -> AOD keeps/prearms the AOD family owner across the transient native Keyguard source while Home presentation ownership is still present;
- all handoffs continue to use one Keyguard-family Session / one RenderView with existing presentation ownership, masking, reservation and fail-native cleanup.

No timer, delay, polling, alpha/visibility/translation writer, copied native animation, or second scene-state machine is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003639` / Build `20261003-639`.
- Work branch is based on current `dev` with no behind commits at the correction checkpoint.
- Focused ScenePolicy coverage locks:
  - Keyguard-only acquisition during the native AOD animation tail once steady Keyguard is verified;
  - AOD-only release at the same steady-Keyguard boundary when Home no longer owns the source;
  - Home-owned AOD prearm retention across the transient Keyguard source;
  - existing dual-enabled ownership continuity independent of unreliable direction fields.
- Exact-HEAD Runtime CI is required before a signed Canary.
- Device evidence is required because the correction changes visible Keyguard/AOD family ownership timing.

## Device gate

After exact-HEAD Runtime CI passes, validate one signed Build-639 Canary in two independent configurations:

1. Keyguard ON / AOD OFF
   - Home -> AOD may use native AOD presentation because AOD child is disabled;
   - AOD -> Keyguard should acquire Guiyuan at the verified Keyguard scene boundary without a delayed entrance animation.

2. Keyguard OFF / AOD ON
   - Home -> AOD should not expose a transient native represented-icon frame before Guiyuan AOD ownership;
   - AOD -> Keyguard should release Guiyuan when Keyguard becomes the verified steady source, without a delayed Guiyuan exit animation.

Also verify dual-enabled Keyguard <-> AOD remains unchanged and continuous.

## Immediate next step

Run exact-HEAD Runtime CI for Build 639. If green, request one signed Work Branch Canary and freeze #196 runtime for the focused two-configuration device test.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

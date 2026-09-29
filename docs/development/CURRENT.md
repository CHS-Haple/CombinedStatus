# Current Development State

This file is the concise recovery point for active Guiyuan development.

## Accepted baseline

- Guiyuan 0.0.3.
- Accepted runtime/build baseline: Build 473 / 20260929-473.
- main and dev share the accepted Build-473 stable history before this workflow simplification.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Modern Xposed API 102.
- Build 472 companion-app presentation and Build 473 Wi-Fi optical normalization are accepted behavior.

Historical CI numbers, promotion mechanics, rejected Builds, and earlier hypotheses remain in DEVLOG/GitHub history.

## Active objective

Control Center transition animation is the active runtime task.

Current implementation line:
- PR #177 — feat/control-center-transition-matrix;
- exact-target direction: QS_FAKE role-5 to final-QS role-6 matrix projection;
- HyperOS remains translation/appearance authority;
- Guiyuan reads native transition state and renders only its owned presentation;
- generic peer projection, Guiyuan-owned network composition, and native-only unsupported peers remain separated.

PR #174 is an older transition route and must not overwrite the newer active matrix line or accepted Build-473 renderer state.

## Current architecture boundary

### Home
SystemUI owns surrounding layout, Battery/slot behavior, tint semantics, scene visibility, and charging/Super-Island motion. Guiyuan owns its compact composition and narrowly scoped reversible presentation state.

### Control Center
Home/Keyguard may bridge through verified QS_FAKE transition ownership. Fully expanded Control Center remains native. Guiyuan must not create a second gesture timeline or take ownership of final Control Center layout/alpha/translation. Notification Shade remains native-only on the pinned target.

### Keyguard / AOD
Keyguard has an independent host/session while sharing domain/render semantics. AOD remains native-only. Home and Keyguard do not share mutable host ownership.

## Validation state

Build 473 is device-accepted and stable.

The active Control Center transition line is not yet accepted. It requires focused device evidence for path/peer motion and Home/Keyguard entry/return behavior before integration.

Device testing is requested only when the result can change implementation choice or acceptance. Mechanical/documentation steps continue without a new APK round trip.

## Non-negotiable boundaries

- Root-cause first; no speculative geometry/timing compensation.
- Prefer authoritative HyperOS state/resources/motion.
- One mutable runtime property has one writer.
- No duplicate polling, state machines, gesture animators, or high-frequency diagnostics.
- Keep stable geometry, transition geometry, native occupancy, and optical adjustment separate.
- Cleanup/Hot Reload restores only Guiyuan-owned state.
- Compatibility uncertainty fails native.
- Accepted Build-473 Wi-Fi optical behavior must not be overwritten by older transition branches.

## Immediate next step

1. Land the workflow simplification without changing APK/runtime behavior.
2. Synchronize active PR #177 with the updated dev governance baseline before eventual merge.
3. Continue Control Center transition work on PR #177; generate signed Canary only when focused device evidence is needed.
4. Keep 1.0.0 gated by actual product/compatibility acceptance rather than extra promotion-marker branches.

## Reference priority

1. CONTRIBUTING.md;
2. this file;
3. task-specific architecture/reference docs;
4. current source and exact-target SystemUI evidence;
5. relevant historical DEVLOG entries when needed.

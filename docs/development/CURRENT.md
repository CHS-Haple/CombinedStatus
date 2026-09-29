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
- current work-branch checkpoint: Build 487 / 20260930-487;
- exact-target direction: QS_FAKE role-5 to final-QS role-6 matrix projection;
- HyperOS remains translation/appearance authority; raw native expansion drives transition geometry and native fake/final Folme owns visual handoff;
- Guiyuan reads native transition state and renders only its owned Trinity correspondence; final role-6 top-level slots are read-only occupancy witnesses, while the existing QS_FAKE statusIcons-paddingEnd owner now provides a progress-synchronous semantic reservation so surrounding native peers move through SystemUI's own measure/layout path;
- Trinity transition is component-driven: Painter owns source optical bounds/semantic target/shape policy; role-6 top-level slots own final occupancy, child/drawable data refines optical alignment, and reservation width is the union of native-progress-interpolated semantic spans rather than the rendered pixel envelope;
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

The active Control Center transition line is not yet accepted. Build 480 is device-rejected on non-charging Home because full-target affine interpolation visibly flattened Trinity elements and parent-View targets collapsed unrelated semantics into the same geometry. Build 481 is also device-rejected: Mobile did not reliably move when internal final children were 0×0, Wi-Fi optical size/endpoint did not coincide with the native glyph, transition tint did not consistently follow final native peers, and child-first targeting did not represent real final slot occupancy. Build 482 replaces child-first targeting with read-only role-6 top-level slot witnesses; internal children/drawables only refine optical alignment, raw native expansion owns external motion, native fake alpha drives the local Mobile morph, and final SystemUI appearance remains native-owned. Exact-head Runtime Build #1833 and signed Work Branch Canary #522 pass for source `cfdf12ff4c2e8249f833e52e871cb35f1bad953b`. Focused device feedback says Build 482 is substantially improved and its Battery/Wi-Fi trajectory is the explicit rollback baseline, but it is not accepted because decomposed elements do not create live layout occupancy, surrounding native peers therefore do not move with the split, and Mobile needs a staged dots -> row -> bars morph before native handoff.

Build 483 kept the Build-482 external trajectory and added semantic reservation plus a local dots -> row -> bars Mobile morph. Build 484 then split component scale policy, added fast exit for semantics without a destination, and attempted a Battery ring -> Battery outline morph.

Build-484 device review rejects three details without rejecting the overall matrix/reservation line: (1) the Battery morph used the whole 105x169/135x169 MiuiBatteryMeterView fallback as if it were the glyph and therefore produced an oversized, hard-looking Battery; (2) the transition source sampled MiuiBatteryMeterView each frame, so charging/status-bar press transforms could flatten the whole Guiyuan source and every later component trajectory; (3) Mobile bars grew only upward from a bottom baseline and remained too short relative to the final native signal.

Build 485 removed the charging-press flattening but device review rejects its synthetic Battery contour, exposes a new external trajectory drift from making renderView the full source authority, leaves the Mobile signal baseline visually wrong, and shows 5G overlapping signal when a semantic child target is unavailable.

Build 486 restores the accepted pre-custom Battery treatment: the ring uses the Build-482 fold/projection handoff and no synthetic Battery body is drawn. Source ownership is split: MiuiBatteryMeterView contributes native center/translation while Guiyuan renderView contributes the stable basis, so transient charging/press scale cannot flatten Guiyuan without replacing the accepted native trajectory coordinate authority. Mobile first forms a row, then all four bars grow upward from one shared fixed lower baseline for the entire bar-growth phase. Target resolution now follows one fail-fast rule: semantic children such as wifi_signal, mobile_type(_single), and mobile_signal must be visible, attached and non-zero; otherwise that component uses the existing fast fade/slight-shrink exit instead of guessing a top-level slot position. HyperOS remains external progress, final appearance, peer-layout, and themed-asset authority.

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

1. Run exact-head Runtime CI for Build 487; if green, request one signed work-branch Canary.
2. Device gate A — HyperCeiler dual-row: verify the four-dot/mobile-signal component now migrates toward the visible dual-row signal container instead of staying behind or fading.
3. Device gate B — 5G separation: verify real `mobile_type` geometry still wins and 5G does not overlap the signal morph; if a type child is unavailable, the bounded type region remains separated from the bounded signal region.
4. Device gate C — native/no-modifier regression: verify native HyperOS mobile/Wi-Fi paths still prefer real semantic children and are unchanged when those children are available.
5. Keep Build-486 trajectory, Battery ring-fold, flat signal baseline, and charging-press behavior unchanged.

## Reference priority

1. CONTRIBUTING.md;
2. this file;
3. task-specific architecture/reference docs;
4. current source and exact-target SystemUI evidence;
5. relevant historical DEVLOG entries when needed.

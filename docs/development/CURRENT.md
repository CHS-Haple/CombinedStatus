# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.4.
- `main` and `dev` are synchronized at the promoted Build 613 repository baseline before this work branch.
- Latest accepted Runtime/SystemUI behavior before this branch: Build 612, maintainer device-accepted with no reported anomaly.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- GPL-3.0-or-later remains the project license.

## Active objective

Branch: `fix/wifi-ring-shape-avoidance`.

Build 615 / `20261002-615` contains three bounded visual-geometry corrections:
1. shape-aware battery-ring avoidance for Wi-Fi;
2. requested size-range expansion;
3. mobile-type overall-scale ownership plus visual-settings main-thread serialization.

### Wi-Fi ring avoidance

Root cause:
- the native Wi-Fi drawable was already probed into disconnected visible optical components;
- the painter discarded those components and kept only their union envelope;
- the ring therefore reserved empty corners between Wi-Fi arcs.

Correction:
- preserve read-only drawable components through the native optical probe/cache;
- map each component through the same draw geometry as the rendered resource;
- carry components through top-slot translation/appearance scaling;
- resolve/merge only the angular gap actually required by visible components;
- use real fallback Wi-Fi path bounds if the native resource is unavailable;
- numeric/readout avoidance remains the existing single-envelope path.

### Visual sizing

Requested ranges:
- overall size: 60%-100%;
- Wi-Fi size: 40%-125%;
- mobile-type size: 40%-125%.

Implementation:
- persisted/runtime clamps use those exact limits;
- UI sliders retain 5% discrete cadence;
- renderer Wi-Fi/mobile-type clamps reference the same settings constants instead of a hidden 70% floor;
- mobile-type text/suffix continue compensating host viewport scale but no longer cancel user `combinedScale`, so 5G/5GA now follow overall size in production;
- Preview retains its existing direct Canvas-scale path.

### Visual-settings hot-update robustness

Build-612 device evidence reported one transient loss of the combined presentation while adjusting overall size; toggling the feature off/on restored it. The diagnostic contained no process exception and later battery/Wi-Fi model-to-draw events were still healthy, which is consistent with presentation ownership falling back rather than SystemUI crashing.

Code review found visual preference callbacks lacked the main-thread dispatch already used by feature preferences, despite directly updating RenderViews, manual measure/layout, and native end reservation. Build 615:
- serializes visual-settings commits onto the SystemUI main thread;
- drops stale queued slider snapshots so renderer and reservation commit one latest settings state;
- records combined/Wi-Fi/mobile-type scale plus `mainThread=true` in diagnostics.

No timer/delay repair, polling, hard-coded shrink ratio, device-width constant, duplicate animation, or new native writer is introduced.

## Current transition contract

- HyperOS remains the sole expansion / appearance timeline authority.
- Home and Keyguard bridge only through the verified QS_FAKE interval; fully expanded Control Center remains native-owned.
- Notification Shade and AOD remain native-only on the pinned target.
- Guiyuan does not write native peer translation, alpha, visibility, visibleState, or a second gesture animator.
- `statusIcons.paddingEnd` remains the only progress-driven peer-layout property.
- QS_FAKE capacity remains measurement-only and excluded from transition motion by the accepted Build-612 logical-carrier projection.
- Compatibility uncertainty fails native.

## Validation state

Pre-commit review:
- numeric/readout avoidance remains unchanged;
- native and fallback Wi-Fi both provide component geometry;
- no old envelope-only Wi-Fi gap path remains;
- requested UI/runtime ranges are aligned;
- no hidden 70% renderer floor remains;
- mobile-type production scaling is separated into host compensation × user overall scale;
- Control Center transition bounds and Home avoidance reuse the same mobile-type layout path;
- visual settings now cross one explicit SystemUI-main-thread boundary before View/presentation mutation.

Focused tests cover:
- component-aware Wi-Fi gap vs envelope gap;
- single-component gap preservation;
- requested scale endpoints;
- renderer/settings clamp agreement;
- mobile-type 100%→60% physical scaling while host viewport scale remains compensated;
- Preview direct-scale behavior.

Required next:
1. exact-head Runtime CI for Build 615;
2. signed Work Branch Canary;
3. focused device check: Wi-Fi opening tightens without touching the glyph;
4. drag overall size repeatedly, including 100%↔60%: combined presentation must remain present and 5G/5GA must visibly scale;
5. verify 40% Wi-Fi/mobile-type endpoints and unchanged numeric top avoidance.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

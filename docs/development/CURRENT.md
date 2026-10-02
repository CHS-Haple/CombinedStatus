# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.4.
- `main` and `dev` are synchronized at the promoted Build 613 repository baseline before this work branch.
- Latest accepted Runtime/SystemUI behavior: Build 612, maintainer device-accepted with no reported anomaly.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- GPL-3.0-or-later remains the project license.

## Active objective

Branch: `fix/wifi-ring-shape-avoidance`.

Build 614 / `20261002-614` corrects the oversized battery-ring opening around Wi-Fi when the network is placed above the battery ring.

Root cause:
- the native Wi-Fi drawable was already probed into disconnected visible optical components;
- the painter discarded those components and kept only their union envelope;
- the ring avoidance policy therefore treated the empty corners between Wi-Fi arcs as occupied content;
- numeric readout avoidance looks correct because text is naturally close to a rectangular optical envelope.

Build-614 correction:
- preserve read-only drawable component geometry through the existing native optical probe/cache;
- map each component through the exact same draw geometry used by the rendered native resource;
- carry component geometry through top-slot translation and appearance scaling;
- resolve one gap per visible component and merge only the angular intervals that are actually required;
- use the three real fallback Wi-Fi path bounds when the native resource cannot be resolved;
- keep non-Wi-Fi and numeric/readout content on the existing single-envelope path.

No fixed shrink ratio, device-width constant, new animation, state source, native writer, timing path, or visual-clearance change is introduced.

## Current transition contract

- HyperOS remains the sole expansion / appearance timeline authority.
- Home and Keyguard bridge only through the verified QS_FAKE interval; fully expanded Control Center remains native-owned.
- Notification Shade and AOD remain native-only on the pinned target.
- Guiyuan does not write native peer translation, alpha, visibility, visibleState, or a second gesture animator.
- `statusIcons.paddingEnd` remains the only progress-driven peer-layout property.
- QS_FAKE capacity remains measurement-only and excluded from transition motion by the accepted Build-612 logical-carrier projection.
- Compatibility uncertainty fails native.

## Validation state

Static review before commit:
- numeric/readout avoidance remains a single component and is behaviorally unchanged;
- native and fallback Wi-Fi paths both provide component geometry;
- top-slot transition scale/translation is applied to each component using the existing appearance contract;
- no duplicate helper or stale envelope-only call remains;
- focused unit coverage verifies component-aware gaps remove empty-envelope corner reservation and single-component merging preserves prior geometry.

Required next:
1. exact-head Runtime CI;
2. signed Work Branch Canary;
3. focused device check of battery-center Wi-Fi at default size: opening should visibly tighten while retaining a small clear gap and never touching the glyph;
4. quick regression check that the already-accepted numeric top avoidance is unchanged.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

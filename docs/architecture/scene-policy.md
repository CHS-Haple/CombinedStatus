# Scene capability policy

This document complements [layout-policy.md](layout-policy.md).

The layout policy owns shared Combined Status visual calculations. The scene policy owns only scene capability classification and motion ownership.

## Rule

A scene capability may define:

- whether Combined Status renders on the surface;
- whether rendering is projected against native geometry;
- who owns motion;
- the current evidence maturity.

A scene capability must not define per-scene size formulas, width-difference corrections, offsets, or translation compensation.

Classification is not permission to mutate SystemUI. Runtime integration still requires a verified host, lifecycle, state source, and failure path.

## Current capability map

| Scene | Render mode | Motion ownership | Evidence |
| --- | --- | --- | --- |
| Home stable | PROJECTED | NONE | Runtime verified |
| Notification-shade transition | NATIVE_ONLY | SYSTEM_UI | Static ownership verified |
| Control Center | NATIVE_ONLY | SYSTEM_UI | Static ownership verified |
| Keyguard | NATIVE_ONLY | SYSTEM_UI | Static ownership verified |
| AOD | NATIVE_ONLY | SYSTEM_UI | Static ownership verified |

The map intentionally fails closed outside the verified Home path. Unsupported or not-yet-verified scenes remain native rather than receiving a partial Combined Status implementation.

## Home stable

Home stable is currently the only runtime-verified Combined Status rendering scene.

Its PROJECTED mode means the Combined Status visual is anchored from verified native geometry while the native slot, native motion, and surrounding layout remain SystemUI-owned.

## Notification shade and Control Center

These surfaces remain NATIVE_ONLY because SystemUI owns their transition containers and motion.

A future combined representation must first prove a stable host/lifecycle contract and must not be implemented as an offset correction layered over native animation.

## Keyguard and AOD

Keyguard and AOD currently remain NATIVE_ONLY.

Historical behavior or static knowledge of their hosts is not sufficient to enable Combined Status rendering. Promotion requires runtime verification of host identity, lifecycle, state, tint, geometry, transition ownership, cleanup, and fallback.

## Charging

Charging, quick charging, and super charging are render-state variants, not scenes.

They must not create a second scene geometry policy or a separate slot-width rule.

## Motion ownership

Home stable currently uses `NONE`: Combined Status has no independent motion requirement there.

SystemUI-owned transition scenes use `SYSTEM_UI`.

`COMBINED_STATUS` remains reserved for a future transition that is demonstrated to be genuinely owned by Combined Status from start state through cleanup.

## Promotion rule

Changing a scene from NATIVE_ONLY to PROJECTED or introducing any new geometry/motion ownership requires:

1. exact target-SystemUI evidence;
2. runtime host and lifecycle verification;
3. a single-writer analysis;
4. fail-native behavior;
5. bounded diagnostics;
6. focused real-device validation;
7. an updated capability table and changelog entry.

If any of those are missing, the scene stays NATIVE_ONLY.

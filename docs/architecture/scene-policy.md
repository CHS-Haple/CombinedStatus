# Scene capability policy

This file complements `layout-policy.md`. The layout policy owns geometry rules; scene policy owns only **capability classification**.

## Rule

A scene capability may declare:

- whether CombinedStatus may own a real slot;
- whether it may only project the shared visual into a native slot;
- whether the surface stays native-only;
- who owns motion when motion exists;
- current evidence maturity.

It may not declare per-scene size formulas, offsets, width differences, or translation corrections.

## Initial capability map

| Scene | Render mode | Motion ownership | Evidence |
| --- | --- | --- | --- |
| Home stable | OWNED_SLOT candidate | NONE | runtime verified host/anchor |
| Notification shade transition | NATIVE_ONLY | SYSTEM_UI | static SystemUI ownership verified |
| Control Center | NATIVE_ONLY | SYSTEM_UI | static distinct surface/motion verified |
| Keyguard | PROJECTED candidate | SYSTEM_UI | static host/motion verified |
| AOD | PROJECTED candidate | SYSTEM_UI | static AOD ownership verified |

These are **integration candidates**, not permission to mutate those surfaces yet. Runtime promotion still requires scene-specific host/lifecycle verification.

## Charging

Charging, quick charging, and super charging are not scenes. They are render-state variants inside whichever scene is currently active. Therefore charging must not create a second layout policy or a separate slot-width rule.

## Why motion has NONE

Stable Home currently has no CombinedStatus-owned motion requirement. Explicit `NONE` prevents a future implementation from assuming that every CombinedStatus-owned slot should also invent a custom transition.

`COMBINED_STATUS` remains available only if a future visual transition is proven to be genuinely owned by CombinedStatus rather than SystemUI.

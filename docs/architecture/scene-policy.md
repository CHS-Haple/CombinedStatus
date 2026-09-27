# Scene capability policy

This document complements [layout-policy.md](layout-policy.md).

The layout policy owns shared Combined Status visual calculations. The scene policy owns only scene capability classification and motion ownership.

## 0.0.2 architecture status

The capability map below is retained as the **last verified runtime-scene evidence for the currently implemented path**. It is not a mandate to preserve that Home carrier in 0.0.2.

The permanent extra-participant / occupancy-handoff architecture explored by Builds 386-393 is **superseded as the default starting point for new 0.0.2 work**. Its runtime observations remain valid historical evidence.

Current 0.0.2 work must follow `docs/development/CURRENT.md`, `docs/development/ROADMAP.md`, and `docs/architecture/README.md`. Any future capability-map promotion should describe the architecture actually validated by the new carrier/presentation contract.

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
| Notification-shade transition | NATIVE_ONLY | SYSTEM_UI | Runtime lifetime verified |
| Control Center | PROJECTED | SYSTEM_UI | Build-420 runtime verified |
| Keyguard | NATIVE_ONLY | SYSTEM_UI | Static ownership verified |
| AOD | NATIVE_ONLY | SYSTEM_UI | Static ownership verified |

The map fails closed outside the verified Home and Control Center presentation paths. Unsupported or not-yet-verified scenes remain native rather than receiving a partial Combined Status implementation.

## Home stable

Home stable remains the primary persistent rendering scene. Build 420 additionally runtime-verifies a bounded Control Center projection.

Its PROJECTED mode means the Combined Status visual is anchored from verified native geometry while the native slot, native motion, and surrounding layout remain SystemUI-owned.

## Notification shade and Control Center

The pinned target separates these two panel paths.

### Notification Shade

Notification Shade remains **NATIVE_ONLY**: this target does not present the status-icon row there, so Combined Status must not invent one.

Home departure/return is governed by the verified Notification Header expansion path used by HyperOS itself:
`NotificationPanelExpansionAnimator.expansion -> NotificationPanelExpandController.expansionState -> NotificationHeaderExpandController.notificationCallback.onExpansionChanged(float)`.

Build-422 device/runtime evidence supersedes the earlier assumption that generic `ShadeExpansionStateManager.onPanelExpansionChanged(...)` is the correct Home handoff seam on this target. The generic callback remains useful scene context, but it is not the active Combined Status visibility authority.

Current rules:
- Header progress at the native zero boundary keeps Home eligible;
- positive Header progress transfers Combined Status away from Home;
- Battery `MiuiBatteryMeterView.mStatusBarState` is **not** a Home-visibility authority;
- `KeyguardManager.isKeyguardLocked` is not a valid discriminator for the transient Battery state;
- no local timing threshold, delay, polling loop, or reconstructed panel state machine is permitted.

The Home overlay is hosted in `MiuiNotificationStatusContainer / system_icon_area`. Its HostSession and host drawing lifecycle stay SystemUI-owned; Combined Status must not duplicate that lifecycle with a second global surface gate.

### Control Center

Control Center is **PROJECTED** from Build 420 device evidence.

The projection:
- resolves `ControlCenterHeaderExpandController.realSystemIcons`;
- requires the exact verified `MiuiStatusBatteryContainer` carrier already known to the Home presentation owner;
- reuses the shared renderer/model/tint policy;
- becomes ready before Home yields on entry;
- restores Home before projection cleanup on exit;
- leaves native motion/translation and native suppression ownership with SystemUI/current Home presentation owner.

No project-owned transition animation, fraction interpolation, peer geometry write or second native suppression owner is permitted.

## Keyguard and AOD

Keyguard and AOD currently remain NATIVE_ONLY.

Historical behavior or static knowledge of their hosts is not sufficient to enable Combined Status rendering. Promotion requires runtime verification of host identity, lifecycle, state, tint, geometry, transition ownership, cleanup, and fallback.

## Charging

Charging, quick charging, and super charging are render-state variants, not scenes.

They must not create a second scene geometry policy or a separate slot-width rule.

## Motion ownership

Home stable currently uses `NONE`: Combined Status has no independent motion requirement there.

Notification Shade and the projected Control Center both keep transition motion under `SYSTEM_UI`; projection does not transfer motion ownership to Combined Status.

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

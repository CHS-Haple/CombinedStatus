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
| Control Center transition bridge | PROJECTED | SYSTEM_UI | Build-420 carrier/handoff evidence |
| Control Center fully expanded | NATIVE_ONLY | SYSTEM_UI | Product target; endpoint implementation pending |
| Keyguard | NATIVE_ONLY | SYSTEM_UI | Static ownership verified |
| AOD | NATIVE_ONLY | SYSTEM_UI | Static ownership verified |

The map fails closed outside the verified Home path and the bounded Control Center transition evidence. Unsupported or not-yet-verified scenes remain native rather than receiving a partial Combined Status implementation. A verified transition carrier is not, by itself, permission to keep Combined Status visible as a fully expanded panel surface.

## Home stable

Home stable remains the primary persistent rendering scene. Build 420 additionally runtime-verifies a usable Control Center carrier and readiness-ordered handoff mechanism; that evidence is now scoped to the transition bridge rather than the fully expanded endpoint.

Its PROJECTED mode means the Combined Status visual is anchored from verified native geometry while the native slot, native motion, and surrounding layout remain SystemUI-owned.

## Notification shade and Control Center

The pinned target separates these two panel paths.

### Notification Shade

Notification Shade remains **NATIVE_ONLY**: this target does not present the status-icon row there, so Combined Status must not invent one.

The Home render must inherit the native Home end-side presentation lifecycle instead of deriving its own visibility from panel motion. Exact-target source verifies:

`StatusBarVisibilityInteractor.shouldHomeStatusBarBeVisible`
→ `HomeStatusBarViewModelImpl.isSystemInfoVisible`
→ `systemInfoCombinedVis`
→ `HomeStatusBarViewBinderInjector`
→ `mEndSideContent = R.id.system_icons`.

`showEndSideContent()/hideEndSideContent()` owns the native alpha / visibility / translation transition of `system_icons`. The exact `system_icons` root is `MiuiStatusBatteryContainer`.

Current rules:
- Home Combined Status renders in `MiuiStatusBatteryContainer(system_icons).overlay`, so native end-side alpha/visibility/translation apply naturally;
- Notification Header expansion remains useful motion evidence but is **not** a Combined Status Home-visibility authority;
- Battery `MiuiBatteryMeterView.mStatusBarState`, global Keyguard state, generic Shade expansion state, and local fraction thresholds are not Home-visibility authorities;
- no project-local Notification-Shade visibility Hook, timing threshold, delay, polling loop, or reconstructed panel state machine is permitted;
- the parent `MiuiNotificationStatusContainer / system_icon_area` remains the HostSession discovery/ownership boundary, while the visual carrier is the verified animated `system_icons` child.


### Control Center

Control Center is split into two ownership phases.

**Partial pull / transition bridge — PROJECTED**
- the source steady scene may be Home now and Keyguard later;
- a bounded Combined Status projection may use verified native transition progress/geometry to visually follow HyperOS during the gesture;
- `ControlCenterHeaderExpandController.realSystemIcons` / `MiuiStatusBatteryContainer` remains verified evidence for that bridge;
- the bridge is readiness-ordered so no frame is left without a valid visual owner;
- SystemUI remains the sole motion/geometry owner.

**Fully expanded endpoint — NATIVE_ONLY**
- once HyperOS reaches the native fully expanded Control Center state, Combined Status yields completely;
- the native status-bar presentation is shown without a persistent Combined Status projection;
- the bridge must clean up its own overlay/listeners/masks at the exact native endpoint and restore correctly on reverse motion.

Build 420 proved the carrier/handoff mechanism but kept projection alive through the expanded Control Center lifetime. That remains valuable runtime evidence, not the final product contract.

No project-owned timing threshold, custom animation, polling/frame follower, peer geometry write, or second native suppression owner is permitted.


## Keyguard and AOD

Keyguard and AOD currently remain NATIVE_ONLY in the implemented runtime.

The confirmed future product target gives **Keyguard its own steady Combined Status source adapter**, parallel to Home:
- Keyguard steady is not implemented by reusing the Home View/host;
- it reuses shared renderer/domain semantics but resolves its own native carrier, tint, lifecycle, cleanup and fail-native contract;
- a partial Control Center pull from Keyguard uses the same transition-coordinator policy as Home, but starts from the verified Keyguard source geometry/lifecycle;
- at fully expanded Control Center, ownership is native-only exactly as in the unlocked path.

AOD remains a separate future surface and is not implied by Keyguard support.

Historical behavior or static knowledge of these hosts is not sufficient to enable rendering. Promotion still requires runtime verification of host identity, lifecycle, state, tint, geometry, transition ownership, cleanup, and fallback.

## Charging

Charging, quick charging, and super charging are render-state variants, not scenes.

They must not create a second scene geometry policy or a separate slot-width rule.

## Motion ownership

Unlocked steady currently uses `NONE`: Combined Status has no independent motion requirement there. Its end-side visual inherits native `system_icons` motion when SystemUI transitions that carrier.

Notification Shade, unlocked/lockscreen Control Center transitions, and fully expanded Control Center all keep motion under `SYSTEM_UI`; inheritance/projection does not transfer motion ownership to Combined Status. Fully expanded Control Center is native-only.

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


## Target scene matrix

This target matrix is a product/architecture contract, not a statement that every row is implemented today.

| Source context | Steady state | Partial Control Center pull | Fully expanded Control Center |
| --- | --- | --- | --- |
| Unlocked / Home | Combined Status on verified Home carrier | Combined Status transition bridge follows native HyperOS motion | Native SystemUI status bar only |
| Locked / Keyguard | Combined Status on future verified Keyguard carrier | Combined Status transition bridge follows native HyperOS motion from the Keyguard source | Native SystemUI status bar only |

Design consequences:
- source-scene ownership and transition ownership are separate facts;
- Home and Keyguard each own only their steady adapter;
- one shared transition coordinator may consume source geometry/readiness and native Control Center transition authority;
- the coordinator never becomes a third persistent state machine or steady scene;
- the fully expanded Control Center endpoint is always native-only;
- reverse motion restores the correct source scene before bridge cleanup;
- no source adapter may infer the other source scene from Battery state, global Keyguard booleans, or timing.

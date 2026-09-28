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
| Control Center transition bridge | PROJECTED | SYSTEM_UI | Build 430 device-verifies top-level ControlCenterFakeStatusIcons fake/final ownership; Build 431 projects on its overlay |
| Control Center fully expanded | NATIVE_ONLY candidate | SYSTEM_UI | Maintainer concept + native fake/real appearance evidence; product adoption pending review |
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
- Build 420 proves that source geometry plus readiness-ordered handoff can preserve continuity, but Build 428 proves the selected `realSystemIcons` source itself is hidden during current Control Center ownership and cannot be the active display host;
- Builds 425-427 place the compact presentation inside child `QS_FAKE.system_icon_area`; device evidence rejects that child-carrier implementation;
- exact-target review still verifies the distinct top-level `ControlCenterFakeStatusIcons` presentation and SystemUI-owned Header translation/fake-to-final alpha;
- Build 430 device evidence verifies that the top-level fake View remains visible in normal and charging-island transitions and that HyperOS performs fake->final handoff by changing the root alpha while the child statusBarArea stays visible;
- Build 431 uses `ControlCenterFakeStatusIcons.overlay` as the transition visual host but device evidence rejects clip-only suppression because represented Wi-Fi/mobile layout occupancy remains and creates a large gap;
- Build 432 keeps the **root overlay** as the Combined visual carrier and proves the fake child can use the shared reversible compact-layout owner, but device evidence rejects preparing that owner from each Control Center visible cycle; Build 433 moves preparation into the Fake-root lifetime, Build 434 establishes compact readiness after first native root layout, and Build 435 separates render readiness from compact lifetime so transient layout loss on an attached, already-prepared Fake root may hide the Combined overlay but must not restore raw native Fake; cleanup remains Fake-root detach, feature disable, host replacement, Hot Reload, or a genuinely unprepared failure state;
- Build 439 separates Hot Reload restoration from cold-start prearm: a transferred Fake root that is already attached and laid out is restored directly from the main-thread Hot Reload task while outside native layout, so the existing compact owner can request its native status-icon layout from a valid scheduling boundary. Hosts without that lifecycle contract fall back to the existing first-native-layout prearm; no timing threshold or extra presentation writer is introduced.
- when fake Battery is natively hidden, the compact session reserves the same stable Battery logical slot width used in the non-island case; `batteryWidthDiff` is not a Combined endpoint/translation input;
- no additional status-icon measure/layout/battery-hide Hook set, project alpha/visibility/translation writer, interpolation, timer, polling/frame follower, or final-QS mutation is permitted; one low-frequency Fake-root attach Hook plus a temporary root layout listener may own bootstrap/readiness because they follow the native host/layout lifetime, and that listener must be removed after success/final failure/detach;
- SystemUI remains the sole motion/geometry/appearance owner.

**Fully expanded endpoint — current design candidate**
- the maintainer currently prefers a native-only fully expanded Control Center state;
- this is a product-intent hypothesis, not yet a verified endpoint/lifecycle contract;
- exact source/runtime review must determine the true ownership boundary and whether a cleaner native handoff abstraction exists before this becomes implementation policy.

Build 420 proved the carrier/handoff mechanism and kept projection alive through the expanded Control Center lifetime. That remains valuable runtime evidence. Whether the final endpoint should be native-only is still under architecture review.

No project-owned timing threshold, custom animation, polling/frame follower, peer geometry write, or second native suppression owner is permitted.


## Keyguard and AOD

Keyguard and AOD currently remain NATIVE_ONLY in the implemented runtime.

The maintainer's current product concept gives **Keyguard its own steady Combined Status source role**, parallel to Home, but the exact adapter/lifecycle structure remains to be derived from SystemUI evidence:
- Keyguard steady is not implemented by reusing the Home View/host;
- it reuses shared renderer/domain semantics but resolves its own native carrier, tint, lifecycle, cleanup and fail-native contract;
- HyperOS already registers Home and Keyguard system-icon containers separately into `ControlCenterFakeViewController` and selects the active source from native `StatusBarState`; Combined Status should not duplicate that transition-source router;
- the exact project adapter boundary for a Keyguard-originated pull remains under review rather than being forced into a preselected coordinator abstraction;
- native-only fully expanded Control Center remains the maintainer's current product preference, with adoption pending final lifecycle/device review.

AOD remains a separate future surface and is not implied by Keyguard support.

Historical behavior or static knowledge of these hosts is not sufficient to enable rendering. Promotion still requires runtime verification of host identity, lifecycle, state, tint, geometry, transition ownership, cleanup, and fallback.

## Charging

Charging, quick charging, and super charging are render-state variants, not scenes.

They must not create a second scene geometry policy or a separate slot-width rule.

## Motion ownership

Unlocked steady currently uses `NONE`: Combined Status has no independent motion requirement there. Its end-side visual inherits native `system_icons` motion when SystemUI transitions that carrier.

Notification Shade and all Control Center transition/destination motion stay under `SYSTEM_UI`; inheritance/projection does not transfer motion ownership to Combined Status. Whether Combined Status renders on a given verified carrier is a separate capability decision from who owns motion.

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


## Working scene concept matrix

This matrix records the maintainer's current product-intent partition. It is **not** yet an architecture contract. Exact-target lifecycle/source review may produce a better grouping; any such change should be reviewed and discussed before implementation.

| Source context | Steady state | Partial Control Center pull | Fully expanded Control Center |
| --- | --- | --- | --- |
| Unlocked / Home | Combined Status on verified Home carrier | Combined Status transition bridge follows native HyperOS motion | Native SystemUI status bar only |
| Locked / Keyguard | Combined Status on future verified Keyguard carrier | Combined Status transition bridge follows native HyperOS motion from the Keyguard source | Native SystemUI status bar only |

Design consequences:
- source-scene ownership and transition ownership should be evaluated separately;
- Home/Keyguard may end up as separate adapters, a shared higher-level lifecycle, or another exact-target structure; do not decide this from the conceptual table alone;
- a shared transition coordinator is a candidate only if source/runtime evidence supports it without creating a third state machine;
- the maintainer currently prefers a native-only fully expanded Control Center endpoint, pending verification;
- reverse motion restores the correct source scene before bridge cleanup;
- no source adapter may infer the other source scene from Battery state, global Keyguard booleans, or timing.

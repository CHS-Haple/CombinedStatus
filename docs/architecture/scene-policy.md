# Scene capability policy

This document complements [layout-policy.md](layout-policy.md).

The layout policy owns shared Guiyuan visual calculations. The scene policy owns only scene capability classification and motion ownership.

## 0.0.3 architecture status

The capability map below describes the verified runtime-scene contract for the current 0.0.3 development line. Historical carrier experiments remain evidence only and do not override the accepted 0.0.3 ownership model.

The permanent extra-participant / occupancy-handoff architecture explored by Builds 386-393 remains **superseded for current work**. Its runtime observations remain valid historical evidence.

Current 0.0.3 work must follow `docs/development/CURRENT.md`, `docs/development/ROADMAP.md`, and `docs/architecture/README.md`. Capability changes must describe the architecture actually validated by the current carrier/presentation contract.

## Rule

A scene capability may define:

- whether Guiyuan renders on the surface;
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
| Control Center fully expanded | NATIVE_ONLY | SYSTEM_UI | Exact-target fake/final appearance ownership is verified; accepted runtime keeps the final surface native-only |
| Keyguard | PROJECTED | SYSTEM_UI | Build 456 is maintainer device-accepted with a separate opt-in Keyguard host/render/presentation adapter |
| AOD | NATIVE_ONLY | SYSTEM_UI | Static ownership verified; independent runtime gate |

The map fails closed outside the verified Home / opt-in Keyguard steady paths and the bounded Control Center transition bridge. Unsupported scenes remain native rather than receiving a partial Guiyuan implementation. A verified transition carrier is not, by itself, permission to keep Guiyuan visible as a fully expanded panel surface.

## Home stable

Home stable remains the primary persistent rendering scene. Build 420 additionally runtime-verifies a usable Control Center carrier and readiness-ordered handoff mechanism; that evidence is now scoped to the transition bridge rather than the fully expanded endpoint.

Its PROJECTED mode means the Guiyuan visual is anchored from verified native geometry while the native slot, native motion, and surrounding layout remain SystemUI-owned.

## Notification shade and Control Center

The pinned target separates these two panel paths.

### Notification Shade

Notification Shade remains **NATIVE_ONLY**: this target does not present the status-icon row there, so Guiyuan must not invent one.

The Home render must inherit the native Home end-side presentation lifecycle instead of deriving its own visibility from panel motion. Exact-target source verifies:

`StatusBarVisibilityInteractor.shouldHomeStatusBarBeVisible`
→ `HomeStatusBarViewModelImpl.isSystemInfoVisible`
→ `systemInfoCombinedVis`
→ `HomeStatusBarViewBinderInjector`
→ `mEndSideContent = R.id.system_icons`.

`showEndSideContent()/hideEndSideContent()` owns the native alpha / visibility / translation transition of `system_icons`. The exact `system_icons` root is `MiuiStatusBatteryContainer`.

Current rules:
- Home Guiyuan renders in `MiuiStatusBatteryContainer(system_icons).overlay`, so native end-side alpha/visibility/translation apply naturally;
- Notification Header expansion remains useful motion evidence but is **not** a Guiyuan Home-visibility authority;
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
- Build 440 separates pre-compact **visual suppression** from compact **layout ownership**. A deferred QS_FAKE session immediately applies its existing reversible native clip masks while keeping the projected Combined overlay not-ready; Home remains the visual fallback until the next native status-icon measure/layout establishes compact slot exclusion, after which ownership hands off to the QS_FAKE Combined overlay. This avoids raw/mixed native visuals without exposing the stale-occupancy clip-only layout rejected in Build 431.
- Build 441 makes Hot Reload a **generation-to-generation presentation handoff** rather than a native fallback cycle. The old generation remains presentation owner until the new generation has installed its hooks and reaches the main-thread restore transaction; old visual/mask/reservation state is then released without an intermediate layout request, the new owner attaches in the same main-thread turn, and transferred QS_FAKE compact readiness may be adopted when it was already proven before reload. This removes the deliberate native/layout intermediate state that caused Home flashing, peer-icon reflow and immediate-pull blanking, while preserving normal detach/Fail-native restoration outside Hot Reload.
- when fake Battery is natively hidden, the compact session reserves the same stable Battery logical slot width used in the non-island case; exact-target SystemUI review verifies that Header-owned `batteryWidthDiff` is an **unscaled QS_FAKE-root translation term** paired with progress-scaled `normalControlStatusIconsTranslationX`, not a child-slot correction. Combined already inherits that parent motion, so `batteryWidthDiff` must never be applied a second time or converted into a project-owned 30 px compensation;
- no additional status-icon measure/layout/battery-hide Hook set, project alpha/visibility/translation writer, timer, polling loop, or competing native motion owner is permitted; one low-frequency Fake-root attach Hook plus a temporary root layout listener may own bootstrap/readiness because they follow the native host/layout lifetime, and that listener must be removed after success/final failure/detach;
- exact-target HyperOS and KeiMi 2.5.0 review allows a transition-only window-root overlay **only for Guiyuan Trinity**, because compact presentation replaces the native Wi-Fi/mobile/Battery visual correspondence. Its source matrix is sampled from the real role-5 Battery/carrier after the Header callback has applied native transforms;
- all other native status icons, including network speed, remain on SystemUI's own QS_FAKE/final surfaces. Guiyuan does not clip, redraw, pair or assign trajectories to them;
- fake/final icon membership remains native: QS_FAKE follows `RIGHT_BLOCK_LIST`, final QS follows `CONTROL_CENTER_BLOCK_LIST`. A slot absent from QS_FAKE but present in final QS is a native final-only participant and enters only through the final surface;
- transition Trinity keeps source tint fixed to the native fake/source state and follows the actual fake-root alpha. Expanded tint and final appearance are introduced only by the native final surface through `onAppearanceChanged()`;
- SystemUI remains the sole native motion/geometry/appearance/tint owner.

**Fully expanded endpoint — NATIVE_ONLY / verified**
- exact-target `ControlCenterHeaderExpandController$controlCenterCallback$1.onAppearanceChanged(appearance, animate)` owns the fake/final alpha handoff;
- `appearance=true` drives final `ControlCenterStatusBarIcon` alpha to 1 and `ControlCenterFakeStatusIcons` alpha to 0; `appearance=false` reverses that ownership;
- `onExpansionChanged(progress)` owns translation only and must not be repurposed as a project visibility threshold;
- Build-441 device diagnostics reach fraction 1.0 and observe the QS_FAKE root at alpha 0 before the return transition, matching the exact-target source contract;
- the accepted steady Guiyuan renderer remains attached to `ControlCenterFakeStatusIcons.overlay`; transition adaptation may additionally use a temporary **window-root overlay** only for the interval where native expansion progress is strictly between its endpoints;
- fully expanded ownership remains native-only. Build 478 removes role-6 native target masks entirely; final Wi-Fi/mobile/Battery and final-only status icons stay on the native final surface throughout the gesture;
- Build-455 device evidence still rejects overlay-local screen coordinates as a motion authority. Build 470-473 later confirmed the same boundary by failing with Home-source/RectF projection. Build 478 samples the Trinity source from the real role-5 Battery/carrier transform and uses role-6 matrices only as read-only Trinity targets;
- the native fake/final Folme contract remains the endpoint appearance authority. Guiyuan's Trinity overlay follows native expansion for geometry and fake-root alpha for visibility; it does not own a second handoff threshold, tint transition or gesture animator.

No polling/frame follower, native peer geometry write, second layout/suppression owner, or project-owned native alpha/translation/visibility writer is permitted.


## Keyguard and AOD

Build 456 is the current **device-accepted opt-in PROJECTED steady Keyguard implementation**. Build 455 proves the corrected AOD authority can reach steady Keyguard Guiyuan but is rejected for a shared Keyguard/QS_FAKE peer-layout/motion inconsistency caused by temporary ignored-slot state. Build 456 keeps AOD NATIVE_ONLY and makes Keyguard/QS_FAKE represented-slot exclusion session-scoped through the verified native container API; focused maintainer device validation accepted the resulting steady Keyguard and transition behavior.

Exact-target review underlying the accepted Keyguard adapter establishes:
- `MiuiKeyguardStatusBarView.mSystemIconsContainer` / `@id/system_icons_container` is the native Keyguard end-side `MiuiStatusBatteryContainer` registered into `ControlCenterFakeViewController.keyguardSystemIcons`;
- HyperOS itself selects `statusBarSystemIcons` for status-bar state 0 and `keyguardSystemIcons` for state 1, then feeds the selected `realSystemIcons` into Control Center Header geometry. Guiyuan must reuse that native router rather than duplicate it;
- Keyguard steady must use a **separate host/session adapter** from Home. Shared renderer/domain semantics are reusable, but mutable Home View/session ownership is not;
- `MiuiKeyguardStatusBarView.updateIconsAndTextColors()` is the native Keyguard tint authority and also forwards the same Keyguard tint semantics to QS_FAKE;
- the base Keyguard status-bar visibility lifecycle resets `mSystemIconsContainer` translation when hidden, while Keyguard-specific status-icon animations target the child `mStatusIconContainer`; these are distinct ownership layers and must not be collapsed;
- Build 442 observes only the steady Keyguard host/source identity through the already-installed Battery scene callback. It does not install Keyguard lifecycle/tint/AOD hooks and does not draw, hide, compact, reserve, or translate Keyguard content.

AOD remains a separate native-only surface and is not implied by Keyguard support. Exact-target `KeyguardStatusBarViewControllerInject.animateFullAod()` separately drives Battery alpha/AOD mode plus status-icon alpha/visibility/`setIsAodAnimate()`, proving that a steady Keyguard adapter cannot silently own AOD as a boolean sub-state.

Build 442 establishes the structural host/source boundary. Build 456 uses that boundary with a separate mutable Keyguard adapter and a default-off feature switch. The adapter reuses the existing class-wide status-icon presentation Hook substrate by exact View identity; it does not add a Keyguard lifecycle state machine. For transition-capable Keyguard/QS_FAKE containers, represented ignored slots remain present for the whole presentation session through native `addIgnoredSlots/setIgnoredSlots`, so native measure/layout and native motion observe the same slot-state fact. HyperOS still owns carrier visibility/alpha/translation and the shared Control Center source router.

The candidate deliberately separates steady-host identity from transition-router timing: `realSystemIcons` does not need to have switched to Keyguard before the steady Keyguard host can be resolved. Conversely, Keyguard-originated QS_FAKE is not permitted until the steady Keyguard adapter is actually ready.

AOD remains a separate gate. Build 456 retains Build 455's corrected native AOD-state observation Hooks and does not claim AOD alpha/visibility/translation/animation ownership. Any AOD leakage or failed restoration during Canary validation rejects the candidate rather than being patched with timing or alpha thresholds.

## Charging

Charging, quick charging, and super charging are render-state variants, not scenes.

They must not create a second scene geometry policy or a separate slot-width rule.

## Motion ownership

Unlocked steady currently uses `NONE`: Guiyuan has no independent motion requirement there. Its end-side visual inherits native `system_icons` motion when SystemUI transitions that carrier.

Notification Shade and all Control Center transition/destination motion stay under `SYSTEM_UI`; inheritance/projection does not transfer motion ownership to Guiyuan. Whether Guiyuan renders on a given verified carrier is a separate capability decision from who owns motion.

`COMBINED_STATUS` remains reserved for a future transition that is demonstrated to be genuinely owned by Guiyuan from start state through cleanup.

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
| Unlocked / Home | Guiyuan on verified Home carrier | Guiyuan transition bridge follows native HyperOS motion | Native SystemUI status bar only |
| Locked / Keyguard | Guiyuan on the verified opt-in Keyguard carrier | Guiyuan transition bridge follows native HyperOS motion from the Keyguard source | Native SystemUI status bar only |

Design consequences:
- source-scene ownership and transition ownership should be evaluated separately;
- Home and Keyguard use separate steady host/session adapters while reusing shared renderer/domain semantics; do not merge their mutable View/session ownership;
- a shared transition coordinator is a candidate only if source/runtime evidence supports it without creating a third state machine;
- the fully expanded Control Center endpoint is verified and accepted as native-only;
- reverse motion restores the correct source scene before bridge cleanup;
- no source adapter may infer the other source scene from Battery state, global Keyguard booleans, or timing.


### Build 455 AOD exclusion authority — correction of rejected Build 453

Steady Keyguard projection is not equivalent to AOD ownership. Device-rejected Build 453 attempted this gate but incorrectly resolved `toggleAodMode` as zero-argument, so its AOD authority installed zero Hooks and Keyguard failed native. Build 455 corrects the pinned contract: a unique `setIsAodAnimate(boolean): void` and `toggleAodMode(boolean): void` plus Boolean `mToAod` / `mIsAodAnimate` are required before Keyguard projection is allowed. `mToAod || mIsAodAnimate` blocks Keyguard projection and restores the native represented presentation. `mAnimToAod` is diagnostic-only.

If that contract cannot be resolved uniquely, Keyguard remains native while Home/QS_FAKE continues on the accepted Build-446 path. Guiyuan does not write AOD alpha, visibility, translation, animation or geometry.

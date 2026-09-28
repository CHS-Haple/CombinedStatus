# Combined Status Development Roadmap

This file stores future direction, phase boundaries, prerequisites, confirmed product design, and deferred/rejected routes. Current implementation state belongs in `CURRENT.md`; build-by-build investigation belongs in `DEVLOG.md`; release/net changes belong in `CHANGELOG.md`.

## Roadmap rules

- Keep macro phases stable unless new evidence changes sequencing.
- Do not move already-completed capabilities back into future work merely because they need regression testing.
- Distinguish **design confirmed** from **implementation complete**.
- Revalidate implementation details against the latest source, target SystemUI, dependency state, and device evidence before coding.
- Prefer verified HyperOS/SystemUI state, resources, layout, and motion ownership over duplicate project-local machinery.
- Record route reversals and invalidated approaches in `DEVLOG.md`; keep this file focused on future direction.

## Phase 1 — Core Home / native SystemUI foundation — completed

Established capabilities that later phases must preserve include:
- Home Combined Status rendering foundation;
- authoritative Wi-Fi/mobile/battery/domain state;
- single-SIM and dual-SIM presentation paths;
- hotspot, no-SIM, airplane and mobile-type semantics;
- native resource/tint reuse and bounded visual-intensity handling;
- native Wi-Fi/mobile/battery replacement with fail-native restoration;
- master switch and Hot Reload;
- charging/island ownership evidence required for the current carrier architecture.

Dual-SIM/network support and island participation are not separate future phases.

## Phase 2A — 0.0.2 Home carrier / presentation architecture — completed for current dev baseline

Stabilize one coherent Home/end-side presentation contract before extending the visual into other SystemUI surfaces.

Required direction:
- use a verified existing Home host/lifecycle rather than a permanent second status participant by default;
- keep one host-scoped owner for overlay/presentation/restoration state;
- resolve visual geometry and native occupancy as separate facts;
- use the live native battery-body carrier as the current stable replacement-width authority on the pinned target;
- inherit native charging/Super-Island host motion rather than copying Battery translation or creating a project-owned animator;
- keep native Battery scene/hide behavior read-only;
- make every temporary native exclusion/mask/reservation reversible and conflict-detecting;
- fail native when an ownership, compatibility, or restoration contract cannot be established.

Exit criteria:
- correct steady Home placement and optical spacing;
- correct normal and charging/Super-Island behavior;
- no duplicate native/project occupancy owner;
- no hidden persistent peer-geometry writer;
- cleanup and Hot Reload restore only module-owned state;
- current target behavior passes the declared focused device scenarios.

Current acceptance note: Build 408 is accepted for `dev` as the Phase-2A working baseline. Minor residual ring/center/dot optical-weight variance is deferred to later visual polish and does not reopen Home carrier ownership or block Phase 2B.

## Phase 2B — Home ownership continuity + Control Center transition bridge — completed for current dev baseline

Extend the accepted Phase-2A Home visual into panel transitions without reopening steady Home ownership.

Direction:
- treat steady Home geometry as the source contract;
- do **not** create a Notification-Shade Combined Status surface on the pinned target because the native Notification Shade does not expose the status-icon row there;
- keep Notification Shade native-only and inherit Home departure/return from the verified native `system_icons` end-side carrier lifecycle rather than maintaining a project-local shade visibility gate;
- Control Center is a **transition destination**, not a persistent Combined Status scene: exact-target source and Build-441 runtime evidence verify the bounded QS_FAKE bridge and native final-surface endpoint;
- during a partial pull, bridge the source Combined Status presentation through verified native Control Center transition geometry/progress so the gesture visually connects to HyperOS;
- fully expanded Control Center is native-only by verified HyperOS appearance ownership: native `appearance=true` fades the QS_FAKE root to alpha 0 while bringing final `ControlCenterStatusBarIcon` to alpha 1; Combined inherits QS_FAKE root alpha and never owns the final surface;
- use verified native transition progress/endpoints only for that bounded bridge lifetime;
- separate transition bridge lifetime/masking from the steady Home session;
- preserve native peer animation and Control Center geometry ownership;
- avoid first-frame shift, last-frame snap, duplicate occupancy, or a second animation system.

Accepted prerequisite: Build 413 closes the HUN/scene-lifetime boundary on the pinned target. Build 418 closes the shared Home Tint lifecycle blocker exposed while preparing panel projection.

Build-419 diagnostics plus maintainer clarification refine the remaining Phase-2B scope: Notification Shade is a **Home ownership boundary only**, while Control Center is the actual projection surface. The Build-419 bounded Notification-Shade probe is therefore retired rather than promoted into production.

Build 420 establishes useful Control Center source geometry and readiness-ordered handoff evidence. Build 428 now narrows that conclusion: `realSystemIcons` is the selected Home/Keyguard source reference, not the visible Control Center presentation, and the source container can be natively hidden throughout current Control Center ownership. Builds 425-427 reject placing the Combined visual carrier inside child `QS_FAKE.system_icon_area`. Build 430 device evidence validates the **top-level `ControlCenterFakeStatusIcons` presentation carrier** independently from the child Battery/system-icon-area hide lifecycle: the root owns native fake/final alpha while the child remains visible. Build 431 keeps that root host but is device-rejected for clip-only occupancy because represented Wi-Fi/mobile widths remain in native layout. Build 432 retains the root overlay and reuses the existing presentation Hook substrate for fake `MiuiStatusIconContainer` compact layout, but device evidence rejects its visible-time transient-session lifecycle: the same QS_FAKE surface can nondeterministically appear native, partially compact/masked, or Combined because native layout readiness may arrive after expansion/appearance. Build 433 moves QS_FAKE compact preparation out of the visible cycle and into the native `ControlCenterFakeStatusIcons` host lifetime, but cold-start evidence shows bare `onAttachedToWindow()` can still precede usable child/Battery geometry. Build 434 therefore keeps attach as the bootstrap lifetime boundary while establishing compact readiness only after the root's first native layout, with at most one additional native-layout retry for explicit early-geometry failures. Build-434 device diagnostics then show that prearm succeeds but a transient startup layout-unavailable event incorrectly tears the compact owner back down; Build 435 separates render-geometry readiness from compact-presentation lifetime so an attached, already-prepared QS_FAKE owner survives that transient loss. Build 441 closes Fake determinism and Hot Reload continuity. PR #160 closes charging-island trajectory as native QS_FAKE behavior. Exact-target endpoint review verifies native fake/final appearance handoff and the fully expanded native-only endpoint. Build 424 separately remains the accepted Home/Notification-Shade carrier correction.

Exit criteria:
- clean Home departure and return;
- coherent intermediate motion;
- clean partial-pull transition into Control Center and exact yield to native status icons at the fully expanded endpoint; Notification Shade remains native/no-status-icon by design;
- no regression in steady Home or charging/island behavior.

## Phase 3 — Keyguard / lockscreen / AOD scene completion — active

Build 446 is device-accepted and closes the late-eligibility QS_FAKE cutover prerequisite. Phase 3 may now advance to the independent steady Keyguard adapter; AOD remains a later, separate gate.

Build 446 preserves the source-scene gate and closes its late-entry lifecycle hole: when Home eligibility is learned after QS_FAKE has already completed native layout, the existing laid-out status-icons state may serve as the compact cutover proof only if no layout request is pending and geometry is valid. Otherwise the existing native `onLayout` path remains mandatory. This keeps the fail-native/atomic handoff contract while allowing scene-gated Home projection to become ready.

Build 445 tightens the Build-444 source-scene gate without changing its policy: HyperOS `realSystemIcons` remains the selected source endpoint, but Home is identified by object identity against the existing HomePresentationOwner carrier rather than transient View ancestry. Keyguard retains the already device-proven structural fallback. This preserves fail-native behavior while restoring Home-originated QS_FAKE.

Build 444 establishes source-scene eligibility for the shared QS_FAKE bridge before steady Keyguard rendering is enabled. It reuses the existing `CombinedStatusScenePolicy`: structurally verified steady source hosts can pre-seed the policy, and HyperOS `realSystemIcons` is the final Home/Keyguard authority when Control Center becomes visible. Home may project Combined Status; Keyguard/unknown fail native until the Keyguard capability is verified/promoted and its feature setting is enabled. The gate releases both overlay visibility and compact native masking together. The future lockscreen switch is therefore a feature-domain input to this same policy, not a separate QS_FAKE setting.

Build 443 inserts a transition-safety checkpoint before enabling steady Keyguard rendering: the shared QS_FAKE bridge must preserve native visuals until its existing native-layout boundary can atomically commit mask + Combined projection readiness. This keeps fail-native semantics intact for cold-start Keyguard-originated pulls. The later scene-policy layer remains explicit: Home steady and Keyguard steady are separate adapters sharing domain/render semantics, and QS_FAKE must inherit the native-selected source scene rather than act as an independent user-facing scene.

Reuse the stabilized domain state, renderer semantics, ownership rules, and fail-native behavior while giving each scene its own verified host/lifecycle adapter.

Build 442 is the first Phase-3 executable checkpoint and is intentionally narrower than a lifecycle adapter: it reuses the existing Battery scene callback only as a trigger, requires actual `MiuiKeyguardStatusBarView` ancestry, and takes one read-only steady-host/source snapshot. Hook delta is zero; no Keyguard rendering/suppression and no AOD runtime probe is introduced. Positive device evidence from this checkpoint is a prerequisite for choosing the later Keyguard presentation/lifecycle seam.

The unlocked side of the scene concept is now verified through Phase 2B. The locked side remains a planning hypothesis until exact Keyguard lifecycle review:
- **Unlocked concept:** Home steady Combined Status -> partial-pull transition continuity -> fully expanded Control Center native-only.
- **Locked concept:** Keyguard steady Combined Status -> partial-pull transition continuity -> fully expanded Control Center native-only.
- Notification Shade remains native-only on the pinned target.
- AOD keeps its own verified host/lifecycle contract and must not be inferred from either source scene.

Exact-target review should determine whether Home/Keyguard truly map to separate steady adapters plus one transition coordinator, or whether HyperOS exposes a cleaner shared lifecycle abstraction. Discuss that evidence before locking the model. Do not build a second lockscreen-specific state machine or revive historical motion/alignment patch chains.

## Phase 4 — App Home + Preview Sandbox — design confirmed, implementation planned

Primary navigation remains:

`Home | Features | Settings`

The Home page has two conceptual regions:

~~~text
┌─────────────────────────────────────┐
│ Combined Status                 ↻   │
│  Runtime Status + master switch    │
│                                     │
│  Preview Sandbox                   │
│  simulated Wi-Fi / mobile / SIM /  │
│  airplane / charging / battery     │
├─────────────────────────────────────┤
│       Home       Features   Settings│
└─────────────────────────────────────┘
~~~

Runtime Status reflects real module/SystemUI state and retains the real Hot Reload action. Preview Sandbox is simulation-only and must never mutate real Wi-Fi/mobile/SIM/airplane/charging/battery state.

Prefer reusing the real render semantics/model for previews rather than maintaining a second visually similar implementation.

## Phase 5 — Adaptive sizing, spacing and broader visual controls

After the carrier/scene contracts are stable:
- expose user-adjustable Combined Status visual size;
- derive neighboring spacing from resolved geometry rather than a permanent fixed-width assumption;
- keep native occupancy, visual width, transition geometry, and optical spacing independently resolved;
- expose per-state battery-ring color sources for NORMAL / CHARGING / POWER_SAVE / PERFORMANCE / LOW: **System default** (HyperOS semantic color where available), **Follow status icons** (native monochrome/tint authority), or **Custom color**, without creating a second battery-mode state machine;
- make future controls previewable in the Preview Sandbox.

## Phase 6 — 1.0.0 release qualification — final pre-release phase

The first planned formal release is **1.0.0**. Current `0.0.x` versions remain development lines until this qualification is complete and the maintainer explicitly authorizes the formal version transition.

Qualification includes the supported acceptance matrix, as applicable:
- Home steady behavior;
- shade / Control Center transitions;
- keyguard / lockscreen / AOD;
- charging / island states;
- single-SIM and dual-SIM states;
- Wi-Fi / hotspot / no-Internet / no-SIM / airplane combinations;
- master-switch disable/enable;
- Hot Reload and SystemUI recreation;
- light/dark/tint and battery semantic-color behavior;
- adaptive sizing/spacing boundaries intended for release;
- performance/energy and diagnostics boundaries;
- Release/signing/metadata checks;
- public documentation and notices consistency.

Completion of an earlier phase does not by itself advance the display version to `1.0.0`.

## Cross-cutting engineering routes

### Runtime ownership

When lifecycle responsibilities accumulate, move one bounded responsibility at a time into a dedicated owner/session. Keep one active writer for each mutable fact and preserve reversible cleanup.

### Native resource/state reuse

Prefer verified runtime resource identity and native semantic state. Do not maintain copied icon sets, duplicate mode-priority state machines, per-resource gray multipliers, or screenshot-derived compensation when the platform already exposes the needed meaning.

### Compatibility / diagnostics

Keep diagnostics event-driven and bounded. Compatibility-sensitive integration points require exact-target evidence and must fail native when their contract cannot be established.

## Deferred / rejected routes

- Permanent extra status participant as the default 0.0.2 Home carrier — superseded by the existing-host composition direction.
- Overriding native Battery hide/scene decisions to preserve project layout — rejected as a default ownership model.
- Following Battery translation per frame or creating a duplicate island animator — rejected.
- Magic translation/margin/padding/delay compensation without an ownership-level justification — rejected.
- Treating one fixed pixel width as the permanent source for future adaptive sizing — rejected.
- Reusing historical patches solely because they once improved one device symptom — rejected without revalidation.

## Update triggers

Update this file when:
- the project enters a new macro phase;
- a phase prerequisite or exit criterion changes;
- a confirmed future product design changes;
- an architecture decision changes future sequencing;
- the first formal release target or qualification boundary changes.

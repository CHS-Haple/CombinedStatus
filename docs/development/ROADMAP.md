# Combined Status Development Roadmap

This file stores the macro development sequence, confirmed-but-not-yet-implemented product design, active technical route, deferred work, trigger conditions, and design seams that must survive later implementation.

Current implementation state belongs in `CURRENT.md`; investigation/build history belongs in `DEVLOG.md`; stable user-facing net changes belong in `CHANGELOG.md`.

## Roadmap rules

- Read the **macro phase** first, then the active phase's technical route.
- Do not move an already completed capability back into the future roadmap merely because it needs later regression testing.
- Distinguish **design confirmed** from **implementation complete**.
- Revalidate implementation details against the latest source, SystemUI behavior, dependency state, and device evidence before coding.
- Preserve future-compatible seams when doing so is cheap and does not add speculative runtime machinery.
- Prefer native HyperOS/SystemUI behavior and resources when a verified contract exists.
- Record meaningful route reversals and invalidated approaches in `DEVLOG.md`.

## Macro development phases

### Phase 1 — Core state / renderer / native SystemUI foundation — completed

The project has already established the core Home implementation and the runtime contracts needed for later scenes:

- Home rendering foundation and native SystemUI integration evidence;
- authoritative Wi-Fi and mobile state/presentation;
- single-SIM and dual-SIM presentation paths;
- no-SIM, hotspot, airplane mode and mobile-type presentation;
- native resource/tint reuse and shared visual-intensity handling;
- native Wi-Fi/mobile/battery presentation suppression and fail-native restoration groundwork;
- master switch;
- Hot Reload;
- charging/island diagnostics and ownership evidence needed to select the 0.0.2 presentation carrier.

**Roadmap correction:** dual-SIM/network state/presentation support is not future work. The 0.0.2 redesign reopens only the Home/end-side presentation-carrier and scene-handoff architecture; it does not restart the domain model.

### Phase 2A — 0.0.2 Home carrier / presentation architecture — active

The 0.0.2 line is first stabilizing the Home/end-side carrier and presentation contract before continuing transition work.

Builds 378-393 remain evidence, not the required implementation. They demonstrated that a separate permanent status participant can satisfy individual boundaries but repeatedly couples:
- steady occupancy;
- native APPEAR geometry;
- battery-slot release;
- charging presentation width;
- peer MOVE targets;
- panel handoff anchors.

A completed architecture reference review is stored under `docs/reference/`. The strongest reusable patterns are:
- reuse an existing native host as the steady carrier when the target contract supports it;
- suppress represented native slot participation only within the native measure/layout scope, with exact restoration;
- keep native Views alive and mask only their drawing when appropriate;
- keep runtime state host-scoped;
- separate slot size, visual/glyph size, neighbor gap, per-glyph scale, and optical adjustment;
- treat native scene/hide state as authoritative input;
- keep cleanup/restoration explicit and layer-specific.

**Phase 2A target route:**
1. prove an exact-target Home carrier contract;
2. prove how represented native slots can be excluded without persistent duplicate layout ownership;
3. prove reversible visual masking/restoration while native state/tint/lifecycle remain alive;
4. define one shared `ResolvedLayout` / sizing contract that already supports future scale and optical gap;
5. prove an island-time carrier or draw-only handoff that keeps Combined Status network information visible when the steady battery-oriented host is unavailable;
6. keep unsupported scene states fail-native until their host adapters are verified.

The provisional approach that overrides the platform battery-hide request is rejected as a default architecture and has been removed before Build 394.

The permanent extra-participant route used in Builds 386-393 is **superseded as the default 0.0.2 architecture**. It remains historical evidence and may be reconsidered only if exact-target evidence later disproves the existing-host composition route and a new ownership review establishes a safer boundary.

**Phase 2A exit criteria:**
- correct steady Home placement and optical spacing in normal and charging states;
- one coherent effective end-side layout responsibility with no duplicate native occupancy;
- native Wi-Fi/mobile/battery state and tint sources remain live;
- clean native-compatible enable/disable presentation;
- charging/island enter, steady and exit preserve Combined Status network information without peer overlap or custom-only motion;
- no platform hide override;
- future user scale/gap changes require only resolved-layout inputs rather than scene-specific hooks or offsets;
- exact cleanup returns every native View/slot/overlay to its prior state;
- no duplicate geometry, layout, or animation writer.

### Phase 2B — Home -> shade / Control Center projection — next after Phase 2A

After the Home carrier/presentation contract is stable, complete cross-surface transition behavior without reopening steady-state ownership.

**Direction:**
- consume native expansion/transition progress as the timing authority;
- resolve real source and target endpoints from live SystemUI geometry;
- project Combined Status visual content through draw-only translate/scale/alpha where needed;
- keep native target Views and peer motion SystemUI-owned;
- separate transition masking/overlay lifetime from the steady Home HostSession;
- restore native visuals and temporary transition resources exactly on completion, cancellation, host replacement, or failure;
- do not introduce custom duration/interpolator systems, fixed endpoint offsets, or first/last-frame compensation.

**Phase 2B exit criteria:**
- no first-frame shift when leaving Home;
- no last-frame snap when returning Home;
- Control Center / shade endpoints match the live native targets;
- native progress remains authoritative in both directions;
- transition cleanup leaves no stale overlay, listener, mask, geometry, or host reference;
- steady Home geometry from Phase 2A remains unchanged by transition code.

### Phase 3 — Keyguard / lockscreen / AOD scene completion — next

Extend the stabilized state, rendering, ownership and transition contracts into remaining lockscreen-related scenes.

**Direction:**
- reuse the same domain state and rendering semantics instead of creating a second lockscreen-specific state machine;
- establish explicit host/session ownership for Keyguard and AOD;
- handle static vs dynamic scene handoff deliberately;
- cover charging and scene transitions without reviving historical motion/alignment patch chains;
- preserve fail-native behavior when a scene contract is not verified.

Existing keyguard groundwork or probes do not make this phase complete; completion requires a deliberate full-scene acceptance pass.

### Phase 4 — App Home + Preview Sandbox implementation — planned; design confirmed

The implementation is future work, but the high-level information architecture is **already decided** and must not be redesigned from scratch.

#### Primary navigation

`Home | Features | Settings`

- **Features** is the canonical second-tab name.
- Do not rename the second top-level tab back to **Customization**.
- Customization/visual controls belong inside the product capability structure rather than replacing the Features tab identity.

#### Home layout design snapshot

The Home page is vertically organized into two primary regions:

~~~text
┌─────────────────────────────────────┐
│ Combined Status                 ↻   │
│                          Hot Reload │
│                                     │
│  ┌───────────────────────────────┐  │
│  │        Runtime Status         │  │
│  │                               │  │
│  │    real Combined Status       │  │
│  │    runtime / connection       │  │
│  │    takeover / health state    │  │
│  │                               │  │
│  │               [ master switch]│  │
│  └───────────────────────────────┘  │
│                                     │
│  Preview Sandbox                    │
│  ┌───────────────────────────────┐  │
│  │    simulated Combined Status  │  │
│  │                               │  │
│  │ Wi-Fi / mobile / no-SIM       │  │
│  │ airplane / charging / battery │  │
│  │ and later visual parameters   │  │
│  └───────────────────────────────┘  │
│                                     │
├─────────────────────────────────────┤
│       Home       Features   Settings│
└─────────────────────────────────────┘
~~~

The schematic is an information-layout memory, not a pixel specification.

#### Runtime Status — top section

- Reflects **real** module/SystemUI state.
- Communicates whether the runtime connection/replacement path is healthy and whether Combined Status is active/taking over as expected.
- Contains the global Combined Status master switch.
- Retains a direct Hot Reload action backed by the real Modern Xposed Hot Reload implementation.
- Keeps detailed diagnostics out of the primary visual hierarchy; Home is a status surface, not a diagnostic dump.

#### Preview Sandbox — bottom section

- Represents **simulated** preview state only.
- Must never mutate real SystemUI, Wi-Fi, mobile, SIM, airplane, charging or battery state.
- Intended to preview combinations such as Wi-Fi, mobile network, no-SIM, airplane mode, charging and battery conditions.
- Later adaptive size/spacing/color controls should be observable here.
- Prefer reusing the real Combined Status rendering semantics/model rather than maintaining a visually similar but behaviorally separate preview renderer.

#### UI direction

- Continue using the project's MIUIX-based visual language and official component behavior.
- Preserve the already-decided information hierarchy when implementing the page.
- Fine pixel values may evolve with the pinned MIUIX version, but the two-region Home structure and three-tab information architecture are design constraints.

### Phase 5 — User-facing adaptive sizing, spacing and broader visual controls — planned

The **runtime sizing contract belongs to Phase 2A**, not Phase 5. By the time this phase begins, slot intent, visual/glyph size, optical gap, per-glyph scale, and transition endpoints should already resolve through the shared layout contract.

Phase 5 exposes those already-stable inputs to the user rather than redesigning SystemUI integration.

- expose user-adjustable Combined Status visual sizing;
- expose adaptive/optional user-adjustable neighbor spacing through the shared optical-gap model;
- keep slot intent, renderer visual width, transition geometry, and optical adjustment independently resolved;
- expose appropriate visual controls through the **Features** product structure;
- make changes inspectable in the Preview Sandbox without confusing preview-only state with real runtime state;
- require setting changes to alter resolved-layout inputs only, not introduce scene-specific hooks or offsets.

Existing color-link and proportional visual parameters are groundwork, not the final customization surface.

### Phase 6 — Full-system regression and 1.0.0 release qualification — final pre-release phase

The first planned formal release is **1.0.0**. Current `0.0.x` versions remain development lines until this qualification is complete and the maintainer explicitly authorizes the 1.0.0 version transition.

Run the complete acceptance matrix for the intended first-release scope, including as applicable:

- Home steady presentation and optical spacing;
- charging / Super Island enter, steady, and exit;
- shade / Control Center transitions and endpoint continuity;
- Keyguard / lockscreen / AOD within the declared 1.0.0 support scope;
- single-SIM and dual-SIM states;
- Wi-Fi / hotspot / no-Internet / no-SIM / airplane combinations;
- master-switch disable/enable;
- Hot Reload / SystemUI recreation and host replacement;
- light/dark/tint and native-resource intensity behavior;
- adaptive sizing / spacing and visual-control persistence;
- fail-native restoration and cleanup after cancellation/incompatibility;
- performance, wakeup, logging, and energy-use regression boundaries;
- Debug/Canary/Release behavior, Xposed metadata, signing, and non-debuggable Release properties;
- public README / changelog / notices / architecture / compatibility consistency.

**1.0.0 release gate:**
1. the intended support scope is explicit;
2. all required device/scene acceptance tests pass;
3. no known architecture path relies on superseded offsets, duplicate writers, or unresolved occupancy handoff;
4. release documentation and changelog are prepared;
5. formal Release validation/signing gates pass;
6. the maintainer explicitly approves changing the display version to `1.0.0`.

Completing an earlier architecture or feature phase does not by itself advance the display version to `1.0.0`.

## Active phase technical route

### Phase 2A / 0.0.2 — verify the Home carrier and presentation contract before Build 394

The next runtime build is intentionally blocked on target-specific architecture proof.

The reference-library review under `docs/reference/` establishes useful patterns but does not grant write ownership on the target SystemUI.

Before coding Build 394, verify:
- the exact Home native host that can carry Combined Status without creating a second permanent participant;
- the exact native measurement/layout boundary for temporary represented-slot suppression;
- reversible visual masking and restoration behavior;
- the platform battery-hide/island transition point and the carrier/handoff needed to keep network information visible;
- one shared `ResolvedLayout` contract for slot intent, visual/glyph size, optical gap, per-glyph scale, and future transition endpoints;
- host/session cleanup and fail-native restoration.

Builds 386-393 remain regression evidence. Their permanent extra-participant / 0-to-full-width occupancy-handoff route is superseded as the **default** 0.0.2 architecture. Do not recreate its fixed-boundary selection, translation compensation, or occupancy handoff merely to preserve previous work.

The first 0.0.2 Build 394 should test one coherent Phase-2A architecture slice and include diagnostics proving:
- one host/session owner;
- no duplicate slot occupancy;
- no platform hide override;
- no peer geometry writer;
- exact cleanup and restoration;
- sizing/layout decisions come from the shared resolved-layout contract.

Home -> shade / Control Center projection belongs to Phase 2B after this carrier contract passes.

## Cross-cutting engineering routes

### Runtime ownership migration

If long-lived lifecycle responsibilities accumulate in the bootstrap, move one bounded responsibility at a time into dedicated owner/session components. Do not wait for a separate refactor phase if an ownership threshold is crossed during feature work.

### Native resource reuse

New HyperOS/SystemUI visual resources continue through verified runtime resource identity, native semantic authority, and the shared tint/intensity contract rather than copied assets or per-resource visual magic numbers.

### Compatibility / diagnostics

Keep diagnostics event-driven and bounded. Maintain fail-native behavior and use exact SystemUI/runtime evidence when compatibility-sensitive integration points change.

## Deferred / rejected approaches

- **Force native battery-hide requests to remain visible for layout purposes:** rejected as the default 0.0.2 route. It takes ownership of a platform scene decision and is not supported by the completed reference review.
- **Permanent extra status participant as the default 0.0.2 carrier:** superseded. Builds 386-393 remain historical/runtime evidence, but new 0.0.2 work must start from the existing-host composition evaluation. Reconsider the participant route only if exact-target evidence later invalidates the preferred route and a new ownership review proves a safer contract.


- **Native battery slot + full-width Combined Status participant:** rejected; duplicate steady occupancy caused left shift.
- **Hide native battery layout + full-width participant:** rejected; diagnostics showed invalid Control Center anchor semantics and non-steady shift.
- **Zero-width participant without transition-geometry adaptation:** rejected; native APPEAR derives pivot from shell width and writes `pivotX=0`.
- **Build 384 pre-draw / repeated / per-frame pivot rewrites:** rejected; runtime proved the native writer occurs later, and racing it violates ownership/lightweight rules.
- **`HomeStatusBarViewBinderInjector.mBatteryContainer` as renderer wrapper:** rejected; exact `battery_digital_view.xml` proves it is battery-internal icon content with battery-specific alpha behavior.
- **Magic translation/margin/padding/delay compensation:** rejected unless future evidence proves no direct ownership fix is viable.

## Update triggers

Update this file when:
- the project moves to a new macro phase;
- an already-confirmed product design changes;
- the Phase-2A carrier/presentation architecture is selected or materially invalidated;
- Phase 2A completes and Phase 2B transition work begins;
- adaptive sizing gains or changes its validated runtime geometry contract;
- the intended 1.0.0 support/qualification scope changes;
- a cross-cutting ownership/compatibility boundary changes.

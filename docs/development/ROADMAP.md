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

### Phase 2 — Home -> shade / Control Center native transition — active

**Current direction:** the 0.0.2 line has reopened the concrete Home presentation-carrier architecture before another runtime checkpoint.

Builds 378-393 remain evidence, not the required implementation. They demonstrated that a separate custom status participant can satisfy some individual boundaries but repeatedly couples:
- steady occupancy;
- native APPEAR geometry;
- battery-slot release;
- charging presentation width;
- peer MOVE targets;
- panel handoff anchors.

A completed architecture reference review is now stored under `docs/reference/`. The strongest reusable patterns are:
- reuse an existing native host as the steady carrier when the target contract supports it;
- suppress duplicate native slot participation only within the native measure/layout scope, with exact restoration;
- keep native Views alive and mask only their drawing when appropriate;
- keep runtime state host-scoped;
- separate slot/glyph/gap/optical sizing;
- treat native scene/hide state as authoritative input;
- drive cross-surface projection from native progress and real endpoints rather than a project-owned timer.

**0.0.2 target route:**
1. prove an exact-target Home carrier contract;
2. prove how represented native slots can be excluded without persistent layout ownership;
3. prove an island-time carrier or projection that keeps Combined Status network information visible when the battery-oriented Home host is removed;
4. prove Home -> shade / Control Center source/target geometry from native endpoints and native expansion progress;
5. feed all scenes from one shared resolved-layout/sizing contract;
6. keep unsupported scenes fail-native until their host adapters are verified.

The provisional approach that overrides the platform battery-hide request is rejected as a default architecture and has been removed before Build 394.

**Exit criteria:**
- correct steady Home placement and optical spacing in normal and charging states;
- no duplicate native occupancy;
- clean native-compatible enable/disable presentation;
- charging/island enter, steady and exit preserve Combined Status network information without peer overlap or custom-only motion;
- no first/last-frame Home -> shade / Control Center shift;
- native transition progress remains authoritative;
- future user scale/gap changes require only resolved-layout inputs, not new hooks or offsets;
- exact cleanup returns every native View/slot/overlay to its prior state;
- no duplicate geometry or animation writer.

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

### Phase 5 — Adaptive sizing, spacing and broader visual controls — UI planned; runtime contract pulled forward

The user-facing controls remain future work, but the sizing/layout abstraction is now part of the 0.0.2 Phase-2 architecture so later UI does not force another SystemUI redesign. After the host/transition contract is stable across required scenes:

- expose user-adjustable Combined Status sizing;
- derive spacing from resolved visual geometry rather than a permanently fixed 105px assumption;
- keep native slot occupancy, renderer visual width, transition geometry and optical spacing independently resolved;
- expose appropriate visual controls through the **Features** product structure;
- make changes inspectable in the Preview Sandbox without confusing preview-only state with real runtime state.

Existing color-link and proportional visual parameters are groundwork, not the final customization surface.

### Phase 6 — Full-scene compatibility regression and 0.0.2 closure — final pre-release phase

Run the complete acceptance matrix across the supported target scope, including as applicable:

- Home;
- shade / Control Center transitions;
- Keyguard / lockscreen / AOD;
- charging / island states;
- single-SIM and dual-SIM states;
- Wi-Fi / hotspot / no-Internet / no-SIM / airplane combinations;
- master-switch disable/enable;
- Hot Reload / SystemUI recreation;
- light/dark/tint behavior;
- Release/Canary behavior and diagnostics boundaries.

Only after this phase is complete should the current `0.0.2` line be treated as release-ready.

## Active phase technical route

### 0.0.2 — verify the carrier / presentation / projection contract before Build 394

The next runtime build is intentionally blocked on target-specific architecture proof.

The reference-library review under `docs/reference/` establishes useful patterns but does not grant write ownership on the target SystemUI.

Before coding Build 394, verify:
- the exact Home native host that can carry Combined Status without creating a second permanent participant;
- the exact native measurement/layout boundary for temporary represented-slot suppression;
- reversible visual masking and restoration behavior;
- the platform battery-hide/island transition point and the carrier/projection needed to keep network information visible;
- Home -> shade / Control Center native progress and real target endpoints;
- one shared `ResolvedLayout` contract for slot intent, visual/glyph size, optical gap and transition endpoints.

Builds 386-393 remain regression evidence. Do not recreate their 0/full-width occupancy handoff, fixed boundary selection, or translation compensation merely to preserve the previous implementation.

The first 0.0.2 Build 394 should test one coherent architecture slice and include diagnostics proving:
- one host/session owner;
- no duplicate slot occupancy;
- no platform hide override;
- no peer geometry writer;
- exact cleanup;
- native progress authority for any transition under test.

## Cross-cutting engineering routes

### Runtime ownership migration

If long-lived lifecycle responsibilities accumulate in the bootstrap, move one bounded responsibility at a time into dedicated owner/session components. Do not wait for a separate refactor phase if an ownership threshold is crossed during feature work.

### Native resource reuse

New HyperOS/SystemUI visual resources continue through verified runtime resource identity, native semantic authority, and the shared tint/intensity contract rather than copied assets or per-resource visual magic numbers.

### Compatibility / diagnostics

Keep diagnostics event-driven and bounded. Maintain fail-native behavior and use exact SystemUI/runtime evidence when compatibility-sensitive integration points change.

## Deferred / rejected approaches

- **Force native battery-hide requests to remain visible for layout purposes:** rejected as the default 0.0.2 route. It takes ownership of a platform scene decision and is not supported by the completed reference review.
- **Treat one permanent extra status participant as the only possible final carrier:** reopened. Builds 386-393 are evidence, but the 0.0.2 design must compare an existing-host carrier before retaining this model.


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
- the 0.0.2 carrier/presentation architecture is selected or materially invalidated;
- adaptive sizing gains a validated dynamic geometry contract;
- a cross-cutting ownership/compatibility boundary changes.

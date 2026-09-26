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

### Phase 1 — Core Home / native SystemUI foundation — completed

The project has already established the core Home implementation and the runtime contracts needed for later scenes:

- native Combined Status participant / Home rendering;
- authoritative Wi-Fi and mobile state/presentation;
- single-SIM and dual-SIM presentation paths;
- no-SIM, hotspot, airplane mode and mobile-type presentation;
- native resource/tint reuse and shared visual-intensity handling;
- native Wi-Fi/mobile/battery suppression with fail-native restoration;
- master switch;
- Hot Reload;
- charging and island/end-side compatibility through the native SystemUI participant/slot/motion path.

**Roadmap correction:** dual-SIM/network support and island participation are not future standalone phases. They are completed baseline capabilities that later phases must preserve.

### Phase 2 — Home -> shade / Control Center native transition — active

**Current checkpoint:** Build 385 on PR #100.

The goal is to make Home steady state, panel entry, intermediate transition, fully expanded state, return transition, and final Home steady state one coherent native SystemUI path without:
- enable flash/reappearance;
- first/last-frame horizontal shift;
- steady duplicate occupancy/left shift;
- a second project-owned animation system.

The three-symptom cycle and Build 385 APPEAR geometry adapter belong to this phase.

**Exit criteria:**
- centered clean OFF -> ON APPEAR and ON -> OFF DISAPPEAR;
- correct steady Home placement;
- no first/last-frame panel-transition shift;
- native Control Center anchor semantics remain correct;
- charging/island behavior remains native-compatible;
- no duplicate geometry/animation writer;
- temporary diagnostics that no longer provide compatibility value are retired.

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

### Phase 5 — Adaptive sizing, spacing and broader visual controls — planned

After the native slot/transition contract is stable across required scenes:

- expose user-adjustable Combined Status sizing;
- derive spacing from resolved visual geometry rather than a permanently fixed 105px assumption;
- keep native slot occupancy, renderer visual width, transition geometry and optical spacing independently resolved;
- expose appropriate visual controls through the **Features** product structure;
- make changes inspectable in the Preview Sandbox without confusing preview-only state with real runtime state.

Existing color-link and proportional visual parameters are groundwork, not the final customization surface.

### Phase 6 — Full-scene compatibility regression and 0.0.1 closure — final pre-release phase

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

Only after this phase is complete should the current `0.0.1` line be treated as release-ready.

## Active phase technical route

### Build 385 — native slot + Combined Status transition-geometry adapter

Exact SystemUI source and Build 384 runtime evidence confirm why the previous 0px/105px fixes formed a cycle:

- the native battery slot must remain the single 105px end-side layout occupancy owner;
- a full-width custom status-icon participant therefore duplicates occupancy;
- a zero-width custom participant avoids duplicate occupancy, but HyperOS APPEAR normally computes its pivot from that zero View width.

Build 385 tested the smallest source-level pivot separation, but device validation rejected it: the pivot remains centered while the visible entry animation is still missing. The active route therefore moves one layer deeper, from pivot to **transition bounds vs layout occupancy**.

**Acceptance boundary:**
- clean centered OFF -> ON APPEAR;
- clean centered ON -> OFF DISAPPEAR;
- correct steady placement;
- no first/last-frame transition shift;
- native Control Center anchor remains 478+105;
- no repeated/per-frame project writer;
- exact callback incompatibility fails native.

Build 385 failed the visible-entry acceptance check. Reopen transition ownership at the render/bounds layer. The next implementation may proceed only if it gives the native APPEAR a real visual transition extent without making that extent a second steady layout slot. Do not add timing retries, repeated pivot writes, translation offsets, or duplicate layout occupancy.

## Cross-cutting engineering routes

### Runtime ownership migration

If long-lived lifecycle responsibilities accumulate in the bootstrap, move one bounded responsibility at a time into dedicated owner/session components. Do not wait for a separate refactor phase if an ownership threshold is crossed during feature work.

### Native resource reuse

New HyperOS/SystemUI visual resources continue through verified runtime resource identity, native semantic authority, and the shared tint/intensity contract rather than copied assets or per-resource visual magic numbers.

### Compatibility / diagnostics

Keep diagnostics event-driven and bounded. Maintain fail-native behavior and use exact SystemUI/runtime evidence when compatibility-sensitive integration points change.

## Deferred / rejected approaches

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
- Build 385 accepts or invalidates the active transition route;
- adaptive sizing gains a validated dynamic geometry contract;
- a cross-cutting ownership/compatibility boundary changes.

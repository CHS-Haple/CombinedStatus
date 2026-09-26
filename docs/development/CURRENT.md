# Current Development State

This file is the concise recovery point for active Combined Status development. Read it after `CONTRIBUTING.md` and before changing code. Keep detailed history in `DEVLOG.md`, reusable implementation evidence in `docs/reference/`, future/deferred product work in `ROADMAP.md`, and release-target semantics in `VERSIONING.md`.

## Repository baseline

- Last refreshed: 2026-09-27
- Stable branch: `main`
- Stable runtime baseline: Build 351, commit `2477867278483b76b80ed0884de3a07c7ede668a`
- Integration branch: `dev`
- Integration runtime baseline: Build 377, commit `f64fe0e3992eab4dd62ff479c3765d834ec7dfa4`
- Active work branch: `feat/native-panel-transition`
- Active PR: #100, `feat/native-panel-transition -> dev`
- Active development line: **0.0.2**
- First planned formal release target: **1.0.0** (current 0.0.x lines remain pre-release development)
- Last device-tested runtime checkpoint: Build 393 (`0.0.1`)
- Next runtime checkpoint: Build 394 (`0.0.2`) — **architecture gate open, scope defined, not yet built**
- Target profile: HyperOS SystemUI `17.03.260226.r`
- Exact SystemUI SHA-256: `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`
- Modern Xposed API: 102
- Application ID: `com.chaners.combinedstatus`

Documentation-only branch heads do not create a new runtime baseline.

## Current integration state

Build 377 remains the accepted `dev` runtime baseline. Builds 378-393 are work-branch evidence for Home/end-side/panel geometry and are not accepted integration architecture.

Build 393 is rejected on device:
- both tested attach orders remain visually wrong in charging state;
- when SystemUI starts while already charging, Combined Status begins with visibly excessive optical spacing from the nearest native status icon;
- pinning a custom participant to one stable boundary does not solve the underlying ownership conflict.

PR #100 remains blocked from `dev`.

PR #99 (`fix/native-visual-intensity-normalization`) remains open and unmerged. Shared visual-intensity normalization already exists in the accepted Build-377 line, so any future use of PR #99 must be reconciled against current `dev`.

## Macro roadmap position

The project is in **Phase 2A — 0.0.2 Home carrier / presentation architecture**. Home -> shade / Control Center projection is Phase 2B and intentionally waits until the Home carrier contract is stable.

The following capabilities remain reusable and are not being restarted:
- authoritative Wi-Fi/mobile/battery/domain state;
- single-SIM and dual-SIM presentation;
- hotspot, no-SIM, airplane and mobile-type semantics;
- native resource/tint integration and visual-intensity normalization;
- feature settings and master switch;
- Hot Reload and diagnostics;
- fail-native restoration principles.

What is reopened is the **geometry/presentation carrier**, not the domain model.

After Phase 2A, complete Phase 2B Home -> shade / Control Center projection. Keyguard / lockscreen / AOD remains the next macro phase after Phase 2B.

## 0.0.2 architecture boundary

The display version was explicitly advanced to **0.0.2** because the active work is now an architecture redesign rather than another Build-393 charging patch.

0.0.2 must establish one shared geometry/presentation contract for:
- native host/layout semantics;
- Combined Status visual geometry;
- optical spacing;
- future adaptive sizing;
- scene projection / transition geometry;
- host-scoped cleanup and fail-native restoration.

User-facing size/spacing controls remain a later UI task. Their runtime sizing contract is being pulled forward now so they do not require another SystemUI rewrite.

## Completed architecture reference review

A mature status-composition implementation was inspected as **reference evidence only**. No source code, assets, product identity, or implementation-specific naming is imported into this repository.

Reusable patterns are recorded in:
- `docs/reference/README.md`
- `docs/reference/statusbar-composition-patterns.md`

The review confirmed these reusable patterns:

1. **Existing-host composition.** A compact representation can reuse an existing native end-side host instead of adding a second permanent status-icon participant.
2. **Scoped slot suppression.** Represented native slots can be temporarily excluded only during native measure/layout, with an exact restoration token.
3. **Reversible visual masking.** Native Views can stay attached and state-capable while their drawing is temporarily clipped, then restored exactly.
4. **Host-scoped sessions.** Runtime presentation state, overlays, native references and cleanup belong to the concrete host lifetime.
5. **Sizing separation.** Slot size, glyph size, per-glyph scale and optical adjustment are independent inputs.
6. **Native scene authority.** Platform hide/scene semantics are consumed as facts rather than rewritten to preserve a custom host.
7. **Draw-only projection.** Native transition progress plus real source/target screen geometry can drive canvas translation/scale/alpha without taking ownership of native View translation.
8. **Layered cleanup.** Layout mutation, steady visual masking and transition projection have separate lifetimes and restoration paths.

## Architecture implication

The extra custom-participant route used by Builds 386-393 is **no longer assumed to be the final 0.0.2 architecture**.

The reference review explains why the existing route is fragile:
- the platform battery/end-side host has its own scene-dependent occupancy lifecycle;
- a separate custom participant creates a second layout identity;
- releasing the native battery region and then promoting the custom participant from zero to full width changes participant identity during the same scene transition;
- using one width/anchor to repair steady placement, APPEAR, island occupancy and panel handoff repeatedly couples responsibilities that should be independent.

The unfinished experiment that forced the native battery slot to remain present has been removed. The platform hide decision remains authoritative unless new exact-target evidence proves otherwise.

## Selected pre-runtime 0.0.2 direction

The next design review should evaluate:

`native host -> HostSession -> shared ResolvedLayout -> compact presentation`

with these candidate mechanics:

- reuse a verified existing Home end-side host as the steady layout carrier;
- keep native Wi-Fi/mobile/battery Views alive for state/tint/lifecycle;
- suppress duplicate native drawing reversibly;
- if target evidence allows it, exclude represented native slots only inside the platform-owned measure/layout scope and restore them immediately afterward;
- keep slot/glyph/gap/optical sizing in one shared resolved-layout model;
- when a steady host becomes unavailable because of a platform scene decision, switch presentation mode rather than forcing the host to remain;
- use native progress and real endpoints for Home -> shade / Control Center projection;
- give Keyguard and AOD their own HostAdapters while sharing domain state, renderer semantics and layout policy.

### Product-specific island requirement

A battery-oriented host may legitimately disappear during charging-island presentation, but Combined Status still carries network information.

Therefore 0.0.2 must **not** mechanically copy a policy that simply hides the whole compact representation whenever the battery host is hidden.

The target-specific design must prove one of:
- a valid island-time carrier for the Combined Status visual; or
- a draw-only island projection/handoff that preserves network information while native peer layout/motion stays SystemUI-owned.

This island carrier question is now closed for the pinned target: the verified `MiuiNotificationStatusContainer` overlay host is the exact `system_icon_area` animated by HyperOS `IslandStretchAnimation`, so the overlay inherits native island translation without its own follower or timing curve.

## Exact-target evidence added before Build 394

Build 393 diagnostics and the pinned SystemUI reference now narrow the Phase-2A carrier problem further:

- `MiuiNotificationStatusContainer`'s host overlay has already accepted the real Combined Status renderer anchored to the live battery descendant bounds with no native geometry writes and without inheriting the battery ancestor's visibility. This verifies a **Home attachment/lifecycle candidate**, not yet the final production carrier contract.
- Charging-island entry is confirmed to involve two distinct native responsibilities: `MiuiStatusIconContainer` changes its available/occupied width while the real `MiuiBatteryMeterView` independently translates and fades. The old permanent participant attempted to bridge both responsibilities with one custom slot identity, which is the ownership conflict 0.0.2 must remove.
- Exact-target APK method-body inspection now verifies `MiuiStatusIconContainer.ignoredSlots` plus public `addIgnoredSlots(...)` / `setIgnoredSlots(...)`: ignored slots are excluded from native measurement/layout and the add path requests layout. This closes the represented-slot layout-contract question without peer width/translation writes.
- `CombinedStatusHomeRenderSession` already demonstrates the desired host-scoped overlay lifetime and exact overlay removal boundary.
- **Clip-bound writer audit:** no Home status-bar Wi-Fi/mobile/battery implementation in the exact target APK was found writing `clipBounds`. A save -> empty-clip -> exact-restore mask is therefore the preferred non-competing visual-mask candidate for first runtime validation.
- The pre-runtime carrier/island contracts are now closed for the pinned target. Promotion now depends on Build 394 runtime proof of ignored-slot restoration, clip-mask coverage, carrier cutover, cleanup/fail-native restoration, Hot Reload, and focused device behavior.

The shared `ResolvedLayout` semantics are now defined at design level in `docs/architecture/layout-policy.md`. Source/runtime implementation is intentionally deferred so this documentation checkpoint does not create Build 394.



### Exact island-motion closure

JADX 1.5.6 method-body and decoded-resource inspection of the pinned target establishes:
- `translationFlow` carries the configured island translation endpoint, not live animation progress;
- `IslandStretchAnimation` owns the native Folme motion and writes `rightContainer.translationX`;
- Home binds `rightContainer` to `R.id.system_icon_area`;
- `status_bar.xml` declares that ID as `MiuiNotificationStatusContainer`, the existing Combined Status overlay host;
- `statusContainerSpace` is computed by `IslandMonitor.RealContainerIslandMonitor` as layout occupancy/overlap width and mirrored into other status-icon containers; it is not motion progress.

Consequently the selected Home overlay naturally rides the native island transform. Combined Status must not subscribe to either flow as a custom animation clock.

## Ownership / non-negotiable boundaries

- HyperOS remains authoritative for native peer layout, native scene state, native transition progress, and native live View motion.
- Combined Status owns its domain composition, its own drawing, resolved optical geometry, and only explicitly proven presentation/projection state.
- One live property must have one writer.
- No magic 105/135, 448/478 correction chain is architecture.
- No fixed translation offset, timing retry, custom duplicate scene animator, polling, or per-frame compensation may be added to preserve the old participant model.
- Any native layout mutation must be narrowly scoped, reversible, and proven against the exact target.
- Missing compatibility contracts fail native.

## Immediate next step

**Build 394 architecture gate is now open.**

Build 394 may now implement the first bounded 0.0.2 Home carrier checkpoint with this scope:

1. introduce the shared `ResolvedLayout` runtime contract;
2. make `MiuiNotificationStatusContainer` overlay the single active Home carrier;
3. use host-scoped represented-slot exclusion through the exact target `ignoredSlots` contract;
4. use reversible clip-only masking for represented native Wi-Fi/mobile/battery visuals;
5. preserve native HyperOS island motion by inheriting the animated `system_icon_area` host transform rather than following battery motion or writing translation;
6. explicitly cut over ownership so the superseded native-participant/suppression path cannot be active in the same Home session;
7. keep Home -> shade / Control Center projection, Keyguard and AOD out of Build 394;
8. validate cleanup, fail-native restoration, Hot Reload, normal Home, charging/island entry/exit and cold start while charging on device.

## Build 394 gate decision

The pre-runtime architecture gate is satisfied for the pinned target. Exact evidence now covers the Home overlay host/lifecycle, represented-slot measure/layout exclusion, non-competing clip-mask candidate, shared `ResolvedLayout` boundary, carrier ownership cutover requirement, and native island-motion inheritance through the animated `system_icon_area` host.

This is permission to create the first bounded runtime checkpoint, not proof that the runtime implementation is already correct. Build 394 must remain a single-variable architecture checkpoint and requires focused device validation before promotion.

## Reference priority for the next session

Read in this order:
1. latest `CONTRIBUTING.md`;
2. this `CURRENT.md`;
3. `docs/development/ROADMAP.md`;
4. `docs/reference/README.md`;
5. `docs/reference/statusbar-composition-patterns.md`;
6. recent `DEVLOG.md`;
7. exact target `SystemUI-Reference` findings as required.

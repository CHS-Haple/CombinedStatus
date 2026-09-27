# Status-bar composition and scene-projection patterns

## Purpose

This note preserves reusable architecture findings from a mature SystemUI status-composition implementation without importing its product identity, source code, assets, or implementation-specific naming.

The findings are useful because they address the same class of problems Combined Status must solve:

- replacing several native status visuals with one compact representation;
- avoiding duplicate layout occupancy;
- keeping native status Views alive for lifecycle/tint/state ownership;
- surviving platform-owned scene changes;
- transitioning between compact and native representations;
- supporting later size/spacing controls without coupling drawing size to platform slot geometry.

These patterns are **reference evidence**, not yet the production contract for Combined Status 0.0.2.

---

## 1. Existing-host composition instead of an extra persistent participant

**Observed.**

The inspected implementation does not create a second permanent status-icon participant for the compact representation. It reuses an existing native end-side host and its already-established layout lifecycle.

The compact visual is drawn from that host while the surrounding SystemUI layout remains authoritative.

### Reusable principle

When the platform already owns an appropriate end-side layout host, prefer:

`native host -> compact presentation`

over:

`native host + second custom participant -> occupancy reconciliation`.

This avoids creating two independent layout identities that must later exchange width, position, and animation ownership.

### Combined Status applicability

**Adopted for the current Phase-2A Home path on the pinned target.**

The earlier work-branch history showed repeated failure modes when a second permanent participant changed occupancy around native Battery-slot release. The current 0.0.2 Home path instead renders through the existing `MiuiNotificationStatusContainer / system_icon_area` host overlay.

This adoption is Home-specific. It does **not** establish that the same host can be reused for shade / Control Center, keyguard or AOD.

---

## 2. Scoped native slot suppression during native measure/layout

**Observed.**

When the compact representation stands in for native status items, the implementation temporarily adds only the represented native slots to the platform's existing ignored-slot collection.

The mutation is tightly scoped:

1. determine which represented slots are not already ignored by SystemUI;
2. add only those missing slots;
3. allow the native measure/layout call to run;
4. remove exactly the entries added by the module;
5. perform the same restoration on exceptional exit.

The implementation does not clear or replace the platform collection wholesale.

### Reusable principle

A layout mutation can be safe only when all of the following are true:

- it occurs at the platform-owned layout boundary;
- it is limited to the minimum represented slots;
- the pre-existing platform state is preserved;
- restoration is exact and unconditional;
- no persistent parallel layout state is created.

Prefer a restoration-token model over long-lived mutation.

### Combined Status applicability

**Adopted for Home on the pinned target.**

Exact-target inspection verified `MiuiStatusIconContainer.ignoredSlots` and its use by native measure/layout. The current Home session temporarily adds only represented slots around those native calls and restores exactly the entries it owned.

This remains fingerprint-scoped. Other SystemUI builds/scenes and unexpected competing state must be revalidated or fail native.

---

## 3. Reversible visual masking without changing native layout identity

**Observed.**

Native Views whose visuals are replaced remain attached. Their current clip bounds are saved once, an empty clip is applied while the compact representation is active, and the exact original clip is restored afterward.

The reference path does not need to use permanent `GONE`, alpha racing, or repeated translation writes to suppress those visuals.

### Reusable principle

When a native View must retain layout, lifecycle, tint, and state ownership but should not draw:

- prefer a reversible presentation mask;
- preserve the exact prior state;
- restore only module-owned changes;
- do not confuse visual suppression with layout removal.

### Combined Status applicability

**Adopted for the current Home path.**

Exact-target writer review found no competing Home Wi-Fi/mobile/Battery `clipBounds` writers in the directed target audit. The active Home session snapshots each native clip, applies an empty clip while replacement is ready, and restores only its own applied state.

The pattern remains scene- and target-scoped; later surfaces must repeat the writer/lifecycle review.

---

## 4. Host-scoped runtime state

**Observed.**

The implementation keeps an identity-based map from native host to a host-specific session/state object.

Each host state owns the runtime resources that belong to that host, including presentation geometry, native-view references, represented slots, overlay content, and cleanup responsibility.

Detached hosts are removed and cleaned rather than leaving their geometry or references globally reusable.

### Reusable principle

Prefer:

`Host -> HostSession -> owned resources`

and keep all host-derived geometry/state inside that session.

A host/session boundary should answer:

- what native host is authoritative;
- which native Views belong to it;
- which presentation mode is active;
- which temporary mutations are currently owned;
- how cleanup restores native state.

### Combined Status applicability

**Directly compatible with project rules.**

This should remain the lifecycle foundation for Home, future keyguard, and future AOD adapters even if their concrete hosts differ.

Multi-host awareness alone does **not** prove keyguard/AOD compatibility. Each target scene still needs explicit host mapping and device validation.

---

## 5. Slot size, glyph size, per-glyph scale, and optical adjustment are independent

**Observed.**

The inspected sizing model carries separate values for:

- layout slot size;
- visual/glyph size;
- per-glyph scale;
- optical adjustment.

User scaling resolves through this sizing model instead of rewriting hook behavior.

### Reusable principle

Do not use one width value for all of:

- native occupancy;
- renderer canvas size;
- actual visible glyph bounds;
- neighbor optical gap;
- transition endpoint.

A future user scale should feed one shared resolved-layout/sizing object. Scene adapters consume the result; they should not invent their own scale offsets.

### Combined Status applicability

**Required design direction for 0.0.2.**

This matches the planned adaptive size/spacing feature. The UI can remain deferred while the runtime geometry contract is established now.

---

## 6. Platform hide/scene semantics remain authoritative input

**Observed.**

The reference path reads the platform's native hide/scene state as an input to compact-presentation eligibility. No evidence was found of intercepting the native battery-hide setter to force the platform slot to remain present.

### Reusable principle

Do not preserve a host by fighting a platform-owned scene decision unless target evidence proves that ownership transfer is safe.

Prefer:

`native scene/hide fact -> presentation policy`

over:

`native hide request -> module rewrites request -> repair downstream geometry`.

### Combined Status applicability

**Important negative guidance.**

The project rejected both the earlier global battery-hide-preservation idea and the later narrow Build-395 layout-time hide override. The current Home path treats native Battery hide as a read-only fact and adjusts only its own verified replacement-space reservation.

Combined Status differs from a battery-only compact representation because network information must remain represented during charging-island behavior.

---

## 7. Native progress + real endpoints for scene projection

**Observed.**

Scene transition progress is consumed from an existing native expansion callback.

For notification-shade state, Android SystemUI's `ShadeExpansionStateManager` documents `expanded` as independent from the numeric expansion fraction and `tracking` as active gesture ownership. Its closed-state transition occurs only after the panel is no longer expanded and user tracking has ended. The pinned HyperOS target exposes the same three semantic inputs through the already-verified `onPanelExpansionChanged(float, boolean, boolean)` hook contract.

Combined Status consequence: static unlocked status-bar state and numeric `fraction == 0` are not, by themselves, proof that Home presentation has regained ownership. Treat the native expanded/tracking facts as the first scene-lifetime boundary; preserve fraction for native-progress projection rather than inventing a local threshold.

Exact-target device diagnostics also verify a separate Control Center lifetime contract on `com.miui.systemui.controlcenter.container.ControlCenterExpandControllerDelegate`:
- `onVisibleChanged(boolean)` brackets Control Center ownership and remains true throughout the outward/return transition;
- `onExpansionChanged(float)` supplies native progress but is not required to decide whether Home owns the scene.

Combined Status consequence: compose the semantic visibility callback into Home eligibility; keep Control Center fraction for diagnostics/future projection and do not invent a fraction threshold.

Projection endpoints are derived from actual View screen coordinates. The visual transition is then drawn using canvas translation/scale/alpha rather than by taking ownership of the native Views' live translation.

The projection layer is updated only when relevant source/target/progress state changes.

### Reusable principle

For a transition between compact and native representations:

`real source geometry + real target geometry + native progress -> draw-only projection`

is preferable to:

`fixed offset + custom duration + independent interpolator`.

### Combined Status applicability

**Strong candidate for Home -> shade / Control Center.**

This aligns with the project requirement that SystemUI own transition timing and target placement while Combined Status owns only its composed visual projection.

For the current Home charging/Super-Island path, a separate projection is unnecessary: exact-target review shows the Combined Status overlay already rides the native animated `system_icon_area` host. The real-endpoint/native-progress principle remains the preferred candidate for Phase 2B Home -> shade / Control Center projection.

---

## 8. Transition masking and cleanup are separate from steady layout ownership

**Observed.**

The transition path has its own overlay/presentation lifetime. Native target Views can be temporarily masked while their real layout remains intact. At transition end or failure:

- overlay content is removed;
- temporary listeners are removed;
- original native visual state is restored;
- projection tracking state is cleared.

This cleanup is separate from steady compact-host cleanup.

### Reusable principle

Keep three lifetimes separate:

1. steady host/session lifetime;
2. scoped native layout-mutation lifetime;
3. scene-projection lifetime.

Do not use a single global flag or View width to stand in for all three.

---

## 9. Cleanup is a first-class compatibility contract

**Observed.**

Host cleanup removes owned overlay content, restores tracked native presentation state, clears transient compact state, and requests native relayout where necessary.

Global cleanup also removes transition overlays/listeners and clears host tracking.

### Reusable principle

A replacement architecture is incomplete until it can reliably return to untouched native behavior after:

- feature disable;
- host replacement;
- SystemUI recreation;
- Hot Reload;
- transition cancellation;
- reflection/hook incompatibility;
- unexpected runtime failure.

Fail-native cleanup should restore the exact prior state, not merely set a guessed default.

---

## 10. What the reference evidence does not establish

The following remain **not established** for Combined Status:

- that one Home host can also be reused for keyguard or AOD;
- that the target SystemUI exposes identical ignored-slot behavior in every scene;
- that compact presentation should disappear whenever the native battery host disappears;
- that the reference scene policy is appropriate for Combined Status network semantics;
- that the exact sizing constants or source implementation details should be copied;
- that an overlay alone is sufficient for every native APPEAR/DISAPPEAR requirement;
- that any private/obfuscated implementation identifier is a stable platform contract.

These must be verified independently.

---

## 11. 0.0.2 architecture / regression checklist

Use this checklist for current Home regression review and before promoting the same ideas into a new scene or target.

### Steady Home

- exactly one effective end-side layout responsibility;
- no duplicate Wi-Fi/mobile/battery occupancy;
- native state/tint sources remain live;
- Combined Status visual masking is reversible;
- scale/spacing derives from one resolved layout.

### Charging / island

- native battery hide remains platform-owned unless new target evidence proves otherwise;
- no 0 -> full-width participant identity switch;
- network information remains continuously represented;
- island entry/exit uses a verified carrier or projection;
- native peers keep platform-owned motion.

### Shade / Control Center

- use native expansion/progress authority;
- resolve real source/target endpoints;
- no custom timing system;
- no first/last-frame compensation offsets;
- transition cleanup restores native visuals.

### Keyguard / AOD

- separate host adapters;
- same domain state and sizing policy;
- explicit lifecycle and fallback;
- do not infer support from Home host behavior.

### Future size / spacing

- setting changes sizing/layout inputs only;
- no scene-specific hook rewrite;
- slot/glyph/gap/optical values stay independent;
- larger visuals request only geometry that the verified host contract can safely own.

---

## 12. Architecture implication for the current work branch

The reference evidence successfully redirected the project away from the permanent extra-participant / occupancy-handoff route.

Current target-specific status:
- existing-host Home composition is implemented;
- scoped represented-slot exclusion and reversible clip masking are implemented;
- HostSession-scoped cleanup/fail-native boundaries are implemented;
- native charging/Super-Island motion is inherited from `system_icon_area`;
- Build 397 device validation accepted the corrected charging-carrier behavior;
- Build 398 strengthens the stable width source to the live `battery_icon_container`;
- Build 399 is a painter-only battery-intensity checkpoint and does not reopen these architecture decisions.

Builds 386-396 remain useful historical evidence but are not the current design premise.

The reference library should continue to guide Phase 2B and later scene work at the level of ownership, lifecycle, restoration and projection patterns. Exact target contracts must still be proven independently before new writes are introduced.


## 13. Retained state carrier is not visible Tint authority

Build-416 Hot Reload device evidence adds a presentation-ownership distinction to the existing masking pattern.

A represented native status View may remain attached, measured, event-driven and tint-capable because Combined Status deliberately preserves its SystemUI lifecycle. That does **not** make the represented View authoritative for the tint of what the user currently sees after its visual has been replaced/masked.

For Home monochrome tint resolution:
- represented Combined Status slots (`wifi`, `mobile`, `stacked_mobile`, `airplane`, `no_sim`, and any future `combined_status` participant itself) are state/lifecycle carriers, not visible peer/anchor candidates;
- prefer a genuinely visible, non-represented Home peer as the location-aware `DarkIconDispatcher.getTint(...)` anchor and static/applied tint peer;
- require the peer to be visible with positive layout geometry;
- when no eligible visible peer exists, fall back to the native manager/global/cached authority rather than forcing a dark-mode refresh or inventing a color.

This rule is especially important across module Hot Reload: represented native Views can temporarily retain presentation state that a full SystemUI recreation would rebuild, while neighboring visible SystemUI icons already reflect the current surface tint.


## 14. Tint event trigger and visible Tint authority must share one snapshot

Builds 415-417 establish that Home monochrome Tint has two distinct concepts that must not be conflated:

- a **native event trigger** such as Battery `onDarkChangedInternal`;
- the **visible Home status-icon authority** used to decide the monochrome direction of the composition.

A trigger may be timely while a cached authority value is stale. Likewise, a valid status-icon authority observation can occur before a new renderer generation is attached. Combining values from different event generations produces a mixed-scene snapshot even when each individual value is valid.

For Combined Status:
- resolve the current visible Home status-icon Tint synchronously when committing a Battery-triggered renderer Tint update;
- a status-icon observation updates only status-icon authority and must not replay unrelated Battery state;
- after Hot Reload, transfer is a temporary visual-continuity seed only; fresh new-generation native authority supersedes transferred status Tint before visible ownership begins;
- if live status-icon authority is unavailable, fail toward the current native Battery applied tint rather than reuse a stale embedded status-icon field;
- do not add polling, delayed retries, forced DarkIcon refreshes, or a second native Tint writer to compensate for ordering.

The exact-target SystemUI-Reference currently verifies the Home host/status-icon/Battery/scene contracts used by this path, but does **not** establish a stable `DarkIconDispatcher.addDarkReceiver/removeDarkReceiver` registration contract for this artifact. Therefore direct receiver registration is not introduced without separate DEX/runtime verification.


### Build-418 acceptance evidence

Build 418 device validation accepts the snapshot rule above on the pinned HyperOS target.

Observed in the accepted Detailed session:
- Hot Reload restores the Home host without a SystemUI restart and the new generation resolves a visible non-represented status-icon anchor;
- the renderer begins with matching `appliedTint` and `statusIconTint`;
- across repeated native dark/light transitions, `appliedTint`, `statusIconTint`, and the live SystemUI status-icon authority advance together through the same intermediate values;
- repeated entry/exit no longer reproduces the stale white/black inversion seen in Builds 415-417.

This supports the ownership rule that a native event may trigger a renderer update, but the visible status-icon authority must be resolved for the same commit generation. It does not justify a second color writer, polling, delayed retry, or direct DarkIcon registration on an unverified contract.


## 15. Notification-shade target evidence from Build 414

The completed Build-414 bounded probe is historical evidence for Phase 2B; the probe itself is retired from the active runtime after Build 418 integration.

Observed on the pinned target:
- `NotificationHeaderExpandController$notificationCallback$1.onExpansionChanged(float)` follows the native notification-header expansion path;
- captures at native boundary buckets 0/1/7/8 did **not** expose a direct status-icon target View from the controller;
- the only direct Android `View` discovered on the controller was `realClockIcons`;
- controller-level `notificationTranslationX=2` and `notificationTranslationY=-109` remained stable in those captures and therefore are not sufficient by themselves to define Combined Status target geometry;
- the one-time controller field inventory exposed narrower ownership seams including `headerController: Lazy` and `notification: NotificationShadeWrapper`.

### Consequence

Do not interpolate Combined Status toward `realClockIcons` or the two controller translation scalars. They are evidence about the header controller, not a verified status-icon projection endpoint.

The next notification-shade review should follow the controller/wrapper ownership chain and identify:
1. the actual status-icon/native-header surface that owns target layout;
2. its target bounds/location at native endpoints;
3. its native tint authority;
4. the lifecycle boundary for attaching/removing a draw-only Combined Status projection.

Only if exact-target source/reference review cannot resolve one of those facts should a new bounded runtime diagnostic be added. A completed diagnostic hook should not remain resident after its evidence is captured.

---

## 16. Build-420/421 panel ownership correction — host lifecycle over Battery scene inference

Later Phase-2B device evidence supersedes the open Notification-Shade continuation described in section 15.

### Verified target behavior

- The pinned Notification Shade does not expose/present the status-icon row as a Combined Status projection target; it remains native-only.
- Build 420 device validation accepts Control Center projection through the native `realSystemIcons` / `MiuiStatusBatteryContainer` carrier and its readiness-ordered handoff.
- Build 421 device diagnostics show `MiuiBatteryMeterView.updateState()` can emit raw status-bar state `1` at a Notification-Shade boundary while `KeyguardManager.isKeyguardLocked` is also `true`.
- That Battery/global-Keyguard combination fires before the verified shade-fraction owner and therefore cannot be used as a second Home-visibility authority.
- Home Combined Status is already drawn in `MiuiNotificationStatusContainer.overlay`; Android's overlay contract makes it a visual layer of that host rather than an independent global surface.

### Reusable principle

Do not reconstruct a global scene state machine from a retained native presentation carrier when the real host and panel owners already expose their lifecycles.

For the current target:
1. **Home surface drawing:** native `MiuiNotificationStatusContainer / system_icon_area` HostSession.
2. **Notification-Shade handoff:** native `ShadeExpansionStateManager` fraction boundary only.
3. **Control Center:** verified projected carrier + coordinator handoff.
4. **Battery status state:** read-only presentation/tint event context; not Home visibility.
5. **Keyguard/AOD:** separate native hosts/adapters; no inference from Home Battery state.

This removes a competing writer instead of refining it with another boolean. It also keeps future Keyguard/AOD support explicit and host-scoped rather than coupling those scenes to Home's Battery presentation internals.

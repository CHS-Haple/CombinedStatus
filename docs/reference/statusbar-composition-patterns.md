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

**Candidate for Combined Status.**

The earlier work-branch history, now being reevaluated in the 0.0.2 development line, shows repeated failure modes when a custom participant alternates between zero and non-zero occupancy around native battery-slot release. An existing-host composition path should therefore be evaluated before extending that participant model further.

This does **not** prove that the exact native battery host is sufficient for every Combined Status scene. Combined Status carries network state and may need a different presentation carrier when that host is temporarily removed by platform scene policy.

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

**Candidate for Combined Status.**

If 0.0.2 reuses an existing host, this pattern may allow native Wi-Fi/mobile items to stop consuming duplicate layout space while their Views remain alive.

Target verification is still required for:
- the exact ignored-slot owner;
- call ordering;
- behavior with other modules;
- host replacement;
- island/shade/Control Center paths.

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

**Strong candidate.**

Combined Status already benefits from keeping native Wi-Fi/mobile/battery state sources alive. A reversible visual mask can preserve that ownership while preventing duplicate drawing.

Any adopted mask must still be checked against accessibility, hit testing, clipping by ancestors, hardware layers, and transition rendering.

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

The provisional experiment that forced the native battery layout slot to remain present is not supported by this reference pattern and has been removed before the first 0.0.2 runtime checkpoint.

Combined Status differs from a battery-only compact representation because it must preserve network information during charging-island behavior. Therefore the correct response may be a different presentation carrier or projection for that scene, not simply hiding the whole Combined Status visual.

---

## 7. Native progress + real endpoints for scene projection

**Observed.**

Scene transition progress is consumed from an existing native expansion callback.

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

The same principle may be useful for an island-specific presentation carrier, but the exact source/target hosts and progress authority must be proven on the target before implementation.

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

## 11. 0.0.2 architecture evaluation checklist

Before the first 0.0.2 runtime checkpoint, evaluate a target-specific design against this checklist.

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

The reference evidence triggered a reevaluation of the extra-participant integration choice. Subsequent exact-target review has now **superseded the permanent extra-participant route as the default 0.0.2 architecture** and selected the existing-host composition direction for the first runtime checkpoint.

Builds 386-393 remain valuable evidence about:
- native APPEAR requirements;
- battery-slot release;
- peer occupancy;
- stable vs transient geometry;
- panel-anchor semantics.

They should not be treated as proof that the final 0.0.2 architecture must retain the same custom-participant ownership model.

That exact-target verification is now complete for the pre-runtime Home carrier gate on the pinned target. Build 394 is the first runtime validation of the existing-host composition path, represented-slot restoration, reversible masking, carrier cutover, and inherited native island motion. No new offset, forced battery-hide override, or occupancy handoff should be added merely to preserve the previous implementation.

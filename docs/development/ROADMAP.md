# Combined Status Development Roadmap

This file stores future directions, deferred work, trigger conditions, prerequisites, and design seams that should be preserved for later work. It is not a promise that every item will be implemented.

Current behavior belongs in `CURRENT.md`; investigation/build history belongs in `DEVLOG.md`; stable user-facing net changes belong in `CHANGELOG.md`.

## Roadmap rules

- Revalidate every roadmap item against the latest source, SystemUI behavior, dependency state, and device evidence before implementation.
- Do not turn an old plan into code merely because it was once discussed.
- Record the trigger or prerequisite for deferred work whenever known.
- Preserve future-compatible seams when doing so is cheap and does not add speculative runtime machinery.
- Prefer native HyperOS/SystemUI behavior and resources where a verified contract exists.
- Delete or revise roadmap items when evidence makes them obsolete; record a meaningful reversal in `DEVLOG.md`.

## Active near-term route

### Native end-side ownership / three-symptom cycle

The active route is to stop treating status-icon shell width as the sole control for Combined Status placement and motion.

**Evidence already established by Build 384:**
- the one-shot Combined Status pivot bridge is overwritten during native APPEAR and is not a viable final animation owner;
- bounded `homeMotion` evidence shows `mBatteryContainer` and `mBatteryView` remain co-anchored and move together through the sampled end-side motion;
- the battery-wrapper/end-side path is therefore strengthened as a candidate, but not yet accepted.

**Remaining prerequisites:**
- source-level identification of the exact native APPEAR pivot writer;
- verification of battery-wrapper / end-side alpha and visibility semantics across island and privacy states;
- confirmation that a renderer under the selected native layer preserves Combined Status network visibility when native battery presentation is intentionally hidden.

**Preferred direction if evidence confirms the native battery wrapper/container as a stable motion/slot owner:**
- keep exactly one native end-side occupancy owner;
- place Combined Status rendering in the verified battery-slot/motion layer rather than reserving a second ordinary status-icon width;
- preserve the real `MiuiBatteryMeterView` lifecycle/state/tint role unless evidence requires otherwise;
- retire or narrow the custom bindable participant so it no longer simultaneously owns slot geometry and animation geometry;
- retain fail-native restoration and avoid peer geometry writes.

**Alternative if wrapper ownership is disproven:**
- continue source-level ownership investigation before adding compensation;
- do not fall back to a permanent 0px/105px shell toggle, translation patch, margin patch, or delay-based handoff.

**Acceptance boundary:**
- clean enable/disable transition;
- correct steady position;
- no first/last-frame transition shift;
- charging/island/privacy behavior remains native-compatible;
- no duplicate live geometry writer;
- no polling, persistent per-frame logging, or repeated View-tree traversal.

## Known future design themes to revalidate

### Adaptive sizing and spacing

Longer-term presentation may allow user-adjustable Combined Status visual size with spacing derived from the resolved visual geometry rather than a permanently fixed slot assumption.

**Design seam to preserve:** native end-side slot ownership, Combined Status drawing geometry, transition geometry, and optical spacing must remain separate. A future size control must not require reviving duplicate native slot occupancy.

**Prerequisite:** the active native end-side ownership work must first establish a single stable slot/motion contract.

### Dual-SIM behavior

Future mobile presentation may need explicit dual-SIM semantics beyond the currently validated single/selected-source behavior.

**Design seam to preserve:** mobile state acquisition and presentation policy should not hard-wire one transient View topology as the permanent domain model.

### Island / SystemUI transition participation

Combined Status should continue to align with native SystemUI scene/transition behavior rather than implementing a parallel animation authority.

**Design seam to preserve:** stable geometry and transition geometry remain separate, and native visibility/scene/motion ownership should be reused when verifiable.

The current native end-side ownership investigation is now a prerequisite for this theme.

### Runtime ownership migration

If long-lived lifecycle responsibilities begin accumulating again in the module bootstrap, move bounded ownership into dedicated session/owner components one responsibility at a time.

**Trigger:** a responsibility becomes independently ownable, testable, and removable, or the bootstrap begins retaining lifecycle state that violates the ownership rules.

The current transition work should prefer a dedicated end-side rendering/motion owner over adding more state to `CombinedStatusModule`.

### Native resource reuse

New HyperOS/SystemUI visual resources should be integrated through verified runtime resource identity and the shared tint/intensity contract rather than copied or manually gray-matched.

**Design seam to preserve:** resource resolution, semantic state, tint authority, and rendering normalization remain independently testable.

## Deferred / rejected approaches

- **Permanent duplicate occupancy (native battery slot + full-width ordinary Combined Status participant): rejected for the current target.** Device evidence shows it can correct one transition path while shifting steady placement.
- **Permanent zero-width ordinary participant as the complete architecture: deferred/rejected as a final design.** It avoids duplicate occupancy but leaves animation geometry dependent on a shell with no real width.
- **Magic translation/margin/padding/delay compensation: rejected unless later source evidence proves no direct ownership fix is viable.**
- **Treating Build 384 pivot normalization as final architecture: rejected.** Runtime evidence shows HyperOS overwrites the one-shot pivot during APPEAR; racing that writer with repeated or per-frame project writes would violate the ownership and lightweight rules.

## Update trigger

Update this file when:
- source-level review identifies the native APPEAR geometry writer and resolves the battery-wrapper/end-side ownership decision;
- a renderer/owner migration becomes the selected implementation route;
- the bindable participant is narrowed or retired;
- adaptive sizing gains a verified slot-width contract;
- a planned direction, prerequisite, trigger, or intentionally reserved design boundary changes.

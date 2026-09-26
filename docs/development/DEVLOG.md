# Combined Status Development Log

This is the chronological engineering diary for Combined Status. It complements, but does not replace, `CHANGELOG.md`, pull-request history, diagnostics, or CI artifacts.

## Entry requirements

For each engineering checkpoint, record the problem/goal, observed evidence, analysis, root-cause status, references consulted, alternatives, implementation, review, CI/build identity, validation/device feedback, result, durable conclusions, residual risk, and future-design consequences as applicable.

Use explicit confidence labels when root cause is not proven:

- **Confirmed** — directly supported by source/runtime evidence.
- **High confidence** — evidence strongly supports the conclusion but one relevant uncertainty remains.
- **Hypothesis** — plausible and testable, not yet established.

Preserve failed hypotheses and append corrections. Do not rewrite history to hide an invalidated path. Historical entries before this log was introduced may be backfilled only from verifiable evidence.

---

## 2026-09-26 — Development-memory system initialized

**Type:** repository documentation / engineering governance  
**APK build:** none  
**Runtime impact:** none

### Problem / objective

Combined Status development had accumulated important implementation reasoning, CI/build results, device feedback, architectural conclusions, and future plans across conversations and transient context. A new development session could therefore recover an incomplete picture or repeat an already-invalidated investigation.

The objective is to make the repository itself the durable engineering memory.

### Analysis

The existing repository already defines strong root-cause, evidence, ownership, review, CI, validation, and change-record rules. It also deliberately keeps `CHANGELOG.md` focused on durable net project state rather than failed hypotheses or intermediate experiments.

That leaves a legitimate gap: there was no concise current-state recovery file, no chronological engineering diary for Build/CI decisions, and no dedicated place for deferred roadmap/design-preparation information.

### Root cause

**Confirmed:** development continuity relied too heavily on conversation context and scattered Git/CI/diagnostic evidence because the repository had no dedicated development-memory layer.

### Evidence / references consulted

- Latest `CONTRIBUTING.md`, especially:
  - root-cause-first investigation and evidence-driven solution changes;
  - repository-text/governance routing;
  - changelog scope excluding a development diary;
  - checkpoint-based CI/device validation;
  - definition-of-done and concise change-record requirements.
- Current repository branch state at initialization:
  - `main`: Build 351 stable baseline.
  - `dev`: Build 377 integration baseline.

### Alternatives considered

1. **One ever-growing DEVLOG only** — rejected because every new session would eventually need to scan too much history.
2. **Conversation memory only** — rejected because it is not an auditable repository source of truth.
3. **Three-layer repository memory** — selected:
   - small current-state recovery document;
   - complete chronological development diary;
   - separate future/deferred roadmap.

### Measures implemented

- Added `docs/development/CURRENT.md`.
- Added `docs/development/DEVLOG.md`.
- Added `docs/development/ROADMAP.md`.
- Added mandatory startup-read and development-log rules to `CONTRIBUTING.md`.
- Required Build/CI checkpoints to map to attributable DEVLOG records.
- Required failed hypotheses to remain visible with later corrections.
- Required important conclusions to be logged even when no code or CI build is produced.
- Prohibited fabricated historical backfill.

### Review

This change is documentation/governance only. It does not change APK output, SystemUI behavior, dependency resolution, signing, CI execution, or release facts.

Per the repository rules, it follows the text/governance route rather than creating a runtime work branch or consuming Canary/device validation.

The design intentionally separates:
- **current truth** from history;
- **history** from release changelog;
- **future intent** from currently implemented behavior.

### Validation

- Confirmed the new paths did not exist before initialization.
- Confirmed `main` and `dev` used the same pre-change `CONTRIBUTING.md`, avoiding accidental loss of a dev-only policy variant.
- No APK/device test required.

### Outcome / durable conclusion

Repository-local engineering memory is now a required part of Combined Status development. Future sessions must recover context from repository state before relying on remembered conversation history.

### Follow-up

- The active development session should refresh `CURRENT.md` whenever the real dev baseline or active objective changes.
- Future CI/build checkpoints must append their actual reasoning and feedback here.
- Older Build history may be backfilled only when supported by verifiable commits, CI logs/artifacts, diagnostics, recordings, or device feedback.

---

## 2026-09-26 — Build 378: decouple stable slot geometry from battery motion geometry

**Type:** runtime geometry correction  
**APK build:** 20260926-378  
**Commit:** `7dafc723e9f10ec801a78af33c90dd11cadbaa36`  
**CI:** Fast Build #1013 succeeded

### Problem / objective

The Combined Status runtime was using the concrete `MiuiBatteryMeterView` width as if it were always the stable native battery slot width. Device evidence showed that the battery motion view can transiently differ from the stable Home slot, making it unsafe as a single geometry authority.

### Analysis / root cause

**Confirmed:** stable layout slot geometry and battery motion-view geometry are distinct responsibilities on the target SystemUI.

The implementation therefore needed a native-slot resolver based on the owning `MiuiStatusBatteryContainer` layout rather than treating a transient battery-view width as the stable Combined Status slot.

### Evidence / references consulted

- Repository geometry rules in `CONTRIBUTING.md`.
- `SystemUI-Reference/findings/statusbar.md`:
  - stable Home battery slot approximately 105x108;
  - `MiuiStatusBatteryContainer` owns status-icons/battery layout;
  - slot, drawing, and transition geometry must remain separate.
- Exact target SystemUI runtime geometry.

### Alternatives considered

- Continue using `MiuiBatteryMeterView.width`: rejected because it mixes motion-view geometry with stable slot ownership.
- Hard-code 105px: rejected because the fix must follow native ownership rather than a device constant.
- Derive the slot from the owning container: selected.

### Measures implemented

Added `NativeStatusBarSlotGeometry` and changed Combined Status participant slot resolution to use the native container relationship. No peer translation/margin compensation was introduced.

### Review / validation

The change was scoped to slot geometry. Native peer geometry remained SystemUI-owned. Fast CI succeeded.

Device feedback later showed that this correction did **not** by itself eliminate the first-frame / last-frame non-steady right shift, so stable-slot resolution was necessary but not sufficient.

### Outcome / residual risk

**Confirmed:** stable slot width was no longer coupled to transient battery motion width.

**Residual:** panel handoff geometry still had an independent defect and required separate diagnostics.

---

## 2026-09-26 — Build 379: trace native panel icon transition state

**Type:** bounded runtime diagnostics  
**APK build:** 20260926-379  
**Commit:** `1d68ca310eafe1f9fd8ec9db7a94f587a4f44de9`  
**CI:** Fast Build #1014 succeeded

### Problem / objective

Build 378 left the first/last-frame non-steady shift unresolved. Mid-transition geometry appeared stable, so the next question was whether native icon state changed specifically at panel boundaries.

### Analysis / root-cause status

**Hypothesis:** the fault was in boundary visibility/transition ownership rather than steady drawing geometry.

### Evidence / references consulted

- `CONTRIBUTING.md` single-variable diagnostic rule.
- Exact SystemUI modern status-icon classes and transition state fields.
- Build 378 device behavior.

### Alternatives considered

- Add a position offset immediately: rejected as symptom compensation without a verified owner.
- Add continuous frame logging: rejected by the lightweight diagnostics rule.
- Read native transition state at existing event boundaries: selected.

### Measures implemented

Added read-only snapshots for native panel expansion state and relevant Combined Status / Wi-Fi / mobile transition state. No runtime geometry writer was added.

### Review / validation

Diagnostics remained bounded and event-driven. Fast CI succeeded.

### Outcome / follow-up

The checkpoint narrowed the problem toward panel-boundary ownership and justified a more specific Control Center anchor probe in Build 380.

---

## 2026-09-26 — Build 380: capture Control Center anchor boundaries

**Type:** bounded runtime diagnostics / root-cause confirmation  
**APK build:** 20260926-380  
**Commit:** `f5564d68e853c7d41ebf079a4809be95e510b622`  
**CI:** Fast Build #1015 succeeded

### Problem / objective

Determine whether the non-steady first/last-frame shift came from incorrect Home -> Control Center anchor semantics rather than movement of the Combined Status renderer itself.

### Analysis / root cause

**Confirmed from device diagnostics:** the layout-hide configuration could expose a Control Center anchor where status-icons width had already expanded into the battery area while battery width was still represented separately. The captured non-island boundary state included `systemIconsWidth=587`, `statusIconsWidth=583`, and `batteryWidth=105`, with `addBatteryIsland=false` and `batteryWidthDiff=0`.

Combined Status itself remained stable through the middle of the transition. The mismatch was therefore in handoff geometry semantics, not an ordinary mid-animation drift.

### Evidence / references consulted

- `SystemUI-Reference/findings/control-center.md`, including distinct Control Center surfaces and `StatusBarAnchorBounds`.
- Exact `ControlCenterHeaderExpandController` / `StatusBarAnchorBounds` fields.
- Build 380 device diagnostic report.
- `CONTRIBUTING.md` geometry and evidence-driven solution rules.

### Alternatives considered

- Patch `StatusBarAnchorBounds` or subtract a hard-coded offset: rejected because that would add a second geometry writer.
- Preserve native battery layout semantics so HyperOS computes its own anchor from the expected structure: selected for the next checkpoint.

### Measures implemented

Build 380 itself remained diagnostic-only and added no geometry mutation.

### Review / validation

Fast CI succeeded. The diagnostic was bounded to panel boundary buckets and reused existing callbacks.

### Outcome / durable conclusion

**Confirmed:** the non-steady shift had a native-anchor semantic component caused by Combined Status layout ownership, and could not be solved solely by changing the renderer's stable width.

---

## 2026-09-26 — Build 381: preserve native battery slot during replacement

**Type:** runtime ownership correction  
**APK build:** 20260926-381  
**Commit:** `f6ff15f1bdda27ca7e1f47fdc79f686a9acdae1a`  
**CI:** Fast Build #1016 succeeded

### Problem / objective

Remove the duplicated Control Center anchor semantics proven by Build 380 while preserving the validated Combined Status renderer and native lifecycle.

### Analysis / root cause

**Confirmed by device feedback:** forcing the native battery layout hidden was involved in the first/last-frame right shift. Preserving native battery layout removed that symptom.

A second effect then became visible: the full-width Combined Status participant still occupied its own status-icon width while the native battery slot remained present.

### Evidence / references consulted

- Build 380 anchor diagnostics.
- `SystemUI-Reference/findings/statusbar.md` native Home layout ownership.
- `SystemUI-Reference/findings/charging.md` battery hide contract, treated as static contract evidence rather than overriding contradictory device geometry evidence.
- Repository single-writer/fail-native rules.

### Alternatives considered

- Rewrite Control Center anchor values: rejected.
- Preserve the native battery slot and narrow project ownership to battery visual suppression: selected.

### Measures implemented

The battery suppression owner stopped forcing project-owned battery layout hide and preserved HyperOS native layout authority. Combined Status continued to mask only the required battery visual content.

### Review / validation

The change did not alter the participant's active shell width or add translation compensation. Fast CI succeeded.

### Device feedback / outcome

Device feedback: the non-steady first/last-frame right shift disappeared, but steady Combined Status became left-shifted.

**Confirmed:** fixing handoff anchor semantics exposed duplicate steady occupancy as a separate responsibility.

---

## 2026-09-26 — Build 382: anchor Combined Status visual to the preserved native battery slot

**Type:** single-variable runtime geometry experiment  
**APK build:** 20260926-382  
**Commit:** `a80fb7550d601ff37a977941a8b89b088fa29a3e`  
**CI:** Fast Build #1017 succeeded

### Problem / objective

Remove the steady left shift introduced when both the preserved native battery slot and the full-width Combined Status status-icon shell occupied end-side layout space.

### Analysis / root cause

**Confirmed:** the Build 381 steady displacement matched one Combined Status participant width. The work branch therefore tested keeping the participant shell at zero width while drawing the Combined Status visual into the preserved native battery area.

### Evidence / references consulted

- Build 381 device feedback and geometry.
- `CONTRIBUTING.md` single-variable A/B and geometry-separation rules.
- `SystemUI-Reference/findings/statusbar.md` for native status-icon measurement and battery-slot separation.

### Alternatives considered

- Offset the full-width participant back over the battery with translation/margin: rejected as a competing geometry writer.
- Remove duplicate layout occupancy while keeping the native battery slot: selected as the A/B experiment.

### Measures implemented

Removed handoff-time shell promotion from zero width to visual width. Resume validation required the zero-width root to remain aligned with the native battery anchor and preserved the independent 105/108 visual measurement.

### Review / validation

No peer geometry writer was added. Fast CI succeeded.

### Device feedback / contradiction

Device feedback: the Combined Status enable flash / reappearance symptom returned. The user identified the resulting three-symptom repair cycle:
- entry flash / missing clean animation;
- non-steady first/last-frame right shift;
- steady-state left shift.

Build 382 diagnostics further showed that native APPEAR state was delivered: Combined Status started with native alpha/scale values. The zero-width root, however, exposed `pivotX=0` while normal peers used width-centered pivots.

### Outcome / durable conclusion

**High confidence:** switching only between a full-width and zero-width ordinary status-icon shell cannot be the final architecture. The three symptoms are coupled through conflicting ownership responsibilities.

---

## 2026-09-26 — Build 383: trace native battery motion ownership

**Type:** bounded architecture diagnostics  
**APK build:** 20260926-383  
**Commit:** `8b8dbb799409e50d5c40ddc339843a8c1be4f290`  
**CI:** Fast Build #1018 succeeded; Work Branch Canary #286 succeeded

### Problem / objective

Jump out of the 0px/105px repair loop by identifying the native end-side owner that can potentially supply slot and motion semantics without adding a second status-icon occupancy.

### Analysis / root-cause status

**High confidence:** the recurring cycle is architectural, not three unrelated pixel defects.

Exact Home topology exposes distinct binder objects for `mBatteryContainer` and `mBatteryView`. This suggests a possible seam between end-side slot/motion ownership and battery content, but the wrapper's behavior across panel boundaries is not yet proven.

### Evidence / references consulted

- Latest `CONTRIBUTING.md` lightweight and ownership rules.
- `SystemUI-Reference/findings/statusbar.md` battery container / battery view ownership discussion.
- `SystemUI-Reference/findings/control-center.md`.
- Build 382 device diagnostics.

### Alternatives considered

- Continue tuning shell width/pivot/translation immediately: rejected as insufficient architectural evidence.
- Add a new polling/motion follower: rejected by lightweight and single-writer rules.
- Reuse existing island/panel callbacks to take bounded read-only snapshots of native owners: selected.

### Measures implemented

Added weak-reference access to the already observed Home binder and appended bounded `homeMotion` snapshots at existing panel boundary diagnostic buckets. No new hook, View-tree traversal loop, geometry write, or persistent frame logger was added.

### Review / validation

Ownership remains observational only; the probe does not grant write authority. Fast Build and signed Canary succeeded.

### Outcome / residual risk

**Hypothesis still open:** `mBatteryContainer` may be a viable end-side motion/slot owner.

Device panel-boundary evidence is still required before implementing a renderer migration.

---

## 2026-09-26 — Build 384: center native transition on Combined Status visual

**Type:** bounded animation-geometry experiment  
**APK build:** 20260926-384  
**Commit:** `b838b8dfcdf90f575dba3485b0094dfa5c8aacdf`  
**CI:** Fast Build #1020 succeeded; Work Branch Canary #287 succeeded  
**Device validation:** pending

### Problem / objective

Build 382 diagnostics showed that the zero-width shell still receives native APPEAR alpha/scale state but reports a zero horizontal pivot. Build 384 tests whether using the real Combined Status visual center as the transition pivot can remove the visible flash while preserving the single native battery-slot occupancy.

### Analysis / root-cause status

**High confidence:** a zero-width root creates invalid center geometry for the native scale transition.

**Not confirmed as final root cause:** correcting pivot alone does not resolve the larger ownership conflict proven by the three-symptom cycle.

### Evidence / references consulted

- Build 382 transition samples.
- Build 383 architecture investigation.
- `CONTRIBUTING.md` sections on one-writer geometry, bounded experiments, lifecycle cleanup, and evidence-driven solution changes.
- `SystemUI-Reference/findings/statusbar.md` transition-geometry separation.

### Alternatives considered

- Return to a full-width shell: rejected because Build 381 device evidence showed duplicate steady occupancy.
- Add translation/margin compensation: rejected.
- Replace native APPEAR with a project animation: rejected because HyperOS already owns the transition state.
- Bridge only `pivotX` to half the verified Combined Status visual width while leaving native alpha/scale/curve ownership intact: selected as a bounded experiment.

### Measures implemented

Added a one-shot transition-pivot normalization on the module-owned Combined Status root, with pre-draw reapplication and explicit cleanup. The value is derived from the current visual width rather than a fixed pixel offset.

Build 383's read-only battery-owner diagnostics remain present.

### Review

- No peer native layout/translation writer was added.
- The added pre-draw listener is one-shot and explicitly removed on transition suspension/reset.
- The change touches animation geometry and therefore remains runtime-sensitive.
- **Architecture caution:** this is not accepted as the final design. It still leaves an ordinary status-icon participant carrying animation responsibilities while the native battery slot carries layout occupancy.
- If device evidence shows that a native battery wrapper/container can own the visual/motion layer cleanly, this pivot bridge should be removed during that migration.

### CI / validation

- Fast Build #1020: success.
- Work Branch Canary #287: success.
- Signed non-debuggable Canary artifact `CombinedStatus-0.0.1-HyperOS-20260926-384-canary.apk` produced.
- Device validation remains required; CI is not runtime proof.

### Required device scenarios

1. Toggle Combined Status OFF -> ON and observe entry continuity.
2. Verify steady-state position.
3. Pull the panel once and fully close it, checking first/last-frame alignment.
4. Export diagnostics from the same session so `homeMotion`, Control Center anchor, and Combined Status transition state can be correlated.

### Outcome / residual risk

Pending device evidence.

The active design decision remains whether to migrate the renderer/motion ownership away from the ordinary bindable participant and into a verified native end-side battery-slot layer. No further width/translation compensation should be added before that decision.


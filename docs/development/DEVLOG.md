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


## Historical backfill — validated predecessor baselines and Builds 352-377

**Backfill date:** 2026-09-26  
**Evidence boundary:** Git commits and build identities, PR #93 / #95 / #96 / #98, GitHub Actions records, and previously recorded target-device feedback. Conversation/device recollections are used only where they agree with repository evidence; they are not used to invent missing CI or source facts.

### Accepted predecessor baselines

- **Build 328** was promoted through PR #93 after focused Xiaomi 15 Pro / Android 17 / SystemUI `17.03.260226.r` device validation, integrated `dev` Build #831, promotion readiness #44, and an exact `validation/dev` marker. It is retained as an earlier validated native-participant baseline.
- **Build 351** became the next stable baseline through PR #95 and promotion PR #96. PR #95 aligned network presentation with HyperOS authority: native Wi-Fi/hotspot/airplane/no-SIM resource families, active-subscription filtering, Home-scoped suppression, native participant tint authority, final-pixel native center rendering, and shared visual-intensity policy. Promotion PR #96 records `dev` Build #907 success, validation-marker Build #908 success, and stable promotion of internal build `20260926-351`.
- Build 351 is therefore the stable predecessor for the master-switch work below; later experiments must not be read backward as properties of that stable baseline.

### Build-by-build checkpoint index

| Build | Source checkpoint | Final Fast CI | Purpose / disposition |
| --- | --- | --- | --- |
| 352 | `bc9ce3ef` | #914 success | Introduced the global master-switch runtime gate and serialized feature visibility/handoff on the SystemUI main thread. Device feedback showed disable restored native presentation, but re-enable still had a visible delay/flash. |
| 353 | `145b668d` | #919 success | Made the ready-participant handoff frame-safe and released native fallback only after a visible frame. Re-enable was still visibly delayed/flashy. |
| 354 | `d8336801` | #922 success | Kept a validated participant warm and resumed it atomically. The visible delay remained, disproving cold participant creation as the sole cause. |
| 355 | `b835f6a6` | #924 success | Tightened presentation ordering so participant readiness and native suppression were committed without overlap. This narrowed the fault toward presentation-transaction/lifecycle ownership rather than preference transport. |
| 356 | `16a53cff` | #932 success | Added shared native alpha-mask / visual-intensity normalization and aligned its tests after several cancelled/failed intermediate CI runs. Device feedback still showed visual-intensity mismatch, so tint/alpha authority remained open. |
| 357 | `07a45763` | #990 success | Consolidated the shared native intensity ceiling and evidence-bound battery/tint contract after the long same-build 356 investigation. No separate accepted stable baseline was declared here. |
| 358 | `03865491` | #991 success | Restored native switch visibility motion instead of substituting a custom transition. |
| 359 | `383f5685` | #992 success | Honored native participant visibility state as an authoritative input. |
| 360 | `f12c5a39` | #993 success | Resolved native visibility states from SystemUI rather than reconstructing them locally. |
| 361 | `cffdd3de` | #994 success | Kept the master-switch fix single-purpose and avoided expanding the branch into a parallel animation/state owner. |
| 362 | `7a4f385e` | #995 success | Switched enable/disable behavior onto the native status-icon removal lifecycle. This became a durable part of the accepted solution. |
| 363 | `2b2e0bf0` | #996 success | Added bounded transition-frame diagnostics. Runtime evidence showed suppression and the native participant were ready, moving the investigation from readiness toward animation geometry. |
| 364 | `bf8370fe` | #997 success | Tested a zero-slot APPEAR pivot override. PR #98 later records this experiment as rejected: native APPEAR overwrote the pivot and transformed screen bounds broke bridge readiness. |
| 365 | `f5bc88bf` | #998 success | Reverted the ineffective zero-slot pivot override rather than layering another compensation on top. |
| 366 | `76db324d` | #999 success | Tested preserving transform width without slot occupancy. This width/padding occupancy compensation was later rejected by device evidence. |
| 367 | `0087b0f9` | #1000 success | Removed the invalid compensated-slot geometry and returned to evidence-backed geometry ownership. |
| 368 | `393523b4` | #1001 success | Made the warm native handoff atomic after removing the invalid compensation path. |
| 369 | `09e05f4f` | #1002 success | Began handing native battery-slot occupancy to the validated Combined Status participant. |
| 370 | `05b58261` | #1003 success | Completed the native battery-slot handoff while retaining fail-native restoration. |
| 371 | `29cc8b5c` | #1004 cancelled | Tried to keep the validated slot geometry stable while charging. The CI run was cancelled and this checkpoint was superseded; it is not a validation source. |
| 372 | `2611ca01` | #1005 success | Restored the zero-slot contract test arguments after the superseded Build 371 checkpoint. |
| 373 | `c7129c16` | #1006 success | Added bounded native panel-transition progress observation. |
| 374 | `0354f73e` | #1007 success | Hardened the native panel-transition source before drawing conclusions from boundary samples. |
| 375 | `d7c05457` | #1008 success | Captured Control Center transition targets. PR #98 later identifies the Build 375 runtime architecture as the validated state to which Build 377 intentionally returned. |
| 376 | `3bc1bf95` | #1009 success | Tested the observe-only battery-hide / dynamic zero-slot direction. Device charging plus disable/re-enable feedback reintroduced delayed/flashy entry, so the candidate was rejected despite green CI. |
| 377 | `bad5a27c` | #1010 success; Work Branch Canary #279 success | Restored the validated native battery handoff / Build 375 architecture, retained composed native battery-hide ownership, and removed the rejected compensation path. Target-device master-switch acceptance passed; the remaining first/last-frame horizontal offset was explicitly split into the separate panel-transition work that became PR #100. |

### Root-cause and design conclusions carried forward

**Master-switch transport was not the visible-delay bottleneck.** PR #98 records that focused diagnostics measured App -> SystemUI preference propagation at only a few milliseconds. Builds 352-355 showed that making transport/main-thread dispatch faster did not eliminate the symptom. The durable fix therefore moved to the native replacement-session boundary and native status-icon visibility/remove lifecycle.

**A ready participant still needs one coherent presentation transaction.** The branch repeatedly demonstrated that Combined Status visibility, native Wi-Fi/mobile/battery suppression, and slot handoff cannot be staged as unrelated asynchronous presentation writes without transient overlap or gaps.

**Native lifecycle is animation authority.** Builds 358-365 converged on HyperOS visibility/remove/APPEAR/DISAPPEAR ownership. The rejected Build 364 pivot experiment is retained specifically to prevent a future session from reintroducing project-side animation geometry merely because it is easy to write.

**Visual-intensity mismatch was not solved by choosing another gray value.** The Build 356 investigation and the merged PR #98 line established a shared normalization contract for resource-intrinsic alpha plus native tint authority. Per-resource grayscale multipliers or hand-edited native assets remain rejected unless new evidence proves the shared contract insufficient.

**Slot occupancy, drawing width, and transition geometry are separate responsibilities.** Builds 366-377 repeatedly exposed failures when one shell width was asked to satisfy all three. Build 377 deliberately accepted the master-switch boundary while leaving the first/last-frame panel offset unresolved, which is why Builds 378+ belong to a separate work branch and DEVLOG sequence.

### PR / integration boundary

- PR #98 merged the accepted Build 377 master-switch implementation into `dev` as commit `f64fe0e3992eab4dd62ff479c3765d834ec7dfa4`.
- PR #99 (`fix/native-visual-intensity-normalization`) remains open and unmerged. Its head is not an accepted baseline; compare/reconcile it against current `dev` before using any of its code.
- PR #100 (`feat/native-panel-transition`) owns the remaining status-bar -> shade / Control Center transition-geometry problem. Builds 378-384 below are work-branch evidence, not accepted `dev` runtime state.

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
- The later development-memory synchronization commit changed only repository governance/development-history files and did not change APK/runtime source or build identity.
- Fast Build #1023 revalidated the same Build 384 runtime source after that documentation synchronization: success.
- Work Branch Canary #288 revalidated the same Build 384 runtime source after that documentation synchronization: success.
- Signed non-debuggable Canary artifact `CombinedStatus-0.0.1-HyperOS-20260926-384-canary.apk` produced.
- Latest extracted APK SHA-256 after Canary #288: `9551d931012c75eb9ba3a37596d7e0564fc1f16850683759c86a983aaf37f235`.
- Device validation remains required; CI is not runtime proof.

### Required device scenarios

1. Toggle Combined Status OFF -> ON and observe entry continuity.
2. Verify steady-state position.
3. Pull the panel once and fully close it, checking first/last-frame alignment.
4. Export diagnostics from the same session so `homeMotion`, Control Center anchor, and Combined Status transition state can be correlated.

### Outcome / residual risk

Pending device evidence.

The active design decision remains whether to migrate the renderer/motion ownership away from the ordinary bindable participant and into a verified native end-side battery-slot layer. No further width/translation compensation should be added before that decision.
### Device diagnostic update — Build 384

A detailed Build 384 diagnostic report was received from the target Xiaomi 15 Pro / HyperOS SystemUI 17.03.260226.r session.

#### New evidence

- **Confirmed:** OFF -> ON resumes through the native remove lifecycle with the zero-width shell still aligned to the native battery anchor (`rootScreenX=1242`, `batteryScreenX=1242`, render width 105).
- **Confirmed:** DISAPPEAR keeps the project-set visual-center pivot (`pivotX=52.5`) through the sampled transition.
- **Confirmed / correction:** APPEAR does **not** keep that pivot. Frames 1-5 report `pivotX=52.5`; frame 6 onward reports `pivotX=0` while native alpha/scale continue progressing from the 0/0.4 start state. The one-shot pre-draw normalization therefore loses to a later HyperOS writer.
- **Confirmed:** Control Center boundary geometry remains structurally normal in the captured session: `systemIconsWidth=587`, `statusIconsWidth=478`, `batteryWidth=105`, `normalStatusIconsTx=46`, `batteryWidthDiff=0`, `addBatteryIsland=false`.
- **Confirmed:** bounded `homeMotion` sampling shows `mBatteryContainer` and `mBatteryView` remain co-anchored and move together through the observed native end-side/island motion while retaining width 105. The outer `mEndSideContent` / `MiuiStatusBatteryContainer` carries broader visibility/alpha changes.

#### Root-cause correction

The Build 384 hypothesis that a one-shot pivot bridge could make the zero-width ordinary participant a clean native animation carrier is **invalidated**.

The remaining architectural conclusion is stronger: the zero-width participant receives native transition state, but it does not own the geometry that HyperOS derives from its zero measured width. Any repeated/per-frame project pivot correction would become a competing animation writer and is rejected by the current ownership and lightweight rules.

#### Review / alternatives

- Reapply pivot on every frame: rejected; competing writer, hot-path work, and symptom compensation.
- Add another pre-draw/listener retry: rejected; the log already proves the native writer occurs after the current bridge and there is no ownership basis for racing it.
- Restore full-width ordinary participant: rejected as a final path because Build 381 reintroduced duplicate steady occupancy.
- Continue native owner investigation: selected.

The battery-wrapper/end-side candidate is strengthened but not yet accepted. Before renderer migration, source-level review must determine:
1. the exact native APPEAR pivot writer;
2. whether `mBatteryContainer` is safe as a Combined Status visual host across island/privacy alpha semantics;
3. whether placing Combined Status under that layer would preserve desired network visibility when native battery presentation is intentionally hidden.

#### Validation state

Build 384 Fast Build #1020 and Work Branch Canary #287 remain successful. This diagnostic is runtime evidence, not a new APK checkpoint, so no new build number is created.

#### Next step

Inspect exact SystemUI animation code to identify the pivot writer and compare that ownership with the battery/end-side wrapper path. Do not produce Build 385 until the next implementation boundary is justified by that source-level review.


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
---

## 2026-09-26 — Build 385: adapt native APPEAR pivot to Combined Status visual geometry

**Type:** runtime transition-geometry source fix
**APK build:** 20260926-385
**Source checkpoint:** current work-branch runtime commit
**CI:** pending at commit creation
**Device validation:** pending

### Problem / objective

Build 384 proved that a one-shot pre-draw pivot correction is overwritten when HyperOS native APPEAR actually starts. Build 385 aims to close the entry-animation corner of the three-symptom cycle without changing native battery-slot occupancy, steady placement, Control Center anchor semantics, or the native Folme alpha/scale curve.

### Analysis / root cause

**Confirmed by exact SystemUI source and Build 384 runtime evidence:** `MiuiStatusBarIconAnimatorController$FolmeHandler$appearAnimation$appear$1.onStart()` reads the target View height and width and writes `pivotY` and `pivotX`. Build 384 enable frames show project `pivotX=52.5` initially, then native `pivotX=0` once APPEAR starts on the intentional zero-width Combined Status shell.

The defect is therefore not a missing animation callback, preference delay, or random pre-draw race. HyperOS is applying its normal status-icon geometry contract to a custom root whose layout width is deliberately zero while its renderer is 105px wide.

### Additional architecture correction

**Confirmed:** `HomeStatusBarViewBinderInjector.mBatteryContainer` is not an outer slot wrapper. Exact `battery_digital_view.xml` maps it to the internal `FrameLayout` holding `MiuiBatteryMeterIconView` / `MiuiHollowBatteryMeterIconView` inside `MiuiBatteryMeterView`. Build 384 also observes battery-specific alpha changes on this View. Hosting Combined Status there would incorrectly inherit battery-content hiding semantics and is rejected.

### Evidence / references consulted

- Latest `CONTRIBUTING.md`: root-cause order, evidence-driven solution changes, one-writer ownership, geometry separation, lightweight runtime, fail-native, and development-log requirements.
- Exact HyperOS SystemUI `17.03.260226.r` artifact SHA-256 `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`.
- Exact DEX contract for `MiuiStatusBarIconAnimatorController$FolmeHandler$appearAnimation$appear$1`: captured `$view` field and zero-argument `onStart()` whose method references are `getHeight`, `setPivotY`, `getWidth`, and `setPivotX`.
- Exact `system_icons.xml` and `battery_digital_view.xml` resource hierarchy.
- Build 384 detailed device diagnostic.
- `SystemUI-Reference/findings/statusbar.md`, `control-center.md`, and `charging.md`.

### Alternatives considered

1. **Repeat pivot writes on pre-draw / every frame** — rejected; Build 384 proves native writes later and racing it creates a competing hot-path writer.
2. **Restore full-width Combined Status shell** — rejected as a final solution because Build 381 caused duplicate steady occupancy.
3. **Hide native battery layout again** — rejected because Build 380/381 tied that topology to invalid Control Center anchor semantics.
4. **Move renderer into binder `mBatteryContainer`** — rejected after exact resource identification proved it is battery-internal presentation content.
5. **Conditionally replace the exact native APPEAR pivot callback only for the module-owned root** — selected. This preserves native lifecycle/curve while supplying the custom visual geometry that a zero-width shell cannot express.

### Measures implemented

- Removed Build 384's transition-pivot `OnPreDrawListener` and pending listener state.
- Added an exact-target hook for `MiuiStatusBarIconAnimatorController$FolmeHandler$appearAnimation$appear$1.onStart()`.
- The hook checks the callback's captured `$view`; every native peer immediately executes the original callback.
- Only for the current Combined Status root, the module replaces the callback's pivot initialization with `pivotY = resolved visual/root height / 2` and `pivotX = renderer visual width / 2`.
- HyperOS remains owner of visible state, remove lifecycle, APPEAR/DISAPPEAR timing, alpha, scale, Folme properties, panel/island behavior, and peer geometry.
- Exact callback class/method/field are part of participant readiness; missing contract fails closed.
- Hook count becomes two: controller construction plus APPEAR pivot adapter. Partial hook state is rejected.
- Internal version advances to `20260926-385`; display version remains `0.0.1`.

### Review

- **Ownership:** one Combined Status-specific pivot writer for the custom root at native APPEAR start; the original native pivot callback is not executed for that root, so the module does not race a second pivot writer.
- **Lifecycle:** both hooks are tracked; constructor-hook failure unhooks the already-installed pivot adapter; hot-reload accounting includes both.
- **Performance:** one exact event-driven identity check per native APPEAR; no polling, View-tree traversal, frame loop, or resident listener.
- **Fallback:** callback-contract or hook failure blocks the native replacement path instead of leaving a known-bad animation.
- **Geometry:** battery slot remains the sole 105px layout occupancy owner; Combined Status visual stays independent; Control Center anchor inputs are intentionally unchanged.
- **Future sizing:** pivot derives from resolved visual width rather than a fixed 105px constant.

### CI / testing

- Fast Build #1031: **success**.
- Work Branch Canary #290: **success**.
- Canary checkout log confirms exact tested work-branch SHA `d3533828e82a335eab0b3e661cfadd4e70ebee27`.
- Modern Xposed metadata verification: success.
- Haple signature verification: success; APK signature verifies with v3.
- Canary non-debuggable verification: success.
- Artifact ID: `10907652284`.
- Artifact archive digest: `sha256:6074e71cf5640ac5fd8d4e3d21d76a5f0603d733cba8479856ee0c756e3185fc`.
- Extracted APK: `CombinedStatus-0.0.1-HyperOS-20260926-385-canary.apk`.
- Extracted APK SHA-256: `c1c084b2a6b79924bcc2c2e801d3f2c1050f597bff107cbddacbcbea619e3259`.
- Extracted APK size: `3375134` bytes.

Required focused device test after CI:
1. OFF -> ON centered APPEAR with no flash/reappearance;
2. ON -> OFF centered DISAPPEAR;
3. steady placement remains aligned to the native battery slot;
4. pull-down first frame and return-to-steady last frame remain aligned;
5. detailed diagnostic confirms `appearPivotAdapter state=applied`, APPEAR pivot remains centered after animation start, and Control Center anchor remains `statusIconsWidth=478`, `batteryWidth=105`.

### Outcome / residual risk

CI and signed-Canary validation passed. Device evidence remains the gate. If Build 385 still fails any focused scenario, do not add timing retries or repeated writes; reopen native end-side ownership.



---

## 2026-09-26 — Roadmap and App-home design intent restored

**Type:** documentation / product-development continuity correction  
**APK build:** none  
**Runtime impact:** none

### Problem / objective

The repository development memory had become too focused on the active three-symptom transition investigation. A future session could therefore misread completed capabilities as future work or reopen an App Home design that had already been agreed.

The objective is to restore the macro development sequence and preserve the confirmed Home-page information architecture without confusing design completion with implementation completion.

### Evidence / references consulted

- Latest `CONTRIBUTING.md`, especially development continuity, native ownership, and App/MIUIX rules.
- Current repository and PR history through Build 377 and active PR #100 / Build 385.
- Existing runtime evidence for multi-SIM presentation and native island/end-side motion participation.
- Existing app source, including `FeaturesScreen`, the master-switch preference, and the real Modern Xposed Hot Reload entry point.
- Confirmed project-design decisions recovered from prior development discussions and reconfirmed by the maintainer:
  - current macro stage is Home -> shade / Control Center transition;
  - the following macro order is Keyguard/lockscreen/AOD -> App Home/Preview Sandbox -> adaptive sizing/spacing and broader visual controls -> full regression -> 0.0.1 closure;
  - Home is top real Runtime Status + bottom Preview Sandbox;
  - primary tabs are `Home | Features | Settings`; the second tab is **Features**, not Customization.

### Corrections

- **Corrected:** dual-SIM and network presentation are not future roadmap phases. They are part of the completed core capability baseline. Later work may regression-test or extend compatibility, but should not plan them again as unimplemented milestones.
- **Corrected:** island support is not a separate future feature stage. Native participant/slot/motion integration already gives Combined Status the SystemUI-owned island/end-side movement path that later work must preserve.
- **Corrected:** the App Home page is a future implementation phase, but its high-level layout is already decided. Implementation should reproduce the retained design intent instead of reopening the information architecture.
- **Corrected terminology:** the primary navigation is `Home | Features | Settings`; the second tab must not be described as `Customization`.

### Confirmed App Home design snapshot

**Top: Runtime Status**

- Represents real module/SystemUI runtime state, not simulated state.
- Shows the current Combined Status connection/takeover/health condition in a concise status-focused presentation.
- Contains the global Combined Status master switch.
- Retains a direct Hot Reload action backed by the real Modern Xposed Hot Reload path.
- Detailed diagnostics remain secondary; Home must not become a diagnostic dump.

**Bottom: Preview Sandbox**

- A dedicated simulated Combined Status preview area below the real runtime section.
- Can model Wi-Fi, mobile network, no-SIM, airplane mode, charging, battery and later visual-parameter combinations.
- Preview state is local UI state only and must never mutate real SystemUI/network/battery state.
- Future size/spacing/color controls should be observable here while reusing the real rendering semantics rather than creating a separate lookalike renderer.

**Primary navigation**

- `Home | Features | Settings`.
- **Features** is the canonical second-tab name. Customization is a capability inside the product, not the top-level tab identity.

### Review

This is documentation/product-intent work only and adds no executable behavior.

The review separates four categories that future sessions must not conflate:
- **implemented capability** — supported by repository/runtime evidence;
- **confirmed design intent** — already decided, implementation still future;
- **active engineering checkpoint** — Build 385 inside the current transition phase;
- **future macro phase** — work that genuinely follows the current stage.

### Outcome

The macro roadmap is restored above the Build-level route, and the Home page now has enough durable design intent to be reconstructed later without relying on chat memory.
---

## 2026-09-26 — Build 385 validation history synchronization

**Type:** repository history / CI gate recovery
**APK build:** unchanged (`20260926-385`)
**Runtime impact:** none

### Problem

After the Build 385 runtime checkpoint was committed, PR #100 stopped producing `pull_request` validation runs. Repeated PR state events (`ready_for_review`, `reopened`) were recorded by GitHub but no Build workflow was created.

### Root cause

Repository review confirmed PR #100 was `mergeable=false` with `mergeable_state=dirty`. The work branch and `dev` had independently received equivalent development-memory / roadmap updates, so Git history diverged even though the relevant documentation content had already been synchronized semantically. GitHub therefore could not create the PR test-merge ref required for `pull_request` validation.

### Evidence / review

- Latest `CONTRIBUTING.md` and current `CURRENT.md`, `DEVLOG.md`, and `ROADMAP.md` were re-read before changing history.
- The latest `dev` delta from the prior common base was reviewed and contained repository-development documentation/history updates rather than APK/runtime code.
- Previously compared development-document blobs were byte-identical where the same sync had landed on both lines; later macro-roadmap/Home-design restoration was also retrieved from the latest repository state before proceeding.
- PR #100 reported `mergeable=false`, `mergeable_state=dirty`, while its current head remained the Build 385 runtime line.

### Resolution

Create a history-preserving merge commit on `feat/native-panel-transition` with:
- first parent = the current work-branch head;
- second parent = the latest `dev` head;
- current Build 385 work tree retained as the content basis, plus this CI-history note.

This is intentionally **not** a runtime change and does not increment the external version, internal versionCode, or buildId. The merge commit is also intentionally not marked `[skip ci]`, because the purpose is to restore a valid PR merge base and allow trusted Build 385 validation to run against the exact current runtime tree.

### Review boundary

- No app/SystemUI source, Gradle runtime property, dependency, signing, or workflow logic changes are introduced by this history synchronization.
- Build 385 runtime ownership and acceptance criteria remain unchanged.
- CI success after the merge is validation of Build 385; it is not a new Build 386 checkpoint.
### Validation-history resolution result

The history-only merge restored PR #100 to `mergeable=true` and immediately produced Fast Build #1031. That Fast gate succeeded and triggered Work Branch Canary #290.

Canary #290 explicitly checked out `d3533828e82a335eab0b3e661cfadd4e70ebee27`, completed tests/build, Modern Xposed metadata validation, Haple signing verification, non-debuggable verification, and artifact upload successfully.

This confirms the earlier missing-run condition was a PR dirty/test-merge-ref problem rather than a Build 385 source or workflow-classification failure.
### Device validation update — Build 385

Device feedback: **the same flash / missing visible entry animation remains**.

#### What Build 385 did prove

- The exact APPEAR pivot adapter is active.
- Sampled enable frames retain `pivotX=52.5` while root alpha/scale progress through the native Folme APPEAR curve.
- Therefore the Build 384 pivot reset was a real defect, but correcting it is **not sufficient** to restore the visible entry animation.

#### Root-cause correction

The user's symptom is specifically the disappearance of the Combined Status **entry animation**, not an overlap flash between Combined Status and restored native network/battery icons.

A direct 381 -> 382 code/device comparison identifies the decisive boundary: Build 381 promoted the active `ModernStatusBarView` shell to the resolved visual width and had a visible native entry animation; Build 382 removed `promoteActiveShellGeometry()` and kept the active shell at zero width, after which the entry animation disappeared again. Builds 384/385 modified only pivot handling and did not restore the real active shell extent.

**Confirmed conclusion:** pivot is no longer the primary root cause. The remaining problem is that HyperOS native APPEAR owns alpha/scale on the status-icon root, while the actual 105px Combined Status renderer is intentionally laid out outside a zero-width root. Logs can therefore show a valid native animation state without proving that the overflow renderer participates in a visually animated transition.

#### Rejected next moves

- More pivot callbacks / timing retries: rejected by Build 385 device result.
- Returning permanently to full-width active shell while preserving the native battery slot: rejected because Build 381 caused steady left shift.
- Hiding the native battery slot to make room for a full-width shell: rejected because Builds 380/381 tied that topology to non-steady first/last-frame anchor shift.

#### Selected investigation

Inspect the Android/SystemUI render boundary for `ModernStatusBarView` to determine whether a zero-width root can provide native animated visual bounds for an overflowing 105px child. The next solution must separate **transition bounds** from **layout occupancy**: real bounds for APPEAR, zero additional steady slot consumption.

Build 386 is blocked until this boundary is source-justified.
---

## 2026-09-26 — Build 386: separate native layout occupancy from actual transition bounds

**Type:** runtime geometry/transition ownership correction
**APK build:** 20260926-386
**CI:** pending at commit creation
**Device validation:** pending

### Problem / objective

Build 385 kept the native APPEAR pivot centered but the user still observed the same missing entry animation. The previously working visible APPEAR existed when Build 381 promoted the custom `ModernStatusBarView` root to a real 105px width. Build 382 removed that promotion to fix duplicate steady occupancy, and the visible entry animation disappeared again.

Build 386 aims to preserve the two independently validated requirements simultaneously:
- zero extra measured/layout occupancy so the native battery slot remains the only 105px end-side slot;
- real 105px View/RenderNode bounds so HyperOS APPEAR/DISAPPEAR has a real visual animation surface.

### Root cause / source analysis

**Confirmed device/code boundary:** the visible entry animation tracks the presence of real participant bounds, not pivot alone.

**Confirmed exact SystemUI ordering:** `MiuiStatusIconContainer.onMeasure()` measures selected child views and uses child measured widths for its occupancy calculations. `MiuiStatusIconContainer.onLayout()` first lays every child from `getMeasuredWidth()/getMeasuredHeight()`, then performs its `NewStatusIconState` / `layoutTranslationX` calculations. Therefore a custom child can remain measured as 0px during native layout/state computation and receive different actual bounds only after the container's native `onLayout()` completes.

Android's View/ViewGroup contract also distinguishes child clipping/layout from subtree rendering; `clipChildren=false` allows descendants to draw outside parent bounds, but it does not create non-zero bounds for a zero-width animation target. Build 385 runtime evidence showed native alpha/scale state alone was insufficient for the overflowing renderer.

### Alternatives considered

1. More pivot/timing work — rejected by Build 385 device result.
2. Redirect native Folme animation directly to `CombinedStatusRenderView` — deferred because `MiuiStatusBarFolmeViewState.animateTo()` also owns translation and other properties; redirecting the whole target risks applying root layout translation to the child and would require a larger native-animation fork.
3. Return to permanent 105px measured shell — rejected because Build 381 produced duplicate steady occupancy / left shift.
4. Post-native-layout visual bounds — selected. Keep measured/layout width 0 for native occupancy, then expand only the module-owned root's actual bounds to the renderer width after native layout/state calculations.

### Measures implemented

- Removed Build 385's exact APPEAR pivot hook and callback contract.
- Added one hook on exact `MiuiStatusIconContainer.onLayout(boolean,int,int,int,int)`.
- The hook always executes native layout first. After native layout returns, it checks only the current Combined Status root and expands its actual bounds from 0x108 to the resolved renderer dimensions while leaving `layoutParams.width=0` and `measuredWidth=0` unchanged.
- The same visual-bounds preparation runs before `ModernStatusBarView.setRemove(...)` so APPEAR/DISAPPEAR begins with real root bounds.
- No native peer View, container bounds, translation, margin, padding, or Control Center anchor is modified.
- The existing renderer stays a direct child of the custom root; the root now owns a real 105px visual/transition surface rather than relying on child overflow from a zero-width parent.
- Added bounded diagnostic `nativeCombinedParticipant visualBounds` that logs only the first successful application per runtime generation.
- Internal build advances to `20260926-386`; display version remains `0.0.1`.

### Review

- **Single writer:** HyperOS remains the only writer for container measurement, slot ordering, `NewStatusIconState`, translation, alpha/scale curve and peer geometry. The module owns only its custom root's post-layout visual bounds.
- **Ordering:** native parent measurement and layout-state calculation see 0px; module visual bounds are applied only after native `onLayout()` returns.
- **Performance:** one constant-time post-layout identity check on the status-icon container; no tree traversal, polling, per-frame animation copying, or persistent pre-draw listener.
- **Lifecycle:** visual bounds are also prepared synchronously before `setRemove(...)`, avoiding a race where APPEAR begins on 0px bounds.
- **Future sizing:** actual transition width derives from renderer measured width, not a fixed 105px constant.
- **Fallback:** if layout/measured width is not zero or resolved visual geometry is unavailable, the visual-bounds operation fails instead of mutating native peers.

### CI / testing

Fast Build and signed Work Branch Canary are pending.

Focused device acceptance after CI:
1. OFF -> ON: native entry animation is visibly restored, not a flash/direct appearance;
2. ON -> OFF remains animated;
3. steady Combined Status remains aligned with the battery slot, with no left shift;
4. pull-down first frame / return last frame remain aligned, with no right shift;
5. diagnostic confirms `layoutWidth=0`, `measuredWidth=0`, `actualWidth=105` and Control Center anchor remains `statusIconsWidth=478`, `batteryWidth=105`.

### Outcome / residual risk

Pending CI and device validation. If real post-layout bounds still do not restore visible APPEAR, stop and reopen the animation-target architecture rather than adding another offset or timing layer.
### CI update — Build 386

- Fast Build #1032: **success**.
- Work Branch Canary #291: **success**.
- Canary verified tested work-branch SHA checkout, unit/Canary build, pinned HyperOS target profile, Modern Xposed metadata, Haple APK signature, non-debuggable status, and artifact upload.
- This CI update does not create a new runtime build. Device validation remains the acceptance gate.
---

## 2026-09-26 — Build 387: preserve battery-slot translation during charging-island eviction

**Type:** runtime charging/island transition-target correction
**APK build:** 20260926-387
**CI:** pending at commit creation
**Device validation:** pending

### Build 386 device result

Build 386 is the first checkpoint to break the original three-symptom loop on device: steady placement is correct, the Combined Status entry animation is visible, and the previously reported non-steady first/last-frame shift is not observed.

A separate charging Super Island defect remains: while HyperOS evicts native battery presentation, Combined Status is pushed beyond the right display boundary.

### Root cause

Build 386 correctly separates zero measured/layout occupancy from real 105x108 visual bounds. The remaining defect is the translation-target semantic of the zero-width custom participant during charging island.

Runtime evidence shows normal Home uses Combined Status `layoutTranslationX=478`. During charging island, HyperOS translates/fades native battery content out and the custom participant's native target advances to 583, causing the real Build 386 visual surface to start at the battery-eviction endpoint and extend beyond the right edge.

Exact SystemUI review confirms `MiuiStatusIconContainer` / `NewStatusIconState` owns translation-state calculation and `MiuiStatusBarFolmeViewState.applyToView(View, boolean)` consumes that resolved state while HyperOS remains the actual View/Folme writer.

The native battery slot target is directly available from sibling layout coordinates under `MiuiStatusBatteryContainer`: `desiredTranslationX = battery.left - statusIcons.left - root.left`. This excludes battery-only motion translation by construction while retaining native parent/end-side movement.

### Evidence / references consulted

- Latest repository `CONTRIBUTING.md`: root-cause order, authoritative native source hierarchy, one-live-writer rule, geometry separation, fail-native compatibility, bounded diagnostics.
- Build 386 device screenshot and detailed diagnostic from Xiaomi 15 Pro / SystemUI 17.03.260226.r.
- Build 386 runtime geometry: normal target 478; charging-island custom target 583; native battery content translated/faded by HyperOS.
- Exact target DEX: `MiuiStatusIconContainer`, `NewStatusIconState`, `MiuiStatusBarFolmeViewState.applyToView(View, boolean)`, and native Folme state application.
- Build 386 actual-bounds implementation and successful device result.

### Alternatives considered

1. Hard-code -105px while charging — rejected; observed delta is evidence, not architecture, and battery geometry can vary.
2. Write `root.translationX` from the module — rejected; HyperOS already owns the live property and Folme animation.
3. Move actual bounds left outside the native animation — rejected after review because it can fix an endpoint while introducing a start-frame jump.
4. Hide Combined Status with native battery — rejected because Combined Status still carries Wi-Fi/mobile information.
5. Adapt the custom `NewStatusIconState` target before native apply — selected. HyperOS continues to execute the live property update/animation.

### Measures implemented

- Preserved Build 386's `MiuiStatusIconContainer.onLayout(...)` post-layout visual-bounds hook unchanged.
- Added one exact hook on `MiuiStatusBarFolmeViewState.applyToView(View, boolean)`.
- Every non-CombinedStatus View and every non-`NewStatusIconState` proceeds untouched.
- For the Combined Status root only, the adapter resolves the native battery-slot target from sibling layout coordinates and updates the state object's `translationX` and `layoutTranslationX` before original native apply.
- The module never writes the root View's live `translationX`; HyperOS/Folme remains the sole live property writer.
- Handoff readiness now compares against the same native battery-slot screen anchor rather than raw motion-translated battery content.
- Added one bounded `nativeCombinedParticipant slotTranslation` diagnostic only when native and slot-layout targets differ materially.
- Hook accounting becomes three owned hooks; partial installation fails closed and constructor-install rollback unhooks both previously installed integration hooks.
- Internal build advances to `20260926-387`; display version remains `0.0.1`.

### Review

- **Single writer:** HyperOS remains sole writer of live translation, timing, curve, island state and peer geometry.
- **Module ownership:** only custom state-target adaptation plus Build 386 actual bounds.
- **Native source:** sibling layout coordinates are authoritative slot geometry; no local charging/island state machine.
- **Normal invariance:** when native target already equals battery-slot target, the adapter is effectively a no-op.
- **Dynamic geometry:** no 105px compensation constant; current native layout handles 105/135 and future width changes.
- **Performance:** one constant-time identity/class check in existing state application; no polling, frame loop, tree traversal or persistent listener.
- **Fail-native:** missing exact class/method/fields prevents a partial native participant installation.
- **Hot reload:** third hook participates in hook counting/reset and constructor failure rollback.

### CI / test gate

Fast Build and signed Work Branch Canary are pending.

Focused device acceptance after CI:
1. charging Super Island enter/steady/exit keeps Combined Status inside the right boundary;
2. native motion is continuous with no module-side jump;
3. non-charging steady placement remains correct;
4. OFF -> ON entry animation remains visible;
5. shade/Control Center first/last-frame alignment remains correct;
6. diagnostic reports target correction only when necessary and `moduleViewTranslationWrites=0`.

### Route impact

Build 386 remains the structural solution to the original three-symptom loop. Build 387 narrows translation semantics so Combined Status follows the battery slot rather than battery-only Super Island eviction. If this regresses Build 386, revert the adapter and reopen the state-target owner instead of adding offsets or live translation writes.
### CI validation update — Build 387

- Fast Build #1033: **success** on runtime commit `9cce4d2ea1e8ddf2512b1db5df4ac55dd9ff235c`.
- Work Branch Canary #292: **success**.
- Canary exact tested work-branch SHA checkout: success.
- Pinned HyperOS target-profile verification: success.
- Modern Xposed metadata verification: success.
- Haple APK signature verification: success.
- Canary non-debuggable verification: success.
- Artifact upload: success.
- Artifact ID: `10908269727`.
- Artifact archive digest: `sha256:bf91bb40923264f2a76aa6b9be8000373af5f71f0d4331a19695f6d46be02a40`.
- Extracted APK SHA-256: `2788a27aa64dc6c1495f71aaaafc1037b39310a95fab55db89341c3697209dec`.
- Extracted APK size: `3375134` bytes.

### Validation state

Build 387 has cleared repository Fast and signed-Canary gates. Device evidence remains the acceptance gate. The focused device check is charging Super Island enter/steady/exit plus one regression pass of the Build 386 steady/entry/non-steady behavior.


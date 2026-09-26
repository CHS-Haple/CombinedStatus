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



---

## 2026-09-26 — Build 388: reserve the released native battery slot during island hide

**Type:** runtime island/layout-occupancy ownership correction  
**APK build:** 20260926-388  
**CI:** pending at commit creation  
**Device validation:** pending

### Problem / device evidence

Build 387 is rejected as a complete charging-island fix. The supplied screen recording shows that the Combined Status visual remains inside the right edge, but native peer status icons move into and overlap the Combined Status visual while the charging Super Island is active.

The matching Build 387 diagnostic provides the structural evidence: the normal status-icon region is 478px beside a 105px battery slot; when native battery hide is activated for the charging island, `MiuiStatusIconContainer` expands to 583px. The Combined Status renderer remains visually 105px wide while its native shell is measured at 0px, so the native layout has no reason to reserve the released battery region for it.

### Root cause

Build 387 corrected the participant translation target, but its steady-state assumption was too broad: the native battery is the single 105px end-side occupancy owner only while HyperOS keeps that battery slot in layout. Once HyperOS itself applies `MiuiStatusBatteryContainer.setIsHideBattery(true)`, that region is released to the status-icon container. Keeping Combined Status at 0px measured occupancy then allows peer icons to share the same native region as the module-owned 105px visual.

### References consulted

- latest `CONTRIBUTING.md`: root-cause-first flow, native state-source hierarchy, one-live-writer rule, geometry separation, fail-native behavior, lightweight/event-driven implementation, development-log requirements;
- Build 387 maintainer screen recording and detailed device diagnostic;
- `SystemUI-Reference/findings/statusbar.md`: `MiuiStatusIconContainer.onMeasure()` consumes child measured width and status-icon slot geometry must remain distinct from visual/motion geometry;
- `SystemUI-Reference/findings/charging.md`: `MiuiStatusBatteryContainer.setIsHideBattery(Boolean)` is the exact native layout-level battery-hide authority used by HyperOS island behavior.

### Alternatives reviewed

- Move Combined Status farther right: rejected; that reintroduces the Build 386 right-edge eviction.
- Move peer native icons from the module: rejected; peer geometry remains SystemUI-owned and this would create competing writers.
- Hard-code a 105px compensation: rejected; current 105px is runtime evidence, not a future sizing contract.
- Hide Combined Status with the battery: rejected; the combined icon still carries network state.
- Copy/replay island animation: rejected; HyperOS already owns the animation.
- **Selected:** reserve only the native region that HyperOS itself releases, using the already-hooked native battery-hide semantic as the authority.

### Implementation

- No new SystemUI hook or polling source.
- `SystemUiNativeBatterySuppressionOwner` now forwards the verified native `setIsHideBattery(Boolean)` result to the Combined Status participant owner.
- The module-owned `ModernStatusBarView` shell uses width 0 while native battery layout is present.
- While native battery layout is hidden, the shell width becomes the current resolved Combined Status visual/native-slot width.
- On native battery return, shell width returns to 0.
- Width changes are event-driven and request a native layout pass.
- Build 386 post-layout real visual bounds remain for the zero-occupancy mode.
- Build 387's native `NewStatusIconState` translation-target adapter remains.
- No peer translation, live View translation write, fixed pixel offset, frame listener, polling loop, or separate charging/island state machine is added.
- Internal build advances to `20260926-388`; display version remains `0.0.1`.

### Review

- **Authority review:** `MiuiStatusBatteryContainer.setIsHideBattery(Boolean)` remains the single semantic owner for whether the battery region is released.
- **Geometry review:** Combined Status changes only its own slot occupancy; peer measurement/layout and live motion remain HyperOS-owned.
- **Normal-state review:** battery present keeps Build 386's zero additional occupancy.
- **Island review:** battery hidden lets Combined Status claim its own resolved visual width, preventing native peers from using the same released region.
- **Performance review:** no new hook count and no per-frame work; only a layout-width update on a native hide-state change.
- **Future sizing review:** the reserved width comes from the resolved visual width instead of a hard-coded 105px constant.
- **Fallback review:** invalid/unknown visual width does not create speculative occupancy.

### CI / test gate

Fast Build and signed Work Branch Canary are pending.

After CI, validate:
1. charging Super Island enter / steady / exit keeps Combined Status fully inside the right edge;
2. native peer icons do not overlap Combined Status at any island phase;
3. HyperOS motion remains continuous without a module translation jump;
4. non-charging steady placement remains unchanged;
5. OFF -> ON native entry animation remains visible;
6. shade / Control Center first and last frames remain aligned;
7. detailed diagnostics show `slotOccupancy nativeBatteryHidden=true targetLayoutWidth=<visualWidth>` on island entry and restoration to 0 on exit.

If Build 388 fails, reopen native measurement/order ownership. Do not add offsets or peer-translation patches.

### Device validation and final work-branch review update — Build 387

The focused Build 387 device gate is now accepted from the supplied screen recording and the matching detailed diagnostic session.

#### Device evidence

- Charging Super Island enter/steady/exit keeps the Combined Status visual fully within the right status-bar boundary; the previous Build 386 right-edge eviction is not reproduced.
- Motion remains continuous with SystemUI; no project-owned translation jump is visible in the supplied recording.
- The same diagnostic session preserves the restored native OFF -> ON APPEAR from Build 386. The Combined Status root has real 105x108 visual bounds, centered pivot geometry, and native alpha/scale progression.
- Home -> shade / Control Center samples keep the Combined Status state target at `layoutTranslationX=478.0` while the native battery is separately translated/faded by HyperOS during charging/island behavior.
- The state adapter continues to report `moduleViewTranslationWrites=0`, so HyperOS remains the only live View translation/Folme writer.

#### Root-cause conclusion

Build 387 confirms the remaining Build 386 charging defect was a **state-target semantic mismatch**, not a need for another live View writer or charging-specific offset. A zero-occupancy Combined Status participant must resolve its native state target to the battery slot's layout coordinate while leaving battery-only Super Island eviction to the native battery presentation.

#### Review

Final work-branch review rechecked the active implementation against the latest `CONTRIBUTING.md`, the exact target SystemUI contracts, and `SystemUI-Reference/findings/statusbar.md`, `control-center.md`, and `charging.md`.

- **Scope:** PR #100 remains within the Home slot / native transition geometry boundary.
- **Ownership:** HyperOS owns native measurement/state calculation, live translation, alpha/scale/Folme timing, island state, and peer geometry. Combined Status owns only its custom post-layout visual bounds and its custom state target adaptation.
- **Lifecycle:** all three participant hooks are counted and reset together; partial installation rolls back installed hooks; host/battery/root references remain weak or generation-scoped.
- **Performance:** event-driven constant-time hook work only; no polling, per-frame project animation writer, repeated tree traversal, or unbounded diagnostics were introduced.
- **Fallback/compatibility:** exact target-contract failure keeps the native participant from partially activating. No hard-coded 105px translation compensation is used.
- **Alternative review:** direct `View.translationX` writes, charging-state offsets, reintroducing full measured slot occupancy, and hiding Combined Status with the battery remain rejected because they violate one-writer, geometry-separation, or product-semantics boundaries.

#### Validation continuity

The tested runtime commit is `9cce4d2ea1e8ddf2512b1db5df4ac55dd9ff235c`. The later branch-head delta before this record contains only `CURRENT.md` / `DEVLOG.md` development-document updates, so it does not change APK/runtime behavior. Build 387 Fast #1033 and Work Branch Canary #292 remain the applicable automated validation for the runtime tree.

#### Outcome / next step

Build 387 passes the focused work-branch device gate and closes the original three-symptom loop plus the charging-island right-edge regression. PR #100 is ready for squash merge into `dev`, followed by trusted Integration CI on the resulting integrated baseline. Promotion to `main` remains a separate maintainer decision after the integrated baseline is validated.

### Correction — Build 387 device gate was not accepted

The immediately preceding Build 387 validation note is superseded by a closer review of the supplied recording and the repository state that had already advanced to Build 388.

#### Corrected visual interpretation

- Build 387 **does** keep the Combined Status visual inside the right screen boundary during charging Super Island.
- However, the recording clearly shows native peer status icons moving into the same released battery region and overlapping the Combined Status visual.
- Therefore Build 387 does **not** pass the charging-island coexistence gate and PR #100 is not ready to merge to `dev`.

#### Diagnostic confirmation

The matching diagnostic explains the overlap structurally: normal Home has a 478px status-icon region beside a 105px battery slot; when HyperOS applies native battery hide, `MiuiStatusIconContainer` expands to 583px. Build 387 still contributes 0px measured occupancy while drawing a 105px visual, so peers are allowed to occupy that region. The maintained `layoutTranslationX=478.0` and `moduleViewTranslationWrites=0` show that right-edge translation ownership is no longer the remaining defect.

#### Active correction

Build 388 is the current runtime checkpoint. It uses the existing authoritative `MiuiStatusBatteryContainer.setIsHideBattery(Boolean)` event to switch only the module-owned participant occupancy: 0px while the native battery slot is present, resolved visual/native-slot width while HyperOS has released that battery slot, then back to 0px on return. HyperOS remains owner of peer geometry and live Folme translation.

#### Process correction

This correction is intentionally appended rather than rewriting the earlier note. The earlier acceptance statement was made from an incomplete interpretation of the recording and became inconsistent with the already-present Build 388 repository evidence. `CURRENT.md` has been corrected immediately; Build 388 CI/device validation is now the active gate.

### CI validation update — Build 388

- Runtime commit: `bb840c96a9d7ea2376dfb6b02048e91a9976e1fa`.
- Fast Build #1038: **success**.
- Work Branch Canary #297: **success**.
- Canary checked out trusted work-branch SHA `d57f35664e435722025809d3acba456f44ee3883`; the delta after the Build 388 runtime commit is development documentation only.
- Pinned HyperOS target profile: success.
- Modern Xposed metadata: success.
- Haple APK signature verification: success.
- Canary non-debuggable verification: success.
- Artifact ID: `10910080114`.
- Artifact archive digest: `sha256:a18273b7f35646174db9181079b40ad0bd4027bd68238b38c2942006b894baaa`.
- Extracted APK SHA-256: `d737521d285fdc35433c1e5b852db2063ef637362c45e0ff2ebfc9a0fece57f8`.
- Extracted APK size: `3375134` bytes.

### Post-CI review

- **Authority:** the occupancy handoff is driven only by the verified native `MiuiStatusBatteryContainer.setIsHideBattery(Boolean)` result; no duplicate charging/island semantic source was added.
- **Ownership:** while the native battery slot exists, Combined Status keeps 0px additional measured occupancy. Only after HyperOS releases that slot does the module-owned participant claim its own resolved visual width; peer geometry and live translation remain HyperOS-owned.
- **Lifecycle:** activation seeds the current native hide value, verified hide callbacks update it, and participant reset/hot reload clears the state. No persistent polling or frame listener was added.
- **Performance:** the new work occurs only on native hide-state changes and the resulting normal layout traversal.
- **Fallback:** an invalid visual width does not create speculative occupancy; exact native hook/participant readiness remains fail-closed.

### Remaining device gate

CI proves source/build/signing/metadata correctness, not SystemUI runtime behavior. Device validation is still required for charging Super Island peer separation, right-edge containment, motion continuity, non-charging steady placement, OFF -> ON APPEAR, and shade / Control Center first/last-frame alignment. PR #100 remains unmerged until this gate passes.


---

## 2026-09-26 — Build 389: keep Combined Status on the stable end-side slot anchor

**Type:** runtime island/motion-anchor correction  
**APK build:** 20260926-389  
**CI:** pending at commit creation  
**Device validation:** pending

### Build 388 device result

Build 388 fixes the Build 387 overlap boundary by claiming module-owned participant occupancy while HyperOS releases the native battery region. The supplied Build 388 screen recording nevertheless shows a remaining visual mismatch: the native peer icons do not move with the Combined Status visual during charging Super Island entry/exit.

The matching detailed diagnostic confirms this is not a missing peer-translation writer. In the captured charging baseline, `MiuiBatteryMeterView` is 135px wide and starts at layout x=452 with a 30px battery motion translation, while the stable status-icon/battery boundary remains at x=482. The existing state-target adapter resolves Combined Status from the battery view's `left`, producing an initial target of 448. Later native charging/island layout returns the custom target to 478 while Wi-Fi/mobile peer targets remain 373/281. The recording shows the corresponding roughly 30px relative-motion split.

### Root cause

`MiuiBatteryMeterView.left` is not the native battery-slot boundary under charging. It is presentation/motion geometry and can change with the 105/135 battery presentation path. Build 387 therefore reused the wrong geometry authority even though it correctly left live `View.translationX` to HyperOS.

This directly reaffirms the repository's earlier confirmed rule: native slot geometry, Combined Status visual geometry, and battery/transition motion geometry must remain separate.

### References consulted

- latest `CONTRIBUTING.md` sections 3.1-3.4, 4.1-4.4, 5.1, 10 and 11;
- current `CURRENT.md`, `ROADMAP.md`, and recent `DEVLOG.md`;
- Build 388 maintainer screen recording and detailed diagnostic;
- `SystemUI-Reference/findings/statusbar.md`: `MiuiStatusIconContainer` owns native participant measurement/state transitions and slot geometry must remain distinct from motion geometry;
- `SystemUI-Reference/findings/charging.md`: native battery hide remains `MiuiStatusBatteryContainer.setIsHideBattery(Boolean)`; no project charging/island state machine is needed.

### Alternatives reviewed

1. Move native peer icons explicitly — rejected; peer geometry and live motion remain SystemUI-owned.
2. Copy the battery view's 30px charging delta to peers — rejected; this is a transient presentation artifact and would create a second motion writer.
3. Animate the Build 388 occupancy width per frame — rejected; it introduces a project-owned animation path and is unnecessary if the custom target uses the correct stable boundary.
4. **Selected:** keep Build 388 occupancy logic unchanged, but source the custom `NewStatusIconState` translation target from the stable laid-out end-side status-icon boundary captured while the native battery slot is present. Preserve that anchor while native battery layout is hidden.

### Implementation

- Add one generation-scoped cached slot translation anchor owned by the native Combined Status participant.
- Seed it from the laid-out `MiuiStatusIconContainer` width/boundary at attach.
- Refresh it only after native `MiuiStatusIconContainer.onLayout(...)` while the native battery slot is present.
- Freeze the last verified anchor while `setIsHideBattery(true)` releases the battery region.
- Use the same anchor for custom `NewStatusIconState.translationX/layoutTranslationX` adaptation and handoff-readiness screen coordinates.
- Battery `left`, width and translation remain diagnostic evidence only; they no longer define the Combined Status target.
- Build 388 occupancy handoff remains unchanged.
- No new hook, observer, polling loop, frame listener, peer geometry write or live `View.translationX` write is added.
- Internal build advances to `20260926-389`; display version remains `0.0.1`.

### Review

- **Authority review:** stable end-side layout boundary replaces battery presentation geometry as the translation target source.
- **One-writer review:** HyperOS remains the sole live translation/Folme writer for Combined Status and all peers.
- **Lifecycle review:** the cached anchor is generation-scoped and cleared on Hot Reload/runtime reset.
- **Performance review:** one constant-time boundary refresh inside the already-owned post-layout hook; no new high-frequency source.
- **Fallback review:** missing/invalid positive layout boundary fails the native participant path instead of guessing an offset.
- **Regression boundary:** occupancy behavior from Build 388, visual-bounds behavior from Build 386, and native state application remain otherwise unchanged.

### CI / device gate

Fast Build and signed Work Branch Canary are pending.

Focused device acceptance:
1. charging-island entry/exit no longer shows a CombinedStatus-only ~30px shift relative to native peers;
2. Build 388 peer separation remains;
3. Build 387 right-edge containment remains;
4. non-charging steady placement and native OFF -> ON APPEAR remain;
5. shade / Control Center first/last-frame alignment remains;
6. diagnostic uses `authority=native-end-side-slot-boundary` and continues to report `moduleViewTranslationWrites=0`.

If this still fails, reopen the native `NewStatusIconState` / island-state ordering boundary rather than moving peers or adding per-frame compensation.


### CI validation update — Build 389

- Runtime commit / exact tested work-branch SHA: `3ab0944abaf585f8afc79f483de8d9f08ca11906`.
- Fast Build #1039: **success**.
- Work Branch Canary #298: **success**.
- Pinned HyperOS target profile: success.
- Modern Xposed metadata verification: success.
- Haple APK signature verification: success.
- Canary non-debuggable verification: success.
- Artifact ID: `10910190177`.
- Artifact archive digest: `sha256:2a1964d5bc4231a9c398684ccfcbe470faad044e9b15b58aed1864eba440f788`.
- Extracted APK SHA-256: `22fc81d38a5552ff50bf91e6e1b5c0ec9dd755eac7f76baf86a53b4a56984a22`.
- Extracted APK size: `3375134` bytes.

### Post-CI review

The Fast and signed-Canary gates validate the source/build/signing/metadata boundary for Build 389. The runtime delta remains limited to the module-owned stable slot translation anchor; Build 388 occupancy handoff, peer geometry ownership, native live Folme translation, and existing lifecycle/fallback paths remain unchanged. No additional hook, observer, polling source, frame writer, peer translation or hard-coded pixel compensation was introduced.

Device evidence remains the acceptance authority for the motion fix. PR #100 stays unmerged until the focused charging-island and non-charging regression gate passes.


---

## 2026-09-27 — Build 390: read-only per-participant island motion trace

**Type:** focused runtime diagnostic; no intended feature-behavior change  
**APK build:** 20260927-390  
**Reason:** Build 389 device feedback reports the same relative-motion mismatch.

### Problem / evidence

The Build 389 detailed session confirms the new target authority is active, but it also exposes a stronger structural fact: during authoritative charging-island entry the native `MiuiStatusIconContainer` expands to 583px and its screen X changes only by roughly 10px over the bounded sample, while `MiuiBatteryMeterView` moves more than 100px and fades. The module still reports no native peer geometry writes.

The existing diagnostic does not capture the live screen X / translation of the actual `combined_status` child and the visible peer status-icon children in those same frames. Therefore two materially different hypotheses remain:
1. Combined Status is receiving an extra child-level native motion that peers do not receive.
2. Combined Status is visually stationary relative to the container, while the perceived mismatch comes from occupancy/order or another container/state transition.

Changing geometry again before distinguishing these would violate the root-cause-first and evidence-change rules.

### References reviewed

- latest `CONTRIBUTING.md` sections 3.1-3.4, 4.1-4.4 and 5.1;
- Build 389 detailed diagnostic and maintainer visual feedback;
- current `CURRENT.md`, `ROADMAP.md`, and recent `DEVLOG.md`;
- `SystemUI-Reference/findings/statusbar.md`: native `MiuiStatusIconContainer` owns bindable participant measurement and APPEAR/DISAPPEAR/MOVE/ISLAND transitions;
- `SystemUI-Reference/findings/scene-host-motion.md`: the Home island listener owns `mStatusContainer`, `mEndSideContent`, `mStatusBarIcons`, `mBatteryContainer`, and `mBatteryView`; battery presentation motion must not be copied blindly.

### Review / selected approach

**Selected:** extend only the already-existing, detailed-diagnostics-only island pre-draw probe. At island callback start, snapshot up to ten visible direct children of `MiuiStatusIconContainer` plus `combined_status`, resolving each child's slot once. Each bounded sample records child left, screen X, actual/measured width, live translation X, alpha and visibility.

**Rejected for Build 390:** peer translation writes, Combined Status compensation, another slot-width change, battery-trajectory copying, or a new island state machine. None is justified until the same-frame child motion is measured.

### Runtime cost / lifecycle review

- no new hook;
- no new persistent listener;
- no polling;
- no new writer;
- no native geometry mutation;
- child discovery occurs once per authoritative island callback only when detailed diagnostics are enabled;
- sampling reuses the existing 16-sample / 900ms bounded pre-draw probe and is disposed by the same generation/timeout path;
- tracked views remain weak references.

### Acceptance

One short device capture must show `statusChildren=[...]` for island enter/exit. The next implementation decision will be based on same-frame Combined Status vs peer screen-X/translation deltas, not visual guessing.

PR #100 remains unmerged.


### CI validation update — Build 390

- Exact tested work-branch SHA: `7531a43bbaac7d1d68c649f84a67224fb4f186ce`.
- Fast Build #1040: **success**.
- Work Branch Canary #299: **success**.
- Artifact ID: `10910261548`.
- Artifact archive digest: `sha256:7ac12e94efd4a769046da44f80b0eb9d0cd1dfc4e8b18d79be0ba994d65fcd00`.
- Extracted APK SHA-256: `76e82cd0fb5d9e4e8330987e26aba2353e64e8e274cd35818c889cd79c38b699`.
- Extracted APK size: `3375134` bytes.
- The Canary workflow checked out the exact work-branch SHA above and completed successfully.

### Current gate

Build 390 remains diagnostic-only. No motion/geometry behavior has been intentionally changed from Build 389. The next evidence required is one detailed-diagnostics charging-island enter/steady/exit capture containing the new `statusChildren=[...]` samples. That same-frame child data will decide whether the next runtime change belongs to Combined Status child state, participant occupancy/order, or a higher native owner.

PR #100 remains unmerged.


### Build 390 visual A/B finding — attach state may be the missing variable

A new Build 390 recording does **not** reproduce the earlier Build 389 relative-motion split. Frame comparison shows the visible peer cluster and Combined Status maintain constant horizontal separation through both charging-island entry and exit.

This cannot be attributed to Build 390 code: the 389 -> 390 runtime delta is diagnostic-only in `SystemUiIslandMotionSource`; it adds read-only child snapshots and does not alter geometry, motion, occupancy, state targets, or writers.

The earlier Build 389 detailed session did, however, start with charging already active. At attach, the native battery measured 135px and Combined Status resolved `activeSlotWidth=135` / render width 135 from that battery-container measurement. The current good recording visibly starts from an uncharged steady state before charging begins.

**Leading hypothesis:** the participant's slot/visual width is attach-state dependent. `activeSlotWidth` is seeded once from `NativeStatusBarSlotGeometry.resolve(...)` during attach; island occupancy later consumes the renderer's measured width or that cached slot width. Attaching while the battery is already in the 135px charging presentation can therefore create a different persistent participant geometry than attaching in the ordinary battery state.

This is a stronger explanation than another translation offset because Build 390 behavior is otherwise identical to Build 389.

Next gate is one same-build single-variable A/B:
1. attach/reload while uncharged -> then charge;
2. attach/reload while already charging -> then repeat the island cycle.

Export a fresh detailed diagnostic for each case. If only case 2 reproduces the split and shows a 135px attach-time slot/render width, the fix should normalize participant slot identity against the stable native battery slot contract rather than the transient charging presentation width.


---

## 2026-09-27 — Build 391: normalize attach-time native slot identity

**Type:** root-cause runtime geometry correction  
**APK build:** 20260927-391  
**Device evidence source:** Build 390 charging-attached A/B case

### Confirmed root cause

The Build 390 A/B closes the attach-state hypothesis.

In the failing charging-attached session:
- battery state is already `pluggedIn=true charging=true` during runtime attach;
- the laid-out native status-icon region remains 478px wide, preserving the stable 105px end-side slot in the 587px container with 4px start padding;
- the same status-icon container reports a transient measured width of 448px while the charging battery presentation is 135px;
- participant attach used `statusIcons.measuredWidth`, so `587 - 4 - 448 = 135` became `activeSlotWidth`;
- renderer width and later released-slot occupancy consequently became 135px;
- the new same-frame child trace confirms Combined Status then follows a different child-level motion trajectory from fixed native peers such as Wi-Fi/mobile.

This is not an island interpolation defect. It is an attach-time slot-identity defect caused by treating transient measurement geometry as the stable layout boundary.

### Problem execution flow

1. User visual report identified inconsistent peer motion.
2. Build 389/390 code review proved 390 did not change runtime behavior.
3. Single-variable A/B isolated attach-while-charging as the reproducer.
4. Build 390 bounded child trace measured the exact native child trajectories.
5. Slot-resolution review located the source mismatch: translation already preferred `statusIcons.width`, but slot width still consumed `statusIcons.measuredWidth`.
6. The stable laid-out boundary and transient charging measurement differ by exactly the previously observed 30px.
7. Correct the source boundary instead of adding animation compensation.

### References / rules reviewed

- latest `CONTRIBUTING.md`: root-cause-first, evidence-change, one live writer, geometry separation, lightweight diagnostics, device evidence gate;
- current `CURRENT.md`, `ROADMAP.md`, recent `DEVLOG.md`;
- `SystemUI-Reference/findings/statusbar.md`: native slot/layout geometry must remain separate from battery presentation/motion geometry;
- `SystemUI-Reference/findings/charging.md`: native battery hide remains the authoritative layout-release event;
- Build 390 detailed diagnostic and maintainer recording.

### Selected correction

At participant attach:
- prefer each already-laid-out native sibling's `width` as its stable occupancy;
- use `measuredWidth` only as a pre-layout fallback;
- feed that resolved stable width into `NativeStatusBarSlotGeometry.resolve(...)`;
- log layout, measured, and resolved widths separately.

For the failing observed geometry this changes only the source:
- before: `statusIconsMeasuredWidth=448 -> resolvedSlot=135`;
- after: `statusIconsLayoutWidth=478 -> resolvedStatusIconsWidth=478 -> resolvedSlot=105`.

### Review

- **Geometry review:** fixes slot identity at its source; no offset or interpolation patch.
- **One-writer review:** HyperOS remains the sole live translation/Folme writer.
- **Peer review:** no peer native geometry write.
- **Island review:** Build 388 occupancy release contract remains unchanged; it now consumes the normalized stable visual/slot width.
- **Lifecycle review:** no new persistent state or listener.
- **Performance review:** constant-time width selection at participant attach only.
- **Fallback review:** measured width remains available only when no positive laid-out width exists; invalid geometry still fails closed.
- **Diagnostic review:** Build 390 bounded child trace remains detailed-only while this regression is validated.

### Device gate

Both attach orders must converge:
1. uncharged attach -> charge Super Island enter/steady/exit;
2. already-charging attach -> unplug/replug -> Super Island enter/steady/exit.

Acceptance requires:
- resolved stable slot 105px in both paths;
- no CombinedStatus-only relative-motion split;
- no peer overlap;
- no right-edge escape;
- non-charging steady placement unchanged;
- OFF -> ON APPEAR preserved;
- shade / Control Center first/last-frame alignment preserved.

PR #100 remains unmerged.


### CI validation update — Build 391

- Exact tested work-branch SHA: `b4d8bb7f6aee567dc131a983c9e9323af7bdd1af`.
- Fast Build #1041: **success**.
- Work Branch Canary #300: **success**.
- Pinned HyperOS target profile: success.
- Modern Xposed metadata verification: success.
- Haple APK signature verification: success.
- Canary non-debuggable verification: success.
- Artifact ID: `10910308834`.
- Artifact archive digest: `sha256:3d7bac217ca55909e8a5f7b3ea4de0c61ee1487d8015144f9cd0e32262a8f86e`.
- Extracted APK SHA-256: `63ac90e44d50c54220723ce518c53eacac81e19edebfd3db6eed019233de1a06`.
- Extracted APK size: `3375134` bytes.

### Post-CI review

The runtime delta is restricted to attach-time sibling-width authority selection plus its regression tests. No island callback behavior, animation curve, live translation writer, peer geometry write, or occupancy lifecycle has changed. Device validation remains the authority for confirming that charging-attached and uncharged-attached sessions now converge to the same stable 105px participant identity.

PR #100 remains unmerged.


---

## 2026-09-27 — Build 392: consume the stable host slot snapshot

**Type:** root-cause lifecycle/geometry correction  
**APK build:** 20260927-392  
**Predecessor result:** Build 391 rejected on device

### New evidence

Build 391 proves the previous source correction was still too late.

In the charging-attached session:
- at host capture, the native topology reports `MiuiStatusIconContainer=478px` and native battery `105px`;
- the existing `StatusBarStableSession` then records `statusIconsWidth=478` while charging battery presentation becomes 135px;
- before the native Combined Status participant attaches, HyperOS performs another charging layout and the same status-icon container becomes `layoutWidth=448`, `measuredWidth=448`;
- Build 391 correctly prefers layout over measured width, but both are already transient by that lifecycle point, so it still resolves `587 - 4 - 448 = 135px`.

The failure therefore moves the responsible boundary again: stable-vs-transient geometry is a lifecycle timing issue, not a property-type issue.

### Root cause

A valid stable end-side occupancy snapshot already exists earlier in the host lifecycle, owned by `StatusBarStableSession`. Native participant attach resampled the live container later instead of consuming that host-scoped snapshot.

This creates an avoidable second geometry authority and allows charging presentation timing to change participant identity.

### Selected correction

- keep `StatusBarStableSession` as the one-shot owner of the early stable host geometry;
- retain its captured `SlotMetrics` in the session and expose it only for the matching host;
- native participant attach prefers captured `statusIconsWidth`;
- live `View.width` then `measuredWidth` remain fallback sources only if a matching captured value is unavailable;
- keep current privacy handling unchanged;
- log captured, live-layout, live-measured, and resolved widths independently.

Expected failing-path conversion:
- capture: 478;
- later live layout/measure: 448/448;
- resolved occupancy source: 478;
- stable participant slot: 105px.

### Review

- **Root-cause review:** fixes the lifecycle authority mismatch; no translation compensation.
- **Ownership review:** one host-scoped stable geometry owner; native participant becomes a consumer instead of resampling a competing stable fact.
- **Writer review:** HyperOS remains sole live layout/translation animation writer outside the already accepted custom participant occupancy boundary.
- **Hook review:** no new hook.
- **Performance review:** one in-memory host-scoped snapshot read at participant attach.
- **Lifecycle review:** snapshot dies with `StatusBarStableSession` on detach/host replacement; host identity must match.
- **Fallback review:** absent/invalid captured width falls back to current layout then measured width; invalid final geometry still fails closed.
- **Regression review:** Build 388 occupancy handoff, Build 389 state target adapter, Build 390 bounded diagnostic trace, tint/network suppression, and panel transition behavior are otherwise unchanged.

### Device gate

Test charging-attached first. Required diagnostic:
`stableCaptureStatusIconsWidth=478 statusIconsLayoutWidth=448 statusIconsMeasuredWidth=448 resolvedStatusIconsWidth=478 ... resolvedSlot=105x108`.

Then verify:
1. charging-attached -> unplug/replug has coherent peer spacing;
2. uncharged-attached -> charge remains coherent;
3. no peer overlap or right-edge escape;
4. OFF -> ON APPEAR remains visible;
5. shade / Control Center first/last frames remain aligned.

PR #100 remains unmerged.


### CI validation update — Build 392

- Exact tested work-branch SHA: `18c563b318cf53f68f51f95a079ff6edd0b4186e`.
- Fast Build #1042: **success**.
- Work Branch Canary #301: **success**.
- Pinned HyperOS target profile: success.
- Modern Xposed metadata verification: success.
- Haple APK signature verification: success.
- Canary non-debuggable verification: success.
- Artifact ID: `10911296876`.
- Artifact archive digest: `sha256:99674450d14cf206255e06b7d599705d6c87a470bb2d6556172b865564f4867b`.
- Extracted APK SHA-256: `777fc688ad8290197ee176d795b5842e8a754d248f85069a5cfb1154d9e24285`.
- Extracted APK size: `3375134` bytes.

### Post-CI review

The runtime change remains limited to consuming an already-owned host-scoped stable geometry snapshot at participant attach. No new SystemUI hook, listener, poller, animation/state machine, peer write, or live translation writer was added. Build 390's bounded same-frame diagnostics remain available to verify the resulting child motion.

Device evidence remains the acceptance authority. PR #100 stays unmerged.


---

## 2026-09-27 — Build 393: pin width and translation to one stable slot boundary

**Type:** root-cause geometry-authority correction  
**APK build:** 20260927-393  
**Predecessor result:** Build 392 rejected for charging-state left shift in both A/B attach orders

### New device evidence

Build 392 successfully proves and consumes the stable host slot snapshot for **width**:
`stableCaptureStatusIconsWidth=478`, live charging width `448`, `resolvedStatusIconsWidth=478`, `resolvedSlot=105x108`.

However the same line records `slotTranslationX=448.0`, and the later state adapter also corrects the custom participant to the 448px live boundary. The video shows the resulting charging-state left shift.

The geometry is therefore internally inconsistent:
- visual/slot width = 105px from stable boundary;
- position = 448px from transient charging boundary;
- expected stable end-side slot = 478..583;
- actual custom visual = 448..553;
- error = 30px left.

The island trace independently supports this: when the native status-icon container expands during battery-slot release, Combined Status reaches a 478px target while several peer targets remain native-owned, producing the previously observed relative movement.

### Root cause

Build 392 corrected only one half of slot identity. `activeSlotWidth` consumes the host-scoped stable snapshot, but `activeSlotTranslationX` and its post-layout refresh still consume live `MiuiStatusIconContainer.width`. Charging presentation shrinks that live width from 478 to 448, so the same conceptual slot has two authorities.

### Selected correction

- introduce a generation-scoped `activeSlotBoundaryWidth`;
- seed it from the same resolved stable status-icon width that feeds slot-width resolution;
- derive `activeSlotTranslationX` from that stable boundary;
- post-layout refresh keeps using the stable boundary, recomputing only against current root-local `left`;
- live status-icon width remains fallback only if no stable boundary exists;
- clear the boundary on normal teardown and Hot Reload reset.

### Review

- **Geometry review:** native slot width and custom slot position now share one authority.
- **Animation review:** no animation curve/progress change.
- **Writer review:** HyperOS remains sole live `View.translationX` / Folme writer; the module still adapts only its custom state target.
- **Peer review:** no peer geometry write.
- **Lifecycle review:** stable boundary is generation-scoped and cleared with the participant.
- **Performance review:** no new hook/listener/poller or per-frame work.
- **Fallback review:** live width is used only when the stable boundary is unavailable; invalid geometry still fails closed.
- **Regression boundary:** Build 392 stable slot width, Build 388 released-slot occupancy, Build 390 diagnostics, network/battery suppression, and panel integration remain otherwise unchanged.

### Device gate

Charging steady state must first show:
`stableSlotBoundaryWidth=478 ... statusIconsLayoutWidth=448 ... resolvedSlot=105x108 slotTranslationX=478.0`.

Then verify:
1. no 30px left shift in either A or B attach order;
2. island enter/exit preserves peer-relative spacing;
3. no peer overlap / right-edge escape;
4. OFF -> ON APPEAR remains;
5. shade / Control Center first and last frames remain aligned.

PR #100 remains unmerged.


### CI validation update — Build 393

- Exact tested work-branch SHA: `ee76d8d5319fff4efcc640314318881fecd716ba`.
- Fast Build #1043: **success**.
- Work Branch Canary #302: **success**.
- Pinned HyperOS target profile: success.
- Modern Xposed metadata verification: success.
- Haple APK signature verification: success.
- Canary non-debuggable verification: success.
- Artifact ID: `10911242961`.
- Artifact archive digest: `sha256:cb8d1e8bd2c5b7e4f580553b4331b296db6afa4598c420ea7506c3f9751a4818`.
- Extracted APK SHA-256: `da6e55434dceb81cdaf746a9d725011795105bc346e2242e9c508e8cda674623`.
- Extracted APK size: `3375134` bytes.

### Post-CI review

The runtime delta is restricted to pinning the module-owned slot translation target to the same stable host boundary already used for the normalized slot width. No island animation curve, peer geometry, live View translation, occupancy lifecycle, network/battery suppression, or panel transition owner changed. Build 390's bounded child-motion diagnostics remain enabled for device confirmation.

PR #100 remains unmerged pending device evidence.


---

## 2026-09-27 — 0.0.2 development line opened: unify Combined Status geometry

**Type:** version-boundary / architecture decision  
**Display version:** 0.0.2  
**Runtime build:** not created by this documentation/version checkpoint

### Why the display version advances

The maintainer explicitly approved advancing from 0.0.1 to 0.0.2 because the current work has crossed from a narrow charging/island defect fix into a structural geometry redesign.

The 0.0.2 runtime direction is to make one resolved layout contract authoritative for:
- native end-side slot semantics;
- Combined Status drawing/visual geometry;
- optical neighbor spacing;
- requested/adaptive occupancy;
- transition / projection geometry.

User-facing size and spacing controls are still a later product/UI task, but their underlying geometry contract is pulled forward now so future controls only change layout parameters and do not require another SystemUI hook/animation redesign.

### Acceptance standard

Internal implementation may change substantially, but the installed SystemUI result must behave as one coherent native participant:
- stable visual placement;
- consistent optical spacing to neighboring icons;
- coherent native APPEAR/DISAPPEAR;
- coherent charging/Super Island motion;
- coherent Home <-> shade / Control Center first/last frames;
- future scaling must preserve the same rules rather than adding scene-specific offsets.

### Governance

The existing 0.0.1 Build 386-393 experiments remain evidence, not architecture. The unfinished pre-0.0.2 Build-394 battery-slot experiment is provisional and must be either reconciled with the unified resolved-layout model or reverted before the first real 0.0.2 runtime checkpoint.

The display-version change itself does not claim a validated runtime build and intentionally does not advance the Build ID.


---

## 2026-09-27 — 0.0.2 architecture reference review and reference-library baseline

**Type:** architecture investigation / reference-library preparation  
**Runtime build:** none  
**Display line:** 0.0.2  
**Runtime behavior changed:** no; the earlier provisional battery-slot override experiment was removed before this record was finalized.

### Problem / objective

Builds 386-393 repeatedly solved one geometry boundary while exposing another around steady occupancy, native APPEAR, charging presentation width, battery-slot release, peer motion, and panel handoff.

The objective was to stop extending the existing participant model by assumption and inspect a mature implementation of the same class of compact status composition before defining Build 394.

The review was intentionally performed before another runtime change.

### Problem execution flow

1. Build 393 device feedback showed that both tested attach orders still have a charging-state visual-spacing defect.
2. The current participant route was classified as an architecture question rather than another offset defect.
3. The 0.0.2 display line was opened.
4. A mature Android/SystemUI implementation was inspected at bytecode/runtime-contract level.
5. Host ownership, measure/layout participation, native-view masking, scene progress, projection, sizing, and cleanup were traced.
6. The findings were generalized and stripped of source-specific product/internal naming before being stored in the repository.
7. The provisional experiment that overrode native battery-hide layout behavior was reverted because the completed review did not support taking that platform-owned scene responsibility.
8. No Build 394 was created; exact target-SystemUI proof remains required.

### Observed reusable patterns

#### Existing native host as the compact carrier

The compact representation reuses an existing native end-side host rather than registering a second permanent status-icon participant.

This avoids the need for two independent layout identities to exchange occupancy during scene changes.

#### Scoped represented-slot suppression

Represented native slots are temporarily added to the platform's existing ignored-slot collection only around native measure/layout.

Only entries newly added by the replacement path are recorded. A restoration token removes exactly those entries after the native call and on exceptional exit.

The platform collection is not globally cleared or replaced.

#### Reversible native-view visual masking

Native Views remain attached and state-capable while their drawing is suppressed through a reversible clip boundary.

The pre-existing clip state is saved once and restored exactly when the compact presentation is no longer active.

This separates visual replacement from layout/lifecycle removal.

#### Host-scoped state and cleanup

Runtime composition state is owned per native host. Host state includes native references, resolved sizing, represented slots, overlay presentation, scene state, and cleanup.

Detached hosts are cleaned and removed rather than leaving geometry or references globally reusable.

#### Independent sizing dimensions

Layout slot size, visible glyph size, per-glyph scale, and optical adjustment are modeled independently.

User scaling resolves a sizing/layout object; it does not rewrite integration hooks.

#### Native scene/hide semantics as input

Platform scene/hide state is read as an authoritative fact for presentation eligibility. No evidence was found that the implementation preserves its compact host by overriding the platform's battery-hide request.

This is important negative evidence against the provisional forced-slot experiment.

#### Native progress and real endpoints for projection

Cross-surface transition progress is consumed from a native expansion callback.

Source and target endpoints are derived from real screen geometry. The projection itself is drawn with canvas translation, scale, and alpha rather than taking ownership of native target View translation or introducing an independent timing curve.

#### Layered restoration

The implementation separates:
- temporary layout mutation lifetime;
- steady compact visual-mask lifetime;
- transition projection lifetime.

Each layer restores only its owned state. Global cleanup removes overlays/listeners, restores tracked visual state, cleans host sessions, and returns to native behavior.

### Architecture review

**Review conclusion:** the existing extra-participant architecture is no longer assumed to be the required final 0.0.2 integration.

This does not invalidate the evidence collected by Builds 386-393. Those builds remain valuable proof about the target's APPEAR geometry, battery-slot release, peer occupancy, charging geometry and panel anchors.

The new evidence changes the preferred question from:

`How should the custom participant take over a disappearing native slot?`

to:

`Can Combined Status compose inside an existing native host for steady state, then hand off presentation through target-proven scene projection when that host is no longer available?`

### Product-specific difference that prevents mechanical copying

Combined Status carries network information in addition to battery state.

A platform scene may legitimately remove a battery-oriented host, but Combined Status must not automatically disappear with it if that would discard required network information.

Therefore the reference scene policy is not copied. The 0.0.2 target must prove either:
- a valid island-time carrier; or
- a draw-only island projection/handoff.

Native peer layout/motion should remain SystemUI-owned in either case.

### Repository reference library

Created:
- `docs/reference/README.md`
- `docs/reference/statusbar-composition-patterns.md`

The reference library intentionally contains:
- generalized architecture patterns;
- evidence/confidence boundaries;
- target-validation requirements.

It intentionally excludes:
- third-party product/package/internal names;
- copied source;
- proprietary assets;
- source-specific constants as architecture;
- claims that Home evidence proves keyguard/AOD behavior.

### Provisional experiment rollback

The pre-0.0.2 experiment that forced native battery layout hide to remain false was removed before Build 394.

Rollback commit:
`ccfbb2d0f3efa0c6646afa7ff80b4d592c9de74e`.

Reason:
- it takes ownership of a platform scene decision;
- it can alter island/end-side layout semantics;
- the completed reference review shows a mature alternative pattern that consumes native hide/scene state rather than rewriting it;
- keeping an unvalidated runtime experiment would contaminate the new architecture baseline.

### 0.0.2 next gate

No runtime Build 394 exists yet.

Before coding it:
1. verify an exact-target existing Home carrier;
2. verify the target ignored-slot / native measure-layout scope;
3. verify reversible masking;
4. map the island-time carrier or projection needed to retain network information;
5. map Home -> shade / Control Center native progress and real endpoints;
6. define the shared `ResolvedLayout` / sizing contract;
7. complete an ownership, lifecycle, cleanup, performance, compatibility and fail-native review.

PR #100 remains unmerged.


---

## 2026-09-27 — Roadmap split for 0.0.2 architecture and 1.0.0 release qualification

**Type:** roadmap / release-planning decision  
**Runtime build:** none  
**Runtime impact:** none

### Maintainer decision

The active macro route is refined without rewriting prior Build history.

- Current development display version remains `0.0.2`.
- The first planned formal release target is `1.0.0`.
- Development may continue through `0.0.x` versions until the 1.0.0 acceptance boundary is satisfied and the maintainer explicitly authorizes the formal version transition.

### Roadmap refinement

The previous Phase 2 combined two different engineering problems: selecting a stable Home presentation carrier and implementing Home -> shade / Control Center transition behavior.

It is now split into:

- **Phase 2A — 0.0.2 Home carrier / presentation architecture**
  - target host/carrier proof;
  - scoped represented-slot handling;
  - reversible native-view masking;
  - HostSession ownership and cleanup;
  - shared ResolvedLayout/sizing contract;
  - island-time carrier/handoff preserving network information.

- **Phase 2B — Home -> shade / Control Center projection**
  - native progress authority;
  - real source/target endpoints;
  - draw-only projection;
  - transition-specific masking/overlay lifetime and cleanup;
  - no custom timing or endpoint compensation.

Keyguard/AOD remains after Phase 2B. App Home/Preview Sandbox remains after scene-contract stabilization.

### Sizing boundary

The runtime sizing/layout contract is now a Phase-2A requirement.

Phase 5 remains the user-facing adaptive size/spacing/visual-controls phase and should expose already-stable resolved-layout inputs rather than redesigning runtime SystemUI integration.

### Superseded default route

The permanent extra status participant / occupancy-handoff route explored by Builds 386-393 is now explicitly **superseded as the default 0.0.2 architecture**.

Those builds remain valid historical evidence for individual target-SystemUI behaviors. They are not deleted, rewritten, or retroactively relabeled.

The route may be reconsidered only if later exact-target evidence invalidates the preferred existing-host composition direction and a new ownership review proves a safer participant contract.

### Formal release qualification

The final pre-release macro phase is now **1.0.0 release qualification**, including full supported-scene/device-state regression, cleanup/fail-native behavior, adaptive sizing/spacing, performance/energy boundaries, Release/signing/metadata checks, and public-document consistency.

Completing an earlier architecture phase does not itself advance the display version to `1.0.0`.

---

## 2026-09-27 — Phase 2A exact-target carrier review before Build 394

**Type:** architecture / exact-target evidence review  
**Runtime build:** none  
**Display line:** 0.0.2  
**Runtime impact:** none

### Problem / objective

The 0.0.2 line must select a Home carrier and island handoff without reviving the permanent extra-participant / occupancy-handoff architecture rejected after Build 393.

The immediate objective was to determine what the pinned target and existing runtime evidence already prove, what remains only generalized reference evidence, and what can be specified safely before any APK-affecting source change creates Build 394.

### Problem execution flow

1. Re-read the latest `CONTRIBUTING.md`, `CURRENT.md`, `ROADMAP.md`, recent `DEVLOG.md`, architecture policy and reference library on the active work branch.
2. Re-read the exact-fingerprint SystemUI Reference for Home status-bar ownership, battery/charging, scene/island motion and Control Center.
3. Re-inspect the Build-393 diagnostic instead of extending the prior slot correction.
4. Compare the exact-target evidence with the generalized existing-host / ignored-slot / reversible-mask pattern.
5. Separate facts already proven on the target from contracts that remain unverified.
6. Define the design-level shared `ResolvedLayout` input/output boundary without changing Kotlin/runtime code.
7. Keep Build 394 blocked until the missing target contracts are closed.

### Evidence / references actually consulted

Project sources:
- latest `CONTRIBUTING.md`;
- `docs/development/CURRENT.md`;
- `docs/development/ROADMAP.md`;
- recent Build-386–393 and 0.0.2 entries in this `DEVLOG.md`;
- `docs/architecture/README.md`, `layout-policy.md`, and `scene-policy.md`;
- `docs/reference/README.md` and `statusbar-composition-patterns.md`;
- current `CombinedStatusHomeRenderSession` and layout-policy source/tests.

Exact target reference:
- SystemUI `17.03.260226.r`, SHA-256 `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`;
- SystemUI-Reference `findings/statusbar.md`, `findings/charging.md`, `findings/scene-host-motion.md`, and `findings/control-center.md`;
- `CombinedStatus-Diagnostic-20260927-393-20260927-012041.txt`.

Established-pattern comparison:
- the repository's generalized mature composition reference;
- AOSP `StatusIconContainer` ignored-slot behavior and older public Xiaomi/MIUI examples were checked only as non-target implementation evidence. They do not establish the contract on the pinned HyperOS artifact.

### Exact-target findings

**Confirmed — Home attachment/lifecycle candidate.**  
The existing Home render session attaches the real Combined Status renderer through the `MiuiNotificationStatusContainer` overlay and resolves its bounds from the live `MiuiBatteryMeterView`. Build-393 diagnostics record that candidate with `ancestorVisibilityIndependent=true` and `nativeGeometryWrites=0`. This proves a viable module-owned drawing lifetime on the real Home host, but not yet production acceptance across slot suppression and island transitions.

**Confirmed — island has separate occupancy and battery-presentation owners.**  
During charging-island entry, the diagnostic shows `MiuiStatusIconContainer` expanding to 583 px while `MiuiBatteryMeterView` independently translates and fades. The custom `combined_status` participant simultaneously retains its own 105 px occupancy/translation identity. One custom participant was therefore being asked to reconcile platform status-icon occupancy with a different battery presentation trajectory.

**Root-cause conclusion — high confidence.**  
The remaining Build-393 charging defect is architectural: the permanent custom participant couples responsibilities that the target SystemUI owns separately. Another fixed boundary, width difference, or translation correction would continue the same ownership error rather than fix it.

**Not established — exact ignored-slot contract.**  
The mature reference and AOSP/older MIUI implementations demonstrate an ignored-slot pattern, but the pinned target reference does not yet verify the concrete field/method, mutation boundary, restoration semantics, or interaction with the active HyperOS icon pipeline. No 0.0.2 runtime implementation may assume that contract yet.

### Design-level `ResolvedLayout` decision

The target contract is now specified in `docs/architecture/layout-policy.md` without changing runtime source.

It keeps independent:
- composite visual size and user scale;
- requested neighbor gap;
- requested occupancy;
- host-applied/native occupancy;
- per-glyph relative scales;
- optical adjustment;
- source visual bounds for future projection.

Scene adapters provide verified host/capability facts only. Native progress/timing/target-View motion remain outside the layout resolver.

### Review

- **Ownership review:** HyperOS remains owner of peer layout, status-container occupancy, battery scene state and live motion. Combined Status owns only its composition/drawing and, later, explicitly proven draw-only projection.
- **Lifecycle review:** the Home overlay candidate is host-scoped and removable through the existing render session. A future island projection must have its own shorter lifetime and must not become global state.
- **Single-writer review:** no new native width, layout, translation, alpha, visibility or scene-state writer is introduced by this checkpoint. The old custom-participant correction chain remains superseded.
- **Cleanup review:** no runtime resource is added here. The target architecture still requires exact restoration tokens for any future layout suppression, reversible visual masks, and separate transition cleanup.
- **Fail-native review:** native Wi-Fi/mobile/battery presentation must remain available until replacement readiness and exact-target suppression contracts are proven for the current session.
- **Performance review:** this checkpoint adds no hook, listener, pre-draw loop, polling, reflection hot path, wakeup or per-frame diagnostic.
- **Compatibility review:** conclusions that claim target behavior are scoped to the exact SystemUI SHA-256. Generalized/AOSP/older-MIUI ignored-slot evidence is explicitly not promoted to an exact-target contract.
- **Future-extension review:** size, gap, per-glyph scale and optical adjustment are centralized as shared layout intent so later Home, Keyguard/AOD and user controls do not require scene-specific offsets or new geometry writers.

### Validation / CI

Documentation and architecture-contract update only. No APK-affecting source changed, no Build ID advanced, and no Build 394 was created. Per repository rules, device validation is not required for this checkpoint.

### Outcome / next gate

The Home overlay is now a verified attachment/lifecycle **candidate**, not yet a fully selected production carrier.

Build 394 remains blocked on:
1. exact-target ignored-slot / native measure-layout proof;
2. reversible native-view masking proof;
3. island-time placement/carrier or draw-only handoff that preserves network information without peer overlap;
4. final ownership/lifecycle/single-writer/cleanup/fail-native/performance/compatibility review.

The next investigation must close those contracts rather than modify runtime behavior speculatively.

---

## 2026-09-27 — Exact-target ignored-slot and clip-mask proof

**Type:** exact-target static contract / architecture review  
**Runtime build:** none  
**Display line:** 0.0.2  
**Runtime impact:** none

### Problem / objective

The previous Phase-2A review still treated ignored-slot handling and a non-competing visual mask as unverified target contracts. The retained original target SystemUI APK was recovered from the file library and verified against the pinned SHA-256, allowing the missing contracts to be checked directly instead of inferred from AOSP/older MIUI behavior.

### Problem execution flow

1. Recover the retained original `SystemUI 17.03.260226.r` APK and verify the exact SHA-256.
2. Inspect `MiuiStatusIconContainer` fields/methods and the exact `onMeasure()` / `onLayout()` call path.
3. Verify the behavior of `setIgnoredSlots(...)` / `addIgnoredSlots(...)` rather than assuming AOSP `mIgnoredSlots` semantics.
4. Audit `View.setClipBounds(...)` writers across the exact target DEX set.
5. Re-run the ownership/single-writer/cleanup/fail-native review before granting either mechanism to a future runtime implementation.

### Exact-target findings

- `MiuiStatusIconContainer` defines its own `ignoredSlots: List` and public final `addIgnoredSlots(...)` / `setIgnoredSlots(...)` methods.
- `onMeasure()` excludes a child from the measured set when its slot is in `ignoredSlots`; `onLayout()` also checks the same list.
- the add path requests layout, so this is a native container layout contract rather than a peer-child width workaround.
- the target Home Wi-Fi/mobile/battery implementations were not found writing `clipBounds` in the directed DEX writer audit.

### Architecture consequence

The preferred steady Home composition can now be expressed as:

`MiuiNotificationStatusContainer.overlay carrier + host-scoped represented-slot exclusion + reversible clip-only native visual mask + shared ResolvedLayout`

This does not authorize a global ignored-slot replacement. Combined Status must snapshot/restore only the slot exclusions it owns for the current host session, preserve unrelated ignored entries, and fail native if the active target views/contracts are incomplete.

The clip candidate must save and restore each target View's pre-existing clip state. It must not replace native `alpha`, `visibility`, `translation`, or measured/layout geometry writers.

### Review

- **Ownership review:** `MiuiStatusIconContainer` remains the native layout owner; Combined Status may only use its exposed ignored-slot contract within the active Home session. Combined Status owns only its overlay drawing and its restoration tokens.
- **Lifecycle review:** ignored-slot additions and clip snapshots are HostSession-scoped and invalid on host replacement.
- **Single-writer review:** the new candidate avoids peer width/translation writes and avoids competing with native alpha/visibility animation writers.
- **Cleanup review:** restore only Combined Status-owned ignored entries and the exact saved clip state; cleanup must run on feature disable, host detach/replacement, hot reload, partial activation failure, and module/session reset.
- **Fail-native review:** native Views are not masked until the overlay renderer, target slot set, ignored-slot contract, and restoration tokens are all ready for the current session.
- **Performance review:** no polling, no production pre-draw follower, no per-frame reflection, and no additional background work is required for this steady-state mechanism.
- **Compatibility review:** this contract is proven only for the pinned target SHA-256. Other HyperOS builds must re-prove the class/method contract or remain native.
- **Future-extension review:** represented-slot handling stays independent from `ResolvedLayout`; future visual size/gap controls change layout intent, not suppression hooks.

### Remaining gate

Build 394 is still not created. The primary unresolved Phase-2A question is charging/island presentation: Combined Status must consume verified native motion/geometry while preserving network information instead of inheriting the battery view's fade/hide semantics. Notification-shade endpoint mapping also remains less mature than the Control Center anchor contract.

---

## 2026-09-27 — Phase-boundary and carrier-cutover review

**Type:** architecture consistency review  
**Runtime build:** none  
**Runtime impact:** none

### Review finding

The latest ROADMAP intentionally places Home -> shade / Control Center projection in Phase 2B after the Phase-2A Home carrier is stable. Treating full notification/control-center endpoint implementation as a Build-394 prerequisite would incorrectly expand the first 0.0.2 runtime boundary.

Code review also confirms that the active work branch still routes Hot Reload, feature handoff and native suppression through the superseded `SystemUiNativeCombinedParticipantOwner`, `SystemUiNativeNetworkSuppressionOwner`, and `SystemUiNativeBatterySuppressionOwner` chain.

### Decision

- Phase 2A may retain existing read-only panel-transition evidence, but full projection/endpoints remain Phase 2B.
- The first 0.0.2 runtime must establish an explicit carrier ownership cutover; the old participant/suppression carrier and the new Home overlay/ignored-slot/clip-mask carrier must not operate as concurrent writers.
- Migration should reuse the accepted domain state, renderer, tint/resource pipeline, diagnostics, settings and Hot Reload infrastructure while replacing only presentation-carrier ownership.
- Old participant code may remain temporarily for rollback/history during the checkpoint, but it must be inactive when the new carrier owns the session and should be retired after the new path is device-validated.

### Review dimensions

Ownership: one active carrier per Home session.  
Lifecycle: carrier selection belongs to HostSession and must be re-evaluated on host replacement/hot reload.  
Single writer: no overlapping native participant suppression and ignored-slot/clip-mask mutation.  
Cleanup: carrier deactivation must restore its own state before another carrier can activate.  
Fail native: if the new carrier cannot acquire all target contracts, do not fall through into a partially active mixture; restore native SystemUI.  
Performance: reuse existing event-driven domain state; do not duplicate state observers for the new carrier.  
Compatibility: the new target-specific slot/mask contract stays fingerprint-gated.  
Future extension: Phase 2B consumes the stable Phase-2A source bounds rather than reopening Home carrier ownership.

---

## 2026-09-27 — Home island carrier contract closed; Build 394 gate opened

**Type:** exact-target architecture closure / gate review  
**Runtime build:** none  
**Runtime impact:** none

### Problem execution flow

1. Inspect exact `StatusBarIslandControllerImpl` method bodies rather than relying on member existence.
2. Trace `translationFlow`, `refreshTranslation()`, `HomeStatusBarViewBinderInjector`, and `IslandStretchAnimation` ownership.
3. Resolve `IslandStretchAnimation.rightContainer` through the exact target resource table.
4. Cross-check the resolved host against Build-393 runtime topology and island-frame geometry.
5. Re-run ownership, lifecycle, single-writer, cleanup, fail-native, performance, compatibility and future-extension review.

### Exact-target closure

- `translationFlow` carries the target translation distance refreshed by SystemUI; it is not a per-frame progress source.
- `IslandStretchAnimation` applies SystemUI-owned Folme island show/hide animation to its `rightContainer.translationX`.
- `rightContainer` resolves to the exact target resource `system_icon_area`.
- Build-393 topology identifies `system_icon_area` as the `MiuiNotificationStatusContainer` used by the Home overlay session.
- Runtime island samples show inner status/battery containers keeping zero local translation while their screen coordinates move, confirming ancestor-owned motion.

### Architecture consequence

The Home overlay inherits native charging/Super-Island translation directly from its animated host. No production pre-draw follower, battery-translation copier, custom duration/interpolator, or participant width handoff is required.

The battery view's alpha/hide semantics remain separate and are not inherited by Combined Status, which must continue to preserve network information.

### Final gate review

- **Ownership:** SystemUI owns `system_icon_area` motion and peer layout; Combined Status owns only overlay composition plus its scoped slot/mask restoration state.
- **Lifecycle:** overlay, ignored-slot restoration and clip snapshots are bound to one Home HostSession.
- **Single writer:** no native translation/alpha/visibility writer is added for island motion; old participant/suppression ownership must be inactive during the new carrier session.
- **Cleanup:** deactivate in reverse ownership order and restore exact clip/slot state on feature disable, host replacement, hot reload and partial activation failure.
- **Fail native:** do not mask any native representation until the new Home session has acquired every required target contract and renderer state.
- **Performance:** steady state remains event-driven; island movement is inherited through parent transformation with no module per-frame work.
- **Compatibility:** all new contracts are scoped to the pinned SystemUI SHA-256; unmatched profiles remain native.
- **Future extension:** Phase 2B can consume the stable Home source bounds without reopening carrier ownership.

### Decision

The Build-394 architecture gate is open. The first 0.0.2 runtime checkpoint may implement only the Home carrier cutover, shared ResolvedLayout, target ignored-slot exclusion, reversible clip masking and inherited native island motion. Home -> shade/Control Center projection, Keyguard/AOD and user-facing sizing controls remain out of scope.

---

## 2026-09-27 — Exact Home island carrier contract closed

**Type:** exact-target architecture proof  
**Runtime build:** none  
**Runtime impact:** none

### Problem execution flow

1. Use the maintainer-provided JADX 1.5.6 against the retained original SystemUI APK.
2. Re-verify the target APK SHA-256 before decompilation.
3. Decompile `StatusBarIslandControllerImpl`, `HomeStatusBarViewBinderInjector/HomeStatusBarViewBinderImpl`, `IslandStretchAnimation`, `IslandMonitor`, and decode `res/layout/status_bar.xml`.
4. Trace the actual island endpoint, native animation writer, right-side target View and occupancy flow.
5. Compare the result with the existing Combined Status Home overlay host and the Build-393 split-ownership evidence.

### Findings

- `translationFlow` is a configuration-derived endpoint containing `status_bar_island_translation`; it is refreshed for density/font-scale, layout direction and max-bounds changes and is not a per-frame animation stream.
- `IslandStretchAnimation` consumes that endpoint and uses the native `MiuiStatusBarIconAnimatorController` ISLAND_SHOW/ISLAND_HIDE Folme configuration to animate the phone Home `rightContainer.translationX`.
- `HomeStatusBarViewBinderImpl` passes `R.id.system_icon_area` as `rightContainer`.
- exact `status_bar.xml` declares `system_icon_area` as `MiuiNotificationStatusContainer` — the same host already used by `CombinedStatusHomeRenderSession.overlay`.
- `statusContainerSpace` is written by `IslandMonitor.RealContainerIslandMonitor.updateContainerSize()` from real island/container geometry and is consumed as layout occupancy by fake/mirrored status-icon containers; it is not animation progress.

### Architecture consequence

The preferred Home overlay is also the native island moving host. Keeping the Combined Status View in that overlay means native parent translation moves the replacement automatically, while the battery child's own fade/hide remains independent. No production pre-draw follower, duplicate animator, endpoint compensation, or Combined Status translation writer is required.

### Review

- **Ownership:** SystemUI exclusively owns island translation and animation configuration; Combined Status owns only overlay drawing.
- **Lifecycle:** the carrier is already bound to the Home `MiuiNotificationStatusContainer` host lifetime.
- **Single writer:** no native translation/alpha/visibility writer is added.
- **Cleanup:** removing the overlay/session is sufficient for motion cleanup; no animation observer must outlive the host.
- **Fail native:** if the exact host/slot/mask contracts cannot be acquired, leave native views visible and do not activate the replacement.
- **Performance:** zero production frame polling/following is required.
- **Compatibility:** the conclusion is scoped to the pinned SHA-256; other builds must re-prove the host/motion contract.
- **Future extension:** Phase 2B can project from stable Home bounds without reopening island motion ownership.

### Gate consequence

The previous island-carrier architecture blocker is closed. The remaining pre-Build-394 work is implementation-boundary review: host-scoped ignored-slot restoration, clip-mask restoration, ResolvedLayout source shape, and explicit cutover from the superseded participant/suppression path.


---

## 2026-09-27 — Repository consistency review after Build-394 architecture gate

**Type:** documentation / architecture-state consistency review  
**Runtime build:** none  
**Display line:** 0.0.2  
**Runtime impact:** none

### Problem / objective

The exact-target Phase-2A investigation advanced faster than several current-facing repository surfaces. `CURRENT.md` had already opened the Build-394 gate, while the ROADMAP active route, architecture/reference status text, PR #100, CHANGELOG implementation wording, and bug-report example still described older checkpoints or superseded carrier mechanics.

The objective was to restore one current repository narrative without rewriting historical Build/DEVLOG evidence.

### Problem execution flow

1. Re-read the latest `CONTRIBUTING.md`, `CURRENT.md`, `ROADMAP.md`, and relevant recent `DEVLOG.md`.
2. Cross-check architecture/reference documents, README/CHANGELOG, issue/PR surfaces, version metadata, and CI/release configuration.
3. Separate genuine historical records from current/future/general descriptions.
4. Mark the Build-394 pre-runtime architecture gate as open everywhere that describes current state.
5. Remove or neutralize superseded participant/occupancy-handoff details from the current `[Unreleased]` net-state changelog.
6. Update PR #100 so its current purpose/acceptance boundary matches Phase 2A rather than the original Build-378 slot-geometry checkpoint.
7. Add an explicit contributor rule mapping meaningful checkpoint outcomes to the documents that must be synchronized.
8. Run a final consistency review without creating an APK/runtime checkpoint.

### Documents / references reviewed

- latest `CONTRIBUTING.md`;
- `docs/development/CURRENT.md`, `ROADMAP.md`, recent `DEVLOG.md`, and `VERSIONING.md`;
- `docs/architecture/README.md`, `layout-policy.md`, and `scene-policy.md`;
- `docs/reference/README.md` and `statusbar-composition-patterns.md`;
- public `README.md`, `CHANGELOG.md`, bug-report template, PR #100;
- `gradle.properties`, app build configuration, target profile, and release/build workflows.

### Corrections

- `CURRENT.md` now describes Build 394 as scope-defined, gate-open, and not yet built.
- ROADMAP active work now begins at the Build-394 Home carrier runtime cutover instead of repeating already-closed static prerequisites.
- architecture/reference status now distinguishes selected pre-runtime direction from runtime acceptance.
- the shared `ResolvedLayout` source implementation is explicitly authorized only inside the bounded Build-394 checkpoint.
- PR #100 is retitled/reframed around the 0.0.2 Home carrier architecture and its current validation gate.
- `CHANGELOG.md` no longer presents the superseded permanent participant / battery-occupancy handoff and related participant-specific suppression/Hot-Reload details as the intended current net state.
- the bug-report version example now uses the current 0.0.2 development line.
- `CONTRIBUTING.md` now requires documentation synchronization after each meaningful engineering checkpoint and defines which document changes for which kind of state change.

### Review

- **History review:** existing historical DEVLOG/Build entries were not rewritten. New conclusions are appended only.
- **Architecture review:** current-facing documents consistently treat the permanent extra participant / occupancy-handoff route as superseded by default and Build 394 as the first runtime proof of the selected Home carrier direction.
- **Ownership review:** no documentation correction grants new runtime write ownership; Build 394 remains responsible for runtime proof.
- **Lifecycle/cleanup review:** current acceptance wording keeps HostSession-scoped restoration and fail-native cleanup explicit.
- **Version review:** current development line remains 0.0.2; historical 0.0.1 Build identities remain unchanged; first formal release target remains 1.0.0.
- **CI/runtime review:** documentation/governance only; no Build ID, APK, runtime code, or Canary checkpoint is created.

### Outcome

Repository current-state surfaces are aligned for the Build-394 implementation stage. Future meaningful checkpoints must synchronize repository memory before the checkpoint is treated as complete, using the new `CONTRIBUTING.md` mapping.

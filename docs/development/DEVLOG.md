# Combined Status Development Log

This is the chronological engineering diary for Combined Status. It complements, but does not replace, `CHANGELOG.md`, pull-request history, diagnostics, or CI artifacts.

## 2026-09-28 — CI validation-surface routing and main/dev synchronization

**Type:** repository automation / CI governance
**PR:** #149 `ci: explain validation surface routing`
**main squash:** `497be75c1754e49cb7a49b6abd73dcbd3bc010b3`
**dev history-preserving sync:** `2e9b1716849d6709342a446f63e5b886c0aed9ae`

### Problem / goal

Long-lived work PRs can remain on Full after an earlier build/CI/tooling change even when a later checkpoint is only an ordinary runtime bug fix. That behavior is safe because classification uses the current base-to-head diff, but the reason was not visible enough and could look like "feat always runs Full".

The objective was to improve operator clarity and branch hygiene without introducing a mutable "last Full" trust state, latest-commit-only bypass, or automatic device-test decision.

### Implementation

- Build classification still uses the current base-to-head diff as the safety authority.
- The workflow now reports detected runtime, build, CI, tooling and docs surfaces plus the routing reason.
- A mixed-surface warning appears when runtime work still carries a Full-triggering build/CI/tooling surface.
- The warning is advisory only: it cannot downgrade Full. Splitting is recommended only when the high-risk surface is independently mergeable/reversible.
- Routine `combinedStatus.versionCode` / `combinedStatus.buildId` handling and surface detection share one Gradle-property helper so the two classifiers cannot silently drift.
- Signed work-branch Canary remains explicit/demand-driven; path detection does not decide whether device evidence is required.
- `CONTRIBUTING.md` and `docs/development/README.md` now document the same routing contract.

### Review / 审查

- **safety authority:** no "latest commit only" rule and no mutable successful-Full checkpoint were introduced.
- **scope monotonicity:** mixed-surface detection can only explain an existing Full requirement; it cannot lower validation scope.
- **trust/secrets:** pull-request signing behavior is unchanged; project signing secrets remain unavailable to PR jobs.
- **runtime/APK:** no app/SystemUI source, dependency resolution, signing identity, target profile contract, or release publication permission changed.
- **maintainability:** review caught an over-escaped `gradle.properties` regex before merge; the duplicate logic was replaced by a shared helper.
- **device validation:** not required because this is automation-only and does not change installed behavior.

### Validation

- PR Full Build #1311 / run `36360225987`: **success**. Summary reported `full`, reason `.github/workflows/build.yml`, surfaces `runtime=false, build=false, CI=true, tooling=false, docs=true`, mixed-surface review not required.
- Post-merge `main` Full Build #1313 / run `36360461776`: **success**, including signing restore, Debug + Canary build, metadata/signature checks, non-debuggable verification and artifact publication.
- History-preserving `main -> dev` sync Full Build #1317 / run `36360713070`: **success** with the same CI/docs surface classification. The produced artifacts retained Build 418 identity; Canary artifact id `10944899896`.
- The automation sync does not create Build 419 or change the accepted Build-418 runtime tree.

### Result

The repository now explains why a checkpoint is Light/Fast/Integration/Full and flags mixed validation surfaces early. Ordinary runtime PRs remain Fast when their base-to-head diff contains only ordinary runtime/compatibility plus routine build identity changes; a PR remains Full while it still owns build/CI/tooling risk.


## 2026-09-28 — Build 422: remove duplicate Home scene writer

**Type:** Phase-2B runtime ownership correction
**Build:** 422 / `20260928-422`
**Work branch / PR:** `feat/panel-projection` / Draft #146
**Device validation:** pending

### Build-421 device result

Build 421 is device-rejected for the remaining Notification-Shade first/last-frame gap. The supplied Detailed diagnostic shows the decisive ordering:

- native notification state first reaches `fraction=0.0`;
- `MiuiBatteryMeterView.updateState()` then emits raw status-bar state `1`;
- the Build-421 platform discriminator reports `KeyguardManager.isKeyguardLocked=true`, producing `surface=KEYGUARD` and hiding the Home overlay;
- only afterward does `ShadeExpansionStateManager` publish the positive shade fraction and take legitimate panel ownership.

The supplied video matches that ordering visually: Combined Status leaves before the Notification Shade has taken over on open, and returns after native Home status icons on close. Build-420 Control Center projection/handoff remains normal in the same device evidence.

**Rejected hypothesis:** platform `KeyguardManager.isKeyguardLocked` can disambiguate transient Battery raw state 1 from real Keyguard ownership. On this target it cannot.

### 问题执行流程

**现象与证据 -> 根因 / 责任源:** the edge gap is not an animation-speed or fraction-threshold defect. Home visibility has two asynchronous writers: the native Home host/panel coordinator and a second project-side Battery-status scene gate.

**仓库规范与官方规范:** current architecture requires Host -> HostSession ownership, one live property / one writer, native motion/lifecycle reuse and root-cause-first correction. Android `ViewGroupOverlay` is a visual layer of its host ViewGroup; Combined Status is already attached to the verified Home host rather than a global window.

**HyperOS / SystemUI 原生实现:** the pinned target has a distinct Home `MiuiNotificationStatusContainer / system_icon_area`, distinct `MiuiKeyguardStatusBarView`, verified Notification-Shade `ShadeExpansionStateManager`, and accepted Control Center carrier/coordinator. Battery `mStatusBarState` is a presentation input and is not sufficient proof of global surface ownership.

**成熟实现对比:** the existing-host reference pattern scopes presentation lifetime to a HostSession and consumes platform scene/hide inputs without inventing a parallel global scene machine. No mature evidence justifies a second Battery-derived Home visibility writer.

**方案选择:** remove Battery status state from Home visibility. Keep its existing hook only as read-only presentation/tint event context. Notification Shade remains the sole native fraction handoff for that panel; Control Center keeps the accepted Build-420 coordinator. Keyguard/AOD stay separate native surfaces for their later adapters.

**workaround:** none. No delay, epsilon, retry, polling, pre-draw follower, custom animation, Keyguard boolean substitution, geometry compensation or extra Hook is added.

### Implementation

- `SystemUiSceneStateSource` no longer reads `KeyguardManager`, creates `TRANSIENT_PANEL`, or publishes a Home-visibility decision. Raw Battery status states are retained only as read-only classifications/diagnostics.
- `CombinedStatusHomeRenderSession` no longer stores or seeds a Battery-derived `sceneSurface` and no longer includes it in overlay visibility/readiness diagnostics.
- `CombinedStatusModule.onSceneStateUpdate()` retains the existing Battery-triggered tint refresh / unlocked observation trigger but no longer forwards that event as a Home visibility writer.
- Home overlay eligibility is now the composition of feature enablement, verified Notification-Shade ownership, Control Center coordinator ownership, and existing native handoff state. Its actual drawing remains scoped to the Home host overlay.
- Build identity advances to 422 because executable runtime behavior changes.
- Unit coverage is updated so Battery states remain read-only classifications and Home visibility is governed by feature/panel/handoff gates only.

### 审查 / review

- **ownership:** Home surface drawing belongs to the native Home host; Notification Shade and Control Center retain their own verified authorities. Battery status state no longer owns Home visibility.
- **lifecycle:** no new observer/listener is added; existing HostSession attach/detach and panel callbacks remain.
- **single writer:** removes the conflicting scene writer instead of adding a third discriminator.
- **cleanup:** no new mutable presentation token exists; current overlay/mask/reservation/Control Center cleanup remains unchanged.
- **fail-native:** structural Home readiness and target-profile failure behavior remain unchanged; unsupported Keyguard/AOD surfaces stay native because they use separate SystemUI hosts.
- **performance:** removes the per-scene-event platform Keyguard service read; adds no polling or frame work.
- **compatibility:** no new private SystemUI class/member/Hook is required and the pinned Hook count is unchanged.
- **exception recovery:** existing source-install, host detach, Hot Reload and fail-native paths remain intact.
- **future extension:** Keyguard/AOD can add explicit host adapters without reusing or duplicating a Home Battery-state gate.

### Validation gate

Keep PR #146 Draft for Light repository validation and source review. This is a meaningful runtime checkpoint; after review, move Ready for the prescribed runtime validation. A signed Canary is required because the correction changes scene ownership and must be tested for Notification-Shade continuity plus lock/unlock regression before integration.

### Automated validation follow-up

- Draft Light #1297 passed after a record-format-only trailing-whitespace correction; Build identity remained 422.
- Ready validation #1298 was automatically classified **Full** because the PR-wide diff still contains an earlier `tools/verify_target_profile.py` change. This stronger scope is retained rather than overridden.
- Full #1298 passed wrapper/JDK/API-37/pinned-target verification and reached Kotlin test compilation, then failed because `SystemUiNativeCombinedParticipantOwnerTest` still supplied the removed `sceneAllowsOverlay` test parameter at six historical call sites.
- The failure is test-call-site drift, not a runtime/profile failure. The correction removes those stale arguments, renames the affected tests to panel/handoff semantics, and converts the former scene-false assertion into Notification-Shade ownership denial.
- No executable production source, Hook contract, build identity, geometry, tint, Control Center behavior, or ownership decision changes in this correction.

### Build-422 signed checkpoint

- Draft Light #1300 passed after the stale unit-test call sites were corrected; runtime production source remained unchanged.
- Ready validation #1301 / run `36357104464` then passed under **Full** scope on exact PR head `f5cfbc87c819a776a5476f3ea1e5817b9c776d86`, including Gradle wrapper, JDK/API 37, pinned HyperOS target profile, Kotlin unit tests, Debug build, and Modern Xposed metadata checks.
- Owner `/canary` triggered Work Branch Canary #412 / run `36357295818`.
- Canary trusted-source resolution, exact checkout and source verification all resolved `f5cfbc87c819a776a5476f3ea1e5817b9c776d86`.
- Haple signing restore and APK signature verification passed; signer certificate SHA-256: `7a64fc85325afe79439afb63369d832d7ddc20fd5b3935b4c28d7bcb85e92fc7`.
- Modern Xposed metadata and non-debuggable checks passed.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260928-422-canary.apk`; artifact id `10944007922`; uploaded ZIP digest `sha256:78df112998aa2d38b4b4b24e93b78e2c7d90480d44ba89912d825f593418dd01`.
- Extracted APK size: `3309602` bytes; APK SHA-256: `6d1bcab45ccf01ba3d0110eae2e7b9be5e00a3dc04ae994e308645d144cf5e7e`.
- PR returns to Draft and runtime is frozen pending focused device validation of Notification-Shade edge continuity, quick Control Center regression, and one lock/unlock Home-overlay leak smoke test.

---

## 2026-09-28 — Build 419: narrow NotificationShadeWrapper target probe

**Type:** Phase-2B bounded runtime diagnostics
**Build:** 419 / `20260928-419`
**Work branch / PR:** `feat/panel-projection` / Draft #146
**Device validation:** pending

### Problem execution flow

**Phenomenon / evidence:** Build 414 proved that `NotificationHeaderExpandController` is on the native notification-header expansion path, but its direct Android View inventory exposed only `realClockIcons`; the controller translations remained constant and are not a verified status-icon endpoint. The same exact runtime inventory exposed two narrower ownership seams: `notification: NotificationShadeWrapper` and `headerController: Lazy`.

**Root cause / missing fact:** notification-shade lifetime/progress is already verified, but the actual shade status-icon target host/bounds/tint owner remains unknown. Implementing projection without that fact would invent geometry.

**Repository / exact-target evidence:** current SystemUI-Reference has high-level `NotificationHeaderExpandController` motion contracts and Control Center `StatusBarAnchorBounds`, but its selective source cache does not contain `NotificationShadeWrapper`; the original 52 MB APK is intentionally not stored in the repository. Public source search did not produce a matching source snapshot. Build-414 runtime field inventory is therefore the narrowest exact-target evidence available.

**Mature/native comparison:** Control Center already exposes an explicit anchor-bounds object and distinct fake/status-icon surface. Notification shade should likewise be resolved through its native wrapper/header ownership chain rather than by copying Control Center geometry or inventing an offset.

### Selected diagnostic

Add `SystemUiNotificationShadeTargetProbe`:
- hook the already-known notification-header expansion callback;
- call native first;
- no work unless Detailed diagnostics / development probes are enabled;
- capture only boundary buckets 0/1/7/8;
- resolve the callback's `NotificationHeaderExpandController`;
- read exact controller fields `notification` and `headerController`;
- resolve the Lazy holder without storing cross-generation objects;
- summarize only each owner’s direct View fields plus a bounded one-level set of semantically named candidate owners;
- log a capped field/type inventory once;
- no root traversal, polling, frame listener, layout write, tint write, visibility write, or projection rendering.

### 审查 / review

- **ownership:** SystemUI remains sole shade layout/motion/tint owner; project reads only.
- **lifecycle:** one diagnostics-build hook, generation-scoped; reset on Hot Reload.
- **single writer:** zero new presentation writers.
- **cleanup:** only primitive bucket/inventory flags are retained; reset during teardown.
- **fail-native:** install/read failure leaves the accepted Build-418 Home + panel lifetime behavior untouched.
- **performance:** one callback hook; bounded reflection only at four diagnostic buckets and only while Detailed is enabled.
- **compatibility:** callback/controller/fields are pinned in the exact-target profile and checked by the profile verifier.
- **exception recovery:** reflective owner/value/view reads are guarded and report unavailable/null instead of changing behavior.
- **future extension:** probe should be deleted from active runtime after the real shade target contract is identified.

### Draft validation / source review

- Draft Light #1270 passed on the initial Build-419 implementation.
- Review found the semantic candidate filter was matching the full package name, which could classify unrelated `ConfigurationController` values through the `statusbar` package segment.
- The filter was tightened to field name + type `simpleName`, preserving intended status/icon/header/battery/system/shade/clock/container candidates while avoiding package-name false positives.
- Draft Light #1271 passed on exact runtime head `1532cb33f9ebefc71ad361db226f0434e0de21b1`.
- Ready Full #1274 correctly exercised the full unit/build gate and failed one new probe unit test at `candidateSelectionIsNarrowAndSemantic`. Root cause: the runtime caller had already switched to `Field.type.simpleName`, but the reusable helper still accepted arbitrary full type strings; its test intentionally supplied a fully qualified `ConfigurationController`, whose package path contains `statusbar`, exposing the inconsistent helper contract.
- Fix: normalize `typeName` inside `isCandidateField()` with `substringAfterLast('.')` before semantic token matching. This preserves intended Header/Status/Icon candidates and makes package paths unable to widen the diagnostic scope. No Hook, lifecycle, target-profile, geometry, presentation, or writer behavior changes.
- Build identity remains 419 because this is a correction within the same unaccepted diagnostic checkpoint.
- Hot Reload takeover review confirms the status-host handle is the only preserved old handle; every other old-generation HookHandle is unhooked. The new probe therefore cannot accumulate across Hot Reload generations.
- No additional runtime writer/listener/poller was introduced.

### First device evidence — Build 419

The first signed Build-419 diagnostic run is valid Canary evidence:

- report identity is Build 419 Canary with Detailed diagnostics;
- runtime health is healthy;
- `notificationShadeTargetProbe` installs `1/1` hooks in bounded read-only mode with `nativeGeometryWrites=0`;
- notification-shade buckets 0/1/7/8 are captured across the requested pull-down / return cycle;
- controller field `notification` resolves to `com.miui.systemui.shade.NotificationShadeWrapper`;
- the wrapper exposes real native shade ownership objects, including `NotificationHeaderClipHelper` with `SharedNotificationContainer`, and `StatusBarStateControllerImpl` with `NotificationPanelView`;
- the shared notification/panel Views move from alpha/visibility inactive at the settled top state to active while the notification shade is expanded;
- `headerController` does **not** resolve to the expected header owner and remains `dagger.internal.DoubleCheck`.

### Root-cause correction of the diagnostic

Review of the probe implementation shows `resolveLazyValue()` only searched for zero-arg `getValue()`. The device object is Dagger `DoubleCheck`, whose Lazy contract uses zero-arg `get()`. Therefore the missing header target is a **probe accessor defect**, not evidence that the header controller or status-icon host is absent.

Selected correction within the same unaccepted Build 419:
- recognize only Dagger Lazy/DoubleCheck holders before calling `get()`;
- retain Kotlin Lazy `getValue()` support;
- do not invoke generic `get()` on unrelated objects;
- add deterministic unit coverage for Dagger, Kotlin, and unrelated-get cases;
- leave Hook count, boundary buckets, reflection depth, native writers, profile contract, and projection state unchanged.

**Review / 审查:** ownership remains read-only SystemUI discovery; lifecycle and Hook ownership are unchanged; single-writer boundary remains zero new presentation writers; fail-native returns the original holder if accessor resolution fails; performance remains four bounded boundary reads only; compatibility does not add another SystemUI Hook contract.

### Validation gate

Executable diagnostics plus target-profile/verifier change advance the next runtime identity to Build 419. Ready Full -> one signed Canary only because one focused device diagnostic is now required.


## 2026-09-28 — PR #146 refreshed from accepted Build 418 dev baseline

**Type:** history-preserving feature-branch recovery / documentation checkpoint
**Runtime Build:** unchanged — 418 / `20260928-418`
**Work branch / PR:** `feat/panel-projection` / Draft #146
**dev source merged:** `2a0ddbac7daeeb4129042c1702773df0f47583a0`

### Recovery decision

PR #146 had diverged from `dev` while the independent Hot Reload/Tint defect was resolved through Builds 415-418. Build 418 is now device-accepted and Integration-validated, so the Phase-2B branch can resume.

The branch refresh is deliberately history-preserving:
- retain the old #146 commit history as the first-parent feature history;
- merge the accepted current `dev` history as the second parent;
- use the latest `dev` runtime tree as the resolved runtime baseline;
- preserve Build-414 panel-probe findings in DEVLOG/reference only;
- do **not** retain the completed `SystemUiNotificationHeaderProbe`, its diagnostic-only target-profile expansion, or its verifier/test scaffolding in the active runtime;
- do not increment Build identity because this synchronization leaves executable runtime equal to accepted Build 418.

### Build-414 evidence retained

The bounded Build-414 notification-header probe completed its diagnostic purpose:
- native expansion boundary buckets 0/1/7/8 were observed;
- the only directly discovered Android `View` on `NotificationHeaderExpandController` was `realClockIcons`;
- controller `notificationTranslationX=2` and `notificationTranslationY=-109` remained stable in the captured boundaries;
- the field inventory identified `headerController: Lazy` and `notification: NotificationShadeWrapper` as narrower ownership seams for follow-up;
- the probe was read-only and is not accepted as a production geometry source.

The contemporaneous Hot Reload tint defect exposed during Build 414 is superseded by the accepted Build-418 lifecycle correction and must not be reintroduced while continuing projection work.

### 审查 / review

- **ownership:** latest `dev` Home/Tint/scene owners remain authoritative; no old diagnostic owner is restored.
- **lifecycle:** synchronization adds no runtime lifecycle.
- **single writer:** unchanged from Build 418.
- **cleanup:** obsolete diagnostic hook state is absent from the resolved runtime tree.
- **fail-native:** notification shade and Control Center remain native-only until a verified projection target contract exists.
- **performance:** no runtime change.
- **compatibility:** no extra Build-414 diagnostic contract is imposed on current runtime compatibility.
- **exception recovery:** unchanged from Build 418.
- **future extension:** continue static/reference discovery through `NotificationShadeWrapper` / `headerController`; add another diagnostic only if a specific fact remains unavailable.

### Next

Continue Phase 2B source/contract review before runtime mutation. The next application Build is created only when executable projection/diagnostic source genuinely changes.


## 2026-09-28 — Build 414 device evidence: panel probe succeeds, Hot Reload tint continuity fails

**Type:** maintainer device evidence / root-cause triage
**Display version:** 0.0.2
**Build:** 414 / `20260928-414`
**Work branch / PR:** `feat/panel-projection` / Draft #146
**Device:** Xiaomi 15 Pro / HyperOS 4.0.0.15.XOBCNXM.D01 / SystemUI 17.03.260226.r

### Maintainer feedback

After installing Build 414 and using module Hot Reload, Combined Status inversion/tint is visibly wrong. A full SystemUI restart restores correct behavior. The supplied diagnostic intentionally captures the pre-restart abnormal Hot Reload session; it does not contain the later normal cold/restarted session.

### Diagnostic facts

- Runtime health is otherwise healthy and all expected Build-414 sources install successfully.
- Immediately after Hot Reload, structured health still reports `tint state=unknown event=not-observed` while the Home renderer/session is already restored.
- Status-icon observation and Home renderer then seed `#bf000000`; Home reaches `ownerReady=true` and becomes visible from that seed.
- Only after the Hot Reload restore is complete does `MiuiBatteryMeterView.onDarkChangedInternal` arrive and expose the native tint callback context.
- The Build-414 `notificationHeaderProbe` is explicitly `bounded-read-only` with `nativeGeometryWrites=0`; no evidence shows it writing tint, alpha, visibility, or layout.
- Notification-header target evidence was successfully captured at buckets 0/1/7/8. Direct controller View discovery yielded only `realClockIcons`; controller translations stayed `notificationTranslationX=2`, `notificationTranslationY=-109`. The one-time field inventory exposes `headerController: Lazy` and `notification: NotificationShadeWrapper` as narrower follow-up ownership seams.

### Root-cause review

Source review shows Hot Reload currently resets `SystemUiPresentationRuntimeOwner`, which clears `SystemUiTintStateSource` state, and also resets `CombinedStatusPresentationStateStore`. The classloader-neutral Hot Reload transfer carries model/network state plus shade/Control Center eligibility, but **does not carry stable tint continuity**.

During reattach, `CombinedStatusHomeRenderSession.start()` immediately calls `SystemUiTintStateSource.currentState(battery)`, which refreshes directly from the live battery-percent TextView and can therefore accept a transient handoff color before the new generation receives its first authoritative dark/tint event. This matches the device symptom and the supplied event ordering. A full SystemUI restart rebuilds the native dark/tint lifecycle from cold state, matching the maintainer's report that restart restores correct behavior.

The evidence does **not** support blaming the notification-header probe itself. Its additional hook may change timing enough to expose the pre-existing handoff weakness, but it is not a tint writer.

### Selected direction

Do not add a timer or force SystemUI to resend dark mode. Preserve presentation continuity explicitly:

1. capture the last accepted stable tint before old-generation teardown using classloader-neutral primitive values;
2. transfer it with the existing Hot Reload payload;
3. seed the new Home renderer from that transferred stable tint instead of immediately trusting a possibly transient live View read during handoff;
4. let the first new-generation native tint event replace the transferred seed naturally;
5. keep cold-start behavior unchanged.

Because this is a different runtime owner and independently shippable correction, implement it on a dedicated `fix/*` branch from the accepted `dev` baseline, then update #146 after integration.

### 审查 / review

- **Ownership:** native SystemUI remains tint authority; transfer only preserves the last already-accepted native-derived presentation state across module generations.
- **Lifecycle:** continuity exists only across the bounded Hot Reload handoff and is superseded by the first new native tint event.
- **Single writer:** no new color writer or dark-mode controller is introduced.
- **Cleanup:** transferred primitives are generation-bounded; no View/classloader object needs to cross as tint state.
- **Fail native:** missing/invalid transferred tint falls back to the existing native re-observation path.
- **Performance:** no polling, delay, retry, or extra frame work.
- **Compatibility:** uses existing native tint events and classloader-neutral primitive transfer.
- **Exception recovery:** malformed/older transfer versions continue through backward-compatible restore or native fallback.
- **Future extension:** panel projection remains paused until this shared Hot Reload presentation continuity is stable.

### Panel-projection outcome

Build 414 achieved its diagnostic objective but is not accepted as a mergeable projection checkpoint because of the Hot Reload tint regression. Keep PR #146 Draft. After the separate Hot Reload fix integrates, refresh the branch and continue target-host discovery through the verified notification header controller/wrapper chain.

## 2026-09-28 — Build 414 notification-header target-geometry probe

**Type:** Phase-2B bounded runtime diagnostics
**Display version:** 0.0.2
**Build:** 414 / `20260928-414`
**Work branch / PR:** `feat/panel-projection` / Draft #146
**Device validation:** pending

### Problem / objective

Static exact-target evidence identifies `NotificationHeaderExpandController` as the native notification-header translation owner and its `notificationCallback$1.onExpansionChanged(float)` callback as the expansion signal. The repository still lacks a verified runtime target-host/bounds snapshot comparable to Control Center's existing `StatusBarAnchorBounds` evidence.

Implementing the panel renderer before that fact is known would require guessing a host or inventing a geometry formula.

### Root cause / evidence gap

**Confirmed evidence gap:** notification-shade lifetime/progress is verified, but the actual controller-owned View candidates and their native target geometry are not yet runtime-verified in Combined Status.

This is not evidence that the Home overlay or legacy native participant should be reused. It is a request for one missing native target fact.

### Exact-target contracts used

- `com.android.systemui.controlcenter.shade.NotificationHeaderExpandController`;
- `NotificationHeaderExpandController$notificationCallback$1.onExpansionChanged(F)V`;
- controller scalar fields `notificationTranslationX` and `notificationTranslationY`;
- existing Build-413 `ShadeExpansionStateManager.onPanelExpansionChanged(FZZ)V` remains the runtime Home-lifetime authority;
- existing Control Center `StatusBarAnchorBounds` diagnostics remain unchanged.

The new diagnostic callback contract is recorded in `compat/targets/hyperos-17.03.260226.r.json` and bound to source constants by `tools/verify_target_profile.py`.

### Measures implemented

Added `SystemUiNotificationHeaderProbe`:

- installed only in runtime-diagnostics builds;
- hooks the exact notification-header expansion callback;
- calls the native callback first;
- performs no work unless Detailed diagnostics / development probes are enabled;
- samples only when entering native diagnostic boundary buckets 0, 1, 7, or 8;
- resolves the callback's owning `NotificationHeaderExpandController`;
- reads only the two verified translation scalar fields plus direct controller fields whose declared type is an Android `View`;
- records field name, runtime View type, resource id, parent type, screen coordinates, laid-out/measured bounds, translation, alpha, visibility, and attachment state;
- emits one capped controller field/type inventory to make absence of direct View fields diagnosable without repeated reflection;
- writes no View/layout/translation state.

Probe installation is isolated from `SystemUiPanelTransitionSource`. If the diagnostic contract cannot be installed, Build-413 notification-shade / Control Center runtime authority remains intact and the probe is reported unavailable.

### 审查 / review

- **Ownership:** SystemUI remains the sole notification-header motion/layout owner; the module only observes controller-owned state.
- **Lifecycle:** one diagnostics-build Hook tied to the existing module generation; Hot Reload resets probe bookkeeping and old-generation hooks remain under the existing takeover lifecycle.
- **Single writer:** zero new writers. No Home or panel visibility, layout, translation, alpha, tint, or peer state is changed.
- **Cleanup:** bucket/inventory state is generation-scoped and reset during Hot Reload teardown.
- **Fail native:** diagnostic install failure is fail-soft and explicitly does not affect runtime panel authority; actual panel projection remains native-only.
- **Performance:** no polling, ViewTree traversal, frame listener, or continuous logging. Reflection metadata is resolved once at install and runtime snapshots occur only at four bounded progress buckets while Detailed diagnostics are enabled.
- **Compatibility:** the callback/controller and translation fields are declared in the exact pinned target profile; source constants are CI-checked against that profile.
- **Exception recovery:** all probe reads are guarded; missing optional direct View values report null/unavailable rather than changing behavior.
- **Future extension:** runtime evidence from this probe will determine a surface-specific projection adapter/host. The probe is not itself intended to become the production geometry source.

### Rejected alternatives

- Interpolate from Home to a guessed notification translation: rejected; target host/bounds unverified.
- Reuse the Home overlay in the expanded surface: rejected; wrong scene ownership.
- Traverse the whole root View tree continuously: rejected; broader and more expensive than the controller-scoped probe.
- Add a per-frame listener: rejected; unnecessary for host/anchor discovery.
- Reuse old zero-width native-participant experiments: rejected; superseded architecture with known ownership/geometry conflicts.

### Validation gate

Because executable source and the pinned compatibility/tooling contract changed, this checkpoint advances to Build 414 and requires the applicable automated validation before a signed Canary. No panel-projection behavior should be implemented until the focused device diagnostic closes the target-host/geometry evidence gap.

## 2026-09-28 — Phase 2B panel projection evidence boundary

**Type:** architecture/source review / documentation-only checkpoint
**APK build:** none; integrated runtime remains Build 413 / `20260927-413`
**Work branch:** `feat/panel-projection`

### Problem / objective

Build 413 closes Home scene-lifetime ownership for HUN, notification-shade shallow pull, and Control Center visibility handoff. Phase 2B is still incomplete because Combined Status does not yet render as a verified projection on the notification-shade / Control Center surfaces; those surfaces remain native-only while the Home overlay is suppressed.

### Problem execution flow

**Phenomenon / current state**

- Home rendering is runtime-verified and uses `MiuiNotificationStatusContainer.overlay`.
- Notification shade and Control Center have verified native lifetime owners and progress callbacks.
- Control Center already exposes bounded exact-target anchor evidence through `ControlCenterHeaderExpandController` + `StatusBarAnchorBounds`.
- Notification shade currently exposes the accepted `ShadeExpansionStateManager` motion/lifetime facts, while exact target reference identifies `NotificationHeaderExpandController` as translation owner, but the current repository does not yet have an equivalent verified target-host / target-bounds snapshot for rendering.

**Root-cause / evidence gap**

The remaining Phase-2B blocker is not another Home visibility defect. It is missing evidence for the actual target-surface projection contract, especially notification shade. Without a verified target host/bounds/tint source, writing an interpolation formula would invent geometry ownership.

### References consulted

- latest `CONTRIBUTING.md`, `CURRENT.md`, `ROADMAP.md`, and Build-413 DEVLOG closure;
- `docs/architecture/layout-policy.md` and `scene-policy.md`;
- exact-target `SystemUI-Reference/findings/scene-host-motion.md`;
- exact-target `SystemUI-Reference/findings/control-center.md`;
- exact-target verified-contract index;
- historical Builds 380-390 only as evidence, not as reusable architecture.

### Selected direction

1. Resolve notification-shade controller/host/target geometry first.
2. Reuse existing Control Center anchor diagnostics unless a concrete missing fact is identified.
3. If exact static evidence cannot close the notification target contract, add one bounded Detailed-only read-only probe at the existing native callback boundary.
4. Only after target contracts are known may a scene-specific projection session be implemented using the shared Combined Status render model.

### 审查 / review

- **Ownership:** SystemUI remains owner of notification/Control Center translation, peer layout, and native surface lifecycle.
- **Lifecycle:** no new runtime owner in this checkpoint.
- **Single writer:** no geometry/visibility writer added.
- **Cleanup:** no runtime resource added.
- **Fail native:** both surfaces remain native-only until their projection contracts are established.
- **Performance:** documentation/source review only.
- **Compatibility:** exact pinned SystemUI fingerprint remains the evidence scope.
- **Exception recovery:** unchanged.
- **Future extension:** target-surface adapters should reuse one shared renderer/state model and remain independent from the Home overlay lifecycle.

### Outcome / next step

Open a Draft Phase-2B projection PR from this checkpoint. Continue exact-target notification-header source/contract review. Create Build 414 only if a bounded runtime diagnostic is actually needed to resolve the remaining target-host/geometry facts.

## 2026-09-28 — Build 418 integrated into dev

**Type:** device-accepted runtime integration closure
**Build:** 418 / `20260928-418`
**PR:** #148 `fix/hot-reload-tint-continuity`
**dev merge commit:** `11bc4ff741869e3311d2be697d4dcfb66f5cb39c`

### Acceptance and integration

- Maintainer focused device validation reports the repeated Home/light-app Tint transition is normal and no longer reproduces the stale inversion.
- The accepted Detailed diagnostic confirms Hot Reload restoration and subsequent DarkIcon transitions keep renderer `appliedTint`, renderer `statusIconTint`, and live SystemUI status-icon authority aligned.
- PR #148 merged into `dev` through a merge commit without changing the accepted Build-418 runtime identity.
- Trusted `dev` push/integration Build #1267 / run `36346676416` passed on exact merge commit `11bc4ff741869e3311d2be697d4dcfb66f5cb39c`.
- Integration validation passed Gradle Wrapper, Java/API37 setup, signing restore, pinned HyperOS target profile, unit tests/build, Modern Xposed metadata, APK signature, non-debuggable verification, artifact preparation and upload.
- Integration Canary artifact: `CombinedStatus-0.0.2-HyperOS-20260928-418-canary.apk`, artifact id `10940941306`, GitHub artifact digest `sha256:6d9dd2e53185e4974d74f0d8fcdcd21f0d9388c6bc7362246bb497ce0ffaaec0`.

### Review / 审查

- **ownership:** SystemUI remains Tint authority; accepted snapshot composition is now part of the dev baseline.
- **lifecycle:** Hot Reload transfer is continuity/fallback only; fresh generation authority wins.
- **single writer:** renderer ownership remains unchanged.
- **cleanup/fail-native:** unchanged and Integration-tested.
- **performance:** no polling, delay, retry or frame-level Tint work was introduced.
- **compatibility:** pinned target validation passed after integration.
- **future extension:** the shared Home Tint lifecycle blocker is closed, so Phase-2B projection can resume without duplicating color policy.

### Next

Resume Draft PR #146 from current `dev`. Preserve Build-414 probe findings as historical evidence, but remove the completed diagnostic probe from the active runtime unless a newly identified evidence gap justifies another bounded diagnostic.


## 2026-09-28 — Build 418: live Tint authority snapshot at renderer commit

**Type:** single-variable Tint lifecycle correction
**Build:** 418 / `20260928-418`
**Work branch / PR:** `fix/hot-reload-tint-continuity` / Draft #148
**Device validation:** accepted

### Problem execution flow

**Phenomenon and evidence:** Build 417 selects a valid non-represented `volume` anchor and resolves black native status-icon Tint, but Combined Status can remain white. Maintainer video shows native VPN/mute icons changing black/white across repeated app/Home transitions while Combined Status can stay white; the same scene may be correct once and wrong on a later entry.

**Root cause / responsibility source:** renderer Tint state is assembled from two asynchronous sources. A fresh status-icon event can arrive before renderer attach and be lost. Later Battery/scene events currently combine fresh Battery `appliedTint` with cached presentation-store `statusIconTint`, creating a mixed-generation/mixed-scene snapshot. Normal monochrome policy prefers `statusIconTint`, so the stale secondary field can override an otherwise correct Battery event.

**Native rule:** SystemUI Home status-icon authority owns monochrome presentation. Battery DarkReceiver is a useful event trigger and fallback but must not provide or freeze another status-icon authority generation.

### Implementation

- add `CombinedStatusTintAuthority` as the deterministic composition boundary;
- Battery events resolve current Home status-icon Tint live on every renderer commit;
- status-icon observer events directly refresh only the renderer's status authority while preserving current Battery applied tint;
- after Hot Reload observer attach, transferred Tint is rebased against the new generation's live status-icon authority before renderer attach;
- if live status authority is unavailable, fall back to transferred status Tint, then Battery applied Tint;
- renderer diagnostics emit both `appliedTint` and `statusIconTint` on every changed Tint state in Detailed mode;
- Battery semantic-color, geometry, masking, scene ownership, transfer payload shape and animation code are unchanged.

### Tests

Deterministic tests cover:
- live status-icon authority wins over stale embedded status Tint on Battery events;
- Battery applied tint is the fail-native fallback;
- status-icon events update status authority without overwriting Battery applied tint;
- status-icon events can seed a renderer Tint state;
- Hot Reload transfer is rebased to new-generation live authority;
- transferred status Tint remains fallback when live authority is unavailable.

### 审查 / review

- **Ownership:** one composition boundary decides renderer Tint; SystemUI status icons own monochrome direction, Battery owns its applied-tint input/event timing.
- **Lifecycle:** new-generation authority supersedes transfer before renderer attach.
- **Single writer:** `CombinedStatusHomeRenderSession -> CombinedStatusRenderController` remains the only renderer writer.
- **Cleanup:** no new listener/hook/observer.
- **Fail native:** live authority -> transferred status tint -> Battery applied tint.
- **Performance:** one bounded current-authority read on existing Dark/scene events; no polling or frame work.
- **Compatibility:** reuses already validated Home manager/group contracts and existing hooks.
- **Exception recovery:** null/transparent authority falls through existing visible-color checks.
- **Future extension:** provides a coherent snapshot boundary reusable by panel/lockscreen projections.

### Draft validation / source review

- Draft Light #1254 passed on the first Build-418 implementation checkpoint.
- Post-implementation review removed a redundant Battery replay from the status-icon callback so one status event produces one status-authority commit.
- Battery-event composition was tightened further: when live SystemUI status authority is unavailable it now falls directly to the current Battery applied tint rather than accepting any embedded stale status field.
- Deterministic test coverage was extended for this stale-embedded-status rejection.
- Draft Light #1255 passed on runtime head `dda51a7efa93131ab9fe0203f3b4f42d6ee8f161`.
- Final cleanup removes an unused one-shot Tint-log flag; Detailed diagnostics intentionally log each **changed** renderer Tint state because scene transitions are low-frequency, event-driven checkpoints rather than frame events.
- Draft Light #1256 and #1257 both passed after the single-event/single-commit cleanup. Exact runtime/source head before this record-only closure: `c05d2ee3c094b3331135d66565771b67e7faf6cb`.
- Exact-target SystemUI-Reference review found no verified stable `DarkIconDispatcher.addDarkReceiver/removeDarkReceiver` registration contract. Build 418 therefore deliberately reuses already-validated native events and live Home status-icon reads rather than widening the Hook/registration surface.

### Fast / Canary result

- Final Draft Light #1256 passed on exact PR head `c05d2ee3c094b3331135d66565771b67e7faf6cb`.
- PR #148 was marked Ready only after Draft Light and source review.
- Fast Build #1257 passed on the same exact head, including pinned HyperOS target-profile verification, unit tests, Debug build and Modern Xposed metadata.
- Explicit maintainer `/canary` request produced Work Branch Canary #403.
- Canary trusted-source resolution, tested-head checkout verification, signing restore, pinned target profile, Canary build, Xposed metadata, APK signature, non-debuggable verification, artifact preparation and upload all passed.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260928-418-canary.apk`.
- GitHub artifact digest: `sha256:f02e6db7c1d9c51aaceaf2e60392beffb5e4442f140d4804f350bf978e81d996`.
- Extracted APK SHA-256: `45ffb3d8b894dcaff482d1bdd350bc0b29ddf57e48900347ee9e93d50c4a7761`.
- Runtime is frozen at Build 418 pending repeated scene-switch device evidence.
- This record-only closure does **not** create a new runtime Build.

### Final exact-head revalidation

- The earlier Fast #1257 / Canary #403 validation remains valid historical evidence for the same Build-418 runtime code.
- Subsequent record-only commits advanced PR #148 to exact head `42f350c2bb8d7338906469454fadabc5dcb629de` without changing runtime source after `c05d2ee3c094b3331135d66565771b67e7faf6cb`.
- Ready checkpoint Fast #1261 passed on exact PR head `42f350c2bb8d7338906469454fadabc5dcb629de`, including pinned target profile, unit tests, Debug build and Modern Xposed metadata.
- One duplicate Work Branch Canary request (#404) passed trust/checkout/profile/signing setup but was cancelled during the build by the later Canary request through workflow concurrency; this is **not** a runtime failure.
- Work Branch Canary #405 completed successfully on trusted source SHA `42f350c2bb8d7338906469454fadabc5dcb629de`. The completed job verified trusted-source resolution, exact checkout, pinned target profile, tests/Canary build, Modern Xposed metadata, Haple APK signature, non-debuggable status and artifact upload.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260928-418-canary.apk`.
- GitHub artifact digest: `sha256:2a89a293fe636703494776682d8f08f071872790d327143c54317ff0e1a16a1b`.
- Extracted APK SHA-256: `eac3105741237f361d72f3227db6b8a45daa13508ba244048ae77ff76c8642c5`.
- PR #148 is returned to Draft and runtime remains frozen. This record-only closure does **not** create another runtime Build.

### Device acceptance — Build 418

- Maintainer device result: **normal** after the focused repeated Home -> light app -> Home -> light app test.
- Validation was performed on Build 418 Canary with Detailed diagnostics enabled and without requiring a SystemUI restart to recover presentation.
- Runtime health reports the Hot Reload generation, status-icon observation, renderer, presentation cutover, panel transition source, and network suppression owner as ready.
- Hot Reload observer attach resolves the visible non-represented `volume` peer at `#bf000000`; renderer transfer state starts with `appliedTint=#bf000000` and `statusIconTint=#bf000000`.
- During subsequent DarkIcon transitions, renderer commits track the current native authority rather than stale cache: `appliedTint`, `statusIconTint`, and `liveStatusIconTint` move together through the native intermediate shades and final light/dark endpoints.
- Later status-icon observation transitions from light back to dark are also reflected without requiring SystemUI recreation.
- The maintainer reports the previously reproducible "one entry correct, next entry wrong" behavior is no longer observed.

**Conclusion:** Build 418 is accepted. The root cause is closed as a mixed-generation/mixed-scene Tint snapshot race: cached status authority was being combined with newer Battery/Dark events and fresh authority could arrive before renderer attachment. The accepted mechanism keeps SystemUI as the sole presentation authority, composes one live authority snapshot per renderer commit, and treats Hot Reload transfer only as bounded continuity/fallback.

**Integration decision:** close the fix branch into `dev`, then resume Phase-2B projection. No further Build increment is created by acceptance documentation.

### Device gate

Install the signed Build-418 Canary without first restarting SystemUI. Trigger module Hot Reload, then repeatedly switch Home -> a light app -> Home -> the same light app. Compare Combined Status with native VPN/mute/status icons on every transition. If mismatch appears, export Detailed diagnostics before any SystemUI restart.


### Fast / Canary checkpoint

- Final Draft validation on the reviewed branch state passed before the Ready gate; record-only intermediate runs that were superseded by a newer same-PR run are not treated as runtime failures.
- PR #148 was marked Ready only after source review.
- Fast Build #1261 / run `36345741835` passed on exact PR head `42f350c2bb8d7338906469454fadabc5dcb629de`, including pinned HyperOS target-profile verification, unit tests, Debug build, and Modern Xposed metadata.
- Explicit maintainer Canary #404 began on the same trusted SHA but was cancelled when Canary #405 entered the same `work-canary-148` concurrency group; `cancel-in-progress: true` made #405 supersede #404. #404 therefore is not a code/test rejection.
- Work Branch Canary #405 / run `36345969171` resolved and verified trusted source SHA `42f350c2bb8d7338906469454fadabc5dcb629de` and verified Fast run `36345741835`.
- Canary #405 passed unit tests, Canary build, pinned target profile, Modern Xposed metadata, Haple signature verification, non-debuggable verification, artifact preparation, and upload.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260928-418-canary.apk`.
- GitHub artifact digest: `sha256:2a89a293fe636703494776682d8f08f071872790d327143c54317ff0e1a16a1b`.
- Extracted APK SHA-256: `eac3105741237f361d72f3227db6b8a45daa13508ba244048ae77ff76c8642c5`.
- PR #148 returned to Draft after Canary success. Runtime is frozen until maintainer device feedback.
- This record-only closure does **not** create another runtime Build; Build identity remains 418 / `20260928-418`.

### Device gate

Install the signed Build-418 Canary without restarting SystemUI first. Trigger module Hot Reload, then repeatedly switch between the light app surface(s) and dark Home used in the maintainer video. Compare Combined Status against native VPN/mute icons on every entry/exit. If any mismatch occurs, export Detailed diagnostics before restarting SystemUI.

## 2026-09-28 — Build 417 device rejection: fresh Tint lost before renderer attach / repeated scene race

**Type:** maintainer device rejection / lifecycle root-cause narrowing
**Rejected build:** 417 / `20260928-417`
**Stable baseline:** 413 / `20260927-413`
**Work branch / PR:** `fix/hot-reload-tint-continuity` / Draft #148

### Maintainer evidence

Build 417 remains visually incorrect. The supplied video shows the native VPN/mute status icons switching between light and dark presentation across repeated app/Home transitions while Combined Status can remain white. The maintainer also reports that a scene may be correct on one entry, then become incorrect after leaving and entering again.

### Diagnostic facts

- Build 417 is confirmed Canary.
- Hot Reload completes without SystemUI restart.
- The represented-slot exclusion works: the new generation selects `tintAnchorSlot=volume`, `tintAnchorClass=StatusBarIconView`.
- At observer attach, `locationAwareTint`, peer tint and manager fallback all resolve `#bf000000`.
- The renderer is attached **after** that fresh observer event.
- Renderer seed is reported as `homeRenderTint source=hotReloadTransfer applied=#bf000000`; that diagnostic logs only `appliedTint`, not `statusIconTint`.
- Later scene activity produces Battery `onDarkChangedInternal` events, but no guaranteed synchronized status-icon presentation refresh is paired with each Battery event.

### Root-cause correction

Build 417 rejects the assumption that choosing the correct visible peer/anchor is sufficient. The remaining defect is an authority snapshot / lifecycle ordering problem:

1. the fresh new-generation status-icon Tint can be observed before the renderer exists, so the event cannot update renderer state;
2. the renderer then consumes the transferred old-generation Tint state;
3. later `onTintStateUpdate()` combines a fresh Battery `appliedTint` with `statusIconTint` taken from the presentation store cache, which is not guaranteed to represent the same SystemUI DarkIcon generation/scene;
4. the visual policy prefers `statusIconTint` for normal monochrome rendering, so a stale secondary field can keep Combined Status white even when the logged `appliedTint` is black.

This explains both:
- Hot Reload abnormal / full SystemUI restart healthy;
- repeated scene entry where one transition is correct and a later entry is wrong.

### Selected Build-418 boundary

- Treat Battery DarkReceiver as event trigger and fallback, not primary status-icon color authority.
- For every renderer Tint commit, resolve current Home status-icon Tint live from SystemUI.
- Before renderer attach after Hot Reload, rebase transferred state with the new-generation live status-icon authority.
- Keep transferred/cached status-icon Tint only as fallback if live native authority is unavailable.
- Add renderer diagnostics for both `appliedTint` and `statusIconTint`.
- No timer, polling, delayed retry, forced DarkIcon refresh, or additional native writer.

### 审查 / review

- **Ownership:** SystemUI Home status-icon authority owns monochrome presentation; Battery owns only its own applied tint/event timing.
- **Lifecycle:** fresh generation authority must supersede old-generation transfer before visible renderer ownership starts.
- **Single writer:** renderer state remains owned by `CombinedStatusHomeRenderSession -> CombinedStatusRenderController`; only input authority resolution changes.
- **Cleanup:** unchanged.
- **Fail native:** live status-icon authority falls back to current Battery applied tint / transferred state only when unavailable.
- **Performance:** bounded synchronous read on existing native Tint/scene events; no frame loop.
- **Compatibility:** uses already validated manager/group reflection contracts; no new hook target.
- **Exception recovery:** existing null/reflection fallbacks remain.
- **Future extension:** establishes one-time-consistent Tint snapshots needed by panel/lockscreen projection later.

### Gate

Record this rejection before runtime mutation. Build 418 is the next single-variable checkpoint.


## 2026-09-28 — Build 417: exclude represented slots from visible Home Tint authority

**Type:** single-variable ownership correction
**Build:** 417 / `20260928-417`
**Work branch / PR:** `fix/hot-reload-tint-continuity` / Draft #148
**Device validation:** pending

### Problem execution flow

**Phenomenon and evidence:** Build 416 remains inverted after module Hot Reload, while a full SystemUI restart restores correct behavior. The abnormal diagnostic shows the retained native Wi-Fi path carrying `#bf000000` during the Hot Reload session while visible neighboring status icons can present the opposite monochrome direction.

**Root cause / responsibility source:** the current Home tint resolver treats attached/sized native children as visible tint candidates even when Combined Status has taken over their visible presentation. `resolveTintAnchorView()` can therefore pick the rightmost represented Wi-Fi/mobile slot, and peer tint traversal can also consume represented slots. After Hot Reload those native Views are intentionally kept alive for state/lifecycle continuity but their presentation state is not authoritative for what the user sees.

**Repository/native rule:** preserving a native View for lifecycle ownership does not imply that it remains the visible presentation authority. Tint ownership must follow the actual visible Home peers.

**Selected correction:** exclude `wifi`, `mobile`, `stacked_mobile`, `airplane` and `no_sim` from visible Home Tint anchor/peer eligibility. Require the candidate View to be `VISIBLE` with positive geometry. Use the existing location-aware `DarkIconDispatcher.getTint(...)` against that non-represented peer; keep manager-global/cached fallback.

### Implementation boundary

- one new shared candidate predicate;
- anchor selection and peer tint traversal consume the same predicate;
- represented-slot fallback traversal is removed;
- diagnostics now report the selected tint-anchor slot/class;
- no Hot Reload payload change;
- no Battery semantic-color change;
- no geometry, mask, scene, animation or panel-projection change;
- Build identity advances from 416 to 417.

### Tests

Added deterministic coverage that:
- `wifi`, `mobile`, `stacked_mobile`, `airplane`, and `no_sim` are never eligible visible Tint authorities;
- a visible, positive-geometry non-represented peer (for example `vpn`) is eligible;
- invisible or zero-geometry peers are rejected.

### 审查 / review

- **Ownership:** represented native slots remain state/lifecycle carriers; visible non-represented SystemUI peers own Home monochrome presentation authority.
- **Lifecycle:** directly addresses the Hot Reload-only stale represented-slot state without changing cold-start behavior.
- **Single writer:** read-only Tint selection; no native Tint writer.
- **Cleanup:** unchanged.
- **Fail native:** missing eligible peer falls through to manager-global/cached authority.
- **Performance:** bounded existing-group traversal only on existing events; no polling/frame work.
- **Compatibility:** no new private class/method/hook contract.
- **Exception recovery:** existing reflection fallbacks remain.
- **Future extension:** makes presentation authority explicit and reusable for later scene projection.

### CI / source-review status

- Draft Light #1246 failed before Android/Gradle execution because the newly added DEVLOG metadata lines contained trailing whitespace. This was a documentation-format failure only and did not validate or reject Build 417.
- The whitespace-only record correction preserved Build identity.
- Draft Light #1247 then passed on the documentation-closure head.
- Post-review hardening additionally excludes `combined_status` itself and makes any explicit anchor parameter obey the same candidate predicate, preventing a future caller from bypassing the visible-authority boundary.
- Draft Light #1248 passed on exact source head `be9e14db5f0670f875d828129878b1b2c1c422bd`.
- Source review is clean for ownership, lifecycle, single writer, cleanup, fail-native behavior, performance, compatibility, exception recovery and future extension.

### Fast / Canary result

- PR #148 was marked Ready only after Draft Light and source review.
- Fast Build #1250 passed on exact tested runtime SHA `c341fd52af8e1b873278088f3cc3c7303f9e4da3`, including pinned HyperOS target-profile verification, unit tests, Debug build, and Modern Xposed metadata.
- Maintainer `/canary` request produced Work Branch Canary #402.
- Canary trust gates passed: trusted source resolution, checkout of the tested work-branch SHA, and checked-out-source verification.
- Signing restore, pinned target profile, Canary build, Xposed metadata, APK signature, non-debuggable verification, artifact preparation, and upload all passed.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260928-417-canary.apk`.
- GitHub artifact digest: `sha256:7866447367bb6a0c062384246f30ce6c4ff59db148d8a61cb33a6d2c6d4a740a`.
- Extracted APK SHA-256: `e816e303c08f8ac754bca2333ab780c69dcd728a10cf238c82dff8ae98db1cad`.
- PR returned to Draft after the signed checkpoint was produced. No runtime changes are allowed until maintainer device feedback.
- This record-only closure does **not** create a new runtime Build; runtime identity remains Build 417 / `20260928-417`.

### Device gate

Install the signed Build-417 Canary without first restarting SystemUI. Trigger module Hot Reload and compare dark/light Home surfaces. If the defect remains, export Detailed diagnostics **before** restarting SystemUI. Acceptance requires both correct visible tint and a non-represented `tintAnchorSlot` / `tintAnchorClass`.


## 2026-09-28 — Build 416 device rejection: SystemUI restart isolates Hot Reload lifecycle

**Type:** maintainer device rejection / root-cause narrowing
**Rejected build:** 416 / `20260928-416`
**Stable baseline:** 413 / `20260927-413`
**Work branch / PR:** `fix/hot-reload-tint-continuity` / Draft #148

### Maintainer evidence

The Build-416 signed Canary remains visually incorrect after module Hot Reload. The same installed Build becomes visually correct after a full SystemUI restart.

Screenshots show the Combined Status monochrome direction disagreeing with neighboring native Home icons across dark/light surfaces after Hot Reload. The supplied Detailed diagnostic is Build 416 Canary and captures the abnormal post-Hot-Reload session.

### Diagnostic facts

- Hot Reload completes with `restartScope=false` and restores the existing Home host.
- Initial restored renderer tint is `#bf000000`.
- `statusIconPresentation` later alternates between `#bf000000` and `#e6ffffff`, with `tintAuthority=dispatcher-location-aware`.
- The current resolved anchor/peer search is allowed to use represented native slots that remain attached and sized even while Combined Status owns their visible presentation.
- The native Wi-Fi View remains present and reports a dark tint in the captured abnormal Hot Reload session.
- Full SystemUI recreation clears the defect without changing the installed Build.

### Historical correction

Build 416 rejects the assumption that changing Tint authority precedence alone closes the issue. The dispatcher calculation is not sufficient if the anchor itself belongs to a represented/masked native slot whose post-Hot-Reload presentation state is no longer authoritative for what the user actually sees.

The stronger discriminator is lifecycle: cold SystemUI recreation is healthy while module-generation handoff is not. Therefore steady cold-start color policy is not reopened by default.

### Selected next boundary

Audit and correct the Home Tint authority candidate set:

- represented/masked native slots (`wifi`, `mobile`, `stacked_mobile`, `airplane`, `no_sim`) remain alive for native lifecycle/state ownership but must not be treated as visible Home tint peers/anchors;
- use a genuinely visible, non-represented Home peer as the location-aware dispatcher anchor / static-tint peer when available;
- only then fall back to manager-global/cached sources;
- keep Hot Reload transfer v7, Battery semantic colors, geometry, scene ownership and rendering unchanged.

### 审查 / review

- **Ownership:** visible Home peer tint belongs to still-visible native SystemUI participants; represented/masked slots are state/lifecycle carriers, not visible tint authorities.
- **Lifecycle:** directly addresses the Hot Reload-only stale represented-slot state while preserving the healthy full-restart path.
- **Single writer:** read-only authority selection; no SystemUI tint writer.
- **Cleanup:** unchanged.
- **Fail native:** if no valid visible peer/anchor is available, use the existing manager/global/cached fallback rather than fabricating a color.
- **Performance:** bounded traversal of the already-captured Home icon group on existing events only.
- **Compatibility:** no new private class/method/hook contract.
- **Exception recovery:** existing reflective fallbacks remain.
- **Future extension:** separates “kept alive for state” from “authoritative for visible presentation,” which is required for future scene projections as well.

### Gate

Record this rejection before further runtime mutation. Build 417, if implemented, must be the single-variable represented-slot authority correction and must pass source review/tests, Fast CI and one signed Canary before another device pass.


## 2026-09-28 — Build 416 location-aware Home tint authority

**Type:** single-variable runtime authority correction
**Display version:** 0.0.2
**Build:** 416 / `20260928-416`
**Work branch / PR:** `fix/hot-reload-tint-continuity` / Draft #148
**Device validation:** pending

### Change boundary

Build 416 retains the full Build-415 Hot Reload lifecycle correction and changes only how the Home status-icon monochrome tint is selected.

Previous order:
`recursive peer tint -> manager/global tint -> cached fallback`.

Build-416 order:
`DarkIconDispatcher location-aware tint -> peer/static tint -> manager-global fallback -> cached fallback`.

The location-aware path uses the already-present runtime contract:
`DarkIconDispatcher.getTint(mTintAreas, anchorView, mIconTint)`.

The resolved anchor remains the existing non-Combined Home status-icon anchor. No new SystemUI class, field, hook or callback is introduced.

### Implementation details

`SystemUiNativeNetworkSuppressionOwner` now separates:
- `resolveLocationAwareManagerTint()`: resolves dispatcher `mIconTint`, `mTintAreas`, and invokes `getTint(...)` for the Home anchor;
- `resolveManagerFallbackTint()`: reads manager-global `mColor` and then dispatcher-global `mIconTint` only as fallback;
- `selectStatusIconTint()`: gives the location-aware result first priority.

Diagnostics distinguish:
- `dispatcher-location-aware`;
- `peer-static-applied`;
- `manager-global-fallback`;
- `cached-fallback`.

The peer resolver is intentionally left otherwise unchanged in this checkpoint so Build 416 tests one authority-order correction rather than simultaneously rewriting peer traversal.

### Tests

`SystemUiNativeNetworkSuppressionOwnerTest` now pins:
- location-aware tint wins even when peer/global/cached values disagree;
- peer tint remains the first fallback when location-aware resolution is unavailable;
- manager-global tint remains the next fallback when peer tint is invalid.

Existing Build-415 Hot Reload transfer/fail-native tests remain intact.

### 审查 / review

- **Ownership:** SystemUI's native DarkIconDispatcher remains the source of truth; Combined Status only consumes its computed tint for the actual Home anchor.
- **Lifecycle:** unchanged from Build 415; no new listener or scheduling path.
- **Single writer:** read-only selection only; no native color/tint mutation.
- **Cleanup:** unchanged.
- **Fail native:** dispatcher resolution failure falls through to existing peer/global/cached sources.
- **Performance:** constant-time reflection and one native static tint computation on existing event-driven observation; no periodic work.
- **Compatibility:** no new private runtime contract; only reorders contracts already used by the branch.
- **Exception recovery:** reflective dispatcher failure is local and does not block Home runtime state.
- **Future extension:** establishes one position-aware Home monochrome authority suitable for reuse by future scene projections.

### Validation gate

Keep PR #148 Draft through source review and Light. If clean, move Ready for Fast. Only then request one signed Canary and repeat the same Hot Reload inversion test without restarting SystemUI.


## 2026-09-28 — Build 415 device rejection and tint-authority root-cause correction

**Type:** maintainer device rejection / root-cause correction
**Display version:** 0.0.2
**Rejected build:** 415 / `20260928-415`
**Work branch / PR:** `fix/hot-reload-tint-continuity` / Draft #148
**Stable baseline remains:** Build 413 / `20260927-413`

### Maintainer feedback

Build 415 still shows incorrect Combined Status inversion/tint after installing over Build 414 and using module Hot Reload without restarting SystemUI. Screenshots show Combined Status using the opposite light/dark direction from neighboring native status icons on the same surface.

The supplied Build-415 Detailed diagnostic is the abnormal pre-restart state and therefore is the authoritative device evidence for this failure.

### Diagnostic facts

- Build identity is `20260928-415`, Canary, Detailed diagnostics.
- Hot Reload restores successfully and reports `tintTransfer=native-fallback`, which is expected for the first 414 -> 415 upgrade because Build 414 emits the older payload.
- The new Home generation initially remains non-presenting until a native tint event arrives.
- At the first accepted new-generation tint event, `MiuiBatteryMeterView` / Battery anchor reports `#bf000000`; Home becomes presentation-ready from that event.
- Immediately afterward status-icon observation reports the Home peer/manager tint in the light family (`#e5fcfcfc` / `#e6ffffff`).
- During the same captured session status-icon observation subsequently alternates between light and dark values. This matches the maintainer's visual report that Combined Status remains inverted relative to neighboring native icons.
- Build 415's Hot Reload lifecycle change therefore behaved as designed but waited for the wrong authority source.

### Historical correction

The Build-414 conclusion that missing Hot Reload tint continuity was the root cause was **incomplete**.

The continuity gap is real and Build 415's transfer/fail-native mechanics remain valuable:
- same/new payload generations can carry stable primitive tint across Hot Reload;
- legacy payloads no longer trust an arbitrary handoff-time live Battery TextView color.

However, Build 415 proves that continuity alone cannot fix the defect because the first later Battery tint callback can itself disagree with the actual Home status-icon presentation.

The root cause moves upstream from **when tint is accepted** to **which native tint authority is authoritative for the Combined Status Home location**.

### Source review

Current implementation has two overlapping tint pipelines:

1. `SystemUiTintStateSource` follows `MiuiBatteryMeterView` and reads `mBatteryPercentView.currentTextColor`; this is logged as `battery-anchor-fallback`.
2. `SystemUiNativeNetworkSuppressionOwner` observes the Home `MiuiStatusIconContainer`, its native peers, and the `DarkIconManager / DarkIconDispatcher`; this becomes `statusIcons.appliedTint` and is already preferred by `CombinedStatusColorPolicy` when present.

The second resolver currently has two weak ordering choices:
- `resolveManagerAppliedTint()` returns opaque manager-global `mColor` before attempting the already-implemented `DarkIconDispatcher.getTint(mTintAreas, anchorView, mIconTint)` location-aware calculation;
- `selectStatusIconTint()` gives recursively discovered peer tint priority over manager tint, while recursive peer search can descend into internal child views that are not the final visible status-icon authority.

The exact device symptom is therefore consistent with a project-side authority-selection issue, not a need for new dark-mode timing logic.

### Selected Build-416 boundary

Use the existing native location-aware DarkIconDispatcher calculation as primary Home monochrome tint authority:

- resolve `mDarkIconDispatcher.mIconTint` and `mTintAreas`;
- when a valid Home anchor is available, call the existing exact runtime `DarkIconDispatcher.getTint(...)` path first;
- only fall back to manager-global `mColor`, visible-peer/static tint, or cached tint when the location-aware result cannot be resolved;
- keep `CombinedStatusBatteryColorPolicy` unchanged so semantic charging/power-save/performance/low colors remain native-semantic while NORMAL continues to follow the status-icon tint;
- retain Build-415 Hot Reload transfer/fail-native mechanics unchanged.

### 审查 / review

- **Ownership:** HyperOS/SystemUI remains tint authority; Combined Status stops privileging a project-observed Battery text color or manager-global value over the native location-aware dark dispatcher result.
- **Lifecycle:** no new listener, timer, polling or frame loop.
- **Single writer:** read-only authority selection only; no SystemUI color writer is introduced.
- **Cleanup:** unchanged from Build 415.
- **Fail native:** if location-aware dispatcher tint cannot be resolved, existing manager/peer/cached fallback remains available.
- **Performance:** constant-time reflection/read during existing event-driven observation; no new periodic work.
- **Compatibility:** no new private class/field contract is introduced in the first correction; it reorders already-used runtime contracts.
- **Exception recovery:** dispatcher reflection failure falls through to existing fallback sources.
- **Future extension:** establishes one Home monochrome authority that can later be reused by panel projections without duplicating color policy.

### Validation gate

Advance the next executable correction to Build 416. Keep PR #148 Draft until source review/tests pass, then Fast + one signed Canary + focused device Hot Reload inversion test. Do not resume PR #146 until this shared tint authority is accepted.


## 2026-09-28 — Build 415 Hot Reload tint continuity implementation

**Type:** runtime lifecycle correction
**Display version:** 0.0.2
**Build:** 415 / `20260928-415`
**Work branch / PR:** `fix/hot-reload-tint-continuity` / Draft #148
**Device validation:** pending

### Implementation

The Hot Reload payload advances from v6 to v7 and adds only classloader-neutral tint primitives:

- last stable renderer `appliedTint`;
- optional last stable `statusIconTint`.

The old generation captures these values from `CombinedStatusHomeRenderSession` before teardown. The new generation reconstructs a `CombinedStatusTintState` locally and gives it to the new Home render session as continuity state.

`CombinedStatusHomeRenderSession` now uses a lazy initial-tint policy:

- valid transferred tint wins immediately and the live Battery View is not read;
- cold start keeps the existing native live seed path;
- Hot Reload without transferred tint does **not** read the handoff-time live View and therefore keeps `tintReady=false` / native handoff active until a real new-generation native tint event arrives.

The last case is required for the first Build-414 -> Build-415 Hot Reload because Build 414's v6 payload cannot contain the new tint fields. v6 and older payloads remain accepted by the v7 restore logic.

### Tests

`CombinedStatusHomeRenderSessionTest` now verifies:

- a valid transferred tint wins without invoking the live tint provider;
- invalid transferred tint may fall back to the existing live seed when live seeding is explicitly allowed;
- legacy Hot Reload with no transferred tint and live seeding disabled returns no initial tint and never reads the transient live provider.

### 审查 / review

- **Ownership:** native HyperOS/SystemUI tint events remain authoritative; transferred tint is only continuity of the last already-accepted renderer state.
- **Lifecycle:** v7 continuity spans one Hot Reload generation boundary; first new native tint update supersedes it through the unchanged `onTintStateUpdate` path.
- **Single writer:** no SystemUI color/tint/dark-mode writer is added.
- **Cleanup:** raw transfer contains primitive integers only; no old-generation tint object or View crosses the classloader boundary.
- **Fail native:** legacy/missing/invalid tint transfer keeps native presentation until the new generation observes authoritative tint; it does not show Combined Status with a guessed color.
- **Performance:** constant-time state transfer only; no polling, timer, retry, traversal, or frame listener.
- **Compatibility:** payload v7 explicitly preserves v6 Control Center, v5 shade, and older restore formats. Cold start is unchanged.
- **Exception recovery:** absence of tint continuity does not invalidate network/model/panel state transfer; it narrows fallback to native presentation until tint becomes ready.
- **Future extension:** panel projection remains independent and paused; once this shared Hot Reload presentation path is accepted, #146 can rebase/update from the integrated fix.

### Validation gate

Build 415 changes executable Hot Reload lifecycle behavior and therefore requires Fast validation plus one trusted signed Canary and focused maintainer device validation before integration.


## 2026-09-28 — Hot Reload tint continuity root-cause checkpoint

**Type:** root-cause review / branch-scope checkpoint
**Input evidence:** Build 414 / `20260928-414` device diagnostic
**Fix branch:** `fix/hot-reload-tint-continuity`
**Runtime fix build:** not assigned yet

### Problem execution flow

**Phenomenon**

Installing Build 414 and using module Hot Reload can leave Combined Status with incorrect inversion/tint. Restarting SystemUI restores correct behavior. The supplied Detailed diagnostic captures the abnormal Hot Reload session before that restart.

**Observed ordering**

- new-generation presentation hooks install successfully;
- structured health still reports tint as not yet observed;
- Home renderer/session is restored and seeds a live color immediately;
- Home reaches presentation-ready state;
- only afterward does native `MiuiBatteryMeterView.onDarkChangedInternal` arrive.

**Source review**

Hot Reload teardown resets both `SystemUiPresentationRuntimeOwner` (which clears `SystemUiTintStateSource`) and `CombinedStatusPresentationStateStore`. The current classloader-neutral transfer carries Combined Status model/network state and panel eligibility, but no stable tint continuity.

On reattach, `CombinedStatusHomeRenderSession.start()` calls `SystemUiTintStateSource.currentState(battery)`. That function refreshes from the live battery-percent TextView when possible. During Hot Reload handoff this live View can reflect a transient native color state before the new generation receives its first authoritative tint event.

### Root cause status

**High confidence / evidence-backed:** Hot Reload lacks a transferred stable tint state and can therefore expose a transient live View tint as the new renderer's initial stable tint. The observed device symptom and event ordering match this gap.

The Build-414 notification-header probe is not a tint/alpha/layout writer. Its extra hook may perturb timing enough to expose the weakness, but it is not the tint authority and is not selected as the root cause.

### Selected correction

Transfer the last already-accepted Combined Status tint as classloader-neutral primitive values during Hot Reload preparation. On the new generation:

- seed the Home renderer from that transferred stable tint;
- do not immediately overwrite it with a live View read during the handoff;
- let the first real native tint callback replace it naturally;
- keep cold-start live seeding unchanged;
- fall back to native re-observation if transferred tint is absent/invalid.

### 审查 / review

- **Ownership:** SystemUI remains the sole tint authority; transfer preserves continuity only.
- **Lifecycle:** transferred tint survives one module-generation handoff and is superseded by the first new native event.
- **Single writer:** no dark-mode or SystemUI tint writer is added.
- **Cleanup:** only primitive tint values cross generations; no module object/View is transferred as tint state.
- **Fail native:** older/missing/invalid transfer uses existing native re-observation.
- **Performance:** no delay, polling, retry or frame work.
- **Compatibility:** Hot Reload payload restore remains backward-compatible.
- **Exception recovery:** failed tint transfer does not block other restored runtime state.
- **Future extension:** fix is independent from panel projection and will be integrated before PR #146 resumes.


## 2026-09-28 — Build 413 stable promotion to main

**Type:** validated runtime promotion / stable-baseline closure
**Display version:** 0.0.2
**Build:** 413 / `20260927-413`
**Promotion PR:** #147
**Exact promoted dev candidate:** `36ce04011f0a1fb2c5dd185d911b639bb1787408`
**Stable merge commit:** `3114ade06bcb4846a5654a70f38ea572f5b47b37`

### Promotion evidence

- Build 413 had already passed focused maintainer device validation for the HUN disappearance while preserving the accepted shallow-pull handoff.
- PR #142 integrated the runtime checkpoint into `dev` as `2aa6833cfca69a59af5027a7855b7d8282dbade9`.
- Trusted dev Integration Build #1200 / run `36337302873` passed.
- The only commits after the integrated runtime commit and before promotion candidate `36ce04011f0a1fb2c5dd185d911b639bb1787408` changed only `CURRENT.md` and `DEVLOG.md`; no APK/runtime path changed.
- `validation/dev` was moved to the exact candidate only after the maintainer accepted the required device scenarios.
- Promotion readiness then reported **READY: dev is CI-green and device-validated**.
- Promotion PR #147 contained the exact validated candidate and no Build-414 diagnostic work.
- Stable-boundary Full Build #1213 passed before merge.
- PR #147 was merged with an explicit merge commit, preserving the stable-baseline boundary.
- Post-merge Full Build #1214 passed, including pinned target-profile verification, tests/build, Modern Xposed metadata, Haple signatures, non-debuggable Canary validation, and artifact preparation/upload.
- Push on main #59 completed successfully, including CodeQL analysis.

### 审查 / review

- **Promotion scope:** exact validated dev state only; no new runtime edit on `promote/build-413`.
- **Ownership/lifecycle:** unchanged from the already device-accepted Build-413 model.
- **Single writer / cleanup:** unchanged.
- **Compatibility:** exact HyperOS target profile remained green at promotion and post-merge Full validation.
- **Stable isolation:** PR #146 / Build 414 remained on its separate work branch and was not included in the promotion.
- **Rollback clarity:** stable boundary is the explicit merge commit `3114ade06bcb4846a5654a70f38ea572f5b47b37`.

### Outcome

Build 413 is now the stable `main` runtime baseline and remains the integrated `dev` baseline. Ongoing Phase-2B panel-projection diagnostics continue separately on PR #146 / Build 414.


## 2026-09-28 — Build 413 stable promotion to main

**Type:** stable-baseline promotion closure
**Display version:** 0.0.2
**Runtime build:** 413 / `20260927-413`
**Promotion PR:** #147
**Validated dev candidate:** `36ce04011f0a1fb2c5dd185d911b639bb1787408`
**Main promotion merge:** `3114ade06bcb4846a5654a70f38ea572f5b47b37`

### Promotion evidence

- Build 413 was already focused-device accepted for the HUN disappearance and the preserved shallow-pull scene boundary.
- PR #142 integrated the accepted runtime into `dev` as `2aa6833cfca69a59af5027a7855b7d8282dbade9`.
- Trusted Integration Build #1200 / run `36337302873` passed.
- The two commits after that runtime integration point changed only `CURRENT.md` and `DEVLOG.md`; no APK/runtime path changed before promotion.
- `validation/dev` was advanced to the exact dev candidate only after maintainer device acceptance. Promotion readiness then reported **READY: dev is CI-green and device-validated**.
- Promotion PR #147 was created from that exact candidate and contained no new engineering delta.
- Stable-boundary Full Build #1213 passed before merge.
- PR #147 was merged to `main` with the required merge commit.
- Post-merge `Push on main` #59 and main Full Build #1214 both passed.

### 审查 / review

- **Scope:** promotion contains the accepted Build-413 dev state only.
- **Runtime:** no new runtime code was introduced on the promotion branch.
- **Device evidence:** current and exact for the runtime candidate; no runtime-affecting delta existed after the accepted Build-413 integration.
- **Ownership/lifecycle/single writer/cleanup:** unchanged from the accepted Build-413 architecture.
- **Compatibility/signing:** stable-boundary Full validation passed after merge.
- **Separation from next work:** PR #146 / Build 414 notification-header diagnostics are not part of this promotion and remain a separate Phase-2B work branch.

### Outcome

Build 413 is now the stable `main` runtime baseline. Continue Phase 2B panel-projection investigation on PR #146 without changing the stable baseline until its own evidence and validation gates are satisfied.


## 2026-09-27 — Canary admission gate hardening and bounded automation merge delegation

**Type:** repository automation / CI governance
**APK build:** none
**Runtime impact:** none

### Problem / evidence

Work Branch Canary #395 failed in `Resolve trusted source` with exit 141 after runtime PR #142 had both a successful Draft Light run and a successful Ready Fast run for the same SHA. Review of the default-branch workflow showed a `gh api ... | head -n 1` pipeline under `set -o pipefail`, creating a deterministic SIGPIPE path. The query also filtered to successful runs before choosing a result, so an older Light success could remain admissible while a newer Ready checkpoint was queued, running, or failed.

### Root cause and implementation

The gate now retrieves PR Build runs without a success filter, selects the newest exact-SHA run by `run_number` inside `jq`, and requires that newest run to be `completed/success`. This removes the shell SIGPIPE condition and preserves the intended Fast-before-Canary checkpoint semantics.

An initial edit was rejected during review because shell-tab escaping corrupted the workflow diff. That unmerged state was replaced from the clean `main` workflow before validation.

### 审查 / review

- **Runtime/APK:** no installed/runtime source, dependency resolution, APK contents, signing identity, or target profile change.
- **Security/trust:** no secret scope, actor rule, same-repository rule, or signing permission is broadened.
- **Publication:** Canary remains explicit and owner-triggered; the fix only hardens admission to the existing path.
- **Failure semantics:** newest exact-SHA PR Build must be completed/success; older success cannot mask a newer unfinished or failed checkpoint.
- **Maintenance:** removes pipeline-order ambiguity and keeps the decision in one bounded JSON selection.

### Validation

Full Build #1189 / run `36329234898` passed on `3aee0d206b0c966afed9d30d7921abb07a409f19`, including target-profile verification, tests, Debug + Canary build, Xposed metadata, and non-debuggable validation. After the governance record was added, final PR Full #1190 also passed; the squash merge to `main` was followed by successful main Full #1191, history-preserving back-sync Full #1192, and dev push Build #1193.

### Governance

The maintainer explicitly delegated future merge judgment for bounded automation-only changes. CONTRIBUTING now allows the active development operator to merge and synchronize such changes without a second maintainer confirmation only when Full/self-validation and review pass and there is no runtime/APK/dependency/signing/release/trust-boundary effect. Uncertain or broader changes still require explicit maintainer approval.

## 2026-09-28 — Build 413 HUN / shallow-pull device acceptance

**Type:** focused maintainer device acceptance / runtime checkpoint closure
**Display version:** 0.0.2
**Build:** 413 / `20260927-413`
**Work branch / PR:** `fix/home-hun-ownership` / #142
**Validated PR head:** `15b92d64440eb565e44ce8a7dda3739c9ab8964e`

### Validation evidence

- Fast Build #1195 passed on the exact current PR head after the accepted automation back-sync.
- Work Branch Canary #397 / run `36336524857` passed trusted-source resolution, pinned target-profile verification, unit tests, Canary build, Modern Xposed metadata, Haple certificate verification, and non-debuggable validation.
- Canary artifact: `10937297772`, `CombinedStatus-0.0.2-HyperOS-20260927-413-canary.apk`.
- Artifact ZIP digest: `sha256:9af1b58ad588885d7e1ced83c746c134e7fe7e22f1646a158a5557fed277bd30`.
- Extracted APK SHA-256: `a0c39bc21e81175b7c6fafed0316cd7b807e90ed8515ce257c69b4091088dff3`.
- The maintainer performed the requested focused device validation and reports that the previously reproduced HUN disappearance now appears fixed. The accepted shallow-pull handoff behavior remains satisfactory, so this defect is accepted for `dev` integration.

### Confirmed conclusion

The exact target's `expanded` boolean is too coarse to act as notification-shade ownership authority because HUN can assert `expanded=true` at `fraction=0.0` with `tracking=false`. The accepted Home eligibility contract for this callback is therefore motion-based: non-tracking with native fraction at/below the closed boundary remains Home; active tracking or positive shade motion transfers presentation away from Home. `expanded` remains diagnostic context only.

### 审查 / review

- **Ownership:** persistent Home presentation ownership and the single scene-visibility writer remain unchanged.
- **Lifecycle:** no HUN-specific owner, listener, delay, timer, or polling path was introduced.
- **Single writer:** scene eligibility remains the sole overlay-visibility authority.
- **Cleanup:** transient shade/HUN state does not destructively tear down a structurally valid Home owner.
- **Fail native:** missing motion facts still fail closed.
- **Performance:** constant-time predicate only; no additional wakeup/frame work.
- **Compatibility:** no new private member or reflection contract beyond the already-pinned callback.
- **Future extension:** the accepted lifetime gate is now a prerequisite for, not a substitute for, real Phase-2B shade / Control Center projection.

### Integration closure

PR #142 was squash-merged into `dev` as `2aa6833cfca69a59af5027a7855b7d8282dbade9`. Trusted Integration Build #1200 / run `36337302873` completed **successfully** and produced signed Canary artifact `10937951856` with artifact ZIP digest `sha256:26234811fa453ea3c7b4a57acbdea3886340ca61f1b043f90e0e4c8769eb0f20`. This promotes Build 413 from an accepted work-branch checkpoint to the current integrated `dev` runtime baseline.

### Outcome / next step

Build 413 is closed and accepted for the focused HUN + shallow-pull defect. Continue Phase 2B with actual notification-shade / Control Center Combined Status projection and intermediate motion, rather than adding more Home-lifetime patches.

## 2026-09-27 — Build 412 HUN device evidence and motion-semantic gate fix

**Type:** maintainer device evidence / root-cause confirmation / runtime fix candidate
**Input build:** Build 412 / `20260927-412`
**Work branch / PR:** `fix/home-hun-ownership` / Draft #142

### Problem execution flow

**Phenomenon and evidence**

The maintainer reproduced the notification/HUN disappearance on Build 412 and supplied detailed diagnostics. At 23:07:45.362 the target reports `expanded=true tracking=false fraction=0.0`; the current gate immediately records `homeEligible=false visible=false`. At 23:07:50.549 the same callback returns `expanded=false tracking=false fraction=0.0`, and Home returns `homeEligible=true visible=true`. The interval matches the visible HUN lifetime. The represented native Wi-Fi/mobile/Battery state remains independently managed; there is no participant-visible-state event or Home-owner teardown explaining the disappearance.

**Root cause / responsibility source**

The bug is in Combined Status scene interpretation, not suppression or renderer ownership. `notificationShadeAllowsHome()` equated native `expanded=true` with notification-shade ownership. On this HyperOS target, HUN sets `expanded=true` while the actual shade fraction remains exactly zero and tracking remains false. Therefore `expanded` is not a sufficient ownership discriminator.

**Repository / platform / native evidence**

- Build-412 detailed target evidence is authoritative for this device and callback.
- `SystemUi-Reference` verifies the exact target callback contract and Home carrier.
- Existing architecture requires transient scene visibility to be separate from persistent Home presentation ownership.
- The prior panel-leak evidence requires any real positive shade fraction to leave Home, even before a settled expanded state.

**Selected solution**

Use the native callback's motion semantics for notification-shade ownership:

- Home eligible: `tracking == false && fraction != null && fraction <= 0f`.
- Shade owns presentation: active tracking, any positive finite fraction, missing fraction, or missing tracking.
- Keep `expanded` in the diagnostic payload but do not use it as an independent Home-visibility authority.

This is not a HUN special case. It removes an over-broad semantic assumption and uses the native transition's actual motion facts. It also preserves the accepted slight-pull fix because positive fraction remains non-Home regardless of `expanded`.

### 审查 / review

- **Ownership:** one scene-visibility writer remains; no HUN owner is introduced.
- **Lifecycle:** existing event-driven shade callback only; no new listener or hook.
- **Single writer:** unchanged.
- **Cleanup:** unchanged; persistent Home owner is not torn down.
- **Fail-native:** missing/unknown motion facts fail closed rather than force Home visible.
- **Performance:** constant-time predicate change only; no polling/per-frame work.
- **Compatibility:** uses fields already delivered by the installed target callback; no new reflection contract.
- **Exception recovery:** unchanged.
- **Future extension:** separates motion ownership from coarse `expanded` hints, composing cleanly with later shade/keyguard/AOD policy.

### Tests

`SystemUiPanelTransitionSourceTest` now covers zero-fraction/non-tracking Home eligibility, active tracking, positive fraction, negative settled overshoot, and missing-value fail-closed behavior.

### First Fast result and correction

Fast Build #1179 / run `36328523953` reached the exact-target verification successfully, then failed at `:app:compileDebugKotlin`. The compiler error identified one missed production call site in `CombinedStatusHomeRenderSession.updatePanelTransition()`: the pure helper signature had been changed from `expanded/tracking` to `fraction/tracking`, but this caller still passed `expanded`.

This is an implementation-completeness failure, not evidence against the motion-semantic root cause. The PR was immediately returned to Draft. The missed call site is corrected to pass the same native `update.fraction` consumed by the source owner, and the executable identity is advanced to **Build 413 / `20260927-413`** (`versionCode=260927213`) because the runtime tree has changed.

### Draft validation after correction

- Draft Light #1182 / run `36328726733` failed only `git diff --check` because two newly added DEVLOG metadata lines carried trailing whitespace.
- PR #142 remained/returned Draft while that repository-only issue was corrected.
- Draft Light #1185 / run `36328796244` then **passed** on the corrected Build-413 branch state.
- No Android/Gradle/signing/APK work was required for this Draft documentation closure.

### Gate

Return PR #142 to Ready now that the Build-413 source/call-site correction and required records are complete. Run Fast CI at the exact final PR HEAD. If Fast passes, request one signed Canary because the fix changes the HUN/shade runtime visibility boundary and needs focused maintainer validation before integration.


## 2026-09-27 — Post-Build 412 HUN ownership source review

**Type:** root-cause triage / historical-assumption correction / documentation-only checkpoint
**APK build:** none; executable runtime remains Build 412 / `20260927-412`
**Work branch:** `fix/home-hun-ownership`

### Problem execution flow

**Phenomenon and evidence**

Maintainer device observation after the accepted Build-411 panel-owner correction: when a notification / heads-up notification (HUN) appears, Combined Status can temporarily disappear while the represented native Wi-Fi / mobile / Battery icons do not reappear. No Build-411/412 HUN diagnostic capture has been supplied yet; the available Build-409 diagnostic belongs to the earlier panel investigation and must not be repurposed as HUN evidence.

**Root cause / responsibility source**

Current-source review disproves the tentative explanation that the active Combined Status surface is a native status participant receiving `ICON / DOT / HIDDEN` visible-state callbacks:

- `CombinedStatusHomeRenderSession.Session.start()` adds the renderer to `MiuiNotificationStatusContainer.overlay`;
- `SystemUiHomePresentationOwner` owns represented-slot exclusion, reversible clip masks and end reservation for that Home overlay path;
- the legacy native-participant install/schedule methods still present in `CombinedStatusModule` have no active call site on the Build-412 path;
- Home startup/hot-reload explicitly calls `cleanupLegacyParticipant(...)` for the obsolete `combined_status` slot.

Therefore a participant-visible-state explanation is not an accepted root cause for Build 412.

The two evidence-bearing candidates that remain are:

1. the already-hooked `ShadeExpansionStateManager.onPanelExpansionChanged(...)` reports a non-Home semantic state during HUN and the existing scene gate intentionally hides the Home overlay while keeping the persistent Home owner/masks alive; or
2. Home eligibility/readiness remains valid, but the native Home host/ancestor presentation path temporarily prevents the overlay from being drawn.

**Repository / platform evidence**

- `CONTRIBUTING.md`: root-cause-first, one owner/writer, event-driven native state, no timing/polling workaround.
- `docs/architecture/scene-policy.md`: transient shade/Control Center ownership hides the overlay without tearing down a structurally valid Home presentation owner.
- `docs/architecture/layout-policy.md`: accepted Home carrier is `system_icon_area / MiuiNotificationStatusContainer -> ViewGroupOverlay`; permanent native participant is rejected as the default carrier.
- `SystemUI-Reference/findings/statusbar.md`: exact target identifies `system_icon_area` as the Home carrier/island right-container and records the historical native-participant route as superseded by the overlay architecture.
- Exact target profile verifies `ShadeExpansionStateManager.onPanelExpansionChanged(float, boolean, boolean)`.
- AOSP's documented `ShadeExpansionStateManager` contract states that `expanded` is independent of the numeric fraction, including the possibility of `expanded=true` at fraction 0. This is supporting contract context only; HyperOS HUN behavior still requires target-device evidence.

### Selected next evidence boundary

No runtime change is justified yet. Build 412 already emits the required bounded detailed diagnostics for the first branch of the decision:

- `panelTransition source=notification ... fraction=... expanded=... tracking=...`;
- Home shade eligibility / overlay visibility updates;
- Home presentation readiness and activation/deactivation/fail-native events.

Reproduce a single HUN with detailed diagnostics enabled on the existing signed Build-412 Canary. If the HUN drives the shade gate non-Home, review the target's intended HUN ownership before changing policy. If Home remains eligible and the overlay still disappears, add only the smallest host/ancestor diagnostic needed to resolve the second branch.

### 审查 / review

- **Ownership:** remains `MiuiNotificationStatusContainer.overlay` + persistent Home presentation owner; no second HUN owner added.
- **Lifecycle:** no new Hook/listener/frame callback in this checkpoint.
- **Single writer:** existing scene gate remains the only overlay-visibility writer; native represented-slot masks/reservation remain Home-owner scoped.
- **Cleanup:** unchanged; no new state to restore.
- **Fail-native:** unchanged; no forced always-visible path.
- **Performance:** zero runtime impact; avoids a speculative HUN hook or per-frame visibility probe.
- **Compatibility:** no new private SystemUI member is assumed.
- **Exception recovery:** existing fail-native and structural-readiness paths remain intact.
- **Future extension:** keeps HUN as a scene-ownership question that can later compose with shade/keyguard/AOD instead of a special-case patch.

### Historical correction

The prior tentative statement that the current Combined Status native participant likely receives `ICON / DOT / HIDDEN` and hides itself is rejected for Build 412 by current source/call-site review. Historical native-participant experiments remain valid evidence for those older builds but are not the active carrier architecture.

### Documentation CI

- Draft PR #142 correctly classified this documentation-only checkpoint as **Light** validation.
- Light Build #1174 / run `36327495300` failed only `git diff --check` because two newly added DEVLOG metadata lines carried trailing Markdown whitespace.
- The whitespace was removed without changing engineering content.
- Light Build #1175 / run `36327543097` then **passed**. Android/Gradle/signing/APK work remained skipped as required for documentation-only validation.
- Any record-only closure commit after this entry remains repository-memory maintenance and does not create a runtime Build or require recursive DEVLOG bookkeeping.

### Gate

This checkpoint creates no Build 413. Obtain focused Build-412 HUN diagnostics before any runtime mutation.


## Entry requirements

For each engineering checkpoint, record the problem/goal, observed evidence, analysis, root-cause status, references consulted, alternatives, implementation, review, CI/build identity, validation/device feedback, result, durable conclusions, residual risk, and future-design consequences as applicable.

Use explicit confidence labels when root cause is not proven:

- **Confirmed** — directly supported by source/runtime evidence.
- **High confidence** — evidence strongly supports the conclusion but one relevant uncertainty remains.
- **Hypothesis** — plausible and testable, not yet established.

Preserve failed hypotheses and append corrections. Do not rewrite history to hide an invalidated path. Historical entries before this log was introduced may be backfilled only from verifiable evidence.

## 2026-09-28 — Build 420 device result: Control Center accepted, shade edge rejected

**Type:** maintainer device evidence / root-cause update
**Build:** 420 / `20260928-420`
**PR:** #146 `feat/panel-projection`

### Maintainer result

- Control Center: no abnormal behavior observed. Keep the Build-420 projection architecture and handoff unchanged.
- Notification Shade: first/last-frame Combined Status disappearance remains; the Build-420 `tracking/fraction=0` correction is insufficient.

### Diagnostic evidence

The Build-420 Detailed report is healthy and confirms the new panel source is installed normally. During Notification-Shade transition:

- `homeRenderScene ... raw=1 surface=KEYGUARD ... visible=false` occurs before the notification fraction callback changes Home shade eligibility;
- immediately afterward the panel callback reports `fraction=1.0` and `homeEligible=false`;
- on return, scene state changes back to raw 0 / unlocked before the later `fraction=0.0` callback restores shade Home eligibility.

This proves Home visibility still has two asynchronous writers for the same transition boundary. The earlier Build-420 change fixed the panel predicate, but `SystemUiSceneStateSource` can still evict Home independently.

### Root cause

`SystemUiSceneStateSource` currently maps the Battery view's `mStatusBarState` directly:
- 0 -> unlocked;
- 1 -> Keyguard;
- 2 -> shade locked.

On this target, Battery state 1 can occur as part of Notification-Shade presentation even while the user is not on the actual Keyguard surface. Therefore the Battery field is a presentation-state input, not sufficient proof of global Keyguard ownership.

### Selected Build-421 correction

Use the platform `KeyguardManager.isKeyguardLocked` authority as the additional Keyguard proof:
- raw 1 + `true` -> real `KEYGUARD`, Home remains native-only;
- raw 1 + `false` -> `TRANSIENT_PANEL`, which does not independently veto Home;
- raw 1 + unavailable result -> `UNKNOWN`, fail-native;
- raw 2 remains `SHADE_LOCKED`, native-only;
- the read occurs only on the existing Battery scene event, with no polling, new Hook, reflection contract, or ViewTree traversal;
- no Control Center code, geometry, tint, or handoff changes.

This preserves the accepted Control Center result and addresses the actual remaining Notification-Shade writer conflict instead of adding another threshold or delay.

### Build-421 implementation review

- runtime source: `fb5bd499f01add37c12be8ee38668f0dc5270d52`;
- Draft validation #1291 passed on the exact Build-421 head;
- platform Keyguard state is read only from the existing `MiuiBatteryMeterView.updateState()` event;
- `TRANSIENT_PANEL` is an explicit scene classification rather than pretending raw state 1 is unlocked;
- only `UNLOCKED_STATUS_BAR` and `TRANSIENT_PANEL` allow Home; true Keyguard, Shade Locked, and Unknown remain native-only;
- Control Center projection implementation is unchanged from the device-accepted Build-420 path;
- no delay, threshold, timer, polling, extra SystemUI Hook, or geometry/tint writer is introduced.



---

## 2026-09-28 — Build 420: Notification-Shade edge continuity + Control Center projection

**Type:** Phase-2B runtime implementation / route correction
**Build:** 420 / `20260928-420`
**Exact runtime source after review:** `3635b52c3f3781db74a09ea6ab23a7e9dfcf40e5`
**Stable dev baseline remains:** Build 418 / `20260928-418`

### Problem / target correction

Maintainer clarification corrected an implicit Phase-2B assumption: on the pinned HyperOS target, the Notification Shade does **not** present the native status-icon row. Only Control Center does. Therefore a Notification-Shade Combined Status projection would be non-native behavior and must not be implemented.

One Notification-Shade bug remains in scope: the Home Combined Status can disappear for the first/last transition frame, matching the previously observed Control Center edge gap. The issue is not steady-state shade visibility; it is ownership timing at the exact zero-motion boundary.

### Build-419 evidence and route closure

Build 419 successfully unwrapped `NotificationHeaderExpandController.headerController` through Dagger `DoubleCheck.get()` and exposed `CombinedHeaderController`. The resulting runtime inventory showed:

- Notification side: `normal_shade_header`, notification clock, and shade containers;
- Control Center side: `controlCenterStatusBar`, `controlCenterStatusIcons`, `controlCenterSystemIcons`, and the dedicated Control Center header;
- no evidence that Notification Shade should own a status-icon projection.

Combined with the maintainer's native-behavior clarification, the Build-419 Notification-Shade target probe is retired. Its findings remain historical evidence; the probe source/test and active compatibility hook point are removed from Build 420.

### Root cause — Notification-Shade first/last frame

The existing Home eligibility predicate was:

`tracking == false && fraction <= 0`

At gesture start, HyperOS may report `tracking=true` while native fraction is still exactly `0`. That evicts Home before there is real shade motion. The inverse can occur on return, keeping Home hidden through the final zero-motion frame.

Build 420 changes the ownership fact to the native motion boundary:

- `fraction <= 0` -> Home remains eligible;
- `fraction > 0` -> shade owns the transition;
- `tracking` / `expanded` remain diagnostic context and do not independently hide Home.

This does not make Combined Status visible in steady Notification Shade; once positive shade motion begins, Home yields as before.

### Control Center projection architecture

Build 420 does not invent a second animation path. It promotes the already-evidenced `ControlCenterHeaderExpandController.realSystemIcons` reference from diagnostic use to runtime carrier resolution.

The Control Center projection session:

- accepts only `MiuiStatusBatteryContainer`;
- additionally requires object identity with the exact battery container already owned by `SystemUiHomePresentationOwner`;
- resolves the existing native status-icon group, battery view, and `battery_icon_container`;
- reuses `CombinedStatusHomeLayoutResolver`, the existing render model, visual settings, semantic battery colors, and renderer;
- reads monochrome tint from the Control Center carrier's own visible non-represented status peers;
- adds only a `ViewGroupOverlay` child to the native transformed carrier;
- performs no custom `translationX/Y`, no progress interpolation, no timer, polling, frame follower, or peer alpha/visibility writer.

Because `realSystemIcons` is identity-gated to the existing Home presentation container, Wi-Fi/mobile/battery masking and ignored-slot ownership remain single-writer under `SystemUiHomePresentationOwner`. Build 420 adds no second suppression owner.

### Handoff sequencing

Control Center open:
1. native `onVisibleChanged(true)` completes;
2. resolve and attach the projected surface while Home remains visible;
3. require model + local tint + layout + attached host + carrier identity;
4. make Control Center projection visible;
5. only then mark Home Control Center eligibility false.

Control Center close:
1. restore Home eligibility first;
2. hide/remove the Control Center projection second.

This ordering prevents an intentionally empty ownership frame at either edge.

### Hot Reload review

Post-implementation review found HomeRenderSession still seeded its Control Center gate directly from `SystemUiPanelTransitionSource.currentControlCenterHomeEligibility()`. That could bypass the new readiness coordinator during Hot Reload. The final Build-420 runtime correction makes the Control Center Home gate coordinator-owned from initialization (`true` until the coordinator explicitly yields).

The panel source still retains Control Center visibility as diagnostic/transfer context, but it is no longer a second Home visibility writer.

### Review / 审查

- **ownership:** Home and Control Center visual ownership are coordinated explicitly; native masking remains owned only by HomePresentationOwner.
- **lifecycle:** projection is attached only for native Control Center visibility and cleaned on close/Hot Reload.
- **single writer:** no duplicate suppression, translation, alpha, or visibility writer is introduced.
- **cleanup:** overlay/listeners are removed; Home is restored before projection teardown.
- **fail-native:** unresolved/wrong-identity carrier keeps Home visible and skips Control Center projection.
- **performance:** event-driven only; no frame loop/polling/retry.
- **compatibility:** runtime carrier comes from the already-verified Control Center callback owner; wrong type/identity fails closed.
- **exception recovery:** partial projection readiness cannot evict Home.
- **future extension:** surface adapter shares global render policy/model without creating a second network/battery state machine.

### Validation state

- core projection carrier commit `960131626992e9a149f512a38b6d75e201e61b9e`: Draft Light #1281 passed;
- full Build-420 wiring commit `ccff32bc629b52487996c3ccbfc1525d4b41ed8b`: Draft Light #1282 passed;
- final runtime review correction `3635b52c3f3781db74a09ea6ab23a7e9dfcf40e5`: pending final Draft validation at time of this record;
- runtime remains frozen until that check passes and the checkpoint advances to Ready/Full.

### Device gate

One combined Build-420 device package will validate both requested behaviors:

1. Notification Shade: first and last transition frames no longer show the one-frame Combined Status disappearance; steady shade remains without status icons.
2. Control Center: Combined Status appears in the native status-icon surface, follows native motion without duplicate/blank edge frames, and returns cleanly to Home.

No separate Notification-Shade projection test is required.

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


---

## 2026-09-27 — Repository consistency review and stepwise documentation synchronization

**Type:** documentation / governance consistency review
**Runtime build:** none
**Display line:** 0.0.2
**Runtime impact:** none

### Problem / objective

The Phase-2A architecture evidence advanced faster than several current-facing repository surfaces. Some files still described Build 394 as blocked, the permanent participant/occupancy-handoff route as an active candidate, or PR #100 as a Build-378 native-slot/panel-geometry change.

A second process gap was identified: the contribution rules required CURRENT/DEVLOG/ROADMAP maintenance, but did not state clearly enough that repository memory must be synchronized **after each meaningful engineering step that changes the next action**, rather than only at the end of a long task.

### Problem execution flow

1. Re-read the latest `CONTRIBUTING.md`, `CURRENT.md`, `ROADMAP.md`, relevant recent `DEVLOG.md`, architecture/reference documents, README/CHANGELOG, Issue template, build/version metadata and PR #100.
2. Separate true historical Build/CI records from current/future/net-state descriptions.
3. Cross-check the current Phase-2A decision against the Build-394 gate, 0.0.2 development line and 1.0.0 first-release target.
4. Correct only current-facing surfaces; preserve historical DEVLOG/Build facts.
5. Update contribution governance so each meaningful step explicitly maps to the repository documents that must be synchronized before continuing.
6. Update PR #100 metadata to the branch's current objective.
7. Re-run an architecture/documentation **review** for ownership, lifecycle, single-writer, cleanup, fail-native, compatibility, future-phase boundaries and historical integrity.

### Repository updates

- `CONTRIBUTING.md` now requires stepwise repository-memory synchronization and explicitly maps:
  - investigation/evidence -> CURRENT and, when durable, DEVLOG;
  - architecture/ownership changes -> architecture docs + CURRENT/ROADMAP + DEVLOG;
  - reusable evidence -> reference library;
  - runtime checkpoint -> CURRENT + DEVLOG;
  - CI/device result -> immediate CURRENT + DEVLOG correction;
  - phase/version/release changes -> ROADMAP/VERSIONING plus affected public/net-state docs;
  - durable net behavior -> CHANGELOG;
  - changed PR objective/acceptance boundary -> PR title/body.
- Mechanical sub-steps that do not change engineering meaning do not require their own log entry.
- Current architecture/reference/layout documents now describe the Build-394 pre-runtime gate as open for the pinned target and keep the old participant route as superseded historical evidence.
- `CHANGELOG.md` no longer presents superseded participant/occupancy-handoff mechanics as the intended current net state; durable capabilities are phrased independently of that rejected carrier.
- the bug-report version example follows the current 0.0.2 development line.
- PR #100 is now titled `refactor: establish 0.0.2 Home carrier architecture` and its body describes the Phase-2A carrier cutover, Build-394 validation boundary, historical evidence, and explicit Phase-2B/Keyguard/AOD/UI exclusions.

### Review

- **Historical-integrity review:** previous DEVLOG Build records remain unchanged. Later conclusions are appended rather than retroactively rewriting what was actually implemented or believed.
- **Ownership review:** documentation consistently leaves native peer layout/island motion with SystemUI and gives Combined Status only the selected overlay composition plus scoped restoration state.
- **Lifecycle/cleanup review:** the current path remains HostSession-scoped and requires exact slot/mask restoration on disable, replacement, Hot Reload and partial activation failure.
- **Single-writer review:** current-facing documentation no longer recommends concurrent permanent-participant and existing-host carriers.
- **Fail-native review:** unmatched/incomplete contracts continue to restore or retain native presentation.
- **Phase-boundary review:** Build 394 is Phase 2A only; Home -> shade / Control Center remains Phase 2B, followed by Keyguard/AOD and later user-facing sizing controls.
- **Version review:** current development line remains 0.0.2; the first formal release target remains 1.0.0; historical 0.0.1 Build records remain historical facts.
- **Validation review:** documentation/governance only; no APK/runtime change, Build ID change, Canary or device validation is required.

### Outcome / next step

Repository-facing development state is aligned around the open Build-394 gate. The next engineering step is the first bounded 0.0.2 runtime implementation of the Phase-2A Home carrier cutover. After that implementation step, CURRENT and DEVLOG must be synchronized immediately before CI/device validation proceeds.

---

## 2026-09-27 — Exact Home island carrier contract closed; Build 394 authorized

**Type:** exact-target architecture closure / runtime gate
**Runtime build:** none yet
**Display line:** 0.0.2
**Runtime impact:** none

### Exact method-body findings

- `StatusBarIslandControllerImpl.translationFlow` stores the signed `status_bar_island_translation` resource endpoint and is refreshed by configuration/layout-direction/max-bounds changes; it is not per-frame animation progress.
- `IslandMonitor.RealContainerIslandMonitor.updateContainerSize(...)` writes `statusContainerSpace` from island rectangle + live container location + padding + layout direction + supported native endpoint; fake containers consume it as `islandWidth` and request native layout. It is a layout-space/avoidance contract, not motion progress.
- `HomeStatusBarViewBinderImpl` passes `R.id.system_icon_area` as `IslandStretchAnimation.rightContainer`.
- `IslandStretchAnimation` uses SystemUI's `MiuiStatusBarIconAnimatorController` ISLAND_SHOW/HIDE `AnimConfig` to animate the right container to the controller endpoint; direct `setTranslationX(...)` is the non-animated path.
- exact `status_bar.xml` defines `R.id.system_icon_area` as `MiuiNotificationStatusContainer`; its `system_icons` child is `MiuiStatusBatteryContainer`.

### Root-cause / architecture consequence

The already-proven Home overlay candidate is attached to the exact View that SystemUI itself translates for island avoidance. Therefore native carrier transformation, not a duplicate Combined Status motion source, is the correct Phase-2A contract.

The overlay can inherit `system_icon_area` motion while remaining independent from the battery child's separate `alpha`/hide behavior. This directly satisfies the product requirement to retain network information during charging-island presentation.

### Final review before runtime implementation

- **Ownership:** SystemUI owns `system_icon_area.translationX` and animation configuration; Combined Status owns only overlay content, ResolvedLayout and reversible suppression tokens.
- **Lifecycle:** the overlay, ignored-slot token and clip snapshots belong to one `MiuiNotificationStatusContainer` HostSession.
- **Single writer:** no Combined Status island translation writer is needed. Existing custom-participant motion/occupancy ownership must be inactive under the new carrier.
- **Cleanup:** session teardown restores owned ignored-slot entries and exact clip states, removes overlay content and drops host references.
- **Fail native:** native visual suppression starts only after the complete new Home session is ready; partial activation rolls back to native.
- **Performance:** the design eliminates production pre-draw island following and duplicate animation; native View transform carries the overlay for free.
- **Compatibility:** this closure is scoped to the pinned exact SystemUI fingerprint and must fail native when the host/class/member contract is unavailable.
- **Future extension:** Phase 2B can project from the stable Home source bounds without changing Home carrier ownership.

### Decision

The pre-Build-394 static architecture gate is satisfied. Build 394 is authorized as the first 0.0.2 runtime checkpoint, scoped only to Home carrier ownership cutover + ResolvedLayout + represented-slot exclusion + reversible clip masking. Phase 2B, Keyguard/AOD and user-facing sizing controls remain out of scope.
---

## 2026-09-27 — Build 394 source checkpoint: Home carrier ownership cutover

**Type:** runtime architecture checkpoint
**Display version:** 0.0.2
**Build:** 394 / 20260927-394
**Validation:** Fast CI pending; device validation pending

### Problem / objective

Exact-target evidence now proves the Home overlay host, ignored-slot measure/layout contract, reversible clip-mask candidate and native island-motion inheritance. Build 394 implements that architecture without reviving the permanent custom participant used by Builds 386-393.

### Problem execution flow

1. Re-read the latest CONTRIBUTING, CURRENT, ROADMAP and relevant DEVLOG/architecture/reference evidence.
2. Re-verify the retained exact SystemUI APK with the maintainer-provided JADX 1.5.6.
3. Confirm translationFlow is a target offset rather than frame progress and that IslandStretchAnimation animates system_icon_area / MiuiNotificationStatusContainer.
4. Review current module wiring for ownership, lifecycle, single-writer, cleanup, fail-native, performance, compatibility and future extension.
5. Cut over only the Home presentation carrier; leave Phase 2B, Keyguard/AOD and user-facing sizing controls unchanged.

### Implementation

- Added SystemUiHomePresentationOwner with target-verified MiuiStatusIconContainer onMeasure/onLayout hooks.
- represented slots are temporary owned ignoredSlots entries scoped to the native call and restored in finally;
- Wi-Fi/mobile/stacked-mobile/airplane/no-SIM roots plus MiuiBatteryMeterView are masked with reversible clipBounds state;
- CombinedStatusHomeRenderSession now publishes model+tint+layout+scene+feature readiness before native replacement may activate;
- CombinedStatusModule no longer installs/activates the legacy native Combined Status participant or battery suppression path for Home;
- the old network suppression owner runs in observation-only mode so its suppression writers remain disabled while no-SIM/status-icon presentation evidence is retained;
- pre-0.0.2 Hot Reload migration removes any legacy participant then requests one SystemUI restart instead of guessing restoration of old mask state;
- deterministic unit coverage verifies owned ignored-slot restoration and exceptional cleanup.

### Review

- **Ownership:** HyperOS owns peer layout, scene state and island animation; Combined Status owns overlay drawing, temporary ignored-slot tokens and clip masks.
- **Lifecycle:** presentation state is Home-host scoped and restored on readiness loss, host detach/replacement, feature disable and Hot Reload teardown.
- **Single writer:** native translation/alpha/visibility/geometry writers are not replaced; the superseded participant/suppression carrier is inactive.
- **Cleanup:** ignored entries restore in finally; clip state restores only while the current value still equals the module-applied empty clip; relayout is requested after deactivation.
- **Fail native:** native visuals remain authoritative until renderer model, tint and layout are ready and the exact target owner can activate.
- **Performance:** no polling, frame follower or duplicate animation was added; the two layout hooks do bounded list work and clip refresh occurs after native layout.
- **Compatibility:** missing target class/method/field/group contracts leave native SystemUI active.
- **Future extension:** Phase 2B can use stable Home source bounds without reopening Home carrier ownership.

### Validation boundary

Local Gradle execution is unavailable in the current execution environment because external Git/DNS access is blocked. The existing Fast work-branch CI is therefore the compile/unit/Debug-APK gate. A successful CI run will not count as runtime proof; normal Home, feature disable/enable, charging/Super-Island enter/steady/exit, cold start while charging, cleanup/fail-native and subsequent same-architecture Hot Reload still require focused device validation.

### CI / Canary result

- Source commit: `96fbb97e5d08280fee3c93e8091a61538b3bffcd`.
- Fast PR Build workflow #1044 (`36270093725`): **success**.
- Signed Work Branch Canary #303 (`36270307814`): **success**.
- Unit tests and Canary assembly passed.
- pinned HyperOS target-profile verification passed.
- Modern Xposed metadata/API/scope/Hot Reload metadata verification passed.
- Haple signing certificate verification passed.
- Canary non-debuggable verification passed.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-394-canary.apk`.
- Device validation remains pending; Build 394 is not promoted to `dev`.
---

## 2026-09-27 — Build 394 device rejection; Build 395 carrier reservation correction

**Type:** device contradiction / root-cause correction / runtime checkpoint
**Display version:** 0.0.2
**Rejected build:** 394 / 20260927-394
**Next build:** 395 / 20260927-395
**Validation:** CI pending; device validation pending

### Device feedback

Build 394 does not pass the Phase-2A device gate:
- charging-island entry can move the Combined Status visual left and then immediately right;
- charging state does not retain a stable occupied end-side region;
- cold SystemUI start while already charging still has a larger neighbor gap than normal Home;
- partial shade pull / final held return still shows the Home representation, but this is classified as deferred Phase-2B projection behavior rather than the Phase-2A blocker.

The attached 394 diagnostic reports a healthy Home cutover, represented-slot masking and transformed-host inheritance, but it is a Hot Reload session and the renderer snapshot is `charging=false`. It does not capture the failing charging transition/cold-start geometry.

### Problem execution flow

1. Treat the device contradiction as invalidating the assumption that transformed-host inheritance alone closes the island carrier problem.
2. Re-read the exact target `MiuiStatusBatteryContainer.onMeasure/onLayout/setIsHideBattery` and `MiuiBatteryMeterView.updateIslandChanged` method bodies.
3. Separate native motion ownership from the end-side occupancy contract.
4. Keep the verified native motion owner and remove the battery descendant from Combined Status local-position ownership.
5. Introduce the narrowest reversible layout reservation; do not restore the superseded custom participant or fixed compensation chain.

### Exact root cause

On the pinned target:
- island battery addition sets `mIsHideBattery=true`;
- `MiuiStatusBatteryContainer.onLayout` then allows `MiuiStatusIconContainer` to extend into the battery end-side region;
- the Battery View separately animates to its own width and fades/hides;
- Build 394 overlay is draw-only and still resolves its local rectangle from that Battery descendant.

This creates two independent geometry changes around one overlay: native peer occupancy is released while the overlay's local anchor is tied to a child with its own island presentation lifecycle.

### Build 395 implementation

- Home overlay slot bounds now resolve from the stable `MiuiNotificationStatusContainer` end edge plus measured native battery carrier width instead of `offsetDescendantRectToMyCoords(battery,...)`.
- The shared `CombinedStatusLayoutPolicy` is now consumed for this Home end-anchor/slot calculation.
- `SystemUiHomePresentationOwner` verifies the exact target `MiuiStatusBatteryContainer.mIsHideBattery` and `onLayout` contracts and adds one scoped layout hook.
- When the replacement session is active and native hide is true, only that native `onLayout` invocation temporarily observes hide=false; the exact native value is restored in `finally`.
- The module still does not block `setIsHideBattery`, force Battery visibility, or write Battery translation/alpha.
- Parent layout is requested on activation/deactivation so native peer layout recomputes through the owned reservation boundary.
- Unit coverage records the reservation policy while existing ignored-slot restoration tests remain unchanged.

### Review

- **Ownership:** SystemUI still owns island motion and Battery visual state. Combined Status newly owns only the replacement's end-side carrier reservation during the native parent layout call.
- **Lifecycle:** reservation exists only while the Home presentation session is active; the native field is restored before the hook returns.
- **Single writer:** no competing translation/alpha/visibility writer is added. The native layout method remains the geometry writer; Combined Status temporarily supplies the carrier-preservation input.
- **Cleanup:** native hide state restores in `finally`; session stop requests parent relayout and restores clip masks.
- **Fail native:** missing field/method/container contracts prevent activation; reservation apply/restore failures trigger native fallback.
- **Performance:** one existing-layout-event hook, no polling, no pre-draw follower, no per-frame diagnostics.
- **Compatibility:** exact-target only; no same-version-public-APK inference.
- **Future extension:** Phase 2B still owns Home -> shade / Control Center projection; this checkpoint does not add transition formulas.

### Acceptance gate

Build 395 requires focused device validation of stable Home, island enter/steady/exit, cold start while charging, feature disable/enable and same-architecture Hot Reload before any promotion.

---

## 2026-09-27 — Build 394 device rejection: charging geometry

**Type:** device feedback / root-cause correction
**Display version:** 0.0.2
**Build:** 394 / 20260927-394
**Promotion:** rejected; remains work-branch evidence

### Device feedback

- brief shade pull-down and final held return still show the Home Combined Status visual;
- charging/Super-Island entry shows a visible two-step/twitch motion;
- cold SystemUI start while already charging shows a larger Combined Status-to-neighbor gap than the non-charging state.

### Evidence

The Build-394 diagnostic reports a healthy runtime, `homePresentation=combined`, carrier `MiuiNotificationStatusContainer.overlay`, `representedSlots=5`, `maskedViews=6`, and `islandMotion=inherited-from-system_icon_area`, with zero Combined Status native translation/alpha/visibility writes.

Static source review then found a mismatch between the documented architecture and runtime wiring: `CombinedStatusLayoutPolicy.resolve()` is not called by the Home renderer path. `CombinedStatusHomeRenderSession` still derives the overlay rectangle directly from `MiuiBatteryMeterView` descendant bounds.

Exact target JADX confirms `MiuiBatteryMeterView.updateIslandChanged(...)` drives `MiuiStatusBatteryContainer.setIsHideBattery(...)`; the container's layout then allows `MiuiStatusIconContainer` to expand into the battery region while the battery child independently animates translation/alpha/scale. That makes the battery descendant unsuitable as the stable Combined Status layout anchor during island presentation.

### Problem execution flow

1. classify shade-held visibility separately as Phase 2B projection/handoff;
2. keep Phase 2A focused on charging steady geometry and island enter/exit;
3. remove Battery-child geometry from the steady Home overlay anchor;
4. make the shared resolved-layout contract the actual runtime geometry source;
5. establish a reversible end-side reservation only when native battery layout releases its region;
6. preserve native `mIsHideBattery`, native host Folme translation, and fail-native cleanup.

### Review boundary for Build 395

- **Ownership:** SystemUI keeps battery hide and island translation; Combined Status owns only overlay bounds plus its narrow reversible end reservation.
- **Lifecycle:** reservation state is Home HostSession-scoped and restored on every teardown/failure path.
- **Single writer:** no battery translation/alpha/visibility writes; any reservation property requires an exact writer audit before use.
- **Cleanup:** restore the exact pre-session value only when the live property still matches the module-applied value.
- **Fail native:** if reservation capability or resolved geometry is unavailable, restore native visuals rather than render an overlapping overlay.
- **Performance:** event/layout-driven only; no permanent pre-draw follower.
- **Compatibility:** exact target only until the new reservation contract is re-proven elsewhere.
- **Future extension:** width/gap inputs must flow through resolved layout rather than scene-specific offsets.

---

## 2026-09-27 — Build 395 pre-CI rejection; Build 396 end reservation

**Type:** architecture review / source correction
**Display version:** 0.0.2
**Rejected source checkpoint:** Build 395
**Next checkpoint:** Build 396 / 20260927-396
**Source commit:** `f4c20db514d6767eb027d38cc5b1a800f58a130c`
**Validation:** CI pending; device validation pending

### Review finding

Build 395 temporarily changed `MiuiStatusBatteryContainer.mIsHideBattery` to false only while native `onLayout(...)` executed, restoring it in `finally`. Although this preserved carrier width without writing translation/alpha/visibility, it still changed a HyperOS scene/layout input and matched an approach already rejected by the ROADMAP.

### Exact-target alternative

- `system_icons.xml` authors no padding on `MiuiStatusIconContainer`;
- exact `onMeasure(...)` includes horizontal padding in content width;
- exact `onLayout(...)` uses `getPaddingEnd()` as the end-side placement boundary;
- directed writer audit of the decompiled `com.android.systemui.statusbar` source set found no competing status-icon padding writer.

### Build 396 implementation

- native `mIsHideBattery` remains unchanged and read-only;
- the native battery-hide setter is observed only as a low-frequency event source;
- while native battery layout has released its region, `resolved.requestedSlotWidthPx` is reserved through `statusIcons.paddingEnd`;
- existing relative padding is snapshotted per Home HostSession and restored only if the live value still equals the module-applied value;
- unexpected padding-writer conflict fails native;
- renderer slot bounds and reservation width share `CombinedStatusHomeLayoutResolver`;
- the 395 host-end anchor correction is retained, so Battery descendant translation/visibility no longer defines Combined Status local position.

### Review

- **Ownership:** HyperOS retains battery hide, peer layout behavior and island motion; Combined Status owns only its explicit replacement-space reservation.
- **Lifecycle:** reservation is Home HostSession-scoped and event-driven from native hide-state changes.
- **Single writer:** exact-target audit finds no competing status-icon padding writer; runtime conflict detection remains.
- **Cleanup:** restore only the exact module-applied relative padding state.
- **Fail native:** missing hide/layout/host contracts or writer conflicts restore native presentation.
- **Performance:** one low-frequency native hide-state hook; no polling or frame follower.
- **Compatibility:** exact target fingerprint only.
- **Future extension:** requested width continues through shared resolved-layout input rather than scene offsets.

### Acceptance gate

Build 396 must validate normal Home spacing, charging-island enter/steady/exit, cold SystemUI start while already charging, feature disable/enable and same-architecture Hot Reload. Partial shade-held visibility remains Phase 2B.

### Build 395 CI / Canary result

- Source commit: `632812ba4bbaedca3d42b26c937537479c2a6626`.
- Fast PR Build workflow #1045 (`36271668949`): **success**.
- Signed Work Branch Canary #304 (`36271854940`): **success**.
- Unit tests and Canary assembly passed.
- pinned HyperOS target-profile verification passed.
- Modern Xposed metadata/API/scope/Hot Reload metadata verification passed.
- Haple signing certificate verification passed.
- Canary non-debuggable verification passed.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-395-canary.apk`.
- Device validation remains pending; Build 395 is not promoted to `dev`.

---

## 2026-09-27 — Build 396 pre-device rejection; Build 397 stable charging boundary

**Type:** device clarification / exact-resource proof / runtime checkpoint
**Display version:** 0.0.2
**Rejected checkpoint:** Build 396 / 20260927-396
**Next build:** 397 / 20260927-397
**Validation:** CI pending; device validation pending

### Device clarification

The charging spacing defect is present after restarting SystemUI while already plugged in even when no subsequent user interaction occurs. This classifies the large neighbor gap as a steady charging-layout defect in addition to the separately observed island transition twitch.

### Problem execution flow

1. Separate steady charging geometry from island transition motion.
2. Re-check the exact Battery width resources and live 105/135 runtime evidence.
3. Reject live `MiuiBatteryMeterView.measuredWidth` as the replacement's width authority.
4. Preserve the verified `system_icon_area` motion carrier.
5. Derive one end boundary from the shared requested slot width while treating live Battery width and hide state as native environment inputs.
6. Keep the correction host-scoped, reversible, event-driven and conflict-detected.

### Exact target evidence

The retained exact SystemUI artifact exposes:
- `battery_meter_width = 28dp`;
- `hollow_battery_meter_charge_width = 8dp`.

On the current device these map to the already observed ~105px stable base Battery width and ~30px charging addition; the live charging Battery therefore reaches ~135px. The extra charging width is native presentation content, not Combined Status replacement-slot intent.

### Build 397 implementation

- Added a one-shot SystemUI Home carrier metric resolver for runtime `battery_meter_width`.
- `CombinedStatusHomeLayoutResolver` now receives the stable base carrier width instead of live Battery measured width.
- The Home overlay is therefore host-end anchored to the same resolved slot in charging and non-charging states.
- `EndReservationPolicy` now derives a signed status-icon end adjustment:
  - native Battery present: `requestedSlotWidth - actualBatteryWidth`;
  - native Battery released: `requestedSlotWidth`.
- This produces no adjustment for 105/105 normal Home, reclaims only the charging addition for 135/105 steady charging, and reserves the complete replacement slot when HyperOS releases Battery layout.
- Battery layout changes and `setIsHideBattery` remain low-frequency synchronization triggers; no polling or frame follower is introduced.
- The exact pre-session status-icon padding remains the restoration token and unexpected competing writers still fail native.

### Review

- **Ownership:** HyperOS owns Battery measured width, charging presentation, hide state and island motion. Combined Status owns only its resolved replacement slot and the proven status-icon end-boundary input.
- **Lifecycle:** base width is resolved per Home session; live-width changes are observed through the Battery View's existing layout lifecycle; cleanup removes the listener and restores exact padding/clip state.
- **Single writer:** no Battery width/translation/alpha/visibility write is added. The status-icon padding property has one verified module writer with runtime conflict detection.
- **Cleanup:** teardown restores only the exact module-applied padding and clip snapshots and requests native relayout.
- **Fail native:** missing `battery_meter_width`, unavailable live Battery width, missing hide state, or padding conflict restores native presentation.
- **Performance:** one resource lookup per session plus low-frequency native layout/hide events; no polling or per-frame diagnostics.
- **Compatibility:** resource/class/member claims remain scoped to the pinned exact SystemUI fingerprint.
- **Future extension:** future user size/gap changes alter requested slot width through `ResolvedLayout`; the same signed boundary formula remains valid without charging-specific constants.

### Device gate

Build 397 must first prove normal Home vs plugged-in cold-start steady spacing with no user interaction. Only after that passes should island enter/steady/exit, feature disable/enable and same-architecture Hot Reload be evaluated.

---

## 2026-09-27 — Build 397 statically superseded; Build 398 binds the live battery-body carrier

**Type:** root-cause refinement / higher-authority native contract / runtime checkpoint
**Display version:** 0.0.2
**Last device-rejected build:** 395 / 20260927-395
**Superseded without device validation:** 396, 397
**Next build:** 398 / 20260927-398
**Validation:** CI pending; device validation pending

### Problem execution flow

1. Accept the device clarification that plugged-in cold start has excessive spacing without user interaction.
2. Use the Build-395 geometry samples to separate the 135 px Battery presentation from the stable 105 px battery body.
3. Inspect exact `battery_digital_view.xml`: `battery_icon_container` is the battery-body container; `battery_charge_out_image` is a sibling charging-only View.
4. Reject Build 396 before device testing because it still used live Battery root width as replacement width.
5. Review Build 397 before CI: its signed reservation math is correct, but `battery_meter_width` is still a resource proxy for a fact exposed by a stronger live native View.
6. Move both renderer geometry and reservation intent to the real `battery_icon_container` instance.

### Build 398 implementation

- `SystemUiHomeCarrierMetrics` now resolves the concrete `battery_icon_container` under the active native Battery View and reads its live layout/measured width.
- Home renderer bounds use that carrier directly; charging-only Battery root expansion cannot resize/center-shift the overlay.
- Home presentation reservation stores the same carrier identity and re-resolves its live width.
- Full Battery root width remains observation-only native occupancy input.
- Reservation policy stays signed and native-derived: requested stable carrier minus native already-reserved presentation width, or the full stable carrier when HyperOS releases Battery layout.
- Battery-root width changes and carrier-width changes are event-driven synchronization points only.
- Diagnostics identify `battery_icon_container` as carrier authority and declare the owned status-icon layout reservation separately from native motion/alpha/visibility ownership.

### Review

- **Ownership:** SystemUI owns Battery composition and island animation. Combined Status owns overlay drawing and one reversible statusIcons end-boundary reservation.
- **Lifecycle:** Battery root, core carrier, padding token, clips and listeners are scoped to one Home HostSession.
- **Single writer:** Combined Status writes only its statusIcons relative padding reservation; target source has no competing runtime padding writer and runtime conflict detection remains active.
- **Cleanup:** exact padding/clip state is restored and both Battery/core layout listeners are removed.
- **Fail native:** missing `battery_icon_container`, invalid live widths, stale carrier identity, or writer conflict falls back to native.
- **Performance:** layout-event driven; no polling, no production pre-draw follower, no custom island animator.
- **Compatibility:** the carrier ID/layout relationship is exact-target evidence and remains fingerprint-gated.
- **Future extension:** later size/gap settings may alter requested replacement width without confusing charging-only Battery presentation with carrier capacity.

### Acceptance gate

After Fast CI and signed Canary pass, device validation must begin with charger-connected SystemUI cold start and **no interaction**. Normal neighbor spacing must match non-charging Home before testing island enter/steady/exit, post-island charging steady state, feature disable/enable and same-build Hot Reload.

### Build 397 CI / Canary result

- Source commit: `90c7337443af771435fe2ec0b56837402b44517f`.
- Fast PR Build workflow #1048 (`36273364006`): **success**.
- Signed Work Branch Canary #307 (`36273538705`): core validation and artifact upload **success**.
- Unit tests and Canary assembly passed.
- pinned HyperOS target-profile verification passed.
- Modern Xposed metadata/API/scope/Hot Reload metadata verification passed.
- Haple signing certificate verification passed.
- Canary non-debuggable verification passed.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-397-canary.apk`.
- Device validation remains pending; Build 397 is not promoted to `dev`.


### Build 398 CI / Canary result

- Source commit: `e0cf4ffa523480f2221f710df0563e5346fc0914`.
- Fast PR Build workflow #1049 (`36273586837`): **success**.
- Signed Work Branch Canary #308 (`36273770652`): **success** through build, metadata, certificate, non-debuggable and artifact-upload gates.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-398-canary.apk`.
- APK SHA-256: `e856a3216d60cb4ccc8b239a91102162e83369951c8409a0c77c29133ae97131`.
- Device validation remains pending; Build 398 is not promoted to `dev`.

---

## 2026-09-27 — Build 397 accepted charging carrier; Build 399 battery-intensity checkpoint

**Type:** device acceptance correction / visual root-cause analysis / runtime checkpoint
**Display version:** 0.0.2
**Accepted device checkpoint:** Build 397 / 20260927-397
**Structural refinement carried forward:** Build 398 / 20260927-398
**Next build:** 399 / 20260927-399
**Validation:** CI pending; focused visual device validation pending

### Device correction

The previous repository statement that Build 397 was superseded without device validation was incorrect. Build 397 was installed and tested on the pinned target. User feedback confirms:
- charger-connected SystemUI cold start with no interaction has normal neighbor spacing;
- the previous charging/Super-Island left-then-right twitch is gone;
- charging steady-state placement is normal.

A brief native Battery flash during same-architecture Hot Reload was also reported, but the user clarified that this behavior existed from the earliest implementation. It is therefore not classified as a Build-397 regression and remains a separate Hot Reload handoff-polish item.

### New visual issue

Two same-device screenshots were supplied for color/intensity review. Approximate JPEG-screen sampling shows:
- adjacent native status icons: high-coverage grayscale roughly 58-64;
- Combined Status center Wi-Fi: roughly 59-60;
- active mobile dots: roughly 58-61;
- battery ring: roughly 50 in the same normal gray scene.

The screenshots therefore support a battery-ring intensity mismatch rather than a center/mobile tint-authority mismatch. The charging-green screenshot likewise shows closely matching core green values but a heavier overall ring coverage.

### Root cause

**High confidence / source-confirmed compositing difference:** `drawBattery()` paints the entire ring once with semantic alpha 48, then paints the active battery arc again with semantic alpha 255 over the same pixels. Center/native resources and mobile dots do not use this two-layer steady-state coverage. The overlap increases effective edge coverage and makes the battery ring read darker/heavier even when the resolved tint is identical.

### Build 399 implementation

- Keep `CombinedStatusColorPolicy`, tint source, native-center mask normalization and center/mobile paint paths unchanged.
- Partition battery rendering into:
  - full-strength active segment;
  - dim inactive remainder;
  - no full-length dim underlay below the active segment.
- Preserve start angle, maximum sweep, degrees-per-percent, stroke geometry and semantic alpha values.
- Add a JVM-pure `CombinedStatusBatteryArcPolicy` and tests proving 0/50/100% partitioning, clamping and total sweep preservation.
- No per-glyph brightness multiplier, screenshot-derived alpha correction, native geometry change or animation change is introduced.

### Review boundary

- **Ownership:** unchanged; SystemUI remains tint/scene/motion authority.
- **Lifecycle:** no new listeners, hooks or runtime owner.
- **Single writer:** unchanged painter-only output.
- **Cleanup/fail-native:** unaffected.
- **Performance:** one constant-time arc partition per draw; no allocation-heavy probing or polling.
- **Compatibility:** no new target member/resource dependency.
- **Future extension:** visual-weight/user-size work can remain separate from color/intensity semantics.

---

## 2026-09-27 — Build 400 HyperOS battery semantic color source

**Type:** bounded feature / native semantic-state reuse
**Branch:** `feat/battery-semantic-colors`
**Display version:** 0.0.2
**Build:** 400 / 20260927-400
**Parent source:** Build 399 checkpoint `00e819f6d2c3ad518982016a8bf22d1524fece57`
**Validation:** Fast CI pending; device validation pending

### Requirement

Extend the battery ring beyond charging-only color: follow HyperOS built-in battery semantic modes (power save, performance, low battery, charging) while normal state follows the same status-icon inversion tint as neighboring icons. Keep an architecture seam so every semantic state can later choose either HyperOS System default, the black/white/gray status-icon tint, or a user-selected custom color.

### Exact-target evidence

JADX 1.5.6 inspection of the pinned SystemUI target confirms:
- `MiuiBatteryMeterIconView.getProgressStatus()` owns final progress-color semantics.
- priority: quick/normal charging (including performance + charging) -> power save -> performance -> low -> normal.
- low state threshold is <= 19.
- SystemUI loads `status_bar_battery_charging`, `status_bar_battery_power_save`, `status_bar_battery_performance`, and `status_bar_battery_low` into icon-view fields.
- decoded exact resources currently resolve to #1DCD3A, #FF9F05, #3482FF, and #FA382E respectively; runtime code does not copy these values and instead reads the colors already loaded by SystemUI.
- this exact status-bar battery path has no separate super-power-save progress color.

### Implementation

- Expanded the existing battery source from the level callback to the exact level / charge / power-save / performance callback family.
- After native callback completion, read the native icon's private `getProgressStatus()` semantic result; a field-based priority mirror exists only as a fallback.
- Read the corresponding already-loaded native semantic color field; normal stores no semantic color and resolves against status-icon tint.
- Extend `BatteryState` and `CombinedStatusRenderModel` with semantic state and optional SystemUI semantic color.
- Preserve both across Hot Reload.
- Replace the hard-coded charging green in `CombinedStatusColorPolicy` with `CombinedStatusBatteryColorPolicy`.
- Define per-state future color sources: `SystemDefault`, `FollowStatusIcon`, or `Custom(color)`; current runtime uses `SystemDefault` only.
- Add tests for native semantic mapping/priority, all-state future source support, fallback behavior, color-link behavior, and Hot Reload preservation.

### Review

- **Ownership:** SystemUI remains the only battery-mode/state/color owner; Combined Status is read-only.
- **Lifecycle:** no polling/background service; callbacks stay in the existing battery runtime owner.
- **Single writer:** no native tint/mode/state field is written.
- **Cleanup / Hot Reload:** semantic state/color transfer uses the existing battery snapshot.
- **Fail-native:** unavailable semantic color falls back to current status-icon tint.
- **Performance:** four low-frequency native battery callbacks; no repeated View-tree traversal or per-frame reflection.
- **Compatibility:** reflected method/field contract is exact-target scoped and target-profile CI remains mandatory.
- **Future extension:** settings only need to supply per-state color-source preferences; state acquisition and painter ownership do not change.



---

## 2026-09-27 — Build 401 battery semantic source consolidation

**Type:** pre-device architecture correction / color authority
**Display version:** 0.0.2
**Build:** 401 / 20260927-401
**Supersedes before device validation:** Build 400
**Validation:** CI pending; focused device color validation pending

### Why Build 400 was not sent to device

Review of the first semantic-color implementation found two avoidable ownership costs:
- four separate Battery callbacks were hooked even though the native icon already funnels semantic changes through `MiuiBatteryMeterIconView.onDarkChangeInternal()`;
- a project-local fallback mode-priority mirror remained even though `getProgressStatus()` is available on the pinned target.

Those choices would work functionally but would duplicate native semantics and increase maintenance surface.

### Build 401 correction

- Battery percent/charging continues to use the existing `MiuiBatteryMeterView.onBatteryLevelChanged(...)` source.
- Semantic state/color changes use one additional exact-target hook on `MiuiBatteryMeterIconView.onDarkChangeInternal()`.
- After the native method completes, Combined Status reads the final `getProgressStatus()` enum and maps only its names into the project presentation enum.
- No local charging/power-save/performance/low priority reconstruction remains.
- Native semantic colors come from the already-loaded SystemUI fields; no RGB palette is copied.
- `mMiuiOptimizationEnabled=false` suppresses semantic color usage and falls back to the resolved status-icon tint, matching native SystemUI behavior.
- The native status-icon tint already observed by the status-icon presentation owner is now merged into Battery-derived tint updates before they reach the Home renderer. This closes the 0.0.2 overlay cutover gap where peer tint was observed but discarded.
- Future color-source policy remains available for NORMAL / CHARGING / POWER_SAVE / PERFORMANCE / LOW: System default, Follow status icon, Custom. Current runtime still uses System default only.
- Invalid/missing custom color falls back to that state's System default rather than forcibly falling back to monochrome.

### Review

- **Ownership:** HyperOS owns battery semantic state and built-in colors.
- **Lifecycle:** two battery hooks total for this owner: the existing level callback plus one native semantic callback.
- **Single writer:** Combined Status writes no native mode/tint/Drawable state.
- **Cleanup:** no new listener or observer registration; hook bookkeeping stays under the existing runtime owner.
- **Fail-native:** missing semantic color degrades only that ring state to native status-icon tint.
- **Performance:** event-driven, no polling, no frame callback, no duplicate mode observers.
- **Compatibility:** exact private method/fields remain target-profile gated.
- **Future extension:** settings can bind directly to the pure per-state color-source policy without new SystemUI integration.

The shallow shade-pull Home-overlay leak remains next after this color checkpoint is device-validated.


---

## 2026-09-27 — Build 403 battery color gate

**Type:** native semantic-color completion / CI checkpoint
**Display version:** 0.0.2
**Build:** 403 / 20260927-403
**Runtime source:** `97ef67e648906a4b9bb2ce4d7dd390e955831189`
**Validation:** Fast CI passed; signed Canary passed; focused device color validation pending

### Final pre-device correction

Review of Build 402 identified one remaining native gate: HyperOS conditionally applies its semantic battery colors through `mMiuiOptimizationEnabled`. Build 403 adds that exact target field and its setter to the verified compatibility contract. Semantic status still comes only from native `getProgressStatus()`; the gate controls only whether the native semantic color is consumed.

### Resulting behavior

- NORMAL -> resolved native status-icon tint.
- CHARGING / POWER_SAVE / PERFORMANCE / LOW -> native SystemUI semantic color when HyperOS optimization is enabled.
- When HyperOS disables semantic color optimization or the native semantic color is unavailable -> resolved native status-icon tint.
- The existing center/mobile “follow battery color” options consume the final resolved battery color exactly as before.
- All five states have the same future source choices: System default / Follow status icon / Custom, but no new user-facing settings are persisted yet.

### Review

- **Ownership:** HyperOS owns semantic state, priority, optimization gate and built-in colors.
- **Lifecycle:** event-driven BatteryIcon callbacks only; no polling or background observer.
- **Single writer:** no native field, Drawable, tint or mode state is written.
- **Cleanup:** no registered listener/observer lifecycle is added beyond hook generation ownership.
- **Fail-native/fallback:** unavailable semantic color degrades to the native status-icon tint.
- **Performance:** bounded low-frequency callbacks; no frame or pre-draw work.
- **Compatibility:** BatteryIcon methods/fields and optimization gate are pinned in the target profile and CI verifier.
- **Future extension:** user color preferences can bind to the pure color-source policy without reopening SystemUI state acquisition.

### CI / Canary

- Fast Build #1063: success.
- Signed Work Branch Canary #322: success.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-403-canary.apk`.
- Artifact ZIP digest: `sha256:55535e2c5208e930142b0a4a5256a3740cf9748070a2710520921cde13032b34`.
- Extracted APK SHA-256: `0715c2961264677e95bf96efe80b045cda91c07e02f34e588dba88df39c0d487`.

### Next

After focused device color validation, investigate the supplied shallow panel-pull video. The Home overlay must leave the Home presentation boundary as the shade begins taking ownership; future shade/Control Center Combined Status must move with its own target surface rather than rely on the stationary Home overlay.

---

## 2026-09-27 — Build 403 device visual feedback: grayscale parity remains open

**Type:** device visual-validation feedback / acceptance correction
**Display version:** 0.0.2
**Build:** 403 / 20260927-403
**Runtime source:** `97ef67e648906a4b9bb2ce4d7dd390e955831189`
**Validation:** Fast CI passed; signed Canary passed; visual-intensity acceptance remains open

### Device feedback

Latest target-device screenshots on the active Build-403 color/intensity line still show an apparent grayscale / visual-weight difference between Combined Status and neighboring native status icons.

This feedback changes the acceptance state: the visual-intensity work must not be treated as closed merely because Build 399 removed overlapping battery-arc compositing and Build 403 completed native semantic-color integration.

### Current interpretation

- Build 399's active/inactive battery-arc partition remains valid and stays in force.
- Build 403's semantic-state/color authority remains HyperOS-owned and is not rejected by this grayscale observation.
- Native status-icon tint remains the intended monochrome authority.
- Native drawable alpha normalization remains shared rather than resource-specific.
- The remaining difference has not yet been attributed to one confirmed final compositing/tint/alpha cause.

### Rejected shortcuts

Do not add:
- per-glyph gray multipliers;
- replacement gray constants;
- screenshot-derived magic values;
- source-asset recoloring/preprocessing solely to force a visual match.

Those would hide the remaining cause instead of fixing the shared final-rendering boundary.

### Review

- **Ownership:** unchanged; HyperOS remains tint/semantic-color authority and Combined Status owns only its compact rendering.
- **Lifecycle:** unchanged; no new hook/listener/polling path is justified by this feedback.
- **Single writer:** unchanged; no native tint/Drawable state is written.
- **Cleanup:** unchanged.
- **Fail native:** unchanged.
- **Performance:** no new per-frame or repeated correction is authorized.
- **Compatibility:** any next correction must remain target-profile compatible and avoid resource-specific hard-coded visual policy.
- **Future extension:** user color-source controls remain a later presentation-policy feature and must not be used to mask baseline parity defects.

### Next

1. Quantify and attribute the final rendered difference across battery ring, native center drawable, mobile layer, and neighboring SystemUI icons.
2. Re-open only the demonstrated compositing/tint/alpha cause.
3. Keep Build 403 semantic-color device validation open until the relevant native states and grayscale baseline are both acceptable.
4. After color/intensity closure, continue with the already-identified shallow shade-pull Home-overlay scene-boundary issue in Phase 2B.

### Quantitative screenshot follow-up

A follow-up pixel sample of the target-device gray screenshot compared the dark stroke pixels of Combined Status with adjacent native VPN / headset / mute icons.

The core dark-tone distributions are already closely aligned. The observed mismatch is therefore no longer best described as a simple base-gray/tint mismatch. The stronger current interpretation is **optical intensity / coverage**: stroke geometry, filled-pixel density, antialiasing, and final compositing can still make Combined Status look heavier or lighter even when the underlying dark tone is effectively the same.

This narrows the next review:
- keep the native status-icon tint authority unchanged;
- keep Build 399's non-overlapping battery-arc partition;
- compare final rendered coverage/antialiasing across battery ring, center glyph, and mobile dots;
- do not introduce gray multipliers or screenshot-fitted constants unless later evidence disproves the shared-tint conclusion.

The green screenshot is a separate semantic-color state and is not evidence of monochrome tint mismatch.

---

## 2026-09-27 — Post-Build 403 visual-intensity root-cause review: authored alpha mask becomes the next A/B boundary

**Type:** root-cause review / historical correction / pre-runtime validation gate
**Display version:** 0.0.2
**Current runtime source:** Build 403 / `97ef67e648906a4b9bb2ce4d7dd390e955831189`
**Validation state:** source-level root cause narrowed; next executable checkpoint not yet created

### Problem execution flow

**Phenomenon and evidence**

Target-device screenshots still show an optical-weight mismatch even though sampled core dark tones of the Combined Status center/mobile layers and adjacent native status icons are already close. Build 399 removed overlapping Battery-arc compositing, but the residual visual-parity issue remained open.

**Root cause / responsibility source**

Current painter review found a second project-owned compositing step in the native center path. `resolveNativeVisualProbe()` renders the HyperOS drawable into a bitmap, chooses an 85th-percentile visible-alpha ceiling, and linearly rescales every source-alpha pixel against that ceiling before the native status-icon tint is applied.

This changes the authored alpha mask itself. It can increase edge coverage, saturate the upper part of the mask, and therefore alter the apparent stroke/antialias weight even when the final tint authority is otherwise correct. This does not yet prove that alpha rescaling is the sole remaining visual cause; it does establish an additional non-native coverage transformation at exactly the boundary implicated by the latest screenshots.

**Repository and official platform rules**

- `CONTRIBUTING.md` requires authoritative native resources/semantics to be reused rather than reconstructed when a verified source exists and keeps optical adjustment separate from tint/layout ownership.
- Android `ImageView` applies image tint through the drawable; under SRC_IN semantics the tint is masked by the drawable alpha rather than by a project-derived percentile ceiling.
- The exact target SystemUI Wi-Fi path exposes `Icon.Resource` through its native ImageView-based pipeline, and Combined Status already treats the resource identity and status-icon tint as authoritative.

**HyperOS/SystemUI native implementation**

No exact-target evidence currently shows HyperOS rescaling a Wi-Fi/airplane/no-SIM resource's internal alpha mask to a project-defined percentile ceiling before status-bar rendering.

**Mature implementation comparison**

The standard Android drawable/ImageView contract keeps the drawable's authored alpha mask as part of the asset and composes tint/overall image alpha around it. The Combined Status percentile-normalization layer is additional behavior rather than a reuse of that mature native rendering contract.

### Historical correction

Build 356 introduced shared native alpha-mask / visual-intensity normalization, but its own device feedback still reported a visual-intensity mismatch. The durable conclusion from that investigation is not that percentile normalization itself is proven correct; the durable conclusions are that native tint authority should remain shared and per-resource gray multipliers / hand-edited replacement assets are not justified.

Historical Build-356 facts remain unchanged. This entry narrows the present interpretation using later device evidence and current source review.

### Selected single-variable correction

For the next runtime checkpoint:

- preserve the HyperOS drawable's authored per-pixel alpha mask;
- keep native resource identity, optical-bound measurement, final-pixel alignment and resolved native tint;
- keep center sizing, outer weight, mobile-dot geometry and Build-399 Battery arc partition unchanged;
- remove only the percentile source-alpha ceiling/rescaling path and its obsolete tests.

No gray multiplier, replacement tint, per-resource exception, source-asset preprocessing, new hook, polling path, or geometry writer is authorized.

### Review

- **Ownership:** HyperOS/SystemUI remains resource and tint authority; Combined Status stops rewriting native asset coverage.
- **Lifecycle:** unchanged; no new owner/listener/hook.
- **Single writer:** unchanged for tint/layout/visibility; one project-side alpha-mask transformation is removed.
- **Cleanup:** simpler; cached center assets remain session-local and recyclable.
- **Fail native / recovery:** resource-resolution failure behavior is unchanged.
- **Performance:** removes histogram/percentile normalization work from first-use center-asset preparation.
- **Compatibility:** no new reflected target member/resource contract.
- **Exception recovery:** malformed/unresolvable resources keep the existing native-center fallback.
- **Future extension:** remains independent from later user color-source and size/spacing controls.

### Next

Create the next executable checkpoint with this one rendering-boundary change, run Fast CI plus signed Work Branch Canary, then stop runtime changes for focused device A/B validation of monochrome optical parity and Build-403 semantic battery colors.

---

## 2026-09-27 — Build 404: preserve authored native center alpha mask

**Type:** single-variable runtime rendering correction
**Display version:** 0.0.2
**APK build:** 20260927-404
**Runtime source:** `614c6ae96f1753088e21ce3568d969b900852081`
**CI:** pending at documentation checkpoint
**Device validation:** pending

### Change

Removed the project-side 85th-percentile source-alpha ceiling and per-pixel alpha rescaling from native center resources. The resource is still rendered once for cached optical measurement/composition, but its authored alpha mask is retained and the resolved status-icon tint is applied later through the existing SRC_IN path.

The obsolete normalization helpers and normalization-specific tests were removed. Existing canvas tint-alpha / semantic dimming / transition-opacity tests remain.

### Single-variable boundary

Unchanged:
- HyperOS native resource identity;
- center optical-bound measurement;
- steady final-pixel alignment;
- center size;
- native status-icon tint authority;
- mobile-dot geometry / outer visual weight;
- Build-399 active/inactive Battery arc partition;
- Build-403 battery semantic state/color authority;
- Home carrier, suppression, lifecycle, cleanup, and fail-native behavior.

### Review

- **Ownership:** resource and tint authority stay with HyperOS/SystemUI; Combined Status now owns only placement/scale/composition rather than rewriting asset coverage.
- **Lifecycle:** no change.
- **Single writer:** no additional writer; one visual transformation was deleted.
- **Cleanup:** cached bitmaps continue to be recycled on eviction; no new retained object.
- **Fail native:** unchanged.
- **Performance:** first-use center preparation is cheaper because alpha histogram/quantile/remap work is gone.
- **Compatibility:** no new target-profile dependency.
- **Exception recovery:** unchanged.
- **Future extension:** no coupling added to future per-state colors or user size/spacing controls.

### Validation gate

Run Fast CI and signed Work Branch Canary for Build 404. If both pass, runtime changes stop until target-device A/B evidence answers whether authored-alpha preservation improves optical parity while preserving all Build-403 semantic-color states.

### CI request

The established `dev`-based validation carrier PR #104 was reopened for Build 404 and its body was updated to mark it validation-only / do-not-merge. Stacked PR #105 remains the product PR. This preserves the repository's existing Fast Build -> signed Work Branch Canary path without changing CI workflow logic or retargeting PR #105.

### CI trigger-path note

Reopen and Draft -> Ready mutations on validation carrier PR #104 were recorded by GitHub but did not create an Actions run for the current head. No runtime or workflow change was made in response. A documentation-only Contents-API commit is used as the next minimal trigger attempt so the open `dev`-based carrier receives a normal synchronize event while Build 404 executable source remains `614c6ae96f1753088e21ce3568d969b900852081`.

### Validation-carrier correction

The reopened historical validation PR #104 triggered Build #1087 and signed Work Branch Canary #323, but GitHub associated that run with the PR's historical executable head `97ef67e648906a4b9bb2ce4d7dd390e955831189` (Build 403), not the current Build-404 source. Those green runs are therefore **Build-403 evidence only** and must not be cited for Build 404.

A fresh ephemeral `fix/*` validation branch/PR based on the current Build-404 branch head is the selected CI path. This changes no executable source or workflow logic; it exists only to obtain an unambiguous `opened` PR event against `dev` so the repository's existing Fast Build -> signed Work Branch Canary chain validates the current candidate.



---

## 2026-09-27 — Build 404 CI / signed Canary gate passed

**Type:** CI acceptance / pre-device validation gate
**Display version:** 0.0.2
**Build:** 404 / 20260927-404
**Runtime source:** `614c6ae96f1753088e21ce3568d969b900852081`
**Validation carrier tested head:** `ad55baa47eecadfa7fe1968556d5c7f13a1be460`
**Device validation:** pending

### CI evidence

A fresh validation-only PR #130 (`fix/build-404-validation-carrier -> dev`) produced an unambiguous normal `pull_request` Build event for the Build-404 line.

- Fast Build #1088: **success**.
- Build #1088 tested head: `ad55baa47eecadfa7fe1968556d5c7f13a1be460`.
- Signed Work Branch Canary #324: **success**.
- Canary job explicitly checked out `ad55baa47eecadfa7fe1968556d5c7f13a1be460`.
- Target-profile verification: passed.
- Unit/build checks: passed.
- Modern Xposed API metadata verification: passed.
- Haple signature verification: passed.
- Non-debuggable Canary verification: passed.
- Artifact upload: passed.

The tested validation head is 16 commits ahead of runtime source `614c6ae96f1753088e21ce3568d969b900852081`, and repository compare shows the only file differences after that runtime source are `docs/development/CURRENT.md` and `docs/development/DEVLOG.md`. Therefore the validated executable runtime is still exactly Build 404; no later runtime delta is hidden in the validation carrier.

### Artifact

- `CombinedStatus-0.0.2-HyperOS-20260927-404-canary.apk`
- Workflow run: Work Branch Canary #324 / run id `36281397598`
- Artifact id: `10919007288`
- Artifact ZIP digest: `sha256:694c81c37b5dc8227f0da076211ae538ac9250770da2eb97003be0727734c79f`
- Extracted APK SHA-256: `eb16169738f3e16bcd208463ae4fc638898c3c46f2ca3b43efea3bda625519f1`

### Validation-carrier cleanup

PR #129 and PR #130 were validation-only carriers and are now closed. Neither is product work and neither should be merged. Temporary branches may remain until branch deletion is performed through a GitHub path that exposes ref deletion.

### Review

- **Ownership:** unchanged; HyperOS/SystemUI still owns native resource/tint/semantic state.
- **Lifecycle:** unchanged.
- **Single writer:** unchanged; Build 404 removes one project-side alpha-mask rewrite rather than adding another writer.
- **Cleanup:** validation carriers are closed; runtime cleanup contract is unchanged.
- **Fail native / recovery:** unchanged.
- **Performance:** no new runtime work; Build 404 still removes percentile alpha-remap work.
- **Compatibility:** exact target profile and Modern Xposed metadata passed CI.
- **Future extension:** no coupling added to future color-source or size/spacing settings.

### Device gate

Runtime modification stops here. The exact signed Build-404 Canary must now be tested on the target device for:
1. monochrome optical parity / native center antialiasing and apparent stroke weight;
2. unchanged center size, centering, outer ring/mobile geometry and Home spacing;
3. unchanged HyperOS battery semantic colors for NORMAL / CHARGING / POWER_SAVE / PERFORMANCE / LOW;
4. absence of regressions in Wi-Fi, hotspot, airplane and no-SIM native-resource presentation.

The shallow Home-overlay leak during notification-shade pull remains a separate Phase-2B issue and must not be mixed into this Build-404 A/B gate.


---

## 2026-09-27 — Build 404 device rejection and native Wi-Fi rendering-path closure

**Type:** device rejection / root-cause correction / exact-target rendering review
**Display version:** 0.0.2
**Build under test:** 404 / 20260927-404
**Runtime source:** `614c6ae96f1753088e21ce3568d969b900852081`
**Result:** optical-parity A/B rejected; next root-cause boundary moved upstream

### Device feedback

The target-device Build-404 result was reported as visually worse than the preceding version rather than closer to neighboring native status icons. The supplied screenshots confirm that the optical-parity issue remains open.

The screenshots include different battery semantic-color states, so they are not treated as a pixel-controlled Build-403-vs-404 pair. The reliable acceptance conclusion is narrower: Build 404 did not close parity and must not be promoted as the visual baseline.

### Problem execution flow

**Phenomenon and evidence**

Build 404 removed percentile source-alpha normalization but retained the pre-existing native-center bitmap pipeline. The visual mismatch remained.

Previous screenshot sampling had already shown that core monochrome tint values were close to native peers, pointing toward coverage/antialiasing rather than a simple gray-value mismatch.

**Root cause / responsibility source**

Exact target inspection shows that the previous A/B changed the wrong layer. Preserving alpha values inside an intermediate 96px bitmap does not reproduce the native rendering path because native antialias coverage is created when the VectorDrawable is rasterized at its final bounds.

Combined Status currently rasterizes the native resource at one resolution and resamples those already-rasterized pixels at another resolution before final presentation. That second sampling stage is project-owned and absent from the verified native Wi-Fi steady path.

### Exact HyperOS/SystemUI Wi-Fi path

Verified semantic/binder chain:

`WifiIcon.Visible.icon (Icon.Resource)`
→ `MiuiWifiViewBinder`
→ `MiuiStatusBarIconViewHelper.transformResId(resId, useTint, light)`
→ `ImageView.setImageResource(transformedResId)`

`transformResId(...)` selects the tint/light/dark mapped resource according to the native presentation state. The base semantic resource remains stored in the ImageView tag so UI-mode changes can transform it again.

Tint behavior:
- when `useTint=true`, the ImageView receives `ColorStateList.valueOf(tint)`;
- when `useTint=false`, image tint is cleared and the chosen light/dark resource owns its fill color.

The verified `stat_sys_wifi_signal_3` resource family uses the same vector path geometry for normal/dark/tint variants:
- XML size: 20dp × 20dp;
- viewport: 20 × 20;
- normal fill: light single-tone resource;
- dark fill: dark single-tone resource;
- tint variant: opaque black mask for ImageView tinting.

Native Home layout:
- `AlphaOptimizedImageView`;
- `WRAP_CONTENT × MATCH_PARENT`;
- `adjustViewBounds=true`;
- parent icon height: `status_bar_icon_height=20dp`.

Retained target-device diagnostics verify final native Wi-Fi presentation:
- `VectorDrawable`;
- intrinsic 75 × 75 px;
- drawable bounds `0,0,75,75`;
- ImageView measured 75 × 75 px;
- drawable alpha 255;
- image alpha 255;
- `FIT_CENTER`;
- identity image matrix;
- zero padding;
- observed native tints include `0xBF000000` and `0xE6FFFFFF` on the corresponding surfaces.

Therefore the native steady path is effectively:

`20dp vector resource -> final 75×75 drawable bounds -> one VectorDrawable rasterization -> screen`

There is no verified intermediate bitmap resize.

### Current Build-404 path

`native base Drawable`
→ force-white Drawable into a probe bitmap
→ rasterize at up to 96px
→ inspect bitmap alpha for optical bounds
→ retain that 96px bitmap
→ scale bitmap to Combined Status final center destination with `FILTER_BITMAP_FLAG`
→ apply final tint through `SRC_IN`
→ screen

For the current 105×108 Combined Status visual and existing Wi-Fi geometry, the final center image is roughly in the low-60px range, so the final presentation is a downsample of the 96px intermediate bitmap.

### Controlled coverage comparison

Using the exact Wi-Fi vector path, the same representative 63px target was compared:

Direct vector raster at 63px:
- total alpha sum: 184397;
- visible pixels: 860;
- fully covered pixels: 585;
- partial-edge pixels: 275;
- mean partial-edge alpha: 128.08.

96px raster followed by bilinear reduction to 63px:
- total alpha sum: 184277;
- visible pixels: 1004;
- fully covered pixels: 474;
- partial-edge pixels: 530;
- mean partial-edge alpha: 119.64.

Total alpha mass stays close, but the two-stage path converts many fully covered pixels into semi-transparent edge coverage. This directly explains how a resource can have the correct tint and broadly similar total coverage while still look softer / lighter / optically inconsistent.

### Historical correction

The Build-403 → Build-404 hypothesis was too downstream. The previous conclusion correctly rejected arbitrary gray multipliers and source-asset editing, but it overestimated the value of preserving alpha inside the already-rasterized 96px bitmap.

For the opaque `stat_sys_wifi_signal_3` path, the 85th-percentile alpha ceiling is already effectively 255, so the old normalization can be a no-op for normal Wi-Fi. Build 404 therefore does **not** prove that removing normalization itself caused the reported regression in every Wi-Fi state.

The durable new conclusion is:
- percentile alpha remapping is not native and should remain removed;
- authored vector/drawable semantics must survive until the final resolved draw bounds;
- intermediate bitmap resampling is the next demonstrated project-owned visual transformation.

### Selected next A/B boundary

Keep unchanged:
- semantic/base resource selection currently consumed by Combined Status;
- native tint authority and Build-403 semantic battery colors;
- optical target size and center position;
- outer ring/mobile geometry;
- Build-399 Battery arc policy;
- Home carrier/suppression/lifecycle/scene behavior.

Change only final native-center presentation:
- use a module-owned Drawable clone/ConstantState instance as the final source;
- set/apply the resolved tint/opacity without editing source pixels;
- draw the Drawable directly at the resolved final bounds;
- keep a bitmap probe only if bounded optical measurement still requires it;
- do not feed probe pixels into final rendering.

### Review

- **Ownership:** HyperOS remains semantic resource/tint authority; Combined Status owns only its composition bounds and its clone, not the native ImageView/Drawable instance.
- **Lifecycle:** the renderer/session owns recreated/cached Drawable clones; no new SystemUI listener or owner is introduced.
- **Single writer:** no native View/tint/geometry writer is added.
- **Cleanup:** final presentation no longer needs a retained raster bitmap; any measurement bitmap remains bounded and recyclable.
- **Fail native / recovery:** unresolved/invalid resources keep the existing fallback behavior.
- **Performance:** removes final bitmap resampling; direct VectorDrawable drawing occurs only when the Combined Status View redraws and does not require polling.
- **Compatibility:** no new private member/hook is required for the first A/B.
- **Exception recovery:** Drawable resolution/clone failure remains local to the native-center resource path.
- **Future extension:** direct Drawable tinting is compatible with later per-state custom color policy and adaptive center sizing without regenerating source assets.

### Next

Implement this single rendering-boundary A/B as the next runtime checkpoint, run Fast CI and signed Canary, then stop for target-device comparison. Do not combine the shallow shade-scene fix, new geometry values, or new color policy with this checkpoint.


---

## 2026-09-27 — Build 404 device regression and exact native icon-rendering root cause

**Type:** device A/B rejection / exact-target rendering-path review / historical correction
**Display version:** 0.0.2
**Rejected checkpoint:** Build 404 / 20260927-404
**Runtime source:** `614c6ae96f1753088e21ce3568d969b900852081`
**Device result:** center visual parity regressed versus Build 403

### Problem execution flow

**Phenomenon and evidence**

The target-device Build-404 screenshot shows the center Wi-Fi presentation looking worse than the immediately preceding Build-403 line. Build 404 changed only the center native-resource alpha preparation: it removed the 85th-percentile alpha-ceiling/remap and retained the resource's authored alpha mask. Geometry, native tint authority, outer ring/mobile weight, Battery arc policy, battery semantic colors and Home carrier were unchanged by that runtime commit.

This device A/B therefore rejects the claim that authored-alpha preservation **within the existing bitmap path** is sufficient to fix visual parity.

**Root cause / responsibility source**

A fresh directed reverse engineering pass was performed against the exact retained target SystemUI APK:

- SystemUI `17.03.260226.r`;
- SHA-256 `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`.

The earlier Build-404 premise compared only alpha-mask semantics, but the native pipeline differs at an earlier rendering boundary.

Verified HyperOS Home Wi-Fi path:

`WifiIcon.Visible.icon / Icon.Resource`
-> `MiuiWifiViewBinder`
-> `MiuiStatusBarIconViewHelper.transformResId(rawResId, useTint, isLight)`
-> Light / Dark / Tint VectorDrawable variant
-> `ImageView.setImageResource(...)`
-> optional `ImageView.setImageTintList(...)`
-> direct VectorDrawable rendering in the final native ImageView.

Exact geometry/resource facts:
- modern Home Wi-Fi slot height: `R.dimen.status_bar_icon_height = 20dp`;
- Wi-Fi root: `WRAP_CONTENT x MATCH_PARENT`;
- main `AlphaOptimizedImageView`: `WRAP_CONTENT x MATCH_PARENT`, `adjustViewBounds=true`;
- `stat_sys_wifi_signal_3*`: `20dp x 20dp`, viewport `20 x 20`;
- `AlphaOptimizedImageView` does not replace ImageView drawing with a custom bitmap path.

Verified level-3 presentation variants:
- Light/base `0x7f081b3e`: path fill `#FFFFFFFF`;
- Dark `0x7f081b3f`: path fill `#BF000000`;
- Tint `0x7f081b40`: opaque black mask plus ImageView tint.

By contrast, Build 404 still performs:

`raw semantic resource`
-> clone Drawable
-> tint white
-> rasterize to an ARGB bitmap up to 96px
-> scan bitmap alpha for optical bounds
-> retain that bitmap as the cached rendered asset
-> compute Combined Status size from optical bounds
-> resample the bitmap into final physical bounds
-> apply final native tint through SRC_IN.

The project therefore performs at least one intermediate rasterization plus a later bitmap resample that the verified native Wi-Fi/status-icon draw path does not perform.

**Repository / native guidance**

The current `CONTRIBUTING.md` native-visual rule is already aligned with this newer evidence: preserve authored drawable/vector semantics through the final resolved bounds where practical and do not introduce intermediate rasterization/resampling or project alpha normalization unless exact-target evidence proves the native path does the same.

The new exact-target evidence also narrows the role of optical measurement. Combined Status may still need a bounded optical probe because its compact center is not the native 20dp status-icon slot, but that probe is a measurement implementation detail and should not automatically become the final visual asset.

**Mature implementation comparison**

Both the exact HyperOS Wi-Fi path and the neighboring traditional `StatusBarIconView` path retain Drawable/ImageView rendering. HyperOS changes resource variants and tint state, then lets the Drawable rasterize at the final presentation bounds. No equivalent 96px cached-resource bitmap -> final bitmap-resample stage was found in the directed target audit.

### Historical correction

Build 403's percentile normalization is **not reinstated as a native requirement** merely because Build 404 looks worse.

The more consistent interpretation is:

- Build 403: non-native bitmap/resample path + additional alpha compensation;
- Build 404: same non-native bitmap/resample path without that compensation;
- Build 404 regression: evidence that the alpha compensation had been masking part of the larger rendering-path mismatch.

This correction preserves the valid historical conclusion that per-resource gray constants and hand-edited assets are not justified.

### Selected next A/B boundary

Keep the test single-variable:

- preserve the current raw semantic resource ID;
- preserve current status-icon tint authority;
- preserve current optical-bound measurement and final center dimensions;
- preserve center placement, outer weight, Battery arc policy, battery semantic colors and Home carrier;
- stop using the probe bitmap as the rendered asset;
- cache/clone the native Drawable and draw it directly into the final resolved bounds so VectorDrawable rasterization happens at the final presentation size;
- a bounded bitmap probe may remain **measurement-only** and should be recycled after optical bounds are extracted.

The HyperOS Light / Dark / Tint `transformResId` presentation transformation is a second verified difference. It must remain a separate follow-up boundary unless new evidence proves it is inseparable from direct Drawable rendering.

### Review

- **Ownership:** HyperOS remains semantic resource and tint authority; Combined Status owns only compact placement/scale and its own final Drawable instance.
- **Lifecycle:** resource assets remain painter/session scoped; no new listener, observer or SystemUI owner is introduced.
- **Single writer:** no native View/Drawable property is written; only module-owned cloned Drawable state is changed before draw.
- **Cleanup:** removing cached rendered bitmaps reduces retained bitmap ownership; any measurement bitmap must be recycled immediately.
- **Fail native / recovery:** unresolved/malformed native resources keep the existing fallback behavior.
- **Performance:** direct VectorDrawable draw removes bitmap resampling and persistent bitmap cache cost. Resource cloning/cache policy must avoid allocation per frame.
- **Compatibility:** no new private member/hook is required for the first A/B.
- **Exception recovery:** resource-resolution failure remains bounded to the center native-resource path.
- **Future extension:** direct final-bounds Drawable rendering is compatible with later user size controls because the vector rasterizes at each resolved final size instead of stretching a pre-rasterized source.

### Next

Implement the direct-final-Drawable A/B as the next executable checkpoint, run source review + Fast CI + signed Work Branch Canary, then stop runtime changes for focused target-device comparison against Build 403 and Build 404. Do not mix the later Light / Dark / Tint resource-transform integration or Phase-2B shade work into that checkpoint.


---

## 2026-09-27 — Build 405: direct final-bounds native Drawable rendering

**Type:** single-variable runtime rendering correction / post-review checkpoint
**Display version:** 0.0.2
**Build:** 405 / 20260927-405
**Runtime source:** `bf8091c8680dec7b85c58afded7f476ec95ca49d`
**CI:** pending
**Device validation:** pending

### Change

Build 405 removes the 96px optical-probe bitmap from the **final native-center presentation path** while preserving the rest of the Build-404 geometry/state contract.

The native center asset cache now stores the module-owned cloned `Drawable` instead of a raster bitmap. The existing bounded raster probe remains only to extract optical bounds. Its pixels are never drawn to the status bar and the temporary bitmap is released in `finally` on success, invalid-resource exit, or exception.

Final native-center drawing now:
- applies the existing resolved center tint and opacity to the module-owned Drawable clone;
- uses the same optical ratios, center position and resolved draw width/height as Build 404;
- preserves the existing steady-state final-pixel bounds calculation;
- draws the Drawable directly through Canvas transforms so VectorDrawable rasterization occurs at the final presentation transform rather than at a 96px source followed by bitmap resampling.

### Intentionally unchanged

- raw semantic resource ID consumed by Combined Status;
- HyperOS Light / Dark / Tint `transformResId` integration is **not** added in this build;
- center optical measurement algorithm and threshold;
- center size and centering;
- status-icon tint authority;
- center-family transition contract;
- outer ring/mobile geometry and weight;
- Build-399 Battery arc partition;
- Build-403 battery semantic state/color authority;
- Home carrier, native suppression, reservation, lifecycle and fail-native behavior;
- Phase-2B shade behavior.

### Review

- **Ownership:** cached Drawable is created from SystemUI resource `ConstantState` and `mutate()`d as a module-owned instance. No live native ImageView/Drawable is modified.
- **Lifecycle:** cache remains painter-scoped and bounded to eight center assets. No hook/listener/observer ownership is added.
- **Single writer:** only the module-owned clone receives tint/alpha/bounds writes; native View geometry, tint and visibility writers are unchanged.
- **Cleanup:** the persistent rendered bitmap is removed. The measurement bitmap is unconditionally recycled through `try/finally`; cache eviction no longer owns bitmap recycling.
- **Fail native / recovery:** existing resource-resolution failure returns the same native-center failure/fallback behavior. An exception during probe creation remains confined by the existing `runCatching` asset construction.
- **Performance:** removes filtered bitmap resampling and retained bitmap memory for center assets. Drawable cloning happens only on cache miss; drawing reuses the cached clone rather than allocating per frame.
- **Compatibility:** no new SystemUI private member, hook, resource identifier, reflection dependency or target-profile contract is introduced.
- **Exception recovery:** probe bitmap cleanup is deterministic even if Drawable drawing or pixel extraction fails.
- **Future extension:** direct final-bounds Drawable rendering naturally supports later center-size changes without stretching a pre-rasterized source; current custom-color seam can continue to tint the module-owned clone.

### Validation gate

Run Fast CI and signed Work Branch Canary for Build 405. If both pass, stop runtime changes and compare the exact Canary against Builds 403 and 404 on the target device.

Primary acceptance question: does removing the intermediate raster/resample stage restore native-like edge coverage / antialiasing and apparent stroke weight **without** changing size, centering, tint, outer geometry or semantic battery colors?

The verified HyperOS Light / Dark / Tint resource transformation remains the next separate rendering boundary only if Build 405 still leaves a state-dependent difference.


---

## 2026-09-27 — Build 405 CI event-delivery blocker

**Type:** CI infrastructure / validation-carrier status
**Runtime source:** `bf8091c8680dec7b85c58afded7f476ec95ca49d`
**Build:** 405 / 20260927-405
**Runtime state:** frozen; no further runtime mutation

Two validation-only carriers were attempted against the unchanged `dev` base SHA `6de78d7257c6bd376c57834a052fe51325fbfc1f`:

- PR #131: `fix/build-405-validation-carrier -> dev`;
- PR #132: `fix/build-405-validation-carrier-2 -> dev`.

For #132, the docs-only carrier commit `9b90286499574ec142be302ff1a426e0f7fcf29a` contains no executable delta after the frozen Build-405 runtime line.

The active base workflow remains unchanged and listens to `pull_request` types `opened`, `synchronize`, `reopened`, `ready_for_review`, and `converted_to_draft`. Connector-originated opened, docs-only synchronize, Draft -> Ready, and close -> reopen events produced **zero Actions workflow runs and zero check-runs** for the carrier SHA.

Historical comparison:
- Build-404 validation PR #130 used the same `dev` base SHA and produced Build #1088 within seconds of PR creation.
- Build #1088 actor / triggering_actor were both `CHS-Haple`.

Current conclusion: this is an event-delivery failure before Actions execution, not evidence of a Build-405 compile/test failure and not a repository workflow-condition mismatch.

Validation rule:
- keep Build 405 frozen;
- keep PR #132 as the active validation carrier;
- obtain one normal user-originated `pull_request synchronize` event from GitHub web/local git;
- do not cite Build 405 as CI-passed until a run explicitly tests the carrier head and the downstream signed Work Branch Canary succeeds;
- workflow_dispatch alone is insufficient for the full signed Canary chain because Work Branch Canary requires the upstream Build event to be `pull_request`.


---

## 2026-09-27 — Build 405 trusted Canary validation complete

**Type:** CI validation / device-test handoff
**Display version:** 0.0.2
**Build:** 405 / 20260927-405
**Runtime source:** `bf8091c8680dec7b85c58afded7f476ec95ca49d`
**Validated PR head:** `3cdfcc4db4bd5cd350e17400f2ed71d818b9c8d9`
**Validation:** passed; device A/B pending

### CI-flow resolution

The earlier connector-originated pull-request event-delivery failure was resolved at the repository-process layer rather than by mutating Build 405 runtime code.

PR #134 installed the trusted default-branch Canary fallback and passed:
- Full Build #1095: success on final process head `6c08962914b3af204cf2e94701cd28dc5084f357`;
- signed Work Branch Canary #331: success;
- merge to `main`: `5ca1029bb8383da793b69f81370df2760d4389dc`;
- post-merge main Build #1096: success.

The fallback adds an owner-only exact `/canary` PR-comment admission path plus a manual-dispatch fallback while preserving the normal automatic PR-Build follow-up.

### Build 405 trusted validation

Repository owner posted exact `/canary` on PR #105. Work Branch Canary #332 then:
- resolved the live same-repository PR head;
- resolved branch `feat/battery-semantic-colors`;
- resolved source SHA `3cdfcc4db4bd5cd350e17400f2ed71d818b9c8d9`;
- verified checkout equals the resolved trusted source;
- restored/verified Haple signing;
- verified the pinned HyperOS target profile;
- passed unit tests and Canary build;
- passed Modern Xposed metadata validation;
- passed Haple APK signature verification;
- passed non-debuggable verification;
- uploaded the signed Canary artifact.

Comparison from runtime source `bf8091c8680dec7b85c58afded7f476ec95ca49d` to validated PR head `3cdfcc4db4bd5cd350e17400f2ed71d818b9c8d9` contains only:
- `docs/development/CURRENT.md`;
- `docs/development/DEVLOG.md`.

Therefore the validated executable content remains exactly the frozen Build-405 runtime checkpoint.

### Artifact identity

- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-405-canary.apk`
- Actions artifact ID: `10929226454`
- Artifact ZIP SHA-256: `4d0a9b45bd1ec3cc9ab921226adca8ca6897188468393a4d65137c4fd7843137`
- Extracted APK SHA-256: `106fbfebe88a9e386f00f61271e38cc8f5999e5f43e086627c42d66985310203`

### Review / outcome

- **Ownership/lifecycle/single writer:** unchanged from the Build-405 source review; no runtime mutation was made while resolving CI.
- **Cleanup:** CI carrier PRs are no longer needed for Build 405.
- **Fail native / compatibility / performance:** unchanged from the frozen Build-405 runtime checkpoint.
- **Validation meaning:** CI proves source/build/signing/metadata contract only; optical parity still requires focused target-device comparison.
- **Next:** stop runtime changes and compare Build 405 against Builds 403 and 404 for native center edge coverage/antialiasing, apparent weight, size/centering, and unchanged outer geometry/tint. HyperOS Light/Dark/Tint resource-variant transformation remains a separate follow-up boundary only if a state-dependent mismatch survives.


---

## 2026-09-27 — Build 405 final Canary revalidation after summary escaping fix

**Type:** CI process follow-up / final device-test artifact identity
**Display version:** 0.0.2
**Build:** 405 / 20260927-405
**Runtime source:** `bf8091c8680dec7b85c58afded7f476ec95ca49d`
**Validation:** passed; device A/B pending

### Process follow-up

After the first successful owner-comment validation (#332), PR #135 corrected Markdown backtick escaping in the Work Branch Canary source-summary output:

- changed summary writes from interpolated `echo` lines containing raw backticks to `printf` with escaped Markdown backticks;
- no source-resolution, trust, checkout, signing, test, metadata, non-debuggable, artifact, or runtime behavior changed;
- `main` commit after the fix: `bddf1cff3deb4989d8fabfbeeec28440849d6a7d`;
- PR #137 history-preserving back-synced the fix into `dev`.

### Final Build 405 owner-comment revalidation

A second exact `/canary` comment on PR #105 started Work Branch Canary #334 with the corrected default-branch workflow.

#334:
- resolved the live same-repository PR #105 source;
- verified the checked-out source;
- restored and verified Haple signing;
- passed pinned HyperOS target-profile verification;
- passed unit tests and Canary build;
- passed Modern Xposed metadata checks;
- passed Haple APK signature verification;
- passed non-debuggable verification;
- uploaded the signed Build-405 Canary artifact.

Validated PR #105 head for #334: `b3092d42e428acb6d00a4e0c752459dc8ea64152`.

Comparison from frozen runtime source `bf8091c8680dec7b85c58afded7f476ec95ca49d` to that validated head still contains only:
- `docs/development/CURRENT.md`;
- `docs/development/DEVLOG.md`.

### Final artifact identity

- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-405-canary.apk`
- Actions artifact ID: `10929556081`
- Artifact ZIP SHA-256: `4b3a37d9c83901743122294fa436380769a6c0f9cb5aba84e64632237a3771a9`
- Extracted APK SHA-256: `17b8ed5783373ab37c4bf2ae3e8fda55ddff965e59a9c42d7c2ccb18efb4c566`

The earlier #332 APK had SHA-256 `106fbfebe88a9e386f00f61271e38cc8f5999e5f43e086627c42d66985310203`. A ZIP-entry content comparison between #332 and #334 found:
- identical entry set: 95 entries;
- identical uncompressed content: 94 entries;
- only differing entry: `META-INF/version-control-info.textproto`;
- the only changed payload is the embedded Git revision, from `3cdfcc4db4bd5cd350e17400f2ed71d818b9c8d9` to `b3092d42e428acb6d00a4e0c752459dc8ea64152`.

Therefore the overall APK/signature bytes differ because build provenance metadata differs, while classes/resources/runtime ZIP entries are unchanged.

### Outcome

Use the #334 artifact as the final Build-405 device-test package. Runtime remains frozen. No Light/Dark/Tint `transformResId` integration, Phase-2B scene work, or other runtime modification should occur until the focused device A/B result is returned.


---

## 2026-09-27 — Build 405 device rejection: final presentation-resource contract remains

**Type:** device rejection / root-cause refinement
**Display version:** 0.0.2
**Build under test:** 405 / 20260927-405
**Runtime source:** `bf8091c8680dec7b85c58afded7f476ec95ca49d`
**Result:** optical-parity A/B rejected; direct-Drawable baseline retained

### Problem / objective

Determine whether removing the project-owned 96px bitmap from the final native-center presentation path is sufficient to match neighboring HyperOS status-icon opacity/weight.

### Problem execution flow

**Phenomenon and evidence**

Target-device screenshots on both a light Settings surface and a dark Home/recents surface show the Combined Status center Wi-Fi glyph still materially lighter / lower-opacity than neighboring native status icons. The difference is visible inside the same Combined Status composition: the outer ring remains visually strong while the center native glyph is comparatively faint.

**Root cause / responsibility source**

Build 405 successfully removed the intermediate final-presentation bitmap/resample stage, so that stage is no longer sufficient to explain the remaining mismatch.

Exact-target reference evidence already establishes a second presentation difference that Build 405 intentionally left unchanged: HyperOS transforms the raw semantic resource through `MiuiStatusBarIconViewHelper.transformResId(rawResId, useTint, isLight)` and then follows distinct Tint versus Light/Dark drawing branches. Build 405 still resolves the raw resource and uniformly applies the Combined Status center tint.

**Root-cause status:** high confidence that final presentation-resource/tint-branch mismatch is now the next responsible boundary. It is not yet confirmed as the complete final cause until a single-variable A/B is device-tested.

### Evidence / references consulted

- Latest repository `CONTRIBUTING.md`, especially native visual-resource integration, root-cause order, single-writer and fail-native rules.
- `docs/reference/native-icon-rendering.md`.
- `docs/architecture/layout-policy.md` and `scene-policy.md`.
- Exact target SystemUI reference for Wi-Fi Light / Dark / Tint transformation and final ImageView/VectorDrawable rendering.
- Build 405 source/CI record and the maintainer-provided target-device screenshots.

### Alternatives considered

1. Increase center alpha or apply a per-resource opacity multiplier — rejected as screenshot-fitted symptom compensation and contrary to native-resource rules.
2. Return to Build 403 percentile alpha remapping — rejected; it partially compensated a non-native bitmap pipeline and is not an authoritative HyperOS contract.
3. Restore the bitmap final-render path — rejected; Build 405 removed a verified non-native resampling responsibility and the direct final-bounds Drawable path remains the cleaner baseline.
4. Reproduce the verified HyperOS presentation variant + tint/no-tint branch for native center resources only — selected for the next bounded A/B.

### Decision / next implementation boundary

Build 406 may change only native center presentation selection:
- resolve the final Light / Dark / Tint resource variant using the exact target HyperOS contract;
- in Tint mode, apply the resolved native status-icon tint to the Tint resource;
- in non-Tint Light/Dark modes, draw the selected authored resource without imposing the Combined Status tint;
- retain Build 405 direct final-bounds Drawable rendering and measurement-only optical probe.

Do not change center size/position, outer geometry, Home carrier/spacing, battery semantic-color logic, or Phase-2B scene behavior in the same checkpoint.

### Review

- **Ownership:** presentation-resource choice remains Combined Status-owned only for the module-owned native center clone; no native View is mutated.
- **Lifecycle:** no new observer/listener is justified if the already-resolved native tint/light state can be carried through the existing presentation state.
- **Single writer:** no alpha/tint compensation writer may be layered on top of the selected branch.
- **Cleanup:** no new long-lived resource owner is required beyond the bounded center asset cache.
- **Fail native:** if exact transformation state/resource resolution is unavailable, preserve the existing safe fallback rather than inventing a color.
- **Performance:** resource transformation should be event/state-driven and cacheable; no polling or per-frame reflection.
- **Compatibility:** exact target private helper/member use must remain fingerprint-scoped and optional.
- **Future extension:** preserving semantic resource identity separately from presentation resource selection supports later custom-color policy without duplicating HyperOS state semantics.

### Validation / outcome

Build 405 is **rejected for optical-parity acceptance**. Battery semantic-color acceptance remains a separate gate.

The next executable checkpoint is Build 406. Runtime changes should stop again as soon as its signed Canary is ready for focused light/dark device comparison.


---

## 2026-09-27 — Build 406: use native tint-mask variants for battery-colored center glyphs

**Type:** bounded native-center presentation correction
**Display version:** 0.0.2
**Build:** 406 / 20260927-406
**Runtime source:** `3d5e9d2339824c6d19e50dda170917559369135b`
**Validation:** source review complete; CI/Canary pending

### Problem / objective

Build 405 direct-Drawable rendering still produced a visibly lower-opacity center glyph while the user-facing **center follows battery color** option was enabled. The objective is to match HyperOS's tint-resource contract without changing geometry, state semantics, or Home carrier ownership.

### Problem execution flow

**Phenomenon and evidence**

The supplied light/dark screenshots show a strong battery ring and a visibly fainter green Wi-Fi center under the same intended semantic color.

**Root cause / responsibility source**

The exact-target native Wi-Fi family provides separate Light / Dark / Tint resources. The Tint variant is the opaque mask intended to receive a runtime tint. Build 405 instead loaded the raw semantic resource and called `Drawable.setTint(...)`, which preserves authored alpha/coverage from whichever raw variant was loaded.

Because this failing scenario intentionally recolors the center to the battery semantic color, selecting the native Tint mask is a stronger and narrower correction than reconstructing the entire SystemUI Light/Dark state machine.

**Root-cause status:** high confidence; device validation is still required before calling it confirmed.

### Implementation

- Added an explicit `centerUsesBatteryTint` presentation flag to the resolved color policy.
- Native center rendering keeps the Build-405 direct final-bounds Drawable path.
- When `centerUsesBatteryTint=true`, SystemUI native center resources resolve a cached `_tint` sibling resource when available, after normalizing an input `_darkmode` or `_tint` suffix.
- The existing resolved center color is then applied to that native tint mask.
- Missing/non-SystemUI tint variants fall back to the existing resource path rather than inventing a replacement.
- Added deterministic tests for base, dark, existing-tint, unavailable and hotspot-style resource-name transformation.
- Advanced internal identity to `20260927-406`; display version remains 0.0.2.

### Intentionally unchanged

- no new Hook, callback, listener, observer, polling loop or View-tree traversal;
- default `centerFollowsBatteryColor=false` behavior;
- center size/position and optical probe;
- battery ring/mobile geometry;
- battery semantic-state/color authority;
- Home overlay/carrier/spacing ownership;
- native Battery visibility/translation/alpha/island motion;
- Phase-2B shade behavior.

### Review

- **Ownership:** only module-owned presentation-resource selection changes; native Views/resources are read-only.
- **Lifecycle:** no new lifecycle owner.
- **Single writer:** the module still writes tint only to its cloned Drawable; no competing native View writer is introduced.
- **Cleanup:** bounded resource-ID cache only; no new listener/handle cleanup.
- **Fail native:** missing tint sibling falls back to the existing resource rather than hiding native behavior or inventing a color.
- **Performance:** `Resources.getIdentifier` occurs only on first resolution per cached resource ID; steady drawing uses cached IDs/assets.
- **Compatibility:** the suffix contract is limited to a verified SystemUI resource family and degrades safely when a sibling does not exist.
- **Exception recovery:** resource lookup is guarded; failure does not crash SystemUI.
- **Future extension:** this separates “custom semantic recolor uses native tint mask” from later default Light/Dark presentation mirroring, avoiding a duplicate state machine.

### CI / device gate

Run CI and signed Canary for this exact runtime source. After a signed Build-406 artifact exists, stop runtime changes.

Focused device acceptance:
1. keep **center follows battery color** enabled;
2. compare center glyph vs battery ring/native peers on a light surface;
3. repeat on a dark surface;
4. verify center size/centering, ring/mobile geometry and Home spacing are unchanged;
5. verify charging/power-mode semantic color still propagates to the center when enabled.


---

## 2026-09-27 — Build 406 trusted Canary validation complete

**Type:** CI validation / device-test handoff
**Display version:** 0.0.2
**Build:** 406 / 20260927-406
**Runtime source:** `3d5e9d2339824c6d19e50dda170917559369135b`
**Validation:** passed; focused device A/B pending

### CI path

The normal connector-originated pull-request synchronize event again produced no Build workflow run for either the runtime source or the documentation-only PR head. This matches the already-documented GitHub event-delivery failure and is not a compile/test failure.

The repository-owner exact `/canary` fallback on PR #105 started Work Branch Canary #335 (run `36312717485`).

The trusted workflow:
- resolved the live same-repository PR source;
- checked out and verified the tested source identity;
- validated the Gradle Wrapper and JDK/API environment;
- restored and verified Haple signing;
- passed the pinned HyperOS target-profile check;
- passed unit tests and Canary build;
- passed Modern Xposed metadata validation;
- passed Haple APK signature verification;
- passed non-debuggable verification;
- prepared and uploaded the signed Canary artifact.

### Source identity

Validated PR head: `80f371784ffaee406dd6ea5728219eeee5913318`.

Comparison from frozen runtime source `3d5e9d2339824c6d19e50dda170917559369135b` to that validated head contains only:
- `docs/development/CURRENT.md`;
- `docs/development/DEVLOG.md`.

Therefore the validated executable/runtime content is exactly Build 406.

### Artifact identity

- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-406-canary.apk`
- Actions artifact ID: `10929711688`
- Artifact ZIP digest: `sha256:c7af997217acd171c66beb860d7212c0d72fd672a38978f7b5c2eb5a524f11ba`
- Extracted APK SHA-256: `e086d8914fece7ba8ea86200756f4a6366d56dadaa5510846618a4077e6ddd80`

### Review / outcome

- **Ownership / lifecycle / single writer:** unchanged from source review; no new runtime owner or competing writer was introduced.
- **Cleanup / recovery:** unchanged; the added resource-ID cache is painter-local and bounded by encountered center resources.
- **Fail native:** missing tint siblings fall back to the prior resource path.
- **Performance:** no new callback or polling path; tint sibling lookup is cached.
- **Compatibility:** exact-target resource-family behavior is used only when the sibling exists.
- **Runtime state:** frozen pending device acceptance.

### Focused device test

1. Keep **center follows battery color** enabled.
2. On a light surface, compare the center glyph with the battery ring and neighboring native icons.
3. Repeat on a dark surface.
4. Check that center size/centering, ring/mobile geometry and Home spacing did not move.
5. Switch a semantic battery state when practical and confirm the center still follows the final battery color.

No Phase-2B, opacity multiplier, grayscale compensation, geometry tuning or additional runtime feature work should be added until this A/B result is returned.


### Build 406 CI / Canary result

- Work Branch Canary run: **#335** / run ID `36312717485`.
- Trigger: owner-only PR `/canary` fallback because connector-authored branch updates again produced no PR synchronize workflow run.
- Result: **success**.
- Passed gates:
  - trusted source resolution and exact checkout verification;
  - Gradle Wrapper validation;
  - Java / Android API 37 setup;
  - Haple signing restore/verification;
  - pinned HyperOS target-profile verification;
  - tests and Canary build;
  - modern Xposed metadata verification;
  - Haple APK signature verification;
  - non-debuggable verification;
  - artifact upload.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-406-canary.apk`
- GitHub artifact ID: `10929711688`.
- Extracted APK SHA-256: `e086d8914fece7ba8ea86200756f4a6366d56dadaa5510846618a4077e6ddd80`.
- Extracted APK size: `3293214` bytes.
- Runtime source remains `3d5e9d2339824c6d19e50dda170917559369135b`; subsequent commits are documentation-only.

### Gate

**Runtime is frozen at Build 406.** The next required evidence is maintainer device A/B with `centerFollowsBatteryColor=true` on both light and dark surfaces. No Phase-2B or further center-rendering runtime change should be layered before that result.


---

## 2026-09-27 — Build 406 device rejection: default center branch is also optically light

**Type:** device rejection / hypothesis correction
**Display version:** 0.0.2
**Build under test:** 406 / 20260927-406
**Runtime source:** `3d5e9d2339824c6d19e50dda170917559369135b`
**Result:** optical parity rejected; custom-tint-only hypothesis disproved

### Problem / objective

Build 406 tested whether the remaining low-opacity/low-weight center was primarily caused by recoloring a raw native resource instead of the verified HyperOS `_tint` mask variant.

### Device evidence

The maintainer supplied a new screenshot with **center follows battery color disabled**. The center Wi-Fi glyph remains visibly lighter / less solid than the outer battery ring and native peers.

A direct pixel inspection of that screenshot gives supporting evidence:
- light embedded status-bar crop: ring dark pixels reach approximately gray 53 while center dark pixels reach approximately 56, but the center's dark-pixel median is approximately 96 versus ring approximately 80, indicating less effective optical coverage even when the darkest core is similar;
- live dark status-bar crop: ring bright pixels reach approximately 239 while center reaches approximately 216, indicating a real presentation-alpha/coverage difference on that surface.

These values are screenshot observations, not runtime constants and must **not** be copied into rendering policy.

### Problem execution flow

**Phenomenon and evidence -> root-cause correction**

Because the defect persists with `centerFollowsBatteryColor=false`, Build 406's selected custom-color-only responsibility boundary is too narrow.

**Build-403 comparison**

The exact Build-403 source (`97ef67e648906a4b9bb2ce4d7dd390e955831189`) rasterized the native center into a bounded probe, computed an 85th-percentile visible-alpha ceiling, normalized source alpha against that ceiling, retained the normalized bitmap, then applied the final resolved tint with SRC_IN.

Build 404 removed that source-alpha normalization while retaining the same bitmap path and regressed on device. This establishes that the normalization materially increased apparent center coverage/weight, but does **not** establish that percentile normalization is a native SystemUI contract.

Build 405 moved final rendering back to a direct module-owned Drawable clone and kept the bitmap only for optical measurement. Build 406 added a native `_tint` sibling only for the custom/battery-color branch. Neither closed parity.

**Current root-cause interpretation**

Build 403 was compensating a mismatch in the final presentation/coverage boundary. The remaining issue is broader than custom recolor and narrower than Home layout/geometry. The next native authority to inspect is the already-rendered Home Wi-Fi ImageView after HyperOS has applied its own resource transformation, tint list/mode, drawable alpha and ImageView alpha.

### Selected investigation direction

Prefer reusing the existing `MiuiWifiViewBinder` / Wi-Fi emitter hook lifecycle:
- it already exposes the bound native `ImageView`;
- after `chain.proceed()`, HyperOS has applied the current final drawable/tint presentation;
- the native Wi-Fi View remains alive even when Combined Status masks its pixels, so its presentation can remain authoritative;
- capture only read-only final presentation data and create a module-owned Drawable clone before rendering;
- do not mutate the live native ImageView/Drawable;
- do not add a second Light/Dark/Tint state machine, polling loop or extra lifecycle owner.

### Review boundary

- **Ownership:** native ImageView remains SystemUI-owned; Combined Status may mirror only a module-owned clone.
- **Lifecycle:** reuse the existing Wi-Fi binder/emitter hook instead of installing a new observer.
- **Single writer:** no native View tint/alpha/resource write.
- **Cleanup:** any mirrored Drawable state must stay bounded to the existing runtime/painter lifecycle and Hot Reload cleanup.
- **Fail native:** if final presentation cannot be captured safely, preserve the existing native/fallback behavior.
- **Performance:** update only on existing Wi-Fi presentation events; no per-frame reflection/tree traversal.
- **Compatibility:** use exact-target verified binder/ImageView contracts and degrade safely.
- **Exception recovery:** capture/clone failure must not affect the native Wi-Fi pipeline.
- **Future extension:** a generic final-native-presentation seam may later support other center resources, but the next A/B should remain Wi-Fi-focused.

### Gate

No runtime change is accepted yet from this correction. Complete the final-native-presentation review first, then implement one bounded next A/B and stop for signed-Canary device validation.


### Root-cause refinement before Build 407

A direct source review of the Build-406 Painter found a simpler contract mismatch than the provisional live-ImageView mirroring direction:

- `drawNativeCenterResource(...)` unconditionally calls `drawable.setTint(tint)` for native center assets;
- this is true in both the normal center-color path and the optional battery-follow path;
- therefore Combined Status is effectively always using an **externally tinted native-center presentation**;
- exact-target HyperOS evidence already establishes that the external-tint branch first transforms the semantic resource to its `*_tint` presentation variant, whose authored mask is intended for ImageView tinting;
- Build 406 selected that `_tint` sibling only when `centerFollowsBatteryColor=true`, leaving the default externally-tinted path on the raw authored resource.

This explains the new device evidence without reintroducing percentile normalization:
- raw authored resources can retain their own alpha/coverage when `setTint` is applied;
- the project battery ring is painted at the resolved tint's full active alpha;
- Build 403's source-alpha normalization artificially removed much of that residual authored-alpha difference, which is why it could look more uniform even though its bitmap pipeline was not the native mechanism.

**Selected Build-407 boundary:** use the verified native `_tint` mask for every native center resource that Combined Status externally tints. This is narrower than mirroring a live ImageView, requires no new Hook/lifecycle owner, and directly matches the existing Painter's presentation mode.

The live native-ImageView presentation mirror remains a fallback investigation route only if this exact contract correction still fails on device.


---

## 2026-09-27 — Build 407: apply native tint-mask contract to every tinted center glyph

**Type:** bounded root-cause correction / source review
**Display version:** 0.0.2
**Build:** 407 / 20260927-407
**Runtime source:** `ffe746b24252f974db05e1fa4381ed5c56f0e73e`
**Validation:** source review complete; signed Canary pending

### Problem / objective

Build 406 proved that selecting the native `_tint` mask only for the optional battery-follow color branch was insufficient because the default center branch remained optically light.

### Root cause

Source review establishes that `drawNativeCenterResource(...)` always applies `Drawable.setTint(tint)`, regardless of the source of `tint`.

Therefore both:
- default center color from resolved native status-icon tint; and
- optional battery-follow/custom semantic color

are **external-tint presentation paths**.

Exact-target HyperOS uses the `*_tint` presentation resource for its external-tint branch. Applying `setTint` directly to a raw authored semantic resource preserves that resource's authored alpha/coverage and can make the compact center look lighter than the fully opaque project-painted outer ring.

### Implementation

- Removed the Build-406-only `centerUsesBatteryTint` presentation flag.
- `drawNativeCenterResource(...)` now always resolves `resolveNativeTintVariant(resource) ?: resource` before loading/caching the native center asset.
- The already-authoritative resolved center tint is then applied exactly once through `Drawable.setTint(...)`.
- Resource resolution remains cached and fail-soft.
- Added/retained deterministic tests for base, darkmode, existing-tint, unavailable and hotspot naming.
- Build identity advanced to `20260927-407`.

### Intentionally unchanged

- no new Hook/listener/observer/polling;
- no live native View mutation;
- no center size/position or optical-probe change;
- no percentile/source-alpha normalization;
- no ring/mobile geometry change;
- no battery semantic-state/color change;
- no Home carrier/spacing/suppression change;
- no Phase-2B scene change.

### Review

- **Ownership:** SystemUI remains semantic-resource/tint authority; Combined Status owns only its cloned center Drawable.
- **Lifecycle:** unchanged; no new owner.
- **Single writer:** one tint writer on the module-owned clone.
- **Cleanup:** existing bounded resource/asset caches only.
- **Fail native:** missing `_tint` sibling falls back to the existing resource path; no guessed color/resource.
- **Performance:** one cached resource-name lookup per native resource identity; no per-frame reflection.
- **Compatibility:** uses the already-verified HyperOS tint-resource naming contract and degrades safely.
- **Exception recovery:** guarded resource resolution remains local to the center path.
- **Future extension:** custom semantic colors and default status-icon colors now share one presentation mechanism rather than branching into divergent alpha behavior.

### Validation gate

Run trusted signed Canary for this exact runtime source. After the artifact exists, freeze runtime and test:
1. center-follow-battery **off**, light surface;
2. center-follow-battery **off**, dark surface;
3. center-follow-battery **on**, light surface;
4. center-follow-battery **on**, dark surface;
5. one available semantic color transition.

No further runtime change before those device results.


---

## 2026-09-27 — Build 407: use native tint masks for every externally tinted center glyph

**Type:** bounded root-cause correction / source review
**Display version:** 0.0.2
**Build:** 407 / 20260927-407
**Implementation commit:** `bbb421ef0354ad60e7d046e38c643d16d61504c7`
**Final executable source:** `ffe746b24252f974db05e1fa4381ed5c56f0e73e`
**CI / device validation:** pending

### Problem / objective

Build 406 incorrectly tied HyperOS `_tint` resource selection to whether the center color came from the battery semantic-color option. Device evidence shows the default/unlinked branch is also visually underweight.

### Root cause

`CombinedStatusPainter.drawNativeCenterResource(...)` applies `Drawable.setTint(centerTint)` for native center assets in **all** color-source modes. Therefore the renderer's presentation mode is externally tinted regardless of whether `centerTint` comes from:
- native/status-icon tint; or
- battery semantic color.

Exact-target HyperOS evidence establishes that the externally tinted branch uses the dedicated `*_tint` presentation resource. Build 406 only selected that mask in one color-source mode, leaving the default branch on the raw semantic resource and preserving its authored alpha/coverage.

### Implementation

- Remove the Build-406-only `centerUsesBatteryTint` / `forceNativeTintVariant` presentation flag.
- Every native center resource entering the existing external-tint draw path now attempts to resolve the verified SystemUI `_tint` sibling first.
- Continue applying the already-authoritative resolved center tint through the existing module-owned Drawable clone.
- Keep resource-ID lookup cached.
- If no verified sibling exists, fall back to the original resource path.
- Build identity advances to `20260927-407`.
- Commit `ffe746b...` only fixes indentation introduced by the functional commit and does not change runtime semantics.

### Intentionally unchanged

- Build-405 direct final-bounds Drawable rendering;
- measurement-only optical raster probe;
- center size/position and pixel-aligned final bounds;
- outer ring/mobile geometry;
- battery semantic-state/color authority and user link behavior;
- Home overlay/carrier/spacing ownership;
- native suppression and scene behavior;
- no Build-403 percentile source-alpha normalization;
- no alpha multiplier, gray constant, asset edit or screenshot-derived tuning.

### Review

- **Ownership:** HyperOS remains semantic-resource/tint authority; Combined Status only selects a verified presentation sibling and mutates its own Drawable clone.
- **Lifecycle:** no new Hook, observer, listener, coroutine or lifecycle owner.
- **Single writer:** one existing Drawable tint writer; the removed boolean gate reduces presentation branching.
- **Cleanup:** existing painter-scoped bounded resource caches only.
- **Fail native:** missing/unresolvable `_tint` sibling falls back to the prior resource without affecting SystemUI.
- **Performance:** no additional steady-state work beyond existing cached lookup; no polling/per-frame reflection.
- **Compatibility:** suffix normalization is limited to the exact verified SystemUI resource family and is existence-checked.
- **Exception recovery:** package/resource lookup remains guarded.
- **Future extension:** rendering mode is now separated from color-source policy, so future custom colors reuse the same native tint-mask seam.

### Validation gate

Run signed Canary for the exact Build-407 executable source, then freeze runtime.

Device test must explicitly compare:
1. center-color link OFF on a light surface;
2. center-color link ON on the same light surface;
3. at least one dark surface;
4. unchanged center geometry, outer geometry and Home spacing;
5. semantic-color transition still recolors linked center correctly.

If optical parity still fails, the next investigation returns to the final native Wi-Fi ImageView presentation after HyperOS applies its own transform/tint state. Do not restore Build-403 percentile normalization without that evidence.


### Build 407 CI / Canary result

- First owner-only fallback run: Work Branch Canary #336 / run `36313792930`.
  - It passed source/checkout/wrapper/Java/API/signing/target-profile setup.
  - It was **cancelled during Test and build Canary because a newer Canary run (#337) superseded it**. This is not a compile/test failure.
- Effective validation run: Work Branch Canary **#337** / run `36313837646`.
- Result: **success**.
- Passed gates:
  - trusted source resolution and exact tested-work-branch checkout;
  - Gradle Wrapper validation;
  - Java / Android API 37 setup;
  - Haple signing restore/verification;
  - pinned HyperOS target-profile verification;
  - tests and Canary build;
  - modern Xposed metadata verification;
  - Haple APK signature verification;
  - non-debuggable verification;
  - artifact upload and final summary.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-407-canary.apk`
- GitHub artifact ID: `10929728522`.
- Extracted APK SHA-256: `b3bf76727f1fa5defe0b76d71fb090b5135ab713e9629260a12ec03b3867824a`.
- Extracted APK size: `3293218` bytes.
- Runtime source remains `ffe746b24252f974db05e1fa4381ed5c56f0e73e`; subsequent commits are documentation-only.

### Gate

**Runtime is frozen at Build 407.** The next required evidence is maintainer device A/B with center-follow-battery both disabled and enabled across light/dark surfaces. No Phase-2B or further center-rendering runtime change should be layered before that result.


### Build 407 CI / Canary result

- Work Branch Canary: **#337** / run ID `36313837646`.
- Result: **success**.
- Earlier duplicate Canary #336 was cancelled by workflow concurrency after #337 superseded it; this is not a test failure.
- Passed gates:
  - trusted PR source resolution;
  - exact checked-out source verification;
  - Gradle Wrapper validation;
  - Java / Android API 37 setup;
  - Haple signing restore/verification;
  - pinned HyperOS target-profile verification;
  - tests and Canary build;
  - modern Xposed metadata verification;
  - Haple APK signature verification;
  - non-debuggable verification;
  - artifact preparation/upload.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-407-canary.apk`
- GitHub artifact ID: `10929728522`
- Extracted APK size: `3293218` bytes
- Extracted APK SHA-256: `b3bf76727f1fa5defe0b76d71fb090b5135ab713e9629260a12ec03b3867824a`
- Final executable source remains `ffe746b24252f974db05e1fa4381ed5c56f0e73e`; subsequent commits are documentation-only.

### Gate

**Runtime is frozen at Build 407.** The next required evidence is maintainer device A/B with center-color link both OFF and ON. No Phase-2B, alpha normalization, geometry tuning or additional runtime change should be layered before that result.


---

## 2026-09-27 — Build 407 device result: opacity fixed; residual ring optical-weight mismatch

**Type:** device acceptance correction / visual root-cause refinement
**Build:** 407 / 20260927-407
**Runtime source:** `ffe746b24252f974db05e1fa4381ed5c56f0e73e`
**Result:** native-center opacity correction accepted; overall optical balance still open

### Device evidence

The maintainer reports that the center transparency defect is resolved in Build 407.

New same-device screenshots in the charging-green state still make the battery ring appear darker/heavier than the native Wi-Fi center and the four mobile dots.

### Quantitative interpretation

Same-image sampling was used only to distinguish tint mismatch from coverage mismatch.

On both light and dark screenshots:
- high-coverage ring and center core pixels converge to essentially the same green RGB;
- the ring's overall pixel distribution is darker/more saturated because a wide continuous stroke creates a larger proportion of fully covered pixels;
- the native center mask contains more antialiased edge coverage;
- the small circular mobile dots contain still more edge coverage at their final physical size.

Therefore the remaining defect is **optical weight / raster coverage**, not a second semantic-color or tint-source defect. The sampled values are evidence only and must not become rendering constants.

### Problem execution flow

**Phenomenon and evidence -> responsibility source**

1. Build 407 fixed the raw-resource/tint-mask responsibility and the reported center-transparency problem.
2. Source review confirms active battery ring, linked center and active dots all receive the same resolved tint and full semantic alpha.
3. The ring is a project-owned continuous `Paint.Style.STROKE`; the dots are small project-owned fills; the center is the authoritative HyperOS vector mask.
4. Current outer geometry still carries the Build-332 1.10x shared boldening. The ring therefore remains a wider continuous custom element even though the center should now be treated as native visual authority.

**Repository / historical basis**

Build 332 changed both the original 7.5 canonical ring stroke and 4.9 dot radius through a shared 1.10x default scale. The user accepted the bolder outer geometry at that stage. After native center presentation was corrected in Build 407, the continuous ring now reads optically dominant while the existing dot size remains in the same perceived-weight band as the center.

### Alternatives reviewed

1. Darken center/dots or lighten the ring color — rejected; the actual resolved tint is already shared.
2. Add per-element alpha multipliers — rejected as a visual compensation layer with no native authority.
3. Reintroduce Build-403 alpha normalization — rejected; Build 407 already fixed the native center resource path and the residual issue is broader geometry/coverage.
4. Enlarge the native center — rejected; HyperOS native glyph geometry is now the stronger visual authority.
5. Enlarge dots — rejected for the first A/B; they already read close to the center and their current size was previously accepted.
6. Reduce only the custom ring to the known pre-Build-332 base stroke while keeping dot size stable — selected as the smallest attributable A/B.

### Selected Build-408 boundary

Rebase the outer geometry so the current default resolves to:
- battery ring stroke: historical base 7.5 canonical units;
- mobile dot radius: current accepted 5.39 canonical units;
- shared future weight scale: 1.0 at this newly balanced default, scaling both values proportionally from there.

This preserves a single future thickness-control seam without retaining the assumption that ring and dot require the same historical 1.10 calibration.

### Review

- **Ownership:** only project-owned geometry changes; native center/SystemUI geometry is untouched.
- **Lifecycle:** no new observer/hook/listener.
- **Single writer:** painter geometry remains the only writer.
- **Cleanup:** no new runtime state.
- **Fail native:** unaffected.
- **Performance:** arithmetic/constants only; no new allocation, raster pass or callback.
- **Compatibility:** removes reliance on screenshot/device-specific RGB/alpha compensation.
- **Exception recovery:** unaffected.
- **Future extension:** a future user thickness control can still scale the balanced ring/dot baseline proportionally.

### Gate

Implement one Build-408 geometry-only A/B, run CI/Canary, then stop for device comparison. Do not mix shade/Control Center, tint, alpha or center-resource work into the same candidate.


---

## 2026-09-27 — Build 408: ring-only optical baseline rebalance

**Type:** bounded visual-geometry A/B / source review
**Display version:** 0.0.2
**Build:** 408 / 20260927-408
**Runtime source:** `8a7a39d8297fe926387d56cc8ff5be4b08405f4a`
**Validation:** source review passed; CI/Canary pending

### Objective

Test the residual Build-407 visual mismatch without changing color authority or alpha. Same-image evidence shows ring and center core tint is already effectively identical; the ring appears darker because the project-owned continuous stroke has more full-coverage pixels.

### Implementation

Rebase `CombinedStatusOuterGeometry` default dimensions:
- ring stroke: 7.5 canonical units (historical pre-Build-332 base);
- mobile dot radius: preserve the Build-407 default exactly at `4.9 * 1.10 = 5.39`;
- unavailable-mark stroke/extent: preserve Build-407 defaults exactly;
- default outer weight scale: rebase from 1.10 to 1.00;
- future weight scaling still multiplies ring, dots and unavailable mark proportionally from the new default baseline.

The lower-opening gap solver remains unchanged and recomputes spacing from the resolved ring/dot geometry.

### Intentionally unchanged

- battery/center/mobile tint values;
- active/inactive semantic alpha values;
- native center `_tint` resource selection;
- native center size and optical bounds;
- mobile dot physical default size;
- unavailable-mark physical default size;
- battery arc angles/sweep;
- Home carrier/spacing/suppression;
- scene lifecycle and Phase-2B behavior.

### Tests

Added a deterministic default-baseline test asserting:
- ring resolves to 7.5;
- dot radius remains 5.39;
- unavailable-mark dimensions remain Build-407-equivalent.

Existing tests continue to require proportional scaling, mirror symmetry, balanced five-edge gaps and safe scale clamping.

### Review

- **Ownership:** only module-owned outer geometry changes; native resources/Views remain untouched.
- **Lifecycle:** no change.
- **Single writer:** unchanged painter-only geometry.
- **Cleanup:** no change.
- **Fail native:** no change.
- **Performance:** constants plus the existing bounded geometry solver only.
- **Compatibility:** no target-specific RGB/alpha/physical-pixel constants are introduced.
- **Exception recovery:** no change.
- **Future extension:** one outer weight scale remains available for a future user-facing thickness control while the default baseline can carry an optical ring/dot calibration.

### Gate

Run CI and signed Canary, then freeze runtime for a Build-407 vs Build-408 device A/B. Do not mix any further center-resource, tint, alpha or Phase-2B change into the same candidate.


### Build 408 CI / Canary result

- Work Branch Canary: **#338** / run ID `36315043013`.
- Result: **success**.
- Passed gates:
  - trusted PR source resolution and exact checkout verification;
  - Gradle Wrapper validation;
  - Java / Android API 37 setup;
  - Haple signing restore/verification;
  - pinned HyperOS target-profile verification;
  - tests and Canary build;
  - Modern Xposed metadata verification;
  - Haple APK signature verification;
  - non-debuggable verification;
  - artifact preparation/upload and final summary.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-408-canary.apk`
- GitHub artifact ID: `10930143406`
- Extracted APK size: `3293218` bytes
- Extracted APK SHA-256: `2a5ed37e52ffcae71139bf006e025559192fb3ed3e15e089bc3841ba6bf3efb3`
- Runtime source remains `8a7a39d8297fe926387d56cc8ff5be4b08405f4a`; later commits are documentation-only.

### Gate

**Runtime is frozen at Build 408.** The next required evidence is a Build-407 vs Build-408 device A/B focused on ring optical weight. No tint/alpha compensation, center-resource change or Phase-2B runtime work should be layered before that result.

---

## 2026-09-27 — Build 408 maintainer acceptance for dev integration

**Type:** maintainer acceptance / phase gate / integration authorization
**Display version:** 0.0.2
**Accepted runtime source:** `8a7a39d8297fe926387d56cc8ff5be4b08405f4a`
**Validated Canary:** Work Branch Canary #338 / run `36315043013` / artifact `10930143406`

### Maintainer decision

The maintainer accepts the current color/native-center result for integration into `dev`.

The result is considered **basically compliant with the intended requirement**, with one explicitly recorded limitation: a small residual optical-weight difference between the battery ring, native center and four mobile dots may still be visible.

This acceptance does **not** claim perfect pixel/optical parity. The residual difference is deferred visual polish and is no longer a Phase-2A integration blocker.

### Durable accepted conclusions

- HyperOS remains the semantic battery-state and built-in color authority.
- Build 407's use of the verified native `_tint` presentation sibling for externally tinted native center resources resolves the previously blocking center-opacity defect.
- Build 408 is the accepted working outer-geometry baseline carried into `dev`.
- No per-glyph gray/RGB multiplier, alpha compensation, percentile normalization, source-asset edit or screenshot-derived constant is accepted.
- The current Home existing-host carrier, reversible native masking/reservation, charging/Super-Island ownership and cleanup/fail-native boundaries remain the accepted Phase-2A architecture.
- The slight remaining optical-weight difference may be revisited under later visual/thickness controls without blocking scene-transition work.

### Phase consequence

Phase 2A is accepted for the current `dev` integration baseline. Phase 2B becomes active next, beginning with the shallow notification-shade pull / held-return Home-overlay leak.

The Phase-2B investigation must start from native scene/progress ownership and must not reopen steady Home geometry or introduce threshold/delay/translation compensation as a first response.

---

## 2026-09-27 — Phase 2A merged to dev and Integration validation passed

**Type:** dev integration / CI validation / phase transition
**Display version:** 0.0.2
**Dev merge:** `2f584c3b393dc5ee606284426aa95a9d6beae5d5`
**Integrated build:** 408 / 20260927-408

### Integration

- PR #105 was history-synchronized with the latest `dev` governance, retargeted to `dev`, passed Fast Build #1104, and was squash-merged after explicit maintainer acceptance.
- PR #100 was closed as superseded because #105 carried the final accepted Phase-2A Home architecture plus the semantic-color/native-center work into the single integration boundary.
- Build 408 remains the accepted working runtime baseline; the minor ring/center/dot optical-weight difference remains deferred polish and is not promoted as perfect parity.

### Dev Integration CI

- Build workflow: **#1105** / run `36316290255`.
- Result: **success**.
- Scope: trusted `dev` Integration.
- Passed target-profile verification, unit tests, signed Canary assembly, Modern Xposed metadata, Haple signature validation, non-debuggable verification, and artifact publication.
- Canary artifact ID: `10930783210`.
- Canary APK size: `3293218` bytes.
- Canary APK SHA-256: `44d6b6295b17c016c8af5ddcb28c44a5ac7e37b14a66e44d438f4e2f8ae40a52`.

### Review / phase consequence

- **Ownership:** accepted existing-host Home architecture remains unchanged by integration.
- **Lifecycle / cleanup / single writer / fail-native:** accepted Phase-2A boundaries remain intact.
- **Performance / compatibility:** no new runtime mechanism was introduced during integration/back-sync.
- **Residual:** slight outer optical-weight variance is deferred and does not block subsequent scene work.
- **Next:** Phase 2B is active. Start from a new work branch based on this dev baseline and address the shallow shade-pull Home-overlay leak from native scene/progress ownership rather than geometry or timing compensation.

---

## 2026-09-27 — Phase 2B root-cause review: Home overlay ignores native shade lifetime

**Type:** scene-boundary root-cause review / pre-runtime checkpoint
**Display version:** 0.0.2
**Baseline:** dev Build 408 / `2f584c3b393dc5ee606284426aa95a9d6beae5d5`
**Work branch:** `fix/shade-home-overlay-leak`

### Problem execution flow

**Phenomenon and evidence**

A shallow notification-shade pull and the final held-return frame can leave the Home Combined Status representation visible even though the notification panel has begun owning presentation.

**Root cause / responsibility source**

- `SystemUiSceneStateSource` classifies the Battery view from `mStatusBarState`; raw `SHADE(0)` maps to `UNLOCKED_STATUS_BAR` and allows Home overlay.
- That status-bar mode is not a panel-expansion lifecycle. It can remain `SHADE(0)` while the notification panel opens/closes.
- `SystemUiPanelTransitionSource` already hooks the exact target `ShadeExpansionStateManager.onPanelExpansionChanged(float, boolean, boolean)` and receives native `fraction`, `expanded`, and `tracking`.
- `CombinedStatusModule.onPanelTransitionUpdate()` currently returns immediately when Detailed diagnostics are disabled. When enabled, it only records bounded diagnostic buckets / native transition snapshots.
- `CombinedStatusHomeRenderSession` receives no panel-transition eligibility fact. Its overlay visibility and readiness therefore remain true as long as the static surface stays `UNLOCKED_STATUS_BAR`.

**Root-cause status:** confirmed at source level. The native panel fact exists and is already hooked; the missing responsibility is routing that fact into Home presentation eligibility.

### Platform / reference evidence

- Android SystemUI documents `expanded` as independent from numeric fraction and `tracking` as active user gesture ownership.
- The native shade state manager considers the panel closed only after it is not expanded and tracking has ended.
- This explains the observed held-return case: a zero/near-zero fraction alone cannot establish Home ownership.
- Existing Combined Status reference policy already requires native progress/scene ownership and forbids first/last-frame offset compensation or a second animation system.

### Alternatives reviewed

1. Hide Home when `fraction > 0.01` (or another epsilon) — rejected as a project-owned magic threshold and fails the zero-fraction-but-still-expanded/tracking case.
2. Delay hide/show around touch release — rejected as lifecycle compensation and race-prone.
3. Follow panel translation each frame — rejected; SystemUI owns motion and the immediate defect is eligibility, not geometry.
4. Implement full shade/Control Center Combined Status projection now — rejected for this first fix because it mixes a new surface contract into a confirmed Home-leak correction.
5. Route native `expanded/tracking` into Home eligibility and fail native during notification transition — selected.

### Selected first runtime boundary

- Notification shade stays `NATIVE_ONLY`.
- Home notification-panel eligibility is true only for native `expanded=false && tracking=false`.
- Any native update reporting `expanded=true` or `tracking=true` makes Home presentation not ready; existing presentation-cutover cleanup restores native status visuals.
- Keep `fraction` read-only for diagnostics and later draw-only projection.
- Do not change Control Center behavior in the same Build.

### Review

- **Ownership:** SystemUI `ShadeExpansionStateManager` remains the scene/gesture authority; Combined Status only consumes its facts.
- **Lifecycle:** reuse the three already-installed panel hooks; no new Hook, listener, polling owner or frame callback.
- **Single writer:** visibility/readiness remains owned by the existing Home RenderSession + presentation cutover path.
- **Cleanup:** losing Home readiness uses the existing reversible suppression/reservation cleanup; no new mutable native state.
- **Fail native:** shade transition explicitly restores native presentation rather than trying to render an unverified shade representation.
- **Performance:** event-driven callback already exists; the new path is boolean state/recompute only.
- **Compatibility:** exact target hook contract is already installed and target-profile validated.
- **Exception recovery:** invalid/missing panel payload should not invent a transition; existing static scene/fail-native rules remain.
- **Future extension:** fraction/endpoints remain available for a later projection layer without coupling steady Home lifetime to animation geometry.

### Next

Implement a pure notification-shade eligibility policy plus Home RenderSession routing, advance one runtime Build, run CI/Canary, and stop for focused device validation.


### Phase 2B implementation review refinement — Hot Reload scene continuity

Before promoting the shade callback from diagnostics into runtime eligibility, the Hot Reload path was reviewed.

Finding:
- cold process start may safely retain the existing Home-default behavior until the first native panel callback because there is no pre-existing user gesture from an older SystemUI generation;
- Hot Reload is different: the classloader/source object is replaced while the existing SystemUI scene can already be mid-shade;
- the current source-local `notificationShadeHomeEligible` snapshot would otherwise reset to unknown and the Home session currently treats unknown as the stable-Home default;
- therefore a Hot Reload performed while shade is expanded/tracking could briefly re-enable the Home replacement before another native panel callback arrives.

Selected correction:
- extend the existing classloader-neutral Hot Reload payload with one nullable Boolean: the last known notification-shade Home eligibility;
- restore that value into the new `SystemUiPanelTransitionSource` before `attachHostRuntime(...)`;
- preserve null for old/unknown payloads and retain the existing cold-start default only in that unknown case;
- do not reflect private ShadeExpansionStateManager fields, poll state, or add another observer.

Review:
- **ownership:** SystemUI callback remains the authority; transferred state is only the last observed native fact;
- **lifecycle:** the value lives with the existing panel source and existing Hot Reload transfer lifecycle;
- **single writer:** only the native notification-shade callback updates the live eligibility; Hot Reload restore seeds it once;
- **cleanup:** panel source reset clears the snapshot; no listener/state survives beyond its owner;
- **fail-native:** malformed callback semantics resolve Home-ineligible; unknown transfer stays on the existing cold-start-compatible behavior;
- **performance:** one Boolean snapshot, no additional callback or polling;
- **compatibility:** no new reflection/private-field dependency;
- **future extension:** Control Center remains separate and diagnostics-only in this checkpoint.


---

## 2026-09-27 — Build 409 source review: native notification-shade Home eligibility

**Type:** Phase-2B runtime checkpoint / source review
**Display version:** 0.0.2
**Build:** 409 / 20260927-409
**Runtime source:** `08574da0abcf192ff59b0eb8fa94c818ca1221b9`
**Validation:** source review passed; CI/Canary and device acceptance pending

### Runtime delta

- Promote exactly one `ShadeExpansionStateManager.onPanelExpansionChanged(FZZ)V` hook from diagnostics-only use into the production runtime.
- Keep the two Control Center expansion/visibility hooks diagnostics-only.
- Derive Home notification-shade eligibility only from native `expanded=false && tracking=false`.
- Route the eligibility into the existing Home render/readiness gate.
- Keep fraction as read-only diagnostic/future projection progress; no numeric threshold is used.
- Extend the classloader-neutral Hot Reload payload from v4 to v5 with one nullable Boolean containing the last observed native shade eligibility.
- Restore that snapshot before the new-generation Home runtime attaches.
- Promote the exact ShadeExpansionStateManager method into the pinned HyperOS compatibility profile.
- Advance build identity to `20260927-409`.

### Review

- **Ownership:** SystemUI shade state manager remains scene/gesture authority; Combined Status consumes only native callback facts.
- **Lifecycle:** one event-driven production callback; no new observer, timer or polling loop.
- **Single writer:** the live callback owns eligibility updates; Hot Reload restoration is a one-time generation seed.
- **Cleanup:** source reset clears the snapshot and existing Home presentation cleanup restores only module-owned masks/reservation.
- **Fail native:** malformed/missing expanded/tracking values resolve Home-ineligible; unsupported surfaces remain native.
- **Performance:** Boolean comparison + existing readiness transition only; no per-frame reflection or custom animation.
- **Compatibility:** runtime dependency is now declared in the exact-target profile and SystemUI-Reference contract index.
- **Exception recovery:** existing panel install failure path leaves Home replacement unable to rely on an unverified transition source rather than introducing fallback timing logic.
- **Future extension:** native fraction remains available for a later real Home-to-shade projection without coupling this leak fix to geometry or animation.

### Intentionally unchanged

- Control Center presentation/projection;
- shade Combined Status rendering;
- steady Home carrier, masks, reservation and island inheritance;
- Battery semantic-color policy and Build-408 visual baseline;
- deferred ring/center/dot optical-weight polish;
- keyguard/AOD behavior.

### Validation gate

Fast CI + signed Canary are required before device testing. Once a signed Build-409 Canary exists, runtime must freeze for the focused shade-lifetime scenarios.


### Build 409 pre-CI correction

Repository-latest runtime review found three commits after the first Build-409 implementation checkpoint:

- `a63565f47700148fa349e30dce8d1062c480c746`: if the required notification-shade runtime Hook cannot be installed, mark Home shade eligibility false so the feature fails native instead of silently continuing without scene authority.
- `cf8f53233a6a4f358c0840336efb57cf827e94c8`: empty commit; no executable delta.
- `d4ef6e6bc2be143a961e0130f0978e5788c325c4`: move the cold-start Home bootstrap to after successful Shade Hook registration and remove the transient duplicate restore helper.

Review conclusion:
- these changes tighten fail-native/bootstrap semantics without adding another state source or writer;
- Build identity remains 409 because no validated/issued Build-409 Canary existed before these corrections;
- **final executable source for Build 409 is `d4ef6e6bc2be143a961e0130f0978e5788c325c4`**;
- earlier `08574da...` is an intermediate unvalidated Build-409 source and must not be used for device acceptance.


### Build 409 source correction after review hardening

The earlier Build-409 source-review note preceded the final Fail-native and Hot Reload hardening. Historical checkpoints remain unchanged; the **final executable Build-409 source is now `4ab7b490617757e34c0ea8e0b59e3d7a16bae9ad`**.

Final review additions:
- unknown shade authority no longer defaults to Home-eligible;
- successful authority installation provides the steady-Home bootstrap, while installation failure leaves Home native;
- current shade eligibility is transferred in the version-5 Hot Reload payload and restored before the Home host session is attached;
- version-4/older payloads remain readable; because 408 could not have stored shade eligibility, a 408 -> 409 reload preserves the successfully installed 409 bootstrap until the next native callback;
- duplicate/incorrectly inserted helper code found during source review was removed before CI.

No additional visual, geometry, color, Control Center, or animation behavior was added by this hardening.


---

## 2026-09-27 — Build 409 CI correction: stale test call sites

**Type:** CI failure / test-maintenance correction
**Runtime source:** `4ab7b490617757e34c0ea8e0b59e3d7a16bae9ad`
**Runtime conclusion:** unchanged

### CI evidence

- Draft Build #1122 / run `36317771794` failed during lightweight repository checks because newly added DEVLOG lines contained trailing whitespace. No Android compile step ran. The whitespace was removed in a documentation-only commit.
- Ready Fast Build #1123 / run `36317790493` correctly classified the PR as **Fast** and passed Gradle Wrapper, Java/API setup, and the pinned HyperOS target-profile check.
- Build #1123 then reached Android compilation. Production `compileDebugKotlin` **succeeded**.
- `compileDebugUnitTestKotlin` failed at five existing assertions in `SystemUiNativeCombinedParticipantOwnerTest.kt` because `CombinedStatusHomeRenderSession.resolveOverlayVisible(...)` now requires the additional `notificationShadeAllowsHome` argument.
- Build #1124 / run `36317833217` was created from the whitespace-cleaned head before that test-call-site correction and completed failure from the same pre-fix branch state.

### Root cause

The Phase-2B runtime change intentionally extended the pure visibility policy function with a fourth gate. New dedicated shade-policy tests were added, but five older tests for the feature/scene/native-handoff gates still called the previous three-argument signature.

This is a test-maintenance omission, not a production compile or runtime design failure.

### Selected correction

Update only those five legacy assertions with `notificationShadeAllowsHome=true`.

That preserves their original purpose:
- master switch false still blocks;
- scene false still blocks;
- native handoff true still blocks;
- the shade gate is held neutral/allowing in those pre-existing tests.

No runtime source, Build identity, scene policy, Hook, state source, geometry, color, or lifecycle behavior changes.

### Gate

Re-run Fast CI after the test-only correction. Build 409 remains the same runtime candidate and must still receive a signed Canary before device validation.


---

## 2026-09-27 — Build 409 Fast #1127: one stale Hot Reload assertion

**Type:** CI failure / test-semantics correction
**Fast run:** #1127 / `36318037749`
**Runtime source:** `4ab7b490617757e34c0ea8e0b59e3d7a16bae9ad`
**Runtime conclusion:** unchanged

### Evidence

Fast #1127 passed:
- Fast scope classification;
- Gradle Wrapper / Java / API 37 setup;
- pinned HyperOS target-profile verification, including the promoted Shade callback contract;
- production Debug Kotlin compilation;
- unit-test Kotlin compilation.

The test task executed **239 tests; 238 passed and 1 failed**:
`SystemUiPanelTransitionSourceTest.notificationShadeEligibilitySnapshotCanSeedHotReloadGeneration`.

The failure is at the final assertion after:
`restoreNotificationShadeHomeEligibility(null)`.

### Root cause

Final Build-409 hardening deliberately changed restore semantics so `null` means “this payload contains no shade eligibility fact” and therefore **does not overwrite** the current source state.

That matters for 408/v4 -> 409/v5 Hot Reload:
- the new generation successfully installs the required Shade Hook and establishes its conservative steady-Home bootstrap;
- an older payload cannot contain shade eligibility;
- restoring its null value must not erase the newly-established bootstrap.

The failing test still expected the earlier intermediate behavior where null cleared the snapshot. The assertion is stale; the production implementation matches the documented compatibility boundary.

### Selected correction

Test-only:
- retain reset -> null;
- verify false restores false;
- verify true restores true;
- verify a subsequent null restore preserves true.

No runtime, Hook, transfer format, Build identity, color, geometry or scene-policy change is required.

### Gate

Re-run Fast CI after the assertion correction. A signed Canary remains required before device validation.


---

## 2026-09-27 — Build 409 Fast + signed Canary success

**Type:** validation success / device-test gate
**Build:** 409 / 20260927-409
**Executable runtime source:** `4ab7b490617757e34c0ea8e0b59e3d7a16bae9ad`
**Tested PR head:** `e7f2ca9a104875688fd2b91640ec84f43c8b228a`

### Fast validation

- Build workflow: **#1129** / run `36318210235`.
- Result: **success**.
- Scope: Fast / ordinary product-runtime PR.
- Passed Gradle Wrapper, Java/API 37 setup, pinned HyperOS target-profile verification, all unit tests, Debug assembly, built-APK resolution and Modern Xposed metadata validation.
- Earlier #1122/#1123/#1124/#1127 failures are retained as historical validation evidence and were closed by documentation/test-only corrections; no runtime source change followed `4ab7b490...`.

### Signed Work Branch Canary

- Work Branch Canary: **#363** / run `36318369493`.
- Result: **success**.
- Trusted source resolution and exact checked-out source verification passed for PR #138 head `e7f2ca9a104875688fd2b91640ec84f43c8b228a`.
- Passed:
  - Gradle Wrapper;
  - Java / Android API 37;
  - Haple signing restore and verification;
  - pinned HyperOS target-profile verification, including `ShadeExpansionStateManager.onPanelExpansionChanged(FZZ)V`;
  - tests and Canary build;
  - Modern Xposed metadata;
  - Haple APK signature;
  - non-debuggable verification;
  - artifact preparation/upload.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-409-canary.apk`
- GitHub artifact ID: `10931079487`
- Extracted APK size: `3293218` bytes
- Extracted APK SHA-256: `c8d8aabbaeb8f60abfc1897996b45019e7f9d8d130e5055acacf3848164591ba`

### Gate

**Runtime is frozen at Build 409.**

The next required evidence is maintainer device validation of the notification-shade lifetime boundary. Do not add Control Center projection, shade rendering, timing compensation, geometry/color changes, or any other runtime change before that result.


---

## 2026-09-27 — Build 409 device rejection: unresolved path is Control Center

**Type:** device evidence / root-cause correction
**Build under test:** 409 / 20260927-409
**Executable source:** `4ab7b490617757e34c0ea8e0b59e3d7a16bae9ad`
**Result:** notification-shade gate observed working; overall shallow-pull issue not solved

### Evidence

The maintainer supplied:
- a six-second device recording reproducing the visible overlap/leak;
- `CombinedStatus-Diagnostic-20260927-409-20260927-202347.txt`.

The recording shows a transition interval where native Wi-Fi/mobile/battery presentation has already entered while the Home Combined Status remains visible.

The matching diagnostics identify that transition as Control Center:
- `panelTransition source=control-center ... visible=true` begins before fraction progress and persists through the outward/return motion;
- fractions traverse roughly 0.12 -> 0.50 -> 0.12 while `visible=true`;
- the native callback finally emits `visible=false` only after the Control Center transition has settled;
- Build 409 emits no Home readiness/eligibility change for those Control Center updates because both callbacks were deliberately diagnostics-only.

Separately, later notification-shade evidence shows Build 409 can drive:
`homeRenderShadeEligibility ... homeEligible=false` ->
`homeRenderReadiness ... ready=false` ->
`homeRenderHandoff nativeActive=true overlayVisible=false` ->
Home presentation cleanup/restoration.

Therefore the remaining failure is **not** that the Home cleanup transaction ignores a false eligibility. The missing responsibility is the Control Center lifetime fact.

### Root cause / responsibility source

Build 409 modeled:

`Home eligibility = unlocked surface && notification shade settled`

but the actual SystemUI has another independent end-side transition owner:

`ControlCenterExpandControllerDelegate.onVisibleChanged(boolean)`.

Because that fact was diagnostics-only, Control Center could own the status-bar transition while Combined Status still considered Home eligible.

### Selected Build-410 correction

- production runtime authorities:
  1. notification shade semantic CLOSED: `expanded=false && tracking=false`;
  2. Control Center not visible: `visible=false`;
- keep Control Center `onExpansionChanged(float)` diagnostics/read-only only;
- no fraction threshold;
- route both booleans into the existing Home readiness / cleanup transaction;
- Hot Reload transfers both last-known nullable eligibility facts;
- exact-target profile must verify `ControlCenterExpandControllerDelegate.onVisibleChanged(Z)V`;
- if either required runtime authority fails installation, Home Combined Status fails native.

### Review

- **Ownership:** each SystemUI scene callback owns its own lifetime fact; Combined Status composes them, it does not invent progress state.
- **Lifecycle:** one additional existing event callback becomes production-relevant; no observer/poll/timer is added.
- **Single writer:** notification callback writes notification eligibility; Control Center visibility callback writes Control Center eligibility; the Home session only consumes both.
- **Cleanup:** unchanged existing Home cleanup restores exact module-owned mask/reservation state.
- **Fail native:** missing runtime scene authority disables Home replacement rather than leaving a stationary overlay across an unknown transition.
- **Performance:** Boolean event gate only.
- **Compatibility:** exact-target method moves from diagnostic evidence to declared runtime profile contract.
- **Future extension:** Control Center fraction remains available for later draw-only projection without being repurposed as an eligibility threshold.

### Gate

Implement and validate Build 410 before any projection or animation work.


---

## 2026-09-27 — Build 410 source review: compose Control Center visibility into Home ownership

**Type:** Phase-2B runtime checkpoint / source review
**Build:** 410 / 20260927-410
**Runtime source:** `6e2fc55944753c6cb9ef22f537008c97217f17e1`
**Validation:** source/call-site review passed; Fast/Canary pending

### Runtime delta

Build 410 retains the Build-409 notification-shade authority and adds the second native scene-lifetime authority demonstrated by the maintainer's device evidence:

- production hook 1: `ShadeExpansionStateManager.onPanelExpansionChanged(FZZ)V`;
- production hook 2: `ControlCenterExpandControllerDelegate.onVisibleChanged(Z)V`;
- diagnostics-only hook: `ControlCenterExpandControllerDelegate.onExpansionChanged(F)V`.

Home eligibility now requires:
- unlocked Home surface;
- notification shade semantically settled: `expanded=false && tracking=false`;
- Control Center not visible: `visible=false`;
- existing model/tint/layout readiness;
- no native handoff activity.

### Hot Reload

The classloader-neutral payload advances to v6:
- v6 carries notification-shade + Control Center eligibility;
- v5 remains readable and carries only notification-shade eligibility;
- v4 and older remain readable through their established compatibility paths;
- a missing legacy Control Center field does not erase the successfully installed new generation's bootstrap state.

### Exact-target compatibility

The pinned HyperOS profile now verifies:
`com.miui.systemui.controlcenter.container.ControlCenterExpandControllerDelegate.onVisibleChanged(Z)V`.

The matching SystemUI-Reference contract index records the same callback as semantic scene lifetime. `onExpansionChanged(F)V` remains progress evidence only.

### Review

- **Ownership:** SystemUI remains owner of both transition lifetimes; Combined Status composes two native booleans into Home eligibility.
- **Lifecycle:** exactly two production event hooks; no polling/timer/listener layer added.
- **Single writer:** each native callback writes only its corresponding source snapshot; Home Session is the single consumer that resolves presentation readiness.
- **Cleanup:** unchanged Home cleanup transaction restores only module-owned clip/reservation state.
- **Fail native:** install failure marks both required scene authorities Home-ineligible.
- **Performance:** event-driven Boolean gates; Control Center fraction processing remains diagnostics-only.
- **Compatibility:** both production private contracts are fingerprint/profile checked.
- **Exception recovery:** partial hook install is unhooked on failure; Home falls native.
- **Future extension:** progress callbacks remain available for later draw-only projection without contaminating eligibility with thresholds.

### Call-site review

- all legacy `resolveOverlayVisible(...)` test calls explicitly pass the new Control Center gate;
- Home visibility and presentation readiness both require the gate;
- the sole Hot Reload capture call passes both eligibility facts;
- expected hook count is 2 in production and 3 with diagnostics;
- no RGB/alpha/geometry/translation/animation behavior changed.

### Gate

Fast CI and a signed Canary are required before another device test. Runtime changes stop once the signed Build-410 Canary exists.


---

## 2026-09-27 — Build 410 Fast #1141: stale Home-session test call sites

**Type:** CI failure / test-maintenance correction
**Fast run:** #1141 / `36319627573`
**Runtime source:** `6e2fc55944753c6cb9ef22f537008c97217f17e1`
**Runtime conclusion:** unchanged

### CI evidence

Fast #1141:
- correctly classified as Fast;
- passed Gradle Wrapper / Java / API 37 setup;
- passed the pinned HyperOS target-profile verification, including `ControlCenterExpandControllerDelegate.onVisibleChanged(Z)V`;
- production `compileDebugKotlin` succeeded;
- `compileDebugUnitTestKotlin` failed at five pre-existing `CombinedStatusHomeRenderSessionTest.kt` calls to `resolveOverlayVisible(...)` because they did not yet supply the new `controlCenterAllowsHome` parameter.

### Root cause

Build 410 extends the pure Home visibility policy with a second native scene-lifetime gate. The participant-owner tests were updated during source review, but the separate Home-session policy test file contains five additional legacy calls.

Those tests cover pre-existing feature/scene/notification/handoff semantics. They should hold the new Control Center gate neutral/allowing with `controlCenterAllowsHome=true`.

This is a test-maintenance omission, not a production compilation, target-contract or runtime design failure.

### Selected correction

Update only the five legacy policy-test calls with `controlCenterAllowsHome=true`.

No runtime code, Hook contract, Hot Reload payload, build identity, color, geometry, animation or scene policy changes.

### Gate

Re-run Fast CI after the test-only correction. Build 410 remains the same executable candidate and still requires a signed Canary before device validation.


---

## 2026-09-27 — Build 410 Fast + signed Canary success

**Type:** validation success / device-test gate
**Build:** 410 / 20260927-410
**Executable runtime source:** `6e2fc55944753c6cb9ef22f537008c97217f17e1`
**Tested PR head:** `5320bf87253128de290b4b0809694949a02b6c38`

### Fast validation

- Fast Build #1143 / run `36319763905`: **success**.
- Passed Fast scope classification, Gradle Wrapper, Java/API 37, pinned HyperOS target-profile verification, all unit tests, Debug build, APK resolution and Modern Xposed metadata.
- The preceding #1141 failure is retained as historical evidence; it was a test-only stale-call-site omission and did not require a runtime change.

### Signed Work Branch Canary

- Work Branch Canary #377 / run `36319940085`: **success**.
- Trusted source resolution and exact checked-out source verification passed for PR #138 head `5320bf87253128de290b4b0809694949a02b6c38`.
- Passed:
  - Gradle Wrapper;
  - Java / Android API 37;
  - Haple signing restore/verification;
  - pinned notification-shade + Control Center runtime contracts;
  - tests and Canary build;
  - Modern Xposed metadata;
  - Haple APK signature;
  - non-debuggable verification;
  - artifact preparation/upload.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-410-canary.apk`
- Artifact ID: `10932655599`
- Extracted APK size: `3309598` bytes
- Extracted APK SHA-256: `8c3f3011c214e66d20a89698e902191bdd8bc039803a6c824a261170a5cbf0eb`

### Runtime gate

**Runtime is frozen at Build 410 pending maintainer device validation.**

### Deferred visual note

The maintainer additionally reports that the battery ring now feels somewhat thin. This is consistent with Build 408's ring-only reduction from the previous effective 8.25-unit stroke to 7.5 while the accepted mobile-dot size remained unchanged. No visual change is added to Build 410. After Phase-2B scene validation, evaluate a separate ring-only optical-weight A/B rather than coupling thickness to scene-lifetime work.


---

## 2026-09-27 — Build 411 combined checkpoint: persistent Home owner + ring optical rebalance

**Type:** accelerated combined device candidate / root-cause architecture correction / visual A/B

### New maintainer evidence

After Build 410:
- every notification-shade / Control Center down-up cycle can replay the native status-icon entrance presentation;
- a heads-up/notification appearance can leave the status bar restored to the native icon set;
- the Build-408 battery-ring reduction now reads too thin and the ROUND endpoint curvature looks visually mismatched against the lower dot group.

The maintainer explicitly requested that the next scene/runtime correction and the size/thickness visual coordination be tested in **one version** to reduce iteration time.

### Scene root cause

Current `dispatchPresentationReadiness()` combines two different responsibilities:
1. structural ability of the Home owner to maintain a valid replacement session;
2. scene eligibility of the Home overlay.

Because notification-shade / Control Center gates participate in that single Boolean, every transient panel ownership change currently drives:

`ready=false -> SystemUiHomePresentationOwner.deactivate() -> restore Home native masks/reservation`

and return drives:

`ready=true -> activate() -> recapture/reapply masks/reservation`.

That is unnecessarily destructive. Build-409 device evidence already demonstrated that Control Center has its own native status presentation while the Home overlay can still exist underneath; therefore target-scene native rendering does not require tearing down the Home owner.

### Selected Build-411 ownership correction

Separate:
- **owner structural readiness** = feature enabled + model/tint/layout ready + Home host attached;
- **overlay scene eligibility** = unlocked Home + notification shade settled + Control Center not visible + native handoff inactive.

The persistent Home owner remains active across notification-shade / Control Center scene transitions. Scene callbacks only drive overlay visibility. Full owner teardown remains for real structural invalidation/fail-native/hot-reload/host replacement.

Expected effect:
- no repeated Home native restore/re-mask cycle on every panel gesture;
- no project-triggered replay of native icon entrance caused by that cycle;
- less susceptibility to temporary notification/HUN scene fluctuations restoring the whole Home status representation.

### Visual A/B in the same candidate

Build 408 reduced effective ring width from 8.25 to 7.5 while preserving the accepted mobile-dot radius (~5.39). Maintainer feedback now reports:
- ring too thin;
- ROUND endpoint curvature no longer visually matches the lower opening/dots.

Build 411 uses:
- ring stroke: **8.0 canonical units**;
- mobile dot radius: unchanged;
- center size/geometry: unchanged;
- color/alpha: unchanged;
- existing lower-opening edge-gap solver recomputes dot angles from the new ring width.

This is intentionally between rejected/heavy 8.25 and current/thin 7.5.

### Review boundary

No delay, retry, polling, custom scene animation, fraction threshold, per-glyph color compensation, center resize, or device-pixel magic value is added.


---

## 2026-09-27 — Build 411 source review: stable Home owner + 8.0 ring

**Type:** combined scene/visual checkpoint / source review
**Build:** 411 / 20260927-411
**Executable runtime source:** `aaaaf0810b114b1e90a3de3f1520721a420da2d0`
**Validation:** source review passed; Fast/Canary pending

### Executable delta from Build 410

Only two runtime files change:

1. `CombinedStatusHomeRenderSession.kt`
   - introduces pure `resolveOwnerReady(...)`;
   - owner readiness now depends only on feature/model/tint/layout/host attachment;
   - scene surface, notification-shade eligibility and Control Center eligibility remain overlay-visibility gates only;
   - existing Build-410 panel authorities are unchanged.

2. `CombinedStatusPainter.kt`
   - default battery-ring stroke: `7.5 -> 8.0`;
   - mobile-dot radius unchanged;
   - center geometry unchanged;
   - tint/alpha unchanged;
   - lower-opening solver automatically recomputes balanced edge gaps from the new ring width.

### Why this addresses the new runtime symptoms

Before Build 411, every panel scene-gate change could toggle presentation readiness and therefore execute full Home owner `deactivate()/activate()`, restoring and then reapplying native Home clip masks and end reservation. This creates an unnecessary Home-layer cutover cycle on every down/up gesture and can amplify transient notification/HUN state changes into a full native fallback.

Build 411 keeps the structurally valid Home owner stable. Shade/Control Center remain NATIVE_ONLY target surfaces; only the Home overlay is hidden during their ownership.

### Review

- **Ownership:** Home owner stays scoped to the Home host; target surfaces remain native/SystemUI-owned.
- **Lifecycle:** no new Hook/listener/polling; fewer owner lifecycle transitions.
- **Single writer:** unchanged mask/reservation writer; scene callbacks no longer destroy/recreate it.
- **Cleanup:** real structural invalidation still reaches the existing deactivate/cleanup path.
- **Fail native:** feature/model/tint/layout/host failure still makes owner readiness false.
- **Performance:** removes repeated owner setup/teardown on panel gestures.
- **Compatibility:** no new private SystemUI contract.
- **Exception recovery:** unchanged target-profile and fail-native boundaries.
- **Future extension:** scene progress remains available for later projection without being coupled to owner lifetime.

### Visual review

Ring 8.0 is intentionally between:
- Build 407 effective 8.25, which read too heavy;
- Build 408-410 7.5, which now reads too thin and gives a smaller ROUND endpoint radius.

The existing gap solver remains the only lower-opening spacing authority.

### Gate

Fast CI + signed Canary, then one combined device pass for scene behavior and visual balance.


---

## 2026-09-27 — Build 411 Fast + signed Canary success

**Type:** combined validation success / device-test gate
**Build:** 411 / 20260927-411
**Executable runtime source:** `aaaaf0810b114b1e90a3de3f1520721a420da2d0`
**Tested PR head:** `6bc260143c056bac643daf1c8d3f1af99bcd30df`

### Fast validation

- Fast Build #1150 / run `36321207293`: **success**.
- Passed Fast classification, Gradle Wrapper, Java/API 37, pinned HyperOS target-profile verification, all unit tests, Debug assembly, APK resolution and Modern Xposed metadata.
- The new owner-readiness separation test and Build-411 outer-geometry baseline are included in the passing test suite.

### Signed Work Branch Canary

- Work Branch Canary #384 / run `36321322516`: **success**.
- Trusted source resolution and exact checkout verification passed for PR #138 head `6bc260143c056bac643daf1c8d3f1af99bcd30df`.
- Passed:
  - Gradle Wrapper;
  - Java / Android API 37;
  - Haple signing restore/verification;
  - pinned notification-shade + Control Center runtime contracts;
  - tests and Canary build;
  - Modern Xposed metadata;
  - Haple APK signature;
  - non-debuggable verification;
  - artifact preparation/upload.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-411-canary.apk`
- Artifact ID: `10931903528`
- Extracted APK size: `3309602` bytes
- Extracted APK SHA-256: `a596bd8b7841675174330be27421ee729ffe3d453b0f0b9a6ecb3f5956d7c759`

### Gate

**Runtime is frozen at Build 411.**

The next required evidence is one combined maintainer device pass covering:
- repeated panel gestures / native-icon entrance behavior;
- heads-up notification behavior;
- Control Center and notification-shade leak regression;
- ring 8.0 thickness / ROUND endpoint / lower-dot visual coordination.

No further runtime change should be layered before that result.


---

## 2026-09-27 — Build 411 device result: panel handoff accepted; 8.0 ring rejected

**Type:** maintainer device acceptance / visual baseline correction
**Build under test:** 411 / 20260927-411
**Executable source:** `aaaaf0810b114b1e90a3de3f1520721a420da2d0`

### Maintainer result

- notification-shade / Control Center down-up behavior is now reported as **no problem**;
- therefore the Build-411 persistent Home-owner / scene-visibility separation is accepted for the tested panel gesture path;
- the 8.0-unit battery-ring A/B is rejected: the maintainer prefers the original 8.25 geometry;
- the thinner ring also makes the `ROUND` endpoint curvature look visually less coordinated with the lower four-dot opening.

The earlier report that a heads-up/notification popup can restore the native status bar is **not** automatically marked resolved by the panel acceptance and remains a separate issue unless separately confirmed.

### Geometry interpretation

The renderer uses `Paint.Cap.ROUND`. Therefore ring endpoint radius is intrinsically half the stroke width:
- 7.5 -> 3.75;
- 8.0 -> 4.0;
- 8.25 -> 4.125.

Restoring 8.25 therefore restores both line weight and the previously preferred endpoint curvature without adding a custom cap implementation.

### Selected Build-412 correction

- ring stroke: **8.0 -> 8.25** canonical units;
- `ROUND` cap unchanged;
- mobile-dot radius unchanged;
- center size/geometry unchanged;
- tint/alpha unchanged;
- opening-angle contract unchanged;
- existing lower-opening solver recomputes balanced edge gaps from the restored stroke;
- Build-411 scene ownership/lifecycle logic unchanged.

### Review boundary

This is a deterministic return to an already device-seen and explicitly preferred visual baseline. It still requires source review and CI/Canary, but does not require a separate maintainer thickness A/B before integration.


---

## 2026-09-27 — Build 411 device verdict: scene accepted, 8.0 ring rejected

**Type:** maintainer device acceptance / visual baseline correction
**Build under test:** 411 / 20260927-411
**Executable source:** `aaaaf0810b114b1e90a3de3f1520721a420da2d0`

### Maintainer feedback

The maintainer reports:
- notification-shade / panel down-up behavior is now correct;
- the 8.0 battery ring still looks worse than the previously seen 8.25 geometry;
- the ring/ROUND-endpoint/lower-opening relationship should prioritize visual harmony rather than keeping the 8.0 compromise.

### Accepted runtime conclusion

The Build-411 ownership correction is retained:
- the structurally valid Home owner remains persistent across panel scene transitions;
- notification-shade / Control Center gates affect overlay visibility rather than destructively tearing down/recreating the Home owner;
- no further panel-scene runtime change is requested by this feedback.

### Visual correction

The Build-411 8.0 ring A/B is rejected.

The next candidate restores exactly:
- ring stroke: **8.25 canonical units**;
- `Paint.Cap.ROUND`: unchanged;
- resulting endpoint cap radius: **4.125 canonical units**;
- mobile dot radius: unchanged at the accepted current value;
- center geometry: unchanged;
- colors/alpha: unchanged;
- lower-opening solver: unchanged and allowed to recompute balanced edge gaps from the restored ring width.

This is intentionally an exact historical ring-width restoration, not a new screenshot-derived or device-pixel constant.

### Historical clarification

Earlier Build-407 notes recorded that 8.25 could read optically heavy relative to the then-current center/dot presentation. That historical observation remains valid evidence and is not rewritten. Later center-resource/tint corrections and current maintainer comparison change the present decision: on the current visual/runtime baseline, the maintainer explicitly prefers 8.25 over 8.0/7.5.

### Review boundary

No scene lifecycle, Hook, Home ownership, notification/HUN behavior, center/dot geometry, tint, alpha or animation change is authorized in this follow-up.

Because 8.25 is already a previously observed device geometry and the change is a one-variable restoration using the existing ROUND cap and gap solver, CI/review is sufficient for this follow-up unless an unexpected delta appears.


---

## 2026-09-27 — Build 412 source review: restore preferred 8.25 ring

**Type:** deterministic visual restoration / source review
**Build:** 412 / 20260927-412
**Executable source:** `f794a7c01513364eefc726316fcaf4058d581683`
**Validation:** source review passed; Fast/Canary pending

### Executable delta from Build 411

- `CombinedStatusPainter.kt`: default ring stroke `8.0 -> 8.25`.
- `CombinedStatusOuterGeometryTest.kt`: default baseline assertion updated to 8.25.
- build identity: `20260927-412`.

No other runtime behavior changes.

### Geometry / endpoint review

The renderer continues to use:
- `Paint.Style.STROKE`;
- `Paint.Cap.ROUND`;
- `Paint.Join.ROUND`.

Therefore restoring the stroke from 8.0 to 8.25 naturally restores endpoint radius from 4.0 to 4.125 canonical units. No custom cap path or separate curvature constant is introduced.

The lower-opening solver remains the sole spacing authority. It recomputes dot angles from:
- current ring stroke;
- current dot radius;
- fixed ring/dot orbit radii;
- the existing lower-opening angular contract.

There is no hard-coded 8.0 gap value left behind.

### Review

- **Ownership:** visual-only module geometry; scene/SystemUI ownership unchanged.
- **Lifecycle:** no Hook/listener/session change.
- **Single writer:** painter remains the sole ring geometry writer.
- **Cleanup / fail-native:** unchanged.
- **Performance:** constant change plus existing bounded solver only.
- **Compatibility:** no new private API or device-pixel constant.
- **Exception recovery:** unchanged.
- **Future extension:** shared outer-weight scale continues to scale the restored 8.25 baseline proportionally.
- **Visual consistency:** mobile dots, center geometry, tint/alpha and opening-angle contract remain unchanged; ROUND endpoint curvature returns with the preferred stroke rather than through a custom cap workaround.

### Gate

Fast CI + signed Canary. No separate maintainer thickness A/B is required before integration because the maintainer explicitly prefers the previously experienced 8.25 baseline over both 7.5 and 8.0.


---

## 2026-09-27 — Build 412 source review: restore preferred 8.25 ring

**Type:** low-risk visual restoration / source review
**Build:** 412 / 20260927-412
**Final executable source:** `f794a7c01513364eefc726316fcaf4058d581683`
**Validation:** source review passed; Fast/Canary pending

### Executable delta from Build 411

Only the visual ring baseline and build identity change:

1. `CombinedStatusPainter.kt`
   - `BASE_RING_STROKE: 8.0f -> 8.25f`.
2. `gradle.properties`
   - Build 411 -> Build 412.

No scene/owner/HUN runtime file changes after the device-accepted Build-411 executable source.

### Geometry review

- ring radius: unchanged at 50 canonical units;
- ring cap: unchanged `Paint.Cap.ROUND`;
- endpoint cap radius therefore becomes 4.125 canonical units at the restored 8.25 stroke;
- mobile-dot radius: unchanged;
- center geometry: unchanged;
- lower opening angles: unchanged;
- existing binary-search gap solver remains the sole authority for ring-end/dot and dot/dot edge spacing and recomputes from the restored stroke width.

This restores the previously experienced geometry rather than introducing a new cap, magic pixel value, or screenshot-fitted parameter.

### Call-site / regression review

- the deterministic outer-geometry test now pins 8.25;
- proportional scaling, symmetry, balanced five-edge gaps and clamp tests remain intact;
- no Home scene gate, owner readiness, Hot Reload, Hook contract, color, alpha, center resource or animation code changes.

### Review

- **Ownership:** unchanged.
- **Lifecycle:** unchanged.
- **Single writer:** unchanged Painter geometry ownership.
- **Cleanup:** unchanged.
- **Fail native:** unchanged.
- **Performance:** one constant change; solver complexity unchanged.
- **Compatibility:** no new platform/private contract.
- **Exception recovery:** unchanged.
- **Future extension:** the shared outer-weight scale still applies proportionally from the restored 8.25 default.

### Device-gate decision

The maintainer explicitly prefers the historical 8.25 geometry over 8.0/7.5 and asks to restore it. Because this is a single-variable return to a previously device-seen baseline using the same ROUND cap and solver, Fast CI + signed Canary are sufficient; no dedicated thickness-only maintainer test is required unless validation exposes an unexpected difference.

The Build-411 panel/scene-owner acceptance is retained. The earlier HUN/notification-popup report is not silently marked resolved by this visual change.


---

## 2026-09-27 — Build 412 Fast + signed Canary success; 8.25 baseline accepted

**Type:** validation success / visual baseline acceptance
**Build:** 412 / 20260927-412
**Final executable source:** `f794a7c01513364eefc726316fcaf4058d581683`

### Fast validation

- Fast Build #1156 / run `36323242298`: **success**.
- Passed Fast classification, Gradle Wrapper, Java/API 37, pinned HyperOS target-profile verification, all unit tests, Debug assembly, APK resolution and Modern Xposed metadata.

### Signed Work Branch Canary

- Work Branch Canary #390 / run `36323397870`: **success**.
- Trusted source resolution and exact checkout verification passed for PR #138.
- Passed:
  - Gradle Wrapper;
  - Java / Android API 37;
  - Haple signing restore/verification;
  - pinned notification-shade + Control Center contracts;
  - tests and Canary build;
  - Modern Xposed metadata;
  - Haple APK signature;
  - non-debuggable verification;
  - artifact preparation/upload.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-412-canary.apk`
- Artifact ID: `10933571263`
- Extracted APK size: `3309602` bytes
- Extracted APK SHA-256: `51daaab32c5f3a152a41340eb0c6b8d2cb93f2448b83a32a980942a85cc8e1e8`

### Acceptance

The maintainer explicitly rejected the intermediate 8.0 visual checkpoint in favor of the previously experienced 8.25 ring and requested restoration without a dedicated follow-up thickness test.

Build 412 therefore becomes the current accepted visual baseline after CI/Canary:
- ring stroke 8.25;
- ROUND endpoint radius 4.125;
- dot radius unchanged;
- center geometry unchanged;
- lower-opening solver unchanged;
- Build-411 panel/scene-owner behavior unchanged.

No additional thickness-only device gate is required.

### Separate unresolved boundary

The earlier heads-up/notification-popup report in which native status presentation can remain restored is **not** marked resolved by this acceptance. It remains a separate runtime question unless independently confirmed.

### Integration gate

PR #138 may proceed to `dev` after the final documentation head satisfies the required PR check. No further runtime change is required for the 8.25 restoration.


---

## 2026-09-27 — Build 412 Fast + signed Canary success; integration accepted

**Type:** validation success / maintainer-directed integration gate
**Build:** 412 / 20260927-412
**Executable source:** `f794a7c01513364eefc726316fcaf4058d581683`
**Tested PR head:** `74c234060f77da958c4eae21e8d170e29f2da1cb`

### Fast validation

- Fast Build #1156 / run `36323242298`: **success**.
- Passed Fast classification, Gradle Wrapper, Java/API 37, pinned HyperOS target profile, all unit tests, Debug assembly, APK resolution and Modern Xposed metadata.
- Outer-geometry coverage includes the restored 8.25 default ring plus mirror-symmetry, positive/equalized lower-opening edge gaps and supported scale bounds.
- Build-411 Home owner / panel scene tests remain passing.

### Signed Work Branch Canary

- Work Branch Canary #390 / run `36323397870`: **success**.
- Trusted source resolution and exact checkout verification passed for the tested PR head.
- Passed signing restore/verification, pinned target profile, tests/build, Modern Xposed metadata, Haple APK signature, non-debuggable verification and artifact publication.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260927-412-canary.apk`
- Artifact ID: `10933571263`
- Extracted APK size: `3309602` bytes
- Extracted APK SHA-256: `51daaab32c5f3a152a41340eb0c6b8d2cb93f2448b83a32a980942a85cc8e1e8`

### Maintainer acceptance / integration rule

- Panel down/up behavior is accepted from the Build-411 device result and carried unchanged into Build 412.
- The maintainer explicitly prefers the already-experienced 8.25 ring over both the 7.5 and 8.0 variants.
- Therefore the 8.25 restoration is accepted for `dev` without another dedicated ring-size device A/B.
- The ROUND endpoint implementation remains unchanged; restoring 8.25 restores the preferred endpoint radius naturally.
- The earlier notification/HUN-triggered native-status fallback report remains separate and is not silently marked fixed by this acceptance.

### Gate

PR #138 may integrate to `dev` after its latest documentation-only head satisfies the required PR check. No further runtime change is required for this integration boundary.


---

## 2026-09-27 — PR #138 merged to dev; Build 412 Integration passed

**Type:** dev integration / accepted baseline update
**Merge commit:** `a25cb5ce2aeab235cfaed579474df70596f03a63`
**Integrated build:** 412 / 20260927-412

### Accepted net state

- Build-411 persistent Home owner / panel scene-visibility separation is retained after maintainer acceptance of notification-shade / Control Center down-up behavior.
- Battery ring default is restored to the maintainer-preferred 8.25 canonical units with unchanged ROUND cap.
- Mobile dots, center geometry, semantic colors, alpha and lower-opening solver remain unchanged.
- No dedicated thickness-only device gate is required.

### Dev Integration CI

- Build workflow: **#1159** / run `36323866999`.
- Result: **success**.
- Passed:
  - Integration scope classification;
  - Gradle Wrapper / Java / API 37;
  - Haple signing restore/verification;
  - pinned HyperOS notification-shade + Control Center contracts;
  - all tests and signed Canary build;
  - Modern Xposed metadata;
  - Haple APK signature;
  - non-debuggable verification;
  - artifact upload.
- Integration artifact ID: `10933541364`
- Extracted APK size: `3309602` bytes
- Extracted APK SHA-256: `caca75d56145f58dafd25d4798d79025997113c6965d254ccffe2f7e707d5421`

### Remaining boundary

The earlier heads-up/notification-popup report in which native status presentation can remain restored was not independently closed by the latest maintainer panel-gesture feedback. It remains a separate Phase-2B runtime question and must be investigated without reopening the accepted panel scene gates or 8.25 visual baseline.


---

## 2026-09-27 — PR #138 merged to dev; Build 412 Integration passed

**Type:** dev integration / accepted Phase-2B baseline
**Dev merge:** `a25cb5ce2aeab235cfaed579474df70596f03a63`
**Build:** 412 / 20260927-412
**Executable source:** `f794a7c01513364eefc726316fcaf4058d581683`

### Merge / acceptance

- PR #138 was clean and squash-merged to `dev`.
- Accepted device result carried into the merge:
  - notification-shade / Control Center down-up behavior correct;
  - persistent Home owner / scene-visibility separation retained;
  - 8.25 battery-ring stroke preferred over 7.5 and 8.0;
  - ROUND endpoint geometry restored naturally with the 8.25 stroke.
- No dedicated post-merge ring-size device test is required.

### Dev Integration CI

- Build workflow: **#1159** / run `36323866999`.
- Result: **success**.
- Passed:
  - Gradle Wrapper;
  - Java / Android API 37;
  - Haple signing restore/verification;
  - pinned HyperOS notification-shade + Control Center contracts;
  - tests and required APK build;
  - Modern Xposed metadata;
  - Haple APK signature;
  - non-debuggable verification;
  - Canary artifact publication.
- Integration artifact: `CombinedStatus-0.0.2-HyperOS-20260927-412-canary.apk`
- Artifact ID: `10933541364`
- Extracted APK size: `3309602` bytes
- Extracted APK SHA-256: `caca75d56145f58dafd25d4798d79025997113c6965d254ccffe2f7e707d5421`

### Current boundary

Build 412 is now the dev integration baseline. The next runtime problem is the separately observed notification/HUN-triggered native-status fallback. It is not considered part of the accepted panel scene gate or ring geometry and must be investigated independently.

---

## 2026-09-27 — Checkpoint-driven CI policy accepted and synchronized

**Type:** repository automation / engineering governance
**APK build:** none; runtime identities remain main Build 351 and dev Build 412
**Runtime impact:** none

### Problem / objective

Development iteration was paying avoidable CI cost in two places:

1. a ready work-branch PR could run Fast on every intermediate push;
2. every successful trusted Fast Build could automatically trigger another signed Work Branch Canary even when no device evidence was needed.

At the same time, Build/device results still need to be written into `CURRENT.md` / `DEVLOG.md` so repository state remains recoverable across sessions. Removing those records would reduce continuity and reintroduce dependence on conversation memory.

The objective was therefore to reduce redundant CI **without weakening validation gates or removing repository engineering memory**.

### Analysis / root cause

**Confirmed:** the expensive part was not the existence of Light/Fast/Integration/Full validation itself; it was admission cadence.

- Draft PRs already map to Light.
- Ready ordinary runtime PRs map to Fast.
- Trusted `dev` integration and CI/build-system changes correctly retain stronger Integration/Full validation.
- The old Work Branch Canary workflow additionally followed every qualifying successful Fast Build automatically.
- Documentation closure commits could also create misleading “next Build” bookkeeping if CI execution metadata were treated as application Build identity.

Therefore the correct change is to make expensive validation **checkpoint-driven**, while preserving the existing quality levels.

### Evidence / references consulted

Reviewed current repository authority and implementation before changing the workflow:

- `CONTRIBUTING.md` — branch routing, checkpoint-based validation, CI scopes, development-memory requirements;
- `docs/development/RECORDING.md` — current/runtime identity and cross-file synchronization;
- `docs/development/VERSIONING.md` — application Build versus GitHub Actions run semantics;
- `.github/workflows/build.yml` — Light/Fast/Integration/Full classifier and cancellation policy;
- `.github/workflows/work-branch-canary.yml` — automatic `workflow_run` admission plus owner fallbacks;
- `.github/workflows/promotion-readiness.yml` — post-Build readiness separation.

The review also confirmed that `cancel-in-progress` and the existing combined Gradle invocation were already reasonable; duplicate admission was the higher-value optimization.

### Measures implemented

Accepted through PR #139 on `main`:

- active ordinary runtime work PRs stay **Draft** during implementation so intermediate pushes receive Light validation;
- Ready-for-review is used only for a meaningful Fast checkpoint;
- successful Fast no longer automatically starts a signed Work Branch Canary;
- an exact owner `/canary` comment is now the normal explicit device-validation admission;
- comment-triggered Canary requires:
  - an open same-repository `feat/**` / `fix/**` PR;
  - PR not Draft;
  - a successful trusted pull-request Build for the exact requested head SHA;
- owner-only `workflow_dispatch` remains the independent fallback and still runs the full Canary validation contract;
- documentation-only checkpoint closure is explicitly defined as repository memory, not a new executable Build;
- `CONTRIBUTING.md`, `RECORDING.md`, `VERSIONING.md`, and `CHANGELOG.md` were updated with the same semantics.

### Review

- **Runtime/SystemUI:** unchanged.
- **Application buildId/versionCode:** unchanged by the policy change.
- **Signing boundary:** preserved; PR validation does not expose signing secrets, while trusted branch/Canary flows retain Haple signing verification.
- **Validation strength:** unchanged at each actual checkpoint; only the frequency/admission of expensive checks changed.
- **Device validation:** still required when behavior/risk requires it; signed work-branch APK creation is now demand-driven.
- **Repository memory:** preserved and clarified; Build/device conclusions still land in repository records.
- **Non-recursive recording:** a record-only closure commit may receive Light repository validation but must not fabricate a new Build number or DEVLOG runtime checkpoint.
- **Compatibility/performance:** no installed runtime code path changed.

### Validation / CI

PR #139 / source head `d711c380aa7fad86abecbc53355e5d1dca903525`:

- Build workflow **#1168** / run `36325319934`: **success** under Full validation.
- PR environment correctly skipped trusted signing/upload while passing required source/build checks.

Accepted `main` commit `0ab8e211eb4cac04e591b1ea908a0a9a9aab78e3`:

- trusted push Build workflow **#1169** / run `36325541667`: **success**;
- produced artifacts still identify the unchanged stable runtime as Build 351.

Back-sync:

- direct PR #140 was closed after branch-history conflicts showed that a simple `main -> dev` merge would not preserve the newer dev documentation history cleanly;
- an explicit two-parent merge commit `b2af353653c7f6e357ce22d135c27b69eabb4f82` was created from current dev plus accepted main policy ancestry, resolving only the intended five policy/workflow files;
- PR #141 Build workflow **#1170** / run `36325722071`: **success** under Full validation;
- PR #141 merged into `dev` as `a3fb5d1f70d6ac3d98b37d29ef13d15e3bd4aded`;
- trusted dev push Build workflow **#1171** / run `36325956985`: **success**, including signing, Debug/Canary build, Modern Xposed metadata, Haple signature, non-debuggable checks and artifact upload;
- the resulting dev artifacts still identify **Build 412 / 20260927-412**.

### Outcome / durable conclusion

CI is now **checkpoint-driven rather than commit-driven**:

`Draft iteration -> meaningful Ready/Fast checkpoint -> explicit /canary only when device evidence is needed -> dev integration`

Repository memory remains mandatory. A later documentation-only closure commit records the completed checkpoint but does not advance application Build identity merely because GitHub executes a Light repository check for that commit.

The current runtime development baseline remains **Build 412**. This automation/governance change does not resolve or alter the separate notification/HUN native-status fallback investigation.


---

## 2026-09-28 — Build 422 device rejection — Notification Shade edge persists

**Type:** device feedback / rejected runtime checkpoint
**Build:** 422 / `20260928-422`
**Signed Canary:** Work Branch Canary #412 / run `36357295818`
**Exact tested runtime head:** `f5cfbc87c819a776a5476f3ea1e5817b9c776d86`
**Runtime code after this record:** unchanged

### Device feedback

The maintainer reports that the Notification Shade problem still reproduces on Build 422 and supplied:
- `1000034090.mp4`;
- `CombinedStatus-Diagnostic-20260928-422-20260928-070916.txt`;
- `LSPosed_20260928_071003.zip`.

The Detailed report identifies `version=0.0.2`, `build=20260928-422`, `buildType=canary`, and `channel=canary`. Runtime health is healthy. The `panelTransition` source reports `expectedHooks=3 hooks=3`, with Notification runtime and Control Center hooks installed. Therefore the reproduction is not classified as a stale-install or missing-hook failure.

### Previous hypothesis status

Build 422 removed Battery scene state / `KeyguardManager` from Home visibility authority so Home visibility would be controlled by the native host plus panel coordinator. Device evidence now shows that this correction is **not sufficient** to remove the remaining Notification-Shade first/last-frame defect.

This does not reinstate the rejected Build-421 Battery/Keyguard inference. That path remains rejected.

### Root-cause boundary reopened

Before another runtime edit:
1. correlate screen-recording frames with Detailed + LSPosed event ordering;
2. inspect the exact-target SystemUI reference for every writer that can affect Home host/native status presentation around shade open/close;
3. determine whether the project shade callback is late relative to a different native owner, or whether Home overlay visibility is correct but the underlying host/native masking lifecycle changes independently;
4. preserve accepted Build-420 Control Center projection unless evidence proves a regression.

### Review / constraints

- **Ownership:** unresolved; do not add another visibility writer until the remaining native/project writer is identified.
- **Lifecycle:** no new listener/hook in this record.
- **Single writer:** Build 422 remains evidence; no competing writer is reintroduced.
- **Cleanup:** unchanged.
- **Fail-native:** unchanged.
- **Performance:** no runtime change.
- **Compatibility:** pinned target remains SystemUI `17.03.260226.r`.
- **Forbidden workaround path:** no delays, epsilon thresholds, timers, polling, per-frame followers, or geometry compensation.
- **Build identity:** remains Build 422 because this commit is record-only.


---

## 2026-09-28 — Build 423: follow native Notification Header expansion

**Type:** Phase-2B root-cause correction
**Build:** 423 / `20260928-423`
**Work branch / PR:** `feat/panel-projection` / Draft #146
**Runtime source:** `81deafdb3b25e1d34f4cb57ee57de09c9f1fa5e0`
**Device validation:** pending

### Problem / Build-422 evidence

The maintainer reports that Build 422 still reproduces the Notification-Shade first/last-frame continuity defect. The supplied Detailed report confirms the signed Build-422 Canary was active and all three panel hooks were installed, so stale installation / missing hook is rejected as the cause.

Timeline inspection shows the project's current Notification-Shade source, `ShadeExpansionStateManager.onPanelExpansionChanged(float, boolean, boolean)`, is not exposing the continuous Header transition needed for this handoff on the pinned target: captured Notification-Shade values jump between the closed/open boundaries while Control Center diagnostics expose normal intermediate progress values.

This rejects the Build-422 assumption that the generic Shade expansion callback is the correct Home-vs-Notification-Header handoff authority.

### Exact-target source review

Using maintainer-provided jadx 1.5.6 against the exact SystemUI APK
`17.03.260226.r` / SHA-256 `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`:

1. `CombinedHeaderController.start()` registers
   `NotificationHeaderExpandController.notificationCallback` through
   `NotificationPanelExpandController.addCallback(...)`.
2. `NotificationPanelExpandController.expansionState` is the read-only projection of
   `NotificationPanelExpansionAnimator.expansion`.
3. `NotificationPanelExpandController$2$1` collects that expansion StateFlow and, for each float,
   directly calls every registered `PanelExpandController.Callback.onExpansionChanged(float)`.
4. `NotificationHeaderExpandController$notificationCallback$1.onExpansionChanged(float)`
   consumes that same progress to update native Notification Header color fraction,
   translation, scale and alpha.

Therefore the Notification Header callback is the verified target-specific visual transition seam Combined Status needs; the generic `ShadeExpansionStateManager` broadcast is not used as the Home handoff source in Build 423.

### Implementation

- Replaced the Notification-Shade runtime Hook with
  `NotificationHeaderExpandController$notificationCallback$1.onExpansionChanged(float)`.
- The Hook observes after `chain.proceed()`, so native HyperOS remains authoritative and a native callback failure cannot leave Combined Status ahead of SystemUI state.
- Existing Home visibility ownership is unchanged: the callback only supplies native progress to the existing `notificationShadeAllowsHome` gate.
- Removed stale tracking dependence from that gate; Header progress alone is used.
- Kept Build-420 Control Center visibility/projection path unchanged.
- Updated the pinned target profile hookPoint from generic Shade expansion to the already-verified Notification Header callback.
- Added diagnostic authority `hyperos-notification-header-callback`.

### 审查 / review

- **Ownership:** native Notification Header transition remains SystemUI-owned; Combined Status only observes its already-used callback and controls its own Home overlay visibility.
- **Lifecycle:** no new observer/listener/service. One existing Notification hook is replaced one-for-one; installed hook count remains unchanged.
- **Single writer:** Home overlay visibility still has one project gate; Battery/Keyguard scene inference remains removed.
- **Cleanup:** existing HookHandle teardown / Hot Reload generation cleanup is unchanged.
- **Fail-native:** native callback executes first; reflection/contract failure rejects panel-source installation rather than inventing fallback scene semantics.
- **Performance:** event-driven native callback only; no polling, timer, per-frame tree scan, or added reflection hot path.
- **Compatibility:** callback class/method is already verified in the pinned exact-target profile; the profile hookPoint is updated to match actual runtime integration.
- **Future extension:** Notification Shade remains native-only with no Combined Status projection; Keyguard/AOD remain separate Phase-3 surfaces.

### CI status

Draft Light #1305 on the first Build-423 runtime commit failed only at `git diff --check` because the preceding Build-422 DEVLOG record contained four trailing-space Markdown lines. Runtime code, Kotlin compilation and target-profile validation were not reached by that Light run. The whitespace is corrected in the subsequent record-only commit without changing Build identity.

### Device acceptance boundary

After repository Full validation and a signed Canary:
- repeat Notification-Shade open/close gestures and inspect the very first departure frame plus final Home return frame;
- confirm Combined Status no longer disappears/restores out of phase with the native top-area transition;
- perform one Control Center open/close regression pass;
- if any edge remains, export Detailed diagnostics so the new Header-progress authority can be correlated directly with the video.


### Build-423 signed checkpoint

- Ready Full #1307 / run `36359395894`: **success** on PR head `5d982a74f80d157bfcfd543e7d1706099dd46e64`.
- Trusted Work Branch Canary #413 / run `36359604981`, attempt 1: platform-cancelled during Gradle execution after exact-source, signing restore and target-profile checks; no Kotlin/Gradle failure was reported.
- Canary #413 attempt 2: **success** on exact trusted source `5d982a74f80d157bfcfd543e7d1706099dd46e64`.
- Passed: trusted source resolution, exact checkout, Wrapper/JDK/API37, Haple signing restore, pinned HyperOS target profile, unit tests + Canary build, Modern Xposed metadata, Haple APK signature, non-debuggable verification, artifact upload.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260928-423-canary.apk`
- Artifact ID: `10945257544`
- Artifact ZIP digest: `sha256:d04859b9ec595d43f599174d61fa80fb169a509517b873b309189e418de13d20`
- Extracted APK size: `3309602` bytes
- Extracted APK SHA-256: `9830f24a36d55c5c38914a2b2c23f3cf52a49babfda26572a96420e79138597d`

Runtime is frozen at Build 423 pending focused maintainer device validation.


---

## 2026-09-28 — Build 423 device rejection: Header progress is motion, not ownership

**Type:** device feedback / root-cause refinement
**Build:** 423 / `20260928-423`
**Signed Canary:** Work Branch Canary #413 / run `36359604981`, successful attempt 2
**Runtime source:** `81deafdb3b25e1d34f4cb57ee57de09c9f1fa5e0`
**Runtime after this record:** unchanged

### Device result

The maintainer reports that the Notification-Shade first/last-frame defect still reproduces on Build 423.

The supplied Detailed report confirms the tested package is the expected `0.0.2` Build `20260928-423` Canary and reports `panelTransition` ready with all expected hooks installed. This rejects stale installation or missing Hook as an explanation.

The separate file `28_09-07-52-49_225.log` is a LogFox whole-system capture, not an LSPosed-only export. That distinction matters because it places native HyperOS SystemUI logs and Combined Status events on the same clock.

### New evidence

Build 423's Header callback is valid and continuous. The LogFox capture includes intermediate
`NotificationHeaderExpandController` progress values across the gesture, so the temporary hypothesis that Combined Status was attached to an endpoint-only callback instance is rejected.

The remaining problem is the ownership interpretation:

- Opening sequence:
  - native `StatusBar##isHomeStatusBarAllowed` changes to `false`;
  - `NotificationPanelExpandController` begins increasing panel height;
  - Build 423 then receives a tiny positive Header progress and immediately sets Home overlay eligibility false.
- Closing sequence:
  - Header progress reaches zero and Build 423 restores the overlay;
  - only after that does native `StatusBar##isHomeStatusBarAllowed` change back to `true`.

This establishes that Header progress is native motion context but not the complete native Home-status-bar ownership decision.

### Root-cause direction

The exact-target source for `isHomeStatusBarAllowed` must be traced before Build 424:
1. identify its defining state / Flow and producer;
2. identify the native status-bar presentation consumers;
3. determine whether Combined Status can observe the same authority without creating a second state machine;
4. keep Notification Shade native-only and Control Center on the accepted Build-420 projection path.

### 审查 / review boundary

- **Ownership:** unresolved but now narrowed to the native Home-status-bar allowed contract.
- **Lifecycle:** no runtime change in this record.
- **Single writer:** do not combine Header progress and a new boolean as independent writers; select one verified authority or compose through an existing native contract.
- **Cleanup:** unchanged.
- **Fail-native:** unchanged.
- **Performance:** no polling or frame-loop instrumentation.
- **Compatibility:** exact target remains SystemUI `17.03.260226.r`.
- **Rejected workaround:** no epsilon threshold around zero progress; the new evidence is about ownership ordering, not float noise.
- **Build identity:** remains Build 423.


---

## 2026-09-28 — Build 424: inherit native Home end-side lifecycle

**Type:** runtime root-cause correction / Phase 2B
**Display version:** 0.0.2
**Build:** 424 / `20260928-424`
**Branch / PR:** `feat/panel-projection` / #146
**Target:** HyperOS SystemUI `17.03.260226.r`
**Validation:** implementation complete; automated checkpoint pending; device acceptance not yet claimed

### Problem / objective

Build 423 still reproduces the Notification-Shade first/last-frame disappearance. Its exact Notification Header callback is live and continuously reports progress, so the remaining issue is not callback availability. The objective is to identify the native Home status-bar presentation owner and remove the project-local visibility decision rather than refine its timing.

### Problem execution flow

1. Replayed the maintainer LogFox timeline and retained the Build-423 rejection before changing runtime code.
2. Traced `StatusBar##isHomeStatusBarAllowed` into the exact-target `StatusBarVisibilityInteractor` / Home status-bar ViewModel flow.
3. Traced `shouldHomeStatusBarBeVisible -> isSystemInfoVisible -> systemInfoCombinedVis` into `HomeStatusBarViewBinderInjector`.
4. Verified that `HomeStatusBarViewBinderImpl.bind()` resolves `mEndSideContent = R.id.system_icons`.
5. Decoded exact-target `status_bar.xml` and `system_icons.xml`: `system_icon_area` is the parent `MiuiNotificationStatusContainer`; `system_icons` is the child root `MiuiStatusBatteryContainer`.
6. Verified `showEndSideContent()/hideEndSideContent()` animate `mEndSideContent` with native alpha / visibility / translation.
7. Compared this native carrier to Combined Status: Build 423 attached its visual to `MiuiNotificationStatusContainer.overlay`, one level above the animated child.
8. Selected a carrier-ownership correction instead of adding an `isHomeStatusBarAllowed` Hook, threshold, delay, frame listener, or second state machine.

### Evidence / findings

- The Notification Header callback remains valid motion evidence but is not the native Home-system-info visibility owner.
- HyperOS already has a complete Home visibility semantic pipeline and writes the resulting transition to `system_icons`.
- A parent `MiuiNotificationStatusContainer.overlay` does not acquire a child-specific alpha/visibility/translation animation merely because the child resides below the same HostSession.
- `MiuiStatusBatteryContainer` is the exact `system_icons` root and is already a verified carrier class used by the accepted Control Center projection.
- Therefore the pre-424 Notification fraction visibility gate was compensating for a visual carrier that sat outside the native animated end-side layer.

### Root-cause status

**Confirmed for the Build-424 implementation boundary.**

The project-owned Home overlay was attached above the View that HyperOS actually animates for Home end-side visibility. The project then added a second visibility writer based on Notification Header progress. The two lifecycles were not identical at the first/last frame, producing the observed discontinuity.

Device validation is still required to accept the corrected runtime behavior.

### Alternatives considered

- **Epsilon / fraction threshold:** rejected; changes timing without correcting ownership.
- **Delay / one-frame wait / timer:** rejected; introduces scheduling compensation.
- **Observe `isHomeStatusBarAllowed` with another Hook/Flow bridge:** rejected for the current fix because it would duplicate a semantic already applied to the exact native carrier.
- **Custom alpha/translation follower:** rejected; creates a second motion writer.
- **Move the Home visual into the native animated carrier and delete the duplicate shade writer:** selected as the smallest native-lifecycle correction.

### Implementation / decision

- Home HostSession discovery remains `MiuiNotificationStatusContainer / system_icon_area`.
- Home visual overlay moves from the parent host overlay to the direct `MiuiStatusBatteryContainer(system_icons).overlay`.
- Layout/listener/attach readiness for the visual follows that carrier.
- Notification Header runtime Hook is removed from `SystemUiPanelTransitionSource`.
- Notification shade Home-eligibility state, Home callback, restore/query path, and per-drag diagnostic state are removed.
- `SystemUiPanelTransitionSource` now owns only Control Center runtime visibility plus optional bounded Control Center diagnostics.
- Hot Reload stops querying/transferring live Notification eligibility; the legacy transfer slot remains null for payload compatibility.
- Target profile retires `notificationHeaderExpansion` as an active Hook point while retaining verified source/method evidence.
- Accepted Build-420 Control Center readiness-ordered projection is intentionally unchanged.
- Display version remains `0.0.2`; internal identity advances to Build 424.

### 审查 / review

- **Ownership:** HyperOS remains sole writer of Home end-side alpha/visibility/translation; Combined Status owns only its overlay content.
- **Lifecycle:** Home visual now lives inside the same native `system_icons` carrier lifecycle it visually replaces. No new long-lived observer, service, timer, or coroutine is added.
- **Single writer:** the project-local Notification Home visibility writer is deleted. Control Center remains a separate real-host handoff with one coordinator.
- **Cleanup:** carrier overlay add/remove and attach/layout listeners remain symmetric inside the Home session. Existing represented-slot and native restoration owners remain separate.
- **Fail native:** missing/invalid verified Home carrier prevents Combined Status presentation rather than creating a fallback transition approximation.
- **Performance:** one runtime Notification Header Hook and its gesture-time update path are removed; no polling/frame loop is introduced.
- **Compatibility:** exact target resources/source verify `R.id.system_icons -> MiuiStatusBatteryContainer`; the active compatibility profile no longer requires the retired Notification Hook.
- **Exception recovery:** existing host replacement, Hot Reload teardown, HookHandle cleanup, and native restoration paths remain in force; legacy transfer compatibility is retained without reviving the retired state.
- **Future extension:** Notification Shade stays native-only; Control Center stays a verified second-host projection; Keyguard/AOD remain separate Phase-3 host contracts.

### CI / device validation

Pending. The final Build-424 checkpoint must pass repository validation before one exact-head signed Canary is requested.

Focused device gate after automated validation:
- Notification Shade open/close first and last frame;
- Control Center open/close regression;
- one Hot Reload pass;
- one lock/unlock smoke pass for Home leakage.

### Outcome / next step

Build 424 is the active implementation checkpoint. Finish repository/static validation, update the active PR description, then run the normal Ready validation path. Freeze runtime at the signed-Canary device boundary.


---

## 2026-09-28 — Scene target clarified: Home/Keyguard sources, native full Control Center

**Type:** product-boundary / architecture planning
**Runtime Build:** unchanged; Build 424 remains the active executable checkpoint

### Requirement clarification

The final scene model is not “Home Combined Status plus a persistent Combined Status Control Center surface.”

There are two future source contexts:
- unlocked / Home;
- locked / Keyguard.

Each source context has:
1. a steady Combined Status state;
2. a partial Control Center pull state that must visually follow the native HyperOS transition;
3. a fully expanded Control Center endpoint that is **native SystemUI status-bar presentation only**.

### Architecture consequence

The stable architecture should separate:
- **steady source adapters** — Home now, Keyguard later;
- **one bounded transition coordinator/bridge** — consumes source readiness/geometry plus verified native Control Center motion;
- **native expanded endpoint** — no persistent Combined Status owner.

Home and Keyguard may reuse domain state, renderer semantics, sizing/tint policy and transition-coordinator logic, but must not share View ownership or infer each other through Battery/Keyguard heuristics.

Build 420 remains valid evidence that `realSystemIcons` / `MiuiStatusBatteryContainer` and readiness-ordered handoff can support the bridge. Its persistent expanded-surface lifetime is no longer the final product target.

### Impact on Build 424

No runtime widening is made in Build 424.

Build 424 still addresses one root cause only: the Home visual is moved into the native animated `system_icons` carrier and the duplicate Notification visibility writer is removed.

Mixing the new fully-expanded-native Control Center endpoint into the same executable checkpoint would reduce attribution and violate the single-variable debugging boundary. A separate follow-up checkpoint should narrow the existing Control Center projection lifetime to the native partial-pull interval and hand off to native status icons at the exact fully expanded endpoint.

### 审查 / review

- **Ownership:** Home and future Keyguard are separate steady owners; fully expanded Control Center is always native.
- **Lifecycle:** transition bridge exists only while native transition ownership is active.
- **Single writer:** source adapter owns steady Combined Status; bridge owns only project overlay visibility during bounded handoff; native Control Center owns expanded endpoint.
- **Cleanup:** reverse motion restores the correct source before bridge cleanup; full expansion removes bridge-owned presentation.
- **Fail native:** unresolved source/endpoint contracts leave native SystemUI visible.
- **Performance:** shared event-driven coordinator; no polling or duplicated per-scene state machine.
- **Compatibility:** source host adapters remain target-verified independently.
- **Future extension:** Keyguard can be added without changing Home ownership or creating a second transition engine.


---

## 2026-09-28 — Scene-family boundary clarified before lockscreen work

**Type:** product architecture clarification / future-compatibility review
**Runtime Build:** unchanged; Build 424 remains the active executable checkpoint
**Branch / PR:** `feat/panel-projection` / Draft #146

### Maintainer requirement

The long-term status-bar behavior is defined as two source-scene families:

- **Unlocked:** unlocked steady Combined Status -> HyperOS-owned partial Control Center pull transition -> fully expanded Control Center native-only.
- **Locked:** lockscreen steady Combined Status -> HyperOS-owned partial Control Center pull transition -> fully expanded Control Center native-only.

Notification Shade remains native-only on the pinned target. AOD remains separate until its own host/lifecycle contract is proven.

### Consequence for current work

This clarification does **not** reject Build 424's Home carrier correction. Build 424 is explicitly an unlocked/Home source-adapter fix: it places the visual inside the native `system_icons` carrier and removes the incorrect project-local Notification-Shade visibility writer.

It **does** narrow how Build 420 should be interpreted. The device-accepted `realSystemIcons` carrier and readiness-ordered handoff remain valid transition evidence, but keeping Combined Status projected for the entire fully expanded Control Center lifetime is no longer the final product requirement.

### Architecture decision

Model scene behavior as:

`source surface adapter -> native transition bridge -> native expanded endpoint`

with separate mutable source adapters for unlocked and lockscreen surfaces.

Shared across adapters:
- domain/network/battery state;
- renderer semantics and visual policy;
- immutable transition policy helpers where they are genuinely common.

Not shared:
- host/View references;
- lifecycle/session state;
- scene-specific geometry/tint anchors;
- cleanup tokens;
- transition carrier ownership.

### 审查 / review

- **Ownership:** unlocked and lockscreen each require their own verified source owner; fully expanded Control Center remains SystemUI/native-owned.
- **Lifecycle:** no lockscreen runtime object is created during Phase 2B.
- **Single writer:** do not add a global scene-state writer spanning Home and Keyguard; scene transfer is adapter ownership transfer.
- **Cleanup:** each adapter must clean only its own host/session resources.
- **Fail-native:** unsupported or unresolved lockscreen/transition contracts stay native.
- **Performance:** planning adds zero runtime observers/hooks/services; future adapters must remain event-driven.
- **Compatibility:** exact-target evidence is required independently for unlocked, lockscreen, and the native fully-expanded boundary.
- **Future extension:** Build 424 remains valid as the unlocked source-carrier foundation; Phase 3 can add a lockscreen adapter without rewriting the renderer/domain layer.

### Immediate follow-up

Trace the exact HyperOS semantic that distinguishes **Control Center transition** from **settled fully expanded Control Center**. Do not use a local progress epsilon or timer. Only after that authority is verified should the Build-420 projection lifetime be narrowed.


### Clarification — design concept, not verified lifecycle contract

The maintainer clarified that the Home/Keyguard/partial-pull/full-Control-Center split above is a **conceptual product partition**, not a demand that the implementation adopt those exact lifecycle objects or boundaries.

Therefore the architecture consequence in this entry is downgraded from a confirmed target model to a working hypothesis. The next exact-target lifecycle review may discover a simpler or more native grouping. If it does, the evidence and candidate lifecycle structures must be reviewed with the maintainer before the repository promotes one into architecture policy or runtime code.

Build 424 remains unchanged by this clarification.

---

## 2026-09-28 — Build 424 static checkpoint review

**Type:** pre-CI static review / test correction
**Runtime Build:** 424 / `20260928-424`
**Exact executable source:** `2556a098d35c202e1c5645a06e73757744f721e1`
**Runtime after this record:** unchanged

### Review result

Repository-wide targeted scanning found one stale test call site in `SystemUiNativeCombinedParticipantOwnerTest`: it still passed the retired `notificationShadeAllowsHome` argument to `CombinedStatusHomeRenderSession.resolveOverlayVisible(...)`.

The stale argument and the obsolete assertion that a Notification gate directly hides Home were removed. This is test-only adaptation to the Build-424 ownership model and does not create a new runtime Build.

A commit comparison from executable source `2556a098...` to the post-fix branch head confirms only tests and documentation differ; no later `app/src/main` or build metadata delta is present.

### 审查 / review

- **Ownership:** unchanged; Notification Shade has no Combined Status visibility writer.
- **Lifecycle:** unchanged.
- **Single writer:** the test no longer encodes the rejected shade visibility writer.
- **Cleanup / fail-native / performance / compatibility:** unchanged from Build 424.
- **Validation limitation:** the current GitHub connector can mutate PR state and repository files but its PR mutations are not producing a new Actions run for the Build-424 head; the local container cannot resolve github.com. Static review therefore does not substitute for required repository CI.


---

## 2026-09-28 — Build 424 static review and CI event recovery

**Type:** static review / CI checkpoint recovery
**Runtime Build:** unchanged — 424 / `20260928-424`
**Exact executable source:** `2556a098d35c202e1c5645a06e73757744f721e1`
**PR:** #146

### Static review result

Build 424 runtime review is complete and no further executable change is required before automated validation:

- Home visual is attached to `MiuiStatusBatteryContainer(system_icons).overlay`;
- no active Notification-Shade runtime Hook, Home-eligibility state, restore/query path, or per-drag visibility writer remains;
- `SystemUiPanelTransitionSource` retains only the Control Center visibility runtime Hook plus the optional bounded expansion diagnostic Hook;
- the exact-target profile retains Notification Header class/method evidence only as verified reference data, not as an active Hook point;
- unit-test expectations match one Control Center runtime Hook and one optional diagnostic Hook;
- later branch changes after the exact Build-424 executable source are test/documentation-only.

### 审查 / review

- **Ownership:** Home end-side alpha/visibility/translation remain HyperOS-owned.
- **Lifecycle:** the Home render session follows the native `system_icons` carrier; no new observer/service is introduced.
- **Single writer:** the rejected Notification fraction visibility writer is removed.
- **Cleanup:** overlay/listener cleanup remains symmetric and host-scoped.
- **Fail-native:** unresolved carrier contracts do not fall back to timing/geometry compensation.
- **Performance:** one gesture-time Notification Hook/path is removed; no polling/frame loop is added.
- **Compatibility:** active target profile Hook points match the remaining runtime integration.
- **Exception recovery:** existing host replacement, Hot Reload teardown, and native restoration remain unchanged.
- **Future extension:** Control Center/Keyguard lifecycle research remains analysis-only and is not mixed into Build 424.

### CI event observation

PR #146 was marked Ready at GitHub event time `2026-09-28T08:04:05Z`. The repository `.github/workflows/build.yml` explicitly subscribes to `pull_request.ready_for_review` for `dev`-based PRs, but no workflow run or commit status was created for that Ready event.

This record-only checkpoint intentionally creates a normal PR `synchronize` event so repository validation can resume without toggling Draft/Ready repeatedly. It does not change executable content or Build identity.

---

## 2026-09-28 — Exact-target Control Center ownership topology review

**Type:** architecture investigation / no runtime change
**Runtime Build:** unchanged; Build 424 remains the active executable checkpoint
**Branch / PR:** `feat/panel-projection` / Draft #146

### Objective

Review the maintainer's conceptual unlocked/locked/partial-pull/fully-expanded split against the actual HyperOS SystemUI ownership chain before committing to a follow-up architecture.

### Exact-target findings

The target does not expose one monolithic Control Center status-bar owner.

**Source authority**
- `MiuiPhoneStatusBarView.initDependence()` registers Home `mStatusBatteryContainer` as `ControlCenterFakeViewController.statusBarSystemIcons`.
- `MiuiKeyguardStatusBarView.initCallback()` registers lockscreen `mSystemIconsContainer` as `keyguardSystemIcons`.
- `ControlCenterFakeViewController.adjustRealSystemIcons()` selects the active source from native `StatusBarState`.
- This makes a project-local unlocked/keyguard transition router unnecessary in principle.

**Transition authority**
- `ControlCenterFakeStatusIcons` is a complete native `QS_FAKE` status-bar presentation with its own status-icon group, Battery, `MiuiStatusBatteryContainer`, tint/dark receiver, island owner, and attach/detach cleanup.
- Its width is synchronized from the currently selected source.
- Its tint policy already follows unlocked vs keyguard native authority.

**Destination authority**
- Control Center has a separate real native status-bar presentation.
- Native `onExpansionChanged(float)` moves fake and real presentations.
- Native `onAppearanceChanged(boolean, boolean)` switches alpha/visual ownership between fake and real.
- `PanelExpandController.getAppearance()` exposes the current ownership value; no local fraction threshold is required.

**Home departure**
- `expandStateForStatusBar` feeds `HomeStatusBarViewBinderInjector.mControlPanelExpand`.
- When true, Home binder calls `hideEndSideContent(false)`; when false, it restores with `showEndSideContent(false)`.
- Build 424 therefore places the Home compact visual on the correct carrier to inherit this departure/return naturally.

### Revised candidate model

The strongest current candidate is not “two project-owned three-state machines”. It is native-owner composition:

`steady source adapter (Home / future Keyguard)`
→ `HyperOS QS_FAKE transition presentation`
→ `HyperOS QS real destination`

The project would share renderer/domain semantics, but each rendered carrier would remain scoped to the native View lifecycle that owns its phase.

### 审查 / review

- **Ownership:** HyperOS already owns source selection, transition motion, fake/real appearance, and final Control Center destination. Combined Status should avoid duplicating any of those facts.
- **Lifecycle:** source adapters bind their own native Views; a future transition adapter should bind the fake View lifecycle, not hold source references as its lifecycle authority.
- **Single writer:** candidate route could remove the current project-local Control Center Home visibility gate and avoid any new appearance writer by inheriting native carrier alpha.
- **Cleanup:** `QS_FAKE` already has symmetric native attach/detach registration; any Combined Status overlay must add/remove only its own View/listeners.
- **Fail-native:** if the fake carrier contract cannot be resolved, leave native Control Center untouched.
- **Performance:** candidate requires no polling, timer, per-frame reflection, or local animation; only low-frequency lifecycle/event integration is acceptable.
- **Compatibility:** all findings are exact-target evidence for SystemUI `17.03.260226.r`; plugin-side appearance producer conditions are not visible in this APK and must not be guessed.
- **Exception recovery:** source references can outlive a detached source View until a replacement registers, so Combined Status must not use `realSystemIcons` reference lifetime as its own HostSession lifetime.
- **Future extension:** native source selection already covers Home vs Keyguard and may substantially simplify Phase 3.

### Decision boundary

No follow-up runtime implementation is authorized by this record. Build 424 remains a single-variable Home-carrier correction. After Build 424's automated/device result, compare:
1. retain the Build-420 source-anchor projection;
2. migrate the transition render to the native `QS_FAKE` carrier;
3. any evidence-backed alternative.

Discuss the lifecycle/maintenance tradeoff with the maintainer before selecting the follow-up route.


---

## 2026-09-28 — QS_FAKE first-frame and suppression review

**Type:** architecture investigation / no runtime change
**Runtime Build:** unchanged; Build 424 exact executable remains `2556a098d35c202e1c5645a06e73757744f721e1`

### First-frame evidence

Build-423 device diagnostics show:
- `visible=true` at 07:51:32.034;
- the existing Control Center projection attached and became ready in the same timestamp;
- by the 07:51:32.205 sampled expansion frame, native Home `mEndSideContent` was already alpha 0 / INVISIBLE.

This supports reusing the existing low-frequency visibility seam to prepare a future QS_FAKE session instead of adding a new lifecycle Hook only for entry readiness. The exact time delta is not treated as a contract. Reverse/close ordering remains an explicit future device gate.

### Native suppression evidence

QS_FAKE uses the shared `system_icons.xml` hierarchy. Its native `RIGHT_BLOCK_LIST` does not remove Wi-Fi/mobile/Battery.

`MiuiStatusIconContainer.onMeasure()` uses `ignoredSlots` to exclude represented children from native measurement/underflow. A clip-only implementation would hide pixels but leave layout participation and is rejected.

The current Home owner already globally Hooks the relevant classes once and then filters by owned View identity. A follow-up architecture can therefore generalize the owner from one `current` session to a bounded identity-keyed session registry instead of installing a duplicate Hook set.

### End-reservation evidence

Build-423 normal-state device evidence:
- stable Battery carrier width = 105;
- actual Battery width = 105;
- requested compact slot width = 105;
- resulting Home padding-end delta = 0.

The policy remains necessary for states where those widths differ or native Battery is hidden. Exact Battery source confirms island state is written to the Battery's own associated `MiuiStatusBatteryContainer`, including QS_FAKE's local container. This favors carrier-local reservation state rather than cross-surface copying.

### 审查 / review

- **Ownership:** source anchors remain read-only geometry authority; future QS_FAKE suppression may mutate only the fake carrier it owns.
- **Lifecycle:** candidate session lifetime is `visible=true -> visible=false`; no additional view lifecycle Hook is currently justified by entry evidence.
- **Single writer:** reuse one compact-presentation suppression registry; do not keep independent Home/Fake suppression writers for the same View.
- **Cleanup:** every registry session must restore only its own ignored-slot additions, clip masks and padding baseline.
- **Fail-native:** unresolved fake hierarchy or writer conflict leaves native QS_FAKE untouched.
- **Performance:** no new measure/layout Hook set; O(1) identity lookup on the already-Hooked methods is the preferred bound.
- **Compatibility:** exact target uses the same `MiuiStatusBatteryContainer / MiuiStatusIconContainer / MiuiBatteryMeterView` hierarchy in Home and QS_FAKE.
- **Exception recovery:** stale source references are not session owners; registry lifetime follows actual owned carrier/session cleanup.
- **Future extension:** the same registry pattern may later support a verified Keyguard compact carrier without multiplying Hook sets.

### Decision boundary

No runtime refactor is made before Build 424 automated/device validation and maintainer review of the candidate lifecycle.


---

## 2026-09-28 — QS_FAKE tint and update-cost closure

**Type:** architecture investigation / no runtime change
**Runtime Build:** unchanged; Build 424 exact executable remains `2556a098d35c202e1c5645a06e73757744f721e1`

### Tint authority

The existing tint pipeline is already surface-local enough for a future QS_FAKE carrier:

- `SystemUiTintStateSource` Hooks `MiuiBatteryMeterView` class methods globally, but caches/dispatches state by concrete Battery View identity in weak maps.
- `CombinedStatusControlCenterRenderSession.updateTint()` ignores Battery events that do not originate from its own carrier Battery.
- session refresh reads `SystemUiTintStateSource.currentState(localBattery)`.
- `SystemUiNativeNetworkSuppressionOwner.currentAppliedStatusIconTintForGroup(group)` resolves tint from the supplied group directly; it does not require that group to be the active Home group.
- `CombinedStatusTintAuthority.resolveBatteryEvent()` can therefore combine the local fake Battery state with the local fake status-icon peer tint.

The exact native QS_FAKE View already switches its Battery/icon tint between unlocked and Keyguard semantics. No additional tint Hook, observer, scene boolean, or polling path is justified.

### Update cost

The current Control Center render session receives only existing event-driven domain/presentation/tint/settings updates. It is not driven from expansion progress. When no session exists, calls are no-ops.

A future QS_FAKE session bounded to `visible=true -> visible=false` therefore does not require a continuously resident second renderer and does not add a per-frame render path.

### 审查 / review

- **Ownership:** fake native tint remains SystemUI-owned; Combined Status reads the already-applied local Battery/peer result.
- **Lifecycle:** transition renderer exists only for the Control Center visible lifecycle candidate.
- **Single writer:** no new tint writer or global scene tint cache.
- **Cleanup:** session removal discards only its renderer/listeners; weak tint cache entries follow View lifetime.
- **Fail-native:** missing local tint authority leaves the transition renderer unready/native.
- **Performance:** no new Hook or polling; updates remain event-driven.
- **Compatibility:** exact QS_FAKE hierarchy uses the same Battery/status-icon classes consumed by current sources.
- **Exception recovery:** a stale Home tint cannot become fake authority because fake session filters source View identity and resolves its own peer group.
- **Future extension:** the same per-carrier tint semantics are compatible with a separately verified Keyguard steady adapter.


---

## 2026-09-28 — Exact-target Control Center source / fake / real ownership chain

**Type:** architecture investigation / exact-target evidence
**Runtime Build:** unchanged; Build 424 remains the active executable checkpoint
**Branch / PR:** `feat/panel-projection` / #146

### Objective

Evaluate the maintainer's unlocked/locked/partial-pull/fully-expanded product concept against the actual HyperOS lifecycle before implementing a new scene abstraction.

### Exact-target evidence

Target: SystemUI `17.03.260226.r`.

1. **Unlocked source registration**
   - `MiuiPhoneStatusBarView.onFinishInflate()` resolves `R.id.system_icons` to `mStatusBatteryContainer`.
   - `initDependence(...)` assigns that exact container to `ControlCenterFakeViewController.statusBarSystemIcons` and calls `adjustRealSystemIcons()`.

2. **Keyguard source registration**
   - `MiuiKeyguardStatusBarView` assigns `mSystemIconsContainer` to `ControlCenterFakeViewController.keyguardSystemIcons` and calls `adjustRealSystemIcons()`.
   - Keyguard tint changes are forwarded directly to `controlCenterFakeStatusBar.setKeyguardStatusBarColors(...)`.

3. **Native source authority**
   - `ControlCenterFakeViewController` selects `realSystemIcons` from those two registered source containers according to native status-bar state.
   - Therefore Combined Status does not need a duplicate unlocked/keyguard transition router.

4. **Native transition representation**
   - the Control Center fake status bar owns a complete `QS_FAKE` status-icon/Battery representation with native attach/detach, tint and island participation;
   - `ControlCenterHeaderExpandController` reads source `realSystemIcons` as an anchor and applies transition geometry to Control Center-side status-bar representations.

5. **Native destination ownership**
   - fully expanded Control Center uses its own `QS` native status-bar representation;
   - `appearance` is independent from `visible`, `expansion`, and `tracking` and selects fake-vs-real visual ownership;
   - the plugin-side producer of `appearance` is outside the reviewed SystemUI APK, so its internal threshold must not be guessed or recreated.

### Candidate lifecycle

The source evidence supports a simpler candidate topology than a project-owned `unlocked × lockscreen × three phases` state machine:

`source adapter (Home or future Keyguard) -> native QS_FAKE transition owner -> native QS destination owner`.

This is not yet an implementation decision. The current Build-424 device checkpoint remains single-variable and unchanged.

### 审查 / review

- **Ownership:** HyperOS already owns source selection and fake/real Control Center visual ownership.
- **Lifecycle:** fake status-bar resources follow native View attach/detach; no project service/poller is required.
- **Single writer:** a future implementation should inherit native fake-bar translation/alpha/tint rather than add fraction/appearance-derived writers where unnecessary.
- **Cleanup:** candidate fake-carrier integration should be scoped to that carrier's attach/detach lifecycle.
- **Fail-native:** if fake carrier identity/bootstrap cannot be verified, retain native SystemUI rather than fall back to local interpolation.
- **Performance:** candidate route can remove project handoff work; no high-frequency new observer is justified.
- **Compatibility:** evidence is pinned to SystemUI `17.03.260226.r`; plugin-side `appearance` production is not yet inspected.
- **Future extension:** Home and Keyguard may keep separate steady HostSessions while sharing the native Control Center transition/destination path.

### Next step

Do not change Build 424. Complete its automated/device validation first. In parallel, continue static/runtime review of the fake Control Center carrier bootstrap/readiness and compare it with the current Build-420 `realSystemIcons.overlay` projection before proposing a follow-up runtime checkpoint.


---

## 2026-09-28 — Build 424 Ready validation event not emitted

**Type:** CI infrastructure blocker
**Runtime Build:** 424 / `20260928-424` unchanged
**Executable source:** `2556a098d35c202e1c5645a06e73757744f721e1`

### Evidence

- PR #146 was moved to ready-for-review.
- Additional documentation/test synchronization commits were pushed while the PR remained ready.
- No `Build` workflow run is associated with any of those current heads.
- `.github/workflows/build.yml` on `dev` explicitly listens for `pull_request` events `ready_for_review` and `synchronize`.
- The GitHub workflow-run connector returns the historical Build-423 runs (#1306/#1307/#1308) normally, ruling out a read-side connector failure.

### Conclusion

The current blocker is classified as GitHub Actions event delivery / trigger admission for operations performed through the connected GitHub app, not a Build-424 source failure and not an incorrect validation-scope rule.

### Boundary

- Do not create a replacement runtime Build merely to provoke CI.
- Do not request `/canary` without the required successful exact-head pull-request Build.
- Keep Build 424 runtime frozen.
- Continue static/source review independently; when automated validation becomes reachable, run the normal exact-head checkpoint before device testing.


---

## 2026-09-28 — Post-424 QS_FAKE candidate passes static ownership review

**Type:** architecture review / no runtime change
**Runtime Build:** 424 / `20260928-424` unchanged
**Validation:** candidate only; do not implement before Build 424 device result

### Refined candidate

The exact-target Control Center review supports a smaller follow-up architecture than the current Build-420 source-overlay projection:

`compact-capable source -> native QS_FAKE compact transition -> native QS destination`.

For the current Home-only implementation, source capability can remain the existing identity contract:
`SystemUiHomePresentationOwner.ownsBatteryContainer(realSystemIcons)`.
Do not introduce a generalized capability registry until Keyguard has its own verified compact owner.

### Runtime surface

Retain the existing low-frequency `ControlCenterExpandControllerDelegate.onVisibleChanged(boolean)` production Hook only as transition-surface activation. It must stop acting as a Home visibility writer.

The native fake carrier is resolvable through direct exact-target fields:
`ControlCenterHeaderExpandController.headerController`
-> `CombinedHeaderController.controlCenterFakeStatusBar`
-> `ControlCenterFakeStatusIcons.delegate`
-> `CcFakeStatusBarIcons.statusBarArea`.

A second low-frequency source-change seam at `ControlCenterFakeViewController.adjustRealSystemIcons()` is justified only if runtime/edge review requires source reevaluation while Control Center remains visible. Battery `mStatusBarState` is explicitly rejected for this role.

### Presentation policy

QS_FAKE must preserve native transition occupancy:
- no represented-slot `ignoredSlots`;
- no Home `paddingEnd` reservation;
- no native width/translation/alpha write.

Only the represented native visual roots and Battery are reversibly clipped while the compact fake renderer is ready. HyperOS keeps fake width synchronized from the selected source and remains sole transition geometry/appearance writer.

Existing global `MiuiStatusIconContainer.onMeasure/onLayout` Hooks can route a mask-only fake session without adding a second layout Hook. Existing `MiuiBatteryMeterView` tint Hooks already cover the fake Battery. The legacy binding-level suppression path remains out of scope.

### Entry / exit ordering

- **Entry:** resolve source + fake carrier -> build model/tint/layout -> make compact fake renderer ready -> apply native fake visual masks.
- **Exit or failure:** restore native fake visual masks first -> stop/hide compact fake renderer.
- Native fake visuals therefore remain the fail-native substrate and no blank state is required.

### 审查 / review

- **Ownership:** HyperOS selects source and owns fake/real Control Center transition; Combined Status owns only its fake overlay and reversible fake clip tokens.
- **Lifecycle:** production activity is bounded by native Control Center visibility; no polling, pre-draw follower, timer, or per-frame callback is added.
- **Single writer:** no project geometry/alpha/appearance writer; Home visibility returns entirely to the native Home carrier.
- **Cleanup:** fake masks/render state restore independently from Home. Host detach/replacement and feature disable restore only fake-owned clip state.
- **Fail-native:** missing source capability, fake carrier, statusIcons, Battery, tint, layout, or restoration contract keeps/restores native QS_FAKE without disabling Home.
- **Performance:** keep one low-frequency visible Hook; reuse existing tint/layout Hooks; renderer can be inactive while Control Center is hidden, avoiding offscreen center-indicator animation.
- **Compatibility:** field chain and QS_FAKE/QS source roles are verified only for SystemUI `17.03.260226.r`; plugin-side appearance production remains unmodified and uninterpreted.
- **Exception recovery:** source/host identity is revalidated on activation; an optional source-change seam is event-driven and low-frequency if later proven necessary.
- **Future extension:** Keyguard can later become another compact-capable source without changing the native QS_FAKE/QS transition topology; do not implement that abstraction before Phase 3 evidence exists.

### Decision boundary

This candidate is preferred for the **post-Build-424** Control Center follow-up, but it is not implemented now. Build 424 remains frozen for its single-variable Home carrier validation.


---

## 2026-09-28 — Correction: reject the earlier QS_FAKE mask-only candidate

**Type:** architecture correction / no runtime change
**Runtime Build:** unchanged — Build 424 / `20260928-424`

A prior post-424 candidate entry proposed that QS_FAKE could preserve transition occupancy without represented-slot `ignoredSlots`, using only reversible visual clipping. **That specific mask-only conclusion is rejected by later exact-target evidence.**

Exact `MiuiStatusIconContainer.onMeasure()/onLayout()` review confirms:
- represented Wi-Fi/mobile children continue to participate in native measurement/layout unless their slots are excluded through the container's `ignoredSlots` contract;
- `clipBounds` hides pixels only and does not release their measured occupancy;
- QS_FAKE's native block list does not remove Wi-Fi/mobile/Battery.

Therefore any future QS_FAKE compact replacement must combine:
- host-scoped/reversible represented-slot exclusion for layout participation;
- reversible visual masking for attached native roots/Battery;
- a scene-specific reservation decision based on the fake carrier's own verified geometry.

The Home `paddingEnd` reservation remains Home-scoped and is **not** automatically promoted to QS_FAKE.

### Lifecycle seam refinement

The preferred minimal Phase-2B follow-up remains the existing low-frequency
`ControlCenterExpandControllerDelegate.onVisibleChanged(boolean)` Hook:
- `visible=true`: read HyperOS's already-selected `realSystemIcons` as source capability, resolve the native QS_FAKE carrier, then activate the fake compact session only if the source is a verified compact owner;
- `visible=false`: restore fake-owned slot/mask state and clean up the transition session;
- Home visibility is not written by this callback; Build 424's native Home carrier lifecycle remains authoritative;
- no `appearance` or fraction Hook is required.

`ControlCenterFakeViewController.adjustRealSystemIcons()` remains a verified source-authority seam, but a second Hook is **not** justified pre-emptively. Add it only if later device evidence proves that source can change while Control Center remains visible in a way the visible-lifetime snapshot cannot safely cover.

### 审查 / review

- **Ownership:** native layout remains `MiuiStatusIconContainer`-owned; Combined Status owns only scoped exclusion/mask tokens for the fake session.
- **Lifecycle:** transition session remains bounded to native Control Center visible lifetime.
- **Single writer:** no project alpha/translation/appearance/Home-visibility writer.
- **Cleanup:** restore only the fake session's owned ignored-slot additions, clip states and any later verified local reservation.
- **Fail-native:** unsupported/null source or unresolved fake carrier leaves QS_FAKE fully native.
- **Performance:** normal production retains one low-frequency Control Center runtime Hook; no new polling/frame callback.
- **Compatibility:** exact-target only until the fake carrier/exclusion contract is verified on other targets.
- **Exception recovery:** Hot Reload must restore an active fake session explicitly rather than waiting for another visible event.
- **Future extension:** Keyguard becomes a compact-capable source only after its own steady owner is verified; native source routing remains HyperOS-owned.


---

## 2026-09-28 — Post-424 Control Center architecture comparison closure

**Type:** architecture review / no runtime change
**Runtime Build:** unchanged — Build 424 / `20260928-424`
**Decision status:** preferred follow-up candidate only; implementation remains blocked on Build-424 automated/device result

### Routes compared

**A — retain Build-420 `realSystemIcons.overlay` projection**
- Proven device-capable as a transition/handoff mechanism.
- Uses the source status-bar carrier as the project projection surface rather than HyperOS's dedicated Control Center transition presentation.
- Requires project-level projection readiness and Home handoff coordination.
- A native-only fully expanded endpoint would require additional endpoint ownership handling.
- Future Keyguard source support is less natural because the current runtime contract is explicitly Home-owned.

**B — render compact presentation inside native `QS_FAKE` carrier**
- Uses HyperOS's dedicated transition status-bar presentation.
- Source identity is read from the already-selected native `realSystemIcons`; no project unlocked/keyguard state machine is required.
- Native fake parent owns translation, alpha, source width, unlocked/keyguard tint, island participation and fake->real Control Center appearance.
- Fully expanded Control Center remains the independent native `QS` status bar automatically.
- Home departure/return is inherited from Build-424's native `system_icons` carrier.
- Retains the existing low-frequency `ControlCenterExpandControllerDelegate.onVisibleChanged(boolean)` Hook only as transition-session lifetime; no fraction or appearance Hook is required.
- This is the preferred post-424 candidate.

**C — retain Build-420 source projection and add native `appearance` endpoint gating**
- Smaller immediate delta than B.
- Keeps the source-anchor projection and project handoff choreography, then adds another observed ownership fact.
- Reduces neither conceptual duplication nor future Keyguard complexity.
- Rejected as the preferred long-term route unless device evidence invalidates B.

### QS_FAKE compact-presentation contract

A future B implementation must use a host-scoped session on the fake `MiuiStatusBatteryContainer`.

Required local behavior:
- use the existing native `MiuiStatusIconContainer.ignoredSlots` measure/layout contract for represented-slot exclusion;
- reversibly mask represented native roots and Battery;
- place the Combined Status renderer in the fake carrier overlay;
- derive requested compact width from the shared layout semantics;
- use only the fake carrier's own Battery width / hide state / padding baseline for any local end reservation;
- preserve HyperOS translation, alpha, width and appearance writers untouched.

The earlier mask-only QS_FAKE idea is rejected: clipping does not remove native layout participation.

### Runtime Hook / update cost

No additional normal-production Hook is currently justified:
- reuse the existing `onVisibleChanged(boolean)` runtime Hook for `visible=true -> visible=false` fake-session lifetime;
- resolve current source + fake carrier through the already-resolved `ControlCenterHeaderExpandController` object;
- reuse the already-installed global `MiuiStatusIconContainer.onMeasure/onLayout` and `MiuiStatusBatteryContainer.setIsHideBattery` Hooks through a bounded identity-keyed carrier-session registry rather than installing another Hook set;
- existing Battery/tint event sources are already per-View and can feed the fake carrier without a new tint observer.

`ControlCenterFakeViewController.adjustRealSystemIcons()` remains a verified native source-authority seam, but do **not** add a second Hook unless later device evidence proves source can change during one Control Center visible lifetime in a way the entry snapshot cannot cover.

### Hot Reload / recreation policy

QS_FAKE is a transient transition surface, not a steady source surface.

Preferred fail-native default:
- old-generation teardown restores fake-owned native slot/mask/reservation state;
- if Hot Reload occurs while Control Center is already visible, native QS_FAKE/QS remains authoritative until the next normal visible lifecycle;
- do not expand Hot Reload payload solely to preserve a transient compact transition unless later maintainer/device evidence requires seamless continuity for that developer action.

SystemUI recreation naturally reconstructs the native fake View/source registration; the next visible lifecycle resolves the live objects again.

### Remaining device-risk boundary

Exact target source confirms `ControlCenterHeaderExpandController` applies native `batteryWidthDiff` to the fake status-bar X translation. That value can be non-zero in real island/Battery scenarios and is intentionally SystemUI-owned.

Combined Status must never rewrite/cancel that value. However, the interaction between:
- QS_FAKE local compact reservation,
- tag-5 Battery behavior,
- charging / Battery island,
- and final source-edge visual continuity

remains a required focused device gate for any B implementation.

### 审查 / review

- **Ownership:** B maps each project visual to the native carrier that owns its phase; HyperOS retains source selection, transition motion and final destination.
- **Lifecycle:** Home steady and future Keyguard steady remain separate adapters; QS_FAKE session is bounded to Control Center visible lifetime.
- **Single writer:** no project translation/alpha/appearance/Home-visibility writer; per-carrier exclusion/reservation state is identity-scoped.
- **Cleanup:** fake session restores only its own ignored-slot additions, clip states and verified local padding write before disposal.
- **Fail-native:** unsupported/null source, unresolved fake carrier, writer conflict, invalid layout/tint/model, host detach or Hot Reload leaves/restores native QS_FAKE.
- **Performance:** no polling/timer/frame follower; normal Control Center Hook count does not increase; already-installed layout/Battery Hooks use bounded identity lookup.
- **Compatibility:** exact-target field/resource contracts are pinned to SystemUI `17.03.260226.r`; other targets fail native until separately verified.
- **Exception recovery:** session re-resolves live source/carrier each visible lifecycle; transient Hot Reload falls back to native rather than retaining stale View ownership.
- **Future extension:** once Keyguard steady compact ownership is verified, native `realSystemIcons` source selection can make it transition-capable without introducing a project scene router.

### Decision boundary

Do not implement route B in Build 424. Build 424 remains the single-variable Home carrier correction. After its required repository validation and focused device result, present route B plus its remaining island/charging gate to the maintainer before creating the next executable checkpoint.


---

## 2026-09-28 — QS_FAKE battery translation independence

**Type:** exact-target architecture evidence / no runtime change
**Runtime Build:** unchanged — Build 424 / `20260928-424`

### Finding

Exact `ControlCenterHeaderExpandController` source shows:
- `batteryWidthDiff` is calculated from the selected source `StatusBarAnchorBounds.batteryWidth` and the real Control Center `controlCenterSystemIcons` Battery width;
- island handling may replace it with the negative real-QS Battery width;
- `onExpansionChanged(float)` adds `batteryWidthDiff` to the **parent QS_FAKE status-bar translationX**;
- QS_FAKE internal ignored-slot or padding state is not an input to that formula.

### Consequence

A future QS_FAKE compact session may evaluate carrier-local slot exclusion and end reservation without rewriting or feeding back into HyperOS's parent transition translation. Combined Status must still leave `batteryWidthDiff` and fake-parent translation untouched.

The remaining device gate is local: charging/island compact edge alignment, first/last-frame continuity, and restoration. It is no longer treated as a possible shared-motion ownership conflict.

### 审查 / review

- **Ownership:** parent transition translation remains HyperOS-only.
- **Lifecycle:** no runtime change.
- **Single writer:** compact session may own only fake-local exclusion/mask/reservation tokens.
- **Cleanup:** local state must restore before fake session disposal.
- **Fail-native:** reservation conflict or unresolved local geometry keeps QS_FAKE native.
- **Performance:** no new Hook, listener, polling, or per-frame project work.
- **Compatibility:** exact-target SystemUI `17.03.260226.r` only.
- **Future extension:** the same principle can be revalidated for a future Keyguard transition source without copying source motion state.


---

## 2026-09-28 — Build 424 validation conflict recovery

**Type:** CI/repository-history recovery / no runtime change
**Runtime Build:** unchanged — Build 424 / `20260928-424`
**Exact executable source:** `2556a098d35c202e1c5645a06e73757744f721e1`

### Problem

PR #146 repeatedly produced no pull-request Build after ready/synchronize events. The workflow definition itself included those event types and GitHub's public service status was healthy.

### Root cause

The active branch had diverged from current `dev` and the PR was not mergeable. GitHub does not run `pull_request` workflows for PRs with merge conflicts. Current `dev` had three automation/governance commits not present in the work branch.

### Recovery

- Pre-resolved the dev-only CI/governance content into `feat/panel-projection` without touching App runtime code.
- Created history-preserving merge commit `ca1bbf7e10c485f34846add633c34d06a163e7e8` with parents:
  - feature history `a29d656a7a207645deb8c78ac04000553df48897`;
  - dev tip `947c13956f2b4cbe08faf21de73b3a2f1b7a8b81`.
- The merge tree preserved the already-resolved feature contents; Build 424 executable source remained unchanged.
- A temporary sync PR used long-lived `dev` itself as its head. GitHub's delete-head-branch behavior consequently deleted `dev` after merge.
- `dev` was immediately recreated at exact pre-delete tip `947c13956f2b4cbe08faf21de73b3a2f1b7a8b81`; no commit, runtime, or file content changed.
- The synchronization rule is corrected: future long-lived branch sync uses a temporary `sync/*` head and deletes only that temporary branch.

### Validation

After history conflict resolution, PR #146 became mergeable and Full Build #1321 / run `36413531047` succeeded on exact head `ca1bbf7e10c485f34846add633c34d06a163e7e8`.

The preceding Draft Light #1320 failed only because several newly added DEVLOG metadata lines contained trailing whitespace. It did not indicate an executable failure. This record also normalizes trailing whitespace across DEVLOG before the final exact-head checkpoint.

### 审查 / review

- **Ownership:** no SystemUI/runtime ownership changed.
- **Lifecycle:** no installed behavior changed.
- **Single writer:** unchanged.
- **Cleanup:** repository history now contains both dev and feature parentage; long-lived branch deletion is explicitly prevented procedurally.
- **Fail-native:** unchanged.
- **Performance:** no runtime code or Hook changed.
- **Compatibility:** Build 424 exact target/runtime source is unchanged.
- **Exception recovery:** dev restoration was exact-SHA recreation, not a reconstructed or rebased branch.
- **Future extension:** synchronization procedures must use disposable `sync/*` heads so repository auto-delete behavior cannot remove long-lived branches.

### Next

Run Draft Light on the cleaned repository state, then move PR #146 Ready for one final exact-head Full. Only after that exact head succeeds may the owner request `/canary`.


### Follow-up correction — QS_FAKE block-list is ineffective for modern network Views

A deeper exact-target trace corrects an intermediate investigation hypothesis.

`IconManager.setBlockList()` is instance-local and calls `StatusBarIconControllerImpl.refreshIconGroup()`. Although that refresh calls `setBlocked()` on `StatusIconDisplayable` children, `ModernStatusBarView.setBlocked(boolean)` is an empty override on this target. Both `ModernStatusBarWifiView` and `ModernStatusBarMobileView` inherit that behavior.

Therefore native block-list mutation is **not** a viable Wi-Fi/mobile suppression mechanism for QS_FAKE. A further review of the active Build-424 architecture narrows the preferred route: generalize the **current Home presentation-layer** mechanism (temporary represented-slot exclusion during native measure/layout plus reversible clip masks) into host-scoped carrier sessions. The older binding-identity suppression owner is not the default QS_FAKE route.

Exact-target `MiuiStatusBatteryContainer.onMeasure/onLayout` also shows that Battery continues to consume width even when visually masked. HyperOS synchronizing fake `statusBarArea.width` from the current source therefore does not by itself prove that local end reservation can be omitted. Any future fake adapter must verify its own reservation contract; parent Control Center translation remains SystemUI-owned.

No runtime code or Build identity changes in this correction.


### Build 424 automated validation update

Ready validation Build #1328 / run `36418043111` completed successfully on PR head `3cbf8523cfafeb99a58dcd213053e9a2e020f71f`.

Passed:
- Gradle wrapper validation;
- Android API 37 / JDK setup;
- pinned HyperOS target-profile verification;
- unit tests and Debug APK build;
- Modern Xposed metadata verification;
- non-debuggable check.

As expected for the pull-request Fast path, project signing and Canary artifact publication were not executed.

This successful checkpoint does not change runtime identity: executable source remains Build 424 / `2556a098d35c202e1c5645a06e73757744f721e1`. Documentation corrections after that head require one final exact-head pull-request Build before owner `/canary` admission.

### Build 424 ViewOverlay lifecycle verification

AOSP framework review confirms that a View overlay is rendered from its host View's own draw path after host content/children. The internal overlay group is not an independent window/surface; it redirects invalidation to the host.

This supports the Build-424 carrier correction:
- parent `MiuiNotificationStatusContainer.overlay` does not inherit child-only `system_icons` alpha/visibility writes;
- `MiuiStatusBatteryContainer(system_icons).overlay` participates in the exact host's draw/transform lifecycle, so moving the visual there is a lifecycle correction rather than a coordinate-only change.

The finding resolves the static concern that `ViewGroupOverlay` might remain visually independent from its own host's visibility/alpha. Build 424 remains suitable for focused device validation after the final exact-head CI gate.


### Build-424 signed Canary checkpoint

- Ready Full Build #1328 / run `36418043111`: **success** on exact PR head `3cbf8523cfafeb99a58dcd213053e9a2e020f71f`.
- Owner `/canary` admission created Work Branch Canary #417 / run `36418655598`.
- Canary trusted-source resolution selected branch `feat/panel-projection` and exact SHA `3cbf8523cfafeb99a58dcd213053e9a2e020f71f`; checkout/source verification passed.
- Passed: pinned HyperOS target profile, unit tests + Canary build, Modern Xposed metadata, Haple APK signature, non-debuggable verification, artifact preparation and upload.
- Artifact: `CombinedStatus-0.0.2-HyperOS-20260928-424-canary.apk`.
- Artifact ID: `10968671147`.
- Artifact ZIP digest: `sha256:450d387fbf8819e51e8401d9d925fa9a10ad36ab8e6d664b552968d726817fb7`.
- Extracted APK size: `3309602` bytes.
- Extracted APK SHA-256: `7e1a7bf035207718de3d74c580b87c3d5f20df648a98a78ef8f560d25c95e778`.
- PR returned to Draft. Runtime remains Build 424 and is frozen pending focused maintainer device validation.

#### Device gate

1. Reproduce Notification Shade pull-down / return and inspect the first departure frame plus final Home return frame.
2. Perform one Control Center open/close regression pass; Build-420 behavior must not regress.
3. Perform one Hot Reload pass.
4. Perform one lock/unlock smoke pass for Home-overlay leakage.
5. If any discontinuity remains, export Detailed diagnostics before further runtime mutation.

This record-only closure does not create a new runtime Build.


---

## 2026-09-28 — Build 424 device acceptance

**Build:** 424 / `20260928-424`
**Exact executable source:** `2556a098d35c202e1c5645a06e73757744f721e1`
**Exact tested PR head:** `3cbf8523cfafeb99a58dcd213053e9a2e020f71f`
**Ready Full:** #1328 / run `36418043111` — success
**Signed Canary:** #417 / run `36418655598` — success
**Artifact:** `CombinedStatus-0.0.2-HyperOS-20260928-424-canary.apk` / id `10968671147`
**APK SHA-256:** `7e1a7bf035207718de3d74c580b87c3d5f20df648a98a78ef8f560d25c95e778`

### Maintainer feedback

Focused device validation reports **no visible abnormality**.

Accepted behaviors include:
- Notification Shade first departure / final Home return continuity;
- quick Control Center regression pass;
- Hot Reload;
- lock/unlock smoke pass for Home-overlay leakage.

### Diagnostic confirmation

The supplied Detailed report confirms:
- runtime health = `healthy`;
- Build/channel = 424 Canary;
- Home render carrier = native `MiuiStatusBatteryContainer(system_icons).overlay`;
- `nativeVisibilityInherited=true` and `nativeAlphaInherited=true`;
- Notification runtime Hook = false;
- panel source remains event-driven with zero native geometry writes.

One Control Center entry while the read-only Battery scene reports raw Keyguard state produces:
`controlCenterProjection state=unavailable fallback=home-visible reason=real-system-icons-not-home-owned-container`.

This is not accepted as a new defect because:
- no visible anomaly was reported;
- the fallback is bounded and fail-native;
- later unlocked Control Center pulls in the same report attach `realSystemIcons.overlay` and reach projection readiness normally;
- the condition matches the already-identified architectural weakness of Build-420 source-anchor projection when HyperOS selects a source other than the Home-owned carrier.

It therefore strengthens, rather than blocks, the planned post-424 move toward the native QS_FAKE transition owner.

### 审查 / review

- **Ownership:** Build 424 correctly places unlocked/Home drawing inside the native Home end-side visibility owner.
- **Lifecycle:** Notification Shade needs no project-local visibility state; device behavior confirms the inherited lifecycle.
- **Single writer:** no Notification fraction/visibility writer remains.
- **Cleanup:** Hot Reload and lock/unlock smoke pass show no visible carrier leakage.
- **Fail-native:** the bounded Control Center source mismatch falls back native/Home-visible rather than forcing unsupported ownership.
- **Performance:** event-driven; no polling/timer/frame follower added.
- **Compatibility:** accepted on Xiaomi 15 Pro / haotian / Android 17 / SystemUI 17.03.260226.r.
- **Exception recovery:** Hot Reload completed with healthy runtime state.
- **Future extension:** freeze Build 424 as the accepted unlocked/Home source-carrier baseline; continue Control Center work as a separate QS_FAKE transition checkpoint.

### Next

Complete feature integration of the accepted Build-424 checkpoint into `dev`. Start the QS_FAKE transition-owner implementation from that integrated baseline rather than stacking it onto the already-accepted Build-424 runtime checkpoint.


---

## 2026-09-28 — Build 424 integrated into dev

**Runtime Build:** 424 / `20260928-424`
**Feature PR:** #146
**dev integration SHA:** `a6ba0ddc843d3e8d2fbca6c15786d99b8c0b2826`
**Integration Build:** #1338 / run `36420376141` — success

### Integration result

PR #146 was squash-merged to `dev` only after:
- Build 424 passed focused maintainer device validation;
- final work-branch Ready Full #1337 succeeded;
- the accepted runtime remained unchanged after the signed Canary checkpoint.

The trusted `dev` push then passed Integration #1338:
- signing restore;
- pinned HyperOS target profile;
- unit tests and required APK builds;
- Modern Xposed metadata;
- Haple APK signature;
- Canary non-debuggable verification;
- artifact publication.

Integrated Canary artifact:
- `CombinedStatus-0.0.2-HyperOS-20260928-424-canary.apk`
- artifact id `10969481485`
- artifact ZIP digest `sha256:ff910cd3c6112914cb1e301a3142a855b80def062247569cf90a11f13c058330`

### 审查 / review

- **Ownership:** accepted Home ownership remains the native `system_icons` carrier; no new runtime owner was introduced by integration.
- **Lifecycle:** device-accepted Notification-Shade / Hot Reload / lock-unlock behavior is preserved.
- **Single writer:** no project Notification visibility writer returns.
- **Cleanup:** integration adds no new resource/session lifetime.
- **Fail-native:** existing bounded Control Center source mismatch remains fail-native and is isolated to the superseded source-anchor transition mechanism.
- **Performance:** runtime tree is the already-tested Build 424 tree; no additional Hook, observer, polling, or per-frame work.
- **Compatibility:** exact target remains SystemUI `17.03.260226.r`.
- **Exception recovery:** Integration validation includes signed artifact production from the merged `dev` source.
- **Future extension:** continue the Control Center transition redesign in a new short-lived feature branch from this exact integrated baseline; do not reuse merged PR #146.

This record-only closure does not create Build 425.


---

## 2026-09-28 — Build 425 compact-presentation registry extraction

**Build:** 425 / `20260928-425`
**Branch:** `feat/control-center-native-transition`
**Device validation:** not required unless source review or CI reveals behavior drift

### Goal

Prepare the accepted Build-424 Home presentation layer for a second native carrier without adding a second Hook set or changing Home behavior.

### Implementation

- Introduce `SystemUiCompactPresentationRegistry` as the sole installer for the existing three presentation Hooks:
  - `MiuiStatusIconContainer.onMeasure`;
  - `MiuiStatusIconContainer.onLayout`;
  - `MiuiStatusBatteryContainer.setIsHideBattery`.
- Route those Hooks by exact owned View identity through a small volatile session snapshot.
- Move reusable represented-slot exclusion, reversible clip-mask, local end-reservation, writer-conflict, and cleanup mechanics into registry sessions.
- Keep `SystemUiHomePresentationOwner` as the Home-specific adapter:
  - exact Home host/subtree resolution;
  - Home readiness/fail-native semantics;
  - Home event naming;
  - legacy participant cleanup;
  - existing public/internal API consumed by the module.
- Register only the Home session in Build 425. QS_FAKE is **not** activated in this checkpoint.
- Preserve the legacy Hook IDs so Hot Reload/replacement diagnostics do not gain an unnecessary identity change.
- Correct the install diagnostic carrier label to the already-accepted Build-424 `MiuiStatusBatteryContainer.overlay`.

### 问题执行流程

**现象与证据:** exact-target review shows Home, future Keyguard, and QS_FAKE share the same `MiuiStatusBatteryContainer + MiuiStatusIconContainer + MiuiBatteryMeterView` presentation structure, while current Home presentation Hooks are already target-identity-gated.

**根因 / 责任源:** adding QS_FAKE by copying the Home owner would duplicate Hook/state ownership. Keeping the current single-session implementation would instead force unrelated surface lifecycle state into the Home owner.

**原生实现:** HyperOS keeps each source/transition carrier as a distinct View instance. Exact carrier identity is therefore the natural dispatch key.

**方案:** one global Hook set plus independent host-scoped sessions. Build 425 performs only the behavior-preserving extraction; the native QS_FAKE owner is introduced in the next checkpoint.

**workaround:** none.

### 审查 / review

- **Ownership:** each session owns only the explicit carrier/status-icon/Battery instances supplied by its surface adapter.
- **Lifecycle:** Home remains the only registered session in Build 425; existing attach/detach/fail-native flow is unchanged.
- **Single writer:** the same three Hook points remain the only layout/Battery interception layer.
- **Cleanup:** registry release removes the session from dispatch before restoring its local padding/clip state; reset releases all sessions before unhooking.
- **Fail-native:** Home adapter retains its existing fallback callback and native restoration behavior.
- **Performance:** Hook count stays at three. Hot-path dispatch changes from synchronized access to an immutable volatile array identity lookup; no polling, timer, frame listener, or extra observer is added.
- **Compatibility:** no new private SystemUI contract is required in Build 425.
- **Exception recovery:** Hot Reload keeps the existing module-facing owner API and hook-count accounting.
- **Future extension:** the next checkpoint may register one QS_FAKE session without duplicating Hooks or turning Home into a cross-scene owner.

### Validation

Draft Light first. After source review, move Ready for the normal runtime checkpoint build. This internal extraction does not need a signed Canary/device pass if automated validation shows no behavior drift; continue directly to the QS_FAKE checkpoint.

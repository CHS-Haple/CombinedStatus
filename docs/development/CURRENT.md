# Current Development State

This file is the concise recovery point for active Combined Status development. Read it after `CONTRIBUTING.md`. Keep chronological Build/investigation history in `DEVLOG.md`, future direction in `ROADMAP.md`, version semantics in `VERSIONING.md`, and record-writing rules in `RECORDING.md`.

## Repository baseline

- Last refreshed: 2026-09-28
- Stable branch: `main`
- Stable runtime baseline: Build 413 / `20260927-413`, promotion merge commit `3114ade06bcb4846a5654a70f38ea572f5b47b37`
- Integration branch: `dev`
- Integration runtime baseline: Build 424 / `20260928-424`, `dev` squash integration `a6ba0ddc843d3e8d2fbca6c15786d99b8c0b2826`
- Build 424 / `20260928-424` is the current device-accepted and Integration-validated `dev` runtime baseline. Integration Build #1338 / run `36420376141` passed on exact `dev` SHA `a6ba0ddc843d3e8d2fbca6c15786d99b8c0b2826`, including signed Canary, target-profile, unit/build, Xposed metadata, signature, and non-debuggable checks. Build 413 remains the current `main`-promoted stable runtime baseline.
- Phase-2B Home / Notification-Shade checkpoint PR #146 is merged. Build 420 remains **device-accepted evidence for source-anchor Control Center handoff**, Builds 421-423 are **device-rejected Notification-Shade experiments**, and Build 424 is **accepted/integrated** for the native Home `system_icons` carrier correction. Builds 425-427 / closed PR #152 are now **device-rejected QS_FAKE transition-carrier experiments**: Build 427 still permits intermittent Combined Status disappearance/native overlap and charging-island alignment divergence despite successful native-layout cutover. The documented fallback route is therefore active: retain the Build-420 source projection and close its lifetime with HyperOS's native fake/final `appearance` ownership fact. Build 428 on `fix/control-center-appearance-boundary` is a read-only diagnostic checkpoint to map the exact two-boolean `onAppearanceChanged(...)` semantics before any runtime gating is implemented; no local threshold or inferred boolean meaning is allowed.
- Repository-automation baseline: checkpoint-driven CI from PR #139 remains active; Canary admission hardening and bounded automation-only merge delegation were accepted through PR #143/#144. PR #149 (`497be75c1754e49cb7a49b6abd73dcbd3bc010b3`) adds base-to-head validation-surface reporting, mixed runtime/build/CI/tooling warnings, and readable routing reasons without weakening Full gates; history-preserving `main -> dev` sync `2e9b1716849d6709342a446f63e5b886c0aed9ae` passed dev Full #1317. These automation changes do **not** create a new runtime Build; Build 418 remains the integrated runtime baseline.
- Phase-2A integration: PR #105 merged to `dev` as `2f584c3b393dc5ee606284426aa95a9d6beae5d5`; former stacked PR #100 is closed as superseded.
- Phase-2B panel/scene-owner integration: PR #138 merged to `dev` as `a25cb5ce2aeab235cfaed579474df70596f03a63`.
- PR #142 (`fix/home-hun-ownership`) is merged and closed; its Build-413 HUN/shallow-pull lifetime correction is part of `dev` and was promoted to `main` through PR #147.
- Active development display line: **0.0.2**
- First planned formal release: **1.0.0**
- Target profile: HyperOS SystemUI `17.03.260226.r`
- Modern Xposed API: 102
- Application ID: `com.chaners.combinedstatus`

`main` and `dev` runtime baselines remain unchanged by documentation-only commits. Build 424 is now the accepted `dev` runtime baseline; later record-only `dev` commits may inherit that runtime validation only when their non-runtime diff is proven.

## Current phase

The project has completed the current **Phase 2A — 0.0.2 Home carrier / presentation architecture** gate for `dev` integration and is moving into **Phase 2B — unlocked Home ownership continuity + bounded Control Center transition bridge**.

The selected Home direction is an existing-host composition rather than the superseded permanent extra-participant / occupancy-handoff route:

`MiuiNotificationStatusContainer / system_icon_area (HostSession) -> MiuiStatusBatteryContainer / system_icons.overlay (visual carrier) -> resolved Home layout -> Combined Status renderer`

SystemUI remains authoritative for surrounding native layout, Battery presentation/hide behavior, native tint semantics, Home-host scene visibility, and charging/Super-Island motion. Combined Status owns its compact composition plus only narrowly scoped, reversible Home presentation state.

For the pinned HyperOS target, Notification Shade itself does not present the status-icon row; therefore Phase 2B does **not** project Combined Status into Notification Shade. Build-423 device evidence proves Notification Header progress is motion context rather than the complete Home visibility authority. Exact-target source instead traces `StatusBarVisibilityInteractor.shouldHomeStatusBarBeVisible -> HomeStatusBarViewModelImpl.systemInfoCombinedVis -> HomeStatusBarViewBinderInjector -> mEndSideContent = R.id.system_icons`. Build 424 therefore places the unlocked/Home render overlay on that exact `MiuiStatusBatteryContainer(system_icons)` owner so native alpha/visibility/transition lifecycle is inherited without a project-local shade gate. Control Center projection is now a bounded transition bridge only: the fully expanded endpoint must return to native status-bar presentation. Keyguard/lockscreen follows as a separate source adapter after Phase 2B, reusing shared domain/render semantics without sharing mutable Home host/session ownership.

## Active work checkpoint — Build 428 native appearance probe

- Work branch: `fix/control-center-appearance-boundary`, based directly on accepted `dev` Build 424.
- Runtime identity: Build 428 / `20260928-428` once CI validates the branch head.
- Scope: add one optional Detailed/Canary diagnostic Hook for exact-target `ControlCenterHeaderExpandController$controlCenterCallback$1.onAppearanceChanged(boolean, boolean)`; record both native booleans verbatim plus existing read-only Header/island context.
- Production behavior: unchanged from Build 424. The probe writes no visibility, alpha, translation, geometry, appearance, slot, or animation state.
- Device gate: required. The exact target proves that `appearance` owns QS_FAKE/QS visual handoff, but the plugin-side boolean producer semantics are not present in the reviewed SystemUI APK and must not be guessed.
- Next executable correction after evidence: if the native semantics are stable, restore the device-accepted Build-420 source projection and end it at the verified native appearance boundary; otherwise fail native and continue source review.

## Current runtime checkpoints

- Builds 386-393 are historical evidence for the superseded permanent extra-participant route; they are not the current architecture premise.
- **Build 397** is the first device-accepted checkpoint for the tested Phase-2A charging-carrier scenarios.
- **Build 398** strengthens stable width authority by using the live native `battery_icon_container`; it is carried forward but was not separately device-promoted before the next checkpoints.
- **Build 399** separates active/inactive battery-ring arc compositing without reopening Home carrier ownership.
- **Build 403 / `20260927-403`** established the current HyperOS battery semantic-color implementation. Runtime source: `97ef67e648906a4b9bb2ce4d7dd390e955831189`.
- **Build 404 / `20260927-404`** is a completed but **device-rejected optical-parity A/B checkpoint**. Runtime source: `614c6ae96f1753088e21ce3568d969b900852081`. It removed percentile alpha remapping while retaining the existing bitmap-probe rendering path; target-device feedback shows the center presentation is visually worse than Build 403, so authored-alpha preservation alone is not an accepted fix.
- **Build 405 / `20260927-405`** is a completed but **device-rejected direct-final-Drawable optical-parity A/B checkpoint**. Runtime source: `bf8091c8680dec7b85c58afded7f476ec95ca49d`. It removed the intermediate final-presentation bitmap/resample stage, but target-device screenshots still show the native center glyph materially lighter/lower-opacity than neighboring native status icons across light and dark surfaces. The direct-Drawable mechanism remains preferable to the superseded bitmap presentation path, but it is not sufficient for parity by itself.
- **Build 406 / `20260927-406`** is a completed but **device-rejected tint-variant A/B checkpoint**. Runtime source: `3d5e9d2339824c6d19e50dda170917559369135b`. Work Branch Canary #335 passed all CI/signing gates, but target-device evidence shows the center Wi-Fi glyph remains optically lighter/lower-coverage than the outer ring **even when `centerFollowsBatteryColor=false`**. Therefore the Build-406 hypothesis that the remaining defect was confined to the custom/battery-color tint branch is rejected.
- **Build 407 / `20260927-407`** is **device-accepted for the native-center opacity/resource-mask correction**, but overall optical balance remains open. Runtime source: `ffe746b24252f974db05e1fa4381ed5c56f0e73e`. The maintainer reports that the prior center-transparency defect is resolved. New same-device screenshots show the battery ring still reads darker/heavier than the native center and four mobile dots even when all three consume the same green semantic tint. Work Branch Canary #337 / run `36313837646` passed all gates and produced artifact `10929728522`.
- **Build 408 / `20260927-408`** is the maintainer-accepted Phase-2A working baseline for `dev` integration. Runtime source: `8a7a39d8297fe926387d56cc8ff5be4b08405f4a`. Work Branch Canary #338 / run `36315043013` passed all gates and produced artifact `10930143406`. The maintainer considers the current color/native-center result basically compliant with the intended design. A small residual ring/center/dot optical-weight difference may remain and is explicitly deferred as visual polish rather than treated as a blocker.
- **Build 409 / `20260927-409`** is a completed but **device-incomplete Phase-2B checkpoint**. Executable source: `4ab7b490617757e34c0ea8e0b59e3d7a16bae9ad`; Fast #1129 and Work Branch Canary #363 passed. Device video + diagnostics show the promoted notification-shade gate does transition Home to `homeEligible=false` and runs the existing cleanup path, but the reported leak reproduction is driven by **Control Center**. During that transition `ControlCenterExpandControllerDelegate` reports `visible=true` and changing native fraction while Build 409 leaves Control Center callbacks diagnostics-only, so Home remains eligible and the compact overlay can overlap the native icons. Build 409 is therefore not accepted as the complete shallow-pull fix.
- **Build 410 / `20260927-410`** is the current signed Phase-2B Home scene-lifetime device-test candidate. Executable runtime source: `6e2fc55944753c6cb9ef22f537008c97217f17e1`. Fast Build #1143 / run `36319763905` passed on test head `5320bf87253128de290b4b0809694949a02b6c38`; trusted Work Branch Canary #377 / run `36319940085` passed exact checkout, both pinned HyperOS scene contracts, tests/build, Haple signature, Modern Xposed metadata and non-debuggable validation and produced artifact `10932655599`. APK size: `3309598` bytes; SHA-256: `8c3f3011c214e66d20a89698e902191bdd8bc039803a6c824a261170a5cbf0eb`. Runtime is frozen pending device validation.
- **Build 411 / `20260927-411`** is **device-accepted for the panel/scene-owner correction but visually rejected at the 8.0 ring checkpoint**. Executable runtime source: `aaaaf0810b114b1e90a3de3f1520721a420da2d0`. Fast #1150 and trusted Canary #384 passed. Maintainer device feedback confirms notification-shade / Control Center down-up behavior is now correct, so the persistent Home owner / scene-visibility separation is retained. The 8.0 ring remains visually inferior to the previously seen 8.25 geometry, including endpoint/lower-opening harmony, so 8.0 is not the final visual baseline.
- **Build 412 / `20260927-412`** is the accepted integration candidate for the current Phase-2B panel handoff + preferred ring baseline. Executable source: `f794a7c01513364eefc726316fcaf4058d581683`. Fast Build #1156 / run `36323242298` passed on tested head `74c234060f77da958c4eae21e8d170e29f2da1cb`; trusted Work Branch Canary #390 / run `36323397870` passed exact checkout, target-profile, tests/build, Haple signature, Modern Xposed metadata and non-debuggable validation and produced artifact `10933571263`. Extracted APK size: `3309602` bytes; SHA-256: `51daaab32c5f3a152a41340eb0c6b8d2cb93f2448b83a32a980942a85cc8e1e8`. Panel down/up behavior is maintainer-accepted from Build 411; the 8.25 ring is explicitly preferred over 7.5 and 8.0 and does not require a separate device A/B before `dev` integration.- **Build 413 / `20260927-413`** is **device-accepted for the notification/HUN ownership correction** on PR #142. Executable runtime source closes the missed production caller and advances identity through `75034b7efa9d0c8d59b7e7b9d87e9cc382172d23`; the exact tested PR head after automation back-sync is `15b92d64440eb565e44ce8a7dda3739c9ab8964e`. Fast Build #1195 passed, trusted Work Branch Canary #397 / run `36336524857` passed exact source resolution, target profile, unit/build, Haple signature, Modern Xposed metadata and non-debuggable checks, and produced artifact `10937297772`. Extracted APK SHA-256: `a0c39bc21e81175b7c6fafed0316cd7b807e90ed8515ce257c69b4091088dff3`. Maintainer focused device feedback reports the HUN disappearance is fixed; the accepted shallow-pull handoff remains intact.

- **Build 416 / `20260928-416`** is **device-rejected for Hot Reload tint continuity**. Fast #1243 and signed Work Branch Canary #400 passed, but maintainer screenshots plus Detailed diagnostics show Combined Status can remain inverted relative to neighboring Home status icons after Hot Reload; a full SystemUI restart restores correct behavior. The location-aware dispatcher resolver itself changes between `#bf000000` and `#e6ffffff`, while the Hot Reload path initially restores `#bf000000`. The key discriminant is lifecycle: the same executable behaves correctly after SystemUI recreation, so steady cold-start tint policy is not reopened by default.
- Build 406 trusted validation: owner `/canary` Work Branch Canary #335 **succeeded**. Validated PR head `80f371784ffaee406dd6ea5728219eeee5913318` differs from the frozen runtime source only in `CURRENT.md` and `DEVLOG.md`, so executable content remains exactly Build 406. Artifact `10929711688`; artifact ZIP digest `sha256:c7af997217acd171c66beb860d7212c0d72fd672a38978f7b5c2eb5a524f11ba`; extracted APK SHA-256 `e086d8914fece7ba8ea86200756f4a6366d56dadaa5510846618a4077e6ddd80`.
- **Build 418 / `20260928-418`** is **device-accepted for Hot Reload Tint continuity and repeated app/Home scene switching**. Maintainer validation reports normal behavior after repeated light/dark transitions without a SystemUI restart. Detailed diagnostics show Hot Reload restoration with `statusIconTint` rebased to live SystemUI authority and subsequent event-driven renderer commits keeping `appliedTint`, `statusIconTint`, and `liveStatusIconTint` aligned. Exact tested PR head `42f350c2bb8d7338906469454fadabc5dcb629de`; Fast #1261 and Work Branch Canary #405 passed. This closes the shared Home Tint lifecycle blocker for Phase 2B.
- Documentation/test-only commits after a frozen executable checkpoint may advance the PR head without creating a new runtime Build; runtime identity changes only when executable/build metadata changes.

Build 403 validation already established:
- Fast Build #1063: **success**;
- signed Work Branch Canary #322: **success**;
- target-profile, unit/build, Modern Xposed metadata, signing, non-debuggable and artifact checks: **passed**.

Build 404 validation:
- source review: **passed**;
- Fast Build #1088 on validation PR #130: **success** at tested head `ad55baa47eecadfa7fe1968556d5c7f13a1be460`;
- signed Work Branch Canary #324: **success**; the Canary job explicitly checked out the same tested head and passed target-profile, unit/build, Modern Xposed metadata, Haple signature, non-debuggable, and artifact checks;
- compare evidence from runtime source `614c6ae96f1753088e21ce3568d969b900852081` to tested head `ad55baa47eecadfa7fe1968556d5c7f13a1be460` shows only `CURRENT.md` / `DEVLOG.md` documentation changes, so the validated executable runtime remains Build 404;
- signed artifact: `CombinedStatus-0.0.2-HyperOS-20260927-404-canary.apk`;
- artifact ZIP digest: `sha256:694c81c37b5dc8227f0da076211ae538ac9250770da2eb97003be0727734c79f`;
- extracted APK SHA-256: `eb16169738f3e16bcd208463ae4fc638898c3c46f2ca3b43efea3bda625519f1`;
- device optical-parity acceptance: **rejected / regressed versus Build 403**; battery semantic-color acceptance remains a separate open device gate.
- validation-only PRs #129 and #130 are closed after evidence capture; neither is mergeable product work.

## Active runtime issues / validation

### Visual intensity / optical parity — 8.25 ring baseline accepted in dev

Build 407 resolved the blocking native-center opacity/resource-mask defect. Build 408 reduced the ring from the earlier effective 8.25 to 7.5 while retaining the accepted dot size; Build 411 tried an intermediate 8.0.

Maintainer device conclusion:
- 7.5 is too thin;
- 8.0 still looks worse than the original 8.25;
- the thinner ring also reduces the `ROUND` endpoint radius and makes the lower-opening curvature look less visually unified with the four-dot group;
- the preferred baseline is therefore the previously experienced **8.25 ring**.

Selected Build-412 visual correction:
- restore ring stroke exactly to **8.25 canonical units**;
- retain `Paint.Cap.ROUND`; do not invent a custom cap shape;
- keep four-dot radius, center geometry, tint/alpha, opening angles and scene ownership unchanged;
- let the existing lower-opening solver recompute balanced edge gaps from the restored stroke.

This is now the accepted `dev` visual baseline. The 8.25 restoration passed Fast, signed Canary, and post-merge Integration validation; no dedicated thickness-only maintainer A/B is required.

### Battery semantic colors — implemented and accepted for dev

Build 403 uses HyperOS `MiuiBatteryMeterIconView.getProgressStatus()` as semantic-state authority. Charging, power-save, performance, and low-battery colors come from SystemUI's already-loaded native color fields when HyperOS optimization allows them; normal state and unavailable semantic colors fall back to the resolved native status-icon tint.

No user-facing per-state color picker/source selector is exposed yet. The future policy seam remains: **System default / Follow status icon / Custom** for NORMAL / CHARGING / POWER_SAVE / PERFORMANCE / LOW.

### Home -> panel scene boundary / Control Center projection — Build 424 active

Build 420 remains device-accepted evidence for the Control Center carrier and readiness-ordered handoff:
- Notification Shade has no Combined Status projection surface on this pinned target.
- `realSystemIcons` / `MiuiStatusBatteryContainer` is a verified native Control Center transition carrier.
- Entry remains projection-ready before the source steady scene yields; reverse motion restores the source before bridge cleanup.
- Control Center geometry, transition motion, and native peer animation remain SystemUI-owned.
- New product boundary: this projection is a **partial-pull transition bridge only**. Fully expanded Control Center must be native SystemUI status-bar presentation, not a persistent Combined Status scene.

Builds 421-423 are rejected for Notification-Shade first/last-frame continuity. Build 423 established that the exact Notification Header callback supplies valid continuous motion progress, but device evidence proves that progress is not the native Home-status-bar visibility authority.

**Build 424 root-cause correction — inherit the native Home end-side carrier lifecycle:**
- exact-target source traces `StatusBarVisibilityInteractor.shouldHomeStatusBarBeVisible` into `HomeStatusBarViewModelImpl.isSystemInfoVisible -> systemInfoCombinedVis`;
- `HomeStatusBarViewBinderImpl` binds `mEndSideContent` to `R.id.system_icons`;
- exact `system_icons.xml` shows that `system_icons` is the root `MiuiStatusBatteryContainer` containing status icons and Battery;
- `HomeStatusBarViewBinderInjector.showEndSideContent()/hideEndSideContent()` applies native alpha / visibility / translation animation to that `mEndSideContent`;
- the pre-424 Combined Status visual was instead attached to the parent `MiuiNotificationStatusContainer.overlay`, outside the child `system_icons` animation owner, which is why a project-local Notification fraction gate was needed and could go out of phase at the first/last frame;
- Build 424 moves only the Combined Status Home render overlay to the exact native `MiuiStatusBatteryContainer(system_icons).overlay`;
- the Notification Header runtime Hook, Notification Home-eligibility state, Hot Reload query, and per-drag Home visibility writes are removed;
- the legacy Hot Reload payload slot is retained as a null compatibility field only; it is not an active runtime authority;
- Build-420 Control Center carrier/handoff mechanics are unchanged inside Build 424; their future scope is now explicitly transition-only, with native-only ownership at the fully expanded endpoint.

Build 424 is the current runtime checkpoint (`20260928-424`). Exact executable source: `2556a098d35c202e1c5645a06e73757744f721e1`. Later branch commits remain test/documentation/governance-only as of the current review. Static review confirms no active Notification-Shade visibility Hook/gate remains and the active target-profile panel Hook is only Control Center `onVisibleChanged(Z)` (plus optional bounded expansion diagnostics). A history-preserving refresh merged current `dev` into the work branch at `ca1bbf7e10c485f34846add633c34d06a163e7e8`. Ready Full Build #1328 / run `36418043111` succeeded on exact tested head `3cbf8523cfafeb99a58dcd213053e9a2e020f71f`. Owner-requested Work Branch Canary #417 / run `36418655598` then resolved and checked out that same exact SHA, passed target-profile/tests/build, Modern Xposed metadata, Haple signature and non-debuggable checks, and uploaded artifact `10968671147`. Extracted APK SHA-256 is `7e1a7bf035207718de3d74c580b87c3d5f20df648a98a78ef8f560d25c95e778`. PR is Draft and Build 424 runtime is frozen pending focused device validation; no device acceptance is claimed yet.

**Maintainer working scene concept — planning input, not yet a verified lifecycle contract:**
- The maintainer's current conceptual split is: unlocked/Home steady, locked/Keyguard steady, partial Control Center pull with HyperOS transition continuity, and fully expanded Control Center native-only.
- Treat that split as a product-intent input, not as proof of the underlying SystemUI lifecycle topology.
- Exact-target review now establishes a cleaner native ownership topology: Home and Keyguard register separate real source carriers into `ControlCenterFakeViewController`; native `StatusBarState` selects the source; `ControlCenterFakeStatusIcons` provides a complete `QS_FAKE` transition status bar with native tint/island/lifecycle; `onAppearanceChanged` switches fake/real Control Center visual ownership.
- This makes `source steady adapter -> native QS_FAKE transition carrier -> native QS destination` the leading low-maintenance candidate, but it is **not yet an implementation decision**. First-frame readiness, suppression/masking, Hot Reload and device behavior still require review.
- Home and future Keyguard must not be forced into one View ownership model merely to match the conceptual split.
- This investigation does not widen Build 424: Build 424 remains the Home-carrier/Notification-writer correction only.

## Non-negotiable boundaries

- Home is the currently implemented persistent Combined Status source surface. Build 420 verifies a Control Center carrier/handoff mechanism. The maintainer currently prefers partial-pull transition continuity with a native-only fully expanded endpoint, but that remains a design hypothesis pending exact lifecycle review. Keyguard and AOD remain native-only in current runtime.
- Unsupported/unverified surfaces remain native until their own host/lifecycle/handoff contract is validated.
- Reuse authoritative HyperOS/SystemUI state and resources when a verified source exists.
- Native peer geometry, Battery translation/alpha/visibility, and island animation remain SystemUI-owned.
- No polling, per-frame follower, timing retry, duplicate layout-occupancy owner, or magic translation/margin compensation should be introduced for scene handoff.
- Fail toward native SystemUI when an ownership, compatibility, or restoration contract cannot be established safely.

## Repository / branch synchronization

- PR #138 is merged into `dev` as merge commit `a25cb5ce2aeab235cfaed579474df70596f03a63`, carrying the accepted Build-411 panel-owner stabilization and Build-412 8.25 ring baseline.
- PR #105 remains the earlier accepted Phase-2A integration boundary.
- PR #100 is closed as superseded by that final integration PR.
- PR #138 is merged into `dev` as the accepted Phase-2B panel/scene-owner + Build-412 8.25 ring integration boundary.
- PR #134 installed the trusted work-branch Canary fallback into `main`; PR #136 history-preserving back-synced that accepted process state into `dev`.
- PR #135 then fixed Markdown backtick escaping in the Canary source summary without changing validation/trust semantics; PR #137 history-preserving back-synced that follow-up into `dev`.
- Validation-only PRs #131/#132 and the dev-based automation review PR #133 are superseded and closed without merge.
- The documentation-governance baseline is accepted in `main` and synchronized into `dev`; active runtime branches must preserve it when updated/merged rather than restoring older workflow/process text.
- PR #138 is merged into `dev` as the accepted Phase-2B panel scene-ownership + 8.25 ring integration boundary (`a25cb5ce2aeab235cfaed579474df70596f03a63`).
- PR #142 is merged into `dev` as `2aa6833cfca69a59af5027a7855b7d8282dbade9`, making Build 413 the integrated HUN/shallow-pull lifetime baseline. Trusted Integration Build #1200 / run `36337302873` passed and produced signed Canary artifact `10937951856`.
- `validation/dev` was advanced to exact dev candidate `36ce04011f0a1fb2c5dd185d911b639bb1787408` after maintainer device acceptance. Promotion readiness reported READY; PR #147 promoted that exact candidate to `main` with merge commit `3114ade06bcb4846a5654a70f38ea572f5b47b37`. Promotion Full #1213, post-merge Full #1214, and Push on main #59 all passed.
- Promotion PR #147 promoted exact validated `dev` SHA `36ce04011f0a1fb2c5dd185d911b639bb1787408` to `main` using the required merge commit `3114ade06bcb4846a5654a70f38ea572f5b47b37`. Promotion Full #1213 and post-merge main Full #1214 both passed; `Push on main` #59 also passed.
- PR #139 replaced automatic per-Fast work-branch Canary follow-up with checkpoint-driven validation: active runtime PRs stay Draft during iteration, Ready is reserved for meaningful Fast checkpoints, and signed work-branch Canary is requested explicitly only when device evidence is needed.
- PR #141 history-preserving back-synced that accepted CI/governance state into `dev` without replacing the Build-412 runtime tree. The direct `main -> dev` PR #140 was closed after branch-history conflicts were identified; the accepted sync used an explicit two-parent merge preserving both histories.
- CI self-validation for the policy change passed at PR Build #1168, `main` push #1169, sync PR Build #1170, and trusted `dev` push #1171. The generated artifacts retain stable runtime build identities (`main` Build 351 / `dev` Build 412), so these automation checks are not new application Builds.
- PR #99 remains separate open historical work and is not an accepted baseline; any useful delta must be reconciled against the current line before reuse.



### Control Center lifecycle review — exact-target evidence, no runtime change

The current lifecycle investigation now verifies that HyperOS itself registers both source containers with `ControlCenterFakeViewController`: unlocked `MiuiPhoneStatusBarView.mStatusBatteryContainer` becomes `statusBarSystemIcons`, while `MiuiKeyguardStatusBarView.mSystemIconsContainer` becomes `keyguardSystemIcons`. Native status-bar state selects the current `realSystemIcons` source. The Control Center fake status bar is a complete `QS_FAKE` status representation with native tint/attach/island lifecycle, while the fully expanded Control Center has a separate `QS` native status bar. Native `appearance` selects fake-vs-real visual ownership independently of expansion motion.

This supports a candidate source -> native fake transition -> native real destination topology and argues against a project-owned six-state scene machine. It is evidence only; Build 424 remains unchanged and must be validated first. The plugin-side producer semantics for `appearance` remain outside the reviewed SystemUI APK and must not be guessed.



### Build 424 validation recovery

The earlier absence of pull-request Build runs was not a GitHub-wide Actions outage. PR #146 had diverged from current `dev` and was not mergeable; GitHub does not run `pull_request` workflows for conflicted PRs.

The work branch was history-preserving refreshed from exact dev tip `947c13956f2b4cbe08faf21de73b3a2f1b7a8b81`. During the synchronization experiment, using long-lived `dev` directly as a sync-PR head triggered GitHub's delete-head-branch behavior when the sync PR merged. `dev` was immediately recreated at the exact same SHA, with no runtime/content change. The permanent process rule is now to use a temporary `sync/*` head for long-lived branch synchronization.

After conflict resolution, PR #146 became mergeable and Full Build #1321 / run `36413531047` succeeded on `ca1bbf7e10c485f34846add633c34d06a163e7e8`. Draft Light #1320 exposed only trailing whitespace in DEVLOG; that repository-text defect is being cleaned before the final exact-head Full checkpoint.


## Immediate next step

1. Keep Build 424 executable source `2556a098d35c202e1c5645a06e73757744f721e1` frozen.
2. Obtain one successful pull-request Build on the final documentation-synchronized exact PR head.
3. Request one exact-head signed Canary only after that successful exact-head Build.
4. Device-test Notification-Shade first/last-frame continuity, Control Center regression, Hot Reload, and one lock/unlock smoke pass.
5. Do not mix the post-424 QS_FAKE Control Center redesign into Build 424; discuss/select that follow-up only after Build 424 device evidence.

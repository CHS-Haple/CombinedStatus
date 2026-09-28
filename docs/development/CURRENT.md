# Current Development State

This file is the concise recovery point for active Combined Status development. Read it after `CONTRIBUTING.md`. Keep chronological Build/investigation history in `DEVLOG.md`, future direction in `ROADMAP.md`, version semantics in `VERSIONING.md`, and record-writing rules in `RECORDING.md`.

## Repository baseline

- Last refreshed: 2026-09-29
- Stable branch: `main`
- Stable runtime baseline: Build 413 / `20260927-413`, promotion merge commit `3114ade06bcb4846a5654a70f38ea572f5b47b37`
- Integration branch: `dev`
- Integration build baseline: Build 441 / `20260929-441`, PR #156 squash integration `eab6af041b689a2355b83914c26f2e5735c522ce`
- Build 441 is the current Integration-validated `dev` baseline. Final exact-head PR Fast #1507 / run `36475707633` passed after acceptance/evidence closure; PR #156 then squash-merged as `eab6af041b689a2355b83914c26f2e5735c522ce`, and post-merge `dev` Integration #1508 / run `36476038417` passed target-profile, tests/build, Modern Xposed metadata, Haple signing/signature verification, Canary non-debuggable validation and artifact upload. Git tree equality confirms the squash integration content is identical to the final accepted PR head. Maintainer device validation accepts the Build-441 Hot Reload generation handoff and bounded QS_FAKE ownership behavior; Build 438 companion-app UI acceptance remains carried forward. Build 413 remains the current `main`-promoted stable runtime baseline.
- Phase-2B Home / Notification-Shade checkpoint PR #146 is merged. Build 420 remains **device-accepted historical evidence for Control Center source geometry/readiness-ordered handoff**, Builds 421-423 are **device-rejected Notification-Shade experiments**, and Build 424 is **accepted/integrated** for the native Home `system_icons` carrier correction. Builds 425-427 are **device-rejected child `QS_FAKE.system_icon_area` compact-carrier experiments**. Build 428 proves `realSystemIcons` is only the selected Home/Keyguard source reference and is natively hidden during current Control Center ownership, so it is not the active transition display host. Build 430 device evidence verifies the top-level `ControlCenterFakeStatusIcons` root as the native fake/final appearance owner in both normal and charging-island pulls; Build 431 moves the bounded transition projection onto that root; fully expanded Control Center remains native-only, and Keyguard/AOD remain native-only until separately verified.
- Repository-automation baseline: checkpoint-driven CI from PR #139 remains active; Canary admission hardening and bounded automation-only merge delegation were accepted through PR #143/#144. PR #149 (`497be75c1754e49cb7a49b6abd73dcbd3bc010b3`) adds base-to-head validation-surface reporting, mixed runtime/build/CI/tooling warnings, and readable routing reasons without weakening Full gates; history-preserving `main -> dev` sync `2e9b1716849d6709342a446f63e5b886c0aed9ae` passed dev Full #1317. These automation changes do **not** create a new runtime Build; Build 418 remains the integrated runtime baseline.
- Phase-2A integration: PR #105 merged to `dev` as `2f584c3b393dc5ee606284426aa95a9d6beae5d5`; former stacked PR #100 is closed as superseded.
- Phase-2B panel/scene-owner integration: PR #138 merged to `dev` as `a25cb5ce2aeab235cfaed579474df70596f03a63`.
- PR #142 (`fix/home-hun-ownership`) is merged and closed; its Build-413 HUN/shallow-pull lifetime correction is part of `dev` and was promoted to `main` through PR #147.
- App-UI checkpoint Build 438 is integrated in `dev`: Floating Glass matches the pinned MIUIX example, icon-only vs icon-with-label is persisted and shared by live navigation/preview, and the Appearance preview keeps fixed bounds with bottom-anchored navigation content. Maintainer device validation and post-merge Integration #1487 passed.
- Active companion-app UI refinement: `feat/diagnostics-ui-refinement` keeps diagnostics report actions whole-row clickable but moves their decorative affordances into MIUIX `BasicComponent.startAction`; export uses `FileDownloads`, share uses `Share`, neutral foreground/disabled colors replace the isolated primary-blue trailing treatment, and the framework row now reads `Modern Xposed API 102` / `Framework API`. Report generation/export/share behavior is unchanged; automated validation is pending.
- Active development display line: **0.0.2**
- First planned formal release: **1.0.0**
- Target profile: HyperOS SystemUI `17.03.260226.r`
- Modern Xposed API: 102
- Application ID: `com.chaners.combinedstatus`

`main` remains on the Build-413 stable runtime line. `dev` now carries device-accepted Build 441 plus the previously accepted Build-438 app-UI checkpoint, retaining MIUIX `0.9.4-5c91d5e5-SNAPSHOT`. Later record-only `dev` commits may inherit Build-441 integration validation only when their non-runtime diff is proven.

- Build 428 device evidence is complete and PR #154 is superseded as a frozen diagnostic line. PR #156 (`fix/control-center-fake-root`) is merged and closed; Builds 430-441 form the accepted evidence chain for top-level QS_FAKE ownership, deterministic compact cutover and continuous Hot Reload generation handoff. PR #160 closes the charging-island trajectory question as verified native HyperOS behavior. Exact-target endpoint review also verifies the fully expanded Control Center as a native-only endpoint through HyperOS fake/final appearance ownership. Phase 2B is complete for the current `dev` baseline; the next runtime phase is Phase 3 Keyguard / lockscreen / AOD scene completion.

## Current phase

The project has completed **Phase 2A — 0.0.2 Home carrier / presentation architecture** and **Phase 2B — unlocked Home ownership continuity + bounded Control Center transition bridge** for the current `dev` baseline. Active runtime work now moves to **Phase 3 — Keyguard / lockscreen / AOD scene completion**.

The selected Home direction is an existing-host composition rather than the superseded permanent extra-participant / occupancy-handoff route:

`MiuiNotificationStatusContainer / system_icon_area (HostSession) -> MiuiStatusBatteryContainer / system_icons.overlay (visual carrier) -> resolved Home layout -> Combined Status renderer`

SystemUI remains authoritative for surrounding native layout, Battery presentation/hide behavior, native tint semantics, Home-host scene visibility, and charging/Super-Island motion. Combined Status owns its compact composition plus only narrowly scoped, reversible Home presentation state.

For the pinned HyperOS target, Notification Shade itself does not present the status-icon row; therefore Phase 2B does **not** project Combined Status into Notification Shade. Build-423 device evidence proves Notification Header progress is motion context rather than the complete Home visibility authority. Exact-target source instead traces `StatusBarVisibilityInteractor.shouldHomeStatusBarBeVisible -> HomeStatusBarViewModelImpl.systemInfoCombinedVis -> HomeStatusBarViewBinderInjector -> mEndSideContent = R.id.system_icons`. Build 424 therefore places the unlocked/Home render overlay on that exact `MiuiStatusBatteryContainer(system_icons)` owner so native alpha/visibility/transition lifecycle is inherited without a project-local shade gate. Control Center projection is now a bounded transition bridge only: the fully expanded endpoint must return to native status-bar presentation. Keyguard/lockscreen follows as a separate source adapter after Phase 2B, reusing shared domain/render semantics without sharing mutable Home host/session ownership.

## Integrated checkpoint — Build 441 continuous Hot Reload presentation handoff

- Integration: Build 441 is merged into `dev` as `eab6af041b689a2355b83914c26f2e5735c522ce`; MIUIX remains `5c91d5e5`.
- Work branch / PR: `fix/control-center-fake-root` / #156 is merged and closed.
- Runtime identity: Build 441 / `20260929-441`; executable source begins at `500ae425c5fbe0e9156eefd76c6968aee6e79ca7` with wiring correction `bd224ef39b1f15c4b3f057b14b9835b21dbc5dde` and diagnostic wording cleanup `7a3c1cd9f5708c91cd5eddf1b004da6eeddad016`.
- Build 440 device feedback **accepts the original raw-native/overlap correction**: after settling, repeated Control Center pulls and SystemUI restart no longer reproduce the previous native QS_FAKE leak. Two Hot Reload-only defects remain:
  1. Hot Reload -> immediate Control Center pull can temporarily show no Combined Status until the transferred QS_FAKE owner becomes compact-ready.
  2. Pressing Hot Reload visibly flashes Combined Status and makes neighboring status icons shift left then return.
- Build-440 diagnostics show Hot Reload schedules restore at 03:23:39.512 and reactivates Home presentation around 03:23:39.548, but transferred QS_FAKE compact readiness does not arrive until 03:23:42.824. This explains the immediate-pull blank interval even though native QS_FAKE is correctly pre-masked.
- Source review identifies the layout pulse root cause: old-generation teardown removes the Home render visual, restores native presentation clip/reservation state and explicitly requests layout **before** the new generation reattaches. Even if the raw Wi-Fi/mobile/Battery glyphs do not have time to draw, restoring their layout ownership is sufficient to make adjacent icons reflow for one frame.
- Build 441 replaces the old **tear down -> native intermediate frame -> restore** sequence with a classloader-neutral generation handoff:
  - `onHotReloading` captures state, the attached Fake root, whether its compact presentation is already ready, and a Java `Runnable` old-generation cleanup callback; it no longer posts teardown in advance.
  - The new generation first installs its hooks/runtime sources, then invokes the old-generation handoff callback from the same main-thread restore task.
  - Old presentation state is restored without an intermediate `requestLayout()`; old renderer/listeners are released and the new renderer/presentation is attached in the same main-thread turn.
  - If the transferred QS_FAKE was already compact-ready, the new generation adopts that existing compact geometry immediately and treats the following native layout only as refresh, instead of regressing to pending readiness.
- Fail-native semantics remain: if the new generation cannot restore the transfer/handoff, the reload is reported as incomplete/restart-required rather than leaving an unowned mixed presentation.
- No delay, timer, polling, frame callback, animation patch, translation/alpha writer, second mask writer or QS-real mutation is added.
- **Compatibility gate:** the first transition from an already-running Build 440 generation into Build 441 still originates from old Build-440 code and therefore cannot supply the new v9 handoff callback. Focused validation of the new no-flash protocol must be performed after Build 441 is already active, then Hot Reload again (441 -> 441).
- **Device result: accepted.** After Build 441 is active, repeated 441 -> 441 Hot Reload no longer shows the previous Combined Status flash / peer-icon horizontal layout pulse, and immediate Control Center pull no longer exhibits the temporary Combined Status blank. The earlier Build-440 raw-native / native+Combined overlap correction also remains stable, including after SystemUI restart.
- Build 441 is therefore the current device-accepted and Integration-validated `dev` runtime checkpoint for the Home -> bounded QS_FAKE transition and Hot Reload generation handoff.
- **Charging-island geometry review — exact-target investigation closed the 30 px compensation hypothesis:** HyperOS SystemUI `17.03.260226.r` (APK SHA-256 `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`) was recovered together with JADX 1.5.6 and reviewed directly. `ControlCenterHeaderExpandController.updateLocation()` forces `batteryWidthDiff=-controlCenterBattery.width` when Battery Island is active, and `controlCenterCallback.onExpansionChanged()` adds that **unscaled** term only to the QS_FAKE root while the ordinary status-icon translation remains progress-scaled. With observed `normalStatusIconsTx=181` / `batteryWidthDiff=-135`, QS_FAKE therefore moves from `46` at progress 0 to `-135` at progress 1; non-island `46/0` moves `46 -> 0`. `MiuiBatteryMeterView.updateIslandChanged()` also hides/removes native Battery occupancy through `MiuiStatusBatteryContainer.setIsHideBattery(true)` and uses the full Battery View width for its island motion. Combined Status is already end-anchored to the stable 105 px slot inside `ControlCenterFakeStatusIcons.overlay` and inherits the root translation. No independent 30 px local-anchor defect is established. **Do not add a 30 px compensation or feed `batteryWidthDiff` into Combined translation.** Reopen only with frame-level evidence that Combined diverges from native QS_FAKE peer geometry, not merely because island and non-island trajectories differ.

- **Fully expanded endpoint review — exact-target contract verified:** `ControlCenterHeaderExpandController$controlCenterCallback$1.onAppearanceChanged(appearance, animate)` is the native fake/final visibility authority. When `appearance=true`, HyperOS drives final `ControlCenterStatusBarIcon` alpha to `1` and `ControlCenterFakeStatusIcons` alpha to `0`; when false, it reverses them. `onExpansionChanged(progress)` controls translation only and does not own the fake/final alpha handoff. Build-441 device diagnostics reach `fraction=1.0` and observe the QS_FAKE root at alpha `0.0` before the return transition. Combined Status lives only in `ControlCenterFakeStatusIcons.overlay`, explicitly inherits root alpha/translation, and does not mask or mutate final `ControlCenterStatusBarIcon`. Therefore fully expanded Control Center is already **native-only by native appearance ownership**. Do not add a project `fraction==1` visibility threshold, custom endpoint fade, or final-surface suppression.

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

1. Keep the Build-429 `dev` MIUIX pin at `0.9.4-5c91d5e5-SNAPSHOT`; #423 / OS4 `miuix-glass` remains excluded while it is still an open experimental upstream PR.
2. Keep PR #154 Build 428 frozen and obtain the required device evidence for the exact native `onAppearanceChanged(boolean, boolean)` semantics without rebasing the diagnostic checkpoint.
3. After Build-428 evidence is closed, history-preserving refresh the next executable Control Center correction from current `dev`; retain the Build-429 dependency baseline and allocate the next build identity rather than restoring the older `2afdbb39` pin.
4. During the next convenient app-UI smoke pass, cover Home/Features/Settings navigation, Slider horizontal drag versus page swipe, predictive/system back, and blur/backdrop pages.
5. Keep `main` unchanged until the integrated line meets the existing promotion criteria.
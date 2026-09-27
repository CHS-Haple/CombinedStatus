# Current Development State

This file is the concise recovery point for active Combined Status development. Read it after `CONTRIBUTING.md`. Keep chronological Build/investigation history in `DEVLOG.md`, future direction in `ROADMAP.md`, version semantics in `VERSIONING.md`, and record-writing rules in `RECORDING.md`.

## Repository baseline

- Last refreshed: 2026-09-27
- Stable branch: `main`
- Stable runtime baseline: Build 351, commit `2477867278483b76b80ed0884de3a07c7ede668a`
- Integration branch: `dev`
- Integration runtime baseline: Build 408, commit `2f584c3b393dc5ee606284426aa95a9d6beae5d5`
- Phase-2A integration: PR #105 merged to `dev` as `2f584c3b393dc5ee606284426aa95a9d6beae5d5`; former stacked PR #100 is closed as superseded.
- Active Phase-2B work branch: `fix/shade-home-overlay-leak`; PR will be opened from this bounded fix.
- Active development display line: **0.0.2**
- First planned formal release: **1.0.0**
- Target profile: HyperOS SystemUI `17.03.260226.r`
- Modern Xposed API: 102
- Application ID: `com.chaners.combinedstatus`

`main` and `dev` runtime baselines remain unchanged by documentation-only commits. Work-branch checkpoints are development evidence until their required validation and maintainer acceptance are complete.

## Current phase

The project has completed the current **Phase 2A — 0.0.2 Home carrier / presentation architecture** gate for `dev` integration and is moving into **Phase 2B — Home -> shade / Control Center scene boundary/projection**.

The selected Home direction is an existing-host composition rather than the superseded permanent extra-participant / occupancy-handoff route:

`MiuiNotificationStatusContainer / system_icon_area -> host-scoped overlay -> resolved Home layout -> Combined Status renderer`

SystemUI remains authoritative for surrounding native layout, Battery scene/hide behavior, native tint semantics, and charging/Super-Island motion. Combined Status owns its compact composition plus only narrowly scoped, reversible Home presentation state.

Home -> shade / Control Center projection is now the active Phase 2B direction. Keyguard / lockscreen / AOD follows after Phase 2B.

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
- **Build 409 / `20260927-409`** is the current Phase-2B notification-shade Home-eligibility candidate. Final executable source: `4ab7b490617757e34c0ea8e0b59e3d7a16bae9ad`. It promotes exactly one native `ShadeExpansionStateManager.onPanelExpansionChanged(FZZ)V` callback into production scene eligibility, keeps Control Center callbacks diagnostics-only, uses `expanded=false && tracking=false` as the semantic closed boundary, transfers the last known nullable eligibility through 409+ Hot Reload, and fails Home-native when the required shade authority cannot be established. No fraction threshold, delay, polling, custom animation, peer geometry, tint or alpha write is added. Source review passed; CI/Canary and device acceptance are pending.
- Build 406 trusted validation: owner `/canary` Work Branch Canary #335 **succeeded**. Validated PR head `80f371784ffaee406dd6ea5728219eeee5913318` differs from the frozen runtime source only in `CURRENT.md` and `DEVLOG.md`, so executable content remains exactly Build 406. Artifact `10929711688`; artifact ZIP digest `sha256:c7af997217acd171c66beb860d7212c0d72fd672a38978f7b5c2eb5a524f11ba`; extracted APK SHA-256 `e086d8914fece7ba8ea86200756f4a6366d56dadaa5510846618a4077e6ddd80`.
- Documentation-only commits may advance the Phase-2B work branch beyond Build-409 runtime source `4ab7b490617757e34c0ea8e0b59e3d7a16bae9ad` without creating a new runtime Build; runtime identity remains `20260927-409` until executable source changes.

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

### Visual intensity / optical parity — accepted for dev with deferred minor polish

Build 407 resolved the blocking native-center opacity/resource-mask defect. Build 408 retained the native tint/resource fix and rebalanced the custom outer ring without changing semantic color authority.

Maintainer acceptance:
- current color behavior and center opacity are **accepted for `dev` integration**;
- the result is considered basically compliant with the intended visual design;
- a small residual optical-weight difference between ring, center and dots may still be visible;
- that residual is recorded as deferred polish, **not** as perfect parity and **not** as a Phase-2B blocker.

Do not reopen this with per-glyph RGB/alpha multipliers or screenshot-derived compensation during Phase 2B.

### Battery semantic colors — implemented and accepted for dev

Build 403 uses HyperOS `MiuiBatteryMeterIconView.getProgressStatus()` as semantic-state authority. Charging, power-save, performance, and low-battery colors come from SystemUI's already-loaded native color fields when HyperOS optimization allows them; normal state and unavailable semantic colors fall back to the resolved native status-icon tint.

No user-facing per-state color picker/source selector is exposed yet. The future policy seam remains: **System default / Follow status icon / Custom** for NORMAL / CHARGING / POWER_SAVE / PERFORMANCE / LOW.

### Home -> shade / Control Center scene boundary — Build 409 implemented, validation pending

The shallow notification-shade pull / held-return leak is traced to an incomplete scene-lifetime model: static Battery `mStatusBarState` can remain `SHADE(0)` while the notification panel has already taken transition ownership.

Build 409 now composes two native facts for Home eligibility:
- static surface must remain `UNLOCKED_STATUS_BAR`;
- notification shade must report its semantic CLOSED state, `expanded=false && tracking=false`.

Implementation boundary:
- the exact-target `ShadeExpansionStateManager.onPanelExpansionChanged(float, boolean, boolean)` callback is now a core runtime source in every build channel;
- Release installs only that required notification-shade hook; Debug/Canary keep the two existing Control Center callbacks for diagnostics only;
- any `expanded=true` or `tracking=true` update makes Home presentation not ready and reuses existing Home cleanup to restore native clip/reservation state;
- missing/failed shade authority fails Home-native instead of guessing a presentation state;
- 409+ Hot Reload transfers the last known shade eligibility; a legacy 408 payload has no such field, so it preserves the successfully installed 409 bootstrap until the next native panel callback;
- numeric `fraction` remains read-only for diagnostics/future projection. No fraction threshold, delay, translation follower, custom animator or Control Center projection is introduced.

Source review is complete. Fast CI has exposed only validation-maintenance issues so far:
- Draft Build #1122 failed `git diff --check` on trailing whitespace in newly added DEVLOG lines; the whitespace was corrected without runtime changes.
- Ready Fast Build #1123 reached Android compilation: production Kotlin compiled successfully, but unit-test compilation failed because five pre-existing `resolveOverlayVisible(...)` call sites were not updated for the new `notificationShadeAllowsHome` argument.
- Build #1124 reproduced the same pre-fix validation state on the whitespace-cleaned head.
- the next correction is test-only: pass `notificationShadeAllowsHome=true` to those five legacy assertions so their original feature/scene/handoff semantics remain unchanged.

Build identity remains 409 and executable source remains `4ab7b490617757e34c0ea8e0b59e3d7a16bae9ad`.

## Non-negotiable boundaries

- Home is the only Combined Status rendering surface currently treated as runtime-verified.
- Unsupported/unverified surfaces remain native until their own host/lifecycle/handoff contract is validated.
- Reuse authoritative HyperOS/SystemUI state and resources when a verified source exists.
- Native peer geometry, Battery translation/alpha/visibility, and island animation remain SystemUI-owned.
- No polling, per-frame follower, timing retry, duplicate layout-occupancy owner, or magic translation/margin compensation should be introduced for scene handoff.
- Fail toward native SystemUI when an ownership, compatibility, or restoration contract cannot be established safely.

## Repository / branch synchronization

- PR #105 is merged into `dev` as the accepted Phase-2A integration boundary.
- PR #100 is closed as superseded by that final integration PR.
- PR #134 installed the trusted work-branch Canary fallback into `main`; PR #136 history-preserving back-synced that accepted process state into `dev`.
- PR #135 then fixed Markdown backtick escaping in the Canary source summary without changing validation/trust semantics; PR #137 history-preserving back-synced that follow-up into `dev`.
- Validation-only PRs #131/#132 and the dev-based automation review PR #133 are superseded and closed without merge.
- The documentation-governance baseline is accepted in `main` and synchronized into `dev`; active runtime branches must preserve it when updated/merged rather than restoring older workflow/process text.
- PR #99 remains separate open historical work and is not an accepted baseline; any useful delta must be reconciled against the current line before reuse.

## Immediate next step

1. Apply the test-only Build-409 compatibility fix: update the five legacy `resolveOverlayVisible(...)` assertions to pass `notificationShadeAllowsHome=true`.
2. Do not change runtime or Build identity; executable source remains `4ab7b490617757e34c0ea8e0b59e3d7a16bae9ad`.
3. Re-run PR #138 Fast CI on the corrected test head, then run a signed Work Branch Canary.
4. If CI/Canary passes, freeze runtime.
5. Focused device test: shallow notification pull, hold at a tiny pull, return while still tracking, release back to Home, full notification-shade pull, and return.
6. Expected boundary: Combined Status disappears as soon as native shade expansion/tracking owns the scene, stays absent at fraction 0 while tracking/expanded is still true, and returns only after `expanded=false && tracking=false`.
7. If practical, Hot Reload once while shade is visibly open and verify the Home replacement does not flash back in.
8. Keep Control Center projection, shade Combined Status rendering, steady Home geometry, Build-408 color policy and deferred optical polish unchanged until this gate is accepted.

## Reference priority

1. latest `CONTRIBUTING.md`;
2. this `CURRENT.md`;
3. `ROADMAP.md`;
4. recent/relevant `DEVLOG.md` entries;
5. applicable `docs/architecture/` policy;
6. applicable `docs/reference/` evidence;
7. `VERSIONING.md` for version/release semantics and `RECORDING.md` for documentation maintenance.

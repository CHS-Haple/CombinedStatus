# Current Development State

This file is the concise recovery point for active Combined Status development. Read it after `CONTRIBUTING.md`. Keep chronological Build/investigation history in `DEVLOG.md`, future direction in `ROADMAP.md`, version semantics in `VERSIONING.md`, and record-writing rules in `RECORDING.md`.

## Repository baseline

- Last refreshed: 2026-09-27
- Stable branch: `main`
- Stable runtime baseline: Build 351, commit `2477867278483b76b80ed0884de3a07c7ede668a`
- Integration branch: `dev`
- Integration runtime baseline: Build 377, commit `f64fe0e3992eab4dd62ff479c3765d834ec7dfa4`
- Active architecture PR: #100, `feat/native-panel-transition -> dev`
- Active stacked feature PR: #105, `feat/battery-semantic-colors -> feat/native-panel-transition`
- Active development display line: **0.0.2**
- First planned formal release: **1.0.0**
- Target profile: HyperOS SystemUI `17.03.260226.r`
- Modern Xposed API: 102
- Application ID: `com.chaners.combinedstatus`

`main` and `dev` runtime baselines remain unchanged by documentation-only commits. Work-branch checkpoints are development evidence until their required validation and maintainer acceptance are complete.

## Current phase

The project remains in **Phase 2A — 0.0.2 Home carrier / presentation architecture**.

The selected Home direction is an existing-host composition rather than the superseded permanent extra-participant / occupancy-handoff route:

`MiuiNotificationStatusContainer / system_icon_area -> host-scoped overlay -> resolved Home layout -> Combined Status renderer`

SystemUI remains authoritative for surrounding native layout, Battery scene/hide behavior, native tint semantics, and charging/Super-Island motion. Combined Status owns its compact composition plus only narrowly scoped, reversible Home presentation state.

Home -> shade / Control Center projection is **Phase 2B**. Keyguard / lockscreen / AOD follows after Phase 2B.

## Current runtime checkpoints

- Builds 386-393 are historical evidence for the superseded permanent extra-participant route; they are not the current architecture premise.
- **Build 397** is the first device-accepted checkpoint for the tested Phase-2A charging-carrier scenarios.
- **Build 398** strengthens stable width authority by using the live native `battery_icon_container`; it is carried forward but was not separately device-promoted before the next checkpoints.
- **Build 399** separates active/inactive battery-ring arc compositing without reopening Home carrier ownership.
- **Build 403 / `20260927-403`** established the current HyperOS battery semantic-color implementation. Runtime source: `97ef67e648906a4b9bb2ce4d7dd390e955831189`.
- **Build 404 / `20260927-404`** is a completed but **device-rejected optical-parity A/B checkpoint**. Runtime source: `614c6ae96f1753088e21ce3568d969b900852081`. It removed percentile alpha remapping while retaining the existing bitmap-probe rendering path; target-device feedback shows the center presentation is visually worse than Build 403, so authored-alpha preservation alone is not an accepted fix.
- Documentation-only commits may advance PR #105 beyond the Build-404 runtime source without creating a new runtime Build; runtime identity remains the Build/source pair above until executable source changes.

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

### Visual intensity / optical parity — open

Build 404 device feedback is **negative**: preserving the alpha values of the existing 96px native-center bitmap did not improve parity and is reported to look worse than the previous checkpoint. Do not treat Build 404 as an accepted rendering baseline.

The root-cause boundary has moved upstream after exact-target SystemUI inspection.

Verified native Home Wi-Fi path on the pinned target:
- semantic state carries a base `Icon.Resource`;
- `MiuiWifiViewBinder` resolves the current native resource variant through `MiuiStatusBarIconViewHelper.transformResId(resId, useTint, light)`;
- the native ImageView receives the resulting resource with `setImageResource(...)`;
- tint mode either applies the current `ColorStateList` or clears image tint so the selected light/dark resource owns its color;
- the target resource is a 20dp × 20dp VectorDrawable;
- target-device diagnostics show the final native Wi-Fi ImageView/Drawable at 75 × 75 px, `FIT_CENTER`, identity matrix, zero padding, drawable alpha 255 and image alpha 255;
- the vector is therefore rasterized once at its final native drawable bounds.

Build 404 still uses a different rendering pipeline:
- load the base native Drawable;
- rasterize it first into a 96px ARGB bitmap;
- measure optical bounds from that bitmap;
- scale that bitmap again into the smaller Combined Status center destination using filtered bitmap sampling;
- apply the final tint through SRC_IN.

The earlier assumption that preserving the 96px bitmap's authored alpha values would sufficiently match native rendering is therefore **superseded**. The remaining non-native responsibility is the intermediate bitmap / second resampling stage itself: antialias/coverage is generated at the probe resolution and then redistributed when the bitmap is reduced to the final center size.

A controlled same-path comparison supports that mechanism. At a representative 63px final center size, direct vector rasterization kept 585 fully covered pixels and 275 partial-edge pixels; rendering at 96px then bilinear-downsampling to 63px kept only 474 fully covered pixels and produced 530 partial-edge pixels. Total alpha mass remained close, but edge coverage became materially softer.

Current boundary:
- native status-icon tint remains authoritative; do not tune gray values;
- Build 399's non-overlapping Battery arc partition remains valid;
- do not add per-glyph gray multipliers, percentile alpha remaps, source-asset edits or screenshot-derived compensation;
- the next bounded rendering A/B should remove the intermediate bitmap from the **final presentation path** and draw a module-owned clone of the native Drawable directly at the resolved final bounds;
- a bitmap probe may remain measurement-only if optical bounds are still needed;
- center size, center position, outer ring/mobile geometry, Battery semantic colors, Home carrier and scene behavior must remain unchanged for that A/B.

### Battery semantic colors — implemented, device acceptance still open

Build 403 uses HyperOS `MiuiBatteryMeterIconView.getProgressStatus()` as semantic-state authority. Charging, power-save, performance, and low-battery colors come from SystemUI's already-loaded native color fields when HyperOS optimization allows them; normal state and unavailable semantic colors fall back to the resolved native status-icon tint.

No user-facing per-state color picker/source selector is exposed yet. The future policy seam remains: **System default / Follow status icon / Custom** for NORMAL / CHARGING / POWER_SAVE / PERFORMANCE / LOW.

### Home -> shade scene boundary — next separate issue

A shallow notification-shade pull / final held-return frame can still leave the Home Combined Status overlay visible. This is **not intended final behavior** and is classified as the next Phase-2B scene-boundary/handoff problem, not as a reason to reopen the accepted steady Home carrier.

## Non-negotiable boundaries

- Home is the only Combined Status rendering surface currently treated as runtime-verified.
- Unsupported/unverified surfaces remain native until their own host/lifecycle/handoff contract is validated.
- Reuse authoritative HyperOS/SystemUI state and resources when a verified source exists.
- Native peer geometry, Battery translation/alpha/visibility, and island animation remain SystemUI-owned.
- No polling, per-frame follower, timing retry, duplicate layout-occupancy owner, or magic translation/margin compensation should be introduced for scene handoff.
- Fail toward native SystemUI when an ownership, compatibility, or restoration contract cannot be established safely.

## Repository / branch synchronization

- PR #100 remains the architecture work branch based on `dev`.
- PR #105 is intentionally stacked on PR #100.
- The documentation-governance baseline is already accepted into `main` and back-synced into `dev`; active runtime branches must preserve it when they are updated/merged rather than restoring their older README/CURRENT/ROADMAP variants.
- PR #99 remains separate open historical work and is not an accepted baseline; any useful delta must be reconciled against the current line before reuse.

## Immediate next step

1. Treat Build 404 as a failed optical-parity A/B; do not stack another tint/alpha compensation on it.
2. Implement one new rendering-boundary A/B: keep the current semantic resource, tint authority, optical sizing and geometry, but replace the cached 96px bitmap as the final native-center source with direct Drawable rendering at the resolved final bounds.
3. Keep any bitmap/alpha scan measurement-only; it must not feed final presentation pixels.
4. Review ownership, lifecycle, single writer, cleanup, fail-native behavior, performance, compatibility, exception recovery and future custom color/size support before committing the runtime change.
5. Run Fast CI + signed Work Branch Canary, then stop runtime changes for focused target-device A/B.
6. Only after optical/color closure move to the already-identified Phase-2B shallow-shade scene-boundary leak.

## Reference priority

1. latest `CONTRIBUTING.md`;
2. this `CURRENT.md`;
3. `ROADMAP.md`;
4. recent/relevant `DEVLOG.md` entries;
5. applicable `docs/architecture/` policy;
6. applicable `docs/reference/` evidence;
7. `VERSIONING.md` for version/release semantics and `RECORDING.md` for documentation maintenance.

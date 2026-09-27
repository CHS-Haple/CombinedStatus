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
- **Build 404 / `20260927-404`** is the current optical-parity A/B runtime candidate. Runtime source: `614c6ae96f1753088e21ce3568d969b900852081`. Its only intended visual delta from Build 403 is preserving the HyperOS center resource's authored alpha mask instead of percentile-rescaling that mask before tinting.
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
- device optical-parity and semantic-color acceptance: **pending**.
- validation-only PRs #129 and #130 are closed after evidence capture; neither is mergeable product work.

## Active runtime issues / validation

### Visual intensity / optical parity — open

Latest target-device screenshots still show a visible optical-weight difference between Combined Status and neighboring native status icons, so visual parity is **not accepted yet**.

Screenshot pixel sampling narrows the problem: the core dark grayscale levels are already closely aligned with adjacent native icons. The remaining mismatch therefore appears to be dominated by optical coverage / geometry / antialiasing rather than a different base monochrome tint. Treat that as a strong current finding, not yet a final root-cause proof.

Current boundary:
- Build 399's non-overlapping battery-arc compositing remains the active correction;
- native status-icon tint remains the intended monochrome authority and should not be replaced with a project gray;
- the earlier shared native alpha-mask normalization assumption is now **reopened**: current source review shows that Combined Status rescales the HyperOS-authored per-pixel alpha mask to an 85th-percentile ceiling before tinting, while the native ImageView/SRC_IN path uses the drawable's authored alpha as the tint mask;
- Build 356 already showed that the normalization path did not close the device visual-intensity mismatch, so it must not be treated as final parity evidence merely because it is shared;
- Build 404 implements one bounded A/B correction: preserve the native drawable's authored alpha mask while keeping resource identity, optical bounds, final-pixel alignment, resolved native tint, center size, outer geometry, and Battery arc policy unchanged;
- do **not** add per-glyph gray multipliers, replacement gray constants, screenshot-derived magic numbers, or source-asset recoloring merely to force a visual match;
- the green screenshot is a semantic-color state and is not evidence of a monochrome tint mismatch.

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

1. Runtime changes are stopped at the validated Build-404 Canary. Install that exact Canary and perform focused target-device A/B validation against Build 403 / prior screenshots.
2. Validate monochrome optical parity first: center Wi-Fi / hotspot / airplane / no-SIM resources should keep native visual weight and antialiasing without any geometry, spacing, or tint regression.
3. Validate Build-403 semantic battery-color behavior remains intact for NORMAL / CHARGING / POWER_SAVE / PERFORMANCE / LOW when those states are available.
4. Do not change center geometry, outer weight, tint, Battery arc policy, Home carrier, or shade behavior while this Build-404 device gate is open.
5. Update `CURRENT.md` and append `DEVLOG.md` immediately when device evidence changes acceptance.
6. After color/intensity closure, move to the already-identified Phase-2B shallow-shade scene-boundary leak without reopening steady Home carrier ownership.

## Reference priority

1. latest `CONTRIBUTING.md`;
2. this `CURRENT.md`;
3. `ROADMAP.md`;
4. recent/relevant `DEVLOG.md` entries;
5. applicable `docs/architecture/` policy;
6. applicable `docs/reference/` evidence;
7. `VERSIONING.md` for version/release semantics and `RECORDING.md` for documentation maintenance.

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
- **Build 405 / `20260927-405`** is a completed but **device-rejected direct-final-Drawable optical-parity A/B checkpoint**. Runtime source: `bf8091c8680dec7b85c58afded7f476ec95ca49d`. It removed the intermediate final-presentation bitmap/resample stage, but target-device screenshots still show the native center glyph materially lighter/lower-opacity than neighboring native status icons across light and dark surfaces. The direct-Drawable mechanism remains preferable to the superseded bitmap presentation path, but it is not sufficient for parity by itself.
- **Build 406 / `20260927-406`** is a completed but **device-rejected tint-variant A/B checkpoint**. Runtime source: `3d5e9d2339824c6d19e50dda170917559369135b`. Work Branch Canary #335 passed all CI/signing gates, but target-device evidence shows the center Wi-Fi glyph remains optically lighter/lower-coverage than the outer ring **even when `centerFollowsBatteryColor=false`**. Therefore the Build-406 hypothesis that the remaining defect was confined to the custom/battery-color tint branch is rejected.
- **Build 407 / `20260927-407`** is **device-accepted for the native-center opacity/resource-mask correction**, but overall optical balance remains open. Runtime source: `ffe746b24252f974db05e1fa4381ed5c56f0e73e`. The maintainer reports that the prior center-transparency defect is resolved. New same-device screenshots show the battery ring still reads darker/heavier than the native center and four mobile dots even when all three consume the same green semantic tint. Work Branch Canary #337 / run `36313837646` passed all gates and produced artifact `10929728522`.
- **Build 408 / `20260927-408`** is the current signed outer optical-weight device-test candidate. Runtime source: `8a7a39d8297fe926387d56cc8ff5be4b08405f4a`. It changes no tint or alpha: the battery ring default stroke returns from 8.25 to the historical 7.5 canonical units while the four-dot radius and unavailable-mark geometry remain exactly at their Build-407 physical defaults. The shared future outer-weight scale is rebased to 1.0 around that balanced baseline. Work Branch Canary #338 / run `36315043013` passed all gates and produced artifact `10930143406`; runtime is frozen pending device A/B.
- Build 406 trusted validation: owner `/canary` Work Branch Canary #335 **succeeded**. Validated PR head `80f371784ffaee406dd6ea5728219eeee5913318` differs from the frozen runtime source only in `CURRENT.md` and `DEVLOG.md`, so executable content remains exactly Build 406. Artifact `10929711688`; artifact ZIP digest `sha256:c7af997217acd171c66beb860d7212c0d72fd672a38978f7b5c2eb5a524f11ba`; extracted APK SHA-256 `e086d8914fece7ba8ea86200756f4a6366d56dadaa5510846618a4077e6ddd80`.
- Documentation-only commits may advance PR #105 beyond the frozen Build-408 runtime source without creating a new runtime Build; runtime identity remains `20260927-408` / `8a7a39d8297fe926387d56cc8ff5be4b08405f4a` until executable source changes.

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

Build 407 closes the previous native-center transparency/resource-mask defect: the maintainer reports that center opacity is now correct after routing every externally tinted native center asset through the verified HyperOS `_tint` mask.

A residual **optical-weight** mismatch remains. In the new charging-green screenshots, the continuous battery ring reads darker/heavier while the center Wi-Fi and four mobile dots read lighter.

Same-image sampling is useful only as diagnosis, not as a render constant:
- ring and center **core** green pixels converge to effectively the same RGB/tint on both light and dark screenshots;
- the ring has a much larger proportion of fully covered pixels because it is a wide continuous stroke;
- the native center mask has more antialiased edge coverage;
- the small mobile dots have the highest edge-to-core ratio at the final physical size.

This means the remaining difference is no longer a color-source or alpha-authority bug. The renderer is already feeding the same tint/semantic alpha to active ring, linked center and active mobile dots; the perceived difference comes from custom outer geometry / raster coverage.

Current boundary:
- do **not** introduce a darker center/mobile tint, lighter battery tint, per-glyph alpha multiplier, screenshot-fitted RGB, or resurrect Build-403 source-alpha normalization;
- keep the native center resource/geometry as the visual authority;
- preserve the current mobile-dot size because the dots and native center are now in the same perceived-weight band;
- the next bounded A/B may reduce only the custom continuous ring's base optical stroke while leaving tint, semantic alpha, dot size, center size and host geometry unchanged;
- use the known pre-Build-332 base ring stroke as the A/B anchor rather than inventing a screenshot-derived thickness;
- retain one shared future outer-weight scale by rebasing the default geometry, not by adding a device-specific runtime multiplier.

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
- PR #134 installed the trusted work-branch Canary fallback into `main`; PR #136 history-preserving back-synced that accepted process state into `dev`.
- PR #135 then fixed Markdown backtick escaping in the Canary source summary without changing validation/trust semantics; PR #137 history-preserving back-synced that follow-up into `dev`.
- Validation-only PRs #131/#132 and the dev-based automation review PR #133 are superseded and closed without merge.
- The documentation-governance baseline is accepted in `main` and synchronized into `dev`; active runtime branches must preserve it when updated/merged rather than restoring older workflow/process text.
- PR #99 remains separate open historical work and is not an accepted baseline; any useful delta must be reconciled against the current line before reuse.

## Immediate next step

1. **Runtime freeze:** Build 408 / `20260927-408`, runtime source `8a7a39d8297fe926387d56cc8ff5be4b08405f4a`.
2. Use Work Branch Canary #338 / run `36315043013`, artifact `10930143406`. Trusted source, tests/build, target profile, Xposed metadata, Haple signature and non-debuggable checks all passed.
3. Device A/B: compare Build 407 vs 408 in the same semantic-green state. The desired change is only that the ring no longer reads darker/heavier; center/dot opacity, color and geometry should remain unchanged.
4. If practical, also check one normal monochrome state.
5. If the ring becomes visibly too thin, reject Build 408 and retain Build 407 rather than adding alpha/RGB compensation.
6. If Build 408 balances the visual weight, close this optical checkpoint and move to the Phase-2B shallow-shade scene boundary next.

## Reference priority

1. latest `CONTRIBUTING.md`;
2. this `CURRENT.md`;
3. `ROADMAP.md`;
4. recent/relevant `DEVLOG.md` entries;
5. applicable `docs/architecture/` policy;
6. applicable `docs/reference/` evidence;
7. `VERSIONING.md` for version/release semantics and `RECORDING.md` for documentation maintenance.

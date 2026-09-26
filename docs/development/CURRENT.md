# Current Development State

This file is the concise recovery point for active Combined Status development. Read it after `CONTRIBUTING.md`. Detailed build/investigation history belongs in `DEVLOG.md`; future work belongs in `ROADMAP.md`; reusable evidence belongs in `docs/reference/`.

## Repository baseline

- Last refreshed: 2026-09-27
- Stable branch: `main`
- Stable runtime baseline: Build 351, commit `2477867278483b76b80ed0884de3a07c7ede668a`
- Integration branch: `dev`
- Integration runtime baseline: Build 377, commit `f64fe0e3992eab4dd62ff479c3765d834ec7dfa4`
- Active work branch: `feat/native-panel-transition`
- Active PR: #100, `feat/native-panel-transition -> dev`
- Active development line: **0.0.2**
- First planned formal release target: **1.0.0**
- Last device-accepted work-branch checkpoint: Build 397 (`0.0.2`) for the tested Phase-2A charging-carrier scenarios
- Current runtime checkpoint: Build 399 / `20260927-399` (`0.0.2`) — source commit `00e819f6d2c3ad518982016a8bf22d1524fece57`; CI and focused visual validation pending
- Target profile: HyperOS SystemUI `17.03.260226.r`
- Exact SystemUI SHA-256: `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`
- Modern Xposed API: 102
- Application ID: `com.chaners.combinedstatus`

Documentation-only commits do not create a runtime checkpoint.

## Current phase

The project remains in **Phase 2A — 0.0.2 Home carrier / presentation architecture**.

Phase 1 domain/rendering foundations remain reusable and are not being restarted:
- authoritative Wi-Fi/mobile/battery state and presentation semantics;
- single/dual-SIM, no-SIM, hotspot, airplane and mobile-type handling;
- native resource/tint integration and visual-intensity normalization;
- settings/master switch;
- Hot Reload and bounded diagnostics;
- fail-native restoration principles.

Home -> shade / Control Center projection is Phase 2B. Keyguard / lockscreen / AOD follows after Phase 2B. User-facing adaptive size/spacing controls remain later work.

## Current Phase-2A architecture

The permanent extra `combined_status` participant / occupancy-handoff route used in Builds 386-393 is superseded as the default architecture.

The current Home path is:

`MiuiNotificationStatusContainer (system_icon_area) -> HostSession overlay -> CombinedStatusHomeLayoutResolver -> Combined Status renderer`

with these ownership contracts:

- **Home carrier:** the Combined Status View is attached through the `MiuiNotificationStatusContainer` overlay; the same host is moved by native charging/Super-Island animation.
- **Stable width authority:** the active native `battery_icon_container` is the live battery-body carrier used for replacement-slot width. Charging-only Battery-root expansion is presentation occupancy, not Combined Status visual width.
- **Represented slots:** Wi-Fi/mobile/airplane/no-SIM slot exclusion is temporary and scoped to exact native status-icon measure/layout through `ignoredSlots`, with exact owned-entry restoration.
- **Visual masking:** represented native Views remain attached/state-capable and use reversible `clipBounds` masking; native alpha, visibility and translation remain SystemUI-owned.
- **End reservation:** Combined Status owns one reversible `MiuiStatusIconContainer.paddingEnd` reservation derived from stable replacement width, actual native Battery presentation width, and native hide state. Writer conflicts fail native.
- **Motion:** HyperOS owns island/peer motion. Combined Status inherits the `system_icon_area` transform and has no custom island timing/follower.
- **Resolved layout:** slot intent, visual geometry and future optical/scale inputs remain separate from native motion and scene state.
- **Cleanup:** host/session teardown restores only module-owned clip/ignored-slot/padding state and removes listeners/overlay resources.
- **Compatibility:** these contracts are fingerprint-gated to the pinned SystemUI target.

## Validation state

### Accepted evidence

Build 397 device feedback established for the pinned target:
- charger-connected SystemUI cold start with no interaction has normal neighbor spacing;
- the prior charging/Super-Island left-then-right twitch is gone;
- charging steady-state placement is normal.

Build 398 then refined width authority from the runtime `battery_meter_width` resource proxy to the stronger live `battery_icon_container` carrier. Fast CI and signed Canary passed; Build 398 was not separately promoted or independently device-accepted before the next checkpoint.

### Current Build 399 objective

The remaining visible issue under active test is **battery-ring visual intensity**, not Home carrier geometry.

Same-device screenshot review and source inspection show:
- center Wi-Fi and active mobile dots are already close to adjacent native status-icon intensity;
- the battery ring appears heavier/darker;
- the battery painter previously drew the dim full-ring track and then the full-strength active arc over the same pixels, creating overlapping steady-state coverage.

Build 399 changes only battery arc compositing:
- active and inactive arc segments are partitioned instead of overlapping;
- existing tint authority, geometry, stroke width, charging color and semantic alpha values remain unchanged;
- no screenshot-derived multiplier or per-glyph gray compensation is introduced;
- a pure `CombinedStatusBatteryArcPolicy` test contract covers the partition logic.

A brief native-Battery flash during same-architecture Hot Reload predates Build 397 and is tracked separately; it is not classified as a Build-399 regression.

## Non-negotiable boundaries

- HyperOS remains authoritative for native peer layout, scene state, Battery hide/presentation, tint source and live motion.
- One live property has one writer.
- No fixed 105/135 or 448/478 correction chain is architecture.
- No custom island animator, polling, timing retry, translation compensation, or permanent extra participant may be reintroduced to preserve historical work.
- Native layout mutation must remain narrowly scoped, reversible, conflict-detected and exact-target proven.
- Missing/inconsistent contracts fail native.
- Phase 2B transition behavior must not be folded into Phase 2A visual/carrier fixes.

## Immediate next step

1. Run Build 399 automated CI/Canary gates for the exact source checkpoint.
2. If CI passes, perform focused same-device visual validation of normal gray and charging-green battery-ring intensity against the neighboring native icons and the center/mobile layers.
3. Include a short regression sanity check that accepted Build-397 Home spacing and charging/Super-Island behavior remain intact.
4. Record the actual result immediately in `CURRENT.md` and append it to `DEVLOG.md`.
5. If Build 399 passes, perform a Phase-2A closure review before beginning Phase 2B; if it fails, reopen only the demonstrated visual-compositing cause.

PR #100 remains unmerged until the applicable Phase-2A acceptance boundary is met.

## Reference priority

Read in this order:
1. latest `CONTRIBUTING.md`;
2. this `CURRENT.md`;
3. `docs/development/ROADMAP.md`;
4. recent/relevant `docs/development/DEVLOG.md`;
5. `docs/architecture/README.md` and relevant architecture policy;
6. `docs/reference/README.md` and relevant reference evidence;
7. exact-target SystemUI Reference findings when the task depends on platform internals.

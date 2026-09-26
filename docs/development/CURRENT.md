# Current Development State

This file is the concise recovery point for active Combined Status development. Read it after `CONTRIBUTING.md` and before changing code. Keep detailed history in `DEVLOG.md` and future/deferred work in `ROADMAP.md`.

## Repository baseline

- Last refreshed: 2026-09-26
- Stable branch head: `main` at documentation commit `cb6430beb99a376cf8c13abe2305454d63c74122`
- Stable runtime baseline: Build 351, commit `2477867278483b76b80ed0884de3a07c7ede668a`
- Integration branch head: `dev` at documentation back-sync commit `c2f9ac02596f39467ed2a47891ca8043fedf41bd`
- Integration runtime baseline: Build 377, commit `f64fe0e3992eab4dd62ff479c3765d834ec7dfa4`
- Active work branch: `feat/native-panel-transition`
- Active runtime checkpoint: Build 384, commit `b838b8dfcdf90f575dba3485b0094dfa5c8aacdf`
- Active PR: #100, `feat/native-panel-transition -> dev`
- Target profile: HyperOS SystemUI `17.03.260226.r`
- Modern Xposed API: 102
- Application ID: `com.chaners.combinedstatus`

The documentation-only branch heads above do not change their associated runtime baselines.

## Current integration state

Build 377 remains the accepted `dev` runtime baseline. The active work branch contains Builds 378-384 and has not been merged to `dev`.

The current work is a runtime-sensitive geometry/transition investigation. CI success is available through Build 384, but no Build 378-384 state is accepted for integration until the focused device scenarios and ownership review are complete.

## Active objective

Break the current three-symptom repair cycle without adding another compensating geometry writer:

1. Combined Status enable can flash/reappear instead of entering with a clean native transition.
2. A fix for the entry path can reintroduce first-frame / last-frame right shift during non-steady panel transitions.
3. A fix for transition anchoring can reintroduce steady-state left shift.

The objective is to establish one coherent ownership model for:
- the native end-side layout slot;
- Combined Status drawing geometry;
- native transition/animation geometry;
- Home -> shade / Control Center handoff.

Do not continue choosing between a 0px and 105px status-icon shell as if shell width alone were the architecture.

## Confirmed conclusions

- **Confirmed:** the native battery motion view may transiently report a different width from the stable Home battery slot. Build 378 therefore separates slot geometry from battery motion-view geometry.
- **Confirmed:** under the Build 380 layout-hide path, Control Center observed an invalid handoff geometry in which expanded status-icon width and battery width were both counted. Preserving native battery-slot layout removed the reported non-steady first/last-frame right shift.
- **Confirmed by device feedback:** after preserving the native battery slot while keeping a full-width Combined Status participant, the non-steady shift disappeared but steady Combined Status moved left by one participant width.
- **Confirmed by device feedback:** keeping the native battery slot while returning the Combined Status layout shell to zero width removed the duplicate steady occupancy but brought back the enable flash / no-clean-entry symptom.
- **Confirmed by Build 382 diagnostics:** the Combined Status native APPEAR state is delivered; the zero-width root begins at native alpha/scale values, so the symptom is not simply "no animation callback".
- **Confirmed by Build 382 diagnostics:** the zero-width Combined Status root exposes a zero horizontal pivot while normal visible Wi-Fi/mobile peers use pivots derived from their real widths.
- **High confidence:** the recurring three-symptom cycle is structural. The current implementation asks one ordinary status-icon participant to serve incompatible responsibilities across slot occupancy, visual replacement of the battery area, and animation geometry.
- **Confirmed static/runtime topology fact:** HyperOS exposes distinct Home binder objects for the battery wrapper/container and the concrete `MiuiBatteryMeterView`. This creates a possible seam between slot/motion ownership and battery content.
- **Hypothesis:** the native battery wrapper/container can become the correct visual/motion ownership layer for Combined Status. Build 383 adds bounded read-only evidence collection for this question; it is not yet proven.
- **High confidence:** Build 384's pivot normalization is a bounded experiment, not a final architecture. Even if device-visible entry improves, the ownership split still requires review before acceptance.

## Relevant authoritative references

- Latest repository `CONTRIBUTING.md`, especially sections 3.1-3.4, 4.1-4.4, 5.1, 8, 10, and 11.
- `CHS-Haple/SystemUI-Reference/findings/statusbar.md` for Home host, battery/status-icon geometry, bindable participant ownership, and the separation of slot/drawing/transition geometry.
- `CHS-Haple/SystemUI-Reference/findings/control-center.md` for the distinct Control Center surface and `StatusBarAnchorBounds`.
- `CHS-Haple/SystemUI-Reference/findings/charging.md` for native battery lifecycle and `MiuiStatusBatteryContainer.setIsHideBattery(...)`.
- New device evidence supersedes any earlier preferred integration candidate when they conflict.

## Validation / blockers

Build 384:
- versionName: `0.0.1`
- buildId: `20260926-384`
- commit: `b838b8dfcdf90f575dba3485b0094dfa5c8aacdf`
- Fast Build #1020: success
- Work Branch Canary #287: success
- Signed non-debuggable Canary artifact: produced
- Device validation: pending

Required focused device evidence:
1. master-switch OFF -> ON entry behavior, specifically whether the flash/reappearance remains;
2. steady-state position after enable;
3. first-frame pull-down and last-frame return-to-steady alignment;
4. one pull-down -> full close diagnostic capture so Build 383's `homeMotion` snapshots can compare `mBatteryContainer`, `mBatteryView`, and `mStatusContainer` at panel boundaries.

No merge to `dev` is allowed while the three-symptom cycle or ownership model remains unresolved.

## Immediate next step

Device-test Build 384 once. Use the resulting behavior plus the bounded `homeMotion` / Control Center anchor diagnostics to decide between:

- retiring the ordinary bindable participant as the visual owner and migrating rendering to a verified native battery-slot/motion layer; or
- retaining the participant only if the evidence proves a single-writer transition contract that does not reintroduce duplicate occupancy or handoff geometry.

Do not add another width, translation, margin, delay, or per-frame compensation before this ownership decision.

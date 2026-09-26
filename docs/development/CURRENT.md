# Current Development State

This file is the concise recovery point for active Combined Status development. Read it after `CONTRIBUTING.md` and before changing code. Keep detailed history in `DEVLOG.md` and future/deferred work in `ROADMAP.md`.

## Repository baseline

- Last refreshed: 2026-09-26
- Stable branch: `main`
- Stable runtime baseline: Build 351, commit `2477867278483b76b80ed0884de3a07c7ede668a`
- Integration branch: `dev`
- Integration runtime baseline: Build 377, commit `f64fe0e3992eab4dd62ff479c3765d834ec7dfa4`
- Active work branch: `feat/native-panel-transition`
- Active runtime checkpoint: Build 385 (current work-branch source checkpoint)
- Active PR: #100, `feat/native-panel-transition -> dev`
- Target profile: HyperOS SystemUI `17.03.260226.r`
- Modern Xposed API: 102
- Application ID: `com.chaners.combinedstatus`

Documentation-only branch heads do not change their associated runtime baselines.

## Current integration state

Build 377 remains the accepted `dev` runtime baseline. Builds 378-385 belong to the active work branch and are not accepted for integration until the active three-symptom geometry/transition boundary is device-validated.

PR #99 (`fix/native-visual-intensity-normalization`) remains open and unmerged; its head is not an accepted integration baseline. Shared native visual-intensity normalization is already present in the merged Build 377 line through PR #98, so any future use of PR #99 must be reconciled against current `dev`.

## Active objective

Close the three-symptom repair cycle with one coherent separation of:
- native end-side layout occupancy;
- Combined Status drawing geometry;
- native APPEAR/DISAPPEAR transition geometry;
- Home -> shade / Control Center handoff geometry.

Build 385 preserves the native battery 105px slot as the single layout occupancy owner and keeps the Combined Status status-icon shell at zero width. It changes only how the module-owned custom root receives its APPEAR pivot at the exact native callback boundary.

## Confirmed conclusions

- **Confirmed:** stable native battery-slot geometry and `MiuiBatteryMeterView` motion geometry are distinct.
- **Confirmed:** Build 380 observed invalid Control Center anchor semantics when status-icons expanded into the battery area while battery width was still counted separately.
- **Confirmed by device feedback:** preserving native battery layout removed the non-steady first/last-frame right shift.
- **Confirmed by device feedback:** preserving native battery layout plus a full-width Combined Status participant caused steady left shift by one participant width.
- **Confirmed by device feedback:** preserving native battery layout plus a zero-width participant restored steady placement but brought back the enable flash/no-clean-entry symptom.
- **Confirmed by Build 384 runtime evidence:** native APPEAR is delivered; the one-shot pre-draw pivot bridge is overwritten when APPEAR actually starts.
- **Confirmed by exact SystemUI DEX + Build 384 runtime:** `MiuiStatusBarIconAnimatorController$FolmeHandler$appearAnimation$appear$1.onStart()` computes `pivotY` from View height and `pivotX` from View width. The intentional zero-width Combined Status shell therefore receives native `pivotX=0`.
- **Confirmed:** the three-symptom loop is structural if shell width alone is used for both layout occupancy and transition pivot.
- **Confirmed correction:** `HomeStatusBarViewBinderInjector.mBatteryContainer` is the internal battery-icon `FrameLayout` from `battery_digital_view.xml`, not an outer battery-slot wrapper. Runtime battery-specific alpha changes make it unsuitable as the Combined Status renderer host.
- **Selected Build 385 boundary:** replace the exact native APPEAR pivot callback only for the current Combined Status root, using renderer visual width and root/visual height. All native peer callbacks proceed unchanged.

## Ownership / compatibility boundary

- Native battery slot: HyperOS is the only layout occupancy owner.
- Combined Status renderer: owns its drawing geometry.
- HyperOS: continues to own visible state, remove lifecycle, alpha/scale Folme curve, panel/island transitions, and all peer geometry.
- Combined Status: owns only the custom root's APPEAR pivot geometry because its visual width is intentionally decoupled from its zero layout width.
- The exact APPEAR callback class, zero-argument `onStart()`, and captured `$view` field are required compatibility contracts. Missing contract fails the native Combined Status participant closed.
- No polling, repeated pre-draw correction, per-frame writer, translation offset, margin compensation, or peer geometry write is permitted.

## Relevant authoritative references

- Latest repository `CONTRIBUTING.md`, especially sections 3.1-3.4, 4.1-4.4, 5.1, 8, 10, and 11.
- Exact SystemUI artifact SHA-256 `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`.
- `SystemUI-Reference/findings/statusbar.md`, `findings/control-center.md`, and `findings/charging.md`.
- Build 384 detailed device diagnostic.

## Validation / blockers

Build 385:
- versionName: `0.0.1`
- buildId: `20260926-385`
- Fast Build: pending
- Work Branch Canary: pending
- Device validation: pending

Build 384 remains the immediate evidence baseline:
- Fast Build #1020: success
- Work Branch Canary #287: success
- documentation-sync Fast Build #1023: success
- documentation-sync Work Branch Canary #288: success
- diagnostic evidence: received and analyzed

Required Build 385 focused device scenarios:
1. OFF -> ON: centered APPEAR with no flash/reappearance.
2. ON -> OFF: centered DISAPPEAR.
3. Steady Home placement remains aligned to the native battery slot.
4. Pull down once and fully close: no first-frame / last-frame horizontal shift.
5. Detailed diagnostic: `appearPivotAdapter state=applied`; APPEAR pivot remains visual-centered after animation start; Control Center anchor remains `statusIconsWidth=478` + `batteryWidth=105`.

No merge to `dev` until these pass.

## Immediate next step

Run Build 385 Fast CI and signed Work Branch Canary. If they pass, perform the focused device validation. If any corner of the three-symptom cycle returns, stop and reopen ownership rather than adding another timing or offset patch.

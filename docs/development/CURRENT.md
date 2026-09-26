# Current Development State

This file is the concise recovery point for active Combined Status development. Read it after `CONTRIBUTING.md` and before changing code. Keep detailed history in `DEVLOG.md` and future/deferred work in `ROADMAP.md`.

## Repository baseline

- Last refreshed: 2026-09-26
- Stable branch: `main`
- Stable runtime baseline: Build 351, commit `2477867278483b76b80ed0884de3a07c7ede668a`
- Integration branch: `dev`
- Integration runtime baseline: Build 377, commit `f64fe0e3992eab4dd62ff479c3765d834ec7dfa4`
- Active work branch: `feat/native-panel-transition`
- Active runtime checkpoint: Build 385, runtime commit `6826944a79fbe1cb649ffe23807b32c30d565697`
- Build 385 trusted validation head: `d3533828e82a335eab0b3e661cfadd4e70ebee27` (history-synced tree; runtime-equivalent to Build 385)
- Active PR: #100, `feat/native-panel-transition -> dev`
- Target profile: HyperOS SystemUI `17.03.260226.r`
- Modern Xposed API: 102
- Application ID: `com.chaners.combinedstatus`

Documentation-only branch heads do not change their associated runtime baselines.

## Current integration state

Build 377 remains the accepted `dev` runtime baseline. Builds 378-385 belong to the active work branch and are not accepted for integration until the active three-symptom geometry/transition boundary is device-validated.

PR #99 (`fix/native-visual-intensity-normalization`) remains open and unmerged; its head is not an accepted integration baseline. Shared native visual-intensity normalization is already present in the merged Build 377 line through PR #98, so any future use of PR #99 must be reconciled against current `dev`.


## Macro roadmap position

The project is currently in the **Home -> shade / Control Center native transition stage**. Build 385 is an implementation checkpoint inside this stage, not a new product phase.

The macro sequence is:

1. **Core Home / native participant foundation — completed.**
   - Home Combined Status rendering and native participant integration;
   - authoritative network state/presentation, including single-SIM and dual-SIM paths;
   - Wi-Fi / hotspot / no-SIM / airplane / mobile-type presentation;
   - native resource/tint integration, network/battery suppression, fail-native restoration;
   - master switch and Hot Reload;
   - charging/island compatibility obtained through the native participant / slot / SystemUI ownership path rather than a separate project-owned island animation system.
2. **Home -> shade / Control Center native transition — active.**
   - Current three-symptom geometry/transition work belongs here.
3. **Keyguard / lockscreen / AOD scene completion — next macro phase.**
   - Reuse the stabilized state, ownership, and transition contracts instead of growing a second scene-specific patch stack.
4. **App Home + Preview Sandbox implementation — planned, design already confirmed.**
   - The page structure is not an open design question; see `ROADMAP.md` for the retained design snapshot.
5. **Adaptive sizing / spacing and broader visual controls — planned after the geometry contract is stable.**
6. **Full-scene compatibility regression and 0.0.1 release closure — final pre-release phase.**

Do not reclassify already completed dual-SIM/network support or native island participation as future macro phases.

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
- **Build 385 device result:** rejected as a complete fix. The APPEAR pivot adapter keeps `pivotX=52.5` through the sampled native APPEAR frames, but the user still observes the same "flash / missing entry animation". Pivot was a real geometry defect but not the cause of the missing visible entry animation.
- **Confirmed by 381 -> 382 code/device comparison:** the previously working visible entry animation was lost when `promoteActiveShellGeometry()` was removed and the active `ModernStatusBarView` shell changed from real visual width to zero width. This is the decisive behavioral boundary; later pivot-only fixes do not restore the visible animation.
- **Current root-cause direction:** native alpha/scale are applied to the zero-width participant root while the 105px renderer is deliberately drawn outside that root's layout bounds. The remaining investigation is whether the native transition RenderNode/bounds can visibly animate this overflow content at all, and how to give APPEAR real transition bounds without reintroducing steady layout occupancy.

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
- Fast Build #1031: success on `d3533828e82a335eab0b3e661cfadd4e70ebee27`
- Work Branch Canary #290: success
- Canary checkout: exact tested work-branch SHA `d3533828e82a335eab0b3e661cfadd4e70ebee27`
- Haple signature verification: success
- Canary debuggable check: false / success
- Artifact ID: `10907652284`
- Artifact archive digest: `sha256:6074e71cf5640ac5fd8d4e3d21d76a5f0603d733cba8479856ee0c756e3185fc`
- Extracted APK SHA-256: `c1c084b2a6b79924bcc2c2e801d3f2c1050f597bff107cbddacbcbea619e3259`
- Extracted APK size: `3375134` bytes
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

Perform the focused Build 385 device validation using the signed Canary artifact. If any corner of the three-symptom cycle returns, stop and reopen ownership rather than adding another timing or offset patch.

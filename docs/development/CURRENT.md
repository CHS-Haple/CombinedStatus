# Current Development State

This file is the concise recovery point for active Combined Status development. Read it after `CONTRIBUTING.md` and before changing code. Keep detailed history in `DEVLOG.md` and future/deferred work in `ROADMAP.md`.

## Repository baseline

- Last refreshed: 2026-09-26
- Stable branch: `main`
- Stable runtime baseline: Build 351, commit `2477867278483b76b80ed0884de3a07c7ede668a`
- Integration branch: `dev`
- Integration runtime baseline: Build 377, commit `f64fe0e3992eab4dd62ff479c3765d834ec7dfa4`
- Active work branch: `feat/native-panel-transition`
- Active runtime checkpoint: Build 387 (current work-branch source checkpoint)
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
- **Confirmed source boundary for Build 386:** `MiuiStatusIconContainer.onMeasure()` uses child measured width for occupancy, and `onLayout()` first lays children from measured dimensions before calculating `NewStatusIconState`. Build 386 therefore keeps Combined Status `layoutParams.width=0` and `measuredWidth=0`, lets native layout/state compute zero extra occupancy, then expands only the module-owned root's actual post-layout bounds to the renderer width before draw/animation.
- **Build 386 device result:** accepted for the original three-symptom loop. Steady placement is correct, the Combined Status entry animation is visibly restored, and the previously reported non-steady first/last-frame shift is not observed. A new charging-island boundary remains: native battery eviction can push the custom zero-width participant target beyond the right edge.\n- **Confirmed Build 387 root cause:** charging Super Island replaces native battery presentation, so HyperOS translates/fades `MiuiBatteryMeterView` out while Combined Status must remain visible because it still carries network state. The zero-width custom participant's native `layoutTranslationX` was derived from the expanded status-icon extent instead of the battery slot layout coordinate.\n- **Selected Build 387 correction:** adapt only the Combined Status `NewStatusIconState` target to `battery.left - statusIcons.left - root.left`. HyperOS remains the sole live View `translationX`/Folme writer.

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
- Fast Build #1031: success
- Work Branch Canary #290: success
- Device validation: **rejected for visible entry animation**; the pivot remained centered while the Combined Status entry animation still failed to present visibly.

Build 386:
- versionName: `0.0.1`
- buildId: `20260926-386`
- runtime commit: `805b23ab0ef399b19cebd8b978bc4b76cb207d24`
- Fast Build #1032: **success**
- Work Branch Canary #291: **success**
- Modern Xposed metadata: success
- Haple signature verification: success
- Canary non-debuggable verification: success
- Artifact ID: `10907802307`
- Artifact archive digest: `sha256:9cbf595eaf1ead0034b465bef2594215aba218a1cd859acf3b249174edb3c5c1`
- Extracted APK SHA-256: `edfc56dd07f9aebe014563aa6c939a7ae18ef737022e8fed3f7a6cc28ff6ed48`
- Device validation: **passed the original three-symptom boundary; charging-island right-edge eviction remains.**

Build 387:
- versionName: `0.0.1`
- buildId: `20260926-387`
- Fast Build: pending
- Work Branch Canary: pending
- Device validation: pending

Required Build 387 focused device scenarios:
1. Charging Super Island enter/steady/exit keeps Combined Status fully inside the end-side boundary.
2. Charging island motion remains native-smooth; no project-owned translation jump.
3. Non-charging steady placement remains unchanged.
4. OFF -> ON entry animation remains visible.
5. Pull-down first frame / return last frame remain aligned.
6. Diagnostic emits `nativeCombinedParticipant slotTranslation` only when native custom target differs from the battery-slot layout target and reports `moduleViewTranslationWrites=0`.

No merge to `dev` until these pass.

## Immediate next step

Run Build 387 Fast CI and signed Work Branch Canary. If they pass, device-test charging Super Island enter/steady/exit plus one non-charging regression pass. Build 386 remains the checkpoint that broke the original three-symptom loop; Build 387 must change only the custom participant's native translation-target semantics and must not regress Build 386.

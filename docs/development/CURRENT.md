# Current Development State

This file is the concise recovery point for active Combined Status development. Read it after `CONTRIBUTING.md` and before changing code. Keep detailed history in `DEVLOG.md` and future/deferred work in `ROADMAP.md`.

## Repository baseline

- Last refreshed: 2026-09-26
- Stable branch: `main`
- Stable runtime baseline: Build 351, commit `2477867278483b76b80ed0884de3a07c7ede668a`
- Integration branch: `dev`
- Integration runtime baseline: Build 377, commit `f64fe0e3992eab4dd62ff479c3765d834ec7dfa4`
- Active work branch: `feat/native-panel-transition`
- Active runtime checkpoint: Build 388 (current work-branch source checkpoint)
- Build 385 trusted validation head: `d3533828e82a335eab0b3e661cfadd4e70ebee27` (history-synced tree; runtime-equivalent to Build 385)
- Active PR: #100, `feat/native-panel-transition -> dev`
- Target profile: HyperOS SystemUI `17.03.260226.r`
- Modern Xposed API: 102
- Application ID: `com.chaners.combinedstatus`

Documentation-only branch heads do not change their associated runtime baselines.

## Current integration state

Build 377 remains the accepted `dev` runtime baseline. Builds 378-388 belong to the active work branch. Build 387 is rejected for charging-island peer overlap; Build 388 is the current unvalidated runtime checkpoint. PR #100 remains blocked from `dev` until Build 388 clears CI and focused device validation.

PR #99 (`fix/native-visual-intensity-normalization`) remains open and unmerged; its head is not an accepted integration baseline. Shared native visual-intensity normalization is already present in the merged Build 377 line through PR #98, so any future use of PR #99 must be reconciled against current `dev`.


## Macro roadmap position

The project is currently in the **Home -> shade / Control Center native transition stage**. Build 388 is the active checkpoint inside this stage; the charging-island overlap boundary must be closed before integration into `dev`.

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

Build 386 established the real-bounds / zero-steady-occupancy architecture. Build 387 kept the custom visual inside the battery-slot coordinate but device video proved that this is insufficient while HyperOS hides the battery: native peer status icons expand into the released battery region and overlap the zero-width Combined Status visual. Build 388 keeps zero occupancy while the native battery is present and claims exactly the resolved visual/native-slot width only while the authoritative `setIsHideBattery(true)` state is active.

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
- **Build 386 device result:** accepted for the original three-symptom loop. Steady placement is correct, the Combined Status entry animation is visibly restored, and the previously reported non-steady first/last-frame shift is not observed. A new charging-island boundary remains: native battery eviction can push the custom zero-width participant target beyond the right edge.
- **Confirmed Build 387 root cause:** charging Super Island replaces native battery presentation, so HyperOS translates/fades `MiuiBatteryMeterView` out while Combined Status must remain visible because it still carries network state. The zero-width custom participant's native `layoutTranslationX` was derived from the expanded status-icon extent instead of the battery slot layout coordinate.
- **Build 387 device result:** rejected for charging-island coexistence. Right-edge containment is improved, but the supplied video shows native peer icons moving into and overlapping the Combined Status visual during charging Super Island.
- **Confirmed Build 388 root cause:** native battery hide releases the battery region into `MiuiStatusIconContainer`; the log changes from the normal 478px status-icon region to 583px while the 105px Combined Status visual still has 0px measured occupancy. Native peers can therefore legally occupy the same region.
- **Selected Build 388 correction:** keep the participant at 0px occupancy while the battery slot exists; when the existing authoritative `MiuiStatusBatteryContainer.setIsHideBattery(true)` source reports that HyperOS released the battery slot, change only the module-owned participant width to the resolved visual/native-slot width. Restore it to 0px when the native battery returns. Build 387's state-target adapter remains; peer geometry and live Folme translations remain SystemUI-owned.

## Ownership / compatibility boundary

- Native battery slot while present: HyperOS is the only owner of that native battery occupancy.
- When HyperOS authoritatively releases the battery slot through `MiuiStatusBatteryContainer.setIsHideBattery(true)`, Combined Status may own only its own participant occupancy width, equal to the currently resolved visual/native-slot width; it returns to 0px when the native battery slot returns.
- Combined Status renderer: owns its drawing geometry.
- HyperOS: continues to own visible state, remove lifecycle, alpha/scale Folme curve, panel/island transitions, and all peer geometry.
- Combined Status additionally owns only its custom root's post-layout visual bounds and the custom `NewStatusIconState` translation target adaptation needed to map its participant onto the native battery-slot coordinate.
- Required compatibility contracts are the exact `MiuiStatusIconContainer.onLayout(boolean,int,int,int,int)` boundary plus `MiuiStatusBarFolmeViewState.applyToView(View, boolean)` / `NewStatusIconState.layoutTranslationX`. Missing contracts fail the native Combined Status participant closed.
- No polling, repeated pre-draw correction, per-frame writer, live View translation write, hard-coded slot offset, margin compensation, or peer geometry write is permitted.

## Relevant authoritative references

- Latest repository `CONTRIBUTING.md`, especially sections 3.1-3.4, 4.1-4.4, 5.1, 8, 10, and 11.
- Exact SystemUI artifact SHA-256 `a0e738e41fe599b97950cbf52a9e2ddc6ae2ceff986efbacb1c9840bea78768d`.
- `SystemUI-Reference/findings/statusbar.md`, `findings/control-center.md`, and `findings/charging.md`.
- Build 384 detailed device diagnostic.
- Build 387 detailed diagnostic plus the focused device screen recording covering charging Super Island and panel-transition behavior.

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
- runtime commit: `9cce4d2ea1e8ddf2512b1db5df4ac55dd9ff235c`
- Fast Build #1033: **success**
- Work Branch Canary #292: **success**
- Canary exact tested work-branch SHA checkout: success
- Modern Xposed metadata: success
- Haple signature verification: success
- Canary non-debuggable verification: success
- Artifact ID: `10908269727`
- Artifact archive digest: `sha256:bf91bb40923264f2a76aa6b9be8000373af5f71f0d4331a19695f6d46be02a40`
- Extracted APK SHA-256: `2788a27aa64dc6c1495f71aaaafc1037b39310a95fab55db89341c3697209dec`
- Extracted APK size: `3375134` bytes
- Device validation: **rejected for charging-island overlap**; right-edge containment improved but peer icons overlap the Combined Status visual.

Build 388:
- versionName: `0.0.1`
- buildId: `20260926-388`
- runtime commit: `bb840c96a9d7ea2376dfb6b02048e91a9976e1fa`
- trusted tested work-branch SHA: `d57f35664e435722025809d3acba456f44ee3883` (runtime-equivalent; later delta is development documentation)
- Fast Build #1038: **success**
- Work Branch Canary #297: **success**
- pinned target-profile verification: success
- Modern Xposed metadata verification: success
- Haple signature verification: success
- Canary non-debuggable verification: success
- Artifact ID: `10910080114`
- Artifact archive digest: `sha256:a18273b7f35646174db9181079b40ad0bd4027bd68238b38c2942006b894baaa`
- Extracted APK SHA-256: `d737521d285fdc35433c1e5b852db2063ef637362c45e0ff2ebfc9a0fece57f8`
- Extracted APK size: `3375134` bytes
- Device validation: pending

Required Build 388 focused device scenarios:
1. Charging Super Island enter/steady/exit keeps Combined Status fully inside the end-side boundary.
2. Charging island motion remains native-smooth; no project-owned translation jump.
3. Non-charging steady placement remains unchanged.
4. OFF -> ON entry animation remains visible.
5. Pull-down first frame / return last frame remain aligned.
6. Diagnostic emits `nativeCombinedParticipant slotTranslation` only when native custom target differs from the battery-slot layout target and reports `moduleViewTranslationWrites=0`.

No merge to `dev` until these pass.

## Immediate next step

Device-test the signed Build 388 Canary. First verify charging Super Island enter/steady/exit for both right-edge containment and peer-icon separation, then repeat the Build 386 regression checks for non-charging steady placement, OFF -> ON native entry animation, and shade / Control Center first/last-frame alignment. Do not merge PR #100 until this focused gate passes.

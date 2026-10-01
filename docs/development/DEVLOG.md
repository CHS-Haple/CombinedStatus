## 2026-10-01 — Build 564 MIUIX battery-color scheme library

**Type:** App UI / settings schema / Runtime projection  
**Display version:** 0.0.3  
**Build:** 564 / `20261001-564`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Goal

Replace the temporary Build-561/562 battery-color selector with the agreed hierarchy: one BottomSheet, whole-scheme horizontal paging, aligned six-mode previews, up to five named custom schemes, and one in-sheet mode editor whose first edit automatically becomes Custom.

### Exact-target color basis

Directed review of `CHS-Haple/SystemUI-Reference` for SystemUI `17.03.260226.r` confirms the status-bar semantic resources:
- charging `#1DCD3A`;
- power save `#FF9F05`;
- performance `#3482FF`;
- low battery `#FA382E`;
- no distinct target-proven super-power-save progress color; Guiyuan's HyperOS template reuses power-save for that slot;
- Normal remains the status-icon tint/inversion path and is represented by the checker/mosaic semantic rather than a fake fixed HEX.

The new HyperOS built-in is intentionally a fixed verified template. `Follow inversion` remains a separate per-mode source.

### Data / migration

- Add an App-side `BatteryColorSchemeLibraryRepository` in the existing visual preferences file.
- Built-in order/default: HyperOS -> iOS -> Low saturation.
- Custom scheme cap: five, with stable IDs and smallest-free-ID naming support.
- Each custom mode stores a source reference: HyperOS / iOS / Low saturation / Follow inversion / Custom.
- Template references remain references in App metadata; only Custom stores an authored fixed color.
- Activating a built-in/custom scheme projects to the pre-existing Runtime `batteryColorPreset / mode / override` keys. Scheme names/order/library metadata are not Runtime keys and therefore never cross into SystemUI.
- Legacy migration keeps the currently effective preset/mode result and retains dormant stored custom colors even when the old slot was currently set back to Preset.
- Deleting the active custom scheme returns to its recorded base built-in.

### MIUIX UI

- Keep one `OverlayBottomSheet`; detail editing is an internal spring page transition rather than stacked sheets.
- Scheme card + six mode rows form one `HorizontalPager` page and move together.
- Pager uses the upstream `PagerNavigationSpringSpec`, `pagerGestureOverride`, and `PagerGestureNestedScrollConnection`.
- A fixed adaptive MIUIX `Card` capsule below the pager contains dot indicators; the active page stretches to a short pill.
- Built-in rows are read-only and reserve the same action-column width as custom rows.
- Custom rows expose the mode editor.
- Color source is collapsed by default; selecting a source updates the editor, and changing common colors / HSV / HEX / RGB performs copy-on-write to Custom.
- Create and rename use `OverlayDialog`; Add/More use MIUIX icons from the already-present icons dependency.
- Delete uses `MiuixTheme.colorScheme.error` in both the management row and confirmation action.

### 审查 / review — pre-commit

- **single writer:** no new SystemUI painter, geometry writer, state observer, hook, or animator; Runtime still consumes the existing flattened color keys.
- **process boundary:** custom scheme metadata is App-only; `isCombinedStatusVisualPreferenceKey` is intentionally unchanged for library keys.
- **native-first:** Pager spring/gesture, Card, Dialog, Button, Radio preference, HSV sliders, TextField and icons use MIUIX APIs. Only the checker swatch and compact page dots are project-drawn display primitives because MIUIX 0.9.4 has no PagerIndicator component.
- **migration:** old active color behavior is representable; dormant custom values are retained instead of silently discarded.
- **destructive action:** Delete is error-colored and confirmation-gated.
- **copy-on-write:** a template/follow source remains referenced until the first actual edit; the first edit is applied without a value jump and changes the slot source to Custom.
- **Fail-native / Runtime:** the fixed HyperOS template is tied to the verified target and documented as target evidence, not a universal Xiaomi constant.

### Validation

Run exact-head CI first. If green, a signed Canary is warranted for visual/interaction review of the new BottomSheet hierarchy; Runtime device testing is only required if observed colors differ from the projected fixed/template result.




## 2026-10-01 — Build 549 faster continuous retract AB

**Type:** focused device-evidence timing refinement  
**Display version:** 0.0.3  
**Build:** 549 / `20261001-549`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Device evidence

Build-548 video confirms the continuous retract topology remains correct and the Build-545 discontinuity does not return. A small outlet-side residual arc is still visible for several frames after CENTER/network has already committed to the exit, so the remaining defect is only late completion.

### Root cause

Build 548 finishes the ring at 45% of the existing HyperOS transition clock. The symmetric smoothstep keeps the last visible arc continuous, but the completion point is still later than the desired choreography.

### Change

Single-variable AB:
- `TRANSITION_COMPLETE_PROGRESS: 0.45f -> 0.35f`;
- local ring progress reaches 0.5 at global progress 0.175 and 1.0 at 0.35;
- existing symmetric smoothstep remains unchanged;
- LEFT/RIGHT/NONE ordered-arc semantics remain unchanged;
- CENTER/network native target geometry and timing remain unchanged;
- Battery target/handoff, reverse symmetry, and Build-542 island behavior remain unchanged.

### 审查 / review

- timing scalar only; no new geometry gate, Animator, delay, or second timeline;
- Build-546/547/548 continuity preserved; Build-545 live gate remains rejected;
- ownership, lifecycle, single-writer, Fail-native, and performance behavior unchanged;
- steady rendering, battery-top controls, typography handoff, native peer motion, and island projection untouched;
- remap test now locks 0.175 -> 0.5 and 0.35 -> 1.0; existing arc/direction tests remain authoritative.

### Validation

Run exact-head Runtime CI, then one signed exact-head Canary. Primary device check: residual outlet-side arc should clear earlier than Build 548 while remaining visually continuous on normal/fast pulls and symmetric on reverse collapse.


## 2026-10-01 — Build 550 front-loaded continuous retract AB

**Type:** focused device-evidence curve refinement  
**Display version:** 0.0.3  
**Build:** 550 / `20261001-550`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Device evidence

Build 549 moves full Battery-ring completion to 35% of the existing HyperOS transition clock. Maintainer feedback says the first half of the visible retract still feels too similar to the prior version. The requested change is therefore not another earlier terminal cutoff; it is a faster first half while preserving the accepted continuous topology.

### Root cause

The Build-549 local curve is still symmetric smoothstep. Its zero start slope intentionally eases in, so even with an earlier 35% terminal point the first part of the retract remains visually conservative.

### Change

Keep `TRANSITION_COMPLETE_PROGRESS = 0.35` and front-load only the shape-progress curve:
- warp normalized local progress with `p + 0.45 * p * (1 - p)`;
- feed the warped value into the existing smoothstep;
- at local p=0.25, consumed sweep increases from 15.625% to about 26.065%;
- at local p=0.50, consumed sweep increases from 50.0% to about 66.590%;
- endpoints remain exact (0 -> 0, 1 -> 1), with smoothstep retaining zero endpoint slope;
- no piecewise threshold, jump, delay, Animator, or second timeline is introduced.

### 审查 / review

- **scope:** curve shape only; 35% completion point unchanged.
- **continuity:** one monotonic continuous warp followed by the existing continuous smoothstep; no Build-545 gate behavior.
- **ownership / lifecycle / single writer:** unchanged.
- **native motion:** CENTER/network target path and HyperOS expansion clock remain authoritative.
- **reverse:** the same stateless mapping is evaluated in reverse; no separate collapse animator.
- **performance:** constant arithmetic only; no allocation, probe, reflection, listener, or hierarchy traversal.
- **tests:** explicit curve checkpoints lock the faster first half; ordered-arc tests derive geometry from the policy's remaining fraction so they continue to verify topology rather than hard-code the old easing.

### Validation

Run exact-head Runtime CI and then one signed exact-head Canary. Device focus: compare Build 549 vs 550 during the first half of a normal and slow pull. The ring should yield visibly sooner from the start while the last part remains continuous, with no chunk disappearance or change to CENTER/network trajectory.


## 2026-10-01 — Build 551 Preview Sandbox mobile-network coverage

**Type:** preview/UI coverage + regression tests  
**Display version:** 0.0.3  
**Build:** 551 / `20261001-551`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Goal

Expose the mobile standards already handled by the generic native-label rendering path in Preview Sandbox instead of limiting manual preview to None / 4G / 5G / 5G-A.

### Implementation

- Add Preview Sandbox choices for `2G`, `E`, `3G`, `H+`, and `LTE`.
- Final UI order: None / 2G / E / 3G / H+ / 4G / LTE / 5G / 5G-A.
- Keep the original PreviewMobileNetwork ordinals (None=0, 4G=1, 5G=2, 5G-A=3) stable; new enum values append after them. The UI uses an explicit ordered choice list rather than enum ordinal order.
- Add `systemLabel` to PreviewMobileNetwork and feed it directly into the existing `CenterIndicator.MobileType` model.
- Use one horizontally scrollable MIUIX `TabRowWithContour` at a fixed comfortable content width rather than squeezing nine labels into the card width.
- Add bilingual resource entries; technology labels remain standards notation in both locales.
- Add production-path regression coverage proving 2G / E / 3G / H+ / 4G / LTE labels pass through `NativePresentationResolver.normalizeDrawableNetworkType` unchanged.

### 审查 / review

- **runtime architecture:** unchanged; production still reads HyperOS `mobile_type` / `mobile_type_single` and renders generic native text.
- **no per-standard fork:** no extra production branch for 2G/3G/LTE/H+ is introduced.
- **state compatibility:** legacy preview ordinals are preserved to avoid rememberSaveable restoring an old 4G/5G selection as a newly inserted standard.
- **layout:** scrolling prevents label compression; no custom density/touch geometry.
- **scope:** sandbox UI/model/resources and tests only; current Build-550 battery-ring transition runtime remains untouched.

### Validation

Run Runtime CI. Because the functional mapping is deterministic, no runtime-transition device gate is required. A Canary is useful only to visually review the nine-option sandbox control and confirm scrolling/touch ergonomics on the target device.


## 2026-10-01 — Build 552 smooth long-tail Battery-ring retract AB

**Type:** focused device-evidence curve refinement  
**Display version:** 0.0.3  
**Build:** 552 / `20261001-552`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Goal

Keep the accepted fast first half from Build 550, but make the latter half slower and finish later without creating an obvious two-speed or piecewise animation.

### Root cause / design

A literal 50/50 piecewise timing split would create a slope handoff that can read as layered speed. Build 552 instead keeps the same single analytic form used by Build 550 and retunes its two scalar parameters:
- completion point: `0.35 -> 0.45`;
- continuous front-load coefficient: `0.45 -> 0.92`;
- warp remains `p + FRONT_LOAD * p * (1 - p)`, followed by the same smoothstep.

This produces one monotonic continuous curve. The stronger front-load offsets the longer completion window during the early phase, while the longer terminal window and low end slope stretch the tail naturally.

### Checkpoints

Composite global progress -> consumed ring:
- 0.0875 -> about 26.62% (Build 550: about 26.06%);
- 0.175 -> about 65.88% (Build 550: about 66.59%);
- 0.35 -> about 98.85% (Build 550: 100%);
- 0.45 -> 100%.

So the first half stays visually close to Build 550, while the final ~1.15% decays through the extra tail instead of disappearing at 35%.

### 审查 / review

- one continuous stateless curve; no piecewise speed tier, threshold gate, delay, Animator, or second clock;
- ordered-arc topology and LEFT/RIGHT/NONE semantics unchanged;
- CENTER/network native target path and timing authority unchanged;
- Battery handoff, reverse symmetry, Build-542 island projection, and Build-551 sandbox coverage unchanged;
- constant arithmetic only; no new runtime allocations or hierarchy work;
- tests now lock both local curve continuity checkpoints and the intended global early/tail relationship.

### Validation

Run exact-head Runtime CI, then signed exact-head Canary because the requested difference is visual. Device focus: normal and slow pulls. The first half should feel essentially as quick as Build 550, then decelerate naturally into a slightly later tail without any visible speed step or chunk disappearance.


### Build 552 CI correction

Runtime CI #2066 failed only in `leftExitPreservesBatterySemanticsByIntersection`: the stronger continuous front-load means that at local progress 0.5 the retained LEFT suffix begins after the original 75% active-fill end, so `result.active` is correctly empty. The test's unconditional `active.single()` assumption was stale.

Correction is test-only: sample the active-fill intersection at local progress 0.35, where the retained suffix still overlaps the original active fill. Runtime policy, Build ID, curve parameters, topology, and APK behavior remain unchanged.


## 2026-10-01 — Build 553 compact mobile-standard selector

**Type:** Preview Sandbox UI refinement  
**Display version:** 0.0.3  
**Build:** 553 / `20261001-553`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Device / design feedback

The nine-standard horizontally scrollable segmented control from Build 551/552 exposes every standard but creates too many visible slots and dominates the Network card.

### Implementation

- Replace only the mobile-standard control with MIUIX `OverlayDropdownPreference`.
- Keep Mobile/Wi-Fi as the existing two-option `TabRowWithContour`, since that is a primary mutually-exclusive mode switch with only two choices.
- The new row shows the current standard inline and opens a single-choice MIUIX popup for None / 2G / E / 3G / H+ / 4G / LTE / 5G / 5G-A.
- Limit popup height to 360dp so long option lists scroll inside the native popup instead of expanding the page.
- Reuse `SandboxPreferenceInsideMargin` so title/value spacing aligns with `SliderPreference` and `SwitchPreference`.
- Remove the custom horizontal-scroll segmented helper and its scroll-state imports.

### 审查 / review

- Uses the library's purpose-built preference component rather than custom geometry.
- No preview model, enum ordinal, runtime SystemUI path, transition curve, or state ownership changes.
- All nine network standards remain available in the same explicit display order.
- Build-552 Battery-ring transition runtime remains byte-for-byte untouched by this UI refinement.

### Validation

Run Runtime CI. One signed Canary is justified only to inspect popup placement, row density, current-value alignment, and interaction feel on the target device.


## 2026-10-01 — Build 554 customization settings foundation

**Type:** settings schema / color policy foundation  
**Display version:** 0.0.3  
**Build:** 554 / `20261001-554`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Goal

Prepare the requested combined-icon sizing, ring thickness, Wi-Fi/mobile-type tuning, feature reset, and per-battery-mode color customization before exposing the controls.

### Settings model

Profile-scoped geometry (independent for Network-centered and Battery-centered layouts):
- combined scale 85%-115%, default 100%;
- ring stroke scale 70%-130%, default 100%;
- Wi-Fi size 80%-125%, default 100%;
- mobile-type size 80%-125%, default 100%;
- mobile-type weight 500-950, default 800.

Wi-Fi weight is deliberately not exposed: the steady path preferentially renders a native SystemUI drawable. Synthetic dilation or blur would violate native-first rendering and risks optical fuzziness.

Global battery-color state:
- presets: HyperOS native / iOS style;
- custom opaque overrides for Normal, Power Save, Performance, Super Power Save, Charging, and Low;
- iOS-style defaults use yellow #FFCC00, blue #007AFF, orange #FF9500, green #34C759, red #FF3B30; Normal follows status-icon tint;
- custom per-slot overrides win over the selected preset.

### Runtime semantic groundwork

- Add `SUPER_POWER_SAVE` as a distinct semantic state and accept common native enum aliases if HyperOS exposes one.
- Probe optional `mBatterySuperPowerSaveColor` / `mBatterySuperSaveColor`; if absent, HyperOS-native fallback uses the existing power-save color field.
- Existing semantic authority remains `MiuiBatteryMeterIconView.getProgressStatus()`.

### Reset semantics

- `CombinedStatusVisualSettingsRepository.resetToDefaults()` clears all visual settings back to schema defaults.
- `CombinedStatusFeatureSettingsRepository.resetToDefaults()` clears feature settings and refreshes the feature-change timestamp.
- Battery-color overrides also have a dedicated reset helper so a palette can be restored without resetting unrelated controls.

### 审查 / review

- Color preset/overrides are global; geometry remains layout-profile scoped.
- No second runtime settings owner is introduced; `RuntimeVisualPreferencesOwner` stays the single visual-settings bridge.
- Build-552 transition curve and Build-553 sandbox selector are untouched.
- Geometry fields are not consumed by Painter in this checkpoint, preventing half-wired steady vs transition geometry.

### Validation

Runtime CI must lock normalization, runtime-key participation, preset resolution, override precedence, super-power-save parsing, and compilation before geometry/UI wiring proceeds.


## 2026-10-01 — Build 555 reset lifecycle correction

**Type:** lifecycle / settings synchronization fix  
**Display version:** 0.0.3  
**Build:** 555 / `20261001-555`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Pre-commit 审查 / review

The Build-554 foundation was re-reviewed before continuing geometry/UI work. The review found that both visual and feature reset helpers used `SharedPreferences.clear()`, while App/Runtime listeners filtered only concrete keys. Android clear notifications may use `key == null`, so reset could restore persisted defaults without immediately refreshing observers.

The candidate correction was reviewed before branch update:
- feature key relevance is single-source through `isCombinedStatusFeaturePreferenceKey()`;
- App and Runtime feature listeners share that predicate;
- visual key relevance treats `null` as a whole-domain change and Runtime already delegates to that same predicate;
- unrelated non-null keys still do not trigger feature updates;
- feature reset keeps the existing change timestamp in the same editor transaction;
- no second settings owner, poller, or restart path is introduced.

### Change

- Accept `key == null` as a relevant whole-domain change for the dedicated feature and visual preference files.
- Add shared feature-key predicate to prevent App/Runtime filter drift.
- Add unit coverage for clear notification relevance.
- No changes to Build-554 geometry ranges, color presets, semantic mapping, or painter behavior.

### Validation

Run exact-head Runtime CI. No real-device gate is required because this change only repairs observer invalidation semantics; UI reset controls are not exposed yet.


## 2026-10-01 — Build 556 outer-weight geometry wiring

**Type:** runtime geometry wiring  
**Display version:** 0.0.3  
**Build:** 556 / `20261001-556`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Requirement correction

Maintainer clarified that “outer-ring thickness” is intentionally a coupled visual family: changing it should also change the four mobile dots and the unavailable X mark, with dot spacing adapting so the whole lower opening remains visually even.

The repository already contains the correct primitive: `CombinedStatusOuterGeometry.resolve(weightScale)` scales:
- ring stroke;
- mobile-dot radius;
- unavailable-mark stroke and extent;

and then solves dot angular spacing so ring-to-dot and dot-to-dot edge gaps remain balanced.

### Pre-commit 审查 / review

The Build-556 candidate was reviewed before branch update:
- foundation naming changed from `ringStrokeScale` to `outerWeightScale` so UI/schema semantics match the actual coupled behavior;
- setting remains profile-scoped, default 1.0, supported UI range 0.70-1.30;
- all runtime outer-geometry entry points use `visualSettings.outerWeightScale`;
- steady draw, Battery/Battery-number transition draw, Mobile transition draw, and transition source bounds therefore share one geometry source;
- the only remaining static default is the painter cache initializer, which is replaced on first resolved draw and is not an authoritative runtime path;
- no new solver/animator/listener/writer is introduced;
- existing balanced-gap solver remains authoritative;
- existing `fiveVisualEdgeGapsStayBalancedAcrossSupportedScales` regression coverage is preserved;
- new test explicitly locks that ring, dots, and unavailable mark scale as one family.

### Compatibility

No user-facing Build-554/555 UI exposed the foundation-only `ring_stroke_scale` key, so renaming it to `outer_weight_scale` does not migrate a released user setting. Default 100% preserves the accepted 8.25 ring baseline and existing dot/X geometry.

### Validation

Run exact-head Runtime CI. No device gate is required yet because the control is not exposed in UI and the default value leaves runtime appearance unchanged.


## 2026-10-01 — Build 557 independent center geometry

**Type:** runtime geometry wiring  
**Display version:** 0.0.3  
**Build:** 557 / `20261001-557`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Goal

Expose only the requested center-family controls without letting Wi-Fi sizing accidentally resize native airplane/no-SIM icons.

### Pre-commit 审查 / review

The candidate was reviewed before branch update:
- remove the old shared `centerSizeScale` / `centerTextWeightScale` painter override API;
- one settings-backed resolver now supplies Wi-Fi size, mobile-type size, and mobile-type source weight to all center draw/transition/source-bound paths;
- Wi-Fi fallback vector uses only `wifiSizeScale`;
- airplane/no-SIM max sizes remain fixed at the accepted native optical baselines;
- mobile-type size scales text/suffix geometry only;
- mobile-type weight is absolute 500-950, default 800;
- custom mobile weight remains the transition source weight; existing `MobileTypeTransitionPolicy` still interpolates to the SystemUI target weight;
- settings UI bounds remain 80%-125% while lower-level geometry keeps a wider defensive clamp;
- zero legacy shared-size/shared-weight tokens remain in the candidate painter;
- no new writer, listener, animator, or target-geometry owner is introduced.

### Tests

Replace the obsolete “all center families share one size” test with independent contracts:
- Wi-Fi scale changes Wi-Fi only;
- mobile-type scale changes mobile text/suffix only;
- mobile-type weight changes typography only;
- airplane/no-SIM remain fixed when Wi-Fi changes;
- invalid/out-of-range inputs clamp safely;
- existing 5GA lower-right suffix direction remains locked.

### Validation

Run exact-head Runtime CI. Default values preserve current runtime appearance, so no device gate is required until UI controls are exposed.


## 2026-10-01 — Build 558 shrink-only overall combined scale

**Type:** runtime geometry wiring  
**Display version:** 0.0.3  
**Build:** 558 / `20261001-558`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Final range

Maintainer set 100% as both the default and maximum overall size. The supported range is therefore 75%-100%, with 100% as the future slider key point/magnet.

This intentionally permits shrinking only. It avoids increasing the host viewport requirement and remains safe when outer weight is independently increased.

### Pre-commit 审查 / review

The candidate was reviewed before branch update:
- `COMBINED_SCALE_MIN = 0.75`, `MAX = DEFAULT = 1.00`;
- one `resolveCanvasTransform()` owns effective scale and centered offsets;
- all ten runtime geometry paths use the same helper;
- raw `min(width / CANONICAL_SIZE, height / CANONICAL_SIZE)` calculation remains only inside that helper;
- steady draw, top-overflow calculation, transition drawing/specs, airplane/no-SIM bounds, mobile-type current bounds, and Battery Number current bounds therefore cannot diverge;
- default 100% preserves Build-557 geometry exactly;
- no new View size, LayoutParams, writer, listener, animator, or transition clock is introduced;
- range constants are sourced from settings schema rather than duplicated in Painter.

### Tests

Settings normalization now locks:
- 100% is both default and maximum;
- values above max clamp to 100%;
- values below the supported range clamp to 75%.

### Validation

Run exact-head Runtime CI. No device gate yet because no UI exposes the new setting and the default leaves runtime output unchanged.


### Build 558 CI correction

Runtime CI #2075 failed at Kotlin compilation because two functions retained an obsolete local `NativeRenderTransform(...)` construction after being migrated to `resolveCanvasTransform()`, producing duplicate `nativeTransform` declarations.

Pre-commit review of the correction confirmed:
- both duplicate constructions are removed;
- every function using `resolveCanvasTransform()` now has at most one local `nativeTransform`;
- the raw canonical scale calculation still exists only inside `resolveCanvasTransform()`;
- helper call count and all Build-558 scale semantics remain unchanged.

This correction is compile-only. Build ID, 75%-100% range, default/max 100%, transition geometry, and runtime behavior are unchanged.


### Build 558 CI correction 2

Runtime CI #2076 exposed one remaining compile-only residue in `transitionBatteryNumberCurrentBounds()`: after the duplicate local transform was removed, the returned bounds still referenced deleted local `offsetX/offsetY` names.

Pre-commit review of the correction confirmed:
- the function owns exactly one `nativeTransform`;
- all four returned bound coordinates use `nativeTransform.offsetX/offsetY` directly;
- raw canonical scale calculation remains only inside `resolveCanvasTransform()`;
- helper call count and all Build-558 scale semantics remain unchanged.

No runtime behavior, range, transition timing, or visual default changed.


## 2026-10-01 — Build 559 per-mode battery color sources and Recommended preset

**Type:** color settings schema / runtime policy foundation  
**Display version:** 0.0.3  
**Build:** 559 / `20261001-559`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### User-facing model

Palette order/naming for the upcoming UI:
1. Recommended
2. HyperOS
3. iOS

Recommended is the default for new installs and after a full feature reset. HyperOS keeps the persisted value `hyperos_native` for backward compatibility; only the UI label changes.

Each battery semantic slot independently selects one source mode:
- preset color;
- follow system tint/inversion;
- custom color.

The global palette therefore supplies defaults only for slots currently using “preset color”; it never locks the whole color set.

### Recommended palette candidate

Initial muted status-bar candidate:
- Power save: `#D5A623`
- Performance: `#4A7FC1`
- Super power save: `#D8752C`
- Charging: `#3FA760`
- Low battery: `#D64A4A`
- Normal: follow status-icon tint

These values are intentionally less luminous than the iOS semantic set and are not treated as final until the color BottomSheet/preview receives optical and device review.

### Backward compatibility

- Existing installs without an explicit palette are detected through the pre-existing visual schema marker. The shared `readCombinedStatusVisualSettings()` fallback resolves them as HyperOS immediately, and the App-side one-time migration persists that choice when the repository initializes. SystemUI therefore cannot transiently switch an old install to Recommended merely because it starts first.
- Fresh installs have no previous visual schema marker and default to Recommended.
- Existing `hyperos_native` persisted values map directly to the renamed HyperOS enum member.
- Legacy custom colors that predate per-slot mode keys infer `CUSTOM` automatically.
- A stored custom color remains persisted when a slot switches to Preset or Follow System; it becomes active again if the slot later returns to Custom.
- Custom mode without a valid stored color falls back to that slot’s current preset source.

### Pre-commit 审查 / review

- Palette selection remains global; source mode remains per semantic slot.
- Runtime resolution order is explicit: per-slot mode -> selected palette/custom/system source -> existing visibility fallback.
- Follow System always resolves to the current status-icon tint and therefore retains native black/white inversion behavior.
- HyperOS preset still delegates to SystemUI semantic colors instead of duplicating fixed hex values.
- Recommended/iOS Normal remain monochrome by following status-icon tint.
- Mode keys are included in the visual runtime-key set, so App and SystemUI hot updates use the existing single visual-settings bridge.
- No second battery observer, color owner, listener, or writer is introduced.

### Tests

Coverage added/updated for:
- Recommended as the new default;
- old-install missing-preset migration to HyperOS;
- fresh-install missing-preset default to Recommended;
- legacy stored custom color -> Custom mode inference;
- Recommended semantic values;
- per-slot Follow System overriding an iOS preset;
- stored custom color ignored while slot mode is Preset;
- stored custom color used again when slot mode is Custom;
- all new mode keys participating in runtime synchronization.

### Validation

Run exact-head Runtime CI before any BottomSheet/UI work is committed.


### Build 559 CI correction — legacy color-policy expectations

Runtime CI #2078 compiled the new palette/mode model but exposed four existing `CombinedStatusColorPolicyTest` cases whose expectations still assumed the old global default was HyperOS.

Pre-commit review separated test intent instead of blindly replacing expected colors:
- the native semantic-color test now explicitly selects the HyperOS preset;
- the default performance-mode test now validates the Recommended performance color;
- the optional center/mobile follow test explicitly selects HyperOS so it continues to test propagation of the final battery color rather than palette choice;
- the battery-text / charging-icon independent tint test explicitly selects HyperOS so it continues to isolate its intended follow-system behavior.

Runtime production code is unchanged. This is a test-contract correction for the intentional default-palette change introduced by Build 559.


### Build 559 validation closure

Exact-head Runtime CI #2079 (run `36880365780`) completed successfully on `e37d516`.
- unit tests passed after old HyperOS-default expectations were separated from new Recommended-default behavior;
- debug APK build succeeded;
- pinned HyperOS target verification passed;
- modern Xposed metadata verification passed.

Build 559 color-source foundation is closed. No device gate is required before UI exposure because existing installs remain on HyperOS unless the user explicitly changes the palette, while fresh/reset defaults are not user-visible until the settings UI is completed.


## 2026-10-01 — Build 560 MIUIX feature-page size controls and reset card

**Type:** settings UI / feature-page organization  
**Display version:** 0.0.3  
**Build:** 560 / `20261001-560`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Scope

Keep the feature page as the primary settings surface, grouped by the existing card structure instead of turning it into a navigation-only page.

Cards:
1. Global
2. Network
3. Battery
4. Management

### New direct controls

Global:
- Overall size: 75%-100%, 5% steps, default/max 100%;
- Outer weight: 70%-130%, 5% steps, default 100%; this is the existing coupled ring + four-dot + unavailable-mark family.

Network:
- Wi-Fi size: 80%-125%, 5% steps, default 100%;
- Mobile type size: 80%-125%, 5% steps, default 100%;
- Mobile type weight: 500-950, 50-weight steps, default 800.

All five controls use MIUIX `SliderPreference`, `showKeyPoints = true`, a single default `keyPoints` value, and the existing magnetic snap threshold. No custom slider or gesture implementation is introduced.

### Restore defaults

A Management card adds “Restore defaults”.
- It is intentionally available even when the feature master switch is off.
- Confirmation uses the existing MIUIX `OverlayDialog`.
- Confirming resets both `CombinedStatusFeatureSettingsRepository` and `CombinedStatusVisualSettingsRepository`.
- Feature defaults restore the master feature to enabled and lock-screen combined status to disabled.
- Visual defaults restore the active schema defaults, including Recommended palette and 100% geometry defaults.

### Copy review

Chinese and English copy was shortened and normalized during the same UI pass:
- layout summary is reduced to the memory behavior;
- battery readout summary focuses on percentage + automatic avoidance;
- charging summary removes redundant phrasing;
- lock-screen summary removes repeated “combined icon” wording;
- network color-follow summaries use consistent terminology;
- new controls use concise titles such as “Overall size / 整体大小” and “Mobile type weight / 移动制式字重”.

### Pre-commit 审查 / review

- Existing MIUIX Card / SmallTitle spacing is reused; no custom card style is added.
- `HubPage` gains only an optional fourth section, so Settings and other existing three-section callers remain unchanged.
- New controls bind directly to the existing single visual-settings repository; no additional state owner is introduced.
- Slider ranges and default key points come from the same schema constants consumed by runtime.
- Restore is the only control intentionally not gated by `featureSettings.enabled`.
- Existing battery-number/charging detailed sliders remain on the page in this checkpoint; they are not prematurely moved to drawers before final density review.
- No runtime drawing, transition, or SystemUI hook behavior changes in Build 560.

### Validation

Run exact-head Runtime CI to compile the new MIUIX calls/resources and lock repository wiring. Device review is deferred until the color BottomSheet and final feature-page density pass are complete.


### Build 560 validation closure

Exact-head Runtime CI #2081 (run `36881730709`) completed successfully on `c857759`.
- all unit tests passed;
- MIUIX feature-page controls/resources compiled successfully;
- debug APK build succeeded;
- pinned HyperOS target verification and modern Xposed metadata checks passed.

Build 560 is closed. The next change is isolated to battery-color BottomSheet UI and will use a separate, descriptive commit.


## 2026-10-01 — Build 561 MIUIX battery-color BottomSheet

**Type:** settings UI / battery color overview  
**Display version:** 0.0.3  
**Build:** 561 / `20261001-561`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Scope

Expose the Build-559 color-source model through a native MIUIX BottomSheet without introducing a second battery-state authority.

The Battery card gains one concise entry:
- title: Battery colors / 电量颜色;
- summary: current palette;
- end area: five semantic preview dots for power save, performance, super power save, charging, and low battery.

### Preview semantics

The App process does not own HyperOS battery semantic state; the authoritative source remains SystemUI `MiuiBatteryMeterIconView.getProgressStatus()`.

Therefore:
- Recommended/iOS fixed palette colors render as filled preview dots;
- stored Custom colors render as filled preview dots;
- HyperOS preset colors render as outlined/dynamic dots because the actual semantic value comes from SystemUI at runtime;
- Follow System also renders as outlined/dynamic;
- Custom mode without a stored custom color follows the runtime policy and previews its preset fallback;
- Normal remains dynamic for Recommended/iOS because it follows status-icon tint.

This deliberately does not add PowerManager/BatteryManager inference or another cross-process writer merely to fake a “current mode” preview.

### BottomSheet interaction

A single MIUIX `OverlayBottomSheet` is used as a state machine:
- overview: Recommended / HyperOS / iOS palette selection plus all semantic slots;
- slot detail: Scheme color / Follow system / Custom source selection;
- backing out of slot detail returns to the overview rather than stacking another sheet.

MIUIX `RadioButtonPreference`, `ArrowPreference`, `Card`, and `SmallTitle` are reused. No custom drawer implementation is introduced.

### Pre-commit 审查 / review

- Only one `OverlayBottomSheet` exists in the new color UI.
- Palette selection order is Recommended -> HyperOS -> iOS.
- Slot source writes use the existing `CombinedStatusVisualSettingsRepository.setBatteryColorMode`.
- Palette writes use the existing `setBatteryColorPreset`.
- The existing feature reset dialog remains independent from the color sheet.
- UI copy is bilingual and concise.
- No SystemUI runtime/hook/transition code changes.
- No new state observer, listener, or cross-process writer.

### Tests

Pure preview-resolution tests cover:
- Recommended charging and iOS low-battery fixed colors;
- HyperOS and Follow System remain dynamic;
- Custom uses its stored color;
- Custom without a stored color falls back to the selected preset;
- Recommended Normal remains dynamic/status-tint based.

### Deferred to next isolated change

Custom color editing UI:
- common colors;
- MIUIX ColorPicker / ColorPalette;
- RGB and HEX input;
- per-mode reset-to-default.

The source mode is already persisted in Build 561, but no incomplete custom editor is represented as finished.

### Validation

Run exact-head Runtime CI before adding the custom color editor.


### Build 561 validation closure

Exact-head Runtime CI #2083 (run `36884110591`) completed successfully on `b0ba53f`.
- new MIUIX BottomSheet UI compiled successfully;
- all preview-resolution tests passed;
- debug APK build succeeded;
- pinned HyperOS target and modern Xposed metadata verification passed.

Build 561 is closed. Custom color editing remains isolated to the next commit.


## 2026-10-02 — Build 563 scale-aware compact reservation and Mobile Type weight range

**Type:** runtime geometry / settings correction  
**Display version:** 0.0.3  
**Build:** 563 / `20261001-563`  
**Branch / PR:** `feat/battery-top-readout` / #181

### Device evidence and root cause

Build-562 device feedback identified two independent issues:
- Mobile Type weight still exposed the old 500-950 / 800 contract rather than the requested 400-1400 / 900 midpoint.
- Overall size scaled only Guiyuan painter pixels. The native replacement reservation remained the full stable Battery carrier width, so neighboring HyperOS status icons could not close the visual gap.

The Build-562 diagnostic confirms `compactSlotWidth=105` remained unchanged while the renderer accepted live visual settings. The correction therefore belongs to the existing reservation geometry, not to a new spacing offset.

### Implementation

- Mobile Type weight: 400-1400, 50-weight slider intervals, default/key point 900.
- Add one centered-scale reservation rule: because painter shrink is centered in the stable Battery carrier, peer reservation ends at the scaled visual's leading edge while retaining the transparent end-side inset.
- Reuse that rule for Home/Keyguard/Control Center native padding and the transition reservation/latent-reveal compact baseline.
- Visual preference changes ask the existing `SystemUiHomePresentationOwner` to resync its reservation; no second padding/translation writer is added.

### Review

- Geometry is derived from the same base carrier width + user scale; no device-specific px compensation.
- `paddingEnd` remains single-writer owned by the existing presentation session.
- Scale remains shrink-only and the painter remains the sole Guiyuan pixel owner.
- Transition target geometry, HyperOS island width authority, animation clocks, and native peer motion are unchanged.
- Failure paths remain native because unavailable carrier/layout inputs still abort the existing reservation path.

### Validation

Automated validation is expected to be Full while #181 still includes the independently reviewed CI run-title delta. No work-branch Canary is requested by this change alone; device evidence is deferred until the color-UI/runtime palette work is grouped into one focused checkpoint.

## 2026-10-01 — Build 562 MIUIX custom battery color editor

**Type:** settings UI / custom battery colors  
**Display version:** 0.0.3  
**Build:** 562 / `20261001-562`  
**Branch / PR:** `feat/battery-top-readout` / #181  

### Scope

Complete the Custom source path introduced by Build 559/561 without adding another screen or another BottomSheet instance.

The existing single BottomSheet now has a third internal state:
1. palette/mode overview;
2. per-mode source selection;
3. per-mode custom color editor.

### Custom editor

The editor provides:
- 10 common color shortcuts;
- full opaque HSV adjustment using MIUIX `HsvHueSlider`, `HsvSaturationSlider`, and `HsvValueSlider`;
- exact six-digit HEX input;
- exact RGB input;
- current-color preview;
- per-mode restore-default action.

The built-in MIUIX `ColorPalette` / `ColorPicker` components were reviewed but intentionally not used because MIUIX 0.9.4 always exposes an alpha slider while Guiyuan persists battery semantic colors as opaque. Showing a control whose result is discarded would violate the UI/runtime contract.

### Common colors

Ten compact shortcuts cover red, orange, yellow, green, cyan, blue, indigo, purple, pink, and neutral gray. They are shortcuts only, not a fourth named palette.

### Persistence

- Picking/editing a custom color guarantees the slot is in `CUSTOM` mode and writes the opaque ARGB value through the existing visual-settings repository.
- `resetBatteryColorSlot(slot)` removes that slot’s source-mode key and override in one SharedPreferences editor transaction.
- Resetting a slot therefore returns it to `PRESET` mode using the currently selected Recommended / HyperOS / iOS scheme.
- No global palette or other slot is changed.

### Input rules

- HEX accepts exactly six hexadecimal digits (optional leading `#`) and forces alpha to FF.
- RGB accepts integer channels 0-255.
- Invalid or incomplete input does not mutate the persisted color.
- For an unset custom color, editor initialization prefers: stored override -> selected fixed palette color -> current MIUIX foreground only as a local editing seed for dynamic SystemUI colors. This seed is not persisted until the user changes a value.

### Pre-commit 审查 / review

- No alpha control is exposed.
- No Material color picker or text field is introduced.
- The BottomSheet instance count remains one.
- The new editor uses only existing repository ownership.
- Per-mode reset is atomic.
- Common colors are UI shortcuts, not persisted as a separate scheme.
- No SystemUI hook, transition, or battery-state ownership changes.

### Tests

Battery color UI tests now also cover:
- valid/invalid six-digit HEX parsing;
- RGB 0-255 bounds;
- RGB split/round-trip;
- editor initial-color precedence and dynamic fallback opacity.

### Validation

Run exact-head Runtime CI before any device review.


### Build 562 CI correction — final reviewed editor candidate

Runtime CI #2085 (run `36885816625`) used an earlier editor candidate and failed Kotlin compilation at `BatteryColorControls.kt` because of an explicit `androidx.compose.foundation.layout.weight` import. In this Compose version that import resolves to an internal parent-data property, while `Modifier.weight()` is already available from the RowScope used by the existing project UI.

The correction:
- removes the explicit `weight` import only; layout behavior is unchanged;
- restores the later reviewed interaction where opening the Custom editor does not immediately write `CUSTOM`;
- writes `CUSTOM + color` only after a valid common-color / HSV / HEX / RGB edit;
- uses the Recommended color for the same semantic slot as the editor start when the selected source is dynamic, falling back to current foreground only where no semantic fixed color exists;
- keeps the per-mode atomic reset and opaque-only color contract;
- expands pure UI logic tests for HEX/RGB parsing, RGB round-trip, and editor initial-color priority.

The PR display title process was also verified: CI #2085 displayed `feat: add MIUIX custom battery color editor`, confirming that updating the PR title before the work-branch HEAD update makes the Actions list describe the concrete Build objective without changing workflow trigger/security semantics.

No SystemUI runtime, hook, transition, or rendering behavior changed in this correction.


### Build 562 CI correction 2 — remove duplicate editor tests

Runtime CI #2090 (run `36887864769`) compiled the production app successfully. Unit-test compilation then failed because iterative review had appended a second set of tests covering the same HEX/RGB parsing and editor initial-color priority, including a duplicate function named `editorInitialColorPrefersStoredThenPresetThenDynamicFallback`.

Correction:
- remove the later duplicate parser / initial-color / RGB round-trip block;
- keep the original seven focused tests;
- confirm there are no duplicate test function names;
- production code is unchanged.

The workflow/run-name cleanup is intentionally deferred until after the next Canary is delivered for device testing.


## 2026-10-02 — CI run-title clarity

**Type:** CI presentation only

GitHub PR-triggered workflow runs previously inherited the pull-request title because the workflows did not define `run-name`. This made unrelated commits appear under the same Actions title.

Change:
- Build PR runs now show run number + PR number + work branch + exact PR HEAD SHA.
- Build push runs show run number + branch + push head commit message.
- Manual Build runs show run number + branch.
- Comment-triggered Canary runs show Canary run number + PR number.
- Manual Canary runs show Canary run number + requested source branch.

Review:
- workflow names remain `Build` and `Work Branch Canary`;
- job ids/names remain `build` and `canary`;
- no permissions, triggers, validation scope, signing, artifact, concurrency, or required-check behavior changed.

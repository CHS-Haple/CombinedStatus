

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

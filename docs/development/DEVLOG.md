

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

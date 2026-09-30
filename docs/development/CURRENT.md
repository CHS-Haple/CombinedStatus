# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.3.
- `main` and `dev` are synchronized at Build 511 / `20260930-511`.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- Build 510 non-charging Home transition and Build 511 charging Home transition are device-accepted.
- The current transition ownership, reservation, Fail-native, and release-parity boundaries from Build 511 remain protected.

## Active objective

PR #181 / `feat/battery-top-readout` adds an optional battery percentage readout in the top opening of the Guiyuan battery ring.

Current checkpoint:
- Build 525 / `20261001-525`;
- branch remains based on current `dev` and is not behind it;
- Build 524 Runtime CI #1961 passed and improved the final native Battery-number target resolution for HyperOS hollow-battery presentation;
- Build-523 device evidence then established two steady/readout geometry defects independent of that target work:
  1. positive vertical offset stopped early because canonical `y=0` was incorrectly treated as the physical render-View top even though the 120×120 canonical canvas is vertically centered inside the taller Battery carrier;
  2. charging-bolt vertical placement used the visible envelope midpoint, which is not the visual ink center of an asymmetric folded lightning glyph.
- Build 525 maps the real render-View top through the existing `NativeRenderTransform` into canonical coordinates and uses that as the only positive-offset clipping boundary.
- Positive offset is now linear and literal while real headroom exists: +N moves the readout N canonical units upward; negative offset keeps the existing direct downward semantics.
- Charging safety uses the combined visible readout height, so the full requested travel is available until actual View clipping would occur.
- Build 525 extends the existing bounded native drawable probe with an alpha-weighted ink centroid. The same cached probe still owns native-resource geometry; no second resource parser/probe path is introduced.
- Charging-bolt Y placement aligns that native ink centroid to the percentage text optical center line. Horizontal group centering remains based on visible glyph/text envelope width and is otherwise unchanged.
- Build 524's native Battery-number target improvements remain carried forward unchanged.
- Percentage readout remains opt-in and defaults off.
- HyperOS remains authoritative for charging resource selection through `MiuiBatteryMeterView.getHollowChargingIconId()`.

## Validation state

Confirmed:
- Build 523 focused device evidence reproduced both the vertical-offset ceiling and charging bolt/number optical-center mismatch.
- Build 524 Runtime CI #1961: green.
- PR #181 remains mergeable.
- Build 525 static review: no new native layout, translation, visibility, alpha, or gesture writer.
- The alpha-centroid extension reuses the existing bounded/cached drawable snapshot path; no polling or frame-time raster probe is added.
- New unit coverage checks real View-to-canonical top mapping, full positive travel above canonical zero, physical clipping behavior, and alpha-weighted ink-center extraction.

Pending:
- Build 525 Runtime CI.
- focused Build-525 device validation:
  - + values continue moving upward through the full useful range and stop only at the real View top boundary;
  - charging bolt visible-ink vertical center matches the percentage visible-text center;
  - ordinary / quick / super charging resource changes keep that Y alignment;
  - Home -> Control Center percentage motion still reaches the Build-524 native Battery-number target;
  - accepted Build-510/511 Battery-body transition remains unchanged.

## Runtime / rendering contract

- HyperOS remains authoritative for battery state, charging-glyph resource selection, and Control Center expansion/motion.
- Guiyuan only observes the native charging resource after HyperOS updates its own presentation.
- Missing or zero native charging resource fails native at the glyph level: no project-owned replacement drawable is invented.
- The existing Guiyuan painter remains the only writer of Guiyuan pixels.
- The existing Battery transition component remains the only Guiyuan owner of battery-component transition rendering.
- `statusIcons.paddingEnd` remains the sole Guiyuan native peer-layout writer.
- Native drawable visual geometry is obtained from one bounded cached probe; envelope geometry and alpha-weighted ink center are read-only measurements from the same probe.
- No polling, delayed state inference, new frame hook, duplicate charge-speed observer, or second gesture animator is introduced.
- Feature default-off preserves the accepted Build-511 visual path until the user enables the readout.

## Non-negotiable boundaries

- Root-cause first; no screenshot-fitted timing or fixed per-resource Y compensation.
- Preserve accepted Build-510/511 transition behavior unless contradictory device evidence appears.
- One mutable runtime property has one writer.
- Cleanup / Hot Reload restores only Guiyuan-owned state.
- Compatibility uncertainty fails native.
- HyperOS resources / state / motion are preferred over project-local copies or guesses.
- Do not infer quick/super-charge semantics from parallel fields while the native selected drawable already expresses the required presentation.

## Immediate next step

1. finish Build-525 Runtime CI and automated review;
2. if green, freeze runtime at exact Build 525 and request one signed work-branch Canary;
3. perform only the focused vertical-travel / charging-center / Battery-number-target device validation above;
4. change runtime again only if that device evidence identifies a concrete remaining defect;
5. merge to `dev` only after the visual checkpoint is device-accepted.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. task-specific architecture / reference docs;
4. current source and exact-target SystemUI evidence;
5. relevant `DEVLOG.md` history when needed.

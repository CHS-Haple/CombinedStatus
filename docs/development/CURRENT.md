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
- Build 522 / `20261001-522`;
- branch is based directly on current `dev`;
- Build-513 device feedback showed the original 16 px / weight-600 typography baseline was visibly too small and light, and the 75%-135% size range was too narrow;
- Build 514 redefined 100% as a 24 px authored baseline and expanded size/weight adjustment to 60%-200% / 400-900;
- Build 515 raised the default number weight to 900 and restored +up / −down offset semantics;
- Build 516-518 refine the top-readout optical layout from device evidence: charging-glyph 100% baseline 14→18, visible glyph-to-number gap 2→1, ring clearance now grows from measured ink height and ring stroke, and +0…+30 maps across the currently safe upward travel instead of clipping beyond the status-bar drawing boundary;
- charging keeps one stable 18-unit slot while the native single/double-bolt optical ink is right-aligned inside it, preventing percentage X-position jumps when HyperOS changes the charging drawable;
- Build 519 was a diagnostic-only checkpoint for the requested battery-number Control Center morph;
- Build 520/521 recalibrate battery-top controls from device evidence: number size and charging-glyph size are user-facing 0%-200% ranges with 100% as the MIUIX key point/default reference; the maintainer's Build-519 number 130% maps to Build-521 number 100%, and charging 150% maps to Build-521 charging 100%, preserving both accepted physical sizes while making 100% the meaningful default midpoint;
- number weight is 400-1400 with 900 as the default key point; Android-native weighted Typeface is used through 1000 and a bounded size-relative optical stroke extends the visible range above 1000;
- positive number offset ownership is corrected: number Y safety uses number ink only, charging-glyph height no longer collapses positive travel to zero, and neutral geometry preserves bounded upward headroom;
- the percentage stays optically centered over the battery ring; the charging glyph is placed from the native drawable's measured optical bounds on the left, so transparent viewport margins no longer push the percentage sideways;
- the native Battery-number target probe is retained and emitted into structured Runtime health as `batteryNumberTarget`, preventing later slider traffic from evicting the target evidence from detailed exports;
- Build 522 corrects charging optical composition: the visible native bolt ink + 1-unit gap + percentage ink is centered as one group, while transparent drawable viewport margins remain excluded;
- Build 522 removes the legacy 82° top-gap ceiling for the rebased three-digit/charging readout: measured optical clearance may grow to a bounded 118° maximum, with larger ink/stroke-derived side safety;
- Build 522 restores useful positive vertical movement by reserving 12 canonical units of safe neutral headroom and applying an ease-out response across +0…+30, while +30 still terminates at the real top-safe boundary;
- Build 522 adds a real `BATTERY_NUMBER` transition subcomponent under the existing transition owner. Its target resolves from the final native Battery percentage TextView ink bounds when available, then falls back to the native `MiuiBatteryMeterIconView` Paint typography/content geometry. Number size follows target geometry and weight interpolates toward the native target before native handoff;
- percentage readout is opt-in and defaults off;
- ring top avoidance is derived from the measured readout width rather than a screenshot-fitted fixed gap;
- charging reserves a stable leading glyph slot so the percentage does not shift while native charging presentation updates;
- the charging glyph resource is read from HyperOS `MiuiBatteryMeterView.getHollowChargingIconId()` after `updateChargeAndText()`; Guiyuan does not maintain a parallel quick/super-charge state machine;
- number size, weight, vertical offset, and charging-glyph size are user-adjustable through MIUIX controls;
- the readout remains inside the existing Battery render/transition component; no second motion owner is introduced.

## Validation state

Confirmed:
- PR #181 is mergeable and remains isolated from the old superseded transition branch.
- Build 522 Runtime CI #1957: green.
- Pinned HyperOS target profile: green.
- Unit tests: green, including width-derived battery-top gap coverage.
- Debug APK build: green.
- Modern Xposed metadata validation: green.
- Exact pinned MIUIX `SliderPreference` API was checked against revision `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`.
- Static review found no new native layout/translation/visibility/animation writer.

Pending:
- focused Build-522 device validation that charging bolt+number visible ink is centered as one group and the widened ring opening gives comfortable side clearance;
- confirm positive offset now moves visibly at small/medium values and still avoids top clipping at +30; negative movement remains unchanged;
- confirm Home -> Control Center percentage motion now moves/scales into the native Battery percentage target without a duplicate/squashed number;
- export one detailed diagnostic after the pull and verify `batteryNumberTarget` plus transition witness reports whether the target used native TextView or native Paint fallback;
- confirm no regression to accepted Build-510/511 Battery-body transition behavior.

## Runtime / rendering contract

- HyperOS remains authoritative for battery state and charging-glyph resource selection.
- Guiyuan only observes the native charging resource after HyperOS updates its own presentation.
- Missing or zero native charging resource fails native at the glyph level: no project-owned replacement drawable is invented.
- The existing Guiyuan painter remains the only writer of Guiyuan pixels.
- The existing Battery transition component remains the only Guiyuan owner of battery-component transition rendering.
- `statusIcons.paddingEnd` remains the sole Guiyuan native peer-layout writer.
- No polling, delayed state inference, new frame hook, duplicate charge-speed observer, or second gesture animator is introduced.
- Feature default-off preserves the accepted Build-511 visual path until the user enables the readout.

## Non-negotiable boundaries

- Root-cause first; no screenshot-fitted timing or geometry compensation.
- Preserve accepted Build-510/511 transition behavior unless contradictory device evidence appears.
- One mutable runtime property has one writer.
- Cleanup / Hot Reload restores only Guiyuan-owned state.
- Compatibility uncertainty fails native.
- HyperOS resources / state / motion are preferred over project-local copies or guesses.
- Do not infer quick/super-charge semantics from `mQuickCharging` or other fields while the native selected drawable already expresses the required presentation.

## Immediate next step

1. keep runtime frozen at Build 522;
2. generate one exact-head signed work-branch Canary for PR #181;
3. perform focused optical/vertical validation plus Home -> Control Center number-target validation;
4. change runtime only if Build-522 device evidence identifies a concrete target/geometry defect;
5. merge to `dev` only after the visual checkpoint is device-accepted.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. task-specific architecture / reference docs;
4. current source and exact-target SystemUI evidence;
5. relevant `DEVLOG.md` history when needed.

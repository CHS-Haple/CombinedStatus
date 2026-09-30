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
- Build 515 / `20261001-515`;
- branch is based directly on current `dev`;
- Build-513 device feedback showed the original 16 px / weight-600 typography baseline was visibly too small and light, and the 75%-135% size range was too narrow;
- Build 514 redefined 100% as a 24 px authored baseline and expanded size/weight adjustment to 60%-200% / 400-900;
- Build 515 raises the default number weight to 900 and fixes vertical offset so + values move upward and − values move downward across a full ±30 range;
- percentage readout is opt-in and defaults off;
- ring top avoidance is derived from the measured readout width rather than a screenshot-fitted fixed gap;
- charging reserves a stable leading glyph slot so the percentage does not shift while native charging presentation updates;
- the charging glyph resource is read from HyperOS `MiuiBatteryMeterView.getHollowChargingIconId()` after `updateChargeAndText()`; Guiyuan does not maintain a parallel quick/super-charge state machine;
- number size, weight, vertical offset, and charging-glyph size are user-adjustable through MIUIX controls;
- the readout remains inside the existing Battery render/transition component; no second motion owner is introduced.

## Validation state

Confirmed:
- PR #181 is mergeable and remains isolated from the old superseded transition branch.
- Build 515 Runtime CI #1943: green.
- Pinned HyperOS target profile: green.
- Unit tests: green, including width-derived battery-top gap coverage.
- Debug APK build: green.
- Modern Xposed metadata validation: green.
- Exact pinned MIUIX `SliderPreference` API was checked against revision `5c91d5e5ce1a2fc7e8bdc1258a881c555102bbca`.
- Static review found no new native layout/translation/visibility/animation writer.

Pending:
- first focused device validation for the new top readout;
- non-charging visual alignment and top-ring clearance;
- ordinary charging native single-bolt resource placement;
- quick/super charging native alternate-bolt resource placement when that native state is available;
- no percentage horizontal jump during charging-state/resource handoff;
- Home steady, Keyguard when enabled, and Home -> Control Center transition continuity.

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

1. keep runtime frozen at Build 515;
2. generate one exact-head signed work-branch Canary for PR #181;
3. perform focused device validation of top readout geometry and native charging glyph presentation;
4. change runtime only if device evidence identifies a concrete defect;
5. merge to `dev` only after required device acceptance.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. task-specific architecture / reference docs;
4. current source and exact-target SystemUI evidence;
5. relevant `DEVLOG.md` history when needed.

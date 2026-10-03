# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.5.
- `main` remains on the promoted 0.0.5 / Build 618 stable checkpoint.
- `dev` has advanced to accepted Build 619: `0.0.5` / versionCode `261002419` / Build `20261002-619`.
- Build 619 is the latest accepted runtime-affecting development baseline.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- GPL-3.0-only remains the project license.

## Integrated Build 619 scope

PR #195 added two independent network-state visual controls:
- Airplane mode size: 40%-125%, default 100%.
- No-SIM size: 40%-125%, default 100%.

Both controls are profile-scoped like Wi-Fi/mobile-type sizing, so Network centered and Battery centered remember separate values. Wi-Fi and mobile-type sizing remain independent.

Renderer ownership remains narrow:
- HyperOS/native resources remain the drawable source.
- Each new setting changes only the matching resource draw-size constraint and the same resolved geometry consumed by optical avoidance / transition-source rendering.
- No state source, slot ownership, native peer layout, gesture timeline, timing, alpha, visibility, or translation writer was added.

## Validation evidence

- PR #195 merged to `dev` as `d59b7452cfe7abcad9a48f8ddbf00812adc00da5`.
- Work Branch Canary #650 validated Build 619 on the exact requested work-branch source; trusted checkout/build/signature/non-debuggable checks succeeded.
- Maintainer device validation on Xiaomi 15 Pro accepted the Airplane / No-SIM sizing behavior and independent content-layout memory with no visible regression requiring another runtime change.
- Returned detailed diagnostics report `overall=healthy` on Build 619; the durable device conclusion and the diagnostic-summary limitation are recorded in `DEVLOG.md`.
- Final PR Runtime CI #2230 succeeded after synchronizing latest `dev` ancestry and recording device evidence.
- Trusted `dev` integration CI #2231 succeeded on merge commit `d59b7452cfe7abcad9a48f8ddbf00812adc00da5`, including tests/build, pinned HyperOS target verification, Modern Xposed metadata, Haple APK signature, non-debuggable Canary verification, and Canary artifact upload.

## Active objective

Branch: `feat/battery-fill-retract-follow` / PR #197.

Build 660 is rejected by device evidence.

The returned Build-660 video and detailed diagnostic establish two simultaneous regressions:
- HyperOS island avoidance / knife-hide is absent;
- native peers still settle horizontally at gesture entry and then mainly follow the fake carrier downward.

This matches a documented historical failure. Build 506 used a one-shot final-width occupancy cutover and was device-rejected because the native peer row jumped to its final horizontal layout at gesture entry. Build 507 corrected that class of failure by deriving reservation width from the same raw HyperOS expansion progress that drives Control Center motion.

Build 660 recreated the same structural mistake through a different property: the fixed fake-carrier capacity lease expanded the island QS_FAKE carrier to the full parent width before meaningful expansion. The diagnostic then shows an early island bucket around fraction 0.13 with the fake row already near full width, peers kept at normal island state, and stable large fake-row X offsets. This preserves peer visibility by removing the capacity pressure that HyperOS uses for island avoidance, rather than preserving native avoidance.

Build 661 therefore supersedes Build 660:
- remove the fixed fake-carrier capacity lease from island-native-layout mode;
- keep represented native Wi-Fi/mobile/Battery participants measured and laid out (no island `ignoredSlots`);
- keep the removed `getIslandTranslationX()` compensation absent;
- permit only the already-existing semantic `statusIcons.paddingEnd` reservation while a transition reservation is active, with its width driven directly by raw HyperOS expansion progress;
- no steady/base island padding is applied before transition start, and clearing the transition restores the native padding baseline;
- ordinary no-island Control Center keeps the accepted Build-611/612 fixed capacity lease unchanged;
- the Build-494 charging/Battery-Island reservation exclusion remains unchanged for ordinary layout, but is lifted only when the exact current QS_FAKE Session is already in Build-655 island-native-layout mode, because that mode intentionally removed the compact reservation Build 494 assumed.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003661` / Build `20261003-661`.
- Review must confirm there is no fixed island carrier-width lease, no island-boundary return-value hook, no represented-slot exclusion, and no independent timing curve.
- Exact-HEAD Runtime CI is required before one signed Canary.
- Device evidence is mandatory because the candidate deliberately re-tests progress-synchronous end reservation under the newer Build-655 native-layout split.

## Device gate

Validate one signed Build-661 Canary:

1. Active generic island: slow Home -> Control Center pull and return. Native peers should move horizontally with the gesture instead of settling at the first sample, while HyperOS island avoidance remains active.
2. Charging island + dual SIM: verify the second mobile presentation is neither consumed immediately nor allowed to overlap the island.
3. Verify there is no Build-611 style initial whole-row left jump.
4. Verify fake peer visibility/avoidance changes evolve with expansion rather than one-shot at entry.
5. Recheck one ordinary no-island pull and export one detailed diagnostic.

Expected diagnostic evidence:
- island sessions report `reservationMode=native-island-progress-padding`;
- `fakeCarrierWidth=-1` / no island fixed capacity lease;
- `nativeReservation` follows the progress-derived logical reservation instead of remaining `-1`;
- child island state / visibility remains HyperOS-owned.

No Keyguard/AOD validation is required for this checkpoint.

## Immediate next step

Review Build 661, run exact-HEAD Runtime CI, then one signed Canary. Freeze runtime after Canary until the focused island device gate returns.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. `docs/reference/statusbar-composition-patterns.md` Build-506/507 reservation lifecycle;
5. relevant `DEVLOG.md` history.

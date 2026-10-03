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

Build 656 is the current focused device candidate. It is a single-variable correction on top of Build 655.

Build 655 device feedback:
- Guiyuan transition remains present under island, and island mode correctly reports `reservationMode=native-island-authority` with no Guiyuan native reservation;
- however non-represented native status icons are still removed too early: the steady Home row may show network-speed / VPN / other icons, then the first pull sample removes the icons that would not fit the fully-expanded island layout;
- video evidence shows this happens before meaningful panel travel, then the remaining native row moves downward;
- Build-655 diagnostics show the QS fake `system_icon_area` is already at the island/control-center width (`left=249,width=587`) and `MiuiStatusIconContainer` at `448px` from about fraction 0.11 through 0.86. The horizontal capacity is therefore effectively terminal while only root translation/alpha continues with gesture progress;
- Build 655 proves the remaining issue is not the removed island-width hook and not the removed transition `paddingEnd` writer.

Build 656 hypothesis and correction:
- the native `combined_status` participant still intercepts `MiuiStatusBarFolmeViewState.applyToView(...)` and overwrites both `translationX` and `NewStatusIconState.layoutTranslationX` with Guiyuan's stable end-side slot anchor;
- under island, HyperOS owns collision / visibility state and may consume that Folme layout translation while deciding which status icons fit;
- forcing the Combined participant to its stable/final end-side translation can therefore make native island capacity resolve as if the participant had already reached its terminal layout on the first pull sample;
- while the existing generic island source explicitly reports `showing=true`, Build 656 bypasses Guiyuan's Combined-slot translation correction and passes HyperOS' Folme state through unchanged;
- ordinary non-island behavior keeps the existing stable-slot correction;
- the bypass is diagnostic-visible as `nativeCombinedParticipant slotTranslation authority=hyperos-island bypass=true moduleStateWrites=0`.

Build 655 native-layout-authority behavior, charging Clip/tint/target timing, target geometry, QS fake masking, and no-island transition behavior are otherwise unchanged.

No timer, delay, new progress threshold, fake-row geometry write, island-width patch, native peer alpha/visibility write, or second motion system is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003656` / Build `20261003-656`.
- Focused unit coverage locks:
  - explicit island showing => do not correct native Combined-slot Folme translation;
  - explicit no-island / unknown state => preserve the existing normal stable-slot correction;
  - existing native slot geometry and occupancy tests remain unchanged.
- Runtime diff from Build 655 is limited to the island-aware translation bypass plus one diagnostic marker.
- Lifecycle review: island state comes from the existing event-driven `SystemUiIslandMotionSource`; no new latch is introduced. Island dismissal automatically restores the previous normal correction path. Hot Reload / reset clears only the one-shot diagnostic marker.
- Exact-HEAD Runtime CI is required before Canary.
- Device evidence is mandatory because this tests whether the premature native icon cutover is caused by the project writing `layoutTranslationX`.

## Device gate

Validate one signed Build-656 Canary:

1. Any active island event, slow pull
   - steady Home native icons must remain present on first touch;
   - network-speed / VPN / other non-represented icons must not disappear immediately merely because the final island layout cannot fit them;
   - visibility/avoidance should evolve with HyperOS native gesture ownership rather than jump at gesture start;
   - diagnostic should contain `authority=hyperos-island bypass=true`.

2. Native row motion
   - native status icons should follow the stock HyperOS pull trajectory;
   - specifically, no project-induced early terminal horizontal settlement before the visible downward motion.

3. Charging-only island + dual SIM
   - preserve the expected dual-signal structure;
   - no native-icon overlap with mobile signal;
   - Guiyuan transition remains present.

4. Regression
   - ordinary no-island pull-down stays Build-652/655 behavior;
   - charging lightning Clip/tint/target reveal unchanged;
   - island dismissal, Hot Reload first pull, Keyguard/AOD unchanged.

## Immediate next step

Run exact-HEAD Runtime CI for Build 656. If green, request one signed Canary and freeze runtime for this single-variable device gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

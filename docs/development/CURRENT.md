# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.5.
- `main` remains on the promoted 0.0.5 / Build 618 stable checkpoint.
- `dev` is accepted through Build 619: `0.0.5` / versionCode `261002419` / Build `20261002-619`.
- Build 619 is the latest accepted runtime-affecting development baseline.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- GPL-3.0-only remains the project license.

## Active objective

Branch: `feat/aod-display-control` / PR #196.

Build 652 is the current Keyguard/AOD lifecycle correction candidate after Build-651 device evidence.

Build-651 device evidence:
- with Keyguard and AOD both enabled, AOD -> Keyguard and Keyguard -> Home followed by an immediate fast Control Center pull can begin fully native; holding a partial pull can recover Guiyuan later;
- with Keyguard Guiyuan disabled and AOD enabled, steady Keyguard can still show Guiyuan, Keyguard pull is native, and subsequent Home pull can also remain native;
- with AOD Guiyuan disabled and Keyguard enabled, Keyguard -> Home fast pull can remain native, while Keyguard -> AOD switches to native represented icons before the visible Keyguard scene has actually yielded.

Root cause confirmed by the returned Build-651 diagnostic and code review:
1. Control Center source arbitration made steady SceneState unconditionally override the native panel `realSystemIcons` source. The diagnostic captures native panel HOME while stale steady KEYGUARD is still selected; Keyguard readiness is false at that instant, so the projection is torn down and only reattaches after a later stable-family callback.
2. A non-animating `KEYGUARD + homePresentationOwned` shortcut could prearm the enabled AOD child even when Keyguard projection was explicitly disabled. Home ownership alone is not sufficient AOD-transition evidence.
3. During a real Keyguard/AOD animation, Build 651 immediately fell native when the destination child was disabled. Device evidence shows that crosses the visual boundary too early; the enabled outgoing child must remain the family owner until the native animation reaches its stable target.

Build 652 correction:
- a HOME/KEYGUARD Control Center source disagreement is resolved from the already-latched stable family history: KEYGUARD/AOD history means the family is outgoing so HOME wins; UNKNOWN history means stable Home is outgoing so KEYGUARD wins;
- the same effective source still drives both projection eligibility and TransitionOwner;
- remove non-animating speculative AOD prearm entirely;
- transient Keyguard ancestry may prearm AOD only while an actual AOD animation is active, the last stable family scene is UNKNOWN, Home still owns the visible source, and AOD is enabled;
- a latched AOD/Keyguard family origin cannot be overridden by stale Home ownership;
- during native family animation, if the destination child is disabled, retain the enabled outgoing child until stable-target evidence arrives; the stable disabled child still fails native at that boundary.

No timer, delay, polling, second family owner, native geometry/alpha/visibility writer, or rejected `animToAod` direction inference is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003652` / Build `20261003-652`.
- Branch remains based on current `dev` with no behind commits at the Build-651 checkpoint.
- Focused unit coverage locks:
  - unlock conflicts with latched KEYGUARD/AOD history resolve HOME;
  - lock/AOD-entry conflicts from UNKNOWN family history resolve KEYGUARD;
  - AOD prearm requires an actual animation signal plus UNKNOWN family origin;
  - stale Home ownership cannot override a latched AOD -> Keyguard handoff;
  - a disabled destination child retains only the still-enabled outgoing child during animation and becomes native at the stable disabled target.
- Exact-HEAD Runtime CI is required before Canary.
- Device evidence remains mandatory because the correction changes scene/lifecycle ownership at native visual boundaries.

## Device gate

After exact-HEAD Runtime passes, validate one signed Build-652 Canary:

1. Keyguard + AOD both ON
   - repeat AOD -> Keyguard -> immediate partial/fast Control Center pull;
   - repeat Keyguard -> Home -> immediate fast Control Center pull;
   - the first non-zero pull must already use Guiyuan and must not require holding the gesture to recover.

2. Keyguard Guiyuan OFF / AOD Guiyuan ON
   - steady Keyguard must stay native;
   - Keyguard-originated Control Center remains native;
   - Home-originated Control Center remains Guiyuan;
   - AOD -> Keyguard may retain the outgoing AOD presentation only during the native handoff, then must end native on stable Keyguard.

3. Keyguard Guiyuan ON / AOD Guiyuan OFF
   - steady Keyguard remains Guiyuan and its Home unlock first pull remains Guiyuan;
   - Keyguard -> AOD must not switch represented icons to native before the native AOD visual boundary;
   - stable AOD remains native.

4. Regression
   - both enabled: Keyguard <-> AOD continuity remains one family owner / one render View;
   - ordinary Home pull-down and Hot Reload first pull remain unchanged;
   - Home -> Keyguard/AOD immediate pull must not be misclassified as an unlock;
   - no prepared -> native cleanup -> late reattach loop while a valid source remains projected.

## Immediate next step

Run exact-HEAD Runtime CI for Build 652. If green, request one signed Canary and freeze #196 runtime for the focused device gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

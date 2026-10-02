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

Build 651 is the current Keyguard/AOD lifecycle correction candidate after Build-645 device evidence.

Build-645 device evidence:
- Keyguard + AOD both enabled: AOD -> Home and Keyguard -> Home followed immediately by a fast Control Center pull can probabilistically remain native for the entire gesture.
- Disabling Keyguard Guiyuan does not reliably keep steady Keyguard native.
- With AOD Guiyuan disabled, AOD <-> Keyguard switching can briefly expose native represented icons before Guiyuan returns; AOD -> Keyguard + immediate partial pull can start native and recover only after being held.
- The 645 diagnostic repeatedly shows Keyguard presentation reaching prepared/active and then being immediately cleaned with `keyguard-aod-native` / `cutover-projection-ineligible`, followed by a fresh attach. This matches the visible native flash.

Root cause confirmed in review:
1. Build 645 used current Keyguard/AOD presentation ownership as animation-direction evidence. That ownership is itself mutated by attach/cleanup, so repeated AOD callbacks can self-oscillate: KEYGUARD -> NATIVE -> KEYGUARD (or the reverse).
2. A disabled child therefore was not a hard visibility boundary: the shared family host could be re-entered by the other child after a callback re-evaluated mutable ownership.
3. Direct AOD -> Home unlock could lose AOD origin too early. If HOME immediately cleared family history, a later AOD-animation callback could be misread as Home -> AOD prearm.
4. After unlock, the authoritative steady scene can already be HOME while the first Control Center native callback still reports stale KEYGUARD identity from `realSystemIcons`. Letting that stale callback overwrite HOME can leave the first fast pull on the wrong transition witness.
5. Review also found the new family latch must be cleared explicitly during old-generation hot-reload teardown.

Build 651 correction:
- Replace mutable child-ownership direction inference with a latched `StableKeyguardAodScene` derived only from non-animating, runtime-observed family state.
- Freeze that latch for the entire AOD animation; attach/cleanup cannot change direction.
- Child switches are hard gates:
  - latched KEYGUARD can route only to enabled AOD, otherwise Native;
  - latched AOD can route only to enabled Keyguard, otherwise Native.
- Home -> AOD prearm remains allowed only when there is no latched Keyguard/AOD origin and Home still owns the visible source.
- AOD -> Home keeps AOD origin latched through the outgoing animation and clears it only after non-animating, non-AOD stable HOME evidence.
- Remove obsolete `keyguardPresentationOwned/aodPresentationOwned` inputs from ScenePolicy entirely.
- Once steady scene is known, it is authoritative for the first Control Center source callback. A stale KEYGUARD callback cannot overwrite verified HOME after unlock.
- The same effective source scene is fed to both Control Center eligibility and TransitionOwner so render readiness and transition witness cannot disagree on the first pull.
- Clear the stable-family latch in both normal hot-reload reset and old-generation teardown.

No timer, delay, polling, native geometry/visibility writer, second family owner, or rejected `toAod/animToAod` direction inference is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003651` / Build `20261003-651`.
- Branch remains based on current `dev` with no behind commits at the latest checkpoint.
- Focused coverage now locks:
  - animation routing from stable family latch, independent of mutable current ownership;
  - disabled Keyguard/AOD children never receive routed presentation;
  - direct AOD -> Home cannot reverse-prearm AOD;
  - Home -> AOD can still prearm from UNKNOWN family history;
  - verified HOME/KEYGUARD steady scene overrides stale first Control Center callback source;
  - existing single-enabled and Home-owned prearm rules remain covered.
- Lifecycle review additionally verified:
  - SceneUpdate source is the same MiuiBatteryMeterView class used by the AOD state source;
  - latch survives only as long as needed and is reset across hot reload generations;
  - family RenderSession remains single-writer and retargets in place.
- Exact-HEAD Runtime CI is required before Canary.
- Device evidence is mandatory because this changes scene ownership and first-pull lifecycle ordering.

## Device gate

After exact-HEAD Runtime passes, validate one signed Build-651 Canary:

1. Keyguard + AOD both ON
   - AOD -> Home -> immediate fast Control Center pull, repeated;
   - Keyguard -> Home -> immediate fast Control Center pull, repeated;
   - first non-zero pull fraction must remain Guiyuan; no full native gesture and no late recovery jump.

2. Keyguard Guiyuan OFF / AOD Guiyuan ON
   - steady Keyguard must always remain native;
   - AOD may show Guiyuan only in real AOD / valid Home -> AOD prearm;
   - AOD -> Keyguard must not let AOD child leak into steady Keyguard.

3. Keyguard Guiyuan ON / AOD Guiyuan OFF
   - stable AOD remains native;
   - Keyguard remains Guiyuan;
   - AOD <-> Keyguard transition must not flash native and then reattach Guiyuan;
   - AOD -> Keyguard + immediate partial pull must start with Guiyuan transition and must not need a pause to recover.

4. Regression
   - both ON: AOD <-> Keyguard continuity;
   - ordinary Home pull-down;
   - hot reload first pull;
   - no repeated prepared -> cleanup -> fresh attach loop in detailed diagnostics.

## Immediate next step

Run exact-HEAD Runtime CI for Build 651. If green, request one signed Canary and freeze #196 runtime.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

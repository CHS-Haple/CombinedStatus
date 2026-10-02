# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.5.
- `main` remains on the promoted 0.0.5 / Build 618 stable checkpoint.
- `dev` is accepted through Build 619: `0.0.5` / versionCode `261002419` / Build `20261002-619`.
- Build 619 is the latest accepted runtime-affecting development baseline.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- GPL-3.0-or-later remains the project license.

## Active objective

Branch: `feat/aod-display-control` / PR #196.

Build 620 is the first opt-in AOD projection candidate:
- dedicated AOD display switch, default off;
- global Guiyuan enable remains the parent runtime gate;
- Keyguard and AOD child preferences persist independently and never rewrite one another;
- stable AOD only may acquire Guiyuan presentation; AOD enter/exit animation remains native HyperOS;
- Keyguard and AOD use mutually exclusive render/presentation sessions over the verified Keyguard-family host contract;
- AOD never supplies the Keyguard -> Control Center transition source;
- disabling the global switch or AOD child switch releases AOD presentation and restores native represented slots.

The candidate does not add a second AOD animation timeline, timer, polling source, native alpha/translation writer, or separate platform-state model. Unsupported or ambiguous AOD authority/host/lifecycle fails native for Keyguard/AOD while leaving accepted Home behavior intact.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261002420` / Build `20261002-620`.
- Existing exact-target AOD authority remains `MiuiBatteryMeterView.setIsAodAnimate(boolean)` + `toggleAodMode(boolean)` with `mToAod` / `mIsAodAnimate`.
- Candidate stable-AOD boundary is `mToAod == true && mIsAodAnimate == false`; all AOD transition states stay native.
- Ready PR Runtime CI #2240 succeeded on runtime head `8e349e8a3b7a1731858ee94227007fbde133dcd0`.
- Signed Work Branch Canary #651 independently resolved, checked out, tested, signed, verified non-debuggable, and uploaded Build 620 from that same runtime head.
- Runtime is frozen at this checkpoint until focused device evidence returns because AOD scene ownership and native presentation suppression change.

## Device gate

Validate the four preference combinations and parent gate:
- Keyguard on / AOD off: Keyguard uses Guiyuan; AOD remains native.
- Keyguard off / AOD on: Keyguard remains native; stable AOD uses Guiyuan.
- Keyguard on / AOD on: Keyguard and stable AOD each use Guiyuan in their own scene, with native AOD enter/exit animation between them.
- Global Guiyuan off with either/both child switches still enabled: Home, Keyguard, and AOD all restore native immediately while child preference values remain stored.

Also verify AOD enter/exit does not show duplicate native represented icons, stale slot suppression, a visible snap at the stable cutover, or leakage into Control Center.

## Immediate next step

Await focused Build 620 device evidence. Do not change runtime or merge PR #196 before that evidence is reviewed.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

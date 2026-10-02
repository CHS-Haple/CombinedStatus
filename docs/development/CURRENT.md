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

Build 621 device evidence rejects the direction-guessing continuity rule. The exact-target trace shows AOD transitions can report combinations such as `toAod=true`, `isAodAnimate=true`, `animToAod=false`, followed later by `toggleAodMode(false)`. The Build-621 policy used `toAod` to decide which Guiyuan scene should remain eligible, so it could drop the actually owned AOD/Keyguard presentation and restore native represented icons during the transition.

Build 623 removes direction guessing:
- the global Guiyuan switch remains the parent runtime gate and Keyguard/AOD child preferences remain independently persisted;
- a native steady-scene callback records the currently visible Home/Keyguard source before Keyguard/AOD routing;
- AOD animation routing uses actual presentation ownership, not `toAod` or diagnostic-only `mAnimToAod`;
- an already-owned AOD presentation remains owned through the native AOD animation while the AOD feature remains enabled;
- a visible Keyguard presentation remains owned through Keyguard -> AOD animation only when the AOD feature is enabled;
- Home -> AOD may prearm the AOD render/presentation session during the native animation when Home is the actual source and AOD is enabled;
- AOD -> Home defers AOD cleanup until the native AOD state boundary instead of dropping it as soon as Home is observed;
- stable Home, Keyguard and AOD ownership is still mutually resolved, and AOD remains ineligible as a Control Center source;
- HyperOS continues to own AOD animation timing, alpha, visibility and translation. Guiyuan adds no timer, delay, polling loop or duplicate animator.

Diagnostics copy remains: `设备型号（设备代号）`, `Android 版本（API 等级）`, `HyperOS 版本`, `SystemUI 版本`, and compatibility baseline `SystemUI 17.03.260226.r（HyperOS 4）`.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261002423` / Build `20261002-623`.
- Build 621 steady Keyguard/AOD projection works, but device video/log evidence still shows native represented icons flashing during Home <-> AOD and Keyguard <-> AOD handoff.
- The retained trace shows transition-time readiness loss and presentation cleanup on the outgoing owner; this invalidates Build-621 `toAod`-direction routing.
- Build 623 Runtime CI #2257 succeeded on exact head `393a0e659564239f31aa8f935c57758d137331a7`.
- Runtime is frozen pending one signed Work Branch Canary and focused device evidence.

## Device gate

Validate Build 623 with emphasis on:
- repeated Home -> AOD -> Home switching: no native Wi-Fi/mobile/battery represented-icon flash;
- repeated Keyguard -> AOD -> Keyguard switching with both child switches enabled: no native represented-icon flash or blank interval;
- AOD on / Keyguard off and Keyguard on / AOD off retain independent child behavior;
- no duplicate Guiyuan/native set, stuck outgoing frame, endpoint snap, or AOD leakage into Control Center;
- global Guiyuan off immediately restores Home, Keyguard and AOD native presentation while preserving both child preference values.

## Immediate next step

Build one signed Work Branch Canary from the reviewed Build 623 head, then keep runtime frozen until focused device evidence returns. Do not merge PR #196 before that evidence is reviewed.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

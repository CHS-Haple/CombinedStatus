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

Build 623 device evidence rejects the remaining restore/reacquire handoff. Direction inference was already removed, but Keyguard and AOD still owned separate presentation/render sessions. At a scene boundary the outgoing session restored its ignored-slot delta, clip masks and end reservation before the target session reached compact-layout readiness; the target then stayed `prepared` with native fallback until the next native status-icon layout. That deliberate native interval is the observed flash in both Home <-> AOD and Keyguard <-> AOD.

Build 625 changes the ownership boundary instead of adding timing:
- Keyguard and AOD share one Keyguard-family presentation Session on the verified native host; same-host scene changes retarget that owner instead of stop/restore/reacquire;
- Keyguard and AOD share one render Session / one module RenderView; same-host scene changes retarget scene semantics without detach/re-add;
- role-specific cleanup is guarded by the currently active family surface, so cleanup from the outgoing role cannot tear down a successfully retargeted target;
- prepared presentation claim and compact-layout readiness are separate facts: routing can retain an already-acquired presentation claim while renderer cutover still waits for verified native layout;
- Home -> AOD may apply the existing reversible represented-view mask during explicit AOD prearm so raw native represented icons are not exposed while the AOD host waits for compact layout; this does not mark layout ready early or create a second layout owner;
- AOD child alpha follows native Battery alpha only while the family scene is AOD and resets to 1 when retargeted to Keyguard;
- Keyguard/AOD child preferences remain independent and the global Guiyuan switch remains their parent gate;
- AOD remains ineligible as a Control Center source;
- HyperOS remains the only native AOD motion/timing/translation/visibility owner. Build 625 adds no timer, delay, polling loop or duplicate animator.

Diagnostics copy remains: `设备型号（设备代号）`, `Android 版本（API 等级）`, `HyperOS 版本`, `SystemUI 版本`, and compatibility baseline `SystemUI 17.03.260226.r（HyperOS 4）`.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003625` / Build `20261003-625`.
- Build 623 is device-rejected for native represented-icon flashing in Home <-> AOD and Keyguard <-> AOD.
- The retained Build-623 trace shows the structural gap: outgoing presentation cleanup restores native state, then the target reports `prepared` / `native-until-native-layout`, and only the next native layout reaches `active`.
- Build 625 removes that same-host restore/reacquire cycle and separates presentation claim from compact readiness.
- Runtime CI #2278 succeeded on exact runtime head `5aa8752197ce8328496d3ca68c8ee5875e98ef91`.
- Final ownership/writer/lifecycle review found one Keyguard-family presentation owner, one Keyguard-family RenderView, no stale Keyguard/AOD dual-session state, no new delay/timer/polling path, and no new native translation/visibility writer.
- Runtime is frozen at the reviewed Build-625 code pending one signed Work Branch Canary and focused device evidence.

## Device gate

Validate Build 625 with emphasis on:
- repeated Home -> AOD -> Home switching: no native Wi-Fi/mobile/battery represented-icon flash, blank interval or duplicate set;
- repeated Keyguard -> AOD -> Keyguard switching with both child switches enabled: no native flash, blank interval, duplicate set or endpoint snap;
- AOD on / Keyguard off and Keyguard on / AOD off preserve independent child behavior;
- global Guiyuan off immediately restores native Home/Keyguard/AOD presentation while preserving both child preference values;
- no stuck outgoing frame, stale AOD alpha on Keyguard, or AOD leakage into Control Center.

## Immediate next step

Produce one signed Work Branch Canary from the reviewed Build-625 runtime checkpoint plus documentation-only closure. Keep runtime frozen until focused device evidence returns; do not merge PR #196 before that evidence is reviewed.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

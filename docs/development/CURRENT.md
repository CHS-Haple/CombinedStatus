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

Build 620 established the first opt-in stable-AOD projection, but focused device evidence rejected its transition continuity when both Keyguard and AOD display were enabled. The implementation intentionally treated every `isAodAnimate=true` interval as native-only, so it released the outgoing Guiyuan presentation before the destination scene became stable. That created a visible temporary return to native status presentation during Keyguard <-> AOD switching.

Build 621 is the narrow continuity correction:
- the global Guiyuan switch remains the parent runtime gate;
- Keyguard and AOD child preferences remain independently persisted and default behavior is unchanged;
- when **both** child switches are enabled, the currently owned Guiyuan scene is retained across the native AOD animation interval and hands off only at the stable destination boundary;
- entering AOD retains Keyguard Guiyuan while `toAod=true && isAodAnimate=true`;
- exiting AOD retains AOD Guiyuan while `toAod=false && isAodAnimate=true`;
- if either destination/source child switch is disabled, transition presentation remains native as before;
- HyperOS still owns AOD timing, alpha/visibility/translation lifecycle and animation; Guiyuan adds no timer, delay, polling source, or second animator;
- AOD remains ineligible as a Keyguard -> Control Center transition source.

Diagnostics copy is also clarified without runtime behavior changes: `设备型号（设备代号）`, `Android 版本（API 等级）`, `HyperOS 版本`, `SystemUI 版本`, and compatibility baseline `SystemUI 17.03.260226.r（HyperOS 4）`.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261002421` / Build `20261002-621`.
- Build 620 device evidence confirms AOD and Keyguard steady projection both work, but repeated transitions visibly expose the intentional native-only gap.
- The matching diagnostic path shows `aodRenderReadiness ownerReady=false` followed by `aodPresentation cleanup/inactive` as soon as `setIsAodAnimate` reports an AOD transition; this is the direct cause of the temporary native restoration.
- Build 621 centralizes dual-enabled transition continuity in the existing scene policy/render-session eligibility rather than adding another lifecycle source.
- Ready PR Runtime CI #2245 succeeded on runtime head `3c2b24a73bf1866d85a30135cf03001abb1bf631`.
- A signed Work Branch Canary is required because the correction changes live Keyguard/AOD presentation lifetime.

## Device gate

Validate Build 621 with emphasis on:
- Keyguard on / AOD on: repeated Keyguard -> AOD -> Keyguard switching must no longer temporarily restore native represented icons;
- transition motion/alpha should remain visually coherent with HyperOS, with no stuck Guiyuan frame, double icon set, or endpoint snap;
- Keyguard on / AOD off and Keyguard off / AOD on must retain their previous native-transition behavior;
- global Guiyuan off must immediately restore Home, Keyguard and AOD native presentation while preserving both child preference values;
- AOD must not leak into Control Center source ownership.

## Immediate next step

Build one signed Work Branch Canary from the reviewed Build 621 head, then freeze runtime until focused device evidence returns. Do not merge PR #196 before that evidence is reviewed.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

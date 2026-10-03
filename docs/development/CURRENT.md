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

Build 662 is the current Keyguard/AOD lifecycle candidate after Build-660 device validation.

Build-660 device result:
- Keyguard OFF / AOD ON: Keyguard <-> AOD timing is accepted as perfect; this path is a non-regression baseline.
- Keyguard ON / AOD OFF: Keyguard -> AOD remains responsive, but AOD -> Keyguard is visibly late.
- The AOD -> Keyguard diagnostic proves the native `animateIconContainer(true)` boundary is observed, the Keyguard renderer becomes layout-ready immediately, then the synchronous readiness callback re-enters ordinary pending policy and stops that renderer. The stable Keyguard edge about 0.38 s later recreates it; the delay is self-cancellation, not native-boundary timing or layout cost.
- Dual-enabled Home -> AOD still contains a real composed-owner gap: frame-by-frame video shows Guiyuan disappear and reappear rather than only inheriting the native whole-screen flash.
- At that Home -> AOD window, `homePresentationOwnedAtStart=true` but `homeOriginLatched=false`. The old latch still requires `lastStableFamily=UNKNOWN`, so stale family history can veto an otherwise current Home origin.
- AOD -> Keyguard also demonstrates why Home ownership alone cannot define origin: HyperOS can transiently report Home / retain the Home presentation while the native AOD state is still stable AOD.

Build-662 candidate:
- preserve the native status-icon visual-boundary flag across the synchronous Keyguard renderer/readiness/cutover call stack, then clear it immediately after that native callback returns. This lets the boundary consume the pending target once instead of being invalidated by its own re-entrant readiness callback;
- qualify the Home full-AOD origin with current native Keyguard/AOD state: Home source + Home represented-slot ownership + native `toAod=false` + `isAodAnimate=false`. Stable/animating AOD therefore cannot be mistaken for Home even if ancestry or Home ownership lingers;
- once that explicit Home witness is latched, stale `lastStableFamily` no longer vetoes it. Direction still comes only from native `mToLockScreen`;
- keep the Build-660 accepted Keyguard-OFF/AOD-ON path, target-matched pending lifetime, and existing AOD pre-mask/compact-layout owner unchanged.

No timer, delay, polling, copied native duration/interpolator, native alpha/visibility/translation writer, geometry patch, or second family owner is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003662` / Build `20261003-662`.
- PR #196 remains based on the Build-660 lifecycle branch and is 0 behind `dev` before Build-662 authoring.
- Focused tests cover native-state-qualified Home origin, stale family-history override only after that explicit latch, and the existing target-matched pending endpoint contract.
- Exact-HEAD Runtime CI is required before Canary.
- Real-device validation is mandatory because both corrections depend on exact-target native lifecycle ordering.

## Device gate

1. Keyguard OFF / AOD ON
   - Keyguard <-> AOD must remain identical to the accepted Build-660 timing.

2. Keyguard ON / AOD OFF
   - AOD -> Keyguard must acquire Guiyuan at the native status-icon boundary without the previous ~0.38 s stop/re-attach gap.
   - Keyguard -> AOD must remain as responsive as Build 660.
   - Home -> AOD must remain native once the target is AOD; transient Keyguard ancestry must not reacquire Guiyuan.

3. Both ON
   - Home -> AOD must no longer contain a Guiyuan disappear/reappear interval.
   - Keyguard <-> AOD remains one continuous family owner.

4. Regression
   - no attach -> stop -> attach oscillation around one AOD -> Keyguard boundary;
   - no stable-AOD misclassification as Home;
   - AOD/Keyguard -> Home and immediate Control Center remain unchanged.

## Immediate next step

Review Build 662 against Build 660, run exact-HEAD Runtime, then issue one signed Canary for the focused three-mode lifecycle gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. `SystemUI-Reference` exact-target findings and task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

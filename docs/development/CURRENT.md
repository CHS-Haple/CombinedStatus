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

Build 669 follows Build-668 device rejection. Build 667 remains accepted for the previously failing Keyguard/AOD paths:
- AOD -> Keyguard no longer collapses adjacent peers inward;
- AOD -> Keyguard fast/partial Control Center pull no longer falls back to native in the reproduced path;
- Keyguard -> AOD keeps Guiyuan through the native Keyguard status-icon fade and yields only at the hidden endpoint.

Build-668 device result:
- with Keyguard Guiyuan enabled and AOD Guiyuan disabled, Home/Desktop -> AOD still commonly shows a brief Guiyuan interruption, then Guiyuan again, then finally native;
- detailed diagnostics show the direct screen-off path enters a transient Full-AOD `target=keyguard`, then native `toAod=true / isAodAnimate=true` follows before a stable Keyguard endpoint;
- the tested Build 668 never latched `homeNativeAodFallbackCandidate`, so the transient Keyguard renderer remained eligible and could reappear before native takeover.

Build-669 lifecycle correction:
- arm a Home-native-AOD candidate at Full-AOD entry only while the Home compact owner still owns represented slots **and the exact native Home `system_icons` carrier is still visibly presented**; Keyguard projection must be enabled and AOD projection disabled. This intentionally avoids stale `steadyStatusSourceScene` and does not reuse Battery `mStatusBarState` as Home visibility authority;
- the intermediate `target=keyguard` is explicitly treated as a transient routing stage and does not consume the candidate;
- native `toAod=true / isAodAnimate=true` consumes the candidate, promotes a native-AOD fallback, clears any incoming-Keyguard handoff state, releases the transient Keyguard presentation, and keeps native authoritative until stable AOD;
- a direct native target=AOD may consume the same candidate immediately;
- if stable Keyguard forms first, the candidate/active fallback is cleared so ordinary Keyguard -> AOD keeps Build-667 behavior; returning Home clears only an already-active native fallback, while an inert Home-origin candidate may survive transient routing until the next native AOD animation or stable-family endpoint;
- resolver failure, settings changes, Hot Reload and full teardown remain fail-native / fail-closed.

AOD -> Keyguard Control Center risk review:
- Build 667 fixed the observed fast-pull failure with incoming-boundary presentation readiness;
- Build 669 also closes the remaining callback-order race: if expansion fraction arrives before visible/source reconciliation, an already-valid incoming Keyguard presentation promotes CC source to KEYGUARD before lease acquisition;
- visible/source disagreement also prefers KEYGUARD only while the same incoming-ready fact is true and at least one native source witness explicitly reports KEYGUARD;
- ordinary unlock cannot use this guard because incoming-boundary readiness is absent.

No timer, delay, copied duration/interpolator, native alpha/visibility/translation writer, peer-motion writer, geometry compensation, or second presentation owner is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003669` / Build `20261003-669`.
- PR #196 is 0 behind `dev` at freeze.
- Lifecycle review completed before freeze for Home native-carrier visibility -> transient Keyguard -> native AOD, abort-to-Home, stable-Keyguard fallback, stable-AOD completion, and AOD -> incoming Keyguard -> Control Center source ordering. Build-668 detailed diagnostics specifically rejected `steadyStatusSourceScene` as the Home-origin authority because raw unlocked updates can arrive from a structurally non-Home battery while the visible Home carrier still owns the presentation.
- Unit coverage includes candidate arming, native-AOD animation consumption, direct target=AOD consumption, active-fallback projection override, incoming Keyguard source conflict, and ordinary-unlock rejection.
- Runtime code is frozen pending exact-head Runtime CI and one signed Canary.

## Device gate

1. Keyguard ON / AOD OFF — Home/Desktop -> AOD:
   - native/system flash may remain;
   - transient Keyguard Guiyuan may not reappear after native AOD animation begins;
   - expected sequence is one continuous handoff to native, with no “Guiyuan disappears -> Guiyuan returns -> native” cycle.

2. Keyguard ON / AOD OFF — ordinary Keyguard -> AOD:
   - preserve Build-667 behavior: Guiyuan stays until native Keyguard status-icons reach their hidden endpoint.

3. AOD -> Keyguard, immediate/fast/partial pull:
   - no transient native status row / native QS fake even if fraction arrives before visible/source callback;
   - holding or aborting the partial pull remains combined.

4. AOD -> Keyguard normal path:
   - preserve no-peer-merge fix.

## Immediate next step

Run exact-head Runtime CI for Build 669. If clean, issue one signed Canary and freeze for the focused device gate above.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. `SystemUI-Reference` exact-target findings and task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

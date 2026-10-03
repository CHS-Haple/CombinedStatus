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

Build 672 follows Build-669 device rejection. Build 667 remains accepted for the previously failing Keyguard/AOD paths:
- AOD -> Keyguard no longer collapses adjacent peers inward;
- AOD -> Keyguard fast/partial Control Center pull no longer falls back to native in the reproduced path;
- Keyguard -> AOD keeps Guiyuan through the native Keyguard status-icon fade and yields only at the hidden endpoint.

Build-669 device result:
- with Keyguard Guiyuan enabled and AOD Guiyuan disabled, Home/Desktop -> AOD still commonly shows “Guiyuan disappears -> Guiyuan returns -> native”;
- unlike Build 668, Build 669 now proves the Home provenance path itself is correct: `homeCarrierVisibleAtStart=true`, `homeNativeAodFallbackCandidate=true`, then native `toAod=true / isAodAnimate=true` consumes it and logs `homeNativeAodFallbackActive=true` while releasing the transient Keyguard presentation;
- the remaining flash occurs afterward when HyperOS emits a Keyguard-directed status-icon visual boundary. The incoming Keyguard visual-handoff helper can arm and attach its renderer even while the native-AOD fallback is already active.

Build-670 lifecycle correction:
- keep the Build-669 Home provenance and consumption path unchanged;
- make active Home-native-AOD fallback an explicit veto for incoming Keyguard visual-handoff eligibility;
- apply the same veto to Keyguard boundary layout precommit so a hidden native status-icon layer cannot bypass the fallback through a layout-only path;
- preserve normal AOD -> Keyguard handoff when no Home-native-AOD fallback is active;
- preserve existing stable-family cleanup, Home abort, settings/host failure, Hot Reload and teardown behavior.
AOD -> Keyguard Control Center risk review:
- Build 667 fixed the observed fast-pull failure with incoming-boundary presentation readiness;
- Build 669 also closes the remaining callback-order race: if expansion fraction arrives before visible/source reconciliation, an already-valid incoming Keyguard presentation promotes CC source to KEYGUARD before lease acquisition;
- visible/source disagreement also prefers KEYGUARD only while the same incoming-ready fact is true and at least one native source witness explicitly reports KEYGUARD;
- ordinary unlock cannot use this guard because incoming-boundary readiness is absent.

No timer, delay, copied duration/interpolator, native alpha/visibility/translation writer, peer-motion writer, geometry compensation, or second presentation owner is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003672` / Build `20261003-672`.
- PR #196 is 0 behind `dev` at freeze.
- Build-669 device evidence confirms Home native-carrier provenance and fallback consumption are now correct; the remaining defect is a visual-handoff re-entry path that bypassed the active native fallback. Build 672 changes only that eligibility boundary and does not add a writer or a second lifecycle authority.
- Unit coverage includes candidate arming, native-AOD animation consumption, direct target=AOD consumption, active-fallback projection override, active-fallback visual-handoff/precommit rejection, incoming Keyguard source conflict, and ordinary-unlock rejection.
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

Run exact-head Runtime CI for Build 672. If clean, issue one signed Canary and freeze for the focused device gate above.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. `SystemUI-Reference` exact-target findings and task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

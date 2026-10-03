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

Build 654 is the current Keyguard/AOD lifecycle candidate after Build-653 device evidence.

Build-653 device evidence:
- dual-enabled: Home -> AOD can flash once; AOD may briefly show native; steady Keyguard regressed to fully native; AOD/Keyguard -> Home immediate fast Control Center pull can become native and remain native;
- Keyguard OFF / AOD ON: same steady/first-pull regressions remain;
- Keyguard ON / AOD OFF: AOD <-> Keyguard can briefly flash Guiyuan even though the target child is disabled, then return native.

The Build-653 diagnostic separates two root causes:
1. the visible-host steady-scene gate is invalid for HyperOS Keyguard ownership. `isShown` is an animation/lifecycle visual fact, not a steady scene authority; using it rejected valid Keyguard ownership and caused the fully-native lockscreen regression;
2. the immediate Home Control Center failure occurs after HyperOS already reports `sourceScene=HOME`. The actual fail-native reason is `fake-carrier-width-writer-conflict`: a QS_FAKE capacity lease from the previous visible Control Center cycle survived the native close boundary, while HyperOS had already restored the carrier width.

Build-654 correction:
- restore Build-652 structural + status-bar-state steady scene authority; do not use `isShown` for steady scene ownership or Keyguard host selection;
- keep the Build-652 direction-aware HOME/KEYGUARD Control Center source arbitration unchanged; Build-653 evidence already reaches effective HOME before the failure, so source routing is not reopened;
- keep the QS_FAKE fake-root/presentation owner, represented-slot masks and compact readiness prearmed across pulls, but end only its carrier-width capacity lease on the true `requestedVisible: true -> false` boundary; hidden-state reservation sync is deferred until the next visible cycle reacquires from the current native baseline;
- single-child Keyguard/AOD handoff observes only the native Keyguard status-icons layer alpha/visibility; Battery AOD alpha is independent and no longer extends whole-scene ownership;
- dual-enabled family ownership remains the existing one-session retarget path.

No timer, delay, polling, copied native animation, or native alpha/visibility/translation writer is introduced.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003654` / Build `20261003-654`.
- Build 654 review commit has passed the final ownership/diff review against the Build-653 head; exact-head Runtime remains the next gate.
- Focused unit coverage locks the existing direction-aware Control Center source arbitration, visible-cycle capacity-lease release, single-child status-icons cutover and existing Home->AOD prearm ordering.
- Exact-HEAD Runtime CI is required before Canary.
- Device validation remains mandatory because the changes affect QS_FAKE mutable presentation lifetime and Keyguard/AOD visual cutover.

## Device gate

After Runtime passes, validate one signed Build-654 Canary:

1. Keyguard + AOD both ON
   - steady Keyguard must be Guiyuan again;
   - Home -> AOD may follow native screen flash, but must not expose an extra native represented-icon interval caused by Guiyuan ownership churn;
   - AOD -> Home and Keyguard -> Home immediate fast/partial Control Center pull must be Guiyuan from the first usable frame and remain Guiyuan while held.

2. Keyguard OFF / AOD ON
   - steady Keyguard and Keyguard-originated Control Center remain native;
   - AOD stays Guiyuan;
   - AOD -> Keyguard must not flash Guiyuan after native Keyguard status-icons have started taking over;
   - Home Control Center remains Guiyuan.

3. Keyguard ON / AOD OFF
   - steady Keyguard stays Guiyuan and stable AOD stays native;
   - AOD <-> Keyguard must not briefly attach the disabled child;
   - Keyguard -> Home immediate Control Center pull remains Guiyuan.

4. Regression
   - ordinary repeated Home pull-down / close / pull-down;
   - dual-enabled Keyguard <-> AOD one-family continuity;
   - Hot Reload first pull;
   - no `fake-carrier-width-writer-conflict` during a normal repeated Control Center cycle;
   - no repeated family presentation attach -> cleanup -> attach loop at one visual boundary.

## Immediate next step

Build 654 is code-reviewed. Run exact-HEAD Runtime on #196, then generate one signed Canary only if Runtime is green.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact device diagnostics;
4. `SystemUI-Reference` exact-target findings and task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.

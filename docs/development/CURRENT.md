# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version candidate: Guiyuan 0.0.4.
- `dev` integrates the accepted Build 612 product/runtime line through squash commit `359db16fb3c9cd86d484766a2647093672c70f2b`.
- `main` remains on the prior stable baseline until the 0.0.4 promotion completes.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- Build 612 / `20261002-612` is the latest device-accepted runtime checkpoint; maintainer reports no anomaly.
- GPL-3.0-or-later is the project license; third-party components retain their upstream licenses.

## Current objective

Advance the integrated Build 612 development state to display version 0.0.4, validate the version/build metadata checkpoint, promote the exact accepted state from `dev` to `main`, then perform repository-wide consistency review.

Version checkpoint:
- `combinedStatus.versionName=0.0.4`
- `combinedStatus.versionCode=261002413`
- `combinedStatus.buildId=20261002-613`
- Build 613 is a version/documentation checkpoint only; it does not change Build 612 Runtime/SystemUI behavior.

## Current transition contract

- HyperOS remains the sole expansion / appearance timeline authority.
- Home and Keyguard bridge only through the verified QS_FAKE interval; fully expanded Control Center remains native-owned.
- Notification Shade and AOD remain native-only on the pinned target.
- Guiyuan does not write native peer translation, alpha, visibility, visibleState, or a second gesture animator.
- `statusIcons.paddingEnd` remains the only progress-driven peer-layout property.
- QS_FAKE may acquire one fixed, reversible session capacity lease only from verified unused end-anchored parent capacity.
- Lease-only capacity is measurement-only and is excluded from transition motion by the Build 612 logical-carrier projection.
- Carrier-width conflicts fail native instead of racing HyperOS.
- Mobile exact four-bar geometry remains shape-local; composite/dual-row/unknown topology uses conservative fallback.
- Latent participant reveal remains spatial/reservation-gated without timer or delayed animation ownership.
- Home and Keyguard retain independent host/session ownership and cleanup.
- Build-channel diagnostics are observation-only; Release and Canary share functional control flow.

## Validation state

Confirmed:
- PR #181 was squash-merged into `dev` as `359db16fb3c9cd86d484766a2647093672c70f2b`.
- Build 612 exact-head Runtime validation passed before integration.
- Build 612 focused maintainer device validation passed with no reported anomaly.
- The 0.0.4 bump changes only build/display metadata and public/current documentation; no new device gate is required.

Pending:
- automated validation of the 0.0.4 version checkpoint;
- merge the version checkpoint into `dev`;
- stable `dev -> main` promotion;
- post-promotion repository consistency audit.

## Non-negotiable boundaries

- Root-cause first; no screenshot-fitted geometry/timing compensation.
- One mutable runtime property has one writer.
- Cleanup / Hot Reload restores only Guiyuan-owned state.
- No polling, delayed lifecycle repair, duplicate gesture animator, or high-frequency diagnostics.
- Compatibility uncertainty fails native.
- HyperOS/MIUIX/native resources, state, layout semantics, and motion remain preferred over project-local imitation.
- Do not revive rejected Battery-Island `batteryWidthDiff`, peer-`forceAppear`, or generic-island proxy routes without new exact-target evidence.

## Immediate next step

1. validate the 0.0.4 / Build 613 metadata checkpoint;
2. merge it into `dev`;
3. promote the resulting exact `dev` state to `main`;
4. verify long-lived `dev` still exists and matches promoted `main`;
5. audit repository version, license, branch, CI, documentation, and stale work-line consistency.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. task-specific architecture / reference docs;
4. current source and exact-target SystemUI evidence;
5. relevant `DEVLOG.md` history when needed.

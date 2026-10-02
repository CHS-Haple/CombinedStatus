# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.4.
- `dev` integrates the accepted Build 612 product/runtime line and the Build 613 version checkpoint.
- Version identity: `0.0.4` / versionCode `261002413` / Build `20261002-613`.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- Build 612 is the latest runtime-affecting checkpoint and is maintainer device-accepted with no reported anomaly.
- Build 613 changes only version/build metadata and documentation; Runtime/SystemUI behavior remains Build 612.
- GPL-3.0-or-later is the project license; third-party components retain their upstream licenses.

## Current objective

Promote the exact validated 0.0.4 / Build 613 `dev` state to `main`, then perform repository-wide consistency review.

Promotion evidence:
- PR #181 integrated the accepted product/runtime line into `dev` as `359db16fb3c9cd86d484766a2647093672c70f2b`.
- PR #190 integrated the 0.0.4 / Build 613 version checkpoint into `dev` as `6859f8d549022e929b224d33c2de2dae6346de04`.
- Build 612 exact-head Runtime validation passed before integration.
- Maintainer device validation accepted Build 612 with no reported anomaly.
- 0.0.4 PR Full validation passed.
- `dev` integration Run #2205 passed target-profile checks, unit/build validation, Modern Xposed metadata, Haple APK signature verification, Canary non-debuggable validation, and artifact upload.

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
- 0.0.4 public/version metadata is internally aligned.
- Build 613 `dev` integration validation is green.
- No new runtime delta exists after the accepted Build 612 device checkpoint.
- No additional device gate is required for stable promotion.

Pending:
- Full validation of the exact `dev -> main` promotion head;
- merge to `main`;
- verify/recreate long-lived `dev` at the promoted `main` commit as required;
- final repository consistency audit.

## Non-negotiable boundaries

- Root-cause first; no screenshot-fitted geometry/timing compensation.
- One mutable runtime property has one writer.
- Cleanup / Hot Reload restores only Guiyuan-owned state.
- No polling, delayed lifecycle repair, duplicate gesture animator, or high-frequency diagnostics.
- Compatibility uncertainty fails native.
- HyperOS/MIUIX/native resources, state, layout semantics, and motion remain preferred over project-local imitation.
- Do not revive rejected Battery-Island `batteryWidthDiff`, peer-`forceAppear`, or generic-island proxy routes without new exact-target evidence.

## Immediate next step

1. run the exact `dev -> main` stable-boundary Full validation;
2. merge the validated 0.0.4 / Build 613 state to `main`;
3. ensure `dev` exists and matches the promoted `main`;
4. audit version, license, branch, CI, documentation, package/Xposed identity, and stale work-line consistency.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. task-specific architecture / reference docs;
4. current source and exact-target SystemUI evidence;
5. relevant `DEVLOG.md` history when needed.

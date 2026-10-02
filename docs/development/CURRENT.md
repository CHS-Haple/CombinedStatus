# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.4.
- `main` and `dev` are synchronized on the promoted 0.0.4 / Build 613 development baseline.
- Version identity: `0.0.4` / versionCode `261002413` / Build `20261002-613`.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- Build 612 is the latest runtime-affecting checkpoint and is maintainer device-accepted with no reported anomaly.
- Build 613 changes only version/build metadata and documentation; Runtime/SystemUI behavior remains Build 612.
- GPL-3.0-or-later is the project license; third-party components retain their upstream licenses.

## Current state

The 0.0.4 promotion is complete.

Promotion / validation evidence:
- PR #181 integrated the accepted product/runtime line into `dev`.
- PR #190 advanced the development line to 0.0.4 / Build 613.
- PR #191 promoted the exact validated `dev` state to `main`.
- Build 612 exact-head Runtime validation and focused maintainer device validation passed.
- 0.0.4 version PR Full validation passed.
- `dev` integration Run #2205 passed target-profile, unit/build, Modern Xposed metadata, Haple signature, Canary non-debuggable, and artifact checks.
- stable-boundary PR Run #2207 passed.
- promoted `main` Run #2208 and recreated `dev` Run #2209 both passed the trusted signed Full path.
- repository consistency review aligned current version/public docs, GPL metadata, package/Xposed identity, MIUIX notice metadata, and Phase-4 roadmap state.

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
- `gradle.properties`, README, CHANGELOG and CURRENT agree on 0.0.4 / Build 613.
- public/contributor license metadata agrees on GPL-3.0-or-later while dependency notices retain upstream licenses.
- Android namespace/applicationId is `com.chaners.guiyuan`.
- Modern Xposed metadata remains API 102, static scope `com.android.systemui`, one Java entry, and Hot Reload enabled.
- MIUIX dependency notice now matches the pinned project snapshot/revision.
- ROADMAP no longer lists delivered Phase-4 color/weight controls as future work.
- no new runtime delta exists after the accepted Build 612 device checkpoint.

Independent maintenance remains:
- Dependabot PRs #157, #182 and #183 remain separate build/dependency proposals and are not part of the 0.0.4 baseline.
- obsolete historical branch cleanup is repository hygiene only and does not block development.

## Non-negotiable boundaries

- Root-cause first; no screenshot-fitted geometry/timing compensation.
- One mutable runtime property has one writer.
- Cleanup / Hot Reload restores only Guiyuan-owned state.
- No polling, delayed lifecycle repair, duplicate gesture animator, or high-frequency diagnostics.
- Compatibility uncertainty fails native.
- HyperOS/MIUIX/native resources, state, layout semantics, and motion remain preferred over project-local imitation.
- Do not revive rejected Battery-Island `batteryWidthDiff`, peer-`forceAppear`, or generic-island proxy routes without new exact-target evidence.

## Immediate next step

Start the next bounded feature/fix branch from current `dev`. Preserve the accepted Build 612 runtime contract unless new contradictory device evidence appears.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. task-specific architecture / reference docs;
4. current source and exact-target SystemUI evidence;
5. relevant `DEVLOG.md` history when needed.

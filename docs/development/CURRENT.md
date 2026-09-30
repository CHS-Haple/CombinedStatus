# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.3.
- Integrated stable runtime baseline on `main` / `dev`: Build 473.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- Build 472 companion-app presentation and Build 473 Wi-Fi optical normalization remain protected accepted behavior.

## Active objective

Close the shared Home / Keyguard -> Control Center transition line and integrate PR #177 (`feat/control-center-transition-matrix`) into `dev`.

Current runtime checkpoint:
- Build 511 / `20260930-511`;
- Build 510 non-charging Home transition is device-accepted;
- Build 511 adds review-driven runtime-authority / Fail-native hardening without retuning the accepted Build-510 non-charging geometry or motion;
- runtime is frozen pending the remaining focused device gates.

## Current transition contract

- HyperOS is the sole expansion / appearance timeline authority.
- Home and Keyguard may bridge only through the verified QS_FAKE transition interval; fully expanded Control Center remains native-owned.
- Notification Shade and AOD remain native-only on the pinned target.
- Guiyuan does not write native peer translation, alpha, visibility, or a second gesture animator.
- `statusIcons.paddingEnd` is the single Guiyuan peer-layout writer.
- Transition reservation freezes the final total semantic width, then interpolates compact -> final width directly from raw HyperOS expansion progress.
- Final role-6 top-level slots are read-only occupancy / root-space geometry witnesses.
- Exact drawable / child topology may refine optical geometry but does not grant native layout ownership.
- Mobile outer motion remains similarity/carrier based. Exact four-bar geometry is shape-local and available only for positively verified `FOUR_VERTICAL_BARS` topology.
- Composite / dual-row / unknown Mobile topology stays on the conservative fallback path.
- Latent Airplane / No-SIM / additional-SIM reveal is spatial: real reservation must open before pixels appear; no duration, delay, fraction threshold, or local animator owns reveal timing.
- Home and Keyguard keep independent mutable host/session ownership.
- Keyguard Control Center lease ends on authoritative boundaries such as native fraction zero, AOD block, feature disable, Keyguard disable, host loss, or source-scene change.
- Build-channel diagnostics flags are observation-only. Release and Canary share functional hooks, state authority, ownership/lifecycle, and Fail-native control flow.

## Validation state

Confirmed:
- Build 510 non-charging Home transition: device accepted.
- Build 511 static code review: complete.
- Runtime / unit validation: green after the Build-511 safety fixes and added negative policy coverage.
- Keyguard lease negative boundaries are unit-tested.
- Eight-component dual-row / composite Mobile is unit-tested to expose no exact four-bar capability.
- Release / Canary functional control-flow parity was reviewed after moving Island status authority outside the diagnostics gate.
- PR #177 is Ready, mergeable, and intentionally unmerged until the remaining device evidence passes.
- Superseded PRs #174, #161, #117, and #99 are closed. PR #157 is an independent Gradle-wrapper update and remains deferred pending trusted validation.

## Remaining device gates

1. **Home charging, no native island**
   - preserve the accepted Build-510 trajectory and Mobile morph;
   - no press-entry left shift or endpoint drift.

2. **Home charging, native island showing**
   - native peer motion remains authoritative;
   - no whole-row rebase, overlap, left offset, or endpoint drift.

3. **Latent supplemental semantics**
   - Airplane / No-SIM / additional SIM reveal continuously only after real peer space opens;
   - reverse collapse hides before reservation closes through neighboring content.

4. **Real composite / dual-row Mobile**
   - confirm real third-party topology remains visually on the composite fallback path;
   - no flattening into the exact four-bar morph.

5. **Final Keyguard-originated regression**
   - steady Keyguard -> partial/full Control Center -> return remains responsive;
   - no terminal stall, duplicate native row, stale lease, or cleanup residue.

## Non-negotiable boundaries

- Root-cause first; no speculative geometry or timing compensation.
- Preserve the Build-510 accepted non-charging result unless contradictory device evidence appears.
- Preserve Build-504 root-space endpoint ownership, Build-507 reservation behavior, Build-509 spatial latent reveal, and Build-491 / 497 / 498 lifecycle/safety boundaries.
- One mutable runtime property has one writer.
- No polling, delayed lifecycle fixes, duplicate state machines, duplicate gesture animators, or high-frequency diagnostics.
- Cleanup / Hot Reload restores only Guiyuan-owned state.
- Compatibility uncertainty fails native.
- HyperOS resources / state / motion are preferred over project-local copies or guesses.

## Immediate next step

Do not change runtime without new device evidence.

When the required scene becomes available:
1. generate one signed exact-head Build-511 Canary;
2. test charging without island and charging with native island separately;
3. use the same package for latent / dual-row / Keyguard regression where practical;
4. change only a failed sub-path and preserve accepted paths;
5. once all required device gates pass, mark the branch complete and squash-merge PR #177 into `dev`.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. task-specific architecture / reference docs;
4. current source and exact-target SystemUI evidence;
5. relevant `DEVLOG.md` history when needed.

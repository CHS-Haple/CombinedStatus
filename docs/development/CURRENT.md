# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.3.
- `main` and `dev` are identical at `d773b67d19abc45b576c286e5fc6e7a4032fbe98`; their integrated runtime baseline remains Build 511 plus repository-governance updates.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- Build 510 non-charging Home and Build 511 charging Home transition behavior remain protected accepted baselines.
- Build 473 Wi-Fi optical normalization and the accepted companion-app presentation baseline remain protected.

## Active objective

PR #181 / `feat/battery-top-readout` is the active product/runtime line. It is based directly on current `dev`, is not behind `dev`, and contains the battery-top readout/customization work, battery-color scheme UI, preview/settings refinements, and follow-up Control Center transition corrections.

Current runtime checkpoint:
- Build 612 / `20261002-612`, runtime code SHA `0f8128c5e2cf8ad1c8715acf7aa776a0ad09ef2a`.
- Exact-head Runtime run `36962917018` succeeded.
- Maintainer device validation reports the latest Build 612 has no anomaly.
- No additional device gate is required before PR integration unless later code changes runtime behavior.

## Current transition contract

- HyperOS remains the sole expansion / appearance timeline authority.
- Home and Keyguard bridge only through the verified QS_FAKE interval; fully expanded Control Center remains native-owned.
- Notification Shade and AOD remain native-only on the pinned target.
- Guiyuan does not write native peer translation, alpha, visibility, visibleState, or a second gesture animator.
- `statusIcons.paddingEnd` remains the only progress-driven peer-layout property.
- QS_FAKE may acquire one fixed, reversible session capacity lease only after HyperOS establishes a concrete native carrier width and only from already-unused, verified end-anchored parent capacity.
- The capacity lease is measurement-only. Transition motion samples an end-anchored logical carrier whose width is frozen from the native source motion carrier, so lease-only leading width cannot shift the motion origin.
- Any later carrier-width change is treated as a competing writer; Guiyuan relinquishes ownership and fails native instead of racing HyperOS.
- Compact cutover waits for the capacity-induced native layout and re-validates the end anchor.
- Mobile exact four-bar geometry remains shape-local; composite/dual-row/unknown topology stays on the conservative fallback path.
- Latent Airplane / No-SIM / additional-SIM reveal remains spatial and reservation-gated, without timer/delay ownership.
- Home and Keyguard keep independent host/session ownership and cleanup.
- Build-channel diagnostics remain observation-only; Release and Canary share functional control flow.

## Build 612 conclusion

Build 611 solved the late QS_FAKE native-peer underflow by leasing fixed leading capacity, but the full widened fake status-icon row was then sampled as transition motion geometry. On the verified topology the row changed 478 -> 728px while retaining the same end edge, moving its raw center about 125px left and causing the whole Guiyuan transition to jump.

Build 612 keeps the fixed capacity lease but freezes the native source motion-carrier width. The live widened row is projected to an end-anchored logical carrier of that frozen width before source rebasing. This separates native measurement capacity from transition motion without hard-coded offsets, device-specific width assumptions, new writers, or a new timing path. LTR and RTL projection are unit-covered.

## Validation state

Confirmed:
- Build 612 static review: no ownership/lifecycle/timing regression found in the capacity/motion separation.
- Runtime CI at the Build-612 runtime head: green.
- PR #181 has no unresolved review threads at the Build-612 checkpoint.
- Maintainer device pass: latest Build 612 reports no anomaly.
- The Build-611 initial left-jump regression is therefore closed without reverting the Build-609/611 peer-capacity correction.

No current device blocker:
- the next commit is documentation-only and does not invalidate the accepted Build-612 APK evidence;
- do not generate another Canary unless a later runtime/UI change creates a new device decision gate.

## Non-negotiable boundaries

- Root-cause first; no screenshot-fitted geometry/timing compensation.
- Preserve accepted Build-510/511 transition behavior unless contradictory device evidence appears.
- One mutable runtime property has one writer.
- Cleanup / Hot Reload restores only Guiyuan-owned state.
- No polling, delayed lifecycle repair, duplicate gesture animator, or high-frequency diagnostics.
- Compatibility uncertainty fails native.
- HyperOS/MIUIX/native resources, state, layout semantics, and motion remain preferred over project-local imitation.
- Do not revive rejected Battery-Island `batteryWidthDiff`, peer-`forceAppear`, or generic-island proxy routes without new exact-target evidence.

## Immediate next step

1. let the documentation-only PR checkpoint complete automated validation;
2. if green, squash-merge PR #181 into `dev`;
3. verify the resulting `dev` Runtime integration and branch state;
4. with no new runtime delta or device blocker, continue the normal stable-promotion path from the integrated `dev` baseline.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. task-specific architecture / reference docs;
4. current source and exact-target SystemUI evidence;
5. relevant `DEVLOG.md` history when needed.

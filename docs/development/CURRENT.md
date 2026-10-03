# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.5.
- `main` remains on the promoted Build 618 stable checkpoint.
- `dev` remains on accepted Build 619 and is the integration base for the active work branch.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- GPL-3.0-only remains the project license.

## Active objective

Branch: `feat/battery-fill-retract-follow` / PR #197.

Build 668 device evidence resolves the remaining QS_FAKE island-input ambiguity. The current fake `MiuiStatusIconContainer` owns an `IslandMonitor$FakeContainerIslandMonitor` with native `islandShowing=true`, `islandWidth=147`, `getIslandShowing()`, `getIslandWidth()`, and a direct `StatusBarIslandControllerImpl` reference. The controller exposes `statusContainerSpace`. This confirms the production fake-row island contract is the monitor width path; the Build-665 `MiuiStatusIconContainer.getIslandShowing()` seam is rejected and must not return.

Build 669 is the focused correction:
- hook only `FakeContainerIslandMonitor.getIslandWidth()`;
- scope interception by object identity to the monitor owned by the current QS_FAKE presentation Session;
- preserve the native width while the current fake native-peer band intersects the live HyperOS island rectangle;
- expose width 0 only after true 2D separation, so the translated fake row no longer consumes Home's one-dimensional island constraint;
- keep the Build-663 compact-to-final end reservation as the single Guiyuan horizontal occupancy writer;
- keep Home, Keyguard, final Control Center and unrelated monitor instances unchanged;
- fail native for this correction if monitor identity or live geometry cannot be established.

The existing island-status Hook now retains the exact controller/injector path in all build channels because the live island rectangle is functional authority for Build 669. Detailed diagnostics remain observation-only.

## Validation state

Candidate: 0.0.5 / versionCode 261003669 / Build 20261003-669.

Required automated gate: exact-head Runtime CI.

Required device gate after Runtime passes:
- active charging island + dual SIM, slow Home -> Control Center pull and reverse;
- one ordinary no-island pull;
- detailed diagnostic confirming `islandWidth2DGate` changes from overlap=true/native width to overlap=false/width 0 and re-engages on reverse;
- no peer/island collision, no extra disappearance after vertical separation, no full-row jump, and no fail-native fallback on the pinned target.

## Non-negotiable boundaries

- no peer state, alpha, visibility or translation writes;
- no island rectangle or monitor `islandWidth` field writes;
- no custom gesture timeline, delay, timer, polling or frame follower;
- no fixed fake-carrier capacity lease and no Build-652 island-translation compensation;
- no revival of the rejected Build-665 container getter seam.

## Immediate next step

Review Build 669, run exact-head Runtime CI, then request one exact-head Work Branch Canary and freeze runtime for the focused device gate.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. Build-668 device diagnostic;
4. exact-target island monitor evidence / scene architecture;
5. relevant Build 652-668 `DEVLOG.md` history.

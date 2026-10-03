# Current Development State

This file is the concise recovery point for active Guiyuan development. Historical Build chronology, rejected hypotheses, detailed CI records, and device-by-device reasoning belong in `DEVLOG.md`.

## Accepted baseline

- Product / display version: Guiyuan 0.0.5.
- `main` remains on the promoted 0.0.5 / Build 618 stable checkpoint.
- `dev` has advanced to accepted Build 619: `0.0.5` / versionCode `261002419` / Build `20261002-619`.
- Build 619 is the latest accepted runtime-affecting development baseline.
- Verified target: Xiaomi HyperOS SystemUI 17.03.260226.r, Android 17 / SDK 37, Modern Xposed API 102.
- GPL-3.0-only remains the project license.

## Integrated Build 619 scope

PR #195 added two independent network-state visual controls:
- Airplane mode size: 40%-125%, default 100%.
- No-SIM size: 40%-125%, default 100%.

Both controls are profile-scoped like Wi-Fi/mobile-type sizing, so Network centered and Battery centered remember separate values. Wi-Fi and mobile-type sizing remain independent.

Renderer ownership remains narrow:
- HyperOS/native resources remain the drawable source.
- Each new setting changes only the matching resource draw-size constraint and the same resolved geometry consumed by optical avoidance / transition-source rendering.
- No state source, slot ownership, native peer layout, gesture timeline, timing, alpha, visibility, or translation writer was added.

## Validation evidence

- PR #195 merged to `dev` as `d59b7452cfe7abcad9a48f8ddbf00812adc00da5`.
- Work Branch Canary #650 validated Build 619 on the exact requested work-branch source; trusted checkout/build/signature/non-debuggable checks succeeded.
- Maintainer device validation on Xiaomi 15 Pro accepted the Airplane / No-SIM sizing behavior and independent content-layout memory with no visible regression requiring another runtime change.
- Returned detailed diagnostics report `overall=healthy` on Build 619; the durable device conclusion and the diagnostic-summary limitation are recorded in `DEVLOG.md`.
- Final PR Runtime CI #2230 succeeded after synchronizing latest `dev` ancestry and recording device evidence.
- Trusted `dev` integration CI #2231 succeeded on merge commit `d59b7452cfe7abcad9a48f8ddbf00812adc00da5`, including tests/build, pinned HyperOS target verification, Modern Xposed metadata, Haple APK signature, non-debuggable Canary verification, and Canary artifact upload.

## Active objective

Branch: `feat/battery-fill-retract-follow` / PR #197.

Build 660 is device-rejected: its island-only fixed fake-carrier capacity lease preserved peers only by widening QS_FAKE enough to remove HyperOS island avoidance, while the row still settled horizontally at gesture entry. The supplied Build-660 report is from the expected Canary / exact target.

Build 661 is superseded by review before Canary. It removed the fixed lease and re-enabled progress-synchronous `statusIcons.paddingEnd` only under exact island-native-layout authority. That is structurally cleaner than Build 660 and Runtime CI is green, but it cannot explain the already-proven Build-658 first-bucket failure: with zero project padding and zero fixed capacity lease, QS_FAKE `network_speed` / `vpn` were already terminal/hidden. Adding positive end padding reduces usable width further; it has no mechanism to restore those peers.

Build 662 therefore returns island runtime behavior to the Build-658/655 ownership boundary and changes diagnostics only:
- no island fake-carrier capacity lease;
- no island project end-padding;
- no represented-slot re-exclusion;
- no `getIslandShowing()` / `getIslandTranslationX()` semantic override;
- no peer state / alpha / visibility / translation / island-width write;
- extend the existing bounded `islandProbe` from v2 to v3 with the steady source status row beside QS_FAKE and final QS;
- record each sampled child’s actual screen X/Y in addition to layout-local state.

The missing causal fact is now precise: whether a peer such as `network_speed` / `vpn` is already terminal in the Home source row, or becomes terminal only when QS_FAKE computes its own `NewStatusIconState`.

## Validation state

- Candidate identity: `0.0.5` / versionCode `261003662` / Build `20261003-662`.
- Build 661 Runtime CI `37089713115` succeeded, but 661 is intentionally not Canary-tested because review found the zero-padding contradiction above.
- Build 662 must receive exact-HEAD Runtime CI and one signed Canary because the new source-row evidence is runtime-only.
- Runtime behavior is intentionally restored to the Build-658 island path; this checkpoint is diagnostic, not a visual fix.

## Device gate

One active-island slow Home -> Control Center pull is sufficient, preferably with the same charging-island + dual-SIM setup. Export one detailed diagnostic.

Expected `islandProbe=v3` evidence must contain, in the same expansion buckets:
- `source=` Home status row;
- `fake=` QS_FAKE row;
- `final=` final QS row;
- per-peer `visibleState / inIslandState / beforeInIslandState / layoutTranslationX / sx / sy`.

Decision rule:
- source normal, fake terminal at first bucket -> fix the source-to-QS_FAKE handoff/state calculation;
- source already terminal before fake -> fix Home island occupancy/source state;
- source/fake state match but screen X diverges -> inspect carrier/root transform ownership instead of state membership.

No Keyguard/AOD validation is required for this checkpoint.

## Immediate next step

Review Build 662, run exact-HEAD Runtime CI, then one signed Canary and freeze runtime for the single source-to-fake diagnostic pass.

## Reference priority

1. `CONTRIBUTING.md`;
2. this file;
3. current source / exact-target SystemUI evidence;
4. task-specific architecture/reference docs;
5. relevant `DEVLOG.md` history.
